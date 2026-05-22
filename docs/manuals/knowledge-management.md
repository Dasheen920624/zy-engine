# 知识治理模块用户手册

> MedKernel 医疗知识平台 — 知识治理模块
> 适用对象：医院信息科运维人员
> 版本：v1.0

---

## 1. 模块概述

知识治理模块是 MedKernel 平台的核心基础模块，负责医疗知识全生命周期的管理，包括知识来源注册、审核、订阅、同步、AI 辅助生产、质量检查和来源溯源。

### 1.1 模块价值

| 价值维度 | 说明 |
|---------|------|
| **知识可信** | 所有知识资产必须关联经审核的来源，确保医学内容的权威性和可追溯性 |
| **自动同步** | 支持按订阅计划自动同步知识来源更新，减少人工干预 |
| **AI 辅助** | 利用 AI 模型自动抽取、映射、生成知识候选，经人工审核后入库 |
| **质量可控** | 内置资产质量检查引擎，自动发现缺失来源、过期知识、规则冲突等问题 |
| **跨院流转** | 通过知识包导出/导入/同步机制，实现多院区间的知识共享 |

### 1.2 模块架构

```
知识来源注册 ──→ 来源审核 ──→ 知识订阅 ──→ 知识同步 ──→ 同步审核
     │                                              │
     │              AI知识生产任务 ←──────────────────┘
     │                    │
     │              AI候选审核 ←── 质量检查
     │                    │
     └──→ 来源溯源（文档/引用/绑定）←────────────────┘
                    │
              知识包管理（导出/导入/同步）
```

---

## 2. 功能清单

| 序号 | 功能 | 说明 | API 前缀 |
|------|------|------|----------|
| 1 | 知识来源注册 | 注册和管理知识来源（指南/教材/数据库/临床路径等） | `/api/knowledge/sources` |
| 2 | 来源审核 | 对注册的知识来源进行审核，通过后方可使用 | `/api/knowledge/sources/{sourceCode}/review` |
| 3 | 知识订阅 | 按主题订阅知识更新，支持自动同步 | `/api/knowledge/subscriptions` |
| 4 | 知识同步 | 手动或自动触发知识来源同步，支持全量/增量/试运行 | `/api/knowledge/sync` |
| 5 | 同步审核 | 预览同步差异，审核通过后执行实际同步 | `/api/knowledge/sync/{logId}/review` |
| 6 | 知识包管理 | 导出/导入/同步知识包，支持跨租户知识流转 | `/api/knowledge/packages` |
| 7 | 资产质量检查 | 对知识资产运行质量检查，发现并处理问题 | `/api/quality/findings` |
| 8 | AI 知识任务 | 创建和管理 AI 辅助知识抽取、映射、规则生成等任务 | `/api/knowledge/ai-jobs` |
| 9 | AI 候选审核 | 审核 AI 生成的知识候选，支持单个和批量审核 | `/api/knowledge/candidates` |
| 10 | 来源溯源 | 管理来源文档、引用片段和资产绑定，实现全链路追溯 | `/api/provenance` |

---

## 3. 操作指南

### 3.1 注册知识来源

知识来源是所有知识资产的可信根基。注册来源后需经审核通过，方可被其他模块引用。

#### 操作步骤

1. 进入 **知识治理 → 知识来源** 页面
2. 点击 **「注册来源」** 按钮
3. 填写来源信息表单（见下方字段说明）
4. 点击 **「提交」**，来源状态变为 `PENDING`（待审核）
5. 等待审核人员审核

#### 字段说明

