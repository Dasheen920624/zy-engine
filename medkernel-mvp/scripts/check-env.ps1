# check-env.ps1 - Environment Check and Diagnostics
# Usage: .\scripts\check-env.ps1 [-Verbose] [-Fix]

param(
  [switch]$Verbose,
  [switch]$Fix
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

# Colors
$COLOR_OK = "Green"
$COLOR_WARN = "Yellow"
$COLOR_FAIL = "Red"
$COLOR_INFO = "Cyan"

# Counters
$script:passCount = 0
$script:warnCount = 0
$script:failCount = 0

# Helper functions
function Write-CheckResult {
  param(
    [string]$Name,
    [string]$Status,
    [string]$Message,
    [string]$Color
  )
  $icon = switch ($Status) {
    "PASS" { "[PASS]"; $script:passCount++ }
    "WARN" { "[WARN]"; $script:warnCount++ }
    "FAIL" { "[FAIL]"; $script:failCount++ }
    "INFO" { "[INFO]" }
    default { "[????]" }
  }
  Write-Host "$icon $Name" -ForegroundColor $Color
  if ($Verbose -and $Message) {
    Write-Host "       $Message" -ForegroundColor Gray
  }
}

function Test-CommandExists {
  param([string]$Command)
  $null = Get-Command $Command -ErrorAction SilentlyContinue
  return $?
}

function Test-PortAvailable {
  param([int]$Port)
  $conn = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
  return ($null -eq $conn)
}

# ============================================
# 1. Java/JDK Check
# ============================================
Write-Host "`n=== 1. Java/JDK Environment ===" -ForegroundColor $COLOR_INFO

if (Test-CommandExists "java") {
  $javaVersion = & java -version 2>&1 | Select-Object -First 1
  if ($javaVersion -match '"(\d+)(?:\.(\d+))?') {
    $major = [int]$Matches[1]
    $minor = if ($Matches[2]) { [int]$Matches[2] } else { 0 }
    $effectiveMajor = if ($major -eq 1) { $minor } else { $major }
    if ($effectiveMajor -ge 8) {
      Write-CheckResult "JDK Version" "PASS" "JDK $effectiveMajor ($javaVersion)" $COLOR_OK
    } else {
      Write-CheckResult "JDK Version" "FAIL" "Require JDK 8+, current: $javaVersion" $COLOR_FAIL
    }
  } else {
    Write-CheckResult "JDK Version" "WARN" "Cannot parse version: $javaVersion" $COLOR_WARN
  }
} else {
  Write-CheckResult "JDK Installation" "FAIL" "java command not found, install JDK 8+" $COLOR_FAIL
}

if ($env:JAVA_HOME) {
  if (Test-Path -LiteralPath $env:JAVA_HOME) {
    Write-CheckResult "JAVA_HOME" "PASS" $env:JAVA_HOME $COLOR_OK
  } else {
    Write-CheckResult "JAVA_HOME" "FAIL" "Path not exists: $env:JAVA_HOME" $COLOR_FAIL
  }
} else {
  Write-CheckResult "JAVA_HOME" "WARN" "Not set" $COLOR_WARN
}

if (Test-CommandExists "mvn") {
  try {
    $mvnOutput = & mvn --version 2>&1
    $mvnVersion = $mvnOutput | Select-Object -First 1
    Write-CheckResult "Maven" "PASS" $mvnVersion $COLOR_OK
  } catch {
    Write-CheckResult "Maven" "WARN" "Found but cannot execute: $_" $COLOR_WARN
  }
} else {
  Write-CheckResult "Maven" "FAIL" "mvn command not found" $COLOR_FAIL
}

# ============================================
# 2. Oracle Client Check
# ============================================
Write-Host "`n=== 2. Oracle Client ===" -ForegroundColor $COLOR_INFO

if (Test-CommandExists "sqlplus") {
  $sqlplusVersion = & sqlplus -V 2>&1 | Select-Object -Last 1
  Write-CheckResult "sqlplus" "PASS" $sqlplusVersion $COLOR_OK
} else {
  Write-CheckResult "sqlplus" "WARN" "Not found (only needed for Oracle production)" $COLOR_WARN
}

if ($env:ORACLE_HOME) {
  Write-CheckResult "ORACLE_HOME" "PASS" $env:ORACLE_HOME $COLOR_OK
} else {
  Write-CheckResult "ORACLE_HOME" "WARN" "Not set (only needed for Oracle production)" $COLOR_WARN
}

if ($env:TNS_ADMIN) {
  Write-CheckResult "TNS_ADMIN" "PASS" $env:TNS_ADMIN $COLOR_OK
} else {
  Write-CheckResult "TNS_ADMIN" "INFO" "Not set (optional)" $COLOR_INFO
}

# ============================================
# 3. Database Connection Check
# ============================================
Write-Host "`n=== 3. Database Connection ===" -ForegroundColor $COLOR_INFO

$engineRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $engineRoot
$oracleEnv = Join-Path $repoRoot ".env.oracle.local"

if (Test-Path -LiteralPath $oracleEnv) {
  Write-CheckResult "Oracle Config File" "PASS" $oracleEnv $COLOR_OK
  . (Join-Path $PSScriptRoot "oracle-env.ps1")
  Import-MedKernelOracleLocalEnv -ScriptRoot $PSScriptRoot
} else {
  Write-CheckResult "Oracle Config File" "INFO" "Not found (development mode)" $COLOR_INFO
}

$oracleReady = -not [string]::IsNullOrWhiteSpace($env:MEDKERNEL_DB_URL) -and
               -not [string]::IsNullOrWhiteSpace($env:MEDKERNEL_DB_USERNAME) -and
               -not [string]::IsNullOrWhiteSpace($env:MEDKERNEL_DB_PASSWORD)

if ($oracleReady) {
  Write-CheckResult "Oracle Connection Config" "PASS" "URL=$($env:MEDKERNEL_DB_URL)" $COLOR_OK
  
  if (Test-CommandExists "sqlplus") {
    $connTest = & sqlplus -S "$($env:MEDKERNEL_DB_USERNAME)/$($env:MEDKERNEL_DB_PASSWORD)@$($env:MEDKERNEL_DB_URL)" "SELECT 1 FROM DUAL;" 2>&1
    if ($LASTEXITCODE -eq 0) {
      Write-CheckResult "Oracle Connection Test" "PASS" "Connected" $COLOR_OK
    } else {
      Write-CheckResult "Oracle Connection Test" "FAIL" "Connection failed: $connTest" $COLOR_FAIL
    }
  }
} else {
  Write-CheckResult "Oracle Connection Config" "INFO" "Not configured (will use LOCAL_H2_FILE)" $COLOR_INFO
}

$localDbDir = Join-Path $engineRoot "data\local-db"
if (Test-Path -LiteralPath $localDbDir) {
  Write-CheckResult "Local H2 Database Dir" "PASS" $localDbDir $COLOR_OK
} else {
  Write-CheckResult "Local H2 Database Dir" "INFO" "Not exists (auto-create on first start)" $COLOR_INFO
  if ($Fix) {
    New-Item -ItemType Directory -Force -Path $localDbDir | Out-Null
    Write-CheckResult "Local H2 Database Dir" "PASS" "Created: $localDbDir" $COLOR_OK
  }
}

# ============================================
# 4. Port Check
# ============================================
Write-Host "`n=== 4. Port Availability ===" -ForegroundColor $COLOR_INFO

$requiredPorts = @{
  18080 = "medkernel main service"
  8080 = "Frontend dev server (optional)"
}

foreach ($port in $requiredPorts.GetEnumerator()) {
  if (Test-PortAvailable -Port $port.Key) {
    Write-CheckResult "Port $($port.Key)" "PASS" "$($port.Value) - Available" $COLOR_OK
  } else {
    $proc = Get-NetTCPConnection -LocalPort $port.Key -ErrorAction SilentlyContinue | 
            Select-Object -First 1 -ExpandProperty OwningProcess
    $procName = if ($proc) { (Get-Process -Id $proc -ErrorAction SilentlyContinue).ProcessName } else { "Unknown" }
    Write-CheckResult "Port $($port.Key)" "WARN" "$($port.Value) - In use (Process: $procName)" $COLOR_WARN
  }
}

# ============================================
# 5. Project Files Check
# ============================================
Write-Host "`n=== 5. Project Files ===" -ForegroundColor $COLOR_INFO

$requiredFiles = @(
  @{ Path = "pom.xml"; Name = "Maven Config" },
  @{ Path = "src\main\resources\application.yml"; Name = "Application Config" },
  @{ Path = "src\main\resources\db\local\h2_core_ddl.sql"; Name = "H2 DDL Script" }
)

foreach ($file in $requiredFiles) {
  $fullPath = Join-Path $engineRoot $file.Path
  if (Test-Path -LiteralPath $fullPath) {
    Write-CheckResult $file.Name "PASS" $file.Path $COLOR_OK
  } else {
    Write-CheckResult $file.Name "FAIL" "Missing: $file.Path" $COLOR_FAIL
  }
}

# ============================================
# 6. Build Artifacts Check
# ============================================
Write-Host "`n=== 6. Build Artifacts ===" -ForegroundColor $COLOR_INFO

$jarPath = Join-Path $engineRoot "target\medkernel-mvp-0.1.0-SNAPSHOT.jar"
if (Test-Path -LiteralPath $jarPath) {
  $jarInfo = Get-Item $jarPath
  $sizeMB = [math]::Round($jarInfo.Length / 1MB, 2)
  Write-CheckResult "Application JAR" "PASS" "medkernel-mvp-0.1.0-SNAPSHOT.jar ($sizeMB MB)" $COLOR_OK
} else {
  Write-CheckResult "Application JAR" "WARN" "Not built, run: .\scripts\build.ps1" $COLOR_WARN
}

# ============================================
# Summary Report
# ============================================
Write-Host "`n========================================" -ForegroundColor $COLOR_INFO
Write-Host "Check Complete" -ForegroundColor $COLOR_INFO
Write-Host "========================================" -ForegroundColor $COLOR_INFO
Write-Host "  Pass: $script:passCount" -ForegroundColor $COLOR_OK
Write-Host "  Warn: $script:warnCount" -ForegroundColor $COLOR_WARN
Write-Host "  Fail: $script:failCount" -ForegroundColor $COLOR_FAIL

if ($script:failCount -gt 0) {
  Write-Host "`nCritical issues found. Please fix and retry." -ForegroundColor $COLOR_FAIL
  exit 1
} elseif ($script:warnCount -gt 0) {
  Write-Host "`nWarnings found, but basic functionality not affected." -ForegroundColor $COLOR_WARN
  exit 0
} else {
  Write-Host "`nAll environment checks passed!" -ForegroundColor $COLOR_OK
  exit 0
}
