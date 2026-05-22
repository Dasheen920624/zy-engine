import { useState } from "react";
import {
  Button,
  Card,
  Col,
  Descriptions,
  Input,
  Modal,
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
  CheckCircleOutlined,
  CloseCircleOutlined,
  EditOutlined,
  ExperimentOutlined,
  HistoryOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
} from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  listCandidates,
  getCandidate,
  reviewCandidate,
  batchReview,
  getReviewSummary,
  getReviewHistory,
  type AiCandidateReview,
  type ReviewSummary,
} from "../../api/aiCandidateReview";
import { getOrgContext } from "../../store/orgContext";
import styles from "./AiKnowledgeReview.module.css";

const { Title, Paragraph, Text } = Typography;
const { TextArea } = Input;
const { Option } = Select;

const CANDIDATE_TYPE_OPTIONS = [
  { label: "疾病", value: "DISEASE" },
  { label: "症状", value: "SYMPTOM" },
  { label: "检查", value: "EXAM" },
  { label: "检验", value: "LAB" },
  { label: "药物", value: "DRUG" },
  { label: "手术", value: "PROCEDURE" },
];

const REVIEW_STATUS_OPTIONS = [
  { label: "待审核", value: "PENDING" },
  { label: "已通过", value: "APPROVED" },
  { label: "已拒绝", value: "REJECTED" },
  { label: "已修改", value: "MODIFIED" },
];

const PRIORITY_OPTIONS = [
  { label: "高", value: "HIGH" },
  { label: "中", value: "MEDIUM" },
  { label: "低", value: "LOW" },
];

const REVIEW_STATUS_COLORS: Record<string, string> = {
  PENDING: "orange",
  APPROVED: "green",
  REJECTED: "red",
  MODIFIED: "blue",
};

const PRIORITY_COLORS: Record<string, string> = {
  HIGH: "red",
  MEDIUM: "orange",
  LOW: "green",
};

/**
 * 知识审核台（PR-V2-05）。
 *
 * 功能：
 *  - 查看待审核的AI候选知识
 *  - 审核（通过/拒绝/修改）候选知识
 *  - 批量审核
 *  - 查看审核历史
 *  - 查看审核统计
 *
 * 后端端点：
 *  - GET  /api/knowledge/candidates           候选列表
 *  - GET  /api/knowledge/candidates/:id       候选详情
 *  - POST /api/knowledge/candidates/:id/review 审核
 *  - POST /api/knowledge/candidates/batch-review 批量审核
 *  - GET  /api/knowledge/candidates/summary   统计
 *  - GET  /api/knowledge/candidates/history   历史
 */
