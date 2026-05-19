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
main           主分支，永远可发布；fast-forward 合并
claude/<任务>  worktree 临时分支（多 AI 并行）
release/X.Y    长期支持分支（重大版本进入维护期后）
hotfix/X.Y.Z   紧急修复分支（从 release/X.Y 分出，修完合并回 main + release/X.Y）
```

**禁用**：feature 分支与 main 长期分叉；GitFlow 复杂模型在本项目过度。

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

## 7. 与 main 协作的注意

按 memory `feedback_auto_push_main`：完成任务后直接 ff push main 不再询问。

但 **打 tag 必须由人执行**（不允许 AI 自动 tag），因为：

- tag 是面向外部客户的公告点。
- CHANGELOG 的"用户视角描述"需人审。
- pom.xml / package.json 版本号变更影响构建产物名。

AI 完成一组功能后建议：

- 在最终回复中提示"建议下一次 tag：vX.Y.Z（理由：xxx）"。
- 不自行 `git tag`。

## 8. 演进

修改本文属于 DOC 任务。版本号格式变更需独立 ARCH 任务讨论。
