# 容器化开发平台设计

## 背景

项目需要一套可复现、可持久化的 Docker 开发平台，用于本机长期开发和后续服务器迁移。旧部署入口已清理，当前只保留 Docker 平台作为有效部署路径。

## 目标

- MedKernel 使用 PostgreSQL 作为权威业务数据库。
- 通过 Compose 启动后端、前端网关、PostgreSQL、可选 Neo4j 和监控。
- 完整模式启动固定版本 Dify，但保持 Dify 的数据库和服务拓扑独立。
- 所有运行数据、密钥、Dify 官方副本和备份保存在仓库外。
- 提供健康检查、备份和恢复操作。

## 非目标

- 不构建生产高可用部署。
- 不实现医院内网离线安装包。
- 不实现应用级图投影或 Dify 工作流绑定。

## 关键决策

### Dify 保持官方拓扑

Dify 官方 Compose 工程放在外部 runtime 目录，仓库只提交摘要锁定覆盖文件。这样可以降低升级风险，也避免混淆 MedKernel 权威数据库和 Dify 内部数据库。

### runtime 根目录

默认本机路径为 `/Users/zhikunzheng/work/medkernel/runtime/`。目标服务器可以通过 `MEDKERNEL_RUNTIME_ROOT` 指定自己的路径，仓库内 Compose 不写死主机私有路径。

### 部署入口收束

旧离线包、systemd、Nginx 样例和旧 profile 容易造成未来交付歧义，因此不再作为当前部署入口。

## 风险与缓解

| 风险 | 缓解 |
|---|---|
| Dify 可变镜像标签漂移 | 使用 `compose.lock.yml` 锁定已验证摘要 |
| 密钥误提交 | 只提交模板，真实 `.env` 写入 runtime 目录 |
| 可选服务失败影响核心服务 | core 模式不依赖 Dify；Neo4j 和 Dify 故障不迁移业务数据 |
| 后续服务器路径不同 | 使用 `MEDKERNEL_RUNTIME_ROOT` 外部化 |

## 验证路线

1. 验证 Compose 配置解析。
2. 启动 core 模式，检查 PostgreSQL、后端、前端网关和 Neo4j。
3. 执行健康检查。
4. 启动 full 模式，检查 Dify Web/API。
5. 停止 Dify，确认 MedKernel 核心能力仍可用。
6. 生成 PostgreSQL 备份，并确认恢复命令路径清晰。
