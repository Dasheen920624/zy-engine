import { Card, Form, Input, Select, Button, Space } from "antd";
import { PageShell } from "@/shared/ui/PageShell";
import { StatusBadge } from "@/shared/ui/StatusBadge";

/**
 * GA-TENANT-01 · 租户开通
 */
export default function TenantOnboarding() {
  return (
    <PageShell
      title="租户开通"
      description="建医院 / 院区 / 套餐 / 管理员，5 分钟完成"
      primary={<Button type="primary">提交开通</Button>}
    >
      <Card title="基本信息">
        <Form layout="vertical" className="mk-form-md">
          <Form.Item label="医院名称" required>
            <Input placeholder="如：北京协和医院" />
          </Form.Item>
          <Form.Item label="主院区" required>
            <Input placeholder="如：东院" />
          </Form.Item>
          <Form.Item label="套餐">
            <Select
              defaultValue="standard"
              options={[
                { value: "basic", label: "基础版（单院区）" },
                { value: "standard", label: "标准版（多院区）" },
                { value: "enterprise", label: "企业版（集团版）" },
              ]}
            />
          </Form.Item>
          <Form.Item label="管理员邮箱" required>
            <Input placeholder="admin@hospital.cn" />
          </Form.Item>
        </Form>
      </Card>
      <Card title="已开通租户" extra={<StatusBadge machine="config" status="active" />}>
        <Space direction="vertical" className="mk-full-width">
          <Space>
            <strong>北京协和医院 · 总院</strong>
            <StatusBadge machine="config" status="active" />
            <span>套餐：企业版</span>
          </Space>
          <Space>
            <strong>北京协和医院 · 东区分院</strong>
            <StatusBadge machine="config" status="published" />
            <span>套餐：企业版</span>
          </Space>
        </Space>
      </Card>
    </PageShell>
  );
}
