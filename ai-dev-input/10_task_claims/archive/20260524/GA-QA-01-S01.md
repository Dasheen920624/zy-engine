---
task_id: GA-QA-01
claim_seq: S01
ai: TraeAI-Main
level: senior
branch: ai/GA-QA-01/jacoco-coverage
started: "2026-05-23T20:30:00+08:00"
write_scope:
  - medkernel-mvp/src/test/**
  - medkernel-mvp/pom.xml
  - .github/workflows/**
shared_files:
  - medkernel-mvp/pom.xml
---

# GA-QA-01 · 后端覆盖率与 CI

## Goal

Jacoco 覆盖率报告接入，后端目标 70%。

## Plan

1. 评估当前 Jacoco 配置和覆盖率基线
2. 补充核心 Service 单元测试
3. 提升 Jacoco 最低覆盖率阈值
4. 添加 CI 覆盖率报告步骤

## Acceptance

````text
1. Jacoco 覆盖率报告在 mvn test 后自动生成
2. 后端行覆盖率 >= 70%
3. CI 中包含覆盖率报告步骤
4. mvn verify 通过
````

## Verification

````text
cd medkernel-mvp && mvn verify -q
````

## DoD

- [ ] Jacoco 配置正确
- [ ] 核心模块测试补充
- [ ] 覆盖率达标 70%
- [ ] CI 覆盖率步骤添加
