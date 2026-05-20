# develop 健康自检 — AI 接手任务前必跑
#
# 用法：
#   .\scripts\check-develop-health.ps1
#
# 行为：
#   1) 跑 `mvn -q compile`（不跑测试，~30s）
#   2) 读 ai-dev-input/00_DEVELOP_HEALTH.md 中的状态字段
#   3) 若状态文件标 GREEN 但实际 compile FAIL → 拒绝继续（exit 2）
#   4) 若状态文件标 RED → 提示只允许领 FIX-DEV-* 任务
#   5) 输出建议下一步
#
# 退出码：
#   0  GREEN，可正常领新任务
#   1  YELLOW，可领任务但 review 时附测试修复
#   2  RED，只允许领 FIX-DEV-*
#   10 健康哨兵文件与实测不一致（必须先更新哨兵）

$ErrorActionPreference = "Continue"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $ProjectRoot

function Write-Section($title) {
  Write-Host ""
  Write-Host "=== $title ===" -ForegroundColor Cyan
}

Write-Section "1. 读 ai-dev-input/00_DEVELOP_HEALTH.md"

$healthFile = Join-Path $ProjectRoot "ai-dev-input/00_DEVELOP_HEALTH.md"
if (-not (Test-Path $healthFile)) {
  Write-Host "  [FAIL] 健康哨兵文件不存在：$healthFile" -ForegroundColor Red
  Write-Host "         先建立哨兵文件再继续。"
  exit 10
}

$healthRaw = Get-Content -LiteralPath $healthFile -Raw -Encoding UTF8
$reportedStatus = "UNKNOWN"
if ($healthRaw -match '🟢\s*\*\*GREEN\*\*' -or $healthRaw -match '状态\s*\|\s*🟢') {
  $reportedStatus = "GREEN"
} elseif ($healthRaw -match '🟡\s*\*\*YELLOW\*\*' -or $healthRaw -match '状态\s*\|\s*🟡') {
  $reportedStatus = "YELLOW"
} elseif ($healthRaw -match '🔴\s*\*\*RED\*\*' -or $healthRaw -match '状态\s*\|\s*🔴') {
  $reportedStatus = "RED"
}
Write-Host "  [INFO] 哨兵文件声明状态：$reportedStatus"

Write-Section "2. 跑 mvn -q compile（实测）"

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
  Write-Host "  [WARN] mvn 未安装，跳过实测。请人工确认。" -ForegroundColor Yellow
  Write-Host "         按哨兵声明状态 = $reportedStatus 处理。"
  switch ($reportedStatus) {
    "GREEN"  { exit 0 }
    "YELLOW" { exit 1 }
    "RED"    { exit 2 }
    default  { exit 10 }
  }
}

$mvnLog = & mvn -q -f medkernel-mvp/pom.xml compile 2>&1
$mvnExit = $LASTEXITCODE
$actualStatus = if ($mvnExit -eq 0) { "GREEN_or_YELLOW" } else { "RED" }

if ($mvnExit -eq 0) {
  Write-Host "  [PASS] mvn compile SUCCESS" -ForegroundColor Green
} else {
  $errLines = ($mvnLog | Select-String -Pattern '\[ERROR\]').Count
  Write-Host "  [FAIL] mvn compile FAILED — ~$errLines 个 ERROR" -ForegroundColor Red
}

Write-Section "3. 哨兵 vs 实测 一致性"

if ($actualStatus -eq "RED" -and $reportedStatus -ne "RED") {
  Write-Host "  [BLOCK] 哨兵声明 $reportedStatus 但实测 RED！" -ForegroundColor Red
  Write-Host "          请按 ai-dev-input/00_DEVELOP_HEALTH.md 中"状态转换协议"先更新哨兵，再继续。" -ForegroundColor Red
  exit 10
}

if ($actualStatus -eq "GREEN_or_YELLOW" -and $reportedStatus -eq "RED") {
  Write-Host "  [INFO] 哨兵仍标 RED 但本机 compile 通过 — 可能 RED 已被他人修复但哨兵未同步。" -ForegroundColor Yellow
  Write-Host "         如确认 develop 已 GREEN，请更新哨兵后再开新任务。"
  exit 10
}

Write-Section "4. 准入决策"

switch ($reportedStatus) {
  "GREEN" {
    Write-Host "  [OK] 🟢 GREEN — 可按 docs/engineering/02_任务台账.md 正常领新任务" -ForegroundColor Green
    exit 0
  }
  "YELLOW" {
    Write-Host "  [WARN] 🟡 YELLOW — 可领新任务，review 时必须附测试修复证明" -ForegroundColor Yellow
    exit 1
  }
  "RED" {
    Write-Host "  [BLOCK] 🔴 RED — 仅允许领 FIX-DEV-* 修复任务" -ForegroundColor Red
    Write-Host "         可领清单见 ai-dev-input/00_DEVELOP_HEALTH.md '修复白名单'" -ForegroundColor Red
    exit 2
  }
  default {
    Write-Host "  [FAIL] 无法识别哨兵状态，请人工检查" -ForegroundColor Red
    exit 10
  }
}
