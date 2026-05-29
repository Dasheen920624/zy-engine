# MedKernel v1.0 GA 任务清单

> 版本：7.0 · 2026-05-30 · **单一基线 · 项目未上线**
> 编制原则：本台账推倒重做，废止 v5.0~v6.1 所有补丁式修订（连续 6 轮反馈暴露 done 注水，已无任何 done 可信）。
> 任务依据：[宪法 §1](CONSTITUTION.md) · [基础底座与服务能力总览](MEDKERNEL_FOUNDATION_AND_SERVICES.md) · [落地规划 §17/§18](MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md) · [详规 §7/§8](MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md) · [产品体验固定规范](MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md)
> 验收方法：每项 done 必须在 PR 中提供核查证据。任务通过标准模板 + 10 条验收铁律见 [质量基线指南](audit/质量基线.md)。

---

## 0. 总则

### 0.1 状态二元

| 状态 | 含义 |
|---|---|
| `pending` | 未完成（含未启动 + 进行中）|
| `done` | 已完成且经核查（PR 中含核查证据：代码 permalink + 测试报告 + 文档同步 + 审计员签字）|

废止 `in_progress` / `blocked` 等冗余状态——项目未上线，简单到只有"做了没 / 经核查没"。

### 0.2 10 条验收铁律（所有任务通用）

1. **真实性**：禁止写死结果/Math.random 造数/catch 吞错伪造成功/UUID 充哈希/前端 mock 假闭环/假证据假同步
2. **诚实降级**：无模型/无连接器/无图投影时返回诚实状态 + 真实主链路
3. **医疗安全**：AI 内容明显标识；医师确认才进病历；高风险强制审核/双签；禁自动开医嘱
4. **无模型可运行**：每个含 AI 增强的能力必须先交付"无模型确定性 + 人工"路径
5. **关系库权威**：业务事实唯一权威源为院内关系库；图数据库/Dify/模型/缓存只能是投影或执行器
6. **唯一权威知识**：同一适用域同时只有一个 `ACTIVE_AUTHORITATIVE` 版本；待审新版只能审核不能执行
7. **五维权限**：菜单/动作/数据/资产/环境五维全部生效，到 27 二级菜单 + 5 高级工具粒度
8. **六态完整**：加载/空/错误/无权限/部分成功/正常六态在每个页面齐全
9. **中文优先**：客户可见默认中文；技术对象（JSON/DSL/trace）默认隐藏到专家模式
10. **文档同步**：代码与文档同 PR；文档对应章节锚点必须在 commit message 引用

### 0.3 任务通过标准模板

每项任务 `pending → done` 必须在 PR 中提供：

```
✅ 核查证据清单
- 代码：GitHub permalink × N
- 测试：单测/契约/E2E 覆盖率 + 关键用例文件
- 真实性：T-GATE 通过截图
- 文档：宪法/落地规划/详规 对应章节锚点
- 验收：A1-A9 剧本对应剧本（若涉及）
- 审计员签字：@<reviewer>（owner ≠ reviewer）
```

详见 [docs/audit/质量基线.md](audit/质量基线.md)。

### 0.4 推进节奏

按落地规划 §17 两段式：

```
E0 文档清场 → E1 底座 → E2 引擎接口 → E3 引擎执行
   → E4 嵌入/模型/证据 → E5 引擎全能力验收
   → E6 业务服务包装
```

横切任务（X-SYS / X-AIK / X-LLM / X-MED / X-OPT / X-DOMAIN / X-KNOWGEN / X-INFRA）按依赖图嵌入对应阶段并行推进。

---

## 1. E0 文档与计划清场

| ID | 任务 | 工作量 | 状态 |
|---|---|---:|---|
| DOC-01 | 权威文档统一（宪法/总览/实施方案/详规/台账）| 1d | pending |
| DOC-02 | 清除旧计划和不相关参考入口 | 0.5d | pending |
| DOC-03 | 唯一详细规范继续细化的口径固定 | 0.5d | pending |
| DOC-04 | 全系统产品与交互体验固定规范 | 1d | pending |
| DOC-05 | 引擎能力/业务范围/第三方对接口径统一 | 1d | pending |
| DOC-06 | 业务细节一致性核查（多维治理切片）| 1d | pending |

**小计**：6 项 / 5d

---

## 2. E1 基础底座（11 项）

