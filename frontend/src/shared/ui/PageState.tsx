import { Button, Result, Spin, Typography } from "antd";
import type { ReactNode } from "react";

const { Text } = Typography;

export type PageStateKind = "loading" | "empty" | "error" | "forbidden" | "partial" | "ready";

interface PageStateProps {
  state: PageStateKind;
  title?: string;
  description?: ReactNode;
  traceId?: string;
  successCount?: number;
  failureCount?: number;
  action?: ReactNode;
  onRetry?: () => void;
  children?: ReactNode;
}

const DEFAULT_TITLE: Record<Exclude<PageStateKind, "ready">, string> = {
  loading: "正在加载",
  empty: "暂无数据",
  error: "页面暂时不可用",
  forbidden: "当前权限不足",
  partial: "部分处理完成",
};

const DEFAULT_DESCRIPTION: Record<Exclude<PageStateKind, "ready">, ReactNode> = {
  loading: "正在读取当前组织范围内的数据。",
  empty: "当前筛选条件下没有结果，可调整筛选或创建第一条记录。",
  error: "请稍后重试；如果持续失败，请带 traceId 联系信息科。",
  forbidden: "该页面包含受控数据，请联系信息科主任调整角色或数据范围。",
  partial: "部分项目已完成，其余项目需要查看原因后重试或转人工处理。",
};

export function PageState({
  state,
  title,
  description,
  traceId,
  successCount,
  failureCount,
  action,
  onRetry,
  children,
}: PageStateProps) {
  if (state === "ready") {
    return <>{children}</>;
  }

  if (state === "loading") {
    return (
      <Result
        icon={<Spin size="large" />}
        title={title ?? DEFAULT_TITLE.loading}
        subTitle={description ?? DEFAULT_DESCRIPTION.loading}
      />
    );
  }

  const extra =
    action ??
    (state === "error" && onRetry ? (
      <Button aria-label="重试" onClick={onRetry}>
        重试
      </Button>
    ) : undefined);
  const partialDescription =
    state === "partial" && typeof successCount === "number" && typeof failureCount === "number"
      ? `${successCount} 项成功，${failureCount} 项需处理。`
      : undefined;

  return (
    <Result
      status={state === "error" ? "error" : state === "forbidden" ? "403" : "info"}
      title={title ?? DEFAULT_TITLE[state]}
      subTitle={
        <>
          <div>{description ?? partialDescription ?? DEFAULT_DESCRIPTION[state]}</div>
          {traceId && <Text type="secondary">traceId: {traceId}</Text>}
        </>
      }
      extra={extra}
    />
  );
}
