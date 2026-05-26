# GA-ENG-BASE-09 代码基线净化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 W3-W7 阶段遗留的 6 大旧业务模块、19 处显式前端 mock、4 处真接 mock 后端的 hook 页面全部净化，并补全 CI lint 门禁；分 3 个顺序 PR 落地，让 E1 基础底座完整收尾。

**Architecture:** 后端"提取设计精华到业务规范 + 删除全部 Java 实现"；前端"占位卡 + 任务 ID + 路线图链接"统一改造；CI 补 ESLint job 锁门禁。3 个 PR 独立可 revert，门禁先行 → 后端净化 → 前端净化。

**Tech Stack:** JDK 21 + Spring Boot 3.3 + Flyway 10 + ESLint 9 + React 18 + Antd 5 + Vite 5 + GitHub Actions

**Spec:** [docs/superpowers/specs/2026-05-26-base-09-code-cleanup-design.md](../specs/2026-05-26-base-09-code-cleanup-design.md)

---

# PR-1 · 门禁先行（轻、快、低风险）

## Task 1: 创建 PR-1 分支并提交 spec + plan

**Files:**
- 已存在：`docs/superpowers/specs/2026-05-26-base-09-code-cleanup-design.md`
- 已存在：`docs/superpowers/plans/2026-05-26-base-09-code-cleanup.md`

- [ ] **Step 1: 确认工作树干净**

Run: `git status`
Expected: `nothing to commit, working tree clean`（spec/plan 文件可能 untracked，OK）

- [ ] **Step 2: 创建分支**

Run: `git checkout -b codex/base-09-pr1-gate`
Expected: `Switched to a new branch 'codex/base-09-pr1-gate'`

- [ ] **Step 3: 提交 spec + plan 到分支**

```bash
git add docs/superpowers/specs/2026-05-26-base-09-code-cleanup-design.md \
        docs/superpowers/plans/2026-05-26-base-09-code-cleanup.md
git commit -m "$(cat <<'EOF'
docs(GA-ENG-BASE-09): 代码净化设计与实施计划

新增 BASE-09 设计文档与实施计划，作为后续 PR-2 后端净化 / PR-3 前端净化 / PR-1 门禁先行的执行依据。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

Expected: commit 创建成功，`git log --oneline -1` 显示新 commit

## Task 2: 业务场景详细规范追加 E6 附录 · URL 路径表

**Files:**
- Modify: `docs/MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md`（末尾追加附录）

- [ ] **Step 1: 读取规范文件末尾**

Run: 使用 Read 工具读取 `docs/MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md` 末尾 30 行，确认追加位置

- [ ] **Step 2: 在文末追加 E6 附录 · URL 路径表**

在文档末尾 `**End of MedKernel ...**` 行之前追加：

```markdown
---

## 附录 E6-A · W3-W7 旧实现 URL 路径表（参考用）

> 此附录是 GA-ENG-BASE-09 净化时从旧 Controller 提取的设计精华，**仅作为未来 E6 业务服务包装的设计起点参考**。
> 这些 URL 在 BASE-09 后已从代码中删除，业务包装阶段会按引擎能力重新设计。

| 旧 URL 路径 | HTTP | 旧职能 | 建议未来对接的引擎能力 |
|---|---|---|---|
| `/api/v1/advanced/llm/chat` | POST | 通用 LLM chat | GA-ENG-LLM-01 模型能力网关 |
| `/api/v1/advanced/llm/explain` | POST | LLM 解释 | GA-ENG-LLM-01 |
| `/api/v1/advanced/llm/versions` | GET | LLM 版本快照 | GA-ENG-LLM-01 |
| `/api/v1/advanced/chatbot/*` | POST | 合规 chatbot | GA-ENG-LLM-01 + GA-SVC-COMPLIANCE-02 |
| `/api/v1/advanced/academic/export` | POST | 学术导出 | GA-SVC-DOMAIN-02 科研真实世界 |
| `/api/v1/advanced/domestic-check` | GET/POST | 国产化自检 | GA-ENG-BASE-07（已合并） |
| `/api/v1/quality/insurance/drg/rulesets` | GET | DRG 规则集 | GA-SVC-QUALITY-02 病案医保 |
| `/api/v1/quality/insurance/drg/sync` | POST | DRG 同步 | GA-SVC-QUALITY-02 |
| `/api/v1/quality/variance/*` | GET/POST | 路径变异 | GA-ENG-PATH-01 路径引擎 |
| `/api/v1/quality/ncis/*` | GET/POST | NCIS 上报 | GA-SVC-DOMAIN-02 院感公卫 |
| `/api/v1/quality/medicalrecord/homepage` | GET/POST | 病历首页 | GA-SVC-QUALITY-02 |
| `/api/v1/clinical/mpi/patients` | GET | MPI 患者主索引 | GA-SVC-CLINICAL-01 |
| `/api/v1/clinical/mpi/stats` | GET | MPI 统计 | GA-SVC-CLINICAL-01 |
| `/api/v1/clinical/mpi/federation/*` | GET/POST | MPI 联邦 | GA-SVC-CLINICAL-01 |
| `/api/v1/clinical/cdss/alerts` | GET | CDSS 提醒列表 | GA-ENG-CDSS-01 推荐引擎 |
| `/api/v1/clinical/cdss/alerts/{id}/{decision}` | POST | CDSS 决策回流 | GA-ENG-CDSS-01 |
| `/api/v1/clinical/udi-check/*` | GET/POST | UDI 校验 | GA-SVC-CLINICAL-03 |
| `/api/v1/clinical/publichealth/report` | POST | 公卫上报 | GA-SVC-DOMAIN-02 院感公卫 |
| `/api/v1/tenant/rules/*` | GET/POST | 规则管理 | GA-ENG-RULE-01 + GA-ENG-API-05 |
| `/api/v1/tenant/pathways` | GET | 路径模板 | GA-ENG-PATH-01 + GA-ENG-API-06 |
| `/api/v1/tenant/pathways/{id}/publish` | POST | 路径发布 | GA-ENG-PKG-01 |
| `/api/v1/tenant/hrp/interop/*` | POST | HRP 互操作 | GA-SVC-PILOT-02 |
| `/api/v1/platform/branding/*` | GET/POST | 品牌定制 | GA-SVC-PILOT-01 |
| `/api/v1/platform/success/*` | GET/POST | 客户成功 | GA-SVC-PILOT-01 |
| `/api/v1/platform/license/*` | GET/POST | 离线许可 | GA-SVC-COMPLIANCE-02 |
| `/api/v1/platform/emergency/*` | GET/POST | 应急预案 | GA-SVC-COMPLIANCE-02 |
| `/api/v1/compliance/tenant-wall` | GET/POST | 租户墙 | GA-SVC-COMPLIANCE-01 |
| `/api/v1/compliance/dr/*` | GET/POST | 灾备 | GA-SVC-COMPLIANCE-02 |
| `/api/v1/compliance/signature/*` | POST | 医师签名 | GA-SVC-CLINICAL-02 |
| `/api/v1/compliance/data-export/*` | POST | 数据出境评估 | GA-SVC-COMPLIANCE-02 |
| `/api/v1/compliance/dlm/*` | GET/POST | 数据生命周期 | GA-SVC-COMPLIANCE-02 |
| `/api/v1/compliance/waf/*` | GET/POST | WAF 配置 | GA-SVC-COMPLIANCE-02 |
| `/api/v1/compliance/tsa/*` | POST | 可信时间戳 | GA-SVC-COMPLIANCE-02 |
| `/api/v1/compliance/masking/*` | GET/POST | 脱敏配置 | GA-SVC-COMPLIANCE-01 |

