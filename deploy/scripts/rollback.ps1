# 回滚 —— Windows
# 用法：管理员 PowerShell：
#   .\rollback.ps1 -To last
#   .\rollback.ps1 -To 20260517_083000

[CmdletBinding()]
param(
    [string]$To = "last"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ZyHome = if ($env:ZY_HOME) { $env:ZY_HOME } else { "C:\zy-engine" }
$BackupRoot = if ($env:ZY_BACKUP_DIR) { $env:ZY_BACKUP_DIR } else { "C:\zy-engine.bak" }

# 必须 admin
$winId = [System.Security.Principal.WindowsIdentity]::GetCurrent()
$winPrincipal = New-Object System.Security.Principal.WindowsPrincipal($winId)
if (-not $winPrincipal.IsInRole([System.Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw "需要管理员权限运行 PowerShell"
}

if ($To -eq "last") {
    $lastFile = "$ZyHome\.last-backup"
    if (-not (Test-Path $lastFile)) { throw "未找到 .last-backup；请显式 -To <timestamp>" }
    $backup = (Get-Content $lastFile).Trim()
} else {
    $backup = Join-Path $BackupRoot $To
}
if (-not (Test-Path $backup)) { throw "备份目录不存在：$backup" }

Write-Host "==> 1. 停止服务" -ForegroundColor Cyan
Stop-Service "ZyEngine" -Force -ErrorAction SilentlyContinue

Write-Host "`n==> 2. 还原备份 <- $backup" -ForegroundColor Cyan
foreach ($d in @("lib", "frontend", "conf", "systemd", "nginx")) {
    if (Test-Path "$backup\$d") {
        Remove-Item "$ZyHome\$d" -Recurse -Force -ErrorAction SilentlyContinue
        Copy-Item "$backup\$d" "$ZyHome\" -Recurse -Force
        Write-Host "[OK]   还原 $d" -ForegroundColor Green
    }
}
if (Test-Path "$backup\manifest.json") {
    Copy-Item "$backup\manifest.json" "$ZyHome\" -Force
}

Write-Host "`n==> 3. 启动服务" -ForegroundColor Cyan
Start-Service "ZyEngine"
Start-Sleep -Seconds 5

Write-Host "`n==> 4. 健康检查" -ForegroundColor Cyan
& powershell -ExecutionPolicy Bypass -File (Join-Path $ScriptDir "healthcheck.ps1")
if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK]   回滚完成" -ForegroundColor Green
} else {
    Write-Host "[FAIL] 回滚后健康检查失败" -ForegroundColor Red
    exit 1
}
