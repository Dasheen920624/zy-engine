import { Card, Table, Input, Button, Space, Tag, Statistic, Row, Col } from "antd";
import { MergeCellsOutlined, SearchOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";

const MOCK = [
  { mpi: "MPI-000123456", name: "张**", gender: "男", age: 58, idLast4: "1234", merged: 2, status: "稳定" },
  { mpi: "MPI-000123457", name: "李**", gender: "女", age: 42, idLast4: "5678", merged: 0, status: "稳定" },
  { mpi: "MPI-000123458", name: "王**", gender: "男", age: 65, idLast4: "9012", merged: 1, status: "冲突待处理" },
];

export default function Mpi() {
  return (
    <PageShell
      title="患者主索引"
      description="跨院区患者唯一身份；自动合并多就诊号 + 人工处理冲突"
      primary={<Button type="primary" icon={<MergeCellsOutlined />}>批量合并</Button>}
    >
      <Row gutter={12}>
        <Col span={6}><Card><Statistic title="累计患者" value={1248322} /></Card></Col>
        <Col span={6}><Card><Statistic title="今日新增" value={283} valueStyle={{ color: "#1565c0" }} /></Card></Col>
        <Col span={6}><Card><Statistic title="待处理冲突" value={12} valueStyle={{ color: "#faad14" }} /></Card></Col>
        <Col span={6}><Card><Statistic title="跨院区合并" value={47} valueStyle={{ color: "#52c41a" }} /></Card></Col>
      </Row>
      <Card>
        <Space style={{ marginBottom: 12 }}>
          <Input.Search placeholder="搜身份证 / 姓名 / MPI ID（已脱敏展示）" style={{ width: 360 }} prefix={<SearchOutlined />} />
        </Space>
        <Table
          rowKey="mpi"
          dataSource={MOCK}
          pagination={false}
          columns={[
            { title: "MPI ID", dataIndex: "mpi" },
            { title: "姓名（脱敏）", dataIndex: "name" },
            { title: "性别", dataIndex: "gender" },
            { title: "年龄", dataIndex: "age" },
            { title: "身份证末 4", dataIndex: "idLast4" },
            { title: "合并就诊号", dataIndex: "merged", render: (v) => v > 0 ? <Tag color="blue">{v}</Tag> : "—" },
            { title: "状态", dataIndex: "status", render: (v) => <Tag color={v === "冲突待处理" ? "orange" : "default"}>{v}</Tag> },
          ]}
        />
      </Card>
    </PageShell>
  );
}
