# DATA-001 主数据、数据字典和数据质量治理设计

> 任务编号：DATA-001
> 状态：DONE
> 负责人：CodeBuddy
> 创建时间：2026-05-20
> 完成时间：2026-05-20 06:25
> 依赖：DOC-008 ✅ DONE, ORG-003 ✅ DONE, TERM-001 ✅ DONE
> commit: xxx

## 1. 概述

本文档描述DATA-001任务的设计方案，包括主数据模型定义、数据字典统一管理和数据质量规则机制。

## 2. 主数据模型设计

### 2.1 患者主数据（md_patient）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | VARCHAR(64) | 租户ID |
| patient_id | VARCHAR(64) | 患者ID（业务唯一标识） |
| patient_name | VARCHAR(100) | 患者姓名 |
| gender | VARCHAR(10) | 性别（M/F） |
| birth_date | DATE | 出生日期 |
| id_card_no | VARCHAR(18) | 身份证号（加密存储） |
| phone | VARCHAR(20) | 联系电话 |
| address | VARCHAR(500) | 地址 |
| status | VARCHAR(32) | 状态（ACTIVE/INACTIVE） |
| created_time | TIMESTAMP | 创建时间 |
| updated_time | TIMESTAMP | 更新时间 |

### 2.2 医生主数据（md_doctor）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | VARCHAR(64) | 租户ID |
| doctor_id | VARCHAR(64) | 医生ID（业务唯一标识） |
| doctor_name | VARCHAR(100) | 医生姓名 |
| gender | VARCHAR(10) | 性别 |
| title | VARCHAR(64) | 职称 |
| specialty_code | VARCHAR(64) | 专业编码 |
| department_code | VARCHAR(64) | 所属科室编码 |
| license_no | VARCHAR(64) | 执业证书号 |
| status | VARCHAR(32) | 状态 |
| created_time | TIMESTAMP | 创建时间 |
| updated_time | TIMESTAMP | 更新时间 |

### 2.3 科室主数据（md_department）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | VARCHAR(64) | 租户ID |
| dept_code | VARCHAR(64) | 科室编码（业务唯一标识） |
| dept_name | VARCHAR(200) | 科室名称 |
| dept_type | VARCHAR(32) | 科室类型（临床/医技/行政） |
| parent_dept_code | VARCHAR(64) | 上级科室编码 |
| status | VARCHAR(32) | 状态 |
| created_time | TIMESTAMP | 创建时间 |
| updated_time | TIMESTAMP | 更新时间 |

### 2.4 诊断主数据（md_diagnosis）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | VARCHAR(64) | 租户ID |
| diagnosis_code | VARCHAR(64) | 诊断编码（ICD-10等） |
| diagnosis_name | VARCHAR(200) | 诊断名称 |
| standard_code | VARCHAR(64) | 标准编码 |
| standard_system | VARCHAR(32) | 标准体系（ICD-10/ICD-11） |
| category | VARCHAR(64) | 分类 |
| status | VARCHAR(32) | 状态 |
| created_time | TIMESTAMP | 创建时间 |
| updated_time | TIMESTAMP | 更新时间 |

### 2.5 医嘱主数据（md_order）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | VARCHAR(64) | 租户ID |
| order_code | VARCHAR(64) | 医嘱编码 |
| order_name | VARCHAR(200) | 医嘱名称 |
| order_type | VARCHAR(32) | 医嘱类型（药品/检查/检验/手术） |
| standard_code | VARCHAR(64) | 标准编码 |
| status | VARCHAR(32) | 状态 |
| created_time | TIMESTAMP | 创建时间 |
| updated_time | TIMESTAMP | 更新时间 |

### 2.6 药品主数据（md_drug）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | VARCHAR(64) | 租户ID |
| drug_code | VARCHAR(64) | 药品编码 |
| drug_name | VARCHAR(200) | 药品名称 |
| generic_name | VARCHAR(200) | 通用名 |
| specification | VARCHAR(200) | 规格 |
| manufacturer | VARCHAR(200) | 生产厂家 |
| approval_no | VARCHAR(64) | 批准文号 |
| national_code | VARCHAR(64) | 国家编码 |
| medical_insurance_code | VARCHAR(64) | 医保编码 |
| status | VARCHAR(32) | 状态 |
| created_time | TIMESTAMP | 创建时间 |
| updated_time | TIMESTAMP | 更新时间 |

