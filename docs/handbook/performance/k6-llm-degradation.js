// GA-PERF-01 · LLM Gateway 降级链压测
// 模拟主模型挂掉时降级链自动切到外网 SaaS 的端到端 P95
//
// 使用：k6 run docs/performance/k6-llm-degradation.js
//
// 验收：
// - 主备切换 P95 < 1.5s
// - 错误率 < 0.5%

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18080/medkernel';

export const options = {
  stages: [
    { duration: '1m', target: 50 },
    { duration: '10m', target: 100 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1500'],
    http_req_failed: ['rate<0.005'],
  },
};

const PROMPTS = [
  '胸痛 AMI 患者下一步建议',
  '卒中 4.5h 内 rt-PA 适应症',
  'DRG MS-30 入组条件',
  '高血压随访间隔（80 岁以上）',
];

export default function () {
  const prompt = PROMPTS[Math.floor(Math.random() * PROMPTS.length)];
  const res = http.post(
    `${BASE_URL}/api/v1/advanced/llm/chat`,
    JSON.stringify({ prompt, temperature: 0.3, maxTokens: 256 }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(res, {
    'status 200': (r) => r.status === 200,
    'has text': (r) => {
      try {
        return JSON.parse(r.body).text != null;
      } catch {
        return false;
      }
    },
  });
  sleep(Math.random());
}
