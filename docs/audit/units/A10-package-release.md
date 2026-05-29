# A10 包发布引擎 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude
> backlog 自报状态：PKG-01 / API-10 = done
> **审计结论：⚠️ 需返工。后端发布/回滚/同步骨架真实，但「看影响」科室分析写死、前端系统性假闭环。1 Critical + 2 High。**

---

## 1. 单元概览

| 项 | 信息 |
|---|---|
| 后端 | `engine/pkg`（31 文件）；核心 `PackageEngineService`(511) / `PackageEngineController`(141) / `LenientPackageSyncAdapter`(36) |
| 迁移 | V15 五方言齐全（knowledge_package/package_item/release_plan/sync_target/sync_log） |
| 前端 | `tenant/ConfigPackages.tsx`(1588) |
| 测试 | ServiceTest(9) / ControllerSecurityTest(3) |
| 业务目标 | 导入/校验/看影响/灰度(10%)/全量/同步/回滚/证据（宪法 §4 七步流核心承载） |

---

## 2. 十维度审计结果

### 2.1 业务正确性 — 🟡 骨架真、看影响假
- ✅ diff（`PackageEngineService.java:210-250`）真实计算 added/updated/removed（基准包 vs 目标包条目比对）。
- ✅ syncPackage（256-319）真实：GRAYSCALE 必带 scope 校验（265）、ReleasePlan 落库（287）、逐通道 RUNNING SyncLog（298）、经 `syncPort.sync` 投影（317）、事务模板管理。
- ✅ rollbackPackage（407-429）**真原子回滚**：校验目标曾 PUBLISHED/OFFLINE（418）→ 当前包置 OFFLINE → 目标包置 ACTIVE → ROLLBACK 审计。
- 🟠 **PKG-H-01**：`getAssetDepartment:488`「受影响责任科室分析」对 `RULE`/`PATHWAY` **返回写死 `"dept-default"`**（仅 EVALUATION 返回真实 `responsibleDepartmentId`）。七步流「看影响」的科室影响面对规则/路径资产**是假的**，会误导发布决策与受影响科室通知。

### 2.2 医疗安全合规 — 🟡
- 回滚需 `package.rollback` 专属权限（Controller 122）✅；但后端回滚无高风险二次确认门禁（"二次确认"仅前端，见 2.10），且无失败审计。
- 灰度默认作用域校验存在，但宪法「默认 10% 床位」未在后端体现为默认值（需调用方传 scope），属约定弱化。

### 2.3 多租户隔离 — ✅
Controller 类级 `@DataScope(requireTenant=true)`（29）+ 细粒度权限（read/publish/rollback）；Service 全程 `findBy...AndTenantId`。

### 2.4 审计与证据链 — 🟡
- 发布/回滚有审计（ROLLBACK 428）+ SyncLog 物理存证。
- 🟡 **PKG-M-03**：用 `AuditEventPublisher`（注释写"异步"实为普通发布），回滚/同步失败路径无失败审计；与 Isolated 模式不统一。
- 🟡 **PKG-M-01**：`LenientPackageSyncAdapter:22` 的证据摘要只覆盖 `tenant:plan:target:time`，**不含包内容哈希**，无法证明同步内容未被篡改，"证据存证"语义偏弱。

### 2.5 五方言一致性 — ✅
V15 五方言齐全。

### 2.6 代码净化 — 🟠
- 后端"模拟"注释（227/21）属误导性命名（实际代码真），建议正名。
- 🟠 前端 `ConfigPackages.tsx:1` `eslint-disable no-page-mock` + `:70` 写死"高保真仿真数据集"。

### 2.7 错误处理与降级 — 🟡
- `ENG_PACKAGE_001..003` 声明使用；`LenientPackageSyncAdapter` 是诚实的无外部通道 B0 兜底（Javadoc 明示、真 SHA-256）✅。
- 🟡 **PKG-M-02**：但 SyncLog 成功状态**未区分**"真实投影到外部系统" vs "无通道离线记录"，运维无法分辨同步是否真正落到 HIS/Neo4j/Dify。

### 2.8 可观测性 — 🟡
traceId 贯穿、SyncLog 全留痕；缺发布成功率/灰度→全量转化/回滚频次指标。

### 2.9 测试覆盖与有效性 — 🟡
ServiceTest 9 例覆盖发布/回滚主链路；建议补：getAssetDepartment 真实科室断言（暴露 PKG-H-01）、跨租户负向、灰度缺 scope 拒收。

