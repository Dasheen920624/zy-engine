# MedKernel · 内外网双部署形态规格

> 版本：1.0 · 2026-05-21  
> 适用：本文是 MedKernel **内网部署（医院本地）** 和 **外网部署（SaaS）** 双形态的差异化要求权威。  
> 上游：[`COMPREHENSIVE_REVIEW.md §13/§14`](COMPREHENSIVE_REVIEW.md) + [`PRODUCT_ARCHITECTURE_FINAL.md`](PRODUCT_ARCHITECTURE_FINAL.md)

---

## 0. 双形态战略定位

| 维度 | **内网部署（On-Premise）** | **外网部署（SaaS）** |
|---|---|---|
| **客户** | 公立三甲 / 集团医院 / 地方卫健委 / 国央企医院 | 中小医院 / 民营诊所 / 互联网医院 / 海外华人医院 |
| **数据归属** | 客户机房，**永不出院** | 平台多租户共享，按租户隔离 |
| **付费模式** | 一次性买断 + 年度服务费（30-80%） | 月/年订阅 + 用量计费 |
| **典型客单价** | ¥80 万 - ¥500 万 / 单院（按规模） | ¥0.5 万 - ¥5 万 / 月 / 租户 |
| **法律主体** | 服务合同 + SOW + 验收报告 | 服务条款 + DPA + 隐私政策 |
| **合规级别** | 等保 2.0 三级 + 信通院国密 + 卫健委备案 | 等保 2.0 三级 + ICP/公安备案 + 数据出境评估（如有跨境） |
| **网络环境** | 医院内网（无公网）/ 专网 / 政务外网 | 公网 + 内部 VPC |
| **运维** | 客户 IT 运维 + 我方按 SLA 支持 | 平台全责（24×7）|
| **可用性 SLA** | 99.5%（医院内网通常允许窗口维护） | 99.95% / 99.99%（套餐） |
| **数据本地化** | 100% 本地 | 严格中国境内（阿里云/腾讯云/华为云 中国节点）|
| **GA 时点** | v1.0 GA | v1.1 GA（推迟，等内网商业化跑通） |

**战略**：**内网先行 → 外网 follow**。内网吃下国央企医院市场，外网吃下中小医院长尾。

---

## 1. 通用基础（双形态共享）

不论内外网，以下基础能力**必须一致**：

### 1.1 应用层

| 项 | 要求 |
|---|---|
| JDK | OpenJDK 1.8（Temurin / Bellsoft / Zulu，含国密 ARM 适配） |
| Spring Boot | 2.7.18（LTS，与 JDK 1.8 兼容到 2026） |
| Maven | 3.6.3+ |
| 前端运行时 | 静态资源（nginx 或 CDN 托管），无 Node 服务 |
| Java GC | G1（默认，`-Xms2g -Xmx4g`） |

### 1.2 数据库

| 数据库 | 内网必备 | 外网必备 |
|---|:---:|:---:|
| Oracle 19c | 三甲 / 集团医院主流 | 阿里云 RDS Oracle 兼容（PolarDB Oracle 兼容） |
| 达梦 DM8 | 国央企信创首选 | 否 |
| KingbaseES V8 | 政务 / 央企信创备选 | 否 |
| PostgreSQL 15 | 部分医院 | **SaaS 主选**（成本低 + 云厂商成熟） |
| LOCAL_H2 | 仅开发本地 | 仅开发本地 |
| MySQL | **禁止**（不在支持矩阵）| 同左 |

### 1.3 配置与机密

| 类型 | 内网 | 外网 |
|---|---|---|
| 配置 | `medkernel.env` 文件（chmod 600） | K8s ConfigMap |
| 机密 | env 变量（DBA 录入） | K8s Secret + KMS 加密 + IAM 拉取 |
| JWT 密钥 | 启动注入 `MEDKERNEL_JWT_SECRET` | KMS 加密 + 启动从 KMS 拉取 |
| DB 密码 | 启动注入 + 100% 卫健委 IT 自管 | KMS 加密 + 滚动轮换（90 天） |

### 1.4 日志与追踪

| 项 | 内网 | 外网 |
|---|---|---|
| 日志格式 | JSON（含 traceId / userId / tenantId） | 同左 |
| 日志输出 | 本地 `logs/*.log`，轮转 + 7 天保留 | stdout（K8s 自动采集）+ 集中存储 |
| 链路追踪 | SkyWalking（内网友好，国产）| Aliyun ARMS 或 TKE-APM |
| 审计日志 | `ENGINE_AUDIT_LOG`（DB） + 不允许 DELETE/UPDATE | 同左 + 跨地域备份 |
| 日志聚合 | ELK（医院 IT 选装）/ Loki | Aliyun SLS（推荐）/ 自建 ELK |

