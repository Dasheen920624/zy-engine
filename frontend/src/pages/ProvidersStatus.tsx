import { useQuery } from "@tanstack/react-query";
import { Alert, Badge, Button, Card, Empty, Skeleton, Space, Table, Tag } from "antd";
import { ReloadOutlined } from "@ant-design/icons";
import { fetchSystemProviders } from "../api/system";
import type { ApiError, ProviderStatus, SystemProviders } from "../api/types";

export default function ProvidersStatusPage() {
  const { data, isLoading, isError, error, refetch, isFetching } = useQuery<
    SystemProviders,
    ApiError
  >({
    queryKey: ["system", "providers"],
    queryFn: fetchSystemProviders,
    refetchInterval: 60_000,
  });

  return (
    <div>
      <div className="page-header">
        <h1>Provider 运行状态</h1>
        <div className="subtitle">
          调用 <code>GET /api/system/providers</code> · 每 60s 自动刷新 · 状态使用图标+文字+颜色三重编码
        </div>
      </div>

      <Space style={{ marginBottom: 16 }}>
        <Button
          icon={<ReloadOutlined spin={isFetching} />}
          onClick={() => refetch()}
          disabled={isFetching}
        >
          立即刷新
        </Button>
        {data?.run_mode && (
          <Tag color={runModeColor(data.run_mode)}>运行模式：{data.run_mode}</Tag>
        )}
      </Space>

      {isLoading && (
        <Card>
          <Skeleton active paragraph={{ rows: 4 }} />
        </Card>
      )}

      {isError && (
        <Alert
          type="error"
          showIcon
          message={`后端不可达：${error?.code || "UNKNOWN_ERROR"}`}
          description={
            <>
              <div>{error?.message}</div>
              {error?.traceId && (
                <div className="text-muted" style={{ marginTop: 4 }}>
                  trace_id: <code className="text-mono">{error.traceId}</code>
                </div>
              )}
              <div style={{ marginTop: 8 }}>
                <Button size="small" onClick={() => refetch()}>
                  重试
                </Button>
              </div>
            </>
          }
        />
      )}

      {!isLoading && !isError && (!data || data.providers.length === 0) && (
        <Card>
          <Empty description="后端返回空 Provider 列表" />
        </Card>
      )}

      {!isLoading && !isError && data && data.providers.length > 0 && (
        <Card>
          <Table<ProviderStatus>
            rowKey="name"
            dataSource={data.providers}
            pagination={false}
            size="middle"
            columns={[
              {
                title: "Provider",
                dataIndex: "name",
                render: (v: string) => <strong>{v}</strong>,
              },
              {
                title: "状态",
                dataIndex: "ready",
                width: 160,
                render: (ready: boolean) => (
                  <Badge
                    status={ready ? "success" : "error"}
                    text={ready ? "正常" : "不可用"}
                  />
                ),
              },
              {
                title: "实现",
                dataIndex: "provider",
                width: 220,
                render: (v: string) => <code className="text-mono">{v}</code>,
              },
              {
                title: "降级 / 备注",
                dataIndex: "reason",
                render: (v?: string) =>
                  v ? (
                    <Tag color="warning">{v}</Tag>
                  ) : (
                    <span className="text-muted">—</span>
                  ),
              },
            ]}
          />
        </Card>
      )}
    </div>
  );
}

function runModeColor(mode: SystemProviders["run_mode"]): string {
  switch (mode) {
    case "FULL_INTEGRATION":
      return "green";
    case "HYBRID":
      return "blue";
    case "DB_ONLY":
      return "geekblue";
    case "IN_MEMORY_DEMO":
      return "orange";
    default:
      return "default";
  }
}
