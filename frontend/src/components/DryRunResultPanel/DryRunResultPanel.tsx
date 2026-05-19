import React from "react";
import { Card, List, Tag, Typography, Space, Button, Empty, Spin } from "antd";
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  ReloadOutlined,
  DeleteOutlined,
  ClockCircleOutlined,
} from "@ant-design/icons";
import type { DryRunResultPanelProps, DryRunResult } from "./DryRunResultPanel.types";

const { Text, Paragraph } = Typography;

const statusConfig = {
  success: { color: "success", icon: <CheckCircleOutlined />, text: "成功" },
  error: { color: "error", icon: <CloseCircleOutlined />, text: "失败" },
  warning: { color: "warning", icon: <ExclamationCircleOutlined />, text: "警告" },
  info: { color: "info", icon: <InfoCircleOutlined />, text: "信息" },
};

export default function DryRunResultPanel({
  results,
  loading = false,
  title = "测试运行结果",
  showTimestamp = true,
  showDuration = true,
  maxHeight = 400,
  onRetry,
  onClear,
  emptyText = "暂无测试结果",
}: DryRunResultPanelProps) {
  const renderResultItem = (result: DryRunResult) => {
    const config = statusConfig[result.status];
    
    return (
      <List.Item
        style={{
          padding: "12px 16px",
          borderBottom: "1px solid var(--mk-border-divider)",
        }}
      >
        <div style={{ width: "100%" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
            <Space>
              <Tag color={config.color} icon={config.icon}>
                {config.text}
              </Tag>
              <Text strong>{result.title}</Text>
            </Space>
            
            <Space size="small">
              {showDuration && result.duration && (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {result.duration}ms
                </Text>
              )}
              {showTimestamp && result.timestamp && (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {result.timestamp}
                </Text>
              )}
            </Space>
          </div>
          
          <Paragraph
            style={{ margin: "8px 0 0", color: "var(--mk-text-secondary)" }}
            ellipsis={{ rows: 2, expandable: true, symbol: "展开" }}
          >
            {result.message}
          </Paragraph>
          
          {result.details && (
            <div
              style={{
                marginTop: 8,
                padding: "8px",
                background: "var(--mk-bg-soft)",
                borderRadius: "var(--mk-radius-sm)",
                fontSize: 12,
                fontFamily: "var(--mk-font-mono)",
                maxHeight: 100,
                overflow: "auto",
              }}
            >
              <pre style={{ margin: 0, whiteSpace: "pre-wrap", wordBreak: "break-all" }}>
                {JSON.stringify(result.details, null, 2)}
              </pre>
            </div>
          )}
        </div>
      </List.Item>
    );
  };

  const successCount = results.filter(r => r.status === "success").length;
  const errorCount = results.filter(r => r.status === "error").length;
  const warningCount = results.filter(r => r.status === "warning").length;

  return (
    <Card
      title={
        <Space>
          <span>{title}</span>
          {results.length > 0 && (
            <Space size="small">
              {successCount > 0 && <Tag color="success">{successCount} 成功</Tag>}
              {errorCount > 0 && <Tag color="error">{errorCount} 失败</Tag>}
              {warningCount > 0 && <Tag color="warning">{warningCount} 警告</Tag>}
            </Space>
          )}
        </Space>
      }
      extra={
        <Space>
          {onRetry && (
            <Button size="small" icon={<ReloadOutlined />} onClick={onRetry}>
              重新运行
            </Button>
          )}
          {onClear && (
            <Button size="small" icon={<DeleteOutlined />} onClick={onClear}>
              清除结果
            </Button>
          )}
        </Space>
      }
      style={{ width: "100%" }}
      bodyStyle={{ padding: 0 }}
    >
      {loading ? (
        <div style={{ textAlign: "center", padding: 24 }}>
          <Spin tip="正在运行测试..." />
        </div>
      ) : results.length === 0 ? (
        <Empty description={emptyText} style={{ padding: 24 }} />
      ) : (
        <div style={{ maxHeight, overflow: "auto" }}>
          <List
            dataSource={results}
            renderItem={renderResultItem}
            split={false}
          />
        </div>
      )}
    </Card>
  );
}