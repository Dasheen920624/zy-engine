# 规则治理模块用户手册

> 适用对象：医院信息科运维人员、质控管理人员
> 版本：2026.05
> 模块代号：Rule Governance

---

## 1. 模块概述

### 1.1 什么是规则治理模块

规则治理模块是 MedKernel 临床决策支持系统（CDSS）的核心组件，负责管理、执行和监控临床决策支持规则。通过规则引擎，系统能够在医生开具医嘱、书写病历、开具处方等关键业务节点自动进行安全校验、质控检查和智能提醒，帮助医院实现：

- **医疗安全拦截**：在医嘱提交时自动检测重复用药、禁忌证、药物相互作用等风险，防止不安全医嘱下达
- **病历质控**：自动检查病历完整性、时效性，减少质控缺陷
- **医保合规**：校验医保限定适应症与诊断的一致性，降低拒付风险
- **路径入径推荐**：根据患者临床信息自动推荐适合的临床路径

### 1.2 核心价值

| 价值维度 | 说明 |
|---------|------|
| 患者安全 | 实时拦截不安全医嘱，减少医疗差错 |
| 质控合规 | 自动化质控检查，提升病历质量与医保合规率 |
| 效率提升 | 减少人工质控工作量，缩短质控反馈周期 |
| 知识沉淀 | 将临床指南和医院制度转化为可执行的规则，持续积累 |

### 1.3 规则生命周期

一条规则从创建到上线，经历以下生命周期阶段：

```
草稿(DRAFT) → 已审核(REVIEWED) → 已发布(PUBLISHED) → 已退役(RETIRED)
```

---

## 2. 功能清单

| 功能 | 说明 | 对应接口 |
|------|------|---------|
| 规则列表查询 | 按类型、状态、关键词筛选规则 | `GET /api/rules` |
| 规则详情查看 | 查看规则完整定义，支持历史版本 | `GET /api/rules/{ruleCode}` |
| 创建/导入规则 | 单条或批量创建规则 | `POST /api/rules` |
| 删除规则 | 删除指定规则（仅草稿状态可删） | `DELETE /api/rules/{ruleCode}` |
| 规则发布 | 将规则从草稿状态发布上线 | `POST /api/rules/{ruleCode}/publish` |
| 规则模拟 | 输入患者数据试运行规则，查看命中结果 | `POST /api/rules/simulate` |
| 规则评估 | 按场景批量评估规则 | `POST /api/rules/evaluate` |
| 场景化评估 | 外部系统调用的评估入口 | `POST /api/rule-engine/evaluate` |
| 执行日志查询 | 查看规则触发历史记录 | `GET /api/rules/exec-logs` |
| 执行日志详情 | 查看单次触发的详细信息 | `GET /api/rules/exec-logs/{logId}` |
| 执行日志汇总 | 规则执行的聚合统计 | `GET /api/rules/exec-logs/summary` |
| 决策日志记录 | 记录医生对规则提醒的决策 | `POST /api/rule-action-logs` |
| 决策日志查询 | 查询医生决策历史 | `GET /api/rule-action-logs` |
| 触发点配置 | 配置规则在哪些业务事件触发执行 | `cdss_trigger_point` 表 |
| 配置包管理 | 将规则打包发布、导入导出 | `/api/config-packages/*` |

---

## 3. 操作指南

### 3.1 创建规则

#### 操作步骤

1. 进入 **规则治理** 模块，点击页面右上角 **「新建规则」** 按钮
2. 在弹出的规则编辑表单中填写以下字段
3. 填写完成后点击 **「保存草稿」**，规则状态为 `DRAFT`

#### 字段说明

