# MedKernel 医院信息科 IT 人员培训材料

> **版本**：1.0 | **状态**：GA | **日期**：2026-05-23
>
> 本培训材料面向医院信息科技术人员，涵盖 MedKernel 系统的安装部署、日常运维、监控告警、升级回滚与故障排查。

---

## 1. 培训概述

### 1.1 培训目标

通过 3 天（24 学时）的集中培训，使参训人员能够：

- 独立完成 MedKernel 系统的安装部署和初始化配置
- 掌握日常运维操作：服务管理、日志分析、配置变更
- 熟练使用 Prometheus + Grafana 监控系统，配置告警规则
- 独立完成版本升级和回滚操作
- 能够排查和处置 8 类常见故障

### 1.2 培训安排

| 项目 | 内容 |
|------|------|
| 培训时长 | 3 天（24 学时），每天 8 学时 |
| 培训对象 | 医院信息科技术人员 |
| 培训方式 | 理论讲授（40%）+ 实操练习（60%） |
| 考核方式 | 理论考核（30%）+ 实操考核（70%） |

### 1.3 前置要求

| 技能 | 要求程度 | 说明 |
|------|----------|------|
| Linux 基础 | 熟练 | 命令行操作、文件管理、用户管理 |
| 数据库基础 | 了解 | SQL 基本操作、数据库连接 |
| 网络基础 | 了解 | TCP/IP、端口、防火墙 |
| Nginx 基础 | 了解 | 反向代理、配置文件 |
| systemd 基础 | 了解 | 服务管理命令 |

---

## 2. 第一天：系统架构与安装部署（8 学时）

### 2.1 上午：系统架构概述（4 学时）

#### 2.1.1 MedKernel 系统架构概述

MedKernel 采用经典的三层架构部署：

```
┌─────────────┐    HTTPS     ┌──────────────┐    HTTP     ┌──────────────────┐
│   浏览器     │ ──────────→ │   Nginx      │ ──────────→ │  MedKernel MVP   │
│  (前端SPA)   │             │  (TLS 终结)   │             │  (Spring Boot)   │
└─────────────┘             └──────────────┘             └──────────────────┘
                                                                  │
                                                         ┌────────┼────────┐
                                                         │        │        │
                                                    ┌────┴──┐ ┌──┴───┐ ┌┴──────┐
                                                    │Oracle/ │ │Neo4j │ │Dify/  │
                                                    │PG/DM  │ │(可选) │ │LLM(可选)│
                                                    └───────┘ └──────┘ └───────┘
```

**核心组件说明**：

| 组件 | 作用 | 说明 |
|------|------|------|
| Nginx | 反向代理 + TLS 终结 | 前端静态资源托管、API 反向代理、限速、安全头 |
| MedKernel MVP | 业务后端 | Spring Boot 应用，提供 REST API |
| 数据库 | 数据持久化 | 支持 Oracle / PostgreSQL / 达梦 DM / KingbaseES / H2 |
| Neo4j（可选） | 知识图谱 | 图谱查询和知识推理 |
| Dify/LLM（可选） | AI 模型 | AI 辅助诊断、知识推荐 |

#### 2.1.2 端口规划

| 服务 | 端口 | 用途 | 访问范围 |
|------|------|------|----------|
| Nginx | 443 (HTTPS) / 80 (HTTP) | 前端 + API 反向代理 | 全院可访问 |
| MedKernel 业务 | 18080 | REST API | 仅 Nginx 可访问 |
| MedKernel 管理 | 18081 | Actuator/健康检查/Prometheus | 仅 127.0.0.1 |
| Prometheus | 9090 | 指标采集 | 仅运维网段 |
| Grafana | 3000 | 监控看板 | 仅运维网段 |
| Alertmanager | 9093 | 告警通知 | 仅运维网段 |

> **安全提示**：管理端口 18081 仅绑定 `127.0.0.1`，不对外暴露。生产环境必须通过防火墙限制 18080 端口仅 Nginx 可访问。

#### 2.1.3 目录结构（$MK_HOME）

`$MK_HOME` 默认为 `/zoesoft/medkernel`，目录结构如下：

```
/zoesoft/medkernel/                 # $MK_HOME
├── lib/
│   └── medkernel.jar               # Spring Boot 可执行 JAR
├── frontend/
│   └── dist/                       # 前端 Vite 构建产物
├── conf/
│   ├── medkernel.env               # 环境变量（数据库凭据、端口、JVM 参数）
│   ├── application.yml             # Spring Boot 主配置
│   └── application-local.yml       # 本地覆盖配置（可选）
├── db/
│   ├── oracle/                     # Oracle DDL 脚本
│   ├── postgres/                   # PostgreSQL DDL 脚本
│   ├── dm/                         # 达梦 DDL 脚本
│   └── migration/                  # Flyway 迁移脚本
├── scripts/
│   ├── install-offline.sh          # 离线安装脚本
│   ├── upgrade.sh                  # 升级脚本
│   ├── rollback.sh                 # 回滚脚本
│   ├── healthcheck.sh              # 健康检查脚本
│   ├── check-env.sh                # 环境检查脚本
│   ├── security-baseline.sh        # 安全基线检查脚本
│   └── lib/
│       └── common.sh               # 公共函数库
├── systemd/
│   └── medkernel.service           # systemd 服务文件
├── nginx/
│   ├── medkernel.conf              # Nginx HTTP 配置
│   └── medkernel-tls.conf          # Nginx HTTPS 配置
├── logs/
│   ├── stdout.log                  # 标准输出日志
│   ├── stderr.log                  # 标准错误日志
│   └── heap.hprof                  # OOM 时的堆转储
├── profiles/
│   ├── pg-x86_64.env               # PostgreSQL 部署 profile
│   ├── centos7-x86_64-oracle.env   # Oracle 部署 profile
│   ├── kingbase-x86_64.env         # KingbaseES 部署 profile
│   ├── kylin-aarch64-dm.env        # 银河麒麟 + 达梦 profile
│   └── uos-aarch64-dm.env          # UOS + 达梦 profile
├── monitoring/                     # 监控配置
│   ├── prometheus/
│   ├── grafana/
│   └── alertmanager/
├── manifest.json                   # 版本清单
└── CHANGELOG.md                    # 变更日志
```

#### 2.1.4 支持的操作系统和数据库

**操作系统**：

| 操作系统 | 架构 | 说明 |
|----------|------|------|
| CentOS 7 | x86_64 | 传统医院常见 |
| Rocky Linux 8/9 | x86_64 | CentOS 替代 |
| Ubuntu 22.04 LTS | x86_64 | 通用 Linux |
| 银河麒麟 V10 | aarch64 | 国产化 ARM |
| UOS V20 | aarch64 | 国产化 ARM |
| OpenEuler | x86_64 / aarch64 | 国产化 |

**数据库**：

| 数据库 | 版本 | 说明 |
|--------|------|------|
| Oracle | 12c+ | 大型医院常用 |
| PostgreSQL | 14+ | 开源推荐 |
| 达梦 DM | V8 | 国产化 |
| KingbaseES | V8R6 | 国产化 |
| H2 | 内置 | 仅开发/测试 |

#### 2.1.5 环境准备

**JDK 安装**：

