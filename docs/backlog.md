# MedKernel v1.0 GA 单一任务台账

> 版本：5.2（重做基线 R2 · 补 BASE-11 平台首发种子身份 + R2-NEW 医疗知识首发资产生产）· 2026-05-29
> 当前执行：P0 真实性门禁与归零 →（系统架构 ∥ 无模型 AI 工厂 ∥ 假闭环清零）
> 字段：`id` / `owner` / `status`（pending / in_progress / done / blocked）
> 标记：`⟳R2` = 重做基线重置项（done→in_progress），须按 §0.3 验收铁律重建。
> 重做基线 R2：以 `GA-ENG-FOLLOW-01`（E3）为分界线，该任务（v4.29 起）及其之后全部重做；分界线前经核查证实有假的项一并纳入。详见 [改造任务总清单 R2 v2](audit/2026-05-29-改造任务总清单.md)、[AI 工厂深度报告](audit/2026-05-29-全系统深度核查与AI工厂重构总报告.md)、[一次性核查总报告](audit/2026-05-29-全系统一次性核查与改造总报告.md)。
> v5.1 调整：据全面重读宪法/落地规划/详规/体验规范，删除 v5.0 误造的"R2-NEW 临床安全引擎"层（DRUG/CRITICAL/DOSE/AMS 下沉到 GA-XXX-01 领域门面；DOCPARSE 并入 AIK-STD-02；GRAPH 并入 KNOW-R2；TERMSET 并入 TERM-R2）；按规划本身补齐 GA-SYS-01~08 系统架构、14 项 GA-XXX-01 领域门面、12 项 OPT/EMR-LEVEL 世界级补强。

---

## 0.1 当前边界与执行顺序

| 先做 | 后做 |
|---|---|
| 已 done 真实底座 → P0 真实性门禁 →（P1a 系统架构 SYS-01~08 ∥ P1b 无模型 AI 工厂 AIK-STD-01~12 + MED-C1/C2/C3 ∥ P1c 假闭环清零 KNOW/TERM/CDSS/FOLLOW/PKG/EVID/EMBED/LLM/INTEG R2）| P2 接真实模型（LLM-03~08）→ P3 真连接器与互操作（OPT-01/02/03）→ P4 14 领域门面 GA-XXX-01 → P5 世界级补强（OPT-04~10 + EMR-LEVEL）→ P6 SVC 业务包重做（作为集成包）→ P7 重新验收 |

禁止：单病种硬编码、业务 mock 假闭环、假证据/假同步、业务模块直连模型或 Dify、绕真实性门禁、伪造引擎层（专业领域必须复用引擎不另起）。

---

## 0.3 验收铁律（所有 ⟳R2 与新增任务通用）

1. **真实性**：禁 `写死 switch/常量当结果`、`Math.random 造数`、`catch 吞错伪造成功`、`UUID 充哈希`、`前端写死业务数据`、`假证据/假同步`、`测试钩子混生产`、`硬编码身份署名`。
2. **诚实降级**：无模型/无连接器/无图投影时返回诚实状态（B0/NOT_CONNECTED/NOT_SYNCED）+ 真实主链路。
3. **医疗安全**：AI 内容明显标识；医师确认才进病历；高风险强制审核/双签；禁自动开医嘱/诊断；旧版隔离；高危近似/剂量/禁忌不可批量自动通过。
4. **无模型可运行**：每个含 AI 增强的能力，先交付"无模型确定性 + 人工"路径并通过验收，再叠加模型。
5. **门禁先行**：真实性门禁先于业务重做生效，禁 `eslint-disable` 绕过。
6. **测试有效性**：覆盖 正常/参数失败/跨租户/权限/并发幂等/降级；禁 mock 掉真实现把假算法固化为绿。
7. **关系库权威**：业务事实唯一权威源为院内关系库；图数据库/Dify/模型/缓存只能是投影或执行器（落地规划 §9.4 强制约束）。
8. **唯一权威知识**：同一适用域同时只有一个 `ACTIVE_AUTHORITATIVE` 版本；待审新版只能审核不能执行（详规 §8.13）。

---

## E0 · 文档与计划清场

| id | owner | status |
|---|---|---|
| GA-ENG-DOC-01 当前权威文档统一：README、docs README、宪法、总览、实施方案、详细规范、台账 | codex | done |
| GA-ENG-DOC-02 清除旧计划和不相关参考入口 | codex | done |
| GA-ENG-DOC-03 详细规范保留并允许继续细化，新增细节只进唯一详细规范 | codex | done |
| GA-ENG-DOC-04 全系统产品与交互体验固定规范：角色、页面、分页、低打扰、可信解释和体验门禁 | codex | done |
| GA-ENG-DOC-05 引擎能力、业务范围和第三方对接口径统一：S0-S40 API 归类、第三方接入矩阵、业务包装边界和验收门禁 | codex | done |
| GA-ENG-DOC-06 业务细节一致性核查：多维治理切片、E6 服务包、任务台账和文档导航口径统一 | codex | done |

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
| GA-ENG-BASE-11 平台首发种子身份与生产环境初始化：dev profile 已有 `PlatformCredentialDevSeeder` 种 13 角色账号；生产 profile 缺首次部署初始平台管理员能力——首启动无任何账号无法登录。要求：①首次启动 init token 机制（一次性、自动过期、不可重复使用、写审计）创建首个 `platform-admin`；②强制首次登录改密 + MFA 配置；③可选 CLI 工具（如 `medkernel-cli admin create`）应急重置；④运维手册写清首次部署步骤；⑤反例：硬编码默认密码进入生产、init token 不过期、token 不审计 | - | pending |

---

## E2 · 引擎接口上线

