# 部署前环境检查 —— Windows
# 用法：PowerShell -ExecutionPolicy Bypass -File check-env.ps1 [-Profile centos7-x86_64-oracle]

[CmdletBinding()]
param(
    [string]$Profile = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

function Write-Log([string]$Level, [string]$Msg) {
    $color = switch ($Level) {
        "OK"   { "Green" }
        "INFO" { "Cyan" }
        "WARN" { "Yellow" }
        "FAIL" { "Red" }
        "SKIP" { "DarkGray" }
        default { "White" }
    }
    $tag = "[$Level]".PadRight(7)
    Write-Host "$tag $Msg" -ForegroundColor $color
}

function Detect-OSInfo {
    $os = Get-CimInstance Win32_OperatingSystem
    return "$($os.Caption) $($os.Version) $($os.OSArchitecture)"
}

function Detect-Arch {
    if ([Environment]::Is64BitOperatingSystem) {
        if ($env:PROCESSOR_ARCHITECTURE -eq "ARM64") { return "aarch64" }
        return "x86_64"
    }
    return "x86"
}

function Test-PortInUse([int]$Port) {
    try {
        $listener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Any, $Port)
        $listener.Start()
        $listener.Stop()
        return $false
    } catch { return $true }
}

$Counters = @{ OK = 0; WARN = 0; FAIL = 0; SKIP = 0 }
function Record($Level, $Msg) {
    Write-Log $Level $Msg
    $Counters[$Level]++
}

if ($Profile -ne "") {
    $profilePath = Join-Path $ScriptDir "../profiles/$Profile.env"
    if (Test-Path $profilePath) {
        Get-Content $profilePath | Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } | ForEach-Object {
            $k,$v = $_ -split '=', 2
            Set-Item -Path "Env:$($k.Trim())" -Value $v.Trim()
        }
        Write-Log "INFO" "已加载 profile: $Profile"
    } else { throw "Profile 不存在: $profilePath" }
}

# ============================================================================
Write-Host "`n==> OS / 硬件" -ForegroundColor Cyan
Record "OK" "OS: $(Detect-OSInfo)"
Record "OK" "CPU 架构: $(Detect-Arch)"

# ============================================================================
Write-Host "`n==> JDK" -ForegroundColor Cyan
try {
    $javaVer = (& java -version 2>&1 | Out-String).Trim()
    if ($javaVer -match '"1\.8\.[0-9]+') {
        Record "OK" "JDK: $($javaVer -split "`n" | Select-Object -First 1)"
    } else {
        Record "FAIL" "JDK 非 1.8: $javaVer"
    }
} catch {
    Record "FAIL" "未找到 java；请安装 JDK 1.8（Temurin / OpenJDK / 毕昇）"
}

# ============================================================================
Write-Host "`n==> locale / 时区" -ForegroundColor Cyan
$culture = (Get-Culture).Name
$tz = (Get-TimeZone).Id
Record "OK" "Culture: $culture"
if ($tz -eq "China Standard Time" -or $tz -eq "Asia/Shanghai") {
    Record "OK" "时区: $tz"
} else {
    Record "WARN" "时区非 Asia/Shanghai: $tz"
}

# ============================================================================
Write-Host "`n==> 目录与磁盘" -ForegroundColor Cyan
$zyHome = if ($env:ZY_HOME) { $env:ZY_HOME } else { "C:\zoesoft\zy-engine" }
if (Test-Path $zyHome) {
    $drive = (Split-Path -Qualifier $zyHome).TrimEnd(':')
    $free = (Get-PSDrive $drive).Free
    $freeGB = [int]($free / 1GB)
    if ($freeGB -ge 10) { Record "OK" "$zyHome 可用 ${freeGB}GB" }
    else { Record "WARN" "$zyHome 可用 ${freeGB}GB，建议 >= 10GB" }
} else {
    Record "WARN" "$zyHome 不存在（首次部署会创建）"
}

# ============================================================================
Write-Host "`n==> 端口" -ForegroundColor Cyan
$portBackend = if ($env:ZYENGINE_HTTP_PORT) { [int]$env:ZYENGINE_HTTP_PORT } else { 18080 }
if (Test-PortInUse $portBackend) {
    Record "FAIL" "后端端口 $portBackend 已被占用"
} else {
    Record "OK" "后端端口 $portBackend 空闲"
}

# ============================================================================
Write-Host "`n==> 防火墙" -ForegroundColor Cyan
try {
    $fwState = (Get-NetFirewallProfile -All | Where-Object Enabled -eq $true).Name
    if ($fwState) { Record "WARN" "防火墙启用: $($fwState -join ', '); 如端口未开请用 New-NetFirewallRule" }
    else { Record "OK" "防火墙未启用" }
} catch {
    Record "SKIP" "无法查询防火墙状态"
}

# ============================================================================
Write-Host "`n==> 数据库" -ForegroundColor Cyan
$dialect = $env:ZYENGINE_DB_DIALECT
switch ($dialect) {
    "oracle" {
        if (Get-Command sqlplus -ErrorAction SilentlyContinue) {
            Record "OK" "sqlplus 可用（连通性请手动验证）"
        } else { Record "SKIP" "sqlplus 未安装" }
    }
    "dm" { Record "SKIP" "DM 连通需 disql，详见 db/dm/README.md" }
    "postgres" {
        if (Get-Command psql -ErrorAction SilentlyContinue) {
            Record "OK" "psql 可用（连通性请手动验证）"
        } else { Record "SKIP" "psql 未安装" }
    }
    $null { Record "WARN" "未设置 ZYENGINE_DB_DIALECT" }
    default { Record "WARN" "未知 dialect: $dialect" }
}

# ============================================================================
Write-Host "`n==> 结果" -ForegroundColor Cyan
Write-Host ("  OK   : {0}" -f $Counters.OK)
Write-Host ("  WARN : {0}" -f $Counters.WARN)
Write-Host ("  SKIP : {0}" -f $Counters.SKIP)
Write-Host ("  FAIL : {0}" -f $Counters.FAIL)
Write-Host ""

if ($Counters.FAIL -gt 0) {
    Write-Log "FAIL" "存在 FAIL 项，请先修复后再继续安装。"
    exit 1
}
Write-Log "OK" "检查通过，可继续 install.ps1 / upgrade.ps1"
