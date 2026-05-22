# 路径治理模块用户手册

> 适用对象：医院信息科运维人员、质控管理人员、临床路径管理员
> 版本：2026.05
> 模块代号：Pathway Governance

---

## 1. 模块概述

### 1.1 什么是临床路径治理模块

临床路径治理模块是 MedKernel 临床决策支持系统（CDSS）的重要组成部分，负责管理、执行和监控临床路径的全生命周期。临床路径是基于循证医学证据和临床指南，针对特定疾病或手术制定的标准化诊疗流程，通过规范诊疗行为、减少不必要变异来提升医疗质量和控制医疗费用。

本模块提供从路径设计、发布、患者入径到执行跟踪、统计分析的全流程管理能力。

### 1.2 核心价值

| 价值维度 | 说明 |
|---------|------|
| 规范诊疗 | 通过标准化流程减少不合理的诊疗差异 |
| 质量提升 | 实时监控路径执行，及时发现和处理变异 |
| 效率优化 | 明确各阶段任务和时限，缩短平均住院日 |
| 数据驱动 | 通过路径统计数据持续改进诊疗方案 |
| 合规保障 | 路径执行记录为医保支付和质控评审提供依据 |

### 1.3 路径生命周期

一条临床路径从创建到退役，经历以下生命周期阶段：

```
草稿(DRAFT) → 已发布(PUBLISHED) → 已废弃(RETIRED)
```

---

## 2. 功能清单

| 功能 | 说明 | 对应接口 |
|------|------|---------|
| 路径列表查询 | 按科室、状态、关键词筛选路径 | `GET /api/pathways` |
| 路径详情查看 | 查看路径完整定义，支持历史版本 | `GET /api/pathways/{code}` |
| 创建路径 | 创建新的临床路径 | `POST /api/pathways` |
| 删除路径 | 删除指定路径（仅草稿状态可删） | `DELETE /api/pathways/{code}` |
| 保存草稿 | 保存路径编辑器中的节点和连线 | `PUT /api/pathways/{code}/draft` |
| 校验路径 | 校验路径配置的完整性和正确性 | `POST /api/pathways/{code}/validate` |
| 提交审核 | 将路径草稿提交审核 | `POST /api/pathways/{code}/submit-review` |
| 发布路径 | 审核通过后发布路径 | `POST /api/pathways/{code}/publish` |
| 回滚路径 | 回滚到上一发布版本 | `POST /api/pathways/{code}/rollback` |
| 版本对比 | 对比两个版本的差异 | `GET /api/pathways/{code}/diff` |
| 候选推荐 | 基于患者信息推荐适合的路径 | `POST /api/patient-pathways/candidates` |
| 患者入径 | 将患者纳入指定路径 | `POST /api/patient-pathways/admit` |
| 实例查询 | 查询患者路径实例列表 | `GET /api/pathway-instances` |
| 实例详情 | 查看路径实例详情 | `GET /api/patient-pathways/{instanceId}` |
| 节点状态 | 查看节点及其任务状态 | `GET /api/patient-pathways/{instanceId}/nodes/{nodeCode}` |
| 完成任务 | 标记任务为已完成 | `POST /api/patient-pathways/{instanceId}/nodes/{nodeCode}/tasks/{taskCode}/complete` |
| 跳过任务 | 标记任务为已跳过 | `POST /api/patient-pathways/{instanceId}/nodes/{nodeCode}/tasks/{taskCode}/skip` |
| 完成节点 | 标记节点为已完成 | `POST /api/patient-pathways/{instanceId}/nodes/{nodeCode}/complete` |
| 记录变异 | 记录路径执行中的变异 | `POST /api/patient-pathways/{instanceId}/variations` |
| 变异记录查询 | 查询变异记录列表 | `GET /api/pathway-variations` |
| 变异统计 | 变异记录聚合统计 | `GET /api/pathway-variations/summary` |
| 实例汇总 | 路径实例聚合统计 | `GET /api/pathway-instances/summary` |
| 节点完成度 | 各节点完成率统计 | `GET /api/pathway-instances/node-completion` |
| 节点停留时长 | 各节点平均停留时长 | `GET /api/pathway-instances/node-stay-duration` |

---

## 3. 操作指南

### 3.1 创建路径

