import { Steps, Card, Space, Typography } from "antd";
import type { ReactNode } from "react";

const { Text } = Typography;

/**
 * 7 步极简配置流模板（与 docs/CONSTITUTION.md §4 对齐）。
 *
 * 配置包、规则、路径、图谱、字典、适配器、评估指标 7 处全部复用此组件。
 * 任何配置类页面缺这 7 步即视为 PR 不通过。
 */

export type StepKey =
  | "select_template"
  | "auto_validate"
  | "impact_preview"
  | "submit_review"
  | "canary_release"
  | "full_rollout"
  | "evidence_rollback";

export interface StepMeta {
  key: StepKey;
  title: string;
  description: string;
}

export const SEVEN_STEPS: StepMeta[] = [
  { key: "select_template", title: "选模板 / 导入", description: "从专病模板或文件开始" },
  { key: "auto_validate", title: "自动校验", description: "字段格式 + 业务规则 + 来源核对" },
  { key: "impact_preview", title: "看影响", description: "影响科室、患者、规则、风险" },
  { key: "submit_review", title: "提交审核", description: "送给医务处 / 信息科主任" },
  { key: "canary_release", title: "灰度发布", description: "默认 10% 床位 / 一个科室" },
  { key: "full_rollout", title: "全量", description: "院级管理员确认后全院生效" },
  { key: "evidence_rollback", title: "证据 / 回滚", description: "审计快照 + 一键回滚" },
];

interface StepFlowProps {
  currentStep: StepKey;
  /** 每步右侧可渲染自定义内容（如校验结果、影响表）。 */
  panelByStep?: Partial<Record<StepKey, ReactNode>>;
  /** 步骤进度（0~6）。current 之外可手动指定 finished/error。 */
  status?: "wait" | "process" | "finish" | "error";
}

/**
 * 7 步流页面骨架。业务层只需传 currentStep 和每步 panel。
 *
 * @example
 *   <StepFlow currentStep="impact_preview" panelByStep={{
 *     auto_validate: <ValidationResult ... />,
 *     impact_preview: <ImpactTable ... />,
 *   }} />
 */
export function StepFlow({ currentStep, panelByStep = {}, status = "process" }: StepFlowProps) {
  const currentIdx = SEVEN_STEPS.findIndex((s) => s.key === currentStep);
  const currentMeta = SEVEN_STEPS[currentIdx];
  const currentPanel = panelByStep[currentStep];

  return (
    <Space direction="vertical" size="large" className="mk-full-width">
      <Steps
        current={currentIdx}
        status={status}
        items={SEVEN_STEPS.map((s) => ({ title: s.title, description: s.description }))}
      />
      <Card
        title={currentMeta.title}
        extra={<Text type="secondary">{currentMeta.description}</Text>}
      >
        {currentPanel ?? (
          <Text type="secondary">
            （此步骤待 GA-TENANT-01 / GA-CLINICAL-01 等业务域任务接入内容）
          </Text>
        )}
      </Card>
    </Space>
  );
}
