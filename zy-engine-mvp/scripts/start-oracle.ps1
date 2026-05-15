param(
  [int]$Port = 18081,
  [string]$DbUrl = $env:ZYENGINE_DB_URL,
  [string]$DbUsername = $env:ZYENGINE_DB_USERNAME,
  [string]$DbPassword = $env:ZYENGINE_DB_PASSWORD
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$env:NLS_LANG = "AMERICAN_AMERICA.AL32UTF8"

$engineRoot = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $engineRoot "target\zy-engine-mvp-0.1.0-SNAPSHOT.jar"

if (!(Test-Path -LiteralPath $jar)) {
  throw "Jar not found. Run scripts/build.ps1 first."
}
if ([string]::IsNullOrWhiteSpace($DbUrl)) {
  $DbUrl = "jdbc:oracle:thin:@//192.168.4.25:1521/ORCL"
}
if ([string]::IsNullOrWhiteSpace($DbUsername)) {
  $DbUsername = "ZYENGINE"
}
if ([string]::IsNullOrWhiteSpace($DbPassword)) {
  throw "Please set ZYENGINE_DB_PASSWORD before starting Oracle mode."
}

$env:ZYENGINE_DB_ENABLED = "true"
$env:ZYENGINE_DB_URL = $DbUrl
$env:ZYENGINE_DB_USERNAME = $DbUsername
$env:ZYENGINE_DB_PASSWORD = $DbPassword

$java = "java"
$java8 = "C:\Program Files\Java\jdk1.8.0_51\bin\java.exe"
if (Test-Path -LiteralPath $java8) {
  $java = $java8
}

Set-Location $engineRoot
& $java "-Dfile.encoding=UTF-8" -jar $jar "--server.port=$Port" "--zyengine.database.enabled=true"
