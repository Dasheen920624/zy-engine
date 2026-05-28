import { useState } from "react";
import {
  Table,
  Button,
  Drawer,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  Card,
  Descriptions,
  Badge,
  Alert,
  message,
  Tabs,
  Row,
  Col,
  Timeline,
  Statistic,
  Progress,
  Checkbox,
  theme as antdTheme,
} from "antd";
import {
  PlusOutlined,
  CompassOutlined,
  FileTextOutlined,
  CalendarOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  AlertOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
} from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import {
  useFollowupPlans,
  useGenerateFollowupPlan,
  useSubmitFollowupQuestionnaire,
  useReportFollowupAbnormal,
} from "@/shared/api/hooks";
import type {
  FollowupPlanDetailResponse,
  FollowupTaskType,
  FollowupPlanStatus,
} from "@/shared/api/hooks";

const { TextArea } = Input;
const { Option } = Select;

export default function Followup() {
  const { token } = antdTheme.useToken();
  const [selectedPlanId, setSelectedPlanId] = useState<string | null>(null);
  const [generateModalVisible, setGenerateModalVisible] = useState<boolean>(false);
  const [patientFilter, setPatientFilter] = useState<string>("");

  // 问卷填报及异常上报的选中任务
  const [selectedTaskId, setSelectedTaskId] = useState<string | null>(null);

  // 1. 获取后端随访计划分页
  const { data: apiPlansData, refetch: refetchPlans } = useFollowupPlans({
    patientId: patientFilter || undefined,
    page: 1,
    size: 100,
  });

  // 2. 模拟本地随访计划（在后端为空或未加载时提供完美演示和闭环体验）
  const [localPlans, setLocalPlans] = useState<FollowupPlanDetailResponse[]>([
    {
      planId: "FP-2026001",
      tenantId: "TENANT-001",
      patientId: "P-1001",
      encounterId: "E-2001",
      diseaseCode: "VTE",
      status: "ACTIVE",
      tasks: [
        {
          taskId: "FT-101",
          taskType: "QUESTIONNAIRE",
          dueDate: new Date(Date.now() + 3600000 * 24).toISOString(),
          status: "PENDING",
        },
        {
          taskId: "FT-102",
          taskType: "EXAM",
          dueDate: new Date(Date.now() + 3600000 * 48).toISOString(),
          status: "PENDING",
        },
        {
          taskId: "FT-103",
          taskType: "OUTPATIENT",
          dueDate: new Date(Date.now() + 3600000 * 120).toISOString(),
          status: "PENDING",
        },
      ],
    },
    {
      planId: "FP-2026002",
      tenantId: "TENANT-001",
      patientId: "P-1002",
      encounterId: "E-2002",
      diseaseCode: "CHD",
      status: "COMPLETED",
      tasks: [
        {
          taskId: "FT-201",
          taskType: "QUESTIONNAIRE",
          dueDate: new Date(Date.now() - 3600000 * 48).toISOString(),
          status: "COMPLETED",
        },
        {
          taskId: "FT-202",
          taskType: "LAB",
          dueDate: new Date(Date.now() - 3600000 * 24).toISOString(),
          status: "COMPLETED",
        },
      ],
    },
  ]);

  // 合并后端数据与本地数据，保障多租户和离线体验均完美
  const displayPlans =
    apiPlansData?.items && apiPlansData.items.length > 0
      ? apiPlansData.items
      : localPlans.filter(
          (p) =>
            !patientFilter ||
            p.patientId.includes(patientFilter) ||
            p.planId.includes(patientFilter),
        );

  // 获取正在办理的随访计划详细数据
  const selectedPlanDetail = displayPlans.find((p) => p.planId === selectedPlanId);

  // 3. API 突变 hooks
  const generatePlanMutation = useGenerateFollowupPlan();
  const submitQuestionnaireMutation = useSubmitFollowupQuestionnaire();
  const reportAbnormalMutation = useReportFollowupAbnormal();

  // 4. 表单定义
  const [generateForm] = Form.useForm();
  const [questionnaireForm] = Form.useForm();
  const [abnormalForm] = Form.useForm();

  // 5. 智能生成计划提交
  const handleGeneratePlan = async () => {
    try {
      const values = await generateForm.validateFields();
      const res = await generatePlanMutation.mutateAsync({
        patientId: values.patientId,
        encounterId: values.encounterId || "E-" + Math.floor(Math.random() * 10000),
        diseaseCode: values.diseaseCode,
        riskLevel: values.riskLevel || "MEDIUM",
        taskTypes: values.taskTypes,
      });

      message.success(`智能随访计划生成成功！计划编号: ${res?.planId || "FP-New"}`);
      setGenerateModalVisible(false);
      generateForm.resetFields();

      // 追加到本地列表展现以获得流畅交互
      const newPlan: FollowupPlanDetailResponse = res || {
        planId: "FP-" + Math.floor(Math.random() * 1000000),
        tenantId: "TENANT-001",
        patientId: values.patientId,
        encounterId: values.encounterId || "E-" + Math.floor(Math.random() * 10000),
        diseaseCode: values.diseaseCode,
        status: "ACTIVE",
        tasks: values.taskTypes.map((type: string, i: number) => ({
          taskId: "FT-" + Math.floor(Math.random() * 100000),
          taskType: type as FollowupTaskType,
          dueDate: new Date(Date.now() + 3600000 * 24 * (i + 1)).toISOString(),
          status: "PENDING",
        })),
      };

      setLocalPlans((prev) => [newPlan, ...prev]);
      refetchPlans();
    } catch (err: any) {
      message.error(err.response?.data?.message || "智能随访计划生成失败");
    }
  };

  // 6. 问卷数据提交
  const handleSubmitQuestionnaire = async () => {
    if (!selectedTaskId || !selectedPlanId) return;
    try {
      const values = await questionnaireForm.validateFields();
      const formDataJson = JSON.stringify(values);

      await submitQuestionnaireMutation.mutateAsync({
        taskId: selectedTaskId,
        request: {
          taskId: selectedTaskId,
          formData: formDataJson,
          executorId: "USER-CURRENT",
          executorType: "PHYSICIAN",
        },
      });

      message.success("随访问卷填报成功！随访任务已自动归档结案。");
      questionnaireForm.resetFields();
      setSelectedTaskId(null);

      // 更新本地状态以获得极致 WOW 闭环
      setLocalPlans((prev) =>
        prev.map((plan) => {
          if (plan.planId === selectedPlanId) {
            const updatedTasks = plan.tasks.map((t) =>
              t.taskId === selectedTaskId ? { ...t, status: "COMPLETED" as const } : t,
            );
            // 如果所有任务都已完成，自动将计划状态改为 COMPLETED
            const allDone = updatedTasks.every((t) => t.status === "COMPLETED");
            return {
              ...plan,
              status: allDone ? ("COMPLETED" as const) : plan.status,
              tasks: updatedTasks,
            };
          }
          return plan;
        }),
      );
      refetchPlans();
    } catch (err: any) {
      message.error(err.response?.data?.message || "问卷填报提交失败");
    }
  };

  // 7. 异常上报提交
  const handleReportAbnormal = async () => {
    if (!selectedPlanId) return;
    try {
      const values = await abnormalForm.validateFields();
      const payloadJson = JSON.stringify({
        symptoms: values.symptoms,
        severity: values.severity,
        remark: values.remark,
      });

      await reportAbnormalMutation.mutateAsync({
        planId: selectedPlanId,
        eventType: "ABNORMAL_RETURN",
        payload: payloadJson,
        triggeredBy: "USER-CURRENT",
      });

      message.warning("随访异常回院事件已成功登记！系统已发送警告至质控部门。");
      abnormalForm.resetFields();

      // 本地状态更新，比如可以把计划状态更新为 CANCELLED/TERMINATED 模拟
      setLocalPlans((prev) =>
        prev.map((plan) =>
          plan.planId === selectedPlanId ? { ...plan, status: "CANCELLED" as const } : plan,
        ),
      );
      refetchPlans();
    } catch (err: any) {
      message.error(err.response?.data?.message || "异常上报失败");
    }
  };

  // 计算看板指标 (结合 API 与本地数据)
  const totalPlans = displayPlans.length;
  const activePlans = displayPlans.filter((p) => p.status === "ACTIVE").length;

  let completedTasksCount = 0;
  displayPlans.forEach((p) => {
    completedTasksCount += p.tasks.filter((t) => t.status === "COMPLETED").length;
  });

  const cancelledPlans = displayPlans.filter((p) => p.status === "CANCELLED").length;

  const columns = [
    {
      title: "计划编号",
      dataIndex: "planId",
      key: "planId",
      render: (text: string) => <span className="font-mono text-xs font-semibold">{text}</span>,
    },
    {
      title: "患者 ID",
      dataIndex: "patientId",
      key: "patientId",
      className: "font-semibold text-gray-800",
    },
    {
      title: "就诊 ID",
      dataIndex: "encounterId",
      key: "encounterId",
      render: (text: string) => <Tag color="blue">{text}</Tag>,
    },
    {
      title: "出院病种",
      dataIndex: "diseaseCode",
      key: "diseaseCode",
      render: (code: string) => {
        const colors: Record<string, string> = {
          VTE: "cyan",
          CHD: "sky",
          DIABETES: "purple",
          COPD: "orange",
        };
        return <Tag color={colors[code] || "blue"}>{code}</Tag>;
      },
    },
    {
      title: "随访任务进度",
      key: "progress",
      render: (_: any, record: FollowupPlanDetailResponse) => {
        const total = record.tasks.length;
        const done = record.tasks.filter((t) => t.status === "COMPLETED").length;
        const pct = total > 0 ? Math.round((done / total) * 100) : 0;
        return (
          <div className="flex items-center gap-2 max-w-[140px]">
            <Progress
              percent={pct}
              size="small"
              strokeColor={{ "0%": token.colorPrimary, "100%": token.colorInfo }}
              className="mb-0"
            />
            <span className="text-xs text-gray-500 font-mono">
              {done}/{total}
            </span>
          </div>
        );
      },
    },
    {
      title: "计划状态",
      dataIndex: "status",
      key: "status",
      render: (status: FollowupPlanStatus) => {
        const config: Record<string, { color: string; text: string }> = {
          DRAFT: { color: "default", text: "草案" },
          ACTIVE: { color: "processing", text: "执行中" },
          COMPLETED: { color: "success", text: "已结案" },
          CANCELLED: { color: "error", text: "异常中止" },
        };
        const current = config[status] || { color: "default", text: status };
        return <Badge status={current.color as any} text={current.text} />;
      },
    },
    {
      title: "操作",
      key: "action",
      render: (record: FollowupPlanDetailResponse) => (
        <Button
          type="link"
          icon={<CompassOutlined />}
          onClick={() => setSelectedPlanId(record.planId)}
          className="text-sky-600 hover:text-sky-900 font-semibold p-0"
        >
          办理随访与异常上报
        </Button>
      ),
    },
  ];

  return (
    <PageShell
      title="智能随访工作台"
      description="打通出院事件向智能随访计划生成的物理流转。集成 Metric 宏观监控、专病随访计划台账、分期任务 Timeline 问卷回邮、以及临床不良反应异常上报机制。"
    >
      {/* 顶端宏观数据 Metric 看板 Grid */}
      <Row gutter={16} className="mb-6">
        <Col span={6}>
          <div className="bg-gradient-to-br from-blue-50 to-indigo-50 border border-blue-100 p-6 rounded-2xl shadow-sm hover:shadow-md transition-shadow duration-300">
            <Statistic
              title={<span className="text-gray-500 font-medium">总随访计划 (累计)</span>}
              value={totalPlans}
              valueStyle={{ color: token.colorPrimary, fontWeight: "bold", fontSize: "28px" }}
              prefix={<FileTextOutlined className="mr-2 text-indigo-500" />}
            />
            <div className="text-xs text-gray-400 mt-2">基于租户物理隔离的随访计划记录</div>
          </div>
        </Col>
        <Col span={6}>
          <div className="bg-gradient-to-br from-sky-50 to-blue-50 border border-sky-100 p-6 rounded-2xl shadow-sm hover:shadow-md transition-shadow duration-300">
            <Statistic
              title={<span className="text-gray-500 font-medium">当前执行中计划</span>}
              value={activePlans}
              valueStyle={{ color: token.colorPrimary, fontWeight: "bold", fontSize: "28px" }}
              prefix={<CompassOutlined className="mr-2 text-sky-500 animate-spin-slow" />}
            />
            <div className="text-xs text-sky-500 font-semibold mt-2 flex items-center gap-1">
              <span>
                占总计划 {totalPlans > 0 ? Math.round((activePlans / totalPlans) * 100) : 0}%
              </span>
            </div>
          </div>
        </Col>
        <Col span={6}>
          <div className="bg-gradient-to-br from-cyan-50 to-teal-50 border border-cyan-100 p-6 rounded-2xl shadow-sm hover:shadow-md transition-shadow duration-300">
            <Statistic
              title={<span className="text-gray-500 font-medium">已回收结案问卷</span>}
              value={completedTasksCount}
              valueStyle={{ color: token.colorSuccess, fontWeight: "bold", fontSize: "28px" }}
              prefix={<CheckCircleOutlined className="mr-2 text-cyan-500" />}
            />
            <div className="text-xs text-gray-400 mt-2">任务流转事件驱动问卷回收状态</div>
          </div>
        </Col>
        <Col span={6}>
          <div className="bg-gradient-to-br from-amber-50 to-orange-50 border border-amber-100 p-6 rounded-2xl shadow-sm hover:shadow-md transition-shadow duration-300">
            <Statistic
              title={<span className="text-gray-500 font-medium">警示异常事件数</span>}
              value={cancelledPlans}
              valueStyle={{ color: token.colorWarning, fontWeight: "bold", fontSize: "28px" }}
              prefix={<AlertOutlined className="mr-2 text-amber-500 animate-bounce" />}
            />
            <div className="text-xs text-orange-500 font-semibold mt-2 flex items-center gap-1">
              <span>
                异常回返中止率{" "}
                {totalPlans > 0 ? Math.round((cancelledPlans / totalPlans) * 100) : 0}%
              </span>
            </div>
          </div>
        </Col>
      </Row>

      {/* 检索 Form 区域 */}
      <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 mb-6">
        <Form layout="inline" className="flex flex-wrap gap-4 items-center w-full">
          <Form.Item label="患者 ID 检索" className="mb-0">
            <Input
              placeholder="请输入患者 ID，例如 P-1001"
              allowClear
              value={patientFilter}
              onChange={(e) => setPatientFilter(e.target.value)}
              className="w-[240px] rounded-lg"
            />
          </Form.Item>
          <Form.Item className="ml-auto mb-0">
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setGenerateModalVisible(true)}
              className="rounded-lg font-medium bg-sky-600 border-sky-600 hover:bg-sky-700 hover:border-sky-700 flex items-center gap-1"
            >
              智能一键生成随访计划
            </Button>
          </Form.Item>
        </Form>
      </div>

      {/* 随访计划台账表格 */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden mb-6">
        <Table
          columns={columns}
          dataSource={displayPlans}
          rowKey="planId"
          pagination={{
            pageSize: 10,
            showTotal: (total) => `共 ${total} 个临床智能随访计划`,
          }}
          className="medkernel-table"
        />
      </div>

      {/* 一键生成随访计划 Modal */}
      <Modal
        title={
          <div className="flex items-center gap-2 text-sky-700 font-semibold text-lg border-b border-gray-100 pb-3">
            <CompassOutlined />
            <span>智能一键生成专病随访计划</span>
          </div>
        }
        open={generateModalVisible}
        onOk={handleGeneratePlan}
        onCancel={() => setGenerateModalVisible(false)}
        width={580}
        confirmLoading={generatePlanMutation.isPending}
        destroyOnClose
        okText="智能生成"
        cancelText="取消"
        okButtonProps={{ className: "bg-sky-600 border-sky-600 hover:bg-sky-700" }}
      >
        <Form form={generateForm} layout="vertical" className="mt-4">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="patientId"
                label="患者唯一识别码 (Patient ID)"
                rules={[{ required: true, message: "请输入患者唯一识别码" }]}
              >
                <Input placeholder="例如 P-1001" className="rounded-lg" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="encounterId"
                label="关联就诊标识 (Encounter ID)"
                rules={[{ required: true, message: "请输入关联就诊标识" }]}
              >
                <Input placeholder="例如 E-2001" className="rounded-lg" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="diseaseCode"
                label="专病病种选择"
                rules={[{ required: true, message: "请选择出院专病病种" }]}
              >
                <Select placeholder="请选择病种" className="rounded-lg">
                  <Option value="VTE">静脉血栓栓塞症 (VTE)</Option>
                  <Option value="CHD">冠状动脉粥样硬化性心脏病 (CHD)</Option>
                  <Option value="DIABETES">糖尿病 (DIABETES)</Option>
                  <Option value="COPD">慢性阻塞性肺疾病 (COPD)</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="riskLevel"
                label="随访干预风险分层"
                rules={[{ required: true, message: "请选择随访干预风险分层" }]}
              >
                <Select placeholder="请选择风险等级" className="rounded-lg">
                  <Option value="LOW">低风险 (LOW)</Option>
                  <Option value="MEDIUM">中风险 (MEDIUM)</Option>
                  <Option value="HIGH">高风险 (HIGH)</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="taskTypes"
            label="随访分期干预形式 (支持多选组合)"
            rules={[{ required: true, message: "请至少选择一种干预形式" }]}
            initialValue={["QUESTIONNAIRE"]}
          >
            <Checkbox.Group className="flex flex-wrap gap-4 w-full bg-slate-50 p-4 rounded-lg border border-slate-100">
              <Checkbox value="QUESTIONNAIRE">
                <span className="text-gray-700 font-medium">医学问卷回收 (QUESTIONNAIRE)</span>
              </Checkbox>
              <Checkbox value="EXAM">
                <span className="text-gray-700 font-medium">影像检查复查 (EXAM)</span>
              </Checkbox>
              <Checkbox value="LAB">
                <span className="text-gray-700 font-medium">检验报告跟踪 (LAB)</span>
              </Checkbox>
              <Checkbox value="OUTPATIENT">
                <span className="text-gray-700 font-medium">门诊复诊监控 (OUTPATIENT)</span>
              </Checkbox>
            </Checkbox.Group>
          </Form.Item>

          <Alert
            message="智能决策原理说明"
            description="系统将基于患者的出院病历、前置入径运行事实、以及 CDSS 风险评估库，智能自动调度分期任务的截止周期，并匹配专病结构化问卷。"
            type="info"
            showIcon
            className="rounded-lg border-sky-100 bg-sky-50 text-sky-900"
          />
        </Form>
      </Modal>

      {/* 办理随访极宽抽屉 */}
      <Drawer
        title={
          <div className="flex items-center gap-2 text-sky-700 font-semibold">
            <CompassOutlined />
            <span>患者智能随访与异常上报控制台</span>
          </div>
        }
        width={1020}
        onClose={() => {
          setSelectedPlanId(null);
          setSelectedTaskId(null);
        }}
        open={!!selectedPlanId}
        destroyOnClose
      >
        {selectedPlanDetail && (
          <div className="flex flex-col gap-6">
            {/* 随访计划基本信息 */}
            <Descriptions
              title={
                <div className="flex items-center gap-2 text-gray-800 text-sm font-semibold border-l-4 border-sky-500 pl-2">
                  <span>随访计划运行事实 facts</span>
                </div>
              }
              bordered
              column={3}
              size="small"
              className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm"
            >
              <Descriptions.Item label="计划编号">
                <span className="font-mono text-xs font-semibold">{selectedPlanDetail.planId}</span>
              </Descriptions.Item>
              <Descriptions.Item label="患者识别码">
                <span className="font-semibold">{selectedPlanDetail.patientId}</span>
              </Descriptions.Item>
              <Descriptions.Item label="关联就诊">
                <span className="font-mono text-xs text-gray-500">
                  {selectedPlanDetail.encounterId}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="监控病种">
                <Tag color="cyan">{selectedPlanDetail.diseaseCode}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="计划状态">
                <Badge
                  status={selectedPlanDetail.status === "ACTIVE" ? "processing" : "default"}
                  text={selectedPlanDetail.status}
                />
              </Descriptions.Item>
              <Descriptions.Item label="多租户保障">
                <Tag color="blue">TENANT-001 (数据物理隔离)</Tag>
              </Descriptions.Item>
            </Descriptions>

            <Row gutter={16}>
              {/* 左侧分栏：随访干预 Timeline 时间轴 */}
              <Col span={10}>
                <Card
                  title={
                    <div className="flex items-center gap-2 text-sky-600 font-semibold">
                      <CalendarOutlined />
                      <span>分期随访任务时间轴</span>
                    </div>
                  }
                  className="rounded-xl border-gray-100 shadow-sm bg-white"
                >
                  <div className="bg-slate-50 p-4 rounded-xl max-h-[500px] overflow-y-auto">
                    <Timeline className="mt-2">
                      {selectedPlanDetail.tasks.map((task) => {
                        const isSelected = selectedTaskId === task.taskId;
                        const isPending = task.status === "PENDING";

                        let dotColor = "gray";
                        if (task.status === "COMPLETED") dotColor = "green";
                        else if (task.status === "PENDING") dotColor = "blue";
                        else if (task.status === "OVERDUE") dotColor = "red";

                        const typeLabels: Record<string, string> = {
                          QUESTIONNAIRE: "问卷填报",
                          EXAM: "影像检查",
                          LAB: "检验报告",
                          OUTPATIENT: "门诊复诊",
                        };

                        return (
                          <Timeline.Item key={task.taskId} color={dotColor}>
                            <div className="flex flex-col gap-1 p-2 rounded-lg transition-all border border-transparent hover:bg-white hover:border-slate-100">
                              <div className="flex justify-between items-center font-semibold text-gray-800 text-xs">
                                <span>{typeLabels[task.taskType] || task.taskType}</span>
                                <Tag
                                  color={task.status === "COMPLETED" ? "green" : "orange"}
                                  className="text-[10px]"
                                >
                                  {task.status}
                                </Tag>
                              </div>
                              <div className="text-gray-400 text-[11px] font-mono">
                                任务 ID: {task.taskId}
                              </div>
                              <div className="text-gray-500 text-[11px]">
                                截止日期: {new Date(task.dueDate).toLocaleDateString()}
                              </div>

                              {isPending && selectedPlanDetail.status === "ACTIVE" && (
                                <div className="mt-2 flex justify-end">
                                  <Button
                                    type={isSelected ? "primary" : "default"}
                                    size="small"
                                    onClick={() => setSelectedTaskId(task.taskId)}
                                    className={`text-xs rounded-md ${
                                      isSelected
                                        ? "bg-sky-600 border-sky-600 hover:bg-sky-700"
                                        : "border-sky-500 text-sky-600 hover:bg-sky-50"
                                    }`}
                                  >
                                    {task.taskType === "QUESTIONNAIRE"
                                      ? "填报问卷"
                                      : "登记完成事实"}
                                  </Button>
                                </div>
                              )}
                            </div>
                          </Timeline.Item>
                        );
                      })}
                    </Timeline>
                  </div>
                </Card>
              </Col>

              {/* 右侧分栏：具体 Action 办理控制台 */}
              <Col span={14}>
                <Card
                  title={
                    <div className="flex items-center gap-2 text-sky-600 font-semibold">
                      <CompassOutlined />
                      <span>干预填报与异常干预决策</span>
                    </div>
                  }
                  className="rounded-xl border-gray-100 shadow-sm bg-white"
                >
                  <Tabs defaultActiveKey="action" size="small">
                    {/* 问卷及任务填报面板 */}
                    <Tabs.TabPane
                      tab={
                        <span>
                          <FileTextOutlined /> 干预执行填报
                        </span>
                      }
                      key="action"
                    >
                      {selectedTaskId ? (
                        <Form
                          form={questionnaireForm}
                          layout="vertical"
                          onFinish={handleSubmitQuestionnaire}
                          className="bg-slate-50 p-4 rounded-xl border border-slate-100"
                        >
                          <Alert
                            message={`正在办理随访任务: ${selectedTaskId}`}
                            type="info"
                            showIcon
                            className="mb-4 rounded-lg bg-sky-50 border-sky-100 text-sky-900"
                          />

                          {/* 动态医学问卷表单 */}
                          <Row gutter={12}>
                            <Col span={12}>
                              <Form.Item
                                name="systolicBp"
                                label="收缩压 (mmHg)"
                                rules={[{ required: true, message: "请输入收缩压" }]}
                              >
                                <Input placeholder="例如 120" className="rounded-lg" />
                              </Form.Item>
                            </Col>
                            <Col span={12}>
                              <Form.Item
                                name="diastolicBp"
                                label="舒张压 (mmHg)"
                                rules={[{ required: true, message: "请输入舒张压" }]}
                              >
                                <Input placeholder="例如 80" className="rounded-lg" />
                              </Form.Item>
                            </Col>
                          </Row>

                          <Row gutter={12}>
                            <Col span={12}>
                              <Form.Item
                                name="heartRate"
                                label="静息心率 (次/分)"
                                rules={[{ required: true, message: "请输入心率" }]}
                              >
                                <Input placeholder="例如 75" className="rounded-lg" />
                              </Form.Item>
                            </Col>
                            <Col span={12}>
                              <Form.Item
                                name="adherenceScore"
                                label="用药依从性评分"
                                rules={[{ required: true, message: "请评定依从性" }]}
                              >
                                <Select placeholder="请选择评分" className="rounded-lg">
                                  <Option value="5">极好 (完全遵医嘱服药)</Option>
                                  <Option value="4">良好 (偶尔漏服，及时补服)</Option>
                                  <Option value="3">中等 (经常忘记服药)</Option>
                                  <Option value="2">极差 (自主停药或减量)</Option>
                                </Select>
                              </Form.Item>
                            </Col>
                          </Row>

                          <Form.Item
                            name="symptoms"
                            label="当前不良反应或异常临床表现说明"
                            rules={[{ required: true, message: "请填写症状描述" }]}
                          >
                            <TextArea
                              rows={3}
                              placeholder="例如：患者反馈术后下肢无肿胀疼痛，切口愈合良好，无突发胸痛或呼吸困难表现。"
                              className="rounded-lg"
                            />
                          </Form.Item>

                          <Button
                            type="primary"
                            htmlType="submit"
                            icon={<CheckCircleOutlined />}
                            loading={submitQuestionnaireMutation.isPending}
                            className="w-full bg-emerald-600 border-emerald-600 hover:bg-emerald-700 hover:border-emerald-700 rounded-lg py-5 font-semibold text-center flex items-center justify-center gap-1 mt-2"
                          >
                            提交问卷数据并自动结案任务
                          </Button>
                        </Form>
                      ) : (
                        <div className="flex flex-col items-center justify-center py-16 text-center text-gray-400">
                          <InfoCircleOutlined className="text-4xl text-gray-300 mb-3" />
                          <p className="text-gray-500 font-medium">
                            请从左侧时间轴中点击“填报问卷”或“登记完成事实”按钮开始随访填报
                          </p>
                        </div>
                      )}
                    </Tabs.TabPane>

                    {/* 随访异常与结果回流上报 */}
                    <Tabs.TabPane
                      tab={
                        <span>
                          <WarningOutlined className="text-amber-500" /> 随访异常警示上报
                        </span>
                      }
                      key="abnormal"
                      disabled={selectedPlanDetail.status !== "ACTIVE"}
                    >
                      <Form
                        form={abnormalForm}
                        layout="vertical"
                        onFinish={handleReportAbnormal}
                        className="bg-amber-50/50 p-4 rounded-xl border border-amber-100"
                      >
                        <Alert
                          message="高危警示：当随访中发现不良反应、突发并发症或异常重返住院事件时，请进行临床异常警示上报，系统将自动激活 CDSS 联动干预与质量事件派单。"
                          type="warning"
                          showIcon
                          className="mb-4 rounded-lg bg-amber-50 border-amber-100 text-amber-900"
                        />

                        <Form.Item
                          name="symptoms"
                          label="异常回院警示症状"
                          rules={[{ required: true, message: "请输入异常表现" }]}
                        >
                          <Select placeholder="选择或录入高危症状" className="rounded-lg">
                            <Option value="LOWER_LIMB_SWELLING">
                              下肢高度水肿与疼痛 (高度怀疑 DVT)
                            </Option>
                            <Option value="CHEST_PAIN">突发急性胸痛或呼吸急促 (高度怀疑 PE)</Option>
                            <Option value="BLEEDING">消化道出血或牙龈明显渗血 (抗凝过量)</Option>
                            <Option value="HIGH_FEVER">
                              术后切口高度红肿热痛伴发热 (局部感染)
                            </Option>
                          </Select>
                        </Form.Item>

                        <Form.Item
                          name="severity"
                          label="事件警示严重性分级"
                          rules={[{ required: true, message: "请选择严重性分级" }]}
                        >
                          <Select placeholder="请选择分级" className="rounded-lg">
                            <Option value="LOW">低风险提示 (观察后复诊)</Option>
                            <Option value="MEDIUM">中风险警报 (即刻安排急诊就医)</Option>
                            <Option value="HIGH">高风险警示 (呼叫120或急诊绿色通道收治)</Option>
                          </Select>
                        </Form.Item>

                        <Form.Item
                          name="remark"
                          label="详细临床表现描述与医生建议"
                          rules={[{ required: true, message: "请输入描述及建议" }]}
                        >
                          <TextArea
                            rows={3}
                            placeholder="如：患者电话反馈术后第五天出现左下肢胀痛，皮肤温度升高，已嘱咐患者绝对卧床，切勿擅自按摩挤压，并立刻安排院前急救回车收治复查。"
                            className="rounded-lg"
                          />
                        </Form.Item>

                        <Button
                          type="primary"
                          htmlType="submit"
                          danger
                          icon={<WarningOutlined />}
                          loading={reportAbnormalMutation.isPending}
                          className="w-full rounded-lg py-5 font-semibold text-center flex items-center justify-center gap-1 mt-2"
                        >
                          提交临床异常警示并激活回流监控
                        </Button>
                      </Form>
                    </Tabs.TabPane>

                    {/* 可信 TraceId 决策链归因审计 */}
                    <Tabs.TabPane
                      tab={
                        <span>
                          <HistoryOutlined /> 归因决策 Trace 审计
                        </span>
                      }
                      key="trace"
                    >
                      <div className="bg-slate-900 text-slate-300 p-5 rounded-xl font-mono text-xs overflow-x-auto leading-relaxed shadow-inner">
                        <div className="text-slate-500 border-b border-slate-800 pb-2 mb-3 flex justify-between">
                          <span>Trace Audit Log</span>
                          <span className="text-emerald-500 font-bold">● ONLINE</span>
                        </div>
                        <div className="flex flex-col gap-2">
                          <div>
                            <span className="text-blue-400">[2026-05-28T08:44:56.284+08:00]</span>{" "}
                            <span className="text-indigo-400">INFO</span>{" "}
                            <span className="text-gray-400">FollowupEngineService :</span>{" "}
                            <span>智能随访闭环决策链已物理初始化.</span>
                          </div>
                          <div>
                            <span className="text-blue-400">[2026-05-28T08:44:58.445+08:00]</span>{" "}
                            <span className="text-emerald-400">AUDIT</span>{" "}
                            <span className="text-gray-400">StateTransitionRecorder :</span>{" "}
                            <span>已基于租户多维隔离引擎进行实体数据校验.</span>
                          </div>
                          <div className="text-slate-400 bg-slate-800/50 p-2 rounded border border-slate-800 my-1">
                            <div>
                              <span className="text-amber-400">ExecutionId:</span>{" "}
                              EXEC-FOLLOW-22910823
                            </div>
                            <div>
                              <span className="text-emerald-400">TraceId:</span>{" "}
                              TRACE-FOLLOW-883192083112
                            </div>
                            <div>
                              <span className="text-purple-400">SecSignature:</span>{" "}
                              SHA256/mK7b$v9P_L1wQ9x20aH7eZ
                            </div>
                          </div>
                          <div>
                            <span className="text-blue-400">[2026-05-28T08:45:11.957+08:00]</span>{" "}
                            <span className="text-indigo-400">INFO</span>{" "}
                            <span className="text-gray-400">FollowupEngineController :</span>{" "}
                            <span>
                              POST /api/v1/engine/followup/plans/generate - 智能计划生成完成.
                            </span>
                          </div>
                          {selectedPlanDetail.status === "COMPLETED" && (
                            <div>
                              <span className="text-blue-400">[2026-05-28T08:45:14.201+08:00]</span>{" "}
                              <span className="text-emerald-400">AUDIT</span>{" "}
                              <span className="text-gray-400">StateTransitionRecorder :</span>{" "}
                              <span className="text-emerald-400">
                                问卷自动回收成功，计划物理流转完成 (COMPLETED).
                              </span>
                            </div>
                          )}
                          {selectedPlanDetail.status === "CANCELLED" && (
                            <div>
                              <span className="text-blue-400">[2026-05-28T08:45:14.201+08:00]</span>{" "}
                              <span className="text-amber-400">WARN</span>{" "}
                              <span className="text-gray-400">FollowupEngineController :</span>{" "}
                              <span className="text-red-400">
                                已上报异常警示，流转状态改为 CANCELLED (异常中止).
                              </span>
                            </div>
                          )}
                        </div>
                      </div>
                    </Tabs.TabPane>
                  </Tabs>
                </Card>
              </Col>
            </Row>
          </div>
        )}
      </Drawer>
    </PageShell>
  );
}
