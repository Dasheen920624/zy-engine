# 质控治理模块用户手册

> MedKernel 医疗知识平台 — 质控治理模块
> 适用对象：医院信息科运维人员
> 版本：v1.0

---

## 1. 模块概述

质控治理模块是 MedKernel 平台的核心业务模块，负责医疗质量指标的量化评估、质控告警处理、评估报告生成与整改闭环管理，以及 AI 模型的安全治理。

### 1.1 模块价值

| 价值维度 | 说明 |
|---------|------|
| **量化质控** | 通过加权指标集和评分引擎，将医疗质量量化为可度量、可比较的分数 |
| **实时告警** | 实时监控质控问题，按严重程度分级告警，确保关键问题不被遗漏 |
| **闭环整改** | 从问题发现到整改完成再到再评估，形成完整的质控闭环 |
| **AI 安全** | 对 AI 模型进行红队测试和幻觉检测，确保 AI 辅助决策的安全性 |
| **合规验收** | 提供完整的验收测试和证据归档能力，满足合规审计要求 |

### 1.2 模块架构

```
质量仪表盘（总览/科室下钻/趋势）
       │
  质量告警 ←── 问题分派
       │
  评估指标集 → 评分引擎 → 评估报告
       │              │
  报告复核 ←── 整改任务 ←─┘
       │
  再评估 ──→ 闭环完成
       │
  AI模型治理 ──→ AI安全（红队/幻觉检测）
       │
  验收测试 ──→ 证据归档
```

---

## 2. 功能清单

| 序号 | 功能 | 说明 | API 前缀 |
|------|------|------|----------|
| 1 | 质量仪表盘 | 院级质控驾驶舱，展示 KPI、科室排名、趋势分析 | `/api/quality/dashboard` |
| 2 | 质量告警 | 质控问题告警列表、汇总统计、告警处理 | `/api/quality/alerts` |
| 3 | 问题分派 | 将质控问题分派给责任人，跟踪处理进度 | `/api/quality/problems` |
| 4 | 评估指标集 | 创建和管理评估指标集，配置加权指标 | `/api/quality/eval/sets` |
| 5 | 评分引擎 | 基于指标集执行加权评分，输出风险等级 | `/api/quality/eval/evaluate` |
| 6 | 评估报告 | 生成、导出、归档评估报告 | `/api/quality/eval/report` |
| 7 | 报告复核 | 人工复核报告，创建整改任务 | `/api/quality/eval/report/{id}/review` |
| 8 | 整改任务 | 创建和管理整改任务，跟踪整改进度 | `/api/quality/eval/report/rectification` |
| 9 | 再评估 | 整改完成后重新评估 | `/api/quality/eval/report/re-evaluate` |
| 10 | AI 模型治理 | 注册、审核、上线/下线 AI 模型 | `/api/ai-governance/models` |
| 11 | 提示词模板 | 创建、审核、发布提示词模板 | `/api/ai-governance/prompts` |
| 12 | AI 安全 | 红队测试、幻觉检测与防护 | `/api/ai-safety` |
| 13 | 验收测试 | 创建测试用例、记录结果、收集证据 | `/api/qa/acceptance` |

---

## 3. 操作指南

### 3.1 质量仪表盘

质量仪表盘提供院级质控驾驶舱视图，包含四大 KPI 维度、科室排名和趋势分析。

#### 四大 KPI 维度

| KPI 维度 | 指标项 | 说明 |
|---------|--------|------|
| **路径执行** | 入径人数、完成人数、变异率 | 监控临床路径执行情况 |
| **规则命中** | 实时拦截数、软提醒数、命中率 | 监控 CDSS 规则触发情况 |
| **质控问题** | 问题总数、已关闭数、整改率 | 监控质控问题处理情况 |
| **医保风险** | 潜在退款金额、风险等级 | 监控医保合规风险 |

#### 操作步骤

1. 进入 **质控治理 → 质量仪表盘** 页面
2. 顶部展示四大 KPI 卡片，每个卡片显示当前值和环比变化
3. 可通过时间周期筛选器切换查看周期
4. 可通过科室筛选器下钻到特定科室

#### 科室下钻

1. 在科室排名区域点击目标科室
2. 系统展示该科室的详细 KPI 数据
3. 包含：变异 TOP 列表、医生绩效排名

#### 趋势分析

1. 在趋势图表区域查看近 N 天的质控趋势
2. 支持查看路径完成率、规则命中率、整改率、医保风险金额四条趋势线
3. 可按科室筛选趋势数据

#### API 参考