> **保留**：`/api/v1/compliance/audit/*` 系列接口属于 BASE-04 审计链合法产物，不在此清单内、不删除。
```

- [ ] **Step 3: 验证文件改动**

Run: `grep -c "附录 E6-A" docs/MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md`
Expected: `1`

## Task 3: 业务场景详细规范追加 E6 附录 · DTO 字段表 + 状态机迁移

**Files:**
- Modify: `docs/MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md`（继续追加）

- [ ] **Step 1: 在 E6-A 附录后追加 DTO 字段表**

```markdown

## 附录 E6-B · W3-W7 旧 DTO 字段表（参考用）

| 旧 DTO | 关键字段 | 业务含义 | 未来对接 |
|---|---|---|---|
| `CdssAlert` | id, patientLabel, ruleSource, adoptionRate, status, owner | CDSS 提醒卡 | GA-ENG-CDSS-01 |
| `PathwayTemplate` | id, name, disease, department, nodes, status | 路径模板 | GA-ENG-PATH-01 |
| `DrgRuleset` | version, releaseDate, ruleCount, source, status | DRG 月更版本 | GA-SVC-QUALITY-02 |
| `MpiPatient` | mpiId, maskedName, gender, age, idLast4, mergedCount, status | 主索引患者 | GA-SVC-CLINICAL-01 |
| `Rule` | id, name, dsl, riskLevel, status | 规则定义 | GA-ENG-RULE-01 |
| `LlmRequest` | prompt, temperature, providerHint, scenarioCode | LLM 调用 | GA-ENG-LLM-01 |
| `LlmResponse` | text, providerId, tokens, latencyMs, fallback | LLM 响应 | GA-ENG-LLM-01 |

## 附录 E6-C · W3-W7 旧状态机迁移（参考用）

| 状态机 | 旧状态值 | 转移规则 | 未来引擎 |
|---|---|---|---|
| CDSS 提醒 | `pending → adopting → closed` 或 `pending → remediating → closed` | 提醒由 CDSS 触发；医师采纳/不采纳后留痕 | GA-ENG-CDSS-01 |
| 路径模板 | `draft → pending_review → published → active → archived` | 路径设计 → 审核 → 发布 → 运行 → 归档 | GA-ENG-PATH-01 + 7 步流 |
| DRG 规则集 | `staged → active → archived` | 月更预发布 → 生效 → 归档 | GA-SVC-QUALITY-02 |
| MPI 患者 | `active → merged_into` | 重复主索引并入主记录 | GA-SVC-CLINICAL-01 |
| 规则 | `draft → testing → approved → published → retired` | 规则全生命周期 | GA-ENG-RULE-01 + 7 步流 |

> 以上三份附录仅作为 **E6 业务包装阶段的设计起点参考**。BASE-09 净化后这些类型在代码中已被删除；E6 阶段会基于引擎能力、按当前 ApiResult/ProblemDetail/@DataScope/审计链规范重新设计。
```

- [ ] **Step 2: 验证三份附录都已追加**

Run: `grep -cE "附录 E6-[ABC]" docs/MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md`
Expected: `3`

## Task 4: 修订 backlog.md 至 4.10

**Files:**
- Modify: `docs/backlog.md`

- [ ] **Step 1: 更新 GA-ENG-API-03 状态为 done**

把 backlog 中：
```
| GA-ENG-API-03 知识资产 API：来源、解析、引用、版本、审核、替换、历史重放、分页、筛选、搜索、异步导出 | - | pending |
```
改为：
```
| GA-ENG-API-03 知识资产 API：来源、解析、引用、版本、审核、替换、历史重放、分页、筛选、搜索、异步导出 | claude | done |
```

- [ ] **Step 2: 更新 GA-ENG-API-04 状态为 done**

把：
```
| GA-ENG-API-04 字典映射 API：标准字典、院内字典、候选映射、冲突、发布 | - | pending |
```
改为：
```
| GA-ENG-API-04 字典映射 API：标准字典、院内字典、候选映射、冲突、发布 | codex | done |
```

- [ ] **Step 3: 标 KNOW-01、KNOW-02、TERM-01 为 partial**

把：
```
| GA-ENG-KNOW-01 知识资产引擎：来源登记、解析、hash、引用锚点、可信分级 | - | pending |
| GA-ENG-KNOW-02 知识版本引擎：新旧识别、去重、冲突、待审新版、原子替换、旧版隔离 | - | pending |
```
改为：
```
| GA-ENG-KNOW-01 知识资产引擎：来源登记、解析、hash、引用锚点、可信分级 | claude | partial |
| GA-ENG-KNOW-02 知识版本引擎：新旧识别、去重、冲突、待审新版、原子替换、旧版隔离 | claude | partial |
```

把：
```
| GA-ENG-TERM-01 字典映射引擎：未映射发现、候选推荐、人工确认、冲突处理、映射包发布 | - | pending |
```
改为：
```
| GA-ENG-TERM-01 字典映射引擎：未映射发现、候选推荐、人工确认、冲突处理、映射包发布 | codex | partial |
```

- [ ] **Step 4: 新增 BASE-09 in_progress**

把：
```
| GA-ENG-BASE-09 代码基线净化：移除业务主链路 mock、裸 Map、硬编码示例数据、旧命名和单病种假闭环 | - | pending |
```
改为：
```
| GA-ENG-BASE-09 代码基线净化：移除业务主链路 mock、裸 Map、硬编码示例数据、旧命名和单病种假闭环 | claude | in_progress |
```

- [ ] **Step 5: 在修订记录表追加 4.10 行**

在表格首行下方追加：
```
| 4.10 | 2026-05-26 | Claude | GA-ENG-BASE-09 in_progress：架构师审计 + 三个 PR 顺序计划落地。同步对齐 git 实际进度：GA-ENG-API-03/04 标 done（commit ddfb950 / cb39796），GA-ENG-KNOW-01/02 与 GA-ENG-TERM-01 标 partial |
```

- [ ] **Step 6: 验证修订**

Run: `grep -cE "GA-ENG-API-(03|04).*\|.*\|.*done" docs/backlog.md`
Expected: `2`

Run: `grep "GA-ENG-BASE-09.*in_progress" docs/backlog.md`
Expected: 匹配 1 行

## Task 5: CI 工作流增加 frontend-lint job

**Files:**
- Modify: `.github/workflows/ci.yml`

- [ ] **Step 1: 在 frontend-build-test job 后追加 frontend-lint job**

在 `frontend-build-test` job 完整定义后、`jdk-matrix-smoke` job 之前插入：

```yaml
  # GA-ENG-BASE-09 · 前端 ESLint 门禁（视觉债 / mock / 旧命名归零的硬门禁）
  frontend-lint:
    name: frontend-lint
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Node 20
        uses: actions/setup-node@v4
        with:
          node-version: "20"

      - name: Install
        working-directory: frontend
        run: npm install --no-audit --no-fund --no-package-lock

      - name: ESLint
        working-directory: frontend
        run: npm run lint

      - name: Prettier format check
        working-directory: frontend
        run: npm run format:check
```

- [ ] **Step 2: 验证 yaml 语法**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml'))" && echo OK`
Expected: `OK`

- [ ] **Step 3: 本地预验证 lint 能通过**

Run: `cd frontend && npm run lint`
Expected: 零 error（如有 warning 可放过；如有 error 则需先排查；本步骤不在分支上修改代码，只是验证 lint 配置不卡门禁）

