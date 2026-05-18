# 升级 —— Windows
# 用法：管理员 PowerShell：
#   .\upgrade.ps1 -To v1.3.0 [-MigrateDb] [-BackupOnly]

[CmdletBinding()]
param(
    [string]$To = "",
    [switch]$MigrateDb,
    [switch]$BackupOnly,
    [string]$Package = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$MkHome = if ($env:MK_HOME) { $env:MK_HOME } else { "D:\zoesoft\medkernel" }
$BackupRoot = if ($env:MK_BACKUP_DIR) { $env:MK_BACKUP_DIR } else { "D:\zoesoft\medkernel.bak" }

# 必须 admin
$winId = [System.Security.Principal.WindowsIdentity]::GetCurrent()
$winPrincipal = New-Object System.Security.Principal.WindowsPrincipal($winId)
if (-not $winPrincipal.IsInRole([System.Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw "需要管理员权限运行 PowerShell"
}

Write-Host "==> 1. 备份" -ForegroundColor Cyan
$ts = Get-Date -Format "yyyyMMdd_HHmmss"
$backup = Join-Path $BackupRoot $ts
New-Item -ItemType Directory -Force -Path $backup | Out-Null
foreach ($d in @("lib", "frontend", "conf", "systemd", "nginx")) {
    if (Test-Path "$MkHome\$d") {
        Copy-Item -Path "$MkHome\$d" -Destination $backup -Recurse -Force
        Write-Host "[OK]   备份 $d" -ForegroundColor Green
    }
}
if (Test-Path "$MkHome\manifest.json") {
    Copy-Item "$MkHome\manifest.json" $backup
}
$backup | Out-File -FilePath "$MkHome\.last-backup" -Encoding ascii
Write-Host "[OK]   备份完成：$backup" -ForegroundColor Green

if ($BackupOnly) { Write-Host "[OK]   仅备份，已完成" -ForegroundColor Green; exit 0 }
if ($To -eq "") { throw "请指定 -To <version>" }

Write-Host "`n==> 2. 停止服务" -ForegroundColor Cyan
Stop-Service "MedKernel" -Force -ErrorAction SilentlyContinue

Write-Host "`n==> 3. 解压新发布包" -ForegroundColor Cyan
if ($Package -eq "") {
    $Package = "D:\Temp\medkernel-$To.tar.gz"
}
if (-not (Test-Path $Package)) { throw "未找到 $Package；请先上传发布包并用 -Package 指定" }
$tmpDir = Join-Path $env:TEMP "zy-upgrade-$ts"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null
# Windows 10/Server 2019+ 自带 tar.exe
& tar -xzvf $Package -C $tmpDir --strip-components=1
if ($LASTEXITCODE -ne 0) { throw "tar 解压失败（需 Windows 10/Server 2019+ 自带 tar.exe）" }
Write-Host "[OK]   解压到 $tmpDir" -ForegroundColor Green

Write-Host "`n==> 4. 覆盖（保留 conf）" -ForegroundColor Cyan
foreach ($d in @("lib", "frontend", "db", "scripts", "systemd", "nginx", "docs", "profiles")) {
    if (Test-Path "$tmpDir\$d") {
        Remove-Item "$MkHome\$d" -Recurse -Force -ErrorAction SilentlyContinue
        Copy-Item "$tmpDir\$d" "$MkHome\" -Recurse -Force
    }
}
foreach ($f in @("manifest.json", "CHANGELOG.md")) {
    if (Test-Path "$tmpDir\$f") {
        Copy-Item "$tmpDir\$f" "$MkHome\" -Force
    }
}

Write-Host "`n==> 5. 数据库迁移" -ForegroundColor Cyan
if ($MigrateDb) {
    & powershell -ExecutionPolicy Bypass -File (Join-Path $ScriptDir "install-offline.ps1") -MigrateDb -SkipDb:$false
} else {
    Write-Host "[SKIP] 未指定 -MigrateDb；如有 DDL 变更请评估后人工执行" -ForegroundColor DarkGray
}

Write-Host "`n==> 6. 启动" -ForegroundColor Cyan
Start-Service "MedKernel"
Start-Sleep -Seconds 5

Write-Host "`n==> 7. 健康检查" -ForegroundColor Cyan
& powershell -ExecutionPolicy Bypass -File (Join-Path $ScriptDir "healthcheck.ps1")
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAIL] 升级失败！建议执行：" -ForegroundColor Red
    Write-Host "       .\rollback.ps1 -To $ts" -ForegroundColor Red
    exit 1
}
Write-Host "[OK]   升级成功 → $To" -ForegroundColor Green
Write-Host "回滚指令（5 分钟内可执行）：.\rollback.ps1 -To $ts" -ForegroundColor Cyan
