import { Card, Form, Input, Button, Result, Space, Tag } from "antd";
import { PageShell } from "@/shared/ui/PageShell";

export default function RuleValidate() {
  return (
    <PageShell
      title="规则校验"
      description="对单患者 / 单医嘱立刻试运行规则，看是否命中、为什么命中、有哪些建议"
    >
      <Card title="试运行输入" style={{ marginBottom: 16 }}>
        <Form layout="vertical" style={{ maxWidth: 600 }}>
          <Form.Item label="患者 MPI">
            <Input placeholder="例：MPI-000123456" defaultValue="MPI-000123456" />
          </Form.Item>
          <Form.Item label="医嘱内容（自由输入）">
            <Input.TextArea
              rows={3}
              defaultValue={"开立医嘱：\n  - 头孢曲松 1g qd ivgtt\n  - 阿司匹林 100mg qd po"}
            />
          </Form.Item>
          <Form.Item>
            <Button type="primary">运行规则校验</Button>
          </Form.Item>
        </Form>
      </Card>

      <Result
        status="warning"
        title="命中 2 条规则"
        subTitle="点击规则查看命中字段、来源、建议"
        extra={
          <Space direction="vertical" size="small" style={{ width: "100%" }}>
            <Card size="small">
              <Tag color="orange">WARNING</Tag>
              <strong style={{ marginLeft: 8 }}>R-AB-024 · 头孢曲松皮试缺失</strong>
              <p style={{ marginTop: 4, marginBottom: 0 }}>来源：医嘱安全规则 R-AB-024（2023 抗菌药管理办法）</p>
              <p style={{ marginTop: 4, marginBottom: 0 }}>建议：先开皮试医嘱，皮试通过后再开 头孢曲松</p>
            </Card>
            <Card size="small">
              <Tag color="blue">INFO</Tag>
              <strong style={{ marginLeft: 8 }}>R-ASA-002 · 阿司匹林与既往胃出血史</strong>
              <p style={{ marginTop: 4, marginBottom: 0 }}>来源：抗血小板用药指南 2024</p>
              <p style={{ marginTop: 4, marginBottom: 0 }}>建议：评估出血风险，必要时加 PPI</p>
            </Card>
          </Space>
        }
      />
    </PageShell>
  );
}
