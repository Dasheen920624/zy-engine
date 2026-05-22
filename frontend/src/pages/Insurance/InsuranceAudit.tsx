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
  FileSearchOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
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
import styles from "./InsuranceAudit.module.css";

const { Title, Paragraph, Text } = Typography;
const { TextArea } = Input;
const { Option } = Select;

const AUDIT_TYPE_OPTIONS = [
  { label: "药品适应症审核", value: "DRUG_INDICATION" },
  { label: "检查合理性审核", value: "EXAM_RATIONALITY" },
  { label: "医保合规审核", value: "INSURANCE_QC" },
];

const SEVERITY_COLORS: Record<string, string> = {
  CRITICAL: "red",
  HIGH: "orange",
  MEDIUM: "gold",
  LOW: "green",
  INFO: "blue",
};

const ACTION_MODE_LABELS: Record<string, { color: string; label: string }> = {
  NOTICE: { color: "blue", label: "通知" },
  SOFT: { color: "gold", label: "软提醒" },
  BLOCK: { color: "red", label: "阻断" },
};

/**
 * 医保智能审核（PR-V2-12）。
 *
 * 功能：
 *  - 药品适应症审核
 *  - 检查合理性审核
 *  - 医保合规审核
 *  - 查看审核历史
 *
 * 后端端点：
 *  - POST /api/rule-engine/evaluate  场景化评估
 *  - GET  /api/rules/exec-logs       执行历史
 */
export default function InsuranceAudit() {
  const [form] = Form.useForm();
  const [auditResult, setAuditResult] = useState<EvaluateResponse | null>(null);
  const tenant_id = getOrgContext().tenant_id || "TENANT_DEMO";

  const auditMutation = useMutation({
    mutationFn: async (values: {
      scenario_code: string;
      patient_context: string;
    }) => {
      let parsed: Record<string, unknown>;
      try {
        parsed = JSON.parse(values.patient_context);
      } catch {
        throw new Error("患者上下文 JSON 格式错误");
      }

      // 用户从 JSON 编辑器输入，runtime 校验后转入契约结构（业务侧需保证模板对齐）。
      const request: EvaluateRequest = {
        scenario_code: values.scenario_code as EvaluateRequest["scenario_code"],
        patient_context: parsed as EvaluateRequest["patient_context"],
        tenant_id,
      };

      return evaluateRuleEngine(request);
    },
    onSuccess: (data) => {
      setAuditResult(data);
      message.success(`审核完成，命中 ${data.hits?.length ?? 0} 条规则`);
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : "审核失败");
    },
  });

  const logsQuery = useQuery({
    queryKey: ["insurance", "audit-logs", tenant_id],
    queryFn: () =>
      listRuleExecLogs({
        tenant_id,
        scenario_code: "INSURANCE_QC",
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
        const info = ACTION_MODE_LABELS[mode] || { color: "default", label: mode };
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
            <FileSearchOutlined />
            <span>质控驾驶舱 / 医保审核</span>
          </Space>
          <Title level={2}>医保智能审核</Title>
          <Paragraph type="secondary">
            基于规则引擎的医保智能审核，支持药品适应症、检查合理性、医保合规等场景的自动化审核。
          </Paragraph>
        </div>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="审核配置" className={styles.card}>
            <Form
              form={form}
              layout="vertical"
              onFinish={(values) => auditMutation.mutate(values)}
              initialValues={{
                scenario_code: "INSURANCE_QC",
                patient_context: JSON.stringify(
                  {
                    patient_id: "P001",
                    age: 65,
                    gender: "M",
                    diagnosis: ["I21.0"],
                    medications: ["阿司匹林", "氯吡格雷"],
                    procedures: ["冠状动脉造影"],
                    insurance_type: "职工医保",
                    total_cost: 15000,
                    drug_cost: 8000,
                    exam_cost: 4000,
                  },
                  null,
                  2,
                ),
              }}
            >
              <Form.Item
                name="scenario_code"
                label="审核类型"
                rules={[{ required: true, message: "请选择审核类型" }]}
              >
                <Select placeholder="选择审核类型">
                  {AUDIT_TYPE_OPTIONS.map((option) => (
                    <Option key={option.value} value={option.value}>
                      {option.label}
                    </Option>
                  ))}
                </Select>
              </Form.Item>

              <Form.Item
                name="patient_context"
                label="患者上下文"
                extra="请按模板填写患者信息（JSON 格式），或展开专家模式直接编辑"
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
                  rows={12}
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
                    loading={auditMutation.isPending}
                  >
                    开始审核
                  </Button>
                  <Button
                    icon={<ReloadOutlined />}
                    onClick={() => {
                      form.resetFields();
                      setAuditResult(null);
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
          <Card title="审核结果" className={styles.card}>
            {auditMutation.isPending ? (
              <div className={styles.loading}>
                <Spin size="large" />
                <Text type="secondary">正在执行医保审核...</Text>
              </div>
            ) : auditResult ? (
              <div className={styles.resultSection}>
                <Alert
                  type={auditResult.hits && auditResult.hits.length > 0 ? "warning" : "success"}
                  showIcon
                  className={styles.resultAlert}
                  message={
                    auditResult.hits && auditResult.hits.length > 0
                      ? `命中 ${auditResult.hits.length} 条规则`
                      : "未命中任何规则"
                  }
                  description={
                    auditResult.hits && auditResult.hits.length > 0
                      ? "以下规则被触发，请根据建议操作进行处理"
                      : "当前患者数据未触发任何医保规则，符合预期"
                  }
                />

                {auditResult.hits && auditResult.hits.length > 0 && (
                  <Table
                    columns={hitColumns}
                    dataSource={auditResult.hits}
                    rowKey="rule_code"
                    pagination={false}
                    size="small"
                    className={styles.hitTable}
                    scroll={{ x: 800 }}
                  />
                )}

                {auditResult.result_id && (
                  <Card size="small" title="审核摘要" className={styles.summaryCard}>
                    <Space direction="vertical" className={styles.summarySpace}>
                      <Text>
                        <Text strong>耗时：</Text>
                        {auditResult.elapsed_ms} ms
                      </Text>
                      <Text>
                        <Text strong>规则总数：</Text>
                        {auditResult.evaluated_count}
                      </Text>
                      <Text>
                        <Text strong>命中率：</Text>
                        {auditResult.evaluated_count
                          ? `${((auditResult.hit_count / auditResult.evaluated_count) * 100).toFixed(1)}%`
                          : "-"}
                      </Text>
                    </Space>
                  </Card>
                )}
              </div>
            ) : (
              <div className={styles.empty}>
                <ExperimentOutlined className={styles.emptyIcon} />
                <Text type="secondary">选择审核类型并输入患者上下文，点击"开始审核"查看结果</Text>
              </div>
            )}
          </Card>
        </Col>
      </Row>

      <Card
        title="审核历史"
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