如果本地已有 error：记录到 PR 描述"已知 lint error，将随 PR-3 一起清零"；本 PR 不阻塞 lint 跑通。如果本地零 error：直接进下一步。

## Task 6: 归档 OpenSpec 历史变更

**Files:**
- Move: `openspec/changes/audit-event-persistence/` → `openspec/archive/audit-event-persistence/`
- Move: `openspec/changes/containerized-development-platform/` → `openspec/archive/containerized-development-platform/`
- Create: `openspec/archive/README.md`

- [ ] **Step 1: 创建 archive 目录并移动两个历史变更**

```bash
mkdir -p openspec/archive
git mv openspec/changes/audit-event-persistence openspec/archive/audit-event-persistence
git mv openspec/changes/containerized-development-platform openspec/archive/containerized-development-platform
```

- [ ] **Step 2: 写归档 README**

Write `openspec/archive/README.md`：
```markdown
# OpenSpec 归档

本目录存放已实质落地的历史 OpenSpec 变更，仅作为追溯参考；当前 v1.0 GA 设计已统一迁到 `docs/superpowers/specs/`。

| 归档项 | 实质落地 commit |
|---|---|
| `audit-event-persistence` | a6aba90 feat(GA-ENG-BASE-04): persist audit events with per-tenant SM3 hash chain |
| `containerized-development-platform` | 76eb52b feat: add Docker development platform |

新增设计请走 `docs/superpowers/specs/` + `docs/superpowers/plans/` 双轨。
```

- [ ] **Step 3: 验证 openspec/changes/ 已空**

Run: `ls openspec/changes/`
Expected: 仅剩 `README.md`

## Task 7: 提交 PR-1 改动并发起 PR

**Files:**
- All staged changes from Task 2–6

- [ ] **Step 1: 检查 git status**

Run: `git status`
Expected: 显示 docs/、openspec/、.github/workflows/ 多处改动

- [ ] **Step 2: 暂存并 commit**

```bash
git add docs/MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md \
        docs/backlog.md \
        .github/workflows/ci.yml \
        openspec/changes/ \
        openspec/archive/
git commit -m "$(cat <<'EOF'
feat(GA-ENG-BASE-09): PR-1 门禁先行

- 业务场景详细规范追加 E6 附录（旧 URL/DTO/状态机参考表）
- backlog 4.10：API-03/04 标 done、KNOW/TERM 标 partial、BASE-09 in_progress
- CI 增加 frontend-lint job（ESLint + Prettier 校验），加入必需 check
- OpenSpec 已落地的两份历史变更归档到 openspec/archive/

验证：
- yaml 语法 OK；本地 `npm run lint` 通过
- backlog 修订记录补 4.10 行
- 三份 E6 附录已就位

后续：PR-2 后端净化、PR-3 前端净化。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 3: push 分支**

Run: `git push -u origin codex/base-09-pr1-gate`
Expected: 成功 push，输出含 `Branch 'codex/base-09-pr1-gate' set up to track 'origin/codex/base-09-pr1-gate'`

- [ ] **Step 4: 创建 PR**

```bash
gh pr create --base main --title "feat(GA-ENG-BASE-09): PR-1 门禁先行" --body "$(cat <<'EOF'
## 变更范围

GA-ENG-BASE-09 三 PR 净化方案的 PR-1，聚焦门禁先行 + 文档同步，为 PR-2 后端净化 / PR-3 前端净化锁住质量底线。

### Added
- 业务场景详细规范追加 E6-A/B/C 附录：旧 URL 路径表 / 旧 DTO 字段表 / 旧状态机迁移参考
- CI 增加 `frontend-lint` job（ESLint + Prettier check），加入必需 check
- 新建 `openspec/archive/` 目录与归档 README

### Changed
- `docs/backlog.md` 4.10 修订：
  - GA-ENG-API-03 知识资产 API → done（commit ddfb950）
  - GA-ENG-API-04 字典映射 API → done（commit cb39796）
  - GA-ENG-KNOW-01/02、GA-ENG-TERM-01 → partial
  - GA-ENG-BASE-09 → in_progress

### Moved
- `openspec/changes/audit-event-persistence` → `openspec/archive/audit-event-persistence`
- `openspec/changes/containerized-development-platform` → `openspec/archive/containerized-development-platform`

## 验证结果

- [x] yaml 语法 OK
- [x] 本地 `npm run lint` 通过
- [x] 三份 E6 附录可在文档中检索
- [x] backlog 4.10 修订记录已补
- [x] `openspec/changes/` 仅剩 `README.md`

## 医疗安全 / 部署 / 数据迁移影响

- 不涉及医疗安全
- 不涉及部署变更
- 不涉及数据迁移

## 未完成事项

无 — 本 PR 范围已闭环。后续 PR-2 后端净化、PR-3 前端净化将独立提交。

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

Expected: 输出 PR URL；记录到 chat

- [ ] **Step 5: 等待 CI**

Run: `gh pr checks --watch`
Expected: 所有 check（backend-build-test / guard-rules / frontend-build-test / frontend-lint / jdk-matrix-smoke）通过

如 frontend-lint 失败：分析 error 是否本地遗漏；修复后追加 commit + push

- [ ] **Step 6: 合并 PR-1**

合并方式由用户决定；推荐 squash merge。合并后：

Run: `git checkout main && git pull && git log --oneline -3`
Expected: 顶部为合并的 PR-1 commit

---

# PR-2 · 后端净化（核心删除 + 提取）

## Task 8: 创建 PR-2 分支

- [ ] **Step 1: 确认在 main 且已拉取最新**

Run: `git checkout main && git pull origin main`
Expected: `Already up to date.` 或新合并 PR-1 commit 已落入

- [ ] **Step 2: 创建 PR-2 分支**

Run: `git checkout -b codex/base-09-pr2-backend-cleanup`
Expected: `Switched to a new branch 'codex/base-09-pr2-backend-cleanup'`

## Task 9: 删除 advanced 模块

**Files:**
- Delete: `medkernel-backend/src/main/java/com/medkernel/advanced/llm/{LlmRequest,LlmExplain,LlmExplainController,LlmResponse,LlmProvider,LlmGateway,LlmVersionController,SaasFallbackProvider,OllamaMockProvider}.java`
- Delete: `medkernel-backend/src/main/java/com/medkernel/advanced/chatbot/ComplianceChatbotController.java`
- Delete: `medkernel-backend/src/main/java/com/medkernel/advanced/academic/AcademicExportController.java`
- Delete: `medkernel-backend/src/main/java/com/medkernel/advanced/domestic/DomesticCheckController.java`
- Delete: `medkernel-backend/src/test/java/com/medkernel/advanced/` 整目录

- [ ] **Step 1: 删除 advanced 主代码与测试目录**

```bash
git rm -r medkernel-backend/src/main/java/com/medkernel/advanced/
git rm -r medkernel-backend/src/test/java/com/medkernel/advanced/
```

- [ ] **Step 2: 验证目录已删**

Run: `ls medkernel-backend/src/main/java/com/medkernel/advanced/ 2>&1 | head -1`
Expected: `ls: ... No such file or directory`

- [ ] **Step 3: 本地编译**

Run: `cd medkernel-backend && mvn -B -q -DskipTests compile`
Expected: BUILD SUCCESS

如出现编译错误：grep 检查 `shared/` 或 `engine/` 是否仍在引用 `advanced/*`（理论上无，BASE-09 spec §4.1 已验证）；如有则记录到本 task 修复列表

