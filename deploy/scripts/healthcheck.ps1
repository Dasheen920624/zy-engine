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

# GA-OPS-01: 增强 actuator/health 检查
$actuatorUrl = $Url -replace '/medkernel$', ''
$actuatorUrl = "$actuatorUrl`:18081/actuator/health"
Write-Host "==> Actuator: $actuatorUrl" -ForegroundColor Cyan
try {
    $actuatorResp = Invoke-RestMethod -Uri $actuatorUrl -Method Get -TimeoutSec $TimeoutSec
    if ($actuatorResp.status -eq "UP") {
        Write-Host "[OK]   actuator/health  UP" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] actuator/health  status=$($actuatorResp.status)" -ForegroundColor Red
        $fail++
    }
} catch {
    Write-Host "[FAIL] actuator/health  $($_.Exception.Message)" -ForegroundColor Red
    $fail++
}

# GA-OPS-01: SLO 状态快速检查
$prometheusUrl = if ($env:PROMETHEUS_URL) { $env:PROMETHEUS_URL } else { "http://localhost:9090" }
try {
    $null = Invoke-RestMethod -Uri "$prometheusUrl/-/healthy" -Method Get -TimeoutSec 5
    Write-Host "[OK]   Prometheus  可达" -ForegroundColor Green

    # 检查是否有活跃的 critical 告警
    try {
        $alertsResp = Invoke-RestMethod -Uri "$prometheusUrl/api/v1/alerts?state=firing" -Method Get -TimeoutSec 5
        $criticalAlerts = $alertsResp.data.alerts | Where-Object { $_.labels.severity -eq "critical" }
        if ($criticalAlerts -and $criticalAlerts.Count -gt 0) {
            Write-Host "[FAIL] SLO: $($criticalAlerts.Count) 个 critical 告警正在触发" -ForegroundColor Red
            $fail++
        } else {
            Write-Host "[OK]   SLO: 无 critical 告警触发" -ForegroundColor Green
        }
    } catch {
        Write-Host "[WARN] 无法查询 Prometheus 告警状态" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[WARN] Prometheus 不可达（跳过 SLO 检查）" -ForegroundColor Yellow
}

if ($fail -eq 0) {
    Write-Host "[OK]   全部健康检查通过" -ForegroundColor Green
    exit 0
}
Write-Host "[FAIL] $fail 项失败" -ForegroundColor Red
exit 1