| id | owner | status |
|---|---|---|
| GA-ENG-OBS-01 引擎可观测性骨干：StateTransitionRecorder / PayloadStoragePort / ErrorCode 增强 / DiagnoseResponse / MDC / TraceIdPropagator / V8 五方言迁移 | claude | done |
| GA-ENG-API-01 标准上下文 API：患者、就诊、诊断、医嘱、报告、组织、包版本快照 | claude | done |
| GA-ENG-API-01b 标准上下文 retrofit：snapshot 接 StateTransitionRecorder / canonical_resource 持久化 trace_id / GET /diagnose / PackageVersionPort 抽象 / 失败 audit 留痕 + V9 audit_event +outcome | claude | done |
| GA-ENG-API-02 临床事件 API：同步、异步、批量、回放、重试、死信、回调 | codex | done |
| GA-ENG-API-03 知识资产 API：来源、解析、引用、版本、审核、替换、历史重放、分页、筛选、搜索、异步导出 | claude | done |
| GA-ENG-API-04 ⟳R2 字典映射 API：标准字典、院内字典、候选映射、冲突、发布 | codex | in_progress |
| GA-ENG-API-05 规则引擎 API：定义、测试、影响分析、发布、执行、解释 | codex | done |
| GA-ENG-API-06 路径引擎 API：模板、专病包、患者路径、节点推进、变异、关键时钟 | codex | done |
| GA-ENG-API-07 ⟳R2 推荐/CDSS API：触发、推荐卡、来源解释、医师反馈、疲劳治理输入 | codex | in_progress |
| GA-ENG-API-08 评估质控 API：指标、运行、结果、问题、整改、复核 | codex | done |
| GA-ENG-API-09 随访 API：计划、任务、问卷、异常回院、结果回流 | codex | done |
| GA-ENG-API-10 ⟳R2 包发布 API：知识包、配置包、校验、灰度、全量、同步、回滚 | codex | in_progress |
| GA-ENG-API-11 嵌入 API：launch token、iframe/SDK/纯 API、回调、降级 | codex | done |
| GA-ENG-API-12 ⟳R2 模型能力网关 API：能力代码、路由、脱敏、结构化输出、审计、B0 降级 | codex | in_progress |
| GA-ENG-API-13 大规模列表 API：统一分页/游标、排序、过滤、total estimate、批量任务、导出任务、traceId | codex | done |

> ⟳R2 说明：API-04/07/10/12 接口契约层核查为真，因下游引擎重做随之回退，重做完成后统一复核标 done。

---

## E3 · 引擎执行上线

| id | owner | status |
|---|---|---|
| GA-ENG-KNOW-01 ⟳R2 知识资产引擎（含图投影增强）：来源登记、解析、hash、引用锚点、可信分级；关系库 graph_node/edge/citation 为权威源，Neo4j 仅查询投影可重建，无图降级关系库查询 | claude | in_progress |
| GA-ENG-KNOW-02 ⟳R2 知识版本引擎：新旧识别、去重、冲突、待审新版、原子替换、旧版隔离；对接 GA-SYS-08 + GA-AIK-STD-09/10 | claude | in_progress |
| GA-ENG-TERM-01 ⟳R2 字典映射引擎（含标准编码集导入 + 医学语义匹配 MED-C1）：未映射发现、候选推荐、人工确认、冲突处理、映射包发布；ICD-10 国临版/ICD-9-CM-3/药品本位码/LOINC 兼容映射导入+版本；LCS→同义词典+编码交叉表+模型嵌入；高危近似负样本判别器（钾/钠、肌钙蛋白T/I、左/右、剂量量级强制 HIGH，禁批量/禁自动确认）| codex | in_progress |
| GA-ENG-RULE-01 规则引擎：规则 DSL/模板、测试样例、执行结果、风险动作、解释 | codex | done |
| GA-ENG-PATH-01 路径引擎：专病包、分型分支、节点推进、变异、关键时钟、仿真 | codex | done |
| GA-ENG-CDSS-01 ⟳R2 推荐引擎：规则/路径/知识综合、提醒卡、采纳/拒绝、解释追溯 | codex | in_progress |
| GA-ENG-EVAL-01 评估质控引擎：指标配置、病例命中、问题生成、整改和复核闭环 | codex | done |
| GA-ENG-FOLLOW-01 ⟳R2 随访引擎（重做基线分界线起点）：计划生成、任务、问卷、异常事件和回流 | codex | in_progress |
| GA-ENG-PKG-01 ⟳R2 包发布引擎：导入导出、校验、灰度、全量、同步、回滚、证据 | codex | in_progress |

> ⟳R2 说明：FOLLOW-01 为分界线起点；KNOW-01/02、TERM-01、CDSS-01 为分界线前经核查证实有假的项。RULE-01/PATH-01/EVAL-01 核查广度未见假，保留 done。

---

## E4 · 嵌入、模型与证据上线

| id | owner | status |
|---|---|---|
| GA-ENG-EMBED-01 ⟳R2 iframe/SDK/纯 API 嵌入：启动、安全、最小数据、反馈、降级占位；对接 OPT-02 CDS Hooks 风格事件契约 | codex | in_progress |
| GA-ENG-LLM-01 ⟳R2 模型能力网关：provider 无关、组织/场景路由、结构输出、调用审计 | codex | in_progress |
| GA-ENG-LLM-02 ⟳R2 B0/B1/B2：无模型基线、模型辅助、探索生成的策略和验收 | codex | in_progress |
| GA-ENG-EVID-01 ⟳R2 证据链：来源、生成、审核、发布、运行、反馈、整改、回滚可导出 | codex | in_progress |
| GA-ENG-INTEG-01 ⟳R2 第三方对接能力总线：适配器目录、FHIR/CDS Hooks 风格门面、Webhook 签名、字段映射、健康检查、重试死信和接口证据（契约层）| codex | in_progress |
| GA-ENG-INTEG-02 ⟳R2 第三方接口文档与契约模板：接入概览、OpenAPI/事件 schema、字段映射、鉴权签名、幂等重试、回调、降级和验收证据 | codex | in_progress |
| GA-ENG-DEGRADE-01 ⟳R2 降级链：模型、Dify、图投影、外部系统故障时主链路仍可运行 | codex | in_progress |

---

## E5 · 引擎全能力验收（真实闭环重建后重跑，不可在假闭环上盖章）