```bash
# CentOS / Rocky
sudo yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel

# Ubuntu / Debian
sudo apt install -y openjdk-8-jdk

# 国产化 ARM（银河麒麟 / UOS）— 毕昇 JDK
sudo tar -xzf bisheng-jdk1.8.0_402-linux-aarch64.tar.gz -C /opt/
export JAVA_HOME=/opt/bisheng-jdk1.8.0_402
```

验证 JDK：

```bash
java -version
# 期望输出：openjdk version "1.8.0_xxx" 或 bisheng-jdk
```

**数据库准备**：

```bash
# PostgreSQL 示例
sudo yum install -y postgresql16-server
sudo postgresql-16-setup initdb
sudo systemctl enable postgresql-16
sudo systemctl start postgresql-16

# 创建数据库和用户
sudo -u postgres psql -c "CREATE USER medkernel WITH PASSWORD 'xxx';"
sudo -u postgres psql -c "CREATE DATABASE medkernel OWNER medkernel;"
```

**Nginx 安装**：

```bash
# CentOS
sudo yum install -y nginx
sudo systemctl enable nginx

# Ubuntu
sudo apt install -y nginx
```

---

### 2.2 下午：安装部署实操（4 学时）

#### 2.2.1 安装部署实操：使用 install-offline.sh

**安装脚本用法**：

```bash
sudo ./install-offline.sh [--init-db | --migrate-db | --skip-init-db]
```

| 参数 | 说明 |
|------|------|
| `--init-db` | 首次安装，执行 DDL 初始化 |
| `--migrate-db` | 升级安装，执行数据库迁移 |
| `--skip-init-db` | 跳过数据库操作（已手动初始化时使用） |

**完整安装流程**：

```bash
# 1. 选择部署 profile
#    将对应的 profile 文件复制为 medkernel.env
cp profiles/pg-x86_64.env conf/medkernel.env

# 2. 编辑环境变量（必须修改数据库密码等）
vi conf/medkernel.env
# 必须修改的项：
#   MEDKERNEL_DB_PASSWORD=__REPLACE_ME__  →  实际数据库密码
#   MEDKERNEL_DB_HOST=10.0.0.30           →  实际数据库地址
#   MEDKERNEL_JWT_SECRET=                 →  JWT 签名密钥（生产必须设置）

# 3. 执行环境检查
sudo ./scripts/check-env.sh

# 4. 执行安装（首次安装，含数据库初始化）
sudo ./scripts/install-offline.sh --init-db

# 5. 安装脚本自动执行以下步骤：
#    步骤 1：检查环境
#    步骤 2：创建运行用户（medkernel）与目录
#    步骤 3：处理数据库 DDL（按 dialect 自动选择）
#    步骤 4：注册 systemd 服务
#    步骤 5：启动应用
#    步骤 6：健康检查
#    步骤 7：完成

# 6. 配置 Nginx
sudo cp nginx/medkernel-tls.conf /etc/nginx/conf.d/
sudo nginx -t && sudo systemctl reload nginx
```

**安装后验证**：

```bash
# 检查服务状态
systemctl status medkernel

# 执行健康检查
./scripts/healthcheck.sh

# 检查版本
cat /zoesoft/medkernel/manifest.json | grep version
```

#### 2.2.2 配置文件详解

##### medkernel.env — 环境变量

`$MK_HOME/conf/medkernel.env` 是核心环境变量文件，包含数据库凭据、端口、JVM 参数等。文件权限必须为 `600`。

```bash
# 路径常量
MK_HOME=/zoesoft/medkernel
MK_USER=medkernel
MK_BACKUP_DIR=/zoesoft/medkernel.bak

# ---- JDK ----
JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk

# ---- HTTP ----
MEDKERNEL_HTTP_PORT=18080
MEDKERNEL_HTTP_CONTEXT=/medkernel

# ---- 数据库 ----
MEDKERNEL_DB_ENABLED=true
MEDKERNEL_DB_DIALECT=postgres          # oracle / postgres / dm / kingbase
MEDKERNEL_DB_HOST=10.0.0.30
MEDKERNEL_DB_PORT=5432
MEDKERNEL_DB_NAME=medkernel
MEDKERNEL_DB_URL=jdbc:postgresql://10.0.0.30:5432/medkernel?reWriteBatchedInserts=true&prepareThreshold=0
MEDKERNEL_DB_USERNAME=medkernel
MEDKERNEL_DB_PASSWORD=__REPLACE_ME__   # ⚠️ 必须修改

# ---- 图谱 / Dify ----
MEDKERNEL_GRAPH_ENABLED=false
MEDKERNEL_DIFY_ENABLED=false

# ---- JVM ----
JAVA_OPTS="-server -Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
```

**关键安全参数**：

| 参数 | 说明 | 安全要求 |
|------|------|----------|
| `MEDKERNEL_DB_PASSWORD` | 数据库密码 | 必须强密码，禁止默认值 |
| `MEDKERNEL_JWT_SECRET` | JWT 签名密钥 | 生产必须设置，不低于 32 字符 |
| `MEDKERNEL_SM4_MASTER_KEY` | SM4 字段加密主密钥 | 生产必须通过环境变量覆盖 |
| `MEDKERNEL_FIELD_ENC_ENABLED` | 字段加密开关 | 生产必须为 true |

##### application.yml — Spring Boot 配置

`$MK_HOME/conf/application.yml` 是 Spring Boot 主配置文件，关键配置项：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 18080 | 业务端口 |
| `management.server.port` | 18081 | 管理端口（Actuator） |
| `management.server.address` | 127.0.0.1 | 管理端口绑定地址 |
| `medkernel.database.dialect` | oracle | 数据库方言 |
| `medkernel.database.hikari.maximum-pool-size` | 20 | 连接池最大连接数 |
| `medkernel.database.hikari.minimum-idle` | 2 | 连接池最小空闲连接 |
| `medkernel.database.hikari.connection-timeout-ms` | 3000 | 获取连接超时 |
| `medkernel.database.hikari.leak-detection-threshold-ms` | 2000 | 连接泄漏检测阈值 |
| `medkernel.security.jwt-secret` | 空 | JWT 密钥（必须设置） |
| `medkernel.security.lock-threshold` | 5 | 登录失败锁定次数 |
| `medkernel.security.lock-duration-minutes` | 30 | 锁定时长 |
| `medkernel.flyway.enabled` | false | Flyway 迁移开关 |
| `medkernel.graph.enabled` | false | 图谱开关 |
| `medkernel.dify.enabled` | false | Dify/LLM 开关 |

#### 2.2.3 数据库初始化

**DDL 执行（按数据库类型）**：

```bash
# PostgreSQL（psql 可用时自动执行）
PGPASSWORD="${MEDKERNEL_DB_PASSWORD}" psql \
  -h "${MEDKERNEL_DB_HOST}" -p "${MEDKERNEL_DB_PORT}" \
  -U "${MEDKERNEL_DB_USERNAME}" -d "${MEDKERNEL_DB_NAME}" \
  -v ON_ERROR_STOP=1 \
  -f "$MK_HOME/db/postgres/medkernel_core_ddl_with_comments.sql"

# Oracle（需 DBA 手动执行）
sqlplus MEDKERNEL/password@//192.168.4.25:1521/ORCL @$MK_HOME/db/oracle/medkernel_core_ddl_with_comments.sql

# 达梦 DM（需信息科手动执行）
disql MEDKERNEL/password@10.0.0.20:5236 -e "START '$MK_HOME/db/dm/medkernel_core_ddl_with_comments.sql';"
```