| 字段 | 是否必填 | 说明 |
|------|---------|------|
| 来源编码（`source_code`） | 否 | 系统自动生成，也可手动指定唯一编码 |
| 来源名称（`source_name`） | **是** | 知识来源的显示名称，如"中国急性ST段抬高型心肌梗死诊断和治疗指南" |
| 来源类型（`source_type`） | **是** | 可选值：`GUIDELINE`（指南）、`TEXTBOOK`（教材）、`DATABASE`（数据库）、`CLINICAL_PATHWAY`（临床路径） |
| 发布者（`publisher`） | 否 | 来源发布机构，如"中华医学会心血管病学分会" |
| 地区（`region`） | 否 | 适用地区，如"CN" |
| 语言（`language`） | 否 | 来源语言，如"zh-CN" |
| 发布版本（`release_version`） | 否 | 来源版本号，如"2024版" |
| 发布日期（`release_date`） | 否 | 来源正式发布日期 |
| 生效日期（`effective_date`） | 否 | 来源开始生效的日期 |
| 失效日期（`expiry_date`） | 否 | 来源失效日期，过期后系统将标记告警 |
| 权威等级（`authority_level`） | 否 | 来源的权威性评级，用于排序和筛选 |
| 许可范围（`license_scope`） | 否 | 许可使用范围描述 |
| 许可类型（`license_type`） | 否 | 许可证类型 |
| 允许再分发（`redistribution_allowed`） | 否 | 是否允许将来源内容再分发 |
| 允许商业使用（`commercial_use_allowed`） | 否 | 是否允许商业用途 |
| 允许导出（`export_allowed`） | 否 | 是否允许导出 |
| 获取方式（`fetch_method`） | 否 | 知识获取方式，如"API"、"文件上传" |
| 来源地址（`source_uri`） | 否 | 知识来源的 URL 或文件路径 |
| 描述（`description`） | 否 | 来源的详细描述 |

#### API 参考

```http
POST /api/knowledge/sources
Content-Type: application/json

{
  "source_name": "中国急性ST段抬高型心肌梗死诊断和治疗指南",
  "source_type": "GUIDELINE",
  "publisher": "中华医学会心血管病学分会",
  "release_version": "2024版",
  "authority_level": "NATIONAL",
  "license_scope": "院内使用",
  "export_allowed": false,
  "description": "2024年修订版AMI诊疗指南"
}
```

---

### 3.2 审核知识来源

所有新注册的知识来源需经审核后方可使用。

#### 审核流程

```
注册来源 → PENDING（待审核） → APPROVED（已通过） / REJECTED（已拒绝）
                                      ↓
                               DEPRECATED（已废弃）
```

#### 审核状态说明

| 状态 | 含义 |
|------|------|
| `PENDING` | 待审核，刚注册或修改后等待审核 |
| `APPROVED` | 审核通过，可作为知识来源被引用 |
| `REJECTED` | 审核拒绝，需修改后重新提交 |
| `DEPRECATED` | 已废弃，不再使用但保留记录 |

#### 操作步骤

1. 进入 **知识治理 → 知识来源** 页面
2. 筛选状态为 `PENDING` 的来源
3. 点击目标来源的 **「审核」** 按钮
4. 选择审核结果：**通过** 或 **拒绝**
5. （可选）填写审核备注
6. 点击 **「确认」**

#### API 参考

```http
POST /api/knowledge/sources/{sourceCode}/review
Content-Type: application/json

{
  "review_status": "APPROVED",
  "reviewed_by": "zhangsan"
}
```

---

### 3.3 创建知识订阅

知识订阅用于按主题关注知识来源的更新，可配置自动同步频率。

#### 操作步骤

1. 进入 **知识治理 → 知识订阅** 页面
2. 点击 **「创建订阅」** 按钮
3. 填写订阅信息
4. 点击 **「提交」**

#### 字段说明

| 字段 | 是否必填 | 说明 |
|------|---------|------|
| 订阅者ID（`subscriber_id`） | 否 | 订阅者标识 |
| 订阅者名称（`subscriber_name`） | 否 | 订阅者显示名称 |
| 主题类型（`topic_type`） | **是** | 可选值见下表 |
| 主题编码（`topic_code`） | 否 | 主题的具体编码，如疾病ICD编码 |
| 主题名称（`topic_name`） | **是** | 主题显示名称 |
| 来源类型（`source_types`） | 否 | 限定的来源类型列表 |
| 自动同步（`auto_sync`） | 否 | 是否开启自动同步，默认 `false` |
| 同步频率（`sync_frequency`） | 否 | 可选值：`DAILY`（每日）、`WEEKLY`（每周）、`MONTHLY`（每月）、`MANUAL`（手动） |

#### 主题类型说明