| id | owner | status |
|---|---|---|
| GA-ENG-QA-01 ⟳R2 引擎 E2E：来源到推荐、评估、随访、包发布、嵌入和证据全链路 | codex | in_progress |
| GA-ENG-QA-02 ⟳R2 五方言迁移、性能、并发、备份恢复和国产化自检 | codex | in_progress |
| GA-ENG-QA-03 ⟳R2 医疗安全：AI 候选标识、医师确认、禁忌红线、旧版隔离、高风险审核 | codex | in_progress |
| GA-ENG-QA-04 ⟳R2 无模型/无 Dify/无图投影验收：B0 主链路通过 | codex | in_progress |
| GA-ENG-QA-05 ⟳R2 引擎全能力上线评审：允许进入业务服务包装阶段 | codex | in_progress |
| GA-ENG-QA-06 ⟳R2 产品体验验收：10 万级列表分页筛选、低打扰嵌入、六态、可信解释、证据导出、驾驶舱下钻通过 | codex | in_progress |
| GA-ENG-QA-07 ⟳R2 代码净化验收：生产代码无业务 mock、无新接口裸 Map、无前端假闭环、无旧计划引用 | codex | in_progress |
| GA-ENG-QA-08 ⟳R2 第三方对接验收：HIS/EMR/LIS/PACS/手麻/输血/医保/公卫/区域平台/模型 Provider 断连、重试、降级、审计和证据导出通过 | codex | in_progress |

---

## E6 · 业务服务包装（作为集成包，由前述引擎 + GA-XXX-01 领域门面 + OPT 横切组合而成）

| id | owner | status |
|---|---|---|
| GA-SVC-PILOT-01 ⟳R2 租户与组织服务包：集团、医院、院区、社区、科室、角色、生命周期 | codex | in_progress |
| GA-SVC-PILOT-02 ⟳R2 接入与数据质量服务包：HIS/EMR/LIS/PACS/医保/病案/随访适配、字段映射、体检 | codex | in_progress |
| GA-SVC-PILOT-03 ⟳R2 资产准备服务包：知识包、配置包、字典映射、规则、路径、灰度、全量、回滚 | codex | in_progress |
| GA-SVC-CLINICAL-01 ⟳R2 患者与路径运行服务包：MPI、患者路径、关键时钟、变异、节点推进 | codex | in_progress |
| GA-SVC-CLINICAL-02 ⟳R2 临床提醒与反馈服务包：CDSS 卡片、规则校验、疲劳治理、采纳/不采纳回流；集成 GA-PHARMACY-01 + GA-CRITICAL-01 | codex | in_progress |
| GA-SVC-CLINICAL-03 ⟳R2 临床协同服务包：待办、通知、护理、报告解读、床旁知识、随访触发；集成 GA-NURSING-01 + GA-REPORT-01 + GA-POC-KNOW-01 | codex | in_progress |
| GA-SVC-QUALITY-01 ⟳R2 质控驾驶舱服务包：院级指标、风险热力、价值指标、下钻和证据；集成 OPT-08 价值指标 | codex | in_progress |
| GA-SVC-QUALITY-02 ⟳R2 病案医保服务包：病历内涵质控、DRG/DIP、编码、费用、医保审核 | codex | in_progress |
| GA-SVC-QUALITY-03 ⟳R2 整改闭环服务包：问题生成、责任科室、整改、复核、豁免、报告 | codex | in_progress |
| GA-SVC-COMPLIANCE-01 ⟳R2 身份安全服务包：用户、身份绑定、数据权限、租户隔离、安全基线 | codex | in_progress |
| GA-SVC-COMPLIANCE-02 ⟳R2 审计运维服务包：审计日志、证据包、Provider/模型状态、备份恢复、离线许可；集成 GA-EMR-LEVEL-02 | codex | in_progress |
| GA-SVC-INTEGRATION-01 ⟳R2 第三方业务接口服务包：接入管理、字段映射、健康检查、FHIR/CDS Hooks 门面、Webhook 回调、区域平台和监管/评级证据交换 | codex | in_progress |
| GA-SVC-DOMAIN-01 ⟳R2 专病路径服务包（集成包）= GA-CRITICAL-01 + GA-PERIOP-01 + GA-ONCO-RENAL-01 + GA-SPECIAL-POP-01 + GA-TCM-HEALTH-01 + GA-PRIMARY-CARE-01 + GA-INFECTION-PH-01 等专病分支 | codex | in_progress |
| GA-SVC-DOMAIN-02 ⟳R2 专业协同服务包（集成包）= GA-NURSING-01 + GA-PHARMACY-01 + GA-REPORT-01 + GA-POC-KNOW-01 + GA-ALLIED-CARE-01 + GA-RWD-01 + GA-REGION-COLLAB-01 等专业组合 | codex | in_progress |

---

## P0 · 真实性门禁与归零（前置，必须先做）

| id | owner | status |
|---|---|---|
| T-GATE-01 前端真实性门禁增强：升级 eslint-plugin medkernel/no-page-mock，阻断 catch 内伪造数据/函数包装绕 AST/camelCase 绕过/假数据/`eslint-disable medkernel/*`，放行合法静态 UI 文案 | - | pending |
| T-GATE-02 后端真实性门禁：CI 脚本扫 src/main 阻断 Math.random/写死医学常量(如"高血压"/"I10")/catch 吞错返回成功/UUID 充哈希/Javadoc 模拟占位于生产路径 | - | pending |
| T-RESET-01 backlog 据实重置：按改造清单 R2 v2 调整状态 + 写入 §0.3 验收铁律 + 修订记录，本台账设为 R2 施工基线 | claude | done |

---

## R2-NEW · 系统架构强化（落地规划 §7.11，规划本身要求但 backlog 未登记）