| 字段 | 是否必填 | 说明 | 示例 |
|------|---------|------|------|
| 规则编码（rule_code） | 必填 | 规则唯一标识，全局不可重复 | `R_ORDER_SAFETY_DUP_ANTIBIOTIC` |
| 规则名称（rule_name） | 必填 | 规则的中文描述名称 | `重复抗菌药物医嘱拦截` |
| 规则类型（rule_type） | 必填 | 规则的业务分类 | `SAFETY` |
| 版本号（version_no） | 必填 | 语义化版本号 | `1.0.0` |
| 严重程度（severity） | 必填 | 规则命中时的严重级别 | `HIGH` |
| 动作模式（action_mode） | 选填 | 规则命中后的干预方式 | `BLOCK` |
| 规则包编码（package_code） | 选填 | 所属规则包 | `PKG_AMI_CORE` |
| 规则包版本（package_version） | 选填 | 所属规则包版本 | `2026.05` |
| 是否启用（enabled） | 必填 | 规则是否生效 | `true` |
| 规则DSL（rule_json） | 必填 | 规则的条件与动作定义 | 见下方DSL说明 |

#### 规则类型说明

| 规则类型 | 编码 | 说明 |
|---------|------|------|
| 时限质控 | `TIME_LIMIT_QC` | 检查诊疗行为是否在规定时间内完成 |
| 内容质控 | `CONTENT_QC` | 检查病历、文书等内容的完整性和规范性 |
| 路径节点 | `PATHWAY_NODE` | 临床路径节点相关的规则 |
| 安全拦截 | `SAFETY` | 医疗安全相关规则，如药物禁忌、重复医嘱 |
| 随访提醒 | `FOLLOWUP` | 随访计划相关的提醒规则 |
| 运营管理 | `OPERATION` | 医院运营管理相关规则 |

#### 严重程度说明

| 严重程度 | 编码 | 说明 |
|---------|------|------|
| 高 | `HIGH` | 需要立即处理的安全问题 |
| 中 | `MEDIUM` | 需要关注的质控问题 |
| 低 | `LOW` | 一般性提醒 |
| 信息 | `INFO` | 仅供参考的提示 |

#### 动作模式说明

| 动作模式 | 编码 | 说明 |
|---------|------|------|
| 通知 | `NOTICE` | 仅弹出提示信息，不阻断操作 |
| 软拦截 | `SOFT` | 弹出提醒，医生可选择继续或修改 |
| 硬拦截 | `BLOCK` | 强制阻断操作，必须修改后才能继续 |

#### 规则 DSL 结构

规则 DSL（Domain Specific Language）是规则的核心定义，包含触发条件、数据需求和执行结果：

```json
{
  "rule_code": "R_ORDER_SAFETY_DUP_ANTIBIOTIC",
  "rule_name": "重复抗菌药物医嘱拦截",
  "rule_type": "SAFETY",
  "version": "1.0.0",
  "trigger": {
    "events": ["ORDER_SAVE"],
    "scope": "ORDER_SAFETY"
  },
  "data_requirements": [
    {
      "fact_name": "antibiotic_orders",
      "source": {
        "adapter_code": "HIS_ORDER_ADAPTER",
        "query_code": "QUERY_ANTIBIOTIC_ORDERS_48H"
      }
    }
  ],
  "condition": {
    "all": [
      {
        "field": "orders.antibiotic_duplicate_within_48h",
        "eq": true
      }
    ]
  },
  "result": {
    "hit": {
      "actions": [
        { "type": "BLOCK_ORDER", "order_category": "ANTIBIOTIC" },
        { "type": "PUSH_TO_DOCTOR", "level": "SAFETY_BLOCK" }
      ],
      "message": "检测到48小时内重复开立抗菌药物，请由上级医师确认必要性后再下达。"
    }
  }
}
```

##### DSL 字段说明

| 字段 | 必填 | 说明 |
|------|------|------|
| `trigger.events` | 是 | 触发事件列表，如 `ORDER_SAVE`、`PRESCRIPTION_SAVE` |
| `trigger.scope` | 否 | 触发范围/场景 |
| `data_requirements` | 否 | 规则执行所需的数据来源定义 |
| `condition` | 是 | 规则命中条件 |
| `result.hit` | 否 | 命中时的动作和消息 |
| `result.not_hit` | 否 | 未命中时的动作和消息 |