#### 操作步骤

1. 进入 **路径治理** 模块，点击页面右上角 **「新建路径」** 按钮
2. 在弹出的路径创建表单中填写基本信息
3. 填写完成后点击 **「创建」**，路径状态为 `DRAFT`

#### 基本信息字段

| 字段 | 是否必填 | 说明 | 示例 |
|------|---------|------|------|
| 路径编码（pathway_code） | 必填 | 路径唯一标识，全局不可重复 | `AMI_STEMI` |
| 路径名称（pathway_name） | 必填 | 路径的中文描述名称 | `急性ST段抬高型心肌梗死诊疗路径` |
| 版本号（version） | 必填 | 语义化版本号 | `1.0.0` |
| 适用科室（specialty_code） | 必填 | 路径适用的临床科室编码 | `CARDIOLOGY` |
| 适用疾病（disease_code） | 选填 | 路径适用的疾病编码 | `AMI_STEMI` |
| 路径描述（description） | 选填 | 路径的详细说明 | 用于急诊胸痛患者的STEMI识别... |

#### 入径策略配置

入径策略（entry_policy）定义了患者如何进入该路径：

| 字段 | 是否必填 | 说明 | 可选值 |
|------|---------|------|--------|
| 入径模式（mode） | 必填 | 患者入径的方式 | `AUTO` / `DOCTOR_CONFIRM` / `MANUAL` |
| 候选评分阈值（candidate_score_threshold） | 选填 | 推荐候选的最低评分 | 0-100 的数值 |
| 入径规则（entry_rules） | 选填 | 入径条件关联的规则编码列表 | `["R_AMI_STEMI_CANDIDATE"]` |
| 图谱查询（graph_query） | 选填 | 用于候选推荐的图谱查询编码 | `GQ_AMI_DISEASE_CANDIDATE` |
| 工作流（dify_workflow） | 选填 | 入径说明生成的工作流编码 | `WF_AMI_ENTRY_EXPLAIN` |

入径模式说明：

| 模式 | 编码 | 说明 |
|------|------|------|
| 自动入径 | `AUTO` | 满足条件时自动将患者纳入路径 |
| 医生确认 | `DOCTOR_CONFIRM` | 系统生成候选推荐，医生确认后入径 |
| 手动入径 | `MANUAL` | 完全由医生手动选择入径 |

#### 创建示例

```json
{
  "pathway_code": "AMI_STEMI",
  "pathway_name": "急性ST段抬高型心肌梗死诊疗路径",
  "version": "1.0.0",
  "specialty_code": "CARDIOLOGY",
  "disease_code": "AMI_STEMI",
  "description": "用于急诊胸痛患者的STEMI识别、再灌注评估、住院治疗、出院二级预防和随访管理。",
  "entry_policy": {
    "mode": "DOCTOR_CONFIRM",
    "candidate_score_threshold": 85,
    "entry_rules": ["R_AMI_STEMI_CANDIDATE"],
    "graph_query": "GQ_AMI_DISEASE_CANDIDATE",
    "dify_workflow": "WF_AMI_ENTRY_EXPLAIN"
  }
}
```

---

### 3.2 路径编辑器

路径编辑器是可视化的节点编辑工具，用于设计临床路径的执行流程。

#### 打开编辑器

1. 在路径列表中找到目标路径，点击路径编码进入详情页
2. 点击 **「编辑」** 按钮，进入路径编辑器

#### 编辑器模式

| 模式 | 说明 |
|------|------|
| 编辑模式（edit） | 可添加、删除、修改节点和连线 |
| 查看模式（view） | 只读查看路径结构 |
| 对比模式（diff） | 对比两个版本的差异 |

#### 节点类型

| 节点类型 | 编辑器标识 | 说明 | 典型用途 |
|---------|-----------|------|---------|
| 入径评估 | `start` | 路径起始节点，定义入径条件 | 患者入径评估 |
| 执行任务 | `task` | 需要完成的诊疗任务 | 心电图检查、抗栓治疗 |
| 条件判断 | `decision` | 根据条件选择不同分支 | PCI可及性评估 |
| 出径评估 | `end` | 路径结束节点，定义出径条件 | 出院评估 |

#### 节点配置字段

