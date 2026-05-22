param(
  [string]$BaseUrl = "http://localhost:18081/medkernel/api",
  [string]$Connect = $env:MEDKERNEL_DB_CONNECT,
  [string]$Username = $env:MEDKERNEL_DB_USERNAME,
  [string]$Password = $env:MEDKERNEL_DB_PASSWORD,
  [string]$TenantId = "TENANT_DEMO",
  [string]$GroupCode = "GROUP_DEMO",
  [string]$HospitalCode = "HOSPITAL_DEMO",
  [string]$DepartmentCode = "DEPT_CARDIOLOGY",
  [string]$AuthUsername = $env:MEDKERNEL_SEED_USERNAME,
  [string]$AuthPassword = $env:MEDKERNEL_SEED_PASSWORD,
  [string]$SamplesDir = "",
  [switch]$SkipCleanup
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$env:NLS_LANG = "AMERICAN_AMERICA.AL32UTF8"

. (Join-Path $PSScriptRoot "oracle-env.ps1")
Import-MedKernelOracleLocalEnv -ScriptRoot $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($Connect)) {
  $Connect = $env:MEDKERNEL_DB_CONNECT
}
if ([string]::IsNullOrWhiteSpace($Username)) {
  $Username = $env:MEDKERNEL_DB_USERNAME
}
if ([string]::IsNullOrWhiteSpace($Password)) {
  $Password = $env:MEDKERNEL_DB_PASSWORD
}
if ([string]::IsNullOrWhiteSpace($Connect) -and $env:MEDKERNEL_DB_URL -match "^jdbc:oracle:thin:@(.+)$") {
  $Connect = $matches[1]
}

if ([string]::IsNullOrWhiteSpace($Connect)) {
  throw "Please set MEDKERNEL_DB_CONNECT or MEDKERNEL_DB_URL before seeding Oracle demo data."
}
if ([string]::IsNullOrWhiteSpace($Username)) {
  throw "Please set MEDKERNEL_DB_USERNAME before seeding Oracle demo data."
}
if ([string]::IsNullOrWhiteSpace($Password)) {
  throw "Please set MEDKERNEL_DB_PASSWORD before seeding Oracle demo data."
}
if ([string]::IsNullOrWhiteSpace($AuthUsername)) {
  $AuthUsername = "admin"
}
if ([string]::IsNullOrWhiteSpace($AuthPassword)) {
  $AuthPassword = "demo123"
}

$sqlplus = (Get-Command sqlplus -ErrorAction Stop).Source
$engineRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $engineRoot
if (-not $SamplesDir) {
  $SamplesDir = Join-Path $workspaceRoot "ai-dev-input\06_samples"
}
if (!(Test-Path -LiteralPath $SamplesDir)) {
  throw "Samples directory not found: $SamplesDir"
}

$headers = @{
  "X-Tenant-Id" = $TenantId
  "X-Group-Code" = $GroupCode
  "X-Hospital-Code" = $HospitalCode
  "X-Department-Code" = $DepartmentCode
}

function Sample-Path {
  param([string]$FileName)
  $path = Join-Path $SamplesDir $FileName
  if (!(Test-Path -LiteralPath $path)) {
    throw "Sample file not found: $path"
  }
  return $path
}

function Invoke-JsonPostFile {
  param(
    [string]$Uri,
    [string]$Path
  )
  $bytes = [System.IO.File]::ReadAllBytes($Path)
  Invoke-RestMethod -Uri $Uri -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $bytes
}

function Invoke-JsonPost {
  param(
    [string]$Uri,
    [object]$Body
  )
  $json = $Body | ConvertTo-Json -Depth 50
  $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
  Invoke-RestMethod -Uri $Uri -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $bytes
}

function Escape-SqlLiteral {
  param([string]$Value)
  if ($null -eq $Value) {
    return ""
  }
  return $Value.Replace("'", "''")
}

function Invoke-SqlPlus {
  param([string]$Sql)
  $output = $Sql | & $sqlplus -S /nolog
  if ($LASTEXITCODE -ne 0) {
    throw "SQLPlus failed with exit code $LASTEXITCODE. Output: $($output -join ' | ')"
  }
  return $output
}

function Read-Count {
  param(
    [string[]]$Lines,
    [string]$Name
  )
  $line = $Lines | Where-Object { $_.Trim().StartsWith("$Name=") } | Select-Object -First 1
  if (-not $line) {
    throw "SQLPlus output does not contain $Name. Output: $($Lines -join ' | ')"
  }
  return [int]($line.Trim().Substring($Name.Length + 1))
}

