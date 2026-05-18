param(
  [string]$BaseUrl = "http://localhost:18080/medkernel/api"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$health = Invoke-RestMethod -Uri "$BaseUrl/health" -Method Get
if (-not $health.success) {
  throw "Health check failed."
}

$graphBody = @{
  symptom_codes = @("CHEST_PAIN")
  finding_codes = @("ST_ELEVATION_CONTIGUOUS_LEADS")
  risk_factor_codes = @("HYPERTENSION", "DIABETES")
  graph_version = "AMI_GRAPH_2026_01"
  limit = 10
} | ConvertTo-Json -Depth 20

$graph = Invoke-RestMethod -Uri "$BaseUrl/graph/disease-candidates" -Method Post -ContentType "application/json; charset=utf-8" -Body $graphBody
if (-not $graph.success -or $graph.data.Count -lt 1 -or $graph.data[0].diseaseCode -ne "AMI_STEMI") {
  throw "Graph disease candidate smoke failed."
}

$evidenceBody = @{
  target_code = "AMI_STEMI"
  target_type = "PATHWAY"
  graph_version = "AMI_GRAPH_2026_01"
} | ConvertTo-Json -Depth 10

$evidence = Invoke-RestMethod -Uri "$BaseUrl/graph/evidence" -Method Post -ContentType "application/json; charset=utf-8" -Body $evidenceBody
if (-not $evidence.success -or $evidence.data.Count -lt 1 -or $evidence.data[0].evidence_id -ne "EV_AMI_001") {
  throw "Graph evidence smoke failed."
}

$difyBody = @{
  workflow_code = "WF_AMI_ENTRY_EXPLAIN"
  workflow_version = "1.0.0"
  trace_id = "trace-smoke-dify"
  inputs = @{
    patient_id = "P_AMI_001"
    encounter_id = "E_AMI_001"
    target_code = "AMI_STEMI"
    evidence_refs = @("EV_AMI_001")
  }
} | ConvertTo-Json -Depth 20

$dify = Invoke-RestMethod -Uri "$BaseUrl/dify/workflows/run" -Method Post -ContentType "application/json; charset=utf-8" -Body $difyBody
if (-not $dify.success -or -not $dify.data.status -or -not $dify.data.outputs.explanation) {
  throw "Dify workflow smoke failed."
}

Write-Output "Graph and Dify smoke test passed."
Write-Output "Graph candidate: $($graph.data[0].diseaseCode), source=$($graph.data[0].graphSource), degraded=$($graph.data[0].degraded)"
Write-Output "Evidence: $($evidence.data[0].evidence_id), source=$($evidence.data[0].graph_source)"
Write-Output "Dify workflow: $($dify.data.workflow_code), status=$($dify.data.status), provider=$($dify.data.provider)"
