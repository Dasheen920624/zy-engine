# MedKernel v1.0 GA 单一任务台账

> 版本：4.18 · 2026-05-27
> 当前执行：0 业务引擎全能力上线
> 字段：`id` / `owner` / `status`（pending / in_progress / done / blocked）
> 规则：E1-E5 是当前执行任务；E6 是引擎验收后的业务服务包装清单，不得提前绕过引擎实现。

---

## 0. 当前边界

| 先做 | 后做 |
|---|---|
| 组织、权限、上下文、审计、版本、知识、字典、规则、路径、推荐、评估、随访、包发布、嵌入、模型网关、证据链 | 试点准备、临床运行、质控改进、合规运维、专业领域服务包这些业务菜单包装 |

禁止单病种硬编码、业务 mock 假闭环、业务模块直连模型或 Dify。

---

## E0 · 文档与计划清场

| id | owner | status |
|---|---|---|
| GA-ENG-DOC-01 当前权威文档统一：README、docs README、宪法、总览、实施方案、详细规范、台账 | codex | done |
| GA-ENG-DOC-02 清除旧计划和不相关参考入口 | codex | done |
| GA-ENG-DOC-03 详细规范保留并允许继续细化，新增细节只进唯一详细规范 | codex | done |
| GA-ENG-DOC-04 全系统产品与交互体验固定规范：角色、页面、分页、低打扰、可信解释和体验门禁 | codex | done |

---

## E1 · 基础底座上线

| id | owner | status |
|---|---|---|
| GA-ENG-BASE-01 组织与租户上下文：tenant/group/hospital/campus/site/department/user/role/package version | claude | done |
| GA-ENG-BASE-02 身份权限：用户、角色、菜单权限、动作权限、数据范围、无权限响应 | codex | done |
| GA-ENG-BASE-03 API 契约：ApiResult、ProblemDetail、分页、错误码、DTO 校验、幂等、traceId | claude | done |
| GA-ENG-BASE-04 审计上下文：写操作、审核、发布、运行、反馈、导出、回滚统一留痕 | claude | done |
| GA-ENG-BASE-05 数据迁移：5 方言表族、审计字段、状态字段、版本字段、索引和约束门禁 | codex | done |
| GA-ENG-BASE-06 前端基础：5+1 菜单、路由元数据、PageShell、六态、状态机 Badge、7 步流 | codex | done |
| GA-ENG-BASE-07 运行底座：Feature Flag、配置、监控、健康检查、备份恢复、国产化 profile | codex | done |
| GA-ENG-BASE-08 产品体验底座：一页一目标、角色默认视图、专家模式、服务端分页、详情抽屉、异步导出、保存视图 | codex | done |
| GA-ENG-BASE-09 代码基线净化：移除业务主链路 mock、裸 Map、硬编码示例数据、旧命名和单病种假闭环 | claude | done |
| GA-ENG-BASE-10 前端视觉债净化：硬编码颜色、内联样式、console、localStorage 敏感写入和 axios 直连规则全部归零 | codex | done |

---

## E2 · 引擎接口上线

| id | owner | status |
|---|---|---|
| GA-ENG-OBS-01 引擎可观测性骨干：StateTransitionRecorder / PayloadStoragePort / ErrorCode 增强 / DiagnoseResponse / MDC / TraceIdPropagator / V8 五方言迁移 | claude | done |
| GA-ENG-API-01 标准上下文 API：患者、就诊、诊断、医嘱、报告、组织、包版本快照 | claude | done |
| GA-ENG-API-01b 标准上下文 retrofit：snapshot 接 StateTransitionRecorder / canonical_resource 持久化 trace_id / GET /diagnose / PackageVersionPort 抽象 / 失败 audit 留痕 + V9 audit_event +outcome | claude | done |
| GA-ENG-API-02 临床事件 API：同步、异步、批量、回放、重试、死信、回调 | codex | done |
| GA-ENG-API-03 知识资产 API：来源、解析、引用、版本、审核、替换、历史重放、分页、筛选、搜索、异步导出 | claude | done |
| GA-ENG-API-04 字典映射 API：标准字典、院内字典、候选映射、冲突、发布 | codex | done |
| GA-ENG-API-05 规则引擎 API：定义、测试、影响分析、发布、执行、解释 | codex | done |
| GA-ENG-API-06 路径引擎 API：模板、专病包、患者路径、节点推进、变异、关键时钟 | codex | done |
| GA-ENG-API-07 推荐/CDSS API：触发、推荐卡、来源解释、医师反馈、疲劳治理输入 | codex | done |
| GA-ENG-API-08 评估质控 API：指标、运行、结果、问题、整改、复核 | - | pending |
| GA-ENG-API-09 随访 API：计划、任务、问卷、异常回院、结果回流 | - | pending |
| GA-ENG-API-10 包发布 API：知识包、配置包、校验、灰度、全量、同步、回滚 | - | pending |
| GA-ENG-API-11 嵌入 API：launch token、iframe/SDK/纯 API、回调、降级 | - | pending |
| GA-ENG-API-12 模型能力网关 API：能力代码、路由、脱敏、结构化输出、审计、B0 降级 | - | pending |
| GA-ENG-API-13 大规模列表 API：统一分页/游标、排序、过滤、total estimate、批量任务、导出任务、traceId | - | pending |

