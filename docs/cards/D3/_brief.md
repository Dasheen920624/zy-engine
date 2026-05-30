# D3 临床运行 · 域简报

> 读卡前置：先读 [核心 CONSTITUTION](../../CONSTITUTION.md)，再读本简报，再读你领的那张卡。页面卡另读 [体验契约](../../EXPERIENCE_CONTRACT.md)。
> 本域所有卡共享的上下文放这里，**不在卡间复制**。冲突裁决：核心 > 本简报 > 卡。
> 准入：D2 域级验收通过后才进 D3（核心 §0 纵向推进）——D3 消费 D2 在发布口产出的权威规则/路径/知识/字典/标准上下文。

## 域目标

把**临床主链路**做成 **B0 真实**：建患者 → 入径 → 节点推进 → **确定性提醒/推荐** → 医师采纳/拒绝（带原因）→ 待办/通知闭环 → 随访接续，**全程关模型也真实可跑、可验收**。D3 是"用料"的域——每一次提醒/入径都消费 [D2](../D2/_brief.md) 已发布的权威资产；自己不造料、不写死医学常量、不前端造提醒。

D3 **天然 B0**（铁律 #4 B0 先于模型）：CDSS/随访先交付"规则/路径/知识命中 + 可解释追溯"的确定性路径并验收；模型语义增强/生成（语义检索、鉴别诊断生成）整体后移第二波（wave2 的 LLM-*/AIK-*），D3 仅留模型增强挂点 + 诚实降级（`MODEL_DISABLED` 回确定性兜底），不在 B0 里伪造智能。

## 现状（搬迁时核查 2026-05-30；后端以 `medkernel-backend` 真实包为准复核）

D3 引擎**多有实质骨架**，本域多数卡是"真实化/契约化/补全/框架化"而非从零：

- **临床事件**＝`engine/context/`：`ClinicalEvent` / `ClinicalEventAcceptedResponse` + `ClinicalEventController`（[API-02](API-02.md) 承接同步/异步/批量/回放/死信）。
- **推荐/CDSS**＝`engine/recommendation/`：`RecommendationCard{+DetailResponse/Filter/Repository/Request/Status/Type}` + `RecommendationEngineController`（[API-07](API-07.md) / [CDSS-01](CDSS-01.md) 承接，含医师反馈/疲劳治理）。
- **随访**＝`engine/followup/`：`FollowupEngineService/Controller`、`FollowupEvent(+Repository)`、`FollowupAbnormalReportRequest` + 安全/契约测试（[API-09](API-09.md) / [FOLLOW-01](FOLLOW-01.md) 承接）。
- **嵌入**＝`engine/embed/`：`EmbedEngineService/Controller`、`EmbedLaunchToken`、`EmbedLaunchContextResponse`、`EmbedFeedbackRequest`（[API-11](API-11.md) / [EMBED-01](EMBED-01.md) 承接 launch token 一次性/过期/白名单）。
- **患者主索引**＝`engine/mpi/`：`MpiService/Controller`、`MpiPatient(+Repository)`、`MpiMergeRequest`、`MpiStatsResponse`（[SVC-CLINICAL-01](SVC-CLINICAL-01.md) + 页 [PMI-01](PMI-01.md) 承接）。
- **路径运行 + 规则执行**＝`engine/pathway/`：`ClinicalClock(+Repository)` 关键时钟（[SVC-CLINICAL-01](SVC-CLINICAL-01.md) + 页 [PPATH-01](PPATH-01.md)）；`engine/rule/`：`RuleDslEvaluator` / `RuleDslEvaluation`（[SVC-CLINICAL-02](SVC-CLINICAL-02.md) + 页 [RULECHK-01](RULECHK-01.md)）。
- **前端 7 页面已存在待真实化**（`frontend/src/pages/clinical/`，`app/router.tsx` 真实路由）：`Mpi`→`/mpi` · `PatientPathways`→`/pathway/patients` · `CdssFatigue`→`/cdss/fatigue` · `RuleValidate`→`/rule/validate` · `WorkflowTodos`→`/workflow/todos` · `Notifications`→`/notifications` · `Followup`→`/clinical/followup`；现状＝页面壳已存在，本域页面卡＝去占位/mock + 接真实引擎 API + 六态/五维 RBAC/低打扰齐全。
- **明确缺口**（建卡"现状"段照实写、勿夸大）：CDS Hooks 风格 6 触发点事件契约（[OPT-02](OPT-02.md) 框架化）；器械/CDSS 风险分级矩阵（[OPT-03](OPT-03.md) 新建，NMPA 路径预留）；临床安全红线规则库 + 危害分析 + 静默试运行（[OPT-04](OPT-04.md) 建在 D2 规则引擎之上）；安全撤回与旧版下游隔离端到端（[MED-C3](MED-C3.md) 串 D2 [SYS-08](../D2/SYS-08.md) 权威替换 + 影响病例任务）。

