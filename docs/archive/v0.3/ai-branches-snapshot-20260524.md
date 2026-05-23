# v0.3 ai/GA-* 分支快照 · 2026-05-23

> 用途：v1.0 GA 全量重启前的"保险柜"。这 25 个分支即将被远程删除（已用 legacy tag 备份），本文记录每个分支的 HEAD SHA，以便日后用 `git log <sha>` 查证。
> 备份 tag：`legacy/v0.3-main-20260524`（main HEAD）+ `legacy/v0.3-develop-20260524`（develop HEAD，含 567 commit 历史）

## 分支快照（25 项）

| # | 分支 | HEAD SHA | 日期 | 最近 commit subject |
|---|---|---|---|---|
| 1 | `origin/ai/GA-API-01/openapi-types` | `38157a7` | 2026-05-23 | chore: claim GA-API-01 OpenAPI and frontend type generation |
| 2 | `origin/ai/GA-COMM-01/license-usage` | `4d30035` | 2026-05-23 | GA-COMM-01: License验证+用量报告+到期提醒闭环 |
| 3 | `origin/ai/GA-DB-01/multi-dialect-smoke` | `6e051ef` | 2026-05-23 | feat(GA-DB-01): 多方言 smoke 矩阵和 Flyway rollback 证据 |
| 4 | `origin/ai/GA-DOC-01/user-manuals` | `a3e230c` | 2026-05-23 | GA-DOC-01: 4治理模块用户手册 — 规则/路径/质控/安全 |
| 5 | `origin/ai/GA-DOC-02/ops-handbook` | `09ab5b9` | 2026-05-23 | GA-DOC-02: 运维与应急手册完善 |
| 6 | `origin/ai/GA-DOC-02/ops-manuals` | `4981341` | 2026-05-23 | GA-DOC-02: 领取运维手册任务 |
| 7 | `origin/ai/GA-DOC-03/training-materials` | `41632ca` | 2026-05-23 | GA-DOC-03: 领取培训材料任务 |
| 8 | `origin/ai/GA-DTO-01/adapter-dto` | `e7df392` | 2026-05-23 | GA-DTO-01: Adapter Controller DTO化 |
| 9 | `origin/ai/GA-DTO-02/knowledge-dto` | `76c1b07` | 2026-05-23 | GA-DTO-02: Knowledge/AI review Controller DTO化 |
| 10 | `origin/ai/GA-GOV-01/collaboration-gate` | `8e3b96b` | 2026-05-23 | claim: GA-GOV-01-S01 并发机制硬门禁 |
| 11 | `origin/ai/GA-LEGAL-01/legal-docs` | `ac9bfe3` | 2026-05-23 | GA-LEGAL-01: 法务文档初稿 |
| 12 | `origin/ai/GA-OPS-01/monitoring` | `42fe111` | 2026-05-23 | GA-OPS-01: 领取监控运维任务 |
| 13 | `origin/ai/GA-OPS-01/monitoring-slo` | `36ee9d5` | 2026-05-23 | GA-OPS-01: 认领监控告警与 SLO 任务 |
| 14 | `origin/ai/GA-OPS-01/ops-monitoring` | `f55a431` | 2026-05-23 | feat(GA-OPS-01): 运维监控与 SLO 证据 |
| 15 | `origin/ai/GA-PERF-01/perf-baseline` | `4470d5f` | 2026-05-23 | GA-PERF-01: 领取性能压测报告任务 |
| 16 | `origin/ai/GA-PROD-01/product-simplification` | `8ea87a0` | 2026-05-23 | feat(GA-PROD-01): 产品极简化原则文档 |
| 17 | `origin/ai/GA-QA-01/jacoco-coverage` | `f27da7a` | 2026-05-23 | feat(GA-QA-01): 大幅提升测试覆盖率至51% |
| 18 | `origin/ai/GA-QA-02/frontend-coverage` | `a57c651` | 2026-05-23 | GA-QA-02: 认领前端覆盖率与 CI 任务 |
| 19 | `origin/ai/GA-QA-03/e2e-scenarios` | `0df62dd` | 2026-05-23 | feat(GA-QA-03): 6 大剧本 E2E 测试 |
| 20 | `origin/ai/GA-REFIT-01/file-split` | `589216c` | 2026-05-23 | GA-REFIT-01: 超长文件拆分 — 4个>800行文件拆为31个模块 |
| 21 | `origin/ai/GA-REL-01/branch-protection` | `68b63dd` | 2026-05-23 | GA-REL-01: 认领发布与分支保护证据任务 |
| 22 | `origin/ai/GA-REL-01/release-protection` | `3afd496` | 2026-05-23 | GA-REL-01: 发布与分支保护证据 |
| 23 | `origin/ai/GA-SEC-01/djbp-compliance` | `85a6e7d` | 2026-05-23 | GA-SEC-01: 认领等保 2.0 三级控制点任务 |
| 24 | `origin/ai/GA-UX-01/no-placeholder` | `1ceb122` | 2026-05-23 | GA-UX-01: 客户可见路由无PlaceholderPage |
| 25 | `origin/ai/GA-UX-02/page-simplification` | `c812ffd` | 2026-05-23 | GA-UX-02: 认领全页面极简交互复核任务 |

## 历史保留 tag

| Tag | 指向 | 含义 |
|---|---|---|
| `legacy/v0.3-main-20260524` | `fe8cc0f` | v1.0 GA 重启前的 main 最终态（包含全部 v0.3-final 历史） |
| `legacy/v0.3-develop-20260524` | `29b7d64` | v1.0 GA 重启前的 develop 最终态（含 567 个领先 commit） |

## 查回方式

```bash
# 查看任一旧分支的全部 commit
git log <sha>

# 例如查看 GA-LEGAL-01 法务文档分支
git log ac9bfe3

# 查看 develop 上的全部历史
git log legacy/v0.3-develop-20260524

# 把旧 main 切回来（仅紧急用）
git checkout legacy/v0.3-main-20260524
```

## 关联文档

- [v1.0 GA 重启方案](../../V1_GA_REWRITE_PLAN.md)
- [产品宪法](../../CONSTITUTION.md)
- [新单一任务台账](../../backlog.md)
