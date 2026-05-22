# MedKernel 性能测试指南

> 关联任务：GA-PERF-01

本指南说明如何运行 MedKernel 性能测试、分析结果并集成到 CI 流程。

## 1. 如何运行性能测试

### 1.1 前置条件

- MedKernel 后端已启动，业务端口 18080 可达
- Actuator 端口 18081 可达（用于监控指标采集）
- k6 已安装（见第 2 节）
- 测试数据已准备（见 `ai-dev-input/07_tests/datasets/`）

### 1.2 快速开始

```bash
# 1. 确认后端服务可用
curl -s http://localhost:18080/medkernel/api/health | head -1

# 2. 运行 P0 核心实时 API 压测
k6 run scripts/perf/s1-p0-core.js

# 3. 运行全部压测场景
./scripts/perf/run-all.sh

# 4. 查看结果
ls results/
```

### 1.3 压测脚本清单

| 脚本 | 场景 | 并发数 | 持续时间 | 说明 |
|------|------|--------|---------|------|
| `scripts/perf/s1-p0-core.js` | P0 核心实时 API | 100 | 30min | CDSS 评估、路径入径、规则评估 |
| `scripts/perf/s2-p1-query.js` | P1 查询类 API | 100 | 30min | 告警查询、路径列表、知识源查询 |
| `scripts/perf/s3-p2-admin.js` | P2 管理类 API | 50 | 15min | 配置管理、知识同步 |
| `scripts/perf/s4-mixed.js` | 混合场景 | 100 | 30min | P0:P1:P2 = 5:3:2 |
| `scripts/perf/s5-conn-leak.js` | 连接泄漏检测 | 100 | 30min | 压测前后连接池对比 |
| `scripts/perf/s6-burst.js` | 峰值突发 | 200 | 5min | 2x 常规并发 |
| `scripts/perf/run-all.sh` | 全量执行 | — | — | 顺序执行所有场景 |

## 2. k6 安装方法

### 2.1 macOS

```bash
# Homebrew
brew install k6

# 验证安装
k6 version
```

### 2.2 Linux

```bash
# Debian/Ubuntu
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491B6F3A4EA
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# RHEL/CentOS
sudo dnf install https://dl.k6.io/rpm/repo.rpm
sudo dnf install k6

# 验证安装
k6 version
```

### 2.3 Windows

```powershell
# Chocolatey
choco install k6

# 验证安装
k6 version
```

### 2.4 Docker

```bash
docker pull grafana/k6:latest

# 运行压测
docker run -v ./scripts/perf:/scripts grafana/k6 run /scripts/s1-p0-core.js
```

## 3. 环境变量配置

### 3.1 必需变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `MEDKERNEL_BASE_URL` | `http://localhost:18080` | MedKernel 业务 API 基础 URL |
| `MEDKERNEL_AUTH_URL` | `http://localhost:18080` | 认证 API 基础 URL |

### 3.2 可选变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `MEDKERNEL_TENANT_ID` | `default` | 租户 ID |
| `MEDKERNEL_TEST_USER` | `admin` | 压测用户名 |
| `MEDKERNEL_TEST_PASSWORD` | `admin123` | 压测密码 |
| `K6_PROMETHEUS_RW_SERVER_URL` | `http://localhost:9090/api/v1/write` | Prometheus 远程写入地址 |
| `K6_OUT` | — | k6 输出格式（如 `json=results/output.json`） |

### 3.3 配置方式

```bash
# 方式一：环境变量
export MEDKERNEL_BASE_URL=http://staging.medkernel.local:18080
k6 run scripts/perf/s1-p0-core.js

# 方式二：.env 文件
cat > .env.perf <<'EOF'
MEDKERNEL_BASE_URL=http://staging.medkernel.local:18080
MEDKERNEL_TENANT_ID=hospital-001
MEDKERNEL_TEST_USER=perf_tester
MEDKERNEL_TEST_PASSWORD=secure_password
EOF

# 方式三：命令行参数
k6 run -e MEDKERNEL_BASE_URL=http://staging.medkernel.local:18080 \
  scripts/perf/s1-p0-core.js
```

## 4. 测试脚本说明

### 4.1 脚本结构

每个 k6 测试脚本遵循以下结构：

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// 1. 自定义指标定义
const errorRate = new Rate('errors');
const cdssLatency = new Trend('cdss_evaluate_latency', true);

// 2. 测试选项（含 SLO threshold）
export const options = {
  stages: [
    { duration: '1m', target: 10 },    // 预热
    { duration: '9m', target: 50 },    // 逐步加压
    { duration: '10m', target: 100 },  // 持续加压
    { duration: '10m', target: 100 },  // 稳态
  ],
  thresholds: {
    http_req_duration: ['p(95)<300', 'p(99)<500'],  // P0 SLO
    errors: ['rate<0.01'],                            // 错误率 SLO
  },
};

