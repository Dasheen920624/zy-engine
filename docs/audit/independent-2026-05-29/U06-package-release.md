# U06 知识包/配置包发布 单元 · 独立深度真实性审计

- 审计日期：2026-05-29
- 审计单元：知识包/配置包发布（包项 / 发布计划 / 同步 / 看影响 / 回滚 / 证据）
- 对应 backlog：GA-ENG-PKG-01（包发布引擎）、GA-ENG-API-10（包发布 API）
- 审计方式：从零、独立、逐行；不读不信任 `docs/audit/` 既有报告；仅从真实代码取证
- 取证范围：
  - 后端 `medkernel-backend/src/main/java/com/medkernel/engine/pkg/`（全读 30 个文件）
  - 迁移 `medkernel-backend/src/main/resources/db/migration/{postgres,oracle,dm,kingbase,h2}/V15__package_release_baseline.sql`
  - 测试 `medkernel-backend/src/test/java/com/medkernel/engine/pkg/{PackageEngineServiceTest,PackageEngineControllerSecurityTest}.java`
  - 前端 `frontend/src/pages/tenant/ConfigPackages.tsx`（1588 行全览）
  - 权威规范 `docs/MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md`、`docs/CONSTITUTION.md`、`docs/backlog.md`

---

## 一、单元概览

本单元宣称实现规范 §配置类能力 7 步流（导入/选择 → 校验 → 看影响 → 提交审核 → 灰度 → 全量 → 留证/回滚）中的「打包 / 看影响 / 灰度 / 全量 / 同步 / 回滚 / 证据」环节，对应规范 S13「包发布与院内同步」。

实际代码结构：

| 层 | 文件 | 真实性结论 |
|---|---|---|
| Controller | `PackageEngineController.java` | 端点齐全、权限注解到位；但**缺导入/导出/签名/离线端点**，回滚**无后端二次确认** |
| Service | `PackageEngineService.java` | 创建/入包/diff 计数为真；**看影响科室写死 `dept-default`**；回滚不留 plan/log、不反向投影 |
| Sync Port | `PackageSyncPort.java` + `LenientPackageSyncAdapter.java` | **唯一实现是「模拟离线同步」假投递，永不失败，无任何真实通道** |
| 迁移 | `V15__package_release_baseline.sql` ×5 方言 | DDL 结构合理；五方言一致性需逐一比对（见维度⑤） |
| 测试 | 2 个测试类 | **用 mock syncPort 掩盖假投递**；**diff 不断言受影响科室**；**未测后端回滚二次确认** |
| 前端 | `ConfigPackages.tsx` | **首行禁用 no-page-mock lint**；**写死 fallback 假包**；**catch 降级 Math.random 伪造成功**；**假 SHA256 证据**；**看影响写死 dept** |

**核心定性：本单元是「真实数据库骨架 + 全链路假闭环表演」的混合体。** 后端骨架（建包、入包、diff 计数、状态切换、审计 publish）在 mock 测试下能跑通，但三个对外承诺的核心能力——**真实物理同步投递、真实受影响科室/患者影响分析、真实证据哈希**——全部为伪造。前端在后端无数据/失败时静默切换到写死仿真数据并弹「[仿真模式] 成功」，使整条业务流在演示中「永远成功」，临床用户无法区分真假。

---

## 二、10 维度审计发现（每条带 file:line）

### ① 业务正确性

- **[High] 「看影响」受影响科室分析对 RULE/PATHWAY 全部写死 `dept-default`，非真实计算。**
  `PackageEngineService.java:488-510` `getAssetDepartment()`：`RULE` 分支命中即 `.map(r -> "dept-default")`（L492-493），`PATHWAY` 分支同样 `.map(p -> "dept-default")`（L496-497），`default` 分支直接 `return "dept-default"`（L504）。仅 `EVALUATION` 用了真实字段 `responsibleDepartmentId`（L501）。规范 §126 要求「看影响」展示「影响组织、科室、患者、规则、路径」并「影响范围可导出」，此实现等于把绝大多数资产的影响科室伪造为同一个占位值。注释 L227「模拟受影响的责任科室分析」自证。

