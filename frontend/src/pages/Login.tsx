import { Card, Typography } from "antd";

const { Title } = Typography;

export default function Login() {
  return (
    <div
      style={{
        height: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#f5f5f5",
      }}
    >
      <Card style={{ width: 400 }}>
        <Title level={3}>登录 · MedKernel</Title>
        <p>v1.0 GA · 登录页骨架（GA-PROD-04 待实装）</p>
      </Card>
    </div>
  );
}
