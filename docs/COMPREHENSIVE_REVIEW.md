# MedKernel · 全方位最终评审报告（出厂前 QA）

> 版本：1.0 · 2026-05-21  
> 评审人：架构组（Claude Opus 4.7 切换为「客户 CTO + 信通院评测专家 + 等保测评师 + 内网运维 + 外网 SRE」复合视角）  
> 适用：本文是 **v0.3 → v1.0 GA 的出厂前 QA 唯一清单**，覆盖 **20 个落地维度**，每维度 ✅/🟡/🔴 三态 + P0/P1/P2 行动项。  
> 配套：[`PRODUCT_ARCHITECTURE_FINAL.md`](PRODUCT_ARCHITECTURE_FINAL.md)（架构）+ [`v0.3-DEMO-REDESIGN.md`](v0.3-DEMO-REDESIGN.md)（演示）+ [`DEPLOYMENT_DUAL_MODE.md`](DEPLOYMENT_DUAL_MODE.md)（内外网部署）+ [`AI_TEAM_SOP.md`](AI_TEAM_SOP.md)（团队）+ [`AI_CHARTER.md`](AI_CHARTER.md)（红线）

---

## 0. 评审总览（一张表）

| # | 维度 | 当前状态 | 离 v1.0 GA 缺口 | 优先级 |
|---|---|:---:|---|:---:|
| 1 | **产品定位 / 三产品分层** | 🟢 | 命名统一执行（PR-V3-00） | P0 |
| 2 | **架构层次 / 模块边界** | 🟡 | 双副本归并（ADR-0005/6）+ 超长文件拆 | P0 |
| 3 | **代码质量 / 规范一致性** | 🟡 | 35 Controller DTO 化 + Jackson SNAKE_CASE | P1 |
| 4 | **数据库 5 方言一致性** | 🟡 | **KingbaseES 独立 smoke** + 跨方言回归矩阵 | P0 |
| 5 | **API 契约 / OpenAPI** | 🔴 | **OpenAPI 3.0 文档零** — 必须自动化生成 | P0 |
| 6 | **前端工程化** | 🟡 | code-splitting（bundle 已 761KB 逼近 800KB 红线）+ i18n 框架 | P1 |
| 7 | **后端工程化** | 🔴 | **HikariCP 0 接入** + 缓存层 0 + MQ 0 + 缺连接池监控 | P0 |
| 8 | **测试体系** | 🟡 | E2E 框架 0 + 性能压测 0 + 安全/混沌 0 | P1 |
| 9 | **安全 — 等保 2.0 三级** | 🔴 | **未做评测** — 等保需要 30 天 + ¥20-50 万 | P0 |
| 10 | **安全 — 国密 SM2/SM3/SM4** | 🔴 | **0 集成** — 信通院评测必备 | P0 |
| 11 | **合规 — 个保法 / 数据出境** | 🔴 | **0 落地** — Dify 调用属于跨境数据 | P0 |
| 12 | **国情合规 — ICP / 公安备案 / 节假日** | 🔴 | 登录页 0 国情元素，节假日 0 处理 | P0 |
| 13 | **内网部署（医院本地）** | 🟢 | 6 套脚本就绪，但 KingbaseES + 离线 yum repo 缺验证 | P1 |
| 14 | **外网部署（SaaS）** | 🔴 | **0 准备** — 无 WAF / CDN / DDoS / 多租户隔离方案 | P0 |
| 15 | **运维 / 监控 / 告警** | 🔴 | **0 集成** — 无 Prometheus / Grafana / 日志聚合 / 链路追踪 | P0 |
| 16 | **CI/CD / Release** | 🟡 | CI 有 2 job，缺 npm 验证 / 跨方言 smoke / artifact 签名 | P1 |
| 17 | **文档体系** | 🟢 | 5 金本位 + 30+ 工程规范 + 8 顶层核心已建立 | — |
| 18 | **i18n / 本地化** | 🔴 | **0 框架** — 仅中文硬编码，没法外销港澳台/海外 | P2 |
| 19 | **商业化能力** | 🔴 | **0 计费** — 无订阅 / 用量 / 对账 / 发票 | P1 |
| 20 | **团队流程 / 法务 / IP** | 🟡 | AI 团队 SOP 已建；软著/专利/开源审计 0 | P1 |

**总览结论**：v0.2-demo → v0.3-pilot 约 **8 周**，v0.3-pilot → v1.0 GA 约 **16 周**（含等保测评 4-6 周 + 信通院 4 周）。总周期 **24 周（约 5.5 个月）到正式商业化**。

---

## 1. 产品定位 / 三产品分层 — 🟢

**当前状态**：
- 三产品 ADR-0001 已 Accepted（A 知识工厂 / B 临床嵌入器 / C 质控驾驶舱）
- 22 不变量已写入 `01_产品事实源.md §7`
- PRODUCT_ARCHITECTURE_FINAL §1 已固化「四大模块」终态命名（M1-M4）

**缺口**：
- 命名「配置治理 → 知识工厂」「运营治理 → 质控驾驶舱」未在前端 menuConfig 落地
- 「医疗智能引擎平台」对外宣传名仍占据登录页 Title

