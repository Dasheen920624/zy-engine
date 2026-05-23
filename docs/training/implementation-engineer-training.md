# 实施工程师培训材料

> **MedKernel 集团医疗智能中枢 · 实施工程师培训**
> 版本：1.0 · 2026-05-23
> 适用对象：负责医院现场部署、配置、数据迁移和客户培训的实施工程师

---

## 1. 培训概述

### 1.1 培训目标

完成本培训后，实施工程师应能够：

1. 独立完成 MedKernel 系统在医院内网环境的安装部署
2. 根据医院实际环境完成数据库选型、容量规划和网络架构设计
3. 完成组织机构、用户权限、SSO 集成等业务配置
4. 执行患者数据迁移、知识库初始化、规则库导入等数据迁移任务
5. 编写并执行验收测试计划，完成客户培训交付
6. 处理上线日常见问题，完成运维交接

### 1.2 培训安排

| 项目 | 说明 |
|---|---|
| 培训时长 | 5 天（40 学时） |
| 培训对象 | 实施工程师 |
| 培训方式 | 理论讲授（40%）+ 实操练习（60%） |
| 考核方式 | 理论笔试 + 实操考核 + 培训模拟 |

### 1.3 前置要求

| 技能领域 | 最低要求 |
|---|---|
| Linux 系统管理 | 熟练使用 Shell，掌握 systemd/journalctl，能进行用户和权限管理 |
| 数据库管理 | 熟悉至少一种关系型数据库（Oracle / PostgreSQL / DM / KingbaseES），能执行 DDL 和数据导入 |
| 网络基础 | 理解 TCP/IP、反向代理、SSL/TLS、防火墙配置 |
| 项目管理 | 了解项目实施流程，能编写实施计划和验收报告 |

---

## 2. 第一天：产品架构与部署准备（8 学时）

### 2.1 上午：MedKernel 产品架构深度解析（4 学时）

#### 2.1.1 产品定位

MedKernel 是**集团医疗智能中枢**，解决医院三大核心卡口：

| 卡口 | 现状 | MedKernel 方案 |
|---|---|---|
| 临床路径"画在 Word 里" | 路径定义是文档，无法执行、无法版本化 | 数字化路径模板，版本化、可灰度、可回滚 |
| 规则"埋在医生脑子里" | 医嘱合理性靠经验，新人靠老师傅带 | 规则引擎自动提醒，CDSS 实时拦截 |
| 知识"散在指南/文献里" | 最新指南更新慢、查阅难 | 知识工厂统一管理，来源可追溯 |

**核心定位**：让医院能像产品经理管理软件一样管理临床路径、规则和医学知识——版本化、可灰度、可追溯、可下线，每一次诊疗决策都能回到来源。

#### 2.1.2 三产品 × 四大模块

**三产品**（三种用户节奏）：

| 产品 | 用户 | 节奏 | 说明 |
|---|---|---|---|
| A · 知识工厂 | 医学专家 / 信息科 / 临床路径主任 | 周-月 | 管理路径/规则/图谱/字典/适配器，可版本化/灰度/回滚 |
| B · 临床嵌入器 | 医生 / 护士 / 药师 | 秒-分钟 | 嵌入 HIS/EMR，在医生工作流里推送合理建议 |
| C · 质控驾驶舱 | 集团 CIO / CMO / 质控科 / 医保科 | 日-周 | 合规率/路径执行率/CDSS 命中率/提醒疲劳监控 |

**四大治理模块**（服务端能力归类）：

| 模块 | 内容 | 责任团队 |
|---|---|---|
| M1 · 知识与配置（KnowledgeOps） | 配置包/路径/规则/图谱/字典/适配器/Dify/来源 | 知识团队 |
| M2 · 临床决策（ClinicalDecision） | CDSS 触发点/推荐/提醒疲劳/医疗安全红线 | 临床团队 |
| M3 · 质控与评估（QualityOps） | 院级驾驶舱/质控预警/评估指标/评估结果 | 质控团队 |
| M4 · 平台底座（Platform） | 组织/用户/身份/SSO/MPI/租户/审计/通知/安全基线 | 平台团队 |

#### 2.1.3 技术架构

```
┌─────────────────────────────────────────────────────┐
│                    Nginx 反向代理                      │
│              (HTTP:80 / HTTPS:443)                    │
├──────────────┬──────────────────────────────────────┤
│  前端静态资源   │         后端 API 反代                  │
│  React 18 +   │     /medkernel/* → 127.0.0.1:18080   │
│  Vite 5 +     │     Actuator  → 127.0.0.1:18081      │
│  AntD 5       │     (仅 127.0.0.1 可达)               │
├──────────────┴──────────────────────────────────────┤
│              Spring Boot 2.7.18 (JDK 1.8)             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│  │ 规则引擎  │ │ 路径引擎  │ │ 知识管理  │ │ 质控引擎 │ │
│  └──────────┘ └──────────┘ └──────────┘ └─────────┘ │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│  │ CDSS     │ │ 组织权限  │ │ 审计日志  │ │ 安全基线 │ │
│  └──────────┘ └──────────┘ └──────────┘ └─────────┘ │
│  HikariCP 连接池 │ Flyway 迁移 │ Prometheus 指标       │
├─────────────────────────────────────────────────────┤
│    Oracle │ DM（达梦）│ PostgreSQL │ KingbaseES        │
└─────────────────────────────────────────────────────┘
```

关键技术栈：

| 组件 | 版本/说明 |
|---|---|
| Spring Boot | 2.7.18 |
| JDK | 1.8.0_202+（国产化推荐毕昇 JDK 8） |
| 连接池 | HikariCP |
| 数据库迁移 | Flyway |
| 监控 | Spring Actuator + Prometheus + Grafana |
| 前端 | React 18 + Vite 5 + Ant Design 5 |

#### 2.1.4 多租户架构和权限体系

- **租户隔离**：通过 `X-Tenant-Id` Header 实现租户级数据隔离
- **组织上下文**：医院 → 院区 → 科室 → 病区，通过 Header 透传（`X-Hospital-Code` / `X-Department-Code` 等）
- **权限模型**：RBAC（菜单权限 + 按钮权限 + 数据权限）
- **身份集成**：支持 CAS / OIDC / SAML / LDAP-AD 四种 SSO 协议

#### 2.1.5 国产化适配

MedKernel 全面支持国产化环境：

| 维度 | 支持范围 |
|---|---|
| **数据库（4种）** | Oracle 11g/12c/18c/19c/21c、达梦 DM 7/8、PostgreSQL 14/15/16、人大金仓 KingbaseES V8 R3/R6 |
| **操作系统（4种+）** | 麒麟 Kylin V10 SP3、统信 UOS V20、openEuler 22.03、CentOS 7、Windows Server |
| **CPU 架构** | x86_64、aarch64（鲲鹏/飞腾）、loongarch64（龙芯） |
| **JDK** | 毕昇 JDK 8（鲲鹏优化版）、OpenJDK 8、Temurin 8 |

Profile 命名约定：`<os>-<cpu>-<db>.env`

| Profile 文件 | 适用环境 |
|---|---|
| `centos7-x86_64-oracle.env` | CentOS 7 + Oracle |
| `pg-x86_64.env` | 通用 Linux + PostgreSQL |
| `kylin-aarch64-dm.env` | 麒麟 V10 + 达梦（鲲鹏/飞腾） |
| `uos-aarch64-dm.env` | 统信 UOS + 达梦（鲲鹏/飞腾） |
| `kingbase-x86_64.env` | 通用 Linux + 人大金仓 |

---

### 2.2 下午：部署前环境评估（4 学时）

#### 2.2.1 环境检查脚本 check-env.sh

部署前必须运行环境检查脚本，确认目标机器满足所有前置条件：

```bash
# 基本用法
sudo ./scripts/check-env.sh

# 指定 profile
sudo ./scripts/check-env.sh --profile kylin-aarch64-dm
```

检查项清单：

| 检查项 | 通过条件 | 失败处理 |
|---|---|---|
| OS 与硬件 | 识别为支持列表中的 OS + CPU 架构 | 确认是否在支持列表，联系研发评估 |
| JDK | 检测到 JDK 1.8 | 安装 JDK 1.8（推荐毕昇 JDK / Temurin / OpenJDK） |
| locale | `LANG` 包含 UTF-8 | `export LANG=zh_CN.UTF-8` |
| 时区 | Asia/Shanghai 或 CST | `timedatectl set-timezone Asia/Shanghai` |
| 磁盘空间 | `$MK_HOME` 可用 ≥ 10GB | 清理磁盘或扩容 |
| 后端端口 | 18080 端口空闲 | 释放端口或修改 `MEDKERNEL_HTTP_PORT` |
| 前端端口 | 80 端口空闲（Nginx） | 确认 Nginx 未运行或调整端口 |
| SELinux | Permissive 或 Disabled | 生产环境建议 Permissive + fcontext 配置 |
| 防火墙 | firewalld 端口已开放 | `firewall-cmd --add-port=18080/tcp --permanent` |
| 数据库连通 | 对应 dialect 的客户端能连接 | 检查 DB 凭据和网络连通性 |

