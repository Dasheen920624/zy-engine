# AI研发输入包：专科诊疗三大引擎

本目录是用于 AI 自主开发或研发团队直接拆解任务的“可执行规格包”。

当前版本先提供 P0 级交付物：

1. `02_api_contracts/engines.openapi.yaml`  
   路径引擎、规则引擎、图谱引擎、Dify适配、字典映射、适配器中心的核心 OpenAPI 契约。

2. `03_data_models/*.schema.json`  
   临床事件、患者上下文、路径配置、规则DSL、图谱查询、推荐卡片的 JSON Schema。

3. `04_database/oracle/core_ddl.sql`  
   Oracle 核心表结构。

4. `04_database/dm/core_ddl.sql`  
   达梦数据库核心表结构。

5. `06_samples/*.json`  
   AMI/STEMI 样例路径、规则、患者上下文、图谱数据。

6. `07_tests/*`  
   测试矩阵和可执行验收用例定义。

7. `09_ai_task_cards/*`  
   AI任务卡模板和P0开发任务清单。

## 开发使用顺序

建议按以下顺序交给 AI 编码工具或研发团队：

1. 先读取 `README.md` 和 `09_ai_task_cards/ai_system_prompt.md`。
2. 实现公共模型：临床事件、患者上下文、统一返回、错误码。
3. 实现字典映射与适配器 Mock。
4. 实现规则引擎最小闭环。
5. 实现图谱引擎最小闭环。
6. 实现路径引擎最小闭环。
7. 接入 Dify 适配服务。
8. 使用 `06_samples` 和 `07_tests` 验收 AMI 样例闭环。

## 关键边界

- 路径引擎保存患者路径状态。
- 规则引擎执行确定性判断和质控规则。
- 图谱引擎封装 Neo4j/图数据库查询。
- Dify 只负责编排工具和生成解释，不保存核心状态。
- 所有临床建议必须由医生确认。
- 所有核心配置必须支持版本、审核和审计。