### 2.10 前后端契约一致 — 🔴 前端系统性假闭环
- 🔴 **PKG-CRIT-01**：`ConfigPackages.tsx` 与 A7/A12 同款假闭环：
  - `:70/233-234` 写死 `fallbackPackages/fallbackItems` 仿真数据集，初始即作为 `localPackages/localItems` 展示（后端无记录也显示"完整业务流"）。
  - `:343-363` 创建草稿 catch→伪造 `newPkg`（`Math.random` packageId/traceId）+ `message.success("[仿真模式] 创建成功")`。
  - `:387-406` 添加细项 catch→伪造 item + "[仿真模式] 添加成功"。
  - `:299-322` `localDiff` **客户端假差异计算**，绕过后端真实 diff 接口。
- 这是第 3 个确认此模式的前端页（CdssFatigue/AiWorkflows/ConfigPackages）。

---

## 3. 七角色视角评估

| 角色 | 评估 |
|---|---|
| 实施工程师 | 后端 7 步流骨架（建包/入项/diff/灰度/全量/回滚）真实可用；但前端假闭环 + 假科室影响面会让实施误判发布风险 |
| 信息科主任 | 同步成功未区分真实通道/离线兜底，无法判断配置是否真落到 HIS/图库 |
| 合规审计 | 回滚审计真实，但同步证据不含内容哈希，存证强度不足 |

---

## 4. Findings 汇总

| Severity | ID | 一句话 | 位置 |
|---|---|---|---|
| Critical | PKG-CRIT-01 | 前端写死仿真数据集 + catch 伪造创建/添加成功 + 客户端假 diff | `ConfigPackages.tsx:70,233,343-406,299` |
| High | PKG-H-01 | 看影响的责任科室分析对 RULE/PATHWAY 写死 dept-default | `PackageEngineService.java:488` |
| High | PKG-H-02 | 前端 eslint-disable no-page-mock + 真假数据混展 | `ConfigPackages.tsx:1,237` |
| Medium | PKG-M-01 | 同步证据摘要不含包内容哈希，存证弱 | `LenientPackageSyncAdapter.java:22` |
| Medium | PKG-M-02 | SyncLog 成功未区分真实通道/离线兜底 | `PackageEngineService.java:317` 链路 |
| Medium | PKG-M-03 | auditPublisher 非 Isolated，失败路径无审计 | `:428` 等 |

合计：Critical 1 · High 2 · Medium 3 · Low 0

---

## 5. 改造方案（按优先级）

### PKG-CRIT-01 前端去假闭环
- **改造**：删 `fallbackPackages/fallbackItems` 与 `localPackages/localItems` 兜底；列表/详情/diff 仅用后端 query 数据，空则六态空态；删 catch 伪造与"[仿真模式]"成功；diff 调用后端 `/diff` 接口而非客户端 `localDiff`。
- **工作量**：约 4h（页面较大 1588 行）。
- **验证**：后端无记录时显示空态；断网时显示错误态不弹"创建成功"。

### PKG-H-01 真实科室影响分析
- **改造**：`getAssetDepartment` 对 RULE/PATHWAY 返回真实 `responsibleDepartmentId`（补 Rule/Pathway 实体的责任科室字段查询），消除 `"dept-default"` 写死。
- **工作量**：约 2h（含实体字段确认 + 测试）。
- **验证**：含规则/路径资产的包 diff 返回真实受影响科室。

### PKG-H-02 恢复门禁
- 随 CRIT-01 删 eslint-disable，存量假数据清除后跑通 no-page-mock。约 0.5h。

### PKG-M-01/M-02 证据与通道语义
- 同步证据摘要纳入包内容指纹（package_item 集合哈希）；SyncLog 增 `channelMode`（EXTERNAL/OFFLINE_BASELINE）字段区分。约 3h（含 V?? 迁移加列五方言）。

### PKG-M-03 Isolated 审计 + 失败留痕
- 切 IsolatedAuditPublisher，回滚/同步失败补 FAILED 审计。约 1.5h。

---

## 6. 优化建议
- 后端将「默认灰度 10% 床位」落为 scope 默认值，呼应宪法 §3 变更状态机。
- 回滚增加高风险包（含红线规则/危险路径）的后端二次确认 token，不仅依赖前端弹窗。

---

## 7. 总评
- **done 名副其实性**：PKG-01 / API-10 **部分名副其实**——后端发布/回滚/同步/diff 骨架真实且架构良好（syncPort 抽象 + 事务模板 + 真实 SyncLog），但「看影响」科室分析写死、前端系统性假闭环，整体未达验收。
- **可否进入 GA 验收**：否，需先修 PKG-CRIT-01 + PKG-H-01。
- **与 backlog 联动**：CHANGELOG 4.33 称"配置包中心全面对接物理 API + 受影响科室分析 + 物理存证证据链"，与前端假闭环 + 写死科室 + 弱证据不完全相符，建议据实订正。