### 2.7 医保主数据（md_insurance）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | VARCHAR(64) | 租户ID |
| insurance_code | VARCHAR(64) | 医保编码 |
| insurance_name | VARCHAR(200) | 医保名称 |
| insurance_type | VARCHAR(32) | 医保类型（城镇职工/城镇居民/新农合） |
| region_code | VARCHAR(64) | 地区编码 |
| status | VARCHAR(32) | 状态 |
| created_time | TIMESTAMP | 创建时间 |
| updated_time | TIMESTAMP | 更新时间 |

### 2.8 知识资产主数据（md_knowledge_asset）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | VARCHAR(64) | 租户ID |
| asset_code | VARCHAR(128) | 资产编码 |
| asset_name | VARCHAR(200) | 资产名称 |
| asset_type | VARCHAR(32) | 资产类型（指南/规范/共识/专家意见） |
| version | VARCHAR(32) | 版本 |
| source_org | VARCHAR(200) | 来源机构 |
| publish_date | DATE | 发布日期 |
| effective_date | DATE | 生效日期 |
| expiry_date | DATE | 失效日期 |
| status | VARCHAR(32) | 状态 |
| created_time | TIMESTAMP | 创建时间 |
| updated_time | TIMESTAMP | 更新时间 |

## 3. 数据字典统一管理

### 3.1 与TERM-001集成

数据字典管理将与现有的terminology模块集成，复用以下表：
- `tm_standard_concept`：标准概念表
- `tm_concept_mapping`：概念映射表

### 3.2 新增数据字典表

#### 3.2.1 数据字典分类表（dg_dict_category）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | VARCHAR(64) | 租户ID |
| category_code | VARCHAR(64) | 分类编码 |
| category_name | VARCHAR(200) | 分类名称 |
| parent_category_code | VARCHAR(64) | 上级分类编码 |
| status | VARCHAR(32) | 状态 |
| created_time | TIMESTAMP | 创建时间 |

#### 3.2.2 数据字典项表（dg_dict_item）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | VARCHAR(64) | 租户ID |
| category_code | VARCHAR(64) | 分类编码 |
| item_code | VARCHAR(64) | 字典项编码 |
| item_name | VARCHAR(200) | 字典项名称 |
| item_value | VARCHAR(500) | 字典项值 |
| display_order | INTEGER | 显示顺序 |
| status | VARCHAR(32) | 状态 |
| created_time | TIMESTAMP | 创建时间 |
| updated_time | TIMESTAMP | 更新时间 |

## 4. 数据质量规则

### 4.1 数据质量规则表（dg_quality_rule）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | VARCHAR(64) | 租户ID |
| rule_code | VARCHAR(64) | 规则编码 |
| rule_name | VARCHAR(200) | 规则名称 |
| rule_type | VARCHAR(32) | 规则类型（完整性/准确性/一致性/及时性） |
| target_entity | VARCHAR(64) | 目标实体（表名） |
| target_field | VARCHAR(64) | 目标字段 |
| rule_expression | CLOB | 规则表达式 |
| severity | VARCHAR(32) | 严重程度（CRITICAL/WARNING/INFO） |
| status | VARCHAR(32) | 状态 |
| created_time | TIMESTAMP | 创建时间 |
| updated_time | TIMESTAMP | 更新时间 |

### 4.2 数据质量检查记录表（dg_quality_check）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | VARCHAR(64) | 租户ID |
| check_id | VARCHAR(64) | 检查ID |
| rule_code | VARCHAR(64) | 规则编码 |
| target_entity | VARCHAR(64) | 目标实体 |
| target_id | VARCHAR(64) | 目标记录ID |
| check_result | VARCHAR(32) | 检查结果（PASS/FAIL） |
| error_message | VARCHAR(1000) | 错误信息 |
| check_time | TIMESTAMP | 检查时间 |
| created_time | TIMESTAMP | 创建时间 |

## 5. 实现计划

### 5.1 阶段一：数据库设计
1. 创建主数据表DDL（Oracle/DM/PG/Kingbase/LOCAL_H2_FILE）
2. 创建数据字典表DDL
3. 创建数据质量规则表DDL

### 5.2 阶段二：后端实现
1. 创建data-governance模块
2. 实现主数据实体类
3. 实现数据访问层（Repository）
4. 实现业务服务层（Service）
5. 实现API控制器（Controller）

### 5.3 阶段三：数据质量引擎
1. 实现数据质量规则解析器
2. 实现数据质量检查执行器
3. 实现数据质量报告生成器

### 5.4 阶段四：集成测试
1. 单元测试
2. 集成测试
3. 多数据库兼容性测试