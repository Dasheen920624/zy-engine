# Feature Acceptance: REFIT-001 已实现能力全量盘点与一致性基线

## 功能描述

对项目所有已实现能力进行全量盘点，输出能力矩阵、API/页面/表/测试清单、P0/P1/P2 改造 finding 和验收基线。

## 对应任务

REFIT-001

## 验收等级

SILVER

- 主流程可用：已实现能力矩阵完整、Finding 分类清晰、验收基线可量化
- 非阻断优化项：部分 Finding 需要代码验证（如 Service 层 @Transactional 覆盖率），当前基于 Controller 层扫描

## 验收证据

- 文档：`docs/engineering/REFIT-001_已实现能力盘点与一致性基线.md`
- 后端扫描：16 Controller / 83 端点
- 前端扫描：7 实际页面 / 18 占位 / 5 组件
- 数据库扫描：31 表 / 四库同步率 74%
- 测试扫描：95 契约测试 + 38 前端测试 + 7 smoke 脚本 + 19 样例

## P0 Finding 摘要

1. SEC 表生产部署脚本缺失
2. pe_recommendation_record 仅 H2 存在
3. cfg_config_package 索引不一致
4. 8 个 Controller 未接入组织上下文
5. 图谱/来源/术语/适配器/Dify 仅内存态

## 性能数据

N/A（文档产出型任务）

## 已知问题

- Service 层 @Transactional 覆盖率未深度扫描
- 前端组件 Storybook 覆盖率未验证
- 部分内存态模块的 @PostConstruct 重建逻辑未逐一验证
