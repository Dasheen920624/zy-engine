// k6 性能测试通用配置 — MedKernel 医学知识平台
// 包含公共设置、SLO 阈值和辅助函数

import http from 'k6/http';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const API_PREFIX = '/api';

// GA-PERF-01 要求的 SLO 阈值
export const THRESHOLDS = {
  p95: 300,       // 核心接口 P95 < 300ms
  p99: 500,       // 核心接口 P99 < 500ms
  errorRate: 0.01, // 错误率 < 1%
};

// 标准 k6 选项构建器
export function buildOptions(scenarios, thresholds) {
  return {
    scenarios,
    thresholds: thresholds || {
      http_req_duration: [`p(95)<${THRESHOLDS.p95}`, `p(99)<${THRESHOLDS.p99}`],
      http_req_failed: [`rate<${THRESHOLDS.errorRate}`],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  };
}

// 100 并发用户、30 分钟持续负载场景
export function sustainedLoadScenario(duration = '30m', vus = 100) {
  return {
    sustained: {
      executor: 'constant-vus',
      vus,
      duration,
      gracefulStop: '30s',
    },
  };
}

// 阶梯加压场景 — 寻找系统拐点
export function rampUpScenario(maxVus = 200, duration = '10m') {
  return {
    ramp_up: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '2m', target: 50 },
        { duration: '3m', target: 100 },
        { duration: '3m', target: 150 },
        { duration: '2m', target: maxVus },
      ],
      gracefulRampDown: '30s',
    },
  };
}

// 尖峰测试场景 — 瞬时高并发冲击
export function spikeScenario() {
  return {
    spike: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '1m', target: 10 },
        { duration: '30s', target: 200 },
        { duration: '3m', target: 200 },
        { duration: '30s', target: 10 },
        { duration: '2m', target: 10 },
      ],
      gracefulRampDown: '30s',
    },
  };
}

// 辅助函数：获取认证令牌
export function getAuthToken() {
  const loginRes = http.post(`${BASE_URL}${API_PREFIX}/auth/login`, JSON.stringify({
    username: __ENV.AUTH_USER || 'admin',
    password: __ENV.AUTH_PASS || 'admin123',
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  if (loginRes.status !== 200) {
    console.error(`登录失败: ${loginRes.status} ${loginRes.body}`);
    return null;
  }

  const body = JSON.parse(loginRes.body);
  return body.data?.token || body.token || null;
}

// 辅助函数：构建认证请求头
export function authHeaders(token) {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    'X-Tenant-Id': __ENV.TENANT_ID || 'default',
  };
}

// 辅助函数：从数组中随机选取
export function randomPick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

// 辅助函数：生成随机患者 ID
export function randomPatientId() {
  return `P${String(Math.floor(Math.random() * 10000)).padStart(6, '0')}`;
}

// 辅助函数：生成随机就诊 ID
export function randomEncounterId() {
  return `E${String(Math.floor(Math.random() * 10000)).padStart(6, '0')}`;
}