```http
# 获取 KPI 数据
GET /api/quality/dashboard/kpis?period=2026-Q2&department_code=CARDIOLOGY

# 获取科室排名
GET /api/quality/dashboard/department-ranking?period=2026-Q2

# 获取趋势数据
GET /api/quality/dashboard/trend?days=30&department_code=CARDIOLOGY

# 获取科室详情（下钻）
GET /api/quality/department/CARDIOLOGY?period=2026-Q2
```

---

### 3.2 质量告警

质量告警模块实时展示质控问题告警，支持按严重程度和状态筛选。

#### 告警严重程度

| 级别 | 含义 | 处理要求 |
|------|------|---------|
| `CRITICAL` | 严重 | 必须在 24 小时内处理 |
| `WARNING` | 警告 | 建议在 72 小时内处理 |
| `INFO` | 信息 | 可择时处理 |

#### 告警状态

| 状态 | 含义 |
|------|------|
| `PENDING` | 待处理 |
| `IN_PROGRESS` | 处理中 |
| `RESOLVED` | 已解决 |

#### 操作步骤

1. 进入 **质控治理 → 质量告警** 页面
2. 顶部展示告警汇总：严重数、警告数、信息数、超时数
3. 在告警列表中可按科室、严重程度、日期、状态筛选
4. 点击告警可查看详情
5. 对告警进行处理（分派给责任人见 3.3 节）

#### API 参考

```http
# 告警列表
GET /api/quality/alerts?dept=CARDIOLOGY&severity=CRITICAL&status=PENDING&page=1&size=20

# 告警汇总
GET /api/quality/alerts/summary
```

---

### 3.3 问题分派

将质控告警分派给责任人，跟踪整改进度。

#### 操作步骤

1. 在质量告警列表中选择待处理的告警
2. 点击 **「分派」** 按钮
3. 填写分派信息：
   - 责任人（`assignee`）
   - 责任人角色（`assignee_role`）
   - 截止日期（`deadline`，可选）
   - 分派备注（`note`，可选）
4. 点击 **「确认分派」**
5. 告警状态变为 `IN_PROGRESS`

#### API 参考

```http
POST /api/quality/problems/{alertId}/assign
Content-Type: application/json

{
  "assignee": "dr_wang",
  "assignee_role": "ATTENDING",
  "deadline": "2026-06-01",
  "note": "请尽快处理此质控问题",
  "assigned_by": "admin"
}
```

---

### 3.4 评估指标集

评估指标集是质控评估的基础配置，定义了评估的维度、权重和阈值。

#### 指标集主体类型

| 类型 | 含义 |
|------|------|
| `EMR` | 病历质控 |
| `INSURANCE` | 医保质控 |
| `PATHWAY` | 路径质控 |
| `DEPARTMENT` | 科室质控 |
| `CONFIG` | 配置质控 |

#### 指标集状态

| 状态 | 含义 |
|------|------|
| `DRAFT` | 草稿，可编辑 |
| `PUBLISHED` | 已发布，可用于评估 |
| `DEPRECATED` | 已废弃 |

#### 创建指标集

1. 进入 **质控治理 → 评估指标集** 页面
2. 点击 **「创建指标集」** 按钮
3. 填写指标集信息：
   - 名称（`set_name`）
   - 主体类型（`subject_type`）
   - 描述（`description`）
   - 来源信息（`document_code`、`citation_id`、`binding_type`，用于溯源）
4. 点击 **「确认」**，指标集状态为 `DRAFT`

#### 添加指标

1. 在指标集详情页点击 **「添加指标」** 按钮
2. 填写指标信息（见下方字段说明）
3. 点击 **「确认」**

#### 指标字段说明

| 字段 | 是否必填 | 说明 |
|------|---------|------|
| 指标名称（`indicator_name`） | **是** | 指标的显示名称 |
| 指标类型（`indicator_type`） | **是** | 可选值：`SCORE`（评分）、`RATE`（比率）、`COUNT`（计数）、`BOOLEAN`（是否） |
| 权重（`weight`） | 否 | 指标权重，所有指标权重之和建议为 1.0 |
| 最大值（`max_value`） | 否 | 指标的最大可能值 |
| 阈值表达式（`threshold_expression`） | 否 | 判定阈值的表达式，如 `value >= 80` |
| 风险等级映射（`risk_level_mapping`） | 否 | 分值到风险等级的映射规则 |
| 计算表达式（`calc_expression`） | 否 | 指标的计算公式 |
| 单位（`unit`） | 否 | 指标值的单位，如"%"、"分" |
| 描述（`description`） | 否 | 指标的详细说明 |
| 来源信息 | 否 | 关联的来源文档、引用和绑定类型 |

