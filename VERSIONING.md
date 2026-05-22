# 版本管理约定

## 1. 版本号格式

```
MAJOR.MINOR.PATCH[-prerelease][+build]
例：1.0.0
    1.2.0-rc.1
    2.0.0+a1b2c3d
```

遵循 [Semantic Versioning 2.0](https://semver.org/)。

| 段 | 含义 | 变更示例 |
|---|---|---|
| MAJOR | 不兼容变更 | API 删除、数据库表删除、字段含义变更、Provider 接口签名 break |
| MINOR | 向后兼容新增 | 新模块、新接口、新字段、新页面、新规则场景 |
| PATCH | bug 修复 / 内部优化 | 不改 API 与 DDL 的修复 |
| `-rc.N` / `-beta.N` | 预发布 | UAT 阶段 |
| `+gitShortHash` | 构建标识 | CI 自动注入 |

> 详细发布流程见 [docs/engineering/09_内网部署与版本管理.md](docs/engineering/09_内网部署与版本管理.md)。

## 2. 分支约定

```
main                       稳定发布分支，永远可发布；仅接受 develop → main PR
develop                    日常集成分支；AI 小变更可直接 push，大变更先开任务分支
ai/<TASK-ID>/<slug>        AI 任务分支，基于 origin/develop 创建，完成后 PR 或 push → develop
feature/<TASK-ID>/<slug>   高风险/大型任务分支，完成后 PR → develop
release/X.Y                长期支持分支（重大版本进入维护期后）
hotfix/X.Y.Z               紧急修复分支（从 main 或 release/X.Y 分出，修完回合）
```

**禁用**：AI 直接 push `main`、feature 分支与 `develop` 长期分叉、绕过 CI 把未验收变更合入 `main`。完整规则以 [docs/engineering/分支策略与发布管理.md](docs/engineering/分支策略与发布管理.md) 为准。

## 3. 打 tag 流程

```bash
# 1) 确认 main 干净
git fetch origin
git checkout main
git pull --ff-only

# 2) 更新 CHANGELOG（把 Unreleased 改为 [X.Y.Z] - YYYY-MM-DD）
$EDITOR CHANGELOG.md
git add CHANGELOG.md
git commit -m "标注 v1.2.3 CHANGELOG"

# 3) 同步 pom.xml 与 frontend/package.json 版本号
# 后端：sed -i 's|<version>.*</version>|<version>1.2.3</version>|' medkernel-mvp/pom.xml
# 前端：npm version 1.2.3 --no-git-tag-version --prefix frontend
git add medkernel-mvp/pom.xml frontend/package.json
git commit -m "升版 v1.2.3"

# 4) 打 annotated tag
git tag -a v1.2.3 -m "v1.2.3 - 病历质控样例 + FE-003 演示工作台"

# 5) 推送 tag
git push origin main --tags

# CI 监听 tag 自动构建发布包，产物归档到 nexus/oss
```

## 4. 何时升 MAJOR / MINOR / PATCH

### 4.1 MAJOR（破坏性）

- `/api/xxx` 端点删除或 path 变更
- ApiResult 字段命名变更
- DDL 删表 / 删列 / 列类型缩小
- Provider 接口签名删除方法或改参数
- 配置文件字段含义变更（旧值不再被读取）
- 数据库主键策略变更

每次 MAJOR 升级必须：

- 在 [09_内网部署与版本管理.md](docs/engineering/09_内网部署与版本管理.md) §6 补"升级注意事项"
- 给客户/医院信息科发"破坏性变更通知" ≥ 2 周前
- 提供等长支持窗口（旧 MAJOR 至少再支持 6 个月）

### 4.2 MINOR（兼容新增）

- 新增 controller / 端点
- 新增 ApiResult 字段（可选 / 默认）
- 新增 DDL 列（NULLable 或有 default）
- 新增模块（PROV / SEC 子模块）
- 新增前端页面
- 新增规则场景 / 新增样例

### 4.3 PATCH（修复）

- 修 bug，不改 API / DDL / 字段含义
- 性能优化
- 日志改善 / 注释改善
- 依赖小升（不升 MAJOR）

## 5. 版本号注入

### 5.1 后端

`pom.xml`：

```xml
<version>1.2.3</version>
```

Maven `git-commit-id-plugin`（已通过 properties 注入）→ `application.yml`：

```yaml
medkernel:
  build:
    version: @project.version@
    git-hash: ${git.commit.id.abbrev}
    build-time: ${git.build.time}
```

`/api/system/version` 返回：

```json
{
  "version": "1.2.3",
  "git_hash": "a1b2c3d",
  "build_time": "2026-05-17T08:30:00+08:00",
  "spring_boot": "2.7.18",
  "jdk": "1.8.0_402"
}
```

### 5.2 前端

`frontend/package.json`：

```json
{ "version": "1.2.3" }
```

Vite `define`（vite.config.ts）：

```ts
define: {
  __APP_VERSION__: JSON.stringify(process.env.npm_package_version),
  __GIT_HASH__: JSON.stringify(process.env.GIT_HASH || "dev"),
}
```

页面页脚显示 `v1.2.3-a1b2c3d`。

### 5.3 发布包

`manifest.json`：

```json
{
  "version": "1.2.3",
  "git_hash": "a1b2c3d",
  "build_time": "2026-05-17T08:30:00+08:00",
  "build_host": "ci-builder-1",
  "components": {
    "backend": { "jar": "lib/medkernel.jar", "sha256": "..." },
    "frontend": { "dist": "frontend/dist/", "sha256": "..." },
    "db_oracle": { "version": "1.2.3" },
    "db_dm": { "version": "1.2.3" },
    "db_postgres": { "version": "1.2.3" }
  },
  "supported_os": ["centos7-x86_64", "uos-aarch64", "kylin-aarch64", "windows-x86_64"]
}
```

## 6. CHANGELOG 约定

详见 [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/)。

文件：`CHANGELOG.md`，每次 commit 到 main **不强制**改，但打 tag 前必须更新。

模板：

```markdown
## [Unreleased]

### Added
- 新增什么

### Changed
- 改了什么

### Deprecated
- 即将废弃什么

### Removed
- 删了什么

### Fixed
- 修了什么

### Security
- 安全相关

## [1.2.3] - 2026-05-17

### Added
- FE-003 演示与规则校验工作台落地
...
```

## 7. 与 main / develop 协作的注意

2026-05-19 起采用 `main + develop` 双线：

- AI 日常任务基于 `origin/develop` 开工，完成后推送 `develop` 或先开 `ai/<TASK-ID>/<slug>` / `feature/<TASK-ID>/<slug>`。
- `main` 只接受用户 approve 且 CI PASS 的 `develop → main` PR。
- 如果 GitHub 提示 `develop → main` 不能合并，先检查 `origin/main` 是否为 `origin/develop` 祖先、PR checks 是否通过、`guard-rules` 是否正确以 `origin/main` 为 diff base。

但 **打 tag 必须由人执行**（不允许 AI 自动 tag），因为：

- tag 是面向外部客户的公告点。
- CHANGELOG 的"用户视角描述"需人审。
- pom.xml / package.json 版本号变更影响构建产物名。

AI 完成一组功能后建议：

- 在最终回复中提示"建议下一次 tag：vX.Y.Z（理由：xxx）"。
- 不自行 `git tag`。

## 8. 分支保护规则（GA-REL-01）

### 8.1 main 分支保护

| 规则 | 要求 |
|---|---|
| 直接 push | 禁止。仅接受 `develop → main` PR |
| PR 审批 | 至少 1 人 approve（非 AI） |
| CI 检查 | 全部 PASS（backend-build-test、guard-rules、ai-collaboration-guard） |
| 合并策略 | Merge commit（保留完整历史） |
| force push | 禁止 |
| 删除 | 禁止 |

GitHub 仓库设置路径：Settings → Branches → Branch protection rules → `main`

### 8.2 develop 分支保护

| 规则 | 要求 |
|---|---|
| 直接 push | 允许（AI 任务分支合并后推送） |
| PR 审批 | 不强制（但建议 AI 任务通过 PR 合入） |
| CI 检查 | 全部 PASS |
| 合并策略 | Merge commit 或 Squash merge |
| force push | 禁止 |
| 删除 | 禁止 |

### 8.3 AI 任务分支规范

| 规则 | 要求 |
|---|---|
| 命名 | `ai/<TASK-ID>/<slug>`（如 `ai/GA-REL-01/release-protection`） |
| 基线 | 必须基于 `origin/develop` 创建 |
| 合入 | 完成后 merge 到 `develop`，不直接 push `main` |
| 清理 | 合并后可删除任务分支 |

### 8.4 发布路径

```
ai/<TASK-ID>/<slug> → develop → main → tag v* → Release Evidence
```

1. AI 在任务分支开发，完成后合并到 develop
2. 用户发起 `develop → main` PR，CI 全部 PASS 后 approve 合入
3. 用户在 main 上打 tag `vX.Y.Z`
4. CI 自动触发 Release Evidence 工作流（`.github/workflows/release.yml`）
5. 工作流生成 release manifest 并创建 GitHub Release

## 9. Release Evidence（GA-REL-01）

### 9.1 自动化流程

当 `v*` tag 推送到 GitHub 时，`release.yml` 工作流自动执行：

1. **校验 tag 格式**：确认符合 semver（`vMAJOR.MINOR.PATCH[-prerelease][+build]`）
2. **校验 tag 基线**：确认 tag commit 在 main 分支历史线上
3. **校验分支保护**：确认 main 是 develop 的祖先
4. **构建后端**：`mvn package` 生成 JAR
5. **计算 SHA256**：对 JAR 计算哈希值
6. **生成 release manifest**：包含版本号、git hash、构建时间、组件 SHA256
7. **创建 GitHub Release**：附带 manifest 和自动生成的 changelog

### 9.2 Release Manifest 格式

```json
{
  "version": "1.0.0",
  "git_hash": "a1b2c3d",
  "build_time": "2026-05-23T20:00:00+08:00",
  "build_host": "github-ci",
  "tag": "v1.0.0",
  "components": {
    "backend": {
      "jar": "lib/medkernel.jar",
      "sha256": "..."
    }
  },
  "branch_protection": {
    "main": "protected: only develop->main PR allowed",
    "develop": "integration: AI task branches merge here"
  }
}
```

### 9.3 Tag 校验脚本

打 tag 前可运行校验脚本确认合规：

```powershell
pwsh scripts/verify-tag.ps1 -Tag v1.0.0
pwsh scripts/verify-tag.ps1 -Tag v1.0.0 -Strict  # 严格模式，任何问题抛异常
```

校验项：
1. tag 格式符合 semver
2. tag commit 存在
3. tag 基于 main 分支
4. develop 包含 tag 的所有 commit
5. VERSIONING.md 存在且包含版本策略
6. CHANGELOG.md 存在且包含该版本条目

### 9.4 CI 中的分支保护执行

`ci.yml` 中的 `guard-rules` job 已实现以下保护：

- PR 到 main 必须来自 develop（不允许 AI 直接 PR 到 main）
- PR 到 develop 不允许从 main 合回
- AI 任务分支命名校验（`ai/<TASK-ID>/<slug>`）
- verify-pr.ps1 执行 DoD 自检

## 10. 演进

修改本文属于 DOC 任务。版本号格式变更需独立 ARCH 任务讨论。
