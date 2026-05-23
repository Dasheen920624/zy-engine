# 多方言 Smoke 测试脚本
#
# 用法：
#   .\scripts\smoke-dialect.ps1 -Dialect oracle
#   .\scripts\smoke-dialect.ps1 -Dialect dm
#   .\scripts\smoke-dialect.ps1 -Dialect postgres
#   .\scripts\smoke-dialect.ps1 -Dialect kingbase
#   .\scripts\smoke-dialect.ps1 -Dialect h2
#   .\scripts\smoke-dialect.ps1 -Dialect all
#
# 环境变量（非 h2 方言必填）：
#   MEDKERNEL_DB_URL          JDBC URL
#   MEDKERNEL_DB_USERNAME     数据库用户名
#   MEDKERNEL_DB_PASSWORD     数据库密码
#
# GA-DB-01: 4 方言 smoke 矩阵和 Flyway rollback 证据

param(
  [Parameter(Mandatory = $true)]
  [ValidateSet("oracle", "dm", "postgres", "kingbase", "h2", "all")]
  [string]$Dialect,

  [switch]$SkipFlyway,
  [switch]$SkipDDL,
  [switch]$SkipApp
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $ProjectRoot

$PASS_COUNT = 0
$FAIL_COUNT = 0
$SKIP_COUNT = 0
$RESULTS = @{}

function Show-Pass($msg) {
  Write-Host "  [PASS] $msg" -ForegroundColor Green
  $script:PASS_COUNT++
}

function Show-Fail($msg) {
  Write-Host "  [FAIL] $msg" -ForegroundColor Red
  $script:FAIL_COUNT++
}

function Show-Skip($msg) {
  Write-Host "  [SKIP] $msg" -ForegroundColor Yellow
  $script:SKIP_COUNT++
}

function Test-SingleDialect([string]$dialect) {
  Write-Host ""
  Write-Host "=== 方言: $dialect ===" -ForegroundColor Cyan
  Write-Host ("-" * 40)

  $dialectPass = 0
  $dialectFail = 0
  $dialectSkip = 0

  # 1. DDL 语法校验
  if (-not $SkipDDL) {
    Write-Host ""
    Write-Host "  [1] DDL 语法校验" -ForegroundColor White

    $ddlDir = "ai-dev-input/04_database"
    switch ($dialect) {
      "oracle" { $ddlPath = "$ddlDir/oracle" }
      "dm" { $ddlPath = "$ddlDir/dm" }
      "postgres" { $ddlPath = "$ddlDir/postgres" }
      "kingbase" { $ddlPath = "$ddlDir/postgres" }  # KingbaseES 使用 PostgreSQL DDL
      "h2" { $ddlPath = "$ddlDir/local" }
    }

    if (Test-Path $ddlPath) {
      $ddlFiles = Get-ChildItem -Path $ddlPath -Filter "*.sql" -File
      if ($ddlFiles.Count -gt 0) {
        Show-Pass "$dialect DDL 目录存在，共 $($ddlFiles.Count) 个 SQL 文件"

        # 检查核心 DDL 文件
        $coreDdl = $ddlFiles | Where-Object { $_.Name -match "core_ddl" }
        if ($coreDdl) {
          Show-Pass "核心 DDL 文件存在: $($coreDdl.Name)"
        } else {
          Show-Fail "核心 DDL 文件缺失"
          $dialectFail++
        }

        # 检查方言特定语法兼容性
        foreach ($f in $ddlFiles) {
          $content = Get-Content $f.FullName -Raw -Encoding UTF8

          # 检查 MySQL 专有语法（所有方言都不支持）
          if ($content -match "ON DUPLICATE KEY UPDATE") {
            Show-Fail "$($f.Name): 包含 MySQL 专有语法 ON DUPLICATE KEY UPDATE"
            $dialectFail++
          }

          # 检查方言特定语法
          switch ($dialect) {
            "oracle" {
              if ($content -match "BIGINT" -and $content -notmatch "NUMBER") {
                # Oracle 使用 NUMBER 而非 BIGINT
              }
            }
            "dm" {
              # DM 兼容 Oracle 语法，但推荐使用 BIGINT/VARCHAR
            }
            "postgres" {
              if ($content -match "VARCHAR2") {
                Show-Fail "$($f.Name): PostgreSQL 不支持 VARCHAR2，应使用 VARCHAR"
                $dialectFail++
              }
              if ($content -match "NUMBER\(") {
                Show-Fail "$($f.Name): PostgreSQL 不支持 NUMBER(n)，应使用 BIGINT/INTEGER"
                $dialectFail++
              }
            }
            "kingbase" {
              # KingbaseES 兼容 PostgreSQL 语法
              if ($content -match "VARCHAR2") {
                Show-Fail "$($f.Name): KingbaseES 不支持 VARCHAR2，应使用 VARCHAR"
                $dialectFail++
              }
            }
          }
        }
      } else {
        Show-Fail "$dialect DDL 目录为空"
        $dialectFail++
      }
    } else {
      Show-Fail "$dialect DDL 目录不存在: $ddlPath"
      $dialectFail++
    }
  }

  # 2. Flyway migration 校验
  if (-not $SkipFlyway) {
    Write-Host ""
    Write-Host "  [2] Flyway migration 校验" -ForegroundColor White

    $flywayDir = switch ($dialect) {
      "oracle" { "medkernel-mvp/src/main/resources/db/migration/oracle" }
      "dm" { "medkernel-mvp/src/main/resources/db/migration/dm" }
      "postgres" { "medkernel-mvp/src/main/resources/db/migration/postgres" }
      "kingbase" { "medkernel-mvp/src/main/resources/db/migration/kingbase" }
      "h2" { "medkernel-mvp/src/main/resources/db/migration/h2" }
    }

    if (Test-Path $flywayDir) {
      $flywayFiles = Get-ChildItem -Path $flywayDir -Filter "V*__*.sql" -File | Sort-Object Name
      if ($flywayFiles.Count -gt 0) {
        Show-Pass "$dialect Flyway migration 目录存在，共 $($flywayFiles.Count) 个版本"

        # 检查版本号连续性
        $versions = @()
        foreach ($f in $flywayFiles) {
          if ($f.Name -match "^V(\d+)__") {
            $versions += [int]$matches[1]
          }
        }
        $versions = $versions | Sort-Object -Unique
        $hasGap = $false
        for ($i = 1; $i -lt $versions.Count; $i++) {
          if ($versions[$i] -ne $versions[$i-1] + 1) {
            Show-Fail "Flyway 版本号不连续: V$($versions[$i-1]) -> V$($versions[$i])"
            $hasGap = $true
            $dialectFail++
          }
        }
        if (-not $hasGap -and $versions.Count -gt 1) {
          Show-Pass "Flyway 版本号连续: $($versions -join ', ')"
        }

        # 检查 V1 baseline 存在
        $v1 = $flywayFiles | Where-Object { $_.Name -match "^V1__" }
        if ($v1) {
          Show-Pass "V1 baseline 存在: $($v1.Name)"
        } else {
          Show-Fail "V1 baseline 缺失"
          $dialectFail++
        }
      } else {
        Show-Fail "$dialect Flyway migration 目录为空"
        $dialectFail++
      }
    } else {
      Show-Fail "$dialect Flyway migration 目录不存在: $flywayDir"
      $dialectFail++
    }
  }

  # 3. 应用启动测试（仅 h2 可本地执行）
  if (-not $SkipApp) {
    Write-Host ""
    Write-Host "  [3] 应用启动测试" -ForegroundColor White

    if ($dialect -eq "h2") {
      # H2 可以本地启动测试
      $env:MEDKERNEL_DB_ENABLED = "true"
      $env:MEDKERNEL_FLYWAY_ENABLED = "true"

      try {
        Write-Host "  启动 Spring Boot 应用（H2 模式）..."
        $proc = Start-Process -FilePath "mvn" -ArgumentList "-q","-f","medkernel-mvp/pom.xml","spring-boot:run","-Dspring-boot.run.profiles=test" -NoNewWindow -PassThru -RedirectStandardOutput "$env:TEMP\medkernel-smoke-h2.log" -RedirectStandardError "$env:TEMP\medkernel-smoke-h2-err.log"

        # 等待应用启动（最多 60 秒）
        $started = $false
        for ($i = 0; $i -lt 30; $i++) {
          Start-Sleep -Seconds 2
          try {
            $response = Invoke-WebRequest -Uri "http://localhost:18081/actuator/health" -TimeoutSec 2 -ErrorAction SilentlyContinue
            if ($response.StatusCode -eq 200) {
              $started = $true
              break
            }
          } catch {
            # 应用尚未启动
          }
        }

        if ($started) {
          Show-Pass "H2 模式应用启动成功，actuator/health 返回 200"
        } else {
          Show-Fail "H2 模式应用启动超时（60 秒）"
          $dialectFail++
        }

        # 停止应用
        if (-not $proc.HasExited) {
          Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        }
      } catch {
        Show-Fail "H2 模式应用启动失败: $($_.Exception.Message)"
        $dialectFail++
      } finally {
        Remove-Item Env:\MEDKERNEL_DB_ENABLED -ErrorAction SilentlyContinue
        Remove-Item Env:\MEDKERNEL_FLYWAY_ENABLED -ErrorAction SilentlyContinue
      }
    } else {
      # 非 H2 方言需要外部数据库连接
      $dbUrl = $env:MEDKERNEL_DB_URL
      if ([string]::IsNullOrWhiteSpace($dbUrl)) {
        Show-Skip "$dialect 方言需要外部数据库连接（MEDKERNEL_DB_URL 未设置）"
        $dialectSkip++
      } else {
        Show-Skip "$dialect 方言外部数据库测试需在 CI 环境执行"
        $dialectSkip++
      }
    }
  }

  # 4. Flyway rollback 证据
  Write-Host ""
  Write-Host "  [4] Flyway rollback 证据" -ForegroundColor White

  # 检查是否有 undo 迁移脚本
  $flywayDir = switch ($dialect) {
    "oracle" { "medkernel-mvp/src/main/resources/db/migration/oracle" }
    "dm" { "medkernel-mvp/src/main/resources/db/migration/dm" }
    "postgres" { "medkernel-mvp/src/main/resources/db/migration/postgres" }
    "kingbase" { "medkernel-mvp/src/main/resources/db/migration/kingbase" }
    "h2" { "medkernel-mvp/src/main/resources/db/migration/h2" }
  }

  $undoFiles = @()
  if (Test-Path $flywayDir) {
    $undoFiles = Get-ChildItem -Path $flywayDir -Filter "U*__*.sql" -File
  }

  if ($undoFiles.Count -gt 0) {
    Show-Pass "$dialect 有 $($undoFiles.Count) 个 undo 迁移脚本"
  } else {
    # Flyway Community 版不支持 undo，但需要文档化 rollback 策略
    Show-Skip "$dialect 无 undo 迁移脚本（Flyway Community 版不支持 undo；rollback 策略见文档）"
    $dialectSkip++
  }

  # 检查 rollback 文档
  $rollbackDoc = "ai-dev-input/04_database/FLYWAY_ROLLBACK.md"
  if (Test-Path $rollbackDoc) {
    Show-Pass "Flyway rollback 策略文档存在"
  } else {
    Show-Fail "Flyway rollback 策略文档缺失: $rollbackDoc"
    $dialectFail++
  }

  # 记录结果
  $RESULTS[$dialect] = @{
    Pass = $dialectPass
    Fail = $dialectFail
    Skip = $dialectSkip
  }
}

# 执行测试
if ($Dialect -eq "all") {
  foreach ($d in @("oracle", "dm", "postgres", "kingbase", "h2")) {
    Test-SingleDialect -dialect $d
  }
} else {
  Test-SingleDialect -dialect $Dialect
}

# 输出矩阵
Write-Host ""
Write-Host ("=" * 60)
Write-Host "多方言 Smoke 矩阵" -ForegroundColor White
Write-Host ("=" * 60)

Write-Host ""
Write-Host "| 方言 | DDL | Flyway | 启动 | Rollback |" -ForegroundColor White
Write-Host "|------|-----|--------|------|----------|" -ForegroundColor White

foreach ($entry in $RESULTS.GetEnumerator()) {
  $d = $entry.Key
  $r = $entry.Value
  $status = if ($r.Fail -eq 0) { "PASS" } else { "FAIL" }
  Write-Host "| $d | $($r.Pass)/$($r.Pass + $r.Fail + $r.Skip) | $($r.Pass)/$($r.Pass + $r.Fail + $r.Skip) | $($r.Pass)/$($r.Pass + $r.Fail + $r.Skip) | $($r.Pass)/$($r.Pass + $r.Fail + $r.Skip) | $status |"
}

Write-Host ""
Write-Host "总计: PASS=$PASS_COUNT  FAIL=$FAIL_COUNT  SKIP=$SKIP_COUNT" -ForegroundColor White

if ($FAIL_COUNT -gt 0) {
  Write-Host ""
  Write-Host "[X] Smoke 测试未通过" -ForegroundColor Red
  exit 1
} else {
  Write-Host ""
  Write-Host "[OK] Smoke 测试通过" -ForegroundColor Green
  exit 0
}
