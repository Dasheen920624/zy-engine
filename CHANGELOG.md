# CHANGELOG

本文件记录所有客户可见的变更。版本号遵循 [Semantic Versioning 2.0](https://semver.org/)。

格式参考 [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/)。

> 维护约定见 [VERSIONING.md](VERSIONING.md)。

## [Unreleased]

### Added (2026-05-25) · GA-ENG-BASE 骨架

引擎一切之根：统一 API 契约 + 上下文 + 审计 + 5 方言 DDL 起点。后续 E1-E5 任务都将依赖本骨架。

- **shared/api/**：标准 API 包络。
  - `ApiResult<T>`：统一响应体（`success` / `code` / `message` / `data` / `errors` / `traceId` / `timestamp`），裸 POJO / Map 在 PR 检查阶段视作违反产品宪法第 7 条
  - `ApiError`：字段级错误明细，用于 Bean Validation 失败回传
  - `PageRequest` / `PageResponse`：服务端分页（默认 20、最大 200）
  - `CursorRequest` / `CursorResponse`：游标分页（默认 50、最大 500），用于审计日志、证据流、大规模搜索
- **shared/api/error/**：统一错误处理。
  - `ErrorCode` 枚举：业务错误码 + HTTP 状态映射，命名前缀 `ENG-API-*` / `ENG-BASE-*` / `ENG-SYS-*`
  - `ApiException`：业务异常基类，提供 `notFound` / `forbidden` / `conflict` / `tenantMissing` 等工厂方法
  - `GlobalExceptionHandler`：翻译 Bean Validation / Security / 业务异常 / 未捕获异常为 `ApiResult`，敏感细节不泄露给客户端
- **shared/trace/**：`TraceIdFilter` — `X-Trace-Id` 请求/响应头 + SLF4J MDC + `RequestContext` 三处同步，header 注入防御（仅允许字母数字短横线下划线 + 长度 128 内）
- **shared/context/**：`RequestContext` + `OrgScope`，承载 traceId / 组织六级树 / 用户 ID，提供 `runWith` / `callWith` 闭包，配合 Virtual Threads 一处请求一片上下文
- **shared/audit/**：`AuditAction` 枚举 + `AuditEvent` 记录 + `AuditEventPublisher`（基于 Spring `ApplicationEventPublisher`，内置 `LoggingAuditSink` 写入 audit logger），GA-ENG-BASE-04 实施时将增加 `AuditPersistenceSink` 落库到 `audit_event` 表
- **db/migration/{h2,postgres,oracle,dm,kingbase}/V2__org_audit_baseline.sql**：5 方言一致的 `org_unit`（六级组织树 + 状态/审计字段 + 唯一约束 + 检查约束）和 `audit_event`（操作留痕 + 4 类索引）DDL；Oracle/DM 字段 `level_code` 规避保留字
- **HealthController / RuntimeProbeController**：示范改造为 `ApiResult<PingResponse>` / `ApiResult<RuntimeProbeResponse>` + Record DTO，作为后续 30+ 业务 Controller 改造模板
- **测试**：29 项新单元 + 集成测试（ApiResult 序列化、PageResponse 边界、TraceId 头注入防御、GlobalExceptionHandler 5 类映射、RequestContext ThreadLocal 闭包、AuditEventPublisher 上下文传递、H2 V1+V2 Flyway 应用），均通过

### Added (2026-05-19)
- **分支策略与发布管理**（多 AI 并行 + main 永远稳定）：
  - 新增 [`docs/engineering/分支策略与发布管理.md`](docs/engineering/分支策略与发布管理.md)
  - 启用简化 Git Flow：`main`（稳定基线）+ `develop`（所有 AI 推到这里）+ `feature/<TASK-ID>/<slug>`（可选，大型变更）
  - `main` 仅接受 develop → main 的 PR；每次 merge 后立即打 `stable-YYYY-MM-DD-<short-hash>` tag
  - AI 任务分支命名 `ai/<TASK-ID>/<slug>`（与 02_任务台账 双向可查）
  - 已打基线 tag `stable-2026-05-19`（在 main HEAD = `5043d96`，Phase E 全部完成节点）
  - 已创建 `origin/develop` 分支（与 main 同步起点 = `5043d96`）
- **CI 升级**（`.github/workflows/ci.yml`）：
  - 项目名 `ZY Engine CI` → `MedKernel CI`
  - 触发分支：`main` 和 `develop` 都触发 build + test
  - PR to main 增加 `guard-rules` job：跑 `verify-pr.ps1 -SkipFrontend -SkipBackend`，校验禁用命名 / 路径断链 / UTF-8 / AI 协作
  - 从分支名 `ai/<TASK-ID>/<slug>` 或 `feature/<TASK-ID>/<slug>` 自动解析 TaskId

### Added (2026-05-18)
- **AI 一致性保证 7 套机制**（开工前最后基础设施）：
  - `docs/engineering/AI一致性保证.md` 总览
  - `docs/engineering/adr/` 架构决策记录 4 份（三产品分层、PR-V2 命名空间、禁止硬编码、医学必有来源）
  - `docs/engineering/reference-implementations/` 参考实现样板 4 份（StatusBadge / Controller / 6 状态页 / API 调用）
  - `docs/engineering/forbidden-patterns.md` 禁用清单（9 大类）
  - `frontend/eslint-rules/` 3 条自定义规则（no-hardcoded-color / require-source-info-for-medical / forbid-deprecated-naming）
  - `scripts/verify-task-prereq.ps1` 接手前 7 项自检
  - `scripts/verify-pr.ps1` 提交前 9 项 DoD 自检
  - `docs/engineering/AI能力分级匹配清单.md` 三档能力 × 12 PR 映射
  - `.github/pull_request_template.md` PR review 标准化清单

### Changed (Breaking - 2026-05-18)
- **CSS Token 命名空间 --zy-* → --mk-***：彻底统一为 mk- 命名空间，10 个文件 181 处替换。配套 ESLint 规则 `forbid-deprecated-naming` 加入 `--zy-` 和 `.zy-` 检测。
- **项目重命名 zy-engine → MedKernel**：V2 升级后定位是"三产品 + 三引擎 + 共用底座"的平台，原 "engine" 词不再准确。完整影响：
  - Java 包名：`com.zyengine.*` → `com.medkernel.*`（71 个 Java 类全部迁移）
  - Maven artifact：`zy-engine-mvp` → `medkernel-mvp`（含 groupId、artifactId、name）
  - 主工程目录：`zy-engine-mvp/` → `medkernel-mvp/`
  - Spring Boot 主类：`ZyEngineApplication.java` → `MedKernelApplication.java`
  - HTTP context path：`/zy-engine/api/*` → `/medkernel/api/*`
  - Spring application name：`zy-engine` → `medkernel`
  - 部署目录：`/zoesoft/zy-engine` → `/zoesoft/medkernel`（Linux）、`C:\zoesoft\medkernel`（Windows）
  - 环境变量：`ZY_HOME` → `MK_HOME`、`ZY_BACKUP_DIR` → `MK_BACKUP_DIR`、`ZY_USER` → `MK_USER`、`ZY_ENV_FILE` → `MK_ENV_FILE`、`ZYENGINE_*` → `MEDKERNEL_*`
  - 配置文件：`zyengine.env` → `medkernel.env`
  - systemd 服务：`zy-engine.service` → `medkernel.service`、Windows 服务名 `ZyEngine` → `MedKernel`
  - SQL 文件名：`zyengine_core_ddl_with_comments.sql` 等 7 个 → `medkernel_*` 同名
  - nginx 配置：`zy-engine.conf` / `zy-engine-tls.conf` → `medkernel.conf` / `medkernel-tls.conf`，upstream `zyengine_backend` → `medkernel_backend`
  - 前端 package name：`zy-engine-frontend` → `medkernel-frontend`
  - PowerShell 函数：`Import-ZyEngineOracleLocalEnv` → `Import-MedKernelOracleLocalEnv`
  - 总影响：229 文件 1900+ 处替换
  - **迁移指南：** 客户已部署版本需走升级流程（重新走 install-offline + 旧目录数据迁移），不可直接覆盖

### Removed
- **物理清理旧文件（2026-05-18，tag `pre-cleanup-20260518` 保留快照）：**
  - 删除 `docs/_archive/`（31 份历史文档：V1 草稿 1 + V2 之前主线 19 + 早期资料 11）
  - 删除 `frontend-prototype/`（8 份 HTML 静态原型，FE-002 React 工程已完整替代）
  - 删除 `medkernel-mvp/docs/README.md` 重定向页（已无引用风险）
  - 如需溯源：`git checkout pre-cleanup-20260518`

### Changed
- **文档体系 V2 + 物理重组（2026-05-18）：** 35 份历史文档冗余冲突，重做为"金本位 5 份 + 工程规范 15 份 + 归档 31 份"。
  - 新增项目根 `docs/` 作为唯一文档中心：
    - `docs/01_产品事实源.md` — 产品定义、三产品分层（A 配置工厂 + B 临床嵌入器 + C 质控驾驶舱）、9 角色×3 产品矩阵、6 大剧本、22 不变量
    - `docs/02_场景剧本图.md` — 6 大客户验收剧本精确 storyboard
    - `docs/03_设计系统.md` — Design Tokens + 13 核心组件 API + 三产品密度模式
    - `docs/04_页面规格书.md` — 18 个目标页面精确规格
    - `docs/05_AI实施手册.md` — 接下来 12 个 PR（PR-V2-01~12）精确清单
  - 工程规范迁移：`medkernel-mvp/docs/*.md` → `docs/engineering/`（15 份保留）
  - 历史归档：19 份废弃长文 + 1 份 V1 草稿 + 11 份早期资料 → `docs/_archive/`（全部加废弃 banner）
  - 项目根 README、ai-dev-input/README、各模块 README、deploy 引用路径同步更新
  - `medkernel-mvp/docs/README.md` 保留为重定向页防止断链
  - V2 PR 命名空间隔离：PR-V2-01~12 为权威主键，不复用历史 FE-XXX 编号避免冲突
- 任务台账新增 §2.5 V2 PR 优先级总览，V2 PR 优先级最高
- 05_架构总图新增 §1.1 三产品分层与架构关系

### Added
- 架构规范层（ARCH-001）：架构总图与服务边界、后端开发规范、前端开发规范、`.gitattributes`、ESLint flat config、Prettier。
- 国产化兼容矩阵（ARCH-002）：OS × CPU × JDK × DB 支持矩阵；达梦 DM 完整 DDL（含中文备注与组织上下文迁移）；PostgreSQL / KingbaseES DDL（PG 兼容模式）；数据库方言差异详表；已知坑实战记录。
- 自动化部署 + 版本管理（ARCH-003）：`deploy/` 目录（PowerShell + Bash 双套脚本：build-release / check-env / install-offline / upgrade / rollback / healthcheck）；systemd 服务文件；nginx 反代示例；4 个部署 profile env 模板；`VERSIONING.md` SemVer 约定；本 CHANGELOG。
- 内网管理台高保真原型（FE-001）：5 页静态 HTML（工作台 / 演示与校验 / 配置包中心 / 来源追溯 / 院级驾驶舱）。
- 前端工程脚手架（FE-002）：React 18 + TS 5 + Vite 5 + AntD 5 + react-router 6 + axios + TanStack Query + MSW 2 + vitest。
- 高密度索引文档：00 总入口、01 多角色诉求矩阵、02 任务台账、03 AI 能力分级与并行冲突规约、04 客户验收剧本与报告模板。
- 三类质控场景黄金样例（病历质控 / 医保质控 / 医嘱安全）与测试矩阵扩展。

### Changed
- 根 README 顶部置顶 "AI 首读" 唯一入口。
- `ai-dev-input/09_ai_task_cards/ai_system_prompt.md` 指向 00 总入口。
- **部署路径前缀统一**：从 `/opt/zy-engine` 改为 `/zoesoft/zy-engine`（Linux）；Windows 默认 `C:\zoesoft\zy-engine`。备份目录同步：`/zoesoft/zy-engine.bak`、`C:\zoesoft\zy-engine.bak`。环境变量 `ZY_HOME` / `ZY_BACKUP_DIR` 仍可覆盖默认值。**注：2026-05-18 后项目重命名为 MedKernel，部署路径变更为 `/zoesoft/medkernel`，环境变量变更为 `MK_HOME` / `MK_BACKUP_DIR`，详见本日 Changed 段。**

### Notes
- 当前版本未打 tag。下次 tag 建议 `v0.2.0`（理由：新增架构规范层 + 国产化矩阵 + 自动化部署框架，属于兼容性新增）。

## [0.1.0] - 2026-05-12（起点）

> 项目 MVP 阶段起点，回顾整理（非严格按 SemVer，归档便于追溯）。

### Added
- 路径引擎：候选 / 入径 / 节点流转 / 变异记录（PATH-001 ~ 007）
- 规则引擎：DSL / 包发布 / 第三方 evaluate / batch-evaluate / 结果回查 / 组织上下文（RULE-001 ~ 004）
- 图谱引擎：候选召回 / 证据查询 / Neo4j 降级
- Dify/AI：工作流模板 / 输入映射 / 降级输出 / 调用统计
- 标准化中心：医嘱 / 诊断 / 检验映射；未映射治理
- 适配器中心：REST / SQL / WebService 第三方查询定义
- 配置包：导入 / review / hash / publish / export / 审计（PKG-001 ~ 003）
- 组织：五段式上下文 + 组织目录 + Oracle 持久化（ORG-001 ~ 003）
- 审计：ENGINE_AUDIT_LOG + traceId
- DB：Oracle 完整 DDL + 达梦 DDL 草案 + Oracle smoke 落库验证（DB-ORG-001）
- 系统能力：Provider 状态探测 / 健康检查 / 多运行模式（DB_ONLY / HYBRID / FULL_INTEGRATION / IN_MEMORY_DEMO）

### Infrastructure
- JDK 1.8 + Spring Boot 2.7.18 + Maven
- PowerShell 5.1 / 7 build / test / smoke 脚本
- GitHub Actions CI（Windows runner + Temurin 8）
- UTF-8 + 中文备注 + Oracle UNISTR
