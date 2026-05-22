# MedKernel 运维与应急手册

> 版本：1.0 | 适用：v1.0 GA | 日期：2026-05-23
> 交付对象：医院信息科 / 运维团队

---

## 第一部分：日常运维手册

### 1. 服务管理

#### 1.1 启动服务

```bash
# 加载环境变量
source /zoesoft/medkernel/conf/medkernel.env

# 启动服务
systemctl start medkernel

# 检查状态
systemctl status medkernel
```

启动后验证：
```bash
# 健康检查
./deploy/scripts/healthcheck.sh --url http://127.0.0.1:18080/medkernel

# 或手动检查
curl -s http://127.0.0.1:18080/medkernel/api/health | python3 -m json.tool
curl -s http://127.0.0.1:18080/medkernel/api/system/providers | python3 -m json.tool
```

#### 1.2 停止服务

```bash
# 优雅停止（等待 30s 超时）
systemctl stop medkernel

# 强制停止（仅紧急情况）
systemctl kill medkernel
```

#### 1.3 重启服务

```bash
systemctl restart medkernel
```

#### 1.4 查看日志

```bash
# systemd 日志
journalctl -u medkernel -f

# 应用日志文件
tail -f /zoesoft/medkernel/logs/medkernel.log

# 按时间范围查看
journalctl -u medkernel --since "2026-05-23 10:00" --until "2026-05-23 12:00"
```

### 2. 配置管理

#### 2.1 环境变量

核心配置通过环境变量驱动，配置文件位于 `/zoesoft/medkernel/conf/medkernel.env`。

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MEDKERNEL_HTTP_PORT` | 18080 | 业务端口 |
| `MEDKERNEL_HTTP_CONTEXT` | /medkernel | 上下文路径 |
| `MEDKERNEL_MANAGEMENT_PORT` | 18081 | 管理端口（Actuator） |
| `MEDKERNEL_DB_ENABLED` | false | 数据库开关 |
| `MEDKERNEL_DB_DIALECT` | oracle | 数据库方言 |
| `MEDKERNEL_DB_URL` | — | JDBC URL |
| `MEDKERNEL_DB_USERNAME` | — | 数据库用户名 |
| `MEDKERNEL_DB_PASSWORD` | — | 数据库密码 |
| `MEDKERNEL_FLYWAY_ENABLED` | false | Flyway 迁移开关 |
| `MEDKERNEL_GRAPH_ENABLED` | false | 图谱引擎开关 |
| `MEDKERNEL_DIFY_ENABLED` | false | Dify AI 开关 |

#### 2.2 修改配置

```bash
# 1. 编辑配置文件
vi /zoesoft/medkernel/conf/medkernel.env

# 2. 重启服务
systemctl restart medkernel

