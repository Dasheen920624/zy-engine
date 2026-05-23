# MedKernel · 备份恢复 Runbook（GA-OPS-02）

> 适用：v1.0 GA · 内外网双形态
> 验收：每月演练 ≥ 1 次 + 连续 3 次完整链路通过 + 录像归档

---

## 1. 备份策略

| 数据 | 保留周期 | 频率 | 介质 | 加密 |
|---|---|---|---|---|
| 电子病历主库 | 30 年 | 每日全量 + 每 5 min 增量 | 异地灾备站 + 国密 SM4 加密磁带 | SM4 + KMS |
| MPI 主索引 | 30 年 | 每日全量 + 每小时增量 | 同上 | SM4 + KMS |
| CDSS 提醒日志 | 7 年 | 每日全量 | 同上 | SM4 |
| 审计日志 | ≥ 6 个月（热）+ 10 年（冷） | 实时复制 | SM3 链 + TSA 时间戳 | SM3 验签 |
| 配置包（路径/规则/字典） | 永久 | 每次发布 | Git LFS + S3 兼容对象存储 | TLS |
| LLM Provider 调用记录 | 2 年 | 每日全量 | 冷存 | SM4 |

---

## 2. 恢复演练步骤

### A · 数据库恢复（Oracle 23ai · 主流程）

```bash
# 1. 切流量到只读副本（不停服）
curl -X POST $API/v1/compliance/dr/switchover -d 'role=read-only'

# 2. 选定恢复时间点
RECOVER_POINT="2026-08-15T10:00:00Z"

# 3. 从冷备站还原 RMAN
ssh dr-backup-site
rman target / cmdfile=restore-rman.cmd

# 4. 应用归档日志直到目标时间点
sqlplus / as sysdba <<EOF
recover database until time 'TO_DATE('$RECOVER_POINT','YYYY-MM-DD"T"HH24:MI:SS')';
alter database open resetlogs;
EOF

# 5. 校验数据完整性
psql -c "select count(*) from mpi_patient;"   # 应等于备份时点的行数
psql -c "select * from medkernel_meta order by applied_at desc limit 1;"

# 6. 切流量回主库
curl -X POST $API/v1/compliance/dr/switchover -d 'role=primary'

# 7. 录像归档到 docs/release/evidence/v1.0.0-drill-YYYYMMDD/
```

### B · 配置包恢复（Git LFS）

```bash
# 1. 找回任意历史版本
cd configpack-repo
git log --all --oneline configpack/胸痛AMI急诊路径.json

# 2. checkout 历史版本
git checkout <commit-hash> -- configpack/胸痛AMI急诊路径.json

# 3. 重新发布（走 7 步流的灰度）
curl -X POST $API/v1/tenant/pathways/p1/publish
```

### C · 审计链恢复

```bash
# 审计链验签恢复（每条事件用 SM3 + TSA 验签）
java -jar medkernel-audit-tools.jar verify-chain --from 2026-08-15 --to 2026-08-16

# 输出示例：
# 2026-08-15 ~ 2026-08-16: 共 1,283,924 条审计事件
# SM3 链验签: 1,283,924 / 1,283,924 全部通过
# TSA 时间戳验签: 1,283,924 / 1,283,924 全部通过
```

---

## 3. 演练频率与录像

| 演练 | 频率 | 录像归档 |
|---|---|---|
| 数据库恢复（A） | 每月 1 次 | `docs/release/evidence/v1.0.0-drill-monthly/db/` |
| 配置包恢复（B） | 每季 1 次 | 同上 |
| 审计链验签（C） | 每月 1 次 | 同上 |
| 完整业务连续性演练 | 每年 ≥ 2 次 | 单独目录 + 院方信息中心签字 |

---

## 4. 关键 SLA

| 指标 | 目标 | 测量 |
|---|---|---|
| RPO（恢复点目标） | ≤ 1 小时 | 增量备份间隔 |
| RTO（恢复时间目标） | ≤ 4 小时 | 演练实测均值 |
| 备份成功率 | ≥ 99.5% | Prometheus alert |
| 演练成功率 | ≥ 100%（连续 3 次） | 录像 + 院方签字 |

---

## 5. 故障应急联络

| 角色 | 联系方式 |
|---|---|
| 信息科主任 | （院方填） |
| 乙方 SRE 24×7 | （销售填） |
| 国密 KMS 厂商应急 | （KMS 厂商填） |
| TSA / CA 厂商应急 | （CA 厂商填） |