| ID | 任务 | 范围 | 工作量 | 状态 |
|---|---|---|---:|---|
| BASE-01 | 组织与租户上下文 | tenant/group/hospital/campus/site/department/user/role/package_version 八层 + 全链路注入 | 3d | pending |
| BASE-02 | 身份权限（五维） | 菜单/动作/数据/资产/环境 + 13 角色矩阵 + 27 二级菜单粒度（详规 §7.3 + 宪法 §5）| 5d | pending |
| BASE-03 | 标准 API 契约 | ApiResult + ProblemDetail 口径统一 + Record DTO + Bean Validation + traceId + 幂等 | 4d | pending |
| BASE-04 | 审计骨干 | 写操作/审核/发布/运行/反馈/导出/回滚统一留痕；audit 异步或 fail-soft 不回滚业务 | 3d | pending |
| BASE-05 | 5 方言数据迁移骨架 | h2/postgres/oracle/dm/kingbase + 一致性测试 + 中文注释 + 索引约束 | 4d | pending |
| BASE-06 | 5+1 菜单与前端骨架 | 路由元数据 + PageShell + 六态 + 状态机 Badge + 7 步流组件 + 命令面板 | 4d | pending |
| BASE-07 | 运行底座 | Feature Flag + 监控 + 健康检查 + 备份恢复 + 国产化 profile | 3d | pending |
| BASE-08 | 产品体验底座 | 一页一目标 + 角色默认视图 + 专家模式 + 服务端分页 + 详情抽屉 + 异步导出 + 保存视图 | 3d | pending |
| BASE-09 | 代码基线净化 | 无 mock 假闭环 + 无裸 Map + 无硬编码业务示例 + 无单病种硬编码 | 2d | pending |
| BASE-10 | 设计 Token 系统 | Antd token + 5 主题模式（default/elder/dark/eye/system）+ module.css 全部走 var + stylelint 阻断 hex | 4d | pending |
| BASE-11 | 平台首发种子身份 | 生产环境 init token + 强制改密 + MFA + CLI 应急 + 运维手册首次部署步骤 | 3d | pending |

**小计**：11 项 / 38d

---

## 3. E2 引擎接口（14 项）

| ID | 任务 | 工作量 | 状态 |
|---|---|---:|---|
| OBS-01 | 引擎可观测性骨干（StateTransitionRecorder/PayloadStoragePort/ErrorCode/DiagnoseResponse/MDC/TraceIdPropagator）| 3d | pending |
| API-01 | 标准上下文 API（患者/就诊/诊断/医嘱/报告/组织/包版本快照）| 3d | pending |
| API-02 | 临床事件 API（同步/异步/批量/回放/重试/死信/回调）| 3d | pending |
| API-03 | 知识资产 API（来源/解析/引用/版本/审核/替换/历史重放/分页/筛选/异步导出）| 4d | pending |
| API-04 | 字典映射 API（标准/院内字典/候选/冲突/发布）| 3d | pending |
| API-05 | 规则引擎 API（定义/测试/影响分析/发布/执行/解释）| 3d | pending |
| API-06 | 路径引擎 API（模板/专病包/患者路径/节点推进/变异/关键时钟）| 3d | pending |
| API-07 | 推荐/CDSS API（触发/推荐卡/解释/反馈/疲劳治理）| 3d | pending |
| API-08 | 评估质控 API（指标/运行/结果/问题/整改/复核）| 3d | pending |
| API-09 | 随访 API（计划/任务/问卷/异常回院/结果回流）| 3d | pending |
| API-10 | 包发布 API（知识包/配置包/校验/灰度/全量/同步/回滚）| 3d | pending |
| API-11 | 嵌入 API（launch token/iframe/SDK/纯 API/回调/降级）| 3d | pending |
| API-12 | 模型能力网关 API（能力代码/路由/脱敏/结构化输出/审计/B0 降级）| 4d | pending |
| API-13 | 大规模列表 API（统一分页/游标/排序/过滤/total_estimate/异步导出）| 3d | pending |

**小计**：14 项 / 44d

---

## 4. E3 引擎执行（9 项）

