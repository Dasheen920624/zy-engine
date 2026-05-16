import { Button, Result } from "antd";
import { Link } from "react-router-dom";

export default function NotFound() {
  return (
    <Result
      status="404"
      title="404"
      subTitle="页面不存在 · 该路由可能尚未由 FE-xxx 任务交付"
      extra={
        <Link to="/dashboard">
          <Button type="primary">返回工作台</Button>
        </Link>
      }
    />
  );
}
