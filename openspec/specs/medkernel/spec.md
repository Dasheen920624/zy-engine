# MedKernel

## 目的

定义 MedKernel 集团医疗智能中枢的稳定产品身份、仓库边界、文档权威顺序、中文文档要求和变更规划要求。

## Requirements

### Requirement: 产品身份

系统 SHALL 将 MedKernel 呈现为面向医疗集团和多级医疗网络的集团医疗智能中枢。

#### Scenario: 新贡献者接手

- **GIVEN** 贡献者打开仓库
- **WHEN** 阅读根目录 README、`AGENTS.md` 或本规格
- **THEN** 能识别产品名称、业务使命、当前权威文档和中文协作要求。

### Requirement: 两层模型

系统 SHALL 按「基础底座 + 引擎服务能力」两层理解。

#### Scenario: 架构评审

- **GIVEN** 评审人需要理解平台结构
- **WHEN** 查看项目文档
- **THEN** 能区分组织、权限、审计、部署等共享底座能力，以及知识、字典、规则、路径、推荐、评估、随访、发布、嵌入和模型网关等引擎能力。

### Requirement: 仓库边界

系统 SHALL 将实现边界划分为后端、前端、文档和部署四类区域。

#### Scenario: 代码导航

- **GIVEN** 开发者查找某个功能实现
- **WHEN** 查看仓库结构
- **THEN** 能在 `medkernel-backend/` 找到后端服务，在 `frontend/` 找到界面，在 `deploy/` 找到部署资产，在 `docs/` 找到当前权威文档。

### Requirement: 文档权威

系统 SHALL 以当前产品、实施和任务文档作为行为、约束、上线顺序和中文协作规则的权威来源。

#### Scenario: 规划变更

- **GIVEN** 团队准备规划新变更
- **WHEN** 查看项目上下文
- **THEN** 能通过权威文档确认范围、约束、当前执行顺序、中文书写要求和远程 `main` 合并门禁。

### Requirement: 可审计变更流

系统 SHALL 将拟议工作记录在 `openspec/changes/` 目录，并将当前稳定行为保存在 `openspec/specs/`。

#### Scenario: 准备功能

- **GIVEN** 出现新的变更请求
- **WHEN** 团队开始 OpenSpec 规划
- **THEN** 应创建包含提案、设计、任务和规格增量的变更目录，并用中文描述实施与验证要求。