| ID | 任务 | 范围 | 工作量 | 状态 |
|---|---|---|---:|---|
| KNOW-01 | 知识资产引擎 | 来源登记/解析/hash/引用锚点/可信分级；图投影（关系库 graph_node/edge/citation 为权威源，Neo4j 仅查询投影可重建，无图降级）| 6d | pending |
| KNOW-02 | 知识版本引擎 | 新旧识别/去重/冲突/待审/原子替换/旧版隔离（详规 §8.13）| 5d | pending |
| TERM-01 | 字典映射引擎 | ICD-10 国临版/ICD-9-CM-3/药品本位码/LOINC 兼容映射 + 医学语义匹配 + **高危近似负样本判别器**（钾/钠、肌钙蛋白T/I、左/右、剂量量级强制 HIGH，禁批量/禁自动确认）| 6d | pending |
| RULE-01 | 规则引擎（后端 + 三层前端） | L1 模板模式（业务专家：模板库+参数表单）+ L2 可视化条件树编辑器（专科/质控专家：AND/OR/NOT+条件原子+时间窗+阈值控件）+ L3 DSL 专家模式 + 7 步流 + 仿真（病例选择器，非 JSON 手编）+ 测试病例（阳/阴/边界/冲突）| 12d | pending |
| PATH-01 | 路径引擎（后端 + 三层前端） | L1 模板模式（专病包库+参数化填空）+ L2 节点画布（X6/G6 拖拽：节点类型/责任角色/时间窗/分支条件/关键时钟可视化）+ L3 DSL 专家模式 + 7 步流 + 仿真 + 关键时钟绑定质控指标 + 随访接续 | 16d | pending |
| CDSS-01 | 推荐/CDSS 引擎 | 规则/路径/知识综合 + 解释追溯 + 医师反馈（采纳/不采纳/原因）+ 疲劳治理 + 真实数据展示（去硬编码循证强度/去假审计字段）| 5d | pending |
| EVAL-01 | 评估质控引擎 | 指标配置/病例命中/问题生成/整改/复核闭环 | 5d | pending |
| FOLLOW-01 | 随访引擎 | 计划生成（受控事实驱动，不写死人群）/任务/问卷/异常事件回流 | 5d | pending |
| PKG-01 | 包发布引擎 | 导入/导出/校验/灰度/全量/同步/回滚/真实证据（无通道时返回 NOT_SYNCED 不伪造）| 6d | pending |

**小计**：9 项 / 66d

---

## 5. E4 嵌入、模型与证据（7 项）

| ID | 任务 | 工作量 | 状态 |
|---|---|---:|---|
| EMBED-01 | iframe/SDK/纯 API 嵌入（launch token 一次性消费/过期/白名单为真，CDS Hooks 风格事件契约）| 5d | pending |
| LLM-01 | 模型能力网关（provider 无关契约 + 路由策略持久化 + 组织继承 + B0 诚实空候选不写死病种）| 5d | pending |
| LLM-02 | B0/B1/B2 策略与验收 + 故障切换矩阵（超时/限流/结构失败/断网→B0）| 4d | pending |
| EVID-01 | 证据链（真实文件 + 国密签名 + 验签）+ 大导出真实文件 URI | 4d | pending |
| INTEG-01 | 第三方对接总线（适配器目录 + FHIR/CDS Hooks 风格门面 + Webhook 签名 + 字段映射 + 健康检查 + 重试死信）| 6d | pending |
| INTEG-02 | 第三方接口文档与契约模板（接入概览 + OpenAPI + 字段映射 + 鉴权 + 幂等 + 回调 + 降级 + 审计）| 3d | pending |
| DEGRADE-01 | 降级链端到端用例（模型/Dify/图/外部 四类逐项关闭后主链路可运行）| 3d | pending |

**小计**：7 项 / 30d

---

## 6. E5 引擎全能力验收（8 项）

| ID | 任务 | 工作量 | 状态 |
|---|---|---:|---|
| QA-01 | 引擎 E2E（真跨引擎全链路：源→标准化→规则/路径→CDSS→评估→随访→包→嵌入→证据）| 4d | pending |
| QA-02 | 5 方言迁移 + 性能 + 并发幂等 + 备份恢复 + 国产化自检 | 3d | pending |
| QA-03 | 医疗安全（AI 标识 + 医师确认 + 禁忌红线 + 旧版隔离 + 高风险审核 + 高危近似禁批量）| 3d | pending |
| QA-04 | 无模型 / 无 Dify / 无图投影验收（B0 主链路真实通过）| 3d | pending |
| QA-05 | 引擎全能力上线评审 | 1d | pending |
| QA-06 | 产品体验验收（10 万级列表 + 低打扰嵌入 + 六态 + 可信解释 + 证据导出 + 驾驶舱下钻）| 3d | pending |
| QA-07 | 代码净化验收（真实性门禁全绿 + 无假闭环 + 无 eslint-disable）| 2d | pending |
| QA-08 | 第三方对接验收（HIS/EMR/LIS/PACS/手麻/输血/医保/公卫/区域平台/Provider 断连重试降级证据）| 4d | pending |

**小计**：8 项 / 23d

---

## 7. E6 业务服务包装（14 项）

