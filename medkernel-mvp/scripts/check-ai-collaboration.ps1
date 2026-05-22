param(
  [string]$TaskId = "",
  [string]$ClaimId = "",
  [switch]$Strict,
  [switch]$RequireClean,
  [switch]$FailOnStaleClaim
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$engineRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $engineRoot
Set-Location $repoRoot

function Write-Section($Title) {
  Write-Output ""
  Write-Output "== $Title =="
}

function Get-MetadataField($Content, $Name) {
  $pattern = "(?m)^" + [regex]::Escape($Name) + ":[ \t]*(.*)$"
  $match = [regex]::Match($Content, $pattern)
  if ($match.Success) {
    return $match.Groups[1].Value.Trim()
  }
  return ""
}

function Get-SectionCodeBlock($Content, $Title) {
  $pattern = '(?ms)^##\s+' + [regex]::Escape($Title) + '\s*```(?:text)?\s*(.*?)\s*```'
  $match = [regex]::Match($Content, $pattern)
  if ($match.Success) {
    return $match.Groups[1].Value.Trim()
  }
  return ""
}

function Normalize-ScopeList($Raw) {
  if ([string]::IsNullOrWhiteSpace($Raw)) {
    return @()
  }
  $result = New-Object System.Collections.Generic.List[string]
  $items = $Raw -split '[,;\r\n]+'
  foreach ($item in $items) {
    $scope = $item.Replace([char]92, [char]47).Trim()
    if ([string]::IsNullOrWhiteSpace($scope)) {
      continue
    }
    if ($scope -match '^\s*#' -or $scope -eq '-' -or $scope -eq 'N/A') {
      continue
    }
    if ($scope.StartsWith('./')) {
      $scope = $scope.Substring(2)
    }
    while ($scope.EndsWith('/')) {
      $scope = $scope.Substring(0, $scope.Length - 1)
    }
    if (-not $result.Contains($scope)) {
      [void]$result.Add($scope)
    }
  }
  return $result.ToArray()
}

function Test-ScopeOverlap($A, $B) {
  if ([string]::IsNullOrWhiteSpace($A) -or [string]::IsNullOrWhiteSpace($B)) {
    return $false
  }
  $left = $A.Replace([char]92, [char]47).Trim()
  $right = $B.Replace([char]92, [char]47).Trim()
  while ($left.EndsWith('/')) {
    $left = $left.Substring(0, $left.Length - 1)
  }
  while ($right.EndsWith('/')) {
    $right = $right.Substring(0, $right.Length - 1)
  }
  if ($left -eq $right) {
    return $true
  }
  $leftPrefix = $left.TrimEnd("*")
  $rightPrefix = $right.TrimEnd("*")
  $leftWithSlash = $leftPrefix + "/"
  $rightWithSlash = $rightPrefix + "/"
  return $leftPrefix.StartsWith($rightWithSlash) -or $rightPrefix.StartsWith($leftWithSlash)
}

$collaborationIssues = @()
$collaborationWarnings = @()
$claimRecords = @()
$lockRecords = @()

# GA-GOV-01: 支持 GA-* 任务编号的正则模式
$gaTaskPattern = '^GA-[A-Z]+-\d{2}$'

Write-Section "Git"
git status --short --branch
git log -1 --pretty=format:"head=%H%nsubject=%s"
Write-Output ""

Write-Section "Active Claims"
$activeDir = Join-Path $repoRoot "ai-dev-input\10_task_claims\active"
$lockDir = Join-Path $repoRoot "ai-dev-input\10_task_claims\active_locks"
$activeClaims = Get-ChildItem -Path $activeDir -Filter "*.md" -File -ErrorAction SilentlyContinue
if ($activeClaims.Count -eq 0) {
  Write-Output "active_claims=0"
} else {
  foreach ($claim in $activeClaims) {
    $content = Get-Content -Path $claim.FullName -Raw
    $claimStatus = Get-MetadataField $content "status"
    $claimTask = Get-MetadataField $content "task_id"
    $activeClaimId = Get-MetadataField $content "claim_id"
    $heartbeat = Get-MetadataField $content "last_heartbeat"
    $writeScope = Get-MetadataField $content "write_scope"
    $writeScopeBlock = Get-SectionCodeBlock $content "Write Scope"
    $writeScopeItems = @(Normalize-ScopeList (($writeScope, $writeScopeBlock) -join "`n"))
    $taskLockPath = Get-MetadataField $content "task_lock_path"
    $requiredFields = @(
      "claim_id",
      "task_id",
      "task_lock_path",
      "status",
      "git_base_commit",
      "git_status_at_claim",
      "write_scope",
      "review_required",
      "feature_acceptance_required"
    )
    $missingFields = foreach ($field in $requiredFields) {
      if ($field -eq "write_scope" -and $writeScopeItems.Count -gt 0) {
        continue
      }
      if ([string]::IsNullOrWhiteSpace((Get-MetadataField $content $field))) {
        $field
      }
    }
    Write-Output ("{0} task_id={1} status={2} last_heartbeat={3}" -f $claim.Name, $claimTask, $claimStatus, $heartbeat)
    # GA-GOV-01: GA-* 任务必须使用 ai/<TASK-ID>/<slug> 分支命名
    if (![string]::IsNullOrWhiteSpace($claimTask) -and $claimTask -match $gaTaskPattern) {
      $claimBranch = Get-MetadataField $content "branch"
      if (![string]::IsNullOrWhiteSpace($claimBranch) -and $claimBranch -notmatch "^ai/$claimTask/") {
        $issue = ("ga_task_branch_naming_violation file={0} task_id={1} branch={2} expected_pattern=ai/{1}/<slug>" -f $claim.Name, $claimTask, $claimBranch)
        $collaborationIssues += $issue
        Write-Output $issue
      }
    }
    if ($missingFields.Count -gt 0) {
      $issue = ("claim_missing_fields file={0} fields={1}" -f $claim.Name, ($missingFields -join ","))
      $collaborationIssues += $issue
      Write-Output $issue
    }
    if ($writeScopeItems.Count -eq 0) {
      $issue = ("claim_missing_write_scope file={0}" -f $claim.Name)
      $collaborationIssues += $issue
      Write-Output $issue
    }
    if (![string]::IsNullOrWhiteSpace($claimTask)) {
      $expectedLockPath = "ai-dev-input/10_task_claims/active_locks/$claimTask.lock"
      $normalizedTaskLockPath = $taskLockPath -replace "\\", "/"
      if ($normalizedTaskLockPath -ne $expectedLockPath) {
        $issue = ("claim_lock_path_mismatch file={0} expected={1} actual={2}" -f $claim.Name, $expectedLockPath, $taskLockPath)
        $collaborationIssues += $issue
        Write-Output $issue
      }
      $lockPath = Join-Path $lockDir "$claimTask.lock"
      if (!(Test-Path -LiteralPath $lockPath)) {
        $issue = ("claim_missing_task_lock file={0} expected={1}" -f $claim.Name, $expectedLockPath)
        $collaborationIssues += $issue
        Write-Output $issue
      } else {
        $lockContent = Get-Content -LiteralPath $lockPath -Raw
        $lockClaimId = Get-MetadataField $lockContent "claim_id"
        $lockTaskId = Get-MetadataField $lockContent "task_id"
        if ($lockClaimId -ne $activeClaimId -or $lockTaskId -ne $claimTask) {
          $issue = ("task_lock_mismatch lock={0} expected_claim={1} actual_claim={2} expected_task={3} actual_task={4}" -f $expectedLockPath, $activeClaimId, $lockClaimId, $claimTask, $lockTaskId)
          $collaborationIssues += $issue
          Write-Output $issue
        }
      }
    }
    if (![string]::IsNullOrWhiteSpace($heartbeat)) {
      $parsedHeartbeat = [datetime]::MinValue
      if ([datetime]::TryParse($heartbeat, [ref]$parsedHeartbeat)) {
        $heartbeatAgeHours = ([datetime]::Now - $parsedHeartbeat).TotalHours
        if ($heartbeatAgeHours -gt 4) {
          $stale = ("claim_stale_over_4h file={0} age_hours={1:N1}" -f $claim.Name, $heartbeatAgeHours)
          if ($FailOnStaleClaim) {
            $collaborationIssues += $stale
            Write-Output $stale
          } else {
            $collaborationWarnings += $stale
            Write-Output ("warn_" + $stale)
          }
        }
      }
    }

    $claimRecords += [pscustomobject]@{
      File = $claim.Name
      ClaimId = $activeClaimId
      TaskId = $claimTask
      Status = $claimStatus
      WriteScope = $writeScopeItems
    }
  }

  $duplicateTaskGroups = $claimRecords |
    Where-Object { ![string]::IsNullOrWhiteSpace($_.TaskId) } |
    Group-Object TaskId |
    Where-Object { $_.Count -gt 1 }
  foreach ($group in $duplicateTaskGroups) {
    $files = ($group.Group | ForEach-Object { $_.File }) -join ","
    $issue = ("duplicate_active_task_id task_id={0} files={1}" -f $group.Name, $files)
    $collaborationIssues += $issue
    Write-Output $issue
  }

  for ($i = 0; $i -lt $claimRecords.Count; $i++) {
    for ($j = $i + 1; $j -lt $claimRecords.Count; $j++) {
      foreach ($leftScope in @($claimRecords[$i].WriteScope)) {
        foreach ($rightScope in @($claimRecords[$j].WriteScope)) {
          if (Test-ScopeOverlap $leftScope $rightScope) {
            $issue = ("write_scope_overlap left={0}:{1} right={2}:{3}" -f $claimRecords[$i].File, $leftScope, $claimRecords[$j].File, $rightScope)
            $collaborationIssues += $issue
            Write-Output $issue
          }
        }
      }
    }
  }

  # GA-GOV-01: 检查 GA-* 任务是否修改了共享文件（非架构师任务不允许修改共享文件）
  $sharedFiles = @(
    "frontend/src/api/types.ts",
    "frontend/src/router/menuConfig.tsx",
    "frontend/src/router/routes.tsx",
    "frontend/src/styles/tokens.css",
    "frontend/src/App.tsx",
    "medkernel-mvp/pom.xml",
    "medkernel-mvp/src/main/resources/application.yml",
    "scripts/verify-pr.ps1",
    "scripts/verify-task-prereq.ps1",
    "medkernel-mvp/scripts/check-ai-collaboration.ps1",
    "docs/AI_CHARTER.md",
    "docs/PRODUCT_ARCHITECTURE_FINAL.md",
    "docs/AI_TEAM_PR_BACKLOG_V1.0_GA.md"
  )
  foreach ($claimRec in $claimRecords) {
    if ([string]::IsNullOrWhiteSpace($claimRec.TaskId)) { continue }
    if ($claimRec.TaskId -notmatch $gaTaskPattern) { continue }
    $claimRole = ""
    foreach ($claim in $activeClaims) {
      $content = Get-Content -Path $claim.FullName -Raw
      $cTaskId = Get-MetadataField $content "task_id"
      if ($cTaskId -eq $claimRec.TaskId) {
        $claimRole = Get-MetadataField $content "role"
        break
      }
    }
    # 架构师角色允许修改共享文件
    if ($claimRole -eq "architect" -or $claimRole -eq "senior") { continue }
    foreach ($scope in @($claimRec.WriteScope)) {
      foreach ($shared in $sharedFiles) {
        $normalizedScope = $scope.Replace([char]92, [char]47).TrimEnd('*').TrimEnd('/')
        $normalizedShared = $shared.Replace([char]92, [char]47).TrimEnd('/')
        if ($normalizedShared.StartsWith($normalizedScope) -or $normalizedScope.StartsWith($normalizedShared)) {
          $issue = ("ga_task_shared_file_violation file={0} task_id={1} scope={2} shared_file={3}" -f $claimRec.File, $claimRec.TaskId, $scope, $shared)
          $collaborationIssues += $issue
          Write-Output $issue
        }
      }
    }
  }
}

Write-Section "Active Task Locks"
$activeLocks = Get-ChildItem -Path $lockDir -Filter "*.lock" -File -ErrorAction SilentlyContinue
if ($activeLocks.Count -eq 0) {
  Write-Output "active_task_locks=0"
} else {
  foreach ($lock in $activeLocks) {
    $content = Get-Content -Path $lock.FullName -Raw
    $lockTask = Get-MetadataField $content "task_id"
    $lockClaimId = Get-MetadataField $content "claim_id"
    $expectedName = if (![string]::IsNullOrWhiteSpace($lockTask)) { "$lockTask.lock" } else { "" }
    Write-Output ("{0} task_id={1} claim_id={2}" -f $lock.Name, $lockTask, $lockClaimId)
    if ([string]::IsNullOrWhiteSpace($lockTask) -or [string]::IsNullOrWhiteSpace($lockClaimId)) {
      $issue = ("task_lock_missing_fields file={0}" -f $lock.Name)
      $collaborationIssues += $issue
      Write-Output $issue
    }
    if (![string]::IsNullOrWhiteSpace($expectedName) -and $lock.Name -ne $expectedName) {
      $issue = ("task_lock_filename_mismatch file={0} expected={1}" -f $lock.Name, $expectedName)
      $collaborationIssues += $issue
      Write-Output $issue
    }
    $matchingClaim = $claimRecords | Where-Object { $_.ClaimId -eq $lockClaimId -and $_.TaskId -eq $lockTask } | Select-Object -First 1
    if ($null -eq $matchingClaim) {
      $issue = ("orphan_task_lock file={0} task_id={1} claim_id={2}" -f $lock.Name, $lockTask, $lockClaimId)
      $collaborationIssues += $issue
      Write-Output $issue
    }
    $lockRecords += [pscustomobject]@{
      File = $lock.Name
      ClaimId = $lockClaimId
      TaskId = $lockTask
    }
  }
  $duplicateLockGroups = $lockRecords |
    Where-Object { ![string]::IsNullOrWhiteSpace($_.TaskId) } |
    Group-Object TaskId |
    Where-Object { $_.Count -gt 1 }
  foreach ($group in $duplicateLockGroups) {
    $files = ($group.Group | ForEach-Object { $_.File }) -join ","
    $issue = ("duplicate_task_lock task_id={0} files={1}" -f $group.Name, $files)
    $collaborationIssues += $issue
    Write-Output $issue
  }
}

Write-Section "Pending Reviews"
$reviewDirs = @(
  (Join-Path $repoRoot "ai-dev-input\11_ai_reviews\pending"),
  (Join-Path $repoRoot "ai-dev-input\11_ai_reviews\changes_requested")
)
$reviews = foreach ($dir in $reviewDirs) {
  Get-ChildItem -Path $dir -Filter "*.md" -File -ErrorAction SilentlyContinue
}
if ($reviews.Count -eq 0) {
  Write-Output "pending_or_changes_requested_reviews=0"
} else {
  foreach ($review in $reviews) {
    $content = Get-Content -Path $review.FullName -Raw
    $reviewStatus = [regex]::Match($content, "(?m)^(review_status|status):\s*(.+)$").Groups[2].Value.Trim()
    $openFindings = [regex]::Match($content, "(?m)^open_findings:\s*(.+)$").Groups[1].Value.Trim()
    Write-Output ("{0} status={1} open_findings={2}" -f $review.Name, $reviewStatus, $openFindings)
  }
}

Write-Section "Feature Acceptance"
$acceptanceDirs = @(
  (Join-Path $repoRoot "ai-dev-input\13_feature_acceptance\pending"),
  (Join-Path $repoRoot "ai-dev-input\13_feature_acceptance\needs_optimization")
)
$acceptanceItems = foreach ($dir in $acceptanceDirs) {
  Get-ChildItem -Path $dir -Filter "*.md" -File -ErrorAction SilentlyContinue
}
if ($acceptanceItems.Count -eq 0) {
  Write-Output "pending_or_needs_optimization_acceptance=0"
} else {
  foreach ($item in $acceptanceItems) {
    $content = Get-Content -Path $item.FullName -Raw
    $featureStatus = [regex]::Match($content, "(?m)^status:\s*(.+)$").Groups[1].Value.Trim()
    $qualityLevel = [regex]::Match($content, "(?m)^quality_level:\s*(.+)$").Groups[1].Value.Trim()
    Write-Output ("{0} status={1} quality_level={2}" -f $item.Name, $featureStatus, $qualityLevel)
  }
}

if (![string]::IsNullOrWhiteSpace($ClaimId)) {
  $claimPath = Join-Path $activeDir "$ClaimId.md"
  if (!(Test-Path -LiteralPath $claimPath)) {
    $message = "claim_not_active=$ClaimId"
    if ($Strict) {
      throw $message
    }
    Write-Output $message
  }
}

if (![string]::IsNullOrWhiteSpace($TaskId)) {
  $matched = $false
  foreach ($claim in $activeClaims) {
    $content = Get-Content -Path $claim.FullName -Raw
    if ($content -match "(?m)^task_id:\s*$([regex]::Escape($TaskId))\s*$") {
      $matched = $true
      break
    }
  }
  if (!$matched) {
    $message = "task_has_no_active_claim=$TaskId"
    if ($Strict) {
      throw $message
    }
    Write-Output $message
  }
}

$dirty = git status --porcelain
if ($RequireClean -and $dirty) {
  throw "working_tree_not_clean"
}

if ($collaborationWarnings.Count -gt 0) {
  Write-Output ("collaboration_warnings=" + ($collaborationWarnings -join "; "))
}

if ($collaborationIssues.Count -gt 0) {
  $message = "collaboration_issues=" + ($collaborationIssues -join "; ")
  if ($Strict) {
    throw $message
  }
  Write-Output $message
  exit 1
}
