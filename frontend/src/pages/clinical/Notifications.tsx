import { List, Tag, Card, Space, Typography } from "antd";
import { PageShell } from "@/shared/ui/PageShell";

const MOCK = [
  { id: "1", time: "10:23", title: "DRG 8 月新政已自动同步，建议本周内导入", level: "info" },
  { id: "2", time: "09:15", title: "东区分院 HIS 心跳异常，已派单给实施工程师", level: "warning" },
  { id: "3", time: "昨日", title: "您发布的胸痛 AMI 路径 v2.3 已生效", level: "success" },
  { id: "4", time: "昨日", title: "AI 知识审核：5 条新规则待您审核", level: "info" },
];

const LEVEL: Record<string, string> = { info: "blue", warning: "orange", success: "green", danger: "red" };

export default function Notifications() {
  return (
    <PageShell
      title="通知中心"
      description="业务通知、处理提醒、系统状态；不打扰策略可在合规运维 → 通知设置配置"
    >
      <Card>
        <List
          dataSource={MOCK}
          renderItem={(n) => (
            <List.Item>
              <Space>
                <Typography.Text type="secondary">{n.time}</Typography.Text>
                <Tag color={LEVEL[n.level]}>{n.level}</Tag>
                <Typography.Text>{n.title}</Typography.Text>
              </Space>
            </List.Item>
          )}
        />
      </Card>
    </PageShell>
  );
}
