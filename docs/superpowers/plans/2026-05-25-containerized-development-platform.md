# 容器化开发平台实施计划

日期：2026-05-25
状态：已完成实施，保留为当前设计追溯
关联 OpenSpec：`containerized-development-platform`

## 目标

交付一套 Docker 开发平台：PostgreSQL 为 MedKernel 权威数据库，Neo4j 和 Dify 为可选服务，Prometheus/Grafana 提供开发观测，所有运行数据和密钥保存在仓库外。

## 架构

- 仓库内资产位于 `deploy/docker/`。
- 运行根目录通过 `MEDKERNEL_RUNTIME_ROOT` 指定。
- MedKernel PostgreSQL 与 Dify 内部数据库完全分离。
- `core` 模式启动 MedKernel 核心服务。
- `full` 模式在 `core` 基础上附加监控和固定版本 Dify。

## 任务清单

- [x] 增加部署资产契约测试，确保最终文件集、profile、密钥边界和 Dify 边界符合预期。
- [x] 增加后端 `container` 运行 profile，让容器环境连接 PostgreSQL 并应用迁移。
- [x] 增加仓库内环境模板，默认指向外部 runtime 根目录，不提交真实密钥。
- [x] 增加 PostgreSQL、Neo4j、后端、前端网关 Compose 服务，包含持久化挂载和健康检查。
- [x] 增加 Prometheus/Grafana 附加 Compose 配置，复用当前监控规则。
- [x] 增加 Dify runtime bootstrap：检出官方 `v1.14.0` 展示版本，对应 Git 标签 `1.14.0`，并锁定已验证镜像摘要。
- [x] 增加 `up.sh`、`down.sh`、`healthcheck.sh`、`backup.sh`、`restore.sh` 等操作脚本。
- [x] 移除旧离线包、systemd、Nginx 和旧 profile 部署入口，让 Docker 成为当前唯一部署入口。
- [x] 完成 Docker Desktop 与 Compose 兼容性验证。
- [x] 验证 core 模式、full 模式、Dify 可用性、可选服务降级和 PostgreSQL 备份。

## 当前入口

```bash
./deploy/docker/scripts/up.sh core
./deploy/docker/scripts/healthcheck.sh core

./deploy/docker/scripts/up.sh full
./deploy/docker/scripts/healthcheck.sh full
```

## 验收结果

- Compose 配置可解析。
- 后端、前端和数据库容器可按模式启动。
- PostgreSQL 备份脚本可生成备份文件。
- Dify 由独立官方运行目录管理，不进入 MedKernel 业务数据边界。

## 后续注意

- Dify 升级必须作为单独变更刷新摘要锁定。
- 应用级图投影和 Dify 工作流绑定需要新的 OpenSpec 变更。
- 生产高可用和医院内网交付不在本开发平台任务内。
