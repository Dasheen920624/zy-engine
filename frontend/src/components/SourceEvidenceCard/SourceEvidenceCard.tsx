import { Tag, Button, Typography, Space } from "antd";
import {
  FileTextOutlined,
  LinkOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons";
import type { SourceEvidenceCardProps } from "./SourceEvidenceCard.types";
import styles from "./sourceEvidenceCard.module.css";

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
      <div className={styles.citationContainer}>
        <Text type="secondary" className={styles.citationLabel}>
          引用片段 (ID: {citation.id})
          {citation.pageNumber && ` - 第${citation.pageNumber}页`}
        </Text>
        <Paragraph
          className={styles.citationExcerpt}
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
        className={`${styles.compact} ${review.status === "missing" ? styles.compactMissing : ""}`}
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
        className={`${styles.card} ${review.status === "missing" ? styles.cardMissing : ""}`}
      >
        <div className={styles.cardHeader}>
          {renderSourceHeader()}
          {renderReviewStatus()}
        </div>
        <div className={styles.cardReviewInfo}>{renderReviewerInfo()}</div>
        {renderCitation()}
        {renderDocumentLink()}
      </div>
    );
  }

  // Default inline variant
  return (
    <div
      className={`${styles.inline} ${review.status === "missing" ? styles.inlineMissing : ""}`}
    >
      <div className={styles.inlineHeader}>
        {renderSourceHeader()}
        {renderReviewStatus()}
      </div>
      {renderReviewerInfo()}
      {renderCitation()}
      {renderDocumentLink()}
    </div>
  );
}