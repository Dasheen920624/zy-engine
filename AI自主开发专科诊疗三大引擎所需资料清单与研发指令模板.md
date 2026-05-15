# AI自主开发专科诊疗三大引擎所需资料清单与研发指令模板

版本：V1.0  
适用对象：项目负责人、产品经理、系统架构师、研发负责人、AI编码助手、后端工程师、前端工程师、测试工程师、实施工程师  
适用范围：路径引擎、规则引擎、图谱引擎、字典映射、第三方适配器、Dify适配、数据库适配、国产化部署  
目标：把“让AI按要求自行开发”所需的输入、约束、任务拆解、验收标准和提示词模板一次性梳理清楚，便于后续交给AI编码工具、研发团队或外包团队直接执行。

---

## 1. 核心结论

如果要让AI稳定地自行开发，不能只给AI一份产品方案或架构图，而需要提供一套完整的“AI研发输入包”。

这套输入包至少包括：

```text
业务边界 + 技术栈 + 服务边界 + 数据模型 + 接口契约
+ 配置DSL + 数据库DDL + 字典映射规则 + 适配器规范
+ 权限审计 + 错误码 + 日志规范 + 测试用例
+ Mock数据 + 验收标准 + 任务拆分 + AI开发提示词
```

AI最怕的不是需求复杂，而是边界模糊。  
因此必须把每个模块拆成“输入是什么、输出是什么、状态存在哪里、错误如何处理、怎么测试、完成标准是什么”。

---

## 2. AI自主开发的前置条件

### 2.1 必须先明确的内容

| 类别 | 必须明确的问题 | 不明确的风险 |
|---|---|---|
| 技术栈 | Java/Spring Boot、Node、Python、前端框架、ORM、消息队列 | AI生成的代码风格和架构不统一 |
| 数据库 | Oracle、达梦、PostgreSQL、MySQL支持范围 | SQL不可运行，方言混乱 |
| 服务拆分 | 路径、规则、图谱是否独立服务 | 代码耦合，后续难扩展 |
| 接口协议 | REST、gRPC、MQ、WebService适配方式 | 系统间无法对接 |
| 认证鉴权 | JWT、OAuth2、院内SSO、网关鉴权 | 接口安全缺失 |
| 数据模型 | 患者上下文、临床事件、规则结果、路径实例 | AI各写各的对象 |
| 编码规范 | 包名、目录结构、异常处理、日志规范 | 后续维护困难 |
| 测试要求 | 单测、集成测试、Mock数据、验收病例 | AI写完无法验证 |
| 合规边界 | 医生确认、审计留痕、敏感数据 | 医疗安全和监管风险 |

### 2.2 AI适合做什么

| 适合交给AI | 说明 |
|---|---|
| 生成服务骨架 | Controller、Service、Repository、DTO、Mapper |
| 生成数据模型 | 表结构、实体类、迁移脚本 |
| 生成接口契约 | OpenAPI、请求响应示例 |
| 生成规则DSL解析器 | JSON Schema、表达式解释器、函数库 |
| 生成配置后台页面 | 表单、列表、详情、模拟执行 |
| 生成测试用例 | 单测、集成测试、Mock数据 |
| 生成适配器模板 | REST、SQL、WebService适配器 |
| 生成文档 | README、部署文档、接口文档 |

### 2.3 AI不应直接决定什么

| 不应由AI自行决定 | 应由谁决定 |
|---|---|
| 临床规则阈值 | 专科专家、医务处、质控科 |
| 是否构成诊断建议 | 医务、合规、临床专家 |
| 治疗方案适用条件 | 专科专家、药学部、医务处 |
| 院内字典映射最终确认 | 信息科、数据中心、业务系统负责人 |
| 数据脱敏策略 | 信息安全、医院管理部门 |
| 是否作为医疗器械软件申报 | 法务、合规、监管事务 |

---

## 3. AI研发输入包目录建议

建议建立一个独立目录，作为AI开发和研发团队共同使用的事实来源。

