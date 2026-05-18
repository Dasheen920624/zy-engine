param(
  [int]$Port = 18081,
  [string]$DbUrl = $env:MEDKERNEL_DB_URL,
  [string]$DbUsername = $env:MEDKERNEL_DB_USERNAME,
  [string]$DbPassword = $env:MEDKERNEL_DB_PASSWORD
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$env:NLS_LANG = "AMERICAN_AMERICA.AL32UTF8"

. (Join-Path $PSScriptRoot "oracle-env.ps1")
Import-MedKernelOracleLocalEnv -ScriptRoot $PSScriptRoot

$engineRoot = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $engineRoot "target\medkernel-mvp-0.1.0-SNAPSHOT.jar"

if (!(Test-Path -LiteralPath $jar)) {
  throw "Jar not found. Run scripts/build.ps1 first."
}
if ([string]::IsNullOrWhiteSpace($DbUrl)) {
  $DbUrl = $env:MEDKERNEL_DB_URL
}
if ([string]::IsNullOrWhiteSpace($DbUsername)) {
  $DbUsername = $env:MEDKERNEL_DB_USERNAME
}
if ([string]::IsNullOrWhiteSpace($DbPassword)) {
  $DbPassword = $env:MEDKERNEL_DB_PASSWORD
}
if ([string]::IsNullOrWhiteSpace($DbUrl)) {
  $DbUrl = "jdbc:oracle:thin:@//192.168.4.25:1521/ORCL"
}
if ([string]::IsNullOrWhiteSpace($DbUsername)) {
  $DbUsername = "ZYENGINE"
}
if ([string]::IsNullOrWhiteSpace($DbPassword)) {
  throw "Please set MEDKERNEL_DB_PASSWORD before starting Oracle mode."
}

$env:MEDKERNEL_DB_ENABLED = "true"
$env:MEDKERNEL_DB_ROLE = "production"
$env:MEDKERNEL_DB_URL = $DbUrl
$env:MEDKERNEL_DB_USERNAME = $DbUsername
$env:MEDKERNEL_DB_PASSWORD = $DbPassword

$java = "java"
$java8 = "C:\Program Files\Java\jdk1.8.0_51\bin\java.exe"
if (Test-Path -LiteralPath $java8) {
  $java = $java8
}

Set-Location $engineRoot
& $java "-Dfile.encoding=UTF-8" -jar $jar "--server.port=$Port" "--medkernel.database.enabled=true" "--medkernel.database.role=production"
