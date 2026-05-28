# 任务清单：MedKernel 引擎真实性整治与门禁修复工程

> 计划日期：2026-05-28
> 负责人：Antigravity
> 状态：计划中 (Under Planning)
> 关联任务：GA-ENG-CLEAN-01

---

## 阶段 1：防假闭环门禁升级与存量代码扫描 (Day 1)

### Task 1.1: 升级 ESLint 规则 `no-page-mock.js`
- **内容**：重写 `frontend/eslint-rules/no-page-mock.js`，修改 `VariableDeclarator` 校验逻辑，移去全大写命名过滤，任何定义在 `pages/` 和 `features/` 目录下的非空对象数组字面量直接报错拦截。
- **检查点**：
  - 运行 `npm run lint` 能够成功扫描出包含 camelCase 命名的硬编码 Mock 常量（如 `strokeEvidenceChain`、`defaultLocalAdapters`）。

### Task 1.2: 清理存量页面假闭环与添加豁免
- **内容**：对爆出的 8+ 含有假闭环或 Mock 数据的前端页面进行排查。对菜单、表格列（Columns）等前端配置添加 `/* eslint-disable medkernel/no-page-mock */` 豁免；对其他核心业务数据彻底移除 Mock 数组，接入真后端接口。
- **检查点**：
  - 前端 `npm run lint` 与 `npm run typecheck` 100% 跑通全绿。

---

## 阶段 2：后端算法物理化与 API 重构 (Day 2-3)

### Task 2.1: 修复 B1 - 知识物理 SHA-256 片段去重
- **内容**：
  - 在 `SourceFragment` 实体中增加 `contentHash` 字段。
  - 为 `KnowledgeIdentityService` 的 `createFragment` 编写物理的 SHA-256 算法，并加上数据库的唯一键约束与锁。
- **检查点**：
  - 相同的文本片段被多次插入时，物理拦截并报错抛出哈希冲突。

### Task 2.2: 修复 B2 - 字典最长公共子序列 (LCS) 算法
- **内容**：
  - 重写 `TerminologyService.calculateSimilarity`，编写基于动态规划的 LCS 匹配相似度公式以及编辑距离相似度算子。
- **检查点**：
  - 编写 JUnit 用例对医学术语匹配进行合同校验，阈值提高至 0.8 以上。

### Task 2.3: 修复 B7 - 模型能力网关物理化调用
- **内容**：
  - 重构 `ModelGatewayService`，用 WebClient 替换硬编码的 fake 模型返回，如果没有配置 Provider 或不可用，诚实且清晰地标明 B0 模式并回退，绝对禁止编造引文。
- **检查点**：
  - JUnit 单元测试中，在有大模型配置时验证物理调用，无配置时验证真实的 B0 降级状态。

### Task 2.4: 修复 B8 - 证据链真实物理打包与导出
- **内容**：
  - 重构 `EvidenceService.exportEvidences`，利用 `ZipOutputStream` 真实读取并打包审计快照与证据，使用 `BouncyCastle` 真实计算 ZIP 的物理哈希值返回。
- **检查点**：
  - 导出的物理 ZIP 文件可以用本地解压软件打开，且包含合规的 JSON 数据结构，内容哈希与导出的数字签名完全匹配。

### Task 2.5: 修复 B4/B5 - 集成总线物理连接 Ping 与死信异步投递
- **内容**：
  - 重构 `IntegrationService.pingAdapter` 和 `retryMessage`。
  - 弃用 `Math.random`。使用 `RestClient` 对适配器发起真实的 HTTP 连接计算 RTT 耗时。重试采用真实的投递循环，超限自动划归死信。
- **检查点**：
  - 集成总线测试套件通过真实的物理 HTTP 网络测试，死信队列流转逻辑经过 JUnit 测试。

---

## 阶段 3：前端联动与沙箱自校验重构 (Day 4)

### Task 3.1: 重构 `Provenance.tsx` 可信来源追溯与自校验沙箱
- **内容**：
  - 前端追溯控制台移除写死假病案和假哈希，接入后端物理 ZIP 数据流。
  - 修复自校验沙箱：自校验沙箱下载物理 ZIP，前端使用 `Web Crypto API` 在浏览器物理计算 ZIP 流的 SHA-256 摘要，与后端签发的哈希完全对账匹配。
- **检查点**：
  - 沙箱校验不再无故报错“篡改”，只有在数据被真实修改时才精准报错，且完全能够成功完成物理盖章与验证。

### Task 3.2: 重构 `AdapterHub.tsx` 控制台
- **内容**：
  - 移除每一个 `catch` 块的伪造成功提示。如果是网络错误或服务器错误，必须真实进入 Error/Disabled 状态，提示管理员并显示错误码。
- **检查点**：
  - 后端适配器关闭时，前端 AdapterHub 真实且谦诚地显示离线状态和连接失败的详情，提供专家排错建议。

---

## 阶段 4：系统集成验证与验收 (Day 5)

### Task 4.1: JUnit 与前端 Vitest 合同门禁扫描
- **内容**：
  - 编写物理环境的联调冒烟脚本，跑全量 200+ 后端用例及前端冒烟。
- **检查点**：
  - 所有测试 100% 跑绿，无任何跳过和警告。

### Task 4.2: 递交 PR 并合并至远程 `main`
- **内容**：
  - 功能完成后，使用 `codex/` 前缀分支提交 PR，由 CI 自动核对。通过远端自检和代码核实后，合并到远程 `main`。
- **检查点**：
  - PR 描述详尽，包含本次真实性整治的所有验证结果与证据。