**Flyway 迁移**：

启用 Flyway 后，应用启动时自动执行迁移：

```yaml
# application.yml 中启用 Flyway
medkernel:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
```

迁移脚本按 dialect 自动选取：
- H2 → `db/migration/{common,h2}`
- Oracle/DM → `db/migration/{common,oracle}`
- PostgreSQL/KingbaseES → `db/migration/{common,postgres}`

查看迁移历史：

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

#### 2.2.4 Nginx 配置

**HTTP 配置**（`medkernel.conf`）：

关键配置项：

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
    # 前端静态资源
    root /zoesoft/medkernel/frontend/dist;

    # SPA fallback
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 反代
    location /medkernel/ {
        limit_req zone=medkernel_api burst=200 nodelay;
        proxy_pass http://medkernel_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Trace-Id $http_x_trace_id;
        # 透传组织上下文 Header
        proxy_set_header X-Tenant-Id $http_x_tenant_id;
        proxy_set_header X-Hospital-Code $http_x_hospital_code;
    }

    # 健康检查端点
    location = /healthz {
        access_log off;
        proxy_pass http://medkernel_backend/medkernel/api/health;
    }
}
```

**HTTPS 配置**（`medkernel-tls.conf`）：

```bash
# 生成自签证书（测试用）
openssl req -x509 -nodes -days 3650 -newkey rsa:4096 \
  -keyout /etc/nginx/ssl/medkernel.key \
  -out    /etc/nginx/ssl/medkernel.crt \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=Hospital/OU=IT/CN=medkernel.hospital.local"

# 部署 HTTPS 配置
sudo cp nginx/medkernel-tls.conf /etc/nginx/conf.d/
sudo nginx -t && sudo systemctl reload nginx
```

**安全头配置**（已内置）：

```nginx
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "SAMEORIGIN" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
```

#### 2.2.5 健康检查验证

```bash
# 使用健康检查脚本
./scripts/healthcheck.sh

# 脚本检查以下端点：
# 1. /api/health              — 业务健康
# 2. /api/system/providers    — Provider 状态
# 3. /api/system/org-context  — 组织上下文
# 4. actuator/health          — Spring Boot Actuator
# 5. Prometheus 可达性        — SLO 状态

# 手动检查
curl -s http://localhost:18080/medkernel/api/health | jq .
curl -s http://localhost:18081/medkernel/actuator/health | jq .
curl -s http://localhost:18081/medkernel/actuator/health/readiness
curl -s http://localhost:18081/medkernel/actuator/health/liveness

# 外部健康检查（通过 Nginx）
curl -s http://<服务器IP>/healthz | jq .
```

---

## 3. 第二天：日常运维与监控（8 学时）

### 3.1 上午：日常运维（4 学时）

#### 3.1.1 服务管理

```bash
# 启动服务
sudo systemctl start medkernel

# 停止服务
sudo systemctl stop medkernel

# 重启服务
sudo systemctl restart medkernel

# 查看服务状态
sudo systemctl status medkernel

# 查看服务是否活跃
systemctl is-active medkernel

# 开机自启
sudo systemctl enable medkernel

# 取消开机自启
sudo systemctl disable medkernel

# 重新加载 systemd 配置（修改 service 文件后必须执行）
sudo systemctl daemon-reload
```

**systemd 服务文件关键参数**（`/etc/systemd/system/medkernel.service`）：

| 参数 | 值 | 说明 |
|------|-----|------|
| `User` | medkernel | 运行用户 |
| `WorkingDirectory` | /zoesoft/medkernel | 工作目录 |
| `EnvironmentFile` | /zoesoft/medkernel/conf/medkernel.env | 环境变量文件 |
| `ExecStart` | java $JAVA_OPTS -jar medkernel.jar | 启动命令 |
| `TimeoutStopSec` | 30 | 优雅停止超时 |
| `Restart` | on-failure | 失败自动重启 |
| `RestartSec` | 10 | 重启间隔 |
| `MemoryHigh` | 6G | 内存软限制 |
| `MemoryMax` | 8G | 内存硬限制 |
| `LimitNOFILE` | 65535 | 文件描述符上限 |

#### 3.1.2 日志管理

**查看日志**：

```bash
# 查看 systemd 日志（最近 100 行）
journalctl -u medkernel -n 100

# 实时跟踪日志
journalctl -u medkernel -f

# 查看指定时间段日志
journalctl -u medkernel --since "2026-05-23 08:00:00" --until "2026-05-23 18:00:00"

# 查看应用标准输出日志
tail -f /zoesoft/medkernel/logs/stdout.log

# 查看应用标准错误日志
tail -f /zoesoft/medkernel/logs/stderr.log

# 查看 Nginx 访问日志
tail -f /var/log/nginx/medkernel.access.log

# 查看 Nginx 错误日志
tail -f /var/log/nginx/medkernel.error.log
```

**日志轮转**：

创建 `/etc/logrotate.d/medkernel`：

```bash
/zoesoft/medkernel/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    copytruncate
    dateext
    dateformat -%Y%m%d
}
```

**日志级别调整**：

```bash
# 临时调整（重启后失效）
# 修改 medkernel.env 中的 JAVA_OPTS
JAVA_OPTS="-server -Xms2g -Xmx4g -XX:+UseG1GC \
  -Dlogging.level.com.medkernel=DEBUG \
  -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

# 永久调整：修改 application.yml
logging:
  level:
    com.medkernel: DEBUG    # 开发调试
    # com.medkernel: INFO   # 生产默认
    # com.medkernel: WARN   # 减少日志量
```

**日志保留策略**：

| 日志文件 | 位置 | 保留期 | 说明 |
|----------|------|--------|------|
| 应用日志 | /zoesoft/medkernel/logs/ | 30 天 | Spring Boot 日志 |
| Nginx 访问日志 | /var/log/nginx/medkernel.access.log | 90 天 | HTTP 请求日志 |
| Nginx 错误日志 | /var/log/nginx/medkernel.error.log | 90 天 | Nginx 错误 |
| 审计日志 | 数据库 sec_auth_audit_log | 180 天 | 等保合规 |
| systemd 日志 | journalctl | 30 天 | 系统日志 |

#### 3.1.3 配置管理

**修改配置流程**：

```bash
# 1. 备份当前配置
cp /zoesoft/medkernel/conf/medkernel.env /zoesoft/medkernel/conf/medkernel.env.bak.$(date +%Y%m%d)

# 2. 修改配置
vi /zoesoft/medkernel/conf/medkernel.env

# 3. 重启服务使配置生效
sudo systemctl restart medkernel

