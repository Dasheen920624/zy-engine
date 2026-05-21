/**
 * 把 RuleDefinition.reference_* （后端 snake_case）适配到 <SourceInfo>（前端 camelCase）。
 *
 * ADR-0004：所有医学规则必须显示来源；规则若 reference_document_code 缺失，
 * 仍渲染一个「来源缺失」状态卡作为合规可见性提示。
 */

import { SourceInfo } from "../../../components";
import type { RuleDefinition } from "../../../api/rule";

export interface SourceCitationCardProps {
  rule: Pick<
    RuleDefinition,
    "reference_document_code" | "reference_citation_id" | "reference_binding_type" | "version_no"
  >;
}

export default function SourceCitationCard({ rule }: SourceCitationCardProps) {
  const hasDoc = Boolean(rule.reference_document_code);
  if (!hasDoc) {
    return (
      <SourceInfo
        variant="card"
        source={{
          documentName: "未关联来源文档",
          documentId: "",
        }}
        review={{ status: "missing" }}
        version={rule.version_no}
      />
    );
  }
  return (
    <SourceInfo
      variant="card"
      source={{
        documentName: rule.reference_document_code ?? "",
        documentId: rule.reference_document_code ?? "",
      }}
      citation={
        rule.reference_citation_id
          ? { id: rule.reference_citation_id, excerpt: `引用编号 ${rule.reference_citation_id}` }
          : undefined
      }
      version={rule.version_no}
      review={{ status: "reviewed" }}
    />
  );
}