- **[High] diff 仅比对「目标 vs 单一 base 包」的 added/updated/removed 计数，无患者/规则/路径影响、无导出。**
  `PackageEngineService.java:212-251`。规范 §126 看影响要求「影响组织、科室、患者、规则、路径」+「模拟执行」+「影响范围可导出」。实现只产出三个整型计数 + 写死科室列表（见上），**无患者影响、无规则/路径级联影响、无任何导出端点**。

- **[High] 规范 §609 验收要求「包可导入、导出、签名、校验、灰度、回滚、离线安装」，本单元缺失「导入/导出/签名/离线安装」全部能力。**
  `PackageEngineController.java` 仅有 create/list/detail/items/diff/sync/rollback/sync-targets 八端点（全文件），无 import/export/sign/offline 任何路由；后端包内 grep 无相关实现（`grep import|export|sign|offline` 仅命中 java import 语句与注释）。但 `docs/backlog.md:84` GA-ENG-PKG-01 标题明写「导入导出、…、证据」且标 `done`。

- **[Medium] 回滚目标合法性校验逻辑可疑：允许回滚到 `PUBLISHED` 但从未真正 ACTIVE 过的包。**
  `PackageEngineService.java:418-421`：仅校验目标状态 ∈ {PUBLISHED, OFFLINE}。一个从 DRAFT 经灰度变 PUBLISHED（L385-387）但从未全量激活的包，也能被「回滚」成 ACTIVE，绕过全量发布门禁。这是用回滚通道实现「未经全量发布即激活」的后门。

### ② 医疗安全合规（高危回滚二次确认）

- **[Critical] 高危回滚在后端无任何二次确认 / 复核 token / 审批门禁，仅前端 Popconfirm，可被直接绕过。**
  `PackageEngineController.java:121-127` rollback 端点只有 `@PreAuthorize("@perm.has('package.rollback')")`，请求体仅 `targetPackageId` 一个 query 参数，无确认令牌、无 reason、无二次校验。`PackageEngineService.java:406-432` `rollbackPackage()` 直接改状态。规范 §115「可恢复：发布、替换、撤回、同步和批量处理必须有回滚点或撤回解释」，CONSTITUTION 体验规范对高危操作要求二次确认。前端虽有 `Alert type=error`（`ConfigPackages.tsx:1481-1487`）和 `Popconfirm`（L1561-1577），但纯前端控件，任何直连 API 调用即绕过。backlog `docs/backlog.md:155` 4.33 自称「设计了一键高危原子回滚和二次确认安全门禁」——**门禁不存在于后端，名实不符**。

- **[High] 回滚不反向投影到任何下游通道，真实环境将造成「权威版本」与下游（HIS/图谱/Dify）不一致。**
  `PackageEngineService.java:406-432`：回滚仅切换 `knowledge_package.status`，未创建 ReleasePlan、未写 SyncLog、未调用 `syncPort` 反向同步。规范 §221「同步失败不得破坏权威版本」、§130「一键回滚可用」隐含回滚需作用于已投影的物理目标。当前回滚后下游仍是被回滚掉的新版本，临床在用流程与权威库脱节。

- **[High] 同步失败时不阻断包状态推进，存在「同步失败但包仍部分推进」的危险中间态。**
  `PackageEngineService.java:365-389`：`anySuccess` 为真但非全部成功时 plan 置 `EXECUTING`（L366），灰度路径下仍把包从 DRAFT 推进为 PUBLISHED（L385-387）。即「投影通道部分失败」也让包进入可被后续激活/回滚引用的 PUBLISHED 状态，与「同步失败不得破坏权威版本」相悖。

### ③ 多租户隔离

- **[Pass] 后端所有仓储查询均带 `tenantId`，租户隔离落实到位。**
  `PackageEngineService.java`：`currentTenantId()`（L447-449）从 `RequestContext.snapshot().orgScope().tenantId()` 取；create/detail/item/diff/sync/rollback 全部以 `...AndTenantId(...)` 形式查询（L88, L136, L152, L198, L204, L261, L294, L412, L415）。全量激活时 L375-377 也按 `tenantId` + `packageCode` 过滤，并经测试 `syncPackageDoesNotAffectOtherPackageCodes`（`PackageEngineServiceTest.java:299-360`）验证不污染其他病种包。Controller `@DataScope(requireTenant = true)`（`PackageEngineController.java:29`）。迁移所有表含 `tenant_id` 且唯一约束含租户维度（如 `uk_knowledge_package_tenant_version`，`V15...postgres.sql:18`）。

