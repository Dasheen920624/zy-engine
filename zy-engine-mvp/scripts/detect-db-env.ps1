param(
  [switch]$BootstrapLocal
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$engineRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $engineRoot
$localDbDir = Join-Path $engineRoot "data\local-db"
$oracleEnv = Join-Path $repoRoot ".env.oracle.local"

$oracleConfigured = $false
if (Test-Path -LiteralPath $oracleEnv) {
  . (Join-Path $PSScriptRoot "oracle-env.ps1")
  Import-ZyEngineOracleLocalEnv -ScriptRoot $PSScriptRoot
}

if (![string]::IsNullOrWhiteSpace($env:ZYENGINE_DB_URL) -and
    ![string]::IsNullOrWhiteSpace($env:ZYENGINE_DB_USERNAME) -and
    ![string]::IsNullOrWhiteSpace($env:ZYENGINE_DB_PASSWORD)) {
  $oracleConfigured = $true
}

if ($BootstrapLocal -and !(Test-Path -LiteralPath $localDbDir)) {
  New-Item -ItemType Directory -Force -Path $localDbDir | Out-Null
}

$result = [ordered]@{
  production_db = "ORACLE"
  development_db = "LOCAL_H2_FILE"
  db_role_rule = "PRODUCTION_AUTHORITY for Oracle/DM/PostgreSQL/Kingbase; DEVELOPMENT_LOCAL for LOCAL_H2_FILE"
  oracle_config_file = (Test-Path -LiteralPath $oracleEnv)
  oracle_env_ready = $oracleConfigured
  local_db_dir = $localDbDir
  local_db_ready = (Test-Path -LiteralPath $localDbDir)
  recommended_mode = $(if ($oracleConfigured) { "ORACLE" } else { "LOCAL_H2" })
  start_command = $(if ($oracleConfigured) { ".\scripts\start-oracle.cmd" } else { ".\scripts\start-local-db.cmd" })
}

$result.GetEnumerator() | ForEach-Object {
  "{0}={1}" -f $_.Key, $_.Value
}