##### 条件表达式

条件表达式支持以下逻辑组合和比较操作：

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `all` | 所有子条件都满足（AND） | `{"all": [条件1, 条件2]}` |
| `any` | 任一子条件满足（OR） | `{"any": [条件1, 条件2]}` |
| `not` | 子条件不满足（NOT） | `{"not": 条件}` |
| `field` + `eq` | 字段等于 | `{"field": "age", "eq": 65}` |
| `field` + `ne` | 字段不等于 | `{"field": "status", "ne": "COMPLETED"}` |
| `field` + `gt` | 字段大于 | `{"field": "dose", "gt": 100}` |
| `field` + `gte` | 字段大于等于 | `{"field": "age", "gte": 18}` |
| `field` + `lt` | 字段小于 | `{"field": "dose", "lt": 500}` |
| `field` + `lte` | 字段小于等于 | `{"field": "wait_minutes", "lte": 30}` |
| `field` + `in` | 字段在列表中 | `{"field": "dept", "in": ["CARDIOLOGY", "ER"]}` |
| `exists` | 事实存在 | `{"exists": "allergy_record"}` |
| `not_exists` | 事实不存在 | `{"not_exists": "discharge_summary"}` |
| `contains_any` | 字段包含列表中任一项 | `{"field": "symptoms", "contains_any": ["CHEST_PAIN", "DYSPNEA"]}` |
| `duration_minutes_between` | 两个时间点间隔（分钟） | `{"duration_minutes_between": ["arrival_time", "ecg_time"]}` |

##### 条件嵌套示例

```json
{
  "all": [
    { "field": "encounter.visit_type", "eq": "EMERGENCY" },
    {
      "any": [
        { "field": "chief_complaints.code", "in": ["CHEST_PAIN"] },
        { "field": "exams.finding_codes", "contains_any": ["ST_ELEVATION"] }
      ]
    },
    { "not": { "field": "exams.ECG_12_LEAD.report_time", "eq": null } }
  ]
}
```

---

### 3.2 编辑规则

#### 操作步骤

1. 在规则列表中找到目标规则，点击规则编码进入详情页
2. 点击 **「编辑」** 按钮，进入编辑模式
3. 修改需要调整的字段
4. 点击 **「保存草稿」** 保存修改

#### 版本管理

- 每次编辑保存会更新当前草稿版本，不会影响已发布版本
- 通过 `versionNo` 参数可查看历史版本：`GET /api/rules/{ruleCode}?versionNo=1.0.0`
- 已发布的规则版本不可直接修改，需创建新版本

#### 注意事项

- 只有 `DRAFT` 状态的规则可以编辑
- 已发布（`PUBLISHED`）的规则需要先创建新版本草稿才能修改
- 编辑过程中可随时保存草稿，不会影响线上运行的规则

---

### 3.3 规则模拟

规则模拟（试运行）功能允许在规则正式发布前，使用模拟的患者数据测试规则是否按预期命中，避免上线后出现误判或漏判。

#### 操作步骤

1. 在规则详情页，点击 **「模拟运行」** 按钮
2. 在模拟面板中输入患者上下文数据（patient_context）
3. 点击 **「执行模拟」**，查看评估结果

#### 模拟请求示例

```
POST /api/rules/simulate
```

```json
{
  "rule_code": "R_ORDER_SAFETY_DUP_ANTIBIOTIC",
  "patient_context": {
    "patient": {
      "patient_id": "P001",
      "gender": "MALE",
      "age": 65
    },
    "encounter": {
      "encounter_id": "E001",
      "visit_type": "INPATIENT",
      "department_code": "INTERNAL_MED"
    },
    "facts": {
      "orders": {
        "antibiotic_duplicate_within_48h": true
      }
    }
  }
}
```

