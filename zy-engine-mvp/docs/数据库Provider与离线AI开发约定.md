# 数据库 Provider 与离线 AI 开发约定

## 1. 设计目标

AI 开发环境经常无法连接公司内网 Oracle，但项目不能因此退化成“只能改文档、不能跑功能”。本约定定义数据库 Provider 分层，让无 Oracle 的 AI 也能完整开发、验证和并行交付，同时保证 Oracle/达梦结构文件不缺失，主版本可在正式环境正常运行。

目标：

- Oracle 是生产权威数据库和主版本验收数据库。
- 达梦 DDL 必须持续维护，作为国产化兼容交付物。
- AI 离线开发默认使用本地文件数据库 `LOCAL_H2_FILE`。
- 无 Oracle 环境也必须能启动服务、导入配置、运行规则、执行路径、写审计、跑核心 smoke。
- 本地数据库不能成为生产替代品，也不能反向削弱 Oracle DDL、Oracle smoke 和真实落库要求。

## 2. Provider 分层

数据库 Provider 分三层：

```text
ORACLE
  生产权威、客户内网、正式验收、迁移脚本、中文备注、Oracle smoke。

DM
  达梦兼容 DDL，保证结构设计不锁死 Oracle 专有能力。

LOCAL_H2_FILE
  AI/离线开发本地文件库，JDK 内嵌运行，无需安装服务，用 Oracle mode 尽量贴近 Oracle SQL。
```

运行模式：

```text
FULL_INTEGRATION   Oracle + Neo4j + Dify
HYBRID             Oracle/Local DB + 部分外部 Provider
DB_ONLY            Oracle/Local DB，无 Neo4j/Dify
IN_MEMORY_DEMO     未启用数据库，仅内存演示
```

`IN_MEMORY_DEMO` 只用于快速演示和单元测试。新 AI 默认应优先使用 `LOCAL_H2_FILE`，除非已经具备 Oracle 环境。

## 3. 权威结构文件

Oracle 和达梦结构文件不能缺失，任何表结构变更必须同步维护：

```text
zy-engine-mvp/db/oracle/zyengine_core_ddl_with_comments.sql
ai-dev-input/04_database/oracle/core_ddl.sql
ai-dev-input/04_database/dm/core_ddl.sql
```

本地 H2 结构文件也要同步补齐，用于 AI 离线开发：

```text
zy-engine-mvp/src/main/resources/db/local/h2_core_ddl.sql
ai-dev-input/04_database/local/h2_core_ddl.sql
```

结构维护规则：

- Oracle DDL 是生产权威，字段、索引、约束、中文备注必须最完整。
- 达梦 DDL 必须保持核心结构等价。
- H2 DDL 必须覆盖当前应用持久化链路，确保离线 AI 可运行完整功能。
- H2 DDL 可使用 `CREATE TABLE IF NOT EXISTS`，但不得引入 Oracle 没有的业务字段。
- 新增落库链路时，先补 Oracle/达梦 DDL，再补 H2 DDL，最后接 Java 持久化。

## 4. AI 开发前数据库识别

每个 AI 开始开发前必须先识别数据库环境：

```powershell
.\zy-engine-mvp\scripts\detect-db-env.ps1 -BootstrapLocal
```

识别结果：

```text
recommended_mode=ORACLE
```

表示本地 Oracle 环境变量已配置，可优先走 Oracle。

```text
recommended_mode=LOCAL_H2
```

表示当前没有可用 Oracle 凭据，应使用本地 H2 文件库开发。

启动命令：

```powershell
.\zy-engine-mvp\scripts\start-oracle.ps1
```

或：

```powershell
.\zy-engine-mvp\scripts\start-local-db.ps1
```

## 5. 本地 H2 开发约定

本地 H2 文件库默认位置：

```text
zy-engine-mvp/data/local-db/zyengine-local.mv.db
```

启动后访问：

```text
http://localhost:18082/zy-engine/api/health
http://localhost:18082/zy-engine/api/system/providers
```

