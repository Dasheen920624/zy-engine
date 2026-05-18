# 医疗智能引擎平台 · 前端工程（FE-002 脚手架）

本目录是医疗智能引擎平台 **正式前端工程**。当前阶段为 **FE-002 脚手架**，已具备路由、Layout、组织上下文、API client、查询缓存、MSW mock、单元测试。业务页面由 FE-003～FE-010 逐步落地。

> 视觉与交互参考：[../frontend-prototype/](../frontend-prototype/)（FE-001 高保真静态原型）。

## 技术栈

| 类别 | 选型 | 备注 |
|---|---|---|
| 框架 | React 18 + TypeScript 5 | 严格模式 |
| 构建 | Vite 5 | dev / build / preview |
| UI 库 | Ant Design 5 + @ant-design/icons | 中文 locale + brand 主题 |
| 路由 | react-router-dom 6 | 嵌套路由 |
| 数据请求 | axios + TanStack Query v5 | 自动 traceId + ApiError |
| Mock | MSW 2 | 浏览器 + Node 双侧 |
| 测试 | Vitest + @testing-library/react + jsdom | TDD |

## 快速开始

> 需 Node.js ≥ 18.18（建议 20.x，见 `.nvmrc`）。

### 1. 安装依赖

```powershell
cd frontend
npm install
```

内网部署若需私有 npm 源：

```powershell
npm config set registry https://your-internal-npm-mirror/
npm install
```

### 2. 配置环境变量

复制 `.env.example` 为 `.env.local`，按需修改：

```text
VITE_API_BASE_URL=          # 留空时走 vite proxy（/medkernel → http://localhost:18080）
VITE_ENABLE_MSW=false       # true 启用浏览器 mock，离开后端也可运行
```

### 3. 运行 dev 服务器

```powershell
npm run dev
# → http://localhost:5173
```

启动前确认后端已运行：

```powershell
cd ../medkernel-mvp
./scripts/start-memory.cmd
# 健康检查 http://localhost:18080/medkernel/api/health
```

### 4. 构建产物

```powershell
npm run build
# 产物在 dist/
npm run preview
```

### 5. 运行测试

```powershell
npm test              # 一次性
npm run test:watch    # 监听
npm run typecheck     # 类型检查
```

## 目录结构

```
frontend/
├── index.html              # Vite 入口
├── package.json            # 依赖与脚本
├── vite.config.ts          # Vite + proxy + vitest 配置
├── tsconfig*.json          # TypeScript 配置（分 app / node 引用）
├── .env.example            # 环境变量样例
├── .nvmrc                  # Node 版本声明
└── src/
    ├── main.tsx            # 入口：QueryClient + Router + AntD ConfigProvider
    ├── App.tsx             # 路由出口
    ├── styles/global.css   # 全局样式（与原型对齐）
    ├── api/
    │   ├── client.ts       # axios 实例 + 拦截器（traceId / 组织上下文 / ApiError）
    │   ├── system.ts       # /system/* 接口
    │   └── types.ts        # ApiResult / OrgContext / ApiError
    ├── store/
    │   └── orgContext.ts   # 组织上下文 store（localStorage 持久化）
    ├── hooks/
    │   └── useOrgContext.ts
    ├── utils/
    │   └── traceId.ts      # 前端 traceId 生成器
    ├── layouts/
    │   └── AppLayout.tsx   # Sider + Header + Outlet
    ├── router/
    │   └── routes.tsx      # 路由定义
    ├── pages/
    │   ├── Dashboard.tsx
    │   ├── ProvidersStatus.tsx        # 调真实 /api/system/providers
    │   ├── DemoValidationPlaceholder.tsx
    │   ├── ConfigPackagesPlaceholder.tsx
    │   ├── ProvenancePlaceholder.tsx
    │   └── NotFound.tsx
    ├── mocks/
    │   ├── handlers.ts     # MSW handlers
    │   ├── browser.ts      # MSW browser worker
    │   └── server.ts       # MSW node server（vitest）
    └── test/
        └── setup.ts        # vitest setup（@testing-library/jest-dom + MSW）
```

