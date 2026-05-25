import { Table, Tag, Card, Button, Space, Timeline, Typography, message } from "antd";
import { CloudSyncOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import { useDrgRulesets, useDrgSync } from "@/shared/api/hooks";
import { MetricGrid } from "@/shared/ui/MetricGrid";
import { PageState } from "@/shared/ui/PageState";

const MOCK = [
  { id: "i1", patient: "张**", drg: "MS-30 急性心梗", payment: 28500, audit: "通过", rule: "—" },
  {
    id: "i2",
    patient: "李**",
    drg: "FF-19 缺血性脑卒中",
    payment: 36200,
    audit: "通过",
    rule: "—",
  },
  {
    id: "i3",
    patient: "王**",
    drg: "IC-29 高血压",
    payment: 4200,
    audit: "拒付",
    rule: "DRG 入组条件不满足",
  },
  {
    id: "i4",
    patient: "刘**",
    drg: "QB-21 急性肺炎",
    payment: 8200,
    audit: "审核中",
    rule: "影像缺失",
  },
];

const AUDIT: Record<string, string> = { 通过: "green", 拒付: "red", 审核中: "orange" };
const RULESET_STATUS: Record<string, string> = {
  active: "green",
  staged: "blue",
  archived: "default",
};

export default function InsuranceAudit() {
  const rulesets = useDrgRulesets();
  const drgSync = useDrgSync();

  return (
    <PageShell
      title="医保智能审核"
      description="DRG / DIP 自动入组、规则命中、人工复审、拒付申诉一站式"
      primary={<Button type="primary">导出医保月报</Button>}
      extras={
        <Button
          icon={<CloudSyncOutlined />}
          loading={drgSync.isPending}
          onClick={() =>
            drgSync.mutate(undefined, {
              onSuccess: (d) =>
                message.success(
                  `同步 ${d.newVersion} · 新增 ${d.diff.added} 条、改 ${d.diff.changed} 条`,
                ),
            })
          }
        >
          手动同步 DRG 月更
        </Button>
      }
    >
      <MetricGrid
        items={[
          { key: "cases", title: "本月入组", value: 12834 },
          {
            key: "success",
            title: "入组成功率",
            value: 96.8,
            precision: 1,
            suffix: "%",
            tone: "success",
          },
          { key: "denial", title: "拒付率", value: 2.3, precision: 1, suffix: "%", tone: "danger" },
          { key: "income", title: "预估医保收入", value: "¥4,283,000" },
        ]}
      />

      {/* GA-EXT-01 · DRG/DIP 月更同步 */}
      <Card
        title="DRG / DIP 规则集版本（月更同步）"
        extra={
          <Typography.Text type="secondary">国家医保局每月发布；省级补丁自动叠加</Typography.Text>
        }
      >
        <Timeline
          items={(rulesets.data ?? []).map((r) => ({
            color: r.status === "active" ? "green" : r.status === "staged" ? "blue" : "gray",
            children: (
              <Space>
                <strong>v{r.version}</strong>
                <Tag color={RULESET_STATUS[r.status]}>{r.status}</Tag>
                <Typography.Text type="secondary">
                  生效 {r.effectiveFrom} · {r.groupCount} 组 · 来源：{r.source}
                </Typography.Text>
              </Space>
            ),
          }))}
        />
      </Card>

      <PageState state={MOCK.length ? "ready" : "empty"} title="暂无医保审核记录">
        <Table
          rowKey="id"
          dataSource={MOCK}
          scroll={{ x: "max-content" }}
          pagination={{ pageSize: 20, showSizeChanger: true }}
          columns={[
            { title: "患者", dataIndex: "patient" },
            { title: "DRG 入组", dataIndex: "drg" },
            {
              title: "申报费用",
              dataIndex: "payment",
              align: "right" as const,
              render: (v) => `¥${Number(v).toLocaleString()}`,
            },
            {
              title: "审核结果",
              dataIndex: "audit",
              render: (v) => <Tag color={AUDIT[v]}>{v}</Tag>,
            },
            { title: "命中规则", dataIndex: "rule" },
            {
              title: "操作",
              render: () => (
                <Space>
                  <Button type="link" size="small">
                    详情
                  </Button>
                  <Button type="link" size="small">
                    申诉
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </PageState>
    </PageShell>
  );
}
