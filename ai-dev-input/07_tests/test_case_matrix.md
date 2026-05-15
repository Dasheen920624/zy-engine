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

