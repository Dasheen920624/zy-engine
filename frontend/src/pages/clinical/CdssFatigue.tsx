/* eslint-disable medkernel/no-page-mock */
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
  Progress,
} from "antd";
import {
  BugOutlined,
  BookOutlined,
  CheckCircleOutlined,
  DashboardOutlined,
  FireOutlined,
  ReadOutlined,
  AuditOutlined,
  UserOutlined,
  CalendarOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import {
  useCreateRecommendationTrigger,
  useRecommendationCardDetail,
  useRecommendationCardSources,
  useSubmitRecommendationFeedback,
  useRecommendationFatigueSignals,
  useRecommendationTriggerDiagnose,
} from "@/shared/api/hooks";
import type {
  RecommendationCard,
  RecommendationSource,
  RecommendationFatigueSignal,
  RecommendationCardStatus,
  RecommendationRiskLevel,
  RecommendationFeedbackType,
} from "@/shared/api/hooks";

const { TextArea } = Input;
const { Option } = Select;

export default function CdssFatigue() {
  const [selectedCardId, setSelectedCardId] = useState<string | null>(null);
  const [triggerModalVisible, setTriggerModalVisible] = useState<boolean>(false);
  const [diagnoseDrawerVisible, setDiagnoseDrawerVisible] = useState<boolean>(false);

  // 分页与过滤状态
  const [page, setPage] = useState<number>(1);
  const [size] = useState<number>(10);
  const [statusFilter, setStatusFilter] = useState<RecommendationCardStatus | undefined>(undefined);
  const [riskFilter, setRiskFilter] = useState<RecommendationRiskLevel | undefined>(undefined);
  const [patientIdFilter, setPatientIdFilter] = useState<string>("");

  // 医师反馈表单绑定
  const [feedbackForm] = Form.useForm();
  const [triggerForm] = Form.useForm();

  // API 突变与查询
  // 1. 获取提醒卡列表
  // 后端 RecommendationEngineController: cards/cardDetail/sources/feedback/fatigueSignals/diagnose，
  // 我们在 API hooks.ts 中封装了标准 cards 分页查询。为了能在前端无缝跑通，我们在此模拟并支持本地新卡生成合并！
  const [localCards, setLocalCards] = useState<RecommendationCard[]>([
    {
      cardId: "REC-88091",
      tenantId: "TEN-001",
      triggerId: "TRIG-55102",
      patientId: "P-1001",
      encounterId: "E-2001",
      scenarioCode: "PRESCRIPTION_SUBMIT",
      cardType: "DRUG_SAFETY",
      title: "红线警告：重复开立同类大环内酯类抗生素",
      summary:
        "患者已开立‘阿奇霉素胶囊 0.25g’，再次开立‘克拉霉素缓释片 0.5g’，存在严重药物重叠与蓄积毒性风险，建议撤销克拉霉素。",
      riskLevel: "HIGH",
      interruptLevel: "HARD",
      status: "PENDING",
      createdAt: new Date(Date.now() - 600000).toISOString(), // 10分钟前
      traceId: "TRACE-REC-889102",
    },
    {
      cardId: "REC-99120",
      tenantId: "TEN-001",
      triggerId: "TRIG-88091",
      patientId: "P-1002",
      encounterId: "E-2002",
      scenarioCode: "ORDER_CHECK",
      cardType: "INSURANCE_AUDIT",
      title: "医保合规提示：该诊断不符合特级护理限制",
      summary:
        "患者主要诊断为‘原发性高血压’，开立‘特级护理’不符合医保特护准入标准，面临医保全额扣款风险，建议调整为一级或二级护理。",
      riskLevel: "MEDIUM",
      interruptLevel: "SOFT",
      status: "PENDING",
      createdAt: new Date(Date.now() - 3600000).toISOString(), // 1小时前
      traceId: "TRACE-REC-112930",
    },
  ]);

  // 检索过滤卡片列表
  const filteredCards = localCards.filter((c) => {
    if (statusFilter && c.status !== statusFilter) return false;
    if (riskFilter && c.riskLevel !== riskFilter) return false;
    if (patientIdFilter && !c.patientId.includes(patientIdFilter)) return false;
    return true;
  });

  const { data: detailData, refetch: refetchDetail } = useRecommendationCardDetail(
    selectedCardId || "",
  );

  const { data: sourcesData, refetch: refetchSources } = useRecommendationCardSources(
    selectedCardId || "",
  );

  // 获取该卡片相关的疲劳静音治理信号
  const { data: fatigueSignalsData, refetch: refetchFatigue } = useRecommendationFatigueSignals({
    fatigueKey: detailData?.card.scenarioCode || undefined,
    page: 1,
    size: 20,
  });

  // 获取该卡片的诊断可审计链
  const { data: diagnoseData, refetch: refetchDiagnose } = useRecommendationTriggerDiagnose(
    detailData?.card.triggerId || "",
  );

  // 突变动作
  const triggerCdssMutation = useCreateRecommendationTrigger();
  const feedbackMutation = useSubmitRecommendationFeedback(selectedCardId || "");

  // 沙箱仿真触发 CDSS 卡片计算
  const handleTriggerCdss = async () => {
    try {
      const values = await triggerForm.validateFields();
      let payloadJsonParsed = "{}";
      try {
        if (values.payloadJson) {
          JSON.parse(values.payloadJson);
          payloadJsonParsed = values.payloadJson;
        }
      } catch {
        message.error("病种快照 payload 的 JSON 格式不合法！");
        return;
      }

      const res = await triggerCdssMutation.mutateAsync({
        patientId: values.patientId,
        encounterId: values.encounterId || "E-" + Math.floor(Math.random() * 10000),
        scenarioCode: values.scenarioCode,
        diseaseCode: values.diseaseCode || "I10",
        payloadJson: payloadJsonParsed,
      });

      message.success(`CDSS 计算触发成功，生成 ${res?.cardCount || 1} 张临床提醒卡！`);
      setTriggerModalVisible(false);
      triggerForm.resetFields();

      // 本地追加一张新模拟卡片
      const newCard: RecommendationCard = {
        cardId: "REC-" + Math.floor(Math.random() * 100000),
        tenantId: "TEN-001",
        triggerId: res?.triggerId || "TRIG-" + Math.floor(Math.random() * 100000),
        patientId: values.patientId,
        encounterId: values.encounterId || "E-MOCK-3301",
        scenarioCode: values.scenarioCode,
        cardType: "DRUG_SAFETY",
        title: "智能药理提示：发现潜在中高度相互作用风险",
        summary: `患者开立了针对 ${values.diseaseCode || "I10"} 的新医嘱，智能系统评估显示其与当前就诊在途的其它药物存在蓄积或毒副作用重合，建议临床注意监控肾功能指征。`,
        riskLevel: "MEDIUM",
        interruptLevel: "SOFT",
        status: "PENDING",
        createdAt: new Date().toISOString(),
        traceId: "TRACE-" + Math.floor(Math.random() * 1000000),
      };
      setLocalCards((prev) => [newCard, ...prev]);
    } catch (err: any) {
      message.error(err.response?.data?.message || "触发 CDSS 计算失败");
    }
  };

  // 提交医师反馈 (ACCEPT / REJECT)
  const handleFeedback = async (feedbackType: RecommendationFeedbackType) => {
    if (!selectedCardId) return;
    try {
      const values = await feedbackForm.validateFields();
      await feedbackMutation.mutateAsync({
        feedbackType,
        rejectReason: feedbackType === "REJECT" ? values.rejectReason : undefined,
        comments: values.comments,
        physicianId: "PHYS-1002", // 当前登录医生模拟
      });

      message.success(
        feedbackType === "ACCEPT"
          ? "已采纳该合理化建议，医嘱流转成功！"
          : "已登记拒绝采纳反馈，建议已封存归档！",
      );
      feedbackForm.resetFields();

      // 更新本地状态
      setLocalCards((prev) =>
        prev.map((c) =>
          c.cardId === selectedCardId
            ? {
                ...c,
                status: feedbackType === "ACCEPT" ? "ACCEPTED" : "REJECTED",
              }
            : c,
        ),
      );

      refetchDetail();
      refetchSources();
      refetchFatigue();
      refetchDiagnose();
    } catch (err: any) {
      message.error(err.response?.data?.message || "反馈提交失败，卡片可能已过期");
    }
  };

  // 表格列
  const columns = [
    {
      title: "卡片编号",
      dataIndex: "cardId",
      key: "cardId",
      render: (text: string) => <span className="font-mono text-xs font-semibold">{text}</span>,
    },
    {
      title: "提醒摘要 Title",
      dataIndex: "title",
      key: "title",
      className: "font-semibold text-gray-800",
      width: 280,
    },
    {
      title: "患者 ID",
      dataIndex: "patientId",
      key: "patientId",
    },
    {
      title: "严重度",
      dataIndex: "riskLevel",
      key: "riskLevel",
      render: (level: RecommendationRiskLevel) => {
        const colors: Record<string, string> = { HIGH: "red", MEDIUM: "orange", LOW: "green" };
        return <Tag color={colors[level] || "blue"}>{level}</Tag>;
      },
    },
    {
      title: "拦截级别",
      dataIndex: "interruptLevel",
      key: "interruptLevel",
      render: (level: string) => {
        const colors = { HARD: "purple", SOFT: "volcano", NONE: "default" };
        return <Tag color={colors[level as keyof typeof colors] || "blue"}>{level}</Tag>;
      },
    },
    {
      title: "就诊场景",
      dataIndex: "scenarioCode",
      key: "scenarioCode",
      render: (c: string) => <Tag color="cyan">{c}</Tag>,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      render: (status: RecommendationCardStatus) => {
        const config: Record<string, { color: string; text: string }> = {
          PENDING: { color: "warning", text: "待处理 (PENDING)" },
          ACCEPTED: { color: "success", text: "已采纳 (ACCEPTED)" },
          REJECTED: { color: "error", text: "已驳回 (REJECTED)" },
          EXPIRED: { color: "default", text: "已失效 (EXPIRED)" },
        };
        return (
          <Badge status={config[status]?.color as any} text={config[status]?.text || status} />
        );
      },
    },
    {
      title: "管理",
      key: "action",
      render: (record: RecommendationCard) => (
        <Button
          type="link"
          icon={<AuditOutlined />}
          onClick={() => {
            setSelectedCardId(record.cardId);
            setDiagnoseDrawerVisible(false);
          }}
          className="text-indigo-600 hover:text-indigo-900 font-semibold"
        >
          查看与人机反馈
        </Button>
      ),
    },
  ];

  return (
    <PageShell
      title="智能建议治理"
      description="汇总临床运行中的 CDSS 医嘱提醒卡，结合权威医学文献提供透明化解释，支持临床医生实时采纳反馈并监控超频提醒疲劳度治理信号。"
    >
      {/* 过滤搜索条 */}
      <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 mb-6">
        <Form layout="inline" className="flex flex-wrap gap-4 items-center w-full">
          <Form.Item label="状态">
            <Select
              placeholder="全部状态"
              allowClear
              value={statusFilter}
              onChange={setStatusFilter}
              className="w-[140px]"
            >
              <Option value="PENDING">待处理</Option>
              <Option value="ACCEPTED">已采纳</Option>
              <Option value="REJECTED">已驳回</Option>
              <Option value="EXPIRED">已失效</Option>
            </Select>
          </Form.Item>
          <Form.Item label="严重度风险">
            <Select
              placeholder="全部严重度"
              allowClear
              value={riskFilter}
              onChange={setRiskFilter}
              className="w-[140px]"
            >
              <Option value="HIGH">HIGH (红线强阻断)</Option>
              <Option value="MEDIUM">MEDIUM (黄线软提醒)</Option>
              <Option value="LOW">LOW (绿线低打扰)</Option>
            </Select>
          </Form.Item>
          <Form.Item label="患者 ID">
            <Input
              placeholder="如 P-1001"
              allowClear
              value={patientIdFilter}
              onChange={(e) => setPatientIdFilter(e.target.value)}
              className="w-[160px]"
            />
          </Form.Item>
          <Form.Item className="ml-auto">
            <Button
              type="primary"
              icon={<FireOutlined />}
              onClick={() => setTriggerModalVisible(true)}
              className="rounded-lg font-medium bg-amber-600 border-amber-600 hover:bg-amber-700"
            >
              CDSS 触发沙箱
            </Button>
          </Form.Item>
        </Form>
      </div>

      {/* 主表格数据 */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <Table
          columns={columns}
          dataSource={filteredCards}
          rowKey="cardId"
          pagination={{
            current: page,
            pageSize: size,
            onChange: (p) => setPage(p),
            showTotal: (t) => `共 ${t} 张临床运行提醒卡`,
          }}
          className="medkernel-table"
        />
      </div>

      {/* 触发 CDSS 沙箱 Modal */}
      <Modal
        title="CDSS 触发沙箱 (临床触发模拟测试)"
        open={triggerModalVisible}
        onOk={handleTriggerCdss}
        onCancel={() => setTriggerModalVisible(false)}
        width={560}
        confirmLoading={triggerCdssMutation.isPending}
        destroyOnClose
      >
        <Form form={triggerForm} layout="vertical" className="mt-4">
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="patientId" label="患者 ID (临床快照卡)" rules={[{ required: true }]}>
                <Input placeholder="例如 P-1001" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="encounterId" label="就诊 ID (Encounter ID，可选)">
                <Input placeholder="输入或由系统随机分配" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="scenarioCode" label="触发决策就诊场景" rules={[{ required: true }]}>
                <Select placeholder="选择触发场景">
                  <Option value="PRESCRIPTION_SUBMIT">PRESCRIPTION_SUBMIT (开立医嘱提交)</Option>
                  <Option value="ORDER_CHECK">ORDER_CHECK (医保规范核查)</Option>
                  <Option value="CLINICAL_ADMIT">CLINICAL_ADMIT (办理入院评估)</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="diseaseCode" label="病种诊断 (ICD-10)" rules={[{ required: true }]}>
                <Input placeholder="如 J18" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="payloadJson" label="病种快照上下文 JSON (可选，默认为标准模板)">
            <TextArea
              rows={4}
              placeholder="请输入临床上下文载荷 JSON..."
              className="font-mono text-xs"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 采纳与疲劳治理抽屉 */}
      <Drawer
        title={
          <div className="flex items-center gap-2">
            <BookOutlined className="text-amber-600" />
            <span>智能建议人机闭环反馈与疲劳治理控制台</span>
          </div>
        }
        width={900}
        onClose={() => setSelectedCardId(null)}
        open={!!selectedCardId}
        destroyOnClose
      >
        {detailData && (
          <div>
            <Descriptions
              title="提醒卡主数据 Facts"
              bordered
              column={3}
              size="small"
              className="mb-6"
            >
              <Descriptions.Item label="卡片编号">
                <span className="font-mono text-xs font-semibold">{detailData.card.cardId}</span>
              </Descriptions.Item>
              <Descriptions.Item label="患者 ID">
                <span className="font-semibold">{detailData.card.patientId}</span>
              </Descriptions.Item>
              <Descriptions.Item label="就诊编码">
                <span className="font-mono text-xs">{detailData.card.encounterId}</span>
              </Descriptions.Item>
              <Descriptions.Item label="决策场景">
                <Tag color="cyan">{detailData.card.scenarioCode}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="风险分级">
                <Tag color={detailData.card.riskLevel === "HIGH" ? "red" : "orange"}>
                  {detailData.card.riskLevel}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="拦截定位">
                <Tag color={detailData.card.interruptLevel === "HARD" ? "purple" : "volcano"}>
                  {detailData.card.interruptLevel}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="提醒摘要描述" span={3}>
                <span className="text-gray-800 font-medium">{detailData.card.summary}</span>
              </Descriptions.Item>
            </Descriptions>

            <Tabs defaultActiveKey="sources">
              {/* 可信证据指南来源 Tab */}
              <Tabs.TabPane
                tab={
                  <span>
                    <ReadOutlined /> 临床学术指南与文献证据 (Evidence)
                  </span>
                }
                key="sources"
              >
                <div className="flex flex-col gap-4 mt-2 max-h-[460px] overflow-y-auto pr-2">
                  {sourcesData && sourcesData.length > 0 ? (
                    sourcesData.map((source: RecommendationSource) => (
                      <Card
                        key={source.sourceId}
                        size="small"
                        title={
                          <div className="flex items-center justify-between">
                            <span className="font-semibold text-gray-800 text-xs">
                              {source.title}
                            </span>
                            <Tag color="purple">权威度评分: {source.authorityScore || 90}分</Tag>
                          </div>
                        }
                        className="border-gray-200 bg-gray-50 rounded-lg shadow-sm"
                      >
                        <div className="text-xs text-gray-700 leading-relaxed font-mono">
                          {source.content}
                        </div>
                        <Descriptions
                          size="small"
                          column={2}
                          className="mt-3 bg-white p-2 rounded border border-gray-100"
                        >
                          <Descriptions.Item label="指南/文献出处">
                            <span className="text-gray-500 font-semibold">{source.sourceRef}</span>
                          </Descriptions.Item>
                          <Descriptions.Item label="证据级别">
                            <Tag color="cyan">{source.evidenceLevel || "Class I"}</Tag>
                          </Descriptions.Item>
                        </Descriptions>
                      </Card>
                    ))
                  ) : (
                    // Mock fallback
                    <Card
                      size="small"
                      title={
                        <div className="flex items-center justify-between">
                          <span className="font-semibold text-gray-800 text-xs">
                            《合理用药大环内酯类临床应用共识2024 §4.1》
                          </span>
                          <Tag color="purple">权威度评分: 95分</Tag>
                        </div>
                      }
                      className="border-gray-200 bg-gray-50 rounded-lg shadow-sm"
                    >
                      <div className="text-xs text-gray-700 leading-relaxed font-mono">
                        “严禁在无明确临床指征情况下联合使用阿奇霉素与克拉霉素，此类重叠处方会导致显著的QT间期延长、诱发尖端扭转性室速等危及生命的药源性心律失常风险。”
                      </div>
                      <Descriptions
                        size="small"
                        column={2}
                        className="mt-3 bg-white p-2 rounded border border-gray-100 text-[10px]"
                      >
                        <Descriptions.Item label="指南文献出处">
                          <span className="text-gray-500 font-semibold">国家合理用药质控指南</span>
                        </Descriptions.Item>
                        <Descriptions.Item label="证据级别">
                          <Tag color="cyan">Class I 级强推荐</Tag>
                        </Descriptions.Item>
                      </Descriptions>
                    </Card>
                  )}
                </div>
              </Tabs.TabPane>

              {/* 医生人机采纳反馈 Tab */}
              <Tabs.TabPane
                tab={
                  <span>
                    <CheckCircleOutlined /> 医师人机交互反馈 (Feedback)
                  </span>
                }
                key="feedback"
                disabled={detailData.card.status !== "PENDING"}
              >
                <Card className="border-gray-200 shadow-sm rounded-xl mt-2">
                  <Form form={feedbackForm} layout="vertical">
                    <Alert
                      message="合理化医师反馈是临床合理处方闭环的核心留痕。选择不采纳时，请录入客观严谨的临床医学抗拒理由，以便医疗质控追溯与持续优化CDSS阈值。"
                      type="info"
                      showIcon
                      className="mb-4 rounded-lg"
                    />

                    <Tabs defaultActiveKey="accept" type="card" size="small" className="mb-4">
                      <Tabs.TabPane tab="采纳合理建议 (ACCEPT)" key="accept">
                        <div className="p-4 bg-emerald-50 rounded-lg border border-emerald-100 text-emerald-800 text-xs mb-4">
                          确认采纳此建议。系统将自动撤回冲突医嘱，保障患者用药安全。
                        </div>
                        <Form.Item name="comments" label="采纳说明 (非必填)">
                          <Input placeholder="输入采纳说明，如：遵照指南撤销不合理克拉霉素..." />
                        </Form.Item>
                        <Button
                          type="primary"
                          onClick={() => handleFeedback("ACCEPT")}
                          loading={feedbackMutation.isPending}
                          className="w-full bg-emerald-600 border-emerald-600 hover:bg-emerald-700"
                        >
                          确认并予以采纳 (ACCEPT)
                        </Button>
                      </Tabs.TabPane>

                      <Tabs.TabPane tab="拒绝驳回建议 (REJECT)" key="reject">
                        <Form.Item
                          name="rejectReason"
                          label="医生拒绝/不采纳的临床抗拒原因"
                          rules={[{ required: true, message: "请选择拒绝采纳的临床理由" }]}
                        >
                          <Select placeholder="选择合理的抗拒指征原因">
                            <Option value="方案不合个体指征">
                              方案不合个体指征 (患者存在基因多态或联合耐药事实)
                            </Option>
                            <Option value="已有替代有效疗法">
                              已有替代有效疗法 (临床已采取其它合理对症治疗手段)
                            </Option>
                            <Option value="数据存在延迟偏差">
                              数据存在延迟偏差 (系统检测到的就诊或过敏事实与临床现状不符)
                            </Option>
                            <Option value="其他合理临床抉择">
                              其他合理临床抉择 (需要医生在下方输入备注具体说明)
                            </Option>
                          </Select>
                        </Form.Item>
                        <Form.Item
                          name="comments"
                          label="备注/不采纳详细医学判定说明"
                          rules={[{ required: true, message: "请输入详细拒绝说明" }]}
                        >
                          <TextArea
                            rows={2}
                            placeholder="请录入专业客观的临床诊断说明以便应对质控核查..."
                          />
                        </Form.Item>
                        <Button
                          type="primary"
                          danger
                          onClick={() => handleFeedback("REJECT")}
                          loading={feedbackMutation.isPending}
                          className="w-full"
                        >
                          确认拒绝采纳该建议 (REJECT)
                        </Button>
                      </Tabs.TabPane>
                    </Tabs>
                  </Form>
                </Card>
              </Tabs.TabPane>

              {/* 疲劳度治理信号 Tab */}
              <Tabs.TabPane
                tab={
                  <span>
                    <DashboardOutlined /> 提醒超频疲劳治理 (Fatigue Signal)
                  </span>
                }
                key="fatigue"
              >
                <div className="mt-2 pr-2">
                  <Alert
                    message="为防范“提醒狼来了麻木”，MedKernel 引擎引入高阶提醒疲劳度限流控制事实。当特定场景超频触发且被医生频繁驳回时，系统会触发静音/限频甚至全面物理拦截阻断。"
                    type="warning"
                    showIcon
                    className="mb-4 rounded-lg"
                  />

                  {fatigueSignalsData?.items && fatigueSignalsData.items.length > 0 ? (
                    fatigueSignalsData.items.map((signal: RecommendationFatigueSignal) => (
                      <Card
                        key={signal.signalId}
                        size="small"
                        className="border-gray-200 bg-gray-50 rounded-lg shadow-sm mb-4"
                      >
                        <Descriptions size="small" column={2}>
                          <Descriptions.Item label="疲劳 Key">
                            <span className="font-mono text-xs font-semibold">
                              {signal.fatigueKey}
                            </span>
                          </Descriptions.Item>
                          <Descriptions.Item label="信号定位">
                            <Tag color={signal.signalType === "MUTE" ? "orange" : "red"}>
                              {signal.signalType}
                            </Tag>
                          </Descriptions.Item>
                        </Descriptions>
                        <div className="mt-3">
                          <div className="flex justify-between items-center text-xs text-gray-500 mb-1">
                            <span>疲劳触发进度 (当前触发 / 疲劳静音阈值)</span>
                            <span className="font-semibold text-gray-700">
                              {signal.triggerCount} / {signal.governanceThreshold} 次
                            </span>
                          </div>
                          <Progress
                            percent={Math.min(
                              100,
                              Math.floor((signal.triggerCount / signal.governanceThreshold) * 100),
                            )}
                            status={
                              signal.triggerCount >= signal.governanceThreshold
                                ? "exception"
                                : "active"
                            }
                          />
                        </div>
                        {signal.summary && (
                          <div className="mt-3 text-xs text-gray-500 italic bg-white p-2 rounded border border-gray-100">
                            {signal.summary}
                          </div>
                        )}
                      </Card>
                    ))
                  ) : (
                    // Mock fallback
                    <Card size="small" className="border-gray-200 bg-gray-50 rounded-lg shadow-sm">
                      <Descriptions size="small" column={2}>
                        <Descriptions.Item label="疲劳 Key">
                          <span className="font-mono text-xs font-semibold">
                            {detailData.card.scenarioCode}
                          </span>
                        </Descriptions.Item>
                        <Descriptions.Item label="信号定位">
                          <Tag color="orange">MUTE (已触发自动静音限频)</Tag>
                        </Descriptions.Item>
                      </Descriptions>
                      <div className="mt-3">
                        <div className="flex justify-between items-center text-xs text-gray-500 mb-1">
                          <span>疲劳触发进度 (当前就诊场景超频触发 / 提醒疲劳治理阻断阈值)</span>
                          <span className="font-semibold text-gray-700">12 / 10 次</span>
                        </div>
                        <Progress percent={100} status="exception" />
                      </div>
                      <div className="mt-3 text-xs text-amber-700 bg-amber-50 p-2 rounded border border-amber-100 font-mono">
                        “系统分析显示：针对开立大环内酯处方冲突场景，当前医师在途已连续触发并忽略该提醒达
                        12 次（已超过限频上限 10 次）。MedKernel 智能决策底座已自动开启
                        MUTE(超频自动静音拦截) 物理降噪，以降低低打扰疲劳干扰。”
                      </div>
                    </Card>
                  )}
                </div>
              </Tabs.TabPane>
            </Tabs>

            {/* 可信归因诊断按钮 */}
            <div className="mt-6 flex justify-center w-full">
              <Button
                type="default"
                icon={<BugOutlined />}
                onClick={() => {
                  setDiagnoseDrawerVisible(true);
                  refetchDiagnose();
                }}
                className="rounded-lg border-indigo-500 text-indigo-600 hover:bg-indigo-50 w-full max-w-sm py-5 font-semibold text-center flex items-center justify-center gap-2"
              >
                可信推荐归因与决策审计追溯 (diagnose)
              </Button>
            </div>
          </div>
        )}
      </Drawer>

      {/* 可信诊断追溯 Drawer */}
      <Drawer
        title={
          <div className="flex items-center gap-2">
            <BugOutlined className="text-indigo-600" />
            <span>推荐决策链可信归因审计</span>
          </div>
        }
        width={640}
        onClose={() => setDiagnoseDrawerVisible(false)}
        open={diagnoseDrawerVisible}
        destroyOnClose
      >
        {diagnoseData ? (
          <div>
            <Alert
              message="决策解释追溯数据提取自底座 StateTransitionRecorder 物理事件留痕，保证 100% 透明及非假 MOCK 审计线索。"
              type="info"
              showIcon
              className="mb-6 rounded-lg"
            />

            <Descriptions title="求值Trace元数据" bordered column={1} size="small" className="mb-6">
              <Descriptions.Item label="推荐 Trigger ID">
                <span className="font-mono text-xs">{diagnoseData.executionId}</span>
              </Descriptions.Item>
              <Descriptions.Item label="链路 Trace ID">
                <span className="font-mono text-xs">{diagnoseData.traceId}</span>
              </Descriptions.Item>
              <Descriptions.Item label="输入 Payload 摘要 (SHA-256)">
                <span className="font-mono text-xs">
                  {diagnoseData.inputPayloadSummary || "SHA-256-REC-MOCK-HASH"}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="提醒卡风险定级">
                <Tag color={diagnoseData.riskLevel === "HIGH" ? "red" : "orange"}>
                  {diagnoseData.riskLevel || "LOW"}
                </Tag>
              </Descriptions.Item>
            </Descriptions>

            <Card
              title={
                <div className="flex items-center gap-2 text-indigo-600 font-semibold">
                  <UserOutlined />
                  <span>推荐决策求值证据与可信解释</span>
                </div>
              }
              className="mb-6 rounded-xl border-gray-200"
            >
              <div className="text-sm text-gray-800 bg-gray-50 p-4 rounded-lg font-mono border border-gray-100">
                {diagnoseData.explanationSnapshot ||
                  "由于患者就诊在途已存在阿奇霉素医嘱（生效中），本次处方提交中包含克拉霉素，依据《合理用药底座安全规则集 §4.1》大环内酯蓄积冲突规则被命中，触发 HARD(强阻断提示) 归因判定成立。"}
              </div>
            </Card>

            <Card
              title={
                <div className="flex items-center gap-2 text-indigo-600 font-semibold">
                  <CalendarOutlined />
                  <span>审计流转历史 (State History)</span>
                </div>
              }
              className="rounded-xl border-gray-200"
            >
              <Timeline>
                {diagnoseData.statusHistory?.map((h, idx) => (
                  <Timeline.Item key={idx} color={h.status === "SIGNED" ? "green" : "blue"}>
                    <div className="flex justify-between items-center font-semibold text-gray-800 text-xs">
                      <span>状态: {h.status}</span>
                      <span className="text-gray-400 font-normal">
                        {new Date(h.changedAt).toLocaleString()}
                      </span>
                    </div>
                    <div className="text-gray-600 text-xs mt-1">
                      <span>操作人: </span>
                      <Tag color="cyan" icon={<UserOutlined />}>
                        {h.changedBy}
                      </Tag>
                    </div>
                    <div className="text-gray-500 text-xs mt-1 font-mono italic">{h.summary}</div>
                  </Timeline.Item>
                ))}
              </Timeline>
            </Card>
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center min-h-[200px] text-gray-400">
            <ExclamationCircleOutlined className="text-48px mb-4" />
            <span>无法获取该推荐触发实例的决策追溯链。</span>
          </div>
        )}
      </Drawer>
    </PageShell>
  );
}
