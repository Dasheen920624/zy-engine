import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  Card,
  Col,
  Progress,
  Row,
  Space,
  Spin,
  Statistic,
  Table,
  message,
} from "antd";
import {
  BarChartOutlined,
  DownloadOutlined,
  TeamOutlined,
  ApiOutlined,
  AppstoreOutlined,
  ClockCircleOutlined,
} from "@ant-design/icons";
import {
  getUsageReport,
  exportUsageReport,
  type UsageReport,
  type ApiCallCount,
  type FeatureUsage,
} from "../../api/commercial";
import styles from "./commercial.module.css";

const UsageDashboard: React.FC = () => {
  const [report, setReport] = useState<UsageReport | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchReport = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getUsageReport();
      setReport(data);
    } catch {
      message.error("获取用量报告失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchReport();
  }, [fetchReport]);

  const handleExport = async () => {
    try {
      const blob = await exportUsageReport();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `usage-report-${new Date().toISOString().slice(0, 10)}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      message.success("用量报告导出成功");
    } catch {
      message.error("导出失败");
    }
  };

  const apiCallColumns = [
    { title: "接口端点", dataIndex: "endpoint", key: "endpoint", width: 280 },
    {
      title: "调用次数",
      dataIndex: "call_count",
      key: "call_count",
      width: 120,
      sorter: (a: ApiCallCount, b: ApiCallCount) => a.call_count - b.call_count,
      defaultSortOrder: "descend" as const,
    },
    { title: "最后调用时间", dataIndex: "last_called", key: "last_called", width: 180 },
  ];

  const featureUsageColumns = [
    { title: "功能", dataIndex: "feature_name", key: "feature_name", width: 200 },
    { title: "功能标识", dataIndex: "feature_key", key: "feature_key", width: 180 },
    {
      title: "使用次数",
      dataIndex: "usage_count",
      key: "usage_count",
      width: 120,
      sorter: (a: FeatureUsage, b: FeatureUsage) => a.usage_count - b.usage_count,
      defaultSortOrder: "descend" as const,
    },
    { title: "最后使用时间", dataIndex: "last_used", key: "last_used", width: 180 },
  ];

  const userPercent = report
    ? Math.round((report.summary.active_users / report.summary.max_users) * 100)
    : 0;

  return (
    <div className={styles.page}>
      <Spin spinning={loading}>
        {/* 概览卡片 */}
        <Row gutter={16} className={styles.sectionCard}>
          <Col span={6}>
            <Card>
              <Statistic
                title="活跃用户"
                value={report?.summary.active_users ?? 0}
                prefix={<TeamOutlined />}
                suffix={`/ ${report?.summary.max_users ?? 0}`}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="本月 API 调用"
                value={report?.summary.api_calls_this_month ?? 0}
                prefix={<ApiOutlined />}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="已使用功能"
                value={report?.summary.features_used ?? 0}
                prefix={<AppstoreOutlined />}
                suffix={`/ ${report?.summary.total_features ?? 0}`}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="剩余天数"
                value={report?.summary.days_remaining ?? 0}
                prefix={<ClockCircleOutlined />}
                suffix="天"
              />
            </Card>
          </Col>
        </Row>

        {/* 用户限额进度 */}
        <Card title="用户限额" className={styles.sectionCard}>
          <Space direction="vertical" style={{ width: "100%" }}>
            <Progress
              percent={userPercent}
              status={userPercent >= 90 ? "exception" : userPercent >= 70 ? "active" : "normal"}
              format={() => `${report?.summary.active_users ?? 0} / ${report?.summary.max_users ?? 0}`}
            />
          </Space>
        </Card>

        {/* API 调用统计 */}
        <Card
          title={<Space><ApiOutlined />API 调用统计</Space>}
          className={styles.sectionCard}
        >
          {report ? (
            <Table
              rowKey="endpoint"
              columns={apiCallColumns}
              dataSource={report.api_calls}
              pagination={false}
              size="small"
            />
          ) : (
            <div className={styles.emptyHint}>暂无 API 调用数据</div>
          )}
        </Card>

        {/* 功能使用统计 */}
        <Card
          title={<Space><AppstoreOutlined />功能使用统计</Space>}
          extra={
            <Button icon={<DownloadOutlined />} onClick={handleExport}>
              导出报告
            </Button>
          }
        >
          {report ? (
            <Table
              rowKey="feature_key"
              columns={featureUsageColumns}
              dataSource={report.feature_usage}
              pagination={false}
              size="small"
            />
          ) : (
            <div className={styles.emptyHint}>暂无功能使用数据</div>
          )}
        </Card>
      </Spin>
    </div>
  );
};

export default UsageDashboard;
