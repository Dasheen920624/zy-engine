# AI 任务卡模板

使用前按最小阅读策略执行：

- 必读：`zy-engine-mvp/docs/00_总入口与AI接手导航.md`
- 只读任务行：`zy-engine-mvp/docs/02_任务台账.md`
- 按任务触发条件最多补读 1 份专项文档：后端 `06_后端开发规范.md`，前端 `07_前端开发规范.md`，数据库 `数据库Provider与离线AI开发约定.md`，架构 `05_架构总图与服务边界.md`，部署 `09_内网部署与版本管理.md`，医学/医保/质控 `产品功能业务核查与开工清单.md`。

不要把长文档列表当作默认必读清单；`00_总入口与AI接手导航.md` 的硬门禁和 DoD 是默认权威口径。

## 任务认领

开发前必须先创建并推送 claim 文件：

```text
ai-dev-input/10_task_claims/active/<claim_id>.md
```

填写：

```text
claim_id:
owner:
status: ACTIVE
write_scope:
forbidden_scope:
verification:
review_required:
review_id:
review_status:
open_findings:
```

没有成功推送 active claim 前，只允许阅读和规划。

若是自主开发，还必须创建或更新：

```text
ai-dev-input/12_autonomous_runs/active/<run_id>.md
```

完成开发后必须创建质量评审：

```text
ai-dev-input/11_ai_reviews/pending/<review_id>.md
```

未通过 `review_status=APPROVED` 且 `open_findings=0` 前，不允许正式提交、合并或进入主版本。

## 任务编号

例如：`PKG-001`、`ORG-001`、`TERM-001`、`RULE-001`、`FE-001`。

编号前缀约定：

```text
ORG-xxx     组织模型与租户隔离
PKG-xxx     配置包和发布生命周期
TERM-xxx    标准化和字典治理
RULE-xxx    规则引擎
PATH-xxx    路径引擎
GRAPH-xxx   图谱引擎
DIFY-xxx    Dify/AI 工作流
ADAPT-xxx   第三方适配器
QC-xxx      质控指标
PROV-xxx    来源、证据、引用和可追溯性
FE-xxx      前端配置、演示校验和可视化验收
AUDIT-xxx   审计日志
OPS-xxx     运维部署
SEC-xxx     安全权限
TEST-xxx    测试与验收
DOC-xxx     文档
```

## 任务名称

一句话说明本任务要交付什么。

## 业务目标

说明该任务解决哪个业务问题，服务哪个场景：

- 路径入径
- 病历质控
- 医保质控
- 医嘱安全
- 图谱证据
- Dify 解释
- 跨院配置
- 功能演示
- 规则校验
- 病历质控可视化
- 医保质控可视化
- 运维审计

必须填写：

```text
目标角色：
业务闭环：
客户验收故事线：
```

## 总控泳道

填写本任务属于 `顶级多角色评审与AI并行开发总控.md` 中哪条泳道：

```text
A 配置包与发布治理 / B 组织与权限 / C 来源追溯与医学可信 / D 规则质控和医嘱安全 / E 路径图谱Dify适配器 / F 前端配置和客户验收 / G 测试运维和安全
```

说明：

- 本任务 claim_id：
- 自主运行 run_id：
- 数据库模式：ORACLE / DM / POSTGRES / KINGBASE / LOCAL_H2 / IN_MEMORY
- 数据库角色：PRODUCTION_AUTHORITY / PRODUCTION_COMPATIBLE / DEVELOPMENT_LOCAL / IN_MEMORY_DEMO
- Oracle 是否可用：
- 本地 H2 是否已验证：
- 本任务写入范围：
- 明确不改范围：
- 是否可能和其它 AI 冲突：
- 目标用户角色：

## 适用组织范围

填写：

```text
系统内置默认（产品基线配置） / 集团 / 医院 / 院区 / 卫生所或站点 / 科室
```

说明是否涉及：

- `tenant_id`
- `group_code`
- `hospital_code`
- `campus_code`
- `site_code`
- `department_code`

## 所属模块

路径引擎 / 规则引擎 / 图谱引擎 / Dify 工作流 / 标准化中心 / 适配器中心 / 配置包 / 前端配置平台 / 演示校验工作台 / 审计运维 / 安全权限 / 测试文档。

## 背景

说明为什么需要这个任务，以及和哪些业务流程、配置包、外部系统相关。

## 输入资料

- 产品总纲：
- OpenAPI 文件：
- JSON Schema 文件：
- DDL 文件：
- 样例数据：
- 测试用例：
- 相关接口：
- 相关历史提交：
- 来源资料：
- 引用片段：

## 功能要求

1.
2.
3.

## 非功能要求

必须逐项确认：

1. 支持 `traceId`。
2. 使用统一 `ApiResult`。
3. 关键操作有审计。
4. DB-only 模式可运行。
5. Neo4j/Dify/第三方系统不可用时有降级策略。
6. 不硬编码医院逻辑。
7. 配置有版本、状态、review/publish/rollback 设计。
8. 数据库访问考虑 Oracle/达梦/PostgreSQL-Kingbase 生产兼容，并区分 LOCAL_H2_FILE 开发库。
9. 日志不输出密钥和患者完整隐私明文。
10. 规则、知识、图谱证据、Dify解释、字典映射、适配器口径和质控结论必须可查来源。

## 数据模型变更

说明：