**输出示例**：

```
==> OS 与硬件
[OK]    OS: kylin:V10
[OK]    CPU 架构：aarch64

==> JDK
[OK]    JDK：1.8.0_402

==> 目录与磁盘
[OK]    /zoesoft/medkernel 可用空间 50GB

==> 端口
[OK]    后端端口 18080 空闲
[OK]    前端端口 80 空闲

==> 结果
  OK   : 8
  WARN : 1
  SKIP : 1
  FAIL : 0
```

> ⚠️ 存在 FAIL 项时，**必须先修复后再继续安装**。

#### 2.2.2 网络架构规划

```
                    ┌──────────────────┐
                    │  医院内网客户端    │
                    │  (HIS/EMR/浏览器)  │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │   Nginx (80/443)  │ ← 业务端口（对外）
                    │   反向代理 + 静态  │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
    ┌─────────▼────┐  ┌─────▼──────┐  ┌───▼──────────┐
    │ 前端静态资源   │  │ 后端 API    │  │ Actuator     │
    │ /zoesoft/    │  │ 127.0.0.1: │  │ 127.0.0.1:  │
    │ medkernel/   │  │ 18080      │  │ 18081        │
    │ frontend/    │  │            │  │ (仅本地可达)  │
    │ dist/        │  │            │  │              │
    └──────────────┘  └─────┬──────┘  └──────────────┘
                            │
                    ┌───────▼────────┐
                    │   数据库服务器   │ ← 管理端口（内网）
                    │ Oracle/DM/PG/  │
                    │ KingbaseES     │
                    └────────────────┘
```

端口规划：

| 端口 | 用途 | 绑定地址 | 防火墙 |
|---|---|---|---|
| 80 | Nginx HTTP | 0.0.0.0 | 开放 |
| 443 | Nginx HTTPS | 0.0.0.0 | 开放 |
| 18080 | Spring Boot 业务端口 | 127.0.0.1 | 不开放 |
| 18081 | Actuator 管理端口 | 127.0.0.1 | 不开放 |
| 9090 | Prometheus | 10.0.0.0/8 | 仅内网 |
| 3000 | Grafana | 10.0.0.0/8 | 仅内网 |
| 9093 | AlertManager | 10.0.0.0/8 | 仅内网 |

#### 2.2.3 数据库选型和容量规划

| 数据库 | 适用场景 | 最低配置 | 推荐配置 |
|---|---|---|---|
| Oracle | 大型三甲医院，已有 Oracle DBA | 4C/8G/100G | 8C/16G/500G |
| 达梦 DM 8 | 国产化要求，信创项目 | 4C/8G/100G | 8C/16G/500G |
| PostgreSQL | 中型医院，成本敏感 | 2C/4G/50G | 4C/8G/200G |
| KingbaseES | 国产化要求，PG 兼容 | 2C/4G/50G | 4C/8G/200G |

容量估算参考（单院区，1000 床位）：

| 数据类型 | 年增量 | 3年总量 | 说明 |
|---|---|---|---|
| 患者主索引（MPI） | ~5 万条 | ~15 万条 | 含基本信息和就诊记录 |
| 规则评估日志 | ~500 万条 | ~1500 万条 | 按规则触发频率估算 |
| 审计日志 | ~200 万条 | ~600 万条 | 含操作日志和访问日志 |
| 临床路径实例 | ~3 万条 | ~9 万条 | 含节点流转记录 |

#### 2.2.4 高可用和灾备方案设计

| 方案 | 说明 | 适用场景 |
|---|---|---|
| 应用层 HA | Nginx upstream 多实例 + keepalived | 7×24 业务连续性要求 |
| 数据库 HA | Oracle RAC / DM 集群 / PG 流复制 | 数据零丢失要求 |
| 冷备 | 每日全量备份 + 增量备份 | 一般医院 |
| 灾备 | 异地机房 + 数据同步 | 集团化医院 |

备份策略：

```bash
# 备份目录
MK_BACKUP_DIR=/zoesoft/medkernel.bak

# 升级前自动备份（upgrade.sh 自动执行）
# 备份内容：lib/ frontend/ conf/ systemd/ nginx/ manifest.json

# 数据库备份（由 DBA 执行）
# Oracle: RMAN 全量备份
# DM: dexp 全量导出
# PG: pg_dump 全量备份
# KingbaseES: sys_dump 全量备份
```

#### 2.2.5 实施项目计划模板

| 阶段 | 工作日 | 里程碑 | 交付物 |
|---|---|---|---|
| T0 准备阶段 | D-14 ~ D-10 | 环境确认 | 环境评估报告、网络拓扑图 |
| T1 部署阶段 | D-5 ~ D-3 | 系统安装 | 安装验证报告、健康检查通过 |
| T2 配置阶段 | D-2 ~ D+2 | 业务配置完成 | 组织机构/用户/权限配置表 |
| T3 迁移阶段 | D+3 ~ D+7 | 数据迁移完成 | 数据迁移报告、数据校验表 |
| T4 验收阶段 | D+8 ~ D+12 | 验收通过 | 验收测试报告、验收签收单 |
| T5 上线阶段 | D+13 ~ D+14 | 正式上线 | 上线报告、运维交接文档 |
| T6 保障阶段 | D+15 ~ D+30 | 稳定运行 | 运维周报、问题清单 |

---

## 3. 第二天：安装部署与初始配置（8 学时）

### 3.1 上午：完整安装流程（4 学时）

#### 3.1.1 发布包构建（在 dev/CI 机器上执行）

```bash
# Linux / macOS
./deploy/scripts/build-release.sh \
  --version 1.2.3 \
  --jdk-targets linux-x86_64,linux-aarch64,windows-x86_64 \
  --include-frontend \
  --output ./dist

# 产物：dist/medkernel-v1.2.3-a1b2c3d.tar.gz
```

发布包目录结构：

```
medkernel/
├── lib/medkernel.jar              # 后端 JAR
├── frontend/dist/                  # 前端构建产物
├── db/                             # DDL 和迁移脚本
│   ├── oracle/
│   ├── dm/
│   ├── postgres/
│   └── kingbase/
├── scripts/                        # 部署脚本
├── systemd/medkernel.service       # systemd 单元文件
├── nginx/                          # Nginx 配置模板
│   ├── medkernel.conf              # HTTP 版
│   └── medkernel-tls.conf          # HTTPS 版
├── profiles/                       # 环境 Profile 模板
├── monitoring/                     # 监控栈配置
│   ├── prometheus/
│   ├── grafana/dashboards/
│   └── alertmanager/
├── conf/                           # 配置目录
│   └── medkernel.env               # 环境变量（需手动创建）
├── manifest.json                   # 发布包清单
└── CHANGELOG.md                    # 变更日志
```

#### 3.1.2 安装流程 7 步详解

**第 1 步：上传发布包**

```bash
# 上传
scp medkernel-v1.2.3-a1b2c3d.tar.gz deploy-host:/tmp/

# SHA256 校验
ssh deploy-host
cd /tmp
sha256sum -c medkernel-v1.2.3-a1b2c3d.tar.gz.sha256
```

**第 2 步：解压**

```bash
sudo mkdir -p /zoesoft/medkernel
sudo tar -xzvf medkernel-v1.2.3-a1b2c3d.tar.gz -C /zoesoft/medkernel --strip-components=1
```

**第 3 步：选择 Profile 并配置环境变量**

```bash
cd /zoesoft/medkernel

# 根据目标环境选择 profile
sudo cp profiles/kylin-aarch64-dm.env conf/medkernel.env

# 编辑配置，填入实际 DB 凭据
sudo vi conf/medkernel.env

# 设置安全权限
sudo chmod 600 conf/medkernel.env
sudo chown medkernel:medkernel conf/medkernel.env
```

**第 4 步：环境检查**

```bash
sudo ./scripts/check-env.sh
# 或指定 profile
sudo ./scripts/check-env.sh --profile kylin-aarch64-dm
```

**第 5 步：执行安装**

```bash
# 首次安装（含 DDL 初始化）
sudo ./scripts/install-offline.sh --init-db

# 仅迁移（升级场景）
sudo ./scripts/install-offline.sh --migrate-db

# 跳过数据库（DBA 手动执行 DDL）
sudo ./scripts/install-offline.sh --skip-init-db
```

install-offline.sh 7 步执行流程：

