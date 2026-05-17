param(
  [int]$Port = 18082,
  [string]$DbFile = ""
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$engineRoot = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $engineRoot "target\zy-engine-mvp-0.1.0-SNAPSHOT.jar"

if (!(Test-Path -LiteralPath $jar)) {
  throw "Jar not found. Run scripts/build.ps1 first."
}

if ([string]::IsNullOrWhiteSpace($DbFile)) {
  $localDbDir = Join-Path $engineRoot "data\local-db"
  New-Item -ItemType Directory -Force -Path $localDbDir | Out-Null
  $DbFile = Join-Path $localDbDir "zyengine-local"
}

$dbFileForJdbc = $DbFile.Replace("\", "/")
$env:ZYENGINE_DB_ENABLED = "true"
$env:ZYENGINE_DB_DIALECT = "h2"
$env:ZYENGINE_DB_URL = "jdbc:h2:file:$dbFileForJdbc;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_ON_EXIT=FALSE"
$env:ZYENGINE_DB_USERNAME = "sa"
$env:ZYENGINE_DB_PASSWORD = ""
$env:ZYENGINE_DB_INIT_SCHEMA = "true"

$java = "java"
$java8 = "C:\Program Files\Java\jdk1.8.0_51\bin\java.exe"
if (Test-Path -LiteralPath $java8) {
  $java = $java8
}

Set-Location $engineRoot
& $java "-Dfile.encoding=UTF-8" -jar $jar "--server.port=$Port" `
  "--zyengine.database.enabled=true" `
  "--zyengine.database.dialect=h2" `
  "--zyengine.database.url=$env:ZYENGINE_DB_URL" `
  "--zyengine.database.username=sa" `
  "--zyengine.database.password=" `
  "--zyengine.database.init-schema=true"