# 3. 验证配置生效
curl -s http://127.0.0.1:18081/medkernel/actuator/env | grep MEDKERNEL_DB_DIALECT
```

#### 2.3 JVM 参数调优

JVM 参数在 systemd 服务文件中配置：`/etc/systemd/system/medkernel.service`

| 参数 | 默认值 | 建议范围 | 说明 |
|------|--------|---------|------|
| `-Xms` | 2g | 2g-4g | 初始堆大小 |
| `-Xmx` | 4g | 4g-8g | 最大堆大小 |
| `-XX:+UseG1GC` | — | — | G1 垃圾收集器 |
| `-XX:MaxGCPauseMillis` | 200 | 100-500 | 最大 GC 停顿目标 |

修改后需重新加载：
```bash
systemctl daemon-reload
systemctl restart medkernel
```

### 3. 监控告警

#### 3.1 监控栈访问

| 服务 | URL | 默认凭据 |
|------|-----|---------|
| Grafana | http://localhost:3000 | admin / medkernel_admin |
| Prometheus | http://localhost:9090 | — |
| Alertmanager | http://localhost:9093 | — |

#### 3.2 关键看板

| 看板 | 内容 |
|------|------|
| 系统总览 | Provider 状态、服务存活 |
| JVM 运行时 | 堆使用、GC 统计、线程数 |
| 数据库连接池 | HikariCP 活跃/空闲/等待 |
| API 性能 | P50/P95/P99 延迟、QPS |
| 业务操作 | 请求分布、错误率 |

#### 3.3 告警规则

共 19 条告警规则，详见 `deploy/monitoring/prometheus/medkernel-alert-rules.yml`。

关键告警：

| 告警 | 级别 | 条件 | 处理 |
|------|------|------|------|
| MedKernelBackendDown | critical | 服务不可达 | 立即检查服务状态 |
| MedKernelDatabaseProviderDown | critical | 数据库不可用 | 检查数据库连接 |
| MedKernelModelGatewayDown | critical | AI 网关不可用 | 检查 Dify/模型服务 |
| MedKernelSLOAvailabilityBreach | critical | 可用性 < 99.5% | 启动应急流程 |
| MedKernelJvmHeapCritical | critical | 堆使用 > 90% | 检查内存泄漏 |

### 4. 日志管理

#### 4.1 日志位置

| 日志 | 路径 | 保留策略 |
|------|------|---------|
| 应用日志 | `/zoesoft/medkernel/logs/` | 按天滚动，保留 30 天 |
| 审计日志 | 数据库 `sec_audit_chain` 表 | 永久保留 |
| 系统日志 | `journalctl -u medkernel` | 按 journald 配置 |

#### 4.2 日志级别

通过 Actuator 动态调整：
```bash
# 查看当前日志级别
curl http://127.0.0.1:18081/medkernel/actuator/loggers/com.medkernel

# 调整日志级别（调试时使用）
curl -X POST http://127.0.0.1:18081/medkernel/actuator/loggers/com.medkernel \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

---

## 第二部分：备份恢复手册

### 1. 备份策略

| 备份类型 | 频率 | 保留 | 范围 |
|---------|------|------|------|
| 全量数据库 | 每日 02:00 | 30 天 | 所有业务表 + Flyway 历史 |
| 增量数据库 | 每小时 | 7 天 | 变更数据 |
| 配置文件 | 每次变更 | 10 份 | medkernel.env + profile |
| 应用包 | 每次升级 | 5 份 | JAR + 依赖 |

### 2. Oracle 备份

```bash
# 全量导出（expdp）
expdp medkernel/password DIRECTORY=backup_dir DUMPFILE=medkernel_$(date +%Y%m%d).dmp LOGFILE=medkernel_$(date +%Y%m%d).log

# 全量恢复（impdp）
impdp medkernel/password DIRECTORY=backup_dir DUMPFILE=medkernel_20260523.dmp LOGFILE=restore_20260523.log
```

### 3. PostgreSQL 备份

```bash
# 全量备份
pg_dump -h 10.0.0.30 -U medkernel -d medkernel -Fc -f /backup/medkernel_$(date +%Y%m%d).dump

# 全量恢复
pg_restore -h 10.0.0.30 -U medkernel -d medkernel -c /backup/medkernel_20260523.dump
```

### 4. 达梦 DM 备份

```bash
# 全量备份
dmbackup ... 

# 全量恢复
dmrestore ...
```

### 5. KingbaseES 备份

```bash
# 全量备份（兼容 pg_dump）
sys_dump -h 10.0.0.40 -U medkernel -d medkernel -Fc -f /backup/medkernel_$(date +%Y%m%d).dump

# 全量恢复
sys_restore -h 10.0.0.40 -U medkernel -d medkernel -c /backup/medkernel_20260523.dump
```

### 6. 配置文件备份

```bash
# 备份配置
cp /zoesoft/medkernel/conf/medkernel.env /zoesoft/medkernel.bak/medkernel.env.$(date +%Y%m%d%H%M)

# 恢复配置
cp /zoesoft/medkernel.bak/medkernel.env.202605231000 /zoesoft/medkernel/conf/medkernel.env
```

### 7. 备份验证