#### 发布指标集

1. 确认指标集内所有指标配置正确
2. 点击 **「发布」** 按钮
3. 指标集状态变为 `PUBLISHED`，可用于执行评估

#### API 参考

```http
# 创建指标集
POST /api/quality/eval/sets
Content-Type: application/json

{
  "set_name": "病历质控指标集 V2",
  "subject_type": "EMR",
  "description": "2026年版病历质控评估指标集",
  "document_code": "DOC_EMR_QC_2026",
  "binding_type": "GUIDELINE"
}

# 添加指标
POST /api/quality/eval/sets/{setCode}/indicators
Content-Type: application/json

{
  "indicator_name": "入院记录24小时完成率",
  "indicator_type": "RATE",
  "weight": 0.15,
  "max_value": 100,
  "threshold_expression": "value >= 95",
  "risk_level_mapping": "{\">=95\":\"LOW\",\">=80\":\"MEDIUM\",\"<80\":\"HIGH\"}",
  "calc_expression": "completed_count / total_count * 100",
  "unit": "%"
}

# 发布指标集
POST /api/quality/eval/sets/{setCode}/publish

# 废弃指标集
POST /api/quality/eval/sets/{setCode}/deprecate

# 查询指标集列表
GET /api/quality/eval/sets?subject_type=EMR&status=PUBLISHED

# 查询指标集详情
GET /api/quality/eval/sets/{setCode}

# 查询指标列表
GET /api/quality/eval/sets/{setCode}/indicators
```

---

### 3.5 执行评估

基于已发布的指标集对目标对象执行评估，系统自动计算加权评分并输出风险等级。

#### 操作步骤

1. 进入 **质控治理 → 执行评估** 页面
2. 选择已发布的指标集（`set_code`）
3. 输入评估对象信息：
   - 对象ID（`subject_id`）
   - 对象名称（`subject_name`）
   - 输入数据（`input_data`）：各指标的实际值
4. 点击 **「执行评估」**
5. 系统自动计算并返回评估结果

#### 评估结果说明

| 字段 | 说明 |
|------|------|
| `eval_id` | 评估ID，用于后续生成报告 |
| `total_score` | 加权总分 |
| `max_possible_score` | 满分 |
| `score_percentage` | 得分百分比 |
| `risk_level` | 风险等级 |
| `indicator_scores` | 各指标的评分详情 |
| `abnormal_facts` | 异常事实列表 |
| `missing_facts` | 缺失事实列表 |

#### 指标评分详情

每条指标评分包含：

| 字段 | 说明 |
|------|------|
| `indicator_code` | 指标编码 |
| `indicator_name` | 指标名称 |
| `weight` | 权重 |
| `raw_score` | 原始得分 |
| `weighted_score` | 加权得分 |
| `max_value` | 满分值 |
| `risk_level` | 风险等级 |
| `threshold_met` | 是否达到阈值 |
| `explanation` | 评分说明 |

#### API 参考

```http
POST /api/quality/eval/evaluate
Content-Type: application/json

{
  "set_code": "SET_EMR_QC_V2",
  "subject_id": "ENC_20260501_001",
  "subject_name": "患者张三入院记录",
  "input_data": {
    "admission_record_24h_rate": 98.5,
    "discharge_summary_rate": 95.0,
    "diagnosis_consistency_rate": 92.3,
    "order_standard_rate": 88.0
  }
}
```

---

### 3.6 评估报告

基于评估结果生成正式的评估报告，支持导出和归档。

#### 报告状态

| 状态 | 含义 |
|------|------|
| `DRAFT` | 草稿 |
| `REVIEWED` | 已复核 |
| `REVIEW_REJECTED` | 复核拒绝 |
| `CONDITIONALLY_APPROVED` | 有条件通过 |
| `ARCHIVED` | 已归档 |

#### 生成报告

1. 进入 **质控治理 → 评估报告** 页面
2. 点击 **「生成报告」** 按钮
3. 输入评估ID（`eval_id`）
4. 系统自动生成报告，状态为 `DRAFT`

#### 导出报告

1. 在报告列表中选择目标报告
2. 点击 **「导出」** 按钮
3. 系统返回包含指标评分详情、异常事实、缺失事实的完整报告数据

#### 归档报告

1. 报告复核通过后，点击 **「归档」** 按钮
2. 报告状态变为 `ARCHIVED`，不可再修改