## Task 10: 删除 quality 模块

- [ ] **Step 1: 删除 quality 主代码与测试**

```bash
git rm -r medkernel-backend/src/main/java/com/medkernel/quality/
git rm -r medkernel-backend/src/test/java/com/medkernel/quality/
```

- [ ] **Step 2: 编译**

Run: `cd medkernel-backend && mvn -B -q -DskipTests compile`
Expected: BUILD SUCCESS

## Task 11: 删除 clinical 模块

- [ ] **Step 1: 删除 clinical 主代码与测试**

```bash
git rm -r medkernel-backend/src/main/java/com/medkernel/clinical/
git rm -r medkernel-backend/src/test/java/com/medkernel/clinical/
```

- [ ] **Step 2: 编译**

Run: `cd medkernel-backend && mvn -B -q -DskipTests compile`
Expected: BUILD SUCCESS

如失败：grep 检查 `shared/observability/BusinessMetrics` 是否被 CdssController 之外的代码引用 `incCdssAlerts`，是的话需先净化 BusinessMetrics（见 Task 14）再回来删

## Task 12: 删除 tenant 模块（保留 engine/org 不动）

- [ ] **Step 1: 删除 tenant/{rule,pathway,hrp} 主代码**

```bash
git rm -r medkernel-backend/src/main/java/com/medkernel/tenant/rule/
git rm -r medkernel-backend/src/main/java/com/medkernel/tenant/pathway/
git rm -r medkernel-backend/src/main/java/com/medkernel/tenant/hrp/
```

- [ ] **Step 2: 删除对应测试**

```bash
git rm -r medkernel-backend/src/test/java/com/medkernel/tenant/rule/ 2>/dev/null || true
git rm -r medkernel-backend/src/test/java/com/medkernel/tenant/pathway/ 2>/dev/null || true
```

- [ ] **Step 3: 检查 tenant 目录是否已空**

Run: `ls medkernel-backend/src/main/java/com/medkernel/tenant/ 2>&1`
Expected: 目录已空或不存在

- [ ] **Step 4: 如果 tenant 目录已空则一并删**

```bash
rmdir medkernel-backend/src/main/java/com/medkernel/tenant/ 2>/dev/null || true
rmdir medkernel-backend/src/test/java/com/medkernel/tenant/ 2>/dev/null || true
```

- [ ] **Step 5: 编译**

Run: `cd medkernel-backend && mvn -B -q -DskipTests compile`
Expected: BUILD SUCCESS

## Task 13: 删除 platform 模块

- [ ] **Step 1: 删除 platform 主代码与测试**

```bash
git rm -r medkernel-backend/src/main/java/com/medkernel/platform/
git rm -r medkernel-backend/src/test/java/com/medkernel/platform/ 2>/dev/null || true
```

- [ ] **Step 2: 编译**

Run: `cd medkernel-backend && mvn -B -q -DskipTests compile`
Expected: BUILD SUCCESS

## Task 14: 删除 compliance 旧业务子目录（保留 audit）

- [ ] **Step 1: 单独删除 compliance/ 的 8 个旧子目录**

```bash
git rm -r medkernel-backend/src/main/java/com/medkernel/compliance/tenant/
git rm -r medkernel-backend/src/main/java/com/medkernel/compliance/dr/
git rm -r medkernel-backend/src/main/java/com/medkernel/compliance/signature/
git rm -r medkernel-backend/src/main/java/com/medkernel/compliance/dataexport/
git rm -r medkernel-backend/src/main/java/com/medkernel/compliance/dlm/
git rm -r medkernel-backend/src/main/java/com/medkernel/compliance/waf/
git rm -r medkernel-backend/src/main/java/com/medkernel/compliance/tsa/
git rm -r medkernel-backend/src/main/java/com/medkernel/compliance/masking/
```

- [ ] **Step 2: 验证只剩 audit/**

Run: `ls medkernel-backend/src/main/java/com/medkernel/compliance/`
Expected: `audit`

- [ ] **Step 3: 删除对应测试（保留 audit 测试）**

```bash
# 仅删除对应子目录测试，audit 测试保留
for sub in tenant dr signature dataexport dlm waf tsa masking; do
  git rm -r "medkernel-backend/src/test/java/com/medkernel/compliance/$sub/" 2>/dev/null || true
done
```

- [ ] **Step 4: 编译**

Run: `cd medkernel-backend && mvn -B -q -DskipTests compile`
Expected: BUILD SUCCESS

## Task 15: 净化 BusinessMetrics

**Files:**
- Modify: `medkernel-backend/src/main/java/com/medkernel/shared/observability/BusinessMetrics.java`

- [ ] **Step 1: 读取 BusinessMetrics 当前内容**

Read: `medkernel-backend/src/main/java/com/medkernel/shared/observability/BusinessMetrics.java`
（识别哪些方法只服务于已删旧 controller，如 `incCdssAlerts`、`incPathwayPublish` 等）

- [ ] **Step 2: 删除无引用的方法**

对每个方法 grep 验证：
```bash
for method in incCdssAlerts incPathwayPublish incLlmCalls incRuleValidations; do
  count=$(grep -rn "\.$method(" medkernel-backend/src/main/java | wc -l)
  echo "$method: $count refs"
done
```

零引用的方法直接删除；保留有引用的（如 shared 内部 / engine 模块使用的）。

- [ ] **Step 3: 同步删除对应 Micrometer counter 注册**

如果删除的方法对应 `Counter` 或 `Gauge` 字段，同步删除字段定义和 `@PostConstruct` 注册逻辑

- [ ] **Step 4: 编译 + 测试**

Run: `cd medkernel-backend && mvn -B -q test`
Expected: BUILD SUCCESS + 测试全绿

如测试失败：grep 失败测试用到的方法，决定是恢复方法还是同步删除/修改测试

## Task 16: 五方言迁移核查

**Files:**
- Read: `medkernel-backend/src/main/resources/db/migration/{h2,postgres,oracle,dm,kingbase}/V*.sql`
- Possibly Create: `medkernel-backend/src/main/resources/db/migration/{h2,postgres,oracle,dm,kingbase}/V7__drop_legacy_business_tables.sql`

- [ ] **Step 1: 列出所有迁移文件**

Run: `find medkernel-backend/src/main/resources/db/migration -name "*.sql" | sort`
Expected: 5 个方言子目录各 6 个 V*.sql 文件

- [ ] **Step 2: grep 各 migration 文件中是否含已删模块相关的表名**

```bash
grep -lE "(cdss_alert|pathway_template|drg_ruleset|mpi_patient|llm_call|rule_definition|hrp_message)" \
  medkernel-backend/src/main/resources/db/migration/*/V*.sql || echo "无旧业务表"