#### 模拟结果说明

| 字段 | 说明 |
|------|------|
| `hit` | 规则是否命中（`true`/`false`） |
| `severity` | 命中严重程度 |
| `message` | 命中提示消息 |
| `condition_summary` | 条件匹配摘要 |
| `facts_matched` | 匹配的事实数据 |
| `actions` | 触发的动作列表 |
| `evidence` | 证据引用 |

#### 也可以直接模拟 DSL

如果规则尚未保存，可以直接提交 `rule_json` 进行模拟：

```json
{
  "rule_json": {
    "rule_code": "R_TEST",
    "rule_name": "测试规则",
    "rule_type": "SAFETY",
    "version": "0.1.0",
    "trigger": { "events": ["ORDER_SAVE"] },
    "condition": { "all": [{ "field": "orders.duplicate", "eq": true }] },
    "result": { "hit": { "message": "检测到重复医嘱" } }
  },
  "patient_context": {
    "patient": { "patient_id": "P001" },
    "encounter": { "encounter_id": "E001" },
    "facts": { "orders": { "duplicate": true } }
  }
}
```

---

### 3.4 规则发布

规则发布遵循严格的审批流程，确保上线规则经过充分验证。

#### 发布流程

```
草稿(DRAFT) → 提交审核 → 审核通过(REVIEWED) → 发布(PUBLISHED)
```

#### 操作步骤

1. **验证规则**：在规则详情页点击 **「模拟运行」**，确认规则逻辑正确
2. **提交发布**：点击 **「发布」** 按钮，填写发布信息
3. **审核确认**：审核人员审核通过后，规则状态变为 `PUBLISHED`
4. **上线生效**：发布后的规则在对应触发点自动生效

#### 发布请求

```
POST /api/rules/{ruleCode}/publish
```

```json
{
  "package_code": "PKG_AMI_CORE",
  "package_version": "2026.05",
  "version_no": "1.0.0",
  "reviewer_comment": "经模拟验证，规则逻辑正确，准予发布"
}
```

#### 发布参数说明

| 参数 | 必填 | 说明 |
|------|------|------|
| `package_code` | 否 | 发布到的规则包编码 |
| `package_version` | 否 | 规则包版本 |
| `version_no` | 否 | 发布版本号 |
| `reviewer_comment` | 否 | 审核意见 |

---

### 3.5 规则回滚

当已发布的规则在线上出现问题时，可以通过配置包回滚机制恢复到上一版本。

#### 操作步骤

1. 进入 **配置包管理** 模块
2. 找到当前生效的规则配置包
3. 点击 **「回滚」** 按钮，选择要回滚到的目标版本
4. 确认回滚操作

#### 回滚注意事项

- 回滚操作会将整个配置包恢复到指定版本，影响包内所有规则
- 回滚前建议先导出当前配置包快照作为备份
- 回滚后需验证规则执行是否恢复正常

---

### 3.6 规则包管理

规则包（Config Package）是将一组规则打包管理的单元，支持批量发布、导入导出和版本管理。

#### 创建规则包

1. 进入 **配置包管理** 模块，点击 **「新建配置包」**
2. 填写配置包基本信息

| 字段 | 说明 | 示例 |
|------|------|------|
| 配置包编码（package_code） | 全局唯一标识 | `PKG_AMI_RULE_CONFIG` |
| 配置包版本（package_version） | 语义化版本 | `2026.05.01` |
| 资产类型（asset_type） | 固定为 `RULE` | `RULE` |
| 作用域层级（scope_level） | 生效范围 | `HOSPITAL` |
| 作用域编码（scope_code） | 具体范围标识 | `HOSPITAL_DEMO` |

#### 规则包状态

