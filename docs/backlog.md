# MedKernel v1.0 GA 单一任务台账

> 版本：1.0 · 2026-05-23
> 范围：v1.0 GA 全部任务
> 命名归一：旧 5 套（PR-V2-* / PR-V3-* / PR-FINAL-* / GA-* / DOC-V2-*）已废，本文是唯一权威
> 字段：`id` / `owner` / `status`（pending / in_progress / done / blocked）
> 协作：领单时把 owner 填上 + status 改 in_progress + 同 PR commit

---

## Phase-0 · 基线重置（W1 Day-1~3）

| ID | 名 | owner | status |
|---|---|---|---|
| GA-RESET-01 | 仓库结构重组（保留 git 历史，新目录骨架） | - | pending |
| GA-RESET-02 | trunk-based 切换：废除 develop，main 单线 + feat/* 短分支 | - | pending |
| GA-RESET-03 | 任务台账归一为 docs/backlog.md（本文）| - | done |
| GA-RESET-04 | 旧 5 套命名 + claim/lock + 金本位 → docs/archive/v0.3/ | - | pending |
| GA-RESET-05 | 新 docs/CONSTITUTION.md + 必要 ADR 起步 | - | done |

---

## Phase-1 · 现代化骨架（W1 Day-3~5）

| ID | 名 | owner | status |
|---|---|---|---|
| GA-CORE-01 | JDK 21 + Spring Boot 3.3 + Jakarta EE 9 + Maven 3.9 baseline | - | pending |
| GA-CORE-02 | Spring Security 6 OAuth2/OIDC + 国密 BC-FJA 1.78.1 | - | pending |
| GA-CORE-03 | Spring Data JDBC + Hikari 5 + 5 方言 SPI + Flyway 10 | - | pending |
| GA-CORE-04 | OpenTelemetry 1.41 + Prometheus + Tempo + Loki + Grafana | - | pending |
| GA-CORE-05 | Spring Cloud Gateway 4 + RateLimiter + WAF 链 | - | pending |
| GA-CORE-06 | Global ProblemDetail + Sealed Exception + i18n + KMS Vault | - | pending |
| GA-CORE-07 | Virtual Threads 默认开启 + Tomcat 10 virtual executor | - | pending |
| GA-CORE-08 | 前端 Node 20 + FSD 重组 + 路由元数据 + 4 状态机 Badge + 6 态模板 | - | pending |

---

## Phase-2 · 产品收口（W2）

| ID | 名 | owner | status |
|---|---|---|---|
| GA-PROD-01 | 5 组菜单 + 隐藏式高级工具 + 工作台 1 屏即懂 | - | pending |
| GA-PROD-02 | 4 套统一状态机全平台实装 | - | pending |
| GA-PROD-03 | 7 步极简配置流模板（7 处复用）| - | pending |
| GA-PROD-04 | 默认登录 1 路径 + MFA/SSO/国密折叠区 | - | pending |
| GA-PROD-05 | 全局命令面板（⌘K）| - | pending |
| GA-PROD-06 | 全局"导出审计快照"按钮 | - | pending |
| GA-PROD-07 | 权限指纹 chip | - | pending |
| GA-PROD-08 | 老年医生模式 + 暗黑模式 + 护眼模式 | - | pending |
| GA-PROD-09 | 列管理 + 视图保存 + 视图分享 | - | pending |
| GA-PROD-10 | 客户演示模式（一键 fixture + 隐藏调试 + 水印）| - | pending |
| GA-PROD-11 | 租户生命周期管理（6 阶段 + 工作台面板 + 3 维切片 + 自动推进）| - | pending |

---

## Phase-3 · 业务域实装（W3-W4，5 域 5 AI 并行）

| ID | 名 | owner | status |
|---|---|---|---|
| GA-TENANT-01 | 试点准备域（7 菜单）按 7 步流标准实装 | - | pending |
| GA-CLINICAL-01 | 临床运行域（6 菜单）按统一状态机实装 | - | pending |
| GA-QUALITY-01 | 质控改进域（6 菜单，AI 知识审核 / 知识审核台合并）| - | pending |
| GA-COMPLIANCE-01 | 合规运维域（6 菜单）| - | pending |
| GA-ADVANCED-01 | 高级工具域（5 菜单，含国产化自检 + 开发者控制台）| - | pending |

---

## Phase-4 · 客户必查能力（W5-W6 + 增量 W8）

### Phase-4 P0（原 Batch 9 P0 16 项）

| ID | 名 | owner | status |
|---|---|---|---|
| GA-EXT-01 | 医保 DRG/DIP/付费目录每月自动同步 + 历史对照 | - | pending |
| GA-EXT-02 | 医疗器械 UDI + 药品本位码自动校验 | - | pending |
| GA-EXT-03 | 传染病 / VTE / 死亡报告卡一键预填 | - | pending |
| GA-EXT-04 | 病案首页质控接入 + DRG 入组率看板 | - | pending |
| GA-EXT-05 | NCIS 国家平台数据上报适配 | - | pending |
| GA-EXT-06 | 多机构主索引联邦（集团多院区）| - | pending |
| GA-EXT-08 | 可信时间戳服务（TSA + 国密 CA）| - | pending |
| GA-EXT-09 | 数据生命周期管理（DLM）+ 自动归档 + 7 年保留 | - | pending |
| GA-EXT-10 | 数据脱敏中心（开发/测试/培训/出境 4 套规则）| - | pending |
| GA-EXT-11 | 医师 CA 电子签名（BJCA / GDCA 集成）| - | pending |
| GA-EXT-12 | 多租户隔离审计 + 数据墙 | - | pending |
| GA-EXT-13 | 离线许可证签发 | - | pending |
| GA-EXT-14 | AI 决策可解释性看板（置信度 + 来源 + 训练范围）| - | pending |
| GA-EXT-15 | AI 模型版本管理 + 一键回滚 | - | pending |
| GA-EXT-18 | WAF / IPS 攻击溯源接入 | - | pending |
| GA-EXT-21 | 国产化兼容性自检页 | - | pending |

### Phase-4 P1（原 Batch 9 P1 10 项）

| ID | 名 | owner | status |
|---|---|---|---|
| GA-EXT-07 | HRP / 财务 / 物资接口（集团版） | - | pending |
| GA-EXT-16 | 多机房灾备方案（RPO ≤ 1h, RTO ≤ 4h）| - | pending |
| GA-EXT-17 | 同城双活 + 异地容灾架构 | - | pending |
| GA-EXT-19 | 数据出境评估包 | - | pending |
| GA-EXT-20 | 合规 chatbot | - | pending |
| GA-EXT-22 | 客户成功看板 | - | pending |
| GA-EXT-23 | 临床路径变异分析 + 优化建议 | - | pending |
| GA-EXT-24 | 学术出口（脱敏数据集 + 投稿模板）| - | pending |
| GA-EXT-25 | 应急预案触发器（疫情/突发）| - | pending |
| GA-EXT-26 | 集团版品牌定制（多租户 logo/颜色/域名）| - | pending |

---

## Phase-5 · 质量、运维、文档、法务（W7-W9）

| ID | 名 | owner | status |
|---|---|---|---|
| GA-QA-01 | 后端覆盖率 ≥ 70%（Jacoco + JUnit 5 + Testcontainers）| - | pending |
| GA-QA-02 | 前端覆盖率 ≥ 60%（Vitest 2 + RTL 16 + msw 2）| - | pending |
| GA-QA-03 | E2E 6 剧本 Playwright 稳定绿 | - | pending |
| GA-OPS-01 | OpenTelemetry + Prometheus + Tempo + Loki + Grafana + 告警 | - | pending |
| GA-OPS-02 | 备份恢复演练 ≥ 3 次录像 + 升级回滚演练 | - | pending |
| GA-OPS-03 | 4 OS × JDK 21 国产化矩阵 CI | - | pending |
| GA-OPS-04 | SBOM (CycloneDX) + 镜像 cosign 签名 + Reproducible Build | - | pending |
| GA-OPS-05 | 离线安装包 + 一键安装/升级/回滚脚本 | - | pending |
| GA-PERF-01 | 1000 并发 60 min + 5000 万 MPI 压测 + LLM 降级链压测 | - | pending |
| GA-SEC-01 | 等保 2.0 三级第三方测评（外部 4 周并行）| - | pending |
| GA-SEC-02 | 商密 GM/T 0054 + GB/T 39786 评测（外部 4 周并行）| - | pending |
| GA-SEC-03 | 渗透测试（外部 2 周）| - | pending |
| GA-DOC-01 | 用户手册 4 治理模块 + 实施手册 + 运维手册 + 培训材料 3 角色 | - | pending |
| GA-LEGAL-01 | 合同 + SLA + 隐私政策 + DPA + License + 用量报告 | - | pending |

---

## Phase-6 · 试点与发版（W10-W12）

| ID | 名 | owner | status |
|---|---|---|---|
| GA-PILOT-01 | 试点医院切真实流量 W1（7×24 真实数据）| - | pending |
| GA-PILOT-02 | 试点医院 W2 + 整改修复 | - | pending |
| GA-PILOT-03 | 试点医院验收单（院方 + 乙方双签）| - | pending |
| GA-RELEASE-01 | 出厂评审 + v1.0.0-ga-evidence.md 填齐 | - | pending |
| GA-RELEASE-02 | tag v1.0.0 发版 + 后置 7 天稳定性观察 | - | pending |

---

## 统计

| Phase | 任务数 | 工时（人日）|
|---|---|---|
| Phase-0 基线重置 | 5 | 2 |
| Phase-1 现代化骨架 | 8 | 8 |
| Phase-2 产品收口 | 11 | 25 |
| Phase-3 业务域实装 | 5（× 5 AI 并行）| 100 |
| Phase-4 客户必查 P0 | 16 | 60 |
| Phase-4 客户必查 P1 | 10 | 35 |
| Phase-5 质量/运维/文档/法务 | 14 | 50 |
| Phase-6 试点与发版 | 5 | 15 |
| **合计** | **74 项** | **~295 人日** |

→ **平均 5 AI 并行**：~12 周 = **GA tag 日期 2026-08-15**

---

## 修订记录

| 版本 | 日期 | 修改人 | 主要变更 |
|---|---|---|---|
| 1.0 | 2026-05-23 | 架构 + 产品评审 | 初版（5 套旧命名归一）|