```text
ai-dev-input/
  00_project_overview/
    product_scope.md
    engine_boundary.md
    glossary.md
    non_functional_requirements.md

  01_architecture/
    service_architecture.md
    deployment_architecture.md
    module_dependencies.md
    technology_stack.md

  02_api_contracts/
    pathway-engine.openapi.yaml
    rule-engine.openapi.yaml
    graph-engine.openapi.yaml
    terminology-service.openapi.yaml
    adapter-hub.openapi.yaml
    dify-adapter.openapi.yaml

  03_data_models/
    clinical_event.schema.json
    patient_context.schema.json
    pathway_config.schema.json
    rule_dsl.schema.json
    graph_query.schema.json
    recommendation.schema.json

  04_database/
    oracle/
      pathway_engine.sql
      rule_engine.sql
      terminology.sql
    dm/
      pathway_engine.sql
      rule_engine.sql
      terminology.sql
    postgresql/
      pathway_engine.sql
      rule_engine.sql
      terminology.sql
    migration_strategy.md

  05_engine_specs/
    pathway_engine_spec.md
    rule_engine_spec.md
    graph_engine_spec.md
    terminology_spec.md
    adapter_spec.md
    dify_adapter_spec.md

  06_samples/
    sample_ami_pathway.json
    sample_ami_rules.json
    sample_dictionary_mapping.xlsx
    sample_patient_context_ami.json
    sample_graph_nodes_edges.json

  07_tests/
    test_case_matrix.md
    pathway_engine_cases.json
    rule_engine_cases.json
    graph_engine_cases.json
    integration_cases.md

  08_coding_standards/
    backend_code_style.md
    frontend_code_style.md
    error_code_standard.md
    logging_standard.md
    audit_standard.md

  09_ai_task_cards/
    task_card_template.md
    pathway_engine_tasks.md
    rule_engine_tasks.md
    graph_engine_tasks.md
    frontend_tasks.md
    test_tasks.md

  10_prompts/
    ai_system_prompt.md
    ai_backend_prompt_template.md
    ai_frontend_prompt_template.md
    ai_test_prompt_template.md
    ai_review_prompt_template.md
```

---

## 4. 需要补齐的详细内容清单

### 4.1 产品级资料

| 文档 | 内容 | 用途 |
|---|---|---|
| 产品范围说明 | 三大引擎做什么、不做什么 | 防止AI扩展过度 |
| 用户角色矩阵 | 医生、质控、信息科、配置员、专家审核员 | 生成权限和页面 |
| 业务流程 | AMI样例流程、入径、节点、变异、随访 | 生成路径逻辑 |
| 页面清单 | 管理后台、医生工作台、质控看板 | 前端开发 |
| 术语表 | 路径、节点、规则、图谱、证据、变异 | 保持命名一致 |
| 验收目标 | MVP、二期、三期验收范围 | 控制开发节奏 |

### 4.2 架构级资料

| 文档 | 内容 | 用途 |
|---|---|---|
| 服务边界图 | 每个服务职责、依赖关系 | 防止服务耦合 |
| 调用链图 | 事件到路径、规则、图谱、Dify、医生反馈 | 生成接口调用 |
| 部署架构 | 单院部署、高可用、国产化环境 | 运维和部署 |
| 数据流图 | 数据从HIS/EMR/LIS/PACS到引擎的流程 | 适配器开发 |
| 安全架构 | 鉴权、脱敏、审计、日志 | 医疗安全 |
| 数据库适配策略 | Oracle、达梦、PostgreSQL差异 | 数据层开发 |

### 4.3 开发级资料

| 文档 | 内容 | 用途 |
|---|---|---|
| OpenAPI接口 | 每个接口的请求、响应、错误码 | 前后端并行 |
| JSON Schema | DSL、上下文、事件、推荐卡片 | 参数校验 |
| DDL脚本 | 表、字段、索引、约束 | 数据库初始化 |
| 枚举字典 | 状态、类型、动作、严重级别 | 代码常量 |
| 错误码规范 | 业务错误、系统错误、适配器错误 | 统一异常 |
| 日志规范 | trace_id、span_id、patient_id脱敏 | 排查问题 |
| 测试数据 | AMI患者、规则、路径、字典映射 | 自动化测试 |
| 验收用例 | 输入、动作、期望输出 | 判断AI代码能否交付 |

