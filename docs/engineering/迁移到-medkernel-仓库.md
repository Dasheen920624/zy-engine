# 迁移到 medkernel 仓库（GitHub + 本地文件夹同步改名）

> 适用场景：把本地 `D:\aidev\claudecode\zy-engine\` 与 GitHub `Dasheen920624/zy-engine`  
> 双双改名为 `medkernel`。  
> 方案：**重新 clone**（最干净、零残留）。  
> 执行环境：**关闭 Claude Code 当前会话后**，在外部 PowerShell。

---

## 前置检查

### 1. 确认所有工作已推送

在 Claude Code 内（本会话）跑：

```powershell
cd "D:\aidev\claudecode\zy-engine\.claude\worktrees\dazzling-northcutt-cecf04"
git status -sb
# 应显示：## claude/... [up-to-date with origin]
# 如果 ahead 不为 0，先 push 再继续
```

### 2. 备份未跟踪的本地文件（可选）

如果你在本地仓库有未提交的私人配置文件（如 `.idea/`、IDE settings），先备份：

```powershell
Copy-Item "D:\aidev\claudecode\zy-engine\.idea" "D:\backup\zy-idea-bak-$(Get-Date -Format yyyyMMdd)" -Recurse -ErrorAction SilentlyContinue
```

---

## 迁移步骤

### Step 1：在 GitHub UI 改仓库名

1. 浏览器打开 https://github.com/Dasheen920624/zy-engine
2. 点 **Settings**（顶部菜单最右）
3. 在 **General** 段顶部 "Repository name" 字段
4. 把 `zy-engine` 改为 `medkernel`
5. 点 **Rename**

GitHub 会自动建立从旧 URL 到新 URL 的重定向（旧 clone 命令仍能工作，但建议更新）。

### Step 2：关闭 Claude Code 当前会话

⚠️ 必须做，否则 worktree 内有文件句柄锁住。

### Step 3：在外部 PowerShell 执行迁移脚本

打开**新的** PowerShell（不要用 Claude Code 内的）：

```powershell
# 跑外部迁移脚本
& "D:\aidev\claudecode\migrate-to-medkernel.ps1"
```

或者手动按顺序执行：

```powershell
# 1. 进入父目录
cd D:\aidev\claudecode

# 2. clone 新仓库（GitHub rename 后新 URL 立即可用）
git clone https://github.com/Dasheen920624/medkernel.git medkernel

# 3. 验证 clone 成功
cd medkernel
git log --oneline -5
# 应看到：
# 395f324 Phase C+D: AI 一致性 7 套机制 + CSS Token --zy- 彻底替换为 --mk-
# 868e978 Phase B: 项目重命名 zy-engine → MedKernel（229 文件 1900+ 处）
# 631f08c Phase A: 物理清理旧文件（tag pre-cleanup-20260518 已保留快照）
# e336bb8 开工前一致性核查：修复 3 阻断 + 4 警告
# 89f81c7 重组文档目录：金本位提升到项目根 docs/

# 4. 验证 tag
git tag
# 应看到：pre-cleanup-20260518

# 5. 验证文档结构
Get-ChildItem docs -Recurse -Filter "*.md" | Measure-Object
# 应有 27 个 .md 文件

# 6. 验证项目结构
Get-ChildItem -Directory
# 应看到：ai-dev-input, deploy, docs, frontend, medkernel-mvp, scripts

# 7. 确认 OK 后删除旧目录
cd D:\aidev\claudecode
Remove-Item zy-engine -Recurse -Force
```

### Step 4：通知 AI 团队

新 clone URL：`https://github.com/Dasheen920624/medkernel.git`  
本地工作目录：`D:\aidev\claudecode\medkernel\`

更新所有 AI 的接手指引中的路径（如 Claude Code 项目配置）。

---

## 验证清单

迁移完成后，在 `D:\aidev\claudecode\medkernel\` 内验证：

- [ ] `git remote -v` 显示 origin 指向 `Dasheen920624/medkernel.git`
- [ ] `git log --oneline -5` 显示最新 5 个 commit
- [ ] `git tag` 含 `pre-cleanup-20260518`
- [ ] `docs/README.md` 存在
- [ ] `docs/01_产品事实源.md` ~ `docs/05_AI实施手册.md` 存在
- [ ] `medkernel-mvp/src/main/java/com/medkernel/MedKernelApplication.java` 存在
- [ ] `scripts/verify-task-prereq.ps1` 和 `verify-pr.ps1` 存在
- [ ] `frontend/eslint-rules/` 含 3 个 .js 文件
- [ ] `docs/engineering/adr/` 含 4 份 ADR

---

## 回滚（如果出问题）

如果 clone 出问题或验证失败：

```powershell
# 删掉新 clone
Remove-Item D:\aidev\claudecode\medkernel -Recurse -Force

# 旧 zy-engine 仍在原地，继续用即可
cd D:\aidev\claudecode\zy-engine

# 如需把 GitHub rename 改回：
# 在 GitHub Settings → Rename 为 zy-engine（GitHub 允许改回，原 URL 自动重定向）
```

---

## 其它需要更新的地方（迁移后）

| 项 | 是否需要 |
|---|---|
| Claude Code 项目配置（路径绑定） | ✅ 需要（路径改了） |
| `.workbuddy/memory/*.md` 内引用旧路径 | ⚠ 可选（工具内部记忆，不影响业务） |
| IDE 项目（IntelliJ / VSCode） | ✅ 需要（重新打开新路径） |
| CI/CD 中如有硬编码本地路径 | ✅ 需要（但项目用 GitHub Actions，不依赖本地路径） |
| 个人 shell 别名（如有 `cd zy`） | ✅ 需要 |
| `.gitignore` 中如有路径相关 | ❌ 无（已检查） |

---

## 安全保障

- GitHub rename 后**自动建立从旧 URL 到新 URL 的重定向**，旧 clone 命令仍能工作（持续若干年）
- 远端 `pre-cleanup-20260518` tag 已推送，作为恢复点
- 主分支 main 已稳定，最新 commit `395f324`
- 所有 AI 协作运行时数据（claim/review/feature_acceptance）已随主仓库迁移
