# INTEROP-001 院内互联互通标准适配矩阵

## 1. 任务概述

**任务编号：** INTEROP-001  
**任务名称：** 院内互联互通标准适配矩阵  
**状态：** IN_PROGRESS  
**负责人：** CodeBuddy  
**开始时间：** 2026-05-20T06:33:00+08:00  

## 2. 目标

建立院内信息系统（HIS/EMR/LIS/PACS/医保/OA）与医疗信息交换标准（HL7 v2、FHIR、CDA、IHE、CDS Hooks、SMART on FHIR、DICOM）的适配矩阵，实现：

1. **标准化映射**：将院内系统接口映射到国际标准
2. **协议适配**：支持多种通信协议和消息格式
3. **数据转换**：实现不同数据模型之间的转换
4. **集成策略**：为每种系统提供集成方案

## 3. 适配矩阵概览

### 3.1 院内系统 × 标准矩阵

| 院内系统 | HL7 v2 | FHIR | CDA | IHE | CDS Hooks | SMART on FHIR | DICOM |
|---------|--------|------|-----|-----|-----------|---------------|-------|
| **HIS** | ✅ 支持 | ✅ 支持 | ✅ 支持 | ✅ 支持 | ✅ 支持 | ✅ 支持 | ❌ 不适用 |
| **EMR** | ✅ 支持 | ✅ 支持 | ✅ 支持 | ✅ 支持 | ✅ 支持 | ✅ 支持 | ❌ 不适用 |
| **LIS** | ✅ 支持 | ✅ 支持 | ✅ 支持 | ✅ 支持 | ⚠️ 部分支持 | ⚠️ 部分支持 | ❌ 不适用 |
| **PACS** | ⚠️ 部分支持 | ⚠️ 部分支持 | ⚠️ 部分支持 | ✅ 支持 | ❌ 不适用 | ❌ 不适用 | ✅ 支持 |
| **医保** | ⚠️ 部分支持 | ⚠️ 部分支持 | ⚠️ 部分支持 | ⚠️ 部分支持 | ❌ 不适用 | ❌ 不适用 | ❌ 不适用 |
| **OA** | ⚠️ 部分支持 | ⚠️ 部分支持 | ⚠️ 部分支持 | ⚠️ 部分支持 | ❌ 不适用 | ❌ 不适用 | ❌ 不适用 |

**图例：** ✅ 完整支持 | ⚠️ 部分支持/需要扩展 | ❌ 不适用

### 3.2 标准适用场景

| 标准 | 主要用途 | 适用系统 | 典型场景 |
|------|---------|---------|---------|
| **HL7 v2** | 传统消息交换 | HIS/EMR/LIS | 患者入院、医嘱下达、检验结果回报 |
| **FHIR** | 现代RESTful API | HIS/EMR/LIS | 资源查询、数据订阅、移动端集成 |
| **CDA** | 临床文档交换 | HIS/EMR | 病历文档、出院小结、转诊记录 |
| **IHE** | 集成规范 | PACS/LIS | 影像获取、检验工作流 |
| **CDS Hooks** | 临床决策支持 | HIS/EMR/LIS | 医嘱提醒、诊断建议、安全警示 |
| **SMART on FHIR** | 应用集成框架 | HIS/EMR | 第三方应用嵌入、数据可视化 |
| **DICOM** | 医学影像标准 | PACS | 影像存储、传输、显示 |

## 4. 详细适配方案

### 4.1 HIS系统适配

#### 4.1.1 HL7 v2适配
- **消息类型**：ADT（入出院）、ORM（医嘱）、ORU（检验结果）
- **集成方式**：MLLP/TCP Socket
- **数据映射**：
  - 患者信息 → PID段
  - 医嘱信息 → ORC段
  - 诊断信息 → DG1段