## 全局约定

### ApiResult 信封

所有后端接口返回 `ApiResult<T>`：

```ts
{ success, code, message, data, trace_id }
```

`client.ts` 拦截器会：

- 自动给请求加 `X-Trace-Id`。
- 自动给请求加组织上下文 Header（`X-Tenant-Id` 等）。
- `success === false` 自动抛 `ApiError`，业务侧不重复判 `success`。
- HTTP 非 2xx 也抛 `ApiError` 并保留 `trace_id` 供排错。

业务代码用 `get<T>` / `post<T>` 直接拿 `data`：

```ts
const providers = await get<SystemProviders>("/system/providers");
```

### 组织上下文

- 通过 `useOrgContext()` hook 读取与修改。
- 持久化到 `localStorage`。
- 任何请求都会自动把当前上下文加到 Header（Body 仍可覆盖）。
- 切换上下文示例：

```ts
const [org, setOrg] = useOrgContext();
setOrg({ ...org, hospital_code: "HOSPITAL_BETA" });
```

### TanStack Query

- `QueryClient` 默认 `staleTime: 30s`、`retry: 1`、`refetchOnWindowFocus: false`。
- 看板/列表使用 `useQuery`；表单提交使用 `useMutation`。

### MSW

- 默认 **关闭**（`VITE_ENABLE_MSW=false`），调真实后端。
- 设为 `true` 后可离线/无后端运行（用于演示和单测）。
- 单测自动启用 MSW（见 `src/test/setup.ts`）。

## 与后端的对接

开发期：

- vite proxy：`/medkernel` → `http://localhost:18080`，无 CORS 困扰。
- `VITE_API_BASE_URL` 默认为 `/medkernel/api`，与后端 servlet path 对齐。

内网部署：

- 通过 nginx 把 `dist/` 静态资源与 `/medkernel/api` 反代到同源。

## 9 角色诉求自检（FE-002 范围内）

- [x] **产品设计师**：状态颜色 + 图标 + 文字三重编码（AntD `Badge` + `Tag` + `Statistic`）；空 / 加载 / 错误 / 无权限 4 态（`Skeleton` / `Empty` / `Alert` + 404 `Result`）。
- [x] **前端**：Layout / 路由 / API client / 错误处理 / 组织上下文 / mock / 测试 全部就位。
- [x] **测试**：vitest + MSW 单元/集成测试基础设施，覆盖 `traceId` 与 `ProvidersStatus`。
- [x] **架构师**：组织上下文 Header 三方合并（Body 优先由后端处理）；`ApiError` 统一错误码；traceId 端到端。
- [x] **信息科**：内网离线构建、私有 npm 源、`.env.local` 凭据不进 Git。
- [ ] **临床医生 / 院领导 / 产品经理**：等 FE-003、FE-007 业务页面落地后才能完整满足。

## 后续任务

参见 [../docs/engineering/02_任务台账.md](../docs/engineering/02_任务台账.md) F 泳道：

- FE-003 演示与规则校验工作台
- FE-004 配置包中心
- FE-005 规则配置器
- FE-006 路径画布（AntV X6）
- FE-007 质控看板（ECharts）
- FE-008 来源追溯
- FE-010 Playwright E2E + axe-core 可访问性扫描

## 维护约定

- 任何新页面需在 `src/router/routes.tsx` 注册 + Layout 菜单同步更新。
- 任何新接口需在 `src/api/*` 单独文件封装，不在组件内直接 axios。
- 真正权限必须由后端校验，前端只做体验控制（菜单禁用 / 按钮隐藏）。
- 不在前端代码或 localStorage 存放 token / API Key / 数据库密码 / 患者完整隐私。