**行动项**：
- [P0] PR-V3-00：tokens.css 主色迁移 + 全前端 Title 改「集团医疗智能中枢 MedKernel」

---

## 2. 架构层次 / 模块边界 — 🟡

**当前状态**：
- 四层架构清晰（Provider 抽象 / Service / Repository / Controller）
- 5 数据库方言接入抽象（Oracle / DM / PG / Kingbase / H2）
- 55 个 Controller 已归类到 4 大模块

**缺口**：
- 6 个 Service 超 1000 行（最重 EnginePersistenceService 2175）— 单文件无法人脑覆盖
- MpiController / UserSyncController 双副本未归并
- CDSS 5 个 Controller 职责重叠，需合并为 facade
- `persistence/` 包是「上帝包」，应按领域拆到各业务包

**行动项**：
- [P0] ADR-0005 + ADR-0006 起草 + 双副本归并
- [P0] PR-V3-SPLIT-FILE 拆 6 个超长 Service（19 个工作日，可 5 AI 并行 1 周）
- [P1] PR-V3-CDSS-REFACTOR

---

## 3. 代码质量 / 规范一致性 — 🟡

**当前状态**：
- `mvn compile` ✅ / `mvn test` ✅ / 前端全套 ✅ / lint 0 warn
- 4 项硬门禁（包名 / ON DUPLICATE / DriverManager / raw Map）已立 verify-pr
- 编译警告（被 -q 静默）数未知

**缺口**：
- 35 个 Controller 用 raw Map（KD-OPEN，verify-pr 拦了新增但存量仍在）
- Jackson SNAKE_CASE 未启用（KD-005）— 前后端契约对不齐
- 后端测试覆盖率未知（无 jacoco 配置）
- 前端代码覆盖率未知（无 c8 / istanbul 配置）

**行动项**：
- [P0] PR-V3-SNAKE-CASE（Jackson 全局启用 + 修 30 测试断言）
- [P1] PR-V3-DTO-P0（3 P0 Controller 改 DTO）
- [P1] 加 jacoco（后端覆盖率 ≥ 60% 准入）+ vitest coverage（前端 ≥ 40%）

---

## 4. 数据库 5 方言一致性 — 🟡

**当前状态**：
- `db/oracle/` `db/dm/` `db/postgres/` 三个目录有 6 套 DDL（core / comments / org_migration / mpi / rule_eval_result / data_governance）
- 每个目录有 README + 文件对齐
- LOCAL_H2 通过 application 自动 init

**缺口**：
- **KingbaseES 没有独立目录** — README 说「PG 兼容模式启动等价」，但实际：
  - KingbaseES V8 的存储过程语法、`SYSDATE` 等差异未测
  - PG 14/15/16 与 KingbaseES V8 PG 兼容模式的 SQL 行为差异未矩阵化
  - locale `zh_CN.UTF-8` 在 KingbaseES 默认安装是否可用未验证
- 5 方言 smoke 实测证据只在 Oracle 跑（其它仅靠 DDL 文件存在但未实跑）
- 没有 DB migration tool（Flyway / Liquibase）— 升级靠人工跑 SQL，install-offline.sh 第 60 行显式让 DBA 手动 `sqlplus`
- 没有 `.down.sql` 回滚脚本

**行动项**：
- [P0] PR-V3-KINGBASE-SMOKE：建 KingbaseES V8 测试实例 + 跑 5 个 smoke 矩阵 + 出 known-issue 报告
- [P0] PR-V3-FLYWAY：引入 Flyway 自动化 migration + 改造现有 6 套 DDL 为 Flyway script + 加 `.down` 回滚
- [P1] PR-V3-DB-MATRIX：建立 4 方言每月自动 smoke 矩阵（GitHub Actions matrix job）

---

## 5. API 契约 / OpenAPI — 🔴

**当前状态**：
- `docs/engineering/api-examples.http` 手工 HTTP 样例
- 55 Controller 端点散落
- 前端 `frontend/src/api/types.ts` 754 行手工维护类型
- 没有自动化 OpenAPI 文档

**缺口**：
- **零 OpenAPI 3.0 文档** — 外网客户接 API 无规范，无 SDK 自动生成可能
- 前端 types.ts 与后端 DTO 类型漂移风险（每次后端改字段，前端手工同步）
- 没有 API 版本管理策略（`/v1/` 用了但 `/api/` 没用 v1 的）

**行动项**：
- [P0] PR-V3-OPENAPI：引入 springdoc-openapi（Spring Boot 2.7 兼容版）+ 自动生成 `/v3/api-docs` + Swagger UI（仅内网开 + 鉴权后）
- [P0] PR-V3-API-VERSION：制定 API 版本规则（`/api/v1/*` 为标准，`/api/*` 平台底座可不带），写 ADR-0011
- [P1] PR-V3-TYPE-GEN：用 openapi-typescript 自动生成 `types.ts`，删手工维护

---

## 6. 前端工程化 — 🟡

**当前状态**：
- React 18 + TS strict + Vite 5 + AntD 5 + react-router 6 + axios + TanStack Query + MSW 2 + vitest
- ESLint 自定义规则 3 条
- 39/39 单测通过

