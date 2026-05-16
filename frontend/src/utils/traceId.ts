// 生成 traceId。前端每个请求都会带上 X-Trace-Id，便于端到端排查。
// 不依赖 crypto.randomUUID（部分内网/旧浏览器不可用），改用 timestamp + random hex。
export function generateTraceId(): string {
  const ts = Date.now().toString(16);
  const rand = Math.random().toString(16).slice(2, 10);
  return `fe-${ts}-${rand}`;
}
