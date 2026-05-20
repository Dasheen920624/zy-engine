# 提交前自检脚本：AI 完成 PR 后必须跑通此脚本才能 commit + push。
#
# 用法：
#   .\scripts\verify-pr.ps1 -TaskId PR-V2-01
#
# 参数：
#   -TaskId         任务编号（必填）
#   -SkipFrontend   跳过前端检查（仅后端 PR）
#   -SkipBackend    跳过后端检查（仅前端 PR）
#
# 9 项自动检查：
#   1. 工作树或 PR 差异有改动（不是空 PR）
#   2. ADR 不变量未被违反（grep 禁用模式）
#   3. 后端 build + test（如适用）
#   4. 前端 lint + test + build（如适用）
#   5. UTF-8 / 中文乱码检查
#   6. 独占文件 / 共享文件冲突（check-ai-collaboration.ps1）
#   7. 禁用命名检查（zy-engine / ZyEngine* 等）
#   8. 引用断链检查（docs/_archive/ 等已删除路径）
#   9. DoD 自动抽取与对比（从 V2 实施手册）

param(
  [Parameter(Mandatory = $true)]
  [string]$TaskId,

  [switch]$SkipFrontend,
  [switch]$SkipBackend
)

# 锚定到项目根（脚本所在目录的父目录），让后续所有相对路径检查不受调用方 cwd 影响
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $ProjectRoot

$ErrorActionPreference = "Continue"
$PASS_COUNT = 0
$FAIL_COUNT = 0
$WARN_COUNT = 0

function Show-Pass($msg) {
  Write-Host "  [PASS] $msg" -ForegroundColor Green
  $script:PASS_COUNT++
}

function Show-Fail($msg) {
  Write-Host "  [FAIL] $msg" -ForegroundColor Red
  $script:FAIL_COUNT++
}

function Show-Warn($msg) {
  Write-Host "  [WARN] $msg" -ForegroundColor Yellow
  $script:WARN_COUNT++
}

function Show-Section($title) {
  Write-Host ""
  Write-Host "=== $title ===" -ForegroundColor Cyan
}

function Test-GitRef([string]$Ref) {
  if ([string]::IsNullOrWhiteSpace($Ref)) {
    return $false
  }
  git rev-parse --verify $Ref 2>$null | Out-Null
  return $LASTEXITCODE -eq 0
}

function Get-CurrentGitBranch {
  if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_HEAD_REF)) {
    return $env:GITHUB_HEAD_REF
  }
  if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_REF_NAME)) {
    return $env:GITHUB_REF_NAME
  }

  $branch = git branch --show-current 2>$null
  if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($branch)) {
    return $branch.Trim()
  }
  return ""
}

function Get-GitBaseRef {
  if (-not [string]::IsNullOrWhiteSpace($env:MEDKERNEL_DIFF_BASE) -and (Test-GitRef $env:MEDKERNEL_DIFF_BASE)) {
    return $env:MEDKERNEL_DIFF_BASE
  }

  if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_BASE_REF)) {
    $candidate = "origin/$($env:GITHUB_BASE_REF)"
    if (Test-GitRef $candidate) {
      return $candidate
    }
    if (Test-GitRef $env:GITHUB_BASE_REF) {
      return $env:GITHUB_BASE_REF
    }
    return $candidate
  }

  git rev-parse --verify origin/develop 2>$null | Out-Null
  if ($LASTEXITCODE -eq 0) {
    return "origin/develop"
  }

  $upstream = git rev-parse --abbrev-ref --symbolic-full-name "@{u}" 2>$null
  if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($upstream)) {
    return $upstream.Trim()
  }

  return "HEAD"
}

function Get-TaskAliases([string]$TaskId) {
  $aliases = New-Object System.Collections.Generic.List[string]
  $aliases.Add($TaskId)
  if ($TaskId -match '^PR-V2-(\d{2})$') {
    $aliases.Add("PR-$($matches[1])")
  } elseif ($TaskId -match '^PR-(\d{2})$') {
    $aliases.Add("PR-V2-$($matches[1])")
  }
  return $aliases | Select-Object -Unique
}