**缺口**：
- Bundle gzip **761KB**，距离 800KB 红线只剩 39KB；vendor-antd 单独 1.3MB 未 split
- **无 i18n 框架**（react-i18next 等）— 所有中文硬编码在 JSX
- 无 PWA / 离线缓存策略
- 无 Service Worker
- 无 Sentry / 错误监控
- 浏览器兼容矩阵未声明（IE11？Edge 旧版？）
- 无 a11y 自动检测（axe-core）

**行动项**：
- [P0] PR-V3-CODESPLIT：vendor-antd 按需 import（dayjs / icons / each module）+ 路由级 lazy
- [P1] PR-V3-I18N：引入 react-i18next + 提取所有中文为 zh-CN.json + 准备 zh-TW / en-US 占位
- [P1] PR-V3-SENTRY：集成 Sentry（内网部署默认关，外网 SaaS 启用）
- [P2] PR-V3-A11Y：vite-plugin-axe + 关键页面无障碍报告

---

## 7. 后端工程化 — 🔴

**当前状态**：
- Spring Boot 2.7.18 / JDK 1.8 / Maven 3
- 55 Controller / 28 Java 包 / 248 单测
- 主依赖：web、validation、websocket、neo4j-driver、h2、spring-security-crypto、jjwt 0.11.5、ojdbc8

**缺口**：
- **HikariCP 零接入**（pom 缺 `spring-boot-starter-jdbc`） — 29 处 `DriverManager.getConnection`，KD-004
- **零缓存层** — 无 Redis / Caffeine；rule eval / graph query / org context 都直查 DB
- **零消息队列** — 无 RabbitMQ / Kafka / RocketMQ；Dify 异步、AI 知识 job 都是同步阻塞
- **零分布式协调** — 无 ZooKeeper / Etcd / Consul；多实例部署 lock 靠 DB
- **零熔断限流** — 无 Sentinel / Resilience4j；外部 Provider 抖动会拖垮全系统
- **零 actuator endpoints** — application.yml 没 management 配置，无健康/metrics 暴露

**行动项**：
- [P0] PR-V3-HIKARI（已拆，最高优先级）
- [P0] PR-V3-ACTUATOR：开启 `management.endpoints.web.exposure.include=health,info,metrics,prometheus`（仅 internal 端口 18081 暴露 + 鉴权）
- [P0] PR-V3-RESILIENCE：引入 Resilience4j 包装 Neo4j / Dify / 第三方适配器调用（熔断 + 重试 + 限时）
- [P1] PR-V3-CACHE：引入 Caffeine 本地缓存（rule / org context）+ Redis 二级缓存（外网 SaaS 部署）
- [P1] PR-V3-MQ：评估 RabbitMQ vs Kafka，Dify 异步调用走 MQ

---

## 8. 测试体系 — 🟡

**当前状态**：
- 后端 248 个 JUnit 单测 + Spring Boot Test 契约测试
- 前端 39 个 vitest（覆盖 StatusBadge / SourceInfo / AiBadge / ProvidersStatus）
- API 样例 `api-examples.http`
- smoke plan 3 个：rule-eval-result / cfg-config-package / ddl-consistency

**缺口**：
- **零 E2E 框架**（Playwright 已装但 `e2e/` 目录空）
- **零性能压测**（JMeter / wrk / k6 都没有 config）
- **零安全测试**（无 OWASP ZAP / Burp Suite 集成）
- **零混沌测试**（无 Chaos Monkey）
- **零契约测试自动化跨方言**（只在 H2/Oracle 跑过）
- 后端 jacoco 未开，实际覆盖率不可见
- 前端 vitest 未配 coverage，实际覆盖率不可见

**行动项**：
- [P0] PR-V3-E2E：补 Playwright e2e/ 实际 case（6 大演示剧本各 1 个 E2E）
- [P0] PR-V3-COVERAGE：jacoco + vitest --coverage 启用 + CI 报告
- [P1] PR-V3-PERF：k6 压测脚本（核心 API 100 并发 30 min）+ 性能基线报告
- [P1] PR-V3-OWASP：OWASP ZAP 自动化扫描（夜间 nightly job）
- [P2] PR-V3-CHAOS：Chaos Monkey 注入（Provider 抖动 / DB 断连）

---

## 9. 安全 — 等保 2.0 三级 — 🔴

**等保 2.0 三级要求**（医疗信息系统必备）：

