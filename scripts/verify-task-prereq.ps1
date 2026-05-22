# 接手前自检脚本：AI 领任务前必须跑通此脚本才能创建 active claim。
#
# 用法：
#   .\scripts\verify-task-prereq.ps1 -TaskId PR-V2-01 -Level senior
#
# 参数：
#   -TaskId       任务编号（必填，如 PR-V2-01）
#   -Level        AI 能力等级（junior / middle / senior）
#   -SkipGitPull  跳过 git pull（仅离线开发用）
#   -SuggestCount 锁冲突时推荐的可切换任务数量（默认 3）
#
# 检查项：
#   1. git 工作树干净
#   2. 当前分支远端基线已同步
#   3. 任务编号在台账存在
#   4. 任务等级与你的等级匹配
#   5. 依赖任务全部 DONE
#   6. 文档体系（docs/）完整
#   7. 是否已有别人在领同一任务（active claim + task lock）
#
# 任何一项 FAIL 都不允许创建 active claim。

param(
  [Parameter(Mandatory = $true)]
  [string]$TaskId,

  [Parameter(Mandatory = $false)]
  [ValidateSet("junior", "middle", "senior")]
  [string]$Level = "senior",

  [Parameter(Mandatory = $false)]
  [switch]$SkipGitPull,

  [Parameter(Mandatory = $false)]
  [ValidateRange(1, 10)]
  [int]$SuggestCount = 3
)

# 锚定到项目根（脚本所在目录的父目录），让后续所有相对路径检查不受调用方 cwd 影响
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $ProjectRoot

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

function Get-LevelWeight([string]$LevelName) {
  if ([string]::IsNullOrWhiteSpace($LevelName)) {
    return 3
  }
  switch ($LevelName.Trim().ToLower()) {
    "junior" { return 1 }
    "middle" { return 2 }
    "senior" { return 3 }
    "初级" { return 1 }
    "中级" { return 2 }
    "高级" { return 3 }
    default { return 3 }
  }
}

function Get-ActiveTaskIdSet([string]$ActiveDirPath) {
  $set = @{}
  if (-not (Test-Path $ActiveDirPath)) {
    return $set
  }
  $claims = Get-ChildItem -Path $ActiveDirPath -Filter "*.md" -File -ErrorAction SilentlyContinue
  foreach ($claim in $claims) {
    $raw = Get-Content -Path $claim.FullName -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
    $taskMatch = [regex]::Match($raw, "(?m)^task_id:\s*([A-Za-z0-9\-]+)\s*$")
    if ($taskMatch.Success) {
      $taskKey = $taskMatch.Groups[1].Value.Trim().ToUpperInvariant()
      if (-not [string]::IsNullOrWhiteSpace($taskKey)) {
        $set[$taskKey] = $true
      }
    }
  }
  return $set
}

function Get-LedgerTaskSuggestions([string]$CurrentTaskId, [string]$CurrentLevel, [int]$Limit) {
  $ledgerPath = "docs/engineering/02_任务台账.md"
  $lockDirPath = "ai-dev-input/10_task_claims/active_locks"
  $activeDirPath = "ai-dev-input/10_task_claims/active"
  $activeTaskSet = Get-ActiveTaskIdSet $activeDirPath

  $result = New-Object System.Collections.Generic.List[object]
  if (-not (Test-Path $ledgerPath)) {
    return $result
  }

  $myLevel = Get-LevelWeight $CurrentLevel
  $seen = @{}
  $lines = Get-Content -Path $ledgerPath -Encoding UTF8

  foreach ($line in $lines) {
    if ($null -eq $line) {
      continue
    }
    if ($line -notmatch '^\|\s*[A-Z][A-Z0-9\-]+\s*\|') {
      continue
    }

    $cells = $line.Split('|')
    if ($cells.Count -lt 6) {
      continue
    }

    $taskId = $cells[1].Trim()
    $taskName = $cells[2].Trim()
    $status = $cells[3].Trim().ToUpper()
    $minLevel = $cells[4].Trim()
    $batch = $cells[5].Trim()

    if ($status -ne "TODO") {
      continue
    }
    if ([string]::IsNullOrWhiteSpace($taskId) -or $taskId -eq $CurrentTaskId) {
      continue
    }
    $taskKey = $taskId.ToUpperInvariant()
    if ($seen.ContainsKey($taskKey)) {
      continue
    }
    $seen[$taskKey] = $true

    $requiredLevel = Get-LevelWeight $minLevel
    if ($myLevel -lt $requiredLevel) {
      continue
    }

    if ($activeTaskSet.ContainsKey($taskKey)) {
      continue
    }

    $lockPath = Join-Path $lockDirPath "$taskId.lock"
    if (Test-Path -LiteralPath $lockPath) {
      continue
    }

    $result.Add([pscustomobject]@{
      TaskId = $taskId
      TaskName = $taskName
      Batch = $batch
      MinLevel = $minLevel
    }) | Out-Null

    if ($result.Count -ge $Limit) {
      break
    }
  }

  return $result
}

