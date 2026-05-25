import { Card, Col, Row, Statistic, theme as antdTheme } from "antd";
import type { ReactNode } from "react";

export type MetricTone = "default" | "primary" | "success" | "warning" | "danger";

export interface MetricItem {
  key: string;
  title: string;
  value: number | string;
  suffix?: ReactNode;
  prefix?: ReactNode;
  precision?: number;
  tone?: MetricTone;
}

interface MetricGridProps {
  items: MetricItem[];
}

export function MetricGrid({ items }: MetricGridProps) {
  const { token } = antdTheme.useToken();
  const toneColor: Record<MetricTone, string | undefined> = {
    default: undefined,
    primary: token.colorPrimary,
    success: token.colorSuccess,
    warning: token.colorWarning,
    danger: token.colorError,
  };

  return (
    <Row gutter={[12, 12]}>
      {items.map((item) => (
        <Col xs={24} sm={12} xl={6} key={item.key}>
          <Card>
            <Statistic
              title={item.title}
              value={item.value}
              suffix={item.suffix}
              prefix={item.prefix}
              precision={item.precision}
              valueStyle={{ color: toneColor[item.tone ?? "default"] }}
            />
          </Card>
        </Col>
      ))}
    </Row>
  );
}
