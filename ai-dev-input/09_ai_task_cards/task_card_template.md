# AI 任务卡模板

使用前先阅读：

- `zy-engine-mvp/docs/AI接手执行手册.md`
- `zy-engine-mvp/docs/顶级多角色评审与AI并行开发总控.md`
- `zy-engine-mvp/docs/产品化方案与AI开发编排.md`

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

## 总控泳道

填写本任务属于 `顶级多角色评审与AI并行开发总控.md` 中哪条泳道：

```text
A 配置包与发布治理 / B 组织与权限 / C 来源追溯与医学可信 / D 规则质控和医嘱安全 / E 路径图谱Dify适配器 / F 前端配置和客户验收 / G 测试运维和安全
```

说明：

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
8. 数据库访问考虑 Oracle/达梦兼容。
9. 日志不输出密钥和患者完整隐私明文。
10. 规则、知识、图谱证据、Dify解释、字典映射、适配器口径和质控结论必须可查来源。

## 数据模型变更

说明：

- 新增表：
- 新增字段：
- 索引：
- Oracle/达梦差异：
- 是否需要迁移脚本：
- 是否必须同步真实 Oracle：
- Oracle smoke 脚本：

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
7. 涉及表结构、索引、约束、迁移或持久化 SQL 时，必须执行真实 Oracle DDL/迁移和 Oracle 版本 smoke，不能只跑内存/JUnit。

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

必须通过：

```powershell
.\zy-engine-mvp\scripts\run-tests.ps1
.\zy-engine-mvp\scripts\build.ps1
git diff --check
```

若任务涉及 Oracle 落库，还必须通过：

```powershell
.\zy-engine-mvp\scripts\run-oracle-ddl.ps1
.\zy-engine-mvp\scripts\run-oracle-org-smoke.ps1
```

Oracle 脚本会自动读取仓库根目录 `.env.oracle.local`。`.env.oracle.local.example` 为可提交模板，记录 Oracle 连接目标；真实 `.env.oracle.local` 为本地忽略文件，只记录本机 Oracle 凭据，禁止提交。

提交与推送：

1. 只暂存并提交本任务相关文件。
2. 每完成一个明确任务，必须立即推送到远端当前分支。
3. 最终回复必须说明提交 hash、推送分支；若无法提交或推送，必须说明原因、影响和替代交接方式。

## 风险与边界

说明：

- 本任务不做什么。
- 后续任务是什么。
- 可能影响哪些旧接口。
- 是否需要院方医学/医保/质控人员审核。