---

## 5. 路径引擎AI开发需要补齐的内容

### 5.1 路径引擎最小开发边界

AI开发路径引擎前，必须明确以下边界：

```text
路径引擎只负责路径定义、版本、实例、节点状态、任务状态、变异记录。
路径引擎可以调用规则引擎、图谱引擎、Dify适配服务。
路径引擎不直接写临床规则，不直接访问HIS/EMR/LIS/PACS原始库。
路径引擎不直接生成大模型文本，文本生成通过Dify适配服务完成。
```

### 5.2 路径引擎必须提供的规格

| 规格 | 内容 |
|---|---|
| 路径状态枚举 | DRAFT、PUBLISHED、DISABLED、CANDIDATE、ACTIVE、EXITED、CLOSED |
| 节点状态枚举 | WAITING、RUNNING、COMPLETED、SKIPPED、BLOCKED、TIMEOUT |
| 任务类型枚举 | FORM、LAB、EXAM、ORDER_SET、SCORE、DOCUMENT、FOLLOW_UP、DIFY_WORKFLOW、RULE_CHECK |
| 变异类型枚举 | PATIENT_REASON、DOCTOR_DECISION、RESOURCE_LIMIT、CONTRAINDICATION、TRANSFER、OTHER |
| 流转规则 | 节点如何进入、完成、跳过、退出 |
| 版本规则 | 草稿、审核、发布、回滚，患者实例绑定已发布版本 |
| 并发规则 | 同一患者同一路径是否允许重复入径 |
| 幂等规则 | 重复事件如何避免重复创建实例 |

### 5.3 路径引擎AI任务拆分

| 任务编号 | 任务 | 输入 | 输出 | 验收 |
|---|---|---|---|---|
| PE-001 | 创建路径引擎服务骨架 | 技术栈、包名、数据库 | 可启动服务 | 健康检查通过 |
| PE-002 | 实现路径定义与版本模型 | DDL、实体字段 | CRUD接口 | 单测通过 |
| PE-003 | 实现节点与任务模型 | 节点DSL | 节点配置接口 | 可保存AMI路径 |
| PE-004 | 实现患者路径实例 | 患者上下文 | 创建实例接口 | 同一患者幂等 |
| PE-005 | 实现节点状态机 | 状态流转规则 | 节点进入/完成/退出接口 | 状态转换正确 |
| PE-006 | 实现规则引擎调用 | 规则API契约 | 入径/节点规则判断 | Mock规则通过 |
| PE-007 | 实现Dify调用 | Dify适配API | 推荐卡片记录 | 超时可降级 |
| PE-008 | 实现变异记录 | 变异枚举 | 变异接口 | 可审计 |
| PE-009 | 实现路径模拟 | 测试病例JSON | 模拟结果 | AMI样例通过 |

### 5.4 路径引擎任务卡模板

```markdown
## 任务编号
PE-xxx

## 任务名称
实现患者路径实例创建接口

## 背景
路径引擎需要在医生确认入径后，为患者创建路径实例，并绑定路径版本。

## 输入资料
- patient_context.schema.json
- pathway_config.schema.json
- pe_patient_instance表结构
- OpenAPI: POST /api/patient-pathways/admit

## 功能要求
1. 校验路径版本必须为PUBLISHED。
2. 同一encounter_id + pathway_code默认只允许一个ACTIVE实例。
3. 创建实例后自动创建第一个节点状态。
4. 返回instance_id、current_node_code、status。
5. 写入审计日志。

## 非功能要求
- 接口响应时间小于500ms。
- 重复请求需要幂等。
- 不直接调用Dify。

## 验收标准
- 单元测试覆盖正常入径、重复入径、路径未发布、患者不存在。
- 集成测试使用AMI样例可创建实例。
- 数据库Oracle/达梦方言均可运行。
```

---

## 6. 规则引擎AI开发需要补齐的内容

### 6.1 规则引擎最小开发边界