```bash
# 验证备份文件完整性
ls -la /backup/medkernel_$(date +%Y%m%d).*

# 验证数据库可恢复（在测试环境）
# Oracle: impdp ... REMAP_SCHEMA=...
# PG: pg_restore -l /backup/medkernel_20260523.dump
```

---

## 第三部分：升级回滚手册

### 1. 升级流程

```
1. 通知用户：提前 24 小时通知升级窗口
2. 停止服务：systemctl stop medkernel
3. 备份当前版本：cp -r /zoesoft/medkernel /zoesoft/medkernel.bak/pre-upgrade
4. 备份数据库：执行全量备份
5. 部署新版本：./deploy/scripts/upgrade.sh
6. 执行 Flyway 迁移：自动（MEDKERNEL_FLYWAY_ENABLED=true）
7. 启动服务：systemctl start medkernel
8. 健康检查：./deploy/scripts/healthcheck.sh
9. DDL 一致性验证：./deploy/scripts/smoke-ddl-consistency.sh
10. 通知用户：升级完成
```

### 2. 回滚流程

```
1. 停止服务：systemctl stop medkernel
2. 恢复应用：cp -r /zoesoft/medkernel.bak/pre-upgrade/* /zoesoft/medkernel/
3. 恢复数据库：执行数据库恢复（见备份恢复手册）
4. Flyway 回滚：见 docs/engineering/flyway-rollback-guide.md
5. 启动服务：systemctl start medkernel
6. 健康检查：./deploy/scripts/healthcheck.sh
7. 通知用户：回滚完成
```

### 3. 回滚决策

| 场景 | 决策 | 时间窗口 |
|------|------|---------|
| 升级后健康检查失败 | 立即回滚 | < 5 分钟 |
| 升级后功能异常 | 评估影响后决定 | < 30 分钟 |
| Flyway 迁移失败 | 修复迁移或回滚 | < 1 小时 |
| 数据库不兼容 | 回滚数据库 + 应用 | < 2 小时 |

---

## 第四部分：故障应急手册

### 1. 故障分级

| 级别 | 定义 | 响应时间 | 示例 |
|------|------|---------|------|
| P0 紧急 | 系统不可用，影响全部用户 | 5 分钟 | 服务宕机、数据库不可用 |
| P1 严重 | 核心功能不可用，影响多数用户 | 15 分钟 | 登录失败、路径引擎异常 |
| P2 一般 | 非核心功能异常，影响部分用户 | 1 小时 | 报表导出失败、告警延迟 |
| P3 轻微 | 体验问题，不影响业务 | 4 小时 | UI 显示异常、日志告警 |

### 2. 常见故障排查

#### 2.1 服务无法启动

```bash
# 检查服务状态
systemctl status medkernel

# 查看启动日志
journalctl -u medkernel -n 100 --no-pager

# 常见原因：
# 1. 端口占用
ss -ltnp | grep 18080
ss -ltnp | grep 18081

# 2. 配置错误
cat /zoesoft/medkernel/conf/medkernel.env | grep -v '^#' | grep -v '^$'

# 3. JVM 内存不足
free -h
dmesg | grep -i oom

# 4. 数据库连接失败
# Oracle
echo "SELECT 1 FROM DUAL;" | sqlplus user/pass@host:port:sid
# PostgreSQL
PGPASSWORD=xxx psql -h host -U user -d db -c 'SELECT 1;'
```

#### 2.2 数据库连接失败

```bash
# 检查 Provider 状态
curl -s http://127.0.0.1:18080/medkernel/api/system/providers | python3 -m json.tool

# 检查数据库连通性
./deploy/scripts/check-env.sh

# 检查连接池状态（Actuator）
curl -s http://127.0.0.1:18081/medkernel/actuator/metrics/hikaricp.connections.active
```

#### 2.3 API 响应缓慢

```bash
# 检查 JVM 堆使用
curl -s http://127.0.0.1:18081/medkernel/actuator/metrics/jvm.memory.used?tag=area:heap

# 检查 GC 统计
curl -s http://127.0.0.1:18081/medkernel/actuator/metrics/jvm.gc.pause

# 检查连接池饱和度
curl -s http://127.0.0.1:18081/medkernel/actuator/metrics/hikaricp.connections.active

# 检查线程数
curl -s http://127.0.0.1:18081/medkernel/actuator/metrics/jvm.threads.live
```

