param(
  [int]$Port = 18080
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$engineRoot = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $engineRoot "target\medkernel-mvp-0.1.0-SNAPSHOT.jar"

if (!(Test-Path -LiteralPath $jar)) {
  throw "Jar not found. Run scripts/build.ps1 first."
}

$java = "java"
$java8 = "C:\Program Files\Java\jdk1.8.0_51\bin\java.exe"
if (Test-Path -LiteralPath $java8) {
  $java = $java8
}

Set-Location $engineRoot
& $java "-Dfile.encoding=UTF-8" -jar $jar "--server.port=$Port"