| 控制项 | 当前状态 | 行动 |
|---|---|---|
| 8.1.4.1 身份鉴别 — 密码复杂度 / 失败锁定 / 会话超时 | 🟡 lock-threshold/duration 已配，密码复杂度未强制 | PR-V3-COMPLIANCE-BACKEND |
| 8.1.4.1 身份鉴别 — 多因素认证 | 🔴 0 | PR-V3-MFA |
| 8.1.4.2 访问控制 — RBAC 最小权限 | 🟡 角色已有但权限矩阵未文档化 | PR-V3-RBAC-MATRIX |
| 8.1.4.3 安全审计 — 操作日志 / 不可篡改 | 🟢 `ENGINE_AUDIT_LOG` + 审计链验签 SEC-010 | — |
| 8.1.4.4 入侵防范 — WAF / 主机加固 | 🔴 0 — 内网靠医院网络隔离，外网 0 | DEPLOYMENT_DUAL_MODE §3 |
| 8.1.4.5 恶意代码防范 | 🔴 0 — 无 ClamAV 等 | 内网部署文档加 |
| 8.1.4.6 可信验证 — 签名 | 🟡 release artifact 有 sha256，无 GPG 签名 | PR-V3-SIGN |
| 8.1.4.7 数据完整性 — 传输/存储 | 🟡 HTTPS 有，DB 透明加密未启 | PR-V3-TDE |
| 8.1.4.8 数据保密性 — 加密存储 | 🔴 患者数据明文存 DB | PR-V3-FIELD-CRYPTO |
| 8.1.4.9 数据备份恢复 | 🟡 deploy/scripts 有备份，无恢复演练 | PR-V3-BACKUP-DRILL |
| 8.1.4.10 剩余信息保护 | 🔴 0 — 删除用户数据未清理缓存/日志 | PR-V3-GDPR |
| 8.1.4.11 个人信息保护 | 🔴 0 — 详见 §11 个保法 | PR-V3-COMPLIANCE-BACKEND |

**预算**：等保 2.0 三级测评 ¥20-50 万 + 30-60 天周期（含 5 个工作环节）

**行动项**：
- [P0] 立项「等保 2.0 三级测评」预算 + 选定测评机构（推荐：信通院、公安部一所、中国电子技术标准化研究院）
- [P0] PR-V3-SECURITY-BASELINE：把上表 12 项控制点全部落代码 + 文档
- [P0] PR-V3-PENTEST：内部渗透测试（小厂可外包，¥5-10 万）

---

## 10. 安全 — 国密 SM2/SM3/SM4 — 🔴

**当前状态**：
- 使用国际标准算法（JWT 用 HMAC-SHA256，密码用 BCrypt，HTTPS 用 RSA + AES）
- **零国密集成**（grep 全仓 `SM2|SM3|SM4|国密` 没找到任何代码引用）

**缺口**：
- 信通院、公安部、卫健委的「商用密码合规」要求医疗信息系统**逐步全面替换**国际算法为国密
- 国密算法清单：
  - **SM2** — 公钥密码（替代 RSA / ECC），用于数字签名、密钥交换
  - **SM3** — 杂凑算法（替代 SHA-256），用于审计链 hash、密码加盐
  - **SM4** — 分组密码（替代 AES），用于患者数据字段加密、配置包加密

**行动项**：
- [P0] PR-V3-GMSSL：评估并引入 BouncyCastle 国密扩展 或 Tongsuo（铜锁）库；JWT 改用 SM2 签名、密码改 SM3 加盐、敏感字段 SM4 加密
- [P0] PR-V3-CRYPTO-SUITE：application.yml 加 `medkernel.security.crypto-suite=SM | RSA-AES` 双套支持，按部署环境切换（医院老系统先 RSA-AES，新系统强制 SM）
- [P1] 拿到「商用密码应用安全性评估」资质（密评，¥10-30 万）

---

## 11. 合规 — 个保法 / 数据出境 — 🔴

**法规清单**（医疗 + 个保叠加）：

| 法规 | 关键要求 | 当前 | 行动 |
|---|---|:---:|---|
| 《个人信息保护法》（2021.11）| 最小化 / 知情同意 / 撤回 / 删除权 / 自动决策告知 | 🔴 | PR-V3-PII-MIN（字段最小化）+ PR-V3-CONSENT（用户协议勾选）|
| 《数据安全法》（2021.9）| 数据分级分类 / 重要数据 / 国家核心数据 | 🔴 | PR-V3-DATA-CLASS（数据分级标签） |
| 《数据出境安全评估办法》（2022.9）| 100 万人以上个人信息出境必评估；Dify 调用 OpenAI 即跨境 | 🔴 | PR-V3-DATA-CROSS（Dify 调用前 banner + 审计 + 评估申请） |
| 《医疗卫生机构网络安全管理办法》（2022.8）| 医疗数据本地化、卫健委备案 | 🔴 | DEPLOYMENT_DUAL_MODE §4 |
| 《互联网诊疗监管细则》（2022.3）| 医疗机构互联网诊疗信息留存 ≥ 15 年 | 🔴 | PR-V3-RETENTION（数据归档策略） |
| 《医疗器械软件注册技术审查指导原则》（NMPA）| 三类医疗器械软件需要注册 | ⚠️ | 法务咨询：本系统是否构成三类（CDSS 可能算）|
| GB/T 35273-2020 《个人信息安全规范》| 11 类敏感个人信息（含医疗健康）| 🔴 | PR-V3-PII-MIN |
| ISO 27001 / ISO 27799 | 国际医疗信息安全标准 | 🔴 | v1.1 GA 后考虑 |

**关键风险**：
- **Dify 调用大模型 = 跨境数据** — 即使医院在国内，Dify Cloud 默认在境外。必须：
  - 私有化部署 Dify（推荐）
  - 或者 → 限制只调国内大模型（通义 / 文心 / 智谱 / 月之暗面 / DeepSeek）
  - 或者 → 数据出境评估申请（30-60 天周期）

