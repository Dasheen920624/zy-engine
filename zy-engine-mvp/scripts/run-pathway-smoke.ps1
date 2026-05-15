param(
  [string]$BaseUrl = "http://localhost:18080/zy-engine/api"
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

$pathwayJson = Get-Content -LiteralPath $pathwayFile -Raw -Encoding UTF8
$pathway = $pathwayJson | ConvertFrom-Json
$patient = Get-Content -LiteralPath $patientFile -Raw -Encoding UTF8 | ConvertFrom-Json

$encounterId = "E_AMI_SMOKE_" + (Get-Date -Format "yyyyMMddHHmmss")
$patient.encounter.encounter_id = $encounterId

$created = Invoke-RestMethod -Uri "$BaseUrl/pathways" -Method Post -ContentType "application/json; charset=utf-8" -Body $pathwayJson
if (-not $created.success -or $created.data.status -ne "DRAFT") {
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
$complete = Invoke-RestMethod -Uri "$BaseUrl/patient-pathways/$instanceId/nodes/AMI_CHEST_PAIN_IDENTIFY/complete" -Method Post -ContentType "application/json; charset=utf-8" -Body "{}"
if (-not $complete.success -or $complete.data.currentNodeCode -ne "AMI_REPERFUSION_EVAL") {
  throw "Pathway node transition failed."
}

Write-Host "Pathway smoke test passed."
Write-Host "Pathway: $($pathway.pathway_code)@$($pathway.version)"
Write-Host "Encounter: $encounterId"
Write-Host "Instance: $instanceId"
Write-Host "Current node after completion: $($complete.data.currentNodeCode)"
