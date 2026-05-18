param(
  [switch]$WithTests
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$engineRoot = Split-Path -Parent $PSScriptRoot
Set-Location $engineRoot

$mvnArgs = @("-gs", ".mvn/http-settings.xml", "-s", ".mvn/http-settings.xml", "-q")
if (-not $WithTests) {
  $mvnArgs += "-DskipTests"
}
$mvnArgs += "package"

& mvn @mvnArgs
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

Write-Host "Build completed: target/medkernel-mvp-0.1.0-SNAPSHOT.jar"