export default function AiKnowledgeReview() {
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [selectedCandidate, setSelectedCandidate] = useState<AiCandidateReview | null>(null);
  const [reviewNote, setReviewNote] = useState("");
  const [modifiedContent, setModifiedContent] = useState("");
  const queryClient = useQueryClient();
  const tenant_id = getOrgContext().tenant_id || "TENANT_DEMO";

  // 候选列表查询
  const candidatesQuery = useQuery({
    queryKey: ["knowledge", "candidates", tenant_id],
    queryFn: () =>
      listCandidates({
        limit: 50,
      }),
  });

  // 审核统计查询
  const summaryQuery = useQuery({
    queryKey: ["knowledge", "summary", tenant_id],
    queryFn: () => getReviewSummary(),
  });

  // 审核历史查询
  const historyQuery = useQuery({
    queryKey: ["knowledge", "history", tenant_id],
    queryFn: () =>
      getReviewHistory({
        limit: 20,
      }),
  });

  // 单个审核
  const reviewMutation = useMutation({
    mutationFn: async ({
      candidateId,
      reviewStatus,
    }: {
      candidateId: number;
      reviewStatus: string;
    }) => {
      await reviewCandidate(candidateId, {
        reviewStatus,
        reviewNote,
        modifiedContent: reviewStatus === "MODIFIED" ? modifiedContent : undefined,
      });
    },
    onSuccess: () => {
      message.success("审核成功");
      setDetailModalVisible(false);
      setSelectedCandidate(null);
      setReviewNote("");
      setModifiedContent("");
      queryClient.invalidateQueries({ queryKey: ["knowledge"] });
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : "审核失败");
    },
  });

  // 批量审核
  const batchReviewMutation = useMutation({
    mutationFn: async ({ reviewStatus }: { reviewStatus: string }) => {
      await batchReview({
        candidateIds: selectedRowKeys,
        reviewStatus,
        reviewNote,
      });
    },
    onSuccess: () => {
      message.success(`批量审核成功，共 ${selectedRowKeys.length} 条`);
      setSelectedRowKeys([]);
      setReviewNote("");
      queryClient.invalidateQueries({ queryKey: ["knowledge"] });
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : "批量审核失败");
    },
  });

  const handleViewDetail = async (candidateId: number) => {
    try {
      const candidate = await getCandidate(candidateId);
      setSelectedCandidate(candidate);
      setDetailModalVisible(true);
    } catch (error) {
      message.error("获取候选详情失败");
    }
  };

  const candidateColumns: ColumnsType<AiCandidateReview> = [
    {
      title: "候选编码",
      dataIndex: "candidateCode",
      key: "candidateCode",
      width: 150,
      render: (code: string) => (
        <Text code className={styles.codeText}>
          {code}
        </Text>
      ),
    },
    {
      title: "候选名称",
      dataIndex: "candidateName",
      key: "candidateName",
      width: 200,
    },
    {
      title: "类型",
      dataIndex: "candidateType",
      key: "candidateType",
      width: 100,
      render: (type: string) => <Tag>{type}</Tag>,
    },
    {
      title: "来源",
      dataIndex: "sourceName",
      key: "sourceName",
      width: 150,
    },
    {
      title: "模型",
      dataIndex: "modelName",
      key: "modelName",
      width: 120,
    },
    {
      title: "置信度",
      dataIndex: "confidence",
      key: "confidence",
      width: 100,
      render: (confidence: number) => (
        <Tag color={confidence > 0.8 ? "green" : confidence > 0.6 ? "orange" : "red"}>
          {(confidence * 100).toFixed(1)}%
        </Tag>
      ),
    },
    {
      title: "优先级",
      dataIndex: "priority",
      key: "priority",
      width: 80,
      render: (priority: string) => (
        <Tag color={PRIORITY_COLORS[priority] || "default"}>{priority}</Tag>
      ),
    },
    {
      title: "审核状态",
      dataIndex: "reviewStatus",
      key: "reviewStatus",
      width: 100,
      render: (status: string) => (
        <Tag color={REVIEW_STATUS_COLORS[status] || "default"}>{status}</Tag>
      ),
    },
    {
      title: "操作",
      key: "action",
      width: 120,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            onClick={() => handleViewDetail(record.id)}
          >
            查看
          </Button>
          {record.reviewStatus === "PENDING" && (
            <>
              <Button
                type="link"
                size="small"
                icon={<CheckCircleOutlined />}
                onClick={() =>
                  reviewMutation.mutate({
                    candidateId: record.id,
                    reviewStatus: "APPROVED",
                  })
                }
              >
                通过
              </Button>
              <Button
                type="link"
                size="small"
                danger
                icon={<CloseCircleOutlined />}
                onClick={() =>
                  reviewMutation.mutate({
                    candidateId: record.id,
                    reviewStatus: "REJECTED",
                  })
                }
              >
                拒绝
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  const historyColumns: ColumnsType<AiCandidateReview> = [
    {
      title: "候选编码",
      dataIndex: "candidateCode",
      key: "candidateCode",
      width: 150,
    },
    {
      title: "候选名称",
      dataIndex: "candidateName",
      key: "candidateName",
      width: 200,
    },
    {
      title: "审核状态",
      dataIndex: "reviewStatus",
      key: "reviewStatus",
      width: 100,
      render: (status: string) => (
        <Tag color={REVIEW_STATUS_COLORS[status] || "default"}>{status}</Tag>
      ),
    },
    {
      title: "审核人",
      dataIndex: "reviewedBy",
      key: "reviewedBy",
      width: 120,
    },
    {
      title: "审核时间",
      dataIndex: "reviewedTime",
      key: "reviewedTime",
      width: 180,
    },
    {
      title: "审核备注",
      dataIndex: "reviewNote",
      key: "reviewNote",
      ellipsis: true,
    },
  ];

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <div>
          <Space className={styles.eyebrow}>
            <SafetyCertificateOutlined />
            <span>质控驾驶舱 / 知识审核</span>
          </Space>
          <Title level={2}>知识审核台</Title>
          <Paragraph type="secondary">
            审核AI生成的候选知识，确保知识库的准确性和可靠性。支持单个审核和批量审核。
          </Paragraph>
        </div>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={8}>
          <Card title="审核统计" className={styles.card}>
            {summaryQuery.isLoading ? (
              <div className={styles.loading}>
                <Spin size="small" />
              </div>
            ) : summaryQuery.data ? (
              <Descriptions column={1} size="small">
                <Descriptions.Item label="总数">
                  <Text strong>{summaryQuery.data.total}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="待审核">
                  <Tag color="orange">{summaryQuery.data.pending}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="已通过">
                  <Tag color="green">{summaryQuery.data.approved}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="已拒绝">
                  <Tag color="red">{summaryQuery.data.rejected}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="已修改">
                  <Tag color="blue">{summaryQuery.data.modified}</Tag>
                </Descriptions.Item>
              </Descriptions>
            ) : null}
          </Card>
        </Col>

        <Col xs={24} lg={16}>
          <Card
            title="待审核候选"
            className={styles.card}
            extra={
              <Space>
                {selectedRowKeys.length > 0 && (
                  <>
                    <Input
                      placeholder="审核备注"
                      value={reviewNote}
                      onChange={(e) => setReviewNote(e.target.value)}
                      style={{ width: 200 }}
                    />
                    <Button
                      type="primary"
                      icon={<CheckCircleOutlined />}
                      loading={batchReviewMutation.isPending}
                      onClick={() =>
                        batchReviewMutation.mutate({ reviewStatus: "APPROVED" })
                      }
                    >
                      批量通过
                    </Button>
                    <Button
                      danger
                      icon={<CloseCircleOutlined />}
                      loading={batchReviewMutation.isPending}
                      onClick={() =>
                        batchReviewMutation.mutate({ reviewStatus: "REJECTED" })
                      }
                    >
                      批量拒绝
                    </Button>
                  </>
                )}
                <Button
                  icon={<ReloadOutlined />}
                  onClick={() => candidatesQuery.refetch()}
                  loading={candidatesQuery.isFetching}
                >
                  刷新
                </Button>
              </Space>
            }
          >
            <Table
              columns={candidateColumns}
              dataSource={candidatesQuery.data?.items ?? []}
              rowKey="id"
              pagination={{ pageSize: 10 }}
              size="small"
              loading={candidatesQuery.isLoading}
              scroll={{ x: 1200 }}
              rowSelection={{
                selectedRowKeys,
                onChange: (keys) => setSelectedRowKeys(keys as number[]),
                getCheckboxProps: (record) => ({
                  disabled: record.reviewStatus !== "PENDING",
                }),
              }}
            />
          </Card>
        </Col>
      </Row>

      <Card
        title="审核历史"
        className={styles.card}
        extra={
          <Button
            icon={<HistoryOutlined />}
            onClick={() => historyQuery.refetch()}
            loading={historyQuery.isFetching}
          >
            刷新
          </Button>
        }
      >
        <Table
          columns={historyColumns}
          dataSource={historyQuery.data?.items ?? []}
          rowKey="id"
          pagination={{ pageSize: 10 }}
          size="small"
          loading={historyQuery.isLoading}
          scroll={{ x: 800 }}
        />
      </Card>

      <Modal
        title="候选详情"
        open={detailModalVisible}
        onCancel={() => {
          setDetailModalVisible(false);
          setSelectedCandidate(null);
          setReviewNote("");
          setModifiedContent("");
        }}
        footer={
          selectedCandidate?.reviewStatus === "PENDING"
            ? [
                <Button
                  key="reject"
                  danger
                  icon={<CloseCircleOutlined />}
                  loading={reviewMutation.isPending}
                  onClick={() =>
                    reviewMutation.mutate({
                      candidateId: selectedCandidate.id,
                      reviewStatus: "REJECTED",
                    })
                  }
                >
                  拒绝
                </Button>,
                <Button
                  key="modify"
                  icon={<EditOutlined />}
                  loading={reviewMutation.isPending}
                  onClick={() =>
                    reviewMutation.mutate({
                      candidateId: selectedCandidate.id,
                      reviewStatus: "MODIFIED",
                    })
                  }
                >
                  修改后通过
                </Button>,
                <Button
                  key="approve"
                  type="primary"
                  icon={<CheckCircleOutlined />}
                  loading={reviewMutation.isPending}
                  onClick={() =>
                    reviewMutation.mutate({
                      candidateId: selectedCandidate.id,
                      reviewStatus: "APPROVED",
                    })
                  }
                >
                  通过
                </Button>,
              ]
            : undefined
        }
        width={800}
      >
        {selectedCandidate && (
          <div className={styles.detailContent}>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="候选编码">
                <Text code>{selectedCandidate.candidateCode}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="候选名称">
                {selectedCandidate.candidateName}
              </Descriptions.Item>
              <Descriptions.Item label="类型">
                <Tag>{selectedCandidate.candidateType}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="来源">
                {selectedCandidate.sourceName}
              </Descriptions.Item>
              <Descriptions.Item label="模型">
                {selectedCandidate.modelName}
              </Descriptions.Item>
              <Descriptions.Item label="置信度">
                <Tag
                  color={
                    selectedCandidate.confidence > 0.8
                      ? "green"
                      : selectedCandidate.confidence > 0.6
                      ? "orange"
                      : "red"
                  }
                >
                  {(selectedCandidate.confidence * 100).toFixed(1)}%
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="优先级">
                <Tag color={PRIORITY_COLORS[selectedCandidate.priority] || "default"}>
                  {selectedCandidate.priority}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="审核状态">
                <Tag color={REVIEW_STATUS_COLORS[selectedCandidate.reviewStatus] || "default"}>
                  {selectedCandidate.reviewStatus}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间" span={2}>
                {selectedCandidate.createdTime}
              </Descriptions.Item>
              <Descriptions.Item label="候选内容" span={2}>
                <div className={styles.contentBlock}>
                  {selectedCandidate.candidateContent}
                </div>
              </Descriptions.Item>
              {selectedCandidate.qualityFindings && (
                <Descriptions.Item label="质量发现" span={2}>
                  <div className={styles.contentBlock}>
                    {selectedCandidate.qualityFindings}
                  </div>
                </Descriptions.Item>
              )}
            </Descriptions>

            {selectedCandidate.reviewStatus === "PENDING" && (
              <div className={styles.reviewForm}>
                <div className={styles.formItem}>
                  <Text strong>审核备注</Text>
                  <TextArea
                    rows={3}
                    placeholder="请输入审核备注"
                    value={reviewNote}
                    onChange={(e) => setReviewNote(e.target.value)}
                  />
                </div>
                <div className={styles.formItem}>
                  <Text strong>修改后内容（仅修改时填写）</Text>
                  <TextArea
                    rows={5}
                    placeholder="如需修改候选内容，请在此填写修改后的版本"
                    value={modifiedContent}
                    onChange={(e) => setModifiedContent(e.target.value)}
                  />
                </div>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}