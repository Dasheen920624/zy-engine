import React, { useState, useEffect } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Drawer,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Timeline,
  Typography,
  message,
} from "antd";
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  StopOutlined,
  SwapOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import type {
  TodoTask,
  TodoSummary,
  BusinessType,
  TodoStatus,
  TodoPriority,
} from "../api/types";
import {
  fetchTodoTasks,
  fetchTodoSummary,
  approveTask,
  rejectTask,
  delegateTask,
} from "../api/workflow";
import { OrgContextSelector } from "../components";
import styles from "./workflowTodos.module.css";

const { Text, Paragraph } = Typography;
const { TextArea } = Input;

// 业务类型映射
const BUSINESS_TYPE_MAP: Record<BusinessType, { label: string; color: string }> = {
  REVIEW: { label: "审核", color: "blue" },
  PUBLISH: { label: "发布", color: "green" },
  ROLLBACK: { label: "回滚", color: "orange" },
  RECTIFY: { label: "整改", color: "red" },
  KNOWLEDGE: { label: "知识包", color: "purple" },
  COMPLIANCE: { label: "合规", color: "cyan" },
  SYNC: { label: "同步异常", color: "magenta" },
};

// 优先级映射
const PRIORITY_MAP: Record<TodoPriority, { label: string; color: string; icon: React.ReactNode }> = {
  URGENT: { label: "紧急", color: "red", icon: <ThunderboltOutlined /> },
  HIGH: { label: "高", color: "orange", icon: <ExclamationCircleOutlined /> },
  NORMAL: { label: "普通", color: "blue", icon: <ClockCircleOutlined /> },
  LOW: { label: "低", color: "default", icon: null },
};

// 状态映射
const STATUS_MAP: Record<TodoStatus, { label: string; color: string }> = {
  PENDING: { label: "待处理", color: "processing" },
  APPROVED: { label: "已通过", color: "success" },
  REJECTED: { label: "已驳回", color: "error" },
  CANCELLED: { label: "已取消", color: "default" },
  EXPIRED: { label: "已过期", color: "warning" },
};

// 模拟数据
const MOCK_TODOS: TodoTask[] = [
  {
    taskCode: "WF-20260519-001",
    businessType: "REVIEW",
    businessCode: "CFG-AMI-001",
    businessVersion: "1.0.0",
    title: "AMI推荐规则包审核",
    description: "配置包CFG-AMI-001 v1.0.0待审核发布",
    priority: "HIGH",
    status: "PENDING",
    assignedType: "ROLE",
    assignedTo: "MEDICAL_EXPERT",
    createdBy: "admin",
    createdTime: "2026-05-19T20:00:00+08:00",
    dueTime: "2026-05-20T18:00:00+08:00",
  },
  {
    taskCode: "WF-20260519-002",
    businessType: "PUBLISH",
    businessCode: "PATH-CHD-001",
    businessVersion: "2.1.0",
    title: "儿童哮喘路径发布审批",
    description: "路径PATH-CHD-001 v2.1.0待发布审批",
    priority: "NORMAL",
    status: "PENDING",
    assignedType: "USER",
    assignedTo: "zheng07",
    createdBy: "zhao01",
    createdTime: "2026-05-19T19:30:00+08:00",
    dueTime: "2026-05-21T18:00:00+08:00",
  },
  {
    taskCode: "WF-20260519-003",
    businessType: "SYNC",
    businessCode: "SYNC-HIS-20260519",
    title: "HIS用户同步异常处理",
    description: "HIS用户同步任务失败，需要人工处理",
    priority: "URGENT",
    status: "PENDING",
    assignedType: "ROLE",
    assignedTo: "IT_ADMIN",
    createdBy: "system",
    createdTime: "2026-05-19T21:00:00+08:00",
    dueTime: "2026-05-19T23:00:00+08:00",
  },
];

const MOCK_SUMMARY: TodoSummary = {
  totalPending: 3,
  urgentCount: 1,
  highCount: 1,
  normalCount: 1,
  lowCount: 0,
  overdueCount: 0,
  byBusinessType: {
    REVIEW: 1,
    PUBLISH: 1,
    SYNC: 1,
    ROLLBACK: 0,
    RECTIFY: 0,
    KNOWLEDGE: 0,
    COMPLIANCE: 0,
  },
};

