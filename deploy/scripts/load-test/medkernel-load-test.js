import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:18080/medkernel';

// Custom metrics
const errorRate = new Rate('errors');
const healthLatency = new Trend('health_latency');
const providerLatency = new Trend('provider_latency');
const pathwayLatency = new Trend('pathway_latency');
const ruleLatency = new Trend('rule_latency');
const terminologyLatency = new Trend('terminology_latency');
const graphLatency = new Trend('graph_latency');

export const options = {
  stages: [
    { duration: '5m', target: 10 },   // Warmup
    { duration: '10m', target: 100 },  // Ramp up
    { duration: '30m', target: 100 },  // Steady state
    { duration: '5m', target: 0 },     // Ramp down
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
    // S1: Health check
    const res = http.get(`${BASE_URL}/api/health`);
    healthLatency.add(res.timings.duration);
    check(res, { 'health status 200': (r) => r.status === 200 }) || errorRate.add(1);
  } else if (rand < 0.20) {
    // S2: Provider status
    const res = http.get(`${BASE_URL}/api/system/providers`);
    providerLatency.add(res.timings.duration);
    check(res, { 'providers status 200': (r) => r.status === 200 }) || errorRate.add(1);
  } else if (rand < 0.45) {
    // S3: Pathway query
    const res = http.get(`${BASE_URL}/api/pathway/definitions`);
    pathwayLatency.add(res.timings.duration);
    check(res, { 'pathway status 200': (r) => r.status === 200 || r.status === 404 }) || errorRate.add(1);
  } else if (rand < 0.70) {
    // S4: Rule execution
    const res = http.post(
      `${BASE_URL}/api/rule/execute`,
      JSON.stringify({ patientId: 'TEST-PID-001', ruleCodes: [] }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    ruleLatency.add(res.timings.duration);
    check(res, { 'rule status 200': (r) => r.status === 200 || r.status === 400 }) || errorRate.add(1);
  } else if (rand < 0.85) {
    // S5: Terminology search
    const res = http.get(`${BASE_URL}/api/terminology/search?q=diabetes`);
    terminologyLatency.add(res.timings.duration);
    check(res, { 'terminology status 200': (r) => r.status === 200 || r.status === 404 }) || errorRate.add(1);
  } else {
    // S6: Knowledge graph
    const res = http.get(`${BASE_URL}/api/knowledge/graph/query?entity=diabetes`);
    graphLatency.add(res.timings.duration);
    check(res, { 'graph status 200': (r) => r.status === 200 || r.status === 404 }) || errorRate.add(1);
  }

  sleep(1);
}
