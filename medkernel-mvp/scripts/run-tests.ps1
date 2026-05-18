$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$engineRoot = Split-Path -Parent $PSScriptRoot
Set-Location $engineRoot

& mvn -gs .mvn/http-settings.xml -s .mvn/http-settings.xml -q test
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

Write-Host "JUnit tests passed."
