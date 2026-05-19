import { useEffect, useState } from "react";
import { Alert, Descriptions, Select, Spin, Tag, Typography } from "antd";
import { CheckCircleOutlined, WarningOutlined } from "@ant-design/icons";
import { importPackageImpact } from "@/api/configPackage";
import type { ImportImpactResult } from "@/api/types";
import type { WizardContext } from "../types";

const { Text } = Typography;

interface Step4ImpactProps {
  context: WizardContext;
  onImpactComplete: (result: ImportImpactResult) => void;
  onTargetEnvironmentChange: (env: string) => void;
}

export default function Step4Impact({ context, onImpactComplete, onTargetEnvironmentChange }: Step4ImpactProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (context.impactResult || !context.uploadId) return;

    let cancelled = false;
    setLoading(true);
    setError(null);

    importPackageImpact({ upload_id: context.uploadId, target_environment: context.targetEnvironment })
      .then((result) => {
        if (!cancelled) {
          onImpactComplete(result);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "影响评估失败");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [context.uploadId, context.impactResult, context.targetEnvironment, onImpactComplete]);

  if (loading) {
    return (
      <div style={{ textAlign: "center", padding: 40 }}>
        <Spin size="large" />
        <div style={{ marginTop: 12, color: "var(--mk-text-tertiary)" }}>正在评估影响范围...</div>
      </div>
    );
  }

  if (error) {
    return (
      <Alert
        type="error"
        showIcon
        message="影响评估失败"
        description={error}
      />
    );
  }

  if (!context.impactResult) {
    return <Text type="secondary">等待影响评估...</Text>;
  }

  const { impact, target_environment } = context.impactResult;
  const hasConflicts = impact.conflicts.length > 0;

  return (
    <div>
      {/* 目标环境选择 */}
      <div style={{ marginBottom: 16 }}>
        <Text type="secondary" style={{ fontSize: 12, display: "block", marginBottom: 4 }}>
          目标环境
        </Text>
        <Select
          value={context.targetEnvironment}
          onChange={onTargetEnvironmentChange}
          style={{ width: 200 }}
          options={[
            { value: "production", label: "生产环境" },
            { value: "staging", label: "测试环境" },
          ]}
        />
      </div>

      {/* 影响总览 */}
      {hasConflicts ? (
        <Alert
          type="warning"
          showIcon
          icon={<WarningOutlined />}
          message="存在冲突"
          description={`发现 ${impact.conflicts.length} 个资产冲突，请仔细评估后再继续。`}
          style={{ marginBottom: 16 }}
        />
      ) : (
        <Alert
          type="success"
          showIcon
          icon={<CheckCircleOutlined />}
          message="无冲突"
          description="未发现资产冲突，可以安全发布。"
          style={{ marginBottom: 16 }}
        />
      )}

      {/* 影响统计 */}
      <Descriptions
        title="影响范围"
        column={2}
        size="small"
        labelStyle={{ color: "var(--mk-text-tertiary)" }}
        style={{ marginBottom: 16 }}
      >
        <Descriptions.Item label="目标环境">
          <Tag color={target_environment === "production" ? "red" : "blue"}>
            {target_environment === "production" ? "生产" : "测试"}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="受影响资产数">{impact.assets_affected}</Descriptions.Item>
        <Descriptions.Item label="新增资产">
          <Text style={{ color: "var(--mk-success)" }}>{impact.assets_added}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="修改资产">
          <Text style={{ color: "var(--mk-brand-primary)" }}>{impact.assets_modified}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="移除资产">
          <Text style={{ color: "var(--mk-danger)" }}>{impact.assets_removed}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="影响范围">
          {impact.scopes_affected.length > 0 ? impact.scopes_affected.join("、") : "无"}
        </Descriptions.Item>
      </Descriptions>

      {/* 冲突明细 */}
      {hasConflicts && (
        <div>
          <h4 style={{ marginBottom: 8, fontWeight: 500, color: "var(--mk-warning)" }}>
            <WarningOutlined style={{ marginRight: 8 }} />
            冲突明细
          </h4>
          <div style={{ maxHeight: 200, overflow: "auto" }}>
            {impact.conflicts.map((conflict, i) => (
              <div
                key={i}
                style={{
                  padding: "8px 12px",
                  borderBottom: i < impact.conflicts.length - 1 ? "1px solid var(--mk-border-divider)" : "none",
                  display: "flex",
                  alignItems: "center",
                  gap: 8,
                  fontSize: 13,
                }}
              >
                <Tag color="warning">{conflict.conflict_type}</Tag>
                <code style={{ fontFamily: "var(--mk-font-mono)", fontSize: 12 }}>{conflict.asset_code}</code>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  现有版本 {conflict.existing_version} → 新版本 {conflict.new_version}
                </Text>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
