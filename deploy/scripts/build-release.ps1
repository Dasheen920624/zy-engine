# 构建发布包 —— Windows
# 用法：PowerShell -ExecutionPolicy Bypass -File build-release.ps1 `
#         -Version 1.2.3 `
#         [-IncludeFrontend] `
#         [-JdkTargets "linux-x86_64,linux-aarch64,windows-x86_64"] `
#         [-Output .\dist]

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [string]$Version,
    [switch]$IncludeFrontend,
    [string]$JdkTargets = "",
    [string]$Output = ".\dist"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..\..")).Path

$gitHash = (git -C $RepoRoot rev-parse --short HEAD).Trim()
$buildTime = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssK")
$pkgName = "medkernel-v$Version-$gitHash"

$staging = Join-Path ([System.IO.Path]::GetTempPath()) "zy-release-$gitHash"
if (Test-Path $staging) { Remove-Item -Recurse -Force $staging }
$stageRoot = Join-Path $staging $pkgName
New-Item -ItemType Directory -Force -Path $stageRoot | Out-Null
Write-Host "==> Staging: $stageRoot" -ForegroundColor Cyan

Write-Host "`n==> 1. 构建后端 jar" -ForegroundColor Cyan
Push-Location (Join-Path $RepoRoot "medkernel-mvp")
try {
    & mvn -B -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Maven 构建失败" }
} finally { Pop-Location }
$jarFile = Get-ChildItem (Join-Path $RepoRoot "medkernel-mvp\target") -Filter "medkernel-mvp-*.jar" | Select-Object -First 1
if (-not $jarFile) { throw "未找到 jar" }
New-Item -ItemType Directory -Force -Path "$stageRoot\lib" | Out-Null
Copy-Item $jarFile.FullName "$stageRoot\lib\medkernel.jar"
$jarSha = (Get-FileHash "$stageRoot\lib\medkernel.jar" -Algorithm SHA256).Hash.ToLower()
Write-Host "[OK]   jar sha256: $jarSha" -ForegroundColor Green

Write-Host "`n==> 2. 前端" -ForegroundColor Cyan
$feSha = "-"
if ($IncludeFrontend -and (Test-Path (Join-Path $RepoRoot "frontend"))) {
    Push-Location (Join-Path $RepoRoot "frontend")
    try {
        & npm ci --prefer-offline
        if ($LASTEXITCODE -ne 0) { throw "npm ci 失败" }
        & npm run build
        if ($LASTEXITCODE -ne 0) { throw "npm run build 失败" }
    } finally { Pop-Location }
    New-Item -ItemType Directory -Force -Path "$stageRoot\frontend" | Out-Null
    Copy-Item (Join-Path $RepoRoot "frontend\dist") "$stageRoot\frontend\" -Recurse
    Write-Host "[OK]   frontend dist 已打入" -ForegroundColor Green
} else {
    Write-Host "[SKIP] 未指定 -IncludeFrontend 或目录不存在" -ForegroundColor DarkGray
}

Write-Host "`n==> 3. 拷贝 DDL / scripts / docs / profiles" -ForegroundColor Cyan
Copy-Item (Join-Path $RepoRoot "medkernel-mvp\db") "$stageRoot\" -Recurse
Copy-Item $ScriptDir "$stageRoot\scripts" -Recurse
$engineScripts = Join-Path $RepoRoot "medkernel-mvp\scripts"
if (Test-Path $engineScripts) {
    Copy-Item $engineScripts "$stageRoot\engine-scripts" -Recurse
}
foreach ($d in @("systemd", "nginx", "profiles")) {
    $src = Join-Path $ScriptDir "..\$d"
    if (Test-Path $src) {
        Copy-Item $src "$stageRoot\" -Recurse
    }
}
if (Test-Path "$RepoRoot\CHANGELOG.md") {
    Copy-Item "$RepoRoot\CHANGELOG.md" "$stageRoot\CHANGELOG.md"
}

Write-Host "`n==> 4. manifest.json" -ForegroundColor Cyan
$manifest = @{
    name = "medkernel"
    version = $Version
    git_hash = $gitHash
    build_time = $buildTime
    build_host = $env:COMPUTERNAME
    components = @{
        backend  = @{ jar = "lib/medkernel.jar"; sha256 = $jarSha }
        frontend = @{ dist = "frontend/dist/"; sha256 = $feSha }
    }
    supported_os = @(
        "centos7-x86_64", "openeuler-22.03-x86_64", "openeuler-22.03-aarch64",
        "uos-v20-x86_64", "uos-v20-aarch64",
        "kylin-v10-x86_64", "kylin-v10-aarch64",
        "windows-server-x86_64"
    )
}
$manifest | ConvertTo-Json -Depth 10 | Set-Content -Path "$stageRoot\manifest.json" -Encoding UTF8
Write-Host "[OK]   manifest.json" -ForegroundColor Green

Write-Host "`n==> 5. checksums.sha256" -ForegroundColor Cyan
$sumFile = "$stageRoot\checksums.sha256"
Get-ChildItem -Path $stageRoot -Recurse -File | Where-Object { $_.Name -ne "checksums.sha256" } | ForEach-Object {
    $h = (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLower()
    $rel = $_.FullName.Substring($stageRoot.Length + 1).Replace("\", "/")
    "$h  $rel"
} | Set-Content $sumFile -Encoding ASCII
Write-Host "[OK]   checksums.sha256" -ForegroundColor Green

Write-Host "`n==> 6. 打 tar.gz" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $Output | Out-Null
$outFile = Join-Path $Output "$pkgName.tar.gz"
& tar -czvf $outFile -C $staging $pkgName
if ($LASTEXITCODE -ne 0) { throw "tar 打包失败" }
$tarSha = (Get-FileHash $outFile -Algorithm SHA256).Hash.ToLower()
"$tarSha  $(Split-Path $outFile -Leaf)" | Set-Content "$outFile.sha256" -Encoding ASCII

Write-Host "`n[OK]   完成" -ForegroundColor Green
Write-Host "产物：$outFile"
Write-Host "sha256：$tarSha"
