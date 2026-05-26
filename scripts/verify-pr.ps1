# MedKernel 提交前自检脚本
#
# 用法：
#   ./scripts/verify-pr.ps1 -TaskId GA-ENG-BASE-10
#
# 当前项目按远程 main 单主干运行：所有任务基于 origin/main 开短分支，完成后 PR 合并到远程 main。

param(
  [Parameter(Mandatory = $true)]
  [string]$TaskId,

  [switch]$SkipFrontend,
  [switch]$SkipBackend
)

$ErrorActionPreference = "Continue"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $ProjectRoot

$PassCount = 0
$FailCount = 0
$WarnCount = 0

function Pass($Message) {
  Write-Host "  [PASS] $Message" -ForegroundColor Green
  $script:PassCount++
}

function Fail($Message) {
  Write-Host "  [FAIL] $Message" -ForegroundColor Red
  $script:FailCount++
}

function Warn($Message) {
  Write-Host "  [WARN] $Message" -ForegroundColor Yellow
  $script:WarnCount++
}

function Section($Title) {
  Write-Host ""
  Write-Host "=== $Title ===" -ForegroundColor Cyan
}

function GitRefExists($Ref) {
  git rev-parse --verify $Ref 2>$null | Out-Null
  return $LASTEXITCODE -eq 0
}

function CurrentBranch {
  if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_HEAD_REF)) { return $env:GITHUB_HEAD_REF }
  if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_REF_NAME)) { return $env:GITHUB_REF_NAME }
  $branch = git branch --show-current 2>$null
  if ($LASTEXITCODE -eq 0) { return $branch.Trim() }
  return ""
}

function DiffFiles {
  if ($env:GITHUB_EVENT_NAME -eq "pull_request" -and -not [string]::IsNullOrWhiteSpace($env:GITHUB_BASE_REF)) {
    $baseRef = "origin/$($env:GITHUB_BASE_REF)"
  } elseif (GitRefExists "origin/main") {
    $baseRef = "origin/main"
  } else {
    $baseRef = "HEAD~1"
  }

  if (GitRefExists $baseRef) {
    git diff --name-only "$baseRef...HEAD" 2>$null
  } else {
    git diff --name-only HEAD 2>$null
  }
}

Write-Host ""
Write-Host "MedKernel | 提交前自检 | Task=$TaskId" -ForegroundColor White
Write-Host ("=" * 60)

Section "1. 分支与远程 main 规则"
$branch = CurrentBranch
if ($env:GITHUB_EVENT_NAME -eq "pull_request") {
  if ($env:GITHUB_BASE_REF -eq "main") {
    Pass "PR 目标分支为 main"
  } else {
    Fail "PR 目标分支必须是 main，当前为 $($env:GITHUB_BASE_REF)"
  }
} elseif ($branch -eq "main") {
  Warn "当前在本地 main；请只用于同步，不要在 main 上直接开发或提交"
} elseif ([string]::IsNullOrWhiteSpace($branch)) {
  Warn "当前为 detached HEAD，请确认变更最终通过 PR 合并到远程 main"
} else {
  Pass "当前开发分支为 $branch"
}

if (GitRefExists "origin/main") {
  Pass "远程 main 基线存在"
} else {
  Fail "缺少 origin/main，请先 git fetch origin"
}

Section "2. 变更范围"
$changed = @(DiffFiles | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
if ($changed.Count -gt 0) {
  Pass "检测到 $($changed.Count) 个变更文件"
} else {
  Fail "未检测到相对 main 的变更"
}

Section "3. 中文文档与旧包袱"
$currentDocs = @()
if (Test-Path "docs") {
  $currentDocs += Get-ChildItem docs -Recurse -File -Include *.md | Where-Object { $_.FullName -notmatch '[\\/](archive)[\\/]' }
}
$currentDocs += Get-ChildItem . -File -Include README.md,CHANGELOG.md,VERSIONING.md,AGENTS.md -ErrorAction SilentlyContinue
$currentDocs += Get-ChildItem openspec -Recurse -File -Include *.md -ErrorAction SilentlyContinue
$currentDocs += Get-ChildItem .codex -Recurse -File -Include *.md -ErrorAction SilentlyContinue

$englishHeavy = @()
foreach ($doc in $currentDocs) {
  $text = Get-Content -LiteralPath $doc.FullName -Raw -Encoding UTF8
  $han = ([regex]::Matches($text, '\p{IsCJKUnifiedIdeographs}')).Count
  $latin = ([regex]::Matches($text, '[A-Za-z]')).Count
  if ($han -eq 0 -and $latin -gt 120) {
    $englishHeavy += $doc.FullName.Substring($ProjectRoot.Length + 1)
  }
}
if ($englishHeavy.Count -eq 0) {
  Pass "当前文档没有纯英文主体"
} else {
  Fail "存在英文主体文档：$($englishHeavy -join ', ')"
}

$legacyPaths = @("docs/archive", "ai-dev-input", "docs/_archive", "frontend-prototype")
foreach ($path in $legacyPaths) {
  if (Test-Path $path) {
    Fail "发现旧体系目录：$path"
  } else {
    Pass "未发现旧体系目录：$path"
  }
}

$legacyRefs = rg -n "\bdevelop\b|PR-V2|PR-V3|PR-FINAL|docs/archive|archive/v0\.3|ai-dev-input|zy-engine|ZyEngine|zyengine" README.md docs deploy openspec .github/pull_request_template.md AGENTS.md VERSIONING.md CHANGELOG.md 2>$null
if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($legacyRefs)) {
  Warn "仍存在旧词或历史词引用，请确认是否为禁用规则或必要说明："
  Write-Host $legacyRefs
} else {
  Pass "未发现旧分支、旧任务或旧归档引用"
}

Section "4. 后端验证"
if ($SkipBackend) {
  Warn "已跳过后端验证"
} elseif (Test-Path "medkernel-backend/pom.xml") {
  Push-Location medkernel-backend
  mvn -B test
  if ($LASTEXITCODE -eq 0) { Pass "mvn test 通过" } else { Fail "mvn test 失败" }
  Pop-Location
} else {
  Warn "未发现后端工程"
}

Section "5. 前端验证"
if ($SkipFrontend) {
  Warn "已跳过前端验证"
} elseif (Test-Path "frontend/package.json") {
  Push-Location frontend
  npm run verify
  if ($LASTEXITCODE -eq 0) { Pass "npm run verify 通过" } else { Fail "npm run verify 失败" }
  npm run build
  if ($LASTEXITCODE -eq 0) { Pass "npm run build 通过" } else { Fail "npm run build 失败" }
  Pop-Location
} else {
  Warn "未发现前端工程"
}

Section "6. 差异格式"
git diff --check
if ($LASTEXITCODE -eq 0) { Pass "git diff --check 通过" } else { Fail "git diff --check 失败" }

Write-Host ""
Write-Host ("=" * 60)
Write-Host "自检完成：PASS=$PassCount WARN=$WarnCount FAIL=$FailCount" -ForegroundColor White

if ($FailCount -gt 0) {
  exit 1
}
exit 0