// 3. 初始化（加载测试数据）
const baseUrl = __ENV.MEDKERNEL_BASE_URL || 'http://localhost:18080';

// 4. 测试主函数
export default function () {
  const res = http.post(`${baseUrl}/medkernel/api/cdss/evaluate`, JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'has result': (r) => JSON.parse(r.body).data !== undefined,
  });

  errorRate.add(res.status >= 400);
  cdssLatency.add(res.timings.duration);
  sleep(1);
}
```

### 4.2 SLO Threshold 配置

各场景的 threshold 对应 `docs/performance/SLO.md` 中的 SLO 目标：

| 场景 | P95 threshold | P99 threshold | 错误率 threshold |
|------|-------------|-------------|----------------|
| S1 P0 核心 | `p(95)<300` | `p(99)<500` | `rate<0.01` |
| S2 P1 查询 | `p(95)<500` | `p(99)<1000` | `rate<0.01` |
| S3 P2 管理 | `p(95)<1000` | `p(99)<2000` | `rate<0.01` |
| S4 混合 | `p(95)<500` | `p(99)<1000` | `rate<0.01` |

### 4.3 测试数据

测试数据位于 `ai-dev-input/07_tests/datasets/`，脚本通过 `open()` 或 `SharedArray` 加载：

```javascript
import { SharedArray } from 'k6/data';

const patients = new SharedArray('patients', function () {
  return JSON.parse(open('../../ai-dev-input/07_tests/datasets/patient/ami-patient.json'));
});
```

## 5. 结果分析方法

### 5.1 k6 控制台输出

k6 运行结束后自动输出摘要，关注以下指标：

```
     ✓ cdss_evaluate_p95_under_300ms   ← threshold 是否通过
     ✓ error_rate_under_1_percent       ← 错误率是否达标

     http_req_duration..........: avg=85ms  p(95)=198ms  p(99)=385ms
     http_req_failed............: 0.12%
     http_reqs..................: 542310  301.285/s      ← 吞吐量
     vus........................: 100
```

**关键判定**：

- `✓` 表示 threshold 通过，`✗` 表示未通过
- `p(95)` 和 `p(99)` 对应 SLO 中的延迟目标
- `http_req_failed` 对应错误率目标
- 若任何 threshold 未通过，k6 退出码为非 0

### 5.2 JSON 结果分析

```bash
# 生成 JSON 结果
k6 run --out json=results/s1-p0-core.json scripts/perf/s1-p0-core.js

# 使用 k6 报告汇总
k6 run --out json=results/s1-p0-core.json scripts/perf/s1-p0-core.js

# 使用 jq 分析
cat results/s1-p0-core.json | jq -s '
  .[] | select(.type=="Point" and .metric=="http_req_duration")
  | .data.value
' | jq -s 'sort | .[ (length | floor * 0.95 | floor) ]'
```

### 5.3 Grafana 看板分析

1. 启动监控栈：`docker compose -f deploy/docker-compose.monitoring.yml up -d`
2. 访问 Grafana：`http://localhost:3000`（admin / medkernel_admin）
3. 导入看板：`deploy/monitoring/grafana/dashboards/`
4. 关注面板：
   - API 延迟百分位趋势
   - 错误率趋势
   - 连接池饱和度
   - JVM 堆使用率

### 5.4 填写基线报告

将测试结果填入 `docs/performance/baseline-report.md` 模板：

1. 填写报告信息（版本、日期、环境）
2. 填写各场景测试结果表格
3. 填写资源监控数据
4. 进行 SLO 达标评估
5. 编写结论与建议

## 6. CI 集成方法

### 6.1 GitHub Actions 集成

在 `.github/workflows/ci.yml` 中添加性能测试 Job：

```yaml
  # GA-PERF-01: 性能压测（仅在 develop 分支合并时触发）
  performance-test:
    name: performance-test
    runs-on: windows-latest
    needs: backend-build-test
    if: github.event_name == 'push' && github.ref == 'refs/heads/develop'
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Install k6
        run: |
          choco install k6 -y
          k6 version

      - name: Start MedKernel backend
        run: |
          # 启动后端服务（使用 H2 内存数据库）
          cd medkernel-mvp
          ./mvnw spring-boot:run -Dspring-boot.run.profiles=perf &
          sleep 60  # 等待服务就绪

      - name: Health check
        run: |
          curl -sf http://localhost:18080/medkernel/api/health || exit 1

      - name: Run P0 core API perf test
        run: |
          k6 run --vus 50 --duration 5m \
            -e MEDKERNEL_BASE_URL=http://localhost:18080 \
            scripts/perf/s1-p0-core.js

      - name: Run P1 query API perf test
        run: |
          k6 run --vus 50 --duration 5m \
            -e MEDKERNEL_BASE_URL=http://localhost:18080 \
            scripts/perf/s2-p1-query.js

      - name: Upload perf results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: perf-test-results
          path: results/
          retention-days: 30
```