- **[Low] 前端 fallback 假数据硬编码 `tenantId: "TENANT-001"`（`ConfigPackages.tsx:75,90,105` 等），与真实租户无关；属假数据问题（见维度⑥），非隔离缺陷。**

### ④ 审计证据链（发布/同步/回滚留痕）

- **[Critical] 同步「证据哈希」是对元数据自拼字符串算的 SHA-256，不证明任何真实投递内容，构成伪造证据。**
  `LenientPackageSyncAdapter.java:20-35`：`sync()` 把 `tenant:%s;plan:%s;target:%s;time:%s`（L22-23）拼成字符串做 SHA-256（L25-26），返回 `"LNT-" + hex.substring(0,32)`。哈希算法虽真，但**输入只有自家元数据，从未读取/传输任何包内容**——这是「UUID/随机数充哈希」的变体：哈希真、所证之事假。注释 L21「模拟离线同步逻辑…作为证据存证」自证。sync_log.sync_evidence 列注释（`V15...postgres.sql:146`）号称「投影执行数字签名与内容摘要存证」，名实不符。

- **[High] 回滚审计走 `@Transactional` 内普通 `AuditEventPublisher`，若回滚事务异常，审计随主事务回滚而丢失。**
  `PackageEngineService.java:406`（`@Transactional`）+ L428 `auditPublisher.publish(AuditAction.ROLLBACK, ...)`。系统已有 `IsolatedAuditPublisher`（`shared/audit/IsolatedAuditPublisher.java:31-33`，`PROPAGATION_REQUIRES_NEW`）专为「失败留痕不被主事务回滚带走」设计，但本单元回滚/发布路径**未使用**它。违反严重度定义中「失败审计丢失 = Critical」的精神（此处定 High，因正常路径审计可达，仅异常路径丢失）。

- **[High] 回滚不在 release_plan / sync_log 留任何执行记录，证据链断裂。**
  `PackageEngineService.java:406-432` 全过程不写 plan/log；`ReleasePlanStatus.ROLLBACKED` 枚举（`ReleasePlanStatus.java`）定义了却从未被任何代码写入（全包 grep 无 `ROLLBACKED` 赋值）。规范 §34「一个证据链：…发布、同步、运行、反馈、回滚均可追溯」要求回滚可追溯到记录级，当前仅一条 audit 文本。

- **[Medium] syncPackage 主方法非 `@Transactional`（`PackageEngineService.java:256`），最终成功/失败审计 publish（L392-398）在无环绕事务上下文中触发，AFTER_COMMIT 监听语义存疑。**

### ⑤ 五方言一致性

- **[Medium] 五方言 DDL 存在（postgres/oracle/dm/kingbase/h2 各一份 V15），但本次仅完整核读 postgres 版；需逐一比对 CHECK 约束枚举值、类型映射（TIMESTAMPTZ/TEXT 在 oracle/dm 的等价）。**
  已确认五文件均存在（`find` 命中 5 份于 `src/main/resources/db/migration/*/`）。postgres 版 `V15...postgres.sql` 的 CHECK 约束：`knowledge_package.status`（L19-21）、`package_item.asset_type`（L41-43）、`release_plan.strategy/scope_type/status`（L64-66）、`sync_target.target_type`（L85）、`sync_log.status`（L108）。建议对 oracle（无 TIMESTAMPTZ，需 TIMESTAMP WITH TIME ZONE；无 TEXT，需 CLOB/VARCHAR2）逐列核对，否则迁移在 oracle/dm 上可能失败或语义漂移。

