function Import-MedKernelOracleLocalEnv {
  param(
    [string]$ScriptRoot
  )

  $engineRoot = Split-Path -Parent $ScriptRoot
  $workspaceRoot = Split-Path -Parent $engineRoot
  $envFile = Join-Path $workspaceRoot ".env.oracle.local"
  if (!(Test-Path -LiteralPath $envFile)) {
    return
  }

  Get-Content -LiteralPath $envFile -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
      return
    }

    if ($line -match '^\$env:([A-Za-z0-9_]+)\s*=\s*''(.*)''\s*$') {
      [Environment]::SetEnvironmentVariable($matches[1], $matches[2])
      return
    }
    if ($line -match '^\$env:([A-Za-z0-9_]+)\s*=\s*"(.*)"\s*$') {
      [Environment]::SetEnvironmentVariable($matches[1], $matches[2])
      return
    }
    if ($line -match '^\$env:([A-Za-z0-9_]+)\s*=\s*(.*)\s*$') {
      [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim())
    }
  }
}
