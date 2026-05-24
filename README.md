# 集团医疗智能中枢 · MedKernel

> v1.0 GA 0 业务引擎全能力上线基线 · 2026-05-24
> 把指南、路径、规则和院内数据接起来，在临床现场提醒医生，在质控侧形成整改闭环，并留下合规证据。

---

## 一句话定位

MedKernel = **集团医疗智能中枢**，面向集团医院 / 多院区 / 三甲三乙 / 分院 / 社区卫生服务中心 / 街道卫生所，提供 4 大主线 + 1 高级工具：

为了保持方案简单纯粹，v1.0 GA 按 **基础底座 + 服务能力** 两层理解：

| 层 | 说明 |
|---|---|
| 基础底座 | 组织、权限、数据、知识、规则、发布、审计、模型、部署、嵌入等共用能力 |
| 服务能力 | 试点准备、临床运行、质控改进、合规运维、专病路径服务包、专业协同服务包和高级工具 |

当前执行方式是 **0 业务引擎全能力上线**：先打通知识、字典、规则、路径、推荐、评估、随访、包发布、嵌入、模型网关、审计证据和降级链路；引擎全能力验收后，再按试点准备、临床运行、质控改进、合规运维、专病路径和专业协同服务包包装业务。

| 主线 | 客户能理解的话 |
|---|---|
| 试点准备 | 把医院、系统、路径、规则准备好 |
| 临床运行 | 患者进路径、医生收到提醒、任务有人处理 |
| 质控改进 | 看问题、派整改、看效果 |
| 合规运维 | 身份、审计、安全、监控、交付证据 |
| 专病路径 | 将疾病来源、路径、规则、质控、随访和证据打包上线 |
| 专业协同 | 护理、药事、医技、手麻输血、营养康复心理疼痛、院感公卫等复用同一引擎 |

> 详见 [产品宪法](docs/CONSTITUTION.md)。

---

## 技术栈（v1.0 GA）

| 层 | 选型 |
|---|---|
| 后端 | JDK 21 LTS + Spring Boot 3.3 + Jakarta EE + Spring Security 6 + Spring Data JDBC + Hikari 5 + Flyway 10 |
| 加密 | BouncyCastle 1.78.1（SM2 / SM3 / SM4 + FIPS 路径预留） |
| 数据库 | PostgreSQL / Oracle 23ai / 达梦 8 / 人大金仓 V9 / H2（5 方言全支持） |
| 知识图谱 | 业务权威源在关系库；Neo4j 5.23 仅作可重建查询投影 |
| 监控 | OpenTelemetry 1.41 + Prometheus + Tempo + Loki + Grafana |
| 前端 | Node 20 LTS + React 18 + Antd 5 + Vite 5 + TypeScript 5.6 + React Query 5 + Zustand 5 |
| 部署 | 内外网双形态：内网（国产化栈）+ 外网（SaaS） |

---

## 仓库结构

```
medkernel/
├─ medkernel-backend/    ← Spring Boot 3 + JDK 21 + Jakarta EE
├─ frontend/             ← React 18 + Antd 5 + FSD（app/pages/widgets/features/entities/shared）
├─ docs/                 ← 文档中心（引擎总览 + 宪法 + 产品体验规范 + 引擎上线方案 + 详细规范 + 任务台账）
└─ deploy/               ← 部署脚本 + 监控配置（Grafana / Prometheus / Nginx / systemd）
```

---

## 启动

### 后端

```bash
cd medkernel-backend
mvn spring-boot:run
```

→ `http://localhost:18080/medkernel/api/v1/system/ping`
→ `http://localhost:18080/medkernel/actuator/health`
→ `http://localhost:18080/medkernel/swagger-ui.html`

### 前端

```bash
cd frontend
npm install
npm run dev
```

→ `http://localhost:5173`

---

## 关键文档

| 文档 | 一句话 |
|---|---|
| [docs/MEDKERNEL_FOUNDATION_AND_SERVICES.md](docs/MEDKERNEL_FOUNDATION_AND_SERVICES.md) | 总览：基础底座 + 引擎服务能力 + 业务服务包装 |
| [docs/MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md](docs/MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md) | 产品体验固定规范：全系统交互、分页、低打扰、可信解释和验收门禁 |
| [docs/MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md](docs/MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md) | 实施落地方案：当前按 0 业务引擎全能力上线，验收后再拆业务服务 |
| [docs/MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md](docs/MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md) | 唯一实现级详细规范：S0-S40、独立专病路径、护理/报告/床旁知识/中医药、AI 工厂、引擎与评级 |
| [docs/CONSTITUTION.md](docs/CONSTITUTION.md) | 产品宪法：16 条硬约束 / 5 菜单 / 4 状态机 / 7 步流 / 6 阶段租户生命周期 |
| [docs/backlog.md](docs/backlog.md) | 单一任务台账：当前只登记基础底座与引擎全能力上线任务 |
| [docs/README.md](docs/README.md) | 文档中心导航 |

---

## 国情合规底线

| 维度 | 标准 |
|---|---|
| 等级保护 | 等保 2.0 三级 |
| 商用密码 | GM/T 0054 + GB/T 39786 商密评测 |
| 个人信息 | 个保法 + GB/T 35273-2020 个人信息安全规范 |
| 数据出境 | 数据出境安全评估办法 |
| 医疗法规 | 电子病历应用管理规范 + 医师法 + 医疗卫生机构网络安全管理办法 |
| 备案 | ICP 备案 + 公安备案 + 算法备案 |

---

## v0.3 历史

v0.3-final 已发布并归档。需要查询历史：

```bash
git checkout legacy/v0.3-main-20260524     # v0.3-final main
git checkout legacy/v0.3-develop-20260524  # v0.3-final develop（含 567 commit）
```

旧文档全部归档在 [docs/archive/v0.3/](docs/archive/v0.3/)。

---

**MedKernel · v1.0 GA · 发版日待引擎全能力验收后重新冻结**