```text
规则引擎负责规则定义、规则解析、事实构建、条件判断、结果动作和执行日志。
规则引擎可以通过适配器中心取数，可以通过字典服务做编码映射。
规则引擎不直接管理患者路径实例。
规则引擎不直接维护医学知识图谱。
规则引擎可以被路径引擎、医生工作站、质控任务、Dify调用。
```

### 6.2 规则引擎必须补齐的规则语法

AI开发前必须提供规则DSL的完整操作符清单。

#### 6.2.1 基础逻辑操作符

| 操作符 | 说明 | 示例 |
|---|---|---|
| `all` | 全部满足 | 多条件AND |
| `any` | 任一满足 | 多条件OR |
| `not` | 取反 | 不存在某文书 |
| `exists` | 字段存在 | 是否有心电图 |
| `not_exists` | 字段不存在 | 无首次病程 |

#### 6.2.2 比较操作符

| 操作符 | 说明 |
|---|---|
| `eq` | 等于 |
| `ne` | 不等于 |
| `gt` | 大于 |
| `gte` | 大于等于 |
| `lt` | 小于 |
| `lte` | 小于等于 |
| `in` | 在集合内 |
| `not_in` | 不在集合内 |
| `between` | 区间 |

#### 6.2.3 文本操作符

| 操作符 | 说明 |
|---|---|
| `contains` | 包含文本 |
| `contains_any` | 包含任一关键词 |
| `contains_all` | 包含全部关键词 |
| `regex_match` | 正则匹配 |
| `section_exists` | 文书段落存在 |
| `section_missing` | 文书段落缺失 |

#### 6.2.4 时间操作符

| 操作符 | 说明 |
|---|---|
| `duration_minutes_between` | 两个时间点分钟差 |
| `within_minutes` | 是否在指定分钟内 |
| `after` | 晚于某时间 |
| `before` | 早于某时间 |
| `date_diff_days` | 天数差 |

#### 6.2.5 医疗扩展操作符

| 操作符 | 说明 |
|---|---|
| `lab_abnormal` | 检验异常判断 |
| `critical_value` | 危急值判断 |
| `diagnosis_match` | 诊断匹配 |
| `drug_class_match` | 药品类别匹配 |
| `order_exists` | 医嘱存在 |
| `score_gte` | 评分大于等于 |
| `contraindication_exists` | 禁忌证存在 |

### 6.3 规则函数库要求

AI需要实现规则函数库，函数必须有单元测试。

| 函数 | 输入 | 输出 |
|---|---|---|
| `exists(path)` | JSON路径 | boolean |
| `get(path)` | JSON路径 | value |
| `contains(text, keyword)` | 文本、关键词 | boolean |
| `durationMinutes(t1, t2)` | 两个时间 | number |
| `normalizeCode(sourceCode)` | 原始编码 | 标准编码 |
| `convertUnit(value, from, to)` | 值、原单位、目标单位 | number |
| `matchDiagnosis(code, concept)` | 诊断码、标准概念 | boolean |
| `matchDrugClass(drugCode, classCode)` | 药品码、药品类别 | boolean |

### 6.4 规则引擎AI任务拆分

| 任务编号 | 任务 | 输入 | 输出 | 验收 |
|---|---|---|---|---|
| RE-001 | 规则服务骨架 | 技术栈 | 可启动服务 | 健康检查 |
| RE-002 | 规则DSL JSON Schema | DSL样例 | schema文件 | schema校验通过 |
| RE-003 | 规则解析器 | DSL操作符 | 条件树对象 | 单测覆盖 |
| RE-004 | 事实构建器 | 患者上下文、数据需求 | RuleFact对象 | Mock取数通过 |
| RE-005 | 基础操作符 | 操作符清单 | 函数库 | 单测通过 |
| RE-006 | 时间质控函数 | 时间操作符 | 时限规则支持 | 首次病程规则通过 |
| RE-007 | 内涵质控函数 | 文本操作符 | 文书完整性检查 | 出院小结规则通过 |
| RE-008 | 字典映射调用 | terminology API | 标准码转换 | 多系统码映射通过 |
| RE-009 | 适配器调用 | adapter API | 第三方取数 | REST/SQL Mock通过 |
| RE-010 | 规则模拟接口 | 测试病例 | 模拟结果 | UI可展示命中原因 |

