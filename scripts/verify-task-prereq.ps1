# 接手前自检脚本：AI 领任务前必须跑通此脚本才能创建 active claim。
#
# 用法：
#   .\scripts\verify-task-prereq.ps1 -TaskId PR-V2-01 -Level senior
#
# 参数：
#   -TaskId       任务编号（必填，如 PR-V2-01）
#   -Level        AI 能力等级（junior / middle / senior）
#   -SkipGitPull  跳过 git pull（仅离线开发用）
#
# 检查项：
#   1. git 工作树干净
#   2. 当前分支远端基线已同步
#   3. 任务编号在台账存在
#   4. 任务等级与你的等级匹配
#   5. 依赖任务全部 DONE
#   6. 文档体系（docs/）完整
#   7. 是否已有别人在领同一任务（active claim）
#
# 任何一项 FAIL 都不允许创建 active claim。

param(
  [Parameter(Mandatory = $true)]
  [string]$TaskId,

  [Parameter(Mandatory = $false)]
  [ValidateSet("junior", "middle", "senior")]
  [string]$Level = "senior",

  [Parameter(Mandatory = $false)]
  [switch]$SkipGitPull
)

$ErrorActionPreference = "Stop"
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

function Get-GitBaseRef {
  if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_BASE_REF)) {
    $candidate = "origin/$($env:GITHUB_BASE_REF)"
    if (Test-GitRef $candidate) {
      return $candidate
    }
    if (Test-GitRef $env:GITHUB_BASE_REF) {
      return $env:GITHUB_BASE_REF
    }
  }

  $upstream = git rev-parse --abbrev-ref --symbolic-full-name "@{u}" 2>$null
  if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($upstream)) {
    return $upstream.Trim()
  }

  git rev-parse --verify origin/develop 2>$null | Out-Null
  if ($LASTEXITCODE -eq 0) {
    return "origin/develop"
  }

  git rev-parse --verify origin/main 2>$null | Out-Null
  if ($LASTEXITCODE -eq 0) {
    return "origin/main"
  }

  return $null
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

Write-Host ""
Write-Host "MedKernel | 接手前自检 | Task=$TaskId | Level=$Level" -ForegroundColor White
Write-Host ("=" * 60)

# ============================================================
# 1. git 工作树干净
# ============================================================
Show-Section "1. git 工作树干净"

$status = git status --porcelain
if ([string]::IsNullOrWhiteSpace($status)) {
  Show-Pass "工作树干净"
} else {
  Show-Fail "工作树有未提交改动，先 commit / stash 再领任务："
  Write-Host $status -ForegroundColor Gray
}

# ============================================================
# 2. 当前分支远端基线已同步
# ============================================================
Show-Section "2. 当前分支远端基线已同步"

if (-not $SkipGitPull) {
  try {
    git fetch origin 2>&1 | Out-Null
    $baseRef = Get-GitBaseRef
    if ([string]::IsNullOrWhiteSpace($baseRef)) {
      Show-Warn "未找到当前分支 upstream，也未找到 origin/develop 或 origin/main；跳过远端同步检查"
      $behind = 0
      $ahead = 0
    } else {
      $behind = git rev-list --count HEAD..$baseRef
      $ahead = git rev-list --count $baseRef..HEAD
    }
    if ($behind -eq 0) {
      Show-Pass "本地 HEAD 不落后 $baseRef"
    } else {
      Show-Fail "本地落后 $baseRef $behind 个 commit，先 git pull --ff-only"
    }
    if ($ahead -gt 0) {
      Show-Warn "本地超前 $baseRef $ahead 个 commit（可能是上次任务未推送）"
    }
  } catch {
    Show-Warn "无法 fetch origin（网络/权限问题），跳过远端检查"
  }
} else {
  Show-Warn "用户指定 -SkipGitPull，跳过远端同步检查"
}

# ============================================================
# 3. 任务编号在台账存在
# ============================================================
Show-Section "3. 任务编号在台账存在"

$ledger = "docs/engineering/02_任务台账.md"
if (-not (Test-Path $ledger)) {
  Show-Fail "任务台账文件不存在: $ledger"
} else {
  $content = Get-Content $ledger -Raw -Encoding UTF8
  if ($content -match [regex]::Escape($TaskId)) {
    Show-Pass "任务 $TaskId 在台账存在"
  } else {
    Show-Fail "任务 $TaskId 在台账未登记，请先在 §3 对应泳道新增任务行"
  }
}

# ============================================================
# 4. AI 能力等级匹配
# ============================================================
Show-Section "4. AI 能力等级匹配"

# 从台账或 V2 实施手册推断任务难度
$playbook = "docs/05_AI实施手册.md"
$inferredDifficulty = "未知"
if (Test-Path $playbook) {
  $pb = Get-Content $playbook -Raw -Encoding UTF8
  # 抓取该 PR 卡片附近的"难度等级"行
  $section = Get-PlaybookTaskSection $pb $TaskId
  if ($section) {
    if ($section -match "难度\s*[:：]\s*(\S+)") {
      $inferredDifficulty = $matches[1].Trim()
    } elseif ($section -match "初级") {
      $inferredDifficulty = "初级"
    } elseif ($section -match "中级") {
      $inferredDifficulty = "中级"
    } elseif ($section -match "高级") {
      $inferredDifficulty = "高级"
    }
  }
}