| ID | 任务 | 工作量 | 状态 |
|---|---|---:|---|
| SVC-PILOT-01 | 租户与组织服务包（TenantOnboarding + ImplementationGuide + 集团/医院/院区/科室/团队组织树）| 4d | pending |
| SVC-PILOT-02 | 接入与数据质量服务包（HIS/EMR/LIS/PACS/医保/病案/随访适配 + AdapterHub + 字段映射）| 5d | pending |
| SVC-PILOT-03 | 资产准备服务包（知识包/配置包/字典/规则/路径 + ConfigPackages + 灰度全量回滚）| 4d | pending |
| SVC-CLINICAL-01 | 患者与路径运行服务包（MPI + PatientPathways + 关键时钟）| 4d | pending |
| SVC-CLINICAL-02 | 临床提醒与反馈服务包（CDSS 卡片 + 规则校验 + 疲劳治理 + 真实医师署名）| 4d | pending |
| SVC-CLINICAL-03 | 临床协同服务包（待办/通知/护理/报告解读/床旁知识/随访触发）| 4d | pending |
| SVC-QUALITY-01 | 质控驾驶舱服务包（院级指标 + 风险热力 + 价值指标 + 下钻 + 证据）| 4d | pending |
| SVC-QUALITY-02 | 病案医保服务包（病历内涵 + DRG/DIP + 编码 + 费用 + 医保审核）| 5d | pending |
| SVC-QUALITY-03 | 整改闭环服务包（问题 + 责任科室 + 整改 + 复核 + 豁免 + 报告）| 3d | pending |
| SVC-COMPLIANCE-01 | 身份安全服务包（用户 + 身份绑定 + 数据权限 + 租户隔离 + 安全基线）| 5d | pending |
| SVC-COMPLIANCE-02 | 审计运维服务包（审计 + 证据包 + Provider/模型状态 + 备份恢复 + 离线许可）| 4d | pending |
| SVC-INTEGRATION-01 | 第三方业务接口服务包（接入管理 + 字段映射 + 健康检查 + FHIR 门面 + 回调 + 区域协同）| 5d | pending |
| SVC-DOMAIN-01 | 专病路径服务包（集成 CRITICAL/PERIOP/ONCO/SPECIAL-POP/TCM/PRIMARY/INFECTION 等专病分支组合）| 6d | pending |
| SVC-DOMAIN-02 | 专业协同服务包（集成 NURSING/PHARMACY/REPORT/POC-KNOW/ALLIED/RWD/REGION 等专业组合）| 6d | pending |

**小计**：14 项 / 63d

---

## 8. 横切 X-SYS 系统架构强化（落地规划 §7.11，8 项）

| ID | 任务 | 工作量 | 状态 |
|---|---|---:|---|
| SYS-01 | 标准临床模型与事件上下文（12 类标准对象 Patient/Encounter/Condition/Observation/Medication/Procedure/DiagnosticReport/Document/NursingAssessment/CarePlan/FollowUp/Claim）| 5d | pending |
| SYS-02 | 引擎领域边界与服务契约（模块依赖单向 + OpenAPI + 事件契约 + 权限审计要求）| 4d | pending |
| SYS-03 | 关系库权威源与投影同步（图谱/Dify 投影可关闭可重建可审计可降级）| 4d | pending |
| SYS-04 | 版本继承与发布框架（资产不可变 + 组织继承 + 灰度 + 回滚 + 历史重放）| 5d | pending |
| SYS-05 | 在线/异步/批量/离线运行框架（四类运行模式 + 故障重试）| 4d | pending |
| SYS-06 | 安全合规与证据框架（数据权限 + 脱敏 + 审计 + 导出审批 + 证据包）| 5d | pending |
| SYS-07 | 非功能验收基线（性能 P95 + 可用 ≥99.9% + 并发幂等 + 5 方言一致）| 3d | pending |
| SYS-08 | 权威知识版本解析与原子替换框架（唯一有效约束 + 替代链 + 紧急失效 + 影响病例任务）| 5d | pending |

**小计**：8 项 / 35d

---

## 9. 横切 X-AIK AI 医疗知识工厂（详规 §8.11，12 项）

