# A14 第三方对接总线 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude · backlog：INTEG-01/02 = done
> **审计结论：⚠️ 半修复。Math.random 掷骰子已去除，但 ping/retry 仍不真正连接外部系统；前端 AdapterHub 假闭环。1C / 2H / 2M。**

## 1. 概览
- 后端 `engine/integration`（13 文件）：`IntegrationService`(adapter/webhook/重试) + HMAC 签名
- 前端 `tenant/AdapterHub.tsx`(disable=1 / rand=3 / fallback=24，假闭环重灾区)
- 测试 4 文件；#127 已将 Webhook 签名测试端点裸 Map 重构为 DTO（B6 已修）

## 2. 十维度要点
- **业务正确性 🟡 半真**：
  - `pingAdapter:124-155`：**已去 Math.random**（§5.2），但改为"校验本地 configJson 是否合法 JSON + 用本地解析耗时（System.nanoTime）当 RTT"——**根本不连接外部系统**，configJson 合法即报 HEALTHY。
  - `retryMessage:290-315`：**已去 70% 掷骰子**，但改为"payload 非空即标 SUCCESS"——**不真正重投递到 HIS/LIS**。
  - HMAC-SHA256 签名计算真实（§5.3 确认）。
- **医疗安全合规 🟡**：宪法 §10「外部系统统一对接链路」骨架在（适配器/Webhook/launch token），但自检与重试不真实连通，互联互通"健康"失真。
- **多租户隔离 ✅**：@DataScope + 租户过滤。
- **审计证据链 ✅**：适配器/Webhook/重试留痕。
- **五方言 ✅**：adapter/webhook/message_log 表五方言齐全。
- **代码净化 🟠 A14-M-01**：`:115`"模拟网络握手 RTT"、`:282`"以 70% 概率高仿真成功"——**旧造假行为的注释残留**，与已改的确定性代码不符，误导读者。
- **错误处理 🟡**：ENG_INTEG 错误码齐全；但 ping/retry 的"成功"语义失真。
- **可观测性 🟡**：缺真实连通性指标。
- **测试 🟡**：4 测试；建议补"配置非法→UNHEALTHY""payload 空→DEAD_LETTER"等，并明确不再断言随机成功。
- **契约一致 🔴 A14-CRIT-01（前端）**：`AdapterHub.tsx` 真实性审计 F4 同款——各动作 catch 伪造成功（mockRtt/mockSign/"[仿真模式]成功"）、导出弹写死假哈希 `sha256-4c74026f...` 称"可用于互联互通测评"。fallback=24 佐证未清。

## 3. 角色视角
- 信息科主任：适配器中心 UI 完整，但"健康自检"不连外部、重试不真投递、前端伪造成功——无法据此判断真实接入状态。

## 4. Findings
| Sev | ID | 一句话 | 位置 |
|---|---|---|---|
| Critical | A14-CRIT-01 | 前端 AdapterHub catch 伪造成功 + 导出写死假哈希 | `AdapterHub.tsx`（参 F4）|
| High | A14-H-01 | pingAdapter 不连外部，本地 JSON 解析时间当 RTT，配置合法即 HEALTHY | `IntegrationService.java:124-155` |
| High | A14-H-02 | retryMessage 不真重投递，payload 非空即标 SUCCESS | `IntegrationService.java:290-315` |
| Medium | A14-M-01 | 旧造假行为注释残留（115/282），与代码不符 | 同上 |
| Medium | A14-M-02 | 缺真实连通性/重投递指标与用例 | service/测试 |

合计：C1 H2 M2 L0

## 5. 改造
- A14-H-01/H-02：引入 `ExternalSystemConnectorPort`，ping 真发轻量探活（TCP/HTTP/FHIR metadata），retry 真重投递；无连接器时**诚实标 NOT_CONNECTED**，不得报 HEALTHY/SUCCESS。约 2-3 天（含 Adapter）。
- A14-CRIT-01：前端删 catch 伪造与写死导出哈希，导出走后端真实证据。约 4h。
- A14-M-01：清理误导注释。约 0.3h。

## 6. 总评
INTEG-01/02 **部分名副其实**——签名/审计/裸 Map 修复（B6）真实，但 ping/retry 是"去骰子未补真连通"的**半修复**，前端仍系统性造假。**不可进入第三方对接验收（QA-08）**。建议 backlog 标注 INTEG 为部分完成。
