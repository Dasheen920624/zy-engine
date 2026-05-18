# 集团化医疗智能引擎平台（zy-engine）

> 让医院能像产品经理管理软件一样管理临床路径、规则和医学知识：版本化、可灰度、可追溯、可下线，每一次诊疗决策都能回到来源。

面向集团化医院 / 多院区 / 专科医联体，提供路径引擎、规则引擎、图谱引擎、Dify AI 工作流、字典标准化、适配器中心、质控审计能力的产品化解决方案。支持单医院、集团化医院、多院区、卫生所/站点等多组织形态，支持国产化部署（Oracle / 达梦 / PostgreSQL / KingbaseES）。

---

## 文档入口（任何人接手前先看这一份）

**👉 [docs/README.md](docs/README.md)** — 项目所有文档的唯一权威导航。

文档分三层：

| 层级 | 路径 | 内容 |
|---|---|---|
| **金本位**（产品+设计+实施） | [`docs/`](docs/) | 5 份核心：产品事实源 / 场景剧本图 / 设计系统 / 页面规格书 / AI 实施手册 |
| **工程规范**（架构+开发） | [`docs/engineering/`](docs/engineering/) | 架构总图 / 前后端规范 / 国产化适配 / 部署 / 任务台账 / 编码规范 |

> 历史归档已物理删除（2026-05-18，commit `e336bb8` 之后）。如需溯源旧文档（V1 草稿 / 19 份 V2 之前主线 / 11 份早期资料），请 `git checkout pre-cleanup-20260518` 回到清理前快照。

---

## 目录速查

| 目录 | 用途 |
|---|---|
| [`docs/`](docs/) | 所有文档（产品 + 工程 + 归档） |
| [`zy-engine-mvp/`](zy-engine-mvp/README.md) | 后端 MVP 工程（Spring Boot 2.7 / JDK 1.8） |
| [`frontend/`](frontend/README.md) | 前端工程（React 18 + TypeScript + Vite + Ant Design 5） |
| [`ai-dev-input/`](ai-dev-input/README.md) | AI 协作运行时（任务卡 / claim / review / 验收记录 / 演示数据 / DDL） |
| [`deploy/`](deploy/README.md) | 内网自动化部署（Linux *.sh / Windows *.ps1） |

---

## 快速开始

### 后端（首次启动）

```powershell
cd zy-engine-mvp
.\scripts\detect-db-env.ps1 -BootstrapLocal   # 自动选择 Oracle 或 LOCAL_H2
.\scripts\run-tests.cmd                        # 跑测试
.\scripts\build.cmd                            # 构建
.\scripts\start-memory.cmd                     # 启动（内存态）
```

健康检查：

```text
http://localhost:18080/zy-engine/api/health
http://localhost:18080/zy-engine/api/system/providers
```

### 前端（首次启动）

```bash
cd frontend
npm install
npm run dev
# 浏览器访问 http://localhost:5173
```

---

## 当前后端能力（已实现）

- 三大引擎主路径：路径（PATH-001~008）、规则（RULE-001~008）、图谱（GRAPH-001/003/004）
- 配置包统一模型 + 多数据库持久化（PKG-001~004）
- 5 段组织上下文与继承（ORG-001~004）
- 来源追溯（PROV-001~003）、字典映射（TERM-001/002）、Dify 适配（DIFY-001）
- 审计日志、健康检查、Provider 状态接口
- 跨数据库 DDL：Oracle / DM / PostgreSQL / KingbaseES / LOCAL_H2_FILE 五套同步维护

详细 API 见 [docs/04_页面规格书.md](docs/04_页面规格书.md) 与 [docs/engineering/api-examples.http](docs/engineering/api-examples.http)。

---

## 当前前端能力（已实现）

仅 4 个能用页面（脚手架阶段）：

- `/dashboard` 工作台首页（占位）
- `/system/providers` Provider 状态（接真实 API）
- `/config-packages` 配置包列表（部分功能）
- `/demo-validation` 演示与规则校验工作台（4 剧本 dry-run）

完整页面规划见 [docs/04_页面规格书.md](docs/04_页面规格书.md) 共 18 个目标页面。

下一步 12 个 PR 见 [docs/05_AI实施手册.md](docs/05_AI实施手册.md)。

---

## 产品核心原则（22 条不变量摘要）

完整不变量见 [docs/01_产品事实源.md §7](docs/01_产品事实源.md#7-22-个不变量任何-ai-不得违反)。

**红线（违反即停止）：**

- 不允许硬编码单医院 / 单院区 / 单系统厂商逻辑
- 不允许 Neo4j / Dify / 大模型成为测试环境强依赖（DB-only 模式必须可独立验收）
- 不允许配置静默覆盖已发布版本（同 code+version 内容不同 → 错误）
- 不允许关键医疗建议绕过医生确认
- 不允许医学/医保/质控配置缺来源直接发布（`MISSING_SOURCE` 必须阻断）
- 不允许发布/回滚/审核绕过审计
- 不允许日志输出密码 / API Key / 患者完整身份

**架构基线：**

- Oracle 是生产权威数据源；达梦 / PostgreSQL / KingbaseES 是国产化交付；LOCAL_H2_FILE 是开发本地库
- Neo4j 是图谱投影，Dify 是 AI 工作流执行目标，均为可降级 Provider
- 业务服务只调 Provider 接口，不直接依赖具体实现
- 医院差异通过组织范围、配置包、字典映射、适配器绑定实现
- 所有配置版本化、可审核、可发布、可回滚、可追溯

---

## AI 协作流程

本仓库面向**多 AI 并行开发**。AI 接手任务的标准流程：

```
1. 阅读 docs/README.md（5 分钟）
2. 按任务类型读对应金本位文档（15 分钟）
3. 读 docs/engineering/02_任务台账.md 找到任务行
4. 按 docs/engineering/00_总入口与AI接手导航.md 的硬门禁执行
5. 在 ai-dev-input/10_task_claims/active/ 创建认领
6. 开发 → 自检 → 在 ai-dev-input/11_ai_reviews/pending/ 创建评审
7. APPROVED 后 commit + push + 归档 claim/review
```

详细执行清单见 [docs/05_AI实施手册.md](docs/05_AI实施手册.md)。

---

## 维护

- 文档体系：[docs/README.md](docs/README.md)（V2 体系，2026-05-18 重构）
- 版本管理：[VERSIONING.md](VERSIONING.md)
- 变更日志：[CHANGELOG.md](CHANGELOG.md)
- 部署运维：[deploy/README.md](deploy/README.md)