#### 4.1.2 FHIR适配
- **资源类型**：Patient、Encounter、Condition、MedicationRequest
- **API端点**：RESTful API with JSON
- **认证方式**：OAuth2.0 + SMART on FHIR

#### 4.1.3 CDS Hooks适配
- **Hook类型**：patient-view、order-select、order-sign
- **调用时机**：医生工作站打开患者、选择医嘱、签署医嘱
- **返回内容**：卡片式提醒、建议操作、证据链接

### 4.2 EMR系统适配

#### 4.2.1 CDA文档交换
- **文档类型**：入院记录、病程记录、出院小结
- **文档结构**：CDA R2标准
- **传输方式**：XDS.b（IHE集成规范）

#### 4.2.2 FHIR DocumentReference
- **资源映射**：EMR文档 → FHIR DocumentReference
- **元数据**：文档类型、创建时间、作者、患者标识
- **内容**：PDF/CDA文档引用

### 4.3 LIS系统适配

#### 4.3.1 HL7 v2消息
- **消息类型**：ORM（检验医嘱）、OUL（检验结果）
- **数据格式**：OBX段携带检验结果
- **异常标识**：HL7异常标志（H/L/A/N）

#### 4.3.2 FHIR DiagnosticReport
- **资源映射**：检验报告 → FHIR DiagnosticReport
- **包含资源**：Observation（检验项）、Specimen（标本）
- **状态管理**：registered → preliminary → final → amended

### 4.4 PACS系统适配

#### 4.4.1 DICOM标准
- **服务类**：C-STORE（存储）、C-FIND（查询）、C-MOVE（传输）
- **SOP类**：CT、MRI、X-Ray、Ultrasound
- **传输语法**：JPEG2000、JPEG-LS、RLE

#### 4.4.2 IHE XDS-I
- **集成规范**：跨企业文档共享-影像
- **元数据**：ImagingStudy资源
- **访问方式**：WADO-RS（Web Access to DICOM Objects）

### 4.5 医保系统适配

#### 4.5.1 接口规范
- **通信协议**：HTTPS/RESTful
- **数据格式**：JSON/XML（医保局规范）
- **认证方式**：数字证书 + API Key

#### 4.5.2 数据映射
- **医保目录**：药品目录、诊疗项目、疾病诊断
- **结算信息**：费用明细、报销比例、自付金额
- **审核结果**：通过、拒绝、需人工审核

## 5. 适配器架构设计

### 5.1 适配器分层

```
┌─────────────────────────────────────┐
│        业务服务层（规则/路径/质控）    │
├─────────────────────────────────────┤
│        适配器中心（AdapterHub）       │
├─────────────────────────────────────┤
│        协议适配层                    │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐   │
│  │HL7  │ │FHIR │ │CDA  │ │DICOM│   │
│  │适配器│ │适配器│ │适配器│ │适配器│   │
│  └─────┘ └─────┘ └─────┘ └─────┘   │
├─────────────────────────────────────┤
│        数据转换层                    │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐   │
│  │消息  │ │资源  │ │文档  │ │影像  │   │
│  │解析器│ │转换器│ │解析器│ │处理器│   │
│  └─────┘ └─────┘ └─────┘ └─────┘   │
├─────────────────────────────────────┤
│        连接管理层                    │
│  连接池 | 重试 | 熔断 | 监控         │
└─────────────────────────────────────┘
```

### 5.2 适配器配置模型