# 4. 验证配置生效
systemctl status medkernel
./scripts/healthcheck.sh
```

**常见配置修改场景**：

| 场景 | 修改项 | 操作 |
|------|--------|------|
| 调整 JVM 内存 | `JAVA_OPTS` 中 `-Xms` `-Xmx` | 修改后重启 |
| 修改数据库连接 | `MEDKERNEL_DB_URL` 等 | 修改后重启 |
| 开启 Flyway 迁移 | `MEDKERNEL_FLYWAY_ENABLED=true` | 修改后重启 |
| 调整连接池大小 | `MEDKERNEL_DB_POOL_MAX` | 修改后重启 |
| 开启图谱功能 | `MEDKERNEL_GRAPH_ENABLED=true` | 修改后重启 |
| 开启 AI 功能 | `MEDKERNEL_DIFY_ENABLED=true` | 修改后重启 |

> **注意**：所有配置修改都需要重启服务才能生效。生产环境建议在维护窗口操作。

#### 3.1.4 数据库运维

**连接池监控**：

```bash
# 通过 Actuator 查看 HikariCP 连接池状态
curl -s http://localhost:18081/medkernel/actuator/metrics/hikaricp.connections.active | jq .
curl -s http://localhost:18081/medkernel/actuator/metrics/hikaricp.connections.idle | jq .
curl -s http://localhost:18081/medkernel/actuator/metrics/hikaricp.connections.max | jq .
curl -s http://localhost:18081/medkernel/actuator/metrics/hikaricp.connections.pending | jq .
```

**慢查询排查**：

```bash
# 通过 Prometheus 查询慢查询
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.99,sum(rate(medkernel_db_query_duration_seconds_bucket[5m]))by(le))' | jq .

# 通过 Actuator 查询
curl -s http://localhost:18081/medkernel/actuator/prometheus | grep medkernel_db_query_duration
```

**HikariCP 连接池关键参数**：

| 参数 | 默认值 | 说明 | 调优建议 |
|------|--------|------|----------|
| `maximum-pool-size` | 20 | 最大连接数 | 8 核机器建议 20-30 |
| `minimum-idle` | 2 | 最小空闲连接 | 保持 2-5 |
| `connection-timeout-ms` | 3000 | 获取连接超时 | 不建议调大 |
| `idle-timeout-ms` | 600000 | 空闲连接超时 | 10 分钟 |
| `max-lifetime-ms` | 1800000 | 连接最大生命周期 | 30 分钟，避开 DBA 端断连 |
| `leak-detection-threshold-ms` | 2000 | 泄漏检测阈值 | 生产建议 2000-5000 |

#### 3.1.5 安全运维

**密钥管理**：

```bash
# 触发密钥轮换
curl -X POST http://localhost:18080/medkernel/api/security/admin/keys/rotate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"key_alias": "field-encryption", "rotated_by": "admin"}'

# 密钥生命周期：ACTIVE → GRACE → RETIRED → REVOKED
```

**审计日志**：

```bash
# 验证审计链完整性
curl -s http://localhost:18080/medkernel/api/security/admin/audit-chain/verify \
  -H "Authorization: Bearer $TOKEN" | jq .

# 导出审计日志
curl -s "http://localhost:18080/medkernel/api/admin/audit-logs?limit=10000" \
  -H "Authorization: Bearer $TOKEN" > /zoesoft/medkernel/backup/audit_$(date +%Y%m%d).json
```

**安全基线检查**：

```bash
# 执行等保 2.0 三级安全基线检查
sudo ./scripts/security-baseline.sh

# 严格模式（WARN 视为 FAIL）
sudo ./scripts/security-baseline.sh --strict

# 指定应用 URL
sudo ./scripts/security-baseline.sh --app-url http://localhost:18080/medkernel
```

**用户管理**：

```bash
# 创建用户
curl -X POST http://localhost:18080/medkernel/api/security/admin/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username": "doctor1", "password": "Str0ng!Pass", "display_name": "张医生"}'

# 解锁用户
curl -X POST "http://localhost:18080/medkernel/api/security/admin/users/{userId}/unlock" \
  -H "Authorization: Bearer $TOKEN"

# 重置密码
curl -X POST "http://localhost:18080/medkernel/api/security/admin/users/{userId}/reset-password" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"new_password": "TempP@ss123"}'

# 强制用户下线
curl -X POST "http://localhost:18080/medkernel/api/security/admin/users/{userId}/force-logout" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 3.2 下午：监控与备份（4 学时）

#### 3.2.1 监控系统：Prometheus + Grafana

**启动监控栈**：

```bash
# 使用 Docker Compose 一键启动
cd /zoesoft/medkernel
docker compose -f deploy/docker-compose.monitoring.yml up -d

# 检查服务状态
docker compose -f deploy/docker-compose.monitoring.yml ps

# 访问地址
# Prometheus: http://<服务器IP>:9090
# Grafana:    http://<服务器IP>:3000
#   默认账号：admin / medkernel_admin
# Alertmanager: http://<服务器IP>:9093
```

**Prometheus 配置**（`deploy/monitoring/prometheus/prometheus-medkernel.yml`）：

```yaml
# 采集 MedKernel Actuator 指标
scrape_configs:
  - job_name: 'medkernel-backend'
    metrics_path: '/medkernel/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets: ['host.docker.internal:18081']
```

#### 3.2.2 Grafana 看板使用

系统提供以下监控看板：

| 看板 | 文件 | 用途 |
|------|------|------|
| MVP 业务监控 | `medkernel-mvp.json` | Provider 状态、规则评估 QPS、延迟、错误率 |
| SLO 监控 | `medkernel-slo.json` | 可用性 SLO、延迟 SLO、错误预算消耗 |

**MVP 业务看板核心面板**：

| 面板 | 指标 | 说明 |
|------|------|------|
| Provider 状态 | `medkernel_provider_ready` | 数据库/图谱/模型网关就绪状态 |
| 规则评估 QPS | `rate(medkernel_rule_evaluation_total[5m])` | 每秒规则评估数 |
| 规则评估延迟 P95 | `histogram_quantile(0.95, ...)` | 规则评估耗时分布 |
| API 请求延迟 P95 | `http_server_requests_seconds` | HTTP 请求耗时 |
| 5xx 错误率 | `rate(http_server_requests_seconds_count{status=~"5.."}[5m])` | 服务端错误比例 |
| JVM 堆内存 | `jvm_memory_used_bytes{area="heap"}` | 堆内存使用量 |
| HikariCP 连接池 | `hikaricp_connections_active / hikaricp_connections_max` | 连接池使用率 |

#### 3.2.3 告警规则和通知配置

**告警规则分类**（`deploy/monitoring/prometheus/medkernel-alert-rules.yml`）：

| 类别 | 告警名称 | 严重度 | 触发条件 |
|------|----------|--------|----------|
| **基础设施** | MedKernelBackendDown | critical | 后端不可达 > 1 分钟 |
| | MedKernelApiP95LatencyHigh | warning | P95 延迟 > 1s 持续 5 分钟 |
| | MedKernelApiP95LatencyCritical | critical | P95 延迟 > 3s 持续 2 分钟 |
| | MedKernelApi5xxRateHigh | warning | 5xx 错误率 > 0.5% 持续 5 分钟 |
| | MedKernelApi5xxRateCritical | critical | 5xx 错误率 > 2% 持续 2 分钟 |
| | MedKernelJvmHeapHigh | warning | 堆内存 > 75% 持续 10 分钟 |
| | MedKernelJvmHeapCritical | critical | 堆内存 > 90% 持续 2 分钟 |
| | MedKernelHikariPoolSaturation | warning | 连接池使用 > 80% 持续 5 分钟 |
| **系统资源** | MedKernelDiskSpaceWarning | warning | 磁盘使用 > 85% |
| | MedKernelDiskSpaceCritical | critical | 磁盘使用 > 95% |
| | MedKernelCpuLoadHigh | warning | CPU > 80% 持续 10 分钟 |
| | MedKernelFileDescriptorHigh | warning | 文件描述符 > 80% |
| **业务层** | MedKernelModelGatewayDown | critical | AI 模型网关不可用 |
| | MedKernelDatabaseProviderDown | critical | 数据库 Provider 不可用 |
| | MedKernelRuleEvalFailRateHigh | warning | 规则评估错误率 > 1% |
| | MedKernelPathwayStuck | warning | 路径实例停滞 > 30 分钟 |
| | MedKernelAuditLogWriteFailure | critical | 审计日志写入失败 |
| **数据库** | MedKernelDbConnectionLeak | critical | 连接疑似泄漏 |
| | MedKernelSlowQuery | warning | 慢查询 P99 > 500ms |
| | MedKernelDbPoolExhausted | critical | 连接池耗尽 |
| **安全** | MedKernelAuthFailureSpike | warning | 认证失败率 > 50% |
| | MedKernelPermissionDeniedSpike | warning | 403 响应异常增多 |
| | MedKernelDataMaskingFailure | critical | 数据脱敏失败 |
| **SLO** | MedKernelAvailabilitySLOBurnRateFast | critical | 可用性 SLO 快速燃烧 |
| | MedKernelSLOAvailabilityBreach | critical | 可用性 < 99.5% |
| | MedKernelSLOLatencyBreach | warning | P95 延迟 > 500ms |