### 6.5 规则配置后台给AI的要求

AI开发规则配置前，需要提供：

1. 规则模板清单。
2. 表单字段定义。
3. 条件表达式组件设计。
4. 数据来源选择组件设计。
5. 动作配置组件设计。
6. 测试病例输入和结果展示设计。

示例字段：

```json
{
  "rule_code": "R_EMR_FIRST_COURSE_8H",
  "rule_name": "首次病程记录8小时完成",
  "rule_type": "TIME_LIMIT_QC",
  "trigger_events": ["ADMISSION_CREATED", "DOCUMENT_CREATED"],
  "severity": "HIGH",
  "condition_builder": {
    "logic": "ANY",
    "conditions": [
      {"field": "first_course_doc.create_time", "operator": "NOT_EXISTS"},
      {"field": "duration_minutes", "operator": "GT", "value": 480}
    ]
  },
  "actions": ["CREATE_QC_TASK", "PUSH_TO_DOCTOR"]
}
```

---

## 7. 图谱引擎AI开发需要补齐的内容

### 7.1 图谱引擎最小开发边界

```text
图谱引擎负责封装图数据库查询、候选召回、证据关系查询和图谱版本管理。
业务系统不直接写Cypher。
图谱引擎返回结构化结果，不直接生成大模型解释文本。
图谱引擎可以被路径引擎、规则引擎、Dify适配服务调用。
```

### 7.2 图谱Schema必须先定义

AI开发图谱引擎前，需要定义：

1. 节点标签清单。
2. 节点属性字段。
3. 关系类型清单。
4. 关系权重含义。
5. 证据来源字段。
6. 版本管理方式。
7. 索引和唯一约束。

示例：

```json
{
  "node_label": "Disease",
  "properties": {
    "code": "AMI_STEMI",
    "name": "急性ST段抬高型心肌梗死",
    "category": "CARDIOVASCULAR",
    "status": "ACTIVE",
    "version": "KG_2026_05"
  },
  "unique_keys": ["code", "version"]
}
```

### 7.3 图谱查询模板

AI需要根据查询模板实现固定API，而不是让调用方传任意Cypher。

| 查询模板 | 输入 | 输出 |
|---|---|---|
| 候选疾病召回 | 症状、检验、检查、危险因素 | 疾病候选列表 |
| 疾病知识查询 | disease_code | 症状、检查、检验、治疗 |
| 治疗方案召回 | disease_code、stage、patient_constraints | 治疗候选 |
| 证据查询 | target_code | 证据条款 |
| 路径关系查询 | pathway_code | 阶段、节点、规则 |
| 解释路径查询 | source_code、target_code | 关系路径 |

### 7.4 图谱引擎AI任务拆分

| 任务编号 | 任务 | 输入 | 输出 | 验收 |
|---|---|---|---|---|
| GE-001 | 图谱服务骨架 | 技术栈、Neo4j配置 | 可启动服务 | 健康检查 |
| GE-002 | 图谱查询DAO | Cypher模板 | 查询组件 | 单测Mock |
| GE-003 | 候选疾病召回API | 症状/指标输入 | 候选疾病 | AMI召回通过 |
| GE-004 | 证据查询API | target_code | 证据列表 | 证据ID返回 |
| GE-005 | 路径关系API | pathway_code | 节点/规则关系 | AMI路径返回 |
| GE-006 | 图谱导入接口 | JSON/Excel | 导入任务 | 样例导入通过 |
| GE-007 | 图谱版本管理 | 版本策略 | 发布/回滚 | 版本隔离 |

---

## 8. 字典映射与适配器AI开发需要补齐的内容

### 8.1 字典映射必须提供的资料

| 资料 | 内容 |
|---|---|
| 标准概念字典 | 平台内部统一编码 |
| 院内字典样例 | 诊断、检验、检查、药品、科室、文书 |
| 第三方系统字典样例 | HIS、EMR、LIS、PACS编码 |
| 映射规则 | 一对一、一对多、多对一、未映射处理 |
| 单位换算 | 检验单位转换 |
| 审核流程 | 映射草稿、审核、发布 |

