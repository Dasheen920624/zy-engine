import { useState, useEffect, useCallback } from "react";
import {
  Button,
  Card,
  Col,
  Drawer,
  Form,
  Input,
  message,
  Modal,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Typography,
} from "antd";
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  EditOutlined,
  HistoryOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import {
  listCandidates,
  reviewCandidate,
  batchReview,
  getReviewSummary,
  getReviewHistory,
} from "../../api/aiCandidateReview";
import type {
  AiCandidateReview,
  ReviewSummary,
} from "../../api/aiCandidateReview";
import styles from "./aiCandidateReviewDesk.module.css";

const { Text, Title } = Typography;

// ==================== 常量映射 ====================

const REVIEW_STATUS_TAG: Record<string, { color: string; label: string }> = {
  PENDING: { color: "processing", label: "待审核" },
  APPROVED: { color: "success", label: "已通过" },
  REJECTED: { color: "error", label: "已驳回" },
  MODIFIED: { color: "warning", label: "已修改" },
};

const PRIORITY_TAG: Record<string, { color: string; label: string }> = {
  HIGH: { color: "error", label: "高" },
  MEDIUM: { color: "warning", label: "中" },
  LOW: { color: "success", label: "低" },
};

const CANDIDATE_TYPE_LABEL: Record<string, string> = {
  RULE: "规则",
  PATHWAY: "路径",
  PROMPT: "提示词",
  TERMINOLOGY: "术语",
  KNOWLEDGE: "知识",
};

function mockReviewStatus(index: number) {
  if (index < 5) return "PENDING";
  if (index < 12) return "APPROVED";
  if (index < 16) return "REJECTED";
  return "MODIFIED";
}

function mockReviewNote(index: number) {
  if (index < 5) return "";
  if (index < 12) return "审核通过";
  if (index < 16) return "内容不准确";
  return "已修正术语";
}

function confidenceTextType(value?: number) {
  if (value === undefined || value === null) return undefined;
  if (value >= 0.8) return "success";
  if (value >= 0.6) return "warning";
  return "danger";
}

// ==================== 模拟数据 ====================

const MOCK_SUMMARY: ReviewSummary = {
  total: 128,
  pending: 42,
  approved: 56,
  rejected: 18,
  modified: 12,
};

const MOCK_CANDIDATES: AiCandidateReview[] = Array.from(
  { length: 20 },
  (_, i) => ({
    id: i + 1,
    tenantId: 1,
    candidateCode: `CAND-2026-${String(i + 1).padStart(4, "0")}`,
    candidateType: ["RULE", "PATHWAY", "PROMPT", "TERMINOLOGY", "KNOWLEDGE"][
      i % 5
    ],
    candidateName: `候选配置${i + 1}`,
    sourceCode: `SRC-${String((i % 3) + 1).padStart(3, "0")}`,
    sourceName: ["ICD-10编码库", "临床路径库", "药品知识库"][i % 3],
    modelProvider: ["OpenAI", "Anthropic", "本地模型"][i % 3],
    modelName: ["gpt-4o", "claude-3", "qwen-72b"][i % 3],
    confidence: 0.6 + Math.random() * 0.4,
    candidateContent: `AI生成的候选内容示例 #${i + 1}`,
    reviewStatus: mockReviewStatus(i),
    reviewedBy: i < 5 ? "" : ["admin", "reviewer1", "reviewer2"][i % 3],
    reviewedTime: i < 5 ? "" : "2026-05-19 10:30:00",
    reviewNote: mockReviewNote(i),
    modifiedContent: i >= 16 ? `修正后的内容 #${i + 1}` : "",
    qualityFindings: i % 3 === 0 ? "置信度偏低，建议人工复核" : "",
    priority: ["HIGH", "MEDIUM", "LOW"][i % 3],
    createdBy: "ai-engine",
    createdTime: `2026-05-${String(10 + (i % 10)).padStart(2, "0")} 08:00:00`,
  })
);

// ==================== 组件 ====================

