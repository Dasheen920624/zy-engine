import React from "react";
import { Tag, Tooltip, Button, Typography, Space, Progress } from "antd";
import {
  RobotOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons";
import type { AiGeneratedBadgeProps } from "./AiGeneratedBadge.types";

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
  model,
  generatedAt,
  reviewStatus,
  onAccept,
  onModify,
  onReject,
  variant = "badge",
}: AiGeneratedBadgeProps) {
  const reviewConfig = reviewStatusConfig[reviewStatus];
  const confidenceColor = getConfidenceColor(confidence);

  const renderBadge = () => (
    <Tooltip title={`模型: ${model}\n生成时间: ${generatedAt}\n置信度: ${confidence}%`}>
      <Tag
        color="purple"
        icon={<RobotOutlined />}
        style={{
          display: "inline-flex",
          alignItems: "center",
          gap: 4,
          padding: "2px 8px",
          borderRadius: "var(--mk-radius-full)",
        }}
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
    <div
      style={{
        padding: "16px",
        background: "var(--mk-ai-soft)",
        borderRadius: "var(--mk-radius-md)",
        border: "1px solid var(--mk-ai-primary)",
      }}
    >
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Space>
          <RobotOutlined style={{ color: "var(--mk-ai-primary)" }} />
          <Text strong style={{ color: "var(--mk-ai-primary)" }}>
            AI 候选
          </Text>
        </Space>
        <Tag color={reviewConfig.color} icon={reviewConfig.icon}>
          {reviewConfig.text}
        </Tag>
      </div>

      <div style={{ marginTop: 12 }}>
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
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <Progress
                percent={confidence}
                size="small"
                strokeColor={confidenceColor}
                style={{ flex: 1 }}
              />
              <Text style={{ color: confidenceColor, fontWeight: 600 }}>
                {confidence}%
              </Text>
            </div>
          </div>
        </Space>
      </div>

      {reviewStatus === "pending" && (
        <div style={{ marginTop: 16, display: "flex", gap: 8 }}>
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