$health = Invoke-RestMethod -Uri "$BaseUrl/health" -Method Get
if (-not $health.success) {
  throw "Health check failed."
}

$loginBody = @{
  username = $AuthUsername
  password = $AuthPassword
} | ConvertTo-Json -Depth 5
$loginBytes = [System.Text.Encoding]::UTF8.GetBytes($loginBody)
$login = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method Post -ContentType "application/json; charset=utf-8" -Body $loginBytes
if (-not $login.success -or [string]::IsNullOrWhiteSpace($login.data.token)) {
  throw "Seed login failed for user $AuthUsername."
}
$headers["Authorization"] = "Bearer $($login.data.token)"

$providers = Invoke-RestMethod -Uri "$BaseUrl/system/providers" -Method Get -Headers $headers
$database = $providers.data.providers.database
if (-not $database.ready -or $database.provider -ne "ORACLE" -or -not $database.production_ready) {
  throw "Backend is not running in Oracle production mode. Current database provider=$($database.provider), ready=$($database.ready), production_ready=$($database.production_ready)."
}

$tenantSql = Escape-SqlLiteral $TenantId
$hospitalSql = Escape-SqlLiteral $HospitalCode
$connectSql = Escape-SqlLiteral $Connect
$usernameSql = Escape-SqlLiteral $Username
$passwordSql = Escape-SqlLiteral $Password

if (-not $SkipCleanup) {
  $cleanupSql = @"
CONNECT $usernameSql/$passwordSql@$connectSql
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET HEADING OFF FEEDBACK OFF PAGESIZE 0 VERIFY OFF ECHO OFF TRIMSPOOL ON
DELETE FROM re_rule_eval_result WHERE tenant_id = '$tenantSql';
DELETE FROM re_rule_exec_log WHERE tenant_id = '$tenantSql';
DELETE FROM engine_audit_log WHERE tenant_id = '$tenantSql';
DELETE FROM pe_variation_record WHERE tenant_id = '$tenantSql';
DELETE FROM pe_patient_task_state
 WHERE instance_id IN (SELECT id FROM pe_patient_instance WHERE tenant_id = '$tenantSql' AND org_code = '$hospitalSql');
DELETE FROM pe_patient_node_state
 WHERE instance_id IN (SELECT id FROM pe_patient_instance WHERE tenant_id = '$tenantSql' AND org_code = '$hospitalSql');
DELETE FROM pe_patient_instance WHERE tenant_id = '$tenantSql' AND org_code = '$hospitalSql';
DELETE FROM pe_pathway_def WHERE tenant_id = '$tenantSql' AND org_code = '$hospitalSql';
DELETE FROM re_rule_def WHERE tenant_id = '$tenantSql' AND org_code = '$hospitalSql';
DELETE FROM cfg_config_package WHERE tenant_id = '$tenantSql';
DELETE FROM src_asset_binding WHERE tenant_id = '$tenantSql';
DELETE FROM src_citation WHERE tenant_id = '$tenantSql';
DELETE FROM src_document WHERE tenant_id = '$tenantSql';
DELETE FROM org_unit WHERE tenant_id = '$tenantSql';
COMMIT;
EXIT
"@
  Invoke-SqlPlus -Sql $cleanupSql | Out-Null
}

$orgs = Invoke-JsonPostFile -Uri "$BaseUrl/organizations" -Path (Sample-Path "sample_org_units.json")
if (-not $orgs.success -or $orgs.data.imported_count -lt 1) {
  throw "Organization import failed."
}

$sourceDocs = Invoke-JsonPostFile -Uri "$BaseUrl/provenance/source-documents" -Path (Sample-Path "sample_source_documents.json")
if (-not $sourceDocs.success -or $sourceDocs.data.imported_count -lt 1) {
  throw "Source documents import failed."
}

$sourceCitations = Invoke-JsonPostFile -Uri "$BaseUrl/provenance/citations" -Path (Sample-Path "sample_source_citations.json")
if (-not $sourceCitations.success -or $sourceCitations.data.imported_count -lt 1) {
  throw "Source citations import failed."
}

$sourceBindings = Invoke-JsonPostFile -Uri "$BaseUrl/provenance/bindings" -Path (Sample-Path "sample_source_bindings.json")
if (-not $sourceBindings.success -or $sourceBindings.data.imported_count -lt 1) {
  throw "Source bindings import failed."
}