---

## 2. 内网部署详细规格

### 2.1 操作系统与硬件矩阵

| OS | CPU | 支持等级 |
|---|---|:---:|
| CentOS 7.6+ / RHEL 7.6+ | x86_64 | 主流 |
| **UOS V20 1050** | x86_64 + aarch64（鲲鹏 920） | 国央企 |
| **银河麒麟 V10 SP3** | x86_64 + aarch64（鲲鹏 920 / 飞腾 2000）+ loongarch64（龙芯 3A5000） | 国央企 |
| **openEuler 22.03 LTS-SP3** | x86_64 + aarch64 | 推荐升级目标 |
| Windows Server 2019/2022 | x86_64 | 部分老医院 |

**硬件最低规格（单实例）**：
- CPU：4 核（建议 8 核）
- 内存：8GB（建议 16GB）
- 磁盘：100GB SSD（含日志 + 7 天备份）
- 网络：千兆内网

**多实例（集群）**：
- 3 节点最小（一主二备）
- 集群协调：DB row-lock（暂时不引入 ZooKeeper / Etcd，等 PR-V3-COORD 决策）
- 负载均衡：nginx upstream / F5

### 2.2 部署目录结构（约定）

```
/zoesoft/medkernel/                  # 主目录（$MK_HOME）
├── bin/                             # 启动脚本
├── conf/
│   ├── medkernel.env                # 主配置（chmod 600）
│   └── application-prod.yml         # Spring profile
├── lib/                             # JAR（Spring Boot 单 jar）
├── frontend/                        # 前端 dist（nginx 托管）
├── db/                              # DDL（按 dialect）
├── logs/                            # 日志（chmod 750）
├── data/                            # 上传文件 / 临时数据
└── scripts/                         # 部署/运维脚本

/zoesoft/medkernel.bak/              # 备份目录（$MK_BACKUP_DIR）
└── <version>-<timestamp>/           # 升级时自动备份上一版
```

### 2.3 网络拓扑

```
医院内网
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  HIS  ─────┐                                                 │
│            ├──→ nginx (80/443) ──→ MedKernel Backend (18080) │
│  EMR  ─────┤                              │                  │
│            │                              ├──→ Oracle/DM/PG  │
│  PACS ─────┤                              │                  │
│            │                              ├──→ Neo4j (可选)  │
│  浏览器 ───┘                              │                  │
│                                           └──→ Dify (可选)   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
                                                              │
                                                              ↓
                                                       医院信息科监控
                                                       Prometheus + Grafana
```

### 2.4 部署流程

详见 [`deploy/README.md`](../deploy/README.md)。核心 7 步：

1. CI 上 `build-release.sh` 产出 `medkernel-v1.x.y-<hash>.tar.gz`（含 sha256）
2. 移动介质（U 盘 / 内网网盘 / scp）到医院内网
3. 部署机 `sha256sum -c` 校验
4. 解压到 `/zoesoft/medkernel/`
5. 选 profile 文件 → 改 `conf/medkernel.env`（DB 凭据 / JWT 密钥 / 国密参数）
6. `check-env.sh` → `install-offline.sh --init-db`
7. `healthcheck.sh` 验证

### 2.5 升级与回滚

```bash
# 升级
sudo ./scripts/upgrade.sh --to v1.3.0
  # 内部：stop service → 备份当前到 backup/ → 解压新版 → migrate DDL → 启动 → 健康检查

# 回滚（失败时）
sudo ./scripts/rollback.sh --to v1.2.3
  # 内部：stop service → 还原备份 → 反向 migrate（如有 .down 脚本）→ 启动
```

### 2.6 监控与告警

**内网监控套件（推荐）**：
- **Prometheus + Grafana**（开源，国产化兼容）
- 后端暴露 `/medkernel/actuator/prometheus`（端口 18081，仅监控网段访问）
- Grafana 看板 5 套（系统 / JVM / DB 连接池 / API 性能 / 业务指标）
- Alertmanager 告警 → 医院 IT 钉钉 / 企业微信 / 短信

