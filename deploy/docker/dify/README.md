# Dify 运行边界

Dify 是当前开发平台中的可选工作流与模型编排依赖。仓库不会复制 Dify 官方 Docker Compose 工程，只提交一个很小的 `compose.lock.yml` 覆盖文件，用于锁定完整模式下已验证的默认镜像摘要，避免上游 `latest` 等可变标签在不同部署间悄悄漂移。

`../scripts/bootstrap-runtime.sh` 会把固定的官方版本检出到：

```text
${MEDKERNEL_RUNTIME_ROOT}/dify/v1.14.0/
```

官方展示版本是 `v1.14.0`，上游 Git 标签是 `1.14.0`。启动配置同时保留 `DIFY_VERSION` 和 `DIFY_GIT_REF`，让人可读运行目录和可复现检出点都保持明确。

初始化脚本会从 Dify 官方示例创建运行目录内的 `docker/.env`，只覆盖本地入口端口（HTTP `8090`、HTTPS `8443`），并在首次创建时随机生成 PostgreSQL、Redis、会话、沙箱和插件密钥。Dify 自身的 PostgreSQL、Redis、向量库、沙箱和插件服务拓扑仍由官方版本管理；已验证的默认拓扑镜像摘要由 `compose.lock.yml` 管理。

## 常用操作

```bash
./deploy/docker/scripts/up.sh full
./deploy/docker/scripts/healthcheck.sh full
./deploy/docker/scripts/down.sh full
```

如果某个 Dify 运行目录曾从上游示例密钥启动，但还没有创建用户、应用或模型供应商凭据，可以在使用前轮换一次初始密钥：

```bash
./deploy/docker/scripts/rotate-dify-initial-secrets.sh --confirm-unconfigured
```

配置 Dify 后不要再执行这条轮换命令，因为修改 `SECRET_KEY` 可能导致已加密存储的设置无法读取。通过 `bootstrap-runtime.sh` 新建的安装不需要额外执行这一步。

目标服务器适配时，只编辑运行目录中的 `${MEDKERNEL_RUNTIME_ROOT}/dify/v1.14.0/docker/.env`；该文件包含运行密钥，不能提交到仓库。本地入口示例写在 `.env.override.example`。

MedKernel 业务数据始终以自身 PostgreSQL 服务为权威。停止或重建 Dify 不得删除、替代或迁移 MedKernel 业务数据。

升级 Dify 或更换向量存储服务时，必须作为独立部署变更刷新镜像摘要锁定，并重新完成完整模式健康检查和备份检查。
