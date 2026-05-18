# PR 检查清单

> ⚠️ **任何一项不勾不允许合并。**

## 0. 任务信息

- **PR 编号**：PR-V2-XX（或其它任务编号）
- **AI 等级**：junior / middle / senior
- **AI 代号**：（如 Claude-Sonnet-4.7-20260518）
- **claim_id**：ai-dev-input/10_task_claims/active/...
- **review_id**：ai-dev-input/11_ai_reviews/pending/...
- **feature_acceptance_id**：ai-dev-input/13_feature_acceptance/... （高风险 PR 必填）

## 1. 变更说明

<!-- 1-3 句话说明做了什么 -->



## 2. 一致性检查（必须）

- [ ] `.\scripts\verify-task-prereq.ps1 -TaskId PR-V2-XX -Level <level>` 接手前通过
- [ ] `.\scripts\verify-pr.ps1 -TaskId PR-V2-XX` 提交前通过
- [ ] 我读过对应的 [ADR](../docs/engineering/adr/)（列出读过哪些）：
  - ADR-XXXX
- [ ] 我读过对应的 [参考实现样板](../docs/engineering/reference-implementations/)（列出读过哪些）：
  - <文件名>
- [ ] 未违反 [禁用模式清单](../docs/engineering/forbidden-patterns.md)

## 3. DoD 自检（从 V2 实施手册抽取）

<!-- verify-pr.ps1 会自动抽取对应 PR 卡片的 DoD 检查表，复制到这里逐项勾选 -->

- [ ] DoD 项 1
- [ ] DoD 项 2
- [ ] ...

## 4. 验证结果

### 后端（如适用）
- [ ] `.\medkernel-mvp\scripts\run-tests.ps1` PASS
- [ ] `.\medkernel-mvp\scripts\build.ps1` PASS
- [ ] `.\medkernel-mvp\scripts\verify-encoding.cmd` PASS
- [ ] DDL 同步 4 套（Oracle / DM / PostgreSQL / LOCAL_H2_FILE）

### 前端（如适用）
- [ ] `npm run lint` PASS（含 medkernel/* 自定义规则）
- [ ] `npm test` PASS
- [ ] `npm run build` PASS
- [ ] 浏览器手动验证（截图附在评论）
- [ ] Playwright E2E（如适用）

### 演示数据
- [ ] 已在 `ai-dev-input/06_samples/` 提供可复现 demo 数据
- [ ] 6 大剧本 S1-S6 中本 PR 影响的剧本仍可跑通

## 5. 不变量守护（强制）

- [ ] **不变量 #1**：所有业务表含 `tenant_id`
- [ ] **不变量 #2**：所有写操作含 `platform_user_id`
- [ ] **不变量 #8**：医学/医保/质控配置含来源审核（MISSING_SOURCE 阻断）
- [ ] **不变量 #11**：所有写操作写 `ENGINE_AUDIT_LOG`
- [ ] **不变量 #12**：API 含 `trace_id` 全链路
- [ ] **不变量 #16**：业务服务只调 Provider，不直接依赖具体实现
- [ ] **不变量 #18**：DDL 同步 4 套数据库
- [ ] **不变量 #20**：API 返回 `ApiResult` 统一结构
- [ ] **不变量 #22**：前端实现六态

## 6. AI 协作

- [ ] 只改了 active claim 声明的独占文件
- [ ] `.\medkernel-mvp\scripts\check-ai-collaboration.ps1` PASS
- [ ] 未覆盖其它 AI 未提交的工作
- [ ] commit message 包含 PR-V2-XX 编号

## 7. 临床安全（如涉及）

- [ ] 不涉及临床推荐或规则变更
- [ ] 涉及临床推荐 / 规则 / 知识图谱，已：
  - [ ] 标注 `action_mode`（NOTICE / SOFT / BLOCK）
  - [ ] BLOCK 类规则要求医生填写 `doctor_decision` + 知情同意
  - [ ] 含来源条 `<SourceInfo>`
  - [ ] 演示数据可触发该规则

## 8. review 后归档

- [ ] APPROVED 后将 claim 从 `active/` 移到 `archive/YYYYMMDD/`
- [ ] APPROVED 后将 review 从 `pending/` 移到 `archive/YYYYMMDD/`
- [ ] 任务台账状态从 IN_PROGRESS 改为 DONE，填 commit hash

## 9. 备注

<!-- 任何 review 需要注意的细节 / 已知风险 / 后续 TODO -->