**Alertmanager 通知配置**（`deploy/monitoring/alertmanager/alertmanager.yml`）：

```yaml
# 配置告警通知渠道
route:
  receiver: 'hospital-ops'
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

receivers:
  - name: 'hospital-ops'
    # 按医院实际情况选择通知方式
    webhook_configs:
      - url: 'http://hospital-notify/api/alerts'
    # 或邮件通知
    email_configs:
      - to: 'it-ops@hospital.cn'
        from: 'medkernel-alerts@hospital.cn'
```

#### 3.2.4 健康检查端点使用

| 端点 | URL | 说明 |
|------|-----|------|
| 业务健康 | `http://localhost:18080/medkernel/api/health` | 业务层健康状态 |
| Provider 状态 | `http://localhost:18080/medkernel/api/system/providers` | 各 Provider 就绪状态 |
| 组织上下文 | `http://localhost:18080/medkernel/api/system/org-context` | 组织上下文状态 |
| Actuator 健康 | `http://localhost:18081/medkernel/actuator/health` | Spring Boot 健康详情 |
| 就绪探针 | `http://localhost:18081/medkernel/actuator/health/readiness` | 就绪状态 |
| 存活探针 | `http://localhost:18081/medkernel/actuator/health/liveness` | 存活状态 |
| Prometheus 指标 | `http://localhost:18081/medkernel/actuator/prometheus` | Prometheus 格式指标 |
| 外部健康检查 | `http://<IP>/healthz` | 通过 Nginx 的健康检查 |

#### 3.2.5 备份恢复实操

**数据库备份**（按数据库类型）：

```bash
# PostgreSQL 全量备份
PGPASSWORD="${MEDKERNEL_DB_PASSWORD}" pg_dump \
  -h "${MEDKERNEL_DB_HOST}" -p "${MEDKERNEL_DB_PORT}" \
  -U "${MEDKERNEL_DB_USERNAME}" -d "${MEDKERNEL_DB_NAME}" \
  --format=custom --compress=9 \
  -f /backup/postgresql/medkernel_$(date +%Y%m%d_%H%M%S).dump

# Oracle expdp 逻辑备份
expdp MEDKERNEL/password@//localhost:1521/ORCL \
  schemas=MEDKERNEL directory=DATA_PUMP_DIR \
  dumpfile=medkernel_$(date +%Y%m%d).dmp compression=all parallel=4

# 达梦 dexp 逻辑导出
/opt/dmdbms/bin/dexp MEDKERNEL/password@10.0.0.20:5236 \
  FILE=/backup/dm/medkernel_$(date +%Y%m%d).dmp OWNER=MEDKERNEL COMPRESS=Y
```

**应用备份**（升级脚本自动执行）：

```bash
# 手动备份
TS=$(date +%Y%m%d_%H%M%S)
BACKUP_PATH="/zoesoft/medkernel.bak/$TS"
mkdir -p "$BACKUP_PATH"
for item in lib frontend conf db scripts systemd nginx manifest.json; do
  [ -e "/zoesoft/medkernel/$item" ] && cp -a "/zoesoft/medkernel/$item" "$BACKUP_PATH/"
done
```

**数据库恢复**：

```bash
# PostgreSQL 恢复
PGPASSWORD=<密码> pg_restore \
  -h 10.0.0.30 -U medkernel -d medkernel \
  --no-owner --no-privileges \
  /backup/postgresql/medkernel_20260523_080000.dump

# Oracle impdp 恢复
impdp MEDKERNEL/password@//localhost:1521/ORCL \
  schemas=MEDKERNEL directory=DATA_PUMP_DIR \
  dumpfile=medkernel_20260523.dmp parallel=4
```

**自动备份调度**（crontab）：

```crontab
# 数据库每日全量备份（凌晨 2:00）
0 2 * * * /zoesoft/medkernel/scripts/backup/db_full.sh >> /zoesoft/medkernel/logs/backup.log 2>&1

# 配置文件每日备份（凌晨 3:00）
0 3 * * * /zoesoft/medkernel/scripts/backup/backup_config.sh >> /zoesoft/medkernel/logs/backup.log 2>&1

# 备份清理（每周日凌晨 4:00）
0 4 * * 0 /zoesoft/medkernel/scripts/backup/cleanup_backups.sh >> /zoesoft/medkernel/logs/backup.log 2>&1
```

---

## 4. 第三天：升级回滚与故障排查（8 学时）

### 4.1 上午：升级回滚（4 学时）

#### 4.1.1 版本升级流程

**使用 upgrade.sh 升级**：

```bash
sudo ./scripts/upgrade.sh --to v1.3.0 [--backup-only] [--migrate-db]
```

| 参数 | 说明 |
|------|------|
| `--to <version>` | 目标版本号，如 `v1.3.0` |
| `--backup-only` | 仅备份，不执行升级 |
| `--migrate-db` | 执行数据库迁移 |

**完整升级流程**：

```bash
# 1. 升级前检查
#    确认当前版本
curl -s http://localhost:18080/medkernel/api/health | jq '.version'

#    确认服务健康
curl -s http://localhost:18081/medkernel/actuator/health | jq '.status'

#    仅备份（推荐先执行）
sudo ./scripts/upgrade.sh --backup-only

# 2. 上传新版发布包到 /tmp/
scp medkernel-v1.3.0.tar.gz root@<服务器>:/tmp/

# 3. 执行升级（含数据库迁移）
sudo ./scripts/upgrade.sh --to v1.3.0 --migrate-db

# 4. 升级脚本自动执行以下步骤：
#    步骤 1：备份当前版本（lib/frontend/conf/systemd/nginx/manifest.json）
#    步骤 2：停止服务
#    步骤 3：解压新版发布包
#    步骤 4：覆盖到 $MK_HOME（保留 conf，避免覆盖客户凭据）
#    步骤 5：数据库迁移（如指定 --migrate-db）
#    步骤 6：重启 systemd
#    步骤 7：健康检查

# 5. 验证升级
./scripts/healthcheck.sh
cat /zoesoft/medkernel/manifest.json | grep version
```

> **重要提示**：升级脚本会自动保留 `conf/` 目录，不会覆盖客户填写的数据库凭据等配置。