**行动项**：
- [P0] PR-V3-DATA-CLASS：所有 entity 标 `data_class=PUBLIC|INTERNAL|SENSITIVE|HEALTH_DATA`；HEALTH_DATA 类强制 SM4 加密 + 行级权限
- [P0] PR-V3-DIFY-DOMESTIC：Dify 配置改为只允许调国内大模型 provider（白名单），跨境模型 banner 阻断
- [P0] PR-V3-CONSENT：用户协议 / 隐私政策 / 服务条款 3 份模板（法务草案），登录页强制勾选

---

## 12. 国情合规 — ICP / 公安备案 / 节假日 — 🔴

**当前状态**（已在 `v0.3-DEMO-REDESIGN.md §2.1/§2.2` 列 19 条）：
- 登录页 0 国情元素（无 ICP / 无公安备案 / 无手机短信 / 无国密 / 无 MFA / 无协议勾选）
- 演示账号 `zhao01 / demo123` 明文写在卡片
- 全局 0 法定节假日处理（春节、国庆等可能影响排班 / 评估周期）
- 全局 0 时区显式声明（Asia/Shanghai 默认依赖 JVM）

**新发现的缺口**（v0.3-DEMO-REDESIGN 未覆盖）：
- 电子证照对接（医师执业证、护士执业证 — 卫健委发的电子证件）
- 医保电子凭证对接（国家医保局统一规范）
- 健康码 / 行程码对接（应急能力，疫情期间复用）
- 农历日历支持（中医诊疗会用）
- 中国节假日表（春节、清明、五一、端午、中秋、国庆）
- 民族 56 项枚举（患者档案需要）
- 行政区划 6 位编码（GB/T 2260）
- 身份证 18 位校验（GB 11643-1999）
- 简繁体兼容（部分医院仍用繁体）

**行动项**：
- [P0] PR-V3-LOGIN（已拆）— 落地登录页 12 条国情清单
- [P0] PR-V3-CHINA-LOCALE：节假日 / 农历 / 民族 / 行政区划 / 身份证校验 5 套基础库
- [P0] PR-V3-MEDICAL-CARD：医保电子凭证 + 电子证照对接 SDK
- [P1] PR-V3-TZ：全局 `spring.jackson.time-zone=Asia/Shanghai`，所有 entity 时间字段统一 `OffsetDateTime`，前端 dayjs 设 zh-cn locale

---

## 13. 内网部署（医院本地）— 🟢

**当前状态**（实地核查 `deploy/`）：
- 6 套脚本 ×2 平台（Linux .sh + Windows .ps1）：build-release / check-env / install-offline / upgrade / rollback / healthcheck
- 4 个 profile env 模板（centos7/uos/kylin × oracle/dm/pg）
- nginx 2 套（http + tls）
- systemd 单元
- manifest.template.json + sha256 校验

**缺口**：
- `install-offline.sh` 跑 Oracle/DM DDL 仍是 `log_info "请执行...sqlplus"` 让 DBA 手动，没自动化（PG 是自动化的）
- 离线 yum repo / apt repo 打包未提及（医院网络可能无 EPEL / docker-ce 源）
- 「随包带 JDK」选项有但默认关，需文档说明
- 没有部署演练录像 / 实施工程师认证流程
- 没有「医院 IT 培训手册」
- 没有「故障应急手册」（断电 / 数据损坏 / Oracle 实例宕机怎么办）
- 没有 SLA 模板（提供给医院签）

**行动项**：
- [P1] PR-V3-DEPLOY-AUTO：install-offline.sh 改造为 Oracle/DM 也自动跑 DDL（用 `sqlplus -L < file` / `disql ... < file`）
- [P1] PR-V3-OFFLINE-REPO：build-release 加 `--offline-repo` 选项，打包 EPEL / docker-ce / postgres-repo 镜像
- [P1] PR-V3-IMPLEMENT-DOCS：实施手册（200 页）+ 培训 PPT（医院 IT 一上午能学会）+ 故障应急手册
- [P2] PR-V3-SLA：3 套 SLA 模板（白金 99.95% / 黄金 99.9% / 标准 99.5%）

---

## 14. 外网部署（SaaS）— 🔴

**当前状态**：**0 准备**。所有现有内容都是内网部署。

**外网部署需要的全栈能力**（云原生方向）：

| 维度 | 内网部署 | 外网部署（SaaS）|
|---|---|---|
| 部署目标 | 医院机房一台/多台 | 云厂商 K8s 集群 |
| 域名 | 内网 IP / 内部 DNS | 公网域名（必须 ICP/公安备案） |
| 网关 | nginx 反代 | API Gateway（Kong / APISIX）+ 鉴权 |
| WAF | 0 | 必备（云 WAF 或 ModSecurity） |
| CDN | 0 | 必备（前端静态资源） |
| DDoS 防护 | 0（内网） | 必备（云厂商高防 IP） |
| 多租户隔离 | 单租户 / 一套数据 | **共享 DB schema 隔离** 或 **每租户独立 schema** 选择 |
| 容量弹性 | 固定 | 自动扩缩（HPA + Cluster Autoscaler） |
| 密钥管理 | env 变量 | KMS（阿里云 KMS / AWS KMS） |
| 证书 | 自签 / 内部 CA | Let's Encrypt / 商业 CA + ACME 自动更新 |
| 日志 | 本地文件 | 集中（ELK / 阿里云 SLS） |
| 监控 | 内部 Prometheus | 云原生（云监控 + APM）|
| 备份 | 本地 | 跨可用区 + 跨地域 |
| 计费 | 0（按项目收费） | 按租户 + 用量计费 |
| 法律 | 单医院合同 | 服务条款 + 隐私政策 + Cookie 政策 |