| 步骤 | 操作 | 说明 |
|---|---|---|
| 1 | 检查环境 | 调用 check-env.sh，FAIL 则中止 |
| 2 | 创建运行用户与目录 | 创建 `medkernel` 用户，创建 logs/conf/backup 目录 |
| 3 | 处理数据库 DDL | 根据 `MEDKERNEL_DB_DIALECT` 执行对应 DDL |
| 4 | 注册 systemd | 安装 medkernel.service 并 enable |
| 5 | 启动应用 | `systemctl restart medkernel` |
| 6 | 健康检查 | 调用 healthcheck.sh 验证 |
| 7 | 完成 | 输出版本号和下一步建议 |

**第 6 步：验证**

```bash
./scripts/healthcheck.sh
```

**第 7 步：配置 Nginx**

```bash
sudo cp /zoesoft/medkernel/nginx/medkernel.conf /etc/nginx/conf.d/
sudo nginx -t && sudo systemctl reload nginx
```

#### 3.1.3 数据库初始化：4 种数据库 DDL 执行

DDL 目录结构：

```
db/
├── oracle/          # Oracle DDL
│   ├── medkernel_core_ddl_with_comments.sql
│   └── medkernel_org_context_migration.sql
├── dm/              # 达梦 DDL
│   ├── medkernel_core_ddl_with_comments.sql
│   └── medkernel_org_context_migration.sql
├── postgres/        # PostgreSQL DDL
│   ├── medkernel_core_ddl_with_comments.sql
│   └── medkernel_org_context_migration.sql
└── kingbase/        # 人大金仓 DDL（PG 兼容模式）
```

各数据库 DDL 执行方式：

| 数据库 | 执行命令 | 说明 |
|---|---|---|---|
| Oracle | `sqlplus MEDKERNEL/password@//10.0.0.10:1521/ORCL @db/oracle/medkernel_core_ddl_with_comments.sql` | 需 DBA 配合执行 |
| 达梦 DM | `disql MEDKERNEL/password@10.0.0.20:5236 -e "START 'db/dm/medkernel_core_ddl_with_comments.sql';"` | 需信息科配合 |
| PostgreSQL | `PGPASSWORD=xxx psql -h 10.0.0.30 -U medkernel -d medkernel -v ON_ERROR_STOP=1 -f db/postgres/medkernel_core_ddl_with_comments.sql` | install-offline.sh 可自动执行 |
| KingbaseES | 使用 ksql 或 psql 客户端，语法与 PG 兼容 | Flyway 映射到 postgres/ 迁移目录 |

#### 3.1.4 Flyway 迁移配置

MedKernel 使用 Flyway 管理数据库版本迁移：

- 迁移脚本位于各数据库 DDL 目录下
- KingbaseES 使用 PG 兼容模式，Flyway 映射到 `postgres/` 迁移目录
- 升级时使用 `--migrate-db` 参数自动执行迁移
- 迁移失败处理参见 `docs/engineering/flyway-rollback-guide.md`

#### 3.1.5 应用配置详解：medkernel.env 全部参数

以 `pg-x86_64.env` 为例，完整参数说明：

```bash
# ==== 路径常量 ====
MK_HOME=/zoesoft/medkernel              # 安装根目录
MK_USER=medkernel                        # 运行用户
MK_BACKUP_DIR=/zoesoft/medkernel.bak     # 备份目录

# ==== JDK ====
JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk   # JDK 路径
# 国产化 ARM 环境：JAVA_HOME=/opt/bisheng-jdk1.8.0_402

# ==== HTTP ====
MEDKERNEL_HTTP_PORT=18080                # 业务端口
MEDKERNEL_HTTP_CONTEXT=/medkernel        # 上下文路径

# ==== 数据库 ====
MEDKERNEL_DB_ENABLED=true                # 启用数据库
MEDKERNEL_DB_DIALECT=postgres            # 数据库类型：oracle/dm/postgres/kingbase
MEDKERNEL_DB_HOST=10.0.0.30              # 数据库主机
MEDKERNEL_DB_PORT=5432                   # 数据库端口
MEDKERNEL_DB_NAME=medkernel              # 数据库名
MEDKERNEL_DB_URL=jdbc:postgresql://${MEDKERNEL_DB_HOST}:${MEDKERNEL_DB_PORT}/${MEDKERNEL_DB_NAME}?reWriteBatchedInserts=true&prepareThreshold=0
MEDKERNEL_DB_USERNAME=medkernel          # 数据库用户
MEDKERNEL_DB_PASSWORD=__REPLACE_ME__     # 数据库密码（必须修改！）

# ==== 图谱 / Dify ====
MEDKERNEL_GRAPH_ENABLED=false            # 启用图谱功能
MEDKERNEL_DIFY_ENABLED=false             # 启用 Dify 工作流

# ==== JVM ====
JAVA_OPTS="-server -Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
```

各数据库 JDBC URL 模板：

| 数据库 | JDBC URL 模板 |
|---|---|
| Oracle | `jdbc:oracle:thin:@//10.0.0.10:1521/ORCL` |
| 达梦 DM | `jdbc:dm://10.0.0.20:5236?clobAsString=true&serverTimezone=Asia/Shanghai` |
| PostgreSQL | `jdbc:postgresql://10.0.0.30:5432/medkernel?reWriteBatchedInserts=true&prepareThreshold=0` |
| KingbaseES | `jdbc:kingbase8://10.0.0.40:54321/medkernel` |

> ⚠️ **安全要求**：`conf/medkernel.env` 必须设置 600 权限，含密码字段，禁止其他用户读取。

#### 3.1.6 多环境 Profile 配置

| Profile | OS | CPU | 数据库 | JDK | 特殊说明 |
|---|---|---|---|---|---|
| `centos7-x86_64-oracle` | CentOS 7 | x86_64 | Oracle | 随包 JDK | Oracle 连接串格式特殊 |
| `pg-x86_64` | 通用 Linux | x86_64 | PostgreSQL | 系统 OpenJDK | 最通用方案 |
| `kylin-aarch64-dm` | 麒麟 V10 | aarch64 | 达梦 DM 8 | 毕昇 JDK | 需加 `-Djdk.tls.client.protocols=TLSv1.2` |
| `uos-aarch64-dm` | 统信 UOS | aarch64 | 达梦 DM 8 | 毕昇 JDK | 同上 |
| `kingbase-x86_64` | 通用 Linux | x86_64 | KingbaseES | 系统 OpenJDK | PG 兼容模式 |

---

### 3.2 下午：Nginx 与监控栈部署（4 学时）

#### 3.2.1 Nginx 反向代理配置

**HTTP 配置**（`deploy/nginx/medkernel.conf`）：

关键配置项说明：

```nginx
# 后端上游
upstream medkernel_backend {
    server 127.0.0.1:18080 max_fails=3 fail_timeout=10s;
    keepalive 32;
}

# 限速：单 IP 每秒 100 请求
limit_req_zone $binary_remote_addr zone=medkernel_api:10m rate=100r/s;

server {
    listen 80;
    server_name _;

    # 安全头
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header X-XSS-Protection "1; mode=block" always;

    client_max_body_size 16M;

    # 前端静态资源
    root /zoesoft/medkernel/frontend/dist;
    index index.html;

    # SPA fallback
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 反代
    location /medkernel/ {
        limit_req zone=medkernel_api burst=200 nodelay;
        proxy_pass http://medkernel_backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 透传 traceId
        proxy_set_header X-Trace-Id $http_x_trace_id;
        proxy_pass_header X-Trace-Id;

        # 透传组织上下文 Header
        proxy_set_header X-Tenant-Id      $http_x_tenant_id;
        proxy_set_header X-Hospital-Code  $http_x_hospital_code;
        proxy_set_header X-Department-Code $http_x_department_code;

        proxy_connect_timeout 10s;
        proxy_send_timeout    60s;
        proxy_read_timeout    60s;
    }

    # 健康检查（外部可探测）
    location = /healthz {
        access_log off;
        proxy_pass http://medkernel_backend/medkernel/api/health;
    }
}
```

部署步骤：

```bash
sudo cp /zoesoft/medkernel/nginx/medkernel.conf /etc/nginx/conf.d/
sudo nginx -t && sudo systemctl reload nginx
```

#### 3.2.2 SSL 证书配置

**HTTPS 配置**（`deploy/nginx/medkernel-tls.conf`）：

1. **自签证书生成**（测试环境）：

```bash
sudo mkdir -p /etc/nginx/ssl
openssl req -x509 -nodes -days 3650 -newkey rsa:4096 \
  -keyout /etc/nginx/ssl/medkernel.key \
  -out    /etc/nginx/ssl/medkernel.crt \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=Hospital/OU=IT/CN=medkernel.hospital.local"
```

2. **医院 CA 证书**（生产环境）：

```bash
# 将医院 CA 颁发的证书链放入
sudo cp medkernel.crt /etc/nginx/ssl/
sudo cp medkernel.key /etc/nginx/ssl/
sudo chmod 600 /etc/nginx/ssl/medkernel.key
```