| 字段 | 是否必填 | 说明 | 示例 |
|------|---------|------|------|
| 节点编码（node_code） | 必填 | 节点唯一标识 | `AMI_CHEST_PAIN_IDENTIFY` |
| 节点名称（node_name） | 必填 | 节点中文名称 | `胸痛识别` |
| 节点类型（node_type） | 必填 | 节点业务类型 | `ASSESSMENT` |
| SLA时限（sla_minutes） | 选填 | 节点完成时限（分钟） | `10` |
| 入口条件（entry_condition） | 选填 | 进入节点的条件 | 见条件表达式 |
| 出口条件（exit_condition） | 选填 | 离开节点的条件 | 见条件表达式 |
| 任务列表（tasks） | 选填 | 节点包含的任务 | 见任务配置 |
| 绑定规则（bindings.rules） | 选填 | 关联的规则编码 | `["R_AMI_ECG_TIMELY"]` |
| 绑定图谱查询（bindings.graph_queries） | 选填 | 关联的图谱查询 | `["GQ_AMI_ECG_FINDING"]` |
| 绑定工作流（bindings.dify_workflows） | 选填 | 关联的AI工作流 | 见工作流绑定 |
| 转移条件（transitions） | 选填 | 到下一节点的转移规则 | 见转移配置 |

#### 节点业务类型

| 业务类型 | 编码 | 说明 |
|---------|------|------|
| 评估 | `ASSESSMENT` | 入径评估、诊断评估 |
| 治疗决策 | `TREATMENT_DECISION` | 需要做出治疗选择的节点 |
| 治疗 | `TREATMENT` | 执行治疗操作的节点 |
| 出院 | `DISCHARGE` | 出院相关操作 |

#### 任务配置

每个节点可包含多个任务：

| 字段 | 是否必填 | 说明 | 示例 |
|------|---------|------|------|
| 任务编码（task_code） | 必填 | 任务唯一标识 | `TASK_ECG` |
| 任务名称（task_name） | 必填 | 任务中文名称 | `完成十二导联心电图` |
| 任务类型（task_type） | 必填 | 任务的业务类型 | `EXAM` |
| 是否必填（required） | 必填 | 任务是否必须完成 | `true` |
| SLA时限（sla_minutes） | 选填 | 任务完成时限（分钟） | `10` |
| 数据来源（source） | 选填 | 任务完成数据的适配器配置 | 见数据来源 |

任务类型说明：

| 任务类型 | 编码 | 说明 |
|---------|------|------|
| 检查 | `EXAM` | 检查类任务（心电图、影像等） |
| 检验 | `LAB` | 检验类任务（血液、生化等） |
| 表单 | `FORM` | 表单填写类任务 |
| 医嘱组 | `ORDER_SET` | 医嘱套餐类任务 |
| 随访 | `FOLLOW_UP` | 随访类任务 |

数据来源配置：

```json
{
  "source": {
    "adapter_code": "ECG_ADAPTER",
    "query_code": "QUERY_ECG_REPORT"
  }
}
```

#### 连线与转移

节点之间通过连线（Edge）定义执行顺序和转移条件：

| 字段 | 说明 | 示例 |
|------|------|------|
| 目标节点（to_node） | 转移到的目标节点编码 | `AMI_REPERFUSION_EVAL` |
| 转移条件（condition） | 触发转移的条件 | `{"rule_result": "R_AMI_STEMI_CANDIDATE", "equals": true}` |
| 优先级（priority） | 多个转移条件时的优先级 | `1` |

#### 工作流绑定

节点可绑定AI工作流，在特定事件触发时自动执行：

```json
{
  "bindings": {
    "dify_workflows": [
      {
        "trigger": "ON_NODE_ENTER",
        "workflow_code": "WF_AMI_NODE_SUMMARY"
      },
      {
        "trigger": "ON_NODE_COMPLETE",
        "workflow_code": "WF_AMI_TREATMENT_EXPLAIN"
      }
    ]
  }
}
```

工作流触发时机：

| 触发时机 | 编码 | 说明 |
|---------|------|------|
| 节点进入 | `ON_NODE_ENTER` | 患者进入该节点时触发 |
| 节点完成 | `ON_NODE_COMPLETE` | 节点所有任务完成时触发 |

#### 编辑器操作