| 主题类型 | 含义 | 示例 |
|---------|------|------|
| `DISEASE` | 疾病 | 急性心肌梗死 |
| `DEPARTMENT` | 科室 | 心内科 |
| `GUIDELINE` | 指南 | AMI诊疗指南 |
| `INSURANCE` | 医保 | 医保目录变更 |
| `DRUG` | 药品 | 抗凝药物更新 |
| `QUALITY` | 质控 | 质控指标更新 |

#### 订阅状态管理

| 操作 | 说明 | API |
|------|------|-----|
| 暂停订阅 | 临时停止自动同步 | `POST /api/knowledge/subscriptions/{id}/pause` |
| 取消订阅 | 永久取消订阅 | `POST /api/knowledge/subscriptions/{id}/cancel` |
| 修改订阅 | 更新订阅配置 | `PUT /api/knowledge/subscriptions/{id}` |

#### API 参考

```http
POST /api/knowledge/subscriptions
Content-Type: application/json

{
  "topic_type": "DISEASE",
  "topic_code": "I21.9",
  "topic_name": "急性心肌梗死",
  "source_types": ["GUIDELINE", "CLINICAL_PATHWAY"],
  "auto_sync": true,
  "sync_frequency": "WEEKLY"
}
```

---

### 3.4 知识同步

知识同步用于将外部知识来源的更新拉取到系统中。支持手动触发和自动同步两种方式。

#### 同步模式

| 模式 | 说明 | 适用场景 |
|------|------|---------|
| `FULL`（全量同步） | 重新拉取来源的全部知识内容 | 首次同步、数据修复 |
| `INCREMENTAL`（增量同步） | 仅拉取上次同步后的变更 | 日常更新 |
| `DRY_RUN`（试运行） | 模拟同步过程，不实际写入数据 | 预估变更影响 |

#### 同步状态流转

```
PENDING → RUNNING → DIFF_READY → APPROVED → SYNCING → COMPLETED
   │         │          │            │
   │         ↓          ↓            ↓
   │       FAILED    REJECTED     FAILED
   ↓
CANCELLED
```

| 状态 | 含义 |
|------|------|
| `PENDING` | 已创建，等待执行 |
| `RUNNING` | 正在执行同步 |
| `DIFF_READY` | 差异已计算完成，等待审核 |
| `APPROVED` | 审核通过，等待执行实际同步 |
| `SYNCING` | 正在执行实际数据写入 |
| `COMPLETED` | 同步完成 |
| `FAILED` | 同步失败 |
| `CANCELLED` | 已取消 |

#### 手动触发同步

1. 进入 **知识治理 → 知识同步** 页面
2. 点击 **「触发同步」** 按钮
3. 选择来源编码和同步模式
4. 点击 **「确认」**
5. 系统开始执行同步，状态变为 `RUNNING`

#### 自动同步

- 开启自动同步的订阅会按配置的频率自动触发同步
- 也可通过 API 手动触发所有自动同步订阅：`POST /api/knowledge/sync/auto`

#### API 参考

```http
# 手动触发同步
POST /api/knowledge/sync
Content-Type: application/json

{
  "sourceCode": "GUIDELINE_AMI_2024",
  "subscriptionId": "SUB_001",
  "syncMode": "INCREMENTAL"
}

# 查询同步列表
GET /api/knowledge/sync?sourceCode=GUIDELINE_AMI_2024&status=COMPLETED&limit=20

# 查询同步详情
GET /api/knowledge/sync/{logId}

# 重试失败的同步
POST /api/knowledge/sync/{logId}/retry

# 取消同步
POST /api/knowledge/sync/{logId}/cancel

# 同步统计汇总
GET /api/knowledge/sync/summary
```

---

### 3.5 同步审核

同步执行完成后（状态为 `DIFF_READY`），需审核同步差异后方可执行实际数据写入。

#### 操作步骤

1. 进入 **知识治理 → 知识同步** 页面
2. 筛选状态为 `DIFF_READY` 的同步记录
3. 点击目标记录的 **「预览差异」** 按钮
4. 查看差异摘要：新增条目数（`items_added`）、更新条目数（`items_updated`）、删除条目数（`items_deleted`）
5. 选择审核结果：
   - **批准**：审核通过，执行实际同步
   - **拒绝**：拒绝本次同步，不写入数据
6. 点击 **「确认」**

#### API 参考

