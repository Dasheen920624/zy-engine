# AI Task Claim
claim_id: PKG-AI-001-S01
task_id: PKG-AI-001
title: 医疗知识包导出导入与院内同步
owner: TraeAI-Main
status: ACTIVE
claimed_at: 2026-05-20
dependencies: AIK-003 (DONE), PKG-004 (DONE)
write_scope: knowledge/**, package/**, adapter/**
acceptance_criteria:
  - 知识包导出（规则+术语+路径+图谱+来源+配置）
  - 知识包导入（冲突检测+合并策略）
  - 院内同步（增量/全量）
  - 导入预览和差异对比
  - DDL 4 方言