---

## E3 · 引擎执行上线

| id | owner | status |
|---|---|---|
| GA-ENG-KNOW-01 知识资产引擎：来源登记、解析、hash、引用锚点、可信分级 | claude | partial |
| GA-ENG-KNOW-02 知识版本引擎：新旧识别、去重、冲突、待审新版、原子替换、旧版隔离 | claude | partial |
| GA-ENG-TERM-01 字典映射引擎：未映射发现、候选推荐、人工确认、冲突处理、映射包发布 | codex | partial |
| GA-ENG-RULE-01 规则引擎：规则 DSL/模板、测试样例、执行结果、风险动作、解释 | - | pending |
| GA-ENG-PATH-01 路径引擎：专病包、分型分支、节点推进、变异、关键时钟、仿真 | - | pending |
| GA-ENG-CDSS-01 推荐引擎：规则/路径/知识综合、提醒卡、采纳/拒绝、解释追溯 | - | pending |
| GA-ENG-EVAL-01 评估质控引擎：指标配置、病例命中、问题生成、整改和复核闭环 | - | pending |
| GA-ENG-FOLLOW-01 随访引擎：计划生成、任务、问卷、异常事件和回流 | - | pending |
| GA-ENG-PKG-01 包发布引擎：导入导出、校验、灰度、全量、同步、回滚、证据 | - | pending |

---

## E4 · 嵌入、模型与证据上线

| id | owner | status |
|---|---|---|
| GA-ENG-EMBED-01 iframe/SDK/纯 API 嵌入：启动、安全、最小数据、反馈、降级占位 | - | pending |
| GA-ENG-LLM-01 模型能力网关：provider 无关、组织/场景路由、结构输出、调用审计 | - | pending |
| GA-ENG-LLM-02 B0/B1/B2：无模型基线、模型辅助、探索生成的策略和验收 | - | pending |
| GA-ENG-EVID-01 证据链：来源、生成、审核、发布、运行、反馈、整改、回滚可导出 | - | pending |
| GA-ENG-DEGRADE-01 降级链：模型、Dify、图投影、外部系统故障时主链路仍可运行 | - | pending |

---

## E5 · 引擎全能力验收

| id | owner | status |
|---|---|---|
| GA-ENG-QA-01 引擎 E2E：来源到推荐、评估、随访、包发布、嵌入和证据全链路 | - | pending |
| GA-ENG-QA-02 五方言迁移、性能、并发、备份恢复和国产化自检 | - | pending |
| GA-ENG-QA-03 医疗安全：AI 候选标识、医师确认、禁忌红线、旧版隔离、高风险审核 | - | pending |
| GA-ENG-QA-04 无模型/无 Dify/无图投影验收：B0 主链路通过 | - | pending |
| GA-ENG-QA-05 引擎全能力上线评审：允许进入业务服务包装阶段 | - | pending |
| GA-ENG-QA-06 产品体验验收：10 万级列表分页筛选、低打扰嵌入、六态、可信解释、证据导出、驾驶舱下钻通过 | - | pending |
| GA-ENG-QA-07 代码净化验收：生产代码无业务 mock、无新接口裸 Map、无前端假闭环、无旧计划引用 | - | pending |

---

## E6 · 业务服务包装（引擎验收后启动）

| id | owner | status |
|---|---|---|
| GA-SVC-PILOT-01 租户与组织服务包：集团、医院、院区、社区、科室、角色、生命周期 | - | pending |
| GA-SVC-PILOT-02 接入与数据质量服务包：HIS/EMR/LIS/PACS/医保/病案/随访适配、字段映射、体检 | - | pending |
| GA-SVC-PILOT-03 资产准备服务包：知识包、配置包、字典映射、规则、路径、灰度、全量、回滚 | - | pending |
| GA-SVC-CLINICAL-01 患者与路径运行服务包：MPI、患者路径、关键时钟、变异、节点推进 | - | pending |
| GA-SVC-CLINICAL-02 临床提醒与反馈服务包：CDSS 卡片、规则校验、疲劳治理、采纳/不采纳回流 | - | pending |
| GA-SVC-CLINICAL-03 临床协同服务包：待办、通知、护理、报告解读、床旁知识、随访触发 | - | pending |
| GA-SVC-QUALITY-01 质控驾驶舱服务包：院级指标、风险热力、价值指标、下钻和证据 | - | pending |
| GA-SVC-QUALITY-02 病案医保服务包：病历内涵质控、DRG/DIP、编码、费用、医保审核 | - | pending |
| GA-SVC-QUALITY-03 整改闭环服务包：问题生成、责任科室、整改、复核、豁免、报告 | - | pending |
| GA-SVC-COMPLIANCE-01 身份安全服务包：用户、身份绑定、数据权限、租户隔离、安全基线 | - | pending |
| GA-SVC-COMPLIANCE-02 审计运维服务包：审计日志、证据包、Provider/模型状态、备份恢复、离线许可 | - | pending |
| GA-SVC-DOMAIN-01 专病路径服务包：胸痛/心梗、卒中、肿瘤、慢病、感染、围手术期、妇儿、急重症、基层双向转诊、中医药 | - | pending |
| GA-SVC-DOMAIN-02 专业协同服务包：护理、药事、医技报告、手术麻醉输血、营养康复心理疼痛、院感公卫、科研真实世界、区域协同 | - | pending |