| 状态 | 编码 | 说明 |
|------|------|------|
| 草稿 | `DRAFT` | 编辑中，尚未审核 |
| 已审核 | `REVIEWED` | 审核通过，待发布 |
| 已发布 | `PUBLISHED` | 已发布上线 |
| 已同步 | `SYNCED` | 已同步到目标环境 |
| 活跃 | `ACTIVE` | 当前生效的配置包 |
| 已退役 | `RETIRED` | 已下线不再使用 |

#### 规则包操作

| 操作 | 接口 | 说明 |
|------|------|------|
| 查询列表 | `GET /api/config-packages` | 按类型、状态、作用域筛选 |
| 查看详情 | `GET /api/config-packages/{code}/{version}` | 查看配置包完整内容 |
| 审核校验 | `POST /api/config-packages/{code}/{version}/review` | 校验配置包完整性和来源 |
| 发布 | `POST /api/config-packages/{code}/{version}/publish` | 发布配置包 |
| 导出 | `POST /api/config-packages/{code}/{version}/export` | 导出配置包快照 |

#### 规则包导入

配置包支持从文件导入，流程如下：

1. **上传文件**：`POST /api/config-packages/import/upload`
2. **校验格式**：`POST /api/config-packages/import/validate`
3. **来源检查**：`POST /api/config-packages/import/source-check`
4. **影响评估**：`POST /api/config-packages/import/impact`
5. **确认导入**：`POST /api/config-packages/import/confirm`

---

### 3.7 触发点配置

触发点（Trigger Point）定义了规则在哪些业务事件发生时自动执行。触发点配置存储在 `cdss_trigger_point` 表中。

#### 触发点字段说明

| 字段 | 说明 | 示例 |
|------|------|------|
| 触发点编码（trigger_code） | 触发点唯一标识 | `TP_ORDER_SAVE` |
| 触发点名称（trigger_name） | 触发点中文名称 | `医嘱保存触发` |
| 触发类型（trigger_type） | 触发来源分类 | `ORDER` |
| 业务场景（business_scenario） | 所属业务场景 | `PRESCRIBE` |
| 接入策略（access_strategy） | 接入方式 | `API` |
| 关联规则（rule_codes） | 触发时执行的规则编码列表 | `R_ORDER_SAFETY_DUP_ANTIBIOTIC` |
| 关联路径（pathway_codes） | 触发时关联的路径编码 | `AMI_STEMI` |
| 优先级（priority） | 执行优先级 | `100` |
| 风险等级（risk_level） | 触发点风险等级 | `HIGH` |
| 超时时间（timeout_ms） | 执行超时时间（毫秒） | `5000` |
| 是否启用（enabled） | 触发点是否生效 | `TRUE` |

#### 触发类型

| 触发类型 | 编码 | 说明 |
|---------|------|------|
| 医嘱 | `ORDER` | 医嘱开立/保存时触发 |
| 病历 | `EMR` | 病历书写/提交时触发 |
| 检查检验 | `EXAM` | 检查检验报告发布时触发 |
| 路径 | `PATHWAY` | 临床路径相关事件触发 |
| 医保 | `INSURANCE` | 医保结算相关事件触发 |

#### 业务场景

| 业务场景 | 编码 | 说明 |
|---------|------|------|
| 开处方 | `PRESCRIBE` | 医生开立处方/医嘱 |
| 入院 | `ADMISSION` | 患者入院登记 |
| 诊断 | `DIAGNOSIS` | 医生下达诊断 |
| 检查检验 | `EXAM` | 开立或报告检查检验 |
| 路径入径 | `PATHWAY_ADMIT` | 患者进入临床路径 |
| 结算 | `SETTLEMENT` | 医保结算 |

#### 接入策略

| 接入策略 | 编码 | 说明 |
|---------|------|------|
| API调用 | `API` | 通过REST API直接调用 |
| 内嵌框架 | `IFRAME` | 通过iframe嵌入HIS页面 |
| CDS Hooks | `CDS_HOOKS` | 遵循CDS Hooks标准协议 |
| 消息队列 | `MESSAGE` | 通过消息队列异步触发 |