const DEFAULT_SUMMARY: TodoSummary = {
  totalPending: 0,
  urgentCount: 0,
  highCount: 0,
  normalCount: 0,
  lowCount: 0,
  overdueCount: 0,
  byBusinessType: {
    REVIEW: 0,
    PUBLISH: 0,
    ROLLBACK: 0,
    RECTIFY: 0,
    KNOWLEDGE: 0,
    COMPLIANCE: 0,
    SYNC: 0,
  },
};

export default function WorkflowTodos() {
  const [todos, setTodos] = useState<TodoTask[]>([]);
  const [summary, setSummary] = useState<TodoSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedTodo, setSelectedTodo] = useState<TodoTask | null>(null);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [approveModalVisible, setApproveModalVisible] = useState(false);
  const [rejectModalVisible, setRejectModalVisible] = useState(false);
  const [delegateModalVisible, setDelegateModalVisible] = useState(false);
  const [filterStatus, setFilterStatus] = useState<TodoStatus | "">("");
  const [filterBusinessType, setFilterBusinessType] = useState<BusinessType | "">("");
  const [form] = Form.useForm();

  // Fetch todos and summary
  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [todoList, summaryData] = await Promise.all([
        fetchTodoTasks({
          status: filterStatus || undefined,
          businessType: filterBusinessType || undefined,
        }),
        fetchTodoSummary(),
      ]);
      setTodos(todoList);
      setSummary(summaryData);
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载失败");
      // Fallback to mock data on error
      setTodos(MOCK_TODOS);
      setSummary(MOCK_SUMMARY);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterStatus, filterBusinessType]);


  const filteredTodos = todos.filter((t) => {
    if (filterStatus && t.status !== filterStatus) return false;
    if (filterBusinessType && t.businessType !== filterBusinessType) return false;
    return true;
  });

  const showDetail = (todo: TodoTask) => {
    setSelectedTodo(todo);
    setDrawerVisible(true);
  };

  const handleApprove = async () => {
    if (!selectedTodo) return;
    try {
      await approveTask(selectedTodo.taskCode, {
        operatorId: "current-user", // TODO: get from auth context
        comment: form.getFieldValue("comment"),
      });
      message.success("审批通过");
      setApproveModalVisible(false);
      form.resetFields();
      fetchData(); // Refresh data
    } catch {
      message.error("审批失败");
    }
  };

  const handleReject = async () => {
    if (!selectedTodo) return;
    try {
      await rejectTask(selectedTodo.taskCode, {
        operatorId: "current-user", // TODO: get from auth context
        comment: form.getFieldValue("comment"),
      });
      message.success("已驳回");
      setRejectModalVisible(false);
      form.resetFields();
      fetchData(); // Refresh data
    } catch {
      message.error("驳回失败");
    }
  };

  const handleDelegate = async () => {
    if (!selectedTodo) return;
    try {
      await delegateTask(selectedTodo.taskCode, {
        operatorId: "current-user", // TODO: get from auth context
        delegateTo: form.getFieldValue("delegateTo"),
        comment: form.getFieldValue("comment"),
      });
      message.success("已转办");
      setDelegateModalVisible(false);
      form.resetFields();
      fetchData(); // Refresh data
    } catch {
      message.error("转办失败");
    }
  };

  const columns: ColumnsType<TodoTask> = [
    {
      title: "优先级",
      dataIndex: "priority",
      key: "priority",
      width: 80,
      render: (p: TodoPriority) => {
        const cfg = PRIORITY_MAP[p];
        return (
          <Tag color={cfg.color} icon={cfg.icon}>
            {cfg.label}
          </Tag>
        );
      },
      sorter: (a, b) => {
        const order: Record<string, number> = { URGENT: 0, HIGH: 1, NORMAL: 2, LOW: 3 };
        return (order[a.priority] ?? 99) - (order[b.priority] ?? 99);
      },
    },
    {
      title: "标题",
      dataIndex: "title",
      key: "title",
      ellipsis: true,
      render: (title, record) => (
        <a onClick={() => showDetail(record)}>{title}</a>
      ),
    },
    {
      title: "业务类型",
      dataIndex: "businessType",
      key: "businessType",
      width: 100,
      render: (t: BusinessType) => {
        const cfg = BUSINESS_TYPE_MAP[t];
        return <Tag color={cfg.color}>{cfg.label}</Tag>;
      },
    },
    {
      title: "业务编码",
      dataIndex: "businessCode",
      key: "businessCode",
      width: 160,
      ellipsis: true,
      render: (code, record) => (
        <Text code>
          {code}
          {record.businessVersion ? ` v${record.businessVersion}` : ""}
        </Text>
      ),
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (s: TodoStatus) => {
        const cfg = STATUS_MAP[s];
        return <Badge status={cfg.color as "success" | "processing" | "error" | "default" | "warning"} text={cfg.label} />;
      },
    },
    {
      title: "分配给",
      dataIndex: "assignedTo",
      key: "assignedTo",
      width: 120,
      render: (to, record) => (
        <Text>
          {record.assignedType === "ROLE" ? "角色: " : "用户: "}
          {to || "-"}
        </Text>
      ),
    },
    {
      title: "创建时间",
      dataIndex: "createdTime",
      key: "createdTime",
      width: 180,
      render: (t: string) => (t ? t.replace("T", " ").substring(0, 16) : "-"),
    },
    {
      title: "截止时间",
      dataIndex: "dueTime",
      key: "dueTime",
      width: 180,
      render: (t: string) => {
        if (!t) return "-";
        const isOverdue = new Date(t) < new Date();
        return (
          <Text type={isOverdue ? "danger" : undefined}>
            {t.replace("T", " ").substring(0, 16)}
          </Text>
        );
      },
    },
    {
      title: "操作",
      key: "action",
      width: 200,
      render: (_, record) => (
        <Space size="small">
          {record.status === "PENDING" && (
            <>
              <Button
                type="link"
                size="small"
                icon={<CheckCircleOutlined />}
                onClick={() => {
                  setSelectedTodo(record);
                  setApproveModalVisible(true);
                }}
              >
                通过
              </Button>
              <Button
                type="link"
                size="small"
                danger
                icon={<CloseCircleOutlined />}
                onClick={() => {
                  setSelectedTodo(record);
                  setRejectModalVisible(true);
                }}
              >
                驳回
              </Button>
              <Button
                type="link"
                size="small"
                icon={<SwapOutlined />}
                onClick={() => {
                  setSelectedTodo(record);
                  setDelegateModalVisible(true);
                }}
              >
                转办
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div className="page-header">
        <h1>待办中心</h1>
        <div className="subtitle">统一待办和审批工作流 · 审核、发布、回滚、整改、知识包、合规、同步异常</div>
      </div>

      {error && (
        <Alert
          message="加载失败"
          description={error}
          type="error"
          showIcon
          closable
          onClose={() => setError(null)}
          className={styles.alertSpacing}
        />
      )}

      {/* 统计卡片 */}
      <Row gutter={16} className={styles.statsRow}>
        <Col span={6}>
          <Card>
            <Statistic
              title="待处理"
              value={(summary || DEFAULT_SUMMARY).totalPending}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: "var(--mk-primary)" }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="紧急"
              value={(summary || DEFAULT_SUMMARY).urgentCount}
              prefix={<ThunderboltOutlined />}
              valueStyle={{ color: "var(--mk-danger)" }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="高优先级"
              value={(summary || DEFAULT_SUMMARY).highCount}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: "var(--mk-warning)" }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已过期"
              value={(summary || DEFAULT_SUMMARY).overdueCount}
              prefix={<StopOutlined />}
              valueStyle={{ color: "var(--mk-danger)" }}
            />
          </Card>
        </Col>
      </Row>

      {/* 筛选栏 */}
      <Card className={styles.filterCard}>
        <Space>
          <OrgContextSelector />
          <Select
            placeholder="状态筛选"
            allowClear
            className={styles.filterSelect}
            value={filterStatus || undefined}
            onChange={(v) => setFilterStatus(v || "")}
            options={Object.entries(STATUS_MAP).map(([k, v]) => ({
              value: k,
              label: v.label,
            }))}
          />
          <Select
            placeholder="业务类型"
            allowClear
            className={styles.filterSelect}
            value={filterBusinessType || undefined}
            onChange={(v) => setFilterBusinessType(v || "")}
            options={Object.entries(BUSINESS_TYPE_MAP).map(([k, v]) => ({
              value: k,
              label: v.label,
            }))}
          />
          <Button icon={<ReloadOutlined />} onClick={fetchData}>刷新</Button>
        </Space>
      </Card>

      {/* 待办列表 */}
      <Card title="待办列表">
        <Table
          columns={columns}
          dataSource={filteredTodos}
          rowKey="taskCode"
          pagination={{ pageSize: 10 }}
          size="middle"
          loading={loading}
        />
      </Card>

      {/* 详情抽屉 */}
      <Drawer
        title="待办详情"
        width={600}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
        extra={
          selectedTodo?.status === "PENDING" && (
            <Space>
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                onClick={() => {
                  setDrawerVisible(false);
                  setApproveModalVisible(true);
                }}
              >
                通过
              </Button>
              <Button
                danger
                icon={<CloseCircleOutlined />}
                onClick={() => {
                  setDrawerVisible(false);
                  setRejectModalVisible(true);
                }}
              >
                驳回
              </Button>
              <Button
                icon={<SwapOutlined />}
                onClick={() => {
                  setDrawerVisible(false);
                  setDelegateModalVisible(true);
                }}
              >
                转办
              </Button>
            </Space>
          )
        }
      >
        {selectedTodo && (
          <div>
            <Card size="small" className={styles.detailCard}>
              <Space direction="vertical" className={styles.fullWidth}>
                <div>
                  <Tag color={BUSINESS_TYPE_MAP[selectedTodo.businessType].color}>
                    {BUSINESS_TYPE_MAP[selectedTodo.businessType].label}
                  </Tag>
                  <Tag color={PRIORITY_MAP[selectedTodo.priority].color}>
                    {PRIORITY_MAP[selectedTodo.priority].label}
                  </Tag>
                  <Badge
                    status={STATUS_MAP[selectedTodo.status].color as "success" | "processing" | "error" | "default" | "warning"}
                    text={STATUS_MAP[selectedTodo.status].label}
                  />
                </div>
                <Text strong className={styles.detailTitle}>
                  {selectedTodo.title}
                </Text>
                <Paragraph type="secondary">{selectedTodo.description}</Paragraph>
                <Row gutter={16}>
                  <Col span={12}>
                    <Text type="secondary">业务编码：</Text>
                    <Text code>
                      {selectedTodo.businessCode}
                      {selectedTodo.businessVersion
                        ? ` v${selectedTodo.businessVersion}`
                        : ""}
                    </Text>
                  </Col>
                  <Col span={12}>
                    <Text type="secondary">创建人：</Text>
                    <Text>{selectedTodo.createdBy}</Text>
                  </Col>
                </Row>
                <Row gutter={16}>
                  <Col span={12}>
                    <Text type="secondary">分配给：</Text>
                    <Text>
                      {selectedTodo.assignedType === "ROLE" ? "角色: " : "用户: "}
                      {selectedTodo.assignedTo || "-"}
                    </Text>
                  </Col>
                  <Col span={12}>
                    <Text type="secondary">截止时间：</Text>
                    <Text>
                      {selectedTodo.dueTime
                        ? selectedTodo.dueTime.replace("T", " ").substring(0, 16)
                        : "-"}
                    </Text>
                  </Col>
                </Row>
              </Space>
            </Card>

            <Card title="审批记录" size="small">
              <Timeline
                items={[
                  {
                    color: "blue",
                    children: (
                      <>
                        <Text strong>提交审核</Text>
                        <br />
                        <Text type="secondary">admin · 2026-05-19 20:00</Text>
                      </>
                    ),
                  },
                ]}
              />
            </Card>
          </div>
        )}
      </Drawer>

      {/* 审批通过弹窗 */}
      <Modal
        title="审批通过"
        open={approveModalVisible}
        onOk={handleApprove}
        onCancel={() => setApproveModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="审批意见" name="comment">
            <TextArea rows={3} placeholder="请输入审批意见（可选）" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 驳回弹窗 */}
      <Modal
        title="驳回"
        open={rejectModalVisible}
        onOk={handleReject}
        onCancel={() => setRejectModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="驳回原因"
            name="comment"
            rules={[{ required: true, message: "请输入驳回原因" }]}
          >
            <TextArea rows={3} placeholder="请输入驳回原因" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 转办弹窗 */}
      <Modal
        title="转办"
        open={delegateModalVisible}
        onOk={handleDelegate}
        onCancel={() => setDelegateModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="转办给"
            name="delegateTo"
            rules={[{ required: true, message: "请选择转办目标" }]}
          >
            <Select
              placeholder="选择用户"
              options={[
                { value: "zhao01", label: "赵医生" },
                { value: "qian02", label: "钱护士" },
                { value: "sun03", label: "孙医保" },
                { value: "li04", label: "李信息" },
                { value: "zheng07", label: "郑专家" },
              ]}
            />
          </Form.Item>
          <Form.Item label="转办说明" name="comment">
            <TextArea rows={2} placeholder="请输入转办说明（可选）" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
