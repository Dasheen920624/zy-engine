import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Alert, Button, Descriptions, Input, Space, Typography, message } from "antd";
import { ExclamationCircleOutlined } from "@ant-design/icons";
import { importPackageConfirm } from "@/api/configPackage";
import type { ApiError, ConfigPackageDetail } from "@/api/types";
import type { WizardContext } from "../types";
import styles from "./steps.module.css";

const { Text } = Typography;

interface Step5ConfirmProps {
  context: WizardContext;
  onPublishSuccess: (result: ConfigPackageDetail) => void;
}

export default function Step5Confirm({ context, onPublishSuccess }: Step5ConfirmProps) {
  const [confirmName, setConfirmName] = useState(context.confirmPackageName);
  const [reason, setReason] = useState(context.confirmReason);

  const packageCode = context.validateResult?.manifest.package_code ?? "";

  const publishMut = useMutation<ConfigPackageDetail, ApiError>({
    mutationFn: () =>
      importPackageConfirm({
        upload_id: context.uploadId,
        approved_by: confirmName.trim(),
        approved_note: reason.trim(),
        target_environment: context.targetEnvironment,
      }),
    onSuccess: (data) => {
      message.success("发布成功！");
      onPublishSuccess(data);
    },
    onError: (err) => {
      message.error(`发布失败: ${err.message}`);
    },
  });

  const canPublish = confirmName.trim() === packageCode && reason.trim().length > 0;

  return (
    <div>
      {/* 危险提示 */}
      <Alert
        type="error"
        showIcon
        icon={<ExclamationCircleOutlined />}
        message="此操作不可撤销"
        description="发布后配置包将写入目标环境，请仔细确认以下信息。"
        style={{ marginBottom: 16 }}
      />

      {/* 发布信息确认 */}
      <Descriptions
        title="发布信息确认"
        column={1}
        size="small"
        labelStyle={{ color: "var(--mk-text-tertiary)", width: 140 }}
        style={{ marginBottom: 16 }}
      >
        <Descriptions.Item label="包编码">
          <code className={styles.monoCode}>{packageCode}</code>
        </Descriptions.Item>
        <Descriptions.Item label="版本">
          <code className={styles.monoCode}>
            {context.validateResult?.manifest.package_version}
          </code>
        </Descriptions.Item>
        <Descriptions.Item label="资产类型">
          {context.validateResult?.manifest.asset_type}
        </Descriptions.Item>
        <Descriptions.Item label="目标环境">
          <span style={{ color: context.targetEnvironment === "production" ? "var(--mk-danger)" : "var(--mk-brand-primary)" }}>
            {context.targetEnvironment === "production" ? "生产环境" : "测试环境"}
          </span>
        </Descriptions.Item>
        <Descriptions.Item label="受影响资产">
          {context.impactResult?.impact.assets_affected ?? "—"}
        </Descriptions.Item>
        <Descriptions.Item label="冲突数">
          <Text style={{ color: (context.impactResult?.impact.conflicts.length ?? 0) > 0 ? "var(--mk-warning)" : "var(--mk-success)" }}>
            {context.impactResult?.impact.conflicts.length ?? 0}
          </Text>
        </Descriptions.Item>
      </Descriptions>

      {/* 确认输入 */}
      <div className={styles.fieldGroup}>
        <Text type="secondary" className={styles.fieldLabel}>
          请输入包编码确认 <span className={styles.textDanger}>*</span>
        </Text>
        <Input
          placeholder={`请输入 "${packageCode}" 以确认发布`}
          value={confirmName}
          onChange={(e) => setConfirmName(e.target.value)}
          status={confirmName && confirmName !== packageCode ? "error" : undefined}
        />
        {confirmName && confirmName !== packageCode && (
          <Text type="danger" className={styles.smallText}>包编码不匹配</Text>
        )}
      </div>

      <div className={styles.fieldGroup}>
        <Text type="secondary" className={styles.fieldLabel}>
          发布原因 <span className={styles.textDanger}>*</span>
        </Text>
        <Input.TextArea
          placeholder="必填 · 例：医学审核已通过，建议本周二上线"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          rows={3}
        />
      </div>

      {/* 发布按钮 */}
      <div className={styles.formActions}>
        <Space>
          <Button
            type="primary"
            danger
            size="large"
            loading={publishMut.isPending}
            disabled={!canPublish}
            onClick={() => publishMut.mutate()}
            style={{ minWidth: 120 }}
          >
            确认发布
          </Button>
        </Space>
      </div>
    </div>
  );
}
