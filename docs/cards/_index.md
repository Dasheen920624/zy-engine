# 卡索引（场景 → 卡 + 全卡目录）

> 用途：找卡，不通读。S0–S40 → 拥有其要求的卡；全卡目录按域列出所有卡（= backlog 可交付物）。
> 迁移中：未建的卡在"卡"列写 `待建`；逐域搬迁时回填卡 ID。卡数与菜单数无关（页面卡可共用同一二级菜单槽）。

## 场景 → 卡
| 场景 | 名称 | 拥有要求的卡 |
|---|---|---|
| S0 | 工作台与总览 | [WORKBENCH-01](D1/WORKBENCH-01.md) 工作台页 · [WORKBENCH-02](D1/WORKBENCH-02.md) 演示与校验 · [INFRA-09](D1/INFRA-09.md) 清演示页 |
| S1 | 集团与租户开通 | [BASE-01](D0/BASE-01.md) 组织上下文底座 · [SVC-PILOT-01](D2/SVC-PILOT-01.md) 租户与组织服务包 · [TENANT-01](D2/TENANT-01.md) 租户开通页 · [IMPL-01](D2/IMPL-01.md) 客户实施向导页 |
| S2 | 院内系统接入 | [OPT-01](D2/OPT-01.md) FHIR R4/R5 门面 · [API-01](D2/API-01.md) 标准上下文 API · [INTEG-01](D2/INTEG-01.md) 对接总线 · [INTEG-02](D2/INTEG-02.md) 接口契约模板 · [SVC-PILOT-02](D2/SVC-PILOT-02.md)/[SVC-INTEGRATION-01](D2/SVC-INTEGRATION-01.md) 服务包 · [ADAPTER-01](D2/ADAPTER-01.md) 适配器中心页 |
| S3 | AI 知识工厂 | [KNOW-01](D2/KNOW-01.md) 知识资产引擎 · [KNOW-02](D2/KNOW-02.md) 版本/审核去重 · [API-03](D2/API-03.md) 知识 API · [OPT-07](D2/OPT-07.md) 来源分级 · [SYS-08](D2/SYS-08.md) 权威替换；知识工厂页 + AI 抽取生成（AIK-*/KNOWGEN-*）待建（页 D2 / AI wave2）|
| S4 | 字典映射 | [TERM-01](D2/TERM-01.md) 字典映射引擎（含高危近似判别）· [API-04](D2/API-04.md) 字典 API · [DICTMAP-01](D2/DICTMAP-01.md) 字典映射页 |
| S5 | 规则引擎配置 | [RULE-01](D2/RULE-01.md) 规则引擎（三层+仿真，含规则库页）· [MED-C2](D2/MED-C2.md) 临床 DSL 算子 · [API-05](D2/API-05.md) 规则 API |
| S6 | 路径引擎配置 | [PATH-01](D2/PATH-01.md) 路径引擎（三层+关键时钟+随访接续，含路径配置页）· [API-06](D2/API-06.md) 路径 API |
| S7 | 图谱与来源追溯 | 待建 |
| S8 | 临床嵌入运行 | [API-02](D3/API-02.md) 临床事件 · [API-07](D3/API-07.md) 推荐/CDSS · [API-11](D3/API-11.md) 嵌入 · [CDSS-01](D3/CDSS-01.md) CDSS 引擎 · [EMBED-01](D3/EMBED-01.md) 嵌入引擎 · [OPT-02](D3/OPT-02.md) CDS Hooks · [OPT-04](D3/OPT-04.md) 红线 · [SVC-CLINICAL-01](D3/SVC-CLINICAL-01.md)/[02](D3/SVC-CLINICAL-02.md)/[03](D3/SVC-CLINICAL-03.md) 服务包 · 页 [PMI-01](D3/PMI-01.md)/[PPATH-01](D3/PPATH-01.md)/[REMIND-01](D3/REMIND-01.md)/[RULECHK-01](D3/RULECHK-01.md)/[TODO-01](D3/TODO-01.md)/[NOTIFY-01](D3/NOTIFY-01.md)（消费 [API-01](D2/API-01.md) 上下文底座）|
| S9 | 病历内涵质控 | [EVAL-01](D4/EVAL-01.md) 评估引擎 · [SVC-QUALITY-02](D4/SVC-QUALITY-02.md) 病案医保 · 页 [EVALRES-01](D4/EVALRES-01.md) 评估结果 |
| S10 | 医保与病案质控 | [SVC-QUALITY-02](D4/SVC-QUALITY-02.md) 病案医保（DRG/DIP/编码/费用）· 页 [INSAUDIT-01](D4/INSAUDIT-01.md) 医保智能审核 |
| S11 | 智能评估与整改 | [EVAL-01](D4/EVAL-01.md) 评估引擎 · [SVC-QUALITY-01](D4/SVC-QUALITY-01.md) 驾驶舱 · [SVC-QUALITY-03](D4/SVC-QUALITY-03.md) 整改闭环 · [OPT-08](D4/OPT-08.md) 价值/ROI · 页 [QCDASH-01](D4/QCDASH-01.md)/[QCALERT-01](D4/QCALERT-01.md)/[EVALSET-01](D4/EVALSET-01.md)/[EVALRES-01](D4/EVALRES-01.md) |
| S12 | 智能随访 | [API-09](D3/API-09.md) 随访 API · [FOLLOW-01](D3/FOLLOW-01.md) 随访引擎 · 页 [FUP-01](D3/FUP-01.md) 智能随访 |
| S13 | 包发布与院内同步 | [PKG-01](D2/PKG-01.md) 包发布引擎 · [API-10](D2/API-10.md) 包发布 API · [SYS-04](D2/SYS-04.md) 版本发布框架 · [SYS-08](D2/SYS-08.md) 权威知识替换 · [SVC-PILOT-03](D2/SVC-PILOT-03.md) 资产准备服务包 · [CFGPKG-01](D2/CFGPKG-01.md) 配置包中心页 |
| S14 | 用户、权限与合规 | [AUTH-01](D0/AUTH-01.md)/[AUTH-02](D0/AUTH-02.md)/[AUTH-03](D0/AUTH-03.md) 双模登录认证 · [BASE-02](D0/BASE-02.md)/[INFRA-05](D0/INFRA-05.md) 权限 · [SUPERADMIN-01](D0/SUPERADMIN-01.md) 超管 · [CONFIG-01](D0/CONFIG-01.md) 配置 · [BASE-04](D0/BASE-04.md) 审计 · [INFRA-04](D0/INFRA-04.md)/[INFRA-08](D0/INFRA-08.md) 会话；用户管理/审计日志页待建（D5）|
| S15 | AI 验证与验收 | [AIREVIEW-01](D4/AIREVIEW-01.md) AI 知识审核台（人工审/发，AI 生成随 wave2）；S0–S40 全验收/E2E 证据随 GA |
| S16 | 辅助诊疗与鉴别诊断 | [CDSS-01](D3/CDSS-01.md) 推荐/CDSS 引擎（B0 确定性辅助诊疗/候选）；深度鉴别诊断生成随 wave2 |
| S17 | 检查检验推荐 | 待建 |
| S18 | 用药安全与治疗方案 | 待建 |
| S19 | 急危重症与预警 | 待建 |
| S20 | 护理康复与宣教 | 待建 |
| S21 | 院感与公共卫生 | 待建 |
| S22 | MDT 与专科中心协同 | 待建 |
| S23 | 电子病历评级支撑 | [EMR-LEVEL-01](D4/EMR-LEVEL-01.md) 评级目标映射 · [EMR-LEVEL-02](D4/EMR-LEVEL-02.md) 评级数据质量与证据包 |
| S24 | 门急诊全过程支持 | 待建 |
| S25 | 住院诊疗与核心制度 | 待建 |
| S26 | 围手术期、麻醉与输血 | 待建 |
| S27 | 重症与生命支持 | 待建 |
| S28 | 妇产、儿科与特殊人群 | 待建 |
| S29 | 肿瘤与日间诊疗 | 待建 |
| S30 | 慢病、基层与双向转诊 | 待建 |
| S31 | 药事治理与抗菌药物 | 待建 |
| S32 | 医疗安全事件管理 | 待建 |
| S33 | 器械耗材与医疗技术 | 待建 |
| S34 | 科研、真实世界与数据服务 | 待建 |
| S35 | 护理专业智能与护理决策 | 待建 |
| S36 | 医技报告解读与结果闭环 | 待建 |
| S37 | 床旁知识查阅与证据问答 | 待建 |
| S38 | 营养、心理、疼痛与安宁照护 | 待建 |
| S39 | 中医药、预防保健与健康管理 | 待建 |
| S40 | 医技互认、远程协同与区域共享 | 待建 |

