import { useState } from "react";
import { Card, Table, Input, Button, Tag } from "antd";
import { MergeCellsOutlined, SearchOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import { useMpiPatients, useMpiStats } from "@/shared/api/hooks";
import { MetricGrid } from "@/shared/ui/MetricGrid";
import { PageState } from "@/shared/ui/PageState";

export default function Mpi() {
  const [q, setQ] = useState("");
  const stats = useMpiStats();
  const patients = useMpiPatients(q);

  return (
    <PageShell
      title="患者主索引"
      description="跨院区患者唯一身份；自动合并多就诊号 + 人工处理冲突"
      primary={
        <Button type="primary" icon={<MergeCellsOutlined />}>
          批量合并
        </Button>
      }
    >
      <MetricGrid
        items={[
          { key: "total", title: "累计患者", value: stats.data?.total ?? 0 },
          { key: "today", title: "今日新增", value: stats.data?.todayNew ?? 0, tone: "primary" },
          {
            key: "conflicts",
            title: "待处理冲突",
            value: stats.data?.conflicts ?? 0,
            tone: "warning",
          },
          {
            key: "merged",
            title: "跨院区合并",
            value: stats.data?.crossSiteMerged ?? 0,
            tone: "success",
          },
        ]}
      />
      <Card>
        <Input.Search
          placeholder="搜身份证末 4 / 姓名 / MPI ID"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onSearch={(v) => setQ(v)}
          style={{ width: 360, marginBottom: 12 }}
          prefix={<SearchOutlined />}
        />
        <PageState
          state={
            patients.isLoading
              ? "loading"
              : patients.isError
                ? "error"
                : (patients.data?.length ?? 0) === 0
                  ? "empty"
                  : "ready"
          }
          title="暂无患者记录"
          onRetry={() => void patients.refetch()}
        >
          <Table
            rowKey="mpiId"
            dataSource={patients.data ?? []}
            scroll={{ x: "max-content" }}
            pagination={{ pageSize: 20, showSizeChanger: true }}
            columns={[
              { title: "MPI ID", dataIndex: "mpiId" },
              { title: "姓名（脱敏）", dataIndex: "maskedName" },
              { title: "性别", dataIndex: "gender" },
              { title: "年龄", dataIndex: "age" },
              { title: "身份证末 4", dataIndex: "idLast4" },
              {
                title: "合并就诊号",
                dataIndex: "mergedCount",
                render: (v: number) => (v > 0 ? <Tag color="blue">{v}</Tag> : "—"),
              },
              {
                title: "状态",
                dataIndex: "status",
                render: (v: string) => (
                  <Tag color={v === "冲突待处理" ? "orange" : "default"}>{v}</Tag>
                ),
              },
            ]}
          />
        </PageState>
      </Card>
    </PageShell>
  );
}
