import { useState } from "react";
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  Row,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  message,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import {
  ClusterOutlined,
  ExperimentOutlined,
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  getDiseaseCandidates,
  listEvidences,
  listGraphVersions,
  listNodes,
  listEdges,
  type GraphCandidate,
  type GraphEvidence,
  type GraphNode,
  type GraphEdge,
  type GraphVersion,
  type DiseaseCandidatesRequest,
} from "../../api/graph";
import { getOrgContext } from "../../store/orgContext";
import styles from "./GraphExplore.module.css";

const { Title, Paragraph, Text } = Typography;

const VERSION_STATUS_COLORS: Record<string, string> = {
  DRAFT: "default",
  ACTIVE: "green",
  ARCHIVED: "gray",
};

/**
 * 图谱查询工作台（PR-V2-05）。
 *
 * 功能：
 *  - 疾病候选查询（基于症状/体征）
 *  - 证据查询（指南/文献/专家共识）
 *  - 节点/边查询（知识图谱浏览）
 *  - 版本管理（查看/激活版本）
 *
 * 后端端点：
 *  - POST /api/graph/disease-candidates    疾病候选查询
 *  - GET  /api/graph/evidences             证据查询
 *  - GET  /api/graph/nodes                 节点查询
 *  - GET  /api/graph/edges                 边查询
 *  - GET  /api/graph/versions              版本管理
 */
