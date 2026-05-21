import { Tag } from "antd";
import { describeSeverity, SEVERITY_COLOR } from "../helpers/ruleFormatters";
import type { Severity } from "../../../api/types";

export interface SeverityTagProps {
  severity: Severity | string | undefined;
}

export default function SeverityTag({ severity }: SeverityTagProps) {
  if (!severity) return <Tag>—</Tag>;
  const color = SEVERITY_COLOR[severity as Severity] ?? "default";
  return <Tag color={color}>{describeSeverity(severity)}</Tag>;
}