1. **添加节点**：从左侧节点面板拖拽节点类型到画布
2. **编辑节点**：双击节点打开属性面板，编辑节点配置
3. **添加连线**：从节点边缘的连接点拖拽到目标节点
4. **编辑连线**：双击连线设置转移条件和标签
5. **删除节点/连线**：选中后按 Delete 键或右键菜单删除
6. **保存草稿**：点击工具栏 **「保存草稿」** 按钮

---

### 3.3 路径版本管理

#### 版本状态

| 状态 | 编码 | 说明 |
|------|------|------|
| 草稿 | `DRAFT` | 编辑中，尚未发布 |
| 已发布 | `PUBLISHED` | 已发布上线，可入径 |
| 已废弃 | `RETIRED` | 已废弃，不再接受新患者入径 |

#### 版本操作

| 操作 | 说明 |
|------|------|
| 保存草稿 | `PUT /api/pathways/{code}/draft` — 保存当前编辑内容 |
| 校验路径 | `POST /api/pathways/{code}/validate` — 校验路径完整性 |
| 提交审核 | `POST /api/pathways/{code}/submit-review` — 提交审核 |
| 发布路径 | `POST /api/pathways/{code}/publish` — 发布新版本 |
| 回滚路径 | `POST /api/pathways/{code}/rollback` — 回滚到上一版本 |
| 版本对比 | `GET /api/pathways/{code}/diff?from={v1}&to={v2}` — 对比差异 |

#### 版本对比

版本对比功能可以清晰展示两个版本之间的差异：

| 差异类型 | 说明 |
|---------|------|
| `nodes_added` | 新增的节点 |
| `nodes_removed` | 删除的节点 |
| `nodes_modified` | 修改的节点 |
| `edges_added` | 新增的连线 |
| `edges_removed` | 删除的连线 |
| `edges_modified` | 修改的连线 |
| `tasks_added` | 新增的任务 |
| `tasks_removed` | 删除的任务 |
| `tasks_modified` | 修改的任务 |

对比结果包含汇总统计：

```json
{
  "summary": {
    "nodes_added": 1,
    "nodes_removed": 0,
    "nodes_modified": 2,
    "edges_added": 1,
    "edges_removed": 0,
    "edges_modified": 0,
    "tasks_added": 3,
    "tasks_removed": 1,
    "tasks_modified": 0
  }
}
```

#### 注意事项

- 已发布的版本不可修改，只能创建新版本
- 已废弃的路径不再接受新患者入径，但已在路径中的患者可继续执行
- 回滚操作会将路径恢复到上一发布版本

---

### 3.4 患者入径

患者入径是将患者纳入临床路径实例的过程。

#### 候选推荐

系统根据患者信息自动推荐适合的临床路径：

```
POST /api/patient-pathways/candidates
```

请求体为患者上下文数据：

```json
{
  "patient": {
    "patient_id": "P001",
    "gender": "MALE",
    "age": 65
  },
  "encounter": {
    "encounter_id": "E001",
    "visit_type": "EMERGENCY",
    "department_code": "ER"
  },
  "chief_complaints": [
    { "code": "CHEST_PAIN", "name": "胸痛" }
  ],
  "exams": {
    "finding_codes": ["ST_ELEVATION_CONTIGUOUS_LEADS"]
  }
}
```

返回推荐卡片列表：

| 字段 | 说明 |
|------|------|
| `recommendation_id` | 推荐唯一标识 |
| `target_code` | 推荐的路径编码 |
| `target_name` | 路径名称 |
| `score` | 推荐评分（0-100） |
| `confidence` | 置信度 |
| `action_level` | 建议操作级别 |
| `supporting_facts` | 支持推荐的事实依据 |
| `missing_facts` | 缺失的事实数据 |
| `suggested_actions` | 建议的操作 |

#### 入径操作

医生确认后，执行入径操作：

```
POST /api/patient-pathways/admit
```

```json
{
  "pathway_code": "AMI_STEMI",
  "version_no": "1.0.0",
  "patient_id": "P001",
  "encounter_id": "E001",
  "initial_facts": {
    "chief_complaints": [{ "code": "CHEST_PAIN" }],
    "ecg_finding": "ST_ELEVATION_CONTIGUOUS_LEADS"
  },
  "operator_id": "DR_ZHANG"
}
```

