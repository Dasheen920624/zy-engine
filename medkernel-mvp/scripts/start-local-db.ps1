param(
  [int]$Port = 18082,
  [string]$DbFile = ""
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$engineRoot = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $engineRoot "target\medkernel-mvp-0.1.0-SNAPSHOT.jar"

if (!(Test-Path -LiteralPath $jar)) {
  throw "Jar not found. Run scripts/build.ps1 first."
}

if ([string]::IsNullOrWhiteSpace($DbFile)) {
  $localDbDir = Join-Path $engineRoot "data\local-db"
  New-Item -ItemType Directory -Force -Path $localDbDir | Out-Null
  $DbFile = Join-Path $localDbDir "medkernel-local"
}

$dbFileForJdbc = $DbFile.Replace("\", "/")
$env:MEDKERNEL_DB_ENABLED = "true"
$env:MEDKERNEL_DB_ROLE = "development"
$env:MEDKERNEL_DB_DIALECT = "h2"
$env:MEDKERNEL_DB_URL = "jdbc:h2:file:$dbFileForJdbc;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_ON_EXIT=FALSE"
$env:MEDKERNEL_DB_USERNAME = "sa"
$env:MEDKERNEL_DB_PASSWORD = ""
$env:MEDKERNEL_DB_INIT_SCHEMA = "true"

$java = "java"
$java8 = "C:\Program Files\Java\jdk1.8.0_51\bin\java.exe"
if (Test-Path -LiteralPath $java8) {
  $java = $java8
}

Set-Location $engineRoot
& $java "-Dfile.encoding=UTF-8" -jar $jar "--server.port=$Port" `
  "--medkernel.database.enabled=true" `
  "--medkernel.database.role=development" `
  "--medkernel.database.dialect=h2" `
  "--medkernel.database.url=$env:MEDKERNEL_DB_URL" `
  "--medkernel.database.username=sa" `
  "--medkernel.database.password=" `
  "--medkernel.database.init-schema=true"