| id | owner | status |
|---|---|---|
| GA-SYS-01 标准临床模型与事件上下文：12 类标准对象（Patient/Encounter/Condition/Observation/Medication/Procedure/DiagnosticReport/Document/NursingAssessment/CarePlan/FollowUp/Claim）、来源映射、质量字段、事件契约落库和 API | - | pending |
| GA-SYS-02 引擎领域边界与服务契约：模块依赖单向、OpenAPI/事件契约、权限审计要求固定 | - | pending |
| GA-SYS-03 关系库权威源与投影同步：图谱/Dify 投影可关闭、可重建、可审计、可降级 | - | pending |
| GA-SYS-04 版本继承与发布框架：资产不可变版本、组织继承（平台→集团→医院→院区→社区→科室→专病）、灰度、回滚、历史重放 | - | pending |
| GA-SYS-05 在线/异步/批量/离线运行框架：四类运行模式都有故障和重试验证 | - | pending |
| GA-SYS-06 安全合规与证据框架：数据权限、脱敏、审计、导出审批和证据包可验证 | - | pending |
| GA-SYS-07 非功能验收基线：性能、可用、可观测、多方言、降级和恢复测试报告完整 | - | pending |
| GA-SYS-08 权威知识版本解析与原子替换框架：唯一有效约束、替代链、运行解析、紧急失效、影响病例任务和历史重放；与 GA-AIK-STD-09 协同 | - | pending |

---

## R2-NEW · AI 医疗知识工厂（生产核心，详规 §8.11 + §7.12.5）

> 铁律：模型只产候选不产事实；先无模型可运行。

### AI 工厂核心（12 项）

| id | owner | status |
|---|---|---|
| GA-AIK-STD-01 来源与全类资产 schema + 统一元数据（详规 §8.4），五方言迁移落地 | - | pending |
| GA-AIK-STD-02 文档解析、引用锚点与版本存证：PDF/Word 章节识别+表格理解+切片+锚点+hash；任一候选可定位原文+hash；无解析能力时人工录入兜底 | - | pending |
| GA-AIK-STD-03 术语编码与院内映射流水线：标准词/本地词/映射/冲突闭环；与 TERM-01-R2 MED-C1 协同 | - | pending |
| GA-AIK-STD-04 规则/路径/推荐/指标/随访候选生成：候选进统一审核台（无模型时模板+人工）| - | pending |
| GA-AIK-STD-05 安全校验与冲突仲裁（详规 §8.9 11 项门禁）：高风险发布前阻断+双审；冲突仲裁通道；与 OPT-07 联动 | - | pending |
| GA-AIK-STD-06 静默运行、反馈和回归评测：历史脱敏病例批量跑+表现报告；与 OPT-06 联动 | - | pending |
| GA-AIK-STD-07 知识包/配置包生成与院内同步：离线/灰度/回滚/证据（接 PKG-R2）| - | pending |
| GA-AIK-STD-08 最新知识探索、差异检测与过期治理：定时/手动/离线发现+检索时点+待审闭环 | - | pending |
| GA-AIK-STD-09 权威知识替换、旧版失效与影响处置：替代链/紧急撤回/病例复核/证据完整 | - | pending |
| GA-AIK-STD-10 生成期知识身份识别、去重与审核分流：8 态分流；重复/旧版进普通审核数=0 | - | pending |
| GA-AIK-STD-11 待审新版共存与替换提醒：新版未审仅差异审核，旧版继续运行 | - | pending |
| GA-AIK-STD-12 全医疗专业领域标准资产模板与首批专业资产（前后端，详规 §8.12）：替换 AiReview 占位；左右对照/高风险优先/批量限低风险/差异留痕/一键追溯；通用领域包 schema；护理/报告/床旁/综合照护可发布 | - | pending |

### 模型赋能底座（6 项，详规 §7.12.5）

| id | owner | status |
|---|---|---|
| GA-LLM-03 数据最小化与外调安全：字段白名单/脱敏/审批/阻断/证据；与 OPT-09 联动 | - | pending |
| GA-LLM-04 提示词、工具和模型版本治理：输出可重放/版本可回滚/审计可导出 | - | pending |
| GA-LLM-05 全业务模型增强接入矩阵：详规 §7.12.2 全适用业务有能力码+B0 卡 | - | pending |
| GA-LLM-06 可信来源探索编排：受控检索/检索时点/来源核验/候选闭环 | - | pending |
| GA-LLM-07 模型安全和医学回归评测：引用真实性/红线/基准集/专家复核报告；与 OPT-06 联动 | - | pending |
| GA-LLM-08 provider 真实接入（B1 本地/B2 外部/Dify）：缺位仍诚实降级 B0；无模型/故障降级验收 | - | pending |

---

## R2-NEW · 医疗严谨性横切（3 项）

| id | owner | status |
|---|---|---|
| MED-C1 字典映射改医学语义匹配 → 已合并入 GA-ENG-TERM-01-R2（高危近似负样本判别器作为 TERM 引擎子能力）| - | pending |
| MED-C2 规则 DSL 补临床算子：between/unit_compare(单位换算表)/temporal(时间窗·连续次数)/derived(受控算术 eGFR/CrCl/BSA)，高危算子配测试病例门禁；剂量计算能力由 derived 算子提供 | - | pending |
| MED-C3 安全撤回与旧版下游隔离端到端：召回/禁忌升级紧急停用旧版 + 受影响患者/路径复核任务自动生成（详规 §8.13）；与 GA-SYS-08 + GA-AIK-STD-09 协同 | - | pending |

---

## R2-NEW · 世界级 + 国情补强（落地规划 §22.3 + §22.7，12 项）

> **OPT-04 临床安全案例与红线规则库 = "临床安全"真实位置（横切红线规则，非引擎层）**