#### API 参考

```http
# 生成报告
POST /api/quality/eval/report/generate
Content-Type: application/json

{
  "eval_id": "EVAL_20260501_001"
}

# 导出报告
GET /api/quality/eval/report/{reportId}/export

# 查询报告
GET /api/quality/eval/report/{reportId}

# 报告列表
GET /api/quality/eval/report/list?status=DRAFT&subject_type=EMR

# 归档报告
POST /api/quality/eval/report/{reportId}/archive
```

---

### 3.7 报告复核

评估报告生成后需经人工复核，复核结果决定后续流程。

#### 复核结果

| 结果 | 含义 | 后续动作 |
|------|------|---------|
| `APPROVED` | 通过 | 可归档报告 |
| `REJECTED` | 拒绝 | 需修改后重新评估 |
| `CONDITIONALLY_APPROVED` | 有条件通过 | 需创建整改任务 |

#### 操作步骤

1. 进入 **质控治理 → 评估报告** 页面
2. 选择状态为 `DRAFT` 的报告
3. 点击 **「复核」** 按钮
4. 查看报告详情，包括各指标评分、异常事实、缺失事实
5. 选择复核结果，填写复核意见
6. 点击 **「确认」**

#### 创建整改任务

复核后如需整改，可手动或自动创建整改任务。

**手动创建**：

1. 在报告详情页点击 **「创建整改任务」** 按钮
2. 填写整改信息：
   - 标题（`title`）
   - 描述（`description`）
   - 责任人（`assignee_id`、`assignee_name`）
   - 优先级（`priority`）：`HIGH` / `MEDIUM` / `LOW`
   - 截止日期（`due_date`）
   - 关联事实（`related_facts`）
3. 点击 **「确认」**

**自动创建**：

1. 在报告详情页点击 **「自动创建整改」** 按钮
2. 系统根据异常事实和缺失事实自动生成整改任务

#### API 参考

```http
# 提交复核
POST /api/quality/eval/report/{reportId}/review
Content-Type: application/json

{
  "reviewer_id": "admin",
  "reviewer_name": "管理员",
  "review_result": "CONDITIONALLY_APPROVED",
  "review_comment": "部分指标未达标，需整改后复评"
}

# 查看复核记录
GET /api/quality/eval/report/{reportId}/reviews

# 手动创建整改任务
POST /api/quality/eval/report/{reportId}/rectification
Content-Type: application/json

{
  "title": "入院记录完成率整改",
  "description": "入院记录24小时完成率未达95%阈值",
  "assignee_id": "dr_wang",
  "assignee_name": "王医生",
  "priority": "HIGH",
  "due_date": "2026-06-15",
  "related_facts": "ABNORMAL:admission_record_24h_rate"
}

# 自动创建整改任务
POST /api/quality/eval/report/{reportId}/rectification/auto
```

---

### 3.8 整改任务管理

跟踪整改任务的执行进度，确保问题得到有效解决。

#### 整改任务状态

| 状态 | 含义 |
|------|------|
| `PENDING` | 待处理 |
| `IN_PROGRESS` | 进行中 |
| `COMPLETED` | 已完成 |

#### 优先级

| 优先级 | 含义 |
|--------|------|
| `HIGH` | 高优先级，需尽快处理 |
| `MEDIUM` | 中优先级 |
| `LOW` | 低优先级 |

#### 操作步骤

1. 进入 **质控治理 → 整改任务** 页面
2. 查看整改任务列表，可按报告ID和状态筛选
3. 点击任务可查看详情
4. 更新任务状态：
   - 开始处理：状态变为 `IN_PROGRESS`
   - 完成整改：状态变为 `COMPLETED`，填写处理备注

#### API 参考

```http
# 更新整改任务状态
POST /api/quality/eval/report/rectification/{rectId}/status
Content-Type: application/json

{
  "status": "COMPLETED",
  "updated_by": "dr_wang",
  "update_note": "已补充入院记录，完成率达96%"
}

# 查询整改任务列表
GET /api/quality/eval/report/rectification/list?report_id=RPT001&status=PENDING
```

---

### 3.9 再评估

整改完成后，对同一评估对象重新执行评估，验证整改效果。

#### 操作步骤

1. 进入 **质控治理 → 评估报告** 页面
2. 选择已整改的报告
3. 点击 **「再评估」** 按钮
4. 输入更新后的数据（`input_data`）
5. 系统基于同一指标集重新计算评分
6. 对比前后评估结果，确认整改效果

#### API 参考