入径成功后返回路径实例：

| 字段 | 说明 |
|------|------|
| `instance_id` | 路径实例唯一标识 |
| `patient_id` | 患者ID |
| `encounter_id` | 就诊ID |
| `pathway_code` | 路径编码 |
| `version_no` | 路径版本 |
| `status` | 实例状态 |
| `current_node_code` | 当前所在节点编码 |

#### 路径实例状态

| 状态 | 编码 | 说明 |
|------|------|------|
| 执行中 | `ACTIVE` | 患者正在路径中执行 |
| 已完成 | `COMPLETED` | 患者已完成全部路径节点 |
| 已退出 | `EXITED` | 患者中途退出路径 |
| 已终止 | `TERMINATED` | 路径被强制终止 |

#### 路径实例跟踪

查询患者路径实例列表：

```
GET /api/pathway-instances?patient_id={patientId}
```

支持以下筛选条件：

| 参数 | 说明 |
|------|------|
| `pathway_code` | 按路径编码筛选 |
| `status` | 按实例状态筛选 |
| `patient_id` | 按患者ID筛选 |
| `encounter_id` | 按就诊ID筛选 |
| `current_node_code` | 按当前节点筛选 |
| `limit` | 返回条数限制 |

---

### 3.5 任务执行

#### 查看节点状态

```
GET /api/patient-pathways/{instanceId}/nodes/{nodeCode}
```

返回节点状态和任务列表：

| 字段 | 说明 |
|------|------|
| `node_code` | 节点编码 |
| `node_name` | 节点名称 |
| `status` | 节点状态 |
| `enter_time` | 进入节点时间 |
| `complete_time` | 完成节点时间 |
| `timeout_flag` | 是否超时 |
| `tasks` | 任务列表 |

节点状态说明：

| 状态 | 编码 | 说明 |
|------|------|------|
| 待执行 | `PENDING` | 尚未进入该节点 |
| 执行中 | `ACTIVE` | 正在该节点执行 |
| 已完成 | `COMPLETED` | 节点已完成 |
| 已跳过 | `SKIPPED` | 节点已跳过 |
| 已阻塞 | `BLOCKED` | 节点被阻塞 |

#### 完成任务

```
POST /api/patient-pathways/{instanceId}/nodes/{nodeCode}/tasks/{taskCode}/complete
```

请求体可包含任务执行结果：

```json
{
  "result": {
    "ecg_finding": "ST_ELEVATION_CONTIGUOUS_LEADS",
    "report_time": "2026-05-23T10:30:00"
  }
}
```

#### 跳过任务

```
POST /api/patient-pathways/{instanceId}/nodes/{nodeCode}/tasks/{taskCode}/skip
```

跳过任务时建议填写跳过原因：

```json
{
  "reason": "患者拒绝该检查"
}
```

#### 完成节点

当不需要逐任务推进时，可以直接完成整个节点：

```
POST /api/patient-pathways/{instanceId}/nodes/{nodeCode}/complete
```

#### 任务状态说明

| 状态 | 编码 | 说明 |
|------|------|------|
| 待执行 | `PENDING` | 任务尚未开始 |
| 已完成 | `COMPLETED` | 任务已完成 |
| 已跳过 | `SKIPPED` | 任务已跳过 |
| 已失败 | `FAILED` | 任务执行失败 |

---

### 3.6 变异管理

变异（Variation）是指路径执行过程中偏离标准流程的情况。记录和分析变异是持续改进临床路径的重要依据。

#### 记录变异

```
POST /api/patient-pathways/{instanceId}/variations
```

```json
{
  "variation_type": "SKIP",
  "node_code": "AMI_REPERFUSION_EVAL",
  "reason": "患者存在溶栓禁忌证，改为保守治疗",
  "operator_id": "DR_ZHANG"
}
```

#### 变异类型

