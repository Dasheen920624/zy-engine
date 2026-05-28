# 提案：MedKernel 引擎真实性彻底整治与门禁修复工程 (GA-ENG-CLEAN-01)

> 提案日期：2026-05-28
> 提案人：Antigravity
> 状态：提案中 (Under Proposal)
> 关联任务：GA-ENG-KNOW-01 / GA-ENG-KNOW-02 / GA-ENG-TERM-01 / GA-ENG-LLM-01 / GA-ENG-EVID-01 / GA-ENG-INTEG-01

---

## 1. 背景与动机

根据 2026-05-28 的引擎能力真实性代码核查报告（`docs/audit/2026-05-28-engine-capability-authenticity-audit.md`），系统存在严重的真实性代码质量债务，部分标记为 `done` 的旗舰功能包含造假的 mock 实现，违反了产品宪法（`docs/CONSTITUTION.md`）中关于“禁止业务 mock 假闭环”的明确要求。

主要暴露出的问题包括：
1. **防假闭环门禁失效**：ESLint 规则 `no-page-mock` 仅检测全大写命名，导致驼峰命名 (camelCase) 的页面级大体量硬编码数组绕过了代码库门禁。
2. **核心业务返回假哈希/假推理**：证据链大导出（B8）为没有打包与归档的空操作，返回假哈希串；大模型能力网关（B7）在 B2 模式下不调模型，编造引文并强行降级为 B0 写死输出；集成总线（B4/B5）通过 `Math.random` 掷骰子来模拟 Ping 握手 RTT 和 70% 假成功的死信重试投递。
3. **关键去重与匹配算法缺失**：知识资产 SHA-256 片段锚点去重（B1）不计算哈希，同文档重复登记无法拦截；字典映射推荐（B2）使用简单的字符命中比，而非长公共子序列 (LCS)，导致严重的临床词条误配。

为了保证医疗系统的严肃性、高可信度及医疗安全，必须开展一场彻底的**“真实性重构与门禁加固工程”**，将这 6 个核心引擎任务进行重构与整治。

---

## 2. 改造目标

1. **重建防伪防 Mock 前端门禁 (R1)**：重写并收紧 ESLint 规则 `no-page-mock.js`，无差别拦截所有页面内（包含 camelCase 命名）的大型对象数组字面量和 catch 块伪造成功的欺骗性交互。
2. **实现真实的证据打包与防伪导出 (B8 / F1 / F2)**：废除假哈希和固定病案展示，对接后台真实的知识包、配置包发布与审计证据流，使用后台 ZIP 真实压缩打包并计算真实的 SHA-256 指纹，确保前端自校验沙箱能精确完成哈希即时校验。
3. **重构模型能力网关真实通道 (B7)**：在 B1/B2 辅助和生成模式下，必须通过 Spring Boot 统一的模型能力适配器真实调用外部 Provider 或本地大模型。没有 Provider 时，诚实回退至 B0（确定性基线）并进行醒目状态提示，严禁伪造高置信度与引文。
4. **重构集成总线真实 Ping 与重试机制 (B4 / B5 / B6)**：适配器健康 Ping 必须进行物理 Socket/HTTP 握手并计算真实延迟；死信重试队列必须进行物理级异步重新投递；Webhook 签名和调用参数移除裸 Map，采用强类型 Record DTO 进行封装。
5. **重构知识去重与字典映射算法 (B1 / B2 / B3)**：在 `KnowledgeIdentityService` 中真实计算片段的 SHA-256 并持久化到 `SourceFragment` 表中，实现高内聚的原子去重；在 `TerminologyService` 中落地标准 LCS（最长公共子序列）及编辑距离算法，提高相似词条推荐的医学精准度。

---

## 3. 影响范围分析

### 3.1 影响的文档
- `docs/CONSTITUTION.md`：增加第 18 条硬约束（已完成）。
- `docs/backlog.md`：确认将 6 个引擎任务标为 `in_progress` 并添加整治进度说明。
- `docs/superpowers/plans/2026-05-28-engine-authenticity-remediation.md`：记录作为 superpowers 的实施证据。

### 3.2 影响的 API 与 DTO
- `medkernel-backend`：
  - `EvidenceService` / `EvidenceController`：证据打包导出 ZIP API。
  - `ModelGatewayService` / `ModelGatewayController`：B1/B2 真实调用接口、敏感脱敏接口、验证逻辑。
  - `IntegrationService` / `IntegrationController`：真实的 ping 握手与消息重投递机制。
  - `TerminologyService`：最长公共子序列（LCS）算法及映射接口。
  - `KnowledgeIdentityService`：片段真实的 SHA-256 哈希计算与幂等校验。
  - 迁移脚本：如果表结构需要增加 `hash` 字段或表索引，需要增加 Flyway 迁移脚本。

### 3.3 影响的前端组件
- `eslint-rules/no-page-mock.js`
- `advanced/Provenance.tsx` (来源追溯控制台)
- `tenant/AdapterHub.tsx` (适配器中心与 HIS 仿真沙箱)
- 其它 8+ 包含仿真兜底的页面。

---

## 4. 提案结论

本提案是全面洗刷 MedKernel 引擎“假闭环、伪功能”质量债的唯一正规途径，对于通过 v1.0 GA 的 E5 全能力验收、等保评测、商密评测与临床安全门禁具有决定性意义。
我们将以最高标准的专业严谨性与诚实原则执行本次重构。