| id | owner | status |
|---|---|---|
| GA-OPT-01 标准临床模型与 FHIR 门面设计（P0）：至少覆盖 Patient/Encounter/Condition/Observation/Medication/Procedure/CarePlan/ServiceRequest/DiagnosticReport/DocumentReference | - | pending |
| GA-OPT-02 CDS Hooks 风格事件契约（P0）：定义 6 类触发点（patient-view/order-sign/medication-prescribe/result-review/discharge-sign/followup-alert）、上下文、返回卡片、采纳/拒绝反馈 | - | pending |
| GA-OPT-03 医疗器械与 CDSS 风险分级矩阵（P0）：每个 AI/CDSS 功能标记"参考信息/推荐选项/风险评分/诊断输出/治疗指令"级别；NMPA 路径预留 | - | pending |
| **GA-OPT-04 临床安全案例与红线规则库 ⭐**（P0）：危害分析、红线规则、静默试运行、灰度标准、回滚标准；DDI/危急值/剂量上限/抗菌限制/特殊人群禁忌等红线规则作为**规则引擎资产**进入，与 MED-C1/C2/C3 联动 | - | pending |
| GA-OPT-05 互联互通测评映射（P1）：数据资源/标准化/基础设施/应用效果映射到产品证据；与 GA-EMR-LEVEL 联动 | - | pending |
| GA-OPT-06 AI 质量评测中心（P1）：字典/规则/路径/推荐/解释/中文术语回归集；幻觉拦截；与 GA-AIK-STD-06 / GA-LLM-07 联动 | - | pending |
| GA-OPT-07 来源证据分级与冲突仲裁（P1）：来源评分（A 法规/B 国家指南/C 共识文献/D 院内/E 反馈）、证据等级（GRADE 兼容）、冲突队列、专家仲裁；与 GA-AIK-STD-05 联动 | - | pending |
| GA-OPT-08 价值指标与 ROI 看板（P1）：采纳率/误报率/漏报回溯/路径完成率/整改闭环率/医保违规减少可视化；与 SVC-QUALITY-01 联动 | - | pending |
| GA-OPT-09 数据最小化策略引擎（P1）：每个任务和外部调用有字段白名单、脱敏策略、审批记录；与 GA-LLM-03 / GA-SYS-06 联动 | - | pending |
| GA-OPT-10 插件和生态安全边界（P2）：插件权限、审计、数据范围、临床安全门禁完整 | - | pending |
| GA-EMR-LEVEL-01 电子病历评级目标与项目映射（P0）：按医院目标等级（4/5/6 级为主）输出能力差距、依赖系统和实施任务；详规 §9 全章节 | - | pending |
| GA-EMR-LEVEL-02 评级数据质量和证据包（P0）：应用覆盖、数据质量、CDSS/质控闭环和审计证据；与 SVC-COMPLIANCE-02 联动 | - | pending |

---

## R2-NEW · 全医疗领域门面（落地规划 §3.4 + §18，宪法 §1.#15 强制要求）

> 每个 GA-XXX-01 = "规则资产 + 路径资产 + 知识资产 + CDSS 卡 + 嵌入 + 评估 + 随访" 的领域专精组合，**复用同一引擎链路，不另起业务实现**。
> 通用辅助诊疗/路径/质控（GA-CDSS-ASSIST-01 / PATH-SPECIALTY-01 / QC-COMPLEX-01）在引擎层 R2 复核内即可，不单列任务。

### P0 共用临床能力（必须随引擎 GA 一起交付，3 项）

| id | owner | status |
|---|---|---|
| GA-NURSING-01 护理专业领域门面（S20、S35）：护理分级（WS/T 431-2023）/评估/决策/计划/复评/交班/护理质控；规则+路径+推荐卡+随访；护理人员确认才落级 | - | pending |
| GA-REPORT-01 医技报告解读领域门面（S17、S36）：检验/影像/病理/内镜/功能报告解读；危急值闭环；原报告不改写；趋势识别 | - | pending |
| GA-POC-KNOW-01 床旁知识查阅领域门面（S37）：说明书/指南/路径/院内制度的当前权威查询；待审版本不参与；患者关联条款 | - | pending |

### P1 高风险与连续照护（6 项）

| id | owner | status |
|---|---|---|
| **GA-PHARMACY-01 药事与药物治疗领域门面 ⭐**（S18、S31）：药品本位码 + 说明书事实 + DDI/过敏/禁忌/剂量提醒 + 抗菌药物分级管理 + 处方点评；高风险强提醒+医师确认；无库诚实标"未覆盖" | - | pending |
| **GA-CRITICAL-01 急诊重症与生命支持领域门面 ⭐**（S19、S24、S27）：分诊/恶化预警/危急值闭环/脓毒症/VTE/呼吸支持 + 升级处置；接收确认 + 超时升级 + 全程审计 | - | pending |
| **GA-SPECIAL-POP-01 妇产/儿科/老年/特殊人群领域门面 ⭐**（S28）：人群标识 + 剂量计算（依赖 MED-C2 derived 算子）+ 禁忌提醒 + 母婴/儿童路径 + 专用随访 | - | pending |
| GA-PERIOP-01 围术期/麻醉/输血/介入领域门面（S26、S33）：围术路径、安全核查、用血/准入/器械规则；时序规则；输血闭环 | - | pending |
| GA-ONCO-RENAL-01 肿瘤/透析/移植/生殖/日间领域门面（S29）：周期/方案/监测、并发症管理、长期随访；日间病房 | - | pending |
| GA-ALLIED-CARE-01 康复/营养/心理/疼痛/安宁照护领域门面（S38）：评估、计划、复评、转介、连续照护 | - | pending |

### P1 中国场景与协同（4 项）

| id | owner | status |
|---|---|---|
| GA-TCM-HEALTH-01 中医药/中西医结合/健康管理领域门面（S39）：病名/证候/治法/方药/适宜技术、独立中医路径 + 中西医结合分支；不替代/不延迟急危重症标准救治 | - | pending |
| GA-INFECTION-PH-01 院感/公卫/预防/职业健康领域门面（S21）：感染风险、报告卡预填、上报事件、干预闭环 | - | pending |
| GA-PRIMARY-CARE-01 基层慢病/双向转诊领域门面（S30）：分层管理、转诊接续、复诊、连续随访 | - | pending |
| GA-REGION-COLLAB-01 医技互认/远程协同领域门面（S40）：检查检验互认、远程协同、跨机构来源证据 | - | pending |

### P2 扩展（1 项 + 1 横切）

