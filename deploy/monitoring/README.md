# MedKernel Monitoring Pack

PR-FINAL-24 provides the on-prem monitoring baseline for v0.3-final.
GA-OPS-01 adds SLO definitions, burn-rate alerts, security dashboards, and enhanced healthcheck.

- Spring Boot actuator endpoint: `http://127.0.0.1:18081/medkernel/actuator/prometheus`
- Prometheus scrape template: `prometheus/prometheus-medkernel.yml`
- Prometheus alert rules: `prometheus/medkernel-alert-rules.yml`
- Grafana provisioning: `grafana/provisioning`
- Grafana dashboards: system, JVM, DB pool, API performance, business operations, **SLO**, **Security**

## SLO 定义 (GA-OPS-01)

| SLO | 目标 | 计算方式 | 月度预算 |
|---|---|---|---|
| 可用性 | 99.9% | 非 5xx 请求 / 总请求 | ≤ 43.8 分钟停机 |
| 延迟 (P95) | < 300ms | histogram_quantile(0.95, ...) | 5% 请求可超 300ms |
| 错误率 | < 0.1% | 5xx 请求 / 总请求 | 0.1% 错误率 |
| 饱和度 | 连接池 < 80% | active / max connections | 20% 余量 |

### SLO Burn Rate 告警

采用 Google SRE 推荐的多窗口燃烧率策略：

| 告警 | 窗口 | 燃烧率倍数 | 严重度 |
|---|---|---|---|
| 可用性快速燃烧 | 1h | 14.4x | critical |
| 可用性慢速燃烧 | 6h | 6x | warning |
| 延迟快速燃烧 | 1h | 14.4x | critical |

## 告警分级 (GA-OPS-01)

| 严重度 | 响应时间 | 通知方式 | 示例 |
|---|---|---|---|
| **critical** | ≤ 15 分钟 | 电话 + 短信 + IM | 服务不可达、连接池耗尽、审计日志写入失败、数据脱敏失败 |
| **warning** | ≤ 1 小时 | 短信 + IM | 延迟升高、错误率升高、连接池饱和、SLO 慢速燃烧 |
| **info** | 下一工作日 | IM | 非关键指标异常 |

### 告警分组

| 分组 | 说明 | 关键指标 |
|---|---|---|
| `medkernel-backend` | 基础设施：服务可达性、API 延迟/错误率、JVM 内存、Provider 就绪、连接池 | up, http_server_requests_seconds, jvm_memory, hikaricp |
| `medkernel-business` | 业务层：规则评估、临床路径、配置发布、知识同步、审计日志 | medkernel_rule_eval, medkernel_pathway, medkernel_config_publish |
| `medkernel-database` | 数据库：连接泄漏、慢查询、连接池耗尽 | hikaricp, medkernel_db_query_duration |
| `medkernel-security` | 安全：认证失败、权限拒绝、数据脱敏 | http_server_requests_seconds (401/403), medkernel_data_masking |
| `medkernel-slo` | SLO 燃烧率：可用性/延迟预算消耗 | 组合指标 |

## Grafana 看板 (GA-OPS-01)

| 看板 | UID | 说明 |
|---|---|---|
| MedKernel System | medkernel-system | 系统概览 |
| MedKernel JVM | medkernel-jvm | JVM 内存/线程/GC |
| MedKernel DB Pool | medkernel-dbpool | HikariCP 连接池 |
| MedKernel API | medkernel-api | API 性能 |
| MedKernel Business | medkernel-business | 业务操作 |
| **MedKernel SLO** | medkernel-slo | SLO 概览 + 错误率/延迟趋势 + 活跃告警 |
| **MedKernel Security** | medkernel-security | 安全概览 + 认证/权限/脱敏/审计 |

## Healthcheck 增强 (GA-OPS-01)

healthcheck 脚本已增强，新增：

1. **actuator/health 检查**：验证 Spring Boot Actuator 健康端点返回 UP
2. **Prometheus 可达性检查**：验证 Prometheus 服务可用
3. **SLO 告警检查**：查询 Prometheus 活跃 critical 告警数量

```bash
# Linux/Mac
./deploy/scripts/healthcheck.sh

# Windows
.\deploy\scripts\healthcheck.ps1

# 自定义 Prometheus 地址
PROMETHEUS_URL=http://monitoring:9090 ./deploy/scripts/healthcheck.sh
```

## Runtime Boundary

The default management bind address is `127.0.0.1`, so Prometheus should run on the
same host or behind a local sidecar. If the hospital monitoring platform scrapes
from a dedicated monitoring VLAN, set:

```bash
MEDKERNEL_MANAGEMENT_ADDRESS=0.0.0.0
MEDKERNEL_MANAGEMENT_PORT=18081
MEDKERNEL_MANAGEMENT_BASE_PATH=/medkernel/actuator
```

Only the monitoring network should be allowed to reach `18081` at the firewall or
reverse-proxy layer. Do not expose this port to office, patient, or internet
segments.

Optional Neo4j actuator health is disabled by default because graph integration is
an optional provider in v0.3-final. Enable `MEDKERNEL_MANAGEMENT_NEO4J_HEALTH_ENABLED=true`
only when Neo4j is a required runtime dependency for that deployment.

## Local Validation

```bash
curl -fsS http://127.0.0.1:18081/medkernel/actuator/health
curl -fsS http://127.0.0.1:18081/medkernel/actuator/prometheus | grep medkernel_provider_ready
promtool check config deploy/monitoring/prometheus/prometheus-medkernel.yml
promtool check rules deploy/monitoring/prometheus/medkernel-alert-rules.yml

# GA-OPS-01: 增强健康检查
./deploy/scripts/healthcheck.sh
```

The DB pool dashboard is wired to standard HikariCP metric names. It will start
showing data after the Hikari PR lands in the backend.

## 运维手册引用

详细运维手册请参阅：[docs/engineering/09_内网部署与版本管理.md](../../docs/engineering/09_内网部署与版本管理.md)
