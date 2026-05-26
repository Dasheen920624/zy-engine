# MedKernel · 性能压测目录

## 工具

- [k6](https://k6.io) 1000 并发主压测
- JMeter（备选）

## 脚本清单

| 脚本 | 用途 | 验收 |
|---|---|---|
| `k6-1000-concurrent.js` | GA-PERF-01 主压测（1000 并发 60min）| P95<300ms / P99<800ms / 错误率<0.1% |
| `k6-llm-degradation.js` | LLM 降级链端到端 | 主备切换 P95<1.5s |
| `baseline-report.md` | 当前性能基线报告模板 | 待首次正式压测后填充 |
| `SLO.md` | 服务等级目标定义 | — |
| `load-test-plan.md` | 压测计划与场景设计 | — |

## 一键启动

```bash
# Install k6（macOS: brew install k6 / Windows: choco install k6 / Linux: see https://k6.io/docs/get-started/installation/）

# 主压测（需 60 分钟）
k6 run docs/performance/k6-1000-concurrent.js

# LLM 降级链（约 12 分钟）
k6 run docs/performance/k6-llm-degradation.js

# 自定义环境
BASE_URL=https://medkernel-staging.your-hospital.cn k6 run docs/performance/k6-1000-concurrent.js
```

## 结果归档

每次压测后把以下文件归档到 `docs/release/evidence/v1.0.0-perf-YYYYMMDD/`：

- k6 控制台输出 (`*.log`)
- HikariCP leak detection 日志（如有）
- 后端 JFR / Async profiler 报告（建议 60 分钟内连续采）
- Prometheus snapshot（请求量 / P95 / GC / Hikari 池）

## 与 GA 6 大验收硬指标对齐

| 维度 | 目标 | 脚本 |
|---|---|---|
| 高并发 | 1000 并发 60min | `k6-1000-concurrent.js` |
| 长时稳定 | 7×24 稳定运行 | 单独长时跑 |
| 数据规模 | 5000 万 MPI | 由 Testcontainers + 单独种数据脚本 |
| LLM 降级链 | 主挂自动切备 P95<1.5s | `k6-llm-degradation.js` |
| 连接池 | 0 泄漏 | `leakDetectionThreshold=30000` 自动告警 |