## 全卡目录（按域）

> 卡 ID = backlog 可交付物 ID。逐域搬迁时本目录回填链接（`D{n}/<ID>.md`）。`待建`。

### D0 登录域/平台脊柱 ✅ 已建（[域简报](D0/_brief.md)）
[BASE-01](D0/BASE-01.md) 组织与租户上下文 · [BASE-02](D0/BASE-02.md) 身份权限五维 · [BASE-03](D0/BASE-03.md) 标准 API 契约 · [BASE-04](D0/BASE-04.md) 审计骨干 · [BASE-05](D0/BASE-05.md) 5 方言迁移骨架 · [BASE-06](D0/BASE-06.md) 5+1 菜单与前端骨架 · [BASE-07](D0/BASE-07.md) 运行底座 · [BASE-08](D0/BASE-08.md) 产品体验底座 · [BASE-09](D0/BASE-09.md) 代码基线净化 · [BASE-10](D0/BASE-10.md) 设计 Token 系统 · [BASE-11](D0/BASE-11.md) 平台首发种子身份 · [OBS-01](D0/OBS-01.md) 引擎可观测性骨干 · [API-13](D0/API-13.md) 大规模列表 API · [SYS-01](D0/SYS-01.md) 标准临床模型与事件上下文 · [SYS-02](D0/SYS-02.md) 引擎领域边界与服务契约 · [SYS-03](D0/SYS-03.md) 关系库权威源与投影同步 · [SYS-05](D0/SYS-05.md) 在线/异步/批量/离线运行框架 · [INFRA-01](D0/INFRA-01.md) 前端真实性门禁 · [INFRA-02](D0/INFRA-02.md) 后端真实性门禁 · [INFRA-03](D0/INFRA-03.md) 错误处理与表单反馈一致性 · [INFRA-04](D0/INFRA-04.md) 退出登录 UI · [INFRA-05](D0/INFRA-05.md) 27 二级菜单粒度权限模型 · [INFRA-08](D0/INFRA-08.md) 会话超时与多 tab 同步 · [SUPERADMIN-01](D0/SUPERADMIN-01.md) 内置超级管理员 · [CONFIG-01](D0/CONFIG-01.md) 配置中心引擎 · **[AUTH-01](D0/AUTH-01.md) 双模身份与登录闭环** · **[AUTH-02](D0/AUTH-02.md) 登录页** · **[AUTH-03](D0/AUTH-03.md) 凭证自助与口令安全**