| ID | 任务 | 工作量 | 状态 |
|---|---|---:|---|
| AIK-STD-01 | 来源与全类资产 schema + 统一元数据 | 4d | pending |
| AIK-STD-02 | 文档解析、引用锚点与版本存证（PDF/Word 章节识别 + 表格理解 + hash）| 6d | pending |
| AIK-STD-03 | 术语编码与院内映射流水线 | 4d | pending |
| AIK-STD-04 | 规则/路径/推荐/指标/随访候选生成 | 5d | pending |
| AIK-STD-05 | 安全校验与冲突仲裁（详规 §8.9 11 项门禁）| 5d | pending |
| AIK-STD-06 | 静默运行、反馈和回归评测 | 4d | pending |
| AIK-STD-07 | 知识包/配置包生成与院内同步 | 4d | pending |
| AIK-STD-08 | 最新知识探索、差异检测与过期治理 | 5d | pending |
| AIK-STD-09 | 权威知识替换、旧版失效与影响处置 | 4d | pending |
| AIK-STD-10 | 生成期知识身份识别、去重与审核分流（8 态分流：NEW_TOPIC/NEW_VERSION/SAFETY_WITHDRAWAL/SOURCE_CONFLICT/EXACT_DUPLICATE/OLDER_THAN_ACTIVE/LOCAL_OVERRIDE_IMPACT/UNRESOLVED_IDENTITY）| 5d | pending |
| AIK-STD-11 | 待审新版共存与替换提醒 | 3d | pending |
| AIK-STD-12 | 全医疗专业领域标准资产模板与首批专业资产 + AiReview 前端审核台 | 7d | pending |

**小计**：12 项 / 56d

---

## 10. 横切 X-LLM 模型赋能（详规 §7.12.5，6 项；LLM-01/02 在 E4）

| ID | 任务 | 工作量 | 状态 |
|---|---|---:|---|
| LLM-03 | 数据最小化与外调安全（字段白名单 + 脱敏 + 审批 + 阻断 + 证据）| 4d | pending |
| LLM-04 | 提示词、工具和模型版本治理（输出可重放/版本可回滚/审计可导出）| 3d | pending |
| LLM-05 | 全业务模型增强接入矩阵（详规 §7.12.2 全适用业务有能力码 + B0 卡）| 4d | pending |
| LLM-06 | 可信来源探索编排（受控检索 + 检索时点 + 来源核验）| 4d | pending |
| LLM-07 | 模型安全与医学回归评测（引用真实性 + 红线 + 基准集 + 专家复核）| 5d | pending |
| LLM-08 | provider 真实接入（B1 本地 + B2 外部 + Dify 可选；缺位仍诚实降级 B0）| 6d | pending |

**小计**：6 项 / 26d

---

## 11. 横切 X-MED 医疗严谨性（3 项）

| ID | 任务 | 工作量 | 状态 |
|---|---|---:|---|
| MED-C1 | 字典映射改医学语义匹配（已并入 TERM-01：LCS→同义词典+编码交叉表+模型嵌入 + 高危近似判别器）| 含 TERM-01 | - |
| MED-C2 | 规则 DSL 补临床算子（between/unit_compare 单位换算/temporal 时间窗·连续次数/derived 受控算术 eGFR/CrCl/BSA）| 5d | pending |
| MED-C3 | 安全撤回与旧版下游隔离端到端（召回/禁忌升级紧急停用旧版 + 受影响患者/路径复核任务自动生成）| 4d | pending |

**小计**：2 项独立 / 9d（C1 含于 TERM-01）

---

## 12. 横切 X-OPT 世界级 + 国情补强（落地规划 §22.7，12 项）

| ID | 任务 | 工作量 | 状态 |
|---|---|---:|---|
| OPT-01 | 标准临床模型与 FHIR R4/R5 门面（Patient/Encounter/Condition/Observation/Medication/Procedure/CarePlan/ServiceRequest/DiagnosticReport/DocumentReference）| 6d | pending |
| OPT-02 | CDS Hooks 风格事件契约（6 类触发点：patient-view/order-sign/medication-prescribe/result-review/discharge-sign/followup-alert）| 5d | pending |
| OPT-03 | 医疗器械与 CDSS 风险分级矩阵（NMPA 路径预留）| 4d | pending |
| OPT-04 | 临床安全案例与红线规则库（DDI/危急值/剂量上限/抗菌限制/特殊人群禁忌 + 危害分析 + 静默试运行）| 7d | pending |
| OPT-05 | 互联互通测评映射 | 4d | pending |
| OPT-06 | AI 质量评测中心（字典/规则/路径/推荐/解释/中文术语回归集 + 幻觉拦截）| 5d | pending |
| OPT-07 | 来源证据分级与冲突仲裁（A 法规/B 国家指南/C 共识文献/D 院内/E 反馈 + GRADE 兼容）| 5d | pending |
| OPT-08 | 价值指标与 ROI 看板（采纳率/误报率/漏报回溯/路径完成率/整改闭环率/医保违规减少）| 4d | pending |
| OPT-09 | 数据最小化策略引擎（字段白名单 + 脱敏 + 审批）| 4d | pending |
| OPT-10 | 插件和生态安全边界 | 3d | pending |
| EMR-LEVEL-01 | 电子病历评级目标与项目映射（医院目标等级 4/5/6 级 + 能力差距 + 实施任务）| 5d | pending |
| EMR-LEVEL-02 | 评级数据质量和证据包 | 4d | pending |

