param(
  [string]$BaseUrl = "http://localhost:18080/zy-engine/api"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$engineRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $engineRoot
$rulesFile = Join-Path $workspaceRoot "ai-dev-input\06_samples\sample_ami_rules.json"
$patientFile = Join-Path $workspaceRoot "ai-dev-input\06_samples\sample_patient_context_ami.json"

if (!(Test-Path -LiteralPath $rulesFile)) {
  throw "Rules file not found: $rulesFile"
}
if (!(Test-Path -LiteralPath $patientFile)) {
  throw "Patient sample file not found: $patientFile"
}

$health = Invoke-RestMethod -Uri "$BaseUrl/health" -Method Get
if (-not $health.success) {
  throw "Health check failed."
}

$rulesJson = Get-Content -LiteralPath $rulesFile -Raw -Encoding UTF8
$patient = Get-Content -LiteralPath $patientFile -Raw -Encoding UTF8 | ConvertFrom-Json

$imported = Invoke-RestMethod -Uri "$BaseUrl/rules" -Method Post -ContentType "application/json; charset=utf-8" -Body $rulesJson
if (-not $imported.success -or $imported.data.Count -lt 3) {
  throw "Rule import failed."
}

foreach ($rule in $imported.data) {
  $publishBody = @{
    version_no = $rule.versionNo
    approved_by = "SMOKE_TEST"
  } | ConvertTo-Json -Depth 10
  $published = Invoke-RestMethod -Uri "$BaseUrl/rules/$($rule.ruleCode)/publish" -Method Post -ContentType "application/json; charset=utf-8" -Body $publishBody
  if (-not $published.success -or $published.data.status -ne "PUBLISHED") {
    throw "Rule publish failed: $($rule.ruleCode)"
  }
}

$simulateBody = @{
  rule_code = "R_AMI_STEMI_CANDIDATE"
  version_no = "1.0.0"
  patient_context = $patient
} | ConvertTo-Json -Depth 30
$simulate = Invoke-RestMethod -Uri "$BaseUrl/rules/simulate" -Method Post -ContentType "application/json; charset=utf-8" -Body $simulateBody
if (-not $simulate.success -or -not $simulate.data.hit) {
  throw "Rule simulation did not hit AMI STEMI candidate."
}

$evaluateBody = @{
  patient_context = $patient
} | ConvertTo-Json -Depth 30
$evaluate = Invoke-RestMethod -Uri "$BaseUrl/rules/evaluate" -Method Post -ContentType "application/json; charset=utf-8" -Body $evaluateBody
if (-not $evaluate.success -or $evaluate.data.Count -lt 3) {
  throw "Rule evaluation failed."
}

Write-Host "Rule smoke test passed."
Write-Host "Imported rules: $($imported.data.Count)"
Write-Host "Simulate rule: $($simulate.data.ruleCode), hit=$($simulate.data.hit)"
Write-Host "Evaluated rules: $($evaluate.data.Count)"
