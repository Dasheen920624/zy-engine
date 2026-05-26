## Why

MedKernel 需要稳定、可持久化、可迁移的 Docker 开发部署，用来验证 PostgreSQL 优先运行时，并为后续服务器部署提供基线。当前项目已完成容器化开发平台资产，因此本变更作为当前事实记录保留。

## What Changes

- 增加 `deploy/docker/` 下的 Compose、镜像构建、环境模板和操作脚本。
- 新增后端 `container` profile，连接 PostgreSQL 并应用 Flyway 迁移。
- 提供 `core` 和 `full` 两种启动模式。
- 将 Dify 官方自托管 Compose 保留在外部 runtime 目录，并通过仓库内摘要锁定文件固定已验证镜像。
- 增加 PostgreSQL 备份、恢复、健康检查和迁移说明。
- 移除旧离线包、systemd、Nginx 和旧 profile 部署入口，让 Docker 成为当前唯一部署入口。

## Capabilities

### New Capabilities

- `containerized-development-platform`：可持久化的 Docker 开发部署、服务健康检查、备份恢复和可选 Dify 集成。

### Modified Capabilities

- 部署入口统一收束到 Docker 平台。

## Impact

- 基础设施：Docker Desktop、本机 runtime 目录、PostgreSQL 16、Neo4j 5、Prometheus、Grafana、Dify。
- 后端：新增容器 profile，不改变业务 API 行为。
- 前端：新增容器网关构建与路由入口。
- 数据：使用已有 PostgreSQL Flyway 迁移。
- 验证：Compose 配置、后端/前端构建、迁移启动、健康检查、Dify 可用性、降级与备份。
