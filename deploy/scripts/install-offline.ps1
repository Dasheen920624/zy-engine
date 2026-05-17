# 离线安装 —— Windows
# 用法：以管理员 PowerShell：
#   .\install-offline.ps1 [-InitDb] [-MigrateDb] [-SkipDb] [-Profile pg-x86_64]

[CmdletBinding()]
param(
    [switch]$InitDb,
    [switch]$MigrateDb,
    [switch]$SkipDb,
    [string]$Profile = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ZyHome = if ($env:ZY_HOME) { $env:ZY_HOME } else { "C:\zoesoft\zy-engine" }

# 必须 admin
$winId = [System.Security.Principal.WindowsIdentity]::GetCurrent()
$winPrincipal = New-Object System.Security.Principal.WindowsPrincipal($winId)
if (-not $winPrincipal.IsInRole([System.Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw "需要管理员权限运行 PowerShell"
}

Write-Host "==> 1. 检查环境" -ForegroundColor Cyan
$envArgs = @()
if ($Profile -ne "") { $envArgs += "-Profile"; $envArgs += $Profile }
& powershell -ExecutionPolicy Bypass -File (Join-Path $ScriptDir "check-env.ps1") @envArgs
if ($LASTEXITCODE -ne 0) { throw "环境检查失败" }

Write-Host "`n==> 2. 创建目录" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path "$ZyHome\logs" | Out-Null
New-Item -ItemType Directory -Force -Path "$ZyHome\conf" | Out-Null
Write-Host "[OK]   目录就绪：$ZyHome" -ForegroundColor Green

Write-Host "`n==> 3. 数据库 DDL" -ForegroundColor Cyan
$dialect = $env:ZYENGINE_DB_DIALECT
if ($SkipDb) {
    Write-Host "[SKIP] 按 -SkipDb 跳过" -ForegroundColor DarkGray
} elseif ($InitDb -or $MigrateDb) {
    switch ($dialect) {
        "oracle" {
            Write-Host "[INFO] 请由 DBA 执行：sqlplus ... @$ZyHome\db\oracle\zyengine_core_ddl_with_comments.sql" -ForegroundColor Cyan
        }
        "dm" {
            Write-Host "[INFO] 请由 DBA 执行：disql ... -e \"START '$ZyHome\db\dm\zyengine_core_ddl_with_comments.sql';\"" -ForegroundColor Cyan
        }
        "postgres" {
            if (Get-Command psql -ErrorAction SilentlyContinue) {
                $env:PGPASSWORD = $env:ZYENGINE_DB_PASSWORD
                & psql -h $env:ZYENGINE_DB_HOST -p $env:ZYENGINE_DB_PORT -U $env:ZYENGINE_DB_USERNAME -d $env:ZYENGINE_DB_NAME -v ON_ERROR_STOP=1 -f "$ZyHome\db\postgres\zyengine_core_ddl_with_comments.sql"
                if ($LASTEXITCODE -eq 0) { Write-Host "[OK]   PG DDL 已执行" -ForegroundColor Green }
                else { throw "PG DDL 执行失败" }
            } else {
                Write-Host "[WARN] psql 未安装；请由 DBA 执行" -ForegroundColor Yellow
            }
        }
        default { Write-Host "[WARN] 未知 dialect: $dialect" -ForegroundColor Yellow }
    }
} else {
    Write-Host "[SKIP] 未指定 -InitDb / -MigrateDb" -ForegroundColor DarkGray
}

Write-Host "`n==> 4. 注册 Windows 服务（或 NSSM）" -ForegroundColor Cyan
$serviceName = "ZyEngine"
$existing = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "[INFO] 服务已存在，停止后重建" -ForegroundColor Cyan
    Stop-Service $serviceName -Force -ErrorAction SilentlyContinue
}
# 用 sc.exe 创建（简单情况）。生产推荐 NSSM：详见 systemd/zy-engine-windows.md
$jarPath = "$ZyHome\lib\zy-engine.jar"
if (-not (Test-Path $jarPath)) { throw "未找到 $jarPath，请确认发布包已解压到 $ZyHome" }
$javaExe = (Get-Command java).Source
$binPath = "`"$javaExe`" -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -Xms1g -Xmx2g -jar `"$jarPath`""
& sc.exe create $serviceName binPath= "$binPath" start= auto DisplayName= "ZY Engine"
if ($LASTEXITCODE -ne 0) {
    Write-Host "[WARN] Windows 服务注册失败；建议安装 NSSM 后用 'nssm install ZyEngine' 设置" -ForegroundColor Yellow
} else {
    Write-Host "[OK]   Windows 服务已注册：$serviceName" -ForegroundColor Green
}

Write-Host "`n==> 5. 启动" -ForegroundColor Cyan
Start-Service $serviceName -ErrorAction Stop
Start-Sleep -Seconds 5
$svc = Get-Service $serviceName
Write-Host "[OK]   服务状态：$($svc.Status)" -ForegroundColor Green

Write-Host "`n==> 6. 健康检查" -ForegroundColor Cyan
& powershell -ExecutionPolicy Bypass -File (Join-Path $ScriptDir "healthcheck.ps1")
if ($LASTEXITCODE -ne 0) { throw "健康检查失败" }

Write-Host "`n==> 完成" -ForegroundColor Cyan
Write-Host "下一步：配置前端 Nginx / IIS 反向代理；跑客户验收剧本" -ForegroundColor Cyan