#### 4.1.2 数据库迁移：Flyway 管理

```bash
# 查看当前迁移版本
# 通过 SQL 查询
PGPASSWORD=<密码> psql -h 10.0.0.30 -U medkernel -d medkernel \
  -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"

# 启用 Flyway 自动迁移
# 在 medkernel.env 中设置
MEDKERNEL_FLYWAY_ENABLED=true
MEDKERNEL_FLYWAY_BASELINE_ON_MIGRATE=true
```

**Flyway 迁移脚本目录**：

```
db/migration/
├── common/                    # 通用迁移脚本
│   ├── V1.0__initial_schema.sql
│   └── V1.1__add_audit_fields.sql
├── oracle/                    # Oracle 专用迁移
│   └── V1.2__oracle_specific.sql
├── postgres/                  # PostgreSQL 专用迁移
│   └── V1.2__postgres_specific.sql
└── h2/                        # H2 专用迁移
    └── V1.2__h2_specific.sql
```

#### 4.1.3 回滚操作

**使用 rollback.sh 回滚**：

```bash
sudo ./scripts/rollback.sh --to <timestamp 或 last>
```

| 参数 | 说明 |
|------|------|
| `--to last` | 回滚到最近一次自动备份 |
| `--to 20260523_083000` | 回滚到指定备份目录 |

**回滚流程**：

```bash
# 1. 确认需要回滚
#    健康检查失败或业务异常
./scripts/healthcheck.sh

# 2. 执行回滚（回滚到最近一次备份）
sudo ./scripts/rollback.sh --to last

# 3. 回滚脚本自动执行以下步骤：
#    步骤 1：停止服务
#    步骤 2：还原备份（lib/frontend/conf/systemd/nginx/manifest.json）
#    步骤 3：重启服务
#    步骤 4：健康检查

# 4. 验证回滚
./scripts/healthcheck.sh
cat /zoesoft/medkernel/manifest.json | grep version

# 5. 如需回滚到指定备份
sudo ./scripts/rollback.sh --to 20260523_083000
```

> **注意**：回滚仅恢复应用文件和配置，不回滚数据库。如需回滚数据库，需单独执行数据库恢复操作。

#### 4.1.4 升级验证清单

升级完成后，按以下清单逐项验证：

| 序号 | 验证项 | 验证方法 | 预期结果 |
|------|--------|---------|---------|
| 1 | 服务状态 | `systemctl status medkernel` | active (running) |
| 2 | 健康检查 | `./scripts/healthcheck.sh` | 全部通过 |
| 3 | 版本号 | `cat manifest.json \| grep version` | 新版本号 |
| 4 | 业务健康 | `curl /api/health` | success: true |
| 5 | Provider 状态 | `curl /api/system/providers` | database READY |
| 6 | Actuator | `curl actuator/health` | status: UP |
| 7 | 前端访问 | 浏览器打开 | 页面正常加载 |
| 8 | 登录功能 | 使用测试账号登录 | 登录成功 |
| 9 | 规则引擎 | 执行测试规则评估 | 评估正常 |
| 10 | 监控数据 | Grafana 看板 | 指标正常采集 |
| 11 | 数据库迁移 | `SELECT * FROM flyway_schema_history` | 新迁移已执行 |

---

### 4.2 下午：故障排查与考核（4 学时）

#### 4.2.1 故障排查实战：8 类常见故障

##### 故障 1：服务无法启动

**现象**：`systemctl start medkernel` 后服务立即退出

**排查步骤**：

```bash
# 1. 查看启动日志
journalctl -u medkernel --since "5 minutes ago"

# 2. 检查端口占用
ss -tlnp | grep 18080

# 3. 检查 JDK 版本
java -version

# 4. 检查磁盘空间
df -h /zoesoft/medkernel

# 5. 检查配置文件
cat /zoesoft/medkernel/conf/medkernel.env | grep -v "^#" | grep -v "^$"

# 6. 检查文件权限
ls -la /zoesoft/medkernel/conf/medkernel.env
# 期望：-rw------- 1 medkernel medkernel
```

**常见原因与处置**：

| 原因 | 处置方法 |
|------|----------|
| 端口被占用 | `kill` 占用进程或修改 `MEDKERNEL_HTTP_PORT` |
| JDK 版本不匹配 | 安装正确版本的 JDK |
| 配置文件错误 | 恢复备份配置 |
| 磁盘空间不足 | 清理日志/备份文件 |
| 数据库连接失败 | 检查数据库状态和网络连通性 |
| JWT 密钥未设置 | 设置 `MEDKERNEL_JWT_SECRET` |

##### 故障 2：数据库连接失败

**现象**：API 返回 500 错误，日志显示 "Connection refused" 或 "Login failed"

**排查步骤**：

```bash
# 1. 检查数据库进程
ps aux | grep postgres    # PostgreSQL
ps aux | grep oracle      # Oracle

# 2. 检查数据库连通性
PGPASSWORD="${MEDKERNEL_DB_PASSWORD}" psql -h "${MEDKERNEL_DB_HOST}" -U "${MEDKERNEL_DB_USERNAME}" -d "${MEDKERNEL_DB_NAME}" -c 'SELECT 1;'

# 3. 检查 Actuator 数据库健康
curl -s http://localhost:18081/medkernel/actuator/health | jq '.components.db'

# 4. 检查连接池状态
curl -s http://localhost:18081/medkernel/actuator/prometheus | grep hikaricp_connections
```

**处置方法**：

- 重启数据库服务
- 检查数据库连接参数（主机、端口、用户名、密码）
- 检查防火墙规则
- 检查连接池是否耗尽

##### 故障 3：登录失败

**现象**：用户无法登录，提示"用户名或密码错误"或"账户已锁定"

**排查步骤**：

```bash
# 1. 检查用户状态
curl -s "http://localhost:18080/medkernel/api/security/admin/users?username=xxx" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.locked_until'

# 2. 解锁用户
curl -X POST "http://localhost:18080/medkernel/api/security/admin/users/{userId}/unlock" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 3. 重置密码
curl -X POST "http://localhost:18080/medkernel/api/security/admin/users/{userId}/reset-password" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"new_password": "TempP@ss123"}'
```

##### 故障 4：规则引擎无响应

**现象**：规则评估请求超时，返回 504

**排查步骤**：

```bash
# 1. 检查 Provider 状态
curl -s http://localhost:18080/medkernel/api/system/providers | jq '.'

# 2. 检查规则评估延迟
curl -s http://localhost:18081/medkernel/actuator/prometheus | grep medkernel_rule_eval

# 3. 检查是否有错误规则
curl -s "http://localhost:18080/medkernel/api/rule-engine/rules?status=ERROR" \
  -H "Authorization: Bearer $TOKEN"

# 4. 应急处置：重启服务
sudo systemctl restart medkernel
```

##### 故障 5：AI 功能不可用

**现象**：AI 功能返回"服务暂时不可用"

**排查步骤**：

```bash
# 1. 检查 Provider 状态
curl -s http://localhost:18080/medkernel/api/system/providers | jq '.providers.model_gateway'

# 2. 检查 LLM 相关指标
curl -s http://localhost:18081/medkernel/actuator/prometheus | grep medkernel_llm

# 3. 应急处置
#    系统自动降级为规则引擎模式
#    如需禁用 LLM：修改 medkernel.env
#    MEDKERNEL_DIFY_ENABLED=false
```