---

## 修订记录

| 版本 | 日期 | 修改人 | 主要变更 |
|---|---|---|---|
| 4.18 | 2026-05-27 | Codex | GA-ENG-API-07 完成：新增 V13 五方言迁移（`recommendation_trigger`、`recommendation_card`、`recommendation_source`、`recommendation_feedback`、`recommendation_fatigue_signal`）；新增推荐/CDSS 触发、推荐卡、来源解释、医师反馈、疲劳治理输入和诊断 API；补齐 `recommendation.write` 权限并保持医生反馈使用 `recommendation.accept`；推荐卡强制来源解释，高风险/红线推荐强制医师确认，不自动写医嘱、诊断或病历。后端完整测试 372 个通过、0 失败，3 个 Docker 依赖多方言烟测因本机 Docker 不可用按既有机制跳过 |
| 4.17 | 2026-05-27 | Codex | GA-ENG-API-02 完成：V10 五方言迁移扩展 `clinical_event` 并新增 `clinical_event_payload`、`clinical_event_outbox`；新增临床事件同步/异步/批量接收、详情、payload、诊断、重放接口；接入 `event.read` / `event.write` 权限、`ENG-EVENT-001..006` 错误码、状态历史、审计和 outbox worker 重试/死信链路。后端 302 测试通过、3 个 Docker 依赖多方言冒烟测试跳过；前端 79 测试通过，`npm run verify` 与 `npm run build` 通过 |
| 4.16 | 2026-05-27 | Claude | GA-ENG-API-01b 完成：V9 五方言迁移（audit_event +outcome +error_code）+ AuditEvent.failure 工厂 + IsolatedAuditPublisher（PROPAGATION_REQUIRES_NEW 子事务保失败 audit 不丢）+ CanonicalResource +traceId 字段 + Repository findByTraceIdOrderBySeqNoAsc + PackageVersionPort 抽象 + LenientPackageVersionAdapter 默认实现（替代 PackageVersionResolver）+ ContextSnapshotService 接入 StateTransitionRecorder（INITIAL_CREATE → ACTIVE）+ 失败路径发 outcome=FAILED audit（ENG-CONTEXT-002 / ENG-CONTEXT-003）+ GET /snapshots/{id}/diagnose 端点（@perm.has('context.read')）+ 端到端验收测试（spec §6.2）。后端 275 测试 / 前端 79 测试 / lint/typecheck/build 四步全绿 |
| 4.15 | 2026-05-27 | Claude | GA-ENG-OBS-01 完成：V8 五方言迁移（state_transition_history 表 + canonical_resource ADD trace_id）+ ErrorCode 加 ErrorClass(INPUT/AUTH/DATA/EXTERNAL/INTERNAL) + retryable + ENG-OBS-001/002 + StateTransitionRecorder（同事务写历史、RuntimeException 兜底、DataAccessException 向上抛）+ PayloadStoragePort 接口 + InMemoryPayloadStorage 默认实现（@ConditionalOnMissingBean，第三层 DbPayloadStorage 自动让位）+ DiagnoseResponse + Assembler + MdcEnrichmentFilter + TraceIdPropagator + AsyncTaskExecutorConfig。后端 252 测试 / 前端 79 测试 / lint/typecheck/build 四步全绿 |
| 4.14 | 2026-05-26 | Claude | GA-ENG-API-01 完成：V7 五方言迁移（context_snapshot/canonical_resource/clinical_event/context_idempotency_key）+ 12 个 Canonical Record DTO + ContextValidator / PackageVersionResolver / TerminologyMappingPort（@ConditionalOnMissingBean noop）+ ContextSnapshotService（含幂等、按 patient/encounter 翻页倒序）+ Controller 三接口（POST/GET by ID/GET 列表）+ PermissionCode 追加 context.read/context.write + ErrorCode 追加 ENG-CONTEXT-001..004 + DefaultPermissionPolicy 接入临床/接入/审核三类角色 + 审计 action=CREATE/resource_type=context_snapshot。后端 223 测试 / 前端 79 测试 / lint/typecheck/build 四步全绿 |
| 4.13 | 2026-05-26 | Claude | E0/E1 全面核查闭环：BASE-01..10 全部真 done（182 后端测试 + 79 前端测试 + 5 方言迁移通过）。补齐 docs/README.md 声明但缺失的辅助目录骨架（handbook/implementation.md、handbook/operations.md、handbook/user-guides/、handbook/training/、adr/、legal/、release/、release/v1.0.0-ga-evidence.md 占位骨架），目录结构与文档声明完全一致。E2 首单选定 GA-ENG-API-01 标准上下文 API |
| 4.12 | 2026-05-26 | Codex | GA-ENG-BASE-08 完成：新增路由体验声明、公共分页/筛选/详情/导出/视图组件底座，以字典映射作为真实接口只读样板页，补充外部连接与视图敏感内容门禁；验证执行 `npm run lint`、`npm run format:check`、`npm run typecheck`、`npm test`、`npm run build` |
| 4.11 | 2026-05-26 | Claude | GA-ENG-BASE-09 完成：三 PR 顺序合并（#80 门禁、#81 后端净化删 58 个旧 Java 文件、#82 前端净化 22 业务页改占位卡 + TerminologyMapping 真接入 + medkernel/no-page-mock ESLint 门禁 + RoadmapLink 组件）。BusinessMetrics 保留 W1-G6 constitutional helpers；五方言 V1..V6 无旧业务表无需 V7 drop；platform/migration → migration 顶层包重定位 |
| 4.10 | 2026-05-26 | Claude | GA-ENG-BASE-09 in_progress：架构师审计 + 三个 PR 顺序计划落地。同步对齐 git 实际进度：GA-ENG-API-03/04 标 done（commit ddfb950 / cb39796），GA-ENG-KNOW-01/02 与 GA-ENG-TERM-01 标 partial |
| 4.9 | 2026-05-26 | Codex | GA-ENG-BASE-07 完成：运行底座合同接口、`govcloud` 国产化 profile、备份恢复 SHA-256 摘要校验、前端 Provider 状态真实快照和完整验证收口 |
| 4.8 | 2026-05-26 | Codex | GA-ENG-BASE-05 完成：五方言 `V1` 至 `V6` 迁移序列、表族、索引、业务约束、租户/审计/状态/版本字段合同门禁及完整 Flyway smoke 收口 |
| 4.7 | 2026-05-26 | Codex | GA-ENG-BASE-02 完成：角色权限覆盖、范围隔离的用户角色分配、当前用户权限画像接口、受控审计快照入口及菜单/页面/动作/数据范围闭环 |
| 4.6 | 2026-05-26 | Codex | GA-ENG-BASE-10 完成：inline style 归零、门禁 error、视觉债归零 |
| 4.5 | 2026-05-26 | Codex | GA-ENG-BASE-10 基础收敛：新增受控 UI 偏好存储封装，移除生产代码中的 token localStorage 读取和 console 输出，ESLint 阻断生产代码直接访问浏览器存储与 console |
| 4.4 | 2026-05-25 | Codex | GA-ENG-BASE-06 完成：5+1 菜单与路由元数据统一、PageShell/PageState/MetricGrid 体验底座、六态与状态机 Badge、页面分页与移动端表格滚动、桌面/移动布局验证 |
| 4.3 | 2026-05-25 | Claude | GA-ENG-BASE-02 基础：PermissionCode/RoleCode 枚举、默认权限策略、`@perm.has(...)` 动作授权、`@DataScope` 数据范围声明与组织接口接入 |
| 4.2 | 2026-05-25 | Claude | GA-ENG-BASE-01 完成（JwtClaimsResolver + TenantContextEnricherFilter + OrgUnit 实体/Repository/Service/Controller + SecurityConfig 集成；roles claim → ROLE_* 权限）。GA-ENG-BASE-02 仍 pending |
| 4.1 | 2026-05-25 | Claude | GA-ENG-BASE-03 API 契约骨架完成；组织与审计上下文及 `org_unit`、`audit_event` 五方言迁移基础落位 |
| 4.0 | 2026-05-24 | Codex | 最终收束：增加代码净化门禁，细化 E6 业务医疗服务包，明确 AI 团队交付顺序 |
| 3.1 | 2026-05-24 | Codex | 增加全系统产品与交互体验固定规范及分页、低打扰、可信解释验收任务 |
| 3.0 | 2026-05-24 | 用户决策 + Codex | 台账重排为“0 业务引擎全能力上线”，业务划分后置 |

---

**End of MedKernel backlog.**