$rules = Invoke-JsonPostFile -Uri "$BaseUrl/rules" -Path (Sample-Path "sample_ami_rules.json")
if (-not $rules.success -or $rules.data.Count -lt 3) {
  throw "Rules import failed."
}

$ruleReview = Invoke-RestMethod -Uri "$BaseUrl/rules/packages/PKG_AMI_CORE/review?packageVersion=2026.05" -Method Get -Headers $headers
if (-not $ruleReview.success -or -not $ruleReview.data.ready_to_publish) {
  throw "Rule package review failed."
}

$rulePublish = Invoke-JsonPost -Uri "$BaseUrl/rules/packages/PKG_AMI_CORE/publish" -Body @{
  package_version = "2026.05"
  approved_by = "DEMO_APPROVER"
}
if (-not $rulePublish.success -or $rulePublish.data.published_count -lt 3) {
  throw "Rule package publish failed."
}

$pathway = Invoke-JsonPostFile -Uri "$BaseUrl/pathways" -Path (Sample-Path "sample_ami_pathway.json")
if (-not $pathway.success -or $pathway.data.validation -ne "PASSED") {
  throw "Pathway import failed."
}

$pathwayPublish = Invoke-JsonPost -Uri "$BaseUrl/pathways/AMI_STEMI/publish" -Body @{
  version_no = "1.0.0"
  approved_by = "DEMO_APPROVER"
}
if (-not $pathwayPublish.success -or $pathwayPublish.data.status -ne "PUBLISHED") {
  throw "Pathway publish failed."
}

$configImport = Invoke-JsonPostFile -Uri "$BaseUrl/config-packages/import" -Path (Sample-Path "sample_config_package.json")
if (-not $configImport.success -or $configImport.data.Count -lt 1) {
  throw "Config package import failed."
}

$configReview = Invoke-JsonPost -Uri "$BaseUrl/config-packages/PKG_AMI_RULE_CONFIG/2026.05.01/review" -Body @{
  reviewed_by = "DEMO_REVIEWER"
}
if (-not $configReview.success -or -not $configReview.data.ready_to_publish) {
  throw "Config package review failed."
}

$configPublish = Invoke-JsonPost -Uri "$BaseUrl/config-packages/PKG_AMI_RULE_CONFIG/2026.05.01/publish" -Body @{
  approved_by = "DEMO_APPROVER"
  approved_note = "Demo baseline approved for customer validation."
}
if (-not $configPublish.success -or $configPublish.data.status -ne "PUBLISHED") {
  throw "Config package publish failed."
}

$patient = Get-Content -LiteralPath (Sample-Path "sample_patient_context_ami.json") -Raw -Encoding UTF8 | ConvertFrom-Json
$patient.encounter.encounter_id = "E_DEMO_ORACLE_" + (Get-Date -Format "yyyyMMddHHmmss")
$evaluate = Invoke-JsonPost -Uri "$BaseUrl/rule-engine/evaluate" -Body @{
  scenario_code = "PATHWAY_ENTRY"
  rule_package_code = "PKG_AMI_CORE"
  rule_package_version = "2026.05"
  operator_id = "DEMO_DOCTOR"
  patient_context = $patient
  tenant_id = $TenantId
  group_code = $GroupCode
  hospital_code = $HospitalCode
  department_code = $DepartmentCode
}
if (-not $evaluate.success -or $evaluate.data.hit_count -lt 1) {
  throw "Rule-engine demo evaluation did not hit."
}

$admit = Invoke-JsonPost -Uri "$BaseUrl/patient-pathways/admit" -Body @{
  patient_id = $patient.patient.patient_id
  encounter_id = $patient.encounter.encounter_id
  pathway_code = "AMI_STEMI"
  version_no = "1.0.0"
  doctor_id = "DEMO_DOCTOR"
}
if (-not $admit.success -or $admit.data.currentNodeCode -ne "AMI_CHEST_PAIN_IDENTIFY") {
  throw "Patient pathway admission failed."
}

$instanceId = $admit.data.instanceId
$task = Invoke-JsonPost -Uri "$BaseUrl/patient-pathways/$instanceId/nodes/AMI_CHEST_PAIN_IDENTIFY/tasks/TASK_ECG/complete" -Body @{
  operator_id = "DEMO_DOCTOR"
  result = @{
    report_id = "ECG_DEMO_001"
    finding_codes = @("ST_ELEVATION_CONTIGUOUS_LEADS")
    completed_time = (Get-Date).ToString("o")
  }
}
if (-not $task.success -or $task.data.status -ne "COMPLETED") {
  throw "Pathway ECG task completion failed."
}

