import { useState } from "react";
import { Card, Space, Typography, Radio } from "antd";
import { StepFlow, SEVEN_STEPS } from "@/shared/ui/StepFlow";
import type { StepKey } from "@/shared/ui/StepFlow";
import { StatusBadge } from "@/shared/ui/StatusBadge";

const { Text } = Typography;

/**
 * 7 步极简配置流演示页。
 * 真实业务由 GA-TENANT-01 / GA-CLINICAL-01 / GA-QUALITY-01 等域任务接入。
 */
export default function StepFlowDemo() {
  const [current, setCurrent] = useState<StepKey>("impact_preview");

  return (
    <Space direction="vertical" size="large" className="mk-full-width">
      <Card title="配置包中心 · 7 步流演示">
        <Space direction="vertical" size="middle" className="mk-full-width">
          <Space>
            <Text>跳到任意步骤：</Text>
            <Radio.Group value={current} onChange={(e) => setCurrent(e.target.value)}>
              {SEVEN_STEPS.map((s) => (
                <Radio.Button key={s.key} value={s.key}>
                  {s.title}
                </Radio.Button>
              ))}
            </Radio.Group>
          </Space>
          <Space>
            <Text>当前资产状态：</Text>
            <StatusBadge machine="config" status="pending_review" />
            <Text>·</Text>
            <Text>当前变更状态：</Text>
            <StatusBadge machine="change" status="canary" />
          </Space>
        </Space>
      </Card>
      <StepFlow currentStep={current} />
    </Space>
  );
}