**外网部署的核心风险**：
1. **法律**：SaaS 涉及《数据出境安全评估办法》《医疗卫生机构网络安全管理办法》— 患者数据原则上**不允许跨境**，且医疗机构必须卫健委备案
2. **安全**：等保 2.0 三级（医疗系统强制）；公网暴露增大攻击面 10 倍
3. **合规**：必须 ICP 备案 + 公安备案 + 网络文化经营许可证（如涉及）

**行动项**：
- [P0] 商业决策：v1.0 GA 是否做 SaaS？若是 → 启动以下行动项；若否 → 标记「v2.0 后规划」
- [P0] 起草 ADR-0012：SaaS 多租户隔离模型（共享 schema + tenant_id row-level 隔离，已是当前默认；vs 独立 schema vs 独立 DB instance）
- [P0] 起草《SaaS 服务条款》+《隐私政策》+《Cookie 政策》（法务）
- [P0] 启动 ICP 备案 + 公安备案流程（20-30 天）
- [P0] 选定云厂商 + K8s 集群部署方案（推荐：阿里云 ACK + RDS Oracle 兼容 + SLS + ARMS）
- [P1] PR-V3-K8S：Helm Chart + Kustomize overlays
- 详见 [`DEPLOYMENT_DUAL_MODE.md`](DEPLOYMENT_DUAL_MODE.md)

---

## 15. 运维 / 监控 / 告警 — 🔴

**当前状态**：
- 后端日志：logback（默认配置，未优化）
- 没有 actuator endpoint 暴露
- 没有 Prometheus exporter
- 没有 Grafana 看板
- 没有 ELK / 阿里云 SLS 日志聚合
- 没有 APM / 链路追踪（SkyWalking / Pinpoint / Jaeger）
- 没有告警规则（Alertmanager / Prometheus Rules）
- 没有 SLI/SLO 定义
- 没有 on-call 流程

**行动项**：
- [P0] PR-V3-ACTUATOR：开启 management endpoints（health/info/metrics/prometheus），仅内部端口 18081 + 鉴权
- [P0] PR-V3-LOGBACK：logback-spring.xml 改造（JSON 输出 + traceId 字段 + 日志切割 / 归档）
- [P0] PR-V3-PROM：Prometheus 配置模板 + Grafana 看板 5 套（系统/JVM/DB/API/业务）
- [P0] PR-V3-SLI-SLO：定义 SLI（API P95 / 错误率 / 可用性）+ SLO（99.5% 可用性 / P95<500ms / 错误率<0.1%）
- [P1] PR-V3-APM：集成 SkyWalking（内网友好，国产）
- [P1] PR-V3-ALERT：Alertmanager 规则模板（30 条核心告警）
- [P1] PR-V3-ONCALL：on-call 排班模板 + 故障响应流程 P0/P1/P2 SOP

---

## 16. CI/CD / Release — 🟡

**当前状态**：
- GitHub Actions `MedKernel CI`：backend-build-test + guard-rules 2 个 job
- `verify-pr.ps1` 13 项门禁
- Windows runner + JDK 8 + Maven

**缺口**：
- **CI 不跑前端**（只 backend）— Node 18+ 需要单独 job
- **不跑跨方言 smoke**（只测 H2 内存库）
- **不跑覆盖率**（jacoco / vitest coverage 没开）
- **不跑 OpenAPI lint**（spectral）
- **不跑安全扫描**（OWASP dep-check / npm audit）
- **不跑 SBOM 生成**（CycloneDX）
- **artifact 不签名**（GPG）
- **Release 流程不自动化**（手动打 tag + 手动 build-release.sh）

**行动项**：
- [P0] PR-V3-CI-FRONTEND：加 frontend-build-test job（Node 20 + npm ci + lint + test + build）
- [P0] PR-V3-CI-MATRIX：加 db-smoke-matrix job（matrix: [oracle, dm, postgres, kingbase]）
- [P0] PR-V3-CI-SECURITY：加 security-scan job（OWASP dep-check + npm audit + Trivy）
- [P1] PR-V3-RELEASE-AUTO：tag 触发自动 build-release + 上传 GitHub Release + sign artifact
- [P2] PR-V3-SBOM：CycloneDX 生成软件物料清单（信通院评测要）

---

## 17. 文档体系 — 🟢

**当前状态**（这次 v0.3 起重建）：
- 顶层 8 份核心：AI_CHARTER / PRODUCT_ARCHITECTURE_FINAL / AI_TEAM_SOP / v0.3-DEMO-REDESIGN / COMPREHENSIVE_REVIEW / DEPLOYMENT_DUAL_MODE / 01-05 金本位
- engineering/ 35+ 份工程规范
- adr/ 4 ADR + 6 待新增（0005-0010）
- reference-implementations/ 4 样板
- 5 套 forbidden-patterns / verify-pr / AI 一致性保证