```http
# 预览差异
POST /api/knowledge/sync/{logId}/preview

# 审核差异
POST /api/knowledge/sync/{logId}/review
Content-Type: application/json

{
  "reviewStatus": "APPROVED",
  "reviewedBy": "zhangsan",
  "reviewComment": "差异已确认，批准同步"
}

# 审核通过后执行实际同步
POST /api/knowledge/sync/{logId}/approve
```

---

### 3.6 知识包管理

知识包用于在不同租户（院区）之间导出、导入和同步知识资产。

#### 知识包状态

| 状态 | 含义 |
|------|------|
| `DRAFT` | 草稿，正在编辑 |
| `PUBLISHED` | 已发布，可被导入 |
| `IMPORTED` | 已导入目标租户 |
| `ARCHIVED` | 已归档 |

#### 导出类型

| 类型 | 说明 |
|------|------|
| `FULL`（全量导出） | 导出所有知识资产 |
| `INCREMENTAL`（增量导出） | 仅导出指定版本后的变更 |

#### 冲突策略

| 策略 | 说明 |
|------|------|
| `SKIP`（跳过） | 遇到冲突时跳过，保留目标端数据（默认） |
| `OVERWRITE`（覆盖） | 遇到冲突时用源端数据覆盖目标端 |
| `MERGE`（合并） | 尝试合并两端数据 |

#### 同步状态

| 状态 | 含义 |
|------|------|
| `IDLE` | 空闲 |
| `SYNCING` | 正在同步 |
| `SUCCESS` | 同步成功 |
| `FAILED` | 同步失败 |

#### 导出知识包

1. 进入 **知识治理 → 知识包** 页面
2. 点击 **「导出知识包」** 按钮
3. 填写导出信息：包编码、名称、版本、导出类型
4. 指定目标租户信息（可选）
5. 选择冲突策略
6. 点击 **「确认导出」**

#### 导入知识包

1. 进入 **知识治理 → 知识包** 页面
2. 选择已发布状态的知识包
3. 点击 **「预览导入」**，查看导入影响
4. 确认后点击 **「执行导入」**

#### 同步知识包

1. 选择知识包，点击 **「同步」** 按钮
2. 选择同步模式（`MANUAL` / `AUTO`）
3. 查看同步状态

#### API 参考

```http
# 导出知识包
POST /api/knowledge/packages/export
Content-Type: application/json

{
  "package_code": "PKG_AMI_2024",
  "package_name": "AMI知识包",
  "package_version": "1.0.0",
  "export_type": "FULL",
  "target_tenant_id": "2001",
  "target_tenant_name": "XX医院东院区",
  "conflict_strategy": "SKIP"
}

# 预览导入
POST /api/knowledge/packages/{packageId}/preview

# 执行导入
POST /api/knowledge/packages/{packageId}/import
Content-Type: application/json

{
  "conflictStrategy": "SKIP"
}

# 同步知识包
POST /api/knowledge/packages/{packageId}/sync
Content-Type: application/json

{
  "syncMode": "MANUAL"
}

# 查询同步状态
GET /api/knowledge/packages/{packageId}/sync-status

# 知识包列表
GET /api/knowledge/packages?status=PUBLISHED

# 知识包详情
GET /api/knowledge/packages/{packageId}
```

---

### 3.7 资产质量检查

资产质量检查用于自动检测知识资产中的问题，如缺失来源、过期知识、规则冲突等。

#### 发现类型

| 发现类型 | 含义 |
|---------|------|
| `MISSING_SOURCE` | 缺少来源引用 |
| `EXPIRED` | 知识已过期 |
| `UNCLEAR_AUTH` | 权威等级不明确 |
| `RULE_CONFLICT` | 规则冲突 |
| `LOW_CONFIDENCE` | AI 置信度过低 |
| `MULTI_CANDIDATE_CONFLICT` | 多候选冲突 |

#### 严重程度

| 级别 | 含义 |
|------|------|
| `INFO` | 信息提示 |
| `WARNING` | 警告，建议处理 |
| `CRITICAL` | 严重，必须处理 |

#### 发现状态

| 状态 | 含义 |
|------|------|
| `OPEN` | 待处理 |
| `ACKNOWLEDGED` | 已确认 |
| `RESOLVED` | 已解决 |
| `DISMISSED` | 已忽略 |

#### 操作步骤