### D1 工作台 ✅ 已建（[域简报](D1/_brief.md)）
[INFRA-09](D1/INFRA-09.md) StepFlowDemo 清出生产路由 · [WORKBENCH-01](D1/WORKBENCH-01.md) 工作台页面（原 `D1-PAGE-工作台`）· [WORKBENCH-02](D1/WORKBENCH-02.md) 演示与校验页面（原 `D1-PAGE-演示校验`）

### D2 试点准备 ✅ 已建（30 卡：23 ID + 7 页面，[域简报](D2/_brief.md)）
**B1 框架**：[API-01](D2/API-01.md) 标准上下文 API · [OPT-01](D2/OPT-01.md) FHIR R4/R5 门面 · [SYS-04](D2/SYS-04.md) 版本继承与发布框架 · [SYS-08](D2/SYS-08.md) 权威知识版本解析与原子替换框架
**B2 知识+字典**：[KNOW-01](D2/KNOW-01.md) 知识资产引擎 · [KNOW-02](D2/KNOW-02.md) 版本/审核去重 · [TERM-01](D2/TERM-01.md) 字典映射引擎 · [API-03](D2/API-03.md) 知识 API · [API-04](D2/API-04.md) 字典 API
**B3 规则+路径**：[RULE-01](D2/RULE-01.md) 规则引擎（含规则库页）· [PATH-01](D2/PATH-01.md) 路径引擎（含路径配置页）· [MED-C2](D2/MED-C2.md) 临床 DSL 算子 · [API-05](D2/API-05.md) 规则 API · [API-06](D2/API-06.md) 路径 API
**B4 包发布+集成**：[PKG-01](D2/PKG-01.md) 包发布引擎 · [API-10](D2/API-10.md) 包发布 API · [INTEG-01](D2/INTEG-01.md) 对接总线 · [INTEG-02](D2/INTEG-02.md) 接口契约模板 · [OPT-07](D2/OPT-07.md) 来源证据分级与冲突仲裁
**B5 服务包**：[SVC-PILOT-01](D2/SVC-PILOT-01.md) 租户与组织 · [SVC-PILOT-02](D2/SVC-PILOT-02.md) 接入与数据质量 · [SVC-PILOT-03](D2/SVC-PILOT-03.md) 资产准备 · [SVC-INTEGRATION-01](D2/SVC-INTEGRATION-01.md) 第三方业务接口
**B6 页面**（实化占位 `D2-PAGE-*`）：[IMPL-01](D2/IMPL-01.md) 客户实施向导 · [TENANT-01](D2/TENANT-01.md) 租户开通 · [CFGPKG-01](D2/CFGPKG-01.md) 配置包中心 · [DICTMAP-01](D2/DICTMAP-01.md) 字典映射 · [ADAPTER-01](D2/ADAPTER-01.md) 适配器中心 · 规则库=[RULE-01](D2/RULE-01.md) · 路径配置=[PATH-01](D2/PATH-01.md)

