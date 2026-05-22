# MedKernel 故障应急手册

> 版本：1.0 | 适用：MedKernel v1.x GA | 日期：2026-05-23
>
> **本文档面向医院信息科运维人员，用于快速定位和处置 MedKernel 系统故障。**
>
> ⚠️ **P0/P1 故障请同时通知信息科主任和厂商技术支持！**

---

## 目录

1. [故障分级](#1-故障分级)
2. [应急响应流程](#2-应急响应流程)
3. [常见故障处置](#3-常见故障处置)
4. [应急预案](#4-应急预案)
5. [故障复盘](#5-故障复盘)
6. [应急联系方式](#6-应急联系方式)
7. [应急工具箱](#7-应急工具箱)

---

## 1. 故障分级

### 1.1 分级定义

| 级别 | 名称 | 定义 | 影响范围 | 示例 |
|------|------|------|---------|------|
| **P0** | 紧急 | 系统完全不可用 | 全院诊疗受影响 | 服务宕机、数据库崩溃、全院无法访问 |
| **P1** | 严重 | 核心功能不可用 | 部分科室受影响 | 规则引擎无法评估、数据库连接池耗尽、AI 模型网关故障 |
| **P2** | 一般 | 非核心功能异常 | 不影响诊疗流程 | 知识图谱功能降级、通知发送延迟、报表生成缓慢 |
| **P3** | 轻微 | 界面/性能轻微问题 | 几乎无影响 | 页面样式错乱、偶发延迟、非关键日志报错 |

### 1.2 分级决策树

```
发现异常
  │
  ├─ 系统是否完全不可访问？
  │   ├─ 是 ──→ P0 紧急
  │   └─ 否 ──→ 继续
  │
  ├─ 核心功能（规则评估/路径管理/知识库）是否不可用？
  │   ├─ 是 ──→ P1 严重
  │   └─ 否 ──→ 继续
  │
  ├─ 是否影响临床诊疗流程？
  │   ├─ 是 ──→ 升级为 P1
  │   └─ 否 ──→ 继续
  │
  ├─ 非核心功能是否异常？
  │   ├─ 是 ──→ P2 一般
  │   └─ 否 ──→ 继续
  │
  └─ 仅为界面/性能轻微问题？
      ├─ 是 ──→ P3 轻微
      └─ 否 ──→ 重新评估
```

### 1.3 故障与告警映射

| 告警名称 | 严重级别 | 对应故障等级 | 说明 |
|---------|---------|------------|------|
| MedKernelBackendDown | critical | P0 | 后端服务不可达 |
| MedKernelDatabaseProviderDown | critical | P0/P1 | 数据库 Provider 不可用 |
| MedKernelModelGatewayDown | critical | P1 | AI 模型网关故障 |
| MedKernelApi5xxRateCritical | critical | P1 | 5xx 错误率 > 2% |
| MedKernelJvmHeapCritical | critical | P1 | JVM 堆使用率 > 90% |
| MedKernelDiskSpaceCritical | critical | P0 | 磁盘使用率 > 95% |
| MedKernelSLOAvailabilityBreach | critical | P1 | 可用性低于 99.5% |
| MedKernelApiP95LatencyCritical | critical | P1 | P95 延迟 > 3s |
| MedKernelHikariPoolSaturation | warning | P2 | 连接池饱和度 > 80% |
| MedKernelApiP95LatencyHigh | warning | P2 | P95 延迟 > 1s |
| MedKernelApi5xxRateHigh | warning | P2 | 5xx 错误率 > 0.5% |
| MedKernelJvmHeapHigh | warning | P2 | JVM 堆使用率 > 75% |
| MedKernelGraphProviderDegraded | warning | P2 | 图谱 Provider 降级 |
| MedKernelDiskSpaceWarning | warning | P2 | 磁盘使用率 > 85% |
| MedKernelCpuLoadHigh | warning | P2 | CPU 使用率 > 80% |
| MedKernelFileDescriptorHigh | warning | P2 | 文件描述符使用率 > 80% |
| MedKernelSLOLatencyBreach | warning | P2 | SLO 延迟违约 |

---

## 2. 应急响应流程

### 2.1 响应流程总览

```
发现故障 → 分级判定 → 启动响应 → 故障处置 → 恢复确认 → 故障复盘
```

| 阶段 | 负责人 | 关键动作 | 产出 |
|------|--------|---------|------|
| **发现** | 值班人员/监控系统 | 确认告警真实性 | 故障确认 |
| **分级** | 值班人员 | 按决策树判定等级 | 故障等级 |
| **响应** | 值班人员 | 通知相关方、启动处置 | 响应记录 |
| **处置** | 运维人员/厂商 | 排查根因、执行修复 | 处置记录 |
| **恢复** | 运维人员 | 验证功能恢复 | 恢复确认 |
| **复盘** | 团队 | 根因分析、改进措施 | 复盘报告 |

### 2.2 响应时间要求

| 故障等级 | 响应时间 | 处置时间 | 通知范围 |
|---------|---------|---------|---------|
| **P0 紧急** | **5 分钟内** | 30 分钟内 | 信息科主任 + 厂商值班 + 临床科室 |
| **P1 严重** | **15 分钟内** | 2 小时内 | 信息科负责人 + 厂商技术支持 |
| **P2 一般** | **1 小时内** | 8 小时内 | 信息科值班 + 厂商工单 |
| **P3 轻微** | **4 小时内** | 24 小时内 | 厂商工单 |

### 2.3 通知机制

#### P0/P1 通知

```
┌──────────────────────────────────────────────────────────┐
│              P0/P1 故障通知模板                            │
├──────────────────────────────────────────────────────────┤
│ 【紧急/P0】MedKernel 系统故障通知                          │
│                                                          │
│ 故障时间：   ____年__月__日 __:__                         │
│ 故障等级：   P0/P1                                       │
│ 故障现象：   ______________________________________       │
│ 影响范围：   ______________________________________       │
│ 当前状态：   □ 排查中  □ 处置中  □ 已恢复                  │
│ 预计恢复：   ____:____                                   │
│ 联系人：     ____________  电话：___________              │
│                                                          │
│ 请相关人员立即响应！                                       │
└──────────────────────────────────────────────────────────┘
```

**通知对象：**

| 对象 | P0 | P1 | P2 | P3 |
|------|----|----|----|----|
| 信息科主任 | ✅ 电话 | ✅ 电话 | — | — |
| 信息科值班 | ✅ 电话 | ✅ 即时通讯 | ✅ 即时通讯 | — |
| 厂商技术支持 | ✅ 电话 | ✅ 电话 | ✅ 工单 | ✅ 工单 |
| 临床科室 | ✅ 公告 | ✅ 即时通讯 | — | — |
| 数据库管理员 | ✅ 视情况 | ✅ 视情况 | — | — |

### 2.4 升级机制

```
故障发生 → 值班人员响应
  │
  ├─ 30 分钟未恢复 → 升级到信息科负责人
  │
  ├─ 1 小时未恢复 → 升级到信息科主任
  │
  └─ 2 小时未恢复 → 升级到院领导
```

---

## 3. 常见故障处置

### 3.1 服务不可用

#### 现象

- 健康检查失败：`healthcheck.sh` 返回错误
- 页面无法访问：浏览器显示无法连接
- 告警：`MedKernelBackendDown`

#### 排查步骤

```bash
# 1. 检查服务状态
systemctl status medkernel

# 2. 检查端口监听
ss -ltnp | grep 18080

# 3. 检查进程
ps aux | grep medkernel

# 4. 查看应用日志
tail -200 /zoesoft/medkernel/logs/stderr.log
tail -200 /zoesoft/medkernel/logs/stdout.log

# 5. 查看 systemd 日志
journalctl -u medkernel -n 200 --no-pager

# 6. 检查磁盘空间
df -h /zoesoft

# 7. 检查内存
free -h

# 8. 检查 Nginx 状态
systemctl status nginx
ss -ltnp | grep :80
```

#### 处置方案

| 优先级 | 方案 | 命令 | 适用场景 |
|--------|------|------|---------|
| 1 | **重启服务** | `systemctl restart medkernel` | 进程异常退出、OOM 重启后 |
| 2 | **回滚版本** | `sudo /zoesoft/medkernel/scripts/rollback.sh --to last` | 升级后出现问题 |
| 3 | **降级运行** | 关闭非核心 Provider，仅保留数据库 | 资源不足、部分功能异常 |

**重启后验证：**

```bash
# 等待 3 秒
sleep 3

# 检查服务状态
systemctl is-active medkernel

# 执行健康检查
/zoesoft/medkernel/scripts/healthcheck.sh

# 检查版本
curl -s http://localhost:18080/medkernel/api/system/version
```

---

### 3.2 数据库连接失败

#### 现象

- API 返回 500 错误
- 日志出现 `HikariPool` 错误：`Unable to acquire JDBC connection`
- 告警：`MedKernelDatabaseProviderDown`、`MedKernelHikariPoolSaturation`

#### 排查步骤

```bash
# 1. 检查数据库服务状态
# Oracle
systemctl status oracle  # 或 lsnrctl status

# PostgreSQL
systemctl status postgresql
pg_isready -h localhost -p 5432

# 达梦
systemctl status DmServiceDMSERVER

# 2. 检查网络连通
ping <数据库服务器IP>
telnet <数据库服务器IP> <端口>

# 3. 检查连接配置
cat /zoesoft/medkernel/conf/medkernel.env | grep MEDKERNEL_DB

# 4. 测试数据库连接
# Oracle
echo "SELECT 1 FROM DUAL;" | sqlplus MEDKERNEL/PASSWORD@//HOST:1521/ORCL

# PostgreSQL
PGPASSWORD=PASSWORD psql -h HOST -U medkernel -d medkernel -c 'SELECT 1;'

# 5. 检查连接池状态
curl -s http://localhost:18081/medkernel/actuator/prometheus | grep hikaricp_connections

# 6. 检查数据库连接数
# Oracle
sqlplus / as sysdba -c "SELECT COUNT(*) FROM V\$SESSION WHERE USERNAME='MEDKERNEL';"

# PostgreSQL
psql -h HOST -U postgres -c "SELECT COUNT(*) FROM pg_stat_activity WHERE datname='medkernel';"
```

#### 处置方案

| 原因 | 处置 | 命令/操作 |
|------|------|---------|
| 数据库服务停止 | 重启数据库 | `systemctl start oracle` / `systemctl start postgresql` |
| 网络不通 | 检查网络/防火墙 | `ping` / `telnet` / `firewall-cmd` |
| 连接池耗尽 | 清理长事务 + 增大连接池 | 见下方详细步骤 |
| 密码过期 | 联系 DBA 重置密码 | 修改 `medkernel.env` 中 `MEDKERNEL_DB_PASSWORD` |
| 监听器异常 | 重启监听 | Oracle: `lsnrctl start` |

**连接池耗尽处理：**

```bash
# 1. 查看当前活跃连接
# Oracle
sqlplus / as sysdba -c "SELECT SID, SERIAL#, STATUS, LAST_CALL_ET FROM V\$SESSION WHERE USERNAME='MEDKERNEL' AND STATUS='ACTIVE';"

# PostgreSQL
psql -h HOST -U postgres -c "SELECT pid, state, query_start, query FROM pg_stat_activity WHERE datname='medkernel' AND state='active';"

# 2. 清理长事务（谨慎操作！）
# Oracle
sqlplus / as sysdba -c "ALTER SYSTEM KILL SESSION 'SID,SERIAL#';"

# PostgreSQL
psql -h HOST -U postgres -c "SELECT pg_terminate_backend(PID);"

# 3. 增大连接池（修改 medkernel.env）
# 编辑 /zoesoft/medkernel/conf/medkernel.env
# 添加或修改：MEDKERNEL_DB_POOL_MAX=30  （默认 20，建议不超过 50）

# 4. 重启应用
systemctl restart medkernel
```

---

### 3.3 内存溢出

#### 现象

- 日志出现 `java.lang.OutOfMemoryError`
- 服务自动重启（systemd Restart=on-failure）
- 生成 heap dump 文件：`/zoesoft/medkernel/logs/heap.hprof`
- 告警：`MedKernelJvmHeapCritical`

#### 排查步骤

```bash
# 1. 检查 JVM 堆使用率
curl -s http://localhost:18081/medkernel/actuator/prometheus | grep jvm_memory_used_bytes | grep heap

# 2. 检查当前 JVM 参数
cat /zoesoft/medkernel/conf/medkernel.env | grep JAVA_OPTS
# 或
cat /etc/systemd/system/medkernel.service | grep JAVA_OPTS

# 3. 检查 heap dump 文件
ls -lh /zoesoft/medkernel/logs/heap.hprof

# 4. 检查 GC 日志（如已配置）
ls -lh /zoesoft/medkernel/logs/gc*.log

# 5. 检查系统内存
free -h

# 6. 检查 OOM killer
dmesg | grep -i "oom" | tail -20
```

#### 处置方案

| 优先级 | 方案 | 操作 |
|--------|------|------|
| 1 | **增大堆内存** | 修改 `JAVA_OPTS` 中 `-Xmx` 值 |
| 2 | **重启服务** | `systemctl restart medkernel` |
| 3 | **分析 heap dump** | 将 heap.hprof 发送给厂商分析内存泄漏 |

**增大堆内存：**

```bash
# 编辑配置文件
vi /zoesoft/medkernel/conf/medkernel.env

# 修改 JAVA_OPTS（根据系统可用内存调整）
# 默认：-Xms2g -Xmx4g
# 建议：-Xms4g -Xmx6g（需系统可用内存 ≥ 8GB）

# 重启服务
systemctl restart medkernel

# 验证 JVM 参数生效
ps aux | grep medkernel | grep Xmx
```

> ⚠️ **-Xmx 不超过系统可用内存的 75%！** systemd 服务已配置 `MemoryMax=8G`，如需更大堆内存需同步修改 service 文件。

---

### 3.4 磁盘空间不足

#### 现象

- 写入失败，日志报错 `No space left on device`
- 告警：`MedKernelDiskSpaceWarning`（> 85%）或 `MedKernelDiskSpaceCritical`（> 95%）

#### 排查步骤

```bash
# 1. 查看磁盘使用
df -h

# 2. 查看大目录
du -sh /zoesoft/medkernel/* | sort -rh | head -10

# 3. 查看日志目录大小
du -sh /zoesoft/medkernel/logs/

# 4. 查看备份目录大小
du -sh /zoesoft/medkernel.bak/

# 5. 查找大文件（> 100MB）
find /zoesoft -type f -size +100M -exec ls -lh {} \; 2>/dev/null

# 6. 查看日志文件
ls -lhS /zoesoft/medkernel/logs/
```

#### 处置方案

| 优先级 | 方案 | 命令 | 释放空间 |
|--------|------|------|---------|
| 1 | **清理旧日志** | `find /zoesoft/medkernel/logs/ -name "*.log" -mtime +30 -delete` | 数 GB |
| 2 | **清理旧备份** | `find /zoesoft/medkernel.bak/ -maxdepth 1 -type d -mtime +30 -exec rm -rf {} \;` | 数 GB |
| 3 | **清理 heap dump** | `rm -f /zoesoft/medkernel/logs/heap.hprof`（已分析后） | 数百 MB-数 GB |
| 4 | **清理临时文件** | `rm -rf /tmp/medkernel-*` | 数百 MB |
| 5 | **归档到异地** | 将旧日志/备份打包转移到归档存储 | 视情况 |
| 6 | **磁盘扩容** | 联系系统管理员扩容 | 长期方案 |

**安全清理脚本：**

```bash
#!/bin/bash
# 安全清理磁盘空间（保留最近 7 天日志和最近 2 次备份）

# 清理 30 天前的日志
find /zoesoft/medkernel/logs/ -name "*.log" -mtime +30 -delete
find /zoesoft/medkernel/logs/ -name "*.hprof" -mtime +7 -delete

# 清理旧备份（保留最近 2 次）
cd /zoesoft/medkernel.bak/
ls -dt */ | tail -n +3 | xargs rm -rf

# 清理临时文件
find /tmp/ -name "medkernel-*" -mtime +1 -delete 2>/dev/null

echo "清理完成"
df -h /zoesoft
```

---

### 3.5 告警风暴

#### 现象

- 大量告警同时触发，告警通知泛滥
- Alertmanager 持续发送重复告警
- 难以从告警中识别真正的问题

#### 排查步骤

```bash
# 1. 查看 Alertmanager 当前告警
curl -s http://localhost:9093/api/v2/alerts | python3 -m json.tool | grep -c '"status":"firing"'

# 2. 查看 Prometheus 触发的告警
curl -s http://localhost:9090/api/v1/alerts | python3 -m json.tool

# 3. 定位根因告警
# 通常一个根因（如数据库故障）会引发多个关联告警
# 优先关注 critical 级别告警
```

#### 处置方案

| 步骤 | 操作 | 说明 |
|------|------|------|
| 1 | **定位根因** | 从 critical 告警入手，找到根本原因 | 如数据库故障导致所有 Provider 告警 |
| 2 | **静默非关键告警** | Alertmanager 静默规则 | 避免通知干扰 |
| 3 | **修复根因** | 按对应故障处置方案处理 | — |
| 4 | **解除静默** | 根因修复后解除 | — |

**Alertmanager 静默操作：**

```bash
# 通过 API 创建静默规则（静默 warning 级别告警 1 小时）
curl -X POST http://localhost:9093/api/v2/silences \
  -H "Content-Type: application/json" \
  -d '{
    "matchers": [{"name": "severity", "value": "warning", "isRegex": false}],
    "startsAt": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
    "endsAt": "'$(date -u -d '+1 hour' +%Y-%m-%dT%H:%M:%SZ)'",
    "createdBy": "ops-oncall",
    "comment": "告警风暴期间静默 warning 告警"
  }'
```

---

### 3.6 性能劣化

#### 现象

- API 延迟升高，P95 > SLO（500ms）
- 用户反馈系统变慢
- 告警：`MedKernelApiP95LatencyHigh` / `MedKernelApiP95LatencyCritical` / `MedKernelSLOLatencyBreach`

#### 排查步骤

```bash
# 1. 查看 Grafana 看板
# 访问 http://<server>:3000 → MedKernel MVP 业务监控看板
# 重点关注：API 延迟、JVM 堆、连接池、CPU

# 2. 查看当前 P95 延迟
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,sum(rate(http_server_requests_seconds_bucket{application="medkernel-mvp"}[5m]))by(le))' | python3 -m json.tool

# 3. 查看慢请求
curl -s http://localhost:18081/medkernel/actuator/prometheus | grep http_server_requests_seconds_count | grep -v 'status="200"'

# 4. 检查数据库慢查询
# Oracle
sqlplus / as sysdba -c "SELECT SQL_ID, ELAPSED_TIME/1000000 AS SEC, SQL_TEXT FROM V\$SQL WHERE ELAPSED_TIME > 5000000 ORDER BY ELAPSED_TIME DESC FETCH FIRST 10 ROWS ONLY;"

# PostgreSQL
psql -h HOST -U postgres -c "SELECT query, calls, total_exec_time, mean_exec_time FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"

# 5. 检查连接池状态
curl -s http://localhost:18081/medkernel/actuator/prometheus | grep hikaricp_connections

# 6. 检查 JVM 状态
curl -s http://localhost:18081/medkernel/actuator/prometheus | grep -E "jvm_memory|jvm_gc"

# 7. 检查 CPU 使用
top -b -n 1 | head -20
```

#### 处置方案

| 原因 | 处置 | 操作 |
|------|------|------|
| 慢查询 | 优化 SQL / 增加索引 | 联系厂商提供优化 SQL |
| 连接池饱和 | 增大连接池 | 修改 `medkernel.env` 中 `MEDKERNEL_DB_POOL_MAX` |
| JVM GC 频繁 | 调整 JVM 参数 | 增大堆内存或调整 GC 策略 |
| 请求量突增 | 限流降级 | Nginx 限速（已配置 `rate=100r/s`） |
| 资源不足 | 增加资源 | 扩容内存/CPU |

---

### 3.7 安全事件

#### 现象

- 异常登录尝试
- 数据泄露迹象
- 疑似攻击行为
- 密钥/凭据泄露

#### 排查步骤

```bash
# 1. 检查审计日志
curl -s "http://localhost:18080/medkernel/api/audit-logs?startTime=$(date -d '-1 hour' +%Y-%m-%dT%H:%M:%S)" | python3 -m json.tool

# 2. 检查登录日志
grep -i "login\|auth\|security" /zoesoft/medkernel/logs/stdout.log | tail -50

# 3. 检查异常 IP 访问
awk '{print $1}' /var/log/nginx/medkernel.access.log | sort | uniq -c | sort -rn | head -20

# 4. 检查安全基线
curl -s http://localhost:18080/medkernel/api/security-baseline | python3 -m json.tool

# 5. 检查密钥状态
ls -la /zoesoft/medkernel/conf/medkernel.env
stat /zoesoft/medkernel/conf/medkernel.env

# 6. 检查异常进程
ps aux | grep -v "root\|medkernel\|nginx\|postgres\|oracle" | grep -v "\[" | head -20
```

#### 处置方案

| 优先级 | 操作 | 说明 |
|--------|------|------|
| 1 | **锁定可疑账户** | 禁用异常登录账户 |
| 2 | **轮换密钥** | 更换数据库密码、JWT 密钥等 |
| 3 | **网络隔离** | 防火墙阻断异常 IP |
| 4 | **上报** | 通知信息科主任、安全部门 |
| 5 | **取证** | 保留日志、审计记录 |

**紧急操作：**

```bash
# 阻断异常 IP
sudo firewall-cmd --add-source=恶意IP --zone=drop --permanent
sudo firewall-cmd --reload

# 重置数据库密码
# 1. 修改 medkernel.env 中的 MEDKERNEL_DB_PASSWORD
vi /zoesoft/medkernel/conf/medkernel.env

# 2. 在数据库中修改密码
# Oracle: ALTER USER MEDKERNEL IDENTIFIED BY "新密码";
# PostgreSQL: ALTER USER medkernel PASSWORD '新密码';

# 3. 重启服务
systemctl restart medkernel

# 检查配置文件权限
chmod 600 /zoesoft/medkernel/conf/medkernel.env
chown medkernel:medkernel /zoesoft/medkernel/conf/medkernel.env
```

> ⚠️ **安全事件必须上报！** 不可隐瞒或自行处理完毕后不上报。

---

### 3.8 数据库性能问题

#### 现象

- 慢查询频繁
- 连接池饱和
- 告警：`MedKernelHikariPoolSaturation`

#### 排查步骤

```bash
# 1. 查看慢查询日志
# Oracle — 查看当前慢 SQL
sqlplus / as sysdba -c "SELECT SQL_ID, ELAPSED_TIME/1000000 AS SEC, EXECUTIONS, SQL_TEXT FROM V\$SQL WHERE ELAPSED_TIME/EXECUTIONS > 1000000 ORDER BY ELAPSED_TIME DESC FETCH FIRST 20 ROWS ONLY;"

# PostgreSQL — 查看 pg_stat_statements
psql -h HOST -U postgres -c "SELECT query, calls, total_exec_time, mean_exec_time, rows FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 20;"

# 2. 查看执行计划
# Oracle
sqlplus / as sysdba -c "EXPLAIN PLAN FOR <慢SQL>; SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);"

# PostgreSQL
psql -h HOST -U postgres -c "EXPLAIN ANALYZE <慢SQL>;"

# 3. 查看锁等待
# Oracle
sqlplus / as sysdba -c "SELECT SID, SERIAL#, WAIT_CLASS, SECONDS_IN_WAIT, BLOCKING_SESSION FROM V\$SESSION WHERE WAIT_CLASS != 'Idle' AND SECONDS_IN_WAIT > 5;"

# PostgreSQL
psql -h HOST -U postgres -c "SELECT pid, wait_event_type, wait_event, query FROM pg_stat_activity WHERE wait_event IS NOT NULL AND state='active';"

# 4. 查看连接池状态
curl -s http://localhost:18081/medkernel/actuator/prometheus | grep hikaricp_connections
```

#### 处置方案

| 原因 | 处置 | 操作 |
|------|------|------|
| 缺少索引 | 增加索引 | 联系厂商/DBA 添加索引 |
| 慢 SQL | 优化查询 | 联系厂商优化 SQL |
| 锁等待 | 清理阻塞会话 | 终止阻塞会话 |
| 连接池不足 | 调整连接池 | 增大 `MEDKERNEL_DB_POOL_MAX` |
| 统计信息过期 | 更新统计信息 | Oracle: `DBMS_STATS.GATHER_SCHEMA_STATS` / PG: `ANALYZE` |

---

## 4. 应急预案

### 4.1 单点故障 — 服务重启

**触发条件：** 服务进程异常退出、健康检查失败

```
步骤 1：确认服务状态
  systemctl status medkernel

步骤 2：尝试重启
  systemctl restart medkernel

步骤 3：等待启动（3 秒）
  sleep 3

步骤 4：健康检查
  /zoesoft/medkernel/scripts/healthcheck.sh

步骤 5：如重启失败
  ├── 查看日志定位原因
  │   journalctl -u medkernel -n 200
  ├── 尝试回滚
  │   sudo /zoesoft/medkernel/scripts/rollback.sh --to last
  └── 联系厂商技术支持

步骤 6：如重启成功但反复崩溃
  ├── 检查 OOM：dmesg | grep oom
  ├── 检查磁盘：df -h
  └── 联系厂商技术支持
```

### 4.2 数据库故障 — 主从切换 + 应用重连

**触发条件：** 数据库不可用、数据库服务器故障

```
步骤 1：确认数据库故障
  ├── 尝试连接数据库
  └── 确认是否为数据库服务器故障

步骤 2：通知 DBA
  └── 联系数据库管理员执行主从切换

步骤 3：更新应用配置（如 IP 变更）
  vi /zoesoft/medkernel/conf/medkernel.env
  # 修改 MEDKERNEL_DB_CONNECT / MEDKERNEL_DB_URL / MEDKERNEL_DB_HOST

步骤 4：重启应用
  systemctl restart medkernel

步骤 5：健康检查
  /zoesoft/medkernel/scripts/healthcheck.sh

步骤 6：验证数据完整性
  curl -s http://localhost:18080/medkernel/api/system/providers
```

### 4.3 网络故障 — 切换备用链路

**触发条件：** 网络不通、防火墙故障

```
步骤 1：确认网络故障范围
  ├── ping 数据库服务器
  ├── ping 网关
  └── 确认是局部还是全局网络故障

步骤 2：切换备用链路（如有）
  ├── 修改路由指向备用网关
  └── 或修改 medkernel.env 中数据库连接地址

步骤 3：重启应用
  systemctl restart medkernel

步骤 4：如无备用链路
  ├── 通知信息科网络管理员
  └── 等待网络恢复
```

### 4.4 全系统故障 — 从备份恢复

**触发条件：** 服务器故障、数据损坏、勒索病毒

> ⚠️ **全系统恢复是最后手段，需在厂商技术支持指导下进行！**

```
步骤 1：评估故障范围
  ├── 服务器是否可用
  ├── 数据是否损坏
  └── 是否需要更换硬件

步骤 2：准备恢复环境
  ├── 准备新服务器（或修复原服务器）
  ├── 安装操作系统
  ├── 安装 JDK 1.8
  └── 安装数据库

步骤 3：恢复数据库
  ├── 从异机备份恢复数据库
  └── 验证数据完整性

步骤 4：安装 MedKernel
  ├── 解压发布包到 /zoesoft/medkernel/
  ├── 恢复 medkernel.env 配置
  ├── 执行 install-offline.sh
  └── 执行 healthcheck.sh

步骤 5：恢复 Nginx 配置
  cp /zoesoft/medkernel/nginx/medkernel.conf /etc/nginx/conf.d/
  nginx -t && systemctl reload nginx

步骤 6：全面验证
  ├── 健康检查
  ├── 核心功能验证
  └── 性能验证

步骤 7：恢复监控
  docker compose -f /zoesoft/medkernel/docker-compose.monitoring.yml up -d
```

---

## 5. 故障复盘

### 5.1 复盘时间要求

| 故障等级 | 复盘时间 | 参与人 |
|---------|---------|--------|
| **P0** | **24 小时内** | 信息科主任 + 厂商项目组 + 运维团队 |
| **P1** | **72 小时内** | 信息科负责人 + 厂商技术支持 + 运维团队 |
| **P2** | **1 周内** | 运维团队 + 厂商工单 |
| **P3** | **月度汇总** | 运维团队 |

### 5.2 复盘模板

```
┌──────────────────────────────────────────────────────────────────┐
│                    故障复盘报告                                    │
├──────────────────────────────────────────────────────────────────┤
│ 报告编号：     PIR-____-____                                      │
│ 故障等级：     P__                                               │
│ 故障时间：     ____年__月__日 __:__ ~ __:__                       │
│ 持续时间：     ____ 小时 ____ 分钟                                 │
│ 复盘时间：     ____年__月__日 __:__                               │
│ 复盘主持：     ____________                                       │
│ 参与人员：     ________________________________________          │
│                                                                  │
│ 一、时间线                                                        │
│ ─────────────────────────────────────────────                    │
│ 时间         事件                              操作人             │
│ ____:__     ________________                 ________            │
│ ____:__     ________________                 ________            │
│ ____:__     ________________                 ________            │
│                                                                  │
│ 二、根因分析                                                      │
│ ─────────────────────────────────────────────                    │
│ 直接原因：_________________________________________              │
│ 根本原因：_________________________________________              │
│                                                                  │
│ 三、影响评估                                                      │
│ ─────────────────────────────────────────────                    │
│ 影响科室：_________________________________________              │
│ 影响时长：__________ 分钟                                         │
│ 数据影响：□ 无  □ 有（说明：________________）                    │
│ 业务影响：_________________________________________              │
│                                                                  │
│ 四、处置过程评估                                                  │
│ ─────────────────────────────────────────────                    │
│ 响应是否及时：    □ 是  □ 否（延迟 ____ 分钟）                    │
│ 处置是否正确：    □ 是  □ 否（说明：__________）                  │
│ 通知是否到位：    □ 是  □ 否（说明：__________）                  │
│                                                                  │
│ 五、改进措施                                                      │
│ ─────────────────────────────────────────────                    │
│ 序号  改进措施                          负责人    截止日期         │
│ 1    ________________________         ______   ________         │
│ 2    ________________________         ______   ________         │
│ 3    ________________________         ______   ________         │
│                                                                  │
│ 六、附件                                                          │
│ □ 故障日志  □ 监控截图  □ 告警记录  □ 其他：__________           │
└──────────────────────────────────────────────────────────────────┘
```

### 5.3 改进措施跟踪

| 序号 | 改进措施 | 负责人 | 截止日期 | 状态 | 完成日期 |
|------|---------|--------|---------|------|---------|
| 1 | | | | □ 待办 □ 进行中 □ 完成 | |
| 2 | | | | □ 待办 □ 进行中 □ 完成 | |
| 3 | | | | □ 待办 □ 进行中 □ 完成 | |

---

## 6. 应急联系方式

### 6.1 厂商技术支持

| 角色 | 姓名 | 电话 | 邮箱 | 值班时间 |
|------|------|------|------|---------|
| 项目经理 | ________ | ________ | ________ | 工作日 9:00-18:00 |
| 技术支持（一线） | ________ | ________ | ________ | 7×24 |
| 技术支持（二线） | ________ | ________ | ________ | 7×24 |
| DBA 支持 | ________ | ________ | ________ | 工作日 9:00-18:00 |

### 6.2 医院信息科

| 角色 | 姓名 | 电话 | 邮箱 | 值班时间 |
|------|------|------|------|---------|
| 信息科主任 | ________ | ________ | ________ | 工作日 |
| 信息科值班 | ________ | ________ | ________ | 7×24 |
| 网络管理员 | ________ | ________ | ________ | 工作日 |
| 数据库管理员 | ________ | ________ | ________ | 工作日 |
| 安全管理员 | ________ | ________ | ________ | 工作日 |

### 6.3 第三方支持

| 服务商 | 用途 | 电话 | 工单地址 |
|--------|------|------|---------|
| Oracle 支持 | Oracle 数据库 | ________ | ________ |
| 达梦支持 | DM 数据库 | ________ | ________ |
| 网络设备厂商 | 交换机/路由器 | ________ | ________ |
| 服务器厂商 | 硬件故障 | ________ | ________ |

> 📋 **请将以上联系方式填写完整后打印张贴在值班室！**

---

## 7. 应急工具箱

### 7.1 常用命令速查

#### 服务管理

```bash
systemctl start medkernel           # 启动服务
systemctl stop medkernel            # 停止服务
systemctl restart medkernel         # 重启服务
systemctl status medkernel          # 查看状态
systemctl is-active medkernel       # 是否运行
journalctl -u medkernel -n 100 -f   # 实时日志
```

#### 健康检查

```bash
/zoesoft/medkernel/scripts/healthcheck.sh                                    # 自动检查
curl -s http://localhost:18080/medkernel/api/health                           # 手动检查
curl -s http://localhost:18081/medkernel/actuator/health | python3 -m json.tool  # Actuator
curl -s http://localhost:18080/medkernel/api/system/providers                 # Provider 状态
curl -s http://localhost:18080/medkernel/api/system/version                   # 版本信息
```

#### 日志查看

```bash
tail -f /zoesoft/medkernel/logs/stderr.log     # 错误日志（实时）
tail -f /zoesoft/medkernel/logs/stdout.log     # 标准输出（实时）
grep -i "error\|exception\|fail" /zoesoft/medkernel/logs/stderr.log | tail -50  # 错误过滤
journalctl -u medkernel --since "1 hour ago"   # 最近 1 小时 systemd 日志
```

#### 系统资源

```bash
free -h                    # 内存使用
df -h                      # 磁盘使用
top -b -n 1 | head -20     # CPU 和内存概览
ss -ltnp                   # 端口监听
ps aux | grep medkernel    # 进程信息
```

#### 数据库

```bash
# Oracle
echo "SELECT 1 FROM DUAL;" | sqlplus MEDKERNEL/PASSWORD@//HOST:1521/ORCL

# PostgreSQL
PGPASSWORD=PASSWORD psql -h HOST -U medkernel -d medkernel -c 'SELECT 1;'

# 达梦
disql MEDKERNEL/PASSWORD@HOST:5236 -e "SELECT 1;"
```

#### 网络

```bash
ping <IP>                  # 网络连通
telnet <IP> <PORT>         # 端口连通
curl -v http://<URL>       # HTTP 连通
nslookup <域名>            # DNS 解析
```

#### Nginx

```bash
nginx -t                   # 配置测试
systemctl reload nginx     # 重载配置
systemctl status nginx     # 状态
tail -f /var/log/nginx/medkernel.error.log  # Nginx 错误日志
```

### 7.2 诊断脚本

#### 一键诊断脚本

```bash
#!/bin/bash
# MedKernel 一键诊断脚本
# 用法：sudo ./diagnose.sh > /tmp/medkernel-diag-$(date +%Y%m%d_%H%M%S).txt 2>&1

echo "=========================================="
echo "MedKernel 诊断报告"
echo "时间：$(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="

echo ""
echo "=== 1. 服务状态 ==="
systemctl status medkernel --no-pager 2>/dev/null || echo "无法获取服务状态"

echo ""
echo "=== 2. 进程信息 ==="
ps aux | grep medkernel | grep -v grep || echo "未找到 medkernel 进程"

echo ""
echo "=== 3. 端口监听 ==="
ss -ltnp | grep -E "18080|18081|80|443" || echo "未检测到相关端口"

echo ""
echo "=== 4. 内存使用 ==="
free -h

echo ""
echo "=== 5. 磁盘使用 ==="
df -h /zoesoft 2>/dev/null || df -h

echo ""
echo "=== 6. 应用目录大小 ==="
du -sh /zoesoft/medkernel/ 2>/dev/null || echo "目录不存在"
du -sh /zoesoft/medkernel/logs/ 2>/dev/null || echo "日志目录不存在"
du -sh /zoesoft/medkernel.bak/ 2>/dev/null || echo "备份目录不存在"

echo ""
echo "=== 7. JDK 版本 ==="
java -version 2>&1 || echo "未检测到 java"

echo ""
echo "=== 8. 版本信息 ==="
cat /zoesoft/medkernel/manifest.json 2>/dev/null || echo "manifest.json 不存在"

echo ""
echo "=== 9. 配置文件 ==="
cat /zoesoft/medkernel/conf/medkernel.env 2>/dev/null | grep -v PASSWORD || echo "配置文件不存在"

echo ""
echo "=== 10. 健康检查 ==="
/zoesoft/medkernel/scripts/healthcheck.sh 2>&1 || echo "健康检查失败"

echo ""
echo "=== 11. 最近错误日志 ==="
grep -i "error\|exception\|fail" /zoesoft/medkernel/logs/stderr.log 2>/dev/null | tail -20 || echo "无错误日志"

echo ""
echo "=== 12. 最近 systemd 日志 ==="
journalctl -u medkernel -n 30 --no-pager 2>/dev/null || echo "无法获取 systemd 日志"

echo ""
echo "=== 13. JVM 堆使用 ==="
curl -s http://localhost:18081/medkernel/actuator/prometheus 2>/dev/null | grep -E "jvm_memory_used_bytes.*heap|jvm_memory_max_bytes.*heap" || echo "无法获取 JVM 指标"

echo ""
echo "=== 14. 数据库连接池 ==="
curl -s http://localhost:18081/medkernel/actuator/prometheus 2>/dev/null | grep hikaricp_connections || echo "无法获取连接池指标"

echo ""
echo "=== 15. Nginx 状态 ==="
systemctl status nginx --no-pager 2>/dev/null || echo "Nginx 状态未知"

echo ""
echo "=========================================="
echo "诊断完成"
echo "=========================================="
```

### 7.3 日志收集脚本

```bash
#!/bin/bash
# MedKernel 日志收集脚本
# 用法：sudo ./collect-logs.sh
# 产物：/tmp/medkernel-logs-<timestamp>.tar.gz

TS=$(date +%Y%m%d_%H%M%S)
COLLECT_DIR="/tmp/medkernel-logs-$TS"
mkdir -p "$COLLECT_DIR"

echo "收集 MedKernel 日志到 $COLLECT_DIR ..."

# 1. 应用日志
cp -r /zoesoft/medkernel/logs/ "$COLLECT_DIR/app-logs/" 2>/dev/null || echo "应用日志目录不存在"

# 2. systemd 日志
journalctl -u medkernel --since "24 hours ago" --no-pager > "$COLLECT_DIR/systemd-medkernel.log" 2>/dev/null || true

# 3. Nginx 日志
cp /var/log/nginx/medkernel.access.log "$COLLECT_DIR/" 2>/dev/null || true
cp /var/log/nginx/medkernel.error.log "$COLLECT_DIR/" 2>/dev/null || true

# 4. 配置文件（脱敏）
if [ -f /zoesoft/medkernel/conf/medkernel.env ]; then
  sed 's/PASSWORD=.*/PASSWORD=***REDACTED***/g' /zoesoft/medkernel/conf/medkernel.env > "$COLLECT_DIR/medkernel.env.sanitized"
fi

# 5. 版本信息
cat /zoesoft/medkernel/manifest.json > "$COLLECT_DIR/manifest.json" 2>/dev/null || true

# 6. 系统信息
uname -a > "$COLLECT_DIR/system-info.txt"
free -h >> "$COLLECT_DIR/system-info.txt"
df -h >> "$COLLECT_DIR/system-info.txt"
java -version >> "$COLLECT_DIR/system-info.txt" 2>&1

# 7. 健康检查结果
/zoesoft/medkernel/scripts/healthcheck.sh > "$COLLECT_DIR/healthcheck-result.txt" 2>&1 || true

# 打包
tar -czf "/tmp/medkernel-logs-$TS.tar.gz" -C /tmp "medkernel-logs-$TS"
rm -rf "$COLLECT_DIR"

echo "日志已打包：/tmp/medkernel-logs-$TS.tar.gz"
echo "请将此文件发送给厂商技术支持。"
```

### 7.4 监控端点速查

| 端点 | 端口 | 用途 | 访问方式 |
|------|------|------|---------|
| `/medkernel/api/health` | 18080 | 业务健康检查 | `curl http://localhost:18080/medkernel/api/health` |
| `/medkernel/api/system/providers` | 18080 | Provider 状态 | `curl http://localhost:18080/medkernel/api/system/providers` |
| `/medkernel/api/system/version` | 18080 | 版本信息 | `curl http://localhost:18080/medkernel/api/system/version` |
| `/medkernel/api/system/org-context` | 18080 | 组织上下文 | `curl http://localhost:18080/medkernel/api/system/org-context` |
| `/medkernel/actuator/health` | 18081 | Spring Boot 健康 | `curl http://localhost:18081/medkernel/actuator/health` |
| `/medkernel/actuator/health/readiness` | 18081 | 就绪探针 | `curl http://localhost:18081/medkernel/actuator/health/readiness` |
| `/medkernel/actuator/prometheus` | 18081 | Prometheus 指标 | `curl http://localhost:18081/medkernel/actuator/prometheus` |
| `/healthz` | 80/443 | 外部探测 | `curl http://localhost/healthz` |
| Prometheus | 9090 | 监控查询 | `http://<server>:9090` |
| Alertmanager | 9093 | 告警管理 | `http://<server>:9093` |
| Grafana | 3000 | 监控看板 | `http://<server>:3000` |

### 7.5 关键指标速查

| 指标 | PromQL | 告警阈值 | SLO |
|------|--------|---------|-----|
| API 可用性 | `sum(rate(http_server_requests_seconds_count{status!~"5.."}[30m])) / clamp_min(sum(rate(http_server_requests_seconds_count[30m])), 0.001)` | < 99.5% | ≥ 99.5% |
| API P95 延迟 | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))` | > 1s warn, > 3s crit | ≤ 500ms |
| 5xx 错误率 | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count[5m])), 0.001)` | > 0.5% warn, > 2% crit | ≤ 0.1% |
| JVM 堆使用率 | `sum(jvm_memory_used_bytes{area="heap"}) / clamp_min(sum(jvm_memory_max_bytes{area="heap"}), 1)` | > 75% warn, > 90% crit | ≤ 75% |
| 连接池饱和度 | `max(hikaricp_connections_active / clamp_min(hikaricp_connections_max, 1))` | > 80% | ≤ 80% |
| Provider 就绪 | `medkernel_provider_ready` | = 0 | 100% |
| 磁盘使用率 | `1 - node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"}` | > 85% warn, > 95% crit | — |