##### 故障 6：磁盘空间不足

**现象**：日志报 "No space left on device"

**排查步骤**：

```bash
# 1. 检查磁盘使用
df -h
du -sh /zoesoft/medkernel/logs/*
du -sh /zoesoft/medkernel.bak/*

# 2. 清理日志
find /zoesoft/medkernel/logs -name "*.log" -mtime +30 -delete

# 3. 清理旧备份
find /zoesoft/medkernel.bak -maxdepth 1 -type d -mtime +90 -exec rm -rf {} +

# 4. 清理 OOM 堆转储
rm -f /zoesoft/medkernel/logs/heap.hprof

# 5. 清理 Docker 资源（如有）
docker system prune -f
```

##### 故障 7：证书过期

**现象**：浏览器提示"证书无效"

**排查步骤**：

```bash
# 1. 检查证书有效期
openssl x509 -in /etc/nginx/ssl/medkernel.crt -noout -dates

# 2. 更新证书
cp new-cert.crt /etc/nginx/ssl/medkernel.crt
cp new-key.key /etc/nginx/ssl/medkernel.key
chmod 600 /etc/nginx/ssl/medkernel.key

# 3. 重载 Nginx
nginx -t && nginx -s reload
```

##### 故障 8：Nginx 502/504 错误

**现象**：浏览器访问返回 502 Bad Gateway 或 504 Gateway Timeout

**排查步骤**：

```bash
# 1. 检查后端服务是否运行
systemctl status medkernel

# 2. 检查后端端口是否监听
ss -tlnp | grep 18080

# 3. 直接访问后端
curl -s http://localhost:18080/medkernel/api/health

# 4. 检查 Nginx 配置
nginx -t

# 5. 查看 Nginx 错误日志
tail -50 /var/log/nginx/medkernel.error.log

# 6. 应急处置
#    502：后端未启动或崩溃 → 重启 medkernel 服务
#    504：后端响应超时 → 检查后端性能，调整 proxy_read_timeout
```

#### 4.2.2 应急响应流程

```
发现故障 → 初步判断 → 影响评估 → 应急处置 → 恢复验证 → 事后复盘
   5min       10min       15min       30min       10min       24h内
```

**故障分级**：

| 级别 | 定义 | 响应时间 | 示例 |
|------|------|----------|------|
| P0 紧急 | 系统完全不可用 | 5 分钟 | 数据库宕机、服务无法启动 |
| P1 严重 | 核心功能不可用 | 15 分钟 | 规则引擎故障、登录失败 |
| P2 一般 | 非核心功能异常 | 1 小时 | 图谱查询超时、LLM 降级 |
| P3 轻微 | 体验性问题 | 4 小时 | 页面加载慢、样式异常 |

**应急处置记录模板**：

```
故障编号：INC-YYYYMMDD-NNN
故障时间：YYYY-MM-DD HH:MM
发现人：
故障级别：P0/P1/P2/P3
影响范围：
故障现象：
根因分析：
处置措施：
恢复时间：
数据影响：
后续改进：
```

#### 4.2.3 性能调优

**JVM 调优**：

```bash
# 修改 medkernel.env 中的 JAVA_OPTS
# 默认配置（8GB 内存服务器）
JAVA_OPTS="-server -Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/zoesoft/medkernel/logs/heap.hprof \
  -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.timezone=Asia/Shanghai \
  -Djava.security.egd=file:/dev/./urandom"

# 大型医院配置（16GB 内存服务器）
JAVA_OPTS="-server -Xms4g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:InitiatingHeapOccupancyPercent=45 \
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/zoesoft/medkernel/logs/heap.hprof \
  -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.timezone=Asia/Shanghai \
  -Djava.security.egd=file:/dev/./urandom"
```

| 参数 | 说明 | 调优建议 |
|------|------|----------|
| `-Xms` | 初始堆内存 | 与 `-Xmx` 相同，避免动态扩容 |
| `-Xmx` | 最大堆内存 | 物理内存的 50%-75% |
| `-XX:+UseG1GC` | G1 垃圾回收器 | 推荐用于低延迟场景 |
| `-XX:MaxGCPauseMillis` | GC 最大暂停时间 | 200ms，按需调整 |
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM 时生成堆转储 | 必须开启，用于排查 |

**连接池调优**：

```bash
# 修改 medkernel.env
MEDKERNEL_DB_POOL_MAX=30          # 高并发场景增大到 30
MEDKERNEL_DB_POOL_MIN_IDLE=5      # 保持 5 个空闲连接
MEDKERNEL_DB_POOL_TIMEOUT=3000    # 获取连接超时 3 秒
MEDKERNEL_DB_POOL_LEAK=5000       # 泄漏检测阈值 5 秒
```

**Nginx 调优**：

```nginx
# 调整限速（高并发场景）
limit_req_zone $binary_remote_addr zone=medkernel_api:10m rate=200r/s;

# 调整超时
proxy_connect_timeout 10s;
proxy_send_timeout    120s;     # 长时间操作
proxy_read_timeout    120s;

# 调整 keepalive
upstream medkernel_backend {
    server 127.0.0.1:18080 max_fails=3 fail_timeout=10s;
    keepalive 64;               # 增大 keepalive 连接数
}
```

#### 4.2.4 培训考核

**理论考核**（30 分）：

| 题型 | 数量 | 分值 | 考察重点 |
|------|------|------|----------|
| 单选题 | 10 | 10 | 系统架构、端口规划、目录结构 |
| 多选题 | 5 | 10 | 配置参数、告警规则、安全要求 |
| 判断题 | 5 | 5 | 运维流程、安全规范 |
| 简答题 | 1 | 5 | 故障排查思路 |

**实操考核**（70 分）：

| 考核项 | 分值 | 时间限制 | 通过标准 |
|--------|------|----------|----------|
| 完整安装部署 | 20 | 60 分钟 | 健康检查全部通过 |
| 配置修改和服务重启 | 10 | 15 分钟 | 配置生效，服务正常 |
| 监控看板使用 | 10 | 15 分钟 | 能找到关键指标和告警 |
| 备份恢复操作 | 15 | 30 分钟 | 数据完整恢复 |
| 升级回滚操作 | 10 | 20 分钟 | 升级和回滚均成功 |
| 故障排查模拟 | 5 | 15 分钟 | 正确识别故障原因并处置 |

**考核通过标准**：总分 ≥ 70 分，且实操考核每项 ≥ 60%。

---

## 5. 实操练习

### 练习 1：完整安装部署（60 分钟）

**目标**：在全新环境中完成 MedKernel 的安装部署

**步骤**：

1. 准备环境：安装 JDK、PostgreSQL、Nginx
2. 选择部署 profile：`cp profiles/pg-x86_64.env conf/medkernel.env`
3. 修改环境变量：设置数据库密码、JWT 密钥
4. 执行环境检查：`./scripts/check-env.sh`
5. 执行安装：`sudo ./scripts/install-offline.sh --init-db`
6. 配置 Nginx：部署 HTTPS 配置
7. 执行健康检查：`./scripts/healthcheck.sh`
8. 浏览器访问验证

**验收标准**：健康检查全部通过，浏览器可正常访问系统

### 练习 2：配置修改和服务重启（15 分钟）

**目标**：修改 JVM 内存配置并重启服务

**步骤**：