**告警阈值（参考）**：
| 指标 | Warning | Critical |
|---|---|---|
| API P95 延迟 | > 1s 持续 5min | > 3s 持续 2min |
| 5xx 错误率 | > 0.5% 持续 5min | > 2% 持续 2min |
| JVM 堆内存使用 | > 75% 持续 10min | > 90% 持续 2min |
| HikariCP 活跃连接 | > 80% pool | > 95% pool |
| DB 慢查询 | > 1 个 /s > 500ms | > 5 个 /s > 1s |
| 审计日志写入失败 | 1 次 | 3 次连续 |

### 2.7 灾备

| 级别 | RPO | RTO | 实现 |
|---|---|---|---|
| **L1 同机备份** | 24h | 1h | 每日 logical backup + 30 天保留 |
| **L2 同机房不同节点** | 1h | 15min | DB 主从复制（Oracle DG / DM DSC / PG Replication） |
| **L3 同城双机房** | 5min | 5min | 同步复制（如医院两个机房 < 50km） |
| **L4 异地灾备** | 15min | 30min | 异地异步复制（专线） |

医院 L1 必备；L2-L4 按预算和等级要求选。

### 2.8 等保 2.0 三级落地清单（内网部署专用）

| 控制项 | 内网落地方式 |
|---|---|
| 8.1.4.1 身份鉴别 | 密码 + MFA（手机短信，专网短信网关） + 会话超时 |
| 8.1.4.2 访问控制 | RBAC + 行级权限（tenant_id + scope_code） |
| 8.1.4.3 安全审计 | `ENGINE_AUDIT_LOG` + 审计链验签 + 不允许 DELETE |
| 8.1.4.4 入侵防范 | 主机：fail2ban + 防火墙；网络：医院已有 IPS |
| 8.1.4.5 恶意代码防范 | 主机：ClamAV 周扫；CI：Trivy 扫 artifact |
| 8.1.4.6 可信验证 | release artifact GPG 签名 + 客户验签后再部署 |
| 8.1.4.7 数据传输完整性 | HTTPS（TLS 1.2+ 国密 TLCP）+ DB 连接加密 |
| 8.1.4.8 数据存储保密性 | HEALTH_DATA 字段 SM4 加密；DB 透明加密（TDE） |
| 8.1.4.9 数据备份 | L1-L4 灾备 |
| 8.1.4.10 剩余信息保护 | 用户删除 → 30 天后物理清理（cache / log / temp） |
| 8.1.4.11 个人信息保护 | PII 字段最小化展示 + 脱敏 |
| 8.1.4.12 安全管理中心 | 集成统一日志 / 告警 / 配置中心（医院 SOC） |

---

## 3. 外网部署详细规格（SaaS）

### 3.1 云厂商选型

| 维度 | 阿里云（推荐）| 腾讯云 | 华为云 | AWS（仅海外） |
|---|:---:|:---:|:---:|:---:|
| 中国节点 | ✅（杭州/北京/上海） | ✅ | ✅ | ❌ |
| Oracle 兼容 RDS | ✅ PolarDB-O | ✅ TDSQL Oracle | ✅ GaussDB(for openGauss) | ✅ RDS Oracle |
| K8s 服务 | ACK | TKE | CCE | EKS |
| WAF | ✅ | ✅ | ✅ | ✅ |
| CDN | ✅ | ✅ | ✅ | ✅ CloudFront |
| KMS | ✅ | ✅ | ✅ | ✅ |
| APM | ARMS | TAPM | APM | X-Ray |
| 日志 | SLS | CLS | LTS | CloudWatch |
| 网络分级 | 政务云 / 金融云 / 通用云 | 同左 | 同左 | — |
| 信通院评测 | ✅ 多项 | ✅ | ✅ | — |

**推荐组合**（v1.1 GA）：**阿里云金融云 ACK + PolarDB-O + SLS + ARMS + WAF + CDN + KMS**

### 3.2 多租户隔离模型

| 隔离方式 | 数据隔离 | 性能隔离 | 成本 | 适用 |
|---|:---:|:---:|---|---|
| **Schema 内 row-level（tenant_id）** | 弱 | 弱 | 最低 | **当前默认**（小客户） |
| **每租户独立 schema（同 DB）** | 中 | 中 | 中 | 中等客户 |
| **每租户独立 DB instance** | 强 | 强 | 高 | 三甲医院 SaaS（如有） |

**v1.1 GA 默认**：tenant_id row-level + 应用层强校验（每个 query 强制带 `WHERE tenant_id = ?`）

