# MedKernel · 升级回滚 Runbook（GA-OPS-02）

> 适用：v1.0 GA 跨版本升级（如 v1.0.0 → v1.0.1 / v1.0 → v1.1）
> 验收：每次正式发布前必须在测试环境完整演练 ≥ 1 次 + 录像

---

## 1. 升级流程（按宪法 §4 7 步流）

### 步骤 1 · 选模板 / 准备包
- 下载 `medkernel-v1.0.x-offline.tar.gz`（含 JDK 21 + Tomcat 10 + JAR + Docker 镜像）
- 校验 SHA-256 + cosign 签名
- 校验 SBOM (CycloneDX)

### 步骤 2 · 自动校验
- `deploy/scripts/check-env.sh`：检查 OS / 国密 Provider / 数据库连通性 / 端口占用

### 步骤 3 · 看影响
- 查 v1.0.x → v1.0.x+1 的 Flyway migration 数量
- 查 API 兼容性矩阵（与前端 OpenAPI 客户端版本对齐）
- 估算停服时间（蓝绿 = 0，停服 = 5 分钟）

### 步骤 4 · 提交审核
- 院方 IT 主任 + 乙方 SRE 双签

### 步骤 5 · 灰度发布
- 默认 10% 床位 / 1 个科室 / 1 小时观察
- 监控指标：CDSS P95 / 错误率 / Hikari 池 / 日志 ERROR
- 不达标自动回滚

### 步骤 6 · 全量
- 全院切换
- 24 小时观察期

### 步骤 7 · 留证据 / 可回滚
- 升级日志归档到 `docs/release/evidence/v1.0.x-upgrade-YYYYMMDD/`
- 保留 `legacy/v1.0.x-pre-upgrade-YYYYMMDD` Git tag
- Docker 镜像保留前 3 个版本

---

## 2. 一键升级（推荐）

```bash
# 升级
cd /opt/medkernel
sudo ./deploy/scripts/upgrade.sh --from v1.0.0 --to v1.0.1 --strategy blue-green

# 蓝绿切换实时监控
watch -n 2 'curl -s http://localhost:18080/medkernel/actuator/health | jq'

# 回滚（任何时候）
sudo ./deploy/scripts/rollback.sh --to v1.0.0
```

---

## 3. 回滚条件（自动触发）

| 指标 | 阈值 | 触发 |
|---|---|---|
| HTTP 5xx 错误率 | > 1% | 5 分钟内 → 自动回滚 |
| P95 延迟 | > 1s | 持续 10 分钟 → 自动回滚 |
| Hikari 池泄漏 | leak detection 命中 | 立即告警 + 人工确认回滚 |
| CDSS 决策不可写 | 5 分钟内连续 3 次 | 立即回滚 |
| 数据库 migration 失败 | 任何一次 | 立即回滚 + 不停服 |

---

## 4. Flyway 跨版本回滚（GA-DB-01 已支持）

```bash
# 查看当前版本
java -jar medkernel-backend.jar --spring.flyway.command=info

# 回滚到指定版本（Flyway 10 商业 undo / 或手写 down 脚本）
java -jar medkernel-backend.jar \
  --spring.flyway.command=undo \
  --spring.flyway.target=1.0.0-baseline
```

> ⚠️ Flyway 社区版不支持 undo。我们采用 **向前-only schema + soft delete** 策略：所有 DDL 变更都向前兼容，删字段改为 `_deprecated` 列；如需"逻辑回滚"，应用层切回旧字段读取。

---

## 5. 演练频率

| 演练 | 频率 | 录像归档 |
|---|---|---|
| 测试环境完整升级 + 蓝绿切换 | 每次发布前 | `docs/release/evidence/v1.0.x-upgrade-drill/` |
| 自动回滚演练 | 每季 1 次 | 同上 |
| 数据库回滚演练 | 每季 1 次 | 同上 |

---

## 6. 关键 SLA

| 指标 | 目标 |
|---|---|
| 蓝绿切换零停服 | 99% 升级 |
| 回滚 RTO | ≤ 10 分钟 |
| 数据无丢失 | RPO = 0（蓝绿）/ ≤ 5 min（停服） |
| 升级成功率 | ≥ 99.9% |
