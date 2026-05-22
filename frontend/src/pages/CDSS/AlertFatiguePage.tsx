import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  Card,
  Col,
  Descriptions,
  Form,
  InputNumber,
  Modal,
  Row,
  Select,
  Statistic,
  Switch,
  Table,
  Tag,
  message,
} from "antd";
import {
  listFatigueConfigs,
  createFatigueConfig,
  updateFatigueConfig,
  getOverrideAnalysis,
  type AlertFatigueConfigData,
  type OverrideAnalysis,
} from "../../api/cdss";
import styles from "./alertFatiguePage.module.css";

const TRIGGER_OPTIONS = [
  { value: "ORDER_PLACED", label: "医嘱下达" },
  { value: "EMR_SAVED", label: "病历保存" },
  { value: "EXAM_REQUESTED", label: "检查申请" },
  { value: "PATHWAY_ENTRY", label: "入径评估" },
  { value: "INSURANCE_SETTLEMENT", label: "医保结算" },
  { value: "DRUG_DISPENSED", label: "药品调配" },
  { value: "DISCHARGE", label: "出院前" },
];

const RISK_OPTIONS = [
  { value: "INFO", label: "信息提示" },
  { value: "LOW", label: "低风险" },
  { value: "MEDIUM", label: "中风险" },
  { value: "HIGH", label: "高风险" },
  { value: "CRITICAL", label: "危急" },
];

