# Tag 校验脚本：GA-REL-01 发布与分支保护证据。
#
# 用法：
#   .\scripts\verify-tag.ps1 -Tag v1.0.0
#   .\scripts\verify-tag.ps1 -Tag v1.0.0 -Strict
#
# 参数：
#   -Tag     要校验的 tag 名称（必填）
#   -Strict  严格模式，任何问题均抛出异常
#
# 校验项：
#   1. tag 格式符合 semver（vMAJOR.MINOR.PATCH[-prerelease][+build]）
#   2. tag commit 存在
#   3. tag 基于 main 分支
#   4. develop 包含 tag 的所有 commit（main 是 develop 的祖先）
#   5. VERSIONING.md 存在且包含版本策略
#   6. CHANGELOG.md 存在且包含该版本条目

param(
  [Parameter(Mandatory = $true)]
  [string]$Tag,

  [switch]$Strict
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $ProjectRoot

$PASS_COUNT = 0
$FAIL_COUNT = 0

function Show-Pass($msg) {
  Write-Host "  [PASS] $msg" -ForegroundColor Green
  $script:PASS_COUNT++
}

function Show-Fail($msg) {
  Write-Host "  [FAIL] $msg" -ForegroundColor Red
  $script:FAIL_COUNT++
}

Write-Host ""
Write-Host "MedKernel | Tag 校验 | Tag=$Tag" -ForegroundColor White
Write-Host ("=" * 60)

# 1. Tag 格式校验
Write-Host ""
Write-Host "=== 1. Tag 格式校验 ===" -ForegroundColor Cyan

if ($Tag -match '^v(\d+)\.(\d+)\.(\d+)(?:-[\w\.]+)?(?:\+[\w\.]+)?$') {
  $major = $matches[1]
  $minor = $matches[2]
  $patch = $matches[3]
  $version = "$major.$minor.$patch"
  Show-Pass "Tag '$Tag' 符合 semver 格式（版本 $version）"
} else {
  Show-Fail "Tag '$Tag' 不符合 semver 格式（vMAJOR.MINOR.PATCH[-prerelease][+build]）"
}

# 2. Tag commit 存在
Write-Host ""
Write-Host "=== 2. Tag commit 存在 ===" -ForegroundColor Cyan

$tagCommit = git rev-list -n 1 $Tag 2>$null
if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($tagCommit)) {
  Show-Pass "Tag '$Tag' 指向 commit $tagCommit"
} else {
  Show-Fail "Tag '$Tag' 不存在；请先 git fetch --tags"
  if ($Strict) { throw "Tag 不存在" }
}

# 3. Tag 基于 main
Write-Host ""
Write-Host "=== 3. Tag 基于 main ===" -ForegroundColor Cyan

git fetch origin main 2>$null | Out-Null
$mainCommit = git rev-parse origin/main 2>$null
if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($mainCommit)) {
  # 检查 tag commit 是否可从 main 到达
  $isAncestor = git merge-base --is-ancestor $tagCommit origin/main 2>$null
  if ($LASTEXITCODE -eq 0) {
    Show-Pass "Tag commit 是 origin/main 的祖先（tag 基于 main）"
  } else {
    # 也可能是 main 包含 tag commit（tag commit 在 main 之后）
    $reverseCheck = git merge-base --is-ancestor origin/main $tagCommit 2>$null
    if ($LASTEXITCODE -eq 0) {
      Show-Pass "Tag commit 包含 origin/main（tag 在 main 之上）"
    } else {
      Show-Fail "Tag commit 不在 origin/main 的历史线上；发布 tag 必须基于 main"
    }
  }
} else {
  Show-Fail "无法获取 origin/main"
}

# 4. develop 包含 tag 的所有 commit
Write-Host ""
Write-Host "=== 4. develop 包含 tag 的所有 commit ===" -ForegroundColor Cyan

git fetch origin develop 2>$null | Out-Null
$developCommit = git rev-parse origin/develop 2>$null
if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($developCommit)) {
  $isAncestor = git merge-base --is-ancestor origin/main origin/develop 2>$null
  if ($LASTEXITCODE -eq 0) {
    Show-Pass "origin/main 是 origin/develop 的祖先（develop 包含所有已发布内容）"
  } else {
    Show-Fail "origin/main 不是 origin/develop 的祖先；develop 可能需要合并 main"
  }
} else {
  Show-Fail "无法获取 origin/develop"
}

# 5. VERSIONING.md 存在
Write-Host ""
Write-Host "=== 5. VERSIONING.md 存在 ===" -ForegroundColor Cyan

if (Test-Path "VERSIONING.md") {
  $versioningContent = Get-Content "VERSIONING.md" -Raw -Encoding UTF8
  if ($versioningContent -match "分支约定" -and $versioningContent -match "打 tag 流程") {
    Show-Pass "VERSIONING.md 存在且包含分支约定和 tag 流程"
  } else {
    Show-Fail "VERSIONING.md 存在但缺少分支约定或 tag 流程章节"
  }
} else {
  Show-Fail "VERSIONING.md 不存在"
}

# 6. CHANGELOG.md 存在且包含该版本
Write-Host ""
Write-Host "=== 6. CHANGELOG.md 存在且包含该版本 ===" -ForegroundColor Cyan

if (Test-Path "CHANGELOG.md") {
  $changelogContent = Get-Content "CHANGELOG.md" -Raw -Encoding UTF8
  $versionPattern = [regex]::Escape("[$version]")
  if ($changelogContent -match $versionPattern) {
    Show-Pass "CHANGELOG.md 包含版本 $version 的条目"
  } else {
    Show-Fail "CHANGELOG.md 不包含版本 $version 的条目；打 tag 前必须更新 CHANGELOG"
  }
} else {
  Show-Fail "CHANGELOG.md 不存在；打 tag 前必须创建并填写版本变更"
}

# 总结
Write-Host ""
Write-Host ("=" * 60)
Write-Host "结果: PASS=$PASS_COUNT  FAIL=$FAIL_COUNT" -ForegroundColor White

if ($FAIL_COUNT -gt 0) {
  Write-Host ""
  Write-Host "[X] Tag 校验未通过" -ForegroundColor Red
  if ($Strict) { throw "Tag 校验失败" }
  exit 1
} else {
  Write-Host ""
  Write-Host "[OK] Tag 校验通过，可以发布" -ForegroundColor Green
  exit 0
}
