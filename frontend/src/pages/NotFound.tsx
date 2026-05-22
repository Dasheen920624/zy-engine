import { Button, Result, Space } from "antd";
import { useNavigate } from "react-router-dom";

export default function NotFound() {
  const navigate = useNavigate();

  return (
    <Result
      status="404"
      title="404"
      subTitle="页面不存在或尚未开放"
      extra={
        <Space>
          <Button onClick={() => navigate(-1)}>返回上一页</Button>
          <Button type="primary" onClick={() => navigate("/dashboard")}>
            返回工作台
          </Button>
        </Space>
      }
    />
  );
}
