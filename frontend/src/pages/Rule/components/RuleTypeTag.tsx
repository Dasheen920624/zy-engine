import { Tag } from "antd";
import { describeRuleType, RULE_TYPE_LABELS } from "../helpers/ruleFormatters";
import type { RuleType } from "../../../api/rule";
import styles from "../styles.module.css";

const COLOR_MAP: Record<RuleType, string> = {
  TIME_LIMIT_QC: "blue",
  CONTENT_QC: "cyan",
  PATHWAY_NODE: "geekblue",
  SAFETY: "red",
  FOLLOWUP: "purple",
  OPERATION: "gold",
};

export interface RuleTypeTagProps {
  ruleType: RuleType | string | undefined;
}

export default function RuleTypeTag({ ruleType }: RuleTypeTagProps) {
  if (!ruleType) {
    return <Tag>未分类</Tag>;
  }
  const color = COLOR_MAP[ruleType as RuleType] ?? "default";
  const label = describeRuleType(ruleType);
  return (
    <Tag color={color} className={styles.typeTag} title={RULE_TYPE_LABELS[ruleType as RuleType] ?? ruleType}>
      {label}
    </Tag>
  );
}