$levelMap = @{ "junior" = 1; "middle" = 2; "senior" = 3 }
$diffMap  = @{ "初级" = 1; "中级" = 2; "高级" = 3; "未知" = 3 }

$myLevel = $levelMap[$Level]
$reqLevel = $diffMap[$inferredDifficulty]

if ($myLevel -ge $reqLevel) {
  Show-Pass "你的等级 $Level ($myLevel) >= 任务难度 $inferredDifficulty ($reqLevel)"
} else {
  Show-Fail "你的等级 $Level ($myLevel) < 任务难度 $inferredDifficulty ($reqLevel)。请换高级 AI 接手或选其它任务"
}

# ============================================================
# 5. 依赖任务全部 DONE
# ============================================================
Show-Section "5. 依赖任务全部 DONE"

if (Test-Path $playbook) {
  $pb = Get-Content $playbook -Raw -Encoding UTF8
  $section = Get-PlaybookTaskSection $pb $TaskId
  if ($section) {
    if ($section -match "\*\*依赖[:：]\*\*\s*([^\r\n]+)" -or $section -match "依赖[:：]\s*([^\r\n]+)") {
      $depsLine = $matches[1]
      # 提取所有 PR-V2-XX、SEC-001 等编号
      $deps = [regex]::Matches($depsLine, "(?:PR-V2-\d+|[A-Z]+-\d+)") | ForEach-Object { $_.Value }
      if ($deps.Count -eq 0 -or $depsLine -match "^\s*无") {
        Show-Pass "无依赖"
      } else {
        $ledgerContent = Get-Content $ledger -Raw -Encoding UTF8
        foreach ($dep in $deps) {
          $depEscaped = [regex]::Escape($dep)
          $depLine = $ledgerContent -split "`n" | Where-Object { $_ -match "^\|\s*$depEscaped\s*\|" } | Select-Object -First 1
          if ($depLine -and $depLine -match "\|\s*DONE\s*\|") {
            Show-Pass "依赖 $dep = DONE"
          } else {
            Show-Fail "依赖 $dep 未完成（台账非 DONE 状态），先做依赖任务"
          }
        }
      }
    } else {
      Show-Warn "未识别到依赖字段，请手动检查"
    }
  } else {
    Show-Warn "在 V2 实施手册未找到 $TaskId 卡片，请人工核查依赖"
  }
} else {
  Show-Warn "V2 实施手册不存在: $playbook"
}

# ============================================================
# 6. 文档体系完整
# ============================================================
Show-Section "6. 文档体系完整"

$requiredDocs = @(
  "docs/README.md",
  "docs/01_产品事实源.md",
  "docs/02_场景剧本图.md",
  "docs/03_设计系统.md",
  "docs/04_页面规格书.md",
  "docs/05_AI实施手册.md",
  "docs/engineering/00_总入口与AI接手导航.md",
  "docs/engineering/02_任务台账.md",
  "docs/engineering/AI一致性保证.md",
  "docs/engineering/forbidden-patterns.md"
)
foreach ($doc in $requiredDocs) {
  if (Test-Path $doc) {
    Show-Pass "$doc"
  } else {
    Show-Fail "缺失关键文档: $doc"
  }
}

# ============================================================
# 7. 是否已有别人在领同一任务
# ============================================================
Show-Section "7. active claim 冲突检查"

$activeDir = "ai-dev-input/10_task_claims/active"
if (Test-Path $activeDir) {
  $existingClaims = Get-ChildItem -Path $activeDir -Filter "*.md" -ErrorAction SilentlyContinue
  $conflicts = $existingClaims | Where-Object {
    $c = Get-Content $_.FullName -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
    $c -match [regex]::Escape($TaskId)
  }
  if ($conflicts) {
    Show-Fail "已有 active claim 锁定 $TaskId："
    foreach ($c in $conflicts) {
      Write-Host "    - $($c.Name)" -ForegroundColor Gray
    }
    Write-Host "    若 4 小时无心跳可考虑接管（见 00_总入口 §2.1）" -ForegroundColor Gray
  } else {
    Show-Pass "无 active claim 冲突，可创建新 claim"
  }
} else {
  Show-Warn "active 目录不存在: $activeDir"
}

# ============================================================
# 总结
# ============================================================
Write-Host ""
Write-Host ("=" * 60)
Write-Host "结果: PASS=$PASS_COUNT  FAIL=$FAIL_COUNT  WARN=$WARN_COUNT" -ForegroundColor White

if ($FAIL_COUNT -gt 0) {
  Write-Host ""
  Write-Host "[X] 不允许创建 active claim，请先修复 FAIL 项" -ForegroundColor Red
  exit 1
} else {
  Write-Host ""
  Write-Host "[OK] 检查通过，可创建 active claim 开工" -ForegroundColor Green
  Write-Host ""
  Write-Host "下一步：" -ForegroundColor White
  Write-Host "  1. cp ai-dev-input/10_task_claims/task_claim_template.md ai-dev-input/10_task_claims/active/$TaskId-<你的代号>.md"
  Write-Host "  2. 填写 claim 内容"
  Write-Host "  3. git add + commit + push（push 即占领）"
  Write-Host "  4. 开始编码"
  Write-Host "  5. 完成后跑 .\scripts\verify-pr.ps1 -TaskId $TaskId"
  exit 0
}
