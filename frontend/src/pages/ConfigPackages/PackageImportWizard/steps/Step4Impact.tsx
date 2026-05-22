import { useEffect, useState } from "react";
import { Alert, Descriptions, Select, Spin, Tag, Typography } from "antd";
import { CheckCircleOutlined, WarningOutlined } from "@ant-design/icons";
import { importPackageImpact } from "@/api/configPackage";
import type { ImportImpactResult } from "@/api/types";
import type { WizardContext } from "../types";
import styles from "./steps.module.css";

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
      <div className={styles.loadingContainer}>
        <Spin size="large" />
        <div className={styles.loadingHint}>正在评估影响范围...</div>
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
      <div className={styles.fieldGroup}>
        <Text type="secondary" className={styles.fieldLabel}>
          目标环境
        </Text>
        <Select
          value={context.targetEnvironment}
          onChange={onTargetEnvironmentChange}
          className={styles.targetSelect}
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
          className={styles.alertSpacing}
        />
      ) : (
        <Alert
          type="success"
          showIcon
          icon={<CheckCircleOutlined />}
          message="无冲突"
          description="未发现资产冲突，可以安全发布。"
          className={styles.alertSpacing}
        />
      )}

      {/* 影响统计 */}
      <Descriptions
        title="影响范围"
        column={2}
        size="small"
        labelStyle={{ color: "var(--mk-text-tertiary)" }}
        className={styles.descriptionsSpacing}
      >
        <Descriptions.Item label="目标环境">
          <Tag color={target_environment === "production" ? "red" : "blue"}>
            {target_environment === "production" ? "生产" : "测试"}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="受影响资产数">{impact.assets_affected}</Descriptions.Item>
        <Descriptions.Item label="新增资产">
          <Text className={styles.textSuccess}>{impact.assets_added}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="修改资产">
          <Text className={styles.textPrimary}>{impact.assets_modified}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="移除资产">
          <Text className={styles.textDanger}>{impact.assets_removed}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="影响范围">
          {impact.scopes_affected.length > 0 ? impact.scopes_affected.join("、") : "无"}
        </Descriptions.Item>
      </Descriptions>

      {/* 冲突明细 */}
      {hasConflicts && (
        <div>
          <h4 className={styles.sectionHeadingWarning}>
            <WarningOutlined className={styles.iconWithMargin} />
            冲突明细
          </h4>
          <div className={styles.scrollableList}>
            {impact.conflicts.map((conflict, i) => (
              <div
                key={i}
                className={i < impact.conflicts.length - 1 ? styles.listItemDivider : styles.listItem}
              >
                <Tag color="warning">{conflict.conflict_type}</Tag>
                <code className={styles.monoCode}>{conflict.asset_code}</code>
                <Text type="secondary" className={styles.smallText}>
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
