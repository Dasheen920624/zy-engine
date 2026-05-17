# CHANGELOG

本文件记录所有客户可见的变更。版本号遵循 [Semantic Versioning 2.0](https://semver.org/)。

格式参考 [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/)。

> 维护约定见 [VERSIONING.md](VERSIONING.md)。

## [Unreleased]

### Added
- 架构规范层（ARCH-001）：架构总图与服务边界、后端开发规范、前端开发规范、`.gitattributes`、ESLint flat config、Prettier。
- 国产化兼容矩阵（ARCH-002）：OS × CPU × JDK × DB 支持矩阵；达梦 DM 完整 DDL（含中文备注与组织上下文迁移）；PostgreSQL / KingbaseES DDL（PG 兼容模式）；数据库方言差异详表；已知坑实战记录。
- 自动化部署 + 版本管理（ARCH-003）：`deploy/` 目录（PowerShell + Bash 双套脚本：build-release / check-env / install-offline / upgrade / rollback / healthcheck）；systemd 服务文件；nginx 反代示例；4 个部署 profile env 模板；`VERSIONING.md` SemVer 约定；本 CHANGELOG。
- 内网管理台高保真原型（FE-001）：5 页静态 HTML（工作台 / 演示与校验 / 配置包中心 / 来源追溯 / 院级驾驶舱）。
- 前端工程脚手架（FE-002）：React 18 + TS 5 + Vite 5 + AntD 5 + react-router 6 + axios + TanStack Query + MSW 2 + vitest。
- 高密度索引文档：00 总入口、01 多角色诉求矩阵、02 任务台账、03 AI 能力分级与并行冲突规约、04 客户验收剧本与报告模板。
- 三类质控场景黄金样例（病历质控 / 医保质控 / 医嘱安全）与测试矩阵扩展。

### Changed
- 根 README 顶部置顶 "AI 首读" 唯一入口。
- `ai-dev-input/09_ai_task_cards/ai_system_prompt.md` 指向 00 总入口。

### Notes
- 当前版本未打 tag。下次 tag 建议 `v0.2.0`（理由：新增架构规范层 + 国产化矩阵 + 自动化部署框架，属于兼容性新增）。

## [0.1.0] - 2026-05-12（起点）

> 项目 MVP 阶段起点，回顾整理（非严格按 SemVer，归档便于追溯）。

### Added
- 路径引擎：候选 / 入径 / 节点流转 / 变异记录（PATH-001 ~ 007）
- 规则引擎：DSL / 包发布 / 第三方 evaluate / batch-evaluate / 结果回查 / 组织上下文（RULE-001 ~ 004）
- 图谱引擎：候选召回 / 证据查询 / Neo4j 降级
- Dify/AI：工作流模板 / 输入映射 / 降级输出 / 调用统计
- 标准化中心：医嘱 / 诊断 / 检验映射；未映射治理
- 适配器中心：REST / SQL / WebService 第三方查询定义
- 配置包：导入 / review / hash / publish / export / 审计（PKG-001 ~ 003）
- 组织：五段式上下文 + 组织目录 + Oracle 持久化（ORG-001 ~ 003）
- 审计：ENGINE_AUDIT_LOG + traceId
- DB：Oracle 完整 DDL + 达梦 DDL 草案 + Oracle smoke 落库验证（DB-ORG-001）
- 系统能力：Provider 状态探测 / 健康检查 / 多运行模式（DB_ONLY / HYBRID / FULL_INTEGRATION / IN_MEMORY_DEMO）

### Infrastructure
- JDK 1.8 + Spring Boot 2.7.18 + Maven
- PowerShell 5.1 / 7 build / test / smoke 脚本
- GitHub Actions CI（Windows runner + Temurin 8）
- UTF-8 + 中文备注 + Oracle UNISTR
