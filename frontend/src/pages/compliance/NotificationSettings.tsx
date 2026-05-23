import { Card, Form, Switch, Select, TimePicker, Space, Button } from "antd";
import dayjs from "dayjs";
import { PageShell } from "@/shared/ui/PageShell";

export default function NotificationSettings() {
  return (
    <PageShell
      title="通知设置"
      description="短信 / 站内信 / 推送 / 邮件 / 免打扰策略"
      primary={<Button type="primary">保存</Button>}
    >
      <Card title="渠道">
        <Form layout="vertical">
          <Form.Item label="短信通知（紧急 + SLA 即将超时）">
            <Switch defaultChecked />
          </Form.Item>
          <Form.Item label="站内信（默认所有业务通知）">
            <Switch defaultChecked />
          </Form.Item>
          <Form.Item label="邮件（每日摘要）">
            <Switch />
          </Form.Item>
          <Form.Item label="推送（移动端）">
            <Switch defaultChecked />
          </Form.Item>
        </Form>
      </Card>
      <Card title="免打扰策略">
        <Form layout="vertical">
          <Form.Item label="免打扰时段（默认夜班医生静默）">
            <Space>
              <TimePicker.RangePicker defaultValue={[dayjs("22:00", "HH:mm"), dayjs("07:00", "HH:mm")]} format="HH:mm" />
            </Space>
          </Form.Item>
          <Form.Item label="哪类提醒仍然在免打扰时段通过">
            <Select
              mode="multiple"
              defaultValue={["red-alert", "trace-failure"]}
              options={[
                { value: "red-alert", label: "红色告警（系统宕机）" },
                { value: "trace-failure", label: "审计链断" },
                { value: "high-priority", label: "高优先级待办" },
              ]}
            />
          </Form.Item>
        </Form>
      </Card>
    </PageShell>
  );
}
