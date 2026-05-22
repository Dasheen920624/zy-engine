# MedKernel 医学知识平台 — 备份恢复手册

> **版本**：1.0 | **状态**：GA | **日期**：2026-05-23
>
> 本手册面向医院信息科运维人员，涵盖 MedKernel 平台的备份策略、恢复操作、演练规范等内容。

---

## 目录

1. [备份策略概述](#1-备份策略概述)
2. [数据库备份](#2-数据库备份)
3. [应用备份](#3-应用备份)
4. [配置备份](#4-配置备份)
5. [恢复操作](#5-恢复操作)
6. [备份验证与演练](#6-备份验证与演练)
7. [自动化备份配置](#7-自动化备份配置)

---

## 1. 备份策略概述

### 1.1 备份原则

- **全量 + 增量**：数据库采用全量备份 + 增量/归档日志，应用和配置采用全量备份
- **本地 + 异地**：备份数据至少存放在两个物理位置，防止单点故障导致备份丢失
- **加密存储**：含敏感信息（密钥、密码）的备份必须加密存储
- **定期验证**：每月验证备份可恢复性，季度进行灾备演练

### 1.2 灾备等级

| 等级 | 名称 | RPO | RTO | 备份频率 | 存储位置 | 适用场景 |
|------|------|-----|-----|---------|---------|---------|
| L1 | 本机备份 | 24h | 1h | 每日全量 | 同机不同磁盘 | 开发/测试环境 |
| L2 | 同机房备份 | 1h | 15min | 每小时增量 | 同机房不同服务器 | 小型医院 |
| L3 | 同城灾备 | 5min | 5min | 实时归档 + 5min 增量 | 同城异地机房 | 中型医院 |
| L4 | 异地灾备 | 15min | 30min | 15min 增量 + 异地同步 | 异地机房 | 大型医院/区域平台 |

> **RPO**（Recovery Point Objective）：最大可容忍数据丢失时间
> **RTO**（Recovery Time Objective）：最大可容忍服务中断时间

### 1.3 RPO/RTO 目标

| 组件 | RPO 目标 | RTO 目标 | 说明 |
|------|---------|---------|------|
| 数据库 | ≤ 1h（L2）/ ≤ 5min（L3） | ≤ 15min | 核心数据，优先恢复 |
| 应用 | 0（可从发布包重建） | ≤ 10min | JAR + 前端静态资源 |
| 配置 | ≤ 24h | ≤ 5min | 含密钥，需加密备份 |

---

## 2. 数据库备份

### 2.1 Oracle 备份

#### 2.1.1 RMAN 物理备份（推荐）

**全量备份脚本** `/zoesoft/medkernel/scripts/backup/oracle_rman_full.sh`：

```bash
#!/bin/bash
# Oracle RMAN 全量备份
# 用法：sudo -u oracle ./oracle_rman_full.sh

ORACLE_SID=ORCL
BACKUP_DIR=/backup/oracle/$(date +%Y%m%d)
RETENTION_DAYS=7

mkdir -p "$BACKUP_DIR"

rman target / <<EOF
RUN {
  ALLOCATE CHANNEL ch1 TYPE DISK FORMAT '${BACKUP_DIR}/full_%U';
  BACKUP DATABASE TAG 'MEDKERNEL_FULL' PLUS ARCHIVELOG TAG 'MEDKERNEL_ARCH';
  RELEASE CHANNEL ch1;
}
CROSSCHECK BACKUP;
DELETE NOPROMPT EXPIRED BACKUP;
DELETE NOPROMPT OBSOLETE RECOVERY WINDOW OF ${RETENTION_DAYS} DAYS;
EOF

echo "[$(date)] Oracle RMAN 全量备份完成：$BACKUP_DIR"
```

**增量备份脚本** `/zoesoft/medkernel/scripts/backup/oracle_rman_incr.sh`：

```bash
#!/bin/bash
# Oracle RMAN 增量备份（累积增量级别 1）
# 用法：sudo -u oracle ./oracle_rman_incr.sh

ORACLE_SID=ORCL
BACKUP_DIR=/backup/oracle/incr/$(date +%Y%m%d)

mkdir -p "$BACKUP_DIR"

rman target / <<EOF
RUN {
  ALLOCATE CHANNEL ch1 TYPE DISK FORMAT '${BACKUP_DIR}/incr_%U';
  BACKUP INCREMENTAL LEVEL 1 CUMULATIVE DATABASE TAG 'MEDKERNEL_INCR';
  BACKUP ARCHIVELOG ALL TAG 'MEDKERNEL_ARCH_INCR' DELETE INPUT;
  RELEASE CHANNEL ch1;
}
EOF

echo "[$(date)] Oracle RMAN 增量备份完成：$BACKUP_DIR"
```

#### 2.1.2 expdp 逻辑备份

```bash
#!/bin/bash
# Oracle expdp 逻辑备份
# 用法：sudo -u oracle ./oracle_expdp.sh

ORACLE_SID=ORCL
DUMP_DIR=/backup/oracle/expdp
DUMP_FILE=medkernel_$(date +%Y%m%d_%H%M%S).dmp
LOG_FILE=medkernel_$(date +%Y%m%d_%H%M%S).log

mkdir -p "$DUMP_DIR"

expdp MEDKERNEL/<密码>@//localhost:1521/ORCL \
  schemas=MEDKERNEL \
  directory=DATA_PUMP_DIR \
  dumpfile="$DUMP_FILE" \
  logfile="$LOG_FILE" \
  compression=all \
  parallel=4

echo "[$(date)] Oracle expdp 逻辑备份完成：$DUMP_DIR/$DUMP_FILE"
```

### 2.2 达梦 DM 备份

#### 2.2.1 dmbackup 物理备份

**全量备份脚本** `/zoesoft/medkernel/scripts/backup/dm_full.sh`：

```bash
#!/bin/bash
# 达梦 DM 全量备份
# 用法：sudo -u dmdba ./dm_full.sh

DM_HOME=/opt/dmdbms
BACKUP_DIR=/backup/dm/$(date +%Y%m%d)

mkdir -p "$BACKUP_DIR"

$DM_HOME/bin/dmbackup \
  PATH=$BACKUP_DIR/full \
  DATABASE=/opt/dmdbms/data/DAMENG/dm.ini \
  FULL=Y

echo "[$(date)] 达梦全量备份完成：$BACKUP_DIR/full"
```

**增量备份脚本** `/zoesoft/medkernel/scripts/backup/dm_incr.sh`：

```bash
#!/bin/bash
# 达梦 DM 增量备份
# 用法：sudo -u dmdba ./dm_incr.sh

DM_HOME=/opt/dmdbms
BACKUP_DIR=/backup/dm/incr/$(date +%Y%m%d)
BASE_DIR=/backup/dm/$(date -d "yesterday" +%Y%m%d)/full

mkdir -p "$BACKUP_DIR"

$DM_HOME/bin/dmbackup \
  PATH=$BACKUP_DIR/incr \
  DATABASE=/opt/dmdbms/data/DAMENG/dm.ini \
  FULL=N \
  BASE_PATH=$BASE_DIR

echo "[$(date)] 达梦增量备份完成：$BACKUP_DIR/incr"
```

#### 2.2.2 dexp 逻辑导出

```bash
#!/bin/bash
# 达梦 dexp 逻辑导出
# 用法：sudo -u dmdba ./dm_dexp.sh

DM_HOME=/opt/dmdbms
DUMP_DIR=/backup/dm/dexp
DUMP_FILE=medkernel_$(date +%Y%m%d_%H%M%S).dmp
LOG_FILE=medkernel_$(date +%Y%m%d_%H%M%S).log

mkdir -p "$DUMP_DIR"

$DM_HOME/bin/dexp \
  MEDKERNEL/<密码>@10.0.0.20:5236 \
  FILE=$DUMP_DIR/$DUMP_FILE \
  LOG=$DUMP_DIR/$LOG_FILE \
  OWNER=MEDKERNEL \
  COMPRESS=Y

echo "[$(date)] 达梦 dexp 逻辑导出完成：$DUMP_DIR/$DUMP_FILE"
```

### 2.3 PostgreSQL 备份

#### 2.3.1 pg_dump 逻辑备份

**全量备份脚本** `/zoesoft/medkernel/scripts/backup/pg_dump_full.sh`：

```bash
#!/bin/bash
# PostgreSQL pg_dump 全量备份
# 用法：./pg_dump_full.sh

PGHOST=10.0.0.30
PGPORT=5432
PGUSER=medkernel
PGDB=medkernel
BACKUP_DIR=/backup/postgresql/$(date +%Y%m%d)
DUMP_FILE=medkernel_$(date +%Y%m%d_%H%M%S).sql.gz

mkdir -p "$BACKUP_DIR"

PGPASSWORD="${MEDKERNEL_DB_PASSWORD}" pg_dump \
  -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDB" \
  --format=custom \
  --compress=9 \
  --verbose \
  -f "$BACKUP_DIR/medkernel_$(date +%Y%m%d_%H%M%S).dump"

# 同时导出纯 SQL 格式（可读，用于跨版本恢复）
PGPASSWORD="${MEDKERNEL_DB_PASSWORD}" pg_dump \
  -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDB" \
  --format=plain \
  --no-owner \
  --no-privileges \
  | gzip > "$BACKUP_DIR/$DUMP_FILE"

echo "[$(date)] PostgreSQL 全量备份完成：$BACKUP_DIR"
```

#### 2.3.2 pg_basebackup 物理备份

```bash
#!/bin/bash
# PostgreSQL pg_basebackup 物理备份
# 用法：./pg_basebackup_full.sh
# 前提：postgresql.conf 中 wal_level=replica

PGHOST=10.0.0.30
PGPORT=5432
PGUSER=medkernel
BACKUP_DIR=/backup/postgresql/base/$(date +%Y%m%d)

mkdir -p "$BACKUP_DIR"

PGPASSWORD="${MEDKERNEL_DB_PASSWORD}" pg_basebackup \
  -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" \
  -D "$BACKUP_DIR" \
  -Ft -z -P \
  --checkpoint=fast

echo "[$(date)] PostgreSQL 物理备份完成：$BACKUP_DIR"
```

#### 2.3.3 WAL 归档配置

在 `postgresql.conf` 中启用 WAL 归档：

```ini
wal_level = replica
archive_mode = on
archive_command = 'cp %p /backup/postgresql/wal_archive/%f'
```

### 2.4 KingbaseES 备份

KingbaseES 兼容 PostgreSQL 工具链，使用 `sys_dump` 替代 `pg_dump`：

```bash
#!/bin/bash
# KingbaseES sys_dump 全量备份
# 用法：./kingbase_dump_full.sh

KBHOME=/opt/Kingbase/ES/V8
KBHOST=10.0.0.40
KBPORT=54321
KBUSER=medkernel
KBDB=medkernel
BACKUP_DIR=/backup/kingbase/$(date +%Y%m%d)

mkdir -p "$BACKUP_DIR"

$KBHOME/bin/sys_dump \
  -h "$KBHOST" -p "$KBPORT" -U "$KBUSER" -d "$KBDB" \
  --format=custom \
  --compress=9 \
  -f "$BACKUP_DIR/medkernel_$(date +%Y%m%d_%H%M%S).dump"

echo "[$(date)] KingbaseES 全量备份完成：$BACKUP_DIR"
```

### 2.5 备份保留策略

| 备份类型 | 保留数量 | 保留时长 | 说明 |
|----------|---------|---------|------|
| 每日全量 | 7 份 | 7 天 | 滚动覆盖 |
| 每周全量 | 4 份 | 4 周 | 每周日全量 |
| 每月全量 | 12 份 | 12 个月 | 每月 1 日全量 |
| 增量备份 | 与全量配套 | 随全量清理 | 保留对应全量周期内的增量 |
| 逻辑导出 | 7 份 | 7 天 | 辅助恢复手段 |

**清理脚本示例**：

```bash
#!/bin/bash
# 清理过期备份
# 用法：./cleanup_backups.sh

BACKUP_ROOT=/backup

# 清理 7 天前的每日备份
find "$BACKUP_ROOT" -name "medkernel_*" -mtime +7 -type f -delete

# 清理空目录
find "$BACKUP_ROOT" -type d -empty -delete

echo "[$(date)] 过期备份清理完成"
```

---

## 3. 应用备份

### 3.1 备份内容

| 备份项 | 路径 | 说明 | 是否必须 |
|--------|------|------|---------|
| 后端 JAR | `$MK_HOME/lib/medkernel.jar` | Spring Boot 应用包 | 是 |
| 前端资源 | `$MK_HOME/frontend/dist/` | Vite 构建产物 | 是 |
| 配置文件 | `$MK_HOME/conf/` | 环境变量 + Spring 配置 | 是 |
| 数据库脚本 | `$MK_HOME/db/` | DDL / 迁移脚本 | 是 |
| 版本清单 | `$MK_HOME/manifest.json` | 版本信息 | 是 |

### 3.2 升级自动备份

`upgrade.sh` 脚本在升级时自动备份到 `$MK_HOME.bak/<timestamp>/`：

```bash
# 升级时自动备份的目录
lib/          # 后端 JAR
frontend/     # 前端资源
conf/         # 配置文件
systemd/      # systemd 服务文件
nginx/        # Nginx 配置
manifest.json # 版本清单
```

备份路径记录在 `$MK_HOME/.last-backup` 文件中，供 `rollback.sh` 使用。

### 3.3 手动备份命令

```bash
#!/bin/bash
# 手动全量备份
# 用法：sudo ./manual_backup.sh [--comment "升级前备份"]

MK_HOME=/zoesoft/medkernel
MK_BACKUP_DIR=/zoesoft/medkernel.bak
TS=$(date +%Y%m%d_%H%M%S)
COMMENT="${1:-manual}"

BACKUP_PATH="$MK_BACKUP_DIR/$TS"
mkdir -p "$BACKUP_PATH"

# 备份应用文件
for item in lib frontend conf db scripts systemd nginx docs profiles manifest.json CHANGELOG.md; do
  if [ -e "$MK_HOME/$item" ]; then
    cp -a "$MK_HOME/$item" "$BACKUP_PATH/"
    echo "[OK] 备份 $item"
  fi
done

# 记录备份元信息
cat > "$BACKUP_PATH/BACKUP_INFO" <<EOF
backup_time=$TS
backup_type=manual
comment=$COMMENT
hostname=$(hostname)
kernel=$(uname -r)
medkernel_version=$(grep -oE '"version":[[:space:]]*"[^"]+"' "$MK_HOME/manifest.json" 2>/dev/null || echo unknown)
EOF

# 生成校验文件
cd "$BACKUP_PATH"
find . -type f -exec sha256sum {} \; > CHECKSUMS.sha256

echo "[$(date)] 手动备份完成：$BACKUP_PATH"
```

### 3.4 备份验证 — checksum 校验

```bash
#!/bin/bash
# 验证备份完整性
# 用法：./verify_backup.sh <backup_path>

BACKUP_PATH="$1"
[ -d "$BACKUP_PATH" ] || { echo "备份目录不存在：$BACKUP_PATH"; exit 1; }
[ -f "$BACKUP_PATH/CHECKSUMS.sha256" ] || { echo "校验文件不存在"; exit 1; }

cd "$BACKUP_PATH"
sha256sum -c CHECKSUMS.sha256

if [ $? -eq 0 ]; then
  echo "[OK] 备份校验通过"
else
  echo "[FAIL] 备份校验失败，文件可能已损坏"
  exit 1
fi
```

---

## 4. 配置备份

### 4.1 需备份的配置文件

| 配置文件 | 路径 | 含密钥 | 加密要求 |
|----------|------|--------|---------|
| 环境变量 | `$MK_HOME/conf/medkernel.env` | **是** | 必须 AES-256/SM4 加密 |
| Spring Boot 配置 | `$MK_HOME/conf/application.yml` | 否 | 明文即可 |
| Spring Boot 本地覆盖 | `$MK_HOME/conf/application-local.yml` | 可能 | 视内容决定 |
| Nginx 配置 | `/etc/nginx/conf.d/medkernel.conf` | 否 | 明文即可 |
| Nginx TLS 配置 | `/etc/nginx/conf.d/medkernel-tls.conf` | 否 | 明文即可 |
| TLS 证书 | `/etc/nginx/ssl/medkernel.crt` | 否 | 明文 |
| TLS 私钥 | `/etc/nginx/ssl/medkernel.key` | **是** | 必须 AES-256/SM4 加密 |
| systemd 服务 | `/etc/systemd/system/medkernel.service` | 否 | 明文即可 |
| Prometheus 配置 | `deploy/monitoring/prometheus/` | 否 | 明文即可 |
| Alertmanager 配置 | `deploy/monitoring/alertmanager/alertmanager.yml` | 可能 | 视内容决定 |
| Grafana 配置 | `deploy/monitoring/grafana/` | 可能 | 视内容决定 |

### 4.2 配置备份脚本

```bash
#!/bin/bash
# 配置文件备份
# 用法：sudo ./backup_config.sh

MK_HOME=/zoesoft/medkernel
CONFIG_BACKUP_DIR=/backup/medkernel/config/$(date +%Y%m%d_%H%M%S)
mkdir -p "$CONFIG_BACKUP_DIR"

# 1. 应用配置
cp -a "$MK_HOME/conf/" "$CONFIG_BACKUP_DIR/conf/"

# 2. Nginx 配置
mkdir -p "$CONFIG_BACKUP_DIR/nginx"
cp -a /etc/nginx/conf.d/medkernel*.conf "$CONFIG_BACKUP_DIR/nginx/"
[ -d /etc/nginx/ssl ] && cp -a /etc/nginx/ssl/medkernel.* "$CONFIG_BACKUP_DIR/nginx/"

# 3. systemd 服务文件
mkdir -p "$CONFIG_BACKUP_DIR/systemd"
cp -a /etc/systemd/system/medkernel.service "$CONFIG_BACKUP_DIR/systemd/"

# 4. 监控配置
mkdir -p "$CONFIG_BACKUP_DIR/monitoring"
cp -a "$MK_HOME/monitoring/prometheus/" "$CONFIG_BACKUP_DIR/monitoring/prometheus/" 2>/dev/null || true
cp -a "$MK_HOME/monitoring/alertmanager/" "$CONFIG_BACKUP_DIR/monitoring/alertmanager/" 2>/dev/null || true
cp -a "$MK_HOME/monitoring/grafana/" "$CONFIG_BACKUP_DIR/monitoring/grafana/" 2>/dev/null || true

# 5. 加密含密钥的文件
# 使用 openssl AES-256-CBC 加密
ENCRYPTION_KEY="${BACKUP_ENCRYPTION_KEY:-MedKernel-Backup-2026}"

for secret_file in \
  "$CONFIG_BACKUP_DIR/conf/medkernel.env" \
  "$CONFIG_BACKUP_DIR/nginx/medkernel.key"; do
  if [ -f "$secret_file" ]; then
    openssl enc -aes-256-cbc -salt -pbkdf2 \
      -in "$secret_file" \
      -out "$secret_file.enc" \
      -pass "pass:$ENCRYPTION_KEY"
    rm -f "$secret_file"
    echo "[OK] 已加密：$secret_file → $secret_file.enc"
  fi
done

# 6. 生成校验文件
cd "$CONFIG_BACKUP_DIR"
find . -type f -exec sha256sum {} \; > CHECKSUMS.sha256

echo "[$(date)] 配置备份完成：$CONFIG_BACKUP_DIR"
echo "注意：含密钥文件已加密，恢复时需提供加密密码"
```

### 4.3 配置恢复解密

```bash
# 解密 medkernel.env
ENCRYPTION_KEY="${BACKUP_ENCRYPTION_KEY:-MedKernel-Backup-2026}"
openssl enc -aes-256-cbc -d -pbkdf2 \
  -in /backup/medkernel/config/<timestamp>/conf/medkernel.env.enc \
  -out /zoesoft/medkernel/conf/medkernel.env \
  -pass "pass:$ENCRYPTION_KEY"

# 解密 TLS 私钥
openssl enc -aes-256-cbc -d -pbkdf2 \
  -in /backup/medkernel/config/<timestamp>/nginx/medkernel.key.enc \
  -out /etc/nginx/ssl/medkernel.key \
  -pass "pass:$ENCRYPTION_KEY"

# 恢复权限
chmod 600 /zoesoft/medkernel/conf/medkernel.env
chmod 600 /etc/nginx/ssl/medkernel.key
chown medkernel:medkernel /zoesoft/medkernel/conf/medkernel.env
```

---

## 5. 恢复操作

### 5.1 Oracle 数据库恢复

#### 5.1.1 RMAN 完全恢复

```bash
# 1. 停止应用
sudo systemctl stop medkernel

# 2. 确定恢复时间点
# 查看备份列表
rman target / <<EOF
LIST BACKUP SUMMARY;
EOF

# 3. 执行恢复（恢复到最新状态）
rman target / <<EOF
RUN {
  SHUTDOWN IMMEDIATE;
  STARTUP MOUNT;
  RESTORE DATABASE;
  RECOVER DATABASE;
  ALTER DATABASE OPEN;
}
EOF

# 4. 恢复到指定时间点（PITR）
rman target / <<EOF
RUN {
  SHUTDOWN IMMEDIATE;
  STARTUP MOUNT;
  SET UNTIL TIME "TO_DATE('2026-05-23 14:00:00', 'YYYY-MM-DD HH24:MI:SS')";
  RESTORE DATABASE;
  RECOVER DATABASE;
  ALTER DATABASE OPEN RESETLOGS;
}
EOF

# 5. 验证数据
sqlplus MEDKERNEL/<密码>@//localhost:1521/ORCL -e "SELECT COUNT(*) FROM flyway_schema_history;"

# 6. 启动应用
sudo systemctl start medkernel
```

#### 5.1.2 expdp 逻辑恢复

```bash
# 1. 停止应用
sudo systemctl stop medkernel

# 2. 如需重建 schema
sqlplus / as sysdba <<EOF
DROP USER MEDKERNEL CASCADE;
CREATE USER MEDKERNEL IDENTIFIED BY <密码> DEFAULT TABLESPACE USERS;
GRANT CONNECT, RESOURCE, DBA TO MEDKERNEL;
EOF

# 3. 执行导入
impdp MEDKERNEL/<密码>@//localhost:1521/ORCL \
  schemas=MEDKERNEL \
  directory=DATA_PUMP_DIR \
  dumpfile=medkernel_20260523_080000.dmp \
  logfile=impdp_20260523.log \
  parallel=4

# 4. 验证并启动
sudo systemctl start medkernel
```

### 5.2 达梦 DM 数据库恢复

#### 5.2.1 dmrestore 物理恢复

```bash
# 1. 停止应用
sudo systemctl stop medkernel

# 2. 停止达梦服务
sudo systemctl stop DmServiceDMSERVER

# 3. 执行恢复
DM_HOME=/opt/dmdbms
$DM_HOME/bin/dmrestore \
  PATH=/backup/dm/20260523/full \
  DATABASE=/opt/dmdbms/data/DAMENG/dm.ini

# 4. 启动达梦服务
sudo systemctl start DmServiceDMSERVER

# 5. 验证并启动应用
disql MEDKERNEL/<密码>@10.0.0.20:5236 -e "SELECT COUNT(*) FROM flyway_schema_history;"
sudo systemctl start medkernel
```

#### 5.2.2 dimp 逻辑恢复

```bash
# 1. 停止应用
sudo systemctl stop medkernel

# 2. 执行导入
DM_HOME=/opt/dmdbms
$DM_HOME/bin/dimp \
  MEDKERNEL/<密码>@10.0.0.20:5236 \
  FILE=/backup/dm/dexp/medkernel_20260523_080000.dmp \
  LOG=/backup/dm/dexp/imp_20260523.log \
  OWNER=MEDKERNEL \
  IGNORE=Y

# 3. 验证并启动
sudo systemctl start medkernel
```

### 5.3 PostgreSQL 数据库恢复

#### 5.3.1 pg_restore 恢复（custom 格式）

```bash
# 1. 停止应用
sudo systemctl stop medkernel

# 2. 如需重建数据库
PGPASSWORD=<密码> psql -h 10.0.0.30 -U postgres -c "DROP DATABASE IF EXISTS medkernel;"
PGPASSWORD=<密码> psql -h 10.0.0.30 -U postgres -c "CREATE DATABASE medkernel OWNER medkernel;"

# 3. 执行恢复
PGPASSWORD=<密码> pg_restore \
  -h 10.0.0.30 -U medkernel -d medkernel \
  --verbose \
  --no-owner \
  --no-privileges \
  /backup/postgresql/20260523/medkernel_20260523_080000.dump

# 4. 验证
PGPASSWORD=<密码> psql -h 10.0.0.30 -U medkernel -d medkernel -c "SELECT COUNT(*) FROM flyway_schema_history;"

# 5. 启动应用
sudo systemctl start medkernel
```

#### 5.3.2 PITR 时间点恢复

```bash
# 1. 停止 PostgreSQL
sudo systemctl stop postgresql-16

# 2. 清空数据目录
rm -rf /var/lib/pgsql/16/data/*

# 3. 恢复基础备份
PGPASSWORD=<密码> pg_basebackup \
  -h 10.0.0.30 -p 5432 -U medkernel \
  -D /var/lib/pgsql/16/data \
  -Ft -z -P

# 4. 配置恢复目标
cat >> /var/lib/pgsql/16/data/postgresql.auto.conf <<EOF
restore_command = 'cp /backup/postgresql/wal_archive/%f %p'
recovery_target_time = '2026-05-23 14:00:00'
recovery_target_action = 'promote'
EOF

touch /var/lib/pgsql/16/data/recovery.signal

# 5. 启动 PostgreSQL（自动恢复到指定时间点）
sudo systemctl start postgresql-16

# 6. 验证并启动应用
sudo systemctl start medkernel
```

### 5.4 KingbaseES 数据库恢复

```bash
# 1. 停止应用
sudo systemctl stop medkernel

# 2. 恢复（使用 sys_restore，兼容 pg_restore）
KBHOME=/opt/Kingbase/ES/V8
$KBHOME/bin/sys_restore \
  -h 10.0.0.40 -p 54321 -U medkernel -d medkernel \
  --verbose \
  --no-owner \
  /backup/kingbase/20260523/medkernel_20260523_080000.dump

# 3. 验证并启动
sudo systemctl start medkernel
```

### 5.5 应用恢复

#### 5.5.1 使用 rollback.sh 恢复

```bash
# 回滚到最近一次自动备份
sudo $MK_HOME/scripts/rollback.sh --to last

# 回滚到指定备份
sudo $MK_HOME/scripts/rollback.sh --to 20260523_083000
```

#### 5.5.2 手动恢复应用

```bash
#!/bin/bash
# 手动恢复应用
# 用法：sudo ./restore_app.sh <backup_path>

BACKUP_PATH="$1"
MK_HOME=/zoesoft/medkernel

[ -d "$BACKUP_PATH" ] || { echo "备份目录不存在：$BACKUP_PATH"; exit 1; }

# 1. 停止服务
systemctl stop medkernel

# 2. 验证备份完整性
cd "$BACKUP_PATH"
if [ -f CHECKSUMS.sha256 ]; then
  sha256sum -c CHECKSUMS.sha256 || { echo "备份校验失败，中止恢复"; exit 1; }
fi

# 3. 恢复应用文件（保留 conf，避免覆盖凭据）
for d in lib frontend db scripts systemd nginx docs profiles; do
  if [ -d "$BACKUP_PATH/$d" ]; then
    rm -rf "$MK_HOME/$d"
    cp -a "$BACKUP_PATH/$d" "$MK_HOME/"
    echo "[OK] 恢复 $d"
  fi
done

# 4. 恢复版本清单
[ -f "$BACKUP_PATH/manifest.json" ] && cp "$BACKUP_PATH/manifest.json" "$MK_HOME/"

# 5. 修正权限
chown -R medkernel:medkernel "$MK_HOME"
chmod 600 "$MK_HOME/conf/medkernel.env" 2>/dev/null || true

# 6. 重启服务
systemctl daemon-reload
systemctl start medkernel
sleep 3

# 7. 健康检查
"$MK_HOME/scripts/healthcheck.sh"

echo "[$(date)] 应用恢复完成"
```

### 5.6 配置恢复

```bash
#!/bin/bash
# 配置文件恢复
# 用法：sudo ./restore_config.sh <config_backup_path>

CONFIG_BACKUP="$1"
MK_HOME=/zoesoft/medkernel
ENCRYPTION_KEY="${BACKUP_ENCRYPTION_KEY:-MedKernel-Backup-2026}"

[ -d "$CONFIG_BACKUP" ] || { echo "备份目录不存在"; exit 1; }

# 1. 恢复应用配置
# 如果 medkernel.env.enc 存在，先解密
if [ -f "$CONFIG_BACKUP/conf/medkernel.env.enc" ]; then
  openssl enc -aes-256-cbc -d -pbkdf2 \
    -in "$CONFIG_BACKUP/conf/medkernel.env.enc" \
    -out "$MK_HOME/conf/medkernel.env" \
    -pass "pass:$ENCRYPTION_KEY"
  chmod 600 "$MK_HOME/conf/medkernel.env"
  chown medkernel:medkernel "$MK_HOME/conf/medkernel.env"
  echo "[OK] 已解密恢复 medkernel.env"
else
  cp -a "$CONFIG_BACKUP/conf/" "$MK_HOME/conf/"
  echo "[OK] 已恢复 conf/"
fi

# 2. 恢复 Nginx 配置
cp -a "$CONFIG_BACKUP/nginx/"*.conf /etc/nginx/conf.d/
if [ -f "$CONFIG_BACKUP/nginx/medkernel.key.enc" ]; then
  openssl enc -aes-256-cbc -d -pbkdf2 \
    -in "$CONFIG_BACKUP/nginx/medkernel.key.enc" \
    -out /etc/nginx/ssl/medkernel.key \
    -pass "pass:$ENCRYPTION_KEY"
  chmod 600 /etc/nginx/ssl/medkernel.key
fi
nginx -t && systemctl reload nginx

# 3. 恢复 systemd 服务
cp -a "$CONFIG_BACKUP/systemd/medkernel.service" /etc/systemd/system/
systemctl daemon-reload

# 4. 恢复监控配置（如适用）
if [ -d "$CONFIG_BACKUP/monitoring" ]; then
  cp -a "$CONFIG_BACKUP/monitoring/" "$MK_HOME/monitoring/"
fi

echo "[$(date)] 配置恢复完成"
```

### 5.7 全量恢复流程（从零恢复整个系统）

> **适用场景**：服务器完全故障，需在新机器上恢复整个 MedKernel 系统。

#### 前置条件

- 新服务器已安装操作系统（与原服务器相同版本）
- 网络已配置，可访问数据库服务器
- 备份介质已挂载到新服务器

#### 恢复步骤

```
步骤 1：基础环境准备
  ├── 安装 JDK 1.8
  ├── 安装 Nginx
  ├── 创建 medkernel 用户
  └── 创建目录结构

步骤 2：恢复应用文件
  ├── 解压发布包到 $MK_HOME
  └── 或从备份恢复 lib/、frontend/、db/、scripts/ 等

步骤 3：恢复配置文件
  ├── 解密恢复 medkernel.env
  ├── 恢复 application.yml
  ├── 恢复 Nginx 配置
  └── 恢复 systemd 服务文件

步骤 4：恢复数据库
  ├── 按数据库类型执行恢复
  ├── 验证数据完整性
  └── 检查 flyway_schema_history

步骤 5：启动与验证
  ├── 注册并启动 systemd 服务
  ├── 运行健康检查
  ├── 验证业务功能
  └── 接入监控
```

**详细操作**：

```bash
#!/bin/bash
# MedKernel 全量恢复脚本
# 用法：sudo ./full_restore.sh

MK_HOME=/zoesoft/medkernel
MK_USER=medkernel
APP_BACKUP=/backup/medkernel/app/20260523_080000
CONFIG_BACKUP=/backup/medkernel/config/20260523_080000

# ========== 步骤 1：基础环境 ==========
echo "==> 步骤 1：基础环境准备"

# 创建用户
id "$MK_USER" >/dev/null 2>&1 || useradd -m -s /bin/bash "$MK_USER"

# 创建目录
mkdir -p "$MK_HOME"/{bin,conf,lib,frontend,db,logs,scripts,systemd,nginx,docs,profiles}
mkdir -p /zoesoft/medkernel.bak

# 安装 JDK（按实际环境调整）
# yum install -y java-1.8.0-openjdk   # CentOS
# 或解压毕昇 JDK 到 /opt/bisheng-jdk1.8.0_402  # ARM

# 安装 Nginx
# yum install -y nginx   # CentOS
# 或按国产 OS 包管理器安装

# ========== 步骤 2：恢复应用文件 ==========
echo "==> 步骤 2：恢复应用文件"

# 从备份恢复
cp -a "$APP_BACKUP/lib/" "$MK_HOME/"
cp -a "$APP_BACKUP/frontend/" "$MK_HOME/"
cp -a "$APP_BACKUP/db/" "$MK_HOME/"
cp -a "$APP_BACKUP/scripts/" "$MK_HOME/"
[ -f "$APP_BACKUP/manifest.json" ] && cp "$APP_BACKUP/manifest.json" "$MK_HOME/"

# ========== 步骤 3：恢复配置文件 ==========
echo "==> 步骤 3：恢复配置文件"

# 解密恢复 medkernel.env（需提供加密密码）
ENCRYPTION_KEY="${BACKUP_ENCRYPTION_KEY:?请设置 BACKUP_ENCRYPTION_KEY}"
if [ -f "$CONFIG_BACKUP/conf/medkernel.env.enc" ]; then
  openssl enc -aes-256-cbc -d -pbkdf2 \
    -in "$CONFIG_BACKUP/conf/medkernel.env.enc" \
    -out "$MK_HOME/conf/medkernel.env" \
    -pass "pass:$ENCRYPTION_KEY"
fi

# 恢复 Nginx 配置
cp -a "$CONFIG_BACKUP/nginx/"*.conf /etc/nginx/conf.d/
if [ -f "$CONFIG_BACKUP/nginx/medkernel.key.enc" ]; then
  mkdir -p /etc/nginx/ssl
  openssl enc -aes-256-cbc -d -pbkdf2 \
    -in "$CONFIG_BACKUP/nginx/medkernel.key.enc" \
    -out /etc/nginx/ssl/medkernel.key \
    -pass "pass:$ENCRYPTION_KEY"
  chmod 600 /etc/nginx/ssl/medkernel.key
fi
[ -f "$CONFIG_BACKUP/nginx/medkernel.crt" ] && cp "$CONFIG_BACKUP/nginx/medkernel.crt" /etc/nginx/ssl/

# 恢复 systemd 服务
cp -a "$CONFIG_BACKUP/systemd/medkernel.service" /etc/systemd/system/

# 修正权限
chown -R "$MK_USER:$MK_USER" "$MK_HOME"
chmod 600 "$MK_HOME/conf/medkernel.env"

# ========== 步骤 4：恢复数据库 ==========
echo "==> 步骤 4：恢复数据库（需按数据库类型手动执行）"
echo "请参考备份恢复手册 §5.1-5.4 执行数据库恢复"
echo "恢复完成后按 Enter 继续..."
read -r

# ========== 步骤 5：启动与验证 ==========
echo "==> 步骤 5：启动与验证"

# 注册并启动服务
systemctl daemon-reload
systemctl enable medkernel
systemctl start medkernel
sleep 5

# 健康检查
"$MK_HOME/scripts/healthcheck.sh"

# Nginx 测试与启动
nginx -t && systemctl enable nginx && systemctl start nginx

echo ""
echo "=========================================="
echo "  全量恢复完成"
echo "  请执行以下验证："
echo "  1. 浏览器访问 http://<IP> 确认前端正常"
echo "  2. 登录系统确认业务功能正常"
echo "  3. 检查 Grafana 监控数据是否恢复"
echo "  4. 确认告警通知渠道正常"
echo "=========================================="
```

---

## 6. 备份验证与演练

### 6.1 每月备份验证

每月至少执行一次备份可恢复性验证：

| 验证项 | 验证方法 | 通过标准 |
|--------|---------|---------|
| 数据库备份完整性 | checksum 校验 + 试恢复 | 校验通过，可正常恢复 |
| 应用备份完整性 | checksum 校验 + 试部署 | 校验通过，服务可启动 |
| 配置备份完整性 | checksum 校验 + 解密验证 | 校验通过，密钥文件可解密 |
| 备份可恢复性 | 在测试环境执行全量恢复 | 服务正常启动，健康检查通过 |
| 备份时效性 | 检查最新备份时间 | 最新备份不超过 24 小时 |

### 6.2 季度灾备演练

每季度至少执行一次灾备演练，验证从零恢复整个系统的能力。

#### 演练流程

```
1. 演练准备（1 天前）
   ├── 通知相关人员
   ├── 准备演练环境（独立测试服务器）
   └── 确认备份介质可用

2. 模拟故障（演练开始）
   ├── 模拟生产服务器完全故障
   └── 记录故障发生时间

3. 执行恢复
   ├── 按 §5.7 全量恢复流程执行
   ├── 记录每个步骤的耗时
   └── 记录遇到的问题

4. 验证恢复结果
   ├── 健康检查通过
   ├── 业务功能验证
   ├── 数据完整性验证
   └── 监控告警正常

5. 演练总结
   ├── 计算 RTO（从故障到恢复的时间）
   ├── 计算 RPO（数据丢失量）
   ├── 记录改进项
   └── 更新恢复文档
```

### 6.3 演练记录模板

```
┌──────────────────────────────────────────────────────────────┐
│                  MedKernel 灾备演练记录                       │
├──────────────────────────────────────────────────────────────┤
│ 演练编号：DR-2026-Q2-001                                     │
│ 演练日期：2026-__-__                                         │
│ 演练人员：__________  _________  _________                   │
│ 演练场景：[ ] 服务器故障  [ ] 数据库故障  [ ] 全系统故障     │
│                                                              │
│ ── 时间记录 ──                                               │
│ 故障模拟时间：____:____                                       │
│ 开始恢复时间：____:____                                       │
│ 服务恢复时间：____:____                                       │
│ 验证完成时间：____:____                                       │
│ 实际 RTO：____ 分钟（目标：≤ ____ 分钟）                      │
│ 实际 RPO：____ 分钟（目标：≤ ____ 分钟）                      │
│                                                              │
│ ── 恢复步骤记录 ──                                           │
│ 步骤 1：基础环境准备    耗时 ____ 分钟  [ ] 正常  [ ] 异常   │
│ 步骤 2：恢复应用文件    耗时 ____ 分钟  [ ] 正常  [ ] 异常   │
│ 步骤 3：恢复配置文件    耗时 ____ 分钟  [ ] 正常  [ ] 异常   │
│ 步骤 4：恢复数据库      耗时 ____ 分钟  [ ] 正常  [ ] 异常   │
│ 步骤 5：启动与验证      耗时 ____ 分钟  [ ] 正常  [ ] 异常   │
│                                                              │
│ ── 验证结果 ──                                               │
│ 健康检查：    [ ] 通过  [ ] 未通过                            │
│ 业务功能：    [ ] 正常  [ ] 异常                              │
│ 数据完整性：  [ ] 正常  [ ] 异常                              │
│ 监控告警：    [ ] 正常  [ ] 异常                              │
│                                                              │
│ ── 问题与改进 ──                                             │
│ 问题描述：_________________________________________          │
│ 改进措施：_________________________________________          │
│ 责任人：__________  截止日期：__________                      │
│                                                              │
│ ── 结论 ──                                                   │
│ 演练结果：[ ] 通过  [ ] 有条件通过  [ ] 未通过                │
│ 审核人：__________  审核日期：__________                      │
└──────────────────────────────────────────────────────────────┘
```

### 6.4 演练检查清单

| 序号 | 检查项 | 检查方法 | 预期结果 | 实际结果 |
|------|--------|---------|---------|---------|
| 1 | 备份文件可访问 | 检查备份存储 | 所有备份文件可读取 | |
| 2 | 备份文件完整 | sha256sum 校验 | 校验通过 | |
| 3 | 加密备份可解密 | openssl 解密 | 解密成功 | |
| 4 | 数据库可恢复 | 执行恢复操作 | 数据完整恢复 | |
| 5 | 应用可启动 | systemctl start | 服务正常运行 | |
| 6 | 健康检查通过 | healthcheck.sh | 全部端点正常 | |
| 7 | 业务功能正常 | 功能验证 | 核心业务可用 | |
| 8 | 监控数据正常 | Grafana 看板 | 指标正常采集 | |
| 9 | 告警通知正常 | 触发测试告警 | 通知正常送达 | |
| 10 | RTO 达标 | 计时 | ≤ 目标值 | |
| 11 | RPO 达标 | 数据比对 | ≤ 目标值 | |

---

## 7. 自动化备份配置

### 7.1 crontab 配置示例

在 `root` 用户 crontab 中配置定时备份任务：

```bash
# 编辑 crontab
sudo crontab -e
```

```crontab
# MedKernel 自动备份任务
# ─────────────────────────────────────────────

# 数据库每日全量备份（凌晨 2:00 执行）
0 2 * * * /zoesoft/medkernel/scripts/backup/db_full.sh >> /zoesoft/medkernel/logs/backup.log 2>&1

# 数据库每小时增量备份（仅 Oracle/PG）
0 * * * * /zoesoft/medkernel/scripts/backup/db_incr.sh >> /zoesoft/medkernel/logs/backup.log 2>&1

# 应用配置每日备份（凌晨 3:00 执行）
0 3 * * * /zoesoft/medkernel/scripts/backup/backup_config.sh >> /zoesoft/medkernel/logs/backup.log 2>&1

# 备份清理（每周日凌晨 4:00 执行）
0 4 * * 0 /zoesoft/medkernel/scripts/backup/cleanup_backups.sh >> /zoesoft/medkernel/logs/backup.log 2>&1

# 备份验证（每月 1 日凌晨 5:00 执行）
0 5 1 * * /zoesoft/medkernel/scripts/backup/verify_backups.sh >> /zoesoft/medkernel/logs/backup.log 2>&1
```

### 7.2 统一备份调度脚本

```bash
#!/bin/bash
# MedKernel 统一备份调度
# 用法：./medkernel_backup.sh [full|incr|config|cleanup|verify]
# 用法：添加到 crontab 实现自动调度

MK_HOME=/zoesoft/medkernel
BACKUP_SCRIPTS_DIR=$MK_HOME/scripts/backup
LOG_FILE=$MK_HOME/logs/backup.log
TS=$(date +%Y%m%d_%H%M%S)

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

ACTION="${1:-full}"

case "$ACTION" in
  full)
    log "开始数据库全量备份"
    "$BACKUP_SCRIPTS_DIR/db_full.sh" 2>&1 | tee -a "$LOG_FILE"
    if [ ${PIPESTATUS[0]} -eq 0 ]; then
      log "[OK] 数据库全量备份成功"
    else
      log "[FAIL] 数据库全量备份失败"
      # 发送告警通知
      curl -s -X POST "http://localhost:5001/alerts/backup" \
        -H "Content-Type: application/json" \
        -d "{\"alert\":\"backup_failed\",\"type\":\"db_full\",\"time\":\"$TS\"}" || true
    fi
    ;;

  incr)
    log "开始数据库增量备份"
    "$BACKUP_SCRIPTS_DIR/db_incr.sh" 2>&1 | tee -a "$LOG_FILE"
    ;;

  config)
    log "开始配置文件备份"
    "$BACKUP_SCRIPTS_DIR/backup_config.sh" 2>&1 | tee -a "$LOG_FILE"
    ;;

  cleanup)
    log "开始清理过期备份"
    "$BACKUP_SCRIPTS_DIR/cleanup_backups.sh" 2>&1 | tee -a "$LOG_FILE"
    ;;

  verify)
    log "开始备份验证"
    "$BACKUP_SCRIPTS_DIR/verify_backups.sh" 2>&1 | tee -a "$LOG_FILE"
    ;;

  *)
    echo "用法：$0 [full|incr|config|cleanup|verify]"
    exit 1
    ;;
esac
```

### 7.3 备份监控和告警

#### 7.3.1 备份状态检查脚本

```bash
#!/bin/bash
# 备份状态检查
# 用法：./check_backup_status.sh
# 可集成到 Prometheus node_exporter textfile collector

BACKUP_ROOT=/backup
STATUS_FILE=/var/lib/node_exporter/textfile/medkernel_backup.prom
ALERT=0

# 检查最新数据库备份时间
LATEST_DB_BACKUP=$(find "$BACKUP_ROOT" -name "medkernel_*" -type f -mmin -1440 2>/dev/null | head -1)
if [ -z "$LATEST_DB_BACKUP" ]; then
  echo "[WARN] 最近 24 小时无数据库备份"
  ALERT=1
fi

# 检查最新配置备份时间
LATEST_CFG_BACKUP=$(find "$BACKUP_ROOT" -path "*/config/*" -type d -mmin -1440 2>/dev/null | head -1)
if [ -z "$LATEST_CFG_BACKUP" ]; then
  echo "[WARN] 最近 24 小时无配置备份"
  ALERT=1
fi

# 检查磁盘空间
BACKUP_USAGE=$(df "$BACKUP_ROOT" | awk 'NR==2 {print $5}' | tr -d '%')
if [ "$BACKUP_USAGE" -gt 85 ]; then
  echo "[WARN] 备份磁盘使用率 ${BACKUP_USAGE}%，超过 85%"
  ALERT=1
fi

# 输出 Prometheus 指标
mkdir -p /var/lib/node_exporter/textfile
cat > "$STATUS_FILE" <<EOF
# HELP medkernel_backup_status Backup status (0=ok, 1=alert)
# TYPE medkernel_backup_status gauge
medkernel_backup_status ${ALERT}
# HELP medkernel_backup_disk_usage_percent Backup disk usage percentage
# TYPE medkernel_backup_disk_usage_percent gauge
medkernel_backup_disk_usage_percent ${BACKUP_USAGE}
EOF

echo "[$(date)] 备份状态检查完成：ALERT=$ALERT, DISK_USAGE=${BACKUP_USAGE}%"
```

#### 7.3.2 Prometheus 告警规则（备份专用）

在 `medkernel-alert-rules.yml` 中追加：

```yaml
  # ── 备份告警 ──────────────────────────────────────────
  - name: medkernel-backup
    interval: 60m
    rules:
      - alert: MedKernelBackupFailed
        expr: medkernel_backup_status == 1
        for: 1h
        labels:
          severity: warning
          service: medkernel-mvp
        annotations:
          summary: MedKernel backup check failed
          description: "Backup status check has been failing for 1 hour. Check backup logs."

      - alert: MedKernelBackupDiskSpaceWarning
        expr: medkernel_backup_disk_usage_percent > 85
        for: 1h
        labels:
          severity: warning
          service: medkernel-mvp
        annotations:
          summary: MedKernel backup disk usage is above 85%
          description: "Backup storage usage is {{ $value }}%. Consider cleanup or expansion."
```

### 7.4 备份存储空间管理

| 备份类型 | 预估单次大小 | 每日增量 | 月度总量 | 建议磁盘 |
|----------|------------|---------|---------|---------|
| Oracle 全量 | 5-20 GB | 1-5 GB | 50-200 GB | 500 GB |
| DM 全量 | 3-15 GB | 0.5-3 GB | 30-100 GB | 300 GB |
| PostgreSQL 全量 | 2-10 GB | 0.5-2 GB | 20-80 GB | 200 GB |
| 应用备份 | 500 MB-2 GB | — | 5-10 GB | 50 GB |
| 配置备份 | < 10 MB | — | < 100 MB | 1 GB |

> **空间监控**：建议在 Prometheus 中配置备份磁盘使用率告警，超过 85% 时 Warning，超过 95% 时 Critical。

---

## 附录 A：备份文件命名规范

| 类型 | 命名格式 | 示例 |
|------|---------|------|
| 数据库全量 | `medkernel_YYYYMMDD_HHMMSS.{dump,dmp,sql.gz}` | `medkernel_20260523_020000.dump` |
| 数据库增量 | `medkernel_incr_YYYYMMDD_HHMMSS.*` | `medkernel_incr_20260523_030000.arch` |
| 应用备份 | 目录 `YYYYMMDD_HHMMSS/` | `20260523_080000/` |
| 配置备份 | 目录 `config/YYYYMMDD_HHMMSS/` | `config/20260523_030000/` |

## 附录 B：备份脚本目录结构

```
$MK_HOME/scripts/backup/
├── db_full.sh              # 数据库全量备份（按 dialect 自动选择）
├── db_incr.sh              # 数据库增量备份
├── oracle_rman_full.sh     # Oracle RMAN 全量
├── oracle_rman_incr.sh     # Oracle RMAN 增量
├── oracle_expdp.sh         # Oracle expdp 逻辑导出
├── dm_full.sh              # 达梦全量备份
├── dm_incr.sh              # 达梦增量备份
├── dm_dexp.sh              # 达梦 dexp 逻辑导出
├── pg_dump_full.sh         # PostgreSQL pg_dump 全量
├── pg_basebackup_full.sh   # PostgreSQL 物理备份
├── kingbase_dump_full.sh   # KingbaseES 全量备份
├── backup_config.sh        # 配置文件备份
├── manual_backup.sh        # 手动全量备份
├── verify_backup.sh        # 备份校验
├── cleanup_backups.sh      # 过期备份清理
├── check_backup_status.sh  # 备份状态检查
└── medkernel_backup.sh     # 统一备份调度
```

## 附录 C：恢复操作速查卡

| 故障场景 | 恢复方法 | 预计耗时 | 数据丢失 |
|----------|---------|---------|---------|
| 应用版本回退 | `rollback.sh --to last` | 5 分钟 | 无 |
| 应用 JAR 损坏 | 从备份恢复 `lib/medkernel.jar` | 5 分钟 | 无 |
| 前端资源损坏 | 从备份恢复 `frontend/dist/` | 3 分钟 | 无 |
| 配置文件误改 | 从配置备份恢复 | 5 分钟 | 无 |
| 数据库误操作（有归档） | PITR 时间点恢复 | 30-60 分钟 | ≤ RPO |
| 数据库误操作（无归档） | 最近全量恢复 | 15-30 分钟 | ≤ 24h |
| 数据库 Schema 损坏 | expdp/dexp 逻辑恢复 | 30-60 分钟 | ≤ 24h |
| 全系统故障 | 全量恢复流程 | 2-4 小时 | ≤ RPO |
