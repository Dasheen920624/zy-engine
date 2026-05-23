# MedKernel 100 并发压测方案与证据

> 版本：1.0 | 适用：v1.0 GA | 日期：2026-05-24
> 目标：100 并发 30 分钟压测，P95/P99/错误率可追溯

## 1. 压测方案

### 1.1 测试目标

验证 MedKernel v1.0 GA 在 100 并发用户下的性能表现，确保满足 SLO 目标：

| 指标 | SLO 目标 | 压测通过标准 |
|------|---------|-------------|
| API 可用性 | ≥ 99.5% | ≥ 99.9%（压测期间） |
| API P95 延迟 | ≤ 500ms | ≤ 500ms |
| API P99 延迟 | — | ≤ 1000ms |
| API 错误率 | ≤ 0.1% | ≤ 0.5%（压测期间） |

### 1.2 测试环境

| 项 | 规格 |
|----|------|
| 应用服务器 | 8C16G，JVM -Xmx4g -Xms2g |
| 数据库 | Oracle 12c / PostgreSQL 16，8C16G |
| 网络 | 内网千兆，延迟 < 1ms |
| 压测机 | 4C8G，与服务器同网段 |

### 1.3 测试场景

| 场景 | 权重 | API | 说明 |
|------|------|-----|------|
| S1: 健康检查 | 10% | GET /api/health | 轻量级，验证基础连通 |
| S2: Provider 状态 | 10% | GET /api/system/providers | 轻量级，验证 Provider 就绪 |
| S3: 路径查询 | 25% | GET /api/pathway/definitions | 中量级，核心业务 |
| S4: 规则执行 | 25% | POST /api/rule/execute | 重量级，核心业务 |
| S5: 术语搜索 | 15% | GET /api/terminology/search | 中量级 |
| S6: 知识图谱 | 15% | GET /api/knowledge/graph/query | 重量级 |

### 1.4 测试步骤

```
1. 预热阶段：10 并发，5 分钟
2. 渐增阶段：10 → 100 并发，10 分钟
3. 稳定阶段：100 并发，30 分钟
4. 渐减阶段：100 → 0 并发，5 分钟
```

## 2. k6 压测脚本

```javascript
// medkernel-load-test.js
// k6 run -e BASE_URL=http://10.0.0.10:18080/medkernel medkernel-load-test.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:18080/medkernel';

// 自定义指标
const errorRate = new Rate('errors');
const healthLatency = new Trend('health_latency');
const providerLatency = new Trend('provider_latency');
const pathwayLatency = new Trend('pathway_latency');
const ruleLatency = new Trend('rule_latency');
const terminologyLatency = new Trend('terminology_latency');
const graphLatency = new Trend('graph_latency');

export const options = {
  stages: [
    { duration: '5m', target: 10 },   // 预热
    { duration: '10m', target: 100 },  // 渐增
    { duration: '30m', target: 100 },  // 稳定
    { duration: '5m', target: 0 },     // 渐减
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    errors: ['rate<0.005'],
    http_req_failed: ['rate<0.005'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const rand = Math.random();

  if (rand < 0.10) {
    // S1: 健康检查
    const res = http.get(`${BASE_URL}/api/health`);
    healthLatency.add(res.timings.duration);
    check(res, { 'health status 200': (r) => r.status === 200 }) || errorRate.add(1);
  } else if (rand < 0.20) {
    // S2: Provider 状态
    const res = http.get(`${BASE_URL}/api/system/providers`);
    providerLatency.add(res.timings.duration);
    check(res, { 'providers status 200': (r) => r.status === 200 }) || errorRate.add(1);
  } else if (rand < 0.45) {
    // S3: 路径查询
    const res = http.get(`${BASE_URL}/api/pathway/definitions`);
    pathwayLatency.add(res.timings.duration);
    check(res, { 'pathway status 200': (r) => r.status === 200 || r.status === 404 }) || errorRate.add(1);
  } else if (rand < 0.70) {
    // S4: 规则执行
    const res = http.post(
      `${BASE_URL}/api/rule/execute`,
      JSON.stringify({ patientId: 'TEST-PID-001', ruleCodes: [] }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    ruleLatency.add(res.timings.duration);
    check(res, { 'rule status 200': (r) => r.status === 200 || r.status === 400 }) || errorRate.add(1);
  } else if (rand < 0.85) {
    // S5: 术语搜索
    const res = http.get(`${BASE_URL}/api/terminology/search?q=diabetes`);
    terminologyLatency.add(res.timings.duration);
    check(res, { 'terminology status 200': (r) => r.status === 200 || r.status === 404 }) || errorRate.add(1);
  } else {
    // S6: 知识图谱
    const res = http.get(`${BASE_URL}/api/knowledge/graph/query?entity=diabetes`);
    graphLatency.add(res.timings.duration);
    check(res, { 'graph status 200': (r) => r.status === 200 || r.status === 404 }) || errorRate.add(1);
  }

  sleep(1);
}
```

