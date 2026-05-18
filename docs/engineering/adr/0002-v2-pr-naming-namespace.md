# ADR-0002: V2 PR 命名空间隔离（PR-V2-XX 不复用历史 FE-XXX）

- 状态：Accepted
- 日期：2026-05-18
- 决策者：AI 一致性核查
- 涉及范围：任务管理

## 上下文

V2 实施手册第一版用了 FE-004、FE-005、FE-006 等编号作为 PR 任务标识。但任务台账里 FE-004 已 DONE 是"配置包中心"（V1 任务），V2 想用同一编号作"路径模板列表"。这导致：

- AI 接手时混淆：FE-004 到底是 V1 的 DONE 任务还是 V2 的 TODO 任务？
- 任务台账状态不一致：DONE + TODO 同时存在
- 历史 git log / claim / review 全部对应不上

## 决策

**V2 实施手册的 12 PR 全部使用 `PR-V2-XX`（XX 为 01~12）作为权威主键**，与历史 FE-XXX 编号完全隔离。

- PR-V2-01 = 设计 Token 落地（不复用任何历史编号）
- PR-V2-02 = 公共组件库 v1
- ...
- PR-V2-12 = 院级质控驾驶舱

历史 FE-XXX 编号在任务台账保持原义不动。

## 不变量

- I-1：V2 任务在台账登记必须用 `PR-V2-XX` 为主键
- I-2：claim_id、review_id、commit message 必须含 `PR-V2-XX`
- I-3：V2 实施手册可在卡片标注"别名 FE-XXX"作辅助识别，但**别名不作为权威标识**
- I-4：未来 V3 升级时同样隔离命名空间（PR-V3-XX）

## 替代方案及拒绝原因

- **直接复用 FE-XXX 编号** → 拒绝：与历史定义冲突，AI 无法识别
- **改历史 FE-XXX 编号为 V1-FE-XXX** → 拒绝：影响已有 git history、claim/review/feature_acceptance 引用
- **V2 用 V2-PR-XX** → 拒绝：与 PR-XX（github PR 编号）混淆

## 影响

正面：
- AI 接手时无歧义
- 历史 git 数据不受影响
- 命名直观（V2 = 第二版 PR 序列）

负面：
- V2 实施手册多一层"别名"映射
- 客户验收/演示时需要解释 PR-V2 含义

## 强制方式

- `docs/engineering/02_任务台账.md §2.5` V2 PR 总览表用 `PR-V2-XX` 主键
- `docs/05_AI实施手册.md` 每个 PR 卡片"任务编号"用 `PR-V2-XX（别名 ...）` 形式
- `verify-task-prereq.ps1 -TaskId` 接受 `PR-V2-XX` 格式

## 相关参考

- 任务台账：[`docs/engineering/02_任务台账.md §2.5`](../02_任务台账.md)
- AI 实施手册：[`docs/05_AI实施手册.md §2`](../../05_AI实施手册.md#2-pr-拓扑图)
