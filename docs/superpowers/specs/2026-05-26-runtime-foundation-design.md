# GA-ENG-BASE-07 运行底座设计

## 目标

把 Feature Flag、运行配置、监控、健康检查、备份恢复和国产化 profile 从零散脚本、静态页面和配置片段收束为可运行、可展示、可测试的运行底座合同。该合同必须服务信息科、SRE、实施和审计：他们能看到系统当前运行形态、哪些能力启用、哪些依赖降级、备份恢复是否有证据，以及国产化部署 profile 是否具备明确入口。

## 当前事实

- 后端已有 `/api/v1/system/ping`、`/api/v1/system/runtime`、Actuator health 与 Prometheus 暴露，具备运行探针基础。
- 部署目录已有 Docker Compose、Prometheus、Grafana、健康检查、备份和恢复脚本，但备份文件缺少自动校验摘要。
- 前端 `Provider 状态` 仍是静态演示数据，容易误导信息科认为已连接真实运行态。
- `application.yml` 引用了 `prod`、`govcloud` 等 profile，但当前只存在 `dev`、`test`、`container` 配置文件；国产化配置入口需要补齐。
- 国产化自检端点返回裸 `Map`，本任务不扩大到全量 API 治理，只把运行底座需要的国产化 profile 信息纳入统一运行快照。

## 设计选择

推荐方案：新增“运行合同”接口 `/api/v1/system/operations`。

该接口由后端统一汇聚运行配置、Feature Flag、Actuator 健康状态、Prometheus 指标注册情况、备份恢复脚本口径、国产化目标 profile 和当前 JVM/OS 元数据。它不读取或返回数据库密码、JWT 密钥、Dify 密钥等敏感值，只暴露运行决策所需的状态和证据路径。

备选方案一是只改前端静态页面文案，成本低但不能形成门禁。备选方案二是把所有运行信息塞进 Actuator，自定义成本更高且前端业务语义较弱。统一业务接口更适合当前阶段：既能给前端使用，也能被 CI 和交付验收测试直接断言。

## 后端合同

新增 `com.medkernel.shared.runtime` 包：

- `RuntimeProperties`：绑定 `medkernel.runtime.*`，定义环境、部署形态、数据库方言、Feature Flag、备份恢复、国产化目标和外部依赖。
- `RuntimeOperationsService`：只负责组合运行快照，不执行高风险运维动作。
- `RuntimeOperationsController`：返回 `ApiResult<RuntimeOperationsSnapshot>`。
- 记录类型：`RuntimeOperationsSnapshot`、`RuntimeFeatureFlag`、`RuntimeDependencyStatus`、`RuntimeBackupReadiness`、`RuntimeDomesticProfile`。

合同输出必须包含：

- 当前激活 profile、部署模式、数据库方言、迁移路径、JDK、OS 和虚拟线程是否启用。
- Feature Flag 列表：图谱投影、Dify 工作流、审计持久化、外部 Provider、国产化增强，每项含启用状态、风险级别、负责人和中文说明。
- 依赖状态列表：数据库、Prometheus、备份恢复、图谱、Dify。状态只允许 `UP`、`DEGRADED`、`DISABLED`。
- 备份恢复就绪度：是否启用、RPO、RTO、备份脚本、恢复脚本、校验摘要策略。
- 国产化 profile：目标操作系统、JDK、数据库方言和国密能力要求。

## 配置策略

默认 `application.yml` 提供最小运行合同；`application-dev.yml`、`application-test.yml`、`application-container.yml` 覆盖数据库和运行依赖；新增 `application-govcloud.yml` 作为国产化部署 profile 入口。`govcloud` 不在普通测试中激活，因此可声明达梦或金仓驱动类与迁移路径，但不能引入本地 Maven 无法解析的闭源驱动依赖。

配置不存密钥。所有密码、token、证书路径只允许通过环境变量注入，运行快照不返回这些值。

## 前端体验

`Provider 状态` 页面改为真实运行页：

- 顶部用紧凑指标展示整体健康、部署模式、数据库方言、备份就绪和国产化目标。
- 中部用状态表展示依赖：正常、降级、关闭三态，避免静态“Oracle 主库”“PACS”等虚构生产事实。
- 下部展示 Feature Flag 与备份恢复证据，帮助信息科快速判断是否可上线、可降级、可恢复。
- 加载、错误、空数据走现有 `PageState`，保持产品体验一致。

## 备份恢复

`backup.sh` 生成 PostgreSQL dump 后同步生成 `.sha256` 摘要。`restore.sh` 在同名摘要存在时先校验再恢复，避免误拿损坏备份覆盖数据库。部署资产契约测试必须阻断摘要逻辑被删除。

## 测试策略

- 后端先写 `RuntimeOperationsControllerTest`，断言接口返回标准 `ApiResult`、配置合同、依赖三态、Feature Flag 和备份摘要策略。
- 新增配置合同测试，读取 profile 文件和部署脚本，确保 `govcloud` profile、备份摘要和运行配置入口存在。
- 前端先写 `SystemProviders` 页面测试，断言页面读取真实运行快照，不再渲染旧静态 Provider。
- 保留完整后端与前端基线验证，最终走远端 CI 合并。

## 风险与边界

- 本任务不执行真实备份恢复，只让脚本和合同具备可验证入口；真实演练仍由部署环境执行。
- 本任务不引入达梦、金仓闭源 JDBC 依赖，只补国产化 profile 配置入口。
- 本任务不改全仓裸 `Map` 历史端点，避免和 API 基线净化任务交叉。