**缺口**：
- 用户手册 / 客户操作手册（不是 AI 看的，是医院信息科 / 医生看的）
- API 文档（OpenAPI 自动生成后再说）
- 运维手册（监控/告警/应急）
- 培训材料（医院 IT 一上午能学会 + 医生 30 分钟能用上）
- 法务文档（合同模板 / SLA / 服务条款 / 隐私政策）

**行动项**：
- [P1] PR-V3-USER-MANUAL：4 治理模块各一本用户手册（中文，带截图，200-400 页）
- [P1] PR-V3-OPS-MANUAL：运维手册（监控/备份/升级/应急/扩容）
- [P1] PR-V3-TRAINING：培训 PPT × 3（医院 IT / 医生 / 实施工程师）
- [P1] PR-V3-LEGAL：合同 / SLA / 服务条款 / 隐私政策 / DPA（数据处理协议）模板（法务起草）

---

## 18. i18n / 本地化 — 🔴

**当前状态**：
- 所有中文硬编码在 JSX（grep `frontend/src` 中文字符串约 800+ 处）
- 后端 `messages.properties` 没有
- 时区写死 JVM 默认（依赖宿主机时区）
- 货币、单位都没有抽象

**缺口**：
- 港澳台市场需要繁体中文
- 一带一路医院（东南亚 / 中亚）可能需要英文 / 阿拉伯文 / 俄文
- 后端业务异常 message 用英文还是中文（当前混杂）

**行动项**：
- [P1] PR-V3-I18N（前端）：react-i18next + 提取 `zh-CN.json` + 占位 `zh-TW.json` `en-US.json`
- [P1] PR-V3-I18N-BACKEND：Spring `MessageSource` + `messages_zh_CN.properties` `messages_en_US.properties`
- [P2] PR-V3-RTL：右到左布局支持（阿拉伯文 / 希伯来文）— v1.1 GA

---

## 19. 商业化能力 — 🔴

**当前状态**：
- 没有订阅 / 用量 / 计费 / 对账 / 发票模块
- 没有客户管理 / CRM
- 没有合同/SOW 管理
- 02_任务台账 中有 `COMM-001` `LIC-001` 占位但未开始

**缺口**（内网部署的商业化路径相对简单 = 一次性买断 + 年服务费；外网 SaaS 复杂得多）：

| 模块 | 内网部署需要 | 外网 SaaS 需要 |
|---|---|---|
| 计费 | 否（合同管） | 必备（按 tenant + 用量） |
| 订阅 | 否 | 必备（套餐 / 升级 / 降级） |
| 用量统计 | 弱（年度报告）| 强（实时 dashboard） |
| 对账 | 季度 | 月度 |
| 发票 | 公司手动 | 自动开票（接对接票务平台） |
| License | 强（防盗用）| 弱（凭账号） |

**行动项**（按内网先做）：
- [P1] PR-V3-LICENSE：内网部署 license 文件验证（绑定机器特征 + 过期日期 + 模块清单）
- [P1] PR-V3-USAGE-REPORT：年度用量报告（API 调用次数 / 规则触发次数 / 评估次数）
- [P2]（如做 SaaS）PR-V3-BILLING：订阅 / 计费 / 对账 / 发票模块

---

## 20. 团队流程 / 法务 / IP — 🟡

**当前状态**：
- AI 团队 SOP 已建（AI_TEAM_SOP.md 500 行）
- 22 不变量 + 5 架构不变量已定
- verify-pr 13 项门禁
- 没有人工 code review 流程定义
- 没有 release manager 角色定义
- 没有 incident review 流程
- 没有 KPI / OKR 框架

**法务 / IP 缺口**：
- 没有软著（计算机软件著作权登记）
- 没有专利申请
- 没有商标注册（MedKernel 名称）
- 没有开源依赖审计（项目用了多少 GPL / LGPL / Apache / MIT，是否合规）
- 没有出口管制审查（核心算法是否涉密）

**行动项**：
- [P1] PR-V3-CODE-REVIEW：定义人工 code review 流程（架构师每周 1 次 sample review）
- [P1] PR-V3-RELEASE-MANAGER：定义 release manager 角色 + 月度发布节奏 + 紧急 hotfix SOP
- [P1] PR-V3-INCIDENT：定义 incident review 流程 + P0/P1 复盘模板
- [P1] PR-V3-SOFTWARE-COPYRIGHT：申请软著（CSCS，30 天，¥1000）
- [P1] PR-V3-TRADEMARK：注册「集团医疗智能中枢 MedKernel」+「MedKernel」商标（10 个月 + ¥1000）
- [P1] PR-V3-OSS-AUDIT：用 `mvn dependency-check` + `npm-license-checker` 跑开源依赖审计
- [P2] PR-V3-PATENT：评估核心算法（路径推荐 / CDSS 提醒疲劳治理 / 来源追溯）申请专利可能性

---

## 21. 总行动项汇总（按 P0/P1/P2）

