# GA-REL-01: 发布脚本
#
# 用法：
#   .\scripts\release.ps1 -Version 1.0.0
#   .\scripts\release.ps1 -Version 1.0.0 -DryRun
#
# 参数：
#   -Version  要发布的版本号（必填，不含 v 前缀）
#   -DryRun   干跑模式，只打印将要执行的命令
#
# 发布流程：
#   1. 校验版本号格式（semver）
#   2. 校验当前在 develop 分支且工作区干净
#   3. 更新 CHANGELOG.md
#   4. 创建 develop -> main PR
#   5. 合并 PR 到 main
#   6. 在 main 上打 tag
#   7. 推送 tag 触发 release.yml

param(
  [Parameter(Mandatory = $true)]
  [string]$Version,

  [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $ProjectRoot

$Tag = "v$Version"

function Invoke-Step($desc, $cmd) {
  Write-Host ""
  Write-Host "=== $desc ===" -ForegroundColor Cyan
  if ($DryRun) {
    Write-Host "[DRY RUN] $cmd" -ForegroundColor Yellow
  } else {
    Invoke-Expression $cmd
    if ($LASTEXITCODE -ne 0) {
      throw "Step failed: $desc"
    }
  }
}

# 1. 校验版本号格式
Write-Host ""
Write-Host "MedKernel | 发布脚本 | Version=$Version" -ForegroundColor White
Write-Host ("=" * 60)

if ($Version -match '^(\d+)\.(\d+)\.(\d+)(?:-[\w\.]+)?(?:\+[\w\.]+)?$') {
  Write-Host "[PASS] 版本号 '$Version' 符合 semver 格式" -ForegroundColor Green
} else {
  throw "版本号 '$Version' 不符合 semver 格式（MAJOR.MINOR.PATCH[-prerelease][+build]）"
}

# 2. 校验当前分支和工作区状态
Write-Host ""
Write-Host "=== 2. 校验工作区状态 ===" -ForegroundColor Cyan

$currentBranch = git branch --show-current
if ($currentBranch -ne "develop") {
  Write-Host "[WARN] 当前分支为 '$currentBranch'，建议在 develop 分支执行发布" -ForegroundColor Yellow
}

$status = git status --porcelain
if ($status) {
  throw "工作区有未提交的更改，请先 commit 或 stash"
}
Write-Host "[PASS] 工作区干净" -ForegroundColor Green

# 3. 拉取最新代码
Invoke-Step "3. 拉取最新代码" "git pull --ff-only origin develop"

# 4. 校验 CI 状态
Write-Host ""
Write-Host "=== 4. 校验 CI 状态 ===" -ForegroundColor Cyan
Write-Host "请确认 GitHub Actions CI 全绿后再继续发布"
Write-Host "CI 地址: https://github.com/$(git remote get-url origin | ForEach-Object { $_ -replace '.*github.com[:/]', '' -replace '\.git$', '' })/actions"

# 5. 更新 CHANGELOG.md
Write-Host ""
Write-Host "=== 5. 更新 CHANGELOG.md ===" -ForegroundColor Cyan

$changelogEntry = @"
## [$Version] - $(Get-Date -Format "yyyy-MM-dd")

### Changed
- Release $Version

"@

if (Test-Path "CHANGELOG.md") {
  $content = Get-Content "CHANGELOG.md" -Raw -Encoding UTF8
  if ($content -match [regex]::Escape("[$Version]")) {
    Write-Host "[WARN] CHANGELOG.md 已包含版本 $Version 的条目" -ForegroundColor Yellow
  } else {
    if (-not $DryRun) {
      $newContent = $content -replace "(# Changelog\r?\n\r?\n)", "`$1$changelogEntry"
      Set-Content "CHANGELOG.md" -Value $newContent -Encoding UTF8
      Write-Host "[PASS] CHANGELOG.md 已更新" -ForegroundColor Green
    } else {
      Write-Host "[DRY RUN] Would prepend changelog entry for $Version" -ForegroundColor Yellow
    }
  }
} else {
  if (-not $DryRun) {
    $newChangelog = "# Changelog`n`n$changelogEntry"
    Set-Content "CHANGELOG.md" -Value $newChangelog -Encoding UTF8
    Write-Host "[PASS] CHANGELOG.md 已创建" -ForegroundColor Green
  } else {
    Write-Host "[DRY RUN] Would create CHANGELOG.md" -ForegroundColor Yellow
  }
}

# 6. 提交 CHANGELOG 更改
if (-not $DryRun -and (git status --porcelain)) {
  Invoke-Step "6. 提交 CHANGELOG 更改" "git add CHANGELOG.md && git commit -m `"chore: update CHANGELOG for $Tag`""
}

# 7. 创建 develop -> main PR
Write-Host ""
Write-Host "=== 7. 创建 develop -> main PR ===" -ForegroundColor Cyan
Write-Host "发布流程："
Write-Host "  1. 推送 develop 到远程"
Write-Host "  2. 在 GitHub 上创建 develop -> main PR"
Write-Host "  3. 等待 CI 全绿 + 2 人审核"
Write-Host "  4. 合并 PR"
Write-Host "  5. 在 main 上打 tag: $Tag"
Write-Host "  6. 推送 tag 触发 release.yml"

if (-not $DryRun) {
  Write-Host ""
  $confirm = Read-Host "是否推送 develop 并创建 PR？(y/N)"
  if ($confirm -eq "y" -or $confirm -eq "Y") {
    git push origin develop

    # 使用 gh CLI 创建 PR
    $repo = git remote get-url origin | ForEach-Object { $_ -replace '.*github.com[:/]', '' -replace '\.git$', '' }
    gh pr create --base main --head develop --title "Release $Version" --body "Release $Version to main" --repo $repo
    Write-Host "[PASS] PR 已创建" -ForegroundColor Green
  }
} else {
  Write-Host "[DRY RUN] Would push develop and create PR" -ForegroundColor Yellow
}

# 8. 打 tag 并推送
Write-Host ""
Write-Host "=== 8. 打 tag 并推送 ===" -ForegroundColor Cyan
Write-Host "合并 PR 后，在 main 分支上执行："
Write-Host "  git checkout main"
Write-Host "  git pull --ff-only origin main"
Write-Host "  git tag $Tag"
Write-Host "  git push origin $Tag"
Write-Host ""
Write-Host "tag 推送后，release.yml 将自动："
Write-Host "  - 校验 tag 格式和 base 分支"
Write-Host "  - 生成 release manifest（版本号、git hash、构建时间、组件 SHA256）"
Write-Host "  - 创建 GitHub Release"

# 9. 验证发布
Write-Host ""
Write-Host "=== 9. 验证发布 ===" -ForegroundColor Cyan
Write-Host "发布完成后，运行以下命令验证："
Write-Host "  .\scripts\verify-tag.ps1 -Tag $Tag -Strict"

Write-Host ""
Write-Host ("=" * 60)
Write-Host "发布脚本执行完成" -ForegroundColor Green
