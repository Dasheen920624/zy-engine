## 1. 部署契约与运行配置

- [x] 1.1 增加部署资产契约测试，并验证 Docker 资产缺失时测试会失败。
- [x] 1.2 增加 Spring `container` profile，并通过契约测试验证 PostgreSQL/Flyway 配置。
- [x] 1.3 增加已提交的环境模板，默认使用外部 runtime 根目录且不提交密钥。

## 2. MedKernel 容器栈

- [x] 2.1 增加 PostgreSQL、Neo4j、后端、前端网关服务，包含外部持久化挂载和健康检查。
- [x] 2.2 增加后端/前端多阶段镜像，以及 `/medkernel/` 的 Nginx 路由。
- [x] 2.3 增加 Prometheus/Grafana Compose 附加配置，复用当前监控规则。
- [x] 2.4 验证 Compose 渲染，并运行后端与前端构建验证。

## 3. Dify 与运维

- [x] 3.1 增加 runtime bootstrap，准备本地密钥，检出 Dify 官方 `v1.14.0`，并在外部 runtime 根目录下锁定已验证镜像摘要。
- [x] 3.2 增加 `core` 和 `full` 启停与健康检查脚本，保持 Dify 官方 Compose 工程独立。
- [x] 3.3 增加 PostgreSQL 备份/恢复操作和本机、后续服务器部署说明。
- [x] 3.4 移除旧离线包、systemd、Nginx 部署文件，使 Docker 成为当前唯一部署入口。

## 4. 运行安装与验收

- [x] 4.1 安装并启动 Apple Silicon Docker Desktop，验证 Docker Compose 兼容性。
- [x] 4.2 在 `/Users/zhikunzheng/work/medkernel/runtime/` 初始化 runtime 并启动 core 模式。
- [x] 4.3 验证 Flyway 迁移、后端 ping/health、前端网关、Neo4j 可达性和监控。
- [x] 4.4 启动 full 模式，验证 Dify 可用性、可选服务降级，并创建 PostgreSQL 备份。
