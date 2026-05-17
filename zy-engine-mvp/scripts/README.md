# 本地脚本说明

本目录提供项目常用脚本入口，目标是让 PowerShell 7、Windows PowerShell 5.1、Windows Terminal、普通 CMD 都能稳定运行。

## 推荐用法

优先使用 `.cmd` 入口：

```powershell
.\scripts\build.cmd
.\scripts\detect-db-env.cmd
.\scripts\start-memory.cmd
.\scripts\start-local-db.cmd
.\scripts\start-oracle.cmd
.\scripts\run-oracle-ddl.cmd
.\scripts\verify-encoding.cmd
.\scripts\run-rule-smoke.cmd
.\scripts\run-pathway-smoke.cmd
.\scripts\run-oracle-org-smoke.cmd
.\scripts\run-graph-dify-smoke.cmd
.\scripts\run-terminology-adapter-smoke.cmd
.\scripts\stop-local.cmd
```

`.cmd` 会自动检测本机是否安装 `pwsh`：

- 已安装 PowerShell 7：使用 `pwsh` 执行。
- 未安装 PowerShell 7：回退到 Windows PowerShell 5.1。

## 脚本列表

| 脚本 | 用途 |
|---|---|
| `build.cmd` / `build.ps1` | 使用 Maven 构建 JDK 1.8 后端工程 |
| `detect-db-env.cmd` / `detect-db-env.ps1` | 识别当前 AI 环境应使用 Oracle 还是本地 H2 文件库 |
| `start-memory.cmd` / `start-memory.ps1` | 启动内存演示模式，默认端口 `18080` |
| `start-local-db.cmd` / `start-local-db.ps1` | 启动本地 H2 文件数据库模式，默认端口 `18082` |
| `start-oracle.cmd` / `start-oracle.ps1` | 启动 Oracle 持久化模式，默认端口 `18081` |
| `run-oracle-ddl.cmd` / `run-oracle-ddl.ps1` | 初始化 Oracle 核心表、索引、组织上下文迁移、中文表备注和字段备注 |
| `verify-encoding.cmd` / `verify-encoding.ps1` | 校验常见乱码特征，并验证 JSON 样例可解析 |
| `run-rule-smoke.cmd` / `run-rule-smoke.ps1` | 导入、发布并模拟执行 AMI 样例规则 |
| `run-pathway-smoke.cmd` / `run-pathway-smoke.ps1` | 导入、发布并执行 AMI 样例路径闭环 |
| `run-oracle-org-smoke.cmd` / `run-oracle-org-smoke.ps1` | 在 Oracle 模式下通过 API 执行规则，并用 SQLPlus 校验组织字段真实落表 |
| `run-graph-dify-smoke.cmd` / `run-graph-dify-smoke.ps1` | 验证图谱候选召回、证据查询和 Dify 降级调用 |
| `run-terminology-adapter-smoke.cmd` / `run-terminology-adapter-smoke.ps1` | 验证字典映射和第三方适配器 Mock 查询 |
| `stop-local.cmd` / `stop-local.ps1` | 停止本地 `18080` 和 `18081` 演示服务 |

## Oracle 模式

仓库根目录已提供 `.env.oracle.local.example` 说明 Oracle 连接目标。本地可复制为 `.env.oracle.local` 并填写真实密码，该文件已被 `.gitignore` 忽略，禁止提交。Oracle 相关脚本会自动读取仓库根目录的 `.env.oracle.local`。

启动 Oracle 模式前设置环境变量：

```powershell
$env:ZYENGINE_DB_URL='jdbc:oracle:thin:@//192.168.4.25:1521/ORCL'
$env:ZYENGINE_DB_USERNAME='ZYENGINE'
$env:ZYENGINE_DB_PASSWORD='数据库密码'
.\scripts\start-oracle.cmd
```

建表前设置：

```powershell
$env:ZYENGINE_DB_CONNECT='//192.168.4.25:1521/ORCL'
$env:ZYENGINE_DB_USERNAME='ZYENGINE'
$env:ZYENGINE_DB_PASSWORD='数据库密码'
.\scripts\run-oracle-ddl.cmd
```

已有库会额外执行 `db/oracle/zyengine_org_context_migration.sql`，补齐 `PE_VARIATION_RECORD`、`RE_RULE_EXEC_LOG`、`ENGINE_AUDIT_LOG` 的组织字段与索引，并把 `PE_PATIENT_INSTANCE` 活动实例唯一约束升级为 `tenant_id + org_code + encounter_id + pathway_code + status`。

Oracle 真实落库校验需要先启动 Oracle 模式，再执行：

```powershell
.\scripts\run-oracle-org-smoke.cmd
```

普通 `run-tests.cmd` 只运行 Maven/JUnit，默认不连接 Oracle；需要跨实例落表验证时使用上述 Oracle smoke。

约定：凡是修改 Oracle/达梦 DDL、Oracle 迁移脚本、表字段、索引、约束、持久化 SQL 或新增落库链路的任务，完成前必须先执行 `run-oracle-ddl.cmd` 同步真实 Oracle，再启动 Oracle 模式并执行对应 Oracle smoke。若缺少专项 smoke，先补脚本或在最终交接中明确未覆盖风险。

## 本地 H2 文件库模式

当 AI 开发环境不能连接公司内网 Oracle 时，先执行：

```powershell
.\scripts\detect-db-env.cmd -BootstrapLocal
```

若输出 `recommended_mode=LOCAL_H2`，使用本地文件数据库：

```powershell
.\scripts\start-local-db.cmd
```

默认端口：

```text
http://localhost:18082/zy-engine/api/health
http://localhost:18082/zy-engine/api/system/providers
```

本地库文件位于 `zy-engine-mvp/data/local-db/`，已被 `.gitignore` 忽略。H2 只作为 AI/离线开发 Provider，属于开发库；Oracle 是当前生产权威库，达梦、PostgreSQL、KingbaseES 是生产兼容交付库。任何 DDL 变更必须同步维护 Oracle、达梦、PostgreSQL-Kingbase 和 LOCAL_H2_FILE 结构文件。

## 端口

内存模式默认：

```text
http://localhost:18080/zy-engine/api/health
```

Oracle 模式默认：

```text
http://localhost:18081/zy-engine/api/health
```

本地 H2 模式默认：

```text
http://localhost:18082/zy-engine/api/health
```

停止本地服务：

```powershell
.\scripts\stop-local.cmd
```
