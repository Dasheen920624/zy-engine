param(
  [string]$TaskId = "",
  [string]$ClaimId = "",
  [switch]$Strict
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
  $pattern = "(?m)^" + [regex]::Escape($Name) + ":\s*(.*)$"
  $match = [regex]::Match($Content, $pattern)
  if ($match.Success) {
    return $match.Groups[1].Value.Trim()
  }
  return ""
}

$collaborationIssues = @()
$claimRecords = @()

Write-Section "Git"
git status --short --branch
git log -1 --pretty=format:"head=%H%nsubject=%s"
Write-Output ""

Write-Section "Active Claims"
$activeDir = Join-Path $repoRoot "ai-dev-input\10_task_claims\active"
$activeClaims = Get-ChildItem -Path $activeDir -Filter "*.md" -File -ErrorAction SilentlyContinue
if ($activeClaims.Count -eq 0) {
  Write-Output "active_claims=0"
} else {
  foreach ($claim in $activeClaims) {
    $content = Get-Content -Path $claim.FullName -Raw
    $claimStatus = Get-MetadataField $content "status"
    $claimTask = Get-MetadataField $content "task_id"
    $claimId = Get-MetadataField $content "claim_id"
    $heartbeat = Get-MetadataField $content "last_heartbeat"
    $writeScope = Get-MetadataField $content "write_scope"
    $requiredFields = @(
      "claim_id",
      "task_id",
      "status",
      "git_base_commit",
      "git_status_at_claim",
      "write_scope",
      "review_required",
      "feature_acceptance_required"
    )
    $missingFields = foreach ($field in $requiredFields) {
      if ([string]::IsNullOrWhiteSpace((Get-MetadataField $content $field))) {
        $field
      }
    }
    Write-Output ("{0} task_id={1} status={2} last_heartbeat={3}" -f $claim.Name, $claimTask, $claimStatus, $heartbeat)
    if ($missingFields.Count -gt 0) {
      $issue = ("claim_missing_fields file={0} fields={1}" -f $claim.Name, ($missingFields -join ","))
      $collaborationIssues += $issue
      Write-Output $issue
    }
    if ([string]::IsNullOrWhiteSpace($writeScope)) {
      $issue = ("claim_missing_write_scope file={0}" -f $claim.Name)
      $collaborationIssues += $issue
      Write-Output $issue
    }
    if (![string]::IsNullOrWhiteSpace($heartbeat)) {
      $parsedHeartbeat = [datetime]::MinValue
      if ([datetime]::TryParse($heartbeat, [ref]$parsedHeartbeat)) {
        $heartbeatAgeHours = ([datetime]::Now - $parsedHeartbeat).TotalHours
        if ($heartbeatAgeHours -gt 4) {
          $issue = ("claim_stale_over_4h file={0} age_hours={1:N1}" -f $claim.Name, $heartbeatAgeHours)
          $collaborationIssues += $issue
          Write-Output $issue
        }
      }
    }
    $claimRecords += [pscustomobject]@{
      File = $claim.Name
      ClaimId = $claimId
      TaskId = $claimTask
      Status = $claimStatus
      WriteScope = $writeScope
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
if ($Strict -and $dirty) {
  throw "working_tree_not_clean"
}

if ($Strict -and $collaborationIssues.Count -gt 0) {
  throw ("collaboration_issues=" + ($collaborationIssues -join "; "))
}
