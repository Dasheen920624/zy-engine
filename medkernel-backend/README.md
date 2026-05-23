# medkernel-backend

MedKernel v1.0 GA 后端骨架。

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

按业务域，不按技术层。详见 [docs/V1_GA_REWRITE_PLAN.md §4.1](../docs/V1_GA_REWRITE_PLAN.md)。

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

## 关联文档

- [产品宪法](../docs/CONSTITUTION.md)
- [v1.0 GA 12 周方案](../docs/V1_GA_REWRITE_PLAN.md)
- [GA 任务台账](../docs/backlog.md)
