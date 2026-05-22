# MedKernel 医学知识平台 — 运维手册

> **版本**：1.0 | **状态**：GA | **日期**：2026-05-23
>
> 本手册面向医院信息科运维人员，涵盖 MedKernel 平台的日常运维、监控告警、故障排查等操作。

---

## 目录

1. [系统概述](#1-系统概述)
2. [部署架构](#2-部署架构)
3. [日常运维操作](#3-日常运维操作)
4. [监控与告警](#4-监控与告警)
5. [健康检查](#5-健康检查)
6. [数据库运维](#6-数据库运维)
7. [安全运维](#7-安全运维)
8. [性能调优](#8-性能调优)
9. [日志管理](#9-日志管理)
10. [常见问题排查](#10-常见问题排查)

---

## 1. 系统概述

### 1.1 平台简介

MedKernel 是面向医院场景的医学知识平台，提供临床决策支持（CDSS）、路径管理、规则引擎、知识图谱、AI 模型网关等核心能力。平台支持国产化环境部署，满足等保 2.0 三级合规要求。

### 1.2 系统架构

```
┌─────────────┐     ┌──────────────────────┐     ┌───────────────────────────┐
│   浏览器     │────▶│   Nginx (80/443)     │────▶│   Spring Boot (18080)     │
│             │◀────│   反向代理 + 静态资源  │◀────│   业务端口                 │
└─────────────┘     └──────────────────────┘     │                           │
                                                  │   Actuator (18081)        │
                                                  │   管理端口（仅 127.0.0.1） │
                                                  └──────────┬────────────────┘
                                                             │
                                              ┌──────────────┼──────────────┐
                                              │              │              │
                                        ┌─────▼────┐  ┌─────▼────┐  ┌─────▼──────┐
                                        │ Oracle   │  │ DM(达梦)  │  │ PostgreSQL │
                                        │ 11g-21c  │  │ 7/8      │  │ 14-16      │
                                        └──────────┘  └──────────┘  └────────────┘
                                                                        │
                                                                  ┌─────▼──────┐
                                                                  │KingbaseES  │
                                                                  │ V8         │
                                                                  └────────────┘
```

**数据流**：浏览器 → Nginx（反向代理 + 前端静态资源）→ Spring Boot 后端 → 数据库

### 1.3 技术栈

| 组件 | 版本/技术 | 说明 |
|------|----------|------|
| 后端框架 | Spring Boot 2.7.18 | JDK 1.8 目标编译 |
| JDK | 1.8.0_202+ | 推荐 OpenJDK / 毕昇 JDK（ARM） |
| 连接池 | HikariCP | 内置于 Spring Boot Starter |
| 数据库迁移 | Flyway | 按方言自动选取迁移目录 |
| 前端 | React 18 + Vite 5 + AntD 5 | SPA 单页应用 |
| 反向代理 | Nginx | 静态资源 + API 反代 |
| 进程管理 | systemd | Linux 服务管理 |
| 监控采集 | Prometheus 2.54.1 | 15s 采集间隔 |
| 可视化 | Grafana 11.2.0 | 预置看板 |
| 告警 | Alertmanager 0.27.0 | 邮件 + Webhook 通知 |

---

## 2. 部署架构

### 2.1 内网部署模式

MedKernel 采用 **systemd + Nginx** 的内网部署模式：

- **业务端口**：18080（Spring Boot HTTP，仅 Nginx 回环访问）
- **管理端口**：18081（Actuator，仅 127.0.0.1 监听，Prometheus 采集用）
- **前端端口**：80（HTTP）或 443（HTTPS），由 Nginx 对外暴露

### 2.2 目录结构

```
$MK_HOME=/zoesoft/medkernel/
├── bin/                    # 可执行脚本（预留）
├── conf/                   # 配置文件
│   ├── medkernel.env       # 环境变量（数据库凭据、端口、JVM 参数）
│   ├── application.yml     # Spring Boot 主配置
│   └── application-local.yml  # 本地覆盖配置
├── lib/
│   └── medkernel.jar       # 后端 JAR 包
├── frontend/
│   └── dist/               # 前端构建产物（Vite build）
├── db/                     # 数据库 DDL / 迁移脚本
│   ├── oracle/             # Oracle 方言
│   ├── dm/                 # 达梦方言
│   ├── postgres/           # PostgreSQL 方言
│   └── local/              # H2 本地开发
├── logs/                   # 运行日志
│   ├── medkernel.log       # 应用日志
│   ├── stdout.log          # 标准输出
│   ├── stderr.log          # 标准错误
│   └── heap.hprof          # OOM 堆转储（自动生成）
├── scripts/                # 运维脚本
├── systemd/                # systemd 服务文件
├── nginx/                  # Nginx 配置模板
├── profiles/               # 部署 profile 模板
├── docs/                   # 文档
└── manifest.json           # 版本清单
```

### 2.3 运行用户

- **服务运行用户**：`medkernel`（系统用户，install-offline.sh 自动创建）
- **配置文件权限**：`conf/medkernel.env` 必须 `chmod 600`，仅 medkernel 用户可读写

### 2.4 支持的操作系统

| 操作系统 | 架构 | 备注 |
|----------|------|------|
| CentOS 7.6+ | x86_64 | 推荐使用 OpenJDK 1.8 |
| RHEL 7.6+ | x86_64 | 同 CentOS |
| UOS V20 | x86_64 / aarch64 | ARM 需毕昇 JDK |
| 银河麒麟 V10 SP3 | x86_64 / aarch64 | ARM 需毕昇 JDK；SELinux Enforcing 需配置 fcontext |
| openEuler 22.03 | x86_64 / aarch64 | 推荐毕昇 JDK |

> **国产化注意**：ARM 架构（鲲鹏/飞腾）必须使用毕昇 JDK 8 鲲鹏优化版，路径通常为 `/opt/bisheng-jdk1.8.0_402`。麒麟 V10 部分小版本 OpenSSL 1.0.2 与 JDK 8 SSL 握手敏感，必要时在 `JAVA_OPTS` 中添加 `-Djdk.tls.client.protocols=TLSv1.2`。

### 2.5 支持的数据库

| 数据库 | 版本 | JDBC 前缀 | Dialect 值 |
|--------|------|----------|-----------|
| Oracle | 11g R2, 12c, 18c, 19c, 21c | `jdbc:oracle:thin:@` | `oracle` |
| 达梦 DM | DM 7, DM 8 | `jdbc:dm://` | `dm` |
| PostgreSQL | 14, 15, 16 | `jdbc:postgresql://` | `postgres` |
| 人大金仓 KingbaseES | V8 R3, V8 R6 | 兼容 PG 驱动 | `kingbase`（兼容 `postgres`） |

---

## 3. 日常运维操作

### 3.1 服务管理

所有操作需 `root` 或 `sudo` 权限：

```bash
# 启动服务
sudo systemctl start medkernel

# 停止服务
sudo systemctl stop medkernel

# 重启服务
sudo systemctl restart medkernel

# 查看服务状态
sudo systemctl status medkernel

# 设置开机自启
sudo systemctl enable medkernel

# 取消开机自启
sudo systemctl disable medkernel
```

> **优雅停止**：systemd 配置了 `TimeoutStopSec=30`，先发送 SIGTERM，30 秒后发送 SIGKILL。正常情况下 Spring Boot 在 10 秒内完成优雅关闭。

### 3.2 查看日志

```bash
# 实时跟踪应用日志
tail -f $MK_HOME/logs/medkernel.log

# 查看标准输出
tail -f $MK_HOME/logs/stdout.log

# 查看标准错误
tail -f $MK_HOME/logs/stderr.log

# 查看 systemd 日志（最近 100 行）
journalctl -u medkernel -n 100

# 查看指定时间段的日志
journalctl -u medkernel --since "2026-05-23 08:00" --until "2026-05-23 09:00"
```

**日志轮转配置**（建议在 `/etc/logrotate.d/medkernel` 中配置）：

```
/zoesoft/medkernel/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    copytruncate
    dateext
    dateformat -%Y%m%d
}
```

### 3.3 配置修改

1. 编辑环境变量文件：

```bash
sudo vi $MK_HOME/conf/medkernel.env
```

2. 修改完成后**必须重启服务**使配置生效：

```bash
sudo systemctl restart medkernel
```

3. 验证配置是否生效：

```bash
# 检查服务状态
sudo systemctl status medkernel

# 运行健康检查
$MK_HOME/scripts/healthcheck.sh
```

> **安全提醒**：修改 `medkernel.env` 后务必确认文件权限：`sudo chmod 600 $MK_HOME/conf/medkernel.env`

### 3.4 前端更新

前端为纯静态资源，更新后**无需重启后端服务**：

```bash
# 1. 备份当前前端
sudo cp -a $MK_HOME/frontend/dist $MK_HOME/frontend/dist.bak.$(date +%Y%m%d)

# 2. 替换前端文件
sudo rm -rf $MK_HOME/frontend/dist
sudo cp -a /path/to/new/dist $MK_HOME/frontend/dist

# 3. 修正文件权限
sudo chown -R medkernel:medkernel $MK_HOME/frontend/dist

# 4. 验证（浏览器访问即可，无需重启）
```

### 3.5 版本升级

使用项目提供的升级脚本：

```bash
# 升级到指定版本（自动备份 + 停服 + 覆盖 + 重启 + 健康检查）
sudo $MK_HOME/scripts/upgrade.sh --to v1.3.0

# 仅备份，不升级
sudo $MK_HOME/scripts/upgrade.sh --backup-only

# 升级并执行数据库迁移
sudo $MK_HOME/scripts/upgrade.sh --to v1.3.0 --migrate-db
```

升级脚本流程：
1. 自动备份当前版本到 `$MK_HOME.bak/<timestamp>/`
2. 停止服务
3. 解压新版发布包（保留 conf 不覆盖）
4. 可选执行数据库迁移
5. 重启服务
6. 运行健康检查

> **回滚**：升级失败时，5 分钟内可执行回滚：
> ```bash
> sudo $MK_HOME/scripts/rollback.sh --to last
> # 或指定备份时间戳
> sudo $MK_HOME/scripts/rollback.sh --to 20260523_083000
> ```

---

## 4. 监控与告警

### 4.1 监控架构

```
Spring Boot Actuator (18081) → Prometheus (9090) → Grafana (3000)
                                                       ↓
                                               Alertmanager (9093)
                                                       ↓
                                              邮件 + Webhook 通知
```

### 4.2 Prometheus 采集配置

- **采集间隔**：15 秒
- **采集端点**：`http://127.0.0.1:18081/medkernel/actuator/prometheus`
- **配置文件**：`deploy/monitoring/prometheus/prometheus-medkernel.yml`

关键配置：

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    product: medkernel
    environment: pilot

scrape_configs:
  - job_name: medkernel-backend
    scrape_interval: 15s
    scrape_timeout: 5s
    metrics_path: /medkernel/actuator/prometheus
    scheme: http
    static_configs:
      - targets:
          - host.docker.internal:18081
        labels:
          service: medkernel-mvp
          role: backend
```

### 4.3 Grafana 看板

系统预置 6 套监控看板，位于 `deploy/monitoring/grafana/dashboards/`：

| 看板文件 | 看板名称 | 监控内容 |
|----------|---------|---------|
| `medkernel-system-overview.json` | 系统总览 | 服务状态、CPU、内存、磁盘、网络 |
| `medkernel-jvm-runtime.json` | JVM 运行时 | 堆内存、GC 次数/耗时、线程数、类加载数 |
| `medkernel-db-pool.json` | DB 连接池 | 活跃/空闲/最大连接数、饱和度、等待线程 |
| `medkernel-api-performance.json` | API 性能 | P50/P95/P99 延迟、QPS、错误率 |
| `medkernel-business-operations.json` | 业务指标 | Provider 状态、规则评估、路径操作、LLM 调用 |
| `medkernel-mvp.json` | MVP 综合 | 全维度综合看板 |

**Grafana 访问信息**（默认）：

| 项目 | 值 |
|------|-----|
| 地址 | `http://<服务器IP>:3000` |
| 用户名 | `admin` |
| 默认密码 | `medkernel_admin`（**首次登录后必须修改**） |

### 4.4 告警规则

告警规则定义在 `deploy/monitoring/prometheus/medkernel-alert-rules.yml`，共 4 大类 19 条：

#### 4.4.1 后端核心告警（9 条）

| 告警名称 | 条件 | 持续时间 | 级别 |
|----------|------|---------|------|
| MedKernelBackendDown | Prometheus 无法采集 actuator 端点 | 1m | Critical |
| MedKernelApiP95LatencyHigh | API P95 延迟 > 1s | 5m | Warning |
| MedKernelApiP95LatencyCritical | API P95 延迟 > 3s | 2m | Critical |
| MedKernelApi5xxRateHigh | 5xx 错误率 > 0.5% | 5m | Warning |
| MedKernelApi5xxRateCritical | 5xx 错误率 > 2% | 2m | Critical |
| MedKernelJvmHeapHigh | JVM 堆使用率 > 75% | 10m | Warning |
| MedKernelJvmHeapCritical | JVM 堆使用率 > 90% | 2m | Critical |
| MedKernelProviderNotReady | Provider 配置但未就绪 | 5m | Warning |
| MedKernelHikariPoolSaturation | 连接池饱和度 > 80% | 5m | Warning |

#### 4.4.2 系统资源告警（4 条）

| 告警名称 | 条件 | 持续时间 | 级别 |
|----------|------|---------|------|
| MedKernelDiskSpaceWarning | 磁盘使用率 > 85% | 5m | Warning |
| MedKernelDiskSpaceCritical | 磁盘使用率 > 95% | 2m | Critical |
| MedKernelCpuLoadHigh | JVM CPU 使用率 > 80% | 10m | Warning |
| MedKernelFileDescriptorHigh | 文件描述符使用率 > 80% | 5m | Warning |

#### 4.4.3 业务指标告警（4 条）

| 告警名称 | 条件 | 持续时间 | 级别 |
|----------|------|---------|------|
| MedKernelModelGatewayDown | AI 模型网关不可用 | 3m | Critical |
| MedKernelDatabaseProviderDown | 数据库 Provider 不可用 | 2m | Critical |
| MedKernelGraphProviderDegraded | 图谱 Provider 降级 | 10m | Warning |
| MedKernelHighErrorRate5xx | 持续 5xx 错误率 > 1% | 10m | Warning |

#### 4.4.4 SLO 违规告警（2 条）

| 告警名称 | 条件 | 持续时间 | 级别 |
|----------|------|---------|------|
| MedKernelSLOAvailabilityBreach | 30 分钟可用性 < 99.5% | 5m | Critical |
| MedKernelSLOLatencyBreach | 30 分钟 P95 延迟 > 500ms | 5m | Warning |

### 4.5 告警级别与通知

| 级别 | 通知策略 | 响应时间 | 说明 |
|------|---------|---------|------|
| **Critical** | 立即通知，5 分钟重复 | 15 分钟内响应 | 服务不可用、数据丢失风险 |
| **Warning** | 30 分钟内通知，30 分钟重复 | 1 小时内响应 | 性能退化、资源紧张 |

**通知渠道**（配置在 `deploy/monitoring/alertmanager/alertmanager.yml`）：

| 渠道 | 接收者 | 适用级别 |
|------|--------|---------|
| 邮件 | `oncall-medkernel@hospital.local` | Critical |
| 邮件 | `ops-medkernel@hospital.local` | Warning |
| Webhook | `http://localhost:5001/alerts/critical` | Critical（可对接钉钉/企业微信/飞书） |
| Webhook | `http://localhost:5001/alerts/warning` | Warning |

> **抑制规则**：Critical 触发时自动抑制同服务同告警名的 Warning，避免重复通知。

### 4.6 监控栈部署

使用 Docker Compose 一键启动监控栈：

```bash
cd $MK_HOME
docker compose -f docker-compose.monitoring.yml up -d
```

| 服务 | 端口 | 说明 |
|------|------|------|
| Prometheus | 9090 | 指标采集与存储（保留 30 天） |
| Alertmanager | 9093 | 告警路由与通知 |
| Grafana | 3000 | 可视化看板 |

---

## 5. 健康检查

### 5.1 健康检查端点

| 端点 | 端口 | 用途 | 说明 |
|------|------|------|------|
| `GET /api/health` | 18080 | 业务存活检查 | 返回 `{"success": true}` 表示正常 |
| `GET /api/system/providers` | 18080 | Provider 状态 | 检查数据库/模型网关/图谱就绪状态 |
| `GET /api/system/org-context` | 18080 | 组织上下文 | 检查组织架构数据加载状态 |
| `GET /medkernel/actuator/health` | 18081 | K8s liveness 探针 | 含 Provider 详细状态 |
| `GET /medkernel/actuator/health/readiness` | 18081 | K8s readiness 探针 | 含 Provider 就绪状态 |
| `GET /healthz`（Nginx 代理） | 80/443 | 外部负载均衡器探测 | 代理到 `/api/health` |

### 5.2 健康检查脚本

项目提供健康检查脚本 `deploy/scripts/healthcheck.sh`：

```bash
# 默认检查（http://localhost:18080/medkernel）
./healthcheck.sh

# 指定 URL
./healthcheck.sh --url http://10.0.0.1:18080/medkernel

# 指定超时时间（秒）
./healthcheck.sh --timeout 15
```

脚本依次探测 3 个端点：

1. `/api/health` — 基础健康
2. `/api/system/providers` — Provider 状态
3. `/api/system/org-context` — 组织上下文

全部通过返回 0，任一失败返回 1。

### 5.3 Kubernetes 探针配置

如部署在 K8s 环境，建议配置：

```yaml
livenessProbe:
  httpGet:
    path: /medkernel/actuator/health/liveness
    port: 18081
  initialDelaySeconds: 60
  periodSeconds: 30
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /medkernel/actuator/health/readiness
    port: 18081
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

---

## 6. 数据库运维

### 6.1 连接池监控

通过 HikariCP 暴露的 Prometheus 指标监控连接池状态：

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| `hikaricp_connections_active` | 活跃连接数 | — |
| `hikaricp_connections_idle` | 空闲连接数 | — |
| `hikaricp_connections_pending` | 等待获取连接的线程数 | > 0 需关注 |
| `hikaricp_connections_max` | 最大连接数 | — |
| `hikaricp_connections_total` | 总连接数 | — |

**关键 PromQL**：

```promql
# 连接池饱和度
hikaricp_connections_active / hikaricp_connections_max

# 等待线程数
hikaricp_connections_pending
```

### 6.2 连接池参数调优

当前默认配置（`application.yml`）：

| 参数 | 默认值 | 环境变量 | 说明 |
|------|--------|---------|------|
| maximum-pool-size | 20 | `MEDKERNEL_DB_POOL_MAX` | 单实例最大并发连接数 |
| minimum-idle | 2 | `MEDKERNEL_DB_POOL_MIN_IDLE` | 最小空闲连接，避免冷启 |
| connection-timeout | 3000ms | `MEDKERNEL_DB_POOL_TIMEOUT` | 获取连接超时，超时抛异常 |
| idle-timeout | 600000ms (10min) | `MEDKERNEL_DB_POOL_IDLE_TIMEOUT` | 空闲连接回收时间 |
| max-lifetime | 1800000ms (30min) | `MEDKERNEL_DB_POOL_MAX_LIFETIME` | 连接最大存活时间，避开 DBA 端断连 |
| leak-detection-threshold | 2000ms | `MEDKERNEL_DB_POOL_LEAK` | 连接持有超时打 WARN，排查泄漏 |

**调优建议**：

| 场景 | maximum-pool-size | 说明 |
|------|-------------------|------|
| 小型医院（< 200 床） | 10 | 并发量低 |
| 中型医院（200-800 床） | 20 | 默认值，8 核机器经验值 |
| 大型医院（> 800 床） | 30-50 | 需配合数据库端调整最大连接 |

修改方式：在 `medkernel.env` 中设置环境变量：

```bash
MEDKERNEL_DB_POOL_MAX=30
```

### 6.3 数据库切换

如需切换数据库类型，修改 `medkernel.env` 中的以下参数：

```bash
# 切换到 Oracle
MEDKERNEL_DB_DIALECT=oracle
MEDKERNEL_DB_URL=jdbc:oracle:thin:@//10.0.0.10:1521/ORCL
MEDKERNEL_DB_USERNAME=MEDKERNEL
MEDKERNEL_DB_PASSWORD=<密码>

# 切换到达梦 DM
MEDKERNEL_DB_DIALECT=dm
MEDKERNEL_DB_URL=jdbc:dm://10.0.0.20:5236?clobAsString=true&serverTimezone=Asia/Shanghai
MEDKERNEL_DB_USERNAME=MEDKERNEL
MEDKERNEL_DB_PASSWORD=<密码>

# 切换到 PostgreSQL
MEDKERNEL_DB_DIALECT=postgres
MEDKERNEL_DB_URL=jdbc:postgresql://10.0.0.30:5432/medkernel?reWriteBatchedInserts=true&prepareThreshold=0
MEDKERNEL_DB_USERNAME=medkernel
MEDKERNEL_DB_PASSWORD=<密码>
```

> **注意**：切换数据库后需重新执行对应方言的 DDL 脚本，位于 `$MK_HOME/db/<dialect>/` 目录。

### 6.4 Flyway 数据库迁移

Flyway 用于管理数据库版本迁移，默认关闭，生产环境建议开启。

**启用 Flyway**：

```bash
# 在 medkernel.env 中添加
MEDKERNEL_FLYWAY_ENABLED=true
MEDKERNEL_FLYWAY_BASELINE_ON_MIGRATE=true
MEDKERNEL_FLYWAY_BASELINE_VERSION=0
```

**迁移目录自动选取规则**：

| Dialect | 迁移目录 |
|---------|---------|
| h2 | `db/migration/{common,h2}` |
| oracle / dm | `db/migration/{common,oracle}` |
| postgres / kingbase | `db/migration/{common,postgres}` |

**查看迁移历史**：

```sql
-- 通用查询（所有数据库）
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

**Flyway 关键参数**：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `medkernel.flyway.enabled` | false | 是否启用 Flyway |
| `medkernel.flyway.baseline-on-migrate` | true | 首次迁移时自动 baseline |
| `medkernel.flyway.baseline-version` | 0 | Baseline 版本号 |
| `medkernel.flyway.out-of-order` | false | 是否允许乱序迁移 |
| `medkernel.flyway.clean-disabled` | true | 禁止 clean 操作（安全） |
| `medkernel.flyway.table` | flyway_schema_history | 迁移历史表名 |

---

## 7. 安全运维

### 7.1 JWT 密钥管理

- **密钥来源**：环境变量 `MEDKERNEL_JWT_SECRET`
- **安全要求**：不允许在 `application.yml` 中明文配置，生产/UAT/dev 全部走环境变量
- **未设置后果**：`JwtTokenProvider.init` 会拒绝启动

```bash
# 在 medkernel.env 中设置（必须 chmod 600）
MEDKERNEL_JWT_SECRET=<强随机密钥，建议 32 字符以上>
```

> **密钥轮换**：修改 `MEDKERNEL_JWT_SECRET` 后重启服务，所有已发放的 Token 将失效，用户需重新登录。

### 7.2 字段加密密钥轮换

MedKernel 使用 SM4 国密算法对 HEALTH_DATA 级别字段进行加密，支持双密钥并行轮换：

| 参数 | 环境变量 | 说明 |
|------|---------|------|
| master-key-base64 | `MEDKERNEL_SM4_MASTER_KEY` | 当前主密钥（16 字节 Base64） |
| previous-master-key-base64 | `MEDKERNEL_SM4_PREVIOUS_MASTER_KEY` | 上一轮主密钥（轮换过渡期使用） |

**密钥轮换步骤**：

1. 将当前 `MEDKERNEL_SM4_MASTER_KEY` 的值复制到 `MEDKERNEL_SM4_PREVIOUS_MASTER_KEY`
2. 生成新的主密钥并设置到 `MEDKERNEL_SM4_MASTER_KEY`
3. 重启服务
4. 系统自动使用新密钥加密新数据，使用 previous-master-key 解密旧数据
5. 确认所有旧数据已重新加密后，可清除 `MEDKERNEL_SM4_PREVIOUS_MASTER_KEY`

> **HSM 支持**：生产环境可启用硬件安全模块保护密钥，设置 `MEDKERNEL_HSM_ENABLED=true`。

### 7.3 账户锁定策略

| 参数 | 默认值 | 环境变量 | 说明 |
|------|--------|---------|------|
| 锁定阈值 | 5 次 | `MEDKERNEL_LOCK_THRESHOLD` | 连续登录失败次数 |
| 锁定时长 | 30 分钟 | `MEDKERNEL_LOCK_DURATION` | 锁定持续时间 |

### 7.4 配置文件权限

```bash
# medkernel.env 必须设置为 600（仅 owner 可读写）
sudo chmod 600 $MK_HOME/conf/medkernel.env
sudo chown medkernel:medkernel $MK_HOME/conf/medkernel.env

# 验证权限
ls -la $MK_HOME/conf/medkernel.env
# 期望输出：-rw------- 1 medkernel medkernel ... medkernel.env
```

### 7.5 审计日志查询

通过 API 查询审计日志：

```bash
# 查询审计日志（需认证）
curl -H "Authorization: Bearer <token>" \
  http://localhost:18080/medkernel/api/audit-logs
```

### 7.6 安全加固清单

| 项目 | 措施 | 状态 |
|------|------|------|
| 进程隔离 | systemd `PrivateTmp=true`, `ProtectSystem=full`, `ProtectHome=true` | 已配置 |
| 权限提升防护 | systemd `NoNewPrivileges=true` | 已配置 |
| 资源限制 | `LimitNOFILE=65535`, `MemoryHigh=6G`, `MemoryMax=8G` | 已配置 |
| 配置文件权限 | `medkernel.env` 必须 `chmod 600` | 需手动确认 |
| 管理端口绑定 | Actuator 仅监听 `127.0.0.1:18081` | 已配置 |
| 安全响应头 | Nginx 配置 `X-Content-Type-Options`, `X-Frame-Options` 等 | 已配置 |
| TLS 支持 | Nginx HTTPS 配置（`medkernel-tls.conf`） | 可选启用 |

---

## 8. 性能调优

### 8.1 JVM 参数调优

当前默认 JVM 参数（`medkernel.service` 中配置）：

```
-server -Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/zoesoft/medkernel/logs/heap.hprof
-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.timezone=Asia/Shanghai
-Djava.security.egd=file:/dev/./urandom
```

**调优建议**：

| 场景 | -Xms | -Xmx | GC | 说明 |
|------|------|------|----|------|
| 小型医院 | 1g | 2g | G1 | 默认即可 |
| 中型医院 | 2g | 4g | G1 | 当前默认 |
| 大型医院 | 4g | 8g | G1 | 需配合 `MemoryHigh/MemoryMax` 调整 |

修改方式：在 `medkernel.env` 中覆盖 `JAVA_OPTS`：

```bash
JAVA_OPTS="-server -Xms4g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
```

> **注意**：修改 JVM 参数后需重启服务。`-Xms` 和 `-Xmx` 建议设为相同值，避免堆内存动态伸缩带来的停顿。

### 8.2 连接池调优

参见 [6.2 连接池参数调优](#62-连接池参数调优)。

### 8.3 Nginx 调优

关键参数（`deploy/nginx/medkernel.conf`）：

| 参数 | 当前值 | 说明 | 调优建议 |
|------|--------|------|---------|
| `worker_connections` | 默认 1024 | 单 worker 最大并发连接 | 高并发场景设为 4096-8192 |
| `keepalive_timeout` | 默认 75s | 长连接超时 | 内网可设为 120s |
| `client_max_body_size` | 16M | 请求体大小限制 | 按需调整 |
| `proxy_connect_timeout` | 10s | 后端连接超时 | — |
| `proxy_read_timeout` | 60s | 后端读取超时 | LLM 调用慢时可适当增大 |
| `limit_req` | 100r/s burst=200 | 单 IP 限速 | 按实际负载调整 |

Nginx 主配置调优（`/etc/nginx/nginx.conf`）：

```nginx
worker_processes auto;           # 自动匹配 CPU 核心数
worker_rlimit_nofile 65535;     # worker 文件描述符上限

events {
    worker_connections 4096;     # 高并发场景
    use epoll;
}
```

### 8.4 常见性能问题排查

| 现象 | 可能原因 | 排查方法 |
|------|---------|---------|
| API 响应变慢 | JVM GC 停顿 | 查看 Grafana JVM 看板 GC 次数/耗时 |
| API 响应变慢 | 数据库慢查询 | 查看连接池饱和度，检查数据库慢查询日志 |
| 内存持续增长 | 连接泄漏 | 查看 `hikaricp_connections_active` 是否持续增长 |
| 内存持续增长 | 内存泄漏 | 查看 JVM 堆使用率趋势，分析 heap.hprof |
| CPU 飙高 | 规则评估密集 | 查看 `medkernel_rule_evaluation_duration` |
| CPU 飙高 | GC 压力大 | 查看 GC 频率和耗时 |
| 502/504 错误 | 后端不可用 | 检查 `systemctl status medkernel` |
| 502/504 错误 | 后端响应超时 | 检查 `proxy_read_timeout` 是否足够 |

---

## 9. 日志管理

### 9.1 日志位置与格式

| 日志文件 | 路径 | 说明 |
|----------|------|------|
| 应用日志 | `$MK_HOME/logs/medkernel.log` | Spring Boot 应用日志（主要排查依据） |
| 标准输出 | `$MK_HOME/logs/stdout.log` | systemd 捕获的 stdout |
| 标准错误 | `$MK_HOME/logs/stderr.log` | systemd 捕获的 stderr |
| Nginx 访问日志 | `/var/log/nginx/medkernel.access.log` | Nginx 访问日志 |
| Nginx 错误日志 | `/var/log/nginx/medkernel.error.log` | Nginx 错误日志 |
| 堆转储 | `$MK_HOME/logs/heap.hprof` | OOM 时自动生成 |

### 9.2 日志级别调整

通过 `application.yml` 或环境变量调整日志级别：

```yaml
# application.yml
logging:
  level:
    root: INFO
    com.medkernel: DEBUG          # 应用代码调试
    com.zaxxer.hikari: DEBUG      # 连接池调试
    org.springframework.web: DEBUG # Spring Web 调试
```

或通过环境变量：

```bash
# 在 medkernel.env 中添加
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_MEDKERNEL=DEBUG
```

> **注意**：DEBUG 级别会产生大量日志，仅用于故障排查，排查完成后应恢复为 INFO。

### 9.3 日志轮转和归档

建议配置 logrotate（参见 [3.2 查看日志](#32-查看日志)），关键参数：

| 参数 | 建议值 | 说明 |
|------|--------|------|
| daily | — | 每日轮转 |
| rotate | 30 | 保留 30 天 |
| compress | — | 压缩旧日志 |
| dateext | — | 日期后缀 |

**归档策略**：

- 30 天内：本地磁盘保留
- 30-90 天：压缩归档到备份存储
- 90 天以上：按合规要求决定是否删除

### 9.4 敏感信息脱敏

MedKernel 日志中已做以下脱敏处理：

- 数据库密码：日志中不输出连接密码
- JWT Token：日志中截断显示
- 患者数据：业务日志不记录患者敏感信息

> **运维注意**：排查问题时避免将完整日志发送到非安全渠道，如需外发日志请先脱敏处理。

---

## 10. 常见问题排查

### 10.1 服务启动失败

**症状**：`systemctl start medkernel` 后服务未运行

**排查步骤**：

```bash
# 1. 查看服务状态
sudo systemctl status medkernel

# 2. 查看 systemd 日志
sudo journalctl -u medkernel -n 100 --no-pager

# 3. 查看标准错误日志
sudo tail -100 $MK_HOME/logs/stderr.log

# 4. 常见原因
#    - 端口被占用：ss -ltnp | grep 18080
#    - JDK 未安装或版本不对：java -version
#    - medkernel.env 配置错误：检查语法
#    - 数据库连接失败：检查 MEDKERNEL_DB_URL 和凭据
#    - JWT 密钥未设置：检查 MEDKERNEL_JWT_SECRET
#    - 磁盘空间不足：df -h
```

### 10.2 数据库连接失败

**症状**：日志中出现连接超时或认证失败

**排查步骤**：

```bash
# 1. 检查数据库连通性
# Oracle
sqlplus MEDKERNEL/<密码>@//10.0.0.10:1521/ORCL -e "SELECT 1 FROM DUAL;"

# PostgreSQL
PGPASSWORD=<密码> psql -h 10.0.0.30 -U medkernel -d medkernel -c "SELECT 1;"

# 达梦
disql MEDKERNEL/<密码>@10.0.0.20:5236 -e "SELECT 1;"

# 2. 检查连接池状态
curl http://127.0.0.1:18081/medkernel/actuator/prometheus | grep hikaricp

# 3. 检查环境变量配置
grep MEDKERNEL_DB $MK_HOME/conf/medkernel.env

# 4. 常见原因
#    - 数据库服务未启动
#    - 网络不通：ping <数据库IP>
#    - 防火墙阻断端口
#    - 用户名/密码错误
#    - 连接数超限
```

### 10.3 内存溢出（OOM）

**症状**：服务崩溃，日志中出现 `java.lang.OutOfMemoryError`

**排查步骤**：

```bash
# 1. 检查堆转储文件
ls -lh $MK_HOME/logs/heap.hprof

# 2. 查看 JVM 堆使用趋势
# 通过 Grafana JVM 看板观察

# 3. 临时增大堆内存
# 在 medkernel.env 中修改 JAVA_OPTS
JAVA_OPTS="-server -Xms4g -Xmx8g -XX:+UseG1GC ..."

# 4. 分析堆转储（需下载到本地使用 MAT/JVisualVM）
```

### 10.4 磁盘空间不足

**症状**：告警 `MedKernelDiskSpaceWarning/Critical`，或服务写入失败

**排查步骤**：

```bash
# 1. 查看磁盘使用
df -h

# 2. 查看大文件
du -sh $MK_HOME/logs/*
du -sh $MK_HOME/*

# 3. 清理旧日志
find $MK_HOME/logs -name "*.log.*" -mtime +30 -delete
find $MK_HOME/logs -name "heap.hprof" -mtime +7 -delete

# 4. 清理旧备份
find $MK_HOME.bak -maxdepth 1 -type d -mtime +90 | xargs rm -rf

# 5. Prometheus 数据清理（如监控栈在本机）
# 注意：这会删除历史指标数据
```

### 10.5 告警风暴处理

**症状**：短时间内收到大量告警通知

**处理步骤**：

1. **确认是否为真实故障**：查看 Grafana 看板，确认指标是否异常
2. **如果是误报**：
   - 检查 Prometheus 采集是否正常：访问 `http://<IP>:9090/targets`
   - 检查告警规则阈值是否合理
   - 临时静默告警：在 Alertmanager UI 中设置静默规则
3. **如果是真实故障**：
   - 优先处理 Critical 级别告警
   - 参考本文档对应章节排查
4. **调整告警规则**（避免再次风暴）：
   - 适当增大 `for` 持续时间
   - 调整阈值到合理范围
   - 配置告警分组和抑制规则

---

## 附录 A：环境变量速查表

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `MK_HOME` | `/zoesoft/medkernel` | 安装根目录 |
| `MK_USER` | `medkernel` | 运行用户 |
| `MK_BACKUP_DIR` | `/zoesoft/medkernel.bak` | 备份目录 |
| `JAVA_HOME` | `/usr` | JDK 路径 |
| `JAVA_OPTS` | 见 8.1 节 | JVM 参数 |
| `MEDKERNEL_HTTP_PORT` | `18080` | 业务端口 |
| `MEDKERNEL_HTTP_CONTEXT` | `/medkernel` | 上下文路径 |
| `MEDKERNEL_MANAGEMENT_PORT` | `18081` | 管理端口 |
| `MEDKERNEL_DB_DIALECT` | `oracle` | 数据库方言 |
| `MEDKERNEL_DB_URL` | — | 数据库连接 URL |
| `MEDKERNEL_DB_USERNAME` | — | 数据库用户名 |
| `MEDKERNEL_DB_PASSWORD` | — | 数据库密码 |
| `MEDKERNEL_DB_POOL_MAX` | `20` | 连接池最大连接数 |
| `MEDKERNEL_DB_POOL_MIN_IDLE` | `2` | 连接池最小空闲 |
| `MEDKERNEL_DB_POOL_TIMEOUT` | `3000` | 获取连接超时(ms) |
| `MEDKERNEL_DB_POOL_LEAK` | `2000` | 连接泄漏检测(ms) |
| `MEDKERNEL_JWT_SECRET` | — | JWT 密钥（必填） |
| `MEDKERNEL_LOCK_THRESHOLD` | `5` | 登录失败锁定次数 |
| `MEDKERNEL_LOCK_DURATION` | `30` | 锁定时长(分钟) |
| `MEDKERNEL_SM4_MASTER_KEY` | — | SM4 主密钥 |
| `MEDKERNEL_SM4_PREVIOUS_MASTER_KEY` | — | SM4 旧主密钥 |
| `MEDKERNEL_FLYWAY_ENABLED` | `false` | 是否启用 Flyway |
| `MEDKERNEL_GRAPH_ENABLED` | `false` | 是否启用图谱 |
| `MEDKERNEL_DIFY_ENABLED` | `false` | 是否启用 Dify |

## 附录 B：端口清单

| 端口 | 协议 | 用途 | 访问范围 |
|------|------|------|---------|
| 80 | HTTP | Nginx 前端 | 内网 |
| 443 | HTTPS | Nginx 前端（TLS） | 内网 |
| 18080 | HTTP | Spring Boot 业务端口 | 仅 127.0.0.1（Nginx 回环） |
| 18081 | HTTP | Actuator 管理端口 | 仅 127.0.0.1（Prometheus 采集） |
| 9090 | HTTP | Prometheus Web UI | 内网（运维） |
| 9093 | HTTP | Alertmanager Web UI | 内网（运维） |
| 3000 | HTTP | Grafana Web UI | 内网（运维） |

## 附录 C：关键文件路径

| 文件 | 路径 | 说明 |
|------|------|------|
| 环境变量 | `$MK_HOME/conf/medkernel.env` | 数据库凭据、端口、JVM 参数 |
| Spring Boot 配置 | `$MK_HOME/conf/application.yml` | 主配置文件 |
| systemd 服务 | `/etc/systemd/system/medkernel.service` | 服务定义 |
| Nginx 配置 | `/etc/nginx/conf.d/medkernel.conf` | 反向代理配置 |
| 健康检查脚本 | `$MK_HOME/scripts/healthcheck.sh` | 健康检查 |
| 升级脚本 | `$MK_HOME/scripts/upgrade.sh` | 版本升级 |
| 回滚脚本 | `$MK_HOME/scripts/rollback.sh` | 版本回滚 |
| 安装脚本 | `$MK_HOME/scripts/install-offline.sh` | 离线安装 |
| 环境检查脚本 | `$MK_HOME/scripts/check-env.sh` | 部署前环境检查 |
| 告警规则 | `deploy/monitoring/prometheus/medkernel-alert-rules.yml` | Prometheus 告警规则 |
| Alertmanager 配置 | `deploy/monitoring/alertmanager/alertmanager.yml` | 告警通知配置 |
