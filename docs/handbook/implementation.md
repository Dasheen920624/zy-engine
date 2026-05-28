# MedKernel · 实施工程师手册

> 状态：骨架占位 · 待 E5 引擎全能力验收后正式填充
> 适用：v1.0 GA · 现场实施工程师 / 解决方案架构师

---

## 1. 文档定位

本手册面向**现场实施工程师**，负责把已验收的引擎能力按 [业务服务包目录](../MEDKERNEL_FOUNDATION_AND_SERVICES.md#42-业务医疗服务包目录) 在客户医院/集团落地。

实施工程师**不实现业务逻辑**，只做：

- 租户开通与组织结构搭建（参考 [详细规范 S0-S3](../MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md)）
- 适配器接入 HIS/EMR/LIS/PACS/医保/病案/随访/区域平台/监管与评级证据交换
- 字典映射与映射包发布
- 知识包/规则/路径配置上线（7 步流）
- 灰度→全量→回滚演练
- 现场培训与移交（联动 [training/](training/README.md)）

---

## 2. 启用阶段

| 阶段 | 触发条件 |
|---|---|
| 骨架占位（当前） | E0/E1 完成 |
| 正式填充 | E5 引擎全能力验收通过后 |
| 内容来源 | 不另起方案，所有实施细节直接引用 [MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md](../MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md) |

---

## 3. 关联文档

- [产品宪法](../CONSTITUTION.md)
- [基础底座与引擎服务能力总览](../MEDKERNEL_FOUNDATION_AND_SERVICES.md)
- [产品体验固定规范](../MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md)
- [实施落地方案](../MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md)
- [全业务场景详细规范](../MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md)
- [备份恢复 Runbook](runbooks/backup-restore.md)
- [升级回滚 Runbook](runbooks/upgrade-rollback.md)
