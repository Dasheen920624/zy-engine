import { Table, Tag, Button, Space, Typography, Card, Spin, message } from "antd";
import { SafetyCertificateOutlined, ExportOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import { useAuditEvents, useAuditSnapshot } from "@/shared/api/hooks";

export default function AdminAudit() {
  const events = useAuditEvents();
  const snapshot = useAuditSnapshot();

  return (
    <PageShell
      title="审计日志"
      description="所有用户操作 + 系统事件，TSA 时间戳验签，6 个月内随时可查"
      primary={
        <Button
          icon={<ExportOutlined />}
          loading={snapshot.isPending}
          onClick={() =>
            snapshot.mutate("manual-export", {
              onSuccess: (data) => message.success(`快照已生成 · ${data.signature}`),
            })
          }
        >
          导出审计快照
        </Button>
      }
    >
      <Card style={{ background: "#e6f7ff" }}>
        <Space>
          <SafetyCertificateOutlined style={{ color: "#1565c0", fontSize: 18 }} />
          <Typography.Text>
            等保 2.0 三级 + 个保法审计留痕 + 国密 SM2/SM3 验签 + TSA 国家可信时间戳
          </Typography.Text>
        </Space>
      </Card>
      {events.isLoading ? (
        <Spin />
      ) : (
        <Table
          rowKey="id"
          dataSource={events.data ?? []}
          pagination={false}
          columns={[
            { title: "时间", dataIndex: "time" },
            { title: "用户", dataIndex: "user" },
            { title: "操作", dataIndex: "action" },
            { title: "Trace ID", dataIndex: "traceId", render: (v) => <Tag>{v}</Tag> },
            { title: "签名", dataIndex: "signature", render: (v) => <span style={{ color: "#52c41a" }}>{v}</span> },
          ]}
        />
      )}
    </PageShell>
  );
}
