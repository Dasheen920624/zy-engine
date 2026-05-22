# MedKernel v1.0 GA — SLI/SLO 定义与合规证据

> 版本：1.0 | 状态：GA | 日期：2026-05-23

## 1. 概述

本文档定义 MedKernel v1.0 GA 的服务质量指标（SLI）与服务质量目标（SLO），
为医院客户提供可量化的运维质量承诺，并为等保 2.0 三级审计提供合规证据。

## 2. SLI 定义

| SLI 编号 | 指标名称 | 计算方式 | 数据源 |
|----------|---------|---------|--------|
| SLI-01 | API 可用性 | 成功请求（非 5xx）占比 | Prometheus `http_server_requests_seconds_count` |
| SLI-02 | API P95 延迟 | 95 百分位响应时间 | Prometheus `http_server_requests_seconds_bucket` |
| SLI-03 | API 错误率 | 5xx 响应占比 | Prometheus `http_server_requests_seconds_count{status=~"5.."}` |
| SLI-04 | 数据库连接池饱和度 | 活跃连接/最大连接 | Prometheus `hikaricp_connections_active/max` |
| SLI-05 | JVM 堆使用率 | 已用堆/最大堆 | Prometheus `jvm_memory_used/max_bytes{area="heap"}` |
| SLI-06 | Provider 就绪率 | ready provider / configured provider | Prometheus `medkernel_provider_ready/configured` |

## 3. SLO 目标

### 3.1 核心 SLO（P0 — 必须满足）

| SLO 编号 | 对应 SLI | 目标值 | 测量窗口 | 违约动作 |
|----------|---------|--------|---------|---------|
| SLO-01 | SLI-01 可用性 | ≥ 99.5% | 30 天滚动 | 自动告警 + 事件复盘 |
| SLO-02 | SLI-02 P95 延迟 | ≤ 500ms | 30 天滚动 | 自动告警 + 性能优化 |
| SLO-03 | SLI-03 错误率 | ≤ 0.1% | 30 天滚动 | 自动告警 + 根因分析 |

### 3.2 运维 SLO（P1 — 建议满足）

| SLO 编号 | 对应 SLI | 目标值 | 测量窗口 | 违约动作 |
|----------|---------|--------|---------|---------|
| SLO-04 | SLI-04 连接池饱和度 | ≤ 80% | 5 分钟 | 自动告警 |
| SLO-05 | SLI-05 JVM 堆使用率 | ≤ 75% | 10 分钟 | 自动告警 |
| SLO-06 | SLI-06 Provider 就绪率 | 100%（核心） | 5 分钟 | 自动告警 |

### 3.3 SLA 套餐（合同模板参考）

| 套餐 | 可用性 SLO | P95 延迟 SLO | 错误率 SLO | 适用场景 |
|------|-----------|-------------|-----------|---------|
| 标准 | 99.5% | ≤ 500ms | ≤ 0.1% | 本地部署 |
| 黄金 | 99.9% | ≤ 300ms | ≤ 0.05% | 区域 SaaS |
| 白金 | 99.95% | ≤ 200ms | ≤ 0.01% | 中心 SaaS |

## 4. Error Budget

基于 SLO-01（99.5% 可用性），30 天 error budget 计算：

```
30 天总分钟数 = 43,200 分钟
允许宕机时间 = 43,200 × 0.5% = 216 分钟/月 ≈ 3.6 小时/月
```

Error Budget 消耗追踪：

| 月份 | 可用性 | Error Budget 消耗 | 剩余 | 状态 |
|------|--------|------------------|------|------|
| 2026-05 | 待运行时数据 | — | 216 min | — |

## 5. 告警规则与 SLO 映射

| 告警名称 | 对应 SLO | 阈值 | 持续时间 | 级别 |
|----------|---------|------|---------|------|
| MedKernelSLOAvailabilityBreach | SLO-01 | < 99.5% | 5m | critical |
| MedKernelSLOLatencyBreach | SLO-02 | > 500ms | 5m | warning |
| MedKernelApi5xxRateCritical | SLO-03 | > 2% | 2m | critical |
| MedKernelApi5xxRateHigh | SLO-03 | > 0.5% | 5m | warning |
| MedKernelHikariPoolSaturation | SLO-04 | > 80% | 5m | warning |
| MedKernelJvmHeapHigh | SLO-05 | > 75% | 10m | warning |
| MedKernelDatabaseProviderDown | SLO-06 | 0 | 2m | critical |
| MedKernelModelGatewayDown | SLO-06 | 0 | 3m | critical |

## 6. 合规证据

### 6.1 监控基础设施

| 组件 | 版本 | 状态 | 证据 |
|------|------|------|------|
| Spring Boot Actuator | 3.x | 已启用 | application.yml: management.endpoints |
| Micrometer Prometheus | 1.13.x | 已启用 | pom.xml: micrometer-registry-prometheus |
| Prometheus | 2.54.1 | 已配置 | deploy/monitoring/prometheus/ |
| Alertmanager | 0.27.0 | 已配置 | deploy/monitoring/alertmanager/ |
| Grafana | 11.2.0 | 已配置 | deploy/monitoring/grafana/ |
| Docker Compose | — | 已编排 | deploy/docker-compose.monitoring.yml |

### 6.2 健康检查端点

| 端点 | 端口 | 用途 |
|------|------|------|
| `/medkernel/actuator/health` | 18081 | K8s liveness/readiness probe |
| `/medkernel/actuator/prometheus` | 18081 | Prometheus metrics 采集 |
| `/medkernel/api/health` | 18080 | 业务存活检查 |
| `/medkernel/api/system/providers` | 18080 | Provider 就绪状态 |
| `/healthz`（Nginx 代理） | 80/443 | 外部负载均衡器探测 |

### 6.3 告警规则覆盖

| 类别 | 规则数 | 覆盖范围 |
|------|--------|---------|
| 后端核心 | 9 | 服务存活、API 延迟、5xx 错误率、JVM 堆、Provider、连接池 |
| 系统资源 | 4 | 磁盘空间、CPU 使用率、文件描述符 |
| 业务指标 | 4 | Model Gateway、Database、Graph Provider、持续错误率 |
| SLO 违规 | 2 | 可用性违约、延迟违约 |
| **合计** | **19** | — |

## 7. 运维手册参考

- 部署脚本：`deploy/scripts/healthcheck.sh`
- Nginx 配置：`deploy/nginx/medkernel.conf`
- Systemd 服务：`deploy/systemd/medkernel.service`
- Docker Compose：`deploy/docker-compose.monitoring.yml`
- Grafana 看板：`deploy/monitoring/grafana/dashboards/`

## 8. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-05-23 | 1.0 | 初始版本：SLI/SLO 定义、告警映射、合规证据 |
