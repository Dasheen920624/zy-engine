param(
  [string]$BaseUrl = "http://localhost:18080/zy-engine/api",
  [string]$SamplesDir = ""
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

if (-not $SamplesDir -or -not (Test-Path $SamplesDir)) {
  $engineRoot = Split-Path -Parent $PSScriptRoot
  $repoRoot = Split-Path -Parent $engineRoot
  $SamplesDir = Join-Path $repoRoot "ai-dev-input/06_samples"
}
if (-not (Test-Path $SamplesDir)) {
  throw "Samples directory not found: $SamplesDir"
}

function Invoke-JsonPost {
  param(
    [string]$Uri,
    [object]$Body
  )
  $json = $Body | ConvertTo-Json -Depth 30
  $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
  Invoke-RestMethod -Uri $Uri -Method Post -ContentType "application/json; charset=utf-8" -Body $bytes
}

function Invoke-JsonPostFile {
  param(
    [string]$Uri,
    [string]$Path
  )
  $bytes = [System.IO.File]::ReadAllBytes($Path)
  Invoke-RestMethod -Uri $Uri -Method Post -ContentType "application/json; charset=utf-8" -Body $bytes
}

function Read-ErrorBody {
  param([System.Management.Automation.ErrorRecord]$ErrorRecord)
  if ($ErrorRecord.ErrorDetails -and $ErrorRecord.ErrorDetails.Message) {
    return $ErrorRecord.ErrorDetails.Message
  }
  $response = $null
  if ($ErrorRecord.Exception -and $ErrorRecord.Exception.Response) {
    $response = $ErrorRecord.Exception.Response
  }
  if (-not $response) { return "" }
  try {
    $stream = $response.GetResponseStream()
    if (-not $stream) { return "" }
    $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
    return $reader.ReadToEnd()
  } catch {
    return ""
  }
}

$health = Invoke-RestMethod -Uri "$BaseUrl/health" -Method Get
if (-not $health.success) {
  throw "Health check failed."
}

# 1. 字典映射导入 + 列表 + 单项查询
$dictionaryFile = Join-Path $SamplesDir "sample_dictionary_mappings.json"
if (-not (Test-Path $dictionaryFile)) {
  throw "Sample dictionary mappings file not found: $dictionaryFile"
}
$dictionaryImport = Invoke-JsonPostFile -Uri "$BaseUrl/terminology/mappings" -Path $dictionaryFile
if (-not $dictionaryImport.success -or $dictionaryImport.data.Count -lt 1) {
  throw "Dictionary mappings import failed."
}
$dictionaryList = Invoke-RestMethod -Uri "$BaseUrl/terminology/mappings" -Method Get
if (-not $dictionaryList.success -or $dictionaryList.data.Count -lt 1) {
  throw "Dictionary mappings list failed."
}
$dictionaryGet = Invoke-RestMethod -Uri "$BaseUrl/terminology/mappings/HIS/I21.3?conceptType=DIAGNOSIS" -Method Get
if (-not $dictionaryGet.success -or $dictionaryGet.data.standard_code -ne "AMI_STEMI") {
  throw "Dictionary mapping get failed for HIS/I21.3."
}

# 2. 字典映射导入校验失败回退
$invalidMapping = $false
try {
  Invoke-JsonPost -Uri "$BaseUrl/terminology/mappings" -Body @{
    mappings = @(
      @{
        source_system = "HIS"
        concept_type = "DIAGNOSIS"
      }
    )
  } | Out-Null
} catch {
  $body = Read-ErrorBody $_
  if ($body -and $body -match "VALIDATION_ERROR") {
    $invalidMapping = $true
  }
}
if (-not $invalidMapping) {
  throw "Dictionary mappings invalid import did not return VALIDATION_ERROR."
}

# 3. 适配器查询定义导入 + 列表 + 单项查询
$adapterFile = Join-Path $SamplesDir "sample_adapter_definitions.json"
if (-not (Test-Path $adapterFile)) {
  throw "Sample adapter definitions file not found: $adapterFile"
}
$adapterImport = Invoke-JsonPostFile -Uri "$BaseUrl/adapters/definitions" -Path $adapterFile
if (-not $adapterImport.success -or $adapterImport.data.Count -lt 1) {
  throw "Adapter definitions import failed."
}
$pacsDefinition = $adapterImport.data | Where-Object { $_.adapter_code -eq "PACS_ADAPTER" -and $_.query_code -eq "QUERY_CHEST_CT" }
if (-not $pacsDefinition) {
  throw "Adapter definitions import missing PACS_ADAPTER/QUERY_CHEST_CT."
}
$adapterList = Invoke-RestMethod -Uri "$BaseUrl/adapters/definitions" -Method Get
if (-not $adapterList.success -or $adapterList.data.Count -lt 1) {
  throw "Adapter definitions list failed."
}
$adapterGet = Invoke-RestMethod -Uri "$BaseUrl/adapters/definitions/ECG_ADAPTER/QUERY_ECG_REPORT" -Method Get
if (-not $adapterGet.success -or $adapterGet.data.adapter_type -ne "REST") {
  throw "Adapter definition get failed for ECG_ADAPTER/QUERY_ECG_REPORT."
}

# 4. 导入后调用：内置适配器仍可正常返回行；新导入但无内置 Mock 的 PACS 返回空行
$ecgQuery = Invoke-JsonPost -Uri "$BaseUrl/adapters/query" -Body @{
  adapter_code = "ECG_ADAPTER"
  query_code = "QUERY_ECG_REPORT"
  params = @{ patient_id = "P_AMI_001"; encounter_id = "E_AMI_001" }
}
if (-not $ecgQuery.success -or $ecgQuery.data.row_count -lt 1) {
  throw "ECG adapter query after import failed."
}
$pacsQuery = Invoke-JsonPost -Uri "$BaseUrl/adapters/query" -Body @{
  adapter_code = "PACS_ADAPTER"
  query_code = "QUERY_CHEST_CT"
  params = @{ patient_id = "P_AMI_001"; encounter_id = "E_AMI_001" }
}
if (-not $pacsQuery.success -or $pacsQuery.data.status -ne "SUCCESS" -or $pacsQuery.data.row_count -ne 0) {
  throw "PACS adapter query should return SUCCESS with empty rows for imported definition without sample rows."
}

# 5. 适配器导入校验失败回退
$invalidAdapter = $false
try {
  Invoke-JsonPost -Uri "$BaseUrl/adapters/definitions" -Body @{
    definitions = @(
      @{
        adapter_code = "BAD_ADAPTER"
      }
    )
  } | Out-Null
} catch {
  $body = Read-ErrorBody $_
  if ($body -and $body -match "VALIDATION_ERROR") {
    $invalidAdapter = $true
  }
}
if (-not $invalidAdapter) {
  throw "Adapter definitions invalid import did not return VALIDATION_ERROR."
}

Write-Output "Terminology/adapter config import smoke test passed."
Write-Output "Dictionary mappings imported: $($dictionaryImport.data.Count); total registered: $($dictionaryList.data.Count)"
Write-Output "Dictionary get HIS/I21.3 -> $($dictionaryGet.data.standard_code) (confidence=$($dictionaryGet.data.confidence))"
Write-Output "Adapter definitions imported: $($adapterImport.data.Count); total registered: $($adapterList.data.Count)"
Write-Output "PACS_ADAPTER/QUERY_CHEST_CT after import: status=$($pacsQuery.data.status), row_count=$($pacsQuery.data.row_count)"