| id | owner | status |
|---|---|---|
| GA-SPECIALTY-EXT-01 扩展专科领域门面（S33、S34）：口腔/眼耳鼻喉/皮肤/移植/生殖/职业健康/科研按统一领域包模板接入 | - | pending |
| GA-RWD-01 科研/真实世界/数据服务（S34）：脱敏队列、指标数据集、伦理授权、数据证据 | - | pending |

---

## R2-NEW · 医疗知识首发资产生产（GA-KNOWGEN-01~15，共 15 项）

> **定位**：AI 工厂（工具）+ 真实模型（产能）+ 知识首发包（产品兑现）是三件不同的事，缺一就是空壳。
> **时机**：在 P3 真模型接入完成后启动，与 P4 领域门面并行，必须先于 P6 SVC 业务包完成。
> **路线**：AI 工厂大规模生成候选 → 专业审核 → 灰度发布 → 试点医院可用首发集。
> **不是从零写**：100d 是 "AI 跑批 + 专家审核工作量" 的总和，不是 100 天纯人工写。
> **不追求 100% 覆盖**：追求"试点医院能用的最小集合"，后续靠 GA-AIK-STD-08 持续探索补全。

| id | 资产域 | 首发覆盖标准 | 主要依赖 | owner | status |
|---|---|---|---|---|---|
| GA-KNOWGEN-01 标准术语首发包 | 字典 | ICD-10 国临版全量 + ICD-9-CM-3 全量 + 药品本位码 Top 3000 + 院内常用检验/检查 Top 500 + LOINC 兼容映射 | TERM-01-R2、GA-AIK-STD-03 | - | pending |
| GA-KNOWGEN-02 药品说明书事实首发包 ⭐ | 说明书事实 | 国家批准 Top 1500 药品的结构化（适应症/禁忌/剂量/相互作用/不良反应/特殊人群/警示），全部带原文锚点 hash | GA-AIK-STD-02/04、GA-LLM-08 | - | pending |
| GA-KNOWGEN-03 国家/学会指南条款首发包 | 指南建议 | 国家发布临床路径 30 个 + 国家学会指南 50 个，结构化（推荐级别/证据等级/适用人群/条件/动作）| GA-AIK-STD-04、GA-OPT-07 | - | pending |
| GA-KNOWGEN-04 临床规则首发包 | 规则候选 | DDI 高危规则 Top 200 + 危急值规则全量 + 抗菌药物分级规则全量 + 围术期安全核查规则 + 病案首页质控规则；每条带测试病例 | GA-KNOWGEN-02/03、OPT-04 红线规则库、MED-C1/C2 | - | pending |
| GA-KNOWGEN-05 专病路径首发包 | 路径候选 | 国家临床路径 30 个 + 院内常见专病 20 个，含分型分支/关键时钟/节点/变异/随访接续 | GA-KNOWGEN-03、PATH-01 | - | pending |
| GA-KNOWGEN-06 CDSS 推荐模板首发包 | 推荐模板 | 用药推荐/检查推荐/治疗推荐/路径下一步 各 50 个模板 | 上述 + CDSS-01-R2 | - | pending |
| GA-KNOWGEN-07 评估指标首发包 | 评估指标 | 国家发布质控指标全量 + DRG/DIP 核心规则 + 病历内涵质控指标 50+ | EVAL-01、GA-EMR-LEVEL-01 | - | pending |
| GA-KNOWGEN-08 随访计划首发包 | 随访计划 | 30 专病的随访计划模板（时间窗/任务/问卷/异常回院规则）| GA-KNOWGEN-05、FOLLOW-01-R2 | - | pending |
| GA-KNOWGEN-09 护理资产首发包 | 护理分级与计划 | 护理分级（WS/T 431-2023）全量 + 风险量表 20+ + 护理计划模板 30+ + 交班知识 | GA-NURSING-01 | - | pending |
| GA-KNOWGEN-10 医技报告解读首发包 | 报告解读知识 | 检验/影像/病理/内镜/功能 5 类报告解读知识 + 危急值规则 + 趋势规则 | GA-REPORT-01 | - | pending |
| GA-KNOWGEN-11 床旁知识卡首发包 | 床旁知识卡 | 常见说明书/指南/制度的现行权威条款检索资产 | 上述资产 + GA-POC-KNOW-01 | - | pending |
| GA-KNOWGEN-12 中医药资产首发包 | 中医药资产 | 95 个中医优势病种路径 + 适宜技术 + 方药/中成药风险知识 | GA-TCM-HEALTH-01 | - | pending |
| GA-KNOWGEN-13 医保病案资产首发包 | 医保病案事实 | DRG/DIP 全量规则 + 病案首页质控 + ICD 编码规则 | GA-KNOWGEN-01、SVC-QUALITY-02 | - | pending |
| GA-KNOWGEN-14 公卫/院感资产首发包 | 公卫院感规则 | 法定传染病上报规则 + 感染风险评估 + 不良事件分类 | GA-INFECTION-PH-01 | - | pending |
| GA-KNOWGEN-15 首发资产总验收 | 总验收 | 14 类资产合并形成"试点医院首发知识包 v1.0"；A1-A9 验收剧本能跑通；OPT-04 红线规则全部生效 | 上述全部 | - | pending |

> **关键设计**：每项首发包必须经过详规 §8.13 的"知识身份比对 + 8 态分流"——即使是首发也走"新主题/新版本"通道，不走绕过。**OPT-04 红线规则**（DDI/危急值/剂量上限/抗菌限制/特殊人群禁忌）必须先于普通规则进入审核台，**禁止 AI 自动通过，必须临床医师双签**。

---

## 重新验收门（取代旧 QA 注水，全部满足方可宣告 GA）