### 🔴 P0 — v0.3 必须做（21 项）

1. PR-V3-00：视觉/命名基线
2. PR-V3-LOGIN：登录页 12 条国情
3. PR-V3-COMPLIANCE-BACKEND：application.yml 合规配置 + SMS 通道
4. PR-V3-PLACEHOLDER-CULL：砍 / 实装占位入口
5. ADR-0005/0006 + 双副本归并
6. PR-V3-HIKARI：HikariCP 接入（KD-004）
7. PR-V3-SNAKE-CASE：Jackson 全局（KD-005）
8. PR-V3-WF：WorkflowTodoService 持久化（KD-001）
9. PR-V3-KINGBASE-SMOKE：KingbaseES 实跑验证
10. PR-V3-FLYWAY：DB migration 自动化
11. PR-V3-OPENAPI：OpenAPI 3.0 自动生成
12. PR-V3-API-VERSION：API 版本规则
13. PR-V3-ACTUATOR：监控 endpoint
14. PR-V3-RESILIENCE：熔断限流
15. PR-V3-SECURITY-BASELINE：等保 12 项控制点
16. PR-V3-GMSSL：国密 SM2/SM3/SM4
17. PR-V3-DATA-CLASS：数据分级 + HEALTH_DATA SM4 加密
18. PR-V3-DIFY-DOMESTIC：Dify 限国内大模型
19. PR-V3-CONSENT：用户协议 / 隐私政策
20. PR-V3-CHINA-LOCALE：节假日 / 农历 / 民族 / 行政区划 / 身份证
21. PR-V3-MEDICAL-CARD：医保电子凭证 + 电子证照
22. PR-V3-PROM + LOGBACK + SLI-SLO：监控告警基础
23. PR-V3-CI-FRONTEND + CI-MATRIX + CI-SECURITY：CI 升级
24. PR-V3-E2E + COVERAGE：测试基础设施
25. 等保 2.0 三级测评立项 + 渗透测试

### 🟡 P1 — v0.3 完成后做（v1.0 GA 前）

- PR-V3-CDSS-REFACTOR / DTO-P0 / SPLIT-FILE / ORGCTX
- PR-V3-CODESPLIT / I18N / SENTRY
- PR-V3-CACHE / MQ
- PR-V3-DEPLOY-AUTO / OFFLINE-REPO / IMPLEMENT-DOCS / SLA
- PR-V3-APM / ALERT / ONCALL
- PR-V3-RELEASE-AUTO
- PR-V3-USER-MANUAL / OPS-MANUAL / TRAINING / LEGAL
- PR-V3-LICENSE / USAGE-REPORT
- PR-V3-CODE-REVIEW / RELEASE-MANAGER / INCIDENT
- PR-V3-SOFTWARE-COPYRIGHT / TRADEMARK / OSS-AUDIT
- PR-V3-PII-MIN / GDPR / TDE / BACKUP-DRILL
- PR-V3-PENTEST / MFA / RBAC-MATRIX

### 🟢 P2 — v1.0 GA 后做（v1.1 / v2.0）

- PR-V3-A11Y / RTL / CHAOS / SBOM
- PR-V3-PATENT
- 外网 SaaS：K8s / WAF / CDN / Billing / Multi-tenancy（见 §14）

---

## 22. v1.0 GA 准入七维度更新版

更新 [`PRODUCT_ARCHITECTURE_FINAL.md §6`](PRODUCT_ARCHITECTURE_FINAL.md) 的七维度：

| 维度 | 准入硬指标 |
|---|---|
| **D1 · 编译/测试** | 全套 GREEN + 覆盖率（后端 70% / 前端 60%）+ E2E 6 剧本 PASS |
| **D2 · 功能完整** | 27 菜单 100% 可达 + 6 剧本一键 fixture + 4 治理模块用户手册 |
| **D3 · 性能** | HikariCP + Resilience4j + 100 并发 30 min 无 leak + P95<300ms |
| **D4 · 合规** | **等保 2.0 三级评测过** + **国密评测过** + ICP/公安备案 + 12 国情条 + Dify 限国内 + 用户协议 |
| **D5 · 跨数据库** | Oracle/DM/PG/Kingbase 4 套 smoke 全过 + Flyway migration |
| **D6 · 文档** | 用户手册 + 运维手册 + API OpenAPI + 培训 PPT + 法务合同 |
| **D7 · 运维** | Prometheus + Grafana + 告警 + APM + 备份/升级演练 + SLA 模板 |
| **D8 ★ 新增 · 商业化（如外销）** | 软著 + 商标 + 开源审计 + License + 用量报告 |

**距离 v1.0 GA 总缺口**：约 **120 个工作日**（5 AI 并行 + 法务/合规协同 = **约 24 周**）。

---

**End of comprehensive review.**
**最重要 3 件事**：
1. **法律合规优先**（§9/§10/§11/§12）— 没有等保 + 国密 + 个保法落地，技术再好也卖不出去
2. **HikariCP + 拆超长文件**（§7/§2）— 性能/可维护性的核心债，越拖越贵
3. **OpenAPI + 监控**（§5/§15）— 商业化前提，没有这两个永远是「demo 状态」
