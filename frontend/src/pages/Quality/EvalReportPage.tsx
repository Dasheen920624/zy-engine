import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  Card,
  Col,
  Modal,
  Row,
  Select,
  Statistic,
  Table,
  Tag,
  message,
  Form,
  Input,
  DatePicker,
  Timeline,
  Descriptions,
  Space,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import {
  listReports,
  exportReport,
  archiveReport,
  submitReview,
  listReviews,
  createRectification,
  autoCreateRectifications,
  updateRectificationStatus,
  listRectifications,
  generateReport,
  reEvaluate,
  listEvalResults,
} from "../../api/eval";
import type {
  EvalReportData,
  EvalReportExport,
  EvalReviewData,
  EvalRectificationData,
  EvalResultData,
} from "../../api/eval";
import styles from "./evalReportPage.module.css";

const { TextArea } = Input;

const REPORT_STATUS_MAP: Record<string, { color: string; label: string }> = {
  DRAFT: { color: "default", label: "草稿" },
  REVIEWED: { color: "green", label: "已复核" },
  REVIEW_REJECTED: { color: "red", label: "复核驳回" },
  CONDITIONALLY_APPROVED: { color: "orange", label: "有条件通过" },
  ARCHIVED: { color: "blue", label: "已归档" },
};

const RISK_LEVEL_MAP: Record<string, { color: string; label: string }> = {
  CRITICAL: { color: "red", label: "严重" },
  HIGH: { color: "orange", label: "高" },
  MEDIUM: { color: "gold", label: "中" },
  LOW: { color: "blue", label: "低" },
  INFO: { color: "green", label: "信息" },
};

const RECT_STATUS_MAP: Record<string, { color: string; label: string }> = {
  PENDING: { color: "default", label: "待处理" },
  IN_PROGRESS: { color: "processing", label: "进行中" },
  COMPLETED: { color: "success", label: "已完成" },
};

const PRIORITY_MAP: Record<string, { color: string; label: string }> = {
  HIGH: { color: "red", label: "高" },
  MEDIUM: { color: "orange", label: "中" },
  LOW: { color: "blue", label: "低" },
};

function getScoreColor(percentage: number): string {
  if (percentage < 60) return "red";
  if (percentage < 80) return "orange";
  return "green";
}

function getReviewColor(result: string): string {
  if (result === "APPROVED") return "green";
  if (result === "REJECTED") return "red";
  return "orange";
}

function getReviewLabel(result: string): string {
  if (result === "APPROVED") return "通过";
  if (result === "REJECTED") return "驳回";
  return "有条件通过";
}