function Get-PlaybookTaskSection([string]$Content, [string]$TaskId) {
  $aliases = Get-TaskAliases $TaskId
  $sections = [regex]::Matches($Content, '(?ms)^##\s+\d+\.\s+.*?(?=^##\s+\d+\.|\z)')
  foreach ($section in $sections) {
    $heading = ($section.Value -split "`r?`n", 2)[0]
    foreach ($alias in $aliases) {
      $aliasPattern = [regex]::Escape($alias)
      if ($heading -match $aliasPattern -or $section.Value -match "\*\*任务编号[:：]\*\*\s*$aliasPattern") {
        return $section.Value
      }
    }
  }
  foreach ($section in $sections) {
    foreach ($alias in $aliases) {
      if ($section.Value -match [regex]::Escape($alias)) {
        return $section.Value
      }
    }
  }
  return $null
}

function Test-BranchPolicy {
  Show-Section "0. 分支策略"

  $eventName = $env:GITHUB_EVENT_NAME
  $baseBranch = $env:GITHUB_BASE_REF
  $headBranch = Get-CurrentGitBranch

  if ($eventName -eq "pull_request") {
    if ($baseBranch -eq "main") {
      if ($headBranch -eq "develop") {
        Show-Pass "发布 PR 路径正确：develop -> main"
      } else {
        Show-Fail "AI 变更不允许直接 PR 到 main；必须先合入 develop，再由用户发起 develop -> main 发布 PR（当前 $headBranch -> main）"
      }
      return
    }

    if ($baseBranch -eq "develop") {
      if ($headBranch -eq "main") {
        Show-Fail "不允许从 main 合回 develop；AI 任务分支必须基于 develop"
      } else {
        Show-Pass "AI PR 目标分支正确：$headBranch -> develop"
      }
      return
    }

    Show-Fail "PR 目标分支必须是 develop；只有发布 PR 允许 develop -> main（当前目标：$baseBranch）"
    return
  }

  if ([string]::IsNullOrWhiteSpace($eventName)) {
    if ($headBranch -eq "main") {
      Show-Fail "当前在 main，AI 禁止在 main 开发、提交或推送；请切到 develop 或基于 origin/develop 的 ai/<TASK-ID>/<slug> 分支"
    } elseif ([string]::IsNullOrWhiteSpace($headBranch)) {
      Show-Warn "当前为 detached HEAD，无法识别分支；请确认目标基线是 origin/develop"
    } else {
      Show-Pass "当前分支 $headBranch；AI 变更必须以 develop 为合入目标"
    }
    return
  }

  Show-Pass "CI 事件 $eventName；main 只接受 develop -> main 发布路径"
}

$BaseRef = Get-GitBaseRef

Write-Host ""
Write-Host "MedKernel | 提交前 DoD 自检 | Task=$TaskId" -ForegroundColor White
Write-Host "Diff base: $BaseRef" -ForegroundColor Gray
Write-Host ("=" * 60)

Test-BranchPolicy

if ($BaseRef -ne "HEAD" -and -not (Test-GitRef $BaseRef)) {
  Show-Fail "Diff base 不存在: $BaseRef；CI 需先 fetch 目标分支，PR 场景应有 origin/$env:GITHUB_BASE_REF"
}

# ============================================================
# 0.5 develop 健康哨兵预检（不论 SkipBackend，强制跑 mvn compile）
# ============================================================
Show-Section "0.5 develop 健康哨兵 — 强制 mvn compile"

$healthFile = Join-Path $ProjectRoot "ai-dev-input/00_DEVELOP_HEALTH.md"
if (Test-Path $healthFile) {
  $healthRaw = Get-Content -LiteralPath $healthFile -Raw -Encoding UTF8
  $reportedHealth = "UNKNOWN"
  if ($healthRaw -match '🔴') { $reportedHealth = "RED" }
  elseif ($healthRaw -match '🟡') { $reportedHealth = "YELLOW" }
  elseif ($healthRaw -match '🟢') { $reportedHealth = "GREEN" }
  Write-Host "  哨兵声明：$reportedHealth"
} else {
  Show-Warn "ai-dev-input/00_DEVELOP_HEALTH.md 缺失，强烈建议补建"
  $reportedHealth = "UNKNOWN"
}