**小计**：12 项 / 56d

---

## 13. 横切 X-DOMAIN 全医疗领域门面（宪法 §1.#15 强制 + 落地规划 §18，15 项）

> 每项门面 = 规则资产 + 路径资产 + 知识资产 + CDSS 卡 + 嵌入 + 评估 + 随访 的领域专精组合，**复用同一引擎链路，不另起业务实现**。

**P0 共用临床能力**（3 独立项；通用辅助诊疗/路径/质控合并入引擎 RULE/PATH/EVAL）：

| ID | 任务 | 对应场景 | 工作量 | 状态 |
|---|---|---|---:|---|
| NURSING-01 | 护理专业（护理分级 WS/T 431-2023 + 评估 + 决策 + 计划 + 复评 + 交班 + 质控）| S20、S35 | 8d | pending |
| REPORT-01 | 医技报告解读（检验/影像/病理/内镜/功能；原报告不改写；危急值闭环 + 趋势识别）| S17、S36 | 7d | pending |
| POC-KNOW-01 | 床旁知识查阅（说明书/指南/路径/院内制度的当前权威检索；待审版本不参与）| S37 | 5d | pending |

**P1 高风险与连续照护**（6 项）：

| ID | 任务 | 对应场景 | 工作量 | 状态 |
|---|---|---|---:|---|
| PHARMACY-01 | 药事与药物治疗（药品本位码 + 说明书事实 + DDI/过敏/禁忌/剂量 + 抗菌药物分级 + 处方点评）| S18、S31 | 10d | pending |
| CRITICAL-01 | 急诊重症与生命支持（分诊/恶化预警/危急值闭环/脓毒症/VTE/呼吸支持 + 升级处置）| S19、S24、S27 | 8d | pending |
| SPECIAL-POP-01 | 妇产/儿科/老年/特殊人群（人群标识 + 剂量计算依赖 MED-C2 derived + 禁忌 + 母婴/儿童路径）| S28 | 7d | pending |
| PERIOP-01 | 围术期/麻醉/输血/介入（围术路径 + 安全核查 + 用血/准入/器械规则 + 时序规则）| S26、S33 | 6d | pending |
| ONCO-RENAL-01 | 肿瘤/透析/移植/生殖/日间（周期/方案/监测 + 并发症 + 长期随访）| S29 | 5d | pending |
| ALLIED-CARE-01 | 康复/营养/心理/疼痛/安宁照护（评估/计划/复评/转介/连续照护）| S38 | 5d | pending |

**P1 中国场景与协同**（4 项）：

| ID | 任务 | 对应场景 | 工作量 | 状态 |
|---|---|---|---:|---|
| TCM-HEALTH-01 | 中医药/中西医结合/健康管理（病名/证候/治法/方药/适宜技术 + 独立中医路径 + 中西医结合分支）| S39 | 8d | pending |
| INFECTION-PH-01 | 院感/公卫/预防/职业健康（感染风险 + 报告卡预填 + 上报事件 + 干预闭环）| S21 | 5d | pending |
| PRIMARY-CARE-01 | 基层慢病/双向转诊（分层管理 + 转诊接续 + 复诊 + 连续随访）| S30 | 5d | pending |
| REGION-COLLAB-01 | 医技互认/远程协同（检查检验互认 + 远程协同 + 跨机构来源证据）| S40 | 5d | pending |

**P2 扩展**（2 项）：

| ID | 任务 | 对应场景 | 工作量 | 状态 |
|---|---|---|---:|---|
| SPECIALTY-EXT-01 | 扩展专科（口腔/眼耳鼻喉/皮肤/移植/生殖/职业健康）| S33、S34 | 4d | pending |
| RWD-01 | 科研/真实世界/数据服务（脱敏队列 + 指标数据集 + 伦理授权）| S34 | 4d | pending |

**小计**：15 项 / 92d

---

## 14. 横切 X-KNOWGEN 医疗知识首发资产生产（15 项）

> AI 工厂建好后大规模生产试点医院首发知识包 v1.0。**AI 大规模生成候选 + 专家审核 + 灰度发布**——100d 不是纯人工写。

