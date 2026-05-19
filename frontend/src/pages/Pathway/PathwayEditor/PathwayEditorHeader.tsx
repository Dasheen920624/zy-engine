import React from "react";
import { Button, Space, Typography, Tag } from "antd";
import { ArrowLeftOutlined, SaveOutlined, SendOutlined, DiffOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import type { PathwayDef } from "../../../components/PathwayCanvas/types";
import { SourceInfo } from "../../../components";

const { Text } = Typography;

interface PathwayEditorHeaderProps {
  pathway: PathwayDef;
  saving?: boolean;
  onSave: () => void;
  onSubmit: () => void;
  onDiff?: () => void;
}

const PathwayEditorHeader: React.FC<PathwayEditorHeaderProps> = ({
  pathway,
  saving,
  onSave,
  onSubmit,
  onDiff,
}) => {
  const navigate = useNavigate();

  const statusMap: Record<string, { color: string; label: string }> = {
    DRAFT: { color: "processing", label: "草稿" },
    PUBLISHED: { color: "success", label: "已发布" },
    RETIRED: { color: "default", label: "已停用" },
  };

  const statusCfg = statusMap[pathway.status] ?? statusMap.DRAFT;

  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        padding: "8px 16px",
        borderBottom: "1px solid var(--mk-border)",
        background: "var(--mk-bg-elevated)",
      }}
    >
      <Space>
        <Button
          icon={<ArrowLeftOutlined />}
          type="text"
          onClick={() => navigate("/pathway/templates")}
        >
          返回
        </Button>
        <Text strong style={{ fontSize: 16 }}>
          {pathway.name}
        </Text>
        <SourceInfo
          source={{ documentName: pathway.name, documentId: pathway.code }}
          review={{ status: "pending" }}
          version={pathway.version}
          variant="compact"
        />
        <Text type="secondary">· 版本 {pathway.version}</Text>
        <Tag color={statusCfg.color}>{statusCfg.label}</Tag>
        {saving && (
          <Text type="secondary" style={{ fontSize: 12 }}>
            💾 自动保存中...
          </Text>
        )}
      </Space>
      <Space>
        {onDiff && (
          <Button icon={<DiffOutlined />} onClick={onDiff}>
            对比
          </Button>
        )}
        <Button icon={<SaveOutlined />} onClick={onSave}>
          保存
        </Button>
        <Button type="primary" icon={<SendOutlined />} onClick={onSubmit}>
          提交审核
        </Button>
      </Space>
    </div>
  );
};

export default PathwayEditorHeader;
