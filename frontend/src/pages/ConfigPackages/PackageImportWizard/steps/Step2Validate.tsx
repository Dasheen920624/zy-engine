import { useEffect, useState } from "react";
import { Descriptions, Table, Tag, Typography, Spin, Alert } from "antd";
import { CheckCircleOutlined, CloseCircleOutlined } from "@ant-design/icons";
import { importPackageValidate } from "@/api/configPackage";
import type { ImportValidateResult } from "@/api/types";
import type { WizardContext } from "../types";

const { Text } = Typography;

interface Step2ValidateProps {
  context: WizardContext;
  onValidateComplete: (result: ImportValidateResult) => void;
}

function checkStatusTag(status: string) {
  switch (status) {
    case "PASS":
      return <Tag color="success">通过</Tag>;
    case "FAIL":
      return <Tag color="error">失败</Tag>;
    case "WARN":
      return <Tag color="warning">警告</Tag>;
    default:
      return <Tag>{status}</Tag>;
  }
}

export default function Step2Validate({ context, onValidateComplete }: Step2ValidateProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (context.validateResult || !context.uploadId) return;

    let cancelled = false;
    setLoading(true);
    setError(null);

    importPackageValidate({ upload_id: context.uploadId })
      .then((result) => {
        if (!cancelled) {
          onValidateComplete(result);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "校验失败");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [context.uploadId, context.validateResult, onValidateComplete]);

  if (loading) {
    return (
      <div style={{ textAlign: "center", padding: 40 }}>
        <Spin size="large" />
        <div style={{ marginTop: 12, color: "var(--mk-text-tertiary)" }}>正在校验配置包...</div>
      </div>
    );
  }

  if (error) {
    return (
      <Alert
        type="error"
        showIcon
        message="校验失败"
        description={error}
      />
    );
  }

  if (!context.validateResult) {
    return <Text type="secondary">等待校验...</Text>;
  }

  const { manifest, validation_results, valid } = context.validateResult;

  const columns = [
    {
      title: "检查项",
      dataIndex: "check",
      key: "check",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (v: string) => checkStatusTag(v),
    },
    {
      title: "说明",
      dataIndex: "message",
      key: "message",
    },
  ];

  return (
    <div>
      {/* 校验总览 */}
      <Alert
        type={valid ? "success" : "error"}
        showIcon
        icon={valid ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
        message={valid ? "校验通过" : "校验未通过"}
        style={{ marginBottom: 16 }}
      />

      {/* Manifest 信息 */}
      <Descriptions
        title="Manifest 信息"
        column={2}
        size="small"
        labelStyle={{ color: "var(--mk-text-tertiary)" }}
        style={{ marginBottom: 16 }}
      >
        <Descriptions.Item label="包编码">
          <code style={{ fontFamily: "var(--mk-font-mono)" }}>{manifest.package_code}</code>
        </Descriptions.Item>
        <Descriptions.Item label="版本">
          <code style={{ fontFamily: "var(--mk-font-mono)" }}>{manifest.package_version}</code>
        </Descriptions.Item>
        <Descriptions.Item label="资产类型">{manifest.asset_type}</Descriptions.Item>
        <Descriptions.Item label="范围">{manifest.scope_level} · {manifest.scope_code}</Descriptions.Item>
        {manifest.base_version && (
          <Descriptions.Item label="基础版本">
            <code style={{ fontFamily: "var(--mk-font-mono)" }}>{manifest.base_version}</code>
          </Descriptions.Item>
        )}
        <Descriptions.Item label="资产数量">{manifest.items.length}</Descriptions.Item>
      </Descriptions>

      {/* 校验结果 */}
      <h4 style={{ marginBottom: 8, fontWeight: 500 }}>校验结果</h4>
      <Table
        dataSource={validation_results}
        columns={columns}
        rowKey="check"
        size="small"
        pagination={false}
      />
    </div>
  );
}