```http
POST /api/quality/eval/report/re-evaluate
Content-Type: application/json

{
  "eval_id": "EVAL_20260501_001",
  "input_data": {
    "admission_record_24h_rate": 96.0,
    "discharge_summary_rate": 97.0,
    "diagnosis_consistency_rate": 94.5,
    "order_standard_rate": 91.0
  }
}
```

---

### 3.10 AI 模型治理

AI 模型治理模块负责管理平台上使用的 AI 模型，包括注册、审核、上线和下线。

#### 模型状态

| 状态 | 含义 |
|------|------|
| `REGISTERED` | 已注册，待审核 |
| `ACTIVE` | 已上线，正在使用 |
| `DEPRECATED` | 已降级，不再推荐使用 |
| `RETIRED` | 已下线，不可使用 |

#### 模型审核状态

| 状态 | 含义 |
|------|------|
| `PENDING` | 待审核 |
| `APPROVED` | 审核通过 |
| `REJECTED` | 审核拒绝 |

#### 注册模型

1. 进入 **质控治理 → AI 模型治理** 页面
2. 点击 **「注册模型」** 按钮
3. 填写模型信息：

| 字段 | 是否必填 | 说明 |
|------|---------|------|
| 模型编码（`model_code`） | **是** | 模型的唯一标识 |
| 模型名称（`model_name`） | **是** | 模型显示名称 |
| 模型供应商（`model_provider`） | **是** | 供应商：QIANWEN / DEEPSEEK / KIMI / ZHIPU / DOUBAO / YI / BAICHUAN / STEPFUN / OLLAMA_LOCAL |
| 模型版本（`model_version`） | 否 | 版本号 |
| 模型类型（`model_type`） | 否 | 模型类型 |
| 服务端点（`endpoint_url`） | 否 | API 地址 |
| API 密钥引用（`api_key_ref`） | 否 | 密钥的引用标识（不存储明文） |
| 超时时间（`timeout_ms`） | 否 | 默认 5000 毫秒 |
| 最大 Token 数（`max_tokens`） | 否 | 默认 4096 |
| 温度参数（`temperature`） | 否 | 默认 0.70 |
| 描述（`description`） | 否 | 模型用途说明 |

4. 点击 **「确认」**，模型状态为 `REGISTERED`，审核状态为 `PENDING`

#### 审核模型

1. 在模型列表中筛选审核状态为 `PENDING` 的模型
2. 点击 **「审核」** 按钮
3. 选择审核结果（通过/拒绝），填写审核备注
4. 审核通过后模型可上线

#### 上线/下线模型

- **上线**：审核通过的模型可设为 `ACTIVE` 状态，开始提供服务
- **下线**：不再使用的模型可设为 `RETIRED` 状态
- **降级**：出现问题的模型可设为 `DEPRECATED` 状态，系统将自动切换到降级链中的下一个模型

#### 模型评测

1. 在模型详情页点击 **「创建评测任务」** 按钮
2. 配置评测参数：
   - 评测基准（`benchmark_code`）
   - 样本数量（`sample_size`）
   - 关联的提示词模板
3. 执行评测，查看准确率（`accuracy_score`）、延迟（`latency_ms`）、通过率（`pass_rate`）

---

### 3.11 提示词模板管理

提示词模板用于标准化 AI 模型的输入，确保输出质量和一致性。

#### 模板状态

| 状态 | 含义 |
|------|------|
| `DRAFT` | 草稿 |
| `PUBLISHED` | 已发布 |
| `ARCHIVED` | 已归档 |

#### 模板审核状态

| 状态 | 含义 |
|------|------|
| `PENDING` | 待审核 |
| `APPROVED` | 审核通过 |
| `REJECTED` | 审核拒绝 |

#### 创建模板

1. 进入 **质控治理 → 提示词模板** 页面
2. 点击 **「创建模板」** 按钮
3. 填写模板信息：

| 字段 | 是否必填 | 说明 |
|------|---------|------|
| 模板编码（`template_code`） | **是** | 模板唯一标识 |
| 模板名称（`template_name`） | **是** | 模板显示名称 |
| 模板类型（`template_type`） | 否 | 如 EXTRACT / CRITIC / RESEARCH 等 |
| 适用模型类型（`model_type`） | 否 | 适用的模型类型 |
| 模板内容（`content`） | **是** | 提示词正文，支持变量占位符 |
| 版本（`version`） | 否 | 默认 `1.0.0` |
| 变量定义（`variables`） | 否 | 模板中使用的变量列表 |
| 描述（`description`） | 否 | 模板用途说明 |