#### 2.4 内存溢出（OOM）

```bash
# 检查 OOM 日志
ls -la /zoesoft/medkernel/logs/*hprof*

# 分析 Heap Dump（需要工具）
# jhat /zoesoft/medkernel/logs/java_pid*.hprof
# 或使用 Eclipse MAT / VisualVM

# 临时缓解：增大堆内存
# 修改 /etc/systemd/system/medkernel.service
# -Xmx4g → -Xmx6g
systemctl daemon-reload
systemctl restart medkernel
```

#### 2.5 磁盘空间不足

```bash
# 检查磁盘使用
df -h

# 清理日志
find /zoesoft/medkernel/logs/ -name "*.log" -mtime +30 -delete

# 清理备份
find /zoesoft/medkernel.bak/ -name "*.bak" -mtime +30 -delete

# 清理 Prometheus 数据（谨慎）
# 修改 --storage.tsdb.retention.time=15d（默认 30d）
```

### 3. 应急预案

#### 3.1 服务全量宕机

```
1. 确认宕机范围（单节点/集群）
2. 检查系统资源（CPU/内存/磁盘/网络）
3. 尝试重启：systemctl restart medkernel
4. 若重启失败：检查日志定位根因
5. 若无法快速恢复：启动备用节点
6. 通知医院信息科和业务部门
7. 记录事件时间线和处置过程
```

#### 3.2 数据库故障

```
1. 确认数据库状态（连接/响应/空间）
2. 检查数据库日志
3. 若连接池耗尽：重启应用（连接池自动回收）
4. 若数据库宕机：联系 DBA 启动数据库
5. 若数据损坏：从备份恢复（见备份恢复手册）
6. 验证数据完整性：./deploy/scripts/smoke-ddl-consistency.sh
```

#### 3.3 安全事件

```
1. 确认事件类型（入侵/数据泄露/异常访问）
2. 立即隔离：断开外网连接（保留内网）
3. 保留证据：导出审计日志和访问日志
4. 评估影响范围
5. 通知安全负责人和医院信息科
6. 执行修复措施（修补漏洞/重置密码/更新规则）
7. 编写安全事件报告
```

#### 3.4 监控告警风暴

```
1. 确认告警来源和真实性
2. 若为误报：调整告警阈值或静默规则
3. 若为真实故障：按故障级别处理
4. 在 Alertmanager 中设置临时静默（维护窗口期）
5. 事后复盘：优化告警规则，减少误报
```

### 4. 应急联系方式

| 角色 | 职责 | 联系方式 |
|------|------|---------|
| 运维值班 | 一线响应 | __填写__ |
| 系统管理员 | 系统级问题 | __填写__ |
| DBA | 数据库问题 | __填写__ |
| 安全负责人 | 安全事件 | __填写__ |
| 厂商支持 | 产品问题 | __填写__ |

---

## 附录

### A. 端口清单

| 端口 | 用途 | 访问范围 |
|------|------|---------|
| 18080 | 业务 HTTP | 内网 |
| 18081 | Actuator 管理 | 本机（127.0.0.1） |
| 3000 | Grafana | 内网 |
| 9090 | Prometheus | 内网 |
| 9093 | Alertmanager | 内网 |

### B. 关键文件路径

| 文件 | 路径 |
|------|------|
| 应用主目录 | /zoesoft/medkernel |
| 配置文件 | /zoesoft/medkernel/conf/medkernel.env |
| 应用日志 | /zoesoft/medkernel/logs/ |
| 备份目录 | /zoesoft/medkernel.bak/ |
| systemd 服务 | /etc/systemd/system/medkernel.service |
| Nginx 配置 | /etc/nginx/conf.d/medkernel.conf |

### C. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-05-23 | 1.0 | 初始版本：日常运维 + 备份恢复 + 升级回滚 + 故障应急 |