Provider 应显示：

```text
provider=LOCAL_H2_FILE
dialect=h2
production_authority=ORACLE
run_mode=DB_ONLY
```

本地 H2 必须支持：

- 配置包导入、review、publish、export。
- 组织目录导入与组织上下文解析。
- 规则导入、发布、模拟、第三方 evaluate/batch-evaluate。
- 路径导入、发布、入径、节点流转、变异记录。
- 图谱和 Dify 降级模式。
- 字典映射和适配器 Mock。
- 审计写入。
- 来源追溯表结构初始化。

如果某个功能只能在 Oracle 跑，视为功能欠缺，必须补本地 Provider 或明确拆出阻塞任务。

## 6. 验证矩阵

普通 AI 离线开发必须至少执行：

```powershell
.\zy-engine-mvp\scripts\run-tests.ps1
.\zy-engine-mvp\scripts\build.ps1
git diff --check
```

涉及持久化或 DDL 的任务，在没有 Oracle 时必须执行：

```powershell
.\zy-engine-mvp\scripts\detect-db-env.ps1 -BootstrapLocal
.\zy-engine-mvp\scripts\start-local-db.ps1
```

另一个终端执行核心 smoke，端口使用 `18082`：

```powershell
.\zy-engine-mvp\scripts\run-rule-smoke.ps1 -BaseUrl http://localhost:18082/zy-engine/api
.\zy-engine-mvp\scripts\run-pathway-smoke.ps1 -BaseUrl http://localhost:18082/zy-engine/api
.\zy-engine-mvp\scripts\run-config-import-smoke.ps1 -BaseUrl http://localhost:18082/zy-engine/api
.\zy-engine-mvp\scripts\run-graph-dify-smoke.ps1 -BaseUrl http://localhost:18082/zy-engine/api
.\zy-engine-mvp\scripts\run-terminology-adapter-smoke.ps1 -BaseUrl http://localhost:18082/zy-engine/api
```

有 Oracle 的 AI 或最终集成 AI 仍必须执行：

```powershell
.\zy-engine-mvp\scripts\run-oracle-ddl.ps1
.\zy-engine-mvp\scripts\run-oracle-org-smoke.ps1
```

## 7. 并行开发规则

任务 claim 中必须写明数据库环境：

```text
database_mode: ORACLE / LOCAL_H2 / IN_MEMORY
oracle_available: true / false
local_db_verified: true / false
oracle_verification_required: true / false
```

规则：

- 无 Oracle 的 AI 可以认领后端、前端、测试、文档和 DDL 草案任务。
- 涉及真实 Oracle 迁移、Oracle 中文备注、Oracle smoke 的最终确认任务，应由有 Oracle 的 AI 或集成 AI 认领。
- 无 Oracle 的 AI 修改 DDL 时，必须同步维护 Oracle/达梦/H2 三份结构文件。
- 无 Oracle 的 AI 最终回复必须说明“已用 LOCAL_H2 验证，Oracle smoke 留给集成 AI/有内网环境 AI”。
- 有 Oracle 的 AI 接手后不能重写离线 AI 的功能，只做真实 Oracle 验证和必要兼容修正。

## 8. 质量红线

- 禁止因为没有 Oracle 就跳过数据库设计。
- 禁止只补 H2 表，不补 Oracle/达梦表。
- 禁止把 H2 专有 SQL 写进业务逻辑。
- 禁止让本地 Provider 成为生产默认。
- 禁止把 `IN_MEMORY_DEMO` 当作持久化验收。
- 禁止新增功能只在 Oracle 或只在本地库可用。

## 9. 架构判断

这套设计把数据库能力拆成“生产权威”和“开发等价”两条轨道：

- 生产权威保证客户交付、审计、上线和数据治理。
- 开发等价保证没有内网的 AI 也能快速并行、完整验证、持续交付。

后续所有模块都必须按这个原则继续演进：业务能力不绑定单一数据库，结构交付不缺 Oracle，离线开发不缺功能。
