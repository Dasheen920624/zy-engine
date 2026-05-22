# MedKernel v1.0 GA — 性能 SLO 定义

> 版本：1.0 | 状态：GA | 日期：2026-05-23
> 关联任务：GA-PERF-01

## 1. 概述

本文档定义 MedKernel v1.0 GA 的性能服务质量目标（SLO），为医院客户提供可量化的性能承诺，并为等保 2.0 三级审计提供合规证据。

SLO 基于 API 业务关键度分级制定，确保核心临床决策接口满足实时性要求，同时为非核心接口预留合理的性能预算。

本文档与 `docs/slo/medkernel-slo.md` 互补：后者侧重可用性与运维 SLO，本文档侧重性能 SLO 的细粒度定义。

## 2. SLO 目标

### 2.1 核心性能 SLO

| SLO 编号 | 指标 | 目标值 | 测量窗口 | 违约动作 |
|----------|------|--------|---------|---------|
| PERF-SLO-01 | 核心实时 API P95 延迟 | ≤ 300ms | 30 天滚动 | 自动告警 + 性能优化 |
| PERF-SLO-02 | 核心实时 API P99 延迟 | ≤ 500ms | 30 天滚动 | 自动告警 + 根因分析 |
| PERF-SLO-03 | 查询类 API P95 延迟 | ≤ 500ms | 30 天滚动 | 自动告警 + 性能优化 |
| PERF-SLO-04 | 查询类 API P99 延迟 | ≤ 1000ms | 30 天滚动 | 自动告警 + 根因分析 |
| PERF-SLO-05 | 管理类 API P95 延迟 | ≤ 1000ms | 30 天滚动 | 自动告警 |
| PERF-SLO-06 | 管理类 API P99 延迟 | ≤ 2000ms | 30 天滚动 | 自动告警 |
| PERF-SLO-07 | API 错误率（5xx） | ≤ 1% | 30 天滚动 | 自动告警 + 根因分析 |
| PERF-SLO-08 | 100 并发 30 分钟无连接泄漏 | 0 泄漏 | 每次压测 | 阻断发布 |
| PERF-SLO-09 | 系统可用性 | ≥ 99.9% | 30 天滚动 | 事件复盘 + SLA 赔付 |

### 2.2 Error Budget 计算

基于 PERF-SLO-09（99.9% 可用性），30 天 error budget：

```
30 天总分钟数 = 43,200 分钟
允许宕机时间 = 43,200 × 0.1% = 43.2 分钟/月 ≈ 43 分钟/月
```

## 3. API 分级

按业务关键度将 API 分为 P0/P1/P2 三级，不同级别对应不同的性能 SLO。

### 3.1 P0 — 核心实时 API

直接影响临床决策的实时接口，延迟要求最严格。

| API 端点 | HTTP 方法 | 说明 | P95 目标 | P99 目标 |
|----------|----------|------|---------|---------|
| `/api/cdss/evaluate` | POST | CDSS 评估 | 300ms | 500ms |
| `/api/pathways/patient-pathways/candidates` | POST | 路径入径 | 300ms | 500ms |
| `/api/rule-engine/evaluate` | POST | 规则评估 | 300ms | 500ms |
| `/api/cdss/triggers/match` | POST | 触发点匹配 | 300ms | 500ms |
| `/api/adapters/query` | POST | 适配器查询 | 300ms | 500ms |
| `/api/auth/login` | POST | 认证登录 | 300ms | 500ms |
| `/api/auth/sso/callback` | POST | SSO 回调 | 300ms | 500ms |

### 3.2 P1 — 查询类 API

数据查询与列表接口，允许稍高延迟。

| API 端点 | HTTP 方法 | 说明 | P95 目标 | P99 目标 |
|----------|----------|------|---------|---------|
| `/api/cdss/fatigue/alerts` | GET | 告警查询 | 500ms | 1000ms |
| `/api/pathways` | GET | 路径列表 | 500ms | 1000ms |
| `/api/knowledge/sources` | GET | 知识源查询 | 500ms | 1000ms |
| `/api/interop/query` | POST | 互操作查询 | 500ms | 1000ms |
| `/api/quality/dashboard` | GET | 质量仪表盘 | 500ms | 1000ms |
| `/api/rules` | GET | 规则列表 | 500ms | 1000ms |
| `/api/cdss/red-lines` | GET | 安全红线查询 | 500ms | 1000ms |
| `/api/adapters` | GET | 适配器列表 | 500ms | 1000ms |
| `/api/notifications` | GET | 通知查询 | 500ms | 1000ms |
| `/api/clinical-safety/check` | POST | 临床安全检查 | 500ms | 1000ms |

### 3.3 P2 — 管理类 API

配置管理与运维接口，延迟容忍度最高。