| ID | 资产域 | 首发覆盖标准 | 工作量 | 状态 |
|---|---|---|---:|---|
| KNOWGEN-01 | 标准术语 | ICD-10 国临版全量 + ICD-9-CM-3 全量 + 药品本位码 Top 3000 + 院内常用检验/检查 Top 500 + LOINC 兼容映射 | 6d | pending |
| KNOWGEN-02 | 药品说明书事实 | 国家批准 Top 1500 药品的结构化（适应症/禁忌/剂量/相互作用/不良反应/特殊人群/警示）+ 原文锚点 hash | 10d | pending |
| KNOWGEN-03 | 国家/学会指南条款 | 国家发布临床路径 30 个 + 国家学会指南 50 个，结构化（推荐级别/证据等级/适用人群/条件/动作）| 10d | pending |
| KNOWGEN-04 | 临床规则 | DDI 高危规则 Top 200 + 危急值规则全量 + 抗菌药物分级规则全量 + 围术期安全核查 + 病案首页质控；每条带测试病例 | 8d | pending |
| KNOWGEN-05 | 专病路径 | 国家临床路径 30 个 + 院内常见专病 20 个 + 分型分支 + 关键时钟 + 节点 + 变异 + 随访接续 | 10d | pending |
| KNOWGEN-06 | CDSS 推荐模板 | 用药/检查/治疗/路径下一步 各 50 个模板 | 6d | pending |
| KNOWGEN-07 | 评估指标 | 国家发布质控指标全量 + DRG/DIP 核心规则 + 病历内涵质控指标 50+ | 5d | pending |
| KNOWGEN-08 | 随访计划 | 30 专病的随访计划模板（时间窗/任务/问卷/异常回院规则）| 5d | pending |
| KNOWGEN-09 | 护理资产 | 护理分级 WS/T 431-2023 全量 + 风险量表 20+ + 护理计划模板 30+ + 交班知识 | 7d | pending |
| KNOWGEN-10 | 医技报告解读 | 检验/影像/病理/内镜/功能 5 类报告解读知识 + 危急值规则 + 趋势规则 | 6d | pending |
| KNOWGEN-11 | 床旁知识卡 | 常见说明书/指南/制度的现行权威条款检索资产 | 4d | pending |
| KNOWGEN-12 | 中医药资产 | 95 个中医优势病种路径 + 适宜技术 + 方药/中成药风险知识 | 8d | pending |
| KNOWGEN-13 | 医保病案资产 | DRG/DIP 全量规则 + 病案首页质控 + ICD 编码规则 | 6d | pending |
| KNOWGEN-14 | 公卫/院感资产 | 法定传染病上报规则 + 感染风险评估 + 不良事件分类 | 4d | pending |
| KNOWGEN-15 | 首发资产总验收 | 14 类资产合并形成"试点医院首发知识包 v1.0"；A1-A9 验收剧本通过；OPT-04 红线规则生效；通过 PKG-01 同步到试点 | 5d | pending |

**小计**：15 项 / 100d

---

## 15. 横切 X-INFRA 工程基础设施（连续核查识别的工程缺口，10 项）

> 这些不在产品规划本身的命名码里，但是规划要求"必须保证"的工程基础（真实性 + 错误处理 + 设计 token + 权限粒度 + 退出登录），必须建立。

| ID | 任务 | 范围 | 工作量 | 状态 |
|---|---|---|---:|---|
| INFRA-01 | 前端真实性门禁 | ESLint medkernel/no-page-mock 阻断 mock/eslint-disable/写死医学常量/font-mono 暴露/JSON 裸渲染 + stylelint 阻断 `.module.css` 含 hex/rgb/hsl | 2d | pending |
| INFRA-02 | 后端真实性门禁 | CI 脚本阻断 Math.random/写死医学常量（"高血压"/"I10"）/catch 吞错返回成功/UUID 充哈希/Javadoc "模拟仿真演示占位" 于生产路径 | 2d | pending |
| INFRA-03 | 错误处理与表单反馈一致性 | 前端 hooks.ts 全部 useMutation 加统一 onError + Form.Item validateStatus 字段级错误回显 + 后端 service 显式抛 ApiException + DataIntegrityViolation 专项 handler + traceId 复制按钮 | 12d | pending |
| INFRA-04 | 退出登录 UI | AppLayout Header 加 Avatar Dropdown（当前用户/修改密码/退出登录）+ 接 useLogout hook + 401 自动跳登录 | 3d | pending |
| INFRA-05 | 27 二级菜单粒度权限模型 | 后端 MenuPermissionCatalog 扩展到二级菜单粒度 + DefaultPermissionPolicy 实现 13 角色 × N 菜单完整矩阵 + 前端 routes.ts 加 requiredPermissions/requiredRoles | 10d | pending |
| INFRA-06 | 全 36 业务页面真实化整改 | 去 eslint-disable / 去 JSON 裸 / 去 font-mono / 去写死医学常量 / 走 7 步流 / 1 主按钮 ≤3 默认筛选 / 中文化 / 六态完整 | 50d | pending |
| INFRA-07 | 全 36 页面运行时可打开性 E2E | 登录某角色 → 点菜单 → 访问页面 → 无 Hook 错误/null reference/API 404 | 5d | pending |
| INFRA-08 | 会话超时与多 tab 同步 | token 过期自动跳登录 + 多 tab storage event 同步 + 长时间无操作自动登出 | 3d | pending |
| INFRA-09 | StepFlowDemo 处理 | 演示页从生产路由移除（保留 Storybook fixture 或删除）| 0.5d | pending |
| INFRA-10 | 自动化体验验收 | A1-A9 剧本 + 14 验收门禁全绿 + 产品体验门禁通过 | 4d | pending |

