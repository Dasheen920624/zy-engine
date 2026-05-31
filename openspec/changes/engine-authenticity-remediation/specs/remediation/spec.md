# 规格增量：真实性整治与 AI 研发重启

> 日期：2026-05-31
> 状态：规划纠偏中
> 关联 OpenSpec：`engine-authenticity-remediation`

## ADDED Requirements

### Requirement: AI 必须按权威阅读链与交接状态开工

系统 SHALL 要求任何 AI 或人类协作者在写功能代码前读取 `_HANDOFF`，确认最新 `origin/main`，并只以核心、域简报、施工卡、体验契约和质量基线作为当前实现权威。

#### Scenario: 新 AI 领取 D0 AUTH-02

- **GIVEN** AI 准备修改登录页
- **WHEN** 开始执行任务
- **THEN** 它 SHALL 读取 `_HANDOFF`
- **AND** 读取 `docs/CONSTITUTION.md`、`docs/cards/D0/_brief.md`、`docs/cards/D0/AUTH-02.md`、`docs/EXPERIENCE_CONTRACT.md`
- **AND** 不 SHALL 以旧巨物或旧 superpowers 计划作为实现事实源。

### Requirement: 页面假闭环必须被静态和运行验证拦截

前端 SHALL 拦截业务页面和 feature 中的假数据、绕门禁注释、函数包装规避、业务 `Math.random()`、假 hash、默认 JSON/trace/prompt 展示和硬编码视觉 token。

#### Scenario: 页面用函数包装规避 no-page-mock

- **GIVEN** 开发者在 `src/pages/clinical/WorkflowTodos.tsx` 中通过函数返回预设业务数组
- **WHEN** 运行前端 lint
- **THEN** 门禁 SHALL 标记为业务假数据规避
- **AND** PR 不 SHALL 合并，除非该数据被替换为真实 API、受控空态或测试 fixture。

#### Scenario: 登录页使用硬编码颜色

- **GIVEN** 登录页 CSS 写入 `#1565c0`、`rgba(...)` 或硬编码圆角/字号
- **WHEN** 运行 stylelint 或 T-GATE
- **THEN** 构建 SHALL 失败
- **AND** 代码 SHALL 改为引用 Antd token 或 MedKernel token 变量。

### Requirement: 业务实现范围必须先于代码实现核查

系统 SHALL 在每个域开工前核查 S0–S40 场景、27 个客户二级菜单、5 个高级工具、施工卡、实际路由、后端接口、数据库迁移、测试和 B0 主链路的追溯关系。范围覆盖 SHALL 与实现完成分开记录。

#### Scenario: D2 开工前核查范围

- **GIVEN** 团队准备实施 D2 试点准备域
- **WHEN** 进入代码实现前
- **THEN** 系统 SHALL 确认 D2 的场景、菜单页面、规则/路径/知识/字典/适配器接口、迁移和测试均能追溯到施工卡
- **AND** SHALL 明确 RULE-01/PATH-01 同时承载引擎与页面，避免重复建卡
- **AND** 不 SHALL 将“卡索引已有承接”标记为“业务已经完成”。

#### Scenario: wave2 卡缺少 B0 消费点

- **GIVEN** AI 准备实现一个 wave2 领域门面
- **WHEN** 该卡无法指出 D0–D6 中哪个 B0 链路会消费它
- **THEN** 该实现 SHALL 被暂停
- **AND** SHALL 先补充 B0 消费点或调整范围。

### Requirement: 证据、降级和外部连接不得伪造

后端 SHALL 阻断生产路径中的假 hash、随机业务指标、catch 成功返回、占位公共说明和硬编码医学常量。业务 ID 可使用 UUID，但不得声称为完整性 hash 或证据签名。

#### Scenario: 证据导出返回 UUID hash

- **GIVEN** 后端导出证据包
- **WHEN** 生成完整性摘要
- **THEN** 摘要 SHALL 基于物理文件字节计算，按配置选择 SM3 或 SHA-256
- **AND** 不 SHALL 使用 UUID、时间戳或随机字符串作为 hash。

#### Scenario: 外部系统离线

- **GIVEN** HIS 或 EMR 适配器不可连接
- **WHEN** 适配器健康检查运行
- **THEN** 系统 SHALL 返回 `NOT_CONNECTED` 或等价诚实状态
- **AND** 不 SHALL 用随机 RTT 或假成功掩盖断连。

### Requirement: D0 通过前不得推进后续新功能

系统 SHALL 将 D0 登录域作为研发重启第一闸。D0 未通过域级验收前，D1–D6 新功能不得启动；只允许修 D0 依赖、测试、门禁或文档纠偏。

#### Scenario: D0 尚未通过时有人提交 D2 新功能

- **GIVEN** D0-验收未完成
- **WHEN** PR 新增 D2 业务页面或业务能力
- **THEN** reviewer SHALL 要求关闭或改为 D0 依赖修复
- **AND** 该 PR 不 SHALL 合并到 `main`。

### Requirement: 字典映射不得以字符 LCS 作为语义判断依据

系统 SHALL 基于同义词典、编码交叉表、来源权重、组织范围、高危负样本和人工确认完成医学字典映射。字符 LCS、编辑距离或简单字符命中只 MAY 作为低权重召回信号，不 SHALL 作为高置信语义匹配、自动确认或发布依据。

#### Scenario: 肌钙蛋白 T 与肌钙蛋白 I 候选匹配

- **GIVEN** 本地词条为“肌钙蛋白 T”，标准词条包含“肌钙蛋白 I”
- **WHEN** 系统生成候选映射
- **THEN** 结果 SHALL 标记为高危近似 `HIGH`
- **AND** 不 SHALL 批量确认或自动确认
- **AND** 必须等待人工审核和来源证据确认。
