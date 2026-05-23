import { Card, Row, Col, Tag, Space, Typography, Button } from "antd";
import { CheckCircleFilled, CloseCircleFilled } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";

const PROVIDERS = [
  { name: "院方 LDAP", enabled: true, users: 1283, note: "用 LDAP 同步医院 AD 工号" },
  { name: "院方 CAS（中国电信 IAM）", enabled: true, users: 1283, note: "单点登录" },
  { name: "OIDC（卫健委 IAM）", enabled: false, users: 0, note: "等保 + 卫健委对接，待开" },
  { name: "SAML（外部科研机构）", enabled: false, users: 0, note: "科研合作时启用" },
  { name: "国密 CA（BJCA）", enabled: true, users: 1283, note: "医师电子签名" },
];

export default function IdentityBinding() {
  return (
    <PageShell
      title="身份绑定"
      description="一处配置，所有员工通用。CAS / LDAP / OIDC / SAML / 国密 CA 五选 N"
    >
      <Row gutter={[16, 16]}>
        {PROVIDERS.map((p) => (
          <Col span={12} key={p.name}>
            <Card>
              <Space direction="vertical" size="small" style={{ width: "100%" }}>
                <Space style={{ width: "100%", justifyContent: "space-between" }}>
                  <strong>{p.name}</strong>
                  {p.enabled ? (
                    <Tag icon={<CheckCircleFilled />} color="green">已启用</Tag>
                  ) : (
                    <Tag icon={<CloseCircleFilled />}>未启用</Tag>
                  )}
                </Space>
                <Typography.Text type="secondary">{p.note}</Typography.Text>
                <Space>
                  <span>绑定用户数：{p.users}</span>
                  <Button type="link" size="small">{p.enabled ? "查看配置" : "启用"}</Button>
                </Space>
              </Space>
            </Card>
          </Col>
        ))}
      </Row>
    </PageShell>
  );
}
