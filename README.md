# 专科诊疗路径智能管理平台

本仓库用于管理“专科诊疗路径智能管理平台”的产品方案、交互原型、AI研发输入包、三大引擎后端 MVP、数据库 DDL、脚本和验证资料。

项目目标是建设一套适合中国医院实际临床环境的专科诊疗路径管理平台，围绕路径引擎、规则引擎、图谱引擎、Dify 编排、Neo4j 医学知识图谱、医生工作站和质控闭环，形成可配置、可追踪、可解释、可审计的诊疗路径能力。

## 当前内容

- 产品方案：[专科诊疗路径智能管理平台产品与技术设计方案.md](专科诊疗路径智能管理平台产品与技术设计方案.md)
- 三大引擎建设规划：[专科诊疗管理平台三大引擎建设规划与技术设计文档.md](专科诊疗管理平台三大引擎建设规划与技术设计文档.md)
- AI 开发输入包：[ai-dev-input/README.md](ai-dev-input/README.md)
- 后端 MVP 工程：[zy-engine-mvp/README.md](zy-engine-mvp/README.md)
- 清晰演示原型：[专科诊疗路径智能管理平台_清晰演示版.html](专科诊疗路径智能管理平台_清晰演示版.html)
- 方案阅览入口：[方案阅览入口.html](方案阅览入口.html)
- 项目总控计划：[项目总控计划.md](项目总控计划.md)

## 后端 MVP

后端工程位于：

```text
zy-engine-mvp/
```

已验证能力：

- JDK 1.8 兼容。
- PowerShell 7 / Windows PowerShell 5.1 兼容脚本。
- Oracle `ZYENGINE` 用户核心表建表和中文备注。
- AMI/STEMI 候选推荐、医生确认入径、节点流转。
- Oracle 推荐记录和患者路径实例落库。
- 节点任务状态和路径变异原因记录。

常用命令：

```powershell
cd zy-engine-mvp
.\scripts\build.cmd
.\scripts\start-memory.cmd
.\scripts\verify-encoding.cmd
.\scripts\run-rule-smoke.cmd
.\scripts\run-pathway-smoke.cmd
```

内存模式健康检查：

```text
http://localhost:18080/zy-engine/api/health
```

Oracle 模式健康检查：

```text
http://localhost:18081/zy-engine/api/health
```

## 建设原则

- 医生确认优先：系统生成推荐和预警，关键医疗决策必须由医生确认。
- 规则确定性优先：安全拦截、时限质控、内涵质控优先使用规则引擎。
- 图谱权威性优先：疾病、症状、指标、路径、证据关系由图谱统一承载。
- Dify 负责协作：Dify 用于流程编排、解释生成和大模型协作，不保存核心医疗状态。
- 全链路可审计：路径推荐、入径、流转、规则命中、图谱证据、Dify 调用均需可追踪。

## 下一阶段

下一阶段进入 P1 工程化建设：

1. 路径配置 DSL 和版本发布能力。
2. 规则 DSL 解析、模拟、热更新和执行日志。
3. 字典映射与第三方适配器中心。
4. Neo4j 图谱查询接入。
5. Dify 工作流真实调用与降级策略。
6. AMI 样例端到端自动化验收。
