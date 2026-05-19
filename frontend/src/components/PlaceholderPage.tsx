import { ToolOutlined } from "@ant-design/icons";
import { Button, Result } from "antd";
import { useNavigate } from "react-router-dom";

interface PlaceholderPageProps {
  title: string;
  pr?: string;
}

/**
 * 占位页面组件
 * 用于显示尚未实现的页面，统一展示"该页面待实现 (PR-XX)"
 */
export default function PlaceholderPage({ title, pr }: PlaceholderPageProps) {
  const navigate = useNavigate();

  return (
    <Result
      icon={<ToolOutlined style={{ color: "var(--mk-primary)" }} />}
      title={title}
      subTitle={
        <span>
          该页面待实现
          {pr && (
            <span style={{ color: "var(--mk-text-secondary)", marginLeft: 8 }}>
              ({pr})
            </span>
          )}
        </span>
      }
      extra={
        <Button type="primary" onClick={() => navigate("/dashboard")}>
          返回工作台
        </Button>
      }
      style={{
        marginTop: 120,
      }}
    />
  );
}
