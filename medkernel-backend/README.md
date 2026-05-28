# medkernel-backend

MedKernel v1.0 GA 后端工程。

当前执行为 **0 业务引擎全能力上线**：先打通知识、字典、规则、路径、推荐、评估、随访、包发布、嵌入、模型网关、审计证据和降级链路；引擎验收后再按业务服务包装。

## 技术栈

- JDK 21 LTS（Temurin / KAE-JDK21 / LoongJDK21）
- Spring Boot 3.3.5（Jakarta EE 9）
- Spring Security 6.3（OAuth2 Resource Server）
- Spring Data JDBC 3.3 + HikariCP 5
- Flyway 10.20（5 方言：PostgreSQL / Oracle / 达梦 / 人大金仓 / H2）
- BouncyCastle 1.78.1（SM2/SM3/SM4 + FIPS 路径预留）
- springdoc-openapi 2.6
- OpenTelemetry 1.41 埋点预留 + Micrometer Prometheus 指标
- Neo4j Driver 5.23

## 启动

```bash
cd medkernel-backend
mvn spring-boot:run
```

启动后访问：

- `http://localhost:18080/medkernel/api/v1/system/ping`
- `http://localhost:18080/medkernel/actuator/health`
- `http://localhost:18080/medkernel/swagger-ui.html`

## 包结构

当前后端不按业务菜单拆泳道，而是按引擎能力与共享底座组织代码。当前以 [docs/MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md §17](../docs/MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md#17-0-业务引擎全能力上线计划) 和 [docs/backlog.md](../docs/backlog.md) 为准。

```
com.medkernel
├── engine       引擎能力与底座上下文（org/context/knowledge/terminology/rule/pathway/recommendation/evaluation/followup/package/embed/llm/list/security）
├── shared       共享技术能力（api/context/crypto/datascope/observability/persistence/runtime/security/web）
└── compliance   合规审计入口（audit）
```

## 代码基线净化

当前已落地 E1/E2 多数接口和部分 E3 执行链，但不能把“接口存在”直接等同于“引擎全能力验收完成”。AI 团队继续开发时必须保持：

- 新业务接口使用 Record DTO + Bean Validation，禁止新增裸 `Map<String,Object>` 输入。
- 生产主链路不能依赖 mock、硬编码示例数据或单病种假闭环。
- 每个引擎能力必须具备组织上下文、权限、审计、状态、版本、错误码和 traceId。
- 模型、Dify、图投影只能通过统一网关或投影层接入，故障时必须降级并留痕。
- HIS/EMR/LIS/PACS/手麻/输血/护理/医保/公卫/区域平台/模型 Provider 等第三方系统只能通过适配器、标准上下文、临床事件、嵌入、回调、包发布同步和审计证据链接入，不得在业务模块中直连并绕过引擎。
- 第三方接口进入联调前必须具备接入概览、OpenAPI/事件 schema、字段映射、鉴权签名、幂等重试、回调、降级和验收证据文档。

## 关联文档

- [产品宪法](../docs/CONSTITUTION.md)
- [基础底座与引擎服务能力总览](../docs/MEDKERNEL_FOUNDATION_AND_SERVICES.md)
- [产品体验固定规范](../docs/MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md)
- [0 业务引擎全能力上线实施方案](../docs/MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md)
- [当前任务台账](../docs/backlog.md)