### 6.2 CI 中的精简策略

CI 环境资源有限，采用精简配置：

| 参数 | 本地压测 | CI 压测 |
|------|---------|---------|
| 并发数 | 100 VU | 50 VU |
| 持续时间 | 30 min | 5 min |
| 场景覆盖 | S1-S6 | S1 + S2 |
| threshold | 完整 SLO | 放宽 20% |

### 6.3 阻断策略

- k6 threshold 未通过时，退出码为非 0，CI 自动阻断
- 连接泄漏检测失败时，CI 标记为 `failure`
- P0 SLO 未达标时，不允许合并到 develop

## 7. 故障排查

### 7.1 常见问题

#### 问题：k6 连接被拒绝

```
ERRO[0001] Cannot connect to http://localhost:18080
```

**排查步骤**：

1. 确认后端服务已启动：`curl http://localhost:18080/medkernel/api/health`
2. 确认端口未被占用：`lsof -i :18080`
3. 检查防火墙规则：`sudo iptables -L -n`

#### 问题：P95 延迟超标

**排查步骤**：

1. 检查数据库连接池是否饱和：访问 `http://localhost:18081/medkernel/actuator/metrics/hikaricp.connections.active`
2. 检查 JVM 堆使用率：访问 `http://localhost:18081/medkernel/actuator/metrics/jvm.memory.used?tag=area:heap`
3. 检查慢查询日志：查看数据库慢查询日志
4. 检查 GC 日志：`-Xlog:gc*:file=gc.log`
5. 使用 Prometheus 查询延迟分布：
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket{uri="/api/cdss/evaluate"}[5m]))
     by (le)
   )
   ```

#### 问题：错误率 > 1%

**排查步骤**：

1. 查看错误类型分布：
   ```promql
   sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (uri, status)
   ```
2. 检查后端日志：`journalctl -u medkernel -n 1000 --no-pager`
3. 检查数据库连接是否正常
4. 检查外部服务（AI Gateway）是否可用

#### 问题：连接泄漏

**排查步骤**：

1. 压测前记录基线：
   ```bash
   curl -s http://localhost:18081/medkernel/actuator/metrics/hikaricp.connections.active | jq '.measurements[0].value'
   ```
2. 压测后再次记录
3. 若差值 > 2，检查代码中是否有未关闭的连接
4. 检查 `RuleEngineService`、`CdssService`、`PathwayService` 中的数据库操作
5. 使用 Arthas 追踪连接获取/释放：
   ```bash
   watch com.zaxxer.hikari.HikariDataSource getConnection '{params, returnObj}' -n 100
   ```

#### 问题：k6 内存不足

```
ERRO[0010] Go error: runtime: out of memory
```

**解决方案**：

1. 减少 VU 数量
2. 使用 `--no-connection-reuse` 减少内存占用
3. 使用 `--discard-response-bodies` 丢弃响应体
4. 分批执行测试场景

### 7.2 监控端点速查

| 端点 | 端口 | 用途 |
|------|------|------|
| `/medkernel/api/health` | 18080 | 业务健康检查 |
| `/medkernel/actuator/health` | 18081 | Spring Boot 健康检查 |
| `/medkernel/actuator/prometheus` | 18081 | Prometheus 指标采集 |
| `/medkernel/actuator/metrics` | 18081 | 单指标查询 |
| `/medkernel/actuator/metrics/http.server.requests` | 18081 | HTTP 请求指标 |
| `/medkernel/actuator/metrics/hikaricp.connections.active` | 18081 | 连接池活跃连接 |
| `/medkernel/actuator/metrics/jvm.memory.used` | 18081 | JVM 内存使用 |

### 7.3 日志位置

| 日志 | 路径 |
|------|------|
| 应用日志 | `/var/log/medkernel/application.log` |
| GC 日志 | `/var/log/medkernel/gc.log` |
| Nginx 访问日志 | `/var/log/nginx/access.log` |
| Nginx 错误日志 | `/var/log/nginx/error.log` |
| Prometheus 日志 | `docker logs medkernel-prometheus` |
| k6 输出 | `results/*.json` |