#### 常见触发点配置示例

| 触发点编码 | 触发类型 | 业务场景 | 关联规则 |
|-----------|---------|---------|---------|
| `TP_ORDER_SAVE` | ORDER | PRESCRIBE | R_ORDER_SAFETY_DUP_ANTIBIOTIC |
| `TP_PRESCRIPTION_SAVE` | ORDER | PRESCRIBE | R_INS_DRUG_INDICATION_MISMATCH |
| `TP_LAB_RESULT` | EXAM | EXAM | R_AMI_TROPONIN_ALERT |
| `TP_EMR_SUBMIT` | EMR | DIAGNOSIS | R_EMR_DISCHARGE_SUMMARY_COMPLETE |
| `TP_PATHWAY_ADMIT` | PATHWAY | PATHWAY_ADMIT | R_AMI_STEMI_CANDIDATE |

---

### 3.8 执行日志

执行日志记录了每次规则触发的详细信息，用于问题排查和效果评估。

#### 查询执行日志

```
GET /api/rules/exec-logs
```

支持以下筛选条件：

| 参数 | 说明 | 示例 |
|------|------|------|
| `rule_code` | 按规则编码筛选 | `R_ORDER_SAFETY_DUP_ANTIBIOTIC` |
| `trace_id` | 按追踪ID筛选 | `TRACE_20260523_001` |
| `patient_id` | 按患者ID筛选 | `P001` |
| `encounter_id` | 按就诊ID筛选 | `E001` |
| `result_status` | 按执行结果筛选 | `SUCCESS` / `ERROR` |
| `hit` | 按是否命中筛选 | `true` / `false` |
| `scenario_code` | 按场景筛选 | `ORDER_SAFETY` |
| `limit` | 返回条数限制 | `50` |

#### 执行日志字段

| 字段 | 说明 |
|------|------|
| `log_id` | 日志唯一标识 |
| `trace_id` | 追踪ID，用于关联一次请求链路 |
| `rule_code` | 触发的规则编码 |
| `rule_version` | 规则版本 |
| `patient_id` | 患者ID |
| `encounter_id` | 就诊ID |
| `hit` | 是否命中 |
| `severity` | 命中严重程度 |
| `message` | 提示消息 |
| `elapsed_ms` | 执行耗时（毫秒） |
| `result_status` | 执行结果状态 |
| `error_code` | 错误码（执行失败时） |
| `error_message` | 错误信息（执行失败时） |
| `actions` | 触发的动作列表 |
| `evidence` | 证据引用 |
| `created_time` | 执行时间 |

#### 执行日志汇总

```
GET /api/rules/exec-logs/summary
```

返回聚合统计数据：

| 字段 | 说明 |
|------|------|
| `total` | 总执行次数 |
| `hit_count` | 命中次数 |
| `miss_count` | 未命中次数 |
| `error_count` | 错误次数 |
| `avg_elapsed_ms` | 平均执行耗时（毫秒） |
| `by_rule` | 按规则分组的统计明细 |

#### 决策日志

当规则命中后，医生对提醒做出的决策（继续、修改、取消）会被记录：

```
POST /api/rule-action-logs
```

```json
{
  "rule_code": "R_ORDER_SAFETY_DUP_ANTIBIOTIC",
  "rule_version": "1.0.0",
  "patient_id": "P001",
  "encounter_id": "E001",
  "order_id": "ORD001",
  "action_mode": "BLOCK",
  "decision": "MODIFY",
  "decision_by": "DR_ZHANG",
  "reason": "更换为其他抗菌药物",
  "informed_consent": true,
  "family_notified": false
}
```

决策类型说明：