4. 点击 **「确认」**，模板状态为 `DRAFT`

#### 审核与发布模板

1. 审核人员审核模板内容，确保提示词安全、合规
2. 审核通过后，点击 **「发布」** 按钮
3. 模板状态变为 `PUBLISHED`，可被 AI 任务引用

---

### 3.12 AI 安全

AI 安全模块提供红队测试和幻觉检测能力，确保 AI 模型在医疗场景中的安全性。

#### 3.12.1 红队测试

红队测试通过模拟攻击场景，检测 AI 模型的安全漏洞。

**攻击分类**：

| 分类 | 含义 |
|------|------|
| `PROMPT_INJECTION` | 提示注入攻击 |
| `DATA_LEAKAGE` | 数据泄露 |
| `HALLUCINATION` | 幻觉生成 |
| `BIAS` | 偏见输出 |
| `SAFETY_BYPASS` | 安全绕过 |
| `MEDICAL_ERROR` | 医学错误 |

**测试场景状态**：

| 状态 | 含义 |
|------|------|
| `DRAFT` | 草稿 |
| `ACTIVE` | 启用 |
| `DISABLED` | 禁用 |

**操作步骤**：

1. 进入 **质控治理 → AI 安全 → 红队测试** 页面
2. 点击 **「创建场景」** 按钮
3. 填写场景信息：
   - 场景编码和名称
   - 攻击分类
   - 攻击提示词（`attack_prompt`）
   - 期望行为（`expected_behavior`）
   - 严重程度（`severity`）
4. 执行红队测试
5. 查看测试结果：
   - 判定（`verdict`）：`PASS` / `FAIL` / `UNCERTAIN`
   - 漏洞类型和详情
   - 修复建议（`remediation`）

**测试结果判定**：

| 判定 | 含义 |
|------|------|
| `PASS` | 模型正确抵御了攻击 |
| `FAIL` | 模型存在安全漏洞 |
| `UNCERTAIN` | 无法确定，需人工判断 |

#### 3.12.2 幻觉检测

幻觉检测用于识别 AI 模型输出中的虚假信息。

**检测类型**：

| 类型 | 含义 |
|------|------|
| `FACTUAL` | 事实性幻觉（编造不存在的事实） |
| `LOGICAL` | 逻辑性幻觉（推理错误） |
| `CONTEXTUAL` | 上下文幻觉（与输入不一致） |
| `REFERENTIAL` | 引用性幻觉（编造引用来源） |

**判定结果**：

| 判定 | 含义 |
|------|------|
| `HALLUCINATION` | 确认幻觉 |
| `LIKELY_HALLUCINATION` | 疑似幻觉 |
| `UNCERTAIN` | 不确定 |
| `SAFE` | 安全，无幻觉 |

**防护策略**：

| 策略 | 含义 |
|------|------|
| `BLOCK` | 直接拦截，不输出 |
| `DEGRADE` | 降级处理，添加警告标记 |
| `HUMAN_REVIEW` | 转人工审核 |
| `PASS` | 放行 |

**检测记录状态**：

| 状态 | 含义 |
|------|------|
| `DETECTED` | 已检测 |
| `REVIEWING` | 审核中 |
| `RESOLVED` | 已解决 |
| `DISMISSED` | 已忽略 |

**操作步骤**：

1. 进入 **质控治理 → AI 安全 → 幻觉检测** 页面
2. 查看检测记录列表，可按模型、判定结果、状态筛选
3. 点击记录查看详情：输入内容、输出内容、置信度、证据
4. 对检测结果进行人工审核：
   - 确认幻觉：标记为 `RESOLVED`
   - 误报：标记为 `DISMISSED`
5. 填写审核备注

---

### 3.13 验收测试

验收测试模块用于功能上线前的验收测试和证据归档，确保系统功能符合要求。

#### 测试用例分类

| 分类 | 含义 |
|------|------|
| `FUNCTIONAL` | 功能测试 |
| `SECURITY` | 安全测试 |
| `PERFORMANCE` | 性能测试 |
| `COMPLIANCE` | 合规测试 |
| `AI_SAFETY` | AI 安全测试 |

#### 测试用例状态

| 状态 | 含义 |
|------|------|
| `DRAFT` | 草稿 |
| `ACTIVE` | 启用 |
| `DISABLED` | 禁用 |

#### 测试结果判定

| 判定 | 含义 |
|------|------|
| `PASS` | 通过 |
| `FAIL` | 失败 |
| `BLOCKED` | 阻塞 |
| `SKIP` | 跳过 |