- **[Medium] 前后端枚举值不一致：前端假数据用 `BUSINESS_DB` / `NEO4J`，后端枚举与迁移 CHECK 是 `CLINICAL_DB` / `GRAPH_DB`。**
  `ConfigPackages.tsx:205`（`targetType: "BUSINESS_DB"`）、L216（`"NEO4J"`）；后端 `SyncTargetType.java`（`CLINICAL_DB, DIFY, GRAPH_DB, REDIS`）；迁移 `V15...postgres.sql:85` CHECK `('CLINICAL_DB','DIFY','GRAPH_DB','REDIS')`。前端值若真落库会违反 CHECK 约束。当前因前端只用于 fallback 假展示而未暴露，属潜伏契约缺陷。

### ⑥ 代码净化

- **[Critical] 前端首行禁用 `medkernel/no-page-mock` lint 规则，刻意为页面级 mock 开绿灯。**
  `ConfigPackages.tsx:1`：`/* eslint-disable medkernel/no-page-mock */`。项目已内建禁页面 mock 的自定义规则，本文件主动全文件禁用，是「明知有红线、刻意绕过」的强信号。

- **[Critical] 大段写死「高保真仿真数据集」当真展示：3 个完整假病种包 + 假资产 + 假同步通道。**
  `ConfigPackages.tsx:70-222`：`fallbackPackages`（脑卒中 pkg-stroke-v1 / 胸痛 pkg-chestpain-v2 / VTE pkg-vte-v08，含逼真 NIHSS/钙蛋白/Caprini 描述）、`fallbackItems`（L119-186）、`fallbackSyncTargets`（L188-222，含假 jdbc/bolt 连接串）。注释 L70「顶级架构师设计：高保真医学配置包仿真数据集」自证。

- **[Critical] `displayPackages`/`displayTargets` 在后端返回空时静默回退假数据，用户无法区分真假。**
  `ConfigPackages.tsx:238-254`：`apiPackagesData?.items.length > 0 ? 真 : localPackages(假)`（L238-246）；`apiSyncTargets.length > 0 ? 真 : fallbackSyncTargets`（L253-254）。看板指标 `activeCount` 等（L546-549）直接基于 `displayPackages` 统计，假数据被算进真实 KPI。

- **[Critical] catch 降级全链路伪造成功 + Math.random 造 ID/进度，弹「[仿真模式] 成功」。**
  创建 `ConfigPackages.tsx:343-366`（`Math.random()` 造 packageId/traceId，L347/L358）；入包 L387-407；同步 L450-511（含 `setTimeout` 伪造进度条 L456-463）；回滚 L525-542。所有 catch **吞掉真实错误**并以 success message 收尾，违反判伪铁律「catch 吞错伪造成功」。

- **[Critical] 假「SHA256」同步证据：用 `Math.random().toString(36)` 拼成形似哈希的字符串。**
  `ConfigPackages.tsx:465-476`：`const randHash = Math.random().toString(36).substring(2,10).toUpperCase();` → `syncEvidence: \`SHA256-${randHash}-EVIDENCE-PROOF-MEDKERNEL\``（L474）。既非 SHA-256、又无任何被同步内容，纯随机串伪装成密码学证据。

- **[High] 「看影响」前端把写死 `dept-default` 映射成「临床多学科协同中心」展示，与后端写死值串通成假闭环。**
  `ConfigPackages.tsx:1208-1212` `deptsMap` 把 `dept-001-neurology/dept-003-emergency/dept-default` 硬映射中文科室名；而 fallback diff 直接写死 `affectedDepartments: ["dept-001-neurology","dept-003-emergency"]`（L318）。即便走真实后端，后端也只会回 `dept-default`（维度①），前端再映射成好看的中文名，全链路无真实科室计算。

- **[Low] 注释多处「模拟/仿真/WOW 闭环」用语：`PackageEngineService.java:227`、`LenientPackageSyncAdapter.java:13,21`、`ConfigPackages.tsx:70,232,298,306,344,451,526`。**

### ⑦ 错误处理与降级

- **[High] 后端 `getAssetDepartment` catch 吞异常返回 `dept-default`（`PackageEngineService.java:507-509`），把查询失败伪装成「有影响科室」。**
  任何仓储异常都被静默转成占位科室，掩盖真实故障，污染影响分析输出。

