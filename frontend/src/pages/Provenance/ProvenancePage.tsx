import { useState } from "react";
import {
  Button,
  Card,
  Col,
  Descriptions,
  Modal,
  Row,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import {
  BookOutlined,
  BranchesOutlined,
  FileSearchOutlined,
  LinkOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import {
  listSourceDocuments,
  listCitations,
  listBindings,
  getSourceDocument,
  getCitation,
  getBinding,
  type SourceDocument,
  type SourceCitation,
  type SourceAssetBinding,
} from "../../api/provenance";
import { getOrgContext } from "../../store/orgContext";
import styles from "./ProvenancePage.module.css";

const { Title, Paragraph, Text } = Typography;

const REVIEW_STATUS_COLORS: Record<string, string> = {
  PENDING: "orange",
  APPROVED: "green",
  REJECTED: "red",
};

/**
 * 来源追溯（FE-008）。
 *
 * 功能：
 *  - 来源文档库（指南/共识/说明书/路径/文献）
 *  - 引用片段管理（推荐意见/证据摘要/操作流程/注意事项）
 *  - 资产绑定管理（规则/路径/知识与来源文档的关联）
 *
 * 后端端点：
 *  - GET /api/provenance/source-documents    来源文档列表
 *  - GET /api/provenance/citations           引用片段列表
 *  - GET /api/provenance/bindings            资产绑定列表
 */
export default function ProvenancePage() {
  const [documentDetailVisible, setDocumentDetailVisible] = useState(false);
  const [citationDetailVisible, setCitationDetailVisible] = useState(false);
  const [bindingDetailVisible, setBindingDetailVisible] = useState(false);
  const [selectedDocument, setSelectedDocument] = useState<SourceDocument | null>(null);
  const [selectedCitation, setSelectedCitation] = useState<SourceCitation | null>(null);
  const [selectedBinding, setSelectedBinding] = useState<SourceAssetBinding | null>(null);
  const tenant_id = getOrgContext().tenant_id || "TENANT_DEMO";

  // 来源文档列表查询
  const documentsQuery = useQuery({
    queryKey: ["provenance", "documents", tenant_id],
    queryFn: () =>
      listSourceDocuments({
        limit: 50,
      }),
  });

  // 引用片段列表查询
  const citationsQuery = useQuery({
    queryKey: ["provenance", "citations", tenant_id],
    queryFn: () =>
      listCitations({
        limit: 50,
      }),
  });

  // 资产绑定列表查询
  const bindingsQuery = useQuery({
    queryKey: ["provenance", "bindings", tenant_id],
    queryFn: () =>
      listBindings({
        limit: 50,
      }),
  });

  const handleViewDocument = async (documentCode: string) => {
    try {
      const document = await getSourceDocument(documentCode);
      setSelectedDocument(document);
      setDocumentDetailVisible(true);
    } catch {
      message.error("获取文档详情失败");
    }
  };

  const handleViewCitation = async (citationId: string) => {
    try {
      const citation = await getCitation(citationId);
      setSelectedCitation(citation);
      setCitationDetailVisible(true);
    } catch {
      message.error("获取引用详情失败");
    }
  };

  const handleViewBinding = async (bindingId: string) => {
    try {
      const binding = await getBinding(bindingId);
      setSelectedBinding(binding);
      setBindingDetailVisible(true);
    } catch {
      message.error("获取绑定详情失败");
    }
  };

  const documentColumns: ColumnsType<SourceDocument> = [
    {
      title: "文档编码",
      dataIndex: "document_code",
      key: "document_code",
      width: 150,
      render: (code: string) => (
        <Text code className={styles.codeText}>
          {code}
        </Text>
      ),
    },
    {
      title: "文档名称",
      dataIndex: "document_name",
      key: "document_name",
      width: 250,
    },
    {
      title: "来源类型",
      dataIndex: "source_type",
      key: "source_type",
      width: 120,
      render: (type: string) => <Tag>{type}</Tag>,
    },
    {
      title: "发布者",
      dataIndex: "publisher",
      key: "publisher",
      width: 150,
    },
    {
      title: "审核状态",
      dataIndex: "review_status",
      key: "review_status",
      width: 100,
      render: (status: string) => (
        <Tag color={REVIEW_STATUS_COLORS[status] || "default"}>{status}</Tag>
      ),
    },
    {
      title: "创建时间",
      dataIndex: "created_time",
      key: "created_time",
      width: 180,
    },
    {
      title: "操作",
      key: "action",
      width: 100,
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          onClick={() => handleViewDocument(record.document_code)}
        >
          查看
        </Button>
      ),
    },
  ];

  const citationColumns: ColumnsType<SourceCitation> = [
    {
      title: "引用ID",
      dataIndex: "citation_id",
      key: "citation_id",
      width: 150,
      ellipsis: true,
    },
    {
      title: "文档编码",
      dataIndex: "document_code",
      key: "document_code",
      width: 150,
      render: (code: string) => (
        <Text code className={styles.codeText}>
          {code}
        </Text>
      ),
    },
    {
      title: "引用类型",
      dataIndex: "citation_type",
      key: "citation_type",
      width: 120,
      render: (type: string) => <Tag>{type}</Tag>,
    },
    {
      title: "章节",
      dataIndex: "section",
      key: "section",
      width: 120,
    },
    {
      title: "内容",
      dataIndex: "content",
      key: "content",
      ellipsis: true,
    },
    {
      title: "创建时间",
      dataIndex: "created_time",
      key: "created_time",
      width: 180,
    },
    {
      title: "操作",
      key: "action",
      width: 100,
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          onClick={() => handleViewCitation(record.citation_id)}
        >
          查看
        </Button>
      ),
    },
  ];

  const bindingColumns: ColumnsType<SourceAssetBinding> = [
    {
      title: "绑定ID",
      dataIndex: "binding_id",
      key: "binding_id",
      width: 150,
      ellipsis: true,
    },
    {
      title: "文档编码",
      dataIndex: "document_code",
      key: "document_code",
      width: 150,
      render: (code: string) => (
        <Text code className={styles.codeText}>
          {code}
        </Text>
      ),
    },
    {
      title: "资产类型",
      dataIndex: "asset_type",
      key: "asset_type",
      width: 100,
      render: (type: string) => <Tag>{type}</Tag>,
    },
    {
      title: "资产编码",
      dataIndex: "asset_code",
      key: "asset_code",
      width: 150,
      render: (code: string) => (
        <Text code className={styles.codeText}>
          {code}
        </Text>
      ),
    },
    {
      title: "资产名称",
      dataIndex: "asset_name",
      key: "asset_name",
      width: 200,
    },
    {
      title: "绑定类型",
      dataIndex: "binding_type",
      key: "binding_type",
      width: 120,
      render: (type: string) => <Tag>{type}</Tag>,
    },
    {
      title: "置信度",
      dataIndex: "confidence",
      key: "confidence",
      width: 100,
      render: (confidence?: number) =>
        confidence ? (
          <Tag color={confidence > 0.8 ? "green" : confidence > 0.6 ? "orange" : "red"}>
            {(confidence * 100).toFixed(1)}%
          </Tag>
        ) : (
          <Text type="secondary">-</Text>
        ),
    },
    {
      title: "操作",
      key: "action",
      width: 100,
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          onClick={() => handleViewBinding(record.binding_id)}
        >
          查看
        </Button>
      ),
    },
  ];

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <div>
          <Space className={styles.eyebrow}>
            <FileSearchOutlined />
            <span>知识工厂 / 来源追溯</span>
          </Space>
          <Title level={2}>来源追溯</Title>
          <Paragraph type="secondary">
            管理医学知识来源文档、引用片段和资产绑定，确保知识的可追溯性和权威性。
          </Paragraph>
        </div>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={8}>
          <Card title="来源文档库" className={styles.card}>
            <div className={styles.statsContainer}>
              <div className={styles.statItem}>
                <BookOutlined className={styles.statIcon} />
                <div>
                  <Text className={styles.statNumber}>
                    {documentsQuery.data?.length ?? 0}
                  </Text>
                  <Text type="secondary" className={styles.statLabel}>
                    文档总数
                  </Text>
                </div>
              </div>
              <div className={styles.statItem}>
                <BranchesOutlined className={styles.statIcon} />
                <div>
                  <Text className={styles.statNumber}>
                    {citationsQuery.data?.length ?? 0}
                  </Text>
                  <Text type="secondary" className={styles.statLabel}>
                    引用片段
                  </Text>
                </div>
              </div>
              <div className={styles.statItem}>
                <LinkOutlined className={styles.statIcon} />
                <div>
                  <Text className={styles.statNumber}>
                    {bindingsQuery.data?.length ?? 0}
                  </Text>
                  <Text type="secondary" className={styles.statLabel}>
                    资产绑定
                  </Text>
                </div>
              </div>
            </div>
          </Card>
        </Col>

        <Col xs={24} lg={16}>
          <Card
            title="来源文档"
            className={styles.card}
            extra={
              <Button
                icon={<ReloadOutlined />}
                onClick={() => documentsQuery.refetch()}
                loading={documentsQuery.isFetching}
              >
                刷新
              </Button>
            }
          >
            <Table
              columns={documentColumns}
              dataSource={documentsQuery.data ?? []}
              rowKey="document_code"
              pagination={{ pageSize: 10 }}
              size="small"
              loading={documentsQuery.isLoading}
              scroll={{ x: 1000 }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card
            title="引用片段"
            className={styles.card}
            extra={
              <Button
                icon={<ReloadOutlined />}
                onClick={() => citationsQuery.refetch()}
                loading={citationsQuery.isFetching}
              >
                刷新
              </Button>
            }
          >
            <Table
              columns={citationColumns}
              dataSource={citationsQuery.data ?? []}
              rowKey="citation_id"
              pagination={{ pageSize: 10 }}
              size="small"
              loading={citationsQuery.isLoading}
              scroll={{ x: 800 }}
            />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            title="资产绑定"
            className={styles.card}
            extra={
              <Button
                icon={<ReloadOutlined />}
                onClick={() => bindingsQuery.refetch()}
                loading={bindingsQuery.isFetching}
              >
                刷新
              </Button>
            }
          >
            <Table
              columns={bindingColumns}
              dataSource={bindingsQuery.data ?? []}
              rowKey="binding_id"
              pagination={{ pageSize: 10 }}
              size="small"
              loading={bindingsQuery.isLoading}
              scroll={{ x: 1000 }}
            />
          </Card>
        </Col>
      </Row>

      <Modal
        title="文档详情"
        open={documentDetailVisible}
        onCancel={() => {
          setDocumentDetailVisible(false);
          setSelectedDocument(null);
        }}
        footer={null}
        width={800}
      >
        {selectedDocument && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="文档编码">
              <Text code>{selectedDocument.document_code}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="文档名称">
              {selectedDocument.document_name}
            </Descriptions.Item>
            <Descriptions.Item label="来源类型">
              <Tag>{selectedDocument.source_type}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="发布者">
              {selectedDocument.publisher || "-"}
            </Descriptions.Item>
            <Descriptions.Item label="版本">
              {selectedDocument.version || "-"}
            </Descriptions.Item>
            <Descriptions.Item label="发布日期">
              {selectedDocument.publish_date || "-"}
            </Descriptions.Item>
            <Descriptions.Item label="审核状态">
              <Tag color={REVIEW_STATUS_COLORS[selectedDocument.review_status] || "default"}>
                {selectedDocument.review_status}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {selectedDocument.created_time}
            </Descriptions.Item>
            <Descriptions.Item label="内容摘要" span={2}>
              <div className={styles.contentBlock}>
                {selectedDocument.content_summary || "暂无摘要"}
              </div>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      <Modal
        title="引用详情"
        open={citationDetailVisible}
        onCancel={() => {
          setCitationDetailVisible(false);
          setSelectedCitation(null);
        }}
        footer={null}
        width={800}
      >
        {selectedCitation && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="引用ID">
              <Text code>{selectedCitation.citation_id}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="文档编码">
              <Text code>{selectedCitation.document_code}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="引用类型">
              <Tag>{selectedCitation.citation_type}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="章节">
              {selectedCitation.section || "-"}
            </Descriptions.Item>
            <Descriptions.Item label="页码">
              {selectedCitation.page_number || "-"}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {selectedCitation.created_time}
            </Descriptions.Item>
            <Descriptions.Item label="引用内容" span={2}>
              <div className={styles.contentBlock}>{selectedCitation.content}</div>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      <Modal
        title="绑定详情"
        open={bindingDetailVisible}
        onCancel={() => {
          setBindingDetailVisible(false);
          setSelectedBinding(null);
        }}
        footer={null}
        width={800}
      >
        {selectedBinding && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="绑定ID">
              <Text code>{selectedBinding.binding_id}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="文档编码">
              <Text code>{selectedBinding.document_code}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="资产类型">
              <Tag>{selectedBinding.asset_type}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="资产编码">
              <Text code>{selectedBinding.asset_code}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="资产名称">
              {selectedBinding.asset_name || "-"}
            </Descriptions.Item>
            <Descriptions.Item label="绑定类型">
              <Tag>{selectedBinding.binding_type}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="置信度">
              {selectedBinding.confidence ? (
                <Tag
                  color={
                    selectedBinding.confidence > 0.8
                      ? "green"
                      : selectedBinding.confidence > 0.6
                      ? "orange"
                      : "red"
                  }
                >
                  {(selectedBinding.confidence * 100).toFixed(1)}%
                </Tag>
              ) : (
                <Text type="secondary">-</Text>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {selectedBinding.created_time}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  );
}
