# MedKernel Docker 升级回滚 Runbook

> 适用：当前 Docker 部署平台。旧离线包、systemd、Nginx 脚本部署入口已从
> `deploy/` 移除，历史方案仅保留在 `docs/archive/` 中作追溯。

## 1. 升级前准备

1. 确认目标服务器已安装 Docker Compose v2。
2. 设置目标机运行目录，例如 `MEDKERNEL_RUNTIME_ROOT=/data/medkernel/runtime`。
3. 执行 `./deploy/docker/scripts/healthcheck.sh full`，确认升级前环境健康。
4. 执行 `./deploy/docker/scripts/backup.sh`，将生成的 PostgreSQL 备份复制到受控存储。
5. 记录当前 Git commit、Dify 版本和 `deploy/docker/dify/compose.lock.yml` 摘要。

## 2. 升级流程

```bash
git fetch origin
git checkout main
git pull --ff-only origin main

./deploy/docker/scripts/up.sh full
./deploy/docker/scripts/healthcheck.sh full
```

`up.sh full` 会复用现有持久化目录，只重建需要更新的容器。Dify 官方 Compose
拓扑来自运行目录中的固定版本检出，默认完整模式镜像由仓库内
`deploy/docker/dify/compose.lock.yml` 锁定。

## 3. 回滚流程

如果升级后健康检查失败或业务验证不通过：

```bash
git checkout <previous-known-good-commit>
./deploy/docker/scripts/up.sh full
./deploy/docker/scripts/healthcheck.sh full
```

如需恢复数据库：

```bash
./deploy/docker/scripts/restore.sh /path/to/medkernel-backup.dump
./deploy/docker/scripts/healthcheck.sh full
```

数据库恢复会覆盖当前 MedKernel PostgreSQL 数据，应只在确认需要回退数据状态时执行。
Dify 的运行时配置和密钥位于 `${MEDKERNEL_RUNTIME_ROOT}/dify/`，不要提交到仓库。

## 4. 验收证据

每次升级至少保留：

- 升级前后 `healthcheck.sh full` 输出。
- `backup.sh` 生成的备份文件路径和校验值。
- 当前 Git commit。
- 如 Dify 版本或向量存储拓扑变化，保留刷新后的镜像摘要锁定审阅记录。