### 8.2 适配器必须提供的资料

| 资料 | 内容 |
|---|---|
| 接口类型 | REST、SQL、WebService、MQ、文件 |
| 连接配置 | URL、数据源、认证、超时 |
| 查询模板 | 查询患者诊断、医嘱、检验、检查、文书 |
| 返回字段映射 | 原字段到标准字段 |
| 错误处理 | 超时、无数据、字段缺失、权限失败 |
| 缓存策略 | 哪些数据可缓存，缓存多久 |

### 8.3 适配器任务卡示例

```markdown
## 任务编号
ADP-REST-001

## 任务名称
实现REST适配器模板

## 功能要求
1. 支持GET/POST。
2. 支持Header和Token配置。
3. 支持请求参数模板。
4. 支持响应JSONPath字段提取。
5. 支持超时、重试、错误码转换。
6. 返回统一AdapterResult。

## 输入
- adapter_spec.md
- AdapterResult schema
- REST配置样例

## 验收
- 使用Mock接口可查询患者诊断。
- 接口超时时返回ADAPTER_TIMEOUT。
- 响应字段缺失时返回DATA_FIELD_MISSING。
```

---

## 9. 数据库适配AI开发需要补齐的内容

### 9.1 必须明确的数据库支持范围

建议明确优先级：

```text
P0：Oracle、达梦
P1：PostgreSQL
P2：MySQL、人大金仓、神通
```

### 9.2 AI需要的数据库输入

| 输入 | 说明 |
|---|---|
| 数据库方言清单 | oracle、dm、postgresql、mysql |
| DDL规范 | 字段类型、主键、索引、CLOB用法 |
| 分页写法 | 不同数据库分页适配 |
| 时间函数 | 当前时间、时间差计算 |
| 锁策略 | 乐观锁优先 |
| 迁移策略 | Flyway/Liquibase脚本目录 |
| 测试矩阵 | 每个数据库必须跑哪些测试 |

### 9.3 数据库开发约束

给AI的约束必须写清：

```text
1. 禁止在业务代码中直接拼接数据库方言SQL。
2. 所有分页通过DialectProvider生成。
3. 所有ID由应用层生成，不依赖数据库自增。
4. 配置DSL原文可存CLOB，但用于查询的字段必须结构化存储。
5. 每张配置表必须包含状态、版本、审计字段。
6. 每张执行日志表必须包含trace_id、耗时、结果状态。
```

---

## 10. 前端管理后台AI开发需要补齐的内容

### 10.1 页面清单

| 页面 | 功能 |
|---|---|
| 路径列表 | 查询、创建、复制、停用 |
| 路径设计器 | 阶段、节点、任务、流转配置 |
| 路径版本 | 草稿、审核、发布、回滚 |
| 规则列表 | 分类、状态、版本、启停 |
| 规则编辑器 | 模板化配置、条件构建、动作配置 |
| 规则模拟 | 输入病例，查看命中原因 |
| 图谱管理 | 节点、关系、证据维护 |
| 字典映射 | 映射维护、未映射治理 |
| 适配器管理 | 连接、查询模板、测试 |
| 医生路径工作台 | 患者路径、节点任务、推荐卡片 |
| 质控看板 | 指标、规则命中、变异复盘 |

### 10.2 前端给AI的设计约束

```text
1. 不做营销式大屏，做医院业务系统风格。
2. 表单字段必须清晰，配置步骤必须可回退。
3. 所有配置页面必须有“测试/模拟”入口。
4. 所有发布动作必须二次确认。
5. 规则和路径配置必须显示版本状态。
6. 医生工作台必须少打扰，只显示关键推荐和任务。
7. 错误信息必须面向实施人员可理解。
```

---

## 11. 测试资料与验收用例

### 11.1 AI必须获得的测试数据

至少准备以下测试病例：