- 新增表：
- 新增字段：
- 索引：
- 生产库 DDL（Oracle/达梦/PostgreSQL-Kingbase）：
- 开发库 DDL（LOCAL_H2_FILE）：
- Oracle/达梦/PostgreSQL-Kingbase 差异：
- H2 本地库差异：
- 是否需要迁移脚本：
- 是否必须同步真实生产库：
- 无生产库时是否必须用 LOCAL_H2_FILE 验证：
- 生产库 smoke 脚本：

## 来源追溯影响

说明：

- 是否新增来源文档：
- 是否新增引用片段：
- 是否新增资产来源绑定：
- 是否影响发布来源检查：
- 是否影响运行结果 provenance：
- 来源缺失时如何阻断：
- 来源过期时如何处理：
- 医学/医保/质控审核角色：

## 接口变更

说明：

- 新增接口：
- 修改接口：
- 请求示例：
- 响应示例：
- 错误码：

## 前端页面变更

若任务涉及前端，说明：

- 新增或修改页面：
- 菜单和路由：
- 目标用户：
- 组织上下文：
- 配置版本和状态展示：
- 功能演示或 dry-run 场景：
- 规则校验结果展示：
- 空态、加载态、错误态：
- E2E 或组件测试：

## Provider 影响

说明是否影响：

- Database Provider
- Neo4j Provider
- Dify Provider
- Local fallback Provider
- HIS/EMR/LIS/PACS/医保 Adapter

## 降级策略

说明外部依赖不可用、配置缺失、字典未映射、规则异常时如何返回。

## 审计要求

说明哪些动作必须写审计：

- 导入
- review
- publish
- rollback
- sync
- evaluate
- external call

## 测试要求

至少包含：

1. 契约测试。
2. 样例 JSON 或 API 示例。
3. DB-only 场景。
4. 异常/降级场景。
5. 需要时补 smoke 脚本。
6. 涉及来源追溯时补缺来源、过期来源、未审核来源测试。
7. 涉及表结构、索引、约束、迁移或持久化 SQL 时，必须同步生产库 DDL（Oracle/达梦/PostgreSQL-Kingbase）和开发库 DDL（LOCAL_H2_FILE）；有生产库环境时执行对应 smoke，无生产库时至少执行 LOCAL_H2_FILE 验证，不能只跑内存/JUnit。

## 质量评审要求

说明：

- review_id：
- review 类型：独立评审 / 自审 / 领域评审 / 集成评审
- 是否高风险任务：
- 必须参与的 Reviewer AI：
- 医学/医保/质控审查：
- 数据库一致性审查：
- 安全和隐私审查：
- 前端体验和可访问性审查：
- P0/P1/P2 问题处理要求：

评审放行条件：

```text
review_status=APPROVED
open_findings=0
claim.status=READY_TO_SUBMIT
```

## 自主运行要求

若本任务由 AI 自主选择，说明：

- run_id：
- 任务选择依据：
- 是否先检查 pending/changes_requested review：
- 是否存在 active claim 冲突：
- 剩余额度不足时的交接动作：
- 停机条件：
- 下一步任务：

## 文档要求

根据影响范围更新：

- `zy-engine-mvp/README.md`
- `zy-engine-mvp/docs/api-examples.http`
- `zy-engine-mvp/docs/产品化方案与AI开发编排.md`
- `zy-engine-mvp/docs/全功能蓝图与并行开发计划.md`
- `zy-engine-mvp/docs/前端配置平台规划与开发验证.md`
- `ai-dev-input/README.md`
- 样例 JSON
- 测试矩阵

## 验收标准

1.
2.
3.

业务验收必须说明：

```text
目标角色是否能完成动作：
是否可在脚本或前端中演示：
是否可追溯来源/版本/traceId：
是否支持人工复核或医生确认：
是否可导出或记录验收结果：
```

必须通过：

```powershell
.\zy-engine-mvp\scripts\run-tests.ps1
.\zy-engine-mvp\scripts\build.ps1
git diff --check
```

若任务涉及落库，必须说明生产库和开发库验证结果。无生产库环境时至少通过：

```powershell
.\zy-engine-mvp\scripts\detect-db-env.ps1 -BootstrapLocal
.\zy-engine-mvp\scripts\start-local-db.ps1
```

有 Oracle 生产权威库时还必须通过：

```powershell
.\zy-engine-mvp\scripts\run-oracle-ddl.ps1
.\zy-engine-mvp\scripts\run-oracle-org-smoke.ps1
```

Oracle 脚本会自动读取仓库根目录 `.env.oracle.local`。`.env.oracle.local.example` 为可提交模板，记录 Oracle 连接目标；真实 `.env.oracle.local` 为本地忽略文件，只记录本机 Oracle 凭据，禁止提交。达梦/PostgreSQL/Kingbase 若无真实环境，必须提交 DDL 同步和补验计划。

提交与推送：

1. 只暂存并提交本任务相关文件。
2. 创建并通过 `ai-dev-input/11_ai_reviews` 质量评审。
3. 只有 `review_status=APPROVED` 且 `open_findings=0`，才能正式提交业务代码。
4. 每完成一个明确任务并通过质量门禁后，必须立即推送到远端当前分支。
5. 完成后把 claim 更新为 `DONE` 并归档到 `ai-dev-input/10_task_claims/archive/YYYYMMDD/`，把 review 归档到 `ai-dev-input/11_ai_reviews/archive/YYYYMMDD/`；自主运行结束时归档 run log。
6. 最终回复必须说明提交 hash、推送分支、review_id、run_id 和开放问题数；若无法提交或推送，必须说明原因、影响和替代交接方式。

## 风险与边界

说明：

- 本任务不做什么。
- 后续任务是什么。
- 可能影响哪些旧接口。
- 是否需要院方医学/医保/质控人员审核。