| 决策 | 编码 | 说明 |
|------|------|------|
| 继续 | `CONTINUE` | 医生确认继续执行原医嘱 |
| 修改 | `MODIFY` | 医生修改医嘱内容 |
| 取消 | `CANCEL` | 医生取消医嘱 |

---

## 4. 最佳实践

### 4.1 规则命名规范

规则编码采用 `R_{场景}_{类别}_{具体描述}` 的格式：

| 场景前缀 | 说明 | 示例 |
|---------|------|------|
| `R_ORDER_SAFETY_` | 医嘱安全 | `R_ORDER_SAFETY_DUP_ANTIBIOTIC` |
| `R_EMR_` | 病历质控 | `R_EMR_DISCHARGE_SUMMARY_COMPLETE` |
| `R_INS_` | 医保合规 | `R_INS_DRUG_INDICATION_MISMATCH` |
| `R_AMI_` | AMI专项 | `R_AMI_STEMI_CANDIDATE` |
| `R_AMI_ECG_` | AMI心电图 | `R_AMI_ECG_TIMELY` |
| `R_AMI_THROMBOLYSIS_` | AMI溶栓 | `R_AMI_THROMBOLYSIS_CONTRA_BLOCK` |

命名要求：
- 使用大写字母和下划线分隔
- 编码应能体现规则的业务含义
- 同一场景的规则使用统一前缀，便于管理和检索
- 规则名称使用简洁的中文描述

### 4.2 规则优先级设置建议

优先级数值越高，规则越先执行。建议按以下范围设置：

| 优先级范围 | 适用场景 | 示例 |
|-----------|---------|------|
| 120-150 | 安全拦截类（硬拦截） | 溶栓禁忌证拦截：130 |
| 100-119 | 安全提醒类（软拦截） | 重复抗菌药物提醒：110 |
| 80-99 | 质控检查类 | 出院小结完整性：90 |
| 60-79 | 路径推荐类 | AMI候选入径推荐：70 |
| 40-59 | 信息提示类 | 随访提醒：50 |

### 4.3 规则测试流程

发布前必须完成以下测试步骤：

1. **单元模拟**：使用模拟数据测试规则条件逻辑
   - 构造命中场景数据，验证规则正确命中
   - 构造未命中场景数据，验证规则不会误报
   - 构造边界值数据，验证条件判断的准确性

2. **场景评估**：使用场景化评估接口批量测试
   ```
   POST /api/rules/evaluate
   ```
   - 指定 `scenario_code`，使用真实脱敏数据验证
   - 检查 `evaluated_count` 和 `hit_count` 是否符合预期

3. **集成验证**：在测试环境中通过触发点验证端到端流程
   - 模拟HIS系统发送触发事件
   - 验证规则引擎接收事件并正确执行
   - 验证提醒消息正确推送到医生端

### 4.4 规则发布审批流程

建议遵循以下审批流程：

1. **规则作者**：完成规则编写和模拟测试 → 保存为草稿
2. **科室质控员**：审核规则的临床合理性 → 确认审核
3. **信息科管理员**：审核规则的技术实现和影响范围 → 批准发布
4. **发布执行**：系统自动将规则部署到生产环境

审批要点：
- 安全拦截类规则必须由科室主任审核
- 涉及多科室的规则需征求相关科室意见
- 发布后观察3-5天执行日志，确认无异常

---

## 5. 常见问题

### 5.1 规则不生效排查

**现象**：规则已发布但未触发提醒

**排查步骤**：

1. **检查规则状态**：确认规则状态为 `PUBLISHED`，且 `enabled` 为 `true`
2. **检查触发点配置**：确认 `cdss_trigger_point` 表中对应触发点已启用（`enabled = TRUE`），且 `rule_codes` 包含该规则编码
3. **检查执行日志**：查询 `GET /api/rules/exec-logs?rule_code={ruleCode}`，确认规则是否被调用
   - 如果日志为空：触发点未正确配置或HIS系统未发送触发事件
   - 如果日志存在但 `hit = false`：条件不满足，检查事实数据
   - 如果日志存在但 `result_status = ERROR`：规则执行出错，查看 `error_message`