## 登入 / 使用角色（13 角色矩阵本域子集，全量见 [质量基线 §9](../../audit/质量基线.md)）

| 角色 | 在 D3 主要干什么 |
|---|---|
| 临床医生 clinician | 患者主索引/路径运行、接收确定性提醒/推荐、采纳或拒绝（带原因）、待办闭环 |
| 专科专家 specialist | 路径节点推进、规则校验复核、随访方案确认 |
| 科主任 dept-head | 科室待办/路径运行总览、提醒治理（疲劳阈值） |
| 护理 nursing | 待办/通知、随访任务执行、床旁知识 |
| 随访团队 followup | 随访计划/任务/问卷、异常回院触发 |
| 医务处 / 质控（消费） | 提醒采纳率/疲劳治理只读（质控运行在 [D4](../D4/_brief.md)） |

> 角色 → 默认视图与可操作范围由各页五维 RBAC（[BASE-02](../D0/BASE-02.md)/[INFRA-05](../D0/INFRA-05.md)）+ 组织作用域（[BASE-01](../D0/BASE-01.md)）执行；**前端不写死角色逻辑、不前端造提醒/兜底假数**。

## 共享数据模型 / 实体（D3 卡共用，单一归属在此声明、卡内只引用）

- **标准临床模型**：12 类标准对象 + `ClinicalEventContext` 类型骨架单一源在 [SYS-01](../D0/SYS-01.md)（D0）；D3 只消费/读取，不重定义。
- **患者主索引（MPI）**：`MpiPatient` / 合并 / 统计的单一归属在 [SVC-CLINICAL-01](SVC-CLINICAL-01.md)；页 [PMI-01](PMI-01.md) 只消费。
- **推荐卡（RecommendationCard）**：推荐卡模型/状态/反馈/疲劳的单一归属在 [CDSS-01](CDSS-01.md)；[API-07](API-07.md) 暴露契约、页 [REMIND-01](REMIND-01.md) 消费。
- **CDS Hooks 事件契约**：6 触发点（patient-view / order-sign / medication-prescribe / result-review / discharge-sign / followup-alert）单一归属 [OPT-02](OPT-02.md)；[API-02](API-02.md) / [API-07](API-07.md) / [EMBED-01](EMBED-01.md) 引用同一套触发点。
- **临床安全红线**：DDI / 危急值 / 剂量上限 / 抗菌限制 / 特殊人群禁忌的红线库单一归属 [OPT-04](OPT-04.md)（建在 D2 [RULE-01](../D2/RULE-01.md) 规则引擎之上）；[MED-C3](MED-C3.md) 用其做召回升级。
- **嵌入 launch token**：一次性消费 / 过期 / 白名单单一归属 [EMBED-01](EMBED-01.md)；[API-11](API-11.md) 暴露契约。
- **版本与权威**：命中所用规则/路径/知识版本的权威与替换复用 D2 [SYS-04](../D2/SYS-04.md) / [SYS-08](../D2/SYS-08.md)，D3 不另造；旧版仅历史重放（核心 §6）。
- **API 统一输入**：所有 D3 API 复用 [BASE-03](../D0/BASE-03.md) `ApiResult` / `ProblemDetail` / Record DTO + 详规 §1.4 的 12 字段统一入参；大列表复用 [API-13](../D0/API-13.md)。

> **权威源铁律**：唯一权威源是院内关系库（核心 §7 / 铁律 #5）；图（Neo4j）/Dify/缓存/搜索只能是投影或执行器。

## 依赖