**强校验实现**：
- 拦截器 `TenantContextInterceptor` 从 JWT 提取 `tenant_id` 注入 `ThreadLocal`
- 所有 Repository 方法签名必须有 `tenantId` 参数（CI 强制 grep 检查）
- 全 query 走 MyBatis interceptor 自动注入 `WHERE tenant_id = ?`（若未来引入 MyBatis）

### 3.3 K8s 部署架构

```yaml
# 简化示意
namespace: medkernel-prod
deployments:
  - medkernel-backend (replicas=3, HPA 3-10)
  - medkernel-frontend (replicas=2, 静态 nginx)
services:
  - medkernel-backend (ClusterIP)
  - medkernel-frontend (ClusterIP)
ingress:
  - 域名: cockpit.medkernel.cn → frontend
  - 域名: api.medkernel.cn → backend (API Gateway 后)
configmaps:
  - medkernel-config (非敏感)
secrets:
  - medkernel-jwt (KMS 加密)
  - medkernel-db (KMS 加密)
externalServices:
  - PolarDB-O (Oracle 兼容)
  - Redis (Tair / 自建)
  - SLS (日志)
  - ARMS (APM)
  - WAF (前置)
  - CDN (静态资源)
```

### 3.4 API 网关

| 选项 | 优点 | 缺点 | 推荐 |
|---|---|---|:---:|
| **APISIX** | 国产 / 高性能 / 插件丰富 | 学习曲线 | ✅ |
| Kong | 生态成熟 | 重 | |
| 阿里云 API Gateway | 即用 / 计费一体 | 锁定阿里 | 备选 |

**API Gateway 核心能力**：
- 鉴权（JWT 验证 + 租户隔离）
- 限流（按租户配额）
- 熔断（上游异常隔离）
- 协议转换（gRPC / WebSocket 透传）
- 灰度发布
- 审计日志

### 3.5 多租户开通自动化

新租户开通流程（应在 10 分钟内自动完成）：

```
[1. 客户支付] 在线下单 → 支付成功 webhook 触发
[2. 后端创建 tenant]
    - 生成 tenant_id (UUID)
    - 初始化租户元数据（公司名、套餐、过期日）
    - 创建管理员账号 + 发送欢迎邮件 + 重置密码链接
[3. 数据准备]
    - 不创建新 DB schema（用 row-level）
    - 加载默认配置包（fixture 数据）
    - 注册默认 SSO 提供商（账密 + 手机号短信）
[4. 域名 / 路由]
    - 不分配独立子域名（用 ?tenant=xxx 参数 或 path /t/xxx）
    - 或者高级客户：分配 *.medkernel.cn 子域名
[5. 通知]
    - 发送「开通成功」邮件 / 短信
    - 引导到管理员引导流程（5 步向导）
```

详见 PR-V3-04 [`tenant onboarding wizard`]（v0.3 必修）+ PR-V1.1-SAAS-AUTO。

### 3.6 SaaS 计费

| 计费维度 | 单位 | 说明 |
|---|---|---|
| **基础订阅** | ¥X / 月 | 套餐：免费试用 / 标准 / 专业 / 旗舰 |
| **API 调用次数** | 包月（套餐内）+ 超量 ¥X/万次 | 按 trace 计 |
| **规则评估次数** | 包月 + 超量 | |
| **存储** | 包月（套餐内）+ 超量 ¥X/GB/月 | DB + 日志 + 文件 |
| **AI 调用** | 按 token | Dify 调用大模型成本透传 + 服务费 |

实施：v1.1 GA 启动 PR-V1.1-BILLING。

### 3.7 数据本地化与跨境

**绝对禁止**：
- 患者数据离开中国境内
- Dify 调用境外大模型（OpenAI / Claude / Gemini）

**强制约束**（v1.1 GA）：
- application.yml `medkernel.saas.cloud-region=cn-hangzhou` 必填
- Dify provider 白名单：通义千问、文心一言、智谱、月之暗面、DeepSeek、Kimi、星火、混元（仅限国产）
- 数据出境评估：**100 万人个人信息出境**前必须申请评估（30-60 天）

### 3.8 SaaS 特有合规

| 法规 | 行动 |
|---|---|
| ICP 备案 | 主体备案 + 域名备案（20 天工信部） |
| 公安备案 | 公安局网安备案（30 天） |
| 互联网医院许可证 | 如做远程诊疗：卫健委发（30-90 天） |
| 网络文化经营许可证 | 如有内容运营：文旅部 |
| 等保 2.0 三级 | 必须 |
| 商用密码应用安全性评估（密评） | 必须 |