1. T-GATE-01/02 门禁全绿，无 `eslint-disable` 绕过，§0.3 铁律 0 违反。
2. GA-SYS-01~08 系统架构强化通过；关系库唯一权威源 + 投影可重建 + 唯一权威知识版本约束生效。
3. AI 工厂"无模型可运行"组（GA-AIK-STD-01~12）验收通过；审核台真实可审可发。
4. **OPT-04 临床安全案例与红线规则库** + GA-PHARMACY-01 + GA-CRITICAL-01 + GA-SPECIAL-POP-01 通过医疗安全用例。
5. 真跨引擎 E2E（QA-01-R2）通过 + A1-A9 全功能验收剧本（详规 §16.2）通过。
6. 真实模型组（有 provider）+ 故障降级组（无 provider/无 Dify/无图/断网）双向通过。
7. 前端 14 业务包假覆盖清零。
8. 第三方真连接器断连/重试/降级/证据通过（含 OPT-01 FHIR / OPT-02 CDS Hooks）。
9. GA-EMR-LEVEL-01/02 评级目标支撑能力 5/6 级齐备。
10. 15 个 GA-XXX-01 领域门面全部按宪法 §1.#15 实现卡完成。
11. **GA-ENG-BASE-11 平台首发种子身份完成**：生产 profile 部署后可经 init token 创建首个平台管理员、强制改密+MFA、全程审计；运维手册写清首次部署步骤。
12. **GA-KNOWGEN-01~15 医疗知识首发资产生产完成**：14 类资产合并形成"试点医院首发知识包 v1.0"；A1-A9 验收剧本能跑通；OPT-04 红线规则全部生效。

---

## 修订记录