| 病例 | 用途 |
|---|---|
| AMI高风险病例 | 测试候选识别、入径、节点流转 |
| AMI资料不足病例 | 测试缺数据提醒 |
| AMI禁忌证病例 | 测试强拦截 |
| 病历超时病例 | 测试时限质控 |
| 出院小结缺项病例 | 测试内涵质控 |
| 字典不一致病例 | 测试字典映射 |
| 第三方接口超时病例 | 测试适配器错误处理 |
| Dify不可用病例 | 测试降级策略 |

### 11.2 测试用例模板

```json
{
  "case_id": "TC_AMI_001",
  "case_name": "胸痛+ST段抬高触发AMI候选推荐",
  "input": {
    "event_type": "EXAM_RESULTED",
    "patient_context_file": "sample_patient_context_ami.json"
  },
  "steps": [
    "调用候选路径识别接口",
    "路径引擎调用规则引擎",
    "路径引擎调用图谱引擎",
    "生成推荐记录"
  ],
  "expected": {
    "candidate_pathway": "AMI_STEMI",
    "score_gte": 85,
    "action_level": "STRONG_ALERT",
    "doctor_confirm_required": true
  }
}
```

### 11.3 Definition of Done

每个AI开发任务完成必须满足：

```text
1. 功能代码完成。
2. 单元测试完成。
3. 接口文档同步。
4. 错误码和日志符合规范。
5. 数据库脚本完成。
6. 支持Oracle/达梦方言要求。
7. Mock数据可运行。
8. 不破坏既有测试。
9. README说明如何启动和验证。
10. 有明确未完成项说明。
```

---

## 12. AI开发提示词模板

### 12.1 AI通用系统提示词

```text
你是医院专科诊疗管理平台的资深软件工程师。
你正在开发路径引擎、规则引擎、图谱引擎等独立服务能力。

开发原则：
1. 严格遵守服务边界，不能把路径状态写到规则引擎中。
2. 所有接口必须有请求、响应、错误码和审计日志。
3. 所有配置必须支持版本管理。
4. 所有数据库访问必须考虑Oracle和达梦兼容。
5. 规则、路径、图谱配置不得硬编码在业务代码中。
6. Dify只作为流程编排和解释生成能力，不保存核心状态。
7. 所有临床建议必须保留医生确认入口。
8. 每次代码修改必须补充或更新测试。

输出要求：
- 先说明理解的任务边界。
- 再说明将修改哪些文件。
- 然后实现代码。
- 最后说明如何测试和验收。
```

### 12.2 后端开发提示词模板

```text
请基于以下资料实现【模块名称】：

模块：路径引擎 / 规则引擎 / 图谱引擎
任务编号：
任务名称：
技术栈：
数据库：
输入文档：
- OpenAPI：
- 数据库表：
- JSON Schema：
- 样例数据：

功能要求：
1.
2.
3.

非功能要求：
1. 支持trace_id日志。
2. 支持Oracle/达梦数据库方言。
3. 所有接口返回统一Result对象。
4. 异常使用统一错误码。
5. 补充单元测试。

请输出：
1. 代码实现。
2. 新增/修改文件列表。
3. 单元测试。
4. 本地验证命令。
5. 未覆盖风险。
```

### 12.3 前端开发提示词模板

```text
请实现【页面名称】页面。

页面目标：
用户角色：
核心操作：
接口契约：
字段定义：
交互要求：

设计要求：
1. 医院业务系统风格，信息清晰，不做营销页。
2. 表单字段分组明确。
3. 发布、停用、回滚等操作需要二次确认。
4. 配置页面必须支持测试/模拟。
5. 错误信息要可理解。

请输出：
1. 页面代码。
2. 组件拆分。
3. 状态管理说明。
4. 接口调用说明。
5. 基础测试。
```

### 12.4 测试开发提示词模板

```text
请为【模块/接口】生成测试用例。

输入资料：
- 接口契约：
- 数据库表：
- 样例数据：
- 业务规则：

测试范围：
1. 正常流程。
2. 边界条件。
3. 异常输入。
4. 权限校验。
5. 数据库兼容。
6. 幂等。
7. 超时和降级。

请输出：
1. 测试用例清单。
2. 自动化测试代码。
3. Mock数据。
4. 覆盖率说明。
```