3. **部署 HTTPS 配置**：

```bash
sudo cp /zoesoft/medkernel/nginx/medkernel-tls.conf /etc/nginx/conf.d/medkernel.conf
sudo nginx -t && sudo systemctl reload nginx
```

HTTPS 关键配置：

```nginx
server {
    listen 443 ssl http2;
    server_name _;

    ssl_certificate     /etc/nginx/ssl/medkernel.crt;
    ssl_certificate_key /etc/nginx/ssl/medkernel.key;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5:!RC4:!DSS:!3DES;
    ssl_prefer_server_ciphers on;

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
}

# HTTP → HTTPS 重定向
server {
    listen 80;
    server_name _;
    return 301 https://$host$request_uri;
}
```

> ⚠️ 国密支持需 OpenSSL + Nginx 编译国密版，本配置走标准 TLS。

#### 3.2.3 监控栈部署

**一键部署监控栈**（`deploy/docker-compose.monitoring.yml`）：

```bash
cd /zoesoft/medkernel
docker compose -f docker-compose.monitoring.yml up -d
```

前置条件：
- MedKernel 后端已启动，Actuator 端口 18081 可达
- Docker 和 Docker Compose 已安装

监控栈组件：

| 组件 | 端口 | 镜像 | 说明 |
|---|---|---|---|
| Prometheus | 9090 | prom/prometheus:v2.54.1 | 指标采集和存储，30 天保留 |
| AlertManager | 9093 | prom/alertmanager:v0.27.0 | 告警路由和通知 |
| Grafana | 3000 | grafana/grafana:11.2.0 | 可视化看板 |

Grafana 默认账号：

| 项目 | 值 |
|---|---|
| 用户名 | admin |
| 密码 | medkernel_admin |
| 访问地址 | http://\<host\>:3000 |

> ⚠️ 生产环境必须修改 `GF_SECURITY_ADMIN_PASSWORD`。

#### 3.2.4 Prometheus 配置

配置文件：`deploy/monitoring/prometheus/prometheus-medkernel.yml`

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    product: medkernel
    environment: pilot

rule_files:
  - medkernel-alert-rules.yml

alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093

scrape_configs:
  - job_name: medkernel-backend
    scrape_interval: 15s
    scrape_timeout: 5s
    metrics_path: /medkernel/actuator/prometheus
    scheme: http
    static_configs:
      - targets:
          - host.docker.internal:18081
        labels:
          service: medkernel-mvp
          role: backend
```

#### 3.2.5 告警规则配置

告警规则文件：`deploy/monitoring/prometheus/medkernel-alert-rules.yml`

告警分级：

| 级别 | 响应时间 | 通知方式 | 典型场景 |
|---|---|---|---|
| critical | 5 分钟内 | 邮件 + Webhook（钉钉/企微/飞书） | 服务不可达、5xx 错误率 >2%、审计日志写入失败 |
| warning | 30 分钟内 | 邮件 + Webhook | P95 延迟 >1s、5xx 错误率 >0.5%、堆内存 >75% |

关键告警规则：

| 告警名称 | 条件 | 级别 | SLO 维度 |
|---|---|---|---|
| MedKernelBackendDown | `up == 0` 持续 1m | critical | 可用性 |
| MedKernelApiP95LatencyHigh | P95 > 1s 持续 5m | warning | 延迟 |
| MedKernelApiP95LatencyCritical | P95 > 3s 持续 2m | critical | 延迟 |
| MedKernelApi5xxRateHigh | 5xx > 0.5% 持续 5m | warning | 错误率 |
| MedKernelJvmHeapHigh | 堆使用 >75% 持续 10m | warning | 饱和度 |
| MedKernelJvmHeapCritical | 堆使用 >90% 持续 2m | critical | 饱和度 |
| MedKernelHikariPoolSaturation | 连接池使用 >80% 持续 5m | warning | 饱和度 |
| MedKernelDatabaseProviderDown | DB Provider 未就绪 2m | critical | 可用性 |
| MedKernelAuditLogWriteFailure | 审计日志写入失败 | critical | 安全 |

SLO 目标：

| SLO 维度 | 目标 | 说明 |
|---|---|---|
| 可用性 | 99.9% | 每月停机 ≤ 43.8 分钟 |
| 延迟 | P95 < 300ms | API 响应时间 |

#### 3.2.6 AlertManager 配置

配置文件：`deploy/monitoring/alertmanager/alertmanager.yml`

告警路由策略：

```
所有告警
├── critical → critical-receiver（邮件 + Webhook，5 分钟重复）
├── warning → warning-receiver（邮件 + Webhook，30 分钟重复）
└── Provider 相关 → provider-receiver（按 provider 分组）
```

抑制规则：critical 触发时，自动抑制同服务的 warning 告警。

对接钉钉/企业微信/飞书：修改 `webhook_configs.url` 为对应 Webhook 地址。

#### 3.2.7 健康检查和冒烟测试

**健康检查脚本**（`deploy/scripts/healthcheck.sh`）：

```bash
# 基本用法
./scripts/healthcheck.sh

# 指定 URL
./scripts/healthcheck.sh --url http://10.0.0.50:18080/medkernel

# 指定超时
./scripts/healthcheck.sh --timeout 30
```

检查项：

| 检查项 | 端点 | 预期结果 |
|---|---|---|
| 业务健康 | `/api/health` | `{"success": true}` |
| Provider 状态 | `/api/system/providers` | `{"success": true}` |
| 组织上下文 | `/api/system/org-context` | `{"success": true}` |
| Actuator 健康 | `:18081/actuator/health` | `{"status": "UP"}` |
| Prometheus 可达 | `:9090/-/healthy` | 200 OK |
| SLO 状态 | Prometheus 查询 | 无 critical 告警触发 |

**安全基线检查**（`deploy/scripts/security-baseline.sh`）：

```bash
# 基本检查
./scripts/security-baseline.sh