if (Get-Command mvn -ErrorAction SilentlyContinue) {
  Write-Host "  跑 mvn -q -f medkernel-mvp/pom.xml compile（无视 SkipBackend）..."
  $compileOutput = & mvn -q -f medkernel-mvp/pom.xml compile 2>&1
  if ($LASTEXITCODE -eq 0) {
    Show-Pass "mvn compile PASS（实测 GREEN-or-YELLOW）"
    if ($reportedHealth -eq "RED") {
      Show-Warn "哨兵仍标 RED 但本地 compile 通过 — 若已确认所有 FIX-DEV-* 完成，请同步更新 ai-dev-input/00_DEVELOP_HEALTH.md 状态"
    }
  } else {
    Show-Fail "mvn compile FAIL — develop 处于 RED 状态"
    $compileOutput | Select-String -Pattern '\[ERROR\]' | Select-Object -First 8 | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
    if ($reportedHealth -ne "RED") {
      Show-Fail "哨兵未声明 RED 但实测编译失败 — 请立即按 ai-dev-input/00_DEVELOP_HEALTH.md '状态转换协议' 把哨兵改为 RED 并 commit"
    }
    Write-Host "  → 本次 PR 不允许通过，先解决编译错误（领 FIX-DEV-* 任务）" -ForegroundColor Red
  }
} else {
  Show-Warn "mvn 未安装，跳过编译预检 — CI 会再跑一次"
}

# ============================================================
# 1. 工作树或 PR 差异有改动
# ============================================================
Show-Section "1. 工作树或 PR 差异有改动"

$status = git status --porcelain
$diffNames = git diff --name-only $BaseRef -- 2>$null
if (-not $diffNames) {
  $diffNames = git diff --cached --name-only $BaseRef -- 2>$null
}