1. 备份当前配置
2. 修改 `medkernel.env` 中的 `JAVA_OPTS`，将 `-Xmx4g` 改为 `-Xmx6g`
3. 重启服务
4. 验证配置生效：`ps aux | grep medkernel | grep Xmx`

**验收标准**：服务正常启动，JVM 内存参数生效

### 练习 3：监控看板使用（15 分钟）

**目标**：在 Grafana 中查看关键指标和告警

**步骤**：

1. 启动监控栈：`docker compose -f deploy/docker-compose.monitoring.yml up -d`
2. 登录 Grafana（admin / medkernel_admin）
3. 查看 MVP 业务监控看板
4. 找到 Provider 状态面板，确认 database 为 READY
5. 查看规则评估 QPS 和延迟
6. 查看 JVM 堆内存使用
7. 查看告警规则列表

**验收标准**：能独立找到 5 个以上关键指标面板

### 练习 4：备份恢复操作（30 分钟）

**目标**：完成数据库备份和恢复操作

**步骤**：

1. 执行 PostgreSQL 全量备份
2. 验证备份文件完整性
3. 模拟数据丢失：删除一条测试数据
4. 执行数据库恢复
5. 验证数据恢复完整

**验收标准**：备份成功，恢复后数据完整

### 练习 5：升级回滚操作（20 分钟）

**目标**：完成版本升级和回滚操作

**步骤**：

1. 确认当前版本
2. 执行仅备份：`sudo ./scripts/upgrade.sh --backup-only`
3. 模拟升级：`sudo ./scripts/upgrade.sh --to v1.3.0`
4. 验证升级结果
5. 执行回滚：`sudo ./scripts/rollback.sh --to last`
6. 验证回滚结果

**验收标准**：升级和回滚均成功，健康检查通过

### 练习 6：故障排查模拟（15 分钟）

**目标**：在模拟故障场景中快速定位和处置

**场景**：讲师随机选择以下故障之一注入，学员在 15 分钟内排查并恢复

- 场景 A：停止 MedKernel 服务，学员需发现并重启
- 场景 B：修改 `medkernel.env` 中的数据库密码为错误值，学员需定位并修复
- 场景 C：占用 18080 端口，学员需发现并释放端口

**验收标准**：正确识别故障原因，成功恢复服务

---

## 6. 考核标准

### 6.1 理论考核范围

| 知识域 | 考察重点 | 占比 |
|--------|----------|------|
| 系统架构 | 三层架构、端口规划、目录结构 | 20% |
| 安装部署 | install-offline.sh 用法、profile 选择、配置文件 | 20% |
| 配置参数 | medkernel.env 关键参数、application.yml 配置项 | 15% |
| 运维流程 | 服务管理、日志管理、备份恢复 | 15% |
| 监控告警 | Prometheus/Grafana 使用、告警规则分类 | 15% |
| 升级回滚 | upgrade.sh/rollback.sh 用法、Flyway 迁移 | 10% |
| 故障排查 | 8 类常见故障的排查思路和处置方法 | 5% |

### 6.2 实操考核标准

| 考核项 | 通过标准 | 优秀标准 |
|--------|----------|----------|
| 完整安装部署 | 健康检查通过，系统可访问 | 30 分钟内完成 |
| 配置修改和服务重启 | 配置生效，服务正常 | 10 分钟内完成 |
| 监控看板使用 | 能找到关键指标 | 能解释指标含义和告警条件 |
| 备份恢复操作 | 数据完整恢复 | 20 分钟内完成 |
| 升级回滚操作 | 升级和回滚均成功 | 15 分钟内完成 |
| 故障排查模拟 | 正确识别并处置 | 10 分钟内完成 |

### 6.3 综合评定

| 等级 | 条件 |
|------|------|
| 优秀 | 总分 ≥ 90 分，实操每项 ≥ 80% |
| 良好 | 总分 ≥ 80 分，实操每项 ≥ 70% |
| 合格 | 总分 ≥ 70 分，实操每项 ≥ 60% |
| 不合格 | 总分 < 70 分，或实操任一项 < 60% |

---

## 附录 A：常用命令速查

```bash
# === 服务管理 ===
sudo systemctl start medkernel          # 启动
sudo systemctl stop medkernel           # 停止
sudo systemctl restart medkernel        # 重启
systemctl status medkernel              # 状态

# === 日志查看 ===
journalctl -u medkernel -n 100          # 最近 100 行
journalctl -u medkernel -f              # 实时跟踪
tail -f /zoesoft/medkernel/logs/stdout.log  # 应用日志

# === 健康检查 ===
./scripts/healthcheck.sh                # 综合健康检查
curl -s http://localhost:18080/medkernel/api/health | jq .  # 业务健康
curl -s http://localhost:18081/medkernel/actuator/health | jq .  # Actuator

# === 安装/升级/回滚 ===
sudo ./scripts/install-offline.sh --init-db     # 首次安装
sudo ./scripts/upgrade.sh --to v1.3.0 --migrate-db  # 升级
sudo ./scripts/rollback.sh --to last            # 回滚

# === 安全 ===
sudo ./scripts/security-baseline.sh     # 安全基线检查

# === 监控 ===
docker compose -f deploy/docker-compose.monitoring.yml up -d   # 启动监控
docker compose -f deploy/docker-compose.monitoring.yml ps       # 监控状态

# === 数据库 ===
PGPASSWORD=<密码> psql -h <host> -U medkernel -d medkernel -c 'SELECT 1;'  # 连通测试
```

## 附录 B：关键文件路径

| 文件 | 路径 | 说明 |
|------|------|------|
| 环境变量 | `/zoesoft/medkernel/conf/medkernel.env` | 数据库凭据、JVM 参数 |
| Spring 配置 | `/zoesoft/medkernel/conf/application.yml` | Spring Boot 主配置 |
| systemd 服务 | `/etc/systemd/system/medkernel.service` | 服务管理 |
| Nginx HTTP | `/etc/nginx/conf.d/medkernel.conf` | HTTP 反代配置 |
| Nginx HTTPS | `/etc/nginx/conf.d/medkernel-tls.conf` | HTTPS 反代配置 |
| TLS 证书 | `/etc/nginx/ssl/medkernel.crt` | SSL 证书 |
| TLS 私钥 | `/etc/nginx/ssl/medkernel.key` | SSL 私钥 |
| 应用日志 | `/zoesoft/medkernel/logs/stdout.log` | 标准输出 |
| 错误日志 | `/zoesoft/medkernel/logs/stderr.log` | 标准错误 |
| Nginx 访问日志 | `/var/log/nginx/medkernel.access.log` | HTTP 请求 |
| Nginx 错误日志 | `/var/log/nginx/medkernel.error.log` | Nginx 错误 |
| 版本清单 | `/zoesoft/medkernel/manifest.json` | 版本信息 |
| 备份目录 | `/zoesoft/medkernel.bak/` | 升级自动备份 |
| 最近备份标记 | `/zoesoft/medkernel/.last-backup` | 最近备份路径 |

## 附录 C：应急联系人模板

| 角色 | 姓名 | 电话 | 职责 |
|------|------|------|------|
| 系统管理员 | ___ | ___ | 服务启停、配置变更 |
| 数据库管理员 | ___ | ___ | 数据库运维、备份恢复 |
| 安全管理员 | ___ | ___ | 密钥轮换、安全事件 |
| 厂商技术支持 | ___ | ___ | 深度排查、代码修复 |
