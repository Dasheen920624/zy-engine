# MedKernel 升级回滚手册

> 版本：1.0 | 适用：MedKernel v1.x GA | 日期：2026-05-23
>
> **本文档面向医院信息科运维人员，请严格按流程操作，确保诊疗系统安全。**

---

## 目录

1. [版本策略](#1-版本策略)
2. [升级前准备](#2-升级前准备)
3. [升级流程](#3-升级流程)
4. [补丁升级（热修复）](#4-补丁升级热修复)
5. [主版本升级](#5-主版本升级)
6. [回滚操作](#6-回滚操作)
7. [升级后验证](#7-升级后验证)
8. [升级故障处理](#8-升级故障处理)
9. [升级记录模板](#9-升级记录模板)

---

## 1. 版本策略

### 1.1 版本号规范

遵循 [Semantic Versioning 2.0](https://semver.org/)，格式为 `v主.次.补丁`：

```
vMAJOR.MINOR.PATCH[-prerelease][+build]

示例：v1.0.0    v1.0.1    v1.1.0    v2.0.0-rc.1
```

| 段 | 含义 | 变更范围 | 示例 |
|---|---|---|---|
| **MAJOR（主版本）** | 不兼容变更 | API 删除、数据库表删除/列类型变更、Provider 接口签名 break | v1.x → v2.0 |
| **MINOR（次版本）** | 向后兼容新增 | 新模块、新接口、新字段、新页面、新规则场景 | v1.0 → v1.1 |
| **PATCH（补丁）** | Bug 修复/内部优化 | 不改 API 与 DDL 的修复、性能优化、日志改善 | v1.0.0 → v1.0.1 |
| `-rc.N` / `-beta.N` | 预发布 | UAT 阶段 | v2.0.0-rc.1 |
| `+gitShortHash` | 构建标识 | CI 自动注入 | v1.0.0+a1b2c3d |

### 1.2 升级类型

| 升级类型 | 版本变化 | 数据库迁移 | 停机时间 | 风险等级 | 审批要求 |
|---------|---------|-----------|---------|---------|---------|
| **补丁升级** | v1.0.0 → v1.0.1 | 无 | ≤ 5 分钟 | 低 | 信息科负责人 |
| **次版本升级** | v1.0.0 → v1.1.0 | 可能有（仅新增列/表） | 5-15 分钟 | 中 | 信息科负责人 + 厂商技术支持 |
| **主版本升级** | v1.x → v2.0.0 | 有（含破坏性变更） | 15-60 分钟 | 高 | 信息科主任 + 厂商项目组 |

### 1.3 兼容性矩阵

#### 数据库版本

| 数据库 | 支持版本 | DDL 目录 |
|--------|---------|---------|
| Oracle | 11g R2, 12c, 18c, 19c, 21c | `$MK_HOME/db/oracle/` |
| 达梦 DM | DM 7, DM 8 | `$MK_HOME/db/dm/` |
| PostgreSQL | 14, 15, 16 | `$MK_HOME/db/postgres/` |
| 人大金仓 KingbaseES | V8 R3, V8 R6 | `$MK_HOME/db/postgres/`（兼容） |

#### JDK 版本

| 要求 | 说明 |
|------|------|
| **JDK 1.8.0_202+** | 项目固定 JDK 1.8 |
| 推荐 | 毕昇 JDK / Eclipse Temurin / OpenJDK |

#### 操作系统版本

| 操作系统 | 架构 | Profile 文件 |
|---------|------|-------------|
| CentOS 7 | x86_64 | `centos7-x86_64-oracle.env` |
| openEuler 22.03 | x86_64 / aarch64 | — |
| UOS V20 | x86_64 / aarch64 | `uos-aarch64-dm.env` |
| 银河麒麟 V10 | x86_64 / aarch64 | `kylin-aarch64-dm.env` |
| Windows Server | x86_64 | — |

#### 运行时最低要求

| 资源 | 最低值 | 推荐值 |
|------|--------|--------|
| 内存 | 2 GB | 4 GB+ |
| 磁盘 | 10 GB | 50 GB+ |
| 后端端口 | 18080 | — |
| 前端端口 | 80 / 443 | — |
| Actuator 端口 | 18081 | — |

### 1.4 版本清单 — manifest.json

每个发布包根目录包含 `manifest.json`，记录版本指纹：

```json
{
  "name": "medkernel",
  "version": "1.2.3",
  "git_hash": "a1b2c3d",
  "build_time": "2026-05-17T08:30:00+08:00",
  "build_host": "ci-builder-1",
  "components": {
    "backend":  { "jar": "lib/medkernel.jar", "sha256": "..." },
    "frontend": { "dist": "frontend/dist/",   "sha256": "..." },
    "db_oracle":   { "ddl": "db/oracle/medkernel_core_ddl_with_comments.sql" },
    "db_dm":       { "ddl": "db/dm/medkernel_core_ddl_with_comments.sql" },
    "db_postgres": { "ddl": "db/postgres/medkernel_core_ddl_with_comments.sql" }
  },
  "supported_os": ["centos7-x86_64", "uos-aarch64", "kylin-aarch64", "windows-x86_64"]
}
```

**查看当前版本：**

```bash
cat /zoesoft/medkernel/manifest.json | grep -E '"version"|"git_hash"'
```

**查看 API 版本：**

```bash
curl -s http://localhost:18080/medkernel/api/system/version
```

---

## 2. 升级前准备

### 2.1 环境检查

运行 `check-env.sh` 对目标环境进行全面检查：

```bash
# 基础检查
sudo /zoesoft/medkernel/scripts/check-env.sh

# 指定 profile 检查
sudo /zoesoft/medkernel/scripts/check-env.sh --profile centos7-x86_64-oracle
```

检查项包括：

| 检查项 | 通过条件 | 失败处理 |
|--------|---------|---------|
| OS 与 CPU 架构 | 在 supported_os 列表中 | 联系厂商确认兼容性 |
| JDK 版本 | JDK 1.8.0_202+ | 安装毕昇 JDK / Temurin / OpenJDK 1.8 |
| locale | UTF-8 | `export LANG=zh_CN.UTF-8` |
| 时区 | Asia/Shanghai (CST) | `timedatectl set-timezone Asia/Shanghai` |
| 磁盘空间 | ≥ 10 GB 可用 | 清理日志/临时文件 |
| 后端端口 18080 | 空闲 | 检查占用进程 |
| 前端端口 80 | 空闲（Nginx 已运行可忽略） | 检查 Nginx 状态 |
| SELinux | Permissive 或 Disabled | 调整 SELinux 策略 |
| 数据库连通 | 对应 dialect 连接成功 | 检查数据库服务与网络 |

> ⚠️ **环境检查存在 FAIL 项时，禁止继续升级！** 必须先修复所有 FAIL 项。

### 2.2 备份确认

升级前必须完成以下备份：

| 备份项 | 备份方式 | 存储位置 | 验证方法 |
|--------|---------|---------|---------|
| **数据库** | 数据库全量导出（expdp/pg_dump） | 异机存储 | 检查 dump 文件大小与时间戳 |
| **应用** | `upgrade.sh` 自动备份 | `/zoesoft/medkernel.bak/<timestamp>/` | 检查备份目录完整性 |
| **配置文件** | 随应用备份 | `/zoesoft/medkernel.bak/<timestamp>/conf/` | 确认 medkernel.env 存在 |
| **Nginx 配置** | 手动备份 | `cp /etc/nginx/conf.d/medkernel.conf /etc/nginx/conf.d/medkernel.conf.bak` | 文件存在 |

**数据库备份命令参考：**

```bash
# Oracle — RMAN 全量备份
rman target / <<EOF
BACKUP DATABASE PLUS ARCHIVELOG;
EOF

# Oracle — expdp 数据泵导出
expdp MEDKERNEL/PASSWORD@//10.0.0.10:1521/ORCL \
  directory=BACKUP_DIR dumpfile=medkernel_pre_upgrade_$(date +%Y%m%d).dmp \
  logfile=medkernel_pre_upgrade_$(date +%Y%m%d).log

# PostgreSQL — pg_dump 全量导出
pg_dump -h localhost -U medkernel -d medkernel -Fc \
  -f /backup/medkernel_pre_upgrade_$(date +%Y%m%d).dump

# 达梦 — dexp 导出
dexp MEDKERNEL/PASSWORD@localhost:5236 \
  FILE=medkernel_pre_upgrade_$(date +%Y%m%d).dmp LOG=medkernel_pre_upgrade.log
```

**应用备份（可单独执行）：**

```bash
sudo /zoesoft/medkernel/scripts/upgrade.sh --backup-only
```

> ⚠️ **数据库备份必须异机存储！** 确保备份文件不在本机 `/zoesoft` 分区。

### 2.3 变更审批

升级前需提交变更申请单，模板如下：

```
┌─────────────────────────────────────────────────────────────┐
│                  MedKernel 版本升级申请单                      │
├─────────────────────────────────────────────────────────────┤
│ 申请日期：     ____年__月__日                                  │
│ 申请人：       ____________                                   │
│ 申请部门：     ____________                                   │
│                                                             │
│ 当前版本：     v______.__.__                                  │
│ 目标版本：     v______.__.__                                  │
│ 升级类型：     □ 补丁升级  □ 次版本升级  □ 主版本升级            │
│                                                             │
│ 变更原因：                                                    │
│ __________________________________________________________  │
│                                                             │
│ 影响范围：                                                    │
│ □ 全院临床科室  □ 部分科室（请列出）：__________               │
│                                                             │
│ 维护窗口：     ____年__月__日 __:__ ~ __:__                   │
│ 预计停机时间：  ____ 分钟                                      │
│                                                             │
│ 回滚方案：     □ 自动回滚（rollback.sh） □ 手动回滚            │
│ 数据库回滚：   □ 无需回滚  □ 有回滚 SQL（已准备）              │
│                                                             │
│ 审批人：       ____________  签字：________  日期：____        │
│ 信息科主任：   ____________  签字：________  日期：____        │
│ 厂商技术支持： ____________  签字：________  日期：____        │
└─────────────────────────────────────────────────────────────┘
```

### 2.4 通知相关方

| 通知对象 | 通知方式 | 通知时间 | 内容 |
|---------|---------|---------|------|
| 信息科值班人员 | 电话 + 即时通讯 | 升级前 2 小时 | 升级时间、影响范围、联系方式 |
| 临床科室护士站 | 即时通讯 / 公告 | 升级前 1 小时 | 系统维护时段、替代方案 |
| 厂商技术支持 | 电话 | 升级前 1 小时 | 确认在线待命 |
| HIS 对接负责人 | 即时通讯 | 升级前 2 小时 | 接口影响说明 |

### 2.5 维护窗口

| 建议 | 说明 |
|------|------|
| **推荐时段** | 凌晨 2:00 - 5:00 |
| 最短窗口 | 补丁升级 30 分钟，次版本升级 1 小时，主版本升级 2 小时 |
| 禁止时段 | 门诊高峰（8:00-12:00, 14:00-17:00）、急诊高峰 |

---

## 3. 升级流程

### 3.1 使用 upgrade.sh 自动升级

自动升级脚本执行 7 步流程：

```bash
# 完整升级（含数据库迁移）
sudo /zoesoft/medkernel/scripts/upgrade.sh --to v1.1.0 --migrate-db

# 仅升级应用（不含数据库迁移）
sudo /zoesoft/medkernel/scripts/upgrade.sh --to v1.1.0
```

**参数说明：**

| 参数 | 说明 | 示例 |
|------|------|------|
| `--to <version>` | 目标版本号（必填） | `--to v1.1.0` |
| `--migrate-db` | 执行数据库迁移 | `--migrate-db` |
| `--backup-only` | 仅备份，不升级 | `--backup-only` |

**7 步流程详解：**

```
步骤 1 ─ 备份当前版本
  ├── 创建备份目录 /zoesoft/medkernel.bak/<timestamp>/
  ├── 备份 lib/ frontend/ conf/ systemd/ nginx/
  ├── 备份 manifest.json
  └── 记录备份路径到 .last-backup

步骤 2 ─ 停止服务
  └── systemctl stop medkernel

步骤 3 ─ 解压新版发布包
  ├── 检查 /tmp/medkernel-vX.Y.Z.tar.gz 是否存在
  └── 解压到临时目录

步骤 4 ─ 覆盖到 MK_HOME（保留 conf）
  ├── 覆盖 lib/ frontend/ db/ scripts/ systemd/ nginx/ docs/ profiles/
  ├── 覆盖 manifest.json CHANGELOG.md
  ├── ⚠️ conf/ 目录不覆盖（保护客户配置）
  └── 修正文件权限

步骤 5 ─ 数据库迁移（需 --migrate-db）
  ├── 调用 install-offline.sh --migrate-db
  └── 按 dialect 执行对应 DDL

步骤 6 ─ 重启 systemd
  ├── systemctl daemon-reload
  ├── systemctl start medkernel
  └── 等待 3 秒确认启动成功

步骤 7 ─ 健康检查
  ├── /api/health
  ├── /api/system/providers
  └── /api/system/org-context
```

> ⚠️ **conf 目录不会被覆盖！** 如新版本引入新配置项，需手动合并。参见 [3.5 配置迁移](#35-配置迁移)。

### 3.2 手动升级步骤

如自动脚本不可用，可按以下步骤手动升级：

```bash
# ========== 变量定义 ==========
MK_HOME=/zoesoft/medkernel
MK_BACKUP_DIR=/zoesoft/medkernel.bak
TO_VER=v1.1.0
TS=$(date +%Y%m%d_%H%M%S)

# ========== 步骤 1：备份 ==========
mkdir -p $MK_BACKUP_DIR/$TS
for d in lib frontend conf systemd nginx; do
  [ -d "$MK_HOME/$d" ] && cp -a "$MK_HOME/$d" "$MK_BACKUP_DIR/$TS/"
done
[ -f "$MK_HOME/manifest.json" ] && cp "$MK_HOME/manifest.json" "$MK_BACKUP_DIR/$TS/"
echo "$MK_BACKUP_DIR/$TS" > "$MK_HOME/.last-backup"
echo "备份完成：$MK_BACKUP_DIR/$TS"

# ========== 步骤 2：停止服务 ==========
systemctl stop medkernel

# ========== 步骤 3：解压发布包 ==========
NEW_PKG="/tmp/medkernel-${TO_VER}.tar.gz"
[ -f "$NEW_PKG" ] || { echo "错误：未找到 $NEW_PKG"; exit 1; }
TMPDIR_NEW=$(mktemp -d)
tar -xzvf "$NEW_PKG" -C "$TMPDIR_NEW" --strip-components=1

# ========== 步骤 4：覆盖文件（保留 conf） ==========
for d in lib frontend db scripts systemd nginx docs profiles; do
  [ -e "$TMPDIR_NEW/$d" ] && { rm -rf "$MK_HOME/$d"; cp -a "$TMPDIR_NEW/$d" "$MK_HOME/"; }
done
[ -f "$TMPDIR_NEW/manifest.json" ] && cp "$TMPDIR_NEW/manifest.json" "$MK_HOME/"
[ -f "$TMPDIR_NEW/CHANGELOG.md" ] && cp "$TMPDIR_NEW/CHANGELOG.md" "$MK_HOME/"
chown -R medkernel:medkernel $MK_HOME
chmod 600 $MK_HOME/conf/medkernel.env 2>/dev/null || true

# ========== 步骤 5：数据库迁移（如需要） ==========
# 参见 3.3 数据库迁移

# ========== 步骤 6：重启服务 ==========
systemctl daemon-reload
systemctl start medkernel
sleep 3
systemctl is-active --quiet medkernel || { echo "启动失败！查看日志：journalctl -u medkernel -n 100"; exit 1; }

# ========== 步骤 7：健康检查 ==========
/zoesoft/medkernel/scripts/healthcheck.sh
```

### 3.3 数据库迁移

MedKernel 使用按 dialect 分目录的 DDL 脚本进行数据库迁移（非 Flyway 自动迁移），需根据数据库类型手动或通过脚本执行。

**迁移脚本位置：**

| 数据库 | DDL 目录 | 迁移脚本 |
|--------|---------|---------|
| Oracle | `$MK_HOME/db/oracle/` | `medkernel_core_ddl_with_comments.sql`、`medkernel_org_context_migration.sql` |
| 达梦 DM | `$MK_HOME/db/dm/` | `medkernel_core_ddl_with_comments.sql`、`medkernel_org_context_migration.sql` |
| PostgreSQL | `$MK_HOME/db/postgres/` | `medkernel_core_ddl_with_comments.sql`、`medkernel_org_context_migration.sql` |

**通过 upgrade.sh 自动迁移：**

```bash
sudo /zoesoft/medkernel/scripts/upgrade.sh --to v1.1.0 --migrate-db
```

**手动执行迁移：**

```bash
# Oracle
sqlplus MEDKERNEL/PASSWORD@//10.0.0.10:1521/ORCL @$MK_HOME/db/oracle/medkernel_org_context_migration.sql

# 达梦 DM
disql MEDKERNEL/PASSWORD@localhost:5236 -e "START '$MK_HOME/db/dm/medkernel_org_context_migration.sql';"

# PostgreSQL
PGPASSWORD=PASSWORD psql -h localhost -U medkernel -d medkernel \
  -v ON_ERROR_STOP=1 \
  -f $MK_HOME/db/postgres/medkernel_org_context_migration.sql
```

> ⚠️ **数据库迁移前必须确认已有全量备份！** 迁移 DDL 通常只做增量（ADD COLUMN / CREATE TABLE），不会删除已有数据。

### 3.4 前端更新

前端更新在升级步骤 4 中自动完成（覆盖 `frontend/dist/` 目录）。

手动更新前端：

```bash
# 备份旧前端
cp -a /zoesoft/medkernel/frontend/dist /zoesoft/medkernel/frontend/dist.bak

# 替换前端
rm -rf /zoesoft/medkernel/frontend/dist
cp -a $TMPDIR_NEW/frontend/dist /zoesoft/medkernel/frontend/dist

# 重载 Nginx（无需重启后端）
sudo nginx -t && sudo systemctl reload nginx
```

### 3.5 配置迁移

**核心原则：conf 目录不覆盖，保护客户已有配置。**

新版本可能引入新配置项，需手动合并：

```bash
# 1. 查看新版本配置模板
diff /zoesoft/medkernel.bak/$TS/conf/medkernel.env /zoesoft/medkernel/profiles/centos7-x86_64-oracle.env

# 2. 查看新版本 application.yml 变更
diff /zoesoft/medkernel.bak/$TS/conf/application.yml /zoesoft/medkernel/conf/application.yml 2>/dev/null || true

# 3. 手动将新增配置项追加到现有配置文件
# 注意：只追加新项，不修改已有值
```

**常见新增配置项：**

| 配置项 | 引入版本 | 说明 |
|--------|---------|------|
| `MEDKERNEL_GRAPH_ENABLED` | v1.0 | 图谱功能开关 |
| `MEDKERNEL_DIFY_ENABLED` | v1.0 | Dify 工作流开关 |
| `MEDKERNEL_HTTP_PORT` | v1.0 | 后端 HTTP 端口 |
| `MEDKERNEL_DB_DIALECT` | v1.0 | 数据库类型 |

### 3.6 升级验证

升级完成后执行健康检查：

```bash
# 自动健康检查
/zoesoft/medkernel/scripts/healthcheck.sh

# 指定 URL 检查
/zoesoft/medkernel/scripts/healthcheck.sh --url http://localhost:18080/medkernel --timeout 15
```

健康检查探测 3 个端点：

| 端点 | 说明 | 通过条件 |
|------|------|---------|
| `/api/health` | 应用存活 | 返回 `"success": true` |
| `/api/system/providers` | Provider 状态 | 返回 `"success": true` |
| `/api/system/org-context` | 组织上下文 | 返回 `"success": true` |

---

## 4. 补丁升级（热修复）

补丁升级（PATCH 版本）是最简化的升级流程，仅替换 JAR 文件并重启。

### 4.1 快速补丁流程

```bash
# 1. 备份（快速）
sudo /zoesoft/medkernel/scripts/upgrade.sh --backup-only

# 2. 停止服务
sudo systemctl stop medkernel

# 3. 替换 JAR
sudo cp /tmp/medkernel-v1.0.1/lib/medkernel.jar /zoesoft/medkernel/lib/medkernel.jar
sudo chown medkernel:medkernel /zoesoft/medkernel/lib/medkernel.jar

# 4. 替换 manifest.json
sudo cp /tmp/medkernel-v1.0.1/manifest.json /zoesoft/medkernel/manifest.json

# 5. 重启
sudo systemctl start medkernel
sleep 3

# 6. 健康检查
/zoesoft/medkernel/scripts/healthcheck.sh
```

### 4.2 补丁升级特点

| 项目 | 说明 |
|------|------|
| 数据库迁移 | **无需** — PATCH 版本不含 DDL 变更 |
| 前端更新 | 通常不需要 — 如有前端修复，替换 `frontend/dist/` |
| 配置变更 | 通常不需要 — 如有新配置项，会在 CHANGELOG 中说明 |
| 停机时间 | ≤ 5 分钟 |
| 验证步骤 | 简化 — 仅健康检查 + 核心功能验证 |

### 4.3 验证步骤简化

```bash
# 1. 健康检查
/zoesoft/medkernel/scripts/healthcheck.sh

# 2. 版本确认
curl -s http://localhost:18080/medkernel/api/system/version | python3 -m json.tool

# 3. 核心功能快速验证（选择 1-2 个关键 API）
curl -s http://localhost:18080/medkernel/api/health
```

---

## 5. 主版本升级

主版本升级（MAJOR 版本）涉及破坏性变更，需要更严格的准备和验证。

### 5.1 数据库迁移 dry-run

> ⚠️ **主版本升级前，必须在测试环境完整演练！**

```bash
# 1. 在测试环境执行全流程
# 2. 确认迁移 SQL 可重复执行（幂等性）
# 3. 记录迁移耗时，评估维护窗口是否充足

# Oracle — 先在测试库执行并验证
sqlplus MEDKERNEL/PASSWORD@//TEST_DB:1521/TEST @$MK_HOME/db/oracle/medkernel_core_ddl_with_comments.sql

# PostgreSQL — 使用事务包裹测试
psql -h TEST_DB -U medkernel -d medkernel_test -v ON_ERROR_STOP=1 \
  -f $MK_HOME/db/postgres/medkernel_core_ddl_with_comments.sql
```

### 5.2 兼容性测试

| 测试项 | 验证方法 | 通过条件 |
|--------|---------|---------|
| API 兼容性 | HIS 对接接口回归测试 | 所有接口返回格式不变 |
| 数据完整性 | 核心业务表数据抽检 | 数据无丢失、无乱码 |
| 前端功能 | 关键页面操作验证 | 无白屏、无报错 |
| 性能基线 | 压测对比 | P95 延迟不超过基线 20% |

### 5.3 分阶段升级策略

```
阶段 1：测试环境验证（T-7 天）
  ├── 部署新版本到测试环境
  ├── 执行完整功能验证
  ├── 执行性能基线测试
  └── 确认数据库迁移脚本

阶段 2：预发布确认（T-3 天）
  ├── 变更审批通过
  ├── 通知相关方
  ├── 确认备份策略
  └── 确认回滚方案

阶段 3：生产环境升级（D-Day）
  ├── 执行数据库全量备份
  ├── 执行升级
  ├── 执行验证
  └── 确认或回滚

阶段 4：观察期（D+1 ~ D+7）
  ├── 监控系统指标
  ├── 收集用户反馈
  └── 确认稳定后归档
```

### 5.4 灰度发布（集群部署）

如有多节点集群，可采用灰度发布：

```
1. 升级节点 A → 验证 → 切流量到节点 A
2. 观察节点 A 运行 30 分钟
3. 升级节点 B → 验证 → 全量切流
4. 观察全量运行 1 小时
5. 确认稳定
```

---

## 6. 回滚操作

### 6.1 回滚决策树

```
升级后发现问题
  │
  ├─ 健康检查是否通过？
  │   ├─ 否 ──→ 立即回滚
  │   └─ 是 ──→ 继续评估
  │
  ├─ 核心功能是否正常？
  │   ├─ 否 ──→ 立即回滚
  │   └─ 是 ──→ 继续评估
  │
  ├─ 是否影响临床诊疗？
  │   ├─ 是 ──→ 立即回滚
  │   └─ 否 ──→ 继续评估
  │
  ├─ 是否有数据损坏风险？
  │   ├─ 是 ──→ 立即回滚 + 数据库恢复
  │   └─ 否 ──→ 继续观察
  │
  └─ 非核心功能异常？
      ├─ 是 ──→ 评估是否可容忍，否则回滚
      └─ 否 ──→ 升级成功，进入观察期
```

**必须立即回滚的情况：**

- 健康检查失败
- 核心 API 返回 5xx 错误
- 数据库连接失败
- 临床科室报告无法使用
- 数据完整性受损

### 6.2 使用 rollback.sh 自动回滚

```bash
# 回滚到最近一次备份
sudo /zoesoft/medkernel/scripts/rollback.sh --to last

# 回滚到指定备份
sudo /zoesoft/medkernel/scripts/rollback.sh --to 20260523_020000
```

**参数说明：**

| 参数 | 说明 | 示例 |
|------|------|------|
| `--to last` | 回滚到 `.last-backup` 记录的最近备份 | `--to last` |
| `--to <timestamp>` | 回滚到指定时间戳的备份 | `--to 20260523_020000` |

**自动回滚 4 步流程：**

```
步骤 1 ─ 停止服务
  └── systemctl stop medkernel

步骤 2 ─ 还原备份
  ├── 还原 lib/ frontend/ conf/ systemd/ nginx/
  ├── 还原 manifest.json
  └── 修正文件权限

步骤 3 ─ 重启服务
  ├── systemctl daemon-reload
  ├── systemctl start medkernel
  └── 等待 3 秒确认启动成功

步骤 4 ─ 健康检查
  └── 执行 healthcheck.sh
```

> ⚠️ **回滚窗口：升级后 5 分钟内回滚最安全。** 超过 5 分钟如有新数据写入，回滚可能导致数据丢失。

### 6.3 手动回滚步骤

```bash
# ========== 变量定义 ==========
MK_HOME=/zoesoft/medkernel
MK_BACKUP_DIR=/zoesoft/medkernel.bak
BACKUP_TS=20260523_020000  # 替换为实际备份时间戳

# ========== 步骤 1：停止服务 ==========
systemctl stop medkernel

# ========== 步骤 2：还原备份 ==========
for d in lib frontend conf systemd nginx; do
  if [ -d "$MK_BACKUP_DIR/$BACKUP_TS/$d" ]; then
    rm -rf "$MK_HOME/$d"
    cp -a "$MK_BACKUP_DIR/$BACKUP_TS/$d" "$MK_HOME/"
    echo "已还原 $d"
  fi
done
[ -f "$MK_BACKUP_DIR/$BACKUP_TS/manifest.json" ] && cp "$MK_BACKUP_DIR/$BACKUP_TS/manifest.json" "$MK_HOME/"
chown -R medkernel:medkernel $MK_HOME
chmod 600 $MK_HOME/conf/medkernel.env 2>/dev/null || true

# ========== 步骤 3：重启服务 ==========
systemctl daemon-reload
systemctl start medkernel
sleep 3
systemctl is-active --quiet medkernel || { echo "启动失败！查看：journalctl -u medkernel -n 100"; exit 1; }

# ========== 步骤 4：健康检查 ==========
/zoesoft/medkernel/scripts/healthcheck.sh
```

### 6.4 数据库回滚

> ⚠️ **重要：MedKernel 数据库迁移不支持自动回滚！** 需手动准备和执行回滚 SQL。

**回滚策略：**

| 升级类型 | 数据库回滚方式 |
|---------|-------------|
| 补丁升级 | 无需回滚（无 DDL 变更） |
| 次版本升级 | 通常无需回滚（仅新增列/表，不影响旧版本运行） |
| 主版本升级 | 需从备份恢复数据库 |

**从备份恢复数据库：**

```bash
# Oracle — 从 expdp 备份恢复
impdp MEDKERNEL/PASSWORD@//10.0.0.10:1521/ORCL \
  directory=BACKUP_DIR dumpfile=medkernel_pre_upgrade_20260523.dmp \
  logfile=medkernel_restore.log TABLE_EXISTS_ACTION=REPLACE

# PostgreSQL — 从 pg_dump 备份恢复
pg_restore -h localhost -U medkernel -d medkernel -c \
  /backup/medkernel_pre_upgrade_20260523.dump

# 达梦 — 从 dexp 备份恢复
dimp MEDKERNEL/PASSWORD@localhost:5236 \
  FILE=medkernel_pre_upgrade_20260523.dmp LOG=medkernel_restore.log
```

> ⚠️ **数据库恢复是高风险操作！** 必须在厂商技术支持指导下进行。

### 6.5 回滚验证

| 验证项 | 验证方法 | 通过条件 |
|--------|---------|---------|
| 服务状态 | `systemctl status medkernel` | active (running) |
| 健康检查 | `healthcheck.sh` | 3 项全部通过 |
| 版本号 | `cat /zoesoft/medkernel/manifest.json` | 回退到旧版本号 |
| 核心功能 | 关键 API 调用 | 返回正常 |
| 数据完整性 | 核心表数据抽检 | 数据无丢失 |

### 6.6 回滚后处理

```
1. 通知相关方：系统已回滚到 v____，功能恢复正常
2. 保留现场日志：cp /zoesoft/medkernel/logs/* /tmp/upgrade_failure_logs/
3. 记录故障现象和回滚原因
4. 联系厂商技术支持分析升级失败原因
5. 制定修复方案后再安排下次升级
```

---

## 7. 升级后验证

### 7.1 健康检查

```bash
# 1. 自动健康检查
/zoesoft/medkernel/scripts/healthcheck.sh

# 2. 服务状态
systemctl status medkernel

# 3. 版本确认
curl -s http://localhost:18080/medkernel/api/system/version

# 4. Actuator 健康检查
curl -s http://localhost:18081/medkernel/actuator/health | python3 -m json.tool

# 5. Provider 状态
curl -s http://localhost:18080/medkernel/api/system/providers | python3 -m json.tool
```

### 7.2 功能验证清单

| 序号 | 验证项 | API / 操作 | 预期结果 | 实际结果 | 通过 |
|------|--------|-----------|---------|---------|------|
| 1 | 系统登录 | 访问前端页面 | 登录页正常显示 | | □ |
| 2 | 健康检查 | `GET /api/health` | `"success": true` | | □ |
| 3 | Provider 状态 | `GET /api/system/providers` | database provider ready | | □ |
| 4 | 组织上下文 | `GET /api/system/org-context` | 返回组织结构 | | □ |
| 5 | 规则引擎 | `POST /api/rule-engine/evaluate` | 返回评估结果 | | □ |
| 6 | 路径管理 | `GET /api/pathways` | 返回路径列表 | | □ |
| 7 | 知识库 | `GET /api/knowledge` | 返回知识条目 | | □ |
| 8 | 配置包 | `GET /api/config-packages` | 返回配置列表 | | □ |
| 9 | 安全基线 | `GET /api/security-baseline` | 返回安全状态 | | □ |
| 10 | 审计日志 | `GET /api/audit-logs` | 返回日志记录 | | □ |

### 7.3 性能验证

升级后对比升级前性能基线：

```bash
# 查看 API P95 延迟
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,sum(rate(http_server_requests_seconds_bucket{application="medkernel-mvp"}[5m]))by(le))' | python3 -m json.tool

# 查看 JVM 堆使用率
curl -s 'http://localhost:9090/api/v1/query?query=sum(jvm_memory_used_bytes{application="medkernel-mvp",area="heap"})/clamp_min(sum(jvm_memory_max_bytes{application="medkernel-mvp",area="heap"}),1)' | python3 -m json.tool

# 查看数据库连接池使用率
curl -s 'http://localhost:9090/api/v1/query?query=max(hikaricp_connections_active{application="medkernel-mvp"}/clamp_min(hikaricp_connections_max{application="medkernel-mvp"},1))' | python3 -m json.tool
```

| 指标 | SLO 目标 | 升级前基线 | 升级后实测 | 是否达标 |
|------|---------|-----------|-----------|---------|
| API 可用性 | ≥ 99.5% | | | □ |
| API P95 延迟 | ≤ 500ms | | | □ |
| 5xx 错误率 | ≤ 0.1% | | | □ |
| JVM 堆使用率 | ≤ 75% | | | □ |
| 连接池饱和度 | ≤ 80% | | | □ |

### 7.4 监控确认

| 监控项 | 检查方式 | 确认 |
|--------|---------|------|
| Grafana 看板正常 | 访问 `http://<server>:3000` | □ |
| Prometheus 采集正常 | 访问 `http://<server>:9090/targets` | □ |
| Alertmanager 无误报 | 访问 `http://<server>:9093/#/alerts` | □ |
| 无 critical 告警 | 检查告警列表 | □ |
| Provider 指标正常 | Grafana Provider 状态面板 | □ |

---

## 8. 升级故障处理

### 8.1 升级中断恢复

**现象：** 升级脚本执行过程中断（网络断开、SSH 断开、脚本异常退出）

**处理步骤：**

```bash
# 1. 确认当前服务状态
systemctl status medkernel

# 2. 如果服务正在运行，先执行健康检查
/zoesoft/medkernel/scripts/healthcheck.sh

# 3. 如果服务已停止，判断中断位置：
#    - 步骤 1-2（备份/停止）后中断 → 安全，可重新执行升级
#    - 步骤 3-4（解压/覆盖）后中断 → 检查文件完整性后决定继续或回滚
#    - 步骤 5（数据库迁移）后中断 → 高风险，联系厂商
#    - 步骤 6-7（重启/检查）后中断 → 检查服务状态

# 4. 检查文件完整性
cat /zoesoft/medkernel/manifest.json  # 检查版本号
ls -la /zoesoft/medkernel/lib/medkernel.jar  # 检查 JAR 文件

# 5. 根据判断结果：继续升级 或 回滚
```

### 8.2 数据库迁移失败处理

**现象：** 数据库迁移 SQL 执行报错

**处理步骤：**

```bash
# 1. 查看具体错误信息
# Oracle
cat $MK_HOME/db/oracle/migration_error.log

# PostgreSQL
# psql 输出中的 ERROR 行

# 2. 常见错误与处理

# 错误：表已存在 (ORA-00955 / relation already exists)
# 原因：迁移脚本重复执行
# 处理：确认表结构是否完整，如完整则跳过此步

# 错误：列已存在 (ORA-01430 / column already exists)
# 原因：迁移脚本重复执行
# 处理：确认列是否完整，如完整则跳过此步

# 错误：权限不足 (ORA-01031 / permission denied)
# 原因：数据库用户权限不够
# 处理：联系 DBA 授予相应权限

# 错误：表空间不足 (ORA-01658 / no space left)
# 原因：数据库表空间已满
# 处理：联系 DBA 扩展表空间

# 3. 如无法快速解决 → 回滚应用，数据库保持原状
sudo /zoesoft/medkernel/scripts/rollback.sh --to last
```

### 8.3 服务启动失败处理

**现象：** `systemctl start medkernel` 后服务未运行

**处理步骤：**

```bash
# 1. 查看服务状态
systemctl status medkernel

# 2. 查看启动日志
journalctl -u medkernel -n 100 --no-pager

# 3. 查看应用日志
tail -200 /zoesoft/medkernel/logs/stderr.log
tail -200 /zoesoft/medkernel/logs/stdout.log

# 4. 常见启动失败原因

# 原因 1：端口被占用
ss -ltnp | grep 18080
# 处理：kill 占用进程或修改端口

# 原因 2：JDK 版本不对
java -version
# 处理：确认 JDK 1.8，检查 JAVA_HOME 配置

# 原因 3：数据库连接失败
# 日志关键字：HikariPool, Connection refused, ORA-12541
# 处理：检查数据库服务状态、网络连通、凭据配置

# 原因 4：配置文件错误
cat /zoesoft/medkernel/conf/medkernel.env
# 处理：检查配置项格式，对比 profile 模板

# 原因 5：内存不足
dmesg | grep -i "oom"
free -h
# 处理：增大 JAVA_OPTS 中 -Xmx 或释放内存

# 5. 如无法快速解决 → 回滚
sudo /zoesoft/medkernel/scripts/rollback.sh --to last
```

### 8.4 配置冲突处理

**现象：** 升级后服务启动报配置错误

**处理步骤：**

```bash
# 1. 对比新旧配置
diff /zoesoft/medkernel.bak/$(cat /zoesoft/medkernel/.last-backup | xargs basename)/conf/medkernel.env \
     /zoesoft/medkernel/conf/medkernel.env

# 2. 检查 application.yml
cat /zoesoft/medkernel/conf/application.yml

# 3. 常见配置冲突

# 冲突 1：新增必填配置项缺失
# 处理：从 profile 模板复制新配置项到 medkernel.env

# 冲突 2：配置值格式变更
# 处理：按新版本要求调整格式

# 冲突 3：配置文件权限错误
ls -la /zoesoft/medkernel/conf/medkernel.env
# 处理：chmod 600 /zoesoft/medkernel/conf/medkernel.env
#        chown medkernel:medkernel /zoesoft/medkernel/conf/medkernel.env

# 4. 修复后重启
systemctl restart medkernel
```

---

## 9. 升级记录模板

### 9.1 升级记录表

```
┌──────────────────────────────────────────────────────────────────┐
│                    MedKernel 升级记录                              │
├──────────────────────────────────────────────────────────────────┤
│ 记录编号：     UPG-____-____                                      │
│ 升级日期：     ____年__月__日                                      │
│ 执行人：       ____________                                       │
│ 监督人：       ____________                                       │
│                                                                  │
│ 当前版本：     v______.__.__  (git_hash: ________)               │
│ 目标版本：     v______.__.__  (git_hash: ________)               │
│ 升级类型：     □ 补丁升级  □ 次版本升级  □ 主版本升级              │
│                                                                  │
│ 维护窗口：     ____:____ ~ ____:____                              │
│ 实际开始：     ____:____                                          │
│ 实际结束：     ____:____                                          │
│ 实际停机时间：  ____ 分钟                                          │
│                                                                  │
│ 数据库迁移：   □ 无  □ 有（迁移脚本：__________）                  │
│ 前端更新：     □ 无  □ 有                                         │
│ 配置变更：     □ 无  □ 有（变更项：__________）                    │
│                                                                  │
│ 升级结果：     □ 成功  □ 失败（原因：__________）                  │
│ 是否回滚：     □ 否  □ 是（回滚到 v______.__.__）                 │
│                                                                  │
│ 备注：                                                           │
│ _______________________________________________________________  │
└──────────────────────────────────────────────────────────────────┘
```

### 9.2 变更清单

| 序号 | 变更项 | 变更前 | 变更后 | 备注 |
|------|--------|--------|--------|------|
| 1 | medkernel.jar | v1.0.0 (sha256:abc...) | v1.1.0 (sha256:def...) | |
| 2 | frontend/dist | v1.0.0 | v1.1.0 | |
| 3 | DDL 脚本 | — | org_context_migration.sql | |
| 4 | medkernel.env | — | 新增 MEDKERNEL_GRAPH_ENABLED | |
| 5 | application.yml | — | 新增 spring.kafka 配置 | |

### 9.3 验证结果

| 验证项 | 验证时间 | 结果 | 备注 |
|--------|---------|------|------|
| 健康检查 | __:__ | □ 通过 □ 失败 | |
| 版本号确认 | __:__ | □ 通过 □ 失败 | |
| 核心功能验证 | __:__ | □ 通过 □ 失败 | |
| 性能验证 | __:__ | □ 通过 □ 失败 | |
| 监控确认 | __:__ | □ 通过 □ 失败 | |
| 用户确认 | __:__ | □ 通过 □ 失败 | |

---

## 附录

### A. 关键路径速查

| 路径 | 说明 |
|------|------|
| `/zoesoft/medkernel/` | 应用主目录（MK_HOME） |
| `/zoesoft/medkernel/lib/medkernel.jar` | 后端 JAR |
| `/zoesoft/medkernel/frontend/dist/` | 前端静态资源 |
| `/zoesoft/medkernel/conf/medkernel.env` | 环境变量配置 |
| `/zoesoft/medkernel/conf/application.yml` | Spring Boot 配置 |
| `/zoesoft/medkernel/logs/` | 应用日志 |
| `/zoesoft/medkernel.bak/` | 备份目录 |
| `/zoesoft/medkernel/db/` | 数据库 DDL 脚本 |
| `/zoesoft/medkernel/scripts/` | 运维脚本 |
| `/zoesoft/medkernel/manifest.json` | 版本指纹 |
| `/etc/systemd/system/medkernel.service` | systemd 服务单元 |
| `/etc/nginx/conf.d/medkernel.conf` | Nginx 反向代理配置 |

### B. 常用命令速查

```bash
# 服务管理
systemctl start medkernel          # 启动
systemctl stop medkernel           # 停止
systemctl restart medkernel        # 重启
systemctl status medkernel         # 状态

# 健康检查
/zoesoft/medkernel/scripts/healthcheck.sh

# 环境检查
sudo /zoesoft/medkernel/scripts/check-env.sh

# 升级
sudo /zoesoft/medkernel/scripts/upgrade.sh --to v1.1.0 --migrate-db

# 回滚
sudo /zoesoft/medkernel/scripts/rollback.sh --to last

# 查看日志
journalctl -u medkernel -n 100 -f  # systemd 日志
tail -f /zoesoft/medkernel/logs/stderr.log  # 错误日志
tail -f /zoesoft/medkernel/logs/stdout.log  # 标准输出

# 查看版本
cat /zoesoft/medkernel/manifest.json
curl -s http://localhost:18080/medkernel/api/system/version

# 数据库连通测试
# Oracle
echo "SELECT 1 FROM DUAL;" | sqlplus MEDKERNEL/PASSWORD@//HOST:1521/ORCL
# PostgreSQL
PGPASSWORD=PASSWORD psql -h HOST -U medkernel -d medkernel -c 'SELECT 1;'
```

### C. 监控端点

| 端点 | 端口 | 用途 |
|------|------|------|
| `GET /medkernel/api/health` | 18080 | 业务健康检查 |
| `GET /medkernel/api/system/providers` | 18080 | Provider 状态 |
| `GET /medkernel/api/system/version` | 18080 | 版本信息 |
| `GET /medkernel/actuator/health` | 18081 | Spring Boot 健康检查 |
| `GET /medkernel/actuator/health/readiness` | 18081 | 就绪探针 |
| `GET /medkernel/actuator/prometheus` | 18081 | Prometheus 指标 |
| `GET /healthz` | 80/443 | 外部负载均衡器探测 |
