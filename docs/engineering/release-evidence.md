# GA-REL-01: 发布与分支保护证据

> 版本：v1.0 GA | 最后更新：2026-05-23

## 1. 分支保护规则

### 1.1 main 分支（发布分支）

| 规则 | 配置 |
|------|------|
| 直接 push | 禁止 |
| 合并来源 | 仅接受 develop -> main PR |
| 审核人数 | 至少 2 人 |
| Code Owner 审核 | 必须 |
| 过期审核自动清除 | 是 |
| CI 状态检查 | backend-build-test, frontend-build-test, guard-rules, ai-collaboration-guard |
| 严格模式（分支必须最新） | 是 |
| 管理员限制 | 是（管理员也受保护规则约束） |

### 1.2 develop 分支（集成分支）

| 规则 | 配置 |
|------|------|
| 直接 push | 禁止 |
| 合并来源 | AI 任务分支（ai/*）和 feature 分支（feature/*） |
| 审核人数 | 至少 1 人 |
| CI 状态检查 | backend-build-test, frontend-build-test, guard-rules, ai-collaboration-guard |
| 严格模式 | 否 |
| 管理员限制 | 否 |

### 1.3 配置文件

分支保护规则定义在 `.github/branch-protection.json`，可通过 GitHub API 或管理界面应用：

```powershell
# 应用分支保护规则（需要 GitHub admin 权限）
gh api repos/{owner}/{repo}/branches/main/protection \
  --method PUT \
  --input .github/branch-protection.json
```

## 2. 发布流程

### 2.1 发布步骤

```
develop ──PR──> main ──tag──> v1.0.0 ──CI──> GitHub Release
```

1. **准备发布**：在 develop 分支上完成所有功能开发和测试
2. **创建 PR**：从 develop 向 main 创建 Pull Request
3. **审核合并**：至少 2 人审核 + CI 全绿后合并
4. **打 tag**：在 main 分支上打语义化版本 tag（如 `v1.0.0`）
5. **自动发布**：tag 推送触发 `release.yml`，自动生成 release evidence

### 2.2 发布脚本

```powershell
# 完整发布流程
.\scripts\release.ps1 -Version 1.0.0

# 干跑模式（只打印不执行）
.\scripts\release.ps1 -Version 1.0.0 -DryRun
```

### 2.3 Tag 校验

```powershell
# 校验 tag 完整性
.\scripts\verify-tag.ps1 -Tag v1.0.0

# 严格模式（任何问题均阻断）
.\scripts\verify-tag.ps1 -Tag v1.0.0 -Strict
```

## 3. Release Evidence

### 3.1 自动生成的证据

当 `v*` tag 推送时，`release.yml` 自动执行：

| 步骤 | 产出 | 保留期 |
|------|------|--------|
| 校验 tag 格式 | semver 校验结果 | CI 日志 |
| 校验 tag base | main 分支校验结果 | CI 日志 |
| 校验分支保护 | main/develop 保护规则校验 | CI 日志 |
| 构建 backend | JAR + SHA256 | 90 天 |
| 生成 manifest | release-manifest.json | 90 天 |
| 生成 changelog | git log 自动生成 | 永久 |
| 创建 GitHub Release | Release + manifest 附件 | 永久 |

### 3.2 Release Manifest 格式

```json
{
  "version": "1.0.0",
  "git_hash": "abc1234",
  "build_time": "2026-05-23T10:00:00+08:00",
  "build_host": "github-ci",
  "tag": "v1.0.0",
  "components": {
    "backend": {
      "jar": "lib/medkernel.jar",
      "sha256": "abcdef1234567890..."
    }
  },
  "branch_protection": {
    "main": "protected: only develop->main PR allowed",
    "develop": "integration: AI task branches merge here"
  }
}
```

## 4. 版本策略

### 4.1 语义化版本

遵循 [Semantic Versioning 2.0.0](https://semver.org/)：

- **MAJOR**：不兼容的 API 变更
- **MINOR**：向后兼容的功能新增
- **PATCH**：向后兼容的问题修复

### 4.2 预发布版本

- `v1.0.0-alpha.1`：内部测试
- `v1.0.0-beta.1`：外部测试
- `v1.0.0-rc.1`：发布候选

### 4.3 版本策略文档

详见 `VERSIONING.md`。

## 5. CI 门禁

### 5.1 合并到 develop 的门禁

| 检查项 | 说明 |
|--------|------|
| backend-build-test | 后端编译 + 测试 + Jacoco 覆盖率 |
| frontend-build-test | 前端 lint + typecheck + test + coverage + build |
| guard-rules | 分支策略 + 文件命名规则 |
| ai-collaboration-guard | orphan lock + 重复 task + write_scope 重叠 |

### 5.2 合并到 main 的门禁

在 develop 门禁基础上增加：
- 至少 2 人审核
- Code Owner 审核
- 严格模式（分支必须最新）

## 6. 合规证据

### 6.1 可追溯性

每次发布可追溯：
1. **代码变更**：git log + PR 记录
2. **审核记录**：GitHub PR review
3. **CI 结果**：GitHub Actions 运行记录
4. **构建产物**：JAR SHA256 + release manifest
5. **发布时间**：GitHub Release 创建时间

### 6.2 不可篡改性

- main 分支保护禁止 force push
- tag 一旦推送不可修改（需管理员权限删除）
- GitHub Release 附件不可修改
- CI 运行记录不可修改

### 6.3 审计证据

发布审计证据包括：
- `.github/branch-protection.json`：分支保护规则配置
- `.github/workflows/release.yml`：发布工作流定义
- `.github/workflows/ci.yml`：CI 门禁定义
- `scripts/release.ps1`：发布脚本
- `scripts/verify-tag.ps1`：tag 校验脚本
- `release-manifest.json`：每次发布的构建清单
- GitHub Release 页面：发布记录和附件
