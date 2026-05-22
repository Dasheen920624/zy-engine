import { useState } from "react";
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  message,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import {
  ExperimentOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
} from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  evaluateRuleEngine,
  listRuleExecLogs,
  type EvaluateRequest,
  type EvaluateResponse,
  type HitItem,
  type RuleExecLog,
} from "../../api/rule";
import { getOrgContext } from "../../store/orgContext";
import styles from "./RuleValidate.module.css";

const { Title, Paragraph, Text } = Typography;
const { TextArea } = Input;
const { Option } = Select;

const SCENARIO_OPTIONS = [
  { label: "病历质控", value: "EMR_QC" },
  { label: "医保质控", value: "INSURANCE_QC" },
  { label: "医嘱安全", value: "ORDER_SAFETY" },
  { label: "路径节点", value: "PATHWAY_NODE" },
  { label: "随访提醒", value: "FOLLOWUP" },
  { label: "手术核查", value: "OPERATION" },
];

const SEVERITY_COLORS: Record<string, string> = {
  CRITICAL: "red",
  HIGH: "orange",
  MEDIUM: "gold",
  LOW: "green",
  INFO: "blue",
};

/**
 * 规则校验工作台（PR-V3-09）。
 *
 * 功能：
 *  - 选择场景 + 输入患者上下文 → 在线试运行规则引擎
 *  - 查看命中结果（命中规则、严重级别、建议操作、来源证据）
 *  - 查看历史执行日志
 *
 * 后端端点：
 *  - POST /api/rule-engine/evaluate  场景化评估
 *  - GET  /api/rules/exec-logs       执行历史
 */
