import { useEffect, useState } from "react";
import { Alert, Descriptions, Spin, Typography } from "antd";
import { CheckCircleOutlined, CloseCircleOutlined } from "@ant-design/icons";
import { importPackageSourceCheck } from "@/api/configPackage";
import type { ImportSourceCheckResult } from "@/api/types";
import { StatusBadge } from "@/components/StatusBadge";
import type { WizardContext } from "../types";

const { Text } = Typography;

function getSourceBadgeStatus(status: string): "missing_source" | "reviewed" | "pending" | "rejected" {
  if (status === "missing") return "missing_source";
  if (status === "reviewed") return "reviewed";
  if (status === "pending") return "pending";
  return "rejected";
}

interface Step3SourceCheckProps {
  context: WizardContext;
  onSourceCheckComplete: (result: ImportSourceCheckResult) => void;
}

export default function Step3SourceCheck({ context, onSourceCheckComplete }: Step3SourceCheckProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (context.sourceCheckResult || !context.uploadId) return;

    let cancelled = false;
    setLoading(true);
    setError(null);

    importPackageSourceCheck({ upload_id: context.uploadId })
      .then((result) => {
        if (!cancelled) {
          onSourceCheckComplete(result);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "来源检查失败");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [context.uploadId, context.sourceCheckResult, onSourceCheckComplete]);

  if (loading) {
    return (
      <div style={{ textAlign: "center", padding: 40 }}>
        <Spin size="large" />
        <div style={{ marginTop: 12, color: "var(--mk-text-tertiary)" }}>正在检查来源审核状态...</div>
      </div>
    );
  }

  if (error) {
    return (
      <Alert
        type="error"
        showIcon
        message="来源检查失败"
        description={error}
      />
    );
  }

  if (!context.sourceCheckResult) {
    return <Text type="secondary">等待来源检查...</Text>;
  }

  const { source_review } = context.sourceCheckResult;
  const hasMissingSource = source_review.missing_count > 0;

  return (
    <div>
      {/* 来源审核总览 */}
      {hasMissingSource ? (
        <Alert
          type="error"
          showIcon
          icon={<CloseCircleOutlined />}
          message="存在来源缺失，无法继续下一步"
          description={`缺失来源 ${source_review.missing_count} 条，过期来源 ${source_review.expired_count} 条，未审核 ${source_review.unreviewed_count} 条。请先补充来源信息后再继续。`}
          style={{ marginBottom: 16 }}
        />
      ) : (
        <Alert
          type="success"
          showIcon
          icon={<CheckCircleOutlined />}
          message="来源审核通过"
          description="所有资产来源均已审核，可以继续下一步。"
          style={{ marginBottom: 16 }}
        />
      )}

      {/* 来源审核详情 */}
      <Descriptions
        title="来源审核状态"
        column={2}
        size="small"
        labelStyle={{ color: "var(--mk-text-tertiary)" }}
        style={{ marginBottom: 16 }}
      >
        <Descriptions.Item label="来源审核">
          <StatusBadge
            status={hasMissingSource ? "missing_source" : "reviewed"}
            size="sm"
          />
        </Descriptions.Item>
        <Descriptions.Item label="是否阻断">
          <StatusBadge
            status={source_review.blocked ? "danger" : "success"}
            size="sm"
            text={source_review.blocked ? "已阻断" : "未阻断"}
          />
        </Descriptions.Item>
        <Descriptions.Item label="缺失来源数">
          <Text style={{ color: source_review.missing_count > 0 ? "var(--mk-danger)" : "var(--mk-success)" }}>
            {source_review.missing_count}
          </Text>
        </Descriptions.Item>
        <Descriptions.Item label="过期来源数">
          <Text style={{ color: source_review.expired_count > 0 ? "var(--mk-warning)" : "var(--mk-success)" }}>
            {source_review.expired_count}
          </Text>
        </Descriptions.Item>
        <Descriptions.Item label="未审核数">
          <Text style={{ color: source_review.unreviewed_count > 0 ? "var(--mk-warning)" : "var(--mk-success)" }}>
            {source_review.unreviewed_count}
          </Text>
        </Descriptions.Item>
        <Descriptions.Item label="允许发布">
          <StatusBadge
            status={source_review.allow_publish ? "success" : "danger"}
            size="sm"
            text={source_review.allow_publish ? "是" : "否"}
          />
        </Descriptions.Item>
      </Descriptions>

      {/* 资产来源明细 */}
      {source_review.details.length > 0 && (
        <div>
          <h4 style={{ marginBottom: 8, fontWeight: 500 }}>资产来源明细</h4>
          <div style={{ maxHeight: 300, overflow: "auto" }}>
            {source_review.details.map((detail, i) => (
              <div
                key={i}
                style={{
                  padding: "8px 12px",
                  borderBottom: i < source_review.details.length - 1 ? "1px solid var(--mk-border-divider)" : "none",
                  display: "flex",
                  alignItems: "center",
                  gap: 8,
                }}
              >
                <StatusBadge
                  status={getSourceBadgeStatus(detail.status)}
                  size="sm"
                  showIcon
                  showText={false}
                />
                <code style={{ fontFamily: "var(--mk-font-mono)", fontSize: 12 }}>{detail.asset_code}</code>
                <Text type="secondary" style={{ fontSize: 12 }}>{detail.asset_type}</Text>
                {detail.document_name && (
                  <Text style={{ fontSize: 12 }}>{detail.document_name}</Text>
                )}
                {detail.reviewer && (
                  <Text type="secondary" style={{ fontSize: 12 }}>审核人：{detail.reviewer}</Text>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