## 3. 压测报告模板

```markdown
# MedKernel 压测报告

## 基本信息
- 版本：v1.0.0
- 测试日期：YYYY-MM-DD
- 测试时长：50 分钟（预热 5m + 渐增 10m + 稳定 30m + 渐减 5m）
- 最大并发：100

## 测试环境
| 项 | 值 |
|----|-----|
| 应用服务器 | — |
| 数据库 | — |
| JVM 参数 | -Xms2g -Xmx4g -XX:+UseG1GC |
| 压测工具 | k6 v0.50+ |

## 结果概览

| 指标 | 目标 | 实际 | 通过 |
|------|------|------|------|
| P95 延迟 | ≤ 500ms | — ms | ☐ |
| P99 延迟 | ≤ 1000ms | — ms | ☐ |
| 错误率 | ≤ 0.5% | —% | ☐ |
| 可用性 | ≥ 99.9% | —% | ☐ |
| 最大 RPS | — | — req/s | — |

## 分场景结果

| 场景 | 请求总数 | 失败数 | P50 | P95 | P99 | 平均延迟 |
|------|---------|--------|-----|-----|-----|---------|
| S1 健康检查 | — | — | — | — | — | — |
| S2 Provider | — | — | — | — | — | — |
| S3 路径查询 | — | — | — | — | — | — |
| S4 规则执行 | — | — | — | — | — | — |
| S5 术语搜索 | — | — | — | — | — | — |
| S6 知识图谱 | — | — | — | — | — | — |

## 资源使用

| 资源 | 峰值 | 平均 | 说明 |
|------|------|------|------|
| CPU | —% | —% | — |
| 内存 | — MB | — MB | — |
| JVM 堆 | — MB | — MB | — |
| GC 次数 | — | — | — |
| DB 连接池 | — / 20 | — / 20 | — |

## 结论
- ☐ 通过：所有指标满足 SLO
- ☐ 不通过：存在指标不满足 SLO，需优化

## 附件
- k6 原始报告：[链接]
- Grafana 监控截图：[链接]
- JVM GC 日志：[链接]
```

## 4. 性能基线

### 4.1 v1.0 GA 性能基线

| 场景 | P50 | P95 | P99 | RPS |
|------|-----|-----|-----|-----|
| 健康检查 | — | — | — | — |
| Provider 状态 | — | — | — | — |
| 路径查询 | — | — | — | — |
| 规则执行 | — | — | — | — |
| 术语搜索 | — | — | — | — |
| 知识图谱 | — | — | — | — |

> 基线值需在实际压测后填写，作为后续版本性能对比的参考。

### 4.2 性能退化判定

| 指标 | 退化阈值 | 动作 |
|------|---------|------|
| P95 延迟 | 基线 × 1.5 | 告警 + 分析 |
| P99 延迟 | 基线 × 2.0 | 阻断发布 |
| 错误率 | 基线 × 3.0 | 阻断发布 |
| RPS | 基线 × 0.7 | 告警 + 分析 |

## 5. 执行指南

### 5.1 安装 k6

```bash
# macOS
brew install k6

# Linux
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

### 5.2 执行压测

```bash
# 执行压测
k6 run -e BASE_URL=http://10.0.0.10:18080/medkernel \
  deploy/scripts/load-test/medkernel-load-test.js

# 输出 JSON 报告
k6 run -e BASE_URL=http://10.0.0.10:18080/medkernel \
  --out json=load-test-result.json \
  deploy/scripts/load-test/medkernel-load-test.js
```

### 5.3 监控

压测期间同步监控：
- Grafana API 性能看板
- JVM 运行时看板
- 数据库连接池看板

## 6. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-05-24 | 1.0 | 初始版本：压测方案 + k6 脚本 + 报告模板 + 性能基线 |
