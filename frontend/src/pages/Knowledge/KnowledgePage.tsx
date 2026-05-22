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
  Table,
  Tag,
  message,
  Popconfirm,
  Descriptions,
} from "antd";
import {
  PlusOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from "@ant-design/icons";
import { StatusBadge } from "../../components/StatusBadge/StatusBadge";
import type { StatusKey } from "../../components/StatusBadge/StatusBadge.types";
import {
  listKnowledgeSources,
  registerKnowledgeSource,
  reviewKnowledgeSource,
  listKnowledgeSubscriptions,
  createKnowledgeSubscription,
  pauseKnowledgeSubscription,
  cancelKnowledgeSubscription,
  type KnowledgeSourceRegistry,
  type KnowledgeSubscription,
} from "../../api/knowledge";
import styles from "./knowledgePage.module.css";

const SOURCE_TYPE_OPTIONS = [
  { value: "STANDARD_TERMINOLOGY", label: "标准术语集" },
  { value: "INSURANCE_POLICY", label: "医保政策" },
  { value: "DRUG_LABEL", label: "药品说明书" },
  { value: "CLINICAL_GUIDELINE", label: "临床指南" },
  { value: "QUALITY_POLICY", label: "质控政策" },
  { value: "HOSPITAL_DICTIONARY", label: "院内字典" },
  { value: "VENDOR_INTERFACE_DOC", label: "厂商接口文档" },
];

const TOPIC_TYPE_OPTIONS = [
  { value: "DISEASE", label: "疾病" },
  { value: "DEPARTMENT", label: "科室" },
  { value: "GUIDELINE", label: "指南" },
  { value: "INSURANCE", label: "医保" },
  { value: "DRUG", label: "药品" },
  { value: "QUALITY", label: "质控" },
];

const REVIEW_STATUS_MAP: Record<string, { statusKey: StatusKey; label: string }> = {
  PENDING: { statusKey: "pending", label: "待审核" },
  APPROVED: { statusKey: "published", label: "已通过" },
  REJECTED: { statusKey: "rejected", label: "已驳回" },
  DEPRECATED: { statusKey: "retired", label: "已废弃" },
};

const SUB_STATUS_MAP: Record<string, { color: string; label: string }> = {
  ACTIVE: { color: "green", label: "活跃" },
  PAUSED: { color: "orange", label: "暂停" },
  CANCELLED: { color: "red", label: "已取消" },
};

const KnowledgePage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<"sources" | "subscriptions">("sources");
  const [sources, setSources] = useState<KnowledgeSourceRegistry[]>([]);
  const [subscriptions, setSubscriptions] = useState<KnowledgeSubscription[]>([]);
  const [loading, setLoading] = useState(false);
  const [registerModalVisible, setRegisterModalVisible] = useState(false);
  const [subModalVisible, setSubModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [selectedSource, setSelectedSource] = useState<KnowledgeSourceRegistry | null>(null);
  const [registerForm] = Form.useForm();
  const [subForm] = Form.useForm();

  const fetchSources = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listKnowledgeSources();
      setSources(data);
    } catch {
      message.error("获取来源列表失败");
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchSubscriptions = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listKnowledgeSubscriptions();
      setSubscriptions(data);
    } catch {
      message.error("获取订阅列表失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (activeTab === "sources") fetchSources();
    else fetchSubscriptions();
  }, [activeTab, fetchSources, fetchSubscriptions]);

  const handleRegister = async (values: Record<string, unknown>) => {
    try {
      await registerKnowledgeSource(values as Parameters<typeof registerKnowledgeSource>[0]);
      message.success("来源注册成功");
      setRegisterModalVisible(false);
      registerForm.resetFields();
      fetchSources();
    } catch {
      message.error("注册失败");
    }
  };

  const handleReview = async (sourceCode: string, reviewStatus: "APPROVED" | "REJECTED") => {
    try {
      await reviewKnowledgeSource(sourceCode, { review_status: reviewStatus, reviewed_by: "admin" });
      message.success(reviewStatus === "APPROVED" ? "已通过" : "已驳回");
      fetchSources();
    } catch {
      message.error("审核失败");
    }
  };

  const handleCreateSub = async (values: Record<string, unknown>) => {
    try {
      await createKnowledgeSubscription(values as Parameters<typeof createKnowledgeSubscription>[0]);
      message.success("订阅创建成功");
      setSubModalVisible(false);
      subForm.resetFields();
      fetchSubscriptions();
    } catch {
      message.error("创建订阅失败");
    }
  };

  const sourceColumns = [
    { title: "来源编码", dataIndex: "source_code", key: "source_code", width: 140 },
    { title: "名称", dataIndex: "source_name", key: "source_name", width: 180 },
    {
      title: "类型",
      dataIndex: "source_type",
      key: "source_type",
      width: 120,
      render: (v: string) => SOURCE_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? v,
    },
    { title: "发布者", dataIndex: "publisher", key: "publisher", width: 120 },
    { title: "地区", dataIndex: "region", key: "region", width: 80 },
    { title: "语言", dataIndex: "language", key: "language", width: 80 },
    {
      title: "权威等级",
      dataIndex: "authority_level",
      key: "authority_level",
      width: 100,
      render: (v: string) => v ?? "—",
    },
    {
      title: "审核状态",
      dataIndex: "review_status",
      key: "review_status",
      width: 100,
      render: (v: string) => {
        const cfg = REVIEW_STATUS_MAP[v] ?? { statusKey: "pending" as StatusKey, label: v };
        return <StatusBadge status={cfg.statusKey} text={cfg.label} />;
      },
    },
    {
      title: "操作",
      key: "action",
      width: 200,
      render: (_: unknown, record: KnowledgeSourceRegistry) => (
        <Space size="small">
          <Button size="small" onClick={() => { setSelectedSource(record); setDetailModalVisible(true); }}>
            详情
          </Button>
          {record.review_status === "PENDING" && (
            <>
              <Button size="small" type="primary" icon={<CheckCircleOutlined />} onClick={() => handleReview(record.source_code, "APPROVED")}>
                通过
              </Button>
              <Button size="small" danger icon={<CloseCircleOutlined />} onClick={() => handleReview(record.source_code, "REJECTED")}>
                驳回
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  const subColumns = [
    { title: "订阅ID", dataIndex: "subscription_id", key: "subscription_id", width: 120 },
    { title: "订阅人", dataIndex: "subscriber_name", key: "subscriber_name", width: 100 },
    {
      title: "主题类型",
      dataIndex: "topic_type",
      key: "topic_type",
      width: 100,
      render: (v: string) => TOPIC_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? v,
    },
    { title: "主题名称", dataIndex: "topic_name", key: "topic_name", width: 160 },
    {
      title: "自动同步",
      dataIndex: "auto_sync",
      key: "auto_sync",
      width: 80,
      render: (v: boolean) => v ? <Tag color="green">是</Tag> : <Tag>否</Tag>,
    },
    {
      title: "同步频率",
      dataIndex: "sync_frequency",
      key: "sync_frequency",
      width: 80,
      render: (v: string) => {
        const map: Record<string, string> = { DAILY: "每日", WEEKLY: "每周", MONTHLY: "每月", MANUAL: "手动" };
        return map[v] ?? v;
      },
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 80,
      render: (v: string) => {
        const cfg = SUB_STATUS_MAP[v] ?? { color: "default", label: v };
        return <Tag color={cfg.color}>{cfg.label}</Tag>;
      },
    },
    {
      title: "操作",
      key: "action",
      width: 140,
      render: (_: unknown, record: KnowledgeSubscription) => (
        <Space size="small">
          {record.status === "ACTIVE" && (
            <Button size="small" onClick={async () => { await pauseKnowledgeSubscription(record.subscription_id); fetchSubscriptions(); }}>
              暂停
            </Button>
          )}
          {record.status !== "CANCELLED" && (
            <Popconfirm title="确定取消此订阅？" onConfirm={async () => { await cancelKnowledgeSubscription(record.subscription_id); fetchSubscriptions(); }}>
              <Button size="small" danger>取消</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.page}>
      <Card
        title={
          <Space>
            <Button type={activeTab === "sources" ? "primary" : "default"} onClick={() => setActiveTab("sources")}>
              来源注册
            </Button>
            <Button type={activeTab === "subscriptions" ? "primary" : "default"} onClick={() => setActiveTab("subscriptions")}>
              知识订阅
            </Button>
          </Space>
        }
        extra={
          activeTab === "sources" ? (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setRegisterModalVisible(true)}>
              注册来源
            </Button>
          ) : (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setSubModalVisible(true)}>
              新建订阅
            </Button>
          )
        }
      >
        {activeTab === "sources" ? (
          <Table rowKey="source_code" columns={sourceColumns} dataSource={sources} loading={loading} pagination={{ pageSize: 10 }} />
        ) : (
          <Table rowKey="subscription_id" columns={subColumns} dataSource={subscriptions} loading={loading} pagination={{ pageSize: 10 }} />
        )}
      </Card>

      {/* 注册来源弹窗 */}
      <Modal
        title="注册知识来源"
        open={registerModalVisible}
        onCancel={() => { setRegisterModalVisible(false); registerForm.resetFields(); }}
        onOk={() => registerForm.submit()}
        width={640}
      >
        <Form form={registerForm} layout="vertical" onFinish={handleRegister}>
          <Form.Item name="source_name" label="来源名称" rules={[{ required: true, message: "请输入来源名称" }]}>
            <Input placeholder="如：国家医保药品目录 2024 版" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="source_type" label="来源类型" rules={[{ required: true }]}>
                <Select options={SOURCE_TYPE_OPTIONS} placeholder="请选择" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="publisher" label="发布者">
                <Input placeholder="如：国家医保局" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="region" label="地区">
                <Input placeholder="如：CN" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="language" label="语言">
                <Input placeholder="如：zh-CN" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="authority_level" label="权威等级">
                <Select allowClear options={[
                  { value: "OFFICIAL", label: "官方" },
                  { value: "ACADEMIC", label: "学术" },
                  { value: "INDUSTRY", label: "行业" },
                  { value: "HOSPITAL", label: "院内" },
                ]} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="license_type" label="授权类型">
                <Select allowClear options={[
                  { value: "OPEN", label: "开放" },
                  { value: "RESTRICTED", label: "受限" },
                  { value: "COMMERCIAL", label: "商业" },
                  { value: "PROPRIETARY", label: "专有" },
                ]} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="license_scope" label="授权范围">
                <Select allowClear options={[
                  { value: "INTERNAL", label: "院内" },
                  { value: "REGIONAL", label: "区域" },
                  { value: "NATIONAL", label: "全国" },
                  { value: "GLOBAL", label: "全球" },
                ]} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="fetch_method" label="获取方式">
                <Select allowClear options={[
                  { value: "API", label: "API" },
                  { value: "FILE", label: "文件" },
                  { value: "MANUAL", label: "手动" },
                  { value: "CRAWLER", label: "爬虫" },
                ]} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="source_uri" label="来源 URI">
            <Input placeholder="https://..." />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 新建订阅弹窗 */}
      <Modal
        title="新建知识订阅"
        open={subModalVisible}
        onCancel={() => { setSubModalVisible(false); subForm.resetFields(); }}
        onOk={() => subForm.submit()}
      >
        <Form form={subForm} layout="vertical" onFinish={handleCreateSub}>
          <Form.Item name="topic_type" label="主题类型" rules={[{ required: true }]}>
            <Select options={TOPIC_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item name="topic_name" label="主题名称" rules={[{ required: true, message: "请输入主题名称" }]}>
            <Input placeholder="如：急性心肌梗死" />
          </Form.Item>
          <Form.Item name="topic_code" label="主题编码">
            <Input placeholder="如：ICD-I21" />
          </Form.Item>
          <Form.Item name="source_types" label="关注来源类型">
            <Select mode="multiple" options={SOURCE_TYPE_OPTIONS} placeholder="不选则关注全部" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="auto_sync" label="自动同步" initialValue={true}>
                <Select options={[{ value: true, label: "是" }, { value: false, label: "否" }]} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="sync_frequency" label="同步频率" initialValue="MANUAL">
                <Select options={[
                  { value: "DAILY", label: "每日" },
                  { value: "WEEKLY", label: "每周" },
                  { value: "MONTHLY", label: "每月" },
                  { value: "MANUAL", label: "手动" },
                ]} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* 来源详情弹窗 */}
      <Modal
        title={selectedSource?.source_name ?? "来源详情"}
        open={detailModalVisible}
        onCancel={() => { setDetailModalVisible(false); setSelectedSource(null); }}
        footer={null}
        width={640}
      >
        {selectedSource && (
          <Descriptions column={2} size="small" bordered>
            <Descriptions.Item label="编码">{selectedSource.source_code}</Descriptions.Item>
            <Descriptions.Item label="类型">{SOURCE_TYPE_OPTIONS.find((o) => o.value === selectedSource.source_type)?.label ?? selectedSource.source_type}</Descriptions.Item>
            <Descriptions.Item label="发布者">{selectedSource.publisher ?? "—"}</Descriptions.Item>
            <Descriptions.Item label="地区">{selectedSource.region ?? "—"}</Descriptions.Item>
            <Descriptions.Item label="语言">{selectedSource.language ?? "—"}</Descriptions.Item>
            <Descriptions.Item label="权威等级">{selectedSource.authority_level ?? "—"}</Descriptions.Item>
            <Descriptions.Item label="授权类型">{selectedSource.license?.license_type ?? "—"}</Descriptions.Item>
            <Descriptions.Item label="授权范围">{selectedSource.license?.license_scope ?? "—"}</Descriptions.Item>
            <Descriptions.Item label="允许再分发">{selectedSource.license?.redistribution_allowed ? "是" : "否"}</Descriptions.Item>
            <Descriptions.Item label="允许商用">{selectedSource.license?.commercial_use_allowed ? "是" : "否"}</Descriptions.Item>
            <Descriptions.Item label="获取方式">{selectedSource.fetch_method ?? "—"}</Descriptions.Item>
            <Descriptions.Item label="审核状态">{REVIEW_STATUS_MAP[selectedSource.review_status]?.label ?? selectedSource.review_status}</Descriptions.Item>
            <Descriptions.Item label="URI" span={2}>{selectedSource.source_uri ?? "—"}</Descriptions.Item>
            <Descriptions.Item label="描述" span={2}>{selectedSource.description ?? "—"}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default KnowledgePage;
