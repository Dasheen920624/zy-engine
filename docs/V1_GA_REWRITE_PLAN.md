# MedKernel v1.0 GA 全量重启方案

> 版本：1.0 · 2026-05-23
> 范围：v1.0 GA / tag `v1.0.0`，目标发版日 ~2026-08-15
> 前提：项目未上线 / 未部署 / 零生产数据 / 零客户在跑 → 可完全推倒重写
> 原则：**最干净、最纯粹、不留历史包袱**

---

## 0. 关键决策（用户已拍板）

| 决策 | 选择 |
|---|---|
| 项目状态 | 全新未上线 → 可全量重构 |
| 旧 develop 代码 | 完全推倒，从 0 重写（不复用 84k 行业务代码） |
| 分支模型 | trunk-based：`main` 单线 + `feat/*` 短分支，废除 `develop` |
| 命名 / 流程归一 | 批量废除旧 5 套命名（PR-V2-* / PR-V3-* / PR-FINAL-* / GA-* / DOC-V2-*）+ claim/lock + 金本位 5 份，归一为 `GA-<DOMAIN>-<NN>` |
| JDK 升级 | v1.0 GA 内一次到位（JDK 21 + Spring Boot 3.3 + Jakarta EE） |
| Batch 9 客户未提项 | 全做 26 项 + 增量提 6 项 v1.1 候选到 GA |
| 工期 | 12 周（含增量业务）|
| 产品 / UX 收口 | 等 JDK 升级完再启动（W6 之后） |

---

## 1. 12 周时间线

| 周 | 时间 | 主战 | 关键交付 | 闸门 |
|---|---|---|---|---|
| W1 | 5/24-5/30 | Phase-0 归档 + Phase-1 现代化骨架（8 项可行性闸门）| 仓库重置 + JDK 21/Boot 3/5 方言/Security 6/VT/国密/OTel/国产化预演 | 8 项 ≥ 6 通过才进 W2 |
| W2 | 5/31-6/6 | Phase-1 收尾 + Phase-2 产品骨架 | 5 菜单组 + 4 状态机 + 7 步流模板 + 工作台 + 命令面板 + 演示模式 | 产品形态定稿 |
| W3 | 6/7-6/13 | Phase-3 业务域骨架（5 域 5 AI 并行） + 增量 1：业务域细颗粒 | 30 菜单 100% 可达 + 真实数据 + 6 阶段租户生命周期 | 27 菜单全可达 |
| W4 | 6/14-6/20 | Phase-3 业务深化 + 6 剧本 fixture | E2E 6 剧本绿 + 业务域可演示 | E2E 全绿 |
| W5 | 6/21-6/27 | Phase-4 客户必查 1/2（8 项核心：医保/CA/脱敏/可解释性/版本/国产化/UDI/直报）+ 外部 3 评测送审 | Batch 9 前 8 项完成 + 评测受理 | 三甲必查落地 |
| W6 | 6/28-7/4 | Phase-4 客户必查 2/2（剩 18 项）+ GA-PERF-01 + GA-OPS-02 备份/升级演练 | Batch 9 全 26 项 + 演练 ≥ 1 次 | 完整版功能锁 |
| W7 | 7/5-7/11 | 增量 2：性能基线打磨 + 慢 SQL 治理 + LLM 降级链调优 + 全量压测 | 1000 并发 + 5000 万 MPI + LLM 降级全过 | 性能基线通过 |
| W8 | 7/12-7/18 | GA-QA-01/02 覆盖率 70/60 + GA-OPS-03/04/05 国产化矩阵/SBOM/离线包 + 增量 3：5 项 v1.1 提前入 GA | 覆盖率达标 + 国产化矩阵全绿 | CI matrix 全绿 |
| W9 | 7/19-7/25 | 外部评测结果回收 + 整改 + GA-DOC-01 / GA-LEGAL-01 + 增量 4：集团版品牌定制 + 灾备文档 | 评测通过 + 文档全套 | 评测通过 |
| W10 | 7/26-8/1 | 试点医院切真实流量 W1 + 增量 5：客户成功看板 + License 多种计价 | 1 家医院 7×24 真实数据 | SLA 99.9% 达标 |
| W11 | 8/2-8/8 | 试点 W2 + 整改 + 增量 6：医师 PWA 移动只读 + 老年医生模式 + i18n 5 语种 | 试点收尾 | 试点验收 |
| W12 | 8/9-8/15 | 出厂评审 + v1.0.0-ga-evidence 填齐 + tag `v1.0.0` 发版 + 后置 7 天稳定性观察 | **GA 发版** | Release Manager 拍板 |

