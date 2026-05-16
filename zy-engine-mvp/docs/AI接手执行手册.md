# AI 接手执行手册

## 1. 用途

本文是后续 AI 接手本项目时的第一入口。它不替代产品总纲，而是把“接手后怎么做”压缩成可执行流程。

接手 AI 必读顺序：

1. 本文。
2. `zy-engine-mvp/docs/产品化方案与AI开发编排.md`
3. 根目录 `README.md`
4. `zy-engine-mvp/README.md`
5. `ai-dev-input/09_ai_task_cards/ai_system_prompt.md`

## 2. 当前项目一句话

这是面向集团化医院、多院区、卫生所/站点的医疗智能引擎平台。Oracle/关系型数据库是配置主数据源；Neo4j 是图谱查询投影；Dify 是 AI 工作流执行目标；没有 Neo4j 和 Dify 时，DB-only 测试环境也必须可完整验收。

## 3. 接手后 10 分钟检查

先执行：

```powershell
git status -sb
rg -n "当前优先任务池|Definition of Done|PKG-001|ORG-001|TERM-001|RULE-001" zy-engine-mvp/docs
```

再阅读：

```text
zy-engine-mvp/docs/产品化方案与AI开发编排.md
zy-engine-mvp/README.md
ai-dev-input/09_ai_task_cards/ai_system_prompt.md
ai-dev-input/09_ai_task_cards/task_card_template.md
```

若工作树已有未提交改动：

- 不要回滚。
- 先判断是否与当前任务相关。
- 相关则继续兼容；无关则忽略。
- 不确定时向用户说明风险。

## 4. 开发前必须确认

每次开始前必须明确：

- 任务编号，例如 `ORG-001`、`PKG-001`、`TERM-001`。
- 所属模块。
- 是否涉及组织隔离。
- 是否涉及配置版本。
- 是否涉及 Provider。
- 是否需要 Oracle/达梦 DDL。
- DB-only 模式如何验收。
- 需要更新哪些测试、样例和文档。

没有明确任务编号时，优先使用 `产品化方案与AI开发编排.md` 中的“当前优先任务池”。

## 5. 任务编号约定

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
AUDIT-xxx   审计日志
OPS-xxx     运维部署
SEC-xxx     安全权限
TEST-xxx    测试与验收
DOC-xxx     文档
```

## 6. 标准开发流程

### Step 1：确认范围

```powershell
git status -sb
git log -1 --pretty=format:%H%n%s
```

阅读相关模块代码和测试。优先使用 `rg` 搜索，不要凭记忆修改。

### Step 2：形成任务计划

计划至少包含：

- 需要修改的模块。
- 需要新增/修改的接口。
- 数据模型和配置影响。
- 测试点。
- 文档点。

### Step 3：实现

实现时遵守：

- 不硬编码医院逻辑。
- 不引入 Neo4j/Dify 强依赖。
- 所有配置导入必须校验。
- 关键操作必须审计。
- 外部依赖必须 Provider 化。
- API 返回统一 `ApiResult`。
- 新增接口必须透出 `traceId`。

### Step 4：补测试

最低要求：

- 新增接口补 `EngineApiContractTests` 或等价契约测试。
- 新增配置补样例 JSON。
- 新增运行链路补 smoke 脚本或 README 验证方式。

### Step 5：补文档

按影响范围更新：

- `zy-engine-mvp/README.md`
- `zy-engine-mvp/docs/api-examples.http`
- `zy-engine-mvp/docs/产品化方案与AI开发编排.md`
- `ai-dev-input/09_ai_task_cards/*.md`
- 样例 JSON 或测试矩阵。

### Step 6：验证

必须执行：

```powershell
.\zy-engine-mvp\scripts\run-tests.ps1
.\zy-engine-mvp\scripts\build.ps1
git diff --check
```

若只改文档，仍建议运行完整测试和构建，除非用户明确要求快速文档修改。

### Step 7：提交

提交前：

```powershell
git status -sb
git diff --stat
git diff --cached --stat
```

只暂存当前任务相关文件。

提交信息格式：

```text
动词 + 模块 + 能力
```

示例：

```text
增加Provider运行状态接口
补充产品化方案与AI开发编排
增加规则包审核与批量发布
```

## 7. 每批代码 Definition of Done

完成一批任务前必须满足：

- 工作范围清晰，无无关文件混入。
- 新增能力有契约测试。
- 新增配置有样例或 API 示例。
- README 或 docs 已更新。
- `run-tests.ps1` 通过。
- `build.ps1` 通过。
- `git diff --check` 通过。
- DB-only 模式仍可运行。
- 不硬编码单医院逻辑。
- 不把 Neo4j/Dify 作为强依赖。
- 最终回复包含改动、验证、风险。

## 8. 当前优先任务执行顺序

建议按以下顺序推进：

1. `PKG-001` 配置包统一模型。
2. `ORG-001` 组织模型和组织上下文。
3. `TERM-001` 医嘱标准化。
4. `RULE-001` 第三方规则引擎 API。
5. `GRAPH-001` 图谱包发布与同步。
6. `DIFY-001` Dify 契约绑定。
7. `SEC-001` 接口鉴权、签名和组织权限。
8. `OPS-001` 离线部署和运维看板。

若用户没有指定任务，优先从 `PKG-001` 或 `ORG-001` 开始，因为它们是产品化底座。

## 9. 推荐任务切片

### PKG-001 配置包统一模型

第一批只做模型和内存/数据库无关服务骨架：

- `ConfigPackage` 数据结构。
- `package_code/package_version/scope_level/scope_code/status/hash` 字段。
- review 返回 manifest。
- 不急于一次性接所有模块。

### ORG-001 组织模型

第一批只做：

- 组织模型类。
- 组织上下文工具。
- `/api/system/org-context` 或等价状态接口。
- 后续再逐步接入所有业务表。

### TERM-001 医嘱标准化

第一批只做：

- 医嘱映射导入。
- 单条医嘱标准化。
- 未映射返回 `PENDING_MAPPING`。
- 样例覆盖同药不同名称/规格。

### RULE-001 第三方规则引擎 API

第一批只做同步接口：

```http
POST /api/rule-engine/evaluate
```

支持：

- `scenario_code`
- `rule_package_code`
- `patient_context`
- 返回标准 `results`

异步和批量后续再做。

## 10. 禁止事项

- 禁止把客户医院编码、医嘱名称、院区能力写死进 Java 逻辑。
- 禁止直接让外部系统传任意 SQL/Cypher 到引擎执行。
- 禁止 Neo4j/Dify 不可用时导致核心路径/规则接口不可用。
- 禁止绕过 review/publish 直接激活配置。
- 禁止配置发布后静默覆盖同版本。
- 禁止在日志中输出数据库密码、Dify API Key、患者完整隐私明文。
- 禁止回滚用户或其他 AI 的未提交改动。

## 11. 最终回复模板

建议最终回复保持简洁：

```text
已完成并验证。

主要改动：
- ...

验证：
- run-tests.ps1
- build.ps1
- git diff --check

提交：
- <commit hash / link>

剩余风险：
- ...
```

如果没有提交或没有推送，必须明确说明原因。