const EvalReportPage: React.FC = () => {
  const [reports, setReports] = useState<EvalReportData[]>([]);
  const [rectifications, setRectifications] = useState<EvalRectificationData[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string | undefined>();

  // 弹窗状态
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [reviewModalVisible, setReviewModalVisible] = useState(false);
  const [rectModalVisible, setRectModalVisible] = useState(false);
  const [generateModalVisible, setGenerateModalVisible] = useState(false);
  const [reEvalModalVisible, setReEvalModalVisible] = useState(false);

  const [currentReport, setCurrentReport] = useState<EvalReportExport | null>(null);
  const [currentReviews, setCurrentReviews] = useState<EvalReviewData[]>([]);
  const [evalResults, setEvalResults] = useState<EvalResultData[]>([]);
  const [selectedEvalId, setSelectedEvalId] = useState<string>("");

  const [reviewForm] = Form.useForm();
  const [rectForm] = Form.useForm();
  const [reEvalForm] = Form.useForm();

  const fetchReports = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listReports({ status: statusFilter });
      setReports(data);
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  const fetchRectifications = useCallback(async () => {
    try {
      const data = await listRectifications();
      setRectifications(data);
    } catch {
      // ignore
    }
  }, []);

  useEffect(() => {
    fetchReports();
    fetchRectifications();
  }, [fetchReports, fetchRectifications]);

  // 查看报告详情
  const handleViewDetail = async (reportId: string) => {
    try {
      const data = await exportReport(reportId);
      setCurrentReport(data);
      const reviews = await listReviews(reportId);
      setCurrentReviews(reviews);
      setDetailModalVisible(true);
    } catch {
      message.error("获取报告详情失败");
    }
  };

  // 提交复核
  const handleSubmitReview = async (values: {
    reviewer_id: string;
    reviewer_name: string;
    review_result: "APPROVED" | "REJECTED" | "CONDITIONALLY_APPROVED";
    review_comment: string;
  }) => {
    if (!currentReport) return;
    try {
      await submitReview(currentReport.report_id, values);
      message.success("复核提交成功");
      setReviewModalVisible(false);
      reviewForm.resetFields();
      fetchReports();
      // 刷新详情
      const data = await exportReport(currentReport.report_id);
      setCurrentReport(data);
      const reviews = await listReviews(currentReport.report_id);
      setCurrentReviews(reviews);
    } catch {
      message.error("复核提交失败");
    }
  };

  // 归档报告
  const handleArchive = async (reportId: string) => {
    try {
      await archiveReport(reportId);
      message.success("归档成功");
      fetchReports();
    } catch {
      message.error("归档失败");
    }
  };

  // 自动生成整改任务
  const handleAutoRect = async (reportId: string) => {
    try {
      const count = await autoCreateRectifications(reportId);
      message.success(`已自动生成 ${count.length} 项整改任务`);
      fetchRectifications();
    } catch {
      message.error("自动生成整改任务失败");
    }
  };

  // 创建整改任务
  const handleCreateRect = async (values: {
    title: string;
    description?: string;
    assignee_name?: string;
    priority?: string;
    due_date?: string;
  }) => {
    if (!currentReport) return;
    try {
      await createRectification(currentReport.report_id, values);
      message.success("整改任务创建成功");
      setRectModalVisible(false);
      rectForm.resetFields();
      fetchRectifications();
    } catch {
      message.error("创建整改任务失败");
    }
  };

  // 更新整改任务状态
  const handleUpdateRectStatus = async (
    rectId: string,
    newStatus: "IN_PROGRESS" | "COMPLETED",
  ) => {
    try {
      await updateRectificationStatus(rectId, {
        status: newStatus,
        updated_by: "current-user",
        update_note: newStatus === "IN_PROGRESS" ? "开始处理" : "整改完成",
      });
      message.success("状态更新成功");
      fetchRectifications();
    } catch {
      message.error("状态更新失败");
    }
  };

  // 生成报告
  const handleGenerate = async () => {
    if (!selectedEvalId) {
      message.warning("请选择评估结果");
      return;
    }
    try {
      await generateReport(selectedEvalId);
      message.success("报告生成成功");
      setGenerateModalVisible(false);
      setSelectedEvalId("");
      fetchReports();
    } catch {
      message.error("报告生成失败");
    }
  };

  // 再评估
  const handleReEvaluate = async (values: { input_data: string }) => {
    if (!currentReport) return;
    try {
      const inputData = JSON.parse(values.input_data);
      await reEvaluate(currentReport.eval_id, inputData);
      message.success("再评估执行成功");
      setReEvalModalVisible(false);
      reEvalForm.resetFields();
    } catch {
      message.error("再评估失败，请检查输入数据格式");
    }
  };

  // 打开生成报告弹窗时加载评估结果列表
  const openGenerateModal = async () => {
    try {
      const data = await listEvalResults();
      setEvalResults(data);
      setGenerateModalVisible(true);
    } catch {
      message.error("获取评估结果列表失败");
    }
  };

  // 报告列表列定义
  const reportColumns: ColumnsType<EvalReportData> = [
    {
      title: "报告编号",
      dataIndex: "report_id",
      key: "report_id",
      width: 150,
    },
    {
      title: "评估对象",
      dataIndex: "subject_name",
      key: "subject_name",
      width: 150,
      render: (text: string, record: EvalReportData) => text || record.subject_id,
    },
    {
      title: "得分",
      dataIndex: "score_percentage",
      key: "score_percentage",
      width: 100,
      render: (v: number) => {
        const colorClass = v < 60 ? styles.scoreRed : v < 80 ? styles.scoreOrange : styles.scoreGreen;
        return <span className={colorClass}>{v}%</span>;
      },
    },
    {
      title: "风险等级",
      dataIndex: "risk_level",
      key: "risk_level",
      width: 100,
      render: (v: string) => {
        const info = RISK_LEVEL_MAP[v] || { color: "default", label: v };
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: "异常/缺失",
      key: "facts",
      width: 100,
      render: (_: unknown, record: EvalReportData) =>
        `${record.abnormal_fact_count}/${record.missing_fact_count}`,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 120,
      render: (v: string) => {
        const info = REPORT_STATUS_MAP[v] || { color: "default", label: v };
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: "生成时间",
      dataIndex: "generated_at",
      key: "generated_at",
      width: 180,
      render: (v: string) => (v ? v.substring(0, 19) : "-"),
    },
    {
      title: "操作",
      key: "action",
      width: 300,
      render: (_: unknown, record: EvalReportData) => (
        <Space size="small">
          <Button size="small" onClick={() => handleViewDetail(record.report_id)}>
            详情
          </Button>
          {record.status === "DRAFT" && (
            <Button size="small" type="primary" onClick={() => { setCurrentReport(null); handleViewDetail(record.report_id).then(() => setReviewModalVisible(true)); }}>
              复核
            </Button>
          )}
          {record.status === "REVIEWED" && (
            <Button size="small" onClick={() => handleArchive(record.report_id)}>
              归档
            </Button>
          )}
          {(record.status === "DRAFT" || record.status === "REVIEW_REJECTED" || record.status === "CONDITIONALLY_APPROVED") && (
            <Button size="small" onClick={() => handleAutoRect(record.report_id)}>
              自动整改
            </Button>
          )}
        </Space>
      ),
    },
  ];

  // 整改任务列定义
  const rectColumns: ColumnsType<EvalRectificationData> = [
    {
      title: "任务编号",
      dataIndex: "rect_id",
      key: "rect_id",
      width: 150,
    },
    {
      title: "标题",
      dataIndex: "title",
      key: "title",
      width: 200,
    },
    {
      title: "负责人",
      dataIndex: "assignee_name",
      key: "assignee_name",
      width: 100,
      render: (v: string) => v || "-",
    },
    {
      title: "优先级",
      dataIndex: "priority",
      key: "priority",
      width: 80,
      render: (v: string) => {
        const info = PRIORITY_MAP[v] || { color: "default", label: v };
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (v: string) => {
        const info = RECT_STATUS_MAP[v] || { color: "default", label: v };
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: "截止日期",
      dataIndex: "due_date",
      key: "due_date",
      width: 120,
      render: (v: string) => v || "-",
    },
    {
      title: "操作",
      key: "action",
      width: 150,
      render: (_: unknown, record: EvalRectificationData) => (
        <Space size="small">
          {record.status === "PENDING" && (
            <Button size="small" type="primary" onClick={() => handleUpdateRectStatus(record.rect_id, "IN_PROGRESS")}>
              开始
            </Button>
          )}
          {record.status === "IN_PROGRESS" && (
            <Button size="small" onClick={() => handleUpdateRectStatus(record.rect_id, "COMPLETED")}>
              完成
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.page}>
      {/* 报告列表 */}
      <Card
        title="评估报告"
        extra={
          <Space>
            <Select
              placeholder="按状态筛选"
              allowClear
              style={{ width: 150 }}
              value={statusFilter}
              onChange={(v) => setStatusFilter(v)}
            >
              {Object.entries(REPORT_STATUS_MAP).map(([key, { label }]) => (
                <Select.Option key={key} value={key}>
                  {label}
                </Select.Option>
              ))}
            </Select>
            <Button type="primary" onClick={openGenerateModal}>
              生成报告
            </Button>
          </Space>
        }
      >
        <Table
          rowKey="report_id"
          columns={reportColumns}
          dataSource={reports}
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      {/* 整改任务 */}
      <Card title="整改任务" style={{ marginTop: 16 }}>
        <Table
          rowKey="rect_id"
          columns={rectColumns}
          dataSource={rectifications}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      {/* 报告详情弹窗 */}
      <Modal
        title="评估报告详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        width={800}
        footer={[
          <Button key="review" type="primary" onClick={() => setReviewModalVisible(true)}>
            提交复核
          </Button>,
          <Button key="rect" onClick={() => setRectModalVisible(true)}>
            创建整改任务
          </Button>,
          <Button key="reEval" onClick={() => setReEvalModalVisible(true)}>
            再评估
          </Button>,
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>,
        ]}
      >
        {currentReport && (
          <>
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={6}>
                <Statistic
                  title="得分"
                  value={currentReport.score_percentage}
                  suffix="%"
                  valueStyle={{
                    color: getScoreColor(currentReport.score_percentage),
                  }}
                />
              </Col>
              <Col span={6}>
                <Statistic title="风险等级" value={RISK_LEVEL_MAP[currentReport.risk_level]?.label || currentReport.risk_level} />
              </Col>
              <Col span={6}>
                <Statistic title="异常事实" value={currentReport.abnormal_fact_count} />
              </Col>
              <Col span={6}>
                <Statistic title="缺失事实" value={currentReport.missing_fact_count} />
              </Col>
            </Row>

            <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
              <Descriptions.Item label="报告编号">{currentReport.report_id}</Descriptions.Item>
              <Descriptions.Item label="评估编号">{currentReport.eval_id}</Descriptions.Item>
              <Descriptions.Item label="评估对象">{currentReport.subject_name || currentReport.subject_id}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={REPORT_STATUS_MAP[currentReport.status]?.color}>
                  {REPORT_STATUS_MAP[currentReport.status]?.label || currentReport.status}
                </Tag>
              </Descriptions.Item>
            </Descriptions>

            {currentReport.recommendations && currentReport.recommendations.length > 0 && (
              <Card size="small" title="整改建议" style={{ marginBottom: 16 }}>
                <ul className={styles.recommendationList}>
                  {currentReport.recommendations.map((r, i) => (
                    <li key={i}>{r}</li>
                  ))}
                </ul>
              </Card>
            )}

            {currentReport.indicator_scores && currentReport.indicator_scores.length > 0 && (
              <Card size="small" title="指标得分" style={{ marginBottom: 16 }}>
                <Table
                  size="small"
                  pagination={false}
                  dataSource={currentReport.indicator_scores}
                  rowKey="indicator_code"
                  columns={[
                    { title: "指标", dataIndex: "indicator_name", width: 150 },
                    { title: "原始分", dataIndex: "raw_score", width: 80 },
                    { title: "加权分", dataIndex: "weighted_score", width: 80 },
                    { title: "满分", dataIndex: "max_value", width: 80 },
                    {
                      title: "达标",
                      dataIndex: "threshold_met",
                      width: 60,
                      render: (v: boolean) => (v ? <Tag color="green">是</Tag> : <Tag color="red">否</Tag>),
                    },
                    {
                      title: "风险",
                      dataIndex: "risk_level",
                      width: 80,
                      render: (v: string) => {
                        const info = RISK_LEVEL_MAP[v] || { color: "default", label: v };
                        return <Tag color={info.color}>{info.label}</Tag>;
                      },
                    },
                  ]}
                />
              </Card>
            )}

            {currentReviews.length > 0 && (
              <Card size="small" title="复核记录">
                <Timeline
                  items={currentReviews.map((r) => {
                    const reviewColor = getReviewColor(r.review_result);
                    const reviewLabel = getReviewLabel(r.review_result);
                    return {
                      color: reviewColor,
                      children: (
                        <div>
                          <strong>{r.reviewer_name}</strong>
                          {" - "}
                          <Tag color={reviewColor}>{reviewLabel}</Tag>
                          {r.review_comment && <div className={styles.reviewComment}>{r.review_comment}</div>}
                          <div className={styles.reviewTime}>{r.reviewed_at?.substring(0, 19)}</div>
                        </div>
                      ),
                    };
                  })}
                />
              </Card>
            )}
          </>
        )}
      </Modal>

      {/* 复核弹窗 */}
      <Modal
        title="提交复核意见"
        open={reviewModalVisible}
        onCancel={() => setReviewModalVisible(false)}
        onOk={() => reviewForm.submit()}
      >
        <Form form={reviewForm} onFinish={handleSubmitReview} layout="vertical">
          <Form.Item name="reviewer_id" label="复核人ID" rules={[{ required: true, message: "请输入" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="reviewer_name" label="复核人姓名" rules={[{ required: true, message: "请输入" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="review_result" label="复核结果" rules={[{ required: true, message: "请选择" }]}>
            <Select>
              <Select.Option value="APPROVED">通过</Select.Option>
              <Select.Option value="CONDITIONALLY_APPROVED">有条件通过</Select.Option>
              <Select.Option value="REJECTED">驳回</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="review_comment" label="复核意见">
            <TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 创建整改任务弹窗 */}
      <Modal
        title="创建整改任务"
        open={rectModalVisible}
        onCancel={() => setRectModalVisible(false)}
        onOk={() => rectForm.submit()}
      >
        <Form form={rectForm} onFinish={handleCreateRect} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true, message: "请输入" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} />
          </Form.Item>
          <Form.Item name="assignee_name" label="负责人">
            <Input />
          </Form.Item>
          <Form.Item name="priority" label="优先级" initialValue="MEDIUM">
            <Select>
              <Select.Option value="HIGH">高</Select.Option>
              <Select.Option value="MEDIUM">中</Select.Option>
              <Select.Option value="LOW">低</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="due_date" label="截止日期">
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 生成报告弹窗 */}
      <Modal
        title="生成评估报告"
        open={generateModalVisible}
        onCancel={() => setGenerateModalVisible(false)}
        onOk={handleGenerate}
      >
        <div className={styles.generateHint}>选择评估结果：</div>
        <Select
          style={{ width: "100%" }}
          placeholder="请选择评估结果"
          value={selectedEvalId || undefined}
          onChange={setSelectedEvalId}
        >
          {evalResults.map((r) => (
            <Select.Option key={r.eval_id} value={r.eval_id}>
              {r.eval_id} - {r.subject_name || r.subject_id} ({RISK_LEVEL_MAP[r.risk_level]?.label || r.risk_level})
            </Select.Option>
          ))}
        </Select>
      </Modal>

      {/* 再评估弹窗 */}
      <Modal
        title="再评估"
        open={reEvalModalVisible}
        onCancel={() => setReEvalModalVisible(false)}
        onOk={() => reEvalForm.submit()}
      >
        <Form form={reEvalForm} onFinish={handleReEvaluate} layout="vertical">
          <Form.Item
            name="input_data"
            label="输入数据（JSON 格式）"
            rules={[{ required: true, message: "请输入" }]}
          >
            <TextArea rows={6} placeholder='{"INDICATOR-001": 85, "INDICATOR-002": 70}' />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default EvalReportPage;