### D3 临床运行 ✅ 已建（21 卡：14 ID + 7 页面，[域简报](D3/_brief.md)）
**API 契约**：[API-02](D3/API-02.md) 临床事件 · [API-07](D3/API-07.md) 推荐/CDSS · [API-09](D3/API-09.md) 随访 · [API-11](D3/API-11.md) 嵌入
**引擎**：[CDSS-01](D3/CDSS-01.md) 推荐/CDSS 引擎 · [FOLLOW-01](D3/FOLLOW-01.md) 随访引擎 · [EMBED-01](D3/EMBED-01.md) 嵌入引擎
**临床安全**：[OPT-02](D3/OPT-02.md) CDS Hooks 契约 · [OPT-03](D3/OPT-03.md) 风险分级矩阵 · [OPT-04](D3/OPT-04.md) 红线规则库 · [MED-C3](D3/MED-C3.md) 安全撤回与旧版隔离
**服务包**：[SVC-CLINICAL-01](D3/SVC-CLINICAL-01.md) 患者与路径运行 · [SVC-CLINICAL-02](D3/SVC-CLINICAL-02.md) 提醒与反馈 · [SVC-CLINICAL-03](D3/SVC-CLINICAL-03.md) 临床协同
**页面**（实化占位 `D3-PAGE-*`）：[PMI-01](D3/PMI-01.md) 患者主索引 · [PPATH-01](D3/PPATH-01.md) 患者路径 · [REMIND-01](D3/REMIND-01.md) 临床提醒治理 · [RULECHK-01](D3/RULECHK-01.md) 规则校验 · [TODO-01](D3/TODO-01.md) 待办中心 · [NOTIFY-01](D3/NOTIFY-01.md) 通知中心 · [FUP-01](D3/FUP-01.md) 智能随访

### D4 质控改进 ✅ 已建（14 卡：8 ID + 6 页面，[域简报](D4/_brief.md)）
**评估/整改**：[API-08](D4/API-08.md) 评估质控 API · [EVAL-01](D4/EVAL-01.md) 评估引擎 · [SVC-QUALITY-03](D4/SVC-QUALITY-03.md) 整改闭环
**价值/评级**：[OPT-08](D4/OPT-08.md) 价值与 ROI 看板 · [EMR-LEVEL-01](D4/EMR-LEVEL-01.md) 评级目标映射 · [EMR-LEVEL-02](D4/EMR-LEVEL-02.md) 评级数据质量与证据包
**服务包**：[SVC-QUALITY-01](D4/SVC-QUALITY-01.md) 质控驾驶舱 · [SVC-QUALITY-02](D4/SVC-QUALITY-02.md) 病案医保
**页面**（实化占位 `D4-PAGE-*`）：[QCDASH-01](D4/QCDASH-01.md) 院级质控驾驶舱 · [QCALERT-01](D4/QCALERT-01.md) 质控预警 · [INSAUDIT-01](D4/INSAUDIT-01.md) 医保智能审核 · [EVALSET-01](D4/EVALSET-01.md) 评估指标库 · [EVALRES-01](D4/EVALRES-01.md) 评估结果 · [AIREVIEW-01](D4/AIREVIEW-01.md) AI 知识审核

### D5 合规运维
EVID-01 / SYS-06 / OPT-05 / SVC-COMPLIANCE-01 / SVC-COMPLIANCE-02 / 页面：用户管理 · 身份绑定 · 审计日志 · 安全基线与系统配置 · Provider 状态 · 通知设置（待建，系统配置中心页挂"安全基线与系统配置"槽，二级菜单仍 27）

### D6 高级工具
OPT-10 / 页面：来源追溯 · 图谱查询 · AI 工作流 · 国产化自检 · 开发者控制台（待建）

### 第二波 wave2
API-12 / LLM-01 … LLM-08 / OPT-06 / OPT-09 / AIK-STD-01 … AIK-STD-12 / KNOWGEN-01 … KNOWGEN-15 / NURSING-01 / REPORT-01 / POC-KNOW-01 / PHARMACY-01 / CRITICAL-01 / SPECIAL-POP-01 / PERIOP-01 / ONCO-RENAL-01 / ALLIED-CARE-01 / TCM-HEALTH-01 / INFECTION-PH-01 / PRIMARY-CARE-01 / REGION-COLLAB-01 / SPECIALTY-EXT-01 / RWD-01 / SVC-DOMAIN-01 / SVC-DOMAIN-02（待建）

### GA 验收 ga
QA-01 … QA-08 / DEGRADE-01 / SYS-07 / INFRA-07 / INFRA-10（待建）
