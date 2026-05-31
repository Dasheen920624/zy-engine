# 任务清单：真实性整治与研发重启闸门

> 计划日期：2026-05-31
> 状态：执行准备
> 关联：`engine-authenticity-remediation`

---

## 阶段 0：计划纠偏

- [ ] **Task 0.1：权威口径纠偏**
  - 更新 `docs/AI_DEVELOPMENT_RESTART_PLAN.md`、`docs/audit/质量基线.md`、`docs/backlog.md`、`README.md`、`docs/README.md`。
  - 验收：搜索旧权威、旧阶段和“把字符相似度当修复目标”的误导口径，不再命中当前权威文档。

- [ ] **Task 0.2：OpenSpec 纠偏**
  - 重写本变更的 `proposal.md`、`design.md`、`tasks.md`、`specs/remediation-spec.md`。
  - 验收：OpenSpec 中不再把 LCS / 编辑距离作为医学语义映射充分依据；不再以旧 E5/E6 为当前执行主线。

- [ ] **Task 0.3：交接状态纠偏**
  - 更新 `docs/_HANDOFF.md`：确认 #169 已合入 `origin/main`，卡体系迁移已完成；登记本分支为研发重启计划硬化线。
  - 验收：`git log origin/main --max-count=1` 与 `_HANDOFF` 状态一致。

- [ ] **Task 0.4：业务实现范围核查方案**
  - 新增 `docs/BUSINESS_IMPLEMENTATION_SCOPE_AUDIT.md`，明确 S0–S40、27+5 菜单、卡到代码、B0 主链路和 wave2 消费点核查。
  - 验收：README、backlog、质量基线和研发重启方案均指向该范围闸门。

---

## 阶段 1：真实性债务盘点

- [ ] **Task 1.0：业务范围盘点**
  - 核查 `cards/_index.md` 场景覆盖、`cards/_coverage-matrix.md` 待迁锚点、backlog 域级验收和实际卡文件数量。
  - 验收：记录 S0–S40 已有索引承接、D2 物理卡/逻辑交付项计数差异、覆盖矩阵仍待回填的风险。

- [ ] **Task 1.1：前端假闭环清单**
  - 搜索 `eslint-disable medkernel`、`Math.random()`、`SHA-256-MOCK`、`规避 no-page-mock`、`message.success` catch 路径。
  - 输出清单按 D0–D6 / wave2 / ga 归属到施工卡。
  - 验收：每个命中项要么有本 PR 修复，要么在对应施工卡的未完成事项中登记。

- [ ] **Task 1.2：后端假实现清单**
  - 搜索生产路径 `Math.random()`、`UUID.randomUUID()` 充 hash、catch 成功返回、硬编码医学常量、占位 Javadoc。
  - 区分“业务 ID 生成”与“证据 hash 伪造”。
  - 验收：高风险项归属到 [INFRA-02](../../../docs/cards/D0/INFRA-02.md) 或 [BASE-09](../../../docs/cards/D0/BASE-09.md)。

- [ ] **Task 1.3：登录页现状复现**
  - 运行前端 build/typecheck/test；用浏览器打开 `/login`，记录渲染、样式 token、错误态、提交行为。
  - 验收：形成 D0 `AUTH-02` 的红灯证据，禁止把当前页面当作已完成。

---

## 阶段 2：D0 第一闸实现顺序

- [ ] **Task 2.1：INFRA-01 前端门禁 ratchet**
  - 门禁先阻断新增问题和 touched file 问题，并输出存量债务清单。
  - 验收：新增假数据 fixture 测试失败；测试/storybook/静态 UI 配置白名单通过。

- [ ] **Task 2.2：INFRA-02 后端门禁 ratchet**
  - 阻断生产路径假 hash、catch 成功、Math.random 业务造数和占位 Javadoc。
  - 验收：fixture 自测覆盖每类拒绝项；生产路径无新增违反。

- [ ] **Task 2.3：BASE-10 + AUTH-02 登录渲染修复**
  - 登录页样式全部走 token；修复深浅色、移动端、老年模式；提交与错误态真实。
  - 验收：浏览器截图 + `npm run lint/typecheck/test/build` 通过；登录失败显示真实中文错误与 traceId。

- [ ] **Task 2.4：AUTH-01/03 登录闭环**
  - httpOnly cookie、CSRF、MFA/首登改密、错误不泄露存在性。
  - 验收：登录、失败、登出、401、首登改密单测与 E2E 通过。

- [ ] **Task 2.5：BASE-02/INFRA-05 RBAC 菜单**
  - 13 角色 × 27 二级 + 5 高级粒度矩阵真实生效。
  - 验收：13 角色登录后菜单截图/测试；跨租户访问被拒并审计。

- [ ] **Task 2.6：D0 域级验收**
  - 按 [D0 域简报](../../../docs/cards/D0/_brief.md) 的 D0-验收执行。
  - 验收：D0 B0 主链路 E2E + T-GATE 全绿 + reviewer 签字。

---

## 阶段 3：域级纵向推进

- [ ] **Task 3.1：D1 工作台**
  - D0 过闸后启动；工作台不得自造聚合假数据。
  - 验收：工作台与演示校验页消费真实健康/审计/运行状态，六态可达。

- [ ] **Task 3.2：D2 试点准备**
  - 优先完成规则、路径、知识、字典、适配器的 B0 确定性与 7 步流。
  - 验收：A1 试点准备 E2E 全程关模型可跑；字典映射不以 LCS 作为语义判定。

- [ ] **Task 3.3：D3–D6 与 wave2**
  - 依赖 D0–D2 的真实底座逐域推进；模型增强只在 B0 通过后回灌。
  - 验收：每域独立域级 E2E；GA 统一跑 A1–A9。
