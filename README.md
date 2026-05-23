# 集团医疗智能中枢 · MedKernel

> v1.0 GA 全量重启基线 · 2026-05-23
> 把指南、路径、规则和院内数据接起来，在临床现场提醒医生，在质控侧形成整改闭环，并留下合规证据。

---

## 一句话定位

MedKernel = **集团医疗智能中枢**，面向集团医院 / 多院区 / 三甲三乙，提供 4 大主线 + 1 高级工具：

| 主线 | 客户能理解的话 |
|---|---|
| 试点准备 | 把医院、系统、路径、规则准备好 |
| 临床运行 | 患者进路径、医生收到提醒、任务有人处理 |
| 质控改进 | 看问题、派整改、看效果 |
| 合规运维 | 身份、审计、安全、监控、交付证据 |

> 详见 [产品宪法](docs/CONSTITUTION.md)。

---

## 技术栈（v1.0 GA）

| 层 | 选型 |
|---|---|
| 后端 | JDK 21 LTS + Spring Boot 3.3 + Jakarta EE + Spring Security 6 + Spring Data JDBC + Hikari 5 + Flyway 10 |
| 加密 | BouncyCastle 1.78.1（SM2 / SM3 / SM4 + FIPS 路径预留） |
| 数据库 | PostgreSQL / Oracle 23ai / 达梦 8 / 人大金仓 V9 / H2（5 方言全支持） |
| 知识图谱 | Neo4j 5.23 |
| 监控 | OpenTelemetry 1.41 + Prometheus + Tempo + Loki + Grafana |
| 前端 | Node 20 LTS + React 18 + Antd 5 + Vite 5 + TypeScript 5.6 + React Query 5 + Zustand 5 |
| 部署 | 内外网双形态：内网（国产化栈）+ 外网（SaaS） |

---

## 仓库结构

```
medkernel/
├─ medkernel-backend/    ← Spring Boot 3 + JDK 21 + Jakarta EE
├─ frontend/             ← React 18 + Antd 5 + FSD（app/pages/widgets/features/entities/shared）
├─ docs/                 ← 文档中心（CONSTITUTION + 12 周方案 + 任务台账 + handbook）
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
| [docs/CONSTITUTION.md](docs/CONSTITUTION.md) | 产品宪法：12 条硬约束 / 5 菜单 / 4 状态机 / 7 步流 / 6 阶段租户生命周期 |
| [docs/V1_GA_REWRITE_PLAN.md](docs/V1_GA_REWRITE_PLAN.md) | 12 周方案：Phase-0~6 / W1 Day-by-Day / 风险 |
| [docs/backlog.md](docs/backlog.md) | 单一任务台账：74 项 GA-* + 3 字段（id/owner/status） |
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

**MedKernel · v1.0 GA · 2026-08-15 目标发版日**