| 变异类型 | 编码 | 说明 | 典型场景 |
|---------|------|------|---------|
| 跳过 | `SKIP` | 跳过某个节点或任务 | 患者拒绝检查 |
| 延迟 | `DEFER` | 延迟执行某个节点或任务 | 等待检查结果 |
| 延长时限 | `EXTEND_TIME` | 延长节点完成时限 | 病情复杂需延长观察 |
| 替换 | `SUBSTITUTE` | 用其他方案替换标准方案 | 药物过敏替换用药 |
| 退出 | `EXIT` | 中途退出路径 | 患者转院 |
| 回退 | `ROLLBACK` | 回退到之前的节点 | 治疗效果不佳需调整 |
| 人工覆盖 | `MANUAL_OVERRIDE` | 人工干预覆盖系统建议 | 上级医师决策 |

#### 变异原因分类

| 原因类别 | 说明 | 示例 |
|---------|------|------|
| 患者原因 | 因患者自身因素导致变异 | 患者拒绝、过敏、合并症 |
| 医疗原因 | 因诊疗需要导致变异 | 病情变化、并发症、治疗方案调整 |
| 系统原因 | 因系统或流程问题导致变异 | 检查设备故障、科室协调问题 |

#### 变异记录查询

```
GET /api/pathway-variations
```

支持以下筛选条件：

| 参数 | 说明 |
|------|------|
| `pathway_code` | 按路径编码筛选 |
| `patient_id` | 按患者ID筛选 |
| `encounter_id` | 按就诊ID筛选 |
| `variation_type` | 按变异类型筛选 |
| `node_code` | 按节点编码筛选 |
| `instance_id` | 按实例ID筛选 |
| `limit` | 返回条数限制 |

---

### 3.7 路径统计

路径统计功能提供路径执行的各项指标，帮助管理者评估路径效果和发现问题。

#### 实例汇总

```
GET /api/pathway-instances/summary
```

| 指标 | 说明 |
|------|------|
| `total` | 总实例数 |
| `active` | 执行中实例数 |
| `completed` | 已完成实例数 |
| `exited` | 已退出实例数 |
| `terminated` | 已终止实例数 |
| `variation_count` | 变异总数 |
| `avg_node_count` | 平均经过节点数 |

#### 节点完成度

```
GET /api/pathway-instances/node-completion
```

返回各节点的完成率统计：

| 指标 | 说明 |
|------|------|
| `node_code` | 节点编码 |
| `node_name` | 节点名称 |
| `completed` | 已完成数 |
| `total` | 总数 |
| `completion_rate` | 完成率 |

#### 节点停留时长

```
GET /api/pathway-instances/node-stay-duration
```

返回各节点的停留时长统计：

| 指标 | 说明 |
|------|------|
| `node_code` | 节点编码 |
| `node_name` | 节点名称 |
| `avg_stay_hours` | 平均停留时长（小时） |
| `max_stay_hours` | 最长停留时长（小时） |
| `min_stay_hours` | 最短停留时长（小时） |

#### 变异统计

```
GET /api/pathway-variations/summary
```

| 指标 | 说明 |
|------|------|
| `total` | 变异总数 |
| `by_type` | 按变异类型分组统计 |
| `by_node` | 按节点分组统计 |
| `recent_variations` | 最近的变异记录 |

#### 关键指标说明

| 指标 | 计算方式 | 说明 |
|------|---------|------|
| 入径率 | 入径患者数 / 符合条件患者数 | 反映路径推广程度 |
| 完成率 | 已完成实例数 / 总实例数 | 反映路径执行效果 |
| 变异率 | 变异总数 / 总实例数 | 反映路径适用性 |
| 平均住院日 | 平均节点停留时长之和 | 反映路径效率 |

---

### 3.8 路径发布审批

路径发布遵循严格的审批流程，确保上线路径经过充分验证。

#### 发布流程

```
草稿(DRAFT) → 校验(validate) → 提交审核(submit-review) → 发布(publish)
```

#### 操作步骤

1. **编辑路径**：在路径编辑器中完成节点和连线的设计
2. **保存草稿**：点击 **「保存草稿」**，确保编辑内容已保存
3. **校验路径**：点击 **「校验」** 按钮，系统自动检查路径配置的完整性和正确性
4. **模拟验证**：使用模拟数据验证路径执行流程是否符合预期
5. **提交审核**：校验通过后，点击 **「提交审核」**
6. **审核确认**：审核人员审核通过后，点击 **「发布」**
7. **上线生效**：发布后的路径可接受患者入径

#### 校验内容

系统校验以下内容：