const AlertFatiguePage: React.FC = () => {
  const [configs, setConfigs] = useState<AlertFatigueConfigData[]>([]);
  const [analysis, setAnalysis] = useState<OverrideAnalysis | null>(null);
  const [loading, setLoading] = useState(false);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [createForm] = Form.useForm();

  const fetchConfigs = useCallback(async () => {
    try {
      const data = await listFatigueConfigs();
      setConfigs(data);
    } catch {
      message.error("获取疲劳治理配置失败");
    }
  }, []);

  const fetchAnalysis = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getOverrideAnalysis();
      setAnalysis(data);
    } catch {
      message.error("获取覆盖分析失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchConfigs();
    fetchAnalysis();
  }, [fetchConfigs, fetchAnalysis]);

  const handleCreate = async (values: Record<string, unknown>) => {
    try {
      await createFatigueConfig(values as Parameters<typeof createFatigueConfig>[0]);
      message.success("配置创建成功");
      setCreateModalVisible(false);
      createForm.resetFields();
      fetchConfigs();
    } catch {
      message.error("创建失败");
    }
  };

  const handleToggleStatus = async (configId: string, currentStatus: string) => {
    try {
      await updateFatigueConfig(configId, {
        status: currentStatus === "ACTIVE" ? "DISABLED" : "ACTIVE",
      });
      message.success("状态已更新");
      fetchConfigs();
    } catch {
      message.error("更新失败");
    }
  };

  const configColumns = [
    { title: "配置ID", dataIndex: "config_id", key: "config_id", width: 120 },
    {
      title: "触发点",
      dataIndex: "trigger_point",
      key: "trigger_point",
      width: 120,
      render: (v: string) => v ? TRIGGER_OPTIONS.find((o) => o.value === v)?.label ?? v : "全部",
    },
    {
      title: "风险等级",
      dataIndex: "risk_level",
      key: "risk_level",
      width: 100,
      render: (v: string) => v ? RISK_OPTIONS.find((o) => o.value === v)?.label ?? v : "全部",
    },
    {
      title: "去重",
      key: "dedup",
      width: 80,
      render: (_: unknown, record: AlertFatigueConfigData) => (
        record.deduplication_enabled
          ? <Tag color="green">{record.deduplication_window_minutes}分钟</Tag>
          : <Tag>关闭</Tag>
      ),
    },
    {
      title: "抑制",
      key: "supp",
      width: 80,
      render: (_: unknown, record: AlertFatigueConfigData) => (
        record.suppression_enabled
          ? <Tag color="blue">{record.suppression_max_alerts_per_hour}/时</Tag>
          : <Tag>关闭</Tag>
      ),
    },
    {
      title: "静默期",
      key: "quiet",
      width: 80,
      render: (_: unknown, record: AlertFatigueConfigData) => (
        record.quiet_period_enabled
          ? <Tag color="purple">{record.quiet_period_minutes}分钟</Tag>
          : <Tag>关闭</Tag>
      ),
    },
    {
      title: "智能过滤",
      key: "smart",
      width: 80,
      render: (_: unknown, record: AlertFatigueConfigData) => (
        record.smart_filter_enabled
          ? <Tag color="cyan">阈值{Math.round(record.override_rate_threshold * 100)}%</Tag>
          : <Tag>关闭</Tag>
      ),
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 80,
      render: (v: string) => <Tag color={v === "ACTIVE" ? "green" : "default"}>{v === "ACTIVE" ? "启用" : "停用"}</Tag>,
    },
    {
      title: "操作",
      key: "action",
      width: 100,
      render: (_: unknown, record: AlertFatigueConfigData) => (
        <Button size="small" onClick={() => handleToggleStatus(record.config_id, record.status)}>
          {record.status === "ACTIVE" ? "停用" : "启用"}
        </Button>
      ),
    },
  ];

  const highOverrideColumns = [
    { title: "规则编码", dataIndex: "rule_code", key: "rule_code", width: 160 },
    { title: "告警数", dataIndex: "alert_count", key: "alert_count", width: 80 },
    { title: "覆盖数", dataIndex: "override_count", key: "override_count", width: 80 },
    {
      title: "覆盖率",
      dataIndex: "override_rate",
      key: "override_rate",
      width: 100,
      render: (v: number) => {
            let color = "green";
            if (v > 80) color = "red";
            else if (v > 50) color = "orange";
            return <Tag color={color}>{v}%</Tag>;
          },
    },
    { title: "建议", dataIndex: "recommendation", key: "recommendation" },
  ];

  return (
    <div className={styles.page}>
      {/* 覆盖模式分析 */}
      <Card title="覆盖模式分析" style={{ marginBottom: 16 }}>
        {analysis && (
          <>
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={4}>
                <Statistic title="总告警" value={analysis.total_alerts} />
              </Col>
              <Col span={4}>
                <Statistic title="覆盖继续" value={analysis.total_overrides} />
              </Col>
              <Col span={4}>
                <Statistic title="确认知悉" value={analysis.total_acknowledges} />
              </Col>
              <Col span={4}>
                <Statistic title="上报上级" value={analysis.total_escalations} />
              </Col>
              <Col span={4}>
                <Statistic
                  title="覆盖率"
                  value={analysis.override_rate}
                  suffix="%"
                  valueStyle={{ color: analysis.override_rate > 50 ? "var(--mk-warning)" : "var(--mk-success)" }}
                />
              </Col>
              <Col span={4}>
                <Button onClick={fetchAnalysis} loading={loading}>刷新</Button>
              </Col>
            </Row>

            {analysis.high_override_rules.length > 0 && (
              <Card type="inner" title="高覆盖规则预警" size="small">
                <Table
                  rowKey="rule_code"
                  columns={highOverrideColumns}
                  dataSource={analysis.high_override_rules}
                  pagination={false}
                  size="small"
                />
              </Card>
            )}

            <Row gutter={16} style={{ marginTop: 12 }}>
              <Col span={8}>
                <Descriptions column={1} size="small" bordered title="按规则">
                  {Object.entries(analysis.override_by_rule).map(([k, v]) => (
                    <Descriptions.Item key={k} label={k}>{v}</Descriptions.Item>
                  ))}
                </Descriptions>
              </Col>
              <Col span={8}>
                <Descriptions column={1} size="small" bordered title="按触发点">
                  {Object.entries(analysis.override_by_trigger).map(([k, v]) => (
                    <Descriptions.Item key={k} label={k}>{v}</Descriptions.Item>
                  ))}
                </Descriptions>
              </Col>
              <Col span={8}>
                <Descriptions column={1} size="small" bordered title="按操作人">
                  {Object.entries(analysis.override_by_operator).map(([k, v]) => (
                    <Descriptions.Item key={k} label={k}>{v}</Descriptions.Item>
                  ))}
                </Descriptions>
              </Col>
            </Row>
          </>
        )}
      </Card>

      {/* 疲劳治理配置 */}
      <Card
        title="疲劳治理配置"
        extra={
          <Button type="primary" onClick={() => setCreateModalVisible(true)}>
            新建配置
          </Button>
        }
      >
        <Table
          rowKey="config_id"
          columns={configColumns}
          dataSource={configs}
          pagination={false}
          size="small"
        />
      </Card>

      {/* 新建配置弹窗 */}
      <Modal
        title="新建疲劳治理配置"
        open={createModalVisible}
        onCancel={() => { setCreateModalVisible(false); createForm.resetFields(); }}
        onOk={() => createForm.submit()}
        width={640}
      >
        <Form form={createForm} layout="vertical" onFinish={handleCreate} initialValues={{
          deduplication_enabled: true,
          deduplication_window_minutes: 30,
          suppression_enabled: true,
          suppression_max_alerts_per_hour: 20,
          quiet_period_enabled: true,
          quiet_period_minutes: 60,
          smart_filter_enabled: true,
          override_rate_threshold: 0.8,
        }}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="trigger_point" label="触发点">
                <Select allowClear options={TRIGGER_OPTIONS} placeholder="全部触发点" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="risk_level" label="风险等级">
                <Select allowClear options={RISK_OPTIONS} placeholder="全部等级" />
              </Form.Item>
            </Col>
          </Row>
          <Card type="inner" title="去重策略" size="small" style={{ marginBottom: 12 }}>
            <Form.Item name="deduplication_enabled" label="启用去重" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="deduplication_window_minutes" label="去重窗口（分钟）">
              <InputNumber min={1} max={1440} style={{ width: "100%" }} />
            </Form.Item>
          </Card>
          <Card type="inner" title="抑制策略" size="small" style={{ marginBottom: 12 }}>
            <Form.Item name="suppression_enabled" label="启用抑制" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="suppression_max_alerts_per_hour" label="每小时最大告警数">
              <InputNumber min={1} max={1000} style={{ width: "100%" }} />
            </Form.Item>
          </Card>
          <Card type="inner" title="静默期策略" size="small" style={{ marginBottom: 12 }}>
            <Form.Item name="quiet_period_enabled" label="启用静默期" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="quiet_period_minutes" label="静默期（分钟）">
              <InputNumber min={1} max={1440} style={{ width: "100%" }} />
            </Form.Item>
          </Card>
          <Card type="inner" title="智能过滤" size="small">
            <Form.Item name="smart_filter_enabled" label="启用智能过滤" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="override_rate_threshold" label="覆盖率阈值">
              <InputNumber min={0} max={1} step={0.1} style={{ width: "100%" }} />
            </Form.Item>
          </Card>
        </Form>
      </Modal>
    </div>
  );
};

export default AlertFatiguePage;
