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
#   1. 工作树有改动（不是空 PR）
#   2. ADR 不变量未被违反（grep 禁用模式）
#   3. 后端 build + test（如适用）
#   4. 前端 lint + test + build（如适用）
#   5. UTF-8 无 BOM
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

Write-Host ""
Write-Host "MedKernel | 提交前 DoD 自检 | Task=$TaskId" -ForegroundColor White
Write-Host ("=" * 60)

# ============================================================
# 1. 工作树有改动
# ============================================================
Show-Section "1. 工作树有改动"

$status = git status --porcelain
if ([string]::IsNullOrWhiteSpace($status)) {
  Show-Fail "工作树无改动，没什么要提交的"
} else {
  $changedLines = ($status -split "`n").Count
  Show-Pass "$changedLines 个文件改动"
}

# ============================================================
# 2. ADR 不变量检查（禁用模式 grep）
# ============================================================
Show-Section "2. ADR 不变量未被违反"

# 仅检查本次 diff 中新增的内容（不检查全仓库）
$diff = git diff --cached origin/main -- ":(exclude)*.md" ":(exclude)CHANGELOG.md" 2>$null
if (-not $diff) {
  $diff = git diff origin/main -- ":(exclude)*.md" ":(exclude)CHANGELOG.md" 2>$null
}

if ($diff) {
  # 2.1 ADR-0003: 禁止硬编码颜色（仅检查 +号新增行）
  $addedLines = $diff -split "`n" | Where-Object { $_ -match '^\+[^+]' }
  $colorViolations = $addedLines | Where-Object {
    $_ -match '#[0-9a-fA-F]{3,8}\b' -and
    $_ -notmatch 'tokens\.' -and
    $_ -notmatch '^\+\+\+' -and
    $_ -notmatch '^\+\s*//' -and
    $_ -notmatch '^\+\s*\*'
  }
  if ($colorViolations) {
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

  $hasBackendChange = git diff origin/main --name-only -- "medkernel-mvp/" 2>$null
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

  $hasFrontendChange = git diff origin/main --name-only -- "frontend/" 2>$null
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
# 5. UTF-8 无 BOM
# ============================================================
Show-Section "5. UTF-8 无 BOM 检查"

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
  $section = ($pb -split "## \d+\. $TaskId" | Select-Object -Index 1)
  if ($section) {
    $next = ($section -split "\n## ")[0]
    if ($next -match '(?s)DoD 检查表\s*```markdown\s*([^`]+)```') {
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
  Write-Host "  3. git push origin <分支>"
  Write-Host "  4. 创建 ai-dev-input/11_ai_reviews/pending/<review_id>.md"
  Write-Host "  5. review APPROVED 后归档 claim/review"
  exit 0
}