| 校验项 | 说明 |
|--------|------|
| 必填字段 | 路径编码、名称、版本、科室、入径策略、阶段 |
| 节点完整性 | 每个阶段至少包含一个节点 |
| 连线完整性 | 节点之间的连线无断开 |
| 起止节点 | 路径必须包含起始和结束节点 |
| 任务配置 | 必填任务已配置完成 |
| 转移条件 | 所有分支都有明确的转移条件 |
| SLA设置 | 关键节点已设置完成时限 |

校验结果包含错误和警告：

| 级别 | 说明 |
|------|------|
| error | 必须修复后才能发布 |
| warning | 建议修复，不影响发布 |

---

## 4. 最佳实践

### 4.1 路径设计原则

1. **循证依据**：路径设计应基于最新的临床指南和循证医学证据，并在路径中标注来源文献
2. **节点精简**：每个阶段节点数量控制在3-6个，避免路径过于复杂
3. **任务明确**：每个节点的任务应具体可执行，避免模糊描述
4. **分支合理**：条件判断节点分支不超过3条，确保流程清晰
5. **时限合理**：SLA时限参考临床指南和医院实际情况，不宜过紧或过松
6. **来源标注**：每个节点和规则都应标注 `reference_document_code` 和 `reference_citation_id`，确保可追溯

### 4.2 节点配置建议

#### 节点编码命名规范

采用 `{路径缩写}_{阶段}_{功能描述}` 格式：

| 示例 | 说明 |
|------|------|
| `AMI_CHEST_PAIN_IDENTIFY` | AMI路径-胸痛识别 |
| `AMI_REPERFUSION_EVAL` | AMI路径-再灌注评估 |
| `AMI_INPATIENT_TREATMENT` | AMI路径-住院治疗 |
| `AMI_DISCHARGE_PREVENTION` | AMI路径-出院二级预防 |

#### SLA时限设置建议

| 节点类型 | 建议时限 | 说明 |
|---------|---------|------|
| 急诊评估 | 10-30分钟 | 急诊场景要求快速响应 |
| 检查检验 | 30-120分钟 | 取决于检查类型 |
| 治疗决策 | 30-60分钟 | 需要多学科讨论 |
| 住院治疗 | 24-72小时 | 根据疾病特点 |
| 出院评估 | 2-4小时 | 出院前核查 |

#### 任务配置建议

- 每个节点任务数量控制在2-5个
- 必填任务（`required: true`）应为关键诊疗步骤
- 非必填任务用于辅助性检查或可选操作
- 为检查检验类任务配置数据来源（`source`），实现自动完成检测

### 4.3 变异处理流程

1. **发现变异**：系统自动检测或医生主动记录偏离标准流程的情况
2. **记录变异**：选择变异类型，填写变异原因
3. **分类分析**：按患者原因/医疗原因/系统原因分类
4. **制定措施**：针对高频变异制定改进措施
5. **路径优化**：根据变异分析结果优化路径设计

变异处理要点：
- 所有变异必须记录原因，不留空白
- 退出路径的变异需经上级医师确认
- 系统原因导致的变异应及时反馈信息科处理
- 每月汇总变异数据，在质控会议上讨论

### 4.4 路径持续改进

1. **定期评审**：每季度对路径执行数据进行评审
2. **关注指标**：
   - 完成率低于80%：检查路径适用性
   - 变异率高于30%：分析变异原因，考虑调整路径
   - 某节点停留时长异常：排查流程瓶颈
3. **版本迭代**：根据评审结果修订路径，发布新版本
4. **指南同步**：当临床指南更新时，及时同步更新路径内容

---

## 5. 常见问题

### 5.1 入径条件不满足排查

**现象**：患者符合路径条件但未收到入径推荐

**排查步骤**：

1. **检查路径状态**：确认路径状态为 `PUBLISHED`，且存在活跃版本（`active_published_version` 不为空）
2. **检查入径规则**：确认 `entry_policy.entry_rules` 中引用的规则已发布且启用
3. **检查候选评分**：确认患者推荐评分是否达到 `candidate_score_threshold` 阈值
4. **检查图谱查询**：如果配置了 `graph_query`，确认图谱服务可用且查询结果正确
5. **检查组织范围**：确认路径的适用科室与患者当前科室匹配
6. **检查适配器**：确认入径规则依赖的数据适配器正常工作

