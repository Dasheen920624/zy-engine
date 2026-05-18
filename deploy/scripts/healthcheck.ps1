# 健康检查 —— Windows
# 用法：PowerShell -ExecutionPolicy Bypass -File healthcheck.ps1 [-Url http://localhost:18080/medkernel]

[CmdletBinding()]
param(
    [string]$Url = $null,
    [int]$TimeoutSec = 10
)

$ErrorActionPreference = "Stop"

if (-not $Url) {
    $port = if ($env:MEDKERNEL_HTTP_PORT) { $env:MEDKERNEL_HTTP_PORT } else { "18080" }
    $Url = "http://localhost:$port/medkernel"
}

Write-Host "==> Healthcheck: $Url" -ForegroundColor Cyan

function Probe([string]$Path, [string]$Label) {
    $trace = "hc-{0}-{1}" -f ([int][double]::Parse((Get-Date -UFormat %s))), (Get-Random)
    try {
        $resp = Invoke-RestMethod -Uri "$Url$Path" -Method Get -Headers @{ "X-Trace-Id" = $trace } -TimeoutSec $TimeoutSec
        if ($resp.success -eq $true) {
            Write-Host "[OK]   $Label  $Path  trace=$trace" -ForegroundColor Green
            return $true
        }
        Write-Host "[FAIL] $Label  $Path  success!=true: $($resp | ConvertTo-Json -Compress)" -ForegroundColor Red
        return $false
    } catch {
        Write-Host "[FAIL] $Label  $Path  $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

$fail = 0
if (-not (Probe "/api/health"             "health   ")) { $fail++ }
if (-not (Probe "/api/system/providers"   "providers")) { $fail++ }
if (-not (Probe "/api/system/org-context" "org-ctx  ")) { $fail++ }

if ($fail -eq 0) {
    Write-Host "[OK]   全部健康检查通过" -ForegroundColor Green
    exit 0
}
Write-Host "[FAIL] $fail 项失败" -ForegroundColor Red
exit 1