**小计**：10 项 / 91.5d

---

## 16. 任务总数与工作量

| 区块 | 任务数 | 工作量 |
|---|---:|---:|
| E0 文档清场 | 6 | 5d |
| E1 基础底座 | 11 | 38d |
| E2 引擎接口 | 14 | 44d |
| E3 引擎执行 | 9 | 66d |
| E4 嵌入模型证据 | 7 | 30d |
| E5 引擎全能力验收 | 8 | 23d |
| E6 业务服务包装 | 14 | 63d |
| X-SYS 系统架构 | 8 | 35d |
| X-AIK AI 工厂 | 12 | 56d |
| X-LLM 模型赋能 | 6 | 26d |
| X-MED 医疗严谨 | 2 | 9d |
| X-OPT 世界级 + 国情 | 12 | 56d |
| X-DOMAIN 全医疗领域门面 | 15 | 92d |
| X-KNOWGEN 知识首发资产 | 15 | 100d |
| X-INFRA 工程基础设施 | 10 | 91.5d |
| **合计** | **149** | **~734.5d** |

> 单人粗估。多人并行可压缩到实际工期 ~250-350d（8-12 个月）。
> **核心价值排序**：临床安全（OPT-04 + PHARMACY-01 + CRITICAL-01）> AI 工厂（生产核心 56d）> 系统架构（关系库权威 35d）> 全领域门面（92d 可分批）> 知识首发资产（100d，必须在试点医院能用前完成）。

---

## 17. 验收门禁（全部满足才能宣告 v1.0 GA）

1. **真实性**：T-GATE 前端 + 后端门禁全绿；无 `eslint-disable medkernel/*`；§0.2 铁律 0 违反
2. **菜单/路由/权限**：27 二级菜单 + 5 高级工具粒度生效；13 角色矩阵真实运行
3. **AI 工厂**：无模型可运行组（AIK-STD-01~12）通过；审核台真实可审可发
4. **临床安全**：OPT-04 红线规则库 + PHARMACY-01 + CRITICAL-01 + SPECIAL-POP-01 通过医疗安全用例
5. **跨引擎 E2E**：QA-01 + A1-A9 全功能验收剧本（详规 §16.2）通过
6. **真实模型 + 故障降级**：有 provider / 无 provider / 无 Dify / 无图 / 断网 五组双向通过
7. **第三方真连接器**：断连/重试/降级/证据通过（含 OPT-01 FHIR / OPT-02 CDS Hooks）
8. **15 项领域门面**：全部按宪法 §1.#15 实现卡完成
9. **BASE-11**：生产 profile 部署可经 init token 创建管理员、强制改密+MFA、全程审计
10. **KNOWGEN-15**：14 类首发资产形成"试点医院首发知识包 v1.0"通过 A1-A9 + 同步到试点
11. **第三方对接**：HIS/EMR/LIS/PACS/手麻/输血/医保/公卫/区域平台/Provider 全部断连重试降级证据通过
12. **产品体验**：10 万级列表 + 低打扰 + 六态 + 可信解释 + 证据导出 + 驾驶舱下钻 全部通过
13. **集团化**：平台→集团→医院→院区→社区→科室→专病 多层继承覆盖与冲突仲裁可运行
14. **GA 上线评审**：业务+架构+实施+合规四方签字

---

**End of MedKernel v1.0 GA 任务清单 v7.0**