1. 进入 **知识治理 → 质量检查** 页面
2. 点击 **「运行检查」**，系统自动扫描所有知识资产
3. 查看检查结果列表，按严重程度和类型筛选
4. 对每条发现进行处理：
   - **确认**：标记为已确认，安排后续处理
   - **解决**：修复问题后标记为已解决
   - **忽略**：确认无需处理后标记为已忽略

---

### 3.8 AI 知识任务

AI 知识任务用于创建和管理 AI 辅助的知识抽取、映射、规则生成等任务。

#### 任务类型

| 类型 | 含义 |
|------|------|
| `EXTRACT` | 信息抽取（从文档中抽取实体/关系/事实） |
| `MAP` | 术语映射（将非标准术语映射到标准码） |
| `RULE_GENERATE` | 规则生成（根据指南内容生成规则） |
| `GRAPH_BUILD` | 知识图谱构建 |
| `QUALITY_CHECK` | 质量检查 |

#### 任务状态

| 状态 | 含义 |
|------|------|
| `PENDING` | 待执行 |
| `RUNNING` | 执行中 |
| `SUCCESS` | 执行成功 |
| `FAILED` | 执行失败 |
| `RETRY` | 重试中 |
| `CANCELLED` | 已取消 |

#### 任务审核状态

| 状态 | 含义 |
|------|------|
| `PENDING` | 待审核 |
| `APPROVED` | 审核通过 |
| `REJECTED` | 审核拒绝 |

#### 操作步骤

1. 进入 **知识治理 → AI 任务** 页面
2. 点击 **「创建任务」** 按钮
3. 选择任务类型、关联的知识来源和 AI 模型
4. 点击 **「提交」**，任务状态变为 `PENDING`
5. 系统自动执行任务，状态变为 `RUNNING`
6. 执行完成后状态变为 `SUCCESS`，可查看输出摘要
7. 对任务结果进行审核（通过/拒绝）

#### 关键字段

| 字段 | 说明 |
|------|------|
| `job_code` | 任务编码 |
| `job_name` | 任务名称 |
| `job_type` | 任务类型 |
| `source_code` | 关联的知识来源编码 |
| `model_provider` | AI 模型供应商 |
| `model_name` | AI 模型名称 |
| `input_summary` | 输入摘要 |
| `output_summary` | 输出摘要 |
| `retry_count` | 已重试次数 |
| `max_retries` | 最大重试次数（默认 3） |
| `duration_ms` | 执行耗时（毫秒） |

---

### 3.9 AI 候选审核

AI 知识任务执行成功后，生成的知识候选需经人工审核后方可正式入库。

#### 候选审核状态

| 状态 | 含义 |
|------|------|
| `PENDING` | 待审核 |
| `APPROVED` | 审核通过 |
| `REJECTED` | 审核拒绝 |
| `MODIFIED` | 审核时修改后通过 |

#### 优先级

| 优先级 | 含义 |
|--------|------|
| `HIGH` | 高优先级，需优先审核 |
| `MEDIUM` | 中优先级（默认） |
| `LOW` | 低优先级 |

#### 单个审核

1. 进入 **知识治理 → AI 候选审核** 页面
2. 查看待审核候选列表，可按候选类型、审核状态、优先级筛选
3. 点击目标候选，查看详情：
   - 候选内容（`candidate_content`）
   - 来源信息（`source_code`、`source_name`）
   - AI 模型信息（`model_provider`、`model_name`）
   - 置信度（`confidence`）
   - 质检发现（`quality_findings`）
4. 选择审核操作：
   - **通过**：直接采纳 AI 生成的内容
   - **拒绝**：拒绝该候选
   - **修改后通过**：修改候选内容后采纳
5. 填写审核备注
6. 点击 **「确认」**

#### 批量审核

1. 在候选列表中勾选多个候选
2. 点击 **「批量审核」** 按钮
3. 选择批量审核结果（通过/拒绝）
4. 填写统一审核备注
5. 点击 **「确认」**

#### 审核汇总

页面顶部显示审核汇总统计：待审核数、已通过数、已拒绝数、修改后通过数。

#### API 参考