---

## 4. 双形态共同 PR（v0.3 / v1.0 GA / v1.1）

| PR | 内容 | v0.3 | v1.0 GA | v1.1 |
|---|---|:---:|:---:|:---:|
| PR-V3-COMPLIANCE-BACKEND | application.yml 合规配置 | ✅ | | |
| PR-V3-PROM | Prometheus / Grafana | ✅ | | |
| PR-V3-LOGBACK | JSON 日志 + traceId | ✅ | | |
| PR-V3-ACTUATOR | actuator endpoints | ✅ | | |
| PR-V3-RESILIENCE | 熔断限流 | ✅ | | |
| PR-V3-HIKARI | 连接池 | ✅ | | |
| PR-V3-FLYWAY | DB migration | ✅ | | |
| PR-V3-GMSSL | 国密 | ✅ | | |
| PR-V3-DATA-CLASS | 数据分级 + SM4 加密 | ✅ | | |
| PR-V3-OPENAPI | OpenAPI 文档 | ✅ | | |
| 等保 2.0 评测 | 测评机构出报告 | | ✅ | |
| 国密评测 | 信通院出报告 | | ✅ | |
| PR-V1.0-PENTEST | 渗透测试 | | ✅ | |
| PR-V1.0-USER-MANUAL | 用户手册 4 册 | | ✅ | |
| PR-V1.0-OPS-MANUAL | 运维手册 | | ✅ | |
| PR-V1.0-LEGAL | 合同 / SLA / 服务条款 | | ✅ | |
| PR-V1.0-SOFTWARE-COPYRIGHT | 软著 + 商标 | | ✅ | |
| PR-V1.0-DEPLOY-DRILL | 备份恢复 / 升级回滚演练 | | ✅ | |
| PR-V1.1-SAAS-INFRA | K8s + APISIX + WAF + CDN | | | ✅ |
| PR-V1.1-MULTITENANT | tenant_id row-level 强校验 + Repository CI 检查 | | | ✅ |
| PR-V1.1-BILLING | 订阅 + 用量 + 对账 + 发票 | | | ✅ |
| PR-V1.1-DATA-CROSS-EVAL | 数据出境评估（如适用） | | | ✅ |

---

## 5. 选型决策树（给客户售前用）

```
客户来咨询 → 问 3 个问题：

Q1. 数据是否能出客户机房？
  ├─ 不能 → 内网部署 (走 §2)
  └─ 能 → 进 Q2

Q2. 客户付费意愿 / 规模？
  ├─ ¥80 万 + 一次性 → 内网部署
  ├─ ¥0.5-5 万 / 月 / 持续 → SaaS (走 §3)
  └─ 不确定 → 推荐 SaaS 试用 → 满意后转内网部署

Q3. 客户特殊合规要求？
  ├─ 国央企 / 政务 / 涉密 → 内网部署 + 信创栈（UOS/麒麟/达梦）
  ├─ 三甲 + 卫健委标准 → 内网部署 + 等保三级 + 国密
  └─ 中小医院 / 民营 → SaaS 标准套餐
```

---

## 6. 与现有 deploy/ 的关系

| 现有 | 状态 | v0.3 / v1.0 改造 |
|---|---|---|
| `deploy/scripts/install-offline.sh` | ✅ 已完成（Oracle/DM 半自动 + PG 全自动） | v0.3：Oracle/DM DDL 也改为全自动 `sqlplus -L <` |
| `deploy/scripts/upgrade.sh` | ✅ 已完成 | v0.3：加 pre-upgrade 数据完整性检查 |
| `deploy/scripts/rollback.sh` | ✅ 已完成 | v0.3：增加 DDL 反向 migration 调用 |
| `deploy/profiles/*.env` | ✅ 4 个 profile | v0.3：加 `kingbase-x86_64.env` profile |
| `deploy/nginx/*` | ✅ http + tls | v0.3：加 nginx 国密 TLCP 配置示例 |
| `deploy/systemd/medkernel.service` | ✅ Linux | v0.3：加 Windows Service 启动配置 |
| SaaS 部署 | 🔴 0 | v1.1：新增 `deploy/k8s/` Helm Chart |

---

**End of dual-mode deployment spec.**
**核心思想**：**内网与外网共享一份代码 + 一套配置抽象 + 两套部署形态**。差异通过 application.yml profile 和 deploy 脚本切换，业务代码 0 区别。**这是云原生 + 国情合规的最优解**。