export default function GraphExplore() {
  const [form] = Form.useForm();
  const [candidates, setCandidates] = useState<GraphCandidate[]>([]);
  const tenant_id = getOrgContext().tenant_id || "TENANT_DEMO";

  // 疾病候选查询
  const candidatesMutation = useMutation({
    mutationFn: async (values: {
      symptoms: string;
      findings: string;
    }) => {
      const request: DiseaseCandidatesRequest = {
        symptoms: values.symptoms ? values.symptoms.split(",").map((s) => s.trim()) : [],
        findings: values.findings ? values.findings.split(",").map((s) => s.trim()) : [],
        limit: 20,
      };
      return getDiseaseCandidates(request);
    },
    onSuccess: (data) => {
      setCandidates(data);
      message.success(`查询完成，找到 ${data.length} 个候选疾病`);
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : "查询失败");
    },
  });

  // 版本列表查询
  const versionsQuery = useQuery({
    queryKey: ["graph", "versions", tenant_id],
    queryFn: () =>
      listGraphVersions({
        limit: 20,
      }),
  });

  // 证据列表查询
  const evidencesQuery = useQuery({
    queryKey: ["graph", "evidences", tenant_id],
    queryFn: () =>
      listEvidences({
        limit: 20,
      }),
  });

  // 节点列表查询
  const nodesQuery = useQuery({
    queryKey: ["graph", "nodes", tenant_id],
    queryFn: () =>
      listNodes({
        limit: 50,
      }),
  });

  // 边列表查询
  const edgesQuery = useQuery({
    queryKey: ["graph", "edges", tenant_id],
    queryFn: () =>
      listEdges({
        limit: 50,
      }),
  });

  const candidateColumns: ColumnsType<GraphCandidate> = [
    {
      title: "疾病编码",
      dataIndex: "code",
      key: "code",
      width: 120,
      render: (code: string) => (
        <Text code className={styles.codeText}>
          {code}
        </Text>
      ),
    },
    {
      title: "疾病名称",
      dataIndex: "name",
      key: "name",
      width: 200,
    },
    {
      title: "类型",
      dataIndex: "type",
      key: "type",
      width: 100,
      render: (type: string) => <Tag>{type}</Tag>,
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
      title: "证据数",
      dataIndex: "evidence_count",
      key: "evidence_count",
      width: 80,
    },
    {
      title: "图谱版本",
      dataIndex: "graph_version",
      key: "graph_version",
      width: 120,
      render: (version: string) => <Tag color="blue">{version}</Tag>,
    },
  ];

  const evidenceColumns: ColumnsType<GraphEvidence> = [
    {
      title: "证据ID",
      dataIndex: "evidence_id",
      key: "evidence_id",
      width: 150,
      ellipsis: true,
    },
    {
      title: "目标编码",
      dataIndex: "target_code",
      key: "target_code",
      width: 120,
      render: (code: string) => (
        <Text code className={styles.codeText}>
          {code}
        </Text>
      ),
    },
    {
      title: "目标名称",
      dataIndex: "target_name",
      key: "target_name",
      width: 150,
    },
    {
      title: "证据类型",
      dataIndex: "evidence_type",
      key: "evidence_type",
      width: 100,
      render: (type: string) => <Tag>{type}</Tag>,
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
      title: "内容",
      dataIndex: "content",
      key: "content",
      ellipsis: true,
    },
  ];

  const nodeColumns: ColumnsType<GraphNode> = [
    {
      title: "节点编码",
      dataIndex: "node_code",
      key: "node_code",
      width: 120,
      render: (code: string) => (
        <Text code className={styles.codeText}>
          {code}
        </Text>
      ),
    },
    {
      title: "节点名称",
      dataIndex: "node_name",
      key: "node_name",
      width: 150,
    },
    {
      title: "节点类型",
      dataIndex: "node_type",
      key: "node_type",
      width: 100,
      render: (type: string) => <Tag>{type}</Tag>,
    },
    {
      title: "图谱版本",
      dataIndex: "graph_version",
      key: "graph_version",
      width: 120,
      render: (version: string) => <Tag color="blue">{version}</Tag>,
    },
    {
      title: "创建时间",
      dataIndex: "created_time",
      key: "created_time",
      width: 180,
    },
  ];

  const edgeColumns: ColumnsType<GraphEdge> = [
    {
      title: "边ID",
      dataIndex: "edge_id",
      key: "edge_id",
      width: 150,
      ellipsis: true,
    },
    {
      title: "起始节点",
      dataIndex: "from_code",
      key: "from_code",
      width: 120,
      render: (code: string) => (
        <Text code className={styles.codeText}>
          {code}
        </Text>
      ),
    },
    {
      title: "目标节点",
      dataIndex: "to_code",
      key: "to_code",
      width: 120,
      render: (code: string) => (
        <Text code className={styles.codeText}>
          {code}
        </Text>
      ),
    },
    {
      title: "边类型",
      dataIndex: "edge_type",
      key: "edge_type",
      width: 120,
      render: (type: string) => <Tag>{type}</Tag>,
    },
    {
      title: "权重",
      dataIndex: "weight",
      key: "weight",
      width: 80,
    },
    {
      title: "图谱版本",
      dataIndex: "graph_version",
      key: "graph_version",
      width: 120,
      render: (version: string) => <Tag color="blue">{version}</Tag>,
    },
  ];

  const versionColumns: ColumnsType<GraphVersion> = [
    {
      title: "版本编码",
      dataIndex: "version_code",
      key: "version_code",
      width: 120,
      render: (code: string) => (
        <Text code className={styles.codeText}>
          {code}
        </Text>
      ),
    },
    {
      title: "版本名称",
      dataIndex: "version_name",
      key: "version_name",
      width: 150,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (status: string) => (
        <Tag color={VERSION_STATUS_COLORS[status] || "default"}>{status}</Tag>
      ),
    },
    {
      title: "节点数",
      dataIndex: "node_count",
      key: "node_count",
      width: 80,
    },
    {
      title: "边数",
      dataIndex: "edge_count",
      key: "edge_count",
      width: 80,
    },
    {
      title: "证据数",
      dataIndex: "evidence_count",
      key: "evidence_count",
      width: 80,
    },
    {
      title: "创建时间",
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
            <ClusterOutlined />
            <span>知识工厂 / 图谱查询</span>
          </Space>
          <Title level={2}>图谱查询工作台</Title>
          <Paragraph type="secondary">
            查询医学知识图谱，包括疾病候选、证据、节点和边。支持基于症状的疾病推断和证据检索。
          </Paragraph>
        </div>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="疾病候选查询" className={styles.card}>
            <Form
              form={form}
              layout="vertical"
              onFinish={(values) => candidatesMutation.mutate(values)}
              initialValues={{
                symptoms: "胸痛, 呼吸困难",
                findings: "心电图ST段抬高",
              }}
            >
              <Form.Item
                name="symptoms"
                label="症状（逗号分隔）"
                rules={[{ required: true, message: "请输入症状" }]}
              >
                <Input placeholder="胸痛, 呼吸困难, ..." />
              </Form.Item>

              <Form.Item
                name="findings"
                label="体征/检查发现（逗号分隔）"
              >
                <Input placeholder="心电图ST段抬高, ..." />
              </Form.Item>

              <Form.Item>
                <Space>
                  <Button
                    type="primary"
                    htmlType="submit"
                    icon={<SearchOutlined />}
                    loading={candidatesMutation.isPending}
                  >
                    查询候选
                  </Button>
                  <Button
                    icon={<ReloadOutlined />}
                    onClick={() => {
                      form.resetFields();
                      setCandidates([]);
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
          <Card title="候选结果" className={styles.card}>
            {candidatesMutation.isPending ? (
              <div className={styles.loading}>
                <Spin size="large" />
                <Text type="secondary">正在查询疾病候选...</Text>
              </div>
            ) : candidates.length > 0 ? (
              <Table
                columns={candidateColumns}
                dataSource={candidates}
                rowKey="code"
                pagination={false}
                size="small"
                scroll={{ x: 800 }}
              />
            ) : (
              <div className={styles.empty}>
                <ExperimentOutlined className={styles.emptyIcon} />
                <Text type="secondary">输入症状和体征，点击"查询候选"查看结果</Text>
              </div>
            )}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card
            title="证据列表"
            className={styles.card}
            extra={
              <Button
                icon={<ReloadOutlined />}
                onClick={() => evidencesQuery.refetch()}
                loading={evidencesQuery.isFetching}
                size="small"
              >
                刷新
              </Button>
            }
          >
            <Table
              columns={evidenceColumns}
              dataSource={evidencesQuery.data ?? []}
              rowKey="evidence_id"
              pagination={{ pageSize: 10 }}
              size="small"
              loading={evidencesQuery.isLoading}
              scroll={{ x: 800 }}
            />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            title="版本管理"
            className={styles.card}
            extra={
              <Button
                icon={<ReloadOutlined />}
                onClick={() => versionsQuery.refetch()}
                loading={versionsQuery.isFetching}
                size="small"
              >
                刷新
              </Button>
            }
          >
            <Table
              columns={versionColumns}
              dataSource={versionsQuery.data ?? []}
              rowKey="version_code"
              pagination={{ pageSize: 10 }}
              size="small"
              loading={versionsQuery.isLoading}
              scroll={{ x: 800 }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card
            title="节点列表"
            className={styles.card}
            extra={
              <Button
                icon={<ReloadOutlined />}
                onClick={() => nodesQuery.refetch()}
                loading={nodesQuery.isFetching}
                size="small"
              >
                刷新
              </Button>
            }
          >
            <Table
              columns={nodeColumns}
              dataSource={nodesQuery.data ?? []}
              rowKey="node_code"
              pagination={{ pageSize: 10 }}
              size="small"
              loading={nodesQuery.isLoading}
              scroll={{ x: 800 }}
            />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            title="边列表"
            className={styles.card}
            extra={
              <Button
                icon={<ReloadOutlined />}
                onClick={() => edgesQuery.refetch()}
                loading={edgesQuery.isFetching}
                size="small"
              >
                刷新
              </Button>
            }
          >
            <Table
              columns={edgeColumns}
              dataSource={edgesQuery.data ?? []}
              rowKey="edge_id"
              pagination={{ pageSize: 10 }}
              size="small"
              loading={edgesQuery.isLoading}
              scroll={{ x: 800 }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
