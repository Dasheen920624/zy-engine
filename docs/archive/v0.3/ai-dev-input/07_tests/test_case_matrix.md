# 测试矩阵：专科诊疗三大引擎P0验收

## 1. 路径引擎

| 用例ID | 场景 | 输入 | 期望 |
|---|---|---|---|
| PE-TC-001 | 创建AMI路径草稿 | `sample_ami_pathway.json` | 返回成功，路径状态为DRAFT |
| PE-TC-002 | 发布AMI路径 | pathway_code + version_no | 状态变为PUBLISHED |
| PE-TC-003 | 候选路径识别 | `sample_patient_context_ami.json` | 返回AMI_STEMI候选，分数>=85 |
| PE-TC-004 | 医生确认入径 | patient_id + encounter_id + pathway_code | 创建患者路径实例，当前节点为AMI_CHEST_PAIN_IDENTIFY |
| PE-TC-005 | 重复入径幂等 | 重复调用admit接口 | 不创建重复ACTIVE实例 |
| PE-TC-006 | 完成节点 | instance_id + node_code | 流转到下一节点 |
| PE-TC-007 | 记录变异 | variation_type + reason | 生成变异记录与审计日志 |

## 2. 规则引擎

| 用例ID | 场景 | 输入 | 期望 |
|---|---|---|---|
| RE-TC-001 | STEMI候选规则命中 | AMI患者上下文 | R_AMI_STEMI_CANDIDATE命中 |
| RE-TC-002 | 心电图时限合格 | 7分钟完成ECG | R_AMI_ECG_TIMELY不命中 |
| RE-TC-003 | 心电图超时 | 15分钟完成ECG | R_AMI_ECG_TIMELY命中 |
| RE-TC-004 | 缺少数据 | 无arrival_time | 返回DATA_MISSING，不抛系统异常 |
| RE-TC-005 | 规则模拟 | 规则DSL + 患者上下文 | 返回命中原因 |

## 3. 图谱引擎

| 用例ID | 场景 | 输入 | 期望 |
|---|---|---|---|
| GE-TC-001 | 候选疾病召回 | 胸痛、ST段抬高、糖尿病 | 返回AMI_STEMI |
| GE-TC-002 | 证据查询 | AMI_REPERFUSION_EVAL | 返回EV_AMI_001 |
| GE-TC-003 | 路径关系查询 | AMI_STEMI | 返回路径节点 |
| GE-TC-004 | 图谱版本 | KG_2026_05 | 只返回指定版本数据 |

## 4. 集成验收

| 用例ID | 场景 | 步骤 | 期望 |
|---|---|---|---|
| INT-TC-001 | AMI闭环 | 事件->规则->图谱->路径候选->医生入径 | 形成患者路径实例 |
| INT-TC-002 | Dify降级 | Dify超时 | 路径核心状态不受影响，返回降级说明 |
| INT-TC-003 | 数据库兼容 | Oracle/达梦分别初始化DDL | 表创建成功，核心接口可用 |
| INT-TC-004 | 审计追踪 | 执行一次候选识别 | 产生trace_id和审计日志 |

## 5. 病历质控（EMR_QC）

| 用例ID | 场景 | 输入 | 期望 |
|---|---|---|---|
| EMR-QC-001 | 病历缺项与时限混合命中 | `sample_emr_qc_case.json` 经 `POST /api/rule-engine/evaluate` (scenario_code=EMR_QC) | 命中数=4，severity 分布 HIGH=2/MEDIUM=2，每条携带 rule_version/rule_package_version/reference_*/trace_id |
| EMR-QC-002 | 缺医学来源规则不可发布 | 把 `PKG_EMR_QC` 中规则 reference_title 字段置空后调 publish | 返回 `MISSING_SOURCE`，审计中无 publish 事件，状态保持 REVIEWED |
| EMR-QC-003 | 同患者不同医院范围独立评估 | 同样患者上下文，分别传 `X-Hospital-Code=HOSPITAL_DEMO` 与 `HOSPITAL_BETA` | 评估记录 hospital_code 字段正确写入；`GET /api/rule-engine/results?hospitalCode=HOSPITAL_DEMO` 只返回演示医院的结果 |
| EMR-QC-004 | dry-run 不写正式状态 | 演示工作台运行 EMR-QC-001 | 患者路径实例表未产生新行；评估结果写入内存环形缓冲 |
| EMR-QC-005 | 组织上下文 Body 优先级 | Header=HOSPITAL_DEMO、Body=HOSPITAL_GAMMA | 评估记录 `org_source=BODY`，hospital_code=HOSPITAL_GAMMA |

