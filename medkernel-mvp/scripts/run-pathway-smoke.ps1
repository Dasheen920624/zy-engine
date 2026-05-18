param(
  [string]$BaseUrl = "http://localhost:18080/medkernel/api"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$engineRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $engineRoot
$pathwayFile = Join-Path $workspaceRoot "ai-dev-input\06_samples\sample_ami_pathway.json"
$patientFile = Join-Path $workspaceRoot "ai-dev-input\06_samples\sample_patient_context_ami.json"

if (!(Test-Path -LiteralPath $pathwayFile)) {
  throw "Pathway file not found: $pathwayFile"
}
if (!(Test-Path -LiteralPath $patientFile)) {
  throw "Patient sample file not found: $patientFile"
}

$health = Invoke-RestMethod -Uri "$BaseUrl/health" -Method Get
if (-not $health.success) {
  throw "Health check failed."
}

function Get-ErrorBodyText {
  param($ErrorRecord)
  if ($ErrorRecord.ErrorDetails -and $ErrorRecord.ErrorDetails.Message) {
    return $ErrorRecord.ErrorDetails.Message
  }
  if ($ErrorRecord.Exception -and $ErrorRecord.Exception.Response) {
    $stream = $ErrorRecord.Exception.Response.GetResponseStream()
    if ($stream) {
      $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
      return $reader.ReadToEnd()
    }
  }
  return ""
}

$pathwayJson = Get-Content -LiteralPath $pathwayFile -Raw -Encoding UTF8
$pathway = $pathwayJson | ConvertFrom-Json
$patient = Get-Content -LiteralPath $patientFile -Raw -Encoding UTF8 | ConvertFrom-Json

$encounterId = "E_AMI_SMOKE_" + (Get-Date -Format "yyyyMMddHHmmss")
$patient.encounter.encounter_id = $encounterId

$invalidPathway = $pathwayJson | ConvertFrom-Json
$invalidPathway.stages[0].nodes[0].transitions[0].to_node = "NOT_EXISTING_NODE"
$invalidJson = $invalidPathway | ConvertTo-Json -Depth 40
$invalidAccepted = $false
try {
  Invoke-RestMethod -Uri "$BaseUrl/pathways" -Method Post -ContentType "application/json; charset=utf-8" -Body $invalidJson | Out-Null
  $invalidAccepted = $true
} catch {
  $errorBodyText = Get-ErrorBodyText $_
  $errorBody = $errorBodyText | ConvertFrom-Json
  if ($errorBody.code -ne "VALIDATION_ERROR" -or $errorBody.success) {
    throw "Invalid pathway validation response failed: $errorBodyText"
  }
}
if ($invalidAccepted) {
  throw "Invalid pathway config was accepted."
}

$created = Invoke-RestMethod -Uri "$BaseUrl/pathways" -Method Post -ContentType "application/json; charset=utf-8" -Body $pathwayJson
if (-not $created.success -or $created.data.status -ne "DRAFT" -or $created.data.validation -ne "PASSED") {
  throw "Pathway create failed."
}

$publishBody = @{
  version_no = $pathway.version
  approved_by = "SMOKE_TEST"
} | ConvertTo-Json -Depth 10
$published = Invoke-RestMethod -Uri "$BaseUrl/pathways/$($pathway.pathway_code)/publish" -Method Post -ContentType "application/json; charset=utf-8" -Body $publishBody
if (-not $published.success -or $published.data.status -ne "PUBLISHED") {
  throw "Pathway publish failed."
}

$pathwayList = Invoke-RestMethod -Uri "$BaseUrl/pathways" -Method Get
if (-not $pathwayList.success -or -not ($pathwayList.data | Where-Object { $_.pathway_code -eq $pathway.pathway_code })) {
  throw "Pathway list query failed."
}

$pathwayDetail = Invoke-RestMethod -Uri "$BaseUrl/pathways/$($pathway.pathway_code)?versionNo=$($pathway.version)" -Method Get
if (-not $pathwayDetail.success -or $pathwayDetail.data.published_config.pathway_code -ne $pathway.pathway_code) {
  throw "Pathway config detail query failed."
}

$candidateBody = $patient | ConvertTo-Json -Depth 30
$candidate = Invoke-RestMethod -Uri "$BaseUrl/patient-pathways/candidates" -Method Post -ContentType "application/json; charset=utf-8" -Body $candidateBody
if (-not $candidate.success -or $candidate.data.Count -lt 1 -or $candidate.data[0].targetCode -ne "AMI_STEMI") {
  throw "Pathway candidate recognition failed."
}

$admitBody = @{
  patient_id = $patient.patient.patient_id
  encounter_id = $encounterId
  pathway_code = $pathway.pathway_code
  version_no = $pathway.version
  doctor_id = "SMOKE_DOCTOR"
} | ConvertTo-Json -Depth 10
$admit = Invoke-RestMethod -Uri "$BaseUrl/patient-pathways/admit" -Method Post -ContentType "application/json; charset=utf-8" -Body $admitBody
if (-not $admit.success -or $admit.data.currentNodeCode -ne "AMI_CHEST_PAIN_IDENTIFY") {
  throw "Pathway admit failed."
}

$instanceId = $admit.data.instanceId
$firstNode = Invoke-RestMethod -Uri "$BaseUrl/patient-pathways/$instanceId/nodes/AMI_CHEST_PAIN_IDENTIFY" -Method Get
if (-not $firstNode.success -or $firstNode.data.tasks.Count -lt 1 -or $firstNode.data.tasks[0].taskCode -ne "TASK_ECG") {
  throw "Pathway first node task initialization failed."
}

$ecgTaskBody = @{
  operator_id = "SMOKE_DOCTOR"
  result = @{
    report_id = "ECG_SMOKE_001"
    finding_codes = @("ST_ELEVATION_CONTIGUOUS_LEADS")
    completed_time = (Get-Date).ToString("o")
  }
} | ConvertTo-Json -Depth 20
$ecgTask = Invoke-RestMethod -Uri "$BaseUrl/patient-pathways/$instanceId/nodes/AMI_CHEST_PAIN_IDENTIFY/tasks/TASK_ECG/complete" -Method Post -ContentType "application/json; charset=utf-8" -Body $ecgTaskBody
if (-not $ecgTask.success -or $ecgTask.data.status -ne "COMPLETED") {
  throw "Pathway task completion failed."
}
if ($ecgTask.data.result.adapter_status -ne "SUCCESS" -or $ecgTask.data.result.adapter_query.rows[0].finding_standard_code -ne "ST_ELEVATION_CONTIGUOUS_LEADS") {
  throw "Pathway task adapter source fetch failed."
}

$complete = Invoke-RestMethod -Uri "$BaseUrl/patient-pathways/$instanceId/nodes/AMI_CHEST_PAIN_IDENTIFY/complete" -Method Post -ContentType "application/json; charset=utf-8" -Body "{}"
if (-not $complete.success -or $complete.data.currentNodeCode -ne "AMI_REPERFUSION_EVAL") {
  throw "Pathway node transition failed."
}

$skipTaskBody = @{
  operator_id = "SMOKE_DOCTOR"
  variation_type = "PATIENT_REASON"
  reason = "患者暂不具备立即补采肌钙蛋白条件，先完成再灌注策略评估。"
} | ConvertTo-Json -Depth 20
$skipTask = Invoke-RestMethod -Uri "$BaseUrl/patient-pathways/$instanceId/nodes/AMI_REPERFUSION_EVAL/tasks/TASK_TROPONIN/skip" -Method Post -ContentType "application/json; charset=utf-8" -Body $skipTaskBody
if (-not $skipTask.success -or $skipTask.data.status -ne "SKIPPED") {
  throw "Pathway task skip and variation recording failed."
}

$variationBody = @{
  node_code = "AMI_REPERFUSION_EVAL"
  variation_type = "RESOURCE_LIMIT"
  reason = "导管室资源等待，医生已记录路径变异原因。"
  operator_id = "SMOKE_DOCTOR"
} | ConvertTo-Json -Depth 10
$variation = Invoke-RestMethod -Uri "$BaseUrl/patient-pathways/$instanceId/variations" -Method Post -ContentType "application/json; charset=utf-8" -Body $variationBody
if (-not $variation.success -or $variation.data.variationType -ne "RESOURCE_LIMIT") {
  throw "Pathway variation recording failed."
}

$detail = Invoke-RestMethod -Uri "$BaseUrl/patient-pathways/$instanceId" -Method Get
if (-not $detail.success -or $detail.data.variations.Count -lt 2) {
  throw "Pathway runtime detail query failed."
}

Write-Host "Pathway smoke test passed."
Write-Host "Invalid config validation: VALIDATION_ERROR"
Write-Host "Pathway: $($pathway.pathway_code)@$($pathway.version)"
Write-Host "Pathway config detail: selected=$($pathwayDetail.data.selected_version)"
Write-Host "Encounter: $encounterId"
Write-Host "Instance: $instanceId"
Write-Host "Current node after completion: $($complete.data.currentNodeCode)"
Write-Host "Completed task: $($ecgTask.data.taskCode)=$($ecgTask.data.status)"
Write-Host "Adapter source: $($ecgTask.data.result.adapter_query.adapter_code)/$($ecgTask.data.result.adapter_query.query_code), rows=$($ecgTask.data.result.adapter_row_count)"
Write-Host "Skipped task: $($skipTask.data.taskCode)=$($skipTask.data.status)"
Write-Host "Variation records: $($detail.data.variations.Count)"
