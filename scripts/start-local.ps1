param(
  [switch]$SkipBuild,
  [switch]$SkipInstall,
  [switch]$BackendOnly,
  [switch]$FrontendOnly
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$BackendDir = Join-Path $ProjectRoot "medkernel-backend"
$FrontendDir = Join-Path $ProjectRoot "frontend"

function Write-Section([string]$Title) {
  Write-Host ""
  Write-Host "=== $Title ===" -ForegroundColor Cyan
}

function Find-Java21 {
  $candidates = New-Object System.Collections.Generic.List[string]

  if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $candidates.Add((Join-Path $env:JAVA_HOME "bin\java.exe"))
  }
  $candidates.Add("D:\java\jdk-21\bin\java.exe")

  $pathJava = Get-Command java -ErrorAction SilentlyContinue
  if ($pathJava) {
    $candidates.Add($pathJava.Source)
  }

  foreach ($candidate in ($candidates | Select-Object -Unique)) {
    if (-not (Test-Path -LiteralPath $candidate)) {
      continue
    }
    $version = & $candidate -version 2>&1 | Out-String
    if ($version -match 'version "21\.' -or $version -match 'openjdk version "21\.') {
      return $candidate
    }
  }

  throw "未找到 JDK 21。请安装 JDK 21，或将 JAVA_HOME 指向 JDK 21。"
}

function Get-Listener([int]$Port) {
  Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
    Select-Object -First 1
}

function Wait-HttpOk([string]$Url, [int]$Seconds) {
  $deadline = (Get-Date).AddSeconds($Seconds)
  do {
    try {
      $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3
      if ([int]$response.StatusCode -ge 200 -and [int]$response.StatusCode -lt 500) {
        return $true
      }
    } catch {
      Start-Sleep -Milliseconds 700
    }
  } while ((Get-Date) -lt $deadline)

  return $false
}

function Start-Backend {
  Write-Section "Backend"

  $listener = Get-Listener 18080
  if ($listener) {
    Write-Host "  [SKIP] 18080 已被进程 $($listener.OwningProcess) 监听。"
    return
  }

  if (-not $SkipBuild) {
    Push-Location $BackendDir
    try {
      Write-Host "  [RUN] mvn -DskipTests package"
      mvn -DskipTests package
    } finally {
      Pop-Location
    }
  }

  $java = Find-Java21
  $jar = Join-Path $BackendDir "target\medkernel-backend-1.0.0-SNAPSHOT.jar"
  if (-not (Test-Path -LiteralPath $jar)) {
    throw "后端 jar 不存在：$jar。请去掉 -SkipBuild 重新运行。"
  }

  $out = Join-Path $BackendDir "target\backend-dev.out.log"
  $err = Join-Path $BackendDir "target\backend-dev.err.log"
  Remove-Item -LiteralPath $out,$err -ErrorAction SilentlyContinue

  $process = Start-Process -FilePath $java `
    -ArgumentList @("-jar", "target\medkernel-backend-1.0.0-SNAPSHOT.jar") `
    -WorkingDirectory $BackendDir `
    -RedirectStandardOutput $out `
    -RedirectStandardError $err `
    -WindowStyle Hidden `
    -PassThru

  Write-Host "  [START] backend pid=$($process.Id)"
  if (-not (Wait-HttpOk "http://localhost:18080/medkernel/api/v1/system/ping" 40)) {
    throw "后端启动超时，请查看 $out 和 $err"
  }
  Write-Host "  [OK] http://localhost:18080/medkernel"
}

function Start-Frontend {
  Write-Section "Frontend"

  $listener = Get-Listener 5173
  if ($listener) {
    Write-Host "  [SKIP] 5173 已被进程 $($listener.OwningProcess) 监听。"
    return
  }

  Push-Location $FrontendDir
  try {
    if (-not $SkipInstall -and -not (Test-Path -LiteralPath (Join-Path $FrontendDir "node_modules"))) {
      Write-Host "  [RUN] npm ci"
      npm ci
    }

    $out = Join-Path $FrontendDir "frontend-dev.out.log"
    $err = Join-Path $FrontendDir "frontend-dev.err.log"
    Remove-Item -LiteralPath $out,$err -ErrorAction SilentlyContinue

    $process = Start-Process -FilePath "npm.cmd" `
      -ArgumentList @("run", "dev", "--", "--host", "0.0.0.0") `
      -WorkingDirectory $FrontendDir `
      -RedirectStandardOutput $out `
      -RedirectStandardError $err `
      -WindowStyle Hidden `
      -PassThru

    Write-Host "  [START] frontend pid=$($process.Id)"
    if (-not (Wait-HttpOk "http://localhost:5173/" 30)) {
      throw "前端启动超时，请查看 $out 和 $err"
    }
    Write-Host "  [OK] http://localhost:5173"
  } finally {
    Pop-Location
  }
}

if (-not $FrontendOnly) {
  Start-Backend
}

if (-not $BackendOnly) {
  Start-Frontend
}

Write-Section "Ready"
Write-Host "  后端 API:      http://localhost:18080/medkernel/api/v1/system/ping"
Write-Host "  后端健康:     http://localhost:18080/medkernel/actuator/health"
Write-Host "  Swagger:       http://localhost:18080/medkernel/swagger-ui.html"
Write-Host "  前端工作台:   http://localhost:5173"
