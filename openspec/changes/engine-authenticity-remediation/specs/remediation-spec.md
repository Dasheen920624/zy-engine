# 规格增量：引擎真实性与开发严谨度规格

> 日期：2026-05-28
> 状态：规划中 (Under Planning)
> 关联 OpenSpec：`engine-authenticity-remediation`

---

## 1. 前端 ESLint 防 Mock 规则

### Requirement: 页面硬编码数据硬拦截
系统 SHALL 静态拦截一切存在于业务页面 (`src/pages/**`) 和业务功能区 (`src/features/**`) 的包含对象元素的非空数组字面量声明，除非通过特定的 ESLint 注释手动声明豁免。

#### Scenario: 驼峰命名 Mock 数据尝试提交
- **GIVEN** 开发者在 `src/pages/Provenance.tsx` 中定义了 `const patientEvidenceList = [{ name: '张三', proof: 'sha256-xxx' }]`
- **WHEN** 运行 ESLint 静态代码检查 (`npm run lint`)
- **THEN** 扫描器 SHALL 物理拦截并抛出编译错误 `noPageMock`，阻断构建。

---

## 2. 证据链大导出与即时防伪沙箱

### Requirement: 物理打包与内容哈希
后端系统 SHALL 读取数据库中的全部真实审计痕迹并打包输出为物理的 `.zip` 归档包。归档包的哈希签名必须由 `BouncyCastle` 底座基于该打包二进制内容实时计算，严禁伪造内容摘要。

#### Scenario: 专家在 Provenance 控制台导出证据包
- **GIVEN** 专家点击“下载防伪证据包”按钮
- **WHEN** 前端向 `/api/v1/compliance/evidence/export` 发起请求
- **THEN** 后端系统生成一个真实的 `.zip` 包，使用 SHA-256 物理算法计算其哈希 `ZIP_HASH` 并持久化记录，然后向前端传输物理文件流；
- **AND** 前端利用原生 Web Crypto API 物理计算下载文件的 SHA-256 得到 `CLIENT_HASH`；
- **AND** 校验沙箱物理比对 `CLIENT_HASH` 与后端响应的 `ZIP_HASH`。若两者一致，则提示“✅ 物理哈希防篡改即时自校验通过”；若不一致，则精准拉起篡改告警。

---

## 3. 大模型网关调用与诚实基线降级

### Requirement: 大模型网关真实通信
大模型能力网关在 B1（大模型辅助）和 B2（大模型推理）模式下 SHALL 物理连通外部 Provider 接口。若网络断连或无可信 Provider，网关 SHALL 诚实标明 “B0 模式” 并返回确定性规则中枢计算出的数据结论，绝不能编造置信度或参考文献引文。

#### Scenario: 外部模型服务故障时发起病历信息抽取
- **GIVEN** 院内大模型 Provider 离线
- **WHEN** 质控引擎向大模型网关发起提取请求
- **THEN** 网关捕获通信异常并进行日志审计留痕；
- **AND** 网关诚实将输出元数据标记为 `modelMode = "B0"`、`modelVersion = "MedKernel-Deterministic-Baseline"`、`confidence = 1.0`；
- **AND** 网关回退到本地内置的关系数据库和确定性规则引擎物理计算并返回结果，不抛出崩塌式阻断，且前端工作台提示“大模型已降级至基线引擎运行”。

---

## 4. 集成总线物理握手与死信重试队列

### Requirement: 适配器物理联通性 Ping
集成总线在检测适配器状态时 SHALL 向目标接口发送物理的网络请求，并根据请求实际响应延迟计算网络 RTT 耗时，禁止使用随机数进行数值填充。

#### Scenario: 运行 HIS 适配器 Ping 检测
- **GIVEN** 管理员打开适配器中心页面
- **WHEN** 管理员点击“物理 Ping 测试”
- **THEN** 后端向适配器的服务地址发送 `OPTIONS` HTTP 请求并实时计时；
- **AND** 记录并向前端返回真实的物理响应时间差。若响应失败，物理标明 `OFFLINE` 状态。

### Requirement: 消息投递与死信重试
当外部系统接入发送失败时，集成总线 SHALL 对出站队列的消息执行真实的 HTTP 重新投递，若达到最大重试次数上限且依然失败，SHALL 物理将其移入死信数据库表以供人工补偿，禁止掷骰子形式的伪造成功。

#### Scenario: 死信异步重试机制
- **GIVEN** 诊断消息投递至 EMR 适配器因网络间歇性中断失败
- **WHEN** 定时重试任务启动重新投递
- **THEN** 异步线程向 EMR 物理地址发起真实数据发送；
- **AND** 若响应物理成功 (200 OK)，则更新状态为 `SUCCESS`；
- **AND** 若物理投递再次失败，则重试次数加 1。超过 5 次失败后，消息状态变为 `DEAD_LETTER` 并移入 `integration_dead_letter` 表，由审计日志记录物理足迹。