4. **检查组织范围**：确认规则的作用域（`scope_level`/`scope_code`）与当前科室匹配
5. **检查适配器**：如果规则依赖外部数据（`data_requirements`），确认适配器（`adapter_code`）和查询（`query_code`）配置正确且可用

### 5.2 规则冲突处理

**现象**：多条规则对同一医嘱给出矛盾提醒

**处理方法**：

1. **优先级调整**：提高更重要规则的优先级，确保关键安全规则先执行
2. **条件互斥**：检查冲突规则的条件是否存在重叠，通过调整条件使规则互斥
3. **动作模式统一**：对于同一场景的规则，统一动作模式（如安全类统一使用 `BLOCK`）
4. **规则拆分**：将复杂规则拆分为多条简单规则，分别处理不同场景

### 5.3 规则性能优化

**现象**：规则执行耗时过长，影响医生操作体验

**优化建议**：

1. **减少数据依赖**：精简 `data_requirements`，只请求必要的数据
2. **优化条件逻辑**：将高命中率条件放在 `all` 前面，利用短路求值提前退出
3. **设置超时时间**：在触发点配置合理的 `timeout_ms`（建议 3000-5000ms）
4. **异步执行**：非紧急提醒类规则可改为消息队列异步触发（`access_strategy = MESSAGE`）
5. **监控执行耗时**：通过执行日志汇总 `avg_elapsed_ms` 识别慢规则，针对性优化
6. **规则瘦身**：避免单条规则包含过多条件，拆分为多条规则分别执行

---

## 附录

### A. 规则评估场景编码

| 场景编码 | 说明 |
|---------|------|
| `PATHWAY_ENTRY` | 临床路径入径评估 |
| `AMI_RECOMMEND` | AMI路径推荐 |
| `EMR_QC` | 病历质控 |
| `INSURANCE_QC` | 医保合规质控 |
| `ORDER_SAFETY` | 医嘱安全检查 |
| `FOLLOWUP` | 随访提醒 |
| `OPERATION` | 运营管理 |

### B. 通用错误码

| 错误码 | 说明 |
|--------|------|
| `SUCCESS` | 执行成功 |
| `VALIDATION_ERROR` | 参数校验失败 |
| `DATA_MISSING` | 必要数据缺失 |
| `CONFIG_NOT_FOUND` | 配置未找到 |
| `ENGINE_TIMEOUT` | 规则引擎超时 |
| `ADAPTER_TIMEOUT` | 适配器超时 |
| `NO_RULES_MATCHED` | 无匹配规则 |
| `MISSING_SOURCE` | 来源信息缺失 |
| `UNAUTHORIZED` | 未授权 |
| `FORBIDDEN` | 无权限 |

### C. 规则示例参考

系统内置以下示例规则，可在规则列表中查看：

| 规则编码 | 规则名称 | 类型 |
|---------|---------|------|
| `R_AMI_STEMI_CANDIDATE` | AMI/STEMI候选入径规则 | PATHWAY_NODE |
| `R_AMI_ECG_TIMELY` | 胸痛患者10分钟内完成心电图 | TIME_LIMIT_QC |
| `R_AMI_THROMBOLYSIS_CONTRA_BLOCK` | 存在溶栓禁忌证时拦截溶栓医嘱 | SAFETY |
| `R_EMR_DISCHARGE_SUMMARY_COMPLETE` | 出院小结主诉与诊断必须完整 | CONTENT_QC |
| `R_INS_DRUG_INDICATION_MISMATCH` | 医保限定适应症与诊断不一致 | SAFETY |
| `R_ORDER_SAFETY_DUP_ANTIBIOTIC` | 重复抗菌药物医嘱拦截 | SAFETY |
