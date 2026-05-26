# MedKernel · 合同 / SLA / 隐私 / DPA / 许可

> 状态：骨架占位 · 待签约/上线阶段补齐
> 适用：v1.0 GA · 商务、法务、合规

---

## 1. 文档定位

本目录存放与**外部交付**相关的法律/合规文档，工程实现不在此处。这里的文件具有法律效力，必须由法务、合规、商务三方签字后归档。

---

## 2. 计划文件

| 文件 | 用途 | 启用时点 |
|---|---|---|
| `master-agreement.md` 主合同模板 | 集团/医院签约主合同模板 | 进入商务签约阶段 |
| `sla.md` 服务等级协议 | 可用性 / RPO / RTO / 性能 / 安全应急 | E5 验收后 |
| `privacy.md` 隐私政策 | 患者数据保护、最小数据原则、敏感数据脱敏规则 | 上线前 |
| `dpa.md` 数据处理协议 | 数据控制者/处理者职责、跨境与本地化 | 签约附件 |
| `license.md` 软件许可 | 一方代码、依赖三方协议清单（含开源协议） | 发布前 |
| `domestic-compliance.md` 国产化合规 | 等保2.0 / 密评 / 医疗器械与医院信息化备案 | govcloud 部署前 |

---

## 3. 数据交叉

- SLA 指标必须与 [备份恢复 Runbook](../handbook/runbooks/backup-restore.md) §4 中的 RPO/RTO 一致
- 隐私政策必须与 [产品宪法](../CONSTITUTION.md) 中的隐私硬约束一致
- 国产化合规对齐 `application-govcloud.yml` 与 [总览](../MEDKERNEL_FOUNDATION_AND_SERVICES.md) §2

---

## 4. 内容约束

- 文档不存敏感字段（密钥、密码、个人信息）
- 法律文本一律中文优先；如需双语，中文为准
- 不在此处复制业务规则，统一引用 [详细规范](../MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md)