### 12.5 代码审查提示词模板

```text
请以资深架构师和医疗信息化研发负责人的角度审查以下代码。

重点关注：
1. 是否违反路径、规则、图谱服务边界。
2. 是否存在临床安全风险。
3. 是否有硬编码路径、规则、字典。
4. 是否支持Oracle/达梦兼容。
5. 是否有审计日志。
6. 是否有trace_id。
7. 是否有单元测试和异常测试。
8. 是否可能导致医生工作站卡顿。
9. 是否存在敏感数据泄露。

请按严重程度输出问题，并给出具体修改建议。
```

---

## 13. AI开发执行流程

建议采用以下流程，而不是一次性让AI开发全部系统。

```mermaid
flowchart LR
    A[准备AI研发输入包] --> B[生成服务骨架]
    B --> C[实现数据模型和DDL]
    C --> D[实现核心接口]
    D --> E[实现规则/路径/图谱最小闭环]
    E --> F[补充测试用例]
    F --> G[代码审查]
    G --> H[集成Dify与适配器]
    H --> I[AMI样例验收]
    I --> J[扩展多病种]
```

### 13.1 推荐开发顺序

1. 公共对象：临床事件、患者上下文、统一返回、错误码、trace日志。
2. 字典映射服务。
3. 适配器中心Mock版。
4. 规则引擎最小版。
5. 图谱引擎最小版。
6. 路径引擎最小版。
7. Dify适配服务。
8. AMI样例闭环。
9. 配置后台。
10. 质控看板。

### 13.2 不建议的开发方式

```text
1. 不建议先做复杂前端页面，再补后端。
2. 不建议先接真实HIS/EMR，再做Mock和标准模型。
3. 不建议让AI一次性生成完整平台。
4. 不建议把规则写死在Java代码里。
5. 不建议让Dify承担路径状态和强规则。
6. 不建议先适配所有病种，应先跑通AMI闭环。
```

---

## 14. 交付物清单

如果要正式启动AI辅助开发，建议至少准备以下交付物：

### 14.1 P0必须交付

| 编号 | 交付物 |
|---|---|
| P0-01 | 项目范围与服务边界文档 |
| P0-02 | 技术栈与编码规范 |
| P0-03 | 临床事件JSON Schema |
| P0-04 | 患者上下文JSON Schema |
| P0-05 | 路径配置JSON Schema |
| P0-06 | 规则DSL JSON Schema |
| P0-07 | 图谱Schema定义 |
| P0-08 | OpenAPI接口契约 |
| P0-09 | Oracle/达梦DDL初稿 |
| P0-10 | AMI样例路径配置 |
| P0-11 | AMI样例规则配置 |
| P0-12 | AMI样例患者上下文 |
| P0-13 | 统一错误码和日志规范 |
| P0-14 | 单元测试和集成测试用例 |

### 14.2 P1建议交付

| 编号 | 交付物 |
|---|---|
| P1-01 | 前端页面原型和字段说明 |
| P1-02 | 规则模板库 |
| P1-03 | 字典映射样例Excel |
| P1-04 | REST/SQL/WebService适配器样例 |
| P1-05 | 图谱导入样例 |
| P1-06 | Dify Workflow输入输出Schema |
| P1-07 | 国产化部署说明 |
| P1-08 | 性能测试方案 |
| P1-09 | 安全审计方案 |

---

## 15. 最终建议

要让AI按要求自行开发，下一步不应继续扩写业务方案，而应建设一套“AI可执行规格包”。

建议优先生成以下五类文件：

```text
1. OpenAPI接口契约
2. JSON Schema配置契约
3. Oracle/达梦数据库DDL
4. AMI样例路径、规则、患者上下文Mock数据
5. AI任务卡和验收测试用例
```

只要这五类文件足够清楚，AI就可以从“生成文档”进入“稳定生成代码、测试和接口”的阶段。  
否则AI会不断在服务边界、数据库结构、规则语法和验收标准上自行猜测，最终代码难以集成。