function Show-TaskSwitchSuggestions([string]$CurrentTaskId, [string]$CurrentLevel, [int]$Limit, [string]$Reason) {
  try {
    $suggestions = Get-LedgerTaskSuggestions -CurrentTaskId $CurrentTaskId -CurrentLevel $CurrentLevel -Limit $Limit
    if ($null -eq $suggestions -or $suggestions.Count -eq 0) {
      Show-Warn "当前任务受阻（$Reason），但未找到可自动切换候选任务；请在台账 §2 手动挑选未锁定 TODO 任务。"
      return
    }

    Write-Host "  当前任务受阻原因：$Reason" -ForegroundColor Gray
    Write-Host "  建议切换以下任务（TODO + 未锁定 + 等级匹配）：" -ForegroundColor White
    foreach ($task in $suggestions) {
      Write-Host "    - $($task.TaskId) | $($task.TaskName) | $($task.Batch)" -ForegroundColor Gray
    }

    $ids = ($suggestions | ForEach-Object { $_.TaskId }) -join ","
    Write-Host "  fallback_task_candidates=$ids" -ForegroundColor DarkGray
  } catch {
    Show-Warn "切换任务建议生成失败：$($_.Exception.Message)"
  }
}

function Test-GitRef([string]$Ref) {
  if ([string]::IsNullOrWhiteSpace($Ref)) {
    return $false
  }
  git rev-parse --verify $Ref 2>$null | Out-Null
  return $LASTEXITCODE -eq 0
}

function Get-CurrentGitBranch {
  $branch = git branch --show-current 2>$null
  if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($branch)) {
    return $branch.Trim()
  }
  return ""
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

  git rev-parse --verify origin/develop 2>$null | Out-Null
  if ($LASTEXITCODE -eq 0) {
    return "origin/develop"
  }

  return "origin/develop"
}

function Get-TaskAliases([string]$TaskId) {
  $aliases = New-Object System.Collections.Generic.List[string]
  $aliases.Add($TaskId)
  if ($TaskId -match '^PR-V2-(\d{2})$') {
    $aliases.Add("PR-$($matches[1])")
  } elseif ($TaskId -match '^PR-(\d{2})$') {
    $aliases.Add("PR-V2-$($matches[1])")
  }
  # GA-* 任务编号无别名，但需确保正则匹配支持 GA- 前缀
  return $aliases | Select-Object -Unique
}

# GA-* 任务编号模式判断
function Test-GaTaskId([string]$TaskId) {
  return $TaskId -match '^GA-[A-Z]+-\d{2}$'
}