### 5.2 路径变异处理

**现象**：路径执行中出现大量变异

**处理方法**：

1. **统计分析**：通过 `GET /api/pathway-variations/summary` 查看变异分布
2. **定位热点**：关注 `by_node` 中变异次数最多的节点
3. **分类归因**：按 `by_type` 分析变异类型分布
4. **针对性改进**：
   - 跳过（SKIP）为主 → 检查该节点任务是否必要
   - 延迟（DEFER）为主 → 检查前置条件是否合理
   - 替换（SUBSTITUTE）为主 → 检查标准方案是否适用
   - 退出（EXIT）为主 → 检查入径条件是否过宽
5. **调整路径**：根据分析结果修订路径，发布新版本

### 5.3 路径版本切换

**现象**：需要将当前路径切换到新版本

**操作步骤**：

1. **发布新版本**：完成新版本的编辑、校验和审批后发布
2. **观察过渡**：新版本发布后，已在旧版本中的患者继续执行旧版本，新入径患者使用新版本
3. **废弃旧版本**：确认旧版本无活跃实例后，将旧版本标记为 `RETIRED`
4. **版本对比**：使用 `GET /api/pathways/{code}/diff` 确认版本差异

**注意事项**：
- 版本切换不会影响已在执行中的路径实例
- 废弃版本前需确认无活跃实例（`status = ACTIVE`）
- 建议在低峰时段进行版本切换操作
- 切换后观察3-5天，确认新版本运行正常

---

## 附录

### A. 路径示例参考

系统内置以下示例路径，可在路径列表中查看：

| 路径编码 | 路径名称 | 适用科室 |
|---------|---------|---------|
| `AMI_STEMI` | 急性ST段抬高型心肌梗死诊疗路径 | 心内科 |

### B. AMI_STEMI 路径结构示例

```
急诊识别与再灌注评估（ER_STAGE）
├── 胸痛识别（AMI_CHEST_PAIN_IDENTIFY）
│   └── 任务：完成十二导联心电图
├── 再灌注策略评估（AMI_REPERFUSION_EVAL）
│   ├── 任务：评估PCI可及性
│   ├── 任务：评估溶栓禁忌证
│   └── 任务：肌钙蛋白检查
├── 住院治疗与监测（AMI_INPATIENT_TREATMENT）
│   ├── 任务：抗栓治疗评估
│   └── 任务：并发症监测
└── 出院二级预防（AMI_DISCHARGE_PREVENTION）
    ├── 任务：核查出院二级预防用药
    └── 任务：生成随访计划
```

### C. 路径配置完整示例

```json
{
  "pathway_code": "AMI_STEMI",
  "pathway_name": "急性ST段抬高型心肌梗死诊疗路径",
  "version": "1.0.0",
  "specialty_code": "CARDIOLOGY",
  "disease_code": "AMI_STEMI",
  "entry_policy": {
    "mode": "DOCTOR_CONFIRM",
    "candidate_score_threshold": 85,
    "entry_rules": ["R_AMI_STEMI_CANDIDATE"]
  },
  "stages": [
    {
      "stage_code": "ER_STAGE",
      "stage_name": "急诊识别与再灌注评估",
      "sort_no": 1,
      "nodes": [
        {
          "node_code": "AMI_CHEST_PAIN_IDENTIFY",
          "node_name": "胸痛识别",
          "node_type": "ASSESSMENT",
          "sla_minutes": 10,
          "tasks": [
            {
              "task_code": "TASK_ECG",
              "task_name": "完成十二导联心电图",
              "task_type": "EXAM",
              "required": true,
              "sla_minutes": 10
            }
          ],
          "transitions": [
            {
              "to_node": "AMI_REPERFUSION_EVAL",
              "condition": { "rule_result": "R_AMI_STEMI_CANDIDATE", "equals": true },
              "priority": 1
            }
          ]
        }
      ]
    }
  ]
}
```

### D. 节点状态流转图

```
PENDING → ACTIVE → COMPLETED
              ↓
           SKIPPED
              ↓
           BLOCKED
```

### E. 实例状态流转图

```
ACTIVE → COMPLETED（正常完成）
  ↓
EXITED（中途退出）
  ↓
TERMINATED（强制终止）
```