- **[Critical] 前端把「后端调用失败」当「离线 / 无数据」处理并伪造成功（见维度⑥ catch 段）。**
  正确的降级应是明确告知用户「服务不可用 / 同步未连接」，而非伪造业务成功。规范 §494 体检式连接体检要求诚实暴露连通性，此处反其道而行。

- **[Pass] 后端同步对单通道失败有捕获并落 FAILED 日志。**
  `PackageEngineService.java:315-355`：sync 在 try/catch 外加事务边界，失败写 `SyncLogStatus.FAILED` + errorCode `ENG-PACKAGE-005` + 真实 `finalError.getMessage()`（L341-353），未伪造。此为本单元少见的诚实降级——但因 syncPort 永不抛异常（假投递），该路径实战中几乎不触发。

### ⑧ 可观测性

- **[Pass] 全链路有 traceId 透传 + slog。**
  `PackageEngineService.java`：`RequestContext.currentTraceId()`（L84 等）写入所有实体；sync 失败 `log.error(..., e)`（L319）。

- **[Medium] sync_log.retry_count 始终为 0，「重试」能力声明但未实现。**
  `PackageEngineService.java:306,336,350`（retryCount 恒传 0）；迁移列注释（`V15...postgres.sql:142`）号称「重试与加密签名存证」；`SyncLogStatus` 含 `RETRYING` 状态（`SyncLogStatus.java`）却从无代码写入。重试是声明态，非实现态。

### ⑨ 测试覆盖与有效性

- **[Critical] 唯一的同步测试用 mock syncPort 返回写死字符串，完全掩盖了真实实现是假投递这一事实。**
  `PackageEngineServiceTest.java:244-245` `when(syncPort.sync(...)).thenReturn("EVIDENCE-DIFY-001")`；L253 断言 `syncEvidence == "EVIDENCE-DIFY-001"`。测试只验证了「service 会把 port 返回值塞进日志」，对 `LenientPackageSyncAdapter` 的真实行为（拼串算哈希、永不失败）**零覆盖**。这是「测试全绿 ≠ 真」的典型。

- **[High] diff 测试不断言 `affectedDepartments`，受影响科室（看影响核心输出）无任何测试覆盖。**
  `PackageEngineServiceTest.java:190-226` `calculateDiffComputesCorrectStats` 仅断言 added/updated/removed 计数（L223-225），完全不碰 `affectedDepartments`。写死 `dept-default` 因此无测试约束。

- **[High] 安全测试未验证回滚二次确认；仅验证 RBAC 拒绝，未触及高危门禁缺失。**
  `PackageEngineControllerSecurityTest.java:88-107` 仅测 ROLE_DOCTOR 对 sync/rollback 返回 403（L100-106），未对「有权限用户的回滚是否需要二次确认」做任何测试——因为该机制根本不存在。

- **[High] 无失败路径测试：同步通道失败、部分失败、回滚非法目标、回滚到 PUBLISHED 后门等关键分支均无测试。**
  两测试类仅覆盖 happy path + RBAC + 重复版本（L126-140）+ 未审资产拒绝（L142-162）。`SyncLogStatus.FAILED` 落库逻辑（service L341-353）、`anySuccess/allSuccess` 状态机分支（L365-366）零覆盖。

- **[Medium] 前端无任何针对 fallback/catch 降级路径的测试隔离，假闭环代码无门禁拦截（反被 eslint-disable 放行）。**

### ⑩ 前后端契约一致

- **[Pass] 核心响应字段名前后端一致。**
  `PackageDiffResponse.affectedDepartments`（后端 record）↔ 前端 `apiDiffData?.affectedDepartments`（`ConfigPackages.tsx:1206`）；`SyncLogResponse.syncEvidence/retryCount/errorCode`（后端）↔ 前端 `SyncLogResponse`（L63 import）一致使用。

- **[Medium] SyncTargetType 枚举值前后端不一致（见维度⑤ `BUSINESS_DB/NEO4J` vs `CLINICAL_DB/GRAPH_DB`）。**

