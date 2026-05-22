import { Tag, Tooltip, Button, Typography, Space, Progress } from "antd";
import {
  RobotOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  EditOutlined,
} from "@ant-design/icons";
import type { AiGeneratedBadgeProps } from "./AiGeneratedBadge.types";
import styles from "./aiGeneratedBadge.module.css";

const { Text } = Typography;

const reviewStatusConfig = {
  pending: { color: "warning", icon: <ClockCircleOutlined />, text: "待审核" },
  accepted: { color: "success", icon: <CheckCircleOutlined />, text: "已采纳" },
  rejected: { color: "error", icon: <CloseCircleOutlined />, text: "已拒绝" },
  modified: { color: "processing", icon: <EditOutlined />, text: "已修改" },
};

const getConfidenceColor = (confidence: number) => {
  if (confidence >= 80) return "var(--mk-ai-confidence-high)";
  if (confidence >= 60) return "var(--mk-ai-confidence-mid)";
  return "var(--mk-ai-confidence-low)";
};

export default function AiGeneratedBadge({
  confidence,
  model = "",
  generatedAt = "",
  reviewStatus = "pending",
  onAccept,
  onModify,
  onReject,
  variant = "badge",
}: AiGeneratedBadgeProps) {
  const reviewConfig = reviewStatus ? reviewStatusConfig[reviewStatus] : undefined;
  const confidenceValue = confidence ?? 0;
  const confidenceColor = getConfidenceColor(confidenceValue);

  const renderBadge = () => (
    <Tooltip title={`模型: ${model}\n生成时间: ${generatedAt}\n置信度: ${confidenceValue}%`}>
      <Tag
        color="purple"
        icon={<RobotOutlined />}
        className={styles.badgeTag}
      >
        <span>AI 候选</span>
        <span
          style={{
            display: "inline-block",
            width: 8,
            height: 8,
            borderRadius: "50%",
            backgroundColor: confidenceColor,
          }}
        />
      </Tag>
    </Tooltip>
  );

  const renderCard = () => (
    <div className={styles.cardContainer}>
      <div className={styles.cardHeader}>
        <Space>
          <RobotOutlined className={styles.aiIcon} />
          <Text strong className={styles.aiText}>
            AI 候选
          </Text>
        </Space>
        {reviewConfig && (
          <Tag color={reviewConfig.color} icon={reviewConfig.icon}>
            {reviewConfig.text}
          </Tag>
        )}
      </div>

      <div className={styles.cardContent}>
        <Space direction="vertical" size="small" style={{ width: "100%" }}>
          <div>
            <Text type="secondary">模型</Text>
            <div>{model}</div>
          </div>
          <div>
            <Text type="secondary">生成时间</Text>
            <div>{generatedAt}</div>
          </div>
          <div>
            <Text type="secondary">置信度</Text>
            <div className={styles.confidenceRow}>
              <Progress
                percent={confidenceValue}
                size="small"
                strokeColor={confidenceColor}
                className={styles.confidenceProgress}
              />
              <Text style={{ color: confidenceColor, fontWeight: 600 }}>
                {confidenceValue}%
              </Text>
            </div>
          </div>
        </Space>
      </div>

      {reviewStatus === "pending" && (
        <div className={styles.cardActions}>
          {onAccept && (
            <Button type="primary" size="small" onClick={onAccept}>
              采纳
            </Button>
          )}
          {onModify && (
            <Button size="small" onClick={onModify}>
              修改
            </Button>
          )}
          {onReject && (
            <Button danger size="small" onClick={onReject}>
              拒绝
            </Button>
          )}
        </div>
      )}
    </div>
  );

  return variant === "card" ? renderCard() : renderBadge();
}