- **上游（D2 料 + D0 脊柱 + D1）**：D2 已发布规则/路径/知识/字典/标准上下文（[API-01](../D2/API-01.md) / [RULE-01](../D2/RULE-01.md) / [PATH-01](../D2/PATH-01.md) / [KNOW-01](../D2/KNOW-01.md) / [TERM-01](../D2/TERM-01.md) / [SYS-04](../D2/SYS-04.md) / [SYS-08](../D2/SYS-08.md)）· [BASE-01](../D0/BASE-01.md) OrgContext · [BASE-02](../D0/BASE-02.md)/[INFRA-05](../D0/INFRA-05.md) RBAC · [BASE-03](../D0/BASE-03.md) API 契约 · [BASE-04](../D0/BASE-04.md) 审计 · [BASE-05](../D0/BASE-05.md) 方言 · [BASE-06](../D0/BASE-06.md)/[BASE-08](../D0/BASE-08.md)/[BASE-10](../D0/BASE-10.md) 前端骨架/体验/token · [SYS-01](../D0/SYS-01.md) 标准模型 · [SYS-05](../D0/SYS-05.md) 在线/异步/批量 · [OBS-01](../D0/OBS-01.md) 可观测 · [API-13](../D0/API-13.md) 大列表。
- **下游（D4+）**：D4 质控消费 D3 临床运行数据与提醒采纳；D5 合规消费审计/证据。

## 本域最烫的不变量（点核心编号 + 为何在本域最关键）

- **核心 §13 真实性 + 铁律 #1/#2**：D3 直面临床——提醒/推荐/路径状态必须来自真实引擎命中，**绝不前端造提醒、不写死医学常量、不假采纳率**；关模型返回 `MODEL_DISABLED` 回确定性兜底。
- **核心 §11 B0 先于模型**：CDSS/随访先确定性可跑可验收，模型增强后移 wave2。
- **核心 §6 旧版隔离 + §7 唯一权威 + #14**：命中只用 `ACTIVE` 权威版本；安全撤回（[MED-C3](MED-C3.md)）紧急停用旧版 + 自动派发受影响患者/路径复核任务；旧版仅历史重放。
- **临床安全红线（[OPT-04](OPT-04.md)）+ 风险分级（[OPT-03](OPT-03.md)）**：DDI/危急值/剂量/禁忌红线静默试运行达标才上线；高风险 CDSS 走分级矩阵。
- **低打扰（提醒治理）**：疲劳治理——重复/低价值提醒抑制、采纳/拒绝带原因留痕，避免告警疲劳。
- **核心 §10 集成边界 + 嵌入安全**：嵌入只经 launch token（一次性/过期/白名单）、CDS Hooks 风格事件契约；不绕引擎直写医嘱/病历。

## 域级验收（D3-验收）

D3 全部卡（14 ID + 7 页面）`done` 后过域级验收（[质量基线 §2.3](../../audit/质量基线.md)）：

1. 临床医生/专科专家/护理/随访逐角色登入 → 7 个二级菜单页按五维 RBAC 正确呈现、六态齐全、低打扰；
2. 跑通 D3 B0 主链路 E2E（**全程关模型**）：建患者（MPI）→ 入径（消费 D2 路径）→ 节点推进（关键时钟）→ 确定性提醒/推荐命中（消费 D2 规则/知识）→ 医师采纳/拒绝**带原因**留痕 → 待办/通知闭环 → 随访接续；每步状态机正确、可解释追溯、证据可导出；
3. 嵌入：launch token 一次性消费/过期/白名单为真，CDS Hooks 6 触发点事件契约可达，纯 API/iframe/SDK 三路通；
4. 安全撤回（[MED-C3](MED-C3.md)）：召回/禁忌升级紧急停用旧版后，新请求只命中新版、自动派发受影响患者/路径复核任务、旧版仅历史重放；
5. 关闭模型/Dify/图投影 → D3 主链路仍真实通过（`B0` / `MODEL_DISABLED`，无伪造提醒、无 dangling 404）；
6. T-GATE 前后端真实性门禁全绿（无 Math.random 造数/无写死医学常量/无前端造提醒/无绕 no-page-mock）；owner ≠ reviewer 签字。

**域级验收过，才算 D3 落实，才走 D4。** 模型增强不在 D3（第二波）。
