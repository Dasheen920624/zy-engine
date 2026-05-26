import { Card, Form, Input, Button, Result, Space, Tag } from "antd";
import { PageShell } from "@/shared/ui/PageShell";
import { useRuleValidate, type RuleHit, type RuleValidateInput } from "@/shared/api/hooks";
import { PageState } from "@/shared/ui/PageState";

const SEVERITY_COLOR: Record<string, string> = {
  blocker: "red",
  warning: "orange",
  info: "blue",
};

export default function RuleValidate() {
  const [form] = Form.useForm<RuleValidateInput>();
  const validate = useRuleValidate();

  return (
    <PageShell
      title="规则校验"
      description="对单患者 / 单医嘱立刻试运行规则，看是否命中、为什么命中、有哪些建议"
    >
      <Card title="试运行输入" className="mk-card-gap-bottom">
        <Form<RuleValidateInput>
          form={form}
          layout="vertical"
          className="mk-form-md"
          initialValues={{
            patientMpi: "MPI-000123456",
            orderText: "开立医嘱：\n  - 头孢曲松 1g qd ivgtt\n  - 阿司匹林 100mg qd po",
          }}
          onFinish={(values) => validate.mutate(values)}
        >
          <Form.Item label="患者 MPI" name="patientMpi" rules={[{ required: true }]}>
            <Input placeholder="例：MPI-000123456" />
          </Form.Item>
          <Form.Item label="医嘱内容（自由输入）" name="orderText" rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={validate.isPending}>
              运行规则校验
            </Button>
          </Form.Item>
        </Form>
      </Card>

      {validate.isPending && <PageState state="loading" title="正在运行规则校验" />}
      {validate.isError && <PageState state="error" onRetry={() => form.submit()} />}
      {validate.isSuccess && (
        <Result
          status={validate.data.hitCount > 0 ? "warning" : "success"}
          title={`命中 ${validate.data.hitCount} 条规则`}
          subTitle="点击规则查看命中字段、来源、建议"
          extra={
            <Space direction="vertical" size="small" className="mk-full-width">
              {validate.data.hits.map((h: RuleHit) => (
                <Card size="small" key={h.ruleId}>
                  <Tag color={SEVERITY_COLOR[h.severity] ?? "default"}>
                    {h.severity.toUpperCase()}
                  </Tag>
                  <strong>
                    {h.ruleId} · {h.ruleName}
                  </strong>
                  <p>来源：{h.source}</p>
                  <p>建议：{h.suggestion}</p>
                </Card>
              ))}
            </Space>
          }
        />
      )}
    </PageShell>
  );
}
