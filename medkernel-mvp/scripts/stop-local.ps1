param(
  [int[]]$Ports = @(18080, 18081)
)

$ErrorActionPreference = "Stop"

$connections = Get-NetTCPConnection -LocalPort $Ports -ErrorAction SilentlyContinue
$processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique

foreach ($processId in $processIds) {
  if ($processId -and $processId -gt 0) {
    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    Write-Host "Stopped process $processId"
  }
}

if (-not $processIds) {
  Write-Host "No local engine process found."
}
