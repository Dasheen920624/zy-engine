> ℹ️ **本文档是项目最早期的产品设计资料（2025-2026 初），现已被 `docs/` 金本位体系全面更新。**
>
> 保留供产品背景溯源用，**不可作为当前实施依据**。请阅读最新设计：[docs/README.md](../../README.md)
>
> ---

# GitHub版本管理说明

本项目可以使用 GitHub 进行版本管理，建议采用“主分支稳定、功能分支开发、Pull Request评审”的方式。

## 推荐仓库结构

- `main`：稳定版本，只合并已验证内容。
- `develop`：集成开发分支，可用于联调。
- `feature/*`：功能开发分支，例如 `feature/rule-engine-dsl`。
- `fix/*`：缺陷修复分支。
- `docs/*`：文档和方案调整分支。

## 首次提交建议

首次提交内容建议包含：

- 产品方案、建设规划、AI开发输入包。
- HTML 原型和方案阅览入口。
- `zy-engine-mvp` 后端 MVP 工程。
- Oracle/达梦 DDL 和脚本。
- 编码规范、启动脚本、README。

不提交内容：

- 数据库密码。
- Maven 本地缓存 `m2repo`。
- 构建产物 `target`。
- 日志文件。
- 本地环境配置。

## 常用命令

初始化本地仓库：

```powershell
git init
git add .
git commit -m "初始化专科诊疗路径三大引擎项目"
```

关联远程 GitHub 仓库：

```powershell
git remote add origin https://github.com/组织或账号/仓库名.git
git branch -M main
git push -u origin main
```

创建功能分支：

```powershell
git checkout -b feature/pathway-engine-config
```

提交变更：

```powershell
git add .
git commit -m "完善路径引擎配置能力"
git push -u origin feature/pathway-engine-config
```

## 分支保护建议

GitHub 仓库建议开启：

- `main` 分支禁止直接推送。
- 合并前必须通过 Pull Request。
- 至少 1 人评审通过。
- CI 构建通过后允许合并。
- 禁止提交明文密码和 `.env` 文件。

## 后续可增加的 GitHub Actions

建议后续增加 CI：

- 校验 JSON 样例。
- 校验编码。
- Maven JDK 1.8 编译。
- 检查 DDL 文件是否存在。
- 生成接口文档或发布构建包。
