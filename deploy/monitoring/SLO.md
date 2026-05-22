# MedKernel v1.0 GA SLO

> 版本：1.0 · 2026-05-23

## 1. SLO 定义

| SLO | 目标 | 指标 | 计算方式 |
|-----|------|------|----------|
| 可用性 | 99.5% | `medkernel_provider_ready{provider="database"}` | 月度 UP 时间 / 月度总时间 |
| 规则评估延迟 P95 | < 500ms | `medkernel_rule_evaluation_duration` | histogram_quantile(0.95, rate) |
| 路径操作延迟 P95 | < 1s | `medkernel_pathway_operation_duration` | histogram_quantile(0.95, rate) |
| LLM 调用成功率 | > 90% | `medkernel_llm_call_total` / `medkernel_llm_call_errors_total` | 1 - error_rate |
| 规则评估可用性 | 99% | HTTP 2xx rate on /api/rule-engine/evaluate | 成功请求 / 总请求 |

## 2. 告警规则

| 告警 | 条件 | 严重级别 | 通知 |
|------|------|----------|------|
| 数据库 Provider 不可用 | `medkernel_provider_ready{provider="database"} == 0` 持续 1 分钟 | Critical | 立即 |
| 任一 Provider 不可用 | `medkernel_provider_ready == 0` 持续 2 分钟 | Warning | 5 分钟内 |
| LLM 错误率 > 10% | `rate(errors[5m]) / rate(total[5m]) > 0.1` 持续 5 分钟 | Warning | 5 分钟内 |
| 规则评估 P95 > 1s | `histogram_quantile(0.95, rate) > 1` 持续 5 分钟 | Warning | 5 分钟内 |

## 3. 监控端点

| 端点 | 用途 |
|------|------|
| `GET /medkernel/actuator/health` | Kubernetes liveness/readiness 探针 |
| `GET /medkernel/actuator/health/readiness` | 就绪探针（含 Provider 状态） |
| `GET /medkernel/actuator/prometheus` | Prometheus 指标采集 |
| `GET /api/health` | 业务健康检查 |
| `GET /api/system/providers` | Provider 详细状态 |

## 4. 业务指标清单

| 指标 | 类型 | 说明 |
|------|------|------|
| `medkernel_provider_configured` | Gauge | Provider 是否已配置 |
| `medkernel_provider_ready` | Gauge | Provider 是否就绪 |
| `medkernel_model_provider_count` | Gauge | 已注册模型 Provider 数量 |
| `medkernel_capability_enabled` | Gauge | 产品能力开关 |
| `medkernel_rule_evaluation_duration` | Timer | 规则评估耗时 |
| `medkernel_rule_evaluation_total` | Counter | 规则评估总次数 |
| `medkernel_rule_hit_total` | Counter | 规则命中总次数 |
| `medkernel_pathway_operation_duration` | Timer | 路径操作耗时 |
| `medkernel_pathway_operation_total` | Counter | 路径操作总次数 |
| `medkernel_llm_call_duration` | Timer | LLM 调用耗时 |
| `medkernel_llm_call_total` | Counter | LLM 调用总次数 |
| `medkernel_llm_call_errors_total` | Counter | LLM 调用错误次数 |
| `medkernel_quality_alert_total` | Counter | 质控预警总次数 |
| `medkernel_cdss_override_total` | Counter | CDSS 覆盖总次数 |

## 5. 运维部署

```bash
# Prometheus
docker run -d -p 9090:9090 \
  -v ./deploy/monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml \
  -v ./deploy/monitoring/prometheus/alert_rules.yml:/etc/prometheus/alert_rules.yml \
  prom/prometheus

# Grafana
docker run -d -p 3000:3000 \
  -v ./deploy/monitoring/grafana/dashboards:/var/lib/grafana/dashboards \
  grafana/grafana
```
