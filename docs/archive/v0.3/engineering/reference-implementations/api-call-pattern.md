# 前端 API 调用参考实现

> 用途：前端任何 HTTP 调用必须经此模式。  
> 关联 PR：所有前端 PR。

## 文件结构

```
frontend/src/api/
├── client.ts            axios 实例 + 拦截器（已存在）
├── types.ts             ApiResult、OrgContext、ApiError（已存在）
├── pathway.ts           /api/pathways/* 封装
├── pathway.types.ts     接口请求/响应类型
└── ...
```

## 1. API 封装模板

```typescript
// frontend/src/api/pathway.ts
import { client } from './client';
import type { ApiResult, OrgContext } from './types';
import type {
  PathwayListRequest,
  PathwayListResponse,
  PathwayDetail,
  CreatePathwayRequest,
} from './pathway.types';

export const pathwayApi = {
  /**
   * 查询路径模板列表
   */
  list: (ctx: OrgContext, req?: PathwayListRequest) =>
    client.get<ApiResult<PathwayListResponse>>('/api/pathways', {
      headers: ctxToHeaders(ctx),
      params: req,
    }).then(r => r.data.data),

  /**
   * 查询单个路径详情
   */
  detail: (code: string, ctx: OrgContext) =>
    client.get<ApiResult<PathwayDetail>>(`/api/pathways/${code}`, {
      headers: ctxToHeaders(ctx),
    }).then(r => r.data.data),

  /**
   * 新建路径模板（草稿）
   */
  create: (req: CreatePathwayRequest, ctx: OrgContext) =>
    client.post<ApiResult<PathwayDetail>>('/api/pathways', req, {
      headers: ctxToHeaders(ctx),
    }).then(r => r.data.data),

  /**
   * 删除路径草稿
   */
  delete: (code: string, ctx: OrgContext) =>
    client.delete<ApiResult<void>>(`/api/pathways/${code}`, {
      headers: ctxToHeaders(ctx),
    }).then(r => r.data),
};

// 把 OrgContext 转 HTTP Header（统一规范）
function ctxToHeaders(ctx: OrgContext): Record<string, string> {
  const h: Record<string, string> = {};
  if (ctx.tenantId)        h['X-Tenant-Id']        = ctx.tenantId;
  if (ctx.groupCode)       h['X-Group-Code']       = ctx.groupCode;
  if (ctx.hospitalCode)    h['X-Hospital-Code']    = ctx.hospitalCode;
  if (ctx.campusCode)      h['X-Campus-Code']      = ctx.campusCode;
  if (ctx.siteCode)        h['X-Site-Code']        = ctx.siteCode;
  if (ctx.departmentCode)  h['X-Department-Code']  = ctx.departmentCode;
  return h;
}
```

## 2. 在页面使用

```tsx
// 用 react-query 调用
const { data, isLoading, isError, error } = useQuery({
  queryKey: ['pathway-list', ctx, search],
  queryFn: () => pathwayApi.list(ctx, { search }),
  enabled: hasPermission('pathway:read'),
});
```

## 3. 错误处理（已由 client.ts 拦截器统一）

axios 拦截器自动：

- 把 HTTP 错误响应转成 `ApiError` 抛出
- 提取 `trace_id` 放到 error 对象
- 401 自动跳登录页
- 403 触发无权限通知

页面无需自己 try-catch，react-query `onError` 接住即可：

```tsx
const mutation = useMutation({
  mutationFn: pathwayApi.create,
  onError: (err: ApiError) => {
    if (err.code === 'VALIDATION_ERROR') {
      // 字段级错误
      form.setFields(err.fieldErrors);
    } else if (err.code === 'MISSING_SOURCE') {
      // 来源缺失，跳到来源审核
      message.error('来源缺失，请先完成来源审核');
    } else {
      message.error(`操作失败：${err.message}（${err.trace_id}）`);
    }
  },
});
```

## 关键约束（不许变）

1. ✅ 所有 HTTP 调用必须经 `client`（已配 traceId、组织 Header、错误处理）
2. ✅ API 函数返回值必须是 `data`（不是整个 `ApiResult` 包）
3. ✅ 组织上下文必须以 Header 形式传，禁止以 query 或 body
4. ✅ 必须有 `.types.ts` 定义请求/响应类型
5. ✅ 错误必须保留 `trace_id` 给用户和 review 排查

## 禁止模式

- ❌ 直接 `fetch()` 或 `axios.get()`（必须经 client）
- ❌ 在组件里组装 Header（必须用 `ctxToHeaders`）
- ❌ 把 `ApiResult.code` 字符串拼到错误消息里给用户（用 `ErrorCode` 枚举映射中文）
- ❌ try-catch 后 `console.error` 吞掉错误（必须 throw 让 react-query 接住）

## 相关文档

- 后端 API 规范：[`docs/01_产品事实源.md §8`](../../01_产品事实源.md)
- 前端规范：[`docs/engineering/07_前端开发规范.md`](../07_前端开发规范.md)