```http
# 查询候选列表
GET /api/knowledge/candidates?candidateType=RULE&reviewStatus=PENDING&priority=HIGH&limit=50

# 查询候选详情
GET /api/knowledge/candidates/{candidateId}

# 单个审核
POST /api/knowledge/candidates/{candidateId}/review
Content-Type: application/json

{
  "reviewStatus": "APPROVED",
  "reviewNote": "内容准确，采纳"
}

# 修改后通过
POST /api/knowledge/candidates/{candidateId}/review
Content-Type: application/json

{
  "reviewStatus": "MODIFIED",
  "reviewNote": "修改了阈值参数",
  "modifiedContent": "..."
}

# 批量审核
POST /api/knowledge/candidates/batch-review
Content-Type: application/json

{
  "candidateIds": [101, 102, 103],
  "reviewStatus": "APPROVED",
  "reviewNote": "批量审核通过"
}

# 审核汇总
GET /api/knowledge/candidates/summary

# 审核历史
GET /api/knowledge/candidates/history?limit=20
```

---

### 3.10 来源溯源

来源溯源模块提供知识资产的全链路追溯能力，包括来源文档、引用片段和资产绑定三层结构。

#### 三层溯源结构

```
来源文档（SourceDocument）
  └── 引用片段（SourceCitation）
        └── 资产绑定（SourceAssetBinding）
```

#### 来源文档

来源文档记录知识的原始出处。

| 字段 | 说明 |
|------|------|
| `document_code` | 文档编码 |
| `document_name` | 文档名称 |
| `source_type` | 来源类型 |
| `publisher` | 发布者 |
| `publish_date` | 发布日期 |
| `version` | 版本号 |
| `review_status` | 审核状态 |
| `content_summary` | 内容摘要 |

#### 引用片段

引用片段记录来源文档中被引用的具体段落。

| 字段 | 说明 |
|------|------|
| `citation_id` | 引用ID |
| `document_code` | 所属文档编码 |
| `citation_type` | 引用类型 |
| `section` | 章节位置 |
| `content` | 引用内容 |
| `page_number` | 页码 |

#### 资产绑定

资产绑定记录知识资产与来源引用的关联关系。

| 字段 | 说明 |
|------|------|
| `binding_id` | 绑定ID |
| `document_code` | 文档编码 |
| `asset_type` | 资产类型 |
| `asset_code` | 资产编码 |
| `binding_type` | 绑定类型 |
| `confidence` | 绑定置信度 |

#### 操作步骤

1. 进入 **知识治理 → 来源溯源** 页面
2. 可分别查看 **来源文档**、**引用片段**、**资产绑定** 三个标签页
3. 使用筛选条件查询：
   - 来源文档：按来源类型、审核状态、发布者、关键词筛选
   - 引用片段：按文档编码、引用类型、章节筛选
   - 资产绑定：按资产类型、资产编码、文档编码、绑定类型筛选
4. 点击任一记录可查看详情及其关联的子记录

#### API 参考

```http
# 查询来源文档列表
GET /api/provenance/source-documents?sourceType=GUIDELINE&reviewStatus=APPROVED&keyword=AMI

# 查询来源文档详情
GET /api/provenance/source-documents/{documentCode}

# 查询文档的引用片段
GET /api/provenance/source-documents/{documentCode}/citations

# 查询文档的资产绑定
GET /api/provenance/source-documents/{documentCode}/bindings

# 查询引用片段列表
GET /api/provenance/citations?documentCode=DOC001&citationType=GUIDELINE_SECTION

# 查询资产绑定列表
GET /api/provenance/bindings?assetType=RULE&assetCode=RULE_AMI_001

# 查询某资产的所有绑定
GET /api/provenance/assets/{assetType}/{assetCode}/bindings
```

---

## 4. 最佳实践

### 4.1 知识来源选择标准

| 标准 | 建议 |
|------|------|
| **权威性** | 优先选择国家/行业权威机构发布的指南和标准 |
| **时效性** | 确保来源未过期，定期检查 `expiry_date` |
| **完整性** | 来源内容应覆盖所需的知识范围 |
| **许可合规** | 确认 `license_scope` 允许院内使用，`export_allowed` 符合跨院区流转需求 |
| **可追溯** | 确保来源有明确的版本号和发布日期 |

### 4.2 同步策略建议