# 严格模式（WARN 视为 FAIL）
./scripts/security-baseline.sh --strict
```

等保 2.0 三级控制点检查项：

| 控制点 | 检查内容 | 状态 |
|---|---|---|
| 3.1.1.2 通信传输 | HTTPS 是否启用 | WARN（生产必须启用） |
| 3.1.3.1 身份鉴别 | 密码策略 + 登录锁定 + SSO | PASS |
| 3.1.3.2 访问控制 | RBAC + 数据权限 | PASS |
| 3.1.3.8 数据保密性 | SM4-CBC 加密 + 数据脱敏 | PASS |
| 3.1.3.9 数据备份恢复 | 备份脚本 + 回滚 + Flyway 回滚指南 | PASS |

---

## 4. 第三天：业务配置与数据迁移（8 学时）

### 4.1 上午：组织机构与权限配置（4 学时）

#### 4.1.1 组织机构配置

MedKernel 的组织模型：

```
集团/院区
├── 医院（Hospital）
│   ├── 院区（Campus）
│   │   ├── 科室（Department）
│   │   │   └── 病区（Ward）
│   │   └── 科室...
│   └── 院区...
└── 医院...
```

组织上下文通过 HTTP Header 透传：

| Header | 说明 | 示例 |
|---|---|---|
| `X-Tenant-Id` | 租户 ID | `tenant-001` |
| `X-Group-Code` | 集团编码 | `group-henan` |
| `X-Hospital-Code` | 医院编码 | `hospital-zzdy` |
| `X-Campus-Code` | 院区编码 | `campus-main` |
| `X-Site-Code` | 站点编码 | `site-east` |
| `X-Department-Code` | 科室编码 | `dept-cardiology` |

配置步骤：

1. 登录管理工作台，进入「用户与身份」→「组织管理」
2. 创建医院节点，填写医院编码和名称
3. 创建院区、科室、病区层级
4. 配置组织上下文映射关系

#### 4.1.2 用户和角色配置

用户管理功能：

| 功能 | 说明 |
|---|---|
| 用户创建 | 填写用户名、姓名、手机号、邮箱 |
| 角色分配 | 为用户分配一个或多个角色 |
| 科室关联 | 将用户关联到具体科室 |
| 状态管理 | 启用/禁用用户 |
| 密码重置 | 管理员重置用户密码 |

预置角色：

| 角色 | 权限范围 | 适用人员 |
|---|---|---|
| 系统管理员 | 全部权限 | 信息科管理员 |
| 知识管理员 | M1 知识与配置模块 | 医学专家/信息科 |
| 临床管理员 | M2 临床决策配置 | 医务处/临床科室 |
| 质控管理员 | M3 质控与评估 | 质控科/医保科 |
| 审计管理员 | 审计日志查看 | 审计人员（独立于系统管理员） |
| 只读用户 | 查看权限 | 院领导/科室主任 |

#### 4.1.3 权限配置

MedKernel 采用三级权限模型：

| 权限级别 | 说明 | 示例 |
|---|---|---|
| 菜单权限 | 控制可见菜单 | 质控管理员看不到「知识工厂」菜单 |
| 按钮权限 | 控制操作按钮 | 只读用户看不到「新建」「删除」按钮 |
| 数据权限 | 控制数据范围 | 科室主任只能看本科室数据 |

数据权限通过 `DataPermissionService` 实现，支持：

- 全院数据可见
- 本院区数据可见
- 本科室数据可见
- 本人数据可见

#### 4.1.4 SSO 集成

MedKernel 支持四种 SSO 协议：

| 协议 | 适用场景 | 配置方式 |
|---|---|---|
| CAS | 医院已有 CAS 统一认证 | 配置 CAS Server URL + Service URL |
| OIDC | 现代化身份提供商 | 配置 Issuer/Client ID/Client Secret |
| SAML 2.0 | 企业级 IdP（AD FS 等） | 配置 IdP Metadata + SP Entity ID |
| LDAP/AD | 医院域控 | 配置 LDAP URL/Base DN/Bind DN |

SSO 配置步骤：

1. 在「用户与身份」→「SSO 配置」中选择协议
2. 填写身份提供商连接参数
3. 配置用户属性映射（用户名、姓名、邮箱、科室等）
4. 测试连接
5. 启用 SSO 登录

登录页面提供两种方式：账号密码登录 + SSO 登录（Tab 切换）。

---

### 4.2 下午：数据迁移与初始化（4 学时）

#### 4.2.1 数据迁移策略和工具

数据迁移原则：

| 原则 | 说明 |
|---|---|
| 先迁后验 | 迁移完成后必须进行数据校验 |
| 分批迁移 | 大数据量分批导入，避免锁表 |
| 可回滚 | 迁移前备份，失败可回滚 |
| 脱敏传输 | 涉及患者隐私数据需加密传输 |

迁移流程：

```
1. 源系统数据导出 → 2. 数据清洗转换 → 3. 数据导入 → 4. 数据校验 → 5. 业务验证
```

#### 4.2.2 患者数据迁移（MPI）

患者主索引（MPI）迁移是核心任务：

| 数据项 | 源系统 | 目标表 | 说明 |
|---|---|---|---|
| 患者基本信息 | HIS 患者 | `mpi_patient` | 姓名、性别、出生日期、身份证号 |
| 就诊记录 | HIS 门诊/住院 | `mpi_encounter` | 就诊号、科室、时间 |
| 身份标识 | HIS 卡号 | `mpi_identity` | 医保号、就诊卡号 |

迁移注意事项：

- 身份证号等敏感字段需使用 SM4-CBC 加密存储
- 姓名等字段需配置数据脱敏策略
- MPI 去重：基于身份证号 + 姓名进行患者合并

#### 4.2.3 知识库初始化

知识包导入流程：

1. 获取标准知识包（由医学团队提供）
2. 在「知识工厂」→「AI 知识审核」中导入知识包
3. 审核知识条目，确认来源可追溯
4. 发布知识包到目标科室

知识包格式参考：`ai-dev-input/06_samples/sample_config_package.json`

#### 4.2.4 规则库初始化

规则配置包导入流程：

1. 获取标准规则配置包
2. 在「知识工厂」→「配置包中心」中导入
3. 配置规则触发条件和动作
4. 执行试运行（Dry Run）验证规则效果
5. 灰度发布到试点科室
6. 全量发布

规则配置包格式参考：`ai-dev-input/06_samples/sample_ami_rules.json`

#### 4.2.5 临床路径配置

路径模板配置流程：

1. 在「知识工厂」→「路径配置」中创建路径模板
2. 定义路径节点和流转条件
3. 关联规则和知识条目
4. 配置入径条件和出径条件
5. 试运行验证
6. 发布路径模板

路径模板格式参考：`ai-dev-input/06_samples/sample_ami_pathway.json`

#### 4.2.6 术语映射配置

术语映射用于对接院内不同系统的编码标准：

1. 在「知识工厂」→「字典映射」中创建映射表
2. 导入源系统编码和标准编码的对应关系
3. 配置自动映射规则
4. 验证映射覆盖率

术语映射格式参考：`ai-dev-input/06_samples/sample_dictionary_mappings.json`

---

## 5. 第四天：验收测试与客户培训（8 学时）

### 5.1 上午：验收测试（4 学时）

#### 5.1.1 验收测试计划编写

验收测试计划模板：

| 章节 | 内容 |
|---|---|
| 1. 测试范围 | 功能验收、性能验收、安全验收 |
| 2. 测试环境 | 硬件配置、软件版本、网络拓扑 |
| 3. 测试用例 | 按模块分组的测试用例清单 |
| 4. 通过标准 | 各项测试的通过条件 |
| 5. 测试进度 | 测试时间安排和人员分工 |
| 6. 风险和约束 | 测试限制和风险应对 |

#### 5.1.2 功能验收测试执行

按四大模块执行功能验收：

| 模块 | 核心验收项 | 验收标准 |
|---|---|---|
| M1 知识与配置 | 配置包创建/发布/回滚 | 配置包版本化，灰度发布成功 |
| M1 知识与配置 | 路径模板创建/入径/流转 | 路径节点流转正常 |
| M1 知识与配置 | 规则定义/试运行/发布 | Dry Run 结果正确 |
| M2 临床决策 | CDSS 触发/提醒/确认 | 医嘱提醒正常弹出 |
| M2 临床决策 | 医疗安全红线拦截 | 高风险医嘱被拦截 |
| M3 质控与评估 | 质控看板数据展示 | 指标计算正确 |
| M3 质控与评估 | 评估报告生成 | 报告内容完整 |
| M4 平台底座 | 用户登录/权限控制 | RBAC 生效 |
| M4 平台底座 | SSO 集成 | SSO 登录正常 |
| M4 平台底座 | 审计日志记录 | 操作可追溯 |

#### 5.1.3 性能基线测试

性能测试指标和通过标准：

| 指标 | SLO 目标 | 测试方法 |
|---|---|---|
| API 可用性 | ≥ 99.9% | 持续请求 30 分钟，统计 5xx 比例 |
| API P95 延迟 | ≤ 300ms | JMeter 模拟并发请求 |
| API P99 延迟 | ≤ 1s | JMeter 模拟并发请求 |
| 规则评估延迟 | ≤ 100ms | 单条规则评估计时 |
| 数据库查询 P99 | ≤ 500ms | Prometheus 监控指标 |
| HikariCP 连接池 | 使用率 < 80% | Prometheus 监控指标 |
| JVM 堆内存 | 使用率 < 75% | Prometheus 监控指标 |

性能测试工具：

```bash
# JMeter 测试脚本（示例）
# 并发用户数：100
# 持续时间：30 分钟
# 思考时间：1-3 秒随机
```

#### 5.1.4 安全验收测试

运行安全基线检查脚本：

```bash
./scripts/security-baseline.sh --strict
```

安全验收检查清单：

| 检查项 | 验收标准 |
|---|---|
| HTTPS 启用 | 生产环境必须启用 HTTPS |
| 密码策略 | ≥8 位 + 大小写 + 数字 + 特殊字符 |
| 登录锁定 | 5 次失败锁定 30 分钟 |
| Actuator 端口 | 仅绑定 127.0.0.1 |
| 数据加密 | SM4-CBC 加密敏感字段 |
| 数据脱敏 | 6 种脱敏策略可用 |
| 审计日志 | 180 天保留 + 链式校验 |
| RBAC | 菜单 + 按钮 + 数据三级权限 |
| 文件权限 | medkernel.env 权限 600 |

#### 5.1.5 验收报告编写

验收报告模板：

| 章节 | 内容 |
|---|---|
| 1. 项目概述 | 项目背景、实施范围 |
| 2. 测试执行情况 | 测试用例执行统计 |
| 3. 功能验收结果 | 各模块验收结论 |
| 4. 性能验收结果 | 性能指标和 SLO 达标情况 |
| 5. 安全验收结果 | 安全基线检查结果 |
| 6. 遗留问题 | 未解决问题清单和计划 |
| 7. 验收结论 | 通过/有条件通过/不通过 |

---

### 5.2 下午：客户培训交付（4 学时）

#### 5.2.1 客户培训材料准备

按角色准备培训材料：

| 角色 | 培训材料 | 培训时长 |
|---|---|---|
| IT 人员 | 系统架构、运维手册、故障应急、备份恢复 | 4 小时 |
| 知识管理员 | 知识工厂操作手册、配置包管理、规则配置 | 3 小时 |
| 临床用户 | CDSS 使用说明、医嘱提醒操作、路径入径 | 2 小时 |
| 质控人员 | 质控驾驶舱使用、评估报告查看、预警处理 | 2 小时 |
| 院领导 | 看板解读、数据指标说明 | 1 小时 |

#### 5.2.2 IT 人员培训交付

培训内容：

| 模块 | 内容 | 时长 |
|---|---|---|
| 系统架构 | 三层架构、端口规划、日志位置 | 30 分钟 |
| 日常运维 | 启停服务、查看日志、监控看板 | 45 分钟 |
| 备份恢复 | 备份策略、恢复流程、回滚操作 | 30 分钟 |
| 故障应急 | 常见故障排查、应急联系方式 | 30 分钟 |
| 安全管理 | 用户管理、权限配置、审计日志 | 30 分钟 |
| 升级流程 | 升级步骤、回滚步骤、验证方法 | 15 分钟 |

#### 5.2.3 临床用户培训交付

培训重点：

1. **CDSS 提醒操作**：如何查看医嘱提醒、确认/忽略提醒
2. **路径入径操作**：如何为患者选择临床路径、节点流转
3. **待办处理**：如何处理系统待办事项
4. **常见问题**：提醒不出现、路径无法入径等

> 关键原则：临床用户培训应**简短实用**，聚焦日常操作，避免技术细节。

#### 5.2.4 培训效果评估

| 评估方式 | 说明 | 通过标准 |
|---|---|---|
| 操作考核 | 学员独立完成核心操作 | 正确率 ≥ 80% |
| 问答考核 | 关键知识点提问 | 回答正确率 ≥ 70% |
| 满意度调查 | 培训满意度问卷 | 满意度 ≥ 4.0/5.0 |

#### 5.2.5 知识转移和文档交付

交付文档清单：

| 文档 | 位置 | 说明 |
|---|---|---|
| 运维手册 | `docs/ops/01_运维手册.md` | 日常运维操作指南 |
| 升级回滚手册 | `docs/ops/02_升级回滚手册.md` | 版本升级和回滚流程 |
| 故障应急手册 | `docs/ops/03_故障应急手册.md` | 故障排查和应急处理 |
| 用户手册 | `docs/user-guide/` | 各模块操作指南 |
| 安全基线报告 | `security-baseline.sh` 输出 | 等保合规检查结果 |
| 验收报告 | 项目验收文档 | 验收测试结果 |
| 实施记录 | 项目文档 | 配置变更记录 |

---

## 6. 第五天：上线支持与运维交接（8 学时）

### 6.1 上午：上线支持（4 学时）

#### 6.1.1 上线前检查清单

| # | 检查项 | 确认方式 | 负责人 |
|---|---|---|---|
| 1 | 环境检查通过 | `check-env.sh` 无 FAIL | 实施工程师 |
| 2 | 健康检查通过 | `healthcheck.sh` 全部 OK | 实施工程师 |
| 3 | 安全基线通过 | `security-baseline.sh --strict` | 实施工程师 |
| 4 | 数据库备份完成 | DBA 确认 | DBA |
| 5 | Nginx 配置正确 | `nginx -t` 通过 | 实施工程师 |
| 6 | 监控栈运行正常 | Prometheus/Grafana 可达 | 实施工程师 |
| 7 | SSO 集成测试通过 | SSO 登录成功 | 实施工程师 |
| 8 | 数据迁移校验通过 | 数据条目数一致 | 实施工程师 |
| 9 | 客户培训完成 | 培训签到表 | 实施工程师 |
| 10 | 应急联系确认 | 通讯录已分发 | 项目经理 |

#### 6.1.2 上线日操作流程

| 时间 | 操作 | 负责人 | 备注 |
|---|---|---|---|
| T-60min | 数据库全量备份 | DBA | 确认备份完成 |
| T-30min | 通知相关科室即将上线 | 项目经理 | 通知方式：电话+消息 |
| T-15min | 最终健康检查 | 实施工程师 | `healthcheck.sh` |
| T-0 | 正式切换 | 实施工程师 | 开放访问 |
| T+5min | 冒烟测试 | 实施工程师 | 核心功能验证 |
| T+15min | 监控确认 | 实施工程师 | 无 critical 告警 |
| T+30min | 用户验证 | 科室代表 | 实际操作确认 |
| T+60min | 稳定确认 | 实施工程师 | 无异常则上线成功 |
| T+120min | 第一轮巡检 | 实施工程师 | 检查日志和指标 |
| T+240min | 第二轮巡检 | 实施工程师 | 检查日志和指标 |

#### 6.1.3 上线监控和应急响应

上线期间重点监控：

```bash
# 实时查看应用日志
journalctl -u medkernel -f