- **[Medium] 前端发布表单可不选 scopeType 而默认 `"ALL"`（`ConfigPackages.tsx:439`），但后端 `PackageSyncRequest.scopeType` 为 `@NotNull`（`PackageSyncRequest.java`），灰度校验又要求非 ALL（service L265-268）；前端默认值与后端灰度约束存在交互歧义，靠 catch 兜底掩盖。**

---

## 三、七角色评估

| 角色 | 关注点 | 评估结论 |
|---|---|---|
| **实施工程师** | 一键导入配置包是否真进图谱/规则 | **不通过。** 根本无导入端点（维度①）；即便走 sync，`LenientPackageSyncAdapter` 不向 Neo4j/规则库写一个字节（假投递）。实施现场「一键导入配置包」会显示成功、证据齐全，但目标系统空空如也——最危险的「看起来成功」。 |
| **临床医生** | 看影响能否看到真实受累科室/患者 | **不通过。** 受影响科室对规则/路径全写死 `dept-default`→「临床多学科协同中心」，无患者影响。医生据此评估发布风险等于盲评。 |
| **信息科/运维** | 同步通道连通性、失败补偿、回滚一致性 | **不通过。** 无真实通道、retry 恒 0、回滚不反向投影、下游与权威库会脱节。 |
| **医务处/审核** | 高危回滚是否有审批门禁与留痕 | **不通过。** 后端无二次确认，回滚不写 plan/log，异常时审计可丢失。 |
| **合规/审计员** | 证据哈希是否可信、证据链是否完整 | **不通过。** sync_evidence 是元数据自拼哈希（伪证据），前端甚至用 Math.random 造假 SHA256。 |
| **安全工程师** | 跨租户、权限绕过 | **基本通过（隔离/ RBAC 真实）**，但回滚高危门禁缺失（前端可绕过）拉低评分。 |
| **架构师** | 端口抽象、状态机、可演进 | **部分通过。** PackageSyncPort 抽象设计合理（具备接真实 adapter 的扩展点），状态枚举完整；但唯一实现是假投递，ROLLBACKED 状态空挂，状态推进存在「部分失败仍 PUBLISHED」缺陷。 |

---

## 四、Findings 汇总表

