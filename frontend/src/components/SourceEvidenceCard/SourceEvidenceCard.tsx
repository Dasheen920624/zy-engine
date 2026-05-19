import React from "react";
import { Tag, Tooltip, Button, Typography, Space } from "antd";
import {
  FileTextOutlined,
  LinkOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons";
import type { SourceEvidenceCardProps } from "./SourceEvidenceCard.types";

const { Text, Paragraph } = Typography;

const reviewStatusConfig = {
  reviewed: { color: "success", icon: <CheckCircleOutlined />, text: "已审核" },
  pending: { color: "warning", icon: <ClockCircleOutlined />, text: "待审核" },
  rejected: { color: "error", icon: <CloseCircleOutlined />, text: "已拒绝" },
  missing: { color: "orange", icon: <ExclamationCircleOutlined />, text: "来源缺失" },
};

export default function SourceEvidenceCard({
  source,
  citation,
  review,
  version,
  variant = "inline",
  onClickDocument,
}: SourceEvidenceCardProps) {
  const reviewConfig = reviewStatusConfig[review.status];

  const renderSourceHeader = () => (
    <Space size="small">
      <FileTextOutlined />
      <Text strong>{source.documentName}</Text>
      {source.section && <Text type="secondary">§{source.section}</Text>}
      {source.publishYear && <Text type="secondary">({source.publishYear})</Text>}
      <Text type="secondary">v{version}</Text>
    </Space>
  );

  const renderReviewStatus = () => (
    <Tag color={reviewConfig.icon ? reviewConfig.color : undefined} icon={reviewConfig.icon}>
      {reviewConfig.text}
    </Tag>
  );

  const renderReviewerInfo = () => {
    if (review.status === "missing") return null;
    return (
      <Space size="small">
        {review.reviewerName && <Text type="secondary">审核人: {review.reviewerName}</Text>}
        {review.reviewedAt && <Text type="secondary">审核时间: {review.reviewedAt}</Text>}
      </Space>
    );
  };

  const renderCitation = () => {
    if (!citation) return null;
    return (
      <div style={{ marginTop: 8 }}>
        <Text type="secondary" style={{ fontSize: 12 }}>
          引用片段 (ID: {citation.id})
          {citation.pageNumber && ` - 第${citation.pageNumber}页`}
        </Text>
        <Paragraph
          style={{
            margin: "4px 0 0",
            padding: "8px",
            background: "var(--mk-bg-soft)",
            borderRadius: "var(--mk-radius-sm)",
            fontSize: 13,
          }}
          ellipsis={{ rows: 3, expandable: true, symbol: "展开" }}
        >
          {citation.excerpt}
        </Paragraph>
      </div>
    );
  };

  const renderDocumentLink = () => {
    if (!onClickDocument) return null;
    return (
      <Button type="link" size="small" icon={<LinkOutlined />} onClick={onClickDocument}>
        查看原文
      </Button>
    );
  };

  if (variant === "compact") {
    return (
      <div
        style={{
          display: "inline-flex",
          alignItems: "center",
          gap: 8,
          padding: "4px 8px",
          background: review.status === "missing" ? "var(--mk-warning-soft)" : "var(--mk-bg-soft)",
          borderRadius: "var(--mk-radius-sm)",
          border: `1px solid ${review.status === "missing" ? "var(--mk-warning)" : "var(--mk-border)"}`,
        }}
      >
        {renderSourceHeader()}
        {renderReviewStatus()}
        {renderReviewerInfo()}
        {renderDocumentLink()}
      </div>
    );
  }

  if (variant === "card") {
    return (
      <div
        style={{
          padding: "16px",
          background: review.status === "missing" ? "var(--mk-warning-soft)" : "var(--mk-bg-panel)",
          borderRadius: "var(--mk-radius-md)",
          border: `1px solid ${review.status === "missing" ? "var(--mk-warning)" : "var(--mk-border)"}`,
          boxShadow: "var(--mk-shadow-sm)",
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
          {renderSourceHeader()}
          {renderReviewStatus()}
        </div>
        <div style={{ marginTop: 8 }}>{renderReviewerInfo()}</div>
        {renderCitation()}
        {renderDocumentLink()}
      </div>
    );
  }

  // Default inline variant
  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        gap: 4,
        padding: "8px",
        background: review.status === "missing" ? "var(--mk-warning-soft)" : "var(--mk-bg-soft)",
        borderRadius: "var(--mk-radius-sm)",
        border: `1px solid ${review.status === "missing" ? "var(--mk-warning)" : "var(--mk-border)"}`,
      }}
    >
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        {renderSourceHeader()}
        {renderReviewStatus()}
      </div>
      {renderReviewerInfo()}
      {renderCitation()}
      {renderDocumentLink()}
    </div>
  );
}