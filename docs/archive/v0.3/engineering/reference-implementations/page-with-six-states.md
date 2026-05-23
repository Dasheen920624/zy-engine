# 前端页面六态参考实现

> 用途：实现任何前端列表/详情页时复制本样板。  
> 关联 PR：PR-V2-05 ~ PR-V2-12 所有页面 PR。  
> 不变量：[`docs/01_产品事实源.md §7.4 不变量 #22`](../../01_产品事实源.md)

## 六态定义

任何页面必须处理：

1. **加载中（Loading）** — 用骨架屏或 Spin
2. **空状态（Empty）** — 用 `<EmptyState>` 组件
3. **错误（Error）** — 用 `<ErrorState>` 组件
4. **无权限（NoPermission）** — 用 `<NoPermissionState>` 或隐藏
5. **处理中（Mutating）** — 按钮 loading 态、禁止重复点击
6. **成功反馈（Success）** — Toast 或 Banner

## 完整样板

```tsx
// frontend/src/pages/Pathway/PathwayList.tsx
import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Button, Table, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { EmptyState, ErrorState, StatusBadge, OrgContextSelector } from '@/components';
import { useOrgContext } from '@/hooks/useOrgContext';
import { useCurrentUser } from '@/hooks/useCurrentUser';
import { pathwayApi } from '@/api/pathway';
import type { PathwayListResponse } from '@/api/pathway.types';
import { PageSkeleton } from '@/components/PageSkeleton';

export const PathwayList: React.FC = () => {
  const navigate = useNavigate();
  const { ctx } = useOrgContext();
  const { user, hasPermission } = useCurrentUser();
  const qc = useQueryClient();

  // ===== 数据查询 =====
  const {
    data,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['pathway-list', ctx],
    queryFn: () => pathwayApi.list(ctx),
    enabled: hasPermission('pathway:read'),  // 无权限不发请求
  });

  // ===== 删除草稿 mutation =====
  const deleteMutation = useMutation({
    mutationFn: (code: string) => pathwayApi.delete(code, ctx),
    onSuccess: () => {
      message.success('已删除');                            // 6. 成功反馈
      qc.invalidateQueries({ queryKey: ['pathway-list'] });
    },
    onError: (err: any) => {
      message.error(`删除失败：${err.message}`);
    },
  });

  // ===== 六态分支 =====

  // 4. 无权限态
  if (!hasPermission('pathway:read')) {
    return (
      <div className="mk-page-no-permission">
        <span>您没有访问路径模板库的权限</span>
      </div>
    );
  }

  // 1. 加载中
  if (isLoading) return <PageSkeleton />;

  // 3. 错误
  if (isError) {
    return (
      <ErrorState
        errorCode={(error as any)?.code}
        title="加载失败"
        description={(error as any)?.message ?? '请稍后重试'}
        traceId={(error as any)?.trace_id}
        onRetry={() => refetch()}
      />
    );
  }

  const rows = data?.items ?? [];

  // 2. 空态
  if (rows.length === 0) {
    return (
      <EmptyState
        title="还没有路径模板"
        description="新建第一个路径模板，开始数字化你的临床路径"
        action={hasPermission('pathway:create') ? {
          text: '新建路径',
          onClick: () => navigate('/pathway/templates/new'),
        } : undefined}
      />
    );
  }

  // ===== 正常渲染 =====
  return (
    <div className="mk-page" data-product="factory">
      <header className="mk-page-header">
        <h1>路径模板库</h1>
        <div className="mk-page-header-actions">
          <OrgContextSelector />
          {hasPermission('pathway:create') && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => navigate('/pathway/templates/new')}
            >
              新建路径
            </Button>
          )}
        </div>
      </header>

      <Table<PathwayListResponse['items'][number]>
        rowKey="code"
        dataSource={rows}
        columns={[
          { title: '路径名称', dataIndex: 'name' },
          { title: '版本', dataIndex: 'version' },
          {
            title: '状态',
            dataIndex: 'status',
            render: status => <StatusBadge status={status} />,
          },
          { title: '入径数', dataIndex: 'instanceCount' },
          {
            title: '操作',
            render: (_, row) => (
              <Button.Group>
                <Button onClick={() => navigate(`/pathway/templates/${row.code}`)}>查看</Button>
                {row.status === 'draft' && hasPermission('pathway:delete') && (
                  <Button
                    danger
                    loading={deleteMutation.isPending}              // 5. 处理中
                    onClick={() => deleteMutation.mutate(row.code)}
                  >
                    删除
                  </Button>
                )}
              </Button.Group>
            ),
          },
        ]}
        pagination={{ defaultPageSize: 20 }}
      />
    </div>
  );
};
```

## 关键约束（不许变）

1. ✅ **6 态必须全部处理**（grep 看每个分支）
2. ✅ 加载用 `<PageSkeleton>` 或 `<Spin>`，**不要全屏遮罩**
3. ✅ 错误必须用 `<ErrorState>` 组件（带 traceId + retry）
4. ✅ 空态必须用 `<EmptyState>` 组件（带中文 title + CTA）
5. ✅ 无权限或者**返回 NoPermissionState**，或者**隐藏功能入口**（不要显示 disable）
6. ✅ 处理中按钮必须 `loading={mutation.isPending}` + 防重复点击
7. ✅ 成功用 Toast（3 秒消失），不要 alert
8. ✅ 根容器必须有 `data-product` 属性（factory / embed / cockpit）
9. ✅ 数据请求必须用 react-query（不要 useEffect + fetch）
10. ✅ 权限判断用 `useCurrentUser().hasPermission`，不要硬编码 if (role === 'admin')

## 禁止模式

- ❌ 用 `useState` 自管 loading/error（react-query 自动处理）
- ❌ 自己写 try-catch（react-query onError 处理）
- ❌ 用 `alert()` `confirm()`（用 AntD Modal）
- ❌ 用内联样式硬编码颜色（用 token）
- ❌ 直接 `axios.get`（必须经 `api/xxx.ts` 封装层）

## 相关文档

- 前端规范：[`docs/engineering/07_前端开发规范.md`](../07_前端开发规范.md)
- 设计系统 §6.1 六态：[`docs/03_设计系统.md §6`](../../03_设计系统.md#6-全局规范)
- 22 不变量 #22：[`docs/01_产品事实源.md §7.4`](../../01_产品事实源.md#74-架构与运维7-项)