| # | 维度 | 严重度 | 摘要 | 关键 file:line |
|---|---|---|---|---|
| F01 | ② 安全合规 | **Critical** | 高危回滚后端无二次确认/审批门禁，仅前端 Popconfirm 可绕过 | `PackageEngineController.java:121-127`; `PackageEngineService.java:406-432` |
| F02 | ④ 证据链 | **Critical** | 同步证据是元数据自拼 SHA-256，不证明任何真实投递（伪证据） | `LenientPackageSyncAdapter.java:20-35` |
| F03 | ⑥ 净化 | **Critical** | 首行禁用 no-page-mock lint，刻意为页面 mock 开绿灯 | `ConfigPackages.tsx:1` |
| F04 | ⑥ 净化 | **Critical** | 写死 3 个假病种包 + 假资产 + 假同步通道当真展示 | `ConfigPackages.tsx:70-222` |
| F05 | ⑥ 净化 | **Critical** | 后端空数据时静默回退假数据，混入真实 KPI 统计 | `ConfigPackages.tsx:238-254,546-549` |
| F06 | ⑥/⑦ 净化 | **Critical** | catch 吞错全链路伪造成功 + Math.random 造 ID/进度 | `ConfigPackages.tsx:343-366,387-407,450-511,525-542` |
| F07 | ④/⑥ 证据 | **Critical** | 前端用 Math.random 伪造「SHA256」同步证据 | `ConfigPackages.tsx:465-476` |
| F08 | ⑨ 测试 | **Critical** | 同步测试 mock syncPort 写死返回，掩盖假投递实现 | `PackageEngineServiceTest.java:244-253` |
| F09 | ⑦ 错误处理 | **Critical** | 前端把后端失败当离线伪造成功（应诚实标未连接） | `ConfigPackages.tsx:343,387,450,525` |
| F10 | ① 业务 | High | 看影响受影响科室对 RULE/PATHWAY 全写死 dept-default | `PackageEngineService.java:488-510` |
| F11 | ① 业务 | High | diff 无患者/规则路径级联影响、无导出 | `PackageEngineService.java:212-251` |
| F12 | ① 业务 | High | 缺导入/导出/签名/离线安装端点（规范 §609 验收项） | `PackageEngineController.java`（全文件） |
| F13 | ② 安全合规 | High | 回滚不反向投影，权威版本与下游不一致 | `PackageEngineService.java:406-432` |
| F14 | ② 安全合规 | High | 同步部分失败仍把包推进 PUBLISHED（破坏权威版本风险） | `PackageEngineService.java:365-389` |
| F15 | ④ 证据链 | High | 回滚审计未用 IsolatedAuditPublisher，异常时审计丢失 | `PackageEngineService.java:406,428` |
| F16 | ④ 证据链 | High | 回滚不写 plan/log，ROLLBACKED 状态空挂 | `PackageEngineService.java:406-432`; `ReleasePlanStatus.java` |
| F17 | ⑥/① 净化 | High | 前端 deptsMap 把写死 dept-default 美化成中文科室 | `ConfigPackages.tsx:1208-1212,318` |
| F18 | ⑦ 错误处理 | High | 后端 getAssetDepartment catch 吞错返回 dept-default | `PackageEngineService.java:507-509` |
| F19 | ⑨ 测试 | High | diff 测试不断言 affectedDepartments | `PackageEngineServiceTest.java:190-226` |
| F20 | ⑨ 测试 | High | 安全测试未验证回滚二次确认 | `PackageEngineControllerSecurityTest.java:88-107` |
| F21 | ⑨ 测试 | High | 无同步失败/部分失败/回滚非法目标等失败路径测试 | 两测试类全文件 |
| F22 | ① 业务 | Medium | 回滚可激活从未 ACTIVE 的 PUBLISHED 包（绕过全量门禁） | `PackageEngineService.java:418-421` |
| F23 | ⑤ 方言 | Medium | 五方言 V15 需逐列核对（oracle/dm 的 TIMESTAMPTZ/TEXT 等价） | `V15__package_release_baseline.sql` ×5 |
| F24 | ⑤/⑩ 契约 | Medium | SyncTargetType 前后端枚举不一致(BUSINESS_DB/NEO4J vs CLINICAL_DB/GRAPH_DB) | `ConfigPackages.tsx:205,216`; `SyncTargetType.java`; `V15...postgres.sql:85` |
| F25 | ⑧ 可观测 | Medium | retry_count 恒 0，RETRYING 状态空挂，重试仅声明未实现 | `PackageEngineService.java:306,336,350`; `SyncLogStatus.java` |
| F26 | ④ 证据 | Medium | syncPackage 非 @Transactional，最终审计 publish 事务语义存疑 | `PackageEngineService.java:256,392-398` |
| F27 | ⑩ 契约 | Medium | 前端 scopeType 默认 ALL 与后端灰度非 ALL 约束交互歧义 | `ConfigPackages.tsx:439`; `PackageEngineService.java:265-268` |
| F28 | ⑥ 净化 | Low | 后端多处「模拟/仿真」注释 | `PackageEngineService.java:227`; `LenientPackageSyncAdapter.java:13,21` |
| F29 | ③ 隔离 | Low | 前端 fallback 硬编码 tenantId TENANT-001 | `ConfigPackages.tsx:75,90,105` |

**计数：Critical = 9，High = 12，Medium = 7，Low = 2，合计 30。**

---

## 五、改造建议（针对每条 Critical / High）

### Critical

