# MedKernel · 4 治理模块用户手册

> 状态：骨架占位 · 待 E5 引擎全能力验收后正式填充
> 适用：v1.0 GA · 终端使用人员（医生、护士、药师、信息科、临床科室主任、医务/质控部、医保审核、院级管理）

---

## 1. 文档定位

按 [基础底座与引擎服务能力总览](../../MEDKERNEL_FOUNDATION_AND_SERVICES.md) §4 划分的 4 个治理模块（试点准备、临床运行、质控改进、合规运维）+ 专业领域服务包，每个模块对应一份终端用户手册。

---

## 2. 计划目录

| 文件 | 受众 | 启用阶段 |
|---|---|---|
| `tenant-readiness.md` 试点准备用户手册 | 信息科主任 / 临床牵头人 / 知识包负责人 | E6 GA-SVC-PILOT-* 启动后 |
| `clinical-runtime.md` 临床运行用户手册 | 医生 / 护士 / 临床科主任 | E6 GA-SVC-CLINICAL-* 启动后 |
| `quality-improvement.md` 质控改进用户手册 | 医务部 / 质控部 / 病案 / 医保 | E6 GA-SVC-QUALITY-* 启动后 |
| `compliance-operations.md` 合规运维用户手册 | 信息科 / 审计 / 安全 | E6 GA-SVC-COMPLIANCE-* 启动后 |

专业领域服务包（专病路径、专业协同）的用户手册按 [详细规范](../../MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md) §S15+ 单独追加。

---

## 3. 内容约束

- 不重写功能描述，统一引用 [详细规范](../../MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md)
- 重点描述：进入入口 / 默认视图 / 7 步流操作 / 异常处理 / 联系运维
- 截图与视频归档到 `docs/release/evidence/`