#### 测试结果状态

| 状态 | 含义 |
|------|------|
| `EXECUTED` | 已执行 |
| `REVIEWED` | 已审核 |
| `ACCEPTED` | 已验收 |
| `REJECTED` | 已拒绝 |

#### 证据类型

| 类型 | 含义 |
|------|------|
| `SCREENSHOT` | 截图 |
| `LOG` | 日志 |
| `REPORT` | 报告 |
| `VIDEO` | 视频 |
| `CONFIG_DUMP` | 配置导出 |

#### 创建测试用例

1. 进入 **质控治理 → 验收测试** 页面
2. 点击 **「创建用例」** 按钮
3. 填写用例信息：

| 字段 | 是否必填 | 说明 |
|------|---------|------|
| 用例编码（`case_code`） | **是** | 用例唯一标识 |
| 用例名称（`case_name`） | **是** | 用例显示名称 |
| 功能编码（`feature_code`） | 否 | 关联的功能模块编码 |
| 分类（`category`） | **是** | 测试分类 |
| 描述（`description`） | 否 | 用例描述 |
| 前置条件（`preconditions`） | 否 | 执行前置条件 |
| 测试步骤（`steps`） | 否 | JSON 格式的步骤列表 |
| 期望结果（`expected_result`） | 否 | 期望的测试结果 |
| 优先级（`priority`） | 否 | HIGH / MEDIUM / LOW |

4. 点击 **「确认」**

#### 记录测试结果

1. 选择测试用例，点击 **「执行」** 按钮
2. 填写测试结果：
   - 判定（`verdict`）：PASS / FAIL / BLOCKED / SKIP
   - 实际结果（`actual_result`）
   - 偏差描述（`deviation`）
   - 测试环境（`environment`）
3. 点击 **「确认」**

#### 收集证据

1. 在测试结果页面点击 **「添加证据」** 按钮
2. 选择证据类型，上传文件或填写描述
3. 系统自动记录文件哈希（`file_hash`），确保证据不可篡改

#### API 参考

```http
# 验收测试用例管理
POST /api/qa/acceptance/test-cases
GET  /api/qa/acceptance/test-cases?category=FUNCTIONAL&status=ACTIVE

# 记录测试结果
POST /api/qa/acceptance/test-results
GET  /api/qa/acceptance/test-results?case_code=TC001&verdict=FAIL

# 收集证据
POST /api/qa/acceptance/evidence
GET  /api/qa/acceptance/evidence?result_code=TR001
```

---

## 4. 最佳实践

### 4.1 指标设计原则

| 原则 | 说明 |
|------|------|
| **可量化** | 指标必须可量化计算，避免主观判断 |
| **权重合理** | 关键指标权重应高于一般指标，权重之和建议为 1.0 |
| **阈值明确** | 每个指标应设置清晰的阈值表达式，如 `value >= 95` |
| **风险映射** | 配置分值到风险等级的映射规则，便于快速判断 |
| **来源可溯** | 每个指标应关联来源文档和引用，确保有据可依 |
| **定期更新** | 根据政策变化和实际需求定期更新指标集 |

### 4.2 评估周期建议

| 评估类型 | 建议周期 | 说明 |
|---------|---------|------|
| 病历质控 | 每月 | 月度病历质量评估 |
| 医保质控 | 每季度 | 季度医保合规评估 |
| 路径质控 | 每月 | 月度路径执行质量评估 |
| 科室质控 | 每季度 | 季度科室综合质量评估 |
| AI 安全评估 | 每半年 | 半年度 AI 模型安全评估 |

### 4.3 整改闭环流程

```
1. 执行评估 → 发现问题
   ↓
2. 生成评估报告
   ↓
3. 人工复核报告
   ├─ 通过 → 归档
   ├─ 有条件通过 → 创建整改任务
   └─ 拒绝 → 修改后重新评估
   ↓
4. 执行整改任务
   ↓
5. 更新整改状态为 COMPLETED
   ↓
6. 再评估验证整改效果
   ↓
7. 整改有效 → 归档报告（闭环完成）
   整改无效 → 返回步骤 4
```

**关键要求**：

- 每个整改任务必须指定责任人和截止日期
- 高优先级整改任务的截止日期不超过 7 天
- 整改完成后必须在 3 个工作日内完成再评估
- 所有整改记录必须保留，不可删除

### 4.4 AI 模型上线审批