| 版本 | 日期 | 修改人 | 主要变更 |
|---|---|---|---|
| 5.2 | 2026-05-29 | Claude | **补两块产品兑现链条上原本漏掉的环节**——（A）**GA-ENG-BASE-11 平台首发种子身份与生产环境初始化**：核查发现 `PlatformCredentialDevSeeder` 仅 `@Profile("dev")`，V27 platform_credential 表生产环境空白，**首次部署无任何账号无法登录**；新增 BASE-11 要求 init token 机制 + 强制首次改密+MFA + CLI 应急工具 + 运维手册首次部署步骤。（B）**R2-NEW 医疗知识首发资产生产（GA-KNOWGEN-01~15，15 项 pending）**：AI 工厂（工具）+ 真实模型（产能）+ 知识首发包（产品兑现）是三件不同的事，前两者已立项，知识首发包之前缺失。15 项按详规 §8.5 资产模型分域：标准术语/药品说明书事实/指南条款/临床规则/专病路径/CDSS 模板/评估指标/随访计划/护理/医技报告解读/床旁知识卡/中医药/医保病案/公卫院感 + 总验收。AI 大规模生成候选 + 专家审核 + 灰度发布，工作量 ~100d（不是 100d 纯人工写）。时机：P3 真模型接入后启动，必须先于 P6 SVC 业务包完成。新验收门加 #11 BASE-11 + #12 KNOWGEN-15。 |
| 5.1 | 2026-05-29 | Claude | **R2 路线据规划本身形态调整**：全面重读宪法/落地规划/详规/体验规范后，发现 v5.0 R2-NEW 章节存在两类系统性偏差——（A）误造"临床安全引擎"独立层（DRUG/CRITICAL/DOSE/AMS 是 §18 业务领域门面而非引擎；DOCPARSE 是 AIK-STD-02 内涵；GRAPH 是 KNOW 图投影；TERMSET 是 TERM 标准词导入）；（B）漏列规划本身要求的整层（GA-SYS-01~08 系统架构 / 14 项 GA-XXX-01 领域门面 / OPT-01~10 + EMR-LEVEL 共 12 项世界级补强）。本次调整：①删除 v5.0 R2-NEW 临床安全引擎章节；②DOCPARSE 合并入 GA-AIK-STD-02、GRAPH 合并入 GA-ENG-KNOW-01-R2 描述、TERMSET 合并入 GA-ENG-TERM-01-R2 描述、DOSE 算术能力合并入 MED-C2 derived 算子；③新增 R2-NEW 系统架构强化（GA-SYS-01~08，8 项）；④新增 R2-NEW 世界级 + 国情补强（OPT-01~10 + EMR-LEVEL-01/02，12 项，含 OPT-04 临床安全案例与红线规则库——这才是"临床安全"真实位置）；⑤新增 R2-NEW 全医疗领域门面（GA-NURSING/REPORT/POC-KNOW/PHARMACY/CRITICAL/SPECIAL-POP/PERIOP/ONCO-RENAL/ALLIED-CARE/TCM-HEALTH/INFECTION-PH/PRIMARY-CARE/REGION-COLLAB/SPECIALTY-EXT/RWD，14 项，宪法 §1.#15 强制要求覆盖全医疗专业领域）；⑥SVC-DOMAIN-01/02 重定位为"集成包"（由 GA-XXX-01 组合而成，不另起业务实现）；⑦MED-C1 已合并入 TERM-01-R2 描述。施工台账见 [改造任务总清单 R2 v2](audit/2026-05-29-改造任务总清单.md)。 |
| 5.0 | 2026-05-29 | Claude | 重做基线 R2：执行 T-RESET-01，据 [改造任务总清单 R2](audit/2026-05-29-改造任务总清单.md) §0.4 据实重置 backlog。以 GA-ENG-FOLLOW-01（v4.29 起由能力不足 AI 主导，核查证实系统性假闭环/假证据/绕门禁/写死候选/验收注水）为分界线，该任务及其后全部 done→in_progress：E3 KNOW-01/02、TERM-01、CDSS-01、FOLLOW-01、PKG-01；E4 全 7；E5 QA 全 8；E6 SVC 全 14；前置区 E2 API-04/07/10/12（下游引擎重做随之回退）——合计 39 项 ⟳R2。保留 done 30 项（DOC/BASE/OBS/API 真实子集/RULE/PATH/EVAL，核查广度未见假）。新增 pending 31 项（v5.1 据规划重整为 ~57 项）。写入 §0.3 验收铁律，本台账设为 R2 施工基线。撤销 4.40/4.41 将经核查退回项翻回 done 的不实操作。 |
| 4.48 | 2026-05-29 | Codex | 对全量平台进行显微镜级深度核查与架构安全审计，出具 `audit_report.md` 物理审计报告；并针对重试抖动与外部断连震荡场景，对 `ClinicalEventOutboxWorker` 进行精细化优化。 |
| 4.47 | 2026-05-29 | Codex | E6 阶段终极大收官通关（v1.0 GA 版本）。推进并物理交付最后 4 个挂起业务服务包。 |
| 4.46 | 2026-05-29 | Codex | 推进资产准备与审计运维 2 大核心业务服务包物理封仓开发交付。 |
| 4.45 | 2026-05-29 | Codex | 推进临床与质控剩余 3 大核心业务服务包物理封仓开发交付。 |
| 4.43 | 2026-05-29 | Codex | 身份安全与临床协同两大核心业务服务包完美通关。 |
| 4.42 | 2026-05-29 | Codex | 业务服务包装阶段（E6 阶段）首战告捷，完整开发并交付租户与组织服务包。 |
| 4.41 | 2026-05-28 | Codex | 顶级引擎全能力验收（E5 阶段）全链路物理收口完美通关。 |
| 4.40 | 2026-05-28 | Codex | 引擎真实性彻底整治工程完美收官（已被 v5.0 标记为不实操作并撤销）。 |
| 4.39 | 2026-05-28 | Claude | 引擎真实性代码核查：发现 EVID-01 证据大导出为空、LLM-01 编造 B2 引文、INTEG-01 适配器 Ping 用 Math.random、KNOW-01 缺 hash 锚点、TERM-01 字符 LCS 致临床误配、前端 Provenance/AdapterHub 假闭环、no-page-mock 被 camelCase 绕过失效。据实将 KNOW/TERM/LLM-01/EVID-01/INTEG-01 从 done 回退 in_progress。 |
| 4.38 | 2026-05-28 | Codex | GA-ENG-TERM-01 & GA-ENG-KNOW-01/02 完成。 |
| 4.37 | 2026-05-28 | Codex | GA-ENG-EVID-01 完成。 |
| 4.36 | 2026-05-28 | Codex | GA-ENG-INTEG-01 & GA-ENG-INTEG-02 完成。 |
| 4.35 | 2026-05-28 | Codex | GA-ENG-LLM-01 完成。 |
| 4.34 | 2026-05-28 | Codex | GA-ENG-EMBED-01 完成。 |
| 4.33 | 2026-05-28 | Codex | GA-ENG-PKG-01 完成。 |
| 4.32 | 2026-05-28 | Codex | 补充第三方接口文档与契约模板任务。 |
| 4.31 | 2026-05-28 | Codex | 业务细节一致性核查：多维治理切片。 |
| 4.30 | 2026-05-28 | Codex | 统一引擎能力、业务范围和第三方对接口径，新增 GA-ENG-DOC-05。 |
| 4.29 | 2026-05-28 | Codex | GA-ENG-FOLLOW-01 完成（**v5.0 已识别为分界线起点：此版本起由能力不足 AI 主导**）。 |
| 4.27 | 2026-05-28 | Codex | GA-ENG-EVAL-01 完成。 |
| 4.26 | 2026-05-28 | Codex | GA-ENG-CDSS-01 完成。 |
| 4.25 | 2026-05-28 | Codex | GA-ENG-API-13 完成。 |
| 4.24 | 2026-05-27 | Codex | 领单 GA-ENG-API-13。 |
| 4.23 | 2026-05-27 | Codex | GA-ENG-API-12 完成。 |
| 4.22 | 2026-05-27 | Codex | GA-ENG-API-11 完成。 |
| 4.21 | 2026-05-27 | Codex | GA-ENG-API-09 完成。 |
| 4.20 | 2026-05-27 | Codex | GA-ENG-API-10 完成。 |
| 4.19 | 2026-05-27 | Codex | GA-ENG-API-08 完成。 |
| 4.18 | 2026-05-27 | Codex | GA-ENG-API-07 完成。 |
| 4.17 | 2026-05-27 | Codex | GA-ENG-API-02 完成。 |
| 4.16 | 2026-05-27 | Claude | GA-ENG-API-01b 完成。 |
| 4.15 | 2026-05-27 | Claude | GA-ENG-OBS-01 完成。 |
| 4.14 | 2026-05-26 | Claude | GA-ENG-API-01 完成。 |
| 4.13 | 2026-05-26 | Claude | E0/E1 全面核查闭环：BASE 全部 done。 |
| 4.12 | 2026-05-26 | Codex | GA-ENG-BASE-08 完成。 |
| 4.11 | 2026-05-26 | Claude | GA-ENG-BASE-09 完成。 |
| 4.10 | 2026-05-26 | Claude | GA-ENG-BASE-09 in_progress。 |
| 4.9 | 2026-05-26 | Codex | GA-ENG-BASE-07 完成。 |
| 4.8 | 2026-05-26 | Codex | GA-ENG-BASE-05 完成。 |
| 4.7 | 2026-05-26 | Codex | GA-ENG-BASE-02 完成。 |
| 4.6 | 2026-05-26 | Codex | GA-ENG-BASE-10 完成。 |
| 4.5 | 2026-05-26 | Codex | GA-ENG-BASE-10 基础收敛。 |
| 4.4 | 2026-05-25 | Codex | GA-ENG-BASE-06 完成。 |
| 4.3 | 2026-05-25 | Claude | GA-ENG-BASE-02 基础。 |
| 4.2 | 2026-05-25 | Claude | GA-ENG-BASE-01 完成。 |
| 4.1 | 2026-05-25 | Claude | GA-ENG-BASE-03 API 契约骨架完成。 |
| 4.0 | 2026-05-24 | Codex | 最终收束：增加代码净化门禁。 |
| 3.1 | 2026-05-24 | Codex | 增加全系统产品与交互体验固定规范。 |
| 3.0 | 2026-05-24 | 用户决策 + Codex | 台账重排为"0 业务引擎全能力上线"。 |

---

**End of MedKernel backlog.**
