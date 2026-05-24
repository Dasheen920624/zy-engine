# <页面中文名>

> **状态**：草稿 / 评审中 / 已锁定（最后审定日：YYYY-MM-DD）
> **复制本文件**到对应子目录（如 `clinical/<slug>.md`）后填写。

---

## 1. URL

- 主路径：`/xxx/yyy`
- 子路径（如有）：`/xxx/yyy/:id/edit`、`/xxx/yyy/import` 等
- 路由文件位置：[frontend/src/shared/config/routes.ts](../../../frontend/src/shared/config/routes.ts)

## 2. 归属菜单

- 一级：工作台 / 试点准备 / 临床运行 / 质控改进 / 合规运维 / 高级工具（5+1）
- 二级：<二级菜单名>
- 菜单文件位置：[frontend/src/shared/config/menu.ts](../../../frontend/src/shared/config/menu.ts)

## 3. 主要角色

> 引用 [docs/product/facts.md §9 角色矩阵](../../product/facts.md#9-角色矩阵)（R2 完整填充后）

| 角色 | 用本页做什么 | 主要交互 |
|---|---|---|
| R0X <角色名> | <用本页解决什么具体业务问题> | <点击什么、看什么、决策什么> |

## 4. 对应任务

- 主任务：`GA-<DOMAIN>-<NN>`（见 [docs/backlog.md](../../backlog.md)）
- 关联任务：`GA-EXT-XX`、`GA-UI-XX` 等
- 关联剧本：`scenarios/SX-xxx.md`（如本页是某剧本主页面）

## 5. 布局（ASCII wireframe）

```
┌─── 顶部 Header（固定）─────────────────────────────────────┐
│  [面包屑]                       [审计快照] [主题] [权限] [用户] │
├─── 侧栏（固定）─┬─── 主内容（24px padding）────────────────┤
│  <菜单结构>     │  <PageShell：title + description + primary>│
│                 │                                            │
│                 │  <主内容区，遵守 §1.6「1 主按钮 / 1 主    │
│                 │   目标 / ≤ 3 默认筛选」>                  │
│                 │                                            │
└─────────────────┴────────────────────────────────────────────┘
```

## 6. 组件树

```
<PageShell title="..." description="..." primary={<Button>...</Button>}>
  <Filter columns={...} defaultExpanded={3} />     # ≤ 3 默认筛选
  <Table columns={...} />                          # or <Card> / <Detail>
  <Drawer>                                         # 详情抽屉（按需）
    <SourceInfo />                                 # 来源信息条
  </Drawer>
</PageShell>
```

- 关联组件：[frontend/src/shared/ui/](../../../frontend/src/shared/ui/) 中的 `<XXX>`、`<YYY>`
- 自有组件：[frontend/src/features/<feature>/](../../../frontend/src/features/) 中的 `<ZZZ>`

## 7. API

| Method | Path | 入参 | 出参 | 鉴权 | 关联任务 |
|---|---|---|---|---|---|
| GET | `/api/v1/<resource>` | Query: `tenantId, page, size, status?` | `ApiResult<Page<XxxDTO>>` | `XXX_READ` | `GA-<DOMAIN>-<NN>` |
| POST | `/api/v1/<resource>` | Body: `CreateXxxDTO` | `ApiResult<XxxDTO>` | `XXX_WRITE` + 审计 | 同上 |

- DTO 文件位置：`medkernel-backend/src/main/java/.../dto/`
- Controller 文件位置：`medkernel-backend/src/main/java/.../controller/`
- 所有 API 返回 `ApiResult { success, code, message, data, trace_id }`（[CONSTITUTION §不变量 20](../../CONSTITUTION.md)）

## 8. 状态机

> 用 [CONSTITUTION §3 4 套统一状态机](../../CONSTITUTION.md#3-4-套统一状态机全平台一套禁止自创) 之一

适用：配置类 / 变更类 / 待办类 / 告警类（选其一，禁止自创）

```
草稿 → 待审核 → 已发布 → 生效中 → 已下线 →（已归档）
```

UI 表达：`<StatusBadge machine="config" status={...} />`

## 9. 空错态（六态）

| 态 | 表达 | 文案 |
|---|---|---|
| 加载中 | `<Skeleton>` 行级 | — |
| 空 | `<EmptyState icon=... title=... action=...>` | 「暂无 XXX，点击下方按钮创建第一个」 |
| 错误 | `<ErrorState>` + retry | 「加载失败：<具体原因>，[重试]」 |
| 无权限 | `<EmptyState type="forbidden">` | 「您没有 XXX_READ 权限，请联系管理员」 |
| 处理中 | 按钮 loading + 全局 progress | — |
| 成功反馈 | `message.success` + 列表自动刷新 | 「已 XXX 成功」|

→ 六态强制（[CONSTITUTION §不变量 22](../../CONSTITUTION.md)）

## 10. 权限

- 读：`<RESOURCE>_READ`
- 写：`<RESOURCE>_WRITE`
- 审核：`<RESOURCE>_REVIEW`
- 删除：`<RESOURCE>_DELETE`（默认禁，仅管理员）

- 权限模型：[medkernel-backend/.../security/](../../../medkernel-backend/) RBAC
- 数据范围：受组织上下文（`tenant_id` + `org_path`）约束（[CONSTITUTION §不变量 21](../../CONSTITUTION.md)）

## 11. 性能

| 指标 | 目标 | 测量方式 |
|---|---|---|
| 首屏 LCP | ≤ 2.5s | Lighthouse / Web Vitals |
| 列表加载 | ≤ 1s（默认分页 20 行）| P95 from APM |
| API P95 | ≤ 500ms | OTel + Prometheus |
| 并发 | ≥ <N> req/s | k6 / Gatling |

## 12. 验收 DoD

- [ ] 所有 6 态可手工触发（空/错/无权/加载/处理/成功）
- [ ] 角色权限按 §10 实装（误访问返 403 + 友好页）
- [ ] 状态机使用 [CONSTITUTION §3](../../CONSTITUTION.md) 4 套之一（禁止自创）
- [ ] 主按钮 ≤ 1，主目标 ≤ 1，默认筛选 ≤ 3（[CONSTITUTION §1.6](../../CONSTITUTION.md)）
- [ ] 文案无技术名词（[CONSTITUTION §7 术语表](../../CONSTITUTION.md)）
- [ ] AI 生成内容带 🤖 标识（[CONSTITUTION §不变量 9](../../CONSTITUTION.md)）
- [ ] 敏感字段脱敏（[CONSTITUTION §不变量 10](../../CONSTITUTION.md)）
- [ ] 性能达 §11 指标
- [ ] 单测覆盖率 ≥ 60%（关键交互必测）
- [ ] E2E 用例覆盖主路径（若属某剧本）
- [ ] 文档同 PR：本规格 + 代码 + 测试同 PR 合入

## 13. 关联剧本

> 若本页是某剧本主页面，列出对应剧本编号

- [S<N>-<scenario-slug>.md](../../scenarios/S<N>-<scenario-slug>.md)

## 14. 关联代码

- 页面文件：`frontend/src/pages/<domain>/<PageName>.tsx`
- 测试文件：`frontend/src/pages/<domain>/<PageName>.test.tsx`
- E2E 文件（若有）：`frontend/e2e/<id>.spec.ts`
- 后端：`medkernel-backend/src/main/java/.../<module>/`

---

**End of page spec template.**