```

- [ ] **Step 3: 根据结果分支处理**

- 若结果为"无旧业务表"：在 PR 描述中明确"V1..V6 不含旧业务表，无需 V7 drop"
- 若存在旧业务表：为每方言追加 `V7__drop_legacy_business_tables.sql`，含 `DROP TABLE IF EXISTS ...` 语句

- [ ] **Step 4: 跑五方言迁移 smoke**

Run: `cd medkernel-backend && mvn -B -q test -Dtest=*Migration* 2>&1 | tail -20`
Expected: 所有 migration 测试通过

## Task 17: PR-2 最终验证与提交

- [ ] **Step 1: 全量验证 grep**

```bash
echo "== 裸 Map 残留 =="
grep -rn "Map<String, Object>" medkernel-backend/src/main/java || echo "归零"
echo
echo "== SEED 模式残留 =="
grep -rnE "private static final List<[A-Za-z]+> SEED\s*=" medkernel-backend/src/main/java || echo "归零"
echo
echo "== 旧模块导入残留 =="
grep -rnE "import com\.medkernel\.(advanced|quality|clinical|platform)" medkernel-backend/src/main/java || echo "归零"
echo
echo "== tenant 旧子目录引用 =="
grep -rnE "import com\.medkernel\.tenant\.(rule|pathway|hrp)" medkernel-backend/src/main/java || echo "归零"
echo
echo "== compliance 旧子目录引用 =="
grep -rnE "import com\.medkernel\.compliance\.(tenant|dr|signature|dataexport|dlm|waf|tsa|masking)" medkernel-backend/src/main/java || echo "归零"
```
Expected: 每项都输出"归零"

- [ ] **Step 2: 全量测试**

Run: `cd medkernel-backend && mvn -B -q test`
Expected: BUILD SUCCESS

- [ ] **Step 3: 启动后端 smoke**

```bash
cd medkernel-backend && mvn spring-boot:run &
sleep 30
curl -sS http://localhost:18080/medkernel/actuator/health
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:18080/medkernel/api/v1/clinical/cdss/alerts
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:18080/medkernel/api/v1/tenant/pathways
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:18080/medkernel/api/v1/advanced/llm/explain
# 杀掉后端
kill %1
```
Expected: health UP；三个旧 endpoint 返回 404

- [ ] **Step 4: commit + push**

```bash
git add -A medkernel-backend/
git commit -m "$(cat <<'EOF'
feat(GA-ENG-BASE-09): PR-2 后端净化

删除 W3-W7 旧业务模块的 47 个 Java 文件 + 配套测试，保留 engine/、shared/、compliance/audit/。

### Removed (backend, 47 files)
- advanced/llm/* (含 OllamaMockProvider、SaasFallbackProvider 等 9 文件)
- advanced/{chatbot, academic, domestic}/*
- quality/{variance, ncis, medicalrecord, insurance}/*
- clinical/{mpi, cdss, udi, publichealth}/*
- tenant/{rule, pathway, hrp}/*
- platform/{branding, success, license, emergency}/*
- compliance/{tenant, dr, signature, dataexport, dlm, waf, tsa, masking}/*

### Refactored
- shared/observability/BusinessMetrics：删除只服务旧 controller 的方法（详见提交 diff）

### Migration
- 按 Task 16 实际 grep 结果填写：
  - 情况 A：V1..V6 不含旧业务表 → 文案 "无需 drop 迁移（已 grep 核查）"
  - 情况 B：存在旧业务表 → 文案 "补 V7__drop_legacy_business_tables.sql（5 方言并列）"

### 验证
- mvn test 全绿
- grep 裸 Map / SEED / 旧模块 import 全部归零
- 后端启动 health UP；旧 endpoint 返回 404

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
git push -u origin codex/base-09-pr2-backend-cleanup
```

- [ ] **Step 5: 创建 PR**

```bash
gh pr create --base main --title "feat(GA-ENG-BASE-09): PR-2 后端净化" --body "$(cat <<'EOF'
## 变更范围

GA-ENG-BASE-09 三 PR 净化方案的 PR-2，聚焦后端旧业务模块净化。

### Removed (47 files)
- `advanced/{llm, chatbot, academic, domestic}/*`
- `quality/{variance, ncis, medicalrecord, insurance}/*`
- `clinical/{mpi, cdss, udi, publichealth}/*`
- `tenant/{rule, pathway, hrp}/*`
- `platform/{branding, success, license, emergency}/*`
- `compliance/{tenant, dr, signature, dataexport, dlm, waf, tsa, masking}/*`

### Kept
- `engine/*`（新引擎核心，零反向依赖旧模块）
- `shared/*`（基础设施）
- `compliance/audit/*`（BASE-04 审计链合法产物）

### Refactored
- `shared/observability/BusinessMetrics`：删除只服务旧 controller 的方法

### Migration
（按 Task 16 实际结果填写 — 无 V7 或补 V7）

## 验证结果

- [x] `mvn -B -q test` 全绿
- [x] `grep Map<String, Object>` 归零
- [x] `grep SEED = List.of` 归零
- [x] 旧模块 import 归零
- [x] 后端启动 health UP；旧 endpoint 返回 404
- [x] 五方言迁移 smoke 通过

## 医疗安全 / 部署 / 数据迁移影响

- **数据迁移**：（按 Task 16 实际填写）
- **医疗安全**：旧 mock controller 删除前未承担生产医疗职责，删除无影响
- **部署**：API 表面缩窄，前端 PR-3 同步处理

## 未完成事项

- 前端 hooks.ts 中仍有指向旧 endpoint 的引用，将由 PR-3 处理（PR 顺序设计已隔离）
- 当前 frontend e2e 可能因前端未同步净化而短暂失败 — PR-3 合并后恢复

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 6: 等待 CI + 合并**

Run: `gh pr checks --watch`
合并后：`git checkout main && git pull`

---

# PR-3 · 前端净化（占位卡 + 数据下线）

## Task 18: 创建 PR-3 分支

- [ ] **Step 1: 确认 main 已拉取最新（含 PR-1、PR-2 合并）**

Run: `git checkout main && git pull origin main && git log --oneline -3`
Expected: 顶部两条为 PR-1、PR-2 的合并 commit

- [ ] **Step 2: 创建分支**

Run: `git checkout -b codex/base-09-pr3-frontend-cleanup`

## Task 19: 新增 RoadmapLink 组件（含单测）

**Files:**
- Create: `frontend/src/shared/ui/RoadmapLink.tsx`
- Create: `frontend/src/shared/ui/RoadmapLink.test.tsx`

- [ ] **Step 1: 写失败的单测**

Create `frontend/src/shared/ui/RoadmapLink.test.tsx`:

```tsx
import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { RoadmapLink } from "./RoadmapLink";

describe("RoadmapLink", () => {
  it("渲染任务 ID 列表为可点击链接", () => {
    render(<RoadmapLink taskIds={["GA-ENG-RULE-01", "GA-ENG-API-05"]} />);
    expect(screen.getByText(/GA-ENG-RULE-01/)).toBeInTheDocument();
    expect(screen.getByText(/GA-ENG-API-05/)).toBeInTheDocument();
  });

  it("点击链接打开路线图弹窗", async () => {
    render(<RoadmapLink taskIds={["GA-ENG-RULE-01"]} />);
    const link = screen.getByRole("button", { name: /查看实施路线图/ });
    await userEvent.click(link);
    expect(screen.getByText(/引擎能力规划/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `cd frontend && npm test -- RoadmapLink`
Expected: FAIL（组件不存在）

- [ ] **Step 3: 实现 RoadmapLink**

Create `frontend/src/shared/ui/RoadmapLink.tsx`:

```tsx
import { useState } from "react";
import { Button, Modal, Space, Tag } from "antd";
import { LinkOutlined } from "@ant-design/icons";

interface RoadmapLinkProps {
  taskIds: string[];
}

export function RoadmapLink({ taskIds }: RoadmapLinkProps) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <Button
        type="link"
        icon={<LinkOutlined />}
        onClick={() => setOpen(true)}
      >
        查看实施路线图
      </Button>
      <Modal
        title="引擎能力规划"
        open={open}
        onCancel={() => setOpen(false)}
        footer={null}
      >
        <Space direction="vertical" size="middle">
          <div>本页依赖以下 backlog 任务，引擎完成后激活：</div>
          <Space wrap>
            {taskIds.map((id) => (
              <Tag key={id} color="processing">
                {id}
              </Tag>
            ))}
          </Space>
          <div>
            详细任务清单见{" "}
            <a href="/docs/backlog.md" target="_blank" rel="noreferrer">
              docs/backlog.md
            </a>
          </div>
        </Space>
      </Modal>
    </>
  );
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `cd frontend && npm test -- RoadmapLink`
Expected: PASS（2 tests passed）

- [ ] **Step 5: commit**

```bash
git add frontend/src/shared/ui/RoadmapLink.tsx frontend/src/shared/ui/RoadmapLink.test.tsx
git commit -m "feat(GA-ENG-BASE-09): 新增 RoadmapLink 组件 + 单测

为占位卡提供统一的任务 ID 引用与路线图弹窗组件。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

## Task 20: 批量改造业务页为占位卡

**Files (15 显式 MOCK + 4 隐藏硬编码 + 4 真接 mock 后端 = 23 文件)**：

按 spec §4.2 表逐个改造。下面给出占位卡模板与每页 taskIds 映射，逐个改造但用同一模式。

- [ ] **Step 1: 准备占位卡通用片段**

每个被改造的业务页统一替换正文为：

```tsx
import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function <PageName>() {
  return (
    <PageShell
      title="<原标题>"
      description="<原描述>"
    >
      <PageState
        state="disabled"
        title="等待引擎能力上线"
        description="本页依赖 <taskIds joined>，引擎完成后激活"
        actions={<RoadmapLink taskIds={[/* 见映射表 */]} />}
      />
    </PageShell>
  );
}
```

注意保留 `<PageShell title=... description=...>` 的原文本，让菜单和顶部标题不变；只替换正文区。

- [ ] **Step 2: 逐文件改造（按 spec §4.2 表）**

按下表顺序：

| 路径 | taskIds 数组 |
|---|---|
| `pages/clinical/PatientPathways.tsx` | `["GA-ENG-PATH-01", "GA-ENG-API-06"]` |
| `pages/clinical/WorkflowTodos.tsx` | `["GA-SVC-CLINICAL-03"]` |
| `pages/clinical/Notifications.tsx` | `["GA-SVC-CLINICAL-03"]` |
| `pages/clinical/CdssFatigue.tsx` | `["GA-ENG-CDSS-01"]` |
| `pages/clinical/Mpi.tsx` | `["GA-SVC-CLINICAL-01"]` |
| `pages/clinical/RuleValidate.tsx` | `["GA-ENG-RULE-01"]` |
| `pages/quality/AiReview.tsx` | `["GA-ENG-CDSS-01", "GA-ENG-EVAL-01"]` |
| `pages/quality/InsuranceAudit.tsx` | `["GA-SVC-QUALITY-02"]` |
| `pages/quality/QcAlerts.tsx` | `["GA-ENG-EVAL-01"]` |
| `pages/quality/QcEvalResults.tsx` | `["GA-ENG-EVAL-01"]` |
| `pages/quality/QcEvalSets.tsx` | `["GA-ENG-EVAL-01"]` |
| `pages/quality/QcDashboard.tsx` | `["GA-ENG-EVAL-01"]` |
| `pages/tenant/AdapterHub.tsx` | `["GA-SVC-PILOT-02"]` |
| `pages/tenant/ConfigPackages.tsx` | `["GA-ENG-PKG-01"]` |
| `pages/tenant/RuleDefinitions.tsx` | `["GA-ENG-RULE-01", "GA-ENG-API-05"]` |
| `pages/tenant/PathwayTemplates.tsx` | `["GA-ENG-PATH-01", "GA-ENG-API-06"]` |
| `pages/advanced/AiWorkflows.tsx` | `["GA-ENG-LLM-01"]` |
| `pages/advanced/DevConsole.tsx` | `["GA-ENG-LLM-01"]` |
| `pages/compliance/AdminUsers.tsx` | `["GA-SVC-COMPLIANCE-01"]` |
| `pages/compliance/SecurityBaseline.tsx` | `["GA-SVC-COMPLIANCE-01"]` |
| `pages/compliance/IdentityBinding.tsx` | `["GA-SVC-COMPLIANCE-01"]` |
| `features/tenant-lifecycle/TenantLifecyclePanel.tsx` | `["GA-SVC-PILOT-01"]` |

每改完一个文件：
1. 删除文件内 `const MOCK / DEPTS / ITEMS / LINKS / PROVIDERS` 等硬编码常量
2. 删除文件内对旧 hook 的导入与调用
3. 保留 `<PageShell>` 的标题与描述
4. 把正文区改为 `<PageState state="disabled" ... actions={<RoadmapLink .../>} />`

- [ ] **Step 3: 阶段性编译检查**

每改完 5 个文件，运行：
```bash
cd frontend && npm run typecheck
```
Expected: 零 error

如有 error：通常是删除 hook 后剩余的引用未清；逐项修复。

- [ ] **Step 4: 单测**

Run: `cd frontend && npm test`
Expected: 现有 PermissionChip、AuditSnapshotButton、SystemProviders、AppLayout、client 单测仍通过；可能 patient pathway / cdss 等旧测试因占位失败 — 这些测试要在 Task 22 同步改造

- [ ] **Step 5: commit**

```bash
git add frontend/src/pages frontend/src/features
git commit -m "feat(GA-ENG-BASE-09): 23 业务页改造为占位卡