# 查看最近 100 条日志
journalctl -u medkernel -n 100

# 查看应用标准输出
tail -f /zoesoft/medkernel/logs/stdout.log

# 查看应用错误输出
tail -f /zoesoft/medkernel/logs/stderr.log

# 检查 Prometheus 告警
curl -s http://localhost:9090/api/v1/alerts?state=firing | python3 -m json.tool
```

应急响应分级：

| 级别 | 条件 | 响应 | 时限 |
|---|---|---|---|
| P0 紧急 | 系统不可用/数据丢失 | 立即回滚 | 15 分钟 |
| P1 严重 | 核心功能异常 | 评估修复或回滚 | 30 分钟 |
| P2 一般 | 非核心功能异常 | 记录问题，计划修复 | 4 小时 |
| P3 轻微 | UI 显示/体验问题 | 记录问题，后续版本修复 | 24 小时 |

#### 6.1.4 常见上线问题处理

| 问题 | 现象 | 排查步骤 | 解决方案 |
|---|---|---|---|
| 服务启动失败 | `systemctl is-active` 返回 inactive | `journalctl -u medkernel -n 100` | 检查 JDK/端口/DB 连接 |
| 数据库连接失败 | 日志显示连接超时 | 检查 DB 凭据和网络 | 修正 medkernel.env 配置 |
| Nginx 502 | 页面显示 Bad Gateway | 检查后端是否运行 | `systemctl restart medkernel` |
| SSO 登录失败 | SSO 页面报错 | 检查 IdP 配置和网络 | 修正 SSO 配置参数 |
| 性能慢 | API 响应超过 3s | 检查 JVM 堆和 DB 慢查询 | 调整 JVM 参数或优化 SQL |
| 告警风暴 | 大量告警触发 | 检查告警规则和阈值 | 调整告警阈值或静默 |

回滚操作：

```bash
# 回滚到上一个版本
sudo ./scripts/rollback.sh --to last

