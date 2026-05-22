// 认证与健康检查性能测试
// 测试 POST /api/auth/login、GET /api/auth/me、GET /api/health 的响应性能
// 认证是所有操作的前提，属于关键路径接口
// GA-PERF-01：100 并发用户持续 30 分钟

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import {
  BASE_URL, API_PREFIX, buildOptions, sustainedLoadScenario,
  authHeaders, randomPick,
} from './config.js';

// 自定义指标
const authErrorRate = new Rate('auth_errors');
const loginDuration = new Trend('auth_login_duration');
const meDuration = new Trend('auth_me_duration');
const healthDuration = new Trend('health_check_duration');

// 测试用户池 — 模拟不同角色的用户登录
const TEST_USERS = [
  { username: 'admin', password: 'admin123', role: '系统管理员' },
  { username: 'doctor_zhang', password: 'test123', role: '心内科医生' },
  { username: 'doctor_li', password: 'test123', role: '呼吸科医生' },
  { username: 'nurse_wang', password: 'test123', role: '急诊护士' },
  { username: 'qc_chen', password: 'test123', role: '质控专员' },
  { username: 'dept_head_zhao', password: 'test123', role: '科主任' },
];

export const options = buildOptions(sustainedLoadScenario());

// 登录测试 — POST /api/auth/login
function testLogin() {
  const user = randomPick(TEST_USERS);

  const payload = JSON.stringify({
    username: user.username,
    password: user.password,
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}${API_PREFIX}/auth/login`, payload, params);
  const elapsed = Date.now() - startTime;

  loginDuration.add(elapsed);

  let token = null;
  const passed = check(res, {
    '登录状态200': (r) => r.status === 200,
    '登录响应包含令牌': (r) => {
      try {
        const body = JSON.parse(r.body);
        token = body.data?.token || body.token;
        return (body.success === true || body.code === '0') && token !== null;
      } catch (e) {
        return false;
      }
    },
    '登录P95<300ms': () => elapsed < 300,
  });

  authErrorRate.add(!passed);

  if (!passed) {
    console.error(`登录失败: user=${user.username} status=${res.status} body=${res.body?.substring(0, 200)}`);
  }

  return token;
}

// 当前用户查询 — GET /api/auth/me
function testMe(token) {
  if (!token) return;

  const headers = authHeaders(token);

  const startTime = Date.now();
  const res = http.get(`${BASE_URL}${API_PREFIX}/auth/me`, { headers });
  const elapsed = Date.now() - startTime;

  meDuration.add(elapsed);

  const passed = check(res, {
    '当前用户查询状态200': (r) => r.status === 200,
    '当前用户查询响应有效': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true || body.code === '0';
      } catch (e) {
        return false;
      }
    },
  });

  authErrorRate.add(!passed);

  if (!passed) {
    console.error(`当前用户查询失败: status=${res.status} body=${res.body?.substring(0, 200)}`);
  }
}

// 健康检查 — GET /api/health
function testHealth() {
  const startTime = Date.now();
  const res = http.get(`${BASE_URL}${API_PREFIX}/health`);
  const elapsed = Date.now() - startTime;

  healthDuration.add(elapsed);

  const passed = check(res, {
    '健康检查状态200': (r) => r.status === 200,
    '健康检查响应有效': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true || body.code === '0';
      } catch (e) {
        return false;
      }
    },
    '健康检查P95<100ms': () => elapsed < 100,
  });

  authErrorRate.add(!passed);

  if (!passed) {
    console.error(`健康检查失败: status=${res.status} body=${res.body?.substring(0, 200)}`);
  }
}

export default function () {
  // 按比例分配操作：50% 登录、30% 查询当前用户、20% 健康检查
  const rand = Math.random();

  if (rand < 0.5) {
    // 登录测试
    testLogin();
  } else if (rand < 0.8) {
    // 先登录获取令牌，再查询当前用户
    const token = testLogin();
    if (token) {
      testMe(token);
    }
  } else {
    // 健康检查（无需认证）
    testHealth();
  }

  sleep(Math.random() * 1 + 0.3);
}