export default function RuleValidate() {
  const [form] = Form.useForm();
  const [evaluateResult, setEvaluateResult] = useState<EvaluateResponse | null>(null);
  const tenant_id = getOrgContext().tenant_id || "TENANT_DEMO";

  const evaluateMutation = useMutation({
    mutationFn: async (values: {
      scenario_code: string;
      patient_context: string;
    }) => {
      let patient_context: Record<string, unknown>;
      try {
        patient_context = JSON.parse(values.patient_context);
      } catch {
        throw new Error("患者上下文 JSON 格式错误");
      }

      const request: EvaluateRequest = {
        scenario_code: values.scenario_code,
        patient_context,
        tenant_id,
      };

      return evaluateRuleEngine(request);
    },
    onSuccess: (data) => {
      setEvaluateResult(data);
      message.success(`规则评估完成，命中 ${data.hits?.length ?? 0} 条规则`);
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : "规则评估失败");
    },
  });

  const logsQuery = useQuery({
    queryKey: ["rule", "exec-logs", tenant_id],
    queryFn: () =>
      listRuleExecLogs({
        tenant_id,
        limit: 20,
      }),
  });

  const hitColumns: ColumnsType<HitItem> = [
    {
      title: "规则编码",
      dataIndex: "rule_code",
      key: "rule_code",
      width: 180,
    },
    {
      title: "规则名称",
      dataIndex: "rule_name",
      key: "rule_name",
      width: 200,
    },
    {
      title: "严重级别",
      dataIndex: "severity",
      key: "severity",
      width: 100,
      render: (severity: string) => (
        <Tag color={SEVERITY_COLORS[severity] || "default"}>{severity}</Tag>
      ),
    },
    {
      title: "建议操作",
      dataIndex: "action_mode",
      key: "action_mode",
      width: 120,
      render: (mode: string) => {
        const modeLabels: Record<string, { color: string; label: string }> = {
          NOTICE: { color: "blue", label: "通知" },
          SOFT: { color: "gold", label: "软提醒" },
          BLOCK: { color: "red", label: "阻断" },
        };
        const info = modeLabels[mode] || { color: "default", label: mode };
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: "命中说明",
      dataIndex: "hit_reason",
      key: "hit_reason",
      ellipsis: true,
    },
    {
      title: "来源证据",
      dataIndex: "source_evidence",
      key: "source_evidence",
      width: 200,
      render: (evidence?: string) =>
        evidence ? (
          <Text type="secondary" ellipsis={{ tooltip: evidence }}>
            {evidence}
          </Text>
        ) : (
          <Text type="secondary">-</Text>
        ),
    },
  ];

  const logColumns: ColumnsType<RuleExecLog> = [
    {
      title: "执行ID",
      dataIndex: "eval_id",
      key: "eval_id",
      width: 150,
      ellipsis: true,
    },
    {
      title: "场景",
      dataIndex: "scenario_code",
      key: "scenario_code",
      width: 120,
    },
    {
      title: "命中数",
      dataIndex: "hit_count",
      key: "hit_count",
      width: 80,
      render: (count: number) => (
        <Tag color={count > 0 ? "orange" : "green"}>{count}</Tag>
      ),
    },
    {
      title: "耗时(ms)",
      dataIndex: "duration_ms",
      key: "duration_ms",
      width: 100,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (status: string) => (
        <Tag color={status === "SUCCESS" ? "green" : "red"}>{status}</Tag>
      ),
    },
    {
      title: "执行时间",
      dataIndex: "created_time",
      key: "created_time",
      width: 180,
    },
  ];

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <div>
          <Space className={styles.eyebrow}>
            <SafetyCertificateOutlined />
            <span>知识工厂 / 规则校验</span>
          </Space>
          <Title level={2}>规则校验工作台</Title>
          <Paragraph type="secondary">
            在线试运行规则引擎，验证规则配置的准确性和覆盖率。支持病历质控、医保质控、医嘱安全等场景。
          </Paragraph>
        </div>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="规则试运行" className={styles.card}>
            <Form
              form={form}
              layout="vertical"
              onFinish={(values) => evaluateMutation.mutate(values)}
              initialValues={{
                scenario_code: "EMR_QC",
                patient_context: JSON.stringify(
                  {
                    patient_id: "P001",
                    age: 65,
                    gender: "M",
                    diagnosis: ["I21.0"],
                    medications: ["阿司匹林", "氯吡格雷"],
                    lab_results: {
                      troponin: 0.5,
                      creatinine: 1.2,
                    },
                  },
                  null,
                  2,
                ),
              }}
            >
              <Form.Item
                name="scenario_code"
                label="评估场景"
                rules={[{ required: true, message: "请选择评估场景" }]}
              >
                <Select placeholder="选择规则场景">
                  {SCENARIO_OPTIONS.map((option) => (
                    <Option key={option.value} value={option.value}>
                      {option.label}
                    </Option>
                  ))}
                </Select>
              </Form.Item>

              <Form.Item
                name="patient_context"
                label="患者上下文 (JSON)"
                rules={[
                  { required: true, message: "请输入患者上下文" },
                  {
                    validator: (_, value) => {
                      try {
                        JSON.parse(value);
                        return Promise.resolve();
                      } catch {
                        return Promise.reject(new Error("JSON 格式错误"));
                      }
                    },
                  },
                ]}
              >
                <TextArea
                  rows={10}
                  placeholder='{"patient_id": "P001", "age": 65, ...}'
                  className={styles.jsonInput}
                />
              </Form.Item>

              <Form.Item>
                <Space>
                  <Button
                    type="primary"
                    htmlType="submit"
                    icon={<PlayCircleOutlined />}
                    loading={evaluateMutation.isPending}
                  >
                    运行评估
                  </Button>
                  <Button
                    icon={<ReloadOutlined />}
                    onClick={() => {
                      form.resetFields();
                      setEvaluateResult(null);
                    }}
                  >
                    重置
                  </Button>
                </Space>
              </Form.Item>
            </Form>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title="评估结果" className={styles.card}>
            {evaluateMutation.isPending ? (
              <div className={styles.loading}>
                <Spin size="large" />
                <Text type="secondary">正在执行规则评估...</Text>
              </div>
            ) : evaluateResult ? (
              <div className={styles.resultSection}>
                <Alert
                  type={evaluateResult.hits && evaluateResult.hits.length > 0 ? "warning" : "success"}
                  showIcon
                  className={styles.resultAlert}
                  message={
                    evaluateResult.hits && evaluateResult.hits.length > 0
                      ? `命中 ${evaluateResult.hits.length} 条规则`
                      : "未命中任何规则"
                  }
                  description={
                    evaluateResult.hits && evaluateResult.hits.length > 0
                      ? "以下规则被触发，请根据建议操作进行处理"
                      : "当前患者上下文未触发任何规则，数据符合预期"
                  }
                />

                {evaluateResult.hits && evaluateResult.hits.length > 0 && (
                  <Table
                    columns={hitColumns}
                    dataSource={evaluateResult.hits}
                    rowKey="rule_code"
                    pagination={false}
                    size="small"
                    className={styles.hitTable}
                    scroll={{ x: 800 }}
                  />
                )}

                {evaluateResult.summary && (
                  <Card size="small" title="评估摘要" className={styles.summaryCard}>
                    <Space direction="vertical" className={styles.summarySpace}>
                      <Text>
                        <Text strong>评估ID：</Text>
                        {evaluateResult.eval_id}
                      </Text>
                      <Text>
                        <Text strong>耗时：</Text>
                        {evaluateResult.duration_ms} ms
                      </Text>
                      <Text>
                        <Text strong>规则总数：</Text>
                        {evaluateResult.summary.total_rules}
                      </Text>
                      <Text>
                        <Text strong>命中率：</Text>
                        {evaluateResult.summary.hit_rate
                          ? `${(evaluateResult.summary.hit_rate * 100).toFixed(1)}%`
                          : "-"}
                      </Text>
                    </Space>
                  </Card>
                )}
              </div>
            ) : (
              <div className={styles.empty}>
                <ExperimentOutlined className={styles.emptyIcon} />
                <Text type="secondary">选择场景并输入患者上下文，点击"运行评估"查看结果</Text>
              </div>
            )}
          </Card>
        </Col>
      </Row>

      <Card
        title="执行历史"
        className={styles.card}
        extra={
          <Button
            icon={<ReloadOutlined />}
            onClick={() => logsQuery.refetch()}
            loading={logsQuery.isFetching}
          >
            刷新
          </Button>
        }
      >
        <Table
          columns={logColumns}
          dataSource={logsQuery.data ?? []}
          rowKey="eval_id"
          pagination={{ pageSize: 10 }}
          size="small"
          loading={logsQuery.isLoading}
          scroll={{ x: 800 }}
        />
      </Card>
    </div>
  );
}