if (-not [string]::IsNullOrWhiteSpace($status)) {
  $changedLines = ($status -split "`n").Count
  Show-Pass "$changedLines 个文件改动"
} elseif (-not [string]::IsNullOrWhiteSpace($diffNames)) {
  $changedLines = ($diffNames -split "`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }).Count
  Show-Pass "$changedLines 个文件相对 $BaseRef 有差异（CI/已提交场景）"
} elseif ($env:GITHUB_EVENT_NAME -eq "push" -and -not [string]::IsNullOrWhiteSpace($env:MEDKERNEL_DIFF_BASE)) {
  Show-Pass "push 事件无文件差异，视为纯历史同步提交（如 develop 吸收 main），允许通过"
} else {
  Show-Fail "工作树与 $BaseRef 均无差异，没什么要提交或合并的"
}

# ============================================================
# 2. ADR 不变量检查（禁用模式 grep）
# ============================================================
Show-Section "2. ADR 不变量未被违反"

# 仅检查本次 diff 中新增的内容（不检查全仓库）
# 排除：所有 .md（按 forbidden-patterns.md:6 文档类豁免）、ESLint 规则文件（自身需含禁用模式）、本脚本（自身豁免）
$ExcludePathspecs = @(
  ":(exclude)*.md",
  ":(exclude)CHANGELOG.md",
  ":(exclude)frontend/eslint-rules/forbid-deprecated-naming.js",
  ":(exclude)scripts/verify-pr.ps1"
)
$diff = git diff --cached $BaseRef -- $ExcludePathspecs 2>$null
if (-not $diff) {
  $diff = git diff $BaseRef -- $ExcludePathspecs 2>$null
}

if ($diff) {
  # 2.1 ADR-0003: 禁止硬编码颜色（仅检查 +号新增行）
  $addedLines = $diff -split "`n" | Where-Object { $_ -match '^\+[^+]' }
  $colorViolations = New-Object System.Collections.Generic.List[string]
  $currentDiffFile = ""
  foreach ($line in ($diff -split "`n")) {
    if ($line -match '^\+\+\+ b/(.+)$') {
      $currentDiffFile = $Matches[1]
      continue
    }
    if ($line -notmatch '^\+[^+]') {
      continue
    }
    if ($currentDiffFile -match 'frontend/src/styles/tokens\.(css|ts)$') {
      continue
    }
    # 文档类文件（markdown / ADR / 审计报告）允许引用硬编码颜色作为示例代码块。
    if ($currentDiffFile -match '\.(md|mdx)$') {
      continue
    }
    # eslint 规则脚本本身就要列举禁用颜色字面量，跳过。
    if ($currentDiffFile -match 'eslint-rules/.+\.(js|cjs|mjs|ts)$') {
      continue
    }
    if (
      $line -match '#[0-9a-fA-F]{3,8}\b' -and
      $line -notmatch '^\+\s*//' -and
      $line -notmatch '^\+\s*\*'
    ) {
      $colorViolations.Add("${currentDiffFile}: $line")
    }
  }
  if ($colorViolations.Count -gt 0) {
    Show-Fail "ADR-0003 违反：本次新增含硬编码颜色 ($($colorViolations.Count) 处)，必须用 var(--mk-*) / var(--mk-*)"
    $colorViolations | Select-Object -First 3 | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
  } else {
    Show-Pass "ADR-0003 颜色硬编码：无违反"
  }

  # 2.2 项目重命名：禁用 ZyEngine* / zy-engine
  $namingViolations = $addedLines | Where-Object {
    ($_ -match '\bZyEngine\w*\b' -or $_ -match '\bzyengine\w*\b' -or $_ -match '\bzy-engine\b') -and
    $_ -notmatch '^\+\+\+'
  }
  if ($namingViolations) {
    Show-Fail "禁用命名违反：本次新增含 ZyEngine* / zy-engine ($($namingViolations.Count) 处)"
    $namingViolations | Select-Object -First 3 | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
  } else {
    Show-Pass "禁用命名：无违反"
  }

  # 2.3 引用已删除路径
  $deletedPathViolations = $addedLines | Where-Object {
    ($_ -match 'docs/_archive/' -or $_ -match 'frontend-prototype/' -or $_ -match 'zy-engine-mvp/') -and
    $_ -notmatch '^\+\+\+' -and
    $_ -notmatch '^\+\s*//' -and
    $_ -notmatch '历史'
  }
  if ($deletedPathViolations) {
    Show-Fail "引用已删除路径 ($($deletedPathViolations.Count) 处)：docs/_archive/、frontend-prototype/、zy-engine-mvp/ 已物理删除"
    $deletedPathViolations | Select-Object -First 3 | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
  } else {
    Show-Pass "无引用已删除路径"
  }
} else {
  Show-Warn "无 diff 可分析（可能首次提交）"
}

# ============================================================
# 3. 后端 build + test
# ============================================================
if (-not $SkipBackend) {
  Show-Section "3. 后端 build + test"

  $hasBackendChange = git diff $BaseRef --name-only -- "medkernel-mvp/" 2>$null
  if ($hasBackendChange) {
    Push-Location "medkernel-mvp"
    try {
      Write-Host "  跑 run-tests.ps1..."
      $testOutput = & .\scripts\run-tests.ps1 2>&1
      if ($LASTEXITCODE -eq 0) {
        Show-Pass "后端测试 PASS"
      } else {
        Show-Fail "后端测试 FAIL（退出码 $LASTEXITCODE）"
        $testOutput | Select-Object -Last 10 | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
      }

      Write-Host "  跑 build.ps1..."
      $buildOutput = & .\scripts\build.ps1 2>&1
      if ($LASTEXITCODE -eq 0) {
        Show-Pass "后端构建 PASS"
      } else {
        Show-Fail "后端构建 FAIL"
      }
    } catch {
      Show-Fail "后端构建异常: $_"
    } finally {
      Pop-Location
    }
  } else {
    Show-Pass "本次无后端改动，跳过"
  }
}

# ============================================================
# 4. 前端 lint + test + build
# ============================================================
if (-not $SkipFrontend) {
  Show-Section "4. 前端 lint + test + build"

  $hasFrontendChange = git diff $BaseRef --name-only -- "frontend/" 2>$null
  if ($hasFrontendChange) {
    Push-Location "frontend"
    try {
      Write-Host "  跑 npm run lint..."
      npm run lint 2>&1 | Out-Null
      if ($LASTEXITCODE -eq 0) {
        Show-Pass "前端 lint PASS（含 medkernel 自定义规则）"
      } else {
        Show-Fail "前端 lint FAIL"
      }

      Write-Host "  跑 npm test..."
      npm test -- --run 2>&1 | Out-Null
      if ($LASTEXITCODE -eq 0) {
        Show-Pass "前端单元测试 PASS"
      } else {
        Show-Fail "前端测试 FAIL"
      }

      Write-Host "  跑 npm run build..."
      npm run build 2>&1 | Out-Null
      if ($LASTEXITCODE -eq 0) {
        Show-Pass "前端构建 PASS"
      } else {
        Show-Fail "前端构建 FAIL"
      }
    } catch {
      Show-Fail "前端检查异常: $_"
    } finally {
      Pop-Location
    }
  } else {
    Show-Pass "本次无前端改动，跳过"
  }
}

# ============================================================
# 5. UTF-8 / 中文乱码检查
# ============================================================
Show-Section "5. UTF-8 / 中文乱码检查"

if (Test-Path "medkernel-mvp/scripts/verify-encoding.cmd") {
  $encOutput = & "medkernel-mvp/scripts/verify-encoding.cmd" 2>&1
  if ($LASTEXITCODE -eq 0) {
    Show-Pass "UTF-8 编码检查 PASS"
  } else {
    Show-Fail "UTF-8 编码检查 FAIL，请检查中文文件"
  }
} else {
  Show-Warn "verify-encoding.cmd 不存在，跳过"
}

# ============================================================
# 6. 独占文件 / 共享文件冲突
# ============================================================
Show-Section "6. AI 协作冲突检查"

if (Test-Path "medkernel-mvp/scripts/check-ai-collaboration.ps1") {
  $aiOutput = & "medkernel-mvp/scripts/check-ai-collaboration.ps1" 2>&1
  if ($LASTEXITCODE -eq 0) {
    Show-Pass "AI 协作检查 PASS"
  } else {
    Show-Fail "AI 协作冲突检测 FAIL"
    $aiOutput | Select-Object -Last 5 | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
  }
} else {
  Show-Warn "check-ai-collaboration.ps1 不存在"
}

# ============================================================
# 7. DoD 抽取与对比
# ============================================================
Show-Section "7. DoD 检查表自动抽取"

$playbook = "docs/05_AI实施手册.md"
if (Test-Path $playbook) {
  $pb = Get-Content $playbook -Raw -Encoding UTF8
  $section = Get-PlaybookTaskSection $pb $TaskId
  if ($section) {
    if ($section -match '(?s)DoD 检查表\s*```markdown\s*([^`]+)```') {
      $dodList = $matches[1].Trim()
      Show-Pass "DoD 检查表已抽取（请人工对照逐项打钩）："
      $dodList -split "`n" | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
    } else {
      Show-Warn "未在 PR 卡片找到 DoD 检查表"
    }
  } else {
    Show-Warn "在 V2 实施手册未找到 $TaskId 卡片"
  }
} else {
  Show-Warn "V2 实施手册不存在: $playbook"
}

# ============================================================
# 8. feature acceptance 记录（高风险 PR 必须）
# ============================================================
Show-Section "8. feature acceptance 记录"

$faDir = "ai-dev-input/13_feature_acceptance"
$expectedFa = Get-ChildItem -Path $faDir -Filter "FA-$TaskId-*.md" -Recurse -ErrorAction SilentlyContinue
if ($expectedFa) {
  Show-Pass "feature acceptance 记录已创建: $($expectedFa.Name)"
} else {
  Show-Warn "未找到 FA-$TaskId-*.md。客户可见 / 高风险 PR 必须创建（V2 §16.3）"
}

# ============================================================
# 9. 总结
# ============================================================
Write-Host ""
Write-Host ("=" * 60)
Write-Host "结果: PASS=$PASS_COUNT  FAIL=$FAIL_COUNT  WARN=$WARN_COUNT" -ForegroundColor White

if ($FAIL_COUNT -gt 0) {
  Write-Host ""
  Write-Host "[X] DoD 未通过，禁止 commit + push" -ForegroundColor Red
  Write-Host "    修复 FAIL 项后再次跑此脚本" -ForegroundColor Red
  exit 1
} else {
  Write-Host ""
  Write-Host "[OK] DoD 自检通过，可以 commit + push" -ForegroundColor Green
  if ($WARN_COUNT -gt 0) {
    Write-Host "    （$WARN_COUNT 项 WARN 建议关注，但不阻断）" -ForegroundColor Yellow
  }
  Write-Host ""
  Write-Host "下一步：" -ForegroundColor White
  Write-Host "  1. git add <相关文件>"
  Write-Host "  2. git commit -m '<中文短句，PR-V2-XX 编号开头>'"
  Write-Host "  3. git push origin HEAD:develop（或 push 任务分支后开 PR -> develop）"
  Write-Host "  4. 创建 ai-dev-input/11_ai_reviews/pending/<review_id>.md"
  Write-Host "  5. review APPROVED 后归档 claim/review"
  exit 0
}
