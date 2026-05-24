# docs/specs/ — 页面规格书

> v1.0 GA 阶段：本目录承载**所有客户可见页面的精确规格**。
> 任何新增 / 修改页面的 PR 必须**同步写 / 改对应规格文件**，否则 PR 拒（[CONSTITUTION §1.12 文档同 PR](../CONSTITUTION.md)）。
> 规格 ↔ 代码一一对应：`specs/clinical/patient-pathways.md` ↔ `frontend/src/pages/clinical/PatientPathways.tsx`。

## 12 字段标准模板

任何新页规格使用 [_template.md](_template.md) 复制。12 字段：

1. URL  2. 归属菜单  3. 主要角色  4. 对应任务  5. 布局（ASCII wireframe）  6. 组件树
7. API  8. 状态机  9. 空错态  10. 权限  11. 性能  12. 验收 DoD + 关联剧本 + 关联代码

## 目录结构

```
specs/
├─ _template.md            # 12 字段空白模板（R0 ✅）
├─ workbench/              # 工作台 1 页
├─ tenant/                 # 试点准备 7 页（客户实施向导 / 租户开通 / 配置包 / 路径配置 / 规则库 / 字典映射 / 适配器）
├─ clinical/               # 临床运行 6 页（MPI / 患者路径 / 提醒治理 / 规则校验 / 待办 / 通知）
├─ quality/                # 质控改进 6 页（驾驶舱 / 预警 / 医保 / 评估库 / 评估结果 / AI 知识审核）
├─ compliance/             # 合规运维 6 页（用户 / 身份绑定 / 审计 / 安全基线 / Provider / 通知设置）
├─ advanced/               # 高级工具 5 页（来源追溯 / 图谱 / AI 工作流 / 国产化自检 / 开发者控制台）
└─ embed/                  # 4 嵌入组件（B 临床嵌入器：AMI / 质控 / 医保 / 拦截）
```

## 新旧映射

新 35 页 + 4 嵌入与旧 18 页 + 4 嵌入的对应关系见 [../product/ia-mapping.md](../product/ia-mapping.md)。

## 阶段

- R0 ✅ 模板 + 7 子目录骨架 + ia-mapping
- R4 待填 35 + 4 完整规格（预计 ~2500 行，分子目录并行 5 AI 可同步）

## 历史源

完整 V2 时代版本：[docs/archive/v0.3/04_页面规格书.md](../archive/v0.3/04_页面规格书.md)（911 行，18 页 + 4 嵌入 × 12 字段，仅供查阅）

R4 阶段会按 v1.0 GA 的新 IA + 新组件库重写，**不直接搬旧文件**。旧规格中"三产品分层"的字段已废，"组件名"按当前 [frontend/src/shared/ui/](../../frontend/src/shared/ui/) 实装为准。