## 6. 医保质控（INSURANCE_QC）

| 用例ID | 场景 | 输入 | 期望 |
|---|---|---|---|
| INS-QC-001 | 限定药品/检查频次/高值耗材组合风险 | `sample_insurance_qc_case.json` | 命中数=3，total_amount_impact_yuan=22780.00，每条携带 policy_reference_section/policy_version |
| INS-QC-002 | 政策来源版本展示 | 调用 `GET /api/rule-engine/results/{resultId}` | 返回结果含 policy_reference_title 与 policy_version；与发布时规则版本一致 |
| INS-QC-003 | 申诉材料生成 | 命中后调用申诉材料导出（FE-003 落地）| 返回 PDF/HTML 含规则命中清单、政策原文、患者事实快照、traceId |
| INS-QC-004 | DRG 关联 | 输入带 drg_code=FM39 | 评估结果包含 drg_code 字段，便于按 DRG 分组复盘 |
| INS-QC-005 | 缺政策来源不可发布 | 把 policy_reference_section 置空 publish | 返回 `MISSING_SOURCE`，审计中无 publish 事件 |
| INS-QC-006 | 跨实例聚合 | 多组织同时评估，调用 `GET /api/rule-engine/results?scenarioCode=INSURANCE_QC` | 返回多医院结果摘要，可按 hospital_code 过滤 |

## 7. 医嘱安全（ORDER_SAFETY）

| 用例ID | 场景 | 输入 | 期望 |
|---|---|---|---|
| ORD-SF-001 | 4 级分级混合命中 | `sample_order_safety_case.json` | 命中数=4，action_mode 含 BLOCK=1/STRONG_REMIND=2/REMIND=1 |
| ORD-SF-002 | BLOCK 类需上级授权 | 对 R_ORD_ASTHMA_NONSELECTIVE_BETA_BLOCKER 触发"忽略" | 返回 `OVERRIDE_REQUIRES_AUTHORIZATION`，覆盖必须带 senior_doctor_id 与 reason |
| ORD-SF-003 | 标准化先于规则 | 输入院内文本"奥美拉唑肠溶胶囊"作为名称 | 通过 TERM-001 归一到 standard_code=`OMEPRAZOLE` 或 class_codes 含 `PPI`，再被规则命中 |
| ORD-SF-004 | 肾功能调整缺失提醒 | CrCl=38 mL/min + 万古霉素未调整 | 命中 R_ORD_RENAL_DOSE_UNADJUSTED 且 severity=HIGH |
| ORD-SF-005 | 时间窗与重复检测 | 24h 内胸部 CT 3 次 | 命中 R_ORD_REPEAT_EXAM_OVER_LIMIT 且 evidence 含 24h 时间窗计数 |
| ORD-SF-006 | 提醒疲劳防护 | 同一患者同一规则 24h 内多次重复触发 | 提供折叠/合并机制（FE-003 落地后），后端仍记录但不重复审计 |
| ORD-SF-007 | 来源完整性 | 校验 R_ORD_ASTHMA_NONSELECTIVE_BETA_BLOCKER 来源字段 | 必须含 reference_title + reference_section + evidence_level + recommendation_grade + reviewed_by |

## 8. 通用回归矩阵

| 维度 | 必覆盖取值 |
|---|---|
| 组织范围 | 默认（default/ZYHOSPITAL）/ 集团 / 医院 / 院区 / 站点 / 科室 |
| 来源状态 | 缺来源 / 未审核 / 已审核 / 过期 / 被替换 |
| 发布状态 | DRAFT / REVIEWED / PUBLISHED / SYNCED / ACTIVE / RETIRED |
| Provider | DB-only / Neo4j 可用 / Neo4j 不可用 / Dify 可用 / Dify 不可用 / 适配器超时 |
| 数据库 | 内存/JUnit / Oracle / 达梦 |
| 安全 | 匿名 / 低权限 / 越权组织 / 错误签名 / 重复 nonce |
| 输入异常 | 缺必填 / 类型错误 / 超长字符串 / 未知 scenario_code |
| 审计 | 命中即审计 / 失败也审计 / 全链路 traceId 串联 |