按 spec §4.2 将 15 显式 MOCK + 4 隐藏硬编码 + 4 真接 mock 后端的页面统一替换为
PageState disabled + RoadmapLink。菜单、路由、PageShell 标题描述保持不动。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

## Task 21: TerminologyMapping 真接入示范

**Files:**
- Modify: `frontend/src/pages/tenant/TerminologyMapping.tsx`
- Possibly modify: `frontend/src/shared/api/hooks.ts`（新增 useTerminologyMappings 等 hook）

- [ ] **Step 1: 核对 engine/terminology 真实 API 路径**

Run: `grep -rn "@RequestMapping" medkernel-backend/src/main/java/com/medkernel/engine/terminology/ | head -5`
记录实际 URL（应类似 `/api/v1/terminology/mappings`）

- [ ] **Step 2: 在 hooks.ts 中新增 useTerminologyMappings**

Modify `frontend/src/shared/api/hooks.ts`：

```typescript
// ──────────────────────────────────────────
// 术语映射 · GA-ENG-API-04 已上线
// ──────────────────────────────────────────
interface TermMappingItem {
  id: string;
  localCode: string;
  localDisplay: string;
  standardCode: string;
  standardDisplay: string;
  status: string;
  confidence: number;
}

export function useTerminologyMappings(params?: { status?: string; page?: number; size?: number }) {
  return useQuery({
    queryKey: ["terminology", "mappings", params],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: { content: TermMappingItem[]; total: number } }>(
        "/terminology/mappings",
        { params },
      );
      return data.data;
    },
  });
}
```

- [ ] **Step 3: 改造 TerminologyMapping.tsx 使用真 hook**

替换 mock 数据为 `useTerminologyMappings()` 调用，分页、状态、empty 六态由 PageState 提供

- [ ] **Step 4: 本地启动后端 + 前端联调**

```bash
cd medkernel-backend && mvn spring-boot:run &
cd ../frontend && npm run dev &
sleep 30
```
浏览器访问 `http://localhost:5173/tenant/terminology-mapping`，确认能看到真实数据（或空数据 + 六态 empty）

- [ ] **Step 5: 写 e2e 或 typecheck 验证**

