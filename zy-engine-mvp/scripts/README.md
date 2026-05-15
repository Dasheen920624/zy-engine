# 本地脚本说明

本目录提供项目常用脚本入口，目标是让 PowerShell 7、Windows PowerShell 5.1、Windows Terminal、普通 CMD 都能稳定运行。

## 推荐用法

优先使用 `.cmd` 入口：

```powershell
.\scripts\build.cmd
.\scripts\start-memory.cmd
.\scripts\start-oracle.cmd
.\scripts\run-oracle-ddl.cmd
.\scripts\verify-encoding.cmd
.\scripts\run-rule-smoke.cmd
.\scripts\run-pathway-smoke.cmd
.\scripts\stop-local.cmd
```

`.cmd` 会自动检测本机是否安装 `pwsh`：

- 已安装 PowerShell 7：使用 `pwsh` 执行。
- 未安装 PowerShell 7：回退到 Windows PowerShell 5.1。

## 脚本列表

| 脚本 | 用途 |
|---|---|
| `build.cmd` / `build.ps1` | 使用 Maven 构建 JDK 1.8 后端工程 |
| `start-memory.cmd` / `start-memory.ps1` | 启动内存演示模式，默认端口 `18080` |
| `start-oracle.cmd` / `start-oracle.ps1` | 启动 Oracle 持久化模式，默认端口 `18081` |
| `run-oracle-ddl.cmd` / `run-oracle-ddl.ps1` | 初始化 Oracle 核心表、索引、中文表备注和字段备注 |
| `verify-encoding.cmd` / `verify-encoding.ps1` | 校验常见乱码特征，并验证 JSON 样例可解析 |
| `run-rule-smoke.cmd` / `run-rule-smoke.ps1` | 导入、发布并模拟执行 AMI 样例规则 |
| `run-pathway-smoke.cmd` / `run-pathway-smoke.ps1` | 导入、发布并执行 AMI 样例路径闭环 |
| `stop-local.cmd` / `stop-local.ps1` | 停止本地 `18080` 和 `18081` 演示服务 |

## Oracle 模式

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

## 端口

内存模式默认：

```text
http://localhost:18080/zy-engine/api/health
```

Oracle 模式默认：

```text
http://localhost:18081/zy-engine/api/health
```

停止本地服务：

```powershell
.\scripts\stop-local.cmd
```
