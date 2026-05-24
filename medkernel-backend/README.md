# medkernel-backend

MedKernel v1.0 GA 后端骨架。

当前执行为 **0 业务引擎全能力上线**：先打通知识、字典、规则、路径、推荐、评估、随访、包发布、嵌入、模型网关、审计证据和降级链路；引擎验收后再按业务服务包装。

## 技术栈

- JDK 21 LTS（Temurin / KAE-JDK21 / LoongJDK21）
- Spring Boot 3.3.5（Jakarta EE 9）
- Spring Security 6.3（OAuth2 Resource Server）
- Spring Data JDBC 3.3 + HikariCP 5
- Flyway 10.20（5 方言：PostgreSQL / Oracle / 达梦 / 人大金仓 / H2）
- BouncyCastle 1.78.1（SM2/SM3/SM4 + FIPS 路径预留）
- springdoc-openapi 2.6
- OpenTelemetry 1.41 + Micrometer Prometheus
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

保留现有包边界，但当前实施不按业务菜单拆泳道，而是按引擎能力上线。当前以 [docs/MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md §17](../docs/MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md#17-0-业务引擎全能力上线计划) 和 [docs/backlog.md](../docs/backlog.md) 为准。

```
com.medkernel
├── shared       共享技术能力（crypto/persistence/web/observability/security）
├── tenant       试点准备域
├── clinical     临床运行域
├── quality      质控改进域
├── compliance   合规运维域
├── advanced     高级工具域
└── platform     平台底座（license/migration/release）
```

## 代码基线净化

当前后端代码作为历史骨架保留，但不能作为业务完成证明。AI 团队开发时必须逐步完成：

- 新业务接口使用 Record DTO + Bean Validation，禁止新增裸 `Map<String,Object>` 输入。
- 生产主链路不能依赖 mock、硬编码示例数据或单病种假闭环。
- 每个引擎能力必须具备组织上下文、权限、审计、状态、版本、错误码和 traceId。
- 模型、Dify、图投影只能通过统一网关或投影层接入，故障时必须降级并留痕。

## 关联文档

- [产品宪法](../docs/CONSTITUTION.md)
- [基础底座与引擎服务能力总览](../docs/MEDKERNEL_FOUNDATION_AND_SERVICES.md)
- [产品体验固定规范](../docs/MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md)
- [0 业务引擎全能力上线实施方案](../docs/MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md)
- [当前任务台账](../docs/backlog.md)
