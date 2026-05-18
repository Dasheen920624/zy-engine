param(
  [string]$Root = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Root)) {
  $engineRoot = Split-Path -Parent $PSScriptRoot
  $Root = Split-Path -Parent $engineRoot
}

function New-TextFromCodePoints([int[]]$CodePoints) {
  $builder = New-Object System.Text.StringBuilder
  foreach ($codePoint in $CodePoints) {
    [void]$builder.Append([char]$codePoint)
  }
  return $builder.ToString()
}

$badPatterns = @(
  [string][char]0xFFFD,
  (New-TextFromCodePoints @(0x9473, 0x54E5)),
  (New-TextFromCodePoints @(0x6D93, 0x64B6)),
  (New-TextFromCodePoints @(0x9356, 0x8364)),
  (New-TextFromCodePoints @(0x7ED0, 0xFE3D)),
  (New-TextFromCodePoints @(0x93C1, 0x7248)),
  (New-TextFromCodePoints @(0x5BEE, 0x66DF)),
  (New-TextFromCodePoints @(0x7039, 0x5C7E)),
  (New-TextFromCodePoints @(0x59AB, 0x20AC)),
  (New-TextFromCodePoints @(0x74BA, 0xE21A)),
  (New-TextFromCodePoints @(0x6748, 0x70AC)),
  (New-TextFromCodePoints @(0x93AC, 0x30E6)),
  (New-TextFromCodePoints @(0x934A, 0x6B13))
)

$include = @("*.md", "*.json", "*.yml", "*.yaml", "*.sql", "*.java", "*.html", "*.js", "*.http")
$excludeSegments = @("\target\", "\m2repo\")
$issues = New-Object System.Collections.Generic.List[object]

Get-ChildItem -Path $Root -Recurse -File -Include $include | ForEach-Object {
  $path = $_.FullName
  foreach ($segment in $excludeSegments) {
    if ($path.Contains($segment)) {
      return
    }
  }

  $text = Get-Content -LiteralPath $path -Raw -Encoding UTF8
  foreach ($pattern in $badPatterns) {
    if ($text.Contains($pattern)) {
      $issues.Add([pscustomobject]@{
        file = $path
        issue = "Possible mojibake marker found."
      })
    }
  }
}

$jsonRoot = Join-Path $Root "ai-dev-input"
if (Test-Path -LiteralPath $jsonRoot) {
  Get-ChildItem -Path $jsonRoot -Recurse -File -Filter "*.json" | ForEach-Object {
    try {
      Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8 | ConvertFrom-Json | Out-Null
    } catch {
      $issues.Add([pscustomobject]@{
        file = $_.FullName
        issue = "JSON parse failed under UTF-8: $($_.Exception.Message)"
      })
    }
  }
}

if ($issues.Count -gt 0) {
  $issues | Format-Table -AutoSize
  throw "Encoding verification failed."
}

Write-Host "Encoding verification passed. No common mojibake markers found; JSON samples parse as UTF-8."
