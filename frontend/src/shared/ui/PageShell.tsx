import { Card, Space, Typography } from "antd";
import type { ReactNode } from "react";

const { Title, Text } = Typography;

/**
 * 通用页骨架。
 *
 * 所有客户可见页面统一使用此 PageShell，保证：
 * - 1 个主标题 + 1 句业务描述（不超过 30 字）
 * - 右上角放 1 个主按钮（main）+ 次级动作（extras）
 * - 默认 1 个主目标内容区，高级参数折叠到子组件内
 *
 * 与 docs/CONSTITUTION.md §1 第 6 条对齐：默认 1 主按钮 / 1 主目标 / ≤ 3 默认筛选。
 */
interface PageShellProps {
  title: string;
  description?: string;
  primary?: ReactNode;
  extras?: ReactNode;
  children: ReactNode;
}

export function PageShell({ title, description, primary, extras, children }: PageShellProps) {
  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Card
        bordered={false}
        style={{ background: "transparent", boxShadow: "none" }}
        styles={{ body: { padding: 0 } }}
      >
        <Space style={{ width: "100%", justifyContent: "space-between" }} align="start">
          <Space direction="vertical" size={0}>
            <Title level={4} style={{ margin: 0 }}>
              {title}
            </Title>
            {description && <Text type="secondary">{description}</Text>}
          </Space>
          <Space>
            {extras}
            {primary}
          </Space>
        </Space>
      </Card>
      {children}
    </Space>
  );
}
