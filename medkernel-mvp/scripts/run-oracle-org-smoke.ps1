param(
  [string]$BaseUrl = "http://localhost:18081/medkernel/api",
  [string]$Connect = $env:MEDKERNEL_DB_CONNECT,
  [string]$Username = $env:MEDKERNEL_DB_USERNAME,
  [string]$Password = $env:MEDKERNEL_DB_PASSWORD,
  [string]$TenantId = "default",
  [string]$HospitalCode = "ZYHOSPITAL"
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
if ([string]::IsNullOrWhiteSpace($Connect)) {
  $Connect = "//192.168.4.25:1521/ORCL"
}
if ([string]::IsNullOrWhiteSpace($Username)) {
  $Username = "MEDKERNEL"
}
if ([string]::IsNullOrWhiteSpace($Password)) {
  throw "Please set MEDKERNEL_DB_PASSWORD before running this script."
}

$sqlplus = (Get-Command sqlplus -ErrorAction Stop).Source
$engineRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $engineRoot
$patientFile = Join-Path $workspaceRoot "ai-dev-input\06_samples\sample_patient_context_ami.json"

if (!(Test-Path -LiteralPath $patientFile)) {
  throw "Patient sample file not found: $patientFile"
}

function Escape-SqlLiteral {
  param([string]$Value)
  return $Value.Replace("'", "''")
}

function Read-Count {
  param(
    [string[]]$Lines,
    [string]$Name
  )
  $line = $Lines | Where-Object { $_.Trim().StartsWith("$Name=") } | Select-Object -First 1
  if (-not $line) {
    throw "SQLPlus output does not contain $Name count. Output: $($Lines -join ' | ')"
  }
  return [int]($line.Trim().Substring($Name.Length + 1))
}

$health = Invoke-RestMethod -Uri "$BaseUrl/health" -Method Get
if (-not $health.success) {
  throw "Health check failed."
}

$stamp = Get-Date -Format "yyyyMMddHHmmss"
$ruleCode = "R_ORACLE_ORG_SMOKE_$stamp"
$packageCode = "PKG_ORACLE_ORG_SMOKE"
$packageVersion = "2026.05"
$sourceDocCode = "SRC_ORACLE_ORG_SMOKE_$stamp"
$patient = Get-Content -LiteralPath $patientFile -Raw -Encoding UTF8 | ConvertFrom-Json
$patient.encounter.encounter_id = "E_ORACLE_ORG_SMOKE_$stamp"

$headers = @{
  "X-Tenant-Id" = $TenantId
  "X-Hospital-Code" = $HospitalCode
}

$sourceDocBody = @{
  tenant_id = $TenantId
  operator_id = "ORACLE_ORG_SMOKE"
  documents = @(
    @{
      document_code = $sourceDocCode
      title = "Oracle smoke来源文档"
      source_type = "GUIDELINE"
      source_uri = "https://example.org/smoke/oracle-org"
      publisher = "MedKernel Smoke"
      effective_date = "2026-01-01"
      expiry_date = "2030-12-31"
      review_status = "REVIEWED"
      reviewed_by = "ORACLE_ORG_SMOKE"
      reviewed_time = (Get-Date).ToString("o")
      content_hash = "sha256:oracle-org-smoke-$stamp"
    }
  )
} | ConvertTo-Json -Depth 20
$sourceDocImport = Invoke-RestMethod -Uri "$BaseUrl/provenance/source-documents" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $sourceDocBody
if (-not $sourceDocImport.success -or $sourceDocImport.data.imported_count -ne 1) {
  throw "Source document import failed."
}

$ruleImportBody = @{
  tenant_id = $TenantId
  hospital_code = $HospitalCode
  org_code = $HospitalCode
  package_code = $packageCode
  package_version = $packageVersion
  rules = @(
    @{
      rule_code = $ruleCode
      rule_name = "Oracle org context smoke rule"
      rule_type = "PATHWAY_ENTRY"
      version_no = "1.0.0"
      priority = 100
      enabled = $true
      severity = "HIGH"
      reference_document_code = $sourceDocCode
      reference_citation_id = "CIT_ORACLE_ORG_SMOKE_$stamp"
      reference_binding_type = "EVIDENCE"
      condition = @{
        all = @(
          @{
            fact = "chief_complaints.code"
            operator = "in"
            value = @("CHEST_PAIN")
          },
          @{
            fact = "exams.finding_codes"
            operator = "contains"
            value = "ST_ELEVATION_CONTIGUOUS_LEADS"
          }
        )
      }
      actions = @(
        @{
          type = "CREATE_RECOMMENDATION"
          target = "AMI_STEMI"
        }
      )
      message_template = "Oracle org smoke rule hit."
    }
  )
} | ConvertTo-Json -Depth 30

$imported = Invoke-RestMethod -Uri "$BaseUrl/rules" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $ruleImportBody
if (-not $imported.success -or $imported.data.Count -ne 1) {
  throw "Rule import failed."
}

$publishBody = @{
  tenant_id = $TenantId
  hospital_code = $HospitalCode
  org_code = $HospitalCode
  version_no = "1.0.0"
  approved_by = "ORACLE_ORG_SMOKE"
} | ConvertTo-Json -Depth 10
$published = Invoke-RestMethod -Uri "$BaseUrl/rules/$ruleCode/publish" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $publishBody
if (-not $published.success -or $published.data.status -ne "PUBLISHED") {
  throw "Rule publish failed."
}

$simulateBody = @{
  tenant_id = $TenantId
  hospital_code = $HospitalCode
  org_code = $HospitalCode
  rule_code = $ruleCode
  version_no = "1.0.0"
  patient_context = $patient
} | ConvertTo-Json -Depth 30
$simulate = Invoke-RestMethod -Uri "$BaseUrl/rules/simulate" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $simulateBody
if (-not $simulate.success -or -not $simulate.data.hit) {
  throw "Rule simulation did not hit."
}

$tenantSql = Escape-SqlLiteral $TenantId
$hospitalSql = Escape-SqlLiteral $HospitalCode
$ruleSql = Escape-SqlLiteral $ruleCode
$connectSql = Escape-SqlLiteral $Connect
$usernameSql = Escape-SqlLiteral $Username
$passwordSql = Escape-SqlLiteral $Password

$sql = @"
CONNECT $usernameSql/$passwordSql@$connectSql
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET HEADING OFF FEEDBACK OFF PAGESIZE 0 VERIFY OFF ECHO OFF TRIMSPOOL ON
SELECT 'RULE_DEF=' || COUNT(*)
  FROM re_rule_def
 WHERE tenant_id = '$tenantSql'
   AND org_code = '$hospitalSql'
   AND rule_code = '$ruleSql';
SELECT 'RULE_EXEC=' || COUNT(*)
  FROM re_rule_exec_log
 WHERE tenant_id = '$tenantSql'
   AND hospital_code = '$hospitalSql'
   AND scope_level = 'HOSPITAL'
   AND scope_code = '$hospitalSql'
   AND rule_code = '$ruleSql';
SELECT 'AUDIT=' || COUNT(*)
  FROM engine_audit_log
 WHERE tenant_id = '$tenantSql'
   AND hospital_code = '$hospitalSql'
   AND scope_level = 'HOSPITAL'
   AND scope_code = '$hospitalSql'
   AND target_code = '$ruleSql';
SELECT 'SOURCE_DOC=' || COUNT(*)
  FROM src_document
 WHERE tenant_id = '$tenantSql'
   AND document_code = '$(Escape-SqlLiteral $sourceDocCode)';
EXIT
"@

$sqlOutput = $sql | & $sqlplus -S /nolog
if ($LASTEXITCODE -ne 0) {
  throw "SQLPlus org smoke query failed with exit code $LASTEXITCODE. Output: $($sqlOutput -join ' | ')"
}
$ruleDefCount = Read-Count -Lines $sqlOutput -Name "RULE_DEF"
$ruleExecCount = Read-Count -Lines $sqlOutput -Name "RULE_EXEC"
$auditCount = Read-Count -Lines $sqlOutput -Name "AUDIT"
$sourceDocCount = Read-Count -Lines $sqlOutput -Name "SOURCE_DOC"

if ($ruleDefCount -lt 1) {
  throw "Oracle RE_RULE_DEF org persistence check failed."
}
if ($ruleExecCount -lt 1) {
  throw "Oracle RE_RULE_EXEC_LOG org persistence check failed."
}
if ($auditCount -lt 1) {
  throw "Oracle ENGINE_AUDIT_LOG org persistence check failed."
}
if ($sourceDocCount -lt 1) {
  throw "Oracle SRC_DOCUMENT persistence check failed."
}

Write-Host "Oracle org smoke test passed."
Write-Host "Rule: $ruleCode"
Write-Host "Tenant/Hospital: $TenantId/$HospitalCode"
Write-Host "Counts: RE_RULE_DEF=$ruleDefCount, RE_RULE_EXEC_LOG=$ruleExecCount, ENGINE_AUDIT_LOG=$auditCount, SRC_DOCUMENT=$sourceDocCount"
