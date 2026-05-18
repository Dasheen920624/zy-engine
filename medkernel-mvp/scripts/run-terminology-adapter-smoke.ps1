param(
  [string]$BaseUrl = "http://localhost:18080/medkernel/api"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

function Invoke-JsonPost {
  param(
    [string]$Uri,
    [hashtable]$Body
  )
  $json = $Body | ConvertTo-Json -Depth 30
  Invoke-RestMethod -Uri $Uri -Method Post -ContentType "application/json; charset=utf-8" -Body $json
}

$health = Invoke-RestMethod -Uri "$BaseUrl/health" -Method Get
if (-not $health.success) {
  throw "Health check failed."
}

$diagnosisMapping = Invoke-JsonPost -Uri "$BaseUrl/terminology/normalize" -Body @{
  source_system = "HIS"
  source_code = "I21.3"
  source_name = "急性ST段抬高型心肌梗死"
  concept_type = "DIAGNOSIS"
  patient_id = "P_AMI_001"
  encounter_id = "E_AMI_001"
}
if (-not $diagnosisMapping.success -or -not $diagnosisMapping.data.matched -or $diagnosisMapping.data.standard_code -ne "AMI_STEMI") {
  throw "Terminology diagnosis mapping failed."
}

$unmapped = Invoke-JsonPost -Uri "$BaseUrl/terminology/normalize" -Body @{
  source_system = "LIS"
  source_code = "UNKNOWN_TEST"
  source_name = "未知检验项目"
  concept_type = "LAB_ITEM"
}
if (-not $unmapped.success -or $unmapped.data.matched -or $unmapped.data.mapping_status -ne "UNMAPPED") {
  throw "Terminology unmapped governance result failed."
}

$ecg = Invoke-JsonPost -Uri "$BaseUrl/adapters/query" -Body @{
  adapter_code = "ECG_ADAPTER"
  query_code = "QUERY_ECG_REPORT"
  params = @{
    patient_id = "P_AMI_001"
    encounter_id = "E_AMI_001"
  }
}
if (-not $ecg.success -or $ecg.data.status -ne "SUCCESS" -or $ecg.data.row_count -lt 1) {
  throw "ECG adapter mock failed."
}
if ($ecg.data.rows[0].finding_standard_code -ne "ST_ELEVATION_CONTIGUOUS_LEADS") {
  throw "ECG adapter standard finding mapping failed."
}

$lis = Invoke-JsonPost -Uri "$BaseUrl/adapters/query" -Body @{
  adapter_code = "LIS_ADAPTER"
  query_code = "QUERY_TROPONIN"
  params = @{
    patient_id = "P_AMI_001"
    encounter_id = "E_AMI_001"
  }
}
if (-not $lis.success -or $lis.data.rows[0].lab_standard_code -ne "TROPONIN_I") {
  throw "LIS adapter mock failed."
}

$his = Invoke-JsonPost -Uri "$BaseUrl/adapters/query" -Body @{
  adapter_code = "HIS_ADAPTER"
  query_code = "QUERY_DIAGNOSES"
  params = @{
    patient_id = "P_AMI_001"
    encounter_id = "E_AMI_001"
  }
}
if (-not $his.success -or $his.data.rows[0].standard_code -ne "AMI_STEMI") {
  throw "HIS adapter diagnosis mapping failed."
}

$emr = Invoke-JsonPost -Uri "$BaseUrl/adapters/query" -Body @{
  adapter_code = "EMR_WS_ADAPTER"
  query_code = "QUERY_ADMISSION_NOTE"
  params = @{
    patient_id = "P_AMI_001"
    encounter_id = "E_AMI_001"
  }
}
if (-not $emr.success -or $emr.data.adapter_type -ne "WEBSERVICE" -or $emr.data.rows[0].document_type -ne "ADMISSION_NOTE") {
  throw "EMR WebService adapter mock failed."
}

Write-Output "Terminology and adapter smoke test passed."
Write-Output "Diagnosis mapping: HIS I21.3 -> $($diagnosisMapping.data.standard_code), confidence=$($diagnosisMapping.data.confidence)"
Write-Output "Unmapped governance: $($unmapped.data.source_code), status=$($unmapped.data.governance_status)"
Write-Output "ECG adapter: finding=$($ecg.data.rows[0].finding_standard_code), rows=$($ecg.data.row_count)"
Write-Output "LIS adapter: lab=$($lis.data.rows[0].lab_standard_code), value=$($lis.data.rows[0].value)$($lis.data.rows[0].unit)"
Write-Output "HIS adapter: diagnosis=$($his.data.rows[0].standard_code)"
Write-Output "WebService adapter: document=$($emr.data.rows[0].document_id)"