**GA tag 日期：2026-08-15 左右**

---

## 2. 5 阶段产物地图（Phase-0~5）

### Phase-0 · 基线重置（W1 Day-1~3）

| ID | 名 | 工时 |
|---|---|---|
| GA-RESET-01 | 仓库结构重组（保留 git 历史，新目录骨架） | 0.5 d |
| GA-RESET-02 | trunk-based 切换：废除 develop，main 单线 + feat/* 短分支 | 0.5 d |
| GA-RESET-03 | 任务台账归一为 `docs/backlog.md`（3 字段：id/owner/status） | 0.2 d |
| GA-RESET-04 | 旧 5 套命名 + claim/lock + 金本位 → `docs/archive/v0.3/` | 0.5 d |
| GA-RESET-05 | 新 `docs/CONSTITUTION.md`（产品宪法）+ 必要 ADR 起步 | 0.5 d |

### Phase-1 · 现代化骨架（W1 Day-3~5）

| ID | 名 | 工时 |
|---|---|---|
| GA-CORE-01 | JDK 21 + Spring Boot 3.3 + Jakarta EE 9 + Maven 3.9 baseline | 1 d |
| GA-CORE-02 | Spring Security 6 OAuth2/OIDC + 国密 BC-FJA 1.78.1 | 1 d |
| GA-CORE-03 | Spring Data JDBC + Hikari 5 + 5 方言 SPI + Flyway 10 | 1 d |
| GA-CORE-04 | OpenTelemetry 1.41 + Prometheus + Tempo + Loki + Grafana | 1 d |
| GA-CORE-05 | Spring Cloud Gateway 4 + RateLimiter + WAF 链 | 0.5 d |
| GA-CORE-06 | Global ProblemDetail + Sealed Exception + i18n + KMS Vault | 0.5 d |
| GA-CORE-07 | Virtual Threads 默认开启 + Tomcat 10 virtual executor | 0.5 d |
| GA-CORE-08 | 前端 Node 20 + FSD 重组 + 路由元数据 + 4 状态机 Badge + 6 态模板 | 1.5 d |

**W1 闸门**：8 项可行性验证 ≥ 6 通过才进 W2。任一不通过当晚加班排查 / 调整选型。

### Phase-2 · 产品收口（W2）

| ID | 名 |
|---|---|
| GA-PROD-01 | 5 组菜单 + 隐藏式高级工具 + 工作台 1 屏即懂 |
| GA-PROD-02 | 4 套统一状态机全平台实装 |
| GA-PROD-03 | 7 步极简配置流模板（7 处复用） |
| GA-PROD-04 | 默认登录 1 路径 + MFA/SSO/国密折叠区 |
| GA-PROD-05 | 全局命令面板（⌘K） |
| GA-PROD-06 | 全局"导出审计快照"按钮 |
| GA-PROD-07 | 权限指纹 chip |
| GA-PROD-08 | 老年医生模式 + 暗黑模式 + 护眼模式 |
| GA-PROD-09 | 列管理 + 视图保存 + 视图分享 |
| GA-PROD-10 | 客户演示模式 |
| **GA-PROD-11** | **租户生命周期管理**（6 阶段 + 工作台面板 + 3 维切片 + 自动推进规则） |

### Phase-3 · 业务域实装（W3-W4，5 域 5 AI 并行）

| ID | 名 |
|---|---|
| GA-TENANT-01 | 试点准备域（7 菜单）按 7 步流标准实装 |
| GA-CLINICAL-01 | 临床运行域（6 菜单）按统一状态机实装 |
| GA-QUALITY-01 | 质控改进域（6 菜单），AI 知识审核 / 知识审核台合并 |
| GA-COMPLIANCE-01 | 合规运维域（6 菜单） |
| GA-ADVANCED-01 | 高级工具域（5 菜单，含国产化自检 + 开发者控制台） |

### Phase-4 · 客户必查（W5-W6 + 增量 W8）

26 项 Batch 9 EXT + 5 项 v1.1 提前。详见 `docs/backlog.md` 的 GA-EXT-01~31 项。

### Phase-5 · 质量、运维、文档、法务（W7-W9）

| ID | 名 |
|---|---|
| GA-QA-01 | 后端覆盖率 ≥ 70%（Jacoco + JUnit 5 + Testcontainers） |
| GA-QA-02 | 前端覆盖率 ≥ 60%（Vitest 2 + RTL 16 + msw 2） |
| GA-QA-03 | E2E 6 剧本 Playwright 稳定绿 |
| GA-OPS-01 | OpenTelemetry + Prometheus + Tempo + Loki + Grafana + 告警 |
| GA-OPS-02 | 备份恢复演练 ≥ 3 次录像 + 升级回滚演练 |
| GA-OPS-03 | 4 OS × JDK 21 国产化矩阵 CI |
| GA-OPS-04 | SBOM (CycloneDX) + 镜像 cosign 签名 + Reproducible Build |
| GA-OPS-05 | 离线安装包 + 一键安装/升级/回滚脚本 |
| GA-PERF-01 | 1000 并发 60 min + 5000 万 MPI 压测 + LLM 降级链压测 |
| GA-SEC-01 | 等保 2.0 三级第三方测评（外部 4 周并行） |
| GA-SEC-02 | 商密 GM/T 0054 + GB/T 39786 评测（外部 4 周并行） |
| GA-SEC-03 | 渗透测试（外部 2 周） |
| GA-DOC-01 | 产品宪法 + 实施手册 + 用户手册 + 运维手册 + 培训材料 |
| GA-LEGAL-01 | 合同 + SLA + 隐私政策 + DPA + License + 用量报告 |

---

## 3. W1 立即可执行 Day-by-Day 清单

### Day-1（周一）：归档 + 切换分支模型

| 阶段 | 动作 | 风险 |
|---|---|---|
| **A · 安全网**（先备份）| 给 main / develop / 22 个 ai/GA-* 分支统一打 `legacy/*-20260524` tag，push 远程 | 无 |
| **B · trunk-based 切换** | GitHub 把 develop 设为非默认 / 取消保护 / 远程删除（已 tag 留档） | 中（已备份） |
| | 远程删除 22 个 ai/GA-* 分支（已 tag 留档） | 中（已备份） |
| **C · 归档旧产物** | move `docs/01-05_*.md` / `docs/engineering/` 到 `docs/archive/v0.3/` | 低 |
| | move `ai-dev-input/` 整体到 `docs/archive/v0.3/ai-dev-input/` | 低 |

### Day-2~3（周二、周三）：清空 + 新骨架

| 阶段 | 动作 |
|---|---|
| **D · 后端清空 + 骨架** | 删 `medkernel-mvp/`，新建 `medkernel-backend/` 按域骨架（shared/tenant/clinical/quality/compliance/advanced/platform） |
| | 写新 pom.xml（JDK 21 + Boot 3.3.5 + Jakarta + Security 6 + Flyway 10 + BC 18on + Hikari 5 + ojdbc11 + Neo4j 5 + springdoc 2 + Micrometer 1.13 + OTel 1.41） |
| | 写新 application.yml + 5 profile（dev/test/demo/prod/govcloud） |
| | 写 MedKernelApplication.java + `/actuator/health` 跑通 |
| **E · 前端清空 + 骨架** | 保留 frontend/ 目录，清空 src/，按 FSD 重建（app/pages/widgets/features/entities/shared） |
| | 升级 package.json：Node 20 + Zustand 5 + OTel web SDK |
| | 写 src/app/ 入口 + src/shared/config/ 路由 + 菜单元数据（5 组） |
| | 跑通 vite dev 出现登录页空壳 |
| **F · 单一任务台账** | 新 docs/backlog.md：56 项 GA-* + 3 字段（id/owner/status） |

### Day-4~5（周四、周五）：技术栈 8 项可行性验证

| 闸门 | 内容 |
|---|---|
| G1 | JDK 21 + Boot 3.3 hello world |
| G2 | 5 方言 Flyway smoke（Postgres + Oracle 23ai + 达梦 8 + 人大金仓 V9 + H2） |
| G3 | Security 6 OAuth2 Resource Server + JWT |
| G4 | Virtual Threads + Tomcat 10 + Hikari 5 |
| G5 | 国密 BC-FJA SM2/SM3/SM4 |
| G6 | OTel + Prometheus + Grafana 一站式 trace + metric + log |
| G7 | 国产化矩阵预演：麒麟 V10 SP3 + KAE-JDK21 + 达梦 8 + KAE Provider |
| G8 | 前端 FSD + 路由元数据 + 5 菜单组 + 工作台 1 屏 |

**8 项 ≥ 6 通过 → 进 W2**。

---

## 4. 命名 / 流程归一表

| 旧 | 新 |
|---|---|
| `PR-V2-*` / `PR-V3-*` / `PR-FINAL-*` / `GA-*` / `DOC-V2-*` 5 套 | `GA-<DOMAIN>-<NN>` 单一 |
| `docs/01-05_*.md` 5 金本位 | `docs/CONSTITUTION.md` 1 张产品宪法 |
| `docs/engineering/*.md` 35+ 文档 | `docs/handbook/*.md` 实施手册（按需，≤ 10 份） |
| `docs/engineering/adr/0001-*.md` ADR | `docs/adr/` 保留 ADR |
| `ai-dev-input/10_task_claims/active|active_locks|archive` | 单一 `docs/backlog.md` |
| `docs/engineering/02_任务台账.md` | `docs/backlog.md` |
| DOC-V2-* 登记 | 废除（改文档与改代码同 PR） |
| `claude/* + ai/* + feat/* + release/*` 4 套分支 | `feat/*`（≤ 3 天）+ `main` 2 套 |

---

## 5. 技术栈选型表（一次到位）

| 层 | 选型 |
|---|---|
| JDK | OpenJDK 21 LTS（Temurin / KAE-JDK21 / LoongJDK21）|
| Spring Boot | 3.3.5 |
| Spring Framework | 6.1 |
| Spring Security | 6.3 |
| Spring Data JDBC | 3.3 |
| Flyway | 10.20 |
| BouncyCastle | bcprov-jdk18on 1.78.1 + BC-FJA 1.0.2.5 |
| springdoc-openapi | 2.6 |
| JJWT | 0.12.6 |
| Neo4j Driver | 5.23 |
| Oracle JDBC | ojdbc11 23.5.0.24.07 |
| Servlet 容器 | Tomcat 10.1（默认） / Undertow 2.3（国产化备选） |
| 连接池 | HikariCP 5.1.0 |
| 构建 | Maven 3.9.8 + JReleaser + CycloneDX |
| 镜像 | eclipse-temurin:21.0.4-jre-jammy + distroless + openeuler:22.03-LTS-SP4 |
| 前端 Node | Node 20 LTS |
| 前端 React / Antd / Vite | React 18.3 / Antd 5.21 / Vite 5.4 |
| 前端状态 | React Query 5 + Zustand 5 |
| 前端 e2e | Playwright 1.45 |
| 监控 | OpenTelemetry 1.41 + Prometheus + Tempo + Loki + Grafana |

---

## 6. 5 方言数据库支持

| 方言 | 用途 | Flyway 迁移 |
|---|---|---|
| PostgreSQL 15 | 外网 SaaS 主选 | `db/migration/postgres/V*.sql` |
| Oracle 23ai | 大型三甲首选 | `db/migration/oracle/V*.sql` |
| 达梦 8 | 国产化首选 | `db/migration/dm/V*.sql` |
| 人大金仓 V9 | 国产化备选 | `db/migration/kingbase/V*.sql` |
| H2 2.2 | 本地开发 / CI | `db/migration/h2/V*.sql` |

**所有 schema 从 V1__init.sql 重新开始，不复用旧 V1/V2**。

---

## 7. 风险清单（白纸版才会出现的）

| # | 风险 | 对策 |
|---|---|---|
| 1 | 业务知识丢失：旧 develop 上 84k 行真实业务逻辑不再复用 | 保留 `legacy/v0.3-develop-20260524` tag 长期可查；业务设计前先 git log 看历史决策 |
| 2 | 重启 4 周净亏损时间 | W1 同时跑骨架搭建 + 业务设计；5 AI 并行 5 域 |
| 3 | JDK 21 + Boot 3 + Jakarta 全新栈调试成本 | W1 必跑 8 项闸门 + 6 项通过才进 W2 |
| 4 | 5 方言 Flyway 重写跑不齐 | Testcontainers 矩阵 CI 默认开启，每 commit 5 方言全绿 |
| 5 | 国密 BC-FJA + KAE + 麒麟 JDK 兼容性 | W1 末做 1 天国产化矩阵预演 |
| 6 | 外部 3 评测（等保/密评/渗透）周期不可控 | W5 早送审，W9 收回；如有延误，整改保留 ≥ 2 周 buffer |
| 7 | 试点医院切真实流量翻车 | W10-W11 共 2 周试点期；每天对账，发现问题立即回滚配置 |

---

## 8. 产物与流程不变项

| 项 | 为什么不变 |
|---|---|
| 国情合规硬约束（等保 + 国密 + 个保法 + 数据出境 + 备案） | 商业前提 |
| 5 组菜单 + 4 套统一状态机 + 7 步流 + 7 角色硬指标 | 产品定义 |
| Batch 9 全 26 项 EXT | 用户已拍板"全做" |
| 国产化矩阵 4 OS × JDK 21 | 销售前提 |
| 外部 3 评测（等保 + 密评 + 渗透） | 法律前提 |
| 试点医院 2 周真实流量 | SLA 前提 |
| 内外网双形态 | 客户场景 |

---

## 9. 出厂证据清单（v1.0.0-ga-evidence.md）

W12 出版前必须齐备：

| 证据 | 内容 |
|---|---|
| 代码质量 | 全部 commit / CI 链接 / 覆盖率报告 / E2E 报告 |
| 安全合规 | 等保 2.0 三级测评报告 + 商密评测报告 + 渗透测试报告 + 未关闭风险接受表 |
| 客户体验 | 6 大剧本演示录像 / 截图 / 客户可见页面清单 |
| 数据库 | 5 方言 smoke 矩阵结果 + Flyway 升级回滚证据 |
| 运维 | 监控看板 / 告警规则 / 备份恢复演练 ≥ 3 次录像 / 升级回滚演练录像 |
| 文档 | 用户手册 4 治理模块 + 运维手册 + 培训材料 3 角色 + 合同/SLA 模板 |
| 发布 | tag v1.0.0 + artifact SHA-256/cosign 签名 + SBOM (CycloneDX) + 回滚指南 |
| 试点 | 试点医院 2 周真实流量 SLA 报告 + 客户验收单（双签） |
| 国产化 | 4 OS × JDK 21 矩阵证明 + KAE Provider 兼容报告 |

---

**End of v1.0 GA Rewrite Plan.**