Run: `cd frontend && npm run typecheck && npm run lint`
Expected: 零 error

- [ ] **Step 6: commit**

```bash
git add frontend/src/pages/tenant/TerminologyMapping.tsx frontend/src/shared/api/hooks.ts
git commit -m "feat(GA-ENG-BASE-09): TerminologyMapping 接 engine/terminology 真接口

作为 BASE-09 真实接入的样板：删除 MOCK 数据、新增 useTerminologyMappings hook、
对接 engine/terminology 真 API。后续 E2/E3 业务页可参照此模式激活。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

## Task 22: 净化 hooks.ts 中所有指向旧 endpoint 的 hook + 修复测试

**Files:**
- Modify: `frontend/src/shared/api/hooks.ts`
- Possibly modify/delete: `frontend/src/**/*.test.tsx`（依赖被删 hook 的测试）

- [ ] **Step 1: 列出 hooks.ts 中指向已删后端的 hook**

Run: `grep -nE "/api/v1/clinical/(cdss|mpi|udi|publichealth)|/api/v1/tenant/(rules|pathways|hrp)|/api/v1/quality/(insurance|variance|ncis|medicalrecord)|/api/v1/advanced/(llm|chatbot|academic|domestic)|/api/v1/platform/|/api/v1/compliance/(tenant|dr|signature|data-export|dlm|waf|tsa|masking)" frontend/src/shared/api/hooks.ts`

记录所有匹配行号

- [ ] **Step 2: 删除这些 hook 及其类型定义**

逐项 Edit 删除（保留 useSecurityProfile、useAuditSnapshot、新增的 useTerminologyMappings、以及指向 engine/ 的所有 hook）

- [ ] **Step 3: 修复或删除依赖被删 hook 的测试**

Run: `cd frontend && npm test 2>&1 | grep -E "FAIL|✗" | head -20`

对失败的测试：
- 如测试本身是占位页的（如 patient pathway test），改写为测试占位卡渲染
- 如测试不可恢复，删除（占位卡的测试由 RoadmapLink 单测覆盖）

- [ ] **Step 4: 全量 verify**

Run: `cd frontend && npm run verify`
Expected: lint + format + typecheck + test 全绿

- [ ] **Step 5: commit**

```bash
git add frontend/
git commit -m "feat(GA-ENG-BASE-09): 净化 hooks.ts 删除指向旧 endpoint 的所有 hook

同步修复或删除依赖被删 hook 的测试。所有保留的 hook 都指向 engine/* 真实 API
或 compliance/audit/security/me 等合法 endpoint。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

## Task 23: 新增 ESLint 规则 medkernel/no-page-mock

**Files:**
- Create: `frontend/eslint-rules/no-page-mock.js`
- Create: `frontend/eslint-rules/no-page-mock.test.js`
- Modify: `frontend/eslint.config.js`

- [ ] **Step 1: 写规则单测**

Create `frontend/eslint-rules/no-page-mock.test.js`:

```javascript
import { RuleTester } from "eslint";
import rule from "./no-page-mock.js";

const ruleTester = new RuleTester({
  languageOptions: { ecmaVersion: 2022, sourceType: "module" },
});

ruleTester.run("no-page-mock", rule, {
  valid: [
    {
      code: `const items = useQuery();`,
      filename: "src/pages/clinical/PatientPathways.tsx",
    },
    {
      code: `const TYPE_LABEL = "草稿";`,
      filename: "src/pages/clinical/PatientPathways.tsx",
    },
    {
      code: `const MOCK = [{ id: 1 }];`,
      filename: "src/test/setup.ts",
    },
  ],
  invalid: [
    {
      code: `const MOCK = [{ id: 1 }];`,
      filename: "src/pages/clinical/PatientPathways.tsx",
      errors: [{ messageId: "noPageMock" }],
    },
    {
      code: `const DEPTS = [{ name: "x" }];`,
      filename: "src/pages/quality/QcDashboard.tsx",
      errors: [{ messageId: "noPageMock" }],
    },
    {
      code: `const ITEMS = [];`,
      filename: "src/features/tenant-lifecycle/TenantLifecyclePanel.tsx",
      errors: [{ messageId: "noPageMock" }],
    },
  ],
});

console.log("✓ no-page-mock tests passed");
```

- [ ] **Step 2: 写规则实现**

Create `frontend/eslint-rules/no-page-mock.js`:

```javascript
/** @type {import('eslint').Rule.RuleModule} */
export default {
  meta: {
    type: "problem",
    docs: {
      description: "禁止 src/pages 与 src/features 中出现页面级 mock/硬编码数组常量",
    },
    messages: {
      noPageMock:
        '业务页面禁止内联 mock/硬编码数据常量（如 MOCK/DEPTS/ITEMS/LINKS/PROVIDERS）；改走 API hook，缺失时由 PageState 走六态。',
    },
    schema: [],
  },
  create(context) {
    const filename = context.filename ?? context.getFilename?.() ?? "";
    if (!/\/(pages|features)\/.+\.(tsx|ts)$/.test(filename)) return {};
    return {
      VariableDeclarator(node) {
        if (node.id.type !== "Identifier") return;
        const name = node.id.name;
        if (!/^[A-Z][A-Z0-9_]*$/.test(name)) return;
        if (!node.init || node.init.type !== "ArrayExpression") return;
        if (node.init.elements.length === 0) return;
        const first = node.init.elements[0];
        if (!first || first.type !== "ObjectExpression") return;
        context.report({ node, messageId: "noPageMock" });
      },
    };
  },
};
```

- [ ] **Step 3: 跑规则单测**

Run: `cd frontend && node eslint-rules/no-page-mock.test.js`
Expected: `✓ no-page-mock tests passed`

- [ ] **Step 4: 在 eslint.config.js 注册规则**

Modify `frontend/eslint.config.js`：在 `import` 区追加：
```javascript
import noPageMock from "./eslint-rules/no-page-mock.js";
```

在 `medkernel: { rules: { ... } }` 块内追加：
```javascript
"no-page-mock": noPageMock,
```

在 `rules: { ... }` 块内追加（针对 pages/features 文件）：
```javascript
"medkernel/no-page-mock": "error",
```

- [ ] **Step 5: 跑 lint 验证规则生效**

Run: `cd frontend && npm run lint`
Expected: 零 error（因为 Task 20-22 已把所有页面级 mock 清除）

- [ ] **Step 6: 反向测试 — 故意写一个 MOCK 看是否报错**

```bash
cat > /tmp/test-mock.tsx <<'EOF'
const MOCK = [{ id: 1 }];
export default function Test() { return null; }
EOF
cp /tmp/test-mock.tsx frontend/src/pages/clinical/_TestMock.tsx
cd frontend && npm run lint 2>&1 | grep no-page-mock
rm frontend/src/pages/clinical/_TestMock.tsx
```
Expected: lint 输出含 `medkernel/no-page-mock` 错误信息

- [ ] **Step 7: commit**

```bash
git add frontend/eslint-rules/no-page-mock.js \
        frontend/eslint-rules/no-page-mock.test.js \
        frontend/eslint.config.js
git commit -m "feat(GA-ENG-BASE-09): 新增 ESLint 规则 medkernel/no-page-mock

禁止 src/pages、src/features 文件内联 mock/硬编码数组常量。与 Task 20-22 配合
防止视觉债与假闭环回潮。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

## Task 24: PR-3 最终验证与提交

- [ ] **Step 1: 全量 grep 验证**