# 回滚到指定备份
sudo ./scripts/rollback.sh --to 20260523_083000
```

---

### 6.2 下午：运维交接与考核（4 学时）

#### 6.2.1 运维交接

交接内容清单：

| 类别 | 交接内容 | 格式 |
|---|---|---|
| 文档 | 运维手册、升级手册、故障应急手册、用户手册 | 电子版 + 打印版 |
| 脚本 | 部署脚本目录（`/zoesoft/medkernel/scripts/`） | 文件系统 |
| 账号 | 系统管理员账号、Grafana 账号、数据库账号 | 密封信封 |
| 配置 | medkernel.env、Nginx 配置、Prometheus 配置 | 文件系统 |
| 监控 | Grafana 看板、告警规则、通知渠道 | 配置文件 |
| 备份 | 备份策略说明、最近一次备份位置 | 文档 |

#### 6.2.2 运维培训

日常运维操作速查：

| 操作 | 命令 |
|---|---|
| 启动服务 | `sudo systemctl start medkernel` |
| 停止服务 | `sudo systemctl stop medkernel` |
| 重启服务 | `sudo systemctl restart medkernel` |
| 查看状态 | `sudo systemctl status medkernel` |
| 查看日志 | `journalctl -u medkernel -f` |
| 健康检查 | `/zoesoft/medkernel/scripts/healthcheck.sh` |
| 安全检查 | `/zoesoft/medkernel/scripts/security-baseline.sh` |
| 升级 | `sudo /zoesoft/medkernel/scripts/upgrade.sh --to v1.3.0` |
| 回滚 | `sudo /zoesoft/medkernel/scripts/rollback.sh --to last` |
| 查看 Nginx 日志 | `tail -f /var/log/nginx/medkernel.access.log` |

#### 6.2.3 SLA 约定

| 指标 | SLO 目标 | 测量方式 |
|---|---|---|
| 系统可用性 | ≥ 99.9%（月度） | Prometheus 可用性指标 |
| API P95 延迟 | ≤ 300ms | Prometheus 延迟指标 |
| API 5xx 错误率 | ≤ 0.5% | Prometheus 错误率指标 |
| 故障响应时间 | P0: 15min / P1: 30min | 工单系统 |
| 故障恢复时间 | P0: 1h / P1: 4h | 工单系统 |
| 数据备份 | 每日全量 | 备份日志 |

#### 6.2.4 持续支持计划

| 阶段 | 时间 | 支持方式 | 响应时间 |
|---|---|---|---|
| 上线保障期 | D+1 ~ D+14 | 现场驻守 | 15 分钟 |
| 稳定运行期 | D+15 ~ D+30 | 远程 + 定期巡检 | 2 小时 |
| 常规支持期 | D+31 起 | 远程支持 | 4 小时 |

#### 6.2.5 培训考核

**理论考核**（30 分钟，闭卷）：

| 题型 | 数量 | 分值 | 考核范围 |
|---|---|---|---|
| 单选题 | 20 | 40 分 | 产品架构、部署流程、配置参数 |
| 多选题 | 10 | 30 分 | 国产化适配、安全合规、监控告警 |
| 判断题 | 10 | 10 分 | 技术架构、权限模型 |
| 简答题 | 4 | 20 分 | 故障排查、数据迁移 |

**实操考核**（120 分钟）：

| 考核项 | 分值 | 通过标准 |
|---|---|---|
| 独立完成安装部署 | 30 分 | healthcheck.sh 全部通过 |
| Nginx + SSL 配置 | 15 分 | HTTPS 访问正常 |
| 监控栈部署 | 15 分 | Grafana 看板数据正常 |
| 组织机构和权限配置 | 15 分 | RBAC 验证通过 |
| 数据迁移 | 15 分 | 数据校验一致 |
| 验收测试执行 | 10 分 | 验收报告完整 |

**培训交付考核**（30 分钟）：

模拟客户培训场景，考核培训交付能力：

| 考核项 | 分值 | 通过标准 |
|---|---|---|
| 培训材料准备 | 20 分 | 材料完整、逻辑清晰 |
| 培训表达 | 30 分 | 语言清晰、重点突出 |
| 互动答疑 | 30 分 | 回答准确、引导有效 |
| 效果评估 | 20 分 | 学员操作正确率 ≥ 80% |

**综合通过标准**：

| 考核类型 | 及格线 | 说明 |
|---|---|---|
| 理论考核 | ≥ 70 分 | 低于 70 分需补考 |
| 实操考核 | ≥ 80 分 | 每项不得低于该项 60% |
| 培训交付考核 | ≥ 70 分 | 低于 70 分需补考 |
| 综合评定 | 三项均及格 | 任一项不及格则整体不通过 |

---

## 7. 实操练习

### 练习 1：完整安装部署（含 4 种数据库）

**目标**：在实验室环境中，分别使用 4 种数据库完成 MedKernel 安装部署。

**步骤**：

1. 准备 4 台虚拟机，分别安装 Oracle / DM / PostgreSQL / KingbaseES
2. 在每台机器上执行完整安装流程
3. 使用对应的 Profile 模板配置 `medkernel.env`
4. 执行 `check-env.sh` → `install-offline.sh --init-db` → `healthcheck.sh`
5. 记录每种数据库的 DDL 执行方式和注意事项

**验收标准**：4 种数据库环境均 healthcheck.sh 通过。

---

### 练习 2：Nginx + SSL 配置

**目标**：配置 HTTP 和 HTTPS 两种反向代理。

**步骤**：

1. 部署 HTTP 版 Nginx 配置
2. 生成自签 SSL 证书
3. 部署 HTTPS 版 Nginx 配置
4. 验证 HTTP → HTTPS 重定向
5. 验证安全头（HSTS / X-Content-Type-Options 等）

**验收标准**：

- HTTP 自动重定向到 HTTPS
- HTTPS 访问正常
- `curl -I https://localhost` 显示安全头

---

### 练习 3：监控栈部署和看板配置

**目标**：部署 Prometheus + Grafana + AlertManager 监控栈。

**步骤**：

1. 使用 `docker-compose.monitoring.yml` 启动监控栈
2. 验证 Prometheus 采集到 MedKernel 指标
3. 在 Grafana 中导入预置看板（`medkernel-mvp.json` / `medkernel-slo.json`）
4. 配置 AlertManager 通知渠道（邮件或 Webhook）
5. 模拟告警触发，验证通知到达

**验收标准**：

- Prometheus targets 页面显示 UP
- Grafana 看板显示实时数据
- 告警通知正常到达

---

### 练习 4：组织机构和权限配置

**目标**：配置一个模拟医院的完整组织架构和权限体系。

**步骤**：

1. 创建医院 → 院区 → 科室 → 病区层级
2. 创建 5 个用户，分配不同角色
3. 配置菜单权限：不同角色看到不同菜单
4. 配置数据权限：科室主任只能看本科室数据
5. 验证权限生效

**验收标准**：

- 不同角色登录看到不同菜单
- 数据权限过滤生效
- 审计日志记录权限变更

---

### 练习 5：数据迁移实操

**目标**：完成模拟患者数据迁移和知识库初始化。

**步骤**：

1. 准备模拟患者数据（100 条）
2. 执行患者数据导入
3. 校验导入数据条目数
4. 导入知识包
5. 导入规则配置包
6. 导入临床路径模板
7. 执行 Dry Run 验证规则效果

**验收标准**：

- 患者数据条目数一致
- 知识包导入成功
- 规则 Dry Run 结果正确

---

### 练习 6：验收测试执行

**目标**：编写并执行验收测试计划。

**步骤**：

1. 编写验收测试计划
2. 执行功能验收测试（覆盖 4 大模块）
3. 执行性能基线测试
4. 运行安全基线检查
5. 编写验收报告

**验收标准**：

- 验收测试计划完整
- 功能验收全部通过
- 性能指标满足 SLO
- 安全基线检查通过

---

### 练习 7：客户培训模拟

**目标**：模拟对 IT 人员和临床用户的培训交付。

**步骤**：

1. 准备 IT 人员培训材料
2. 准备临床用户培训材料
3. 模拟 IT 人员培训（30 分钟）
4. 模拟临床用户培训（15 分钟）
5. 收集反馈并改进

**验收标准**：

- 培训材料逻辑清晰
- 讲解流畅，重点突出
- 学员操作正确率 ≥ 80%

---

### 练习 8：上线日全流程模拟

**目标**：模拟上线日的完整操作流程。

**步骤**：

1. 执行上线前检查清单
2. 模拟上线日操作流程
3. 模拟常见上线问题（服务启动失败、Nginx 502、SSO 登录失败）
4. 执行回滚操作
5. 重新上线并确认稳定

**验收标准**：

- 上线前检查清单全部通过
- 问题排查和解决在时限内完成
- 回滚操作 5 分钟内完成

---

## 8. 考核标准

### 8.1 理论考核

| 知识域 | 权重 | 核心考点 |
|---|---|---|
| 产品架构 | 25% | 三产品分层、四大模块、多租户架构 |
| 部署流程 | 30% | install-offline.sh 7 步、4 种数据库 DDL、Profile 选择 |
| 配置参数 | 20% | medkernel.env 参数、JVM 调优、Nginx 配置 |
| 安全合规 | 15% | 等保 2.0 三级、SM4 加密、审计日志 |
| 监控运维 | 10% | Prometheus 告警规则、SLO 目标、应急响应 |

### 8.2 实操考核

独立完成从安装到上线的全流程，包括：

1. 环境检查和 Profile 配置
2. 安装部署（含 DDL 执行）
3. Nginx + SSL 配置
4. 监控栈部署
5. 组织机构和权限配置
6. 数据迁移
7. 健康检查和安全基线验证
8. 验收报告编写

**时间限制**：120 分钟
**通过标准**：总分 ≥ 80 分，每项不低于该项 60%

### 8.3 培训交付考核

模拟客户培训场景：

1. 准备培训材料（IT 人员 + 临床用户）
2. 进行 15 分钟培训演示
3. 回答模拟客户提问

**通过标准**：培训表达清晰、内容准确、互动有效

---

## 9. 实施工具箱

### 9.1 必备工具清单

| 类别 | 工具 | 用途 |
|---|---|---|
| 远程连接 | SSH 客户端（MobaXterm / SecureCRT） | 连接服务器 |
| 文件传输 | SCP / WinSCP | 上传发布包 |
| 数据库客户端 | DBeaver / Navicat / 各数据库自带客户端 | 执行 DDL 和数据查询 |
| 压力测试 | JMeter / wrk | 性能基线测试 |
| 抓包分析 | tcpdump / Wireshark | 网络问题排查 |
| 文本编辑 | vim / nano | 编辑配置文件 |
| 浏览器 | Chrome / Firefox（含 DevTools） | 前端验证和 API 调试 |

### 9.2 常用命令速查

#### 系统管理

```bash
# 服务管理
sudo systemctl start medkernel        # 启动
sudo systemctl stop medkernel         # 停止
sudo systemctl restart medkernel      # 重启
sudo systemctl status medkernel       # 查看状态
sudo systemctl enable medkernel       # 开机自启

# 日志查看
journalctl -u medkernel -f            # 实时日志
journalctl -u medkernel -n 100        # 最近 100 条
journalctl -u medkernel --since "1 hour ago"  # 最近 1 小时

# 应用日志
tail -f /zoesoft/medkernel/logs/stdout.log    # 标准输出
tail -f /zoesoft/medkernel/logs/stderr.log    # 错误输出
```

