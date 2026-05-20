import type React from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Button, Descriptions, Spin, Typography, Tag } from "antd";
import { ArrowLeftOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import { StatusBadge, SourceInfo } from "../../components";
import { getPathway } from "../../api/pathway";
import type { PathwayDetail } from "../../api/types";

const { Title } = Typography;

const PathwayDetail: React.FC = () => {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();

  const { data, isLoading } = useQuery({
    queryKey: ["pathway", code],
    queryFn: () => getPathway(code ?? ""),
    enabled: !!code,
  });

  if (isLoading) {
    return (
      <div style={{ padding: 24, textAlign: "center" }}>
        <Spin size="large" />
      </div>
    );
  }

  const detail: PathwayDetail | undefined = data;

  return (
    <div style={{ padding: 24 }}>
      <Button
        icon={<ArrowLeftOutlined />}
        style={{ marginBottom: 16 }}
        onClick={() => navigate("/pathway/templates")}
      >
        返回列表
      </Button>

      <Title level={3}>
        {detail?.pathway_code || code}
        <SourceInfo source={{ documentName: detail?.pathway_code ?? "", documentId: detail?.pathway_code ?? "" }} />
      </Title>

      <Descriptions bordered column={2} style={{ marginBottom: 24 }}>
        <Descriptions.Item label="路径编码">{detail?.pathway_code}</Descriptions.Item>
        <Descriptions.Item label="草稿状态">
          <StatusBadge status={detail?.draft_status === "DRAFT" ? "draft" : "retired"} />
        </Descriptions.Item>
        <Descriptions.Item label="已发布版本">
          {detail?.published_versions?.map((v) => (
            <Tag key={v}>{v}</Tag>
          )) || "—"}
        </Descriptions.Item>
        <Descriptions.Item label="当前激活版本">
          {detail?.active_published_version || "—"}
        </Descriptions.Item>
        <Descriptions.Item label="选中版本">
          {detail?.selected_version || "—"}
        </Descriptions.Item>
      </Descriptions>

      {detail?.reference_warnings && detail.reference_warnings.length > 0 && (
        <div style={{ marginBottom: 16 }}>
          <Typography.Text type="warning">
            引用警告：{detail.reference_warnings.length} 项缺失引用
          </Typography.Text>
        </div>
      )}

      {detail?.draft_config && (
        <>
          <Title level={4}>草稿配置</Title>
          <pre style={{ background: "var(--mk-bg-base)", padding: 16, borderRadius: 8, overflow: "auto" }}>
            {JSON.stringify(detail.draft_config, null, 2)}
          </pre>
        </>
      )}

      {detail?.published_config && (
        <>
          <Title level={4} style={{ marginTop: 24 }}>
            已发布配置
          </Title>
          <pre style={{ background: "var(--mk-bg-base)", padding: 16, borderRadius: 8, overflow: "auto" }}>
            {JSON.stringify(detail.published_config, null, 2)}
          </pre>
        </>
      )}
    </div>
  );
};

export default PathwayDetail;