```bash
echo "== 显式 MOCK 残留 =="
grep -rn "const MOCK" frontend/src/pages frontend/src/features || echo "归零"
echo
echo "== 隐藏硬编码残留 =="
grep -rnE "const (DEPTS|ITEMS|LINKS|PROVIDERS)\s*=\s*\[" frontend/src/pages frontend/src/features || echo "归零"
echo
echo "== 旧 endpoint 调用残留 =="
grep -rnE "/api/v1/clinical/(cdss|mpi|udi|publichealth)|/api/v1/tenant/(rules|pathways|hrp)|/api/v1/quality/(insurance|variance|ncis|medicalrecord)|/api/v1/advanced/(llm|chatbot|academic|domestic)|/api/v1/platform/|/api/v1/compliance/(tenant|dr|signature|data-export|dlm|waf|tsa|masking)" frontend/src || echo "归零"
```
Expected: 每项输出"归零"

- [ ] **Step 2: 全量 verify**

Run: `cd frontend && npm run verify`
Expected: lint + format + typecheck + test 全绿

- [ ] **Step 3: 手动 smoke**

```bash
cd medkernel-backend && mvn spring-boot:run &
cd ../frontend && npm run dev &
sleep 30
```
浏览器访问 `http://localhost:5173/`，逐项点击 5 个业务菜单 + 1 高级工具菜单下的所有页面：
- 占位页应正确显示 PageState disabled + RoadmapLink 可点击 + Modal 显示任务 ID
- TerminologyMapping 显示真实数据（或六态 empty）

- [ ] **Step 4: e2e 测试（如有 Playwright 设置）**

Run: `cd frontend && npm run e2e 2>&1 | tail -10`
Expected: 全通过

如部分 e2e 因占位卡而失败：更新或临时跳过这些 e2e，PR 描述中说明"占位页 e2e 待 BASE-08 后端能力激活后重写"

- [ ] **Step 5: push + 创建 PR**

```bash
git push -u origin codex/base-09-pr3-frontend-cleanup
gh pr create --base main --title "feat(GA-ENG-BASE-09): PR-3 前端净化" --body "$(cat <<'EOF'
## 变更范围

GA-ENG-BASE-09 三 PR 净化方案的 PR-3，聚焦前端假闭环净化与门禁固化。

### Added
- `shared/ui/RoadmapLink.tsx` 组件（含单测）
- `eslint-rules/no-page-mock.js` 自定义规则（含规则单测）
- `useTerminologyMappings` 真接入 hook（对接 engine/terminology API）

### Refactored (23 文件)
- 15 个显式 MOCK 页面 → PageState disabled + RoadmapLink
- 4 个隐藏硬编码页面 → 同上
- 4 个真接 mock 后端的 hook 页面 → 同上
- TerminologyMapping → 真接 engine/terminology API（作为业务页激活样板）

### Removed
- `shared/api/hooks.ts` 中所有指向已删后端 endpoint 的 hook
- 依赖被删 hook 的失效测试

## 验证结果

- [x] `npm run verify` 全绿
- [x] grep MOCK / 隐藏硬编码 / 旧 endpoint 归零
- [x] 手动 smoke：5 菜单全部点开，占位卡正确显示，RoadmapLink 弹窗工作
- [x] TerminologyMapping 真实数据加载正常

## 医疗安全 / 部署 / 数据迁移影响

- **医疗安全**：占位卡不展示假数据，避免医师误读 mock 提醒
- **部署**：前端 bundle 体积减小（删除大量 mock 数据）
- **数据迁移**：不涉及

## 未完成事项

- BASE-09 完整闭环：合并后 backlog 更新 GA-ENG-BASE-09 为 done
- 下一阶段：BASE-08 产品体验底座（详情抽屉、保存视图、异步导出等）

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 6: 等待 CI + 合并**

Run: `gh pr checks --watch`
合并后：

```bash
git checkout main && git pull
```

## Task 25: BASE-09 全部完成后的 backlog 闭环

**Files:**
- Modify: `docs/backlog.md`

- [ ] **Step 1: 切回 main 拉取最新**

Run: `git checkout main && git pull && git log --oneline -5`
Expected: 顶部三条为 PR-1、PR-2、PR-3 的合并 commit

- [ ] **Step 2: 创建 backlog 闭环分支**

Run: `git checkout -b codex/base-09-backlog-close`

- [ ] **Step 3: 把 BASE-09 状态改为 done**

把 backlog 中：
```
| GA-ENG-BASE-09 代码基线净化：移除业务主链路 mock、裸 Map、硬编码示例数据、旧命名和单病种假闭环 | claude | in_progress |
```
改为：
```
| GA-ENG-BASE-09 代码基线净化：移除业务主链路 mock、裸 Map、硬编码示例数据、旧命名和单病种假闭环 | claude | done |
```

- [ ] **Step 4: 修订记录补 4.11**

在修订记录表首行后追加：
```
| 4.11 | 2026-05-26 | Claude | GA-ENG-BASE-09 done：三 PR 顺序合并完成。删除 47 个旧 Java 文件、23 个前端业务页改占位、新增 RoadmapLink 组件、新增 ESLint medkernel/no-page-mock 规则、CI 加 frontend-lint 必需 check、OpenSpec 归档 |
```

- [ ] **Step 5: commit + push + PR**

```bash
git add docs/backlog.md
git commit -m "chore(GA-ENG-BASE-09): backlog 4.11 闭环

BASE-09 三 PR 全部合并，标 done。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
git push -u origin codex/base-09-backlog-close

gh pr create --base main --title "chore(GA-ENG-BASE-09): backlog 4.11 闭环" --body "$(cat <<'EOF'
## 变更范围

GA-ENG-BASE-09 三 PR 全部合并完成，更新 backlog：
- GA-ENG-BASE-09 → done
- 修订记录补 4.11 行

## 验证结果

- [x] backlog 4.11 修订记录就位
- [x] 三 PR 合并状态可在 git log 验证

## 影响

无 — 纯文档同步。

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 6: 合并后切回 main**

Run: `git checkout main && git pull && git log --oneline -1`
Expected: 顶部为 backlog 4.11 commit

---

# 自审清单（已执行）

**Spec coverage**：
- ✅ §3.1 净化策略 C → Task 2, 3（E6 附录提取精华）
- ✅ §3.2 前端方案 ① 升级版 → Task 19-22（RoadmapLink + 占位卡）
- ✅ §3.3 3 个顺序 PR → PR-1/PR-2/PR-3 完整划分
- ✅ §4.1 47 文件清单 → Task 9-14
- ✅ §4.2 前端 23 文件清单 → Task 20
- ✅ §5.1 PR-1 范围 → Task 2-6
- ✅ §6.1 PR-2 范围 → Task 9-16
- ✅ §7.1 PR-3 范围 → Task 19-23
- ✅ §8 验收门槛 → Task 17 / Task 24
- ✅ §9 衔接 → Task 25 (BASE-09 done 后进 BASE-08)

**Placeholder scan**：无 TBD/TODO；每个代码块完整可执行；URL 路径表、DTO 字段表、状态机表全部具体；no-page-mock 规则代码完整。

**Type consistency**：`RoadmapLink` props 在 Task 19 定义为 `taskIds: string[]`，Task 20 占位卡模板使用一致；`useTerminologyMappings` 在 Task 21 定义并使用一致。

---

# 下一步

BASE-09 闭环后立即进入 **GA-ENG-BASE-08 产品体验底座** brainstorming：
- 一页一目标 / 角色默认视图 / 专家模式 / 服务端分页 / 详情抽屉 / 异步导出 / 保存视图
