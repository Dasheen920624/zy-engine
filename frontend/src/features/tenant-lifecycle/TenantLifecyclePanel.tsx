import { useState } from "react";
import { Card, Steps, Space, Typography, Select, Row, Col, Tag } from "antd";

const { Text } = Typography;

/**
 * 租户生命周期面板（与 docs/CONSTITUTION.md §6 对齐）。
 *
 * 6 阶段：0 准备 → 1 试运行 → 2 验收 → 3 推广 → 4 运行 → 5 续约
 *
 * 多维度并行：(院区 × 病种 × 模块) 三维切片。
 * 每次阶段切换由后端自动推进规则触发，前端只负责展示。
 *
 * 当前实装级别：骨架 + mock 数据。GA-TENANT-01 业务域任务实装时接 API。
 */

const STAGES = [
  { key: 0, label: "准备" },
  { key: 1, label: "试运行" },
  { key: 2, label: "验收" },
  { key: 3, label: "推广" },
  { key: 4, label: "运行" },
  { key: 5, label: "续约" },
];

interface SliceMock {
  dim: "院区" | "病种" | "模块";
  name: string;
  stage: number;
  day: number;
  estimateRemaining: string;
}

const SLICES_MOCK: SliceMock[] = [
  { dim: "院区", name: "总院", stage: 4, day: 480, estimateRemaining: "续约期前 90 天" },
  { dim: "院区", name: "东区分院", stage: 3, day: 65, estimateRemaining: "约 95 天" },
  { dim: "院区", name: "南区分院", stage: 0, day: 12, estimateRemaining: "约 18 天" },
  { dim: "病种", name: "胸痛 AMI", stage: 4, day: 380, estimateRemaining: "稳定运行中" },
  { dim: "病种", name: "卒中", stage: 1, day: 22, estimateRemaining: "约 38-68 天" },
  { dim: "病种", name: "高血压", stage: 2, day: 8, estimateRemaining: "本周可签验收" },
  { dim: "模块", name: "CDSS", stage: 4, day: 380, estimateRemaining: "稳定" },
  { dim: "模块", name: "医保审核", stage: 3, day: 60, estimateRemaining: "约 100 天" },
  { dim: "模块", name: "质控驾驶舱", stage: 0, day: 5, estimateRemaining: "约 25 天" },
];

export function TenantLifecyclePanel() {
  const [dim, setDim] = useState<"院区" | "病种" | "模块">("院区");
  const slices = SLICES_MOCK.filter((s) => s.dim === dim);

  return (
    <Card
      title={
        <Space>
          <Text strong>租户生命周期</Text>
          <Tag color="blue">{dim} 维度</Tag>
        </Space>
      }
      extra={
        <Select
          value={dim}
          onChange={setDim}
          options={[
            { value: "院区", label: "按院区" },
            { value: "病种", label: "按病种" },
            { value: "模块", label: "按模块" },
          ]}
          className="mk-select-xs"
        />
      }
    >
      <Space direction="vertical" size="middle" className="mk-full-width">
        {slices.map((s) => (
          <Row key={`${s.dim}-${s.name}`} align="middle" gutter={16}>
            <Col span={3}>
              <Text strong>{s.name}</Text>
            </Col>
            <Col span={16}>
              <Steps
                current={s.stage}
                size="small"
                items={STAGES.map((st) => ({ title: `${st.key} ${st.label}` }))}
              />
            </Col>
            <Col span={5}>
              <Space direction="vertical" size={0}>
                <Text type="secondary" className="mk-text-xs">
                  第 {s.day} 天
                </Text>
                <Text className="mk-text-xs">{s.estimateRemaining}</Text>
              </Space>
            </Col>
          </Row>
        ))}
      </Space>
    </Card>
  );
}
