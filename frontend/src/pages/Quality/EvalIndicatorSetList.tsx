import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
  Popconfirm,
} from "antd";
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  StopOutlined,
} from "@ant-design/icons";
import { SourceInfo } from "../../components/SourceInfo/SourceInfo";
import { StatusBadge } from "../../components/StatusBadge/StatusBadge";
import type { StatusKey } from "../../components/StatusBadge/StatusBadge.types";
import {
  listEvalSets,
  createEvalSet,
  updateEvalSet,
  publishEvalSet,
  deprecateEvalSet,
  listEvalIndicators,
  createEvalIndicator,
  updateEvalIndicator,
  deleteEvalIndicator,
  type EvalIndicatorSet,
  type EvalIndicator,
} from "../../api/eval";

const { Text } = Typography;

// ==================== 常量 ====================

const SUBJECT_TYPE_OPTIONS = [
  { value: "EMR", label: "病历质量" },
  { value: "INSURANCE", label: "医保合规" },
  { value: "PATHWAY", label: "临床路径执行" },
  { value: "DEPARTMENT", label: "科室综合" },
  { value: "CONFIG", label: "配置质量" },
];

const INDICATOR_TYPE_OPTIONS = [
  { value: "SCORE", label: "评分" },
  { value: "RATE", label: "比率" },
  { value: "COUNT", label: "计数" },
  { value: "BOOLEAN", label: "是否达标" },
];

const STATUS_MAP: Record<string, { color: string; label: string }> = {
  DRAFT: { color: "default", label: "草稿" },
  PUBLISHED: { color: "green", label: "已发布" },
  DEPRECATED: { color: "red", label: "已废弃" },
};

// ==================== 指标集列表页 ====================

