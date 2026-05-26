# deploy/ Docker 部署入口

本目录现在只保留一套部署入口：`deploy/docker/`。旧的离线发布包脚本、systemd
单元、Nginx 反代样例、profile 模板和独立 Prometheus 配置已移除，避免和新的
容器化开发/迁移平台同时存在造成误用。

> 完整版本约定仍见 [VERSIONING.md](../VERSIONING.md)。旧离线部署方案不再保留在当前工作树中；需要追溯时使用 Git 历史。

## 当前目录结构

```text
deploy/
├── README.md
├── docker/                     # PostgreSQL / Neo4j / Dify / 监控的一体化 Docker 平台
└── monitoring/                 # Docker 监控栈复用的 Grafana 面板与 Prometheus 告警规则
```

## 启动与健康检查

运行数据、密钥、Dify 官方副本和备份都保存在仓库外：

```text
/Users/zhikunzheng/work/medkernel/runtime/
```

启动核心模式：

```bash
./deploy/docker/scripts/up.sh core
./deploy/docker/scripts/healthcheck.sh core
```

启动完整模式（附加 Prometheus、Grafana 和官方 Dify）：

```bash
./deploy/docker/scripts/up.sh full
./deploy/docker/scripts/healthcheck.sh full
```

更多端口、备份、恢复、Dify 镜像摘要锁定和服务器迁移说明见
[docker/README.md](docker/README.md)。
