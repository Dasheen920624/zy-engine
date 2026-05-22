# MedKernel 分支保护与发布流程

> 版本：1.0 | 适用：v1.0 GA | 日期：2026-05-23

## 1. 分支策略

### 1.1 分支模型

| 分支 | 用途 | 保护级别 | 合并来源 |
|------|------|---------|---------|
| `main` | 生产发布，每个 commit 对应一个正式版本 | 最高 | 仅从 `develop` 合并 |
| `develop` | 开发集成分支，所有功能最终合入 | 高 | 从 `feature/*` / `hotfix/*` 合并 |
| `feature/*` | 功能开发分支 | 无 | 从 `develop` 拉出 |
| `hotfix/*` | 紧急修复分支 | 无 | 从 `main` 拉出 |
| `release/*` | 发布准备分支 | 无 | 从 `develop` 拉出 |

### 1.2 分支命名规范

```
feature/GA-XXX-简短描述    # 功能开发
hotfix/GA-XXX-简短描述     # 紧急修复
release/v1.0.x             # 发布准备
```

## 2. 分支保护规则

### 2.1 `main` 分支保护

| 规则 | 设置 | 说明 |
|------|------|------|
| Require pull request | ✓ | 禁止直接 push |
| Required approving reviews | 2 | 至少 2 人审核通过 |
| Dismiss stale reviews | ✓ | 新 push 后自动清除旧审核 |
| Require status checks | ✓ | CI 全部通过 |
| Require branches to be up to date | ✓ | 合并前必须 rebase |
| Required status checks | `backend-build-test`, `frontend-build-test`, `guard-rules` | 3 个 CI job 必须通过 |
| Require signed commits | ✓ | GPG 签名 |
| Include administrators | ✓ | 管理员也受保护规则约束 |
| Allow force pushes | ✗ | 禁止 force push |
| Allow deletions | ✗ | 禁止删除分支 |

### 2.2 `develop` 分支保护

| 规则 | 设置 | 说明 |
|------|------|------|
| Require pull request | ✓ | 禁止直接 push（AI 协作除外） |
| Required approving reviews | 1 | 至少 1 人审核通过 |
| Require status checks | ✓ | CI 全部通过 |
| Required status checks | `backend-build-test`, `frontend-build-test`, `guard-rules` | 3 个 CI job 必须通过 |
| Allow force pushes | ✗ | 禁止 force push |
| Allow deletions | ✗ | 禁止删除分支 |

### 2.3 AI 协作特殊规则

根据 AI_TEAM_SOP，AI 开发者可以直接 push 到 `develop` 分支（无需 PR），
但必须：
1. 持有有效的 task claim + lock
2. 修改范围在 write_scope 内
3. CI 通过（guard-rules 门禁）
4. 不修改其他 AI 的 write_scope

## 3. 发布流程

### 3.1 版本号规范

采用语义化版本（SemVer）：`MAJOR.MINOR.PATCH`

- MAJOR：不兼容的 API 变更
- MINOR：向后兼容的功能新增
- PATCH：向后兼容的 Bug 修复

v1.0 GA 起始版本：`1.0.0`

### 3.2 发布步骤

```
1. 确认 develop 分支所有 GA-* 任务完成
2. 创建 release 分支：git checkout -b release/v1.0.0 develop
3. 更新版本号（pom.xml, package.json, CHANGELOG.md）
4. 执行全量回归测试
5. 合并到 main：git checkout main && git merge --no-ff release/v1.0.0
6. 打 tag：git tag -s v1.0.0 -m "MedKernel v1.0.0 GA Release"
7. 合并回 develop：git checkout develop && git merge --no-ff release/v1.0.0
8. 推送：git push origin main develop --tags
9. 创建 GitHub Release（附 CHANGELOG + 构建产物）
10. 归档 release evidence
```

### 3.3 Tag 规范

| Tag 格式 | 说明 | 示例 |
|----------|------|------|
| `vMAJOR.MINOR.PATCH` | 正式发布 | `v1.0.0` |
| `vMAJOR.MINOR.PATCH-rc.N` | 发布候选 | `v1.0.0-rc.1` |
| `vMAJOR.MINOR.PATCH-beta.N` | 公开测试 | `v1.0.0-beta.1` |

Tag 必须使用 GPG 签名（`git tag -s`）。

## 4. Release Evidence

每次正式发布必须归档以下证据：

### 4.1 必须归档

| 证据项 | 来源 | 存放位置 |
|--------|------|---------|
| Git tag | `git tag -v v1.0.0` | GitHub Release |
| CI 构建日志 | GitHub Actions run | `docs/release/evidence/v1.0.0/` |
| 测试报告 | CI 产物 | `docs/release/evidence/v1.0.0/` |
| 覆盖率报告 | CI 产物 | `docs/release/evidence/v1.0.0/` |
| CHANGELOG | `CHANGELOG.md` | 仓库根目录 |
| 分支保护截图 | GitHub Settings | `docs/release/evidence/v1.0.0/` |

### 4.2 Release Evidence 模板

```markdown
# Release Evidence: v1.0.0

## 基本信息
- 版本：v1.0.0
- 发布日期：YYYY-MM-DD
- 发布人：XXX
- Git Tag：v1.0.0 (GPG signed)
- Git Commit：xxxxxxxx

## 质量门禁
- [ ] CI 全部通过（backend-build-test, frontend-build-test, guard-rules）
- [ ] 后端覆盖率 ≥ 70%
- [ ] 前端覆盖率 ≥ 60%
- [ ] 所有 GA-* 任务 DONE
- [ ] E2E 测试全部 PASS
- [ ] 安全扫描无高危漏洞

## 分支保护验证
- [ ] main 分支保护规则已启用
- [ ] develop 分支保护规则已启用
- [ ] force push 已禁用
- [ ] 分支删除已禁用
- [ ] 签名提交已要求

## 变更范围
- GA 任务完成数：XX/XX
- 新增功能：...
- 修复缺陷：...
- 破坏性变更：无

## 附件
- CI 构建日志：[链接]
- 测试报告：[链接]
- 覆盖率报告：[链接]
```

## 5. 验证方法

### 5.1 分支保护验证脚本

```bash
./deploy/scripts/verify-branch-protection.sh
```

### 5.2 Tag 验证

```bash
# 验证 tag 签名
git tag -v v1.0.0

# 验证 tag 指向的 commit
git show v1.0.0 --no-patch
```

## 6. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-05-23 | 1.0 | 初始版本：分支保护规则、发布流程、Release Evidence 模板 |