```
1. 注册模型 → 填写模型信息和参数
   ↓
2. 审核模型 → 技术审核 + 安全审核
   ↓
3. 模型评测 → 使用标准基准测试集评测
   ├─ 准确率达标（≥ 阈值）
   ├─ 延迟达标（≤ 阈值）
   └─ 通过率达标（≥ 阈值）
   ↓
4. 红队测试 → 执行安全场景测试
   ├─ 无 CRITICAL 级别漏洞
   └─ 无 MEDICAL_ERROR 类别漏洞
   ↓
5. 幻觉检测 → 运行幻觉检测
   ├─ 幻觉率低于阈值
   └─ 防护策略已配置
   ↓
6. 上线审批 → 审批通过后设为 ACTIVE
   ↓
7. 监控运行 → 持续监控模型表现
   ├─ 异常时自动降级（DEPRECATED）
   └─ 严重问题时下线（RETIRED）
```

**上线前置条件**：

| 条件 | 要求 |
|------|------|
| 模型审核 | 必须通过技术审核和安全审核 |
| 评测结果 | 准确率 ≥ 0.85，通过率 ≥ 0.90 |
| 红队测试 | 无 CRITICAL/HIGH 级别 FAIL |
| 幻觉检测 | 幻觉率 ≤ 5% |
| 降级链配置 | 必须配置降级链，确保故障时可切换 |
| 提示词模板 | 必须关联已审核通过的提示词模板 |

---

## 5. 常见问题

### 5.1 评估结果异常排查

**现象**：评估得分异常偏低或偏高，与预期不符。

**排查步骤**：

1. 检查输入数据（`input_data`）是否正确
   - 确认各指标值的单位和范围是否与指标定义一致
   - 确认数值类型正确（整数/小数）
2. 检查指标集配置
   - 确认权重分配是否合理
   - 确认计算表达式（`calc_expression`）语法正确
   - 确认阈值表达式（`threshold_expression`）逻辑正确
3. 检查风险等级映射（`risk_level_mapping`）
   - 确认映射规则的 JSON 格式正确
   - 确认映射范围覆盖所有可能的分值区间
4. 查看各指标评分详情（`indicator_scores`），定位异常指标
5. 查看异常事实（`abnormal_facts`）和缺失事实（`missing_facts`），了解具体问题

### 5.2 整改任务超时处理

**现象**：整改任务超过截止日期仍未完成。

**处理步骤**：

1. 在整改任务列表中筛选超时任务（`due_date` < 当前日期 且 状态非 `COMPLETED`）
2. 联系责任人了解延期原因
3. 根据情况选择处理方式：
   - **延期**：更新截止日期，继续跟踪
   - **更换责任人**：重新分派给其他人员
   - **升级处理**：将问题升级至科室负责人
4. 更新整改任务备注，记录延期原因和处理决定
5. 对多次延期的任务，建议在科室质控会议中通报

### 5.3 AI 模型降级处理

**现象**：AI 模型被自动降级（状态变为 `DEPRECATED`）或手动降级。

**处理步骤**：

1. 确认降级原因：
   - 查看模型调用日志（`ai_model_call_log`）中的错误信息
   - 检查模型端点（`endpoint_url`）是否可访问
   - 检查 API 密钥是否有效
2. 确认降级链是否正常工作：
   - 查看降级链配置，确认有备用模型
   - 检查备用模型状态是否为 `ACTIVE`
3. 修复问题后：
   - 重新评测模型，确认性能恢复
   - 执行红队测试，确认安全性
   - 审核通过后重新上线
4. 如模型无法恢复：
   - 将模型设为 `RETIRED`
   - 从降级链中移除该模型
   - 配置新的替代模型

**降级链说明**：

系统支持 6 种调用类型的降级链：`RESEARCH`、`EXTRACT`、`EMBEDDING`、`RERANK`、`CRITIC`、`WORKFLOW`。每种类型配置了多个 Provider 的优先级顺序，当首选 Provider 不可用时自动切换到下一个。

| 调用类型 | 用途 | 默认首选 Provider |
|---------|------|------------------|
| `RESEARCH` | 医学研究综合 | QIANWEN / DEEPSEEK |
| `EXTRACT` | 信息抽取 | DEEPSEEK / QIANWEN |
| `EMBEDDING` | 向量嵌入 | QIANWEN |
| `RERANK` | 二阶段重排 | ZHIPU |
| `CRITIC` | AI 评审 | DEEPSEEK / QIANWEN |
| `WORKFLOW` | 多步工作流 | DIFY |

可通过 `GET /api/model-gateway/degradation-chains` 查看当前降级链配置。