- **F01 回滚二次确认下沉后端：** rollback 端点新增强制确认令牌（如要求传入当前 ACTIVE 包的 version/校验码 + reason 必填），服务层校验确认令牌匹配方可执行；高危操作记审批人。前端 Popconfirm 保留为体验层，不作为唯一门禁。
- **F02 真证据：** 证据哈希必须基于「实际被同步的包内容快照（资产清单 + 各资产版本内容指纹）」计算，而非元数据自拼；并落「目标侧回执」（投递确认）作为证据组成。无真实通道时不得产出「证据」。
- **F03/F04/F05/F06/F07/F09 前端去假闭环：** 删除 `eslint-disable medkernel/no-page-mock`、`fallbackPackages/fallbackItems/fallbackSyncTargets`、所有 catch 降级仿真分支、假 SHA256 证据；后端失败/无数据时改为诚实空状态 + 明确错误提示（规范 §475 空状态文案），不得伪造成功。
- **F08 真同步测试：** 为 `LenientPackageSyncAdapter`（或替换后的真实 adapter）写直接单测验证其真实行为；同步集成测试需断言「无真实通道时诚实失败/标未连接」，而非 mock 成功。
- **统筹：** 实现至少一个真实 `PackageSyncPort` adapter（对接规则库/图谱/Dify 之一），并在无连接时诚实降级（参考 A14 第三方对接诚实化，commit 0eac9c6）。

### High

- **F10/F11/F17/F18 真看影响：** `getAssetDepartment` 改为从规则/路径实体的真实责任科室/适用组织字段计算（RULE/PATHWAY 需补该字段或关联查询），去掉 dept-default 兜底与 catch 吞错；diff 补患者/规则/路径级联影响 + 影响范围导出端点（规范 §126）。
- **F12 补能力：** 实现导入/导出/签名/离线安装端点（规范 §609），或若暂不做则**回退 GA-ENG-PKG-01 的 done 状态并从标题删除「导入导出、证据」承诺**。
- **F13/F16 回滚闭环：** 回滚创建 ReleasePlan(ROLLBACKED) + SyncLog，并反向投影到原同步目标；写 ROLLBACKED 状态。
- **F14 状态机收紧：** 同步非全部成功时不得推进包状态；定义清晰的「部分失败→人工介入」中间态，不污染 PUBLISHED/ACTIVE。
- **F15 审计抗回滚：** 回滚/发布失败留痕改用 `IsolatedAuditPublisher.publishInNewTx`。
- **F19/F20/F21 补测试：** affectedDepartments 真实计算断言、回滚二次确认门禁测试、同步失败/部分失败/回滚非法目标/PUBLISHED 后门等失败路径测试。

---

## 六、总评

### done 是否名副其实？

**否。** GA-ENG-PKG-01、GA-ENG-API-10 当前的 `done` 严重名实不符：

- backlog `docs/backlog.md:155`（4.33）自承「针对无通道或无数据库记录场景**自适应唤醒高保真仿真数据集闭环展示**」——这正是判伪铁律点名的「前端无通道时自适应唤醒仿真数据集假闭环」，把假闭环当成验收亮点写进了 done 说明。
- 标题承诺的「导入导出」「证据」「二次确认安全门禁」「物理通道投影」四项，分别为：**缺失 / 伪证据 / 仅前端 / 假投递**。
- 「跑通 80 个测试 + 0 ESLint errors」之所以成立，恰恰是因为：① 同步测试用 mock 掩盖假投递；② 假闭环代码被首行 `eslint-disable` 放行。测试全绿与真实性背离。

### 可否真实验收？

**不可。** 阻塞项（必须先清零）：

1. **9 条 Critical 全部清零**，尤其前端假闭环（F03-F07,F09）、假证据（F02,F07）、回滚无门禁（F01）。
2. 至少一个**真实 PackageSyncPort 实现**或诚实「未连接」降级（消灭 `LenientPackageSyncAdapter` 假投递）。
3. **看影响真实计算**（F10/F11）——医疗安全相关，临床据此评估发布风险。
4. 回滚**后端二次确认 + 反向投影 + 留痕**（F01/F13/F16）。

### 应回退哪些 backlog

- **`GA-ENG-PKG-01`（`docs/backlog.md:84`）：done → 回退（建议 in-progress / blocked）。** 理由：导入导出缺失、证据伪造、前端假闭环、回滚门禁仅前端。
- **`GA-ENG-API-10`（`docs/backlog.md:65`）：done → 回退（建议 in-progress）。** 理由：同步为假投递、看影响写死、回滚不留痕/不反向投影、关键失败路径无测试。

二者均应在 Critical 清零 + 真实同步通道（或诚实降级）+ 真实看影响落地后，重新走验收。
