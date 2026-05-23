import { useState } from "react";
import { Card, Table, Input, Button, Tag, Statistic, Row, Col, Spin } from "antd";
import { MergeCellsOutlined, SearchOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import { useMpiPatients, useMpiStats } from "@/shared/api/hooks";

export default function Mpi() {
  const [q, setQ] = useState("");
  const stats = useMpiStats();
  const patients = useMpiPatients(q);

  return (
    <PageShell
      title="患者主索引"
      description="跨院区患者唯一身份；自动合并多就诊号 + 人工处理冲突"
      primary={<Button type="primary" icon={<MergeCellsOutlined />}>批量合并</Button>}
    >
      <Row gutter={12}>
        <Col span={6}><Card><Statistic title="累计患者" value={stats.data?.total ?? 0} /></Card></Col>
        <Col span={6}><Card><Statistic title="今日新增" value={stats.data?.todayNew ?? 0} valueStyle={{ color: "#1565c0" }} /></Card></Col>
        <Col span={6}><Card><Statistic title="待处理冲突" value={stats.data?.conflicts ?? 0} valueStyle={{ color: "#faad14" }} /></Card></Col>
        <Col span={6}><Card><Statistic title="跨院区合并" value={stats.data?.crossSiteMerged ?? 0} valueStyle={{ color: "#52c41a" }} /></Card></Col>
      </Row>
      <Card>
        <Input.Search
          placeholder="搜身份证末 4 / 姓名 / MPI ID"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onSearch={(v) => setQ(v)}
          style={{ width: 360, marginBottom: 12 }}
          prefix={<SearchOutlined />}
        />
        {patients.isLoading ? (
          <Spin />
        ) : (
          <Table
            rowKey="mpiId"
            dataSource={patients.data ?? []}
            pagination={false}
            columns={[
              { title: "MPI ID", dataIndex: "mpiId" },
              { title: "姓名（脱敏）", dataIndex: "maskedName" },
              { title: "性别", dataIndex: "gender" },
              { title: "年龄", dataIndex: "age" },
              { title: "身份证末 4", dataIndex: "idLast4" },
              { title: "合并就诊号", dataIndex: "mergedCount", render: (v: number) => v > 0 ? <Tag color="blue">{v}</Tag> : "—" },
              { title: "状态", dataIndex: "status", render: (v: string) => <Tag color={v === "冲突待处理" ? "orange" : "default"}>{v}</Tag> },
            ]}
          />
        )}
      </Card>
    </PageShell>
  );
}
