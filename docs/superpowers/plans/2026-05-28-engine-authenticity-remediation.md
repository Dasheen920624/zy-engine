# 引擎真实性彻底整治与门禁修复工程实施计划

日期：2026-05-28
状态：已由 2026-05-31 研发重启闸门纠偏；**不得直接按本文执行**
关联 OpenSpec：`engine-authenticity-remediation`

> 重要纠偏：本文保留为历史计划证据，不再作为当前执行方案。当前执行以 [AI 研发重启执行方案](../../AI_DEVELOPMENT_RESTART_PLAN.md)、施工卡和已纠偏的 OpenSpec 为准。本文旧版曾把字典 LCS 写成修复目标，与核心 §7“医学语义映射、禁止字符 LCS”冲突，后续执行必须按语义映射与高危负样本门禁重做。

---

## 1. 目标

治理 MedKernel 项目中存在的严重真实性代码质量债务。针对防假闭环门禁失效（R1）、大模型网关假推理（B7）、证据链空导出假哈希（B8/F1/F2）、集成总线随机数握手与假死信重试（B4/B5）以及匹配和去重弱算法进行物理化重构，确保 100% 物理真实，彻底根除 Mock 假闭环。

---

## 2. 架构方案

- **前端 ESLint 加固**：修改 `no-page-mock.js`，移去大写命名判断，强制拦截 `src/pages/**` 和 `src/features/**` 下的所有非空对象数组字面量声明。
- **物理证据大打包**：在 `EvidenceService` 中读取真实审计事件并利用 `ZipOutputStream` 压入 ZIP 字节流，由 `BouncyCastle` 实时算物理哈希；前端利用 Web Crypto API 重构自校验沙箱对账逻辑。
- **物理模型网关与真实降级**：模型网关在 B1/B2 模式下发起物理 API 交互；在不可用或断连时诚实标明 `modelMode="B0"`、`modelVersion="MedKernel-Deterministic-Baseline"` 降级返回，绝不编造引文。
- **物理集成总线**：Ping 调用发起真实的 HTTP/Socket 物理握手计算 RTT 延迟；死信异步重试线程发起物理连接进行异步投递，失败达上限进入死信表存储。
- **匹配与去重物理算法**：字典映射改为医学语义候选（同义词典 + 编码交叉表 + 来源权重 + 高危负样本 + 人工确认），字符 LCS / 编辑距离最多作为低权重召回信号；利用真实的 SHA-256/SM3 哈希计算在 `KnowledgeIdentityService` 对指南片段物理指纹去重。

---

## 3. 任务清单

- [ ] 升级 ESLint `no-page-mock.js` 规则，无差别拦截大型对象数组字面量 (R1)
- [ ] 扫描并修复前端 8+ 含有 Mock 数据的业务组件，对必要静态列配置增加 ESLint 注释豁免 (F5)
- [ ] 后端物理打包 DTO 架构及 `ZipOutputStream` 服务层设计 (B8)
- [ ] 前端 Provenance 证据沙箱 Web Crypto API 原生哈希比对代码重写 (F1/F2)
- [ ] 模型能力网关 WebClient/HttpClient 物理大模型端点接入与调试 (B7)
- [ ] 网关 B0 确定性降级数据模型及前端看板真实降级状态提醒 (B7)
- [ ] 适配器中心 Socket/HTTP 物理 Ping 端点开发与 AdapterHub 界面重构 (B4)
- [ ] 死信投递物理重试异步队列设计与死信迁移脚本 (B5)
- [ ] `TerminologyService` 医学语义映射候选与高危负样本门禁实现，替换字符 LCS 作为充分依据的旧逻辑 (TERM-01)
- [ ] `KnowledgeIdentityService` 指南片段真实 SHA-256 哈希去重及表字段扩容物理迁移 (B1)
- [ ] 全中枢 200+ JUnit 测试及 80+ 前端 Vitest 合同门禁验证 100% 跑绿 (QA-07)

---

## 4. 验收结果（预期）

- 前端 `npm run lint` 和 `npm run typecheck` 零警告零报错通过。
- `EvidenceService` 成功导出物理上可解压、数据真实的 ZIP 文件，哈希指纹完全吻合。
- 大模型网关在断连时无闪退，前端清晰警示并降级运行，无任何编造引文。
- 适配器 Ping 真实显示目标离线并提供物理网络异常日志。
- 术语自动推荐对高危近似词条（钾/钠、肌钙蛋白 T/I、左/右、剂量量级）强制 HIGH + 禁批量/禁自动确认。
- 相同指南内容多次导入能够由 SHA-256 指纹唯一索引物理拦截。

---

## 5. 后续注意

- 前端不可妥协：严禁在 `catch` 中直接使用 `message.success` 掩盖错误。
- 医疗合规：任何数字签名与哈希均使用 BouncyCastle 在国密/商密规范下进行。