#### 部署脚本

```bash
# 环境检查
sudo /zoesoft/medkernel/scripts/check-env.sh
sudo /zoesoft/medkernel/scripts/check-env.sh --profile kylin-aarch64-dm

# 安装
sudo /zoesoft/medkernel/scripts/install-offline.sh --init-db
sudo /zoesoft/medkernel/scripts/install-offline.sh --migrate-db
sudo /zoesoft/medkernel/scripts/install-offline.sh --skip-init-db

# 升级
sudo /zoesoft/medkernel/scripts/upgrade.sh --to v1.3.0
sudo /zoesoft/medkernel/scripts/upgrade.sh --to v1.3.0 --migrate-db
sudo /zoesoft/medkernel/scripts/upgrade.sh --backup-only

# 回滚
sudo /zoesoft/medkernel/scripts/rollback.sh --to last
sudo /zoesoft/medkernel/scripts/rollback.sh --to 20260523_083000

# 健康检查
/zoesoft/medkernel/scripts/healthcheck.sh
/zoesoft/medkernel/scripts/healthcheck.sh --url http://10.0.0.50:18080/medkernel

# 安全基线
/zoesoft/medkernel/scripts/security-baseline.sh
/zoesoft/medkernel/scripts/security-baseline.sh --strict
```

#### 数据库操作

```bash
# PostgreSQL
PGPASSWORD=xxx psql -h 10.0.0.30 -U medkernel -d medkernel -c 'SELECT count(*) FROM mpi_patient;'

# Oracle
sqlplus MEDKERNEL/password@//10.0.0.10:1521/ORCL @query.sql

# 达梦 DM
disql MEDKERNEL/password@10.0.0.20:5236 -e "SELECT count(*) FROM mpi_patient;"

# KingbaseES
PGPASSWORD=xxx ksql -h 10.0.0.40 -U medkernel -d medkernel -p 54321 -c 'SELECT count(*) FROM mpi_patient;'
```

#### Nginx

```bash
# 配置测试
sudo nginx -t

# 重载配置
sudo systemctl reload nginx

# 查看日志
tail -f /var/log/nginx/medkernel.access.log
tail -f /var/log/nginx/medkernel.error.log
```

#### Docker（监控栈）

```bash
# 启动监控栈
cd /zoesoft/medkernel && docker compose -f docker-compose.monitoring.yml up -d

# 查看状态
docker compose -f docker-compose.monitoring.yml ps

# 查看日志
docker compose -f docker-compose.monitoring.yml logs -f prometheus

# 停止监控栈
docker compose -f docker-compose.monitoring.yml down
```

#### 网络排查

```bash
# 端口检查
ss -ltnp | grep 18080
netstat -ltnp | grep 18080

# 连通性测试
curl -fsS http://localhost:18080/medkernel/api/health
curl -fsS http://127.0.0.1:18081/actuator/health

# 防火墙
sudo firewall-cmd --list-ports
sudo firewall-cmd --add-port=80/tcp --permanent
sudo firewall-cmd --reload

# SELinux
getenforce
sudo setenforce 0    # 临时 Permissive
```

### 9.3 配置模板

#### medkernel.env 最小配置模板

```bash
# ==== 路径 ====
MK_HOME=/zoesoft/medkernel
MK_USER=medkernel
MK_BACKUP_DIR=/zoesoft/medkernel.bak

# ==== JDK ====
JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk

# ==== HTTP ====
MEDKERNEL_HTTP_PORT=18080
MEDKERNEL_HTTP_CONTEXT=/medkernel

# ==== 数据库（按实际选择）====
MEDKERNEL_DB_ENABLED=true
MEDKERNEL_DB_DIALECT=postgres          # oracle / dm / postgres / kingbase
MEDKERNEL_DB_HOST=10.0.0.30
MEDKERNEL_DB_PORT=5432
MEDKERNEL_DB_NAME=medkernel
MEDKERNEL_DB_URL=jdbc:postgresql://10.0.0.30:5432/medkernel?reWriteBatchedInserts=true&prepareThreshold=0
MEDKERNEL_DB_USERNAME=medkernel
MEDKERNEL_DB_PASSWORD=__REPLACE_ME__

# ==== 扩展功能 ====
MEDKERNEL_GRAPH_ENABLED=false
MEDKERNEL_DIFY_ENABLED=false

# ==== JVM ====
JAVA_OPTS="-server -Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
```

### 9.4 验收检查清单

| # | 检查项 | 检查方法 | 预期结果 | 实际结果 | 通过 |
|---|---|---|---|---|---|
| 1 | 环境检查 | `check-env.sh` | 无 FAIL 项 | | |
| 2 | 服务运行 | `systemctl status medkernel` | active (running) | | |
| 3 | 健康检查 | `healthcheck.sh` | 全部通过 | | |
| 4 | 安全基线 | `security-baseline.sh` | 无 FAIL 项 | | |
| 5 | HTTP 访问 | 浏览器访问 | 页面正常 | | |
| 6 | HTTPS 访问 | 浏览器访问 | 页面正常 | | |
| 7 | 用户登录 | 账号密码登录 | 登录成功 | | |
| 8 | SSO 登录 | SSO 方式登录 | 登录成功 | | |
| 9 | 组织机构 | 创建/查看组织 | 数据正确 | | |
| 10 | 权限控制 | 不同角色验证 | 权限生效 | | |
| 11 | 配置包管理 | 创建/发布/回滚 | 操作成功 | | |
| 12 | 路径管理 | 创建/入径/流转 | 操作成功 | | |
| 13 | 规则管理 | 创建/试运行/发布 | 操作成功 | | |
| 14 | CDSS 提醒 | 触发医嘱提醒 | 提醒正常 | | |
| 15 | 质控看板 | 查看质控数据 | 数据正确 | | |
| 16 | 审计日志 | 查看操作记录 | 记录完整 | | |
| 17 | Prometheus | 访问 9090 | targets UP | | |
| 18 | Grafana | 访问 3000 | 看板正常 | | |
| 19 | 告警通知 | 触发测试告警 | 通知到达 | | |
| 20 | 备份恢复 | 执行备份和恢复 | 恢复成功 | | |

### 9.5 常见问题速查

| # | 问题 | 现象 | 原因 | 解决方案 |
|---|---|---|---|---|
| 1 | 安装脚本报"需要 root 权限" | `die "需要 root 权限（sudo）"` | 未使用 sudo 执行 | `sudo ./scripts/install-offline.sh --init-db` |
| 2 | JDK 版本不匹配 | `JDK 非 1.8` | 安装了 JDK 11+ | 安装 JDK 1.8（毕昇/Temurin/OpenJDK） |
| 3 | 端口被占用 | `后端端口 18080 已被占用` | 其他进程占用 | `ss -ltnp | grep 18080` 查找并释放 |
| 4 | 数据库连接失败 | `Oracle/PG 连通失败` | 凭据或网络问题 | 检查 medkernel.env 中 DB 配置 |
| 5 | SELinux 阻止 | 服务启动后无法读写文件 | SELinux Enforcing | `install-offline.sh` 自动配置 fcontext |
| 6 | locale 非 UTF-8 | `locale 非 UTF-8` | 系统语言设置 | `export LANG=zh_CN.UTF-8` |
| 7 | 时区不对 | `时区非 Asia/Shanghai` | 系统时区设置 | `timedatectl set-timezone Asia/Shanghai` |
| 8 | Nginx 502 Bad Gateway | 页面显示 502 | 后端未启动或端口不对 | 检查 medkernel 服务状态 |
| 9 | Actuator 不可达 | 健康检查 Actuator 失败 | 18081 端口未绑定 | 确认 `management.server.port=18081` |
| 10 | Prometheus 无数据 | targets 显示 DOWN | Actuator 端口不可达 | 检查 18081 端口和 Docker 网络配置 |
| 11 | Grafana 看板空白 | 无数据展示 | Prometheus 数据源未配置 | 检查 Grafana 数据源配置 |
| 12 | 达梦 DDL 执行失败 | disql 报语法错误 | 达梦 SQL 方言差异 | 使用 `db/dm/` 目录下的专用 DDL |
| 13 | 麒麟 V10 SSL 握手失败 | HTTPS 连接失败 | OpenSSL 1.0.2 兼容性 | JVM 参数加 `-Djdk.tls.client.protocols=TLSv1.2` |
| 14 | KingbaseES 迁移失败 | Flyway 报错 | 迁移目录映射 | KingbaseES 使用 `postgres/` 迁移目录 |
| 15 | 升级后启动失败 | 新版本无法启动 | 配置不兼容 | 执行 `rollback.sh --to last` 回滚 |

---

> **文档版本**：1.0 | **最后更新**：2026-05-23 | **维护团队**：MedKernel 实施团队
