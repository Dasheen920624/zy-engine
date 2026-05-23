import { Result, Button } from "antd";
import { useNavigate } from "react-router-dom";

export default function NotFound() {
  const navigate = useNavigate();
  return (
    <Result
      status="info"
      title="此功能待 W3 业务域任务实装"
      subTitle="点击下方按钮回到工作台，查看当前可用的演示路径。"
      extra={
        <Button type="primary" onClick={() => navigate("/dashboard")}>
          返回工作台
        </Button>
      }
    />
  );
}
