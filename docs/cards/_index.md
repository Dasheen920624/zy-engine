# 卡索引（场景 → 卡 + 全卡目录）

> 用途：找卡，不通读。S0–S40 → 拥有其要求的卡；全卡目录按域列出所有卡（= backlog 可交付物）。
> 迁移中：未建的卡在"卡"列写 `待建`；逐域搬迁时回填卡 ID。卡数与菜单数无关（页面卡可共用同一二级菜单槽）。

## 场景 → 卡
| 场景 | 名称 | 拥有要求的卡 |
|---|---|---|
| S0 | 工作台与总览 | 待建 |
| S1 | 集团与租户开通 | 待建 |
| S2 | 院内系统接入 | 待建 |
| S3 | AI 知识工厂 | 待建 |
| S4 | 字典映射 | 待建 |
| S5 | 规则引擎配置 | 待建 |
| S6 | 路径引擎配置 | 待建 |
| S7 | 图谱与来源追溯 | 待建 |
| S8 | 临床嵌入运行 | 待建 |
| S9 | 病历内涵质控 | 待建 |
| S10 | 医保与病案质控 | 待建 |
| S11 | 智能评估与整改 | 待建 |
| S12 | 智能随访 | 待建 |
| S13 | 包发布与院内同步 | 待建 |
| S14 | 用户、权限与合规 | 待建 |
| S15 | AI 验证与验收 | 待建 |
| S16 | 辅助诊疗与鉴别诊断 | 待建 |
| S17 | 检查检验推荐 | 待建 |
| S18 | 用药安全与治疗方案 | 待建 |
| S19 | 急危重症与预警 | 待建 |
| S20 | 护理康复与宣教 | 待建 |
| S21 | 院感与公共卫生 | 待建 |
| S22 | MDT 与专科中心协同 | 待建 |
| S23 | 电子病历评级支撑 | 待建 |
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

### D0 登录域/平台脊柱
BASE-01 / BASE-02 / BASE-03 / BASE-04 / BASE-05 / BASE-06 / BASE-07 / BASE-08 / BASE-09 / BASE-10 / BASE-11 / OBS-01 / API-13 / SYS-01 / SYS-02 / SYS-03 / SYS-05 / INFRA-01 / INFRA-02 / INFRA-03 / INFRA-04 / INFRA-05 / INFRA-08 / **SUPERADMIN-01** / **CONFIG-01**（待建）

### D1 工作台
INFRA-09 / 页面：工作台 · 演示校验（待建）

### D2 试点准备
API-01 / API-03 / API-04 / API-05 / API-06 / API-10 / KNOW-01 / KNOW-02 / TERM-01 / RULE-01 / PATH-01 / PKG-01 / SYS-04 / SYS-08 / INTEG-01 / INTEG-02 / MED-C2 / OPT-01 / OPT-07 / SVC-PILOT-01 / SVC-PILOT-02 / SVC-PILOT-03 / SVC-INTEGRATION-01 / 页面：客户实施向导 · 租户开通 · 配置包中心 · 路径配置 · 规则库 · 字典映射 · 适配器中心（待建）

### D3 临床运行
API-02 / API-07 / API-09 / API-11 / CDSS-01 / FOLLOW-01 / EMBED-01 / MED-C3 / OPT-02 / OPT-03 / OPT-04 / SVC-CLINICAL-01 / SVC-CLINICAL-02 / SVC-CLINICAL-03 / 页面：患者主索引 · 患者路径 · 临床提醒治理 · 规则校验 · 待办中心 · 通知中心 · 智能随访（待建）

### D4 质控改进
API-08 / EVAL-01 / OPT-08 / EMR-LEVEL-01 / EMR-LEVEL-02 / SVC-QUALITY-01 / SVC-QUALITY-02 / SVC-QUALITY-03 / 页面：院级质控驾驶舱 · 质控预警 · 医保智能审核 · 评估指标库 · 评估结果 · AI 知识审核（待建）

### D5 合规运维
EVID-01 / SYS-06 / OPT-05 / SVC-COMPLIANCE-01 / SVC-COMPLIANCE-02 / 页面：用户管理 · 身份绑定 · 审计日志 · 安全基线与系统配置 · Provider 状态 · 通知设置（待建，系统配置中心页挂"安全基线与系统配置"槽，二级菜单仍 27）

### D6 高级工具
OPT-10 / 页面：来源追溯 · 图谱查询 · AI 工作流 · 国产化自检 · 开发者控制台（待建）

### 第二波 wave2
API-12 / LLM-01 … LLM-08 / OPT-06 / OPT-09 / AIK-STD-01 … AIK-STD-12 / KNOWGEN-01 … KNOWGEN-15 / NURSING-01 / REPORT-01 / POC-KNOW-01 / PHARMACY-01 / CRITICAL-01 / SPECIAL-POP-01 / PERIOP-01 / ONCO-RENAL-01 / ALLIED-CARE-01 / TCM-HEALTH-01 / INFECTION-PH-01 / PRIMARY-CARE-01 / REGION-COLLAB-01 / SPECIALTY-EXT-01 / RWD-01 / SVC-DOMAIN-01 / SVC-DOMAIN-02（待建）

### GA 验收 ga
QA-01 … QA-08 / DEGRADE-01 / SYS-07 / INFRA-07 / INFRA-10（待建）
