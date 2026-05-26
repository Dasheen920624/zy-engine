## ADDED Requirements

### Requirement: 持久化 MedKernel 核心部署

项目 SHALL 提供 Docker Compose 开发部署，运行 MedKernel、PostgreSQL 16 业务数据库、后端应用、前端网关和可选 Neo4j 投影服务。

#### Scenario: 启动核心开发部署

- **WHEN** 开发者在配置 runtime 根目录后启动 core 模式
- **THEN** PostgreSQL、Neo4j、后端和前端网关变为健康或可访问
- **AND** PostgreSQL 数据在容器重建后仍保存在外部 runtime 根目录下

### Requirement: 权威数据边界

部署 SHALL 保持 MedKernel PostgreSQL 为业务记录权威存储，不得把 Neo4j 或 Dify 内部存储作为 MedKernel 业务主库。

#### Scenario: 可选图服务不可用

- **WHEN** Neo4j 停止，而 PostgreSQL 和 MedKernel 应用仍在运行
- **THEN** MedKernel 健康检查和 ping 路径仍可访问

#### Scenario: 可选工作流服务不可用

- **WHEN** Dify 未启动或已停止
- **THEN** MedKernel 核心部署仍可用
- **AND** 业务数据不会迁移到 Dify 内部存储

### Requirement: 固定版本 Dify 集成

项目 SHALL 提供完整模式启动路径，在独立 runtime 目录中安装并运行固定版本 `v1.14.0` 的 Dify 官方自托管 Docker Compose 分发。

#### Scenario: 启动完整开发部署

- **WHEN** 开发者在兼容 Docker Compose 的主机上启动 full 模式
- **THEN** MedKernel 服务和固定版本 Dify 服务集分别从独立 Compose 项目启动
- **AND** Dify Web 入口可用于工作流开发

### Requirement: 可移植运行状态与密钥

部署 SHALL 通过 `MEDKERNEL_RUNTIME_ROOT` 把持久化数据和包含密钥的环境文件放在 Git 外部，本机默认路径为 `/Users/zhikunzheng/work/medkernel/runtime`。

#### Scenario: 部署到另一台服务器

- **WHEN** 运维人员把仓库内部署资产复制到另一台支持 Docker 的服务器
- **AND** 设置该主机的 `MEDKERNEL_RUNTIME_ROOT` 和本地密钥
- **THEN** 不需要编辑仓库内 Compose 文件中的主机路径也能启动部署

### Requirement: 备份与健康验证

部署 SHALL 提供运行检查和 PostgreSQL 备份/恢复命令，以验证核心平台并保护权威业务数据。

#### Scenario: 验证健康部署

- **WHEN** 运维人员在启动后运行健康检查
- **THEN** 检查容器状态、PostgreSQL 就绪、后端 Flyway 后可用性和浏览器入口

#### Scenario: 备份权威数据库

- **WHEN** 运维人员运行 PostgreSQL 备份操作
- **THEN** 外部 runtime 根目录下生成带时间戳的数据库备份
- **AND** 文档说明的恢复操作可以指向该备份文件
