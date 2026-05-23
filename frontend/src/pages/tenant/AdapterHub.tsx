import { Card, Row, Col, Button, Space, Tag } from "antd";
import { CheckCircleFilled, ExclamationCircleFilled, QuestionCircleFilled } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";

type AdapterStatus = "ok" | "error" | "pending";

interface Adapter {
  id: string;
  name: string;
  system: string;
  status: AdapterStatus;
  lastSync: string;
}

const MOCK: Adapter[] = [
  { id: "a1", name: "总院 HIS（卫宁 HIS 6.0）", system: "HIS", status: "ok", lastSync: "30 秒前" },
  { id: "a2", name: "总院 EMR（君健 EMR 5.2）", system: "EMR", status: "ok", lastSync: "12 秒前" },
  { id: "a3", name: "总院 LIS（联众 LIS 3.5）", system: "LIS", status: "ok", lastSync: "1 分钟前" },
  { id: "a4", name: "总院 PACS（东软 PACS 7）", system: "PACS", status: "error", lastSync: "5 分钟前 · 心跳失败" },
  { id: "a5", name: "东区分院 HIS（创业 4.1）", system: "HIS", status: "pending", lastSync: "等待联通" },
];

const ICON: Record<AdapterStatus, JSX.Element> = {
  ok: <CheckCircleFilled style={{ color: "#52c41a", fontSize: 18 }} />,
  error: <ExclamationCircleFilled style={{ color: "#ff4d4f", fontSize: 18 }} />,
  pending: <QuestionCircleFilled style={{ color: "#faad14", fontSize: 18 }} />,
};

const LABEL: Record<AdapterStatus, string> = { ok: "运行中", error: "异常", pending: "待配置" };

export default function AdapterHub() {
  return (
    <PageShell
      title="适配器中心"
      description="HIS / EMR / LIS / PACS 接入点，按系统看健康状态，技术参数折叠"
      primary={<Button type="primary">添加适配器</Button>}
    >
      <Row gutter={[16, 16]}>
        {MOCK.map((a) => (
          <Col span={8} key={a.id}>
            <Card>
              <Space direction="vertical" size="small" style={{ width: "100%" }}>
                <Space style={{ justifyContent: "space-between", width: "100%" }}>
                  <Tag color="blue">{a.system}</Tag>
                  {ICON[a.status]}
                </Space>
                <strong>{a.name}</strong>
                <Space style={{ justifyContent: "space-between", width: "100%" }}>
                  <span>状态：{LABEL[a.status]}</span>
                  <span style={{ color: "#999" }}>{a.lastSync}</span>
                </Space>
                <Button block size="small">查看接口详情</Button>
              </Space>
            </Card>
          </Col>
        ))}
      </Row>
    </PageShell>
  );
}