const EvalIndicatorSetList: React.FC = () => {
  const [sets, setSets] = useState<EvalIndicatorSet[]>([]);
  const [loading, setLoading] = useState(false);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editingSet, setEditingSet] = useState<EvalIndicatorSet | null>(null);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [selectedSet, setSelectedSet] = useState<EvalIndicatorSet | null>(null);
  const [indicators, setIndicators] = useState<EvalIndicator[]>([]);
  const [indicatorModalVisible, setIndicatorModalVisible] = useState(false);
  const [editingIndicator, setEditingIndicator] = useState<EvalIndicator | null>(null);
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const [indicatorForm] = Form.useForm();

  const fetchSets = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listEvalSets();
      setSets(data);
    } catch {
      message.error("获取指标集列表失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSets();
  }, [fetchSets]);

  // 创建指标集
  const handleCreate = async (values: Record<string, unknown>) => {
    try {
      await createEvalSet(values as Parameters<typeof createEvalSet>[0]);
      message.success("指标集创建成功");
      setCreateModalVisible(false);
      createForm.resetFields();
      fetchSets();
    } catch {
      message.error("创建失败");
    }
  };

  // 编辑指标集
  const handleEdit = async (values: Record<string, unknown>) => {
    if (!editingSet) return;
    try {
      await updateEvalSet(editingSet.set_code, values);
      message.success("指标集更新成功");
      setEditModalVisible(false);
      editForm.resetFields();
      fetchSets();
    } catch {
      message.error("更新失败");
    }
  };

  // 发布
  const handlePublish = async (setCode: string) => {
    try {
      await publishEvalSet(setCode);
      message.success("指标集已发布");
      fetchSets();
    } catch {
      message.error("发布失败");
    }
  };

  // 废弃
  const handleDeprecate = async (setCode: string) => {
    try {
      await deprecateEvalSet(setCode);
      message.success("指标集已废弃");
      fetchSets();
    } catch {
      message.error("废弃失败");
    }
  };

  // 查看详情
  const handleViewDetail = async (set: EvalIndicatorSet) => {
    setSelectedSet(set);
    setDetailModalVisible(true);
    try {
      const data = await listEvalIndicators(set.set_code);
      setIndicators(data);
    } catch {
      setIndicators([]);
    }
  };

  // 创建指标
  const handleCreateIndicator = async (values: Record<string, unknown>) => {
    if (!selectedSet) return;
    try {
      await createEvalIndicator(selectedSet.set_code, values as Parameters<typeof createEvalIndicator>[1]);
      message.success("指标创建成功");
      setIndicatorModalVisible(false);
      indicatorForm.resetFields();
      const data = await listEvalIndicators(selectedSet.set_code);
      setIndicators(data);
    } catch {
      message.error("创建指标失败");
    }
  };

  // 编辑指标
  const handleEditIndicator = async (values: Record<string, unknown>) => {
    if (!editingIndicator) return;
    try {
      await updateEvalIndicator(editingIndicator.indicator_code, values);
      message.success("指标更新成功");
      setIndicatorModalVisible(false);
      indicatorForm.resetFields();
      setEditingIndicator(null);
      if (selectedSet) {
        const data = await listEvalIndicators(selectedSet.set_code);
        setIndicators(data);
      }
    } catch {
      message.error("更新指标失败");
    }
  };

  // 删除指标
  const handleDeleteIndicator = async (indicatorCode: string) => {
    try {
      await deleteEvalIndicator(indicatorCode);
      message.success("指标已删除");
      if (selectedSet) {
        const data = await listEvalIndicators(selectedSet.set_code);
        setIndicators(data);
      }
    } catch {
      message.error("删除指标失败");
    }
  };

  const setColumns = [
    {
      title: "指标集编码",
      dataIndex: "set_code",
      key: "set_code",
      width: 160,
    },
    {
      title: "名称",
      dataIndex: "set_name",
      key: "set_name",
      width: 200,
    },
    {
      title: "评估对象",
      dataIndex: "subject_type",
      key: "subject_type",
      width: 120,
      render: (v: string) => SUBJECT_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? v,
    },
    {
      title: "版本",
      dataIndex: "version",
      key: "version",
      width: 80,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (v: string) => {
        const statusMap: Record<string, StatusKey> = {
          DRAFT: "draft",
          PUBLISHED: "published",
          DEPRECATED: "retired",
        };
        return <StatusBadge status={statusMap[v] ?? "draft"} text={STATUS_MAP[v]?.label ?? v} />;
      },
    },
    {
      title: "来源",
      key: "source",
      width: 160,
      render: (_: unknown, record: EvalIndicatorSet) => (
        <SourceInfo
          source={{
            documentName: record.source?.document_code ?? "—",
            documentId: record.source?.document_code ?? "",
          }}
          citation={record.source?.citation_id ? { id: record.source.citation_id, excerpt: record.source.binding_type ?? "" } : undefined}
        />
      ),
    },
    {
      title: "操作",
      key: "action",
      width: 280,
      render: (_: unknown, record: EvalIndicatorSet) => (
        <Space size="small">
          <Button size="small" onClick={() => handleViewDetail(record)}>
            详情
          </Button>
          {record.status === "DRAFT" && (
            <>
              <Button
                size="small"
                icon={<EditOutlined />}
                onClick={() => {
                  setEditingSet(record);
                  editForm.setFieldsValue({
                    set_name: record.set_name,
                    subject_type: record.subject_type,
                    description: record.description,
                    document_code: record.source?.document_code,
                    citation_id: record.source?.citation_id,
                    binding_type: record.source?.binding_type,
                  });
                  setEditModalVisible(true);
                }}
              >
                编辑
              </Button>
              <Button
                size="small"
                type="primary"
                icon={<CheckCircleOutlined />}
                onClick={() => handlePublish(record.set_code)}
              >
                发布
              </Button>
            </>
          )}
          {record.status === "PUBLISHED" && (
            <Popconfirm title="确定废弃此指标集？" onConfirm={() => handleDeprecate(record.set_code)}>
              <Button size="small" danger icon={<StopOutlined />}>
                废弃
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  const indicatorColumns = [
    {
      title: "指标编码",
      dataIndex: "indicator_code",
      key: "indicator_code",
      width: 150,
    },
    {
      title: "名称",
      dataIndex: "indicator_name",
      key: "indicator_name",
      width: 180,
    },
    {
      title: "类型",
      dataIndex: "indicator_type",
      key: "indicator_type",
      width: 100,
      render: (v: string) => INDICATOR_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? v,
    },
    {
      title: "权重",
      dataIndex: "weight",
      key: "weight",
      width: 80,
      render: (v: number) => <Text>{v}</Text>,
    },
    {
      title: "满分",
      dataIndex: "max_value",
      key: "max_value",
      width: 80,
    },
    {
      title: "单位",
      dataIndex: "unit",
      key: "unit",
      width: 60,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 80,
      render: (v: string) => {
        const cfg = STATUS_MAP[v] ?? { color: "default", label: v };
        return <Tag color={cfg.color}>{cfg.label}</Tag>;
      },
    },
    {
      title: "来源",
      key: "source",
      width: 140,
      render: (_: unknown, record: EvalIndicator) => (
        <SourceInfo
          source={{
            documentName: record.source?.document_code ?? "—",
            documentId: record.source?.document_code ?? "",
          }}
          citation={record.source?.citation_id ? { id: record.source.citation_id, excerpt: record.source.binding_type ?? "" } : undefined}
        />
      ),
    },
    {
      title: "操作",
      key: "action",
      width: 140,
      render: (_: unknown, record: EvalIndicator) => {
        if (selectedSet?.status !== "DRAFT") return null;
        return (
          <Space size="small">
            <Button
              size="small"
              icon={<EditOutlined />}
              onClick={() => {
                setEditingIndicator(record);
                indicatorForm.setFieldsValue({
                  indicator_name: record.indicator_name,
                  indicator_type: record.indicator_type,
                  weight: record.weight,
                  max_value: record.max_value,
                  threshold_expression: record.threshold_expression,
                  risk_level_mapping: record.risk_level_mapping,
                  calc_expression: record.calc_expression,
                  unit: record.unit,
                  description: record.description,
                  document_code: record.source?.document_code,
                  citation_id: record.source?.citation_id,
                  binding_type: record.source?.binding_type,
                });
                setIndicatorModalVisible(true);
              }}
            />
            <Popconfirm title="确定删除此指标？" onConfirm={() => handleDeleteIndicator(record.indicator_code)}>
              <Button size="small" danger icon={<DeleteOutlined />} />
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card
        title="评估指标集"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateModalVisible(true)}>
            新建指标集
          </Button>
        }
      >
        <Table
          rowKey="set_code"
          columns={setColumns}
          dataSource={sets}
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      {/* 创建指标集弹窗 */}
      <Modal
        title="新建指标集"
        open={createModalVisible}
        onCancel={() => { setCreateModalVisible(false); createForm.resetFields(); }}
        onOk={() => createForm.submit()}
      >
        <Form form={createForm} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="set_name" label="名称" rules={[{ required: true, message: "请输入指标集名称" }]}>
            <Input placeholder="如：病历质量评估指标集" />
          </Form.Item>
          <Form.Item name="subject_type" label="评估对象" rules={[{ required: true, message: "请选择评估对象" }]}>
            <Select options={SUBJECT_TYPE_OPTIONS} placeholder="请选择" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} placeholder="指标集说明" />
          </Form.Item>
          <Form.Item name="document_code" label="来源文档编码">
            <Input placeholder="如：GUIDE-2024-001" />
          </Form.Item>
          <Form.Item name="citation_id" label="来源引用ID">
            <Input placeholder="如：CIT-001" />
          </Form.Item>
          <Form.Item name="binding_type" label="绑定类型">
            <Select allowClear placeholder="请选择" options={[
              { value: "EVIDENCE", label: "证据" },
              { value: "REFERENCE", label: "参考" },
              { value: "DERIVATION", label: "衍生" },
              { value: "COMPLIANCE", label: "合规" },
            ]} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 编辑指标集弹窗 */}
      <Modal
        title="编辑指标集"
        open={editModalVisible}
        onCancel={() => { setEditModalVisible(false); editForm.resetFields(); setEditingSet(null); }}
        onOk={() => editForm.submit()}
      >
        <Form form={editForm} layout="vertical" onFinish={handleEdit}>
          <Form.Item name="set_name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="subject_type" label="评估对象" rules={[{ required: true }]}>
            <Select options={SUBJECT_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="document_code" label="来源文档编码">
            <Input />
          </Form.Item>
          <Form.Item name="citation_id" label="来源引用ID">
            <Input />
          </Form.Item>
          <Form.Item name="binding_type" label="绑定类型">
            <Select allowClear options={[
              { value: "EVIDENCE", label: "证据" },
              { value: "REFERENCE", label: "参考" },
              { value: "DERIVATION", label: "衍生" },
              { value: "COMPLIANCE", label: "合规" },
            ]} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 指标集详情弹窗 */}
      <Modal
        title={selectedSet ? `${selectedSet.set_name} - 指标列表` : "指标列表"}
        open={detailModalVisible}
        onCancel={() => { setDetailModalVisible(false); setSelectedSet(null); setIndicators([]); }}
        width={900}
        footer={selectedSet?.status === "DRAFT" ? (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingIndicator(null); indicatorForm.resetFields(); setIndicatorModalVisible(true); }}>
            新增指标
          </Button>
        ) : null}
      >
        {selectedSet && (
          <div style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={8}><Text type="secondary">编码：</Text>{selectedSet.set_code}</Col>
              <Col span={8}><Text type="secondary">版本：</Text>{selectedSet.version}</Col>
              <Col span={8}>
                <Text type="secondary">状态：</Text>
                <Tag color={STATUS_MAP[selectedSet.status]?.color}>{STATUS_MAP[selectedSet.status]?.label}</Tag>
              </Col>
            </Row>
          </div>
        )}
        <Table
          rowKey="indicator_code"
          columns={indicatorColumns}
          dataSource={indicators}
          pagination={false}
          size="small"
        />
      </Modal>

      {/* 新增/编辑指标弹窗 */}
      <Modal
        title={editingIndicator ? "编辑指标" : "新增指标"}
        open={indicatorModalVisible}
        onCancel={() => { setIndicatorModalVisible(false); indicatorForm.resetFields(); setEditingIndicator(null); }}
        onOk={() => indicatorForm.submit()}
        width={600}
      >
        <Form
          form={indicatorForm}
          layout="vertical"
          onFinish={editingIndicator ? handleEditIndicator : handleCreateIndicator}
        >
          <Form.Item name="indicator_name" label="指标名称" rules={[{ required: true, message: "请输入指标名称" }]}>
            <Input placeholder="如：病历书写完整率" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="indicator_type" label="指标类型" rules={[{ required: true }]}>
                <Select options={INDICATOR_TYPE_OPTIONS} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="unit" label="单位">
                <Input placeholder="如 %、分、次" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="weight" label="权重" initialValue={1}>
                <InputNumber min={0} step={0.1} style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="max_value" label="满分" initialValue={100}>
                <InputNumber min={0} style={{ width: "100%" }} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="calc_expression" label="计算表达式">
            <Input placeholder="如：hit_count / total_count * 100" />
          </Form.Item>
          <Form.Item name="threshold_expression" label="阈值表达式">
            <Input placeholder="如：score >= 60" />
          </Form.Item>
          <Form.Item name="risk_level_mapping" label="风险等级映射">
            <Input.TextArea rows={2} placeholder='如：{"HIGH":"score<60","MEDIUM":"score<80"}' />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="document_code" label="来源文档编码">
            <Input />
          </Form.Item>
          <Form.Item name="citation_id" label="来源引用ID">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default EvalIndicatorSetList;