#### 5.2.1 适配器定义表（ADP_ADAPTER_DEF）
```sql
CREATE TABLE ADP_ADAPTER_DEF (
    adapter_id VARCHAR2(64) PRIMARY KEY,
    adapter_code VARCHAR2(64) NOT NULL,
    adapter_name VARCHAR2(200) NOT NULL,
    adapter_type VARCHAR2(32) NOT NULL, -- HL7/FHIR/CDA/DICOM/REST/SQL
    source_system VARCHAR2(32) NOT NULL, -- HIS/EMR/LIS/PACS/INSURANCE/OA
    protocol VARCHAR2(32) NOT NULL, -- MLLP/HTTP/HTTPS/SOAP/DICOM
    base_url VARCHAR2(500),
    auth_type VARCHAR2(32), -- NONE/BASIC/OAUTH2/CERT/APIKEY
    timeout_ms NUMBER(10) DEFAULT 30000,
    retry_count NUMBER(3) DEFAULT 3,
    tenant_id VARCHAR2(64) NOT NULL,
    hospital_code VARCHAR2(64) NOT NULL,
    status VARCHAR2(16) DEFAULT 'ACTIVE',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 5.2.2 查询定义表（ADP_QUERY_DEF）
```sql
CREATE TABLE ADP_QUERY_DEF (
    query_id VARCHAR2(64) PRIMARY KEY,
    adapter_id VARCHAR2(64) NOT NULL,
    query_code VARCHAR2(64) NOT NULL,
    query_name VARCHAR2(200) NOT NULL,
    query_type VARCHAR2(32) NOT NULL, -- READ/WRITE/SUBSCRIBE
    request_template CLOB, -- 请求模板
    response_mapping CLOB, -- 响应映射规则
    fhir_resource_type VARCHAR2(64), -- FHIR资源类型
    hl7_message_type VARCHAR2(32), -- HL7消息类型
    dicom_sop_class VARCHAR2(64), -- DICOM SOP类
    sample_data CLOB, -- 样例数据
    tenant_id VARCHAR2(64) NOT NULL,
    hospital_code VARCHAR2(64) NOT NULL,
    status VARCHAR2(16) DEFAULT 'ACTIVE',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 6. 实施计划

### 6.1 第一阶段：基础框架（当前）
1. 创建适配矩阵文档（本文档）
2. 设计适配器配置表结构
3. 扩展AdapterHubService支持多协议
4. 创建样例数据和测试用例

### 6.2 第二阶段：协议实现
1. 实现HL7 v2消息解析器
2. 实现FHIR资源转换器
3. 实现CDA文档处理器
4. 实现DICOM连接管理器

### 6.3 第三阶段：系统集成
1. HIS系统HL7 v2集成
2. EMR系统CDA文档交换
3. PACS系统DICOM集成
4. 医保系统RESTful集成

### 6.4 第四阶段：高级功能
1. CDS Hooks集成
2. SMART on FHIR应用框架
3. 数据订阅和事件通知
4. 性能监控和告警

## 7. 验收标准

### 7.1 功能验收
1. 适配矩阵文档完整，覆盖所有系统×标准组合
2. 适配器配置表结构设计完成，支持多数据库
3. 样例数据完整，包含所有系统类型的查询定义
4. 单元测试覆盖核心转换逻辑

### 7.2 性能验收
1. 单次适配器查询响应时间 < 100ms（本地网络）
2. 并发支持 ≥ 100个适配器实例
3. 消息解析吞吐量 ≥ 1000条/秒

### 7.3 安全验收
1. 所有外部连接支持加密传输
2. 敏感数据（患者信息）在日志中脱敏
3. 适配器凭据安全存储

## 8. 风险与缓解

### 8.1 技术风险
- **风险**：院内系统接口不标准
- **缓解**：提供配置化映射，支持自定义转换规则

### 8.2 集成风险
- **风险**：系统版本不兼容
- **缓解**：版本检测和降级策略

### 8.3 性能风险
- **风险**：大量并发请求导致系统过载
- **缓解**：连接池、限流、熔断机制

## 9. 参考资料

1. HL7 v2.5.1标准
2. FHIR R4规范
3. CDA R2标准
4. IHE技术框架
5. CDS Hooks规范
6. SMART on FHIR规范
7. DICOM标准

## 10. 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
|------|------|---------|------|
| 2026-05-20 | 1.0 | 初始版本，建立适配矩阵框架 | CodeBuddy |