| API 端点 | HTTP 方法 | 说明 | P95 目标 | P99 目标 |
|----------|----------|------|---------|---------|
| `/api/knowledge/sync` | POST | 知识同步 | 1000ms | 2000ms |
| `/api/knowledge/packages` | GET | 知识包管理 | 1000ms | 2000ms |
| `/api/security/baseline` | GET | 安全基线 | 1000ms | 2000ms |
| `/api/ops/tasks` | GET | 部署管理 | 1000ms | 2000ms |
| `/api/security/admin` | GET | 安全管理 | 1000ms | 2000ms |
| `/api/admin/users` | GET | 用户管理 | 1000ms | 2000ms |
| `/api/organizations` | GET | 组织管理 | 1000ms | 2000ms |
| `/api/tenant/onboarding` | POST | 租户入驻 | 1000ms | 2000ms |
| `/api/ai-governance` | GET | AI 治理 | 1000ms | 2000ms |
| `/api/ai-safety` | GET | AI 安全 | 1000ms | 2000ms |
| `/api/quality/eval` | GET | 评估报告 | 1000ms | 2000ms |

## 4. 指标定义

### 4.1 百分位延迟

| 指标 | 定义 | 计算方式 |
|------|------|---------|
| P50 | 中位数延迟 | 50% 请求在此时间内完成 |
| P90 | 90 百分位延迟 | 90% 请求在此时间内完成 |
| P95 | 95 百分位延迟 | 95% 请求在此时间内完成（核心 SLO 指标） |
| P99 | 99 百分位延迟 | 99% 请求在此时间内完成（尾部延迟指标） |

**计算公式（基于 Prometheus histogram）：**

```promql
# P95 延迟
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket{application="medkernel-mvp"}[5m]))
  by (le, uri)
)

# P99 延迟
histogram_quantile(0.99,
  sum(rate(http_server_requests_seconds_bucket{application="medkernel-mvp"}[5m]))
  by (le, uri)
)
```

### 4.2 错误率

| 指标 | 定义 | 计算方式 |
|------|------|---------|
| 错误率 | 5xx 响应占比 | 5xx 请求数 / 总请求数 × 100% |

**计算公式：**

```promql
sum(rate(http_server_requests_seconds_count{application="medkernel-mvp",status=~"5.."}[5m]))
/
clamp_min(sum(rate(http_server_requests_seconds_count{application="medkernel-mvp"}[5m])), 0.001)
```

### 4.3 吞吐量

| 指标 | 定义 | 计算方式 |
|------|------|---------|
| RPS | 每秒请求数 | 总请求数 / 压测持续时间（秒） |
| 成功 RPS | 每秒成功请求数 | 非 5xx 请求数 / 压测持续时间（秒） |

### 4.4 并发连接数

| 指标 | 定义 | 计算方式 |
|------|------|---------|
| 并发用户数 | 同时活跃的虚拟用户数 | k6 VU 数量 |
| 活跃连接数 | 数据库连接池活跃连接 | `hikaricp_connections_active` |
| 连接泄漏 | 压测后连接数未恢复基线 | 压测前活跃连接 vs 压测后活跃连接 |

**连接泄漏判定标准：**

- 压测前记录 `hikaricp_connections_active` 基线值
- 100 并发持续 30 分钟压测
- 压测结束后等待 60 秒，再次记录 `hikaricp_connections_active`
- 若差值 > 2，判定为连接泄漏

## 5. 测量方法

### 5.1 压测工具

使用 [k6](https://k6.io/) 作为性能压测工具，原因：

- 脚本化测试场景，支持 JavaScript 编写
- 原生支持 VU（虚拟用户）和阶段式加压
- 内置 Prometheus 远程写入，指标可直接导入监控栈
- 支持 threshold 断言，CI 集成友好

### 5.2 测试配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 压测工具 | k6 v0.50+ | 开源负载测试工具 |
| 并发用户数 | 100 | 模拟中等规模医院场景 |
| 持续时间 | 30 分钟 | 验证稳态性能与连接泄漏 |
| 加压策略 | 阶梯式 | 10→50→100 VU，每阶段 10 分钟 |
| 数据采集 | Prometheus + k6 summary | 双重采集确保数据准确 |
| 测试环境 | 独立环境 | 不与生产环境共享资源 |

### 5.3 测试场景

| 场景编号 | 场景名称 | 并发数 | 持续时间 | 说明 |
|----------|---------|--------|---------|------|
| S1 | P0 核心实时 API 压测 | 100 | 30min | CDSS 评估、路径入径、规则评估 |
| S2 | P1 查询类 API 压测 | 100 | 30min | 告警查询、路径列表、知识源查询 |
| S3 | P2 管理类 API 压测 | 50 | 15min | 配置管理、知识同步 |
| S4 | 混合场景压测 | 100 | 30min | P0:P1:P2 = 5:3:2 流量配比 |
| S5 | 连接泄漏检测 | 100 | 30min | 持续压测后检查连接池状态 |
| S6 | 峰值突发测试 | 200 | 5min | 2x 常规并发验证系统弹性 |

### 5.4 k6 执行命令

```bash
# 基本执行
k6 run --vus 100 --duration 30m scripts/perf/s1-p0-core.js

# 带 threshold 断言
k6 run --vus 100 --duration 30m \
  --out json=results/s1-p0-core.json \
  scripts/perf/s1-p0-core.js

# 输出 Prometheus 指标
k6 run --vus 100 --duration 30m \
  --out experimental-prometheus-rw \
  scripts/perf/s1-p0-core.js
```

## 6. 告警规则

基于 Prometheus/Grafana 的告警体系，与 `deploy/monitoring/prometheus/medkernel-alert-rules.yml` 中的规则互补。

### 6.1 性能 SLO 告警

| 告警名称 | 对应 SLO | PromQL 表达式 | 阈值 | 持续时间 | 级别 |
|----------|---------|--------------|------|---------|------|
| MedKernelP0P95LatencyBreach | PERF-SLO-01 | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri=~"/api/cdss/.*|/api/pathways/patient-pathways/.*|/api/rule-engine/.*|/api/cdss/triggers/.*|/api/adapters/query"}[5m])) by (le))` | > 0.3 | 5m | critical |
| MedKernelP0P99LatencyBreach | PERF-SLO-02 | 同上，quantile=0.99 | > 0.5 | 5m | critical |
| MedKernelP1P95LatencyBreach | PERF-SLO-03 | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri=~"/api/cdss/fatigue/.*|/api/pathways$|/api/knowledge/sources|/api/interop/.*|/api/quality/dashboard"}[5m])) by (le))` | > 0.5 | 5m | warning |
| MedKernelP1P99LatencyBreach | PERF-SLO-04 | 同上，quantile=0.99 | > 1.0 | 5m | warning |
| MedKernelP2P95LatencyBreach | PERF-SLO-05 | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri=~"/api/knowledge/sync/.*|/api/security/.*|/api/ops/.*|/api/admin/.*"}[5m])) by (le))` | > 1.0 | 10m | warning |
| MedKernelErrorRateBreach | PERF-SLO-07 | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count[5m])), 0.001)` | > 0.01 | 5m | critical |
| MedKernelConnPoolLeak | PERF-SLO-08 | `hikaricp_connections_active` 压测后未恢复 | — | — | critical |

### 6.2 Grafana 看板

| 看板名称 | 面板内容 | 数据源 |
|----------|---------|--------|
| MedKernel 性能 SLO | P0/P1/P2 延迟百分位、错误率、吞吐量 | Prometheus |
| MedKernel 连接池监控 | 活跃/空闲/最大连接数、饱和度趋势 | Prometheus |
| MedKernel 压测报告 | k6 压测结果可视化 | k6 JSON + Prometheus |

## 7. SLA 承诺

### 7.1 SLA 条款

面向客户的 SLA 承诺，基于性能 SLO 制定。

| SLA 编号 | 条款 | 承诺值 | 违约赔付 |
|----------|------|--------|---------|
| SLA-PERF-01 | 核心实时 API 可用性 | ≥ 99.9% | 可用性 < 99.9% 时，按宕机时间等比例退还服务费 |
| SLA-PERF-02 | 核心实时 API P95 延迟 | ≤ 300ms | 连续 3 个月 P95 > 300ms 时，提供免费性能优化服务 |
| SLA-PERF-03 | API 错误率 | ≤ 1% | 错误率 > 1% 持续超过 1 小时，启动事件响应 |
| SLA-PERF-04 | 连接泄漏 | 0 泄漏 | 发现连接泄漏后 24 小时内修复，否则提供补偿 |

### 7.2 SLA 套餐

| 套餐 | 可用性 | P0 P95 延迟 | 错误率 | 适用场景 |
|------|--------|------------|--------|---------|
| 标准 | 99.9% | ≤ 300ms | ≤ 1% | 本地部署 |
| 黄金 | 99.95% | ≤ 200ms | ≤ 0.5% | 区域 SaaS |
| 白金 | 99.99% | ≤ 100ms | ≤ 0.1% | 中心 SaaS |

### 7.3 排除条款

以下情况不计入 SLA 违约：

- 客户网络故障导致的延迟
- 客户数据库性能瓶颈导致的延迟
- 计划内维护窗口（提前 48 小时通知）
- 不可抗力因素（自然灾害、政策变更等）
- 客户自定义规则/路径导致的性能退化

## 8. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-05-23 | 1.0 | 初始版本：性能 SLO 定义、API 分级、告警规则、SLA 条款 |
