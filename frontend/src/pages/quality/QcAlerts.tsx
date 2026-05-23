import { Table, Tag, Button, Space } from "antd";
import { PageShell } from "@/shared/ui/PageShell";
import { StatusBadge, type AlertStatus } from "@/shared/ui/StatusBadge";

const MOCK = [
  { id: "1", title: "急诊科 4 月抗菌药使用率超阈值", dept: "急诊科", severity: "high", status: "remediating" as AlertStatus, days: 12 },
  { id: "2", title: "心内 PCI 术后住院日 > 8 天 7 例", dept: "心内科", severity: "medium", status: "assigned" as AlertStatus, days: 5 },
  { id: "3", title: "外科 手术安全核查表缺失 23 份", dept: "外科", severity: "high", status: "new" as AlertStatus, days: 1 },
  { id: "4", title: "ICU 中央静脉导管相关血流感染 2 例", dept: "ICU", severity: "high", status: "closed" as AlertStatus, days: 23 },
];

const SEV: Record<string, string> = { high: "red", medium: "orange", low: "blue" };

export default function QcAlerts() {
  return (
    <PageShell
      title="质控预警"
      description="按责任科室自动派单 + 整改闭环追踪 + 趋势看板"
      primary={<Button type="primary">新建预警</Button>}
    >
      <Table
        rowKey="id"
        dataSource={MOCK}
        pagination={false}
        columns={[
          { title: "问题", dataIndex: "title" },
          { title: "责任科室", dataIndex: "dept", render: (v) => <Tag color="purple">{v}</Tag> },
          { title: "严重度", dataIndex: "severity", render: (v) => <Tag color={SEV[v]}>{v.toUpperCase()}</Tag> },
          { title: "未闭环天数", dataIndex: "days", align: "right" as const },
          { title: "状态", dataIndex: "status", render: (s: AlertStatus) => <StatusBadge machine="alert" status={s} /> },
          {
            title: "操作",
            render: () => (
              <Space>
                <Button type="link" size="small">派单</Button>
                <Button type="link" size="small">查看整改</Button>
              </Space>
            ),
          },
        ]}
      />
    </PageShell>
  );
}
