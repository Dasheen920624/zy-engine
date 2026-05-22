import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  message,
} from "antd";
import {
  CheckCircleOutlined,
  WarningOutlined,
  CloseCircleOutlined,
} from "@ant-design/icons";
import {
  listEvalResults,
  executeEvaluation,
  listEvalSets,
  type EvalResultData,
  type EvalIndicatorSet,
  type IndicatorScore,
  type EvalFact,
} from "../../api/eval";
import styles from "./evalResultList.module.css";

const RISK_LEVEL_CONFIG: Record<string, { color: string; label: string }> = {
  CRITICAL: { color: "red", label: "危急" },
  HIGH: { color: "orange", label: "高风险" },
  MEDIUM: { color: "gold", label: "中风险" },
  LOW: { color: "blue", label: "低风险" },
  INFO: { color: "green", label: "信息" },
};

function getRiskColor(level: string): string {
  if (level === "CRITICAL" || level === "HIGH") return "var(--mk-danger)";
  if (level === "MEDIUM") return "var(--mk-warning)";
  return "var(--mk-success)";
}

const EvalResultList: React.FC = () => {
  const [results, setResults] = useState<EvalResultData[]>([]);
  const [sets, setSets] = useState<EvalIndicatorSet[]>([]);
  const [loading, setLoading] = useState(false);
  const [evalModalVisible, setEvalModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [selectedResult, setSelectedResult] = useState<EvalResultData | null>(null);
  const [evalForm] = Form.useForm();

  const fetchResults = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listEvalResults();
      setResults(data);
    } catch {
      message.error("获取评估结果失败");
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchSets = useCallback(async () => {
    try {
      const data = await listEvalSets({ status: "PUBLISHED" });
      setSets(data);
    } catch {
      // ignore
    }
  }, []);

  useEffect(() => {
    fetchResults();
    fetchSets();
  }, [fetchResults, fetchSets]);

  const handleEvaluate = async (values: Record<string, unknown>) => {
    try {
      const inputStr = values.input_data as string;
      const inputData: Record<string, unknown> = {};
      if (inputStr) {
        try {
          Object.assign(inputData, JSON.parse(inputStr));
        } catch {
          message.error("输入数据格式错误，请使用 JSON 格式");
          return;
        }
      }
      await executeEvaluation({
        set_code: values.set_code as string,
        subject_id: values.subject_id as string,
        subject_name: values.subject_name as string,
        input_data: inputData,
      });
      message.success("评估执行成功");
      setEvalModalVisible(false);
      evalForm.resetFields();
      fetchResults();
    } catch {
      message.error("评估执行失败");
    }
  };

  const columns = [
    { title: "评估ID", dataIndex: "eval_id", key: "eval_id", width: 130 },
    { title: "指标集", dataIndex: "set_code", key: "set_code", width: 130 },
    {
      title: "评估对象",
      key: "subject",
      width: 160,
      render: (_: unknown, record: EvalResultData) => record.subject_name || record.subject_id,
    },
    {
      title: "得分",
      key: "score",
      width: 120,
      render: (_: unknown, record: EvalResultData) => (
        <Space>
          <span>{record.score_percentage}%</span>
          <span className={styles.scoreDetail}>
            ({record.total_score}/{record.max_possible_score})
          </span>
        </Space>
      ),
    },
    {
      title: "风险等级",
      dataIndex: "risk_level",
      key: "risk_level",
      width: 100,
      render: (v: string) => {
        const cfg = RISK_LEVEL_CONFIG[v] ?? { color: "default", label: v };
        return <Tag color={cfg.color}>{cfg.label}</Tag>;
      },
    },
    {
      title: "异常",
      key: "abnormal",
      width: 80,
      render: (_: unknown, record: EvalResultData) => (
        <Tag color={record.abnormal_facts?.length > 0 ? "orange" : "green"}>
          {record.abnormal_facts?.length ?? 0}
        </Tag>
      ),
    },
    {
      title: "缺失",
      key: "missing",
      width: 80,
      render: (_: unknown, record: EvalResultData) => (
        <Tag color={record.missing_facts?.length > 0 ? "red" : "green"}>
          {record.missing_facts?.length ?? 0}
        </Tag>
      ),
    },
    { title: "评估时间", dataIndex: "evaluated_at", key: "evaluated_at", width: 160 },
    {
      title: "操作",
      key: "action",
      width: 80,
      render: (_: unknown, record: EvalResultData) => (
        <Button size="small" onClick={() => { setSelectedResult(record); setDetailModalVisible(true); }}>
          详情
        </Button>
      ),
    },
  ];

  const scoreColumns = [
    { title: "指标", dataIndex: "indicator_name", key: "indicator_name", width: 160 },
    {
      title: "原始得分",
      key: "raw",
      width: 100,
      render: (_: unknown, record: IndicatorScore) => `${record.raw_score}${record.unit ?? ""}`,
    },
    { title: "权重", dataIndex: "weight", key: "weight", width: 60 },
    {
      title: "加权得分",
      dataIndex: "weighted_score",
      key: "weighted_score",
      width: 100,
      render: (v: number) => Math.round(v * 100) / 100,
    },
    {
      title: "达标",
      key: "threshold",
      width: 60,
      render: (_: unknown, record: IndicatorScore) =>
        record.threshold_met ? (
          <CheckCircleOutlined className={styles.iconSuccess} />
        ) : (
          <CloseCircleOutlined className={styles.iconDanger} />
        ),
    },
    {
      title: "风险",
      dataIndex: "risk_level",
      key: "risk_level",
      width: 80,
      render: (v: string) => {
        const cfg = RISK_LEVEL_CONFIG[v] ?? { color: "default", label: v };
        return <Tag color={cfg.color}>{cfg.label}</Tag>;
      },
    },
    { title: "说明", dataIndex: "explanation", key: "explanation" },
  ];

  return (
    <div className={styles.page}>
      <Card
        title="评估结果"
        extra={
          <Button type="primary" onClick={() => setEvalModalVisible(true)}>
            执行评估
          </Button>
        }
      >
        <Table
          rowKey="eval_id"
          columns={columns}
          dataSource={results}
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      {/* 执行评估弹窗 */}
      <Modal
        title="执行评估"
        open={evalModalVisible}
        onCancel={() => { setEvalModalVisible(false); evalForm.resetFields(); }}
        onOk={() => evalForm.submit()}
        width={600}
      >
        <Form form={evalForm} layout="vertical" onFinish={handleEvaluate}>
          <Form.Item name="set_code" label="指标集" rules={[{ required: true, message: "请选择指标集" }]}>
            <Select placeholder="请选择已发布的指标集">
              {sets.map((s) => (
                <Select.Option key={s.set_code} value={s.set_code}>
                  {s.set_name} ({s.set_code})
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="subject_id" label="评估对象ID" rules={[{ required: true }]}>
                <Input placeholder="如：ENC-001" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="subject_name" label="评估对象名称">
                <Input placeholder="如：患者张三" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="input_data"
            label="输入数据（JSON）"
            rules={[{ required: true, message: "请输入评估数据" }]}
          >
            <Input.TextArea
              rows={6}
              placeholder='{"EVAL-IND-0001": 85, "EVAL-IND-0002": 72}'
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 评估结果详情弹窗 */}
      <Modal
        title={selectedResult ? `评估结果 - ${selectedResult.eval_id}` : "评估结果"}
        open={detailModalVisible}
        onCancel={() => { setDetailModalVisible(false); setSelectedResult(null); }}
        footer={null}
        width={900}
      >
        {selectedResult && (
          <>
            <Row gutter={16} className={styles.detailStatsRow}>
              <Col span={6}>
                <Statistic title="得分百分比" value={selectedResult.score_percentage} suffix="%" />
              </Col>
              <Col span={6}>
                <Statistic
                  title="风险等级"
                  value={RISK_LEVEL_CONFIG[selectedResult.risk_level]?.label ?? selectedResult.risk_level}
                  valueStyle={{
                    color: getRiskColor(selectedResult.risk_level),
                  }}
                />
              </Col>
              <Col span={6}>
                <Statistic title="异常事实" value={selectedResult.abnormal_facts?.length ?? 0} />
              </Col>
              <Col span={6}>
                <Statistic title="缺失事实" value={selectedResult.missing_facts?.length ?? 0} />
              </Col>
            </Row>

            <Card type="inner" title="各指标得分" size="small" className={styles.innerCardSpacing}>
              <Table
                rowKey="indicator_code"
                columns={scoreColumns}
                dataSource={selectedResult.indicator_scores ?? []}
                pagination={false}
                size="small"
              />
            </Card>

            {selectedResult.abnormal_facts?.length > 0 && (
              <Card type="inner" title="异常事实" size="small" className={styles.innerCardSpacing}>
                {selectedResult.abnormal_facts.map((f: EvalFact, i: number) => (
                  <div key={i} className={styles.factItem}>
                    <Tag color="orange"><WarningOutlined /> {f.severity}</Tag>
                    <span>{f.indicator_name}: {f.description}</span>
                  </div>
                ))}
              </Card>
            )}

            {selectedResult.missing_facts?.length > 0 && (
              <Card type="inner" title="缺失事实" size="small">
                {selectedResult.missing_facts.map((f: EvalFact, i: number) => (
                  <div key={i} className={styles.factItem}>
                    <Tag color="red"><CloseCircleOutlined /> {f.severity}</Tag>
                    <span>{f.indicator_name}: {f.description}</span>
                  </div>
                ))}
              </Card>
            )}
          </>
        )}
      </Modal>
    </div>
  );
};

export default EvalResultList;
