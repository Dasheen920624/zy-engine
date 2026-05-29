# A11 嵌入引擎 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude · backlog：EMBED-01 / API-11 = done
> **审计结论：✅ 后端达标（一次性 token 真实）/ 前端 EmbedLaunch ⚠️ 假闭环。0C / 1H / 1M。**

## 概览
`engine/embed`(11)：EmbedEngineService（launch token / Origin 白名单 / 反馈）；测试 3；前端 `clinical/EmbedLaunch.tsx`(disable=1/fallback=10)。

## 十维度要点
- **业务正确性 ✅ 后端**：`EmbedEngineService:46` 一次性 launch token，`DEFAULT_EXPIRE_SECONDS=60`、"原子锁定校验"、UNUSED→已消费状态机；Origin 域名租户白名单拦截；医生反馈子事务留痕。
- **医疗安全合规 ✅**：token 60s 短时 + 一次性消费防重放；Origin 白名单防跨域；最小数据。
- **多租户隔离 ✅**：@DataScope；token/白名单租户绑定。
- **审计/五方言/净化 ✅ 后端**：V17（embed_launch_token/origin_whitelist）齐全；后端嗅探 0。
- **测试 🟡**：3 测试；建议补 token 过期/重复消费/非法 Origin 用例。
- **契约一致 🟠 A11-H-01（前端）**：`EmbedLaunch.tsx` disable=1 + fallback=10，疑令牌失败时仿真兜底展示（与 C3 整改）。

## Findings
| Sev | ID | 一句话 | 位置 |
|---|---|---|---|
| High | A11-H-01 | 前端 EmbedLaunch eslint-disable + fallback=10 仿真兜底 | `EmbedLaunch.tsx`（C3）|
| Medium | A11-M-01 | 缺 token 过期/重复消费/非法 Origin 用例 | embed 测试 |

合计：C0 H1 M1 L0

## 总评
EMBED-01 / API-11 **后端名副其实**（一次性 token 安全模型真实）；前端归 C3。后端可进验收。
