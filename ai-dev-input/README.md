# AI 研发输入包（ai-dev-input/）

本目录是 **AI 协作的运行时状态目录**，不是产品文档。它存放：

1. AI 可执行规格包（OpenAPI / JSON Schema / DDL / 样例数据 / 测试矩阵）
2. AI 协作流程产物（任务卡 / 任务认领 / 评审记录 / 自主运行日志 / 功能验收）

**产品文档与设计文档在项目根 [`docs/`](../docs/)，本目录不重复。**

---

## AI 接手必读顺序

```
1. 项目根 README.md           — 项目概览
2. docs/README.md             — 文档体系导航（V2 金本位）
3. docs/01_产品事实源.md       — 产品全貌（15 分钟）
4. docs/05_AI实施手册.md       — 你要做的 PR 在这里
5. docs/engineering/02_任务台账.md  — 找到任务编号
6. docs/engineering/00_总入口与AI接手导航.md  — 硬门禁与 DoD
```

---

## 目录内容

| 目录 | 用途 |
|---|---|
| `02_api_contracts/` | OpenAPI 契约（路径/规则/图谱/Dify/字典/适配器/配置包） |
| `03_data_models/` | JSON Schema（临床事件、患者上下文、路径配置、规则 DSL、图谱查询等） |
| `04_database/` | 多数据库 DDL（Oracle / DM / PostgreSQL / Kingbase / LOCAL_H2_FILE） |
| `06_samples/` | 演示样例数据（AMI/STEMI 路径、规则、患者上下文、组织、图谱、Dify、字典等） |
| `07_tests/` | 测试矩阵与可执行验收用例 |
| `09_ai_task_cards/` | AI 系统提示词、后端提示词模板、任务卡模板 |
| `10_task_claims/` | 多 AI 并行开发的任务认领（active / archive） |
| `11_ai_reviews/` | 多 AI 开发质量评审记录（pending / approved / archive） |
| `12_autonomous_runs/` | AI 自主开发运行记录（active / archive） |
| `13_feature_acceptance/` | 功能验收记录（GOLD / SILVER / BRONZE / REJECTED） |

---

## AI 协作硬性流程

> 详细规则与硬门禁见 [docs/engineering/00_总入口与AI接手导航.md](../docs/engineering/00_总入口与AI接手导航.md)。

简化流程：

```
1. git pull --ff-only origin main
2. git status -sb 确认工作树干净
3. .\medkernel-mvp\scripts\detect-db-env.ps1 -BootstrapLocal
4. 从 docs/engineering/02_任务台账.md "下一批可领" 段选任务
5. 在 10_task_claims/active/<claim_id>.md 创建并推送任务认领
6. 开发：同步修改后端代码、测试、样例、API 示例、文档
7. 跑测试：.\medkernel-mvp\scripts\run-tests.ps1
            .\medkernel-mvp\scripts\build.ps1
            git diff --check
8. 在 11_ai_reviews/pending/<review_id>.md 创建评审记录
9. APPROVED 且 open_findings=0 后才能正式提交
10. 客户可见或高风险功能：创建 13_feature_acceptance/<id>.md 记录
11. 完成后归档 claim/review 到 archive/YYYYMMDD/
```

---

## 当前优先任务

以 [docs/05_AI实施手册.md](../docs/05_AI实施手册.md) 为准。下一批 PR 简表：

| PR | 任务编号 | 内容 | 依赖 |
|---|---|---|---|
| PR-01 | DOC-V2-TOKENS | 设计 Token 落地 | 无 |
| PR-02 | FE-COMP-001 | 公共组件库 v1（C01-C05） | PR-01 |
| PR-03 | FE-LAYOUT-001 | 路由 + 顶级菜单 + AppLayout | PR-01 |
| PR-04 | SEC-001 | 用户体系最小可用 | ORG-003 ✅ |
| PR-05~12 | 业务页面 | 解锁 6 大剧本演示 | PR-02~04 |

---

## 关键边界

- 路径引擎保存患者路径状态
- 规则引擎执行确定性判断和质控规则
- 图谱引擎封装 DB/Neo4j 查询，不暴露任意 Cypher 执行
- Dify 只负责编排工具和生成解释，不保存核心状态
- Oracle/关系型数据库是配置、版本、审计和运行记录的主数据源
- Neo4j 和 Dify 必须是可插拔 Provider，不能成为 DB-only 测试环境强依赖
- 所有临床建议必须由医生确认
- 所有核心配置必须支持版本、审核、发布、审计和回滚
- 所有规则、知识、图谱证据、Dify 解释、字典映射、适配器口径和质控结论必须支持来源追溯
- 医院差异必须通过组织范围、配置包、字典映射和适配器绑定实现

完整不变量见 [docs/01_产品事实源.md §7](../docs/01_产品事实源.md#7-22-个不变量任何-ai-不得违反)。