| 场景 | 建议策略 |
|------|---------|
| 首次接入知识来源 | 使用 `FULL` 全量同步，确保数据完整 |
| 日常更新 | 使用 `INCREMENTAL` 增量同步，减少数据传输量 |
| 重大版本升级 | 先使用 `DRY_RUN` 试运行评估影响，再执行全量同步 |
| 高频更新来源 | 配置 `auto_sync=true`，频率设为 `DAILY` |
| 低频更新来源 | 配置 `auto_sync=true`，频率设为 `WEEKLY` 或 `MONTHLY` |
| 关键知识来源 | 同步后必须经过审核流程，避免错误数据入库 |

### 4.3 AI 辅助审核流程

```
1. 创建 AI 知识任务（EXTRACT/MAP/RULE_GENERATE）
   ↓
2. 系统自动执行，生成知识候选
   ↓
3. 人工审核候选：
   - 置信度 ≥ 0.9：快速审核，重点关注边界情况
   - 置信度 0.7~0.9：仔细审核，必要时修改后通过
   - 置信度 < 0.7：谨慎审核，建议逐条确认
   ↓
4. 批量审核低风险候选，逐条审核高风险候选
   ↓
5. 运行资产质量检查，确认无遗留问题
```

### 4.4 知识包版本管理

| 建议 | 说明 |
|------|------|
| **语义化版本** | 使用 `主版本.次版本.修订号` 格式，如 `1.2.0` |
| **变更记录** | 每次导出知识包时在描述中记录变更内容 |
| **增量导出** | 日常更新使用增量导出，减少包体积 |
| **导入前预览** | 导入前务必执行预览，确认影响范围 |
| **冲突策略** | 默认使用 `SKIP` 策略，避免意外覆盖；确认安全后可使用 `OVERWRITE` |
| **定期归档** | 已完成导入的知识包及时归档，保持列表整洁 |

---

## 5. 常见问题

### 5.1 同步失败排查

**现象**：同步状态为 `FAILED`，`error_message` 有错误信息。

**排查步骤**：

1. 查看同步详情中的 `error_code` 和 `error_message`
2. 常见错误码及处理方式：

| 错误码 | 原因 | 处理方式 |
|--------|------|---------|
| `ADAPTER_TIMEOUT` | 知识来源连接超时 | 检查网络连通性，确认来源地址可访问 |
| `DIFY_TIMEOUT` | AI 工作流超时 | 检查 AI 模型服务状态，适当增加超时时间 |
| `DATA_MISSING` | 来源数据不完整 | 检查来源数据格式是否符合要求 |
| `CONFIG_NOT_FOUND` | 订阅配置不存在 | 确认订阅ID是否正确 |
| `VALIDATION_ERROR` | 数据校验失败 | 检查同步数据的字段格式和完整性 |

3. 修复问题后，点击 **「重试」** 按钮或调用 `POST /api/knowledge/sync/{logId}/retry`
4. 如重试次数超过 `max_retries`（默认 3 次），需联系系统管理员

### 5.2 AI 审核结果不准确

**现象**：AI 生成的知识候选内容有误或置信度异常。

**排查步骤**：

1. 检查 AI 任务的 `model_provider` 和 `model_name`，确认使用的模型是否合适
2. 查看 `quality_findings` 字段，了解质检发现的问题
3. 检查关联的知识来源是否为最新版本
4. 如置信度普遍偏低，考虑：
   - 更换更合适的 AI 模型
   - 优化提示词模板
   - 提供更高质量的输入数据
5. 对不准确的候选选择 **「拒绝」** 或 **「修改后通过」**

### 5.3 知识包导入冲突

**现象**：导入知识包时出现冲突提示。

**排查步骤**：

1. 执行导入预览（`POST /api/knowledge/packages/{packageId}/preview`），查看冲突详情
2. 根据冲突类型选择处理策略：

| 冲突场景 | 建议策略 |
|---------|---------|
| 目标端数据较旧 | 使用 `OVERWRITE` 覆盖 |
| 目标端有本地定制 | 使用 `SKIP` 跳过，手动合并 |
| 双端都有修改 | 使用 `MERGE` 尝试自动合并，合并失败时手动处理 |

3. 修改冲突策略后重新执行导入
4. 导入完成后运行资产质量检查，确认数据一致性
