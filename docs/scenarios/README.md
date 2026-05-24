# docs/scenarios/ — 业务场景剧本

> v1.0 GA 阶段：本目录承载 6 大客户验收剧本。
> 每个剧本是「角色 / 触发 / 逐帧 Storyboard / 系统时序 / 设计抉择 / DoD / 演示数据」7 节标准结构。
> 剧本是业务流程的**可执行表达** — AI 实施页面/API 前必须先读相关剧本，再写代码。
> E2E Playwright 用例与剧本一一对应（剧本编号 ↔ `frontend/e2e/<id>.spec.ts`）。

## 6 剧本（与 [docs/product/facts.md §6](../product/facts.md) 一一对应）

| # | 剧本 | 主页面 | 主角 | 阶段 |
|---|---|---|---|---|
| S1 | [AMI/STEMI 路径推荐与入径](S1-ami-pathway.md) | embed/EM01 + clinical/patient-pathways | 主治医生 + 主任 | R3 待填 |
| S2 | [病历内涵质控](S2-record-qc.md) | quality/qc-dashboard + embed/EM02 | 质控员 + 主治 | R3 待填 |
| S3 | [医保智能审核](S3-insurance-audit.md) | quality/insurance-audit + embed/EM03 | 医保办 + 药师 | R3 待填 |
| S4 | [医嘱安全实时拦截](S4-order-intercept.md) | embed/EM04 | 主治 + 药师 | R3 待填 |
| S5 | [配置包跨环境发布](S5-config-package.md) | tenant/config-packages | 信息科主任 | R3 待填 |
| S6 | [院级质控驾驶舱](S6-executive-dashboard.md) | quality/qc-dashboard | 主任 + 租户管理员 | R3 待填 |

## 剧本通用结构（R3 完整填充时使用）

每剧本必含 7 节：

1. **角色与背景** — 谁在用、什么场景、用什么终端
2. **触发事件** — 什么 HIS / 用户操作触发
3. **逐帧 Storyboard** — 0 秒 / 3 秒 / 5 秒 ... 时间轴的屏幕 + 操作
4. **背后系统时序** — Mermaid 时序图，含外部系统调用
5. **关键设计抉择** — 为什么这么设计，背后的产品权衡
6. **验收点（DoD）** — 通过/不通过的客观标准
7. **演示数据与跑通脚本** — `frontend/e2e/` 与 `medkernel-backend/src/test/` 演示数据位置

## 历史源

完整 V2 时代版本：[docs/archive/v0.3/02_场景剧本图.md](../archive/v0.3/02_场景剧本图.md)（1001 行，仅供查阅）

R3 阶段会按 v1.0 GA IA 适配（菜单名 / 路由 / 组件名都按新栈），但业务流程稳定，可较忠实迁移。
