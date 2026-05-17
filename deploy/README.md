# deploy/ 自动化部署

本目录是 **ARCH-003** 的产物，提供医院内网环境下的自动化部署、升级、回滚、灾备能力。Linux/UnixServer 用 `*.sh`，Windows Server 用 `*.ps1`，两套脚本行为等价。

> 完整流程与版本约定见 [VERSIONING.md](../VERSIONING.md) 与 [zy-engine-mvp/docs/09_内网部署与版本管理.md](../zy-engine-mvp/docs/09_内网部署与版本管理.md)。

## 1. 目录结构

```
deploy/
├── README.md
├── manifest.template.json      # 发布包清单模板
├── scripts/
│   ├── build-release.sh        # 构建发布包（在 dev/CI 机器上跑）
│   ├── build-release.ps1
│   ├── check-env.sh            # 部署前环境检查
│   ├── check-env.ps1
│   ├── install-offline.sh      # 在内网部署机上跑：安装 + 初始化
│   ├── install-offline.ps1
│   ├── upgrade.sh              # 升级到新版本（带备份）
│   ├── upgrade.ps1
│   ├── rollback.sh             # 回滚到上一版本
│   ├── rollback.ps1
│   ├── healthcheck.sh          # 健康检查
│   ├── healthcheck.ps1
│   ├── start.sh / stop.sh / restart.sh
│   └── lib/
│       └── common.sh           # 公共函数（日志 / 探测 / 报错）
├── systemd/
│   └── zy-engine.service       # Linux systemd 单元
├── nginx/
│   ├── zy-engine.conf          # HTTP 反代
│   └── zy-engine-tls.conf      # HTTPS（自签证书）
├── profiles/                   # 不同部署目标 env 模板
│   ├── centos7-x86_64-oracle.env
│   ├── uos-aarch64-dm.env
│   ├── kylin-aarch64-dm.env
│   └── pg-x86_64.env
└── docs/
    └── 实施手册.md
```

## 2. 在 dev/CI 机器上：构建发布包

```bash
# Linux / macOS
./deploy/scripts/build-release.sh \
  --version 1.2.3 \
  --jdk-targets linux-x86_64,linux-aarch64,windows-x86_64 \
  --include-frontend \
  --output ./dist

# 产物：dist/zy-engine-v1.2.3-a1b2c3d.tar.gz
```

```powershell
# Windows
.\deploy\scripts\build-release.ps1 `
  -Version 1.2.3 `
  -JdkTargets "windows-x86_64,linux-x86_64,linux-aarch64" `
  -IncludeFrontend `
  -Output .\dist
```

## 3. 在部署机上：首次安装

```bash
# 1) 上传发布包
scp zy-engine-v1.2.3-a1b2c3d.tar.gz deploy-host:/tmp/

# 2) sha256 校验
ssh deploy-host
cd /tmp
sha256sum -c zy-engine-v1.2.3-a1b2c3d.tar.gz.sha256

# 3) 解压
sudo mkdir -p /opt/zy-engine
sudo tar -xzvf zy-engine-v1.2.3-a1b2c3d.tar.gz -C /opt/zy-engine --strip-components=1

# 4) 选 profile
cd /opt/zy-engine
sudo cp profiles/kylin-aarch64-dm.env conf/zyengine.env
sudo $EDITOR conf/zyengine.env        # 填入 DB 凭据等

# 5) 环境检查
sudo ./scripts/check-env.sh

# 6) 安装
sudo ./scripts/install-offline.sh --init-db

# 7) 验证
./scripts/healthcheck.sh
```

## 4. 升级

```bash
sudo systemctl stop zy-engine     # 升级脚本会自动 stop，这里只是显式
sudo ./scripts/upgrade.sh --to v1.3.0
./scripts/healthcheck.sh
```

升级失败：

```bash
sudo ./scripts/rollback.sh --to v1.2.3
```

## 5. 灾备恢复

参见 [09_内网部署与版本管理.md §9](../zy-engine-mvp/docs/09_内网部署与版本管理.md)。

## 6. Profile 命名约定

```
<os>-<cpu>-<db>.env
  os：centos7 / uos / kylin / openeuler / windows
  cpu：x86_64 / aarch64 / loongarch64
  db：oracle / dm / postgres / kingbase
```

## 7. 安全约束

- 所有脚本必须用普通用户（zyengine）运行，sudo 仅用于初始化（创建目录 / 注册 systemd / firewall）。
- `conf/zyengine.env` 权限 600，含密码字段。
- `*.sh` 必须 `set -euo pipefail`，失败立即停止。
- 不允许 `curl ... | sh` 从公网拉脚本。

## 8. 注意

- 本目录脚本面向"医院已具备 JDK 1.8 + 数据库 + Nginx" 的环境。若需要随包带 JDK，`build-release.sh --jdk-targets` 会下载并打入；下载阶段必须在 dev/CI 机器（有外网）完成，不在医院机器跑。
- 数据库不在脚本里安装；信息科或 DBA 准备好实例后，本脚本只跑 DDL / migration。
