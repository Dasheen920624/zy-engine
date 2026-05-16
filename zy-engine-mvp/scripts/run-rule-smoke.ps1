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

$review = Invoke-RestMethod -Uri "$BaseUrl/rules/packages/PKG_AMI_CORE/review?packageVersion=2026.05" -Method Get
if (-not $review.success -or -not $review.data.ready_to_publish -or $review.data.total_rules -lt 3) {
  throw "Rule package review failed."
}

$packagePublishBody = @{
  package_version = "2026.05"
  approved_by = "SMOKE_TEST"
} | ConvertTo-Json -Depth 10
$packagePublished = Invoke-RestMethod -Uri "$BaseUrl/rules/packages/PKG_AMI_CORE/publish" -Method Post -ContentType "application/json; charset=utf-8" -Body $packagePublishBody
if (-not $packagePublished.success -or $packagePublished.data.published_count -lt 3) {
  throw "Rule package publish failed."
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

$logsAll = Invoke-RestMethod -Uri "$BaseUrl/rules/exec-logs?limit=50" -Method Get
if (-not $logsAll.success -or $logsAll.data.Count -lt 1) {
  throw "Rule exec logs query failed."
}

$logsForCandidate = Invoke-RestMethod -Uri "$BaseUrl/rules/exec-logs?ruleCode=R_AMI_STEMI_CANDIDATE&hit=true&limit=10" -Method Get
if (-not $logsForCandidate.success -or $logsForCandidate.data.Count -lt 1) {
  throw "Rule exec logs filter by ruleCode/hit failed."
}
$firstLog = $logsForCandidate.data[0]
if ($firstLog.ruleCode -ne "R_AMI_STEMI_CANDIDATE" -or -not $firstLog.hit) {
  throw "Rule exec log filter returned unexpected entries."
}

$logDetail = Invoke-RestMethod -Uri "$BaseUrl/rules/exec-logs/$($firstLog.logId)" -Method Get
if (-not $logDetail.success -or $logDetail.data.logId -ne $firstLog.logId) {
  throw "Rule exec log detail query failed."
}

Write-Host "Rule smoke test passed."
Write-Host "Imported rules: $($imported.data.Count)"
Write-Host "Published rule package: $($packagePublished.data.package_code)@$($packagePublished.data.package_version), count=$($packagePublished.data.published_count)"
Write-Host "Simulate rule: $($simulate.data.ruleCode), hit=$($simulate.data.hit)"
Write-Host "Evaluated rules: $($evaluate.data.Count)"
Write-Host "Exec logs total recent: $($logsAll.data.Count); STEMI hit logs: $($logsForCandidate.data.Count); first logId=$($firstLog.logId), elapsedMs=$($firstLog.elapsedMs)"