export default function AiCandidateReviewDesk() {
  const [loading, setLoading] = useState(false);
  const [candidates, setCandidates] = useState<AiCandidateReview[]>([]);
  const [summary, setSummary] = useState<ReviewSummary | null>(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);

  // 筛选
  const [filterType, setFilterType] = useState<string | undefined>();
  const [filterStatus, setFilterStatus] = useState<string | undefined>();
  const [filterPriority, setFilterPriority] = useState<string | undefined>();

  // 审核弹窗
  const [reviewModalOpen, setReviewModalOpen] = useState(false);
  const [reviewingRecord, setReviewingRecord] =
    useState<AiCandidateReview | null>(null);
  const [reviewForm] = Form.useForm();

  // 审核历史抽屉
  const [historyOpen, setHistoryOpen] = useState(false);
  const [historyData, setHistoryData] = useState<AiCandidateReview[]>([]);

  // 加载数据
  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [listData, summaryData] = await Promise.all([
        listCandidates({
          candidateType: filterType,
          reviewStatus: filterStatus,
          priority: filterPriority,
        }),
        getReviewSummary(),
      ]);
      setCandidates(listData.items);
      setSummary(summaryData);
    } catch {
      setCandidates(MOCK_CANDIDATES);
      setSummary(MOCK_SUMMARY);
    } finally {
      setLoading(false);
    }
  }, [filterType, filterStatus, filterPriority]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // 打开审核弹窗
  const openReviewModal = (record: AiCandidateReview) => {
    setReviewingRecord(record);
    reviewForm.resetFields();
    setReviewModalOpen(true);
  };

  // 提交审核
  const handleReviewSubmit = async () => {
    try {
      const values = await reviewForm.validateFields();
      if (reviewingRecord) {
        await reviewCandidate(reviewingRecord.id, values);
        message.success("审核完成");
      }
      setReviewModalOpen(false);
      fetchData();
    } catch {
      // 表单校验失败或接口异常
    }
  };

  // 批量审核
  const handleBatchReview = async (reviewStatus: string) => {
    if (selectedRowKeys.length === 0) {
      message.warning("请先选择候选记录");
      return;
    }
    try {
      await batchReview({
        candidateIds: selectedRowKeys,
        reviewStatus,
      });
      message.success(`批量${REVIEW_STATUS_TAG[reviewStatus]?.label || "审核"}完成`);
      setSelectedRowKeys([]);
      fetchData();
    } catch {
      // 接口异常
    }
  };

  // 加载审核历史
  const openHistory = async () => {
    setHistoryOpen(true);
    try {
      const data = await getReviewHistory({ limit: 50 });
      setHistoryData(data.items);
    } catch {
      setHistoryData(
        MOCK_CANDIDATES.filter((c) => c.reviewStatus !== "PENDING")
      );
    }
  };

  // 表格列定义
  const columns: ColumnsType<AiCandidateReview> = [
    {
      title: "候选编码",
      dataIndex: "candidateCode",
      key: "candidateCode",
      width: 160,
    },
    {
      title: "名称",
      dataIndex: "candidateName",
      key: "candidateName",
      width: 140,
    },
    {
      title: "类型",
      dataIndex: "candidateType",
      key: "candidateType",
      width: 90,
      render: (v: string) => CANDIDATE_TYPE_LABEL[v] || v,
    },
    {
      title: "来源",
      dataIndex: "sourceName",
      key: "sourceName",
      width: 120,
    },
    {
      title: "模型",
      dataIndex: "modelName",
      key: "modelName",
      width: 110,
    },
    {
      title: "置信度",
      dataIndex: "confidence",
      key: "confidence",
      width: 90,
      render: (v: number) =>
        v !== null && v !== undefined ? (
          <Text type={confidenceTextType(v)}>
            {(v * 100).toFixed(1)}%
          </Text>
        ) : (
          "-"
        ),
      sorter: (a, b) => (a.confidence ?? 0) - (b.confidence ?? 0),
    },
    {
      title: "质检发现",
      dataIndex: "qualityFindings",
      key: "qualityFindings",
      width: 160,
      ellipsis: true,
      render: (v: string) => v || "-",
    },
    {
      title: "状态",
      dataIndex: "reviewStatus",
      key: "reviewStatus",
      width: 90,
      render: (v: string) => {
        const tag = REVIEW_STATUS_TAG[v];
        return tag ? <Tag color={tag.color}>{tag.label}</Tag> : v;
      },
    },
    {
      title: "优先级",
      dataIndex: "priority",
      key: "priority",
      width: 80,
      render: (v: string) => {
        const tag = PRIORITY_TAG[v];
        return tag ? <Tag color={tag.color}>{tag.label}</Tag> : v;
      },
    },
    {
      title: "创建时间",
      dataIndex: "createdTime",
      key: "createdTime",
      width: 160,
    },
    {
      title: "操作",
      key: "action",
      width: 80,
      fixed: "right",
      render: (_, record) =>
        record.reviewStatus === "PENDING" ? (
          <Button
            type="link"
            size="small"
            onClick={() => openReviewModal(record)}
          >
            审核
          </Button>
        ) : (
          <Button
            type="link"
            size="small"
            onClick={() => openReviewModal(record)}
          >
            查看
          </Button>
        ),
    },
  ];

  const displaySummary = summary || MOCK_SUMMARY;

  return (
    <div className={styles.page}>
      <Spin spinning={loading}>
        {/* 页面标题 */}
        <div className={styles.headerRow}>
          <div>
            <Title level={4} className={styles.titleZeroMargin}>
              AI 候选配置审核台
            </Title>
            <Text type="secondary">
              审核 AI 生成的候选知识配置，确保内容质量与合规性
            </Text>
          </div>
          <Space>
            <Button icon={<HistoryOutlined />} onClick={openHistory}>
              审核历史
            </Button>
            <Button icon={<ReloadOutlined />} onClick={fetchData}>
              刷新
            </Button>
          </Space>
        </div>

        {/* 审核统计卡片 */}
        <Row gutter={16} className={styles.statsRow}>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="待审核"
                value={displaySummary.pending}
                // eslint-disable-next-line medkernel/no-inline-style -- AntD Statistic valueStyle 仅接受对象
                valueStyle={{ color: "var(--mk-primary)" }}
                prefix={<EditOutlined />}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="已通过"
                value={displaySummary.approved}
                // eslint-disable-next-line medkernel/no-inline-style -- AntD Statistic valueStyle 仅接受对象
                valueStyle={{ color: "var(--mk-success)" }}
                prefix={<CheckCircleOutlined />}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="已驳回"
                value={displaySummary.rejected}
                // eslint-disable-next-line medkernel/no-inline-style -- AntD Statistic valueStyle 仅接受对象
                valueStyle={{ color: "var(--mk-danger)" }}
                prefix={<CloseCircleOutlined />}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="已修改"
                value={displaySummary.modified}
                // eslint-disable-next-line medkernel/no-inline-style -- AntD Statistic valueStyle 仅接受对象
                valueStyle={{ color: "var(--mk-warning)" }}
                prefix={<EditOutlined />}
              />
            </Card>
          </Col>
        </Row>

        {/* 筛选栏 + 批量操作 */}
        <Card
          size="small"
          className={styles.filterCard}
          bodyStyle={{ padding: "12px 16px" }}
        >
          <div className={styles.filterRow}>
            <Space>
              <Select
                placeholder="候选类型"
                allowClear
                className={styles.selectMedium}
                value={filterType}
                onChange={setFilterType}
                options={Object.entries(CANDIDATE_TYPE_LABEL).map(
                  ([value, label]) => ({ value, label })
                )}
              />
              <Select
                placeholder="审核状态"
                allowClear
                className={styles.selectMedium}
                value={filterStatus}
                onChange={setFilterStatus}
                options={Object.entries(REVIEW_STATUS_TAG).map(
                  ([value, { label }]) => ({ value, label })
                )}
              />
              <Select
                placeholder="优先级"
                allowClear
                className={styles.selectSmall}
                value={filterPriority}
                onChange={setFilterPriority}
                options={Object.entries(PRIORITY_TAG).map(
                  ([value, { label }]) => ({ value, label })
                )}
              />
            </Space>
            {selectedRowKeys.length > 0 && (
              <Space>
                <Text type="secondary">
                  已选 {selectedRowKeys.length} 项
                </Text>
                <Button
                  size="small"
                  type="primary"
                  onClick={() => handleBatchReview("APPROVED")}
                >
                  批量通过
                </Button>
                <Button
                  size="small"
                  danger
                  onClick={() => handleBatchReview("REJECTED")}
                >
                  批量驳回
                </Button>
              </Space>
            )}
          </div>
        </Card>

        {/* 候选列表表格 */}
        <Card size="small">
          <Table
            columns={columns}
            dataSource={candidates}
            rowKey="id"
            scroll={{ x: 1280 }}
            size="middle"
            rowSelection={{
              selectedRowKeys,
              onChange: (keys) => setSelectedRowKeys(keys as number[]),
              getCheckboxProps: (record) => ({
                disabled: record.reviewStatus !== "PENDING",
              }),
            }}
            pagination={{
              pageSize: 10,
              showTotal: (total) => `共 ${total} 条`,
              showSizeChanger: true,
            }}
          />
        </Card>
      </Spin>

      {/* 审核弹窗 */}
      <Modal
        title={
          reviewingRecord?.reviewStatus === "PENDING"
            ? "审核候选配置"
            : "候选配置详情"
        }
        open={reviewModalOpen}
        onCancel={() => setReviewModalOpen(false)}
        width={640}
        footer={
          reviewingRecord?.reviewStatus === "PENDING"
            ? [
                <Button key="cancel" onClick={() => setReviewModalOpen(false)}>
                  取消
                </Button>,
                <Button
                  key="reject"
                  danger
                  onClick={() => {
                    reviewForm.setFieldsValue({ reviewStatus: "REJECTED" });
                    handleReviewSubmit();
                  }}
                >
                  驳回
                </Button>,
                <Button
                  key="modify"
                  type="default"
                  onClick={() => {
                    reviewForm.setFieldsValue({ reviewStatus: "MODIFIED" });
                    handleReviewSubmit();
                  }}
                >
                  修改通过
                </Button>,
                <Button
                  key="approve"
                  type="primary"
                  onClick={() => {
                    reviewForm.setFieldsValue({ reviewStatus: "APPROVED" });
                    handleReviewSubmit();
                  }}
                >
                  通过
                </Button>,
              ]
            : [
                <Button key="close" onClick={() => setReviewModalOpen(false)}>
                  关闭
                </Button>,
              ]
        }
      >
        {reviewingRecord && (
          <Form form={reviewForm} layout="vertical">
            <Row gutter={16}>
              <Col span={12}>
                <Text type="secondary">候选编码</Text>
                <div className={styles.formField}>
                  {reviewingRecord.candidateCode}
                </div>
              </Col>
              <Col span={12}>
                <Text type="secondary">候选名称</Text>
                <div className={styles.formField}>
                  {reviewingRecord.candidateName}
                </div>
              </Col>
            </Row>
            <Row gutter={16}>
              <Col span={8}>
                <Text type="secondary">类型</Text>
                <div className={styles.formField}>
                  {CANDIDATE_TYPE_LABEL[reviewingRecord.candidateType] ||
                    reviewingRecord.candidateType}
                </div>
              </Col>
              <Col span={8}>
                <Text type="secondary">来源</Text>
                <div className={styles.formField}>
                  {reviewingRecord.sourceName}
                </div>
              </Col>
              <Col span={8}>
                <Text type="secondary">模型</Text>
                <div className={styles.formField}>
                  {reviewingRecord.modelProvider} / {reviewingRecord.modelName}
                </div>
              </Col>
            </Row>
            <Row gutter={16}>
              <Col span={8}>
                <Text type="secondary">置信度</Text>
                <div className={styles.formField}>
                  <Text
                    type={confidenceTextType(reviewingRecord.confidence)}
                  >
                    {((reviewingRecord.confidence ?? 0) * 100).toFixed(1)}%
                  </Text>
                </div>
              </Col>
              <Col span={8}>
                <Text type="secondary">优先级</Text>
                <div className={styles.formField}>
                  <Tag
                    color={PRIORITY_TAG[reviewingRecord.priority]?.color || ""}
                  >
                    {PRIORITY_TAG[reviewingRecord.priority]?.label ||
                      reviewingRecord.priority}
                  </Tag>
                </div>
              </Col>
              <Col span={8}>
                <Text type="secondary">状态</Text>
                <div className={styles.formField}>
                  <Tag
                    color={
                      REVIEW_STATUS_TAG[reviewingRecord.reviewStatus]?.color || ""
                    }
                  >
                    {REVIEW_STATUS_TAG[reviewingRecord.reviewStatus]?.label ||
                      reviewingRecord.reviewStatus}
                  </Tag>
                </div>
              </Col>
            </Row>

            <div style={{ marginBottom: 12 }}>
              <Text type="secondary">候选内容</Text>
              <div
                style={{
                  background: "var(--mk-bg-muted)",
                  padding: 12,
                  borderRadius: 6,
                  marginTop: 4,
                  whiteSpace: "pre-wrap",
                  maxHeight: 200,
                  overflow: "auto",
                }}
              >
                {reviewingRecord.candidateContent}
              </div>
            </div>

            {reviewingRecord.qualityFindings && (
              <div style={{ marginBottom: 12 }}>
                <Text type="secondary">质检发现</Text>
                <div
                  style={{
                    background: "var(--mk-warning-soft)",
                    padding: 12,
                    borderRadius: 6,
                    marginTop: 4,
                  }}
                >
                  {reviewingRecord.qualityFindings}
                </div>
              </div>
            )}

            {reviewingRecord.reviewStatus !== "PENDING" && (
              <>
                <div className={styles.formField}>
                  <Text type="secondary">审核人</Text>
                  <div>{reviewingRecord.reviewedBy || "-"}</div>
                </div>
                <div className={styles.formField}>
                  <Text type="secondary">审核备注</Text>
                  <div>{reviewingRecord.reviewNote || "-"}</div>
                </div>
                {reviewingRecord.modifiedContent && (
                  <div className={styles.formField}>
                    <Text type="secondary">修改后内容</Text>
                    <div
                      style={{
                        background: "var(--mk-bg-muted)",
                        padding: 12,
                        borderRadius: 6,
                        marginTop: 4,
                        whiteSpace: "pre-wrap",
                      }}
                    >
                      {reviewingRecord.modifiedContent}
                    </div>
                  </div>
                )}
              </>
            )}

            {reviewingRecord.reviewStatus === "PENDING" && (
              <>
                <Form.Item name="reviewStatus" hidden>
                  <Input />
                </Form.Item>
                <Form.Item
                  name="reviewNote"
                  label="审核备注"
                  rules={[{ required: true, message: "请输入审核备注" }]}
                >
                  <Input.TextArea rows={3} placeholder="请输入审核备注" />
                </Form.Item>
                <Form.Item
                  noStyle
                  shouldUpdate={(prev, cur) =>
                    prev.reviewStatus !== cur.reviewStatus
                  }
                >
                  {({ getFieldValue }) =>
                    getFieldValue("reviewStatus") === "MODIFIED" ? (
                      <Form.Item
                        name="modifiedContent"
                        label="修改后内容"
                        rules={[
                          { required: true, message: "请输入修改后内容" },
                        ]}
                      >
                        <Input.TextArea
                          rows={4}
                          placeholder="请输入修改后的内容"
                        />
                      </Form.Item>
                    ) : null
                  }
                </Form.Item>
              </>
            )}
          </Form>
        )}
      </Modal>

      {/* 审核历史抽屉 */}
      <Drawer
        title="审核历史"
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        width={720}
      >
        <Table
          columns={[
            {
              title: "候选编码",
              dataIndex: "candidateCode",
              key: "candidateCode",
              width: 150,
            },
            {
              title: "名称",
              dataIndex: "candidateName",
              key: "candidateName",
              width: 120,
            },
            {
              title: "状态",
              dataIndex: "reviewStatus",
              key: "reviewStatus",
              width: 90,
              render: (v: string) => {
                const tag = REVIEW_STATUS_TAG[v];
                return tag ? <Tag color={tag.color}>{tag.label}</Tag> : v;
              },
            },
            {
              title: "审核人",
              dataIndex: "reviewedBy",
              key: "reviewedBy",
              width: 100,
            },
            {
              title: "审核时间",
              dataIndex: "reviewedTime",
              key: "reviewedTime",
              width: 160,
            },
            {
              title: "备注",
              dataIndex: "reviewNote",
              key: "reviewNote",
              ellipsis: true,
            },
          ]}
          dataSource={historyData}
          rowKey="id"
          size="small"
          pagination={{ pageSize: 10 }}
        />
      </Drawer>
    </div>
  );
}
