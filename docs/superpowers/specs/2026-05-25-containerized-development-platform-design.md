# 容器化开发平台设计

日期：2026-05-25
状态：当前设计
关联变更：`containerized-development-platform`

## 目标

为本机和后续 Linux 服务器提供可复现、可持久化的 MedKernel Docker 开发平台。平台必须运行当前后端、前端网关、PostgreSQL、可选 Neo4j、监控和固定版本 Dify，同时保持 MedKernel 业务数据以自身 PostgreSQL 为唯一权威。

## 设计原则

- MedKernel 的业务数据只写入 MedKernel PostgreSQL。
- Neo4j 是图投影和图查询能力的可选服务，不能成为业务主库。
- Dify 是可选工作流和模型编排服务，不能成为 MedKernel 的业务数据库。
- 运行数据、密钥、Dify 官方副本和备份都放在仓库外的 `${MEDKERNEL_RUNTIME_ROOT}`。
- 仓库只保留可审阅的 Compose、脚本、模板和摘要锁定文件。
- 旧离线包、systemd、Nginx 样例和旧 profile 不再作为当前部署入口。

## 平台形态

| 模式 | 内容 | 用途 |
|---|---|---|
| `core` | PostgreSQL、Neo4j、后端、前端网关 | 日常开发和 DB 优先能力验证 |
| `full` | `core` + Prometheus + Grafana + 固定版本 Dify | 集成验证、工作流调试和初始验收 |

核心模式是默认工作环境。完整模式资源更重，主要用于 Dify 集成和平台级验收。

## 组件边界

| 组件 | 角色 | 持久化 |
|---|---|---|
| PostgreSQL 16 | MedKernel 权威业务数据库 | `${MEDKERNEL_RUNTIME_ROOT}/postgres` 与备份目录 |
| Neo4j 5 | 可选图服务 | 外部 runtime 目录 |
| Backend | Spring Boot 后端 | 无业务数据持久化 |
| Frontend Gateway | 构建后的前端与反向代理 | 无业务数据持久化 |
| Prometheus / Grafana | 开发监控 | 外部 runtime 目录 |
| Dify | 可选工作流与模型编排 | 官方运行目录，独立于 MedKernel 数据 |

## Dify 处理方式

Dify 官方 Compose 拓扑保留在运行目录中的官方检出，不复制到仓库。仓库提交 `deploy/docker/dify/compose.lock.yml` 作为摘要锁定覆盖，防止上游可变镜像标签漂移。

MedKernel 不复用 Dify 内部 PostgreSQL、Redis、向量库、沙箱或插件服务。停止 Dify 时，MedKernel 核心服务仍应可用。

## 安全与密钥

- 提交到仓库的只允许是 `.env.example` 模板。
- 本地密钥由启动脚本生成到 runtime 目录。
- 运行密钥、数据库数据和备份不得提交。
- 开发安全绕过必须显式标注为开发用途，不得用于生产。

## 验证

验收至少覆盖：

1. Compose 配置可解析。
2. PostgreSQL 持久化目录在容器重建后保留数据。
3. Flyway 迁移应用到 PostgreSQL。
4. 后端健康检查和前端入口可访问。
5. Neo4j 停止时，DB 支撑的 MedKernel 核心能力仍可用。
6. 完整模式下 Dify Web/API 可访问。
7. Dify 停止时，MedKernel 业务数据不受影响。
8. PostgreSQL 备份文件能生成，恢复命令有明确说明。

## 后续扩展

应用级图投影、Dify 工作流绑定和生产高可用部署不在本设计内，应分别通过新的 OpenSpec 变更推进。