# 从 GA backlog 或台账提取 GA 任务的依赖
function Get-GaTaskDependencies([string]$TaskId) {
  $deps = New-Object System.Collections.Generic.List[string]

  # 优先从 GA backlog 提取依赖
  $gaBacklog = "docs/AI_TEAM_PR_BACKLOG_V1.0_GA.md"
  if (Test-Path $gaBacklog) {
    $gaContent = Get-Content $gaBacklog -Raw -Encoding UTF8
    # 匹配任务行：| GA-XXX-NN | ... | ... | ... | 依赖 | ... |
    $taskPattern = [regex]::Escape($TaskId)
    $gaLines = $gaContent -split "`n" | Where-Object { $_ -match "^\|\s*$taskPattern\s*\|" }
    foreach ($line in $gaLines) {
      # 提取依赖列（第5列，索引4）
      $cols = $line -split '\|' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
      if ($cols.Count -ge 5) {
        $depCol = $cols[4].Trim()
        $depMatches = [regex]::Matches($depCol, '(?:GA-[A-Z]+-\d{2}|[A-Z]+-\d{3}|PR-V2-\d{2}|PR-FINAL-\d{2})')
        foreach ($m in $depMatches) {
          if (-not $deps.Contains($m.Value)) {
            [void]$deps.Add($m.Value)
          }
        }
      }
    }
  }

  # 回退到台账查找
  if ($deps.Count -eq 0) {
    $ledger = "docs/engineering/02_任务台账.md"
    if (Test-Path $ledger) {
      $ledgerContent = Get-Content $ledger -Raw -Encoding UTF8
      $taskPattern = [regex]::Escape($TaskId)
      $ledgerLines = $ledgerContent -split "`n" | Where-Object { $_ -match "^\|\s*$taskPattern\s*\|" }
      foreach ($line in $ledgerLines) {
        $cols = $line -split '\|' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
        if ($cols.Count -ge 6) {
          $depCol = $cols[5].Trim()
          $depMatches = [regex]::Matches($depCol, '(?:GA-[A-Z]+-\d{2}|[A-Z]+-\d{3}|PR-V2-\d{2}|PR-FINAL-\d{2})')
          foreach ($m in $depMatches) {
            if (-not $deps.Contains($m.Value)) {
              [void]$deps.Add($m.Value)
            }
          }
        }
      }
    }
  }

  return $deps.ToArray()
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
# 0. 分支策略
# ============================================================
Show-Section "0. 分支策略"

$currentBranch = Get-CurrentGitBranch
if ($currentBranch -eq "main") {
  Show-Fail "当前在 main，AI 禁止在 main 领任务、开发、提交或推送；请切到 develop 或基于 origin/develop 创建任务分支"
} elseif ([string]::IsNullOrWhiteSpace($currentBranch)) {
  Show-Warn "当前为 detached HEAD，无法识别分支；请确认目标基线是 origin/develop"
} else {
  Show-Pass "当前分支 $currentBranch；AI 任务只能合入 develop"
}

if (Test-GitRef "origin/develop") {
  Show-Pass "远端 develop 基线存在"
} else {
  Show-Fail "未找到 origin/develop；AI 任务必须以 develop 为开发和合入基线"
}

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
    if (-not (Test-GitRef $baseRef)) {
      Show-Fail "未找到 $baseRef；先 fetch origin develop，AI 不允许退回 main 作为开发基线"
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

$isGaTask = Test-GaTaskId $TaskId

if ($isGaTask) {
  # GA-* 任务：从 GA backlog 或台账提取依赖
  $gaDeps = Get-GaTaskDependencies $TaskId
  if ($gaDeps.Count -eq 0) {
    Show-Pass "GA 任务 $TaskId 无依赖"
  } else {
    $ledgerContent = Get-Content $ledger -Raw -Encoding UTF8
    foreach ($dep in $gaDeps) {
      $depEscaped = [regex]::Escape($dep)
      $depLine = $ledgerContent -split "`n" | Where-Object { $_ -match "^\|\s*$depEscaped\s*\|" } | Select-Object -First 1
      if ($depLine -and $depLine -match "\|\s*DONE\s*\|") {
        Show-Pass "依赖 $dep = DONE"
      } else {
        Show-Fail "依赖 $dep 未完成（台账非 DONE 状态），先做依赖任务"
      }
    }
  }
} elseif (Test-Path $playbook) {
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
Show-Section "7. active claim / task lock 冲突检查"

$activeDir = "ai-dev-input/10_task_claims/active"
$lockDir = "ai-dev-input/10_task_claims/active_locks"
$expectedLock = Join-Path $lockDir "$TaskId.lock"
$hasTaskOwnershipConflict = $false
$hasGlobalCollabConflict = $false
if (Test-Path $activeDir) {
  $existingClaims = Get-ChildItem -Path $activeDir -Filter "*.md" -ErrorAction SilentlyContinue
  $conflicts = $existingClaims | Where-Object {
    $c = Get-Content $_.FullName -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
    $c -match "(?m)^task_id:\s*$([regex]::Escape($TaskId))\s*$"
  }
  if ($conflicts) {
    Show-Fail "已有 active claim 锁定 $TaskId："
    $hasTaskOwnershipConflict = $true
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

if (Test-Path $expectedLock) {
  Show-Fail "已有 task lock 锁定 $TaskId：$expectedLock"
  $hasTaskOwnershipConflict = $true
  Write-Host "    task lock 是 Git 层面的同任务唯一锁；必须等待原 claim 完成、归档或按接管规则处理" -ForegroundColor Gray
} else {
  if (-not (Test-Path $lockDir)) {
    Show-Warn "task lock 目录不存在: $lockDir"
  } else {
    Show-Pass "无 task lock 冲突，可创建 $expectedLock"
  }
}

$collabScript = "medkernel-mvp/scripts/check-ai-collaboration.ps1"
if (Test-Path $collabScript) {
  # GA-REL-01: 全局门禁只检查与当前任务直接相关的冲突。
  # 其他 AI 的内部违规（branch naming 等）不应阻断新任务认领。
  # 只关注：(1) 同任务 claim/lock 冲突 (2) write_scope 与当前任务重叠。
  $collabOutput = @()
  $collabExitCode = 0
  try {
    $collabOutput = & $collabScript -Strict 2>&1
    $collabExitCode = $LASTEXITCODE
  } catch {
    $collabExitCode = 1
    $collabOutput = @($_.Exception.Message)
  }

  if ($collabExitCode -eq 0) {
    Show-Pass "全局并发状态一致：无 orphan lock / 重复任务 / write_scope 重叠"
  } else {
    # 解析协作问题，只关注与当前任务直接相关的冲突
    $allIssues = @()
    if ($collabOutput -match "collaboration_issues=(.+)") {
      $allIssues = $matches[1] -split "; "
    } elseif ($collabOutput -is [string] -and $collabOutput -match "collaboration_issues=(.+)") {
      $allIssues = $matches[1] -split "; "
    } else {
      foreach ($line in $collabOutput) {
        if ($line -match "collaboration_issues=(.+)") {
          $allIssues = $matches[1] -split "; "
          break
        }
      }
    }

    # 过滤：只保留与当前任务直接相关的冲突
    $relevantIssues = @()
    $taskPattern = [regex]::Escape($TaskId)
    foreach ($issue in $allIssues) {
      # 同任务 claim/lock 冲突
      if ($issue -match "task_id=$taskPattern\b") {
        $relevantIssues += $issue
        continue
      }
      # write_scope 重叠涉及当前任务
      if ($issue -match "write_scope_overlap" -and ($issue -match "left=.*$taskPattern" -or $issue -match "right=.*$taskPattern")) {
        $relevantIssues += $issue
        continue
      }
      # orphan lock（始终关注）
      if ($issue -match "orphan_task_lock") {
        $relevantIssues += $issue
        continue
      }
      # duplicate（始终关注）
      if ($issue -match "duplicate_active_task_id" -or $issue -match "duplicate_task_lock") {
        $relevantIssues += $issue
        continue
      }
    }

    if ($relevantIssues.Count -eq 0) {
      Show-Pass "全局门禁通过（与 $TaskId 无直接冲突；其他 AI 的内部问题不阻断）"
      # 仍输出其他问题作为参考
      $otherIssues = $allIssues | Where-Object { $relevantIssues -notcontains $_ }
      if ($otherIssues.Count -gt 0) {
        Show-Warn "其他 AI 存在 $($otherIssues.Count) 个协作问题（不阻断当前任务）："
        $otherIssues | Select-Object -First 3 | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
      }
    } else {
      Show-Fail "与当前任务 $TaskId 存在直接冲突："
      $relevantIssues | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
      $hasGlobalCollabConflict = $true
    }
  }
} else {
  Show-Warn "缺少 $collabScript，无法做全局并发检查"
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
  if ($hasTaskOwnershipConflict -or $hasGlobalCollabConflict) {
    Show-Section "7.1 锁阻断后的切换建议"
    $reasons = New-Object System.Collections.Generic.List[string]
    if ($hasTaskOwnershipConflict) {
      [void]$reasons.Add("task_lock_or_active_claim_conflict")
    }
    if ($hasGlobalCollabConflict) {
      [void]$reasons.Add("global_collaboration_gate")
    }
    Show-TaskSwitchSuggestions -CurrentTaskId $TaskId -CurrentLevel $Level -Limit $SuggestCount -Reason ($reasons -join ",")
  }
  exit 1
} else {
  Write-Host ""
  Write-Host "[OK] 检查通过，可创建 active claim 开工" -ForegroundColor Green
  Write-Host ""
  Write-Host "下一步：" -ForegroundColor White
  Write-Host "  1. cp ai-dev-input/10_task_claims/task_claim_template.md ai-dev-input/10_task_claims/active/$TaskId-<你的代号>.md"
  Write-Host "  2. 创建 ai-dev-input/10_task_claims/active_locks/$TaskId.lock（同任务唯一锁）"
  Write-Host "  3. 填写 claim 与 lock 内容"
  Write-Host "  4. git add + commit + push 到 develop（push 成功才算占领）"
  Write-Host "  5. 开始编码"
  Write-Host "  6. 完成后跑 .\scripts\verify-pr.ps1 -TaskId $TaskId"
  exit 0
}