$complete = Invoke-JsonPost -Uri "$BaseUrl/patient-pathways/$instanceId/nodes/AMI_CHEST_PAIN_IDENTIFY/complete" -Body @{}
if (-not $complete.success -or $complete.data.currentNodeCode -ne "AMI_REPERFUSION_EVAL") {
  throw "Pathway node transition failed."
}

$verifySql = @"
CONNECT $usernameSql/$passwordSql@$connectSql
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET HEADING OFF FEEDBACK OFF PAGESIZE 0 VERIFY OFF ECHO OFF TRIMSPOOL ON
SELECT 'ORG_UNIT=' || COUNT(*) FROM org_unit WHERE tenant_id = '$tenantSql';
SELECT 'SRC_DOCUMENT=' || COUNT(*) FROM src_document WHERE tenant_id = '$tenantSql';
SELECT 'SRC_CITATION=' || COUNT(*) FROM src_citation WHERE tenant_id = '$tenantSql';
SELECT 'SRC_BINDING=' || COUNT(*) FROM src_asset_binding WHERE tenant_id = '$tenantSql';
SELECT 'RULE_DEF=' || COUNT(*) FROM re_rule_def WHERE tenant_id = '$tenantSql' AND org_code = '$hospitalSql' AND status = 'PUBLISHED';
SELECT 'CFG_PACKAGE=' || COUNT(*) FROM cfg_config_package WHERE tenant_id = '$tenantSql' AND package_code = 'PKG_AMI_RULE_CONFIG';
SELECT 'PATHWAY_DEF=' || COUNT(*) FROM pe_pathway_def WHERE tenant_id = '$tenantSql' AND org_code = '$hospitalSql' AND pathway_code = 'AMI_STEMI';
SELECT 'PATIENT_INSTANCE=' || COUNT(*) FROM pe_patient_instance WHERE tenant_id = '$tenantSql' AND org_code = '$hospitalSql' AND encounter_id = '$(Escape-SqlLiteral $patient.encounter.encounter_id)';
SELECT 'RULE_EVAL=' || COUNT(*) FROM re_rule_eval_result WHERE tenant_id = '$tenantSql' AND encounter_id = '$(Escape-SqlLiteral $patient.encounter.encounter_id)';
EXIT
"@

$verifyOutput = Invoke-SqlPlus -Sql $verifySql
$orgCount = Read-Count -Lines $verifyOutput -Name "ORG_UNIT"
$docCount = Read-Count -Lines $verifyOutput -Name "SRC_DOCUMENT"
$citationCount = Read-Count -Lines $verifyOutput -Name "SRC_CITATION"
$bindingCount = Read-Count -Lines $verifyOutput -Name "SRC_BINDING"
$ruleCount = Read-Count -Lines $verifyOutput -Name "RULE_DEF"
$pkgCount = Read-Count -Lines $verifyOutput -Name "CFG_PACKAGE"
$pathwayCount = Read-Count -Lines $verifyOutput -Name "PATHWAY_DEF"
$patientInstanceCount = Read-Count -Lines $verifyOutput -Name "PATIENT_INSTANCE"
$ruleEvalCount = Read-Count -Lines $verifyOutput -Name "RULE_EVAL"

if ($orgCount -lt 5 -or $docCount -lt 4 -or $citationCount -lt 5 -or $bindingCount -lt 5 -or
    $ruleCount -lt 3 -or $pkgCount -lt 1 -or $pathwayCount -lt 1 -or $patientInstanceCount -lt 1 -or
    $ruleEvalCount -lt 1) {
  throw "Oracle verification counts are below expected demo baseline."
}

Write-Host "Oracle demo seed completed."
Write-Host "Cleanup: $(-not $SkipCleanup)"
Write-Host "Tenant/Hospital: $TenantId / $HospitalCode"
Write-Host "Rule evaluation: result_id=$($evaluate.data.result_id), hit_count=$($evaluate.data.hit_count), trace_id=$($evaluate.data.trace_id)"
Write-Host "Pathway instance: $instanceId, current_node=$($complete.data.currentNodeCode)"
Write-Host "Counts: ORG_UNIT=$orgCount, SRC_DOCUMENT=$docCount, SRC_CITATION=$citationCount, SRC_BINDING=$bindingCount, RULE_DEF=$ruleCount, CFG_PACKAGE=$pkgCount, PATHWAY_DEF=$pathwayCount, PATIENT_INSTANCE=$patientInstanceCount, RULE_EVAL=$ruleEvalCount"
