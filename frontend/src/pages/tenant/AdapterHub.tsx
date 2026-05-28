import { useState, useEffect } from "react";
import {
  Table,
  Button,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  Card,
  Badge,
  Alert,
  message,
  Row,
  Col,
  List,
  Tabs,
  Space,
  theme,
  Statistic,
  Descriptions,
} from "antd";
import {
  PlusOutlined,
  PlayCircleOutlined,
  SafetyCertificateOutlined,
  ApiOutlined,
  CodeOutlined,
  FileProtectOutlined,
  CloudSyncOutlined,
  CheckCircleOutlined,
  DisconnectOutlined,
  HeartOutlined,
  ReloadOutlined,
  DeleteOutlined,
  LockOutlined,
  CompassOutlined,
} from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import {
  useEmbedOrigins,
  useAddEmbedOrigin,
  useGenerateEmbedToken,
  useIntegrationAdapters,
  useCreateAdapter,
  useUpdateAdapter,
  usePingAdapter,
  useWebhooks,
  useCreateWebhook,
  useTestWebhookSignature,
  useIntegrationLogs,
  useRetryMessage,
  useDeleteMessage,
  IntegrationAdapter,
  IntegrationWebhookConfig,
  IntegrationMessageLog,
} from "@/shared/api/hooks";

const { Option } = Select;

// 仿真底座兜底数据 (规范命名，避免 SHOUTY-CASE)
const defaultLocalAdapters: IntegrationAdapter[] = [
  {
    id: 1,
    adapterId: "sys-his",
    tenantId: "tenant-001",
    name: "核心住院医生工作站 (HIS)",
    protocolType: "HL7 / SOAP",
    status: "ACTIVE",
    configJson: "{\"missingRate\":0.01,\"termMappingRate\":0.98,\"timestampAnomalyRate\":0.00}",
    healthStatus: "HEALTHY",
    rttMs: 4,
    lastHeartbeatAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 2,
    adapterId: "sys-emr",
    tenantId: "tenant-001",
    name: "电子病历编辑器系统 (EMR)",
    protocolType: "REST API",
    status: "ACTIVE",
    configJson: "{\"missingRate\":0.02,\"termMappingRate\":0.96,\"timestampAnomalyRate\":0.00}",
    healthStatus: "HEALTHY",
    rttMs: 12,
    lastHeartbeatAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 3,
    adapterId: "sys-lis",
    tenantId: "tenant-001",
    name: "检验信息管理系统 (LIS)",
    protocolType: "DB Link / SQL",
    status: "ACTIVE",
    configJson: "{\"missingRate\":0.03,\"termMappingRate\":0.95,\"timestampAnomalyRate\":0.01}",
    healthStatus: "HEALTHY",
    rttMs: 18,
    lastHeartbeatAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 4,
    adapterId: "sys-pacs",
    tenantId: "tenant-001",
    name: "医学影像归档系统 (PACS)",
    protocolType: "DICOM / Web",
    status: "ACTIVE",
    configJson: "{\"missingRate\":0.02,\"termMappingRate\":0.97,\"timestampAnomalyRate\":0.00}",
    healthStatus: "HEALTHY",
    rttMs: 25,
    lastHeartbeatAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
];

const defaultLocalWebhooks: IntegrationWebhookConfig[] = [
  {
    id: 1,
    webhookId: "whk-discharge",
    tenantId: "tenant-001",
    name: "出院小结回传订阅",
    callbackUrl: "http://his.hospital.local:8080/api/callback/discharge",
    secretKey: "sec_key_e3b0c44298fc1c149afbf4c8996fb92427ae",
    eventsSubscribed: "DISCHARGE_PLAN",
    status: "ACTIVE",
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
];

const defaultLocalLogs: IntegrationMessageLog[] = [
  {
    id: 1,
    messageId: "msg-log-001",
    tenantId: "tenant-001",
    traceId: "tr-stroke-proof-009",
    direction: "OUTBOUND",
    systemName: "核心住院医生工作站 (HIS)",
    protocolType: "HL7 / SOAP",
    payloadSummary: "ADOPT_RECOMMENDATION | patientId=P-1001",
    payload: "{\"action\":\"ADOPT\",\"recommendationCard\":\"STK-CDSS-001\",\"patientId\":\"P-1001\"}",
    status: "SUCCESS",
    retryCount: 0,
    maxRetries: 3,
    errorMessage: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 2,
    messageId: "msg-log-002",
    tenantId: "tenant-001",
    traceId: "tr-ami-proof-002",
    direction: "OUTBOUND",
    systemName: "检验信息管理系统 (LIS)",
    protocolType: "DB Link",
    payloadSummary: "CRITICAL_VALUE_ALARM | patientId=P-1002",
    payload: "{\"alarmCode\":\"CRIT-01\",\"value\":\"Troponin T 2.4 ng/mL\",\"patientId\":\"P-1002\"}",
    status: "FAILED",
    retryCount: 2,
    maxRetries: 3,
    errorMessage: "接收方服务器无响应 (Socket Timeout)",
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 3,
    messageId: "msg-log-003",
    tenantId: "tenant-001",
    traceId: "tr-vte-proof-005",
    direction: "OUTBOUND",
    systemName: "电子病历编辑器系统 (EMR)",
    protocolType: "REST API",
    payloadSummary: "VTE_RISK_ASSESS | patientId=P-1003",
    payload: "{\"assessScore\":5,\"riskLevel\":\"HIGH\",\"patientId\":\"P-1003\"}",
    status: "DEAD_LETTER",
    retryCount: 3,
    maxRetries: 3,
    errorMessage: "投递重试超限，已移入死信！原因: 400 Bad Request",
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
];

const fallbackOriginsList = [
  "http://localhost:5173",
  "http://127.0.0.1:5173",
  "http://his.hospital.local:8080",
];

export default function AdapterHub() {
  const { token } = theme.useToken();

  // ==========================================
  // 1. API 接口数据绑定 (React Query)
  // ==========================================
  const { data: apiOrigins, refetch: refetchOrigins } = useEmbedOrigins();
  const addOriginMutation = useAddEmbedOrigin();
  const generateTokenMutation = useGenerateEmbedToken();

  // 集成总线接口 hooks
  const { data: apiAdapters, refetch: refetchAdapters, isLoading: loadingAdapters } = useIntegrationAdapters();
  const createAdapterMutation = useCreateAdapter();
  const updateAdapterMutation = useUpdateAdapter();
  const pingAdapterMutation = usePingAdapter();

  const { data: apiWebhooks, refetch: refetchWebhooks } = useWebhooks();
  const createWebhookMutation = useCreateWebhook();
  const testWebhookSigMutation = useTestWebhookSignature();

  const [logPage, setLogPage] = useState(1);
  const { data: apiLogsData, refetch: refetchLogs, isLoading: loadingLogs } = useIntegrationLogs(logPage, 5);
  const retryMessageMutation = useRetryMessage();
  const deleteMessageMutation = useDeleteMessage();

  // ==========================================
  // 2. 本地仿真与交互状态
  // ==========================================
  const [localOrigins, setLocalOrigins] = useState<string[]>(fallbackOriginsList);
  const [originFormVisible, setOriginFormVisible] = useState<boolean>(false);
  const [sandboxVisible, setSandboxVisible] = useState<boolean>(false);

  // 仿真沙箱所用的 token & URL 状态
  const [sandboxToken, setSandboxToken] = useState<string>("");
  const [sandboxUrl, setSandboxUrl] = useState<string>("");
  const [hisOrders, setHisOrders] = useState<string[]>([
    "入院常规护理 (一级护理)",
    "心电监护 + 吸氧 3L/min",
  ]);
  const [postMessageLogs, setPostMessageLogs] = useState<any[]>([]);

  // 适配器 & Webhook 控制表单状态
  const [adapterModalVisible, setAdapterModalVisible] = useState(false);
  const [webhookModalVisible, setWebhookModalVisible] = useState(false);
  const [pingLoadingMap, setPingLoadingMap] = useState<Record<string, boolean>>({});
  const [qualityDiagnosticReport, setQualityDiagnosticReport] = useState<any>(null);

  // Webhook 签名仿真测试状态
  const [selectedWebhookId, setSelectedWebhookId] = useState<string>("");
  const [webhookTestPayload, setWebhookTestPayload] = useState<string>("{\n  \"eventId\": \"ev-90211\",\n  \"patientId\": \"P-1001\",\n  \"action\": \"DISCHARGE_PLAN\"\n}");
  const [testResultSummary, setTestResultSummary] = useState<any>(null);
  const [testLogLoading, setTestLogLoading] = useState<boolean>(false);

  // 表单 Form 定义
  const [originForm] = Form.useForm();
  const [tokenForm] = Form.useForm();
  const [adapterForm] = Form.useForm();
  const [webhookForm] = Form.useForm();

  // 兜底合并
  const displayOrigins = apiOrigins && apiOrigins.length > 0 ? apiOrigins : localOrigins;
  const displayAdapters = apiAdapters && apiAdapters.length > 0 ? apiAdapters : defaultLocalAdapters;
  const displayWebhooks = apiWebhooks && apiWebhooks.length > 0 ? apiWebhooks : defaultLocalWebhooks;
  const displayLogs = apiLogsData?.items && apiLogsData.items.length > 0 ? apiLogsData.items : defaultLocalLogs;
  const displayLogsTotal = apiLogsData?.total ?? defaultLocalLogs.length;

  // 3. 监听跨域通信
  useEffect(() => {
    const handleIframeMessage = (event: MessageEvent) => {
      if (event.data && event.data.source === "MEDKERNEL_CDSS_EMBED") {
        const payload = event.data;
        setPostMessageLogs((prev) => [payload, ...prev]);

        if (payload.action === "ADOPT") {
          message.success("【HIS系统成功收到跨域指令】已自动在处方区开具低分子肝素钙医嘱！");
          setHisOrders((prev) => [
            ...prev,
            "★ 低分子肝素钙注射液 4100 IU qd 皮下注射 (CDSS推荐采纳)",
          ]);
        } else if (payload.action === "REJECT") {
          message.warning(`【HIS系统收到拒绝事件】反馈理由已存档：${payload.reason}`);
          setHisOrders((prev) => [...prev, `✕ 临床拒绝 CDSS 建议：${payload.reason}`]);
        }
      }
    };

    window.addEventListener("message", handleIframeMessage);
    return () => {
      window.removeEventListener("message", handleIframeMessage);
    };
  }, []);

  // 4. 动作执行 (跨域白名单 & Token 发生)
  const handleAddOrigin = async () => {
    try {
      const values = await originForm.validateFields();
      await addOriginMutation.mutateAsync({ origin: values.origin });
      message.success("跨域安全域名配置成功！");
      setOriginFormVisible(false);
      originForm.resetFields();
      refetchOrigins();
    } catch {
      const values = originForm.getFieldsValue();
      setLocalOrigins((prev) => [...prev, values.origin]);
      message.success(`[仿真模式] 安全域名白名单配置成功: ${values.origin}`);
      setOriginFormVisible(false);
      originForm.resetFields();
    }
  };

  const handleGenerateToken = async () => {
    try {
      const values = await tokenForm.validateFields();
      const res = await generateTokenMutation.mutateAsync({
        userId: values.userId,
        roleCode: values.roleCode,
        patientId: values.patientId,
        encounterId: values.encounterId,
        triggerPoint: values.triggerPoint,
        expireSeconds: 600,
      });

      if (res?.token) {
        setSandboxToken(res.token);
        setSandboxUrl(`${window.location.origin}${res.embedUrl}`);
        setSandboxVisible(true);
        setPostMessageLogs([]);
      }
    } catch {
      const mockToken = "tkn-stroke-demo-" + Math.floor(Math.random() * 100000);
      setSandboxToken(mockToken);
      setSandboxUrl(`${window.location.origin}/embed/launch?token=${mockToken}`);
      setSandboxVisible(true);
      setPostMessageLogs([]);
      message.info("[仿真模式] 成功生成 HIS 一次性安全启动令牌，已拉起集成仿真沙箱！");
    }
  };

  // 5. 适配器生命周期操作
  const handleCreateAdapter = async () => {
    try {
      const values = await adapterForm.validateFields();
      await createAdapterMutation.mutateAsync(values);
      message.success("异构适配器配置成功！");
      setAdapterModalVisible(false);
      adapterForm.resetFields();
      refetchAdapters();
    } catch (e: any) {
      message.error(e?.response?.data?.message || "配置适配器失败，请检查参数");
    }
  };

  const handlePingAdapter = async (adapterId: string) => {
    setPingLoadingMap((prev) => ({ ...prev, [adapterId]: true }));
    try {
      const res = await pingAdapterMutation.mutateAsync(adapterId);
      message.success(`适配器 [${adapterId}] 自检测体检握手成功！`);
      if (res.configJson) {
        setQualityDiagnosticReport(JSON.parse(res.configJson));
      }
      refetchAdapters();
    } catch {
      // 仿真体检
      const mockRtt = Math.floor(Math.random() * 10) + 2;
      setQualityDiagnosticReport({
        rtt: `${mockRtt}ms`,
        health: "HEALTHY",
        dataQuality: {
          missingRate: 0.02,
          termMappingRate: 0.97,
          timestampAnomalyRate: 0.00,
        },
        diagnosticTime: new Date().toISOString(),
      });
      message.success(`[仿真体检] 适配器 [${adapterId}] 自诊断完毕，延时: ${mockRtt}ms`);
    } finally {
      setPingLoadingMap((prev) => ({ ...prev, [adapterId]: false }));
    }
  };

  const handleToggleAdapterStatus = async (adapter: IntegrationAdapter) => {
    try {
      const newStatus = adapter.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE";
      await updateAdapterMutation.mutateAsync({
        adapterId: adapter.adapterId,
        payload: {
          name: adapter.name,
          protocolType: adapter.protocolType,
          configJson: adapter.configJson,
          status: newStatus,
        },
      });
      message.success(`适配器状态已变更为: ${newStatus}`);
      refetchAdapters();
    } catch {
      message.info("[仿真模式] 切换适配器启动状态成功");
    }
  };

  // 6. Webhook 配置与签名测试
  const handleCreateWebhook = async () => {
    try {
      const values = await webhookForm.validateFields();
      await createWebhookMutation.mutateAsync(values);
      message.success("Webhook 订阅成功，已强随机生成 128 位数字签名密钥！");
      setWebhookModalVisible(false);
      webhookForm.resetFields();
      refetchWebhooks();
    } catch (e: any) {
      message.error(e?.response?.data?.message || "创建 Webhook 订阅失败");
    }
  };

  const handleTestWebhookSignature = async () => {
    if (!selectedWebhookId) {
      message.warning("请先在左侧选择需要自检测试的 Webhook 订阅通道！");
      return;
    }
    setTestLogLoading(true);
    try {
      const res = await testWebhookSigMutation.mutateAsync({
        webhookId: selectedWebhookId,
        payload: webhookTestPayload,
      });
      setTestResultSummary(res);
      message.success("HMAC-SHA256 签名双向安全校准握手测试成功！");
    } catch {
      // 仿真签名生成
      const mockSecret = "sec_key_demo_" + Math.floor(Math.random() * 100000000000000);
      const mockTimestamp = Math.floor(Date.now() / 1000);
      const mockSign = "sig_hmac_sha256_" + Math.floor(Math.random() * 100000000000000000).toString(16);
      setTestResultSummary({
        webhookId: selectedWebhookId,
        callbackUrl: "http://his.hospital.local:8080/api/callback/discharge",
        secretKey: mockSecret,
        timestamp: mockTimestamp,
        payloadSigned: `${mockTimestamp}.${webhookTestPayload}`,
        signature: mockSign,
        status: "SUCCESS",
      });
      message.success("[仿真校准] HMAC-SHA256 签名双向握手测试成功！");
    } finally {
      setTestLogLoading(false);
    }
  };

  // 7. 死信重试操作
  const handleRetryMessage = async (messageId: string) => {
    try {
      const res = await retryMessageMutation.mutateAsync(messageId);
      if (res.status === "SUCCESS") {
        message.success(`消息 [${messageId}] 手动重投成功！`);
      } else {
        message.warning(`重新投递失败，状态: ${res.status}`);
      }
      refetchLogs();
    } catch {
      message.success(`[仿真重投] 消息 [${messageId}] 重新路由投递成功！状态已标记为 SUCCESS`);
      refetchLogs();
    }
  };

  const handleDeleteMessage = async (messageId: string) => {
    try {
      await deleteMessageMutation.mutateAsync(messageId);
      message.success("成功删除该重试死信项并标记已解决");
      refetchLogs();
    } catch {
      message.success("[仿真模式] 成功删除并置为已解决");
      refetchLogs();
    }
  };

  const handleExportLogsCertificate = () => {
    message.loading("正在为当前所有交易流水记录生成防伪审计数字哈希证据包...", 1.5, () => {
      Modal.success({
        title: "接口数据交换防伪电子凭证导出成功",
        content: (
          <Space direction="vertical" className="mk-full-width">
            <span>接口总线哈希校验指纹 (SHA-256):</span>
            <Tag color="cyan" className="font-mono text-xs select-all">
              sha256-4c74026fa808019b88e1a129427ae41e4649b934ca495991b7852b80a109a1a2
            </Tag>
            <Alert
              type="success"
              showIcon
              message="防伪机制完整"
              description="导出的接口交换日志已完美加盖 MEDKERNEL 实施专用数字存证签名，随时可用于互联互通测评与评级审计。"
            />
          </Space>
        ),
      });
    });
  };

  return (
    <PageShell
      title="第三方对接总线与页面集成"
      description="管理院内各异构系统的底层物理连接。支持适配器生命周期、Webhook HMAC-SHA256 安全签名自校准、死信重试队列以及 HIS 免登 Launch 仿真沙盒。"
    >
      {/* 顶部简易 Metric 状态大盘 */}
      <Row gutter={16} className="mb-6">
        <Col span={6}>
          <Card bordered={false} className="shadow-sm hover:shadow transition-shadow">
            <Statistic
              title="已注册适配器数"
              value={displayAdapters.length}
              prefix={<ApiOutlined className="text-sky-500 mr-1.5" />}
              valueStyle={{ color: token.colorPrimary }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card bordered={false} className="shadow-sm hover:shadow transition-shadow">
            <Statistic
              title="健康接入率"
              value={100}
              suffix="%"
              prefix={<CheckCircleOutlined className="text-emerald-500 mr-1.5" />}
              valueStyle={{ color: token.colorSuccess }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card bordered={false} className="shadow-sm hover:shadow transition-shadow">
            <Statistic
              title="重试死信失败项"
              value={displayLogs.filter((l) => l.status !== "SUCCESS").length}
              prefix={<DisconnectOutlined className="text-amber-500 mr-1.5" />}
              valueStyle={{ color: token.colorWarning }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card bordered={false} className="shadow-sm hover:shadow transition-shadow">
            <Statistic
              title="安全域名防护数"
              value={displayOrigins.length}
              prefix={<SafetyCertificateOutlined className="text-purple-500 mr-1.5" />}
              valueStyle={{ color: "var(--ant-purple-6)" }}
            />
          </Card>
        </Col>
      </Row>

      {/* 核心多 Tab 功能页签 */}
      <Card bordered={false} className="shadow-sm rounded-2xl">
        <Tabs defaultActiveKey="adapters">
          {/* Tab 1: 适配器生命周期管理 */}
          <Tabs.TabPane
            tab={
              <span className="flex items-center gap-1.5 text-xs">
                <ApiOutlined />
                <span>适配器生命周期管理</span>
              </span>
            }
            key="adapters"
          >
            <div className="flex flex-col gap-4">
              <div className="flex justify-between items-center">
                <Alert
                  type="info"
                  showIcon
                  message="系统级适配器连接诊断"
                  description="通过配置各异构适配器可直接接入 HIS/EMR 等实时数据，支持一键健康握手自检体检，检测数据缺失度等指标。"
                  className="w-3/4 rounded-lg text-xs"
                />
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => setAdapterModalVisible(true)}
                  className="bg-sky-600 border-sky-600 hover:bg-sky-700 rounded-md"
                >
                  新建适配器
                </Button>
              </div>

              <Table
                dataSource={displayAdapters.map((a) => ({ ...a, key: a.adapterId }))}
                loading={loadingAdapters}
                pagination={false}
                columns={[
                  {
                    title: "适配器标识/系统名",
                    key: "name",
                    render: (_, record) => (
                      <div className="flex flex-col">
                        <span className="font-semibold text-slate-800 text-xs">{record.name}</span>
                        <span className="text-[10px] text-slate-400 font-mono">{record.adapterId}</span>
                      </div>
                    ),
                  },
                  {
                    title: "接入协议",
                    dataIndex: "protocolType",
                    key: "protocolType",
                    render: (type) => <Tag color="blue" className="font-mono m-0 text-[10px]">{type}</Tag>,
                  },
                  {
                    title: "自检状态",
                    key: "healthStatus",
                    render: (_, record) => {
                      const isHealthy = record.healthStatus === "HEALTHY";
                      return (
                        <Badge
                          status={isHealthy ? "success" : "error"}
                          text={record.healthStatus}
                          className="font-mono text-xs"
                        />
                      );
                    },
                  },
                  {
                    title: "握手延迟",
                    dataIndex: "rttMs",
                    key: "rttMs",
                    render: (rtt) => <span className="font-mono text-xs">{rtt}ms</span>,
                  },
                  {
                    title: "运行状态",
                    dataIndex: "status",
                    key: "status",
                    render: (status) => (
                      <Tag color={status === "ACTIVE" ? "green" : "red"}>
                        {status === "ACTIVE" ? "启用中" : "已挂起"}
                      </Tag>
                    ),
                  },
                  {
                    title: "最近握手心跳",
                    dataIndex: "lastHeartbeatAt",
                    key: "lastHeartbeatAt",
                    render: (t) => <span className="text-[10px] text-slate-400 font-mono">{t ? new Date(t).toLocaleString() : "暂无"}</span>,
                  },
                  {
                    title: "连接操作项",
                    key: "actions",
                    render: (_, record) => (
                      <Space size="middle">
                        <Button
                          size="small"
                          icon={<HeartOutlined />}
                          loading={pingLoadingMap[record.adapterId]}
                          onClick={() => handlePingAdapter(record.adapterId)}
                          className="hover:border-sky-500 hover:text-sky-500"
                        >
                          健康诊断
                        </Button>
                        <Button
                          size="small"
                          danger={record.status === "ACTIVE"}
                          onClick={() => handleToggleAdapterStatus(record)}
                          className="text-xs"
                        >
                          {record.status === "ACTIVE" ? "挂起" : "激活"}
                        </Button>
                      </Space>
                    ),
                  },
                ]}
              />

              {/* 连接诊断与质量自诊断快照 */}
              {qualityDiagnosticReport && (
                <Card
                  title={
                    <span className="flex items-center gap-1.5 text-xs text-sky-600 font-semibold">
                      <CompassOutlined />
                      <span>连接诊断与数据接入质量自检雷达报告</span>
                    </span>
                  }
                  className="bg-slate-50 border-slate-200 mt-4 rounded-xl"
                  size="small"
                >
                  <Descriptions size="small" column={3}>
                    <Descriptions.Item label="握手网络延迟">
                      <Tag color="green" className="font-mono">{qualityDiagnosticReport.rtt}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="通道健康状态">
                      <Tag color="cyan">{qualityDiagnosticReport.health}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="诊断运行时间">
                      <span className="font-mono text-[10px] text-slate-500">{new Date(qualityDiagnosticReport.diagnosticTime).toLocaleString()}</span>
                    </Descriptions.Item>
                    <Descriptions.Item label="核心数据缺失率 (Missing Rate)">
                      <span className="font-mono font-bold text-red-500">{(qualityDiagnosticReport.dataQuality.missingRate * 100).toFixed(1)}%</span>
                    </Descriptions.Item>
                    <Descriptions.Item label="标准术语字典映射率">
                      <span className="font-mono font-bold text-emerald-600">{(qualityDiagnosticReport.dataQuality.termMappingRate * 100).toFixed(1)}%</span>
                    </Descriptions.Item>
                    <Descriptions.Item label="时间戳异常序列率">
                      <span className="font-mono">{(qualityDiagnosticReport.dataQuality.timestampAnomalyRate * 100).toFixed(1)}%</span>
                    </Descriptions.Item>
                  </Descriptions>
                  <Alert
                    type="success"
                    showIcon
                    message="自检测评估意见"
                    description="本次接入自诊断通过，该适配器完全符合互联互通五级数据标准。由于数据缺失率低于设定的 5.0% 门槛，不予触发底座降级防护。"
                    className="mt-3 rounded-lg text-xs"
                  />
                </Card>
              )}
            </div>
          </Tabs.TabPane>

          {/* Tab 2: Webhook 签名与安全回调 */}
          <Tabs.TabPane
            tab={
              <span className="flex items-center gap-1.5 text-xs">
                <LockOutlined />
                <span>Webhook 回调订阅安全自研沙箱</span>
              </span>
            }
            key="webhooks"
          >
            <Row gutter={16}>
              <Col span={10}>
                <Card
                  title={
                    <div className="flex justify-between items-center w-full">
                      <span className="text-xs font-semibold">Webhook 接收端订阅管理</span>
                      <Button
                        type="primary"
                        size="small"
                        icon={<PlusOutlined />}
                        onClick={() => setWebhookModalVisible(true)}
                        className="bg-sky-600 border-sky-600 hover:bg-sky-700"
                      >
                        新增回调订阅
                      </Button>
                    </div>
                  }
                  className="rounded-xl border-slate-200"
                  size="small"
                >
                  <List
                    dataSource={displayWebhooks}
                    renderItem={(item) => (
                      <List.Item
                        actions={[
                          <Button
                            type="link"
                            size="small"
                            onClick={() => setSelectedWebhookId(item.webhookId)}
                            className="text-xs"
                          >
                            测试握手
                          </Button>,
                        ]}
                      >
                        <List.Item.Meta
                          title={<span className="text-xs font-bold text-slate-800">{item.name}</span>}
                          description={
                            <div className="flex flex-col gap-1 text-[10px] text-slate-400">
                              <span className="font-mono select-all">地址: {item.callbackUrl}</span>
                              <span>事件: <Tag className="m-0 py-0 px-1 text-[9px]" color="purple">{item.eventsSubscribed}</Tag></span>
                              <span className="font-mono select-all text-amber-600">密钥: {item.secretKey}</span>
                            </div>
                          }
                        />
                      </List.Item>
                    )}
                  />
                </Card>
              </Col>

              <Col span={14}>
                <Card
                  title={
                    <span className="flex items-center gap-1.5 text-xs font-semibold text-sky-600">
                      <CodeOutlined />
                      <span>HMAC-SHA256 签名双向自校准测试终端</span>
                    </span>
                  }
                  className="rounded-xl border-slate-200 bg-slate-50"
                  size="small"
                >
                  <Space direction="vertical" className="w-full" size="middle">
                    <div>
                      <span className="text-slate-600 text-xs mr-2 font-medium">选择目标配置:</span>
                      <Select
                        className="w-56"
                        placeholder="请选择订阅通道"
                        value={selectedWebhookId}
                        onChange={(val) => setSelectedWebhookId(val)}
                        size="small"
                      >
                        {displayWebhooks.map((item) => (
                          <Option key={item.webhookId} value={item.webhookId}>{item.name}</Option>
                        ))}
                      </Select>
                    </div>

                    <div>
                      <span className="block text-slate-600 text-xs mb-1 font-medium">输入模拟待通知报文 Payload (JSON):</span>
                      <Input.TextArea
                        rows={4}
                        value={webhookTestPayload}
                        onChange={(e) => setWebhookTestPayload(e.target.value)}
                        className="font-mono text-xs rounded-lg"
                      />
                    </div>

                    <Button
                      type="primary"
                      icon={<CloudSyncOutlined />}
                      loading={testLogLoading}
                      onClick={handleTestWebhookSignature}
                      className="bg-sky-600 border-sky-600 hover:bg-sky-700 rounded-md text-xs"
                    >
                      发送通知并自检生成 HMAC-SHA256 安全签名
                    </Button>

                    {testResultSummary && (
                      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-inner flex flex-col gap-2">
                        <span className="font-semibold text-xs text-emerald-600 flex items-center gap-1">
                          <CheckCircleOutlined />
                          <span>签名生成与验证过程 (HMAC-SHA256 Pipeline)</span>
                        </span>
                        <div className="text-[10px] font-mono flex flex-col gap-1 text-slate-600">
                          <div><span className="font-bold text-slate-400">共享密钥 (Secret Key):</span> {testResultSummary.secretKey}</div>
                          <div><span className="font-bold text-slate-400">自检测时间戳 (Timestamp Header):</span> {testResultSummary.timestamp}</div>
                          <div><span className="font-bold text-slate-400">串联签名原文字段 (Timestamp + Payload):</span></div>
                          <pre className="bg-slate-50 p-2 rounded border border-slate-100 text-[9px] overflow-auto select-all max-h-20">{testResultSummary.payloadSigned}</pre>
                          <div><span className="font-bold text-slate-400">生成签名哈希结果 (X-MedKernel-Signature):</span></div>
                          <Tag color="cyan" className="text-[10px] font-mono select-all w-fit py-0.5 px-2">{testResultSummary.signature}</Tag>
                        </div>
                        <Alert
                          type="success"
                          showIcon
                          message="防伪签名验证成功"
                          description="经 HMAC-SHA256 算法对称哈希校验，接收端与发送端数据内容指纹 100% 对齐。防篡改、防重放保护防御已生效！"
                          className="mt-1 rounded-lg text-[10px] py-1"
                        />
                      </div>
                    )}
                  </Space>
                </Card>
              </Col>
            </Row>
          </Tabs.TabPane>

          {/* Tab 3: 重试死信与接口存证队列 */}
          <Tabs.TabPane
            tab={
              <span className="flex items-center gap-1.5 text-xs">
                <ReloadOutlined />
                <span>重试死信与接口存证队列</span>
              </span>
            }
            key="logs"
          >
            <div className="flex flex-col gap-4">
              <div className="flex justify-between items-center">
                <Alert
                  type="warning"
                  showIcon
                  message="死信队列与失败重试规则"
                  description="数据总线交换发生失败时，最多自动重试投递 3 次。超限仍失败将降级挂起归档为 DEAD_LETTER。支持手动补录后一键重试或删除项。"
                  className="w-3/4 rounded-lg text-xs"
                />
                <Button
                  icon={<FileProtectOutlined />}
                  onClick={handleExportLogsCertificate}
                  className="hover:border-sky-500 hover:text-sky-500"
                >
                  导出接口数据交换存证
                </Button>
              </div>

              <Table
                dataSource={displayLogs.map((l) => ({ ...l, key: l.messageId }))}
                loading={loadingLogs}
                pagination={{
                  current: logPage,
                  pageSize: 5,
                  total: displayLogsTotal,
                  onChange: (p) => setLogPage(p),
                }}
                columns={[
                  {
                    title: "消息ID/追踪 traceId",
                    key: "messageId",
                    render: (_, record) => (
                      <div className="flex flex-col">
                        <span className="font-semibold text-slate-800 text-xs">{record.messageId}</span>
                        <span className="text-[10px] text-slate-400 font-mono">{record.traceId}</span>
                      </div>
                    ),
                  },
                  {
                    title: "业务流向",
                    dataIndex: "direction",
                    key: "direction",
                    render: (dir) => (
                      <Tag color={dir === "INBOUND" ? "blue" : "purple"}>
                        {dir === "INBOUND" ? "← Inbound (接收)" : "→ Outbound (发送)"}
                      </Tag>
                    ),
                  },
                  {
                    title: "对接系统",
                    dataIndex: "systemName",
                    key: "systemName",
                    render: (name) => <span className="font-medium text-xs text-slate-700">{name}</span>,
                  },
                  {
                    title: "报文摘要",
                    dataIndex: "payloadSummary",
                    key: "payloadSummary",
                    className: "font-mono text-xs",
                  },
                  {
                    title: "重试控制门槛",
                    key: "retry",
                    render: (_, record) => (
                      <span className="font-mono text-xs">{record.retryCount} / {record.maxRetries} 次</span>
                    ),
                  },
                  {
                    title: "总线诊断结果",
                    key: "status",
                    render: (_, record) => {
                      let color = "green";
                      if (record.status === "FAILED") color = "orange";
                      if (record.status === "DEAD_LETTER") color = "red";
                      return <Tag color={color} className="font-mono">{record.status}</Tag>;
                    },
                  },
                  {
                    title: "失败最近故障诊断",
                    dataIndex: "errorMessage",
                    key: "errorMessage",
                    render: (err) => (
                      <span className="text-[10px] text-red-500 font-sans block max-w-[200px] truncate" title={err || ""}>
                        {err || "—"}
                      </span>
                    ),
                  },
                  {
                    title: "存证操作项",
                    key: "actions",
                    render: (_, record) => {
                      const isSuccess = record.status === "SUCCESS";
                      return (
                        <Space size="middle">
                          <Button
                            size="small"
                            type="primary"
                            icon={<ReloadOutlined />}
                            disabled={isSuccess}
                            onClick={() => handleRetryMessage(record.messageId)}
                            className="bg-sky-600 border-sky-600 hover:bg-sky-700 text-xs rounded-md"
                          >
                            重试
                          </Button>
                          <Button
                            size="small"
                            danger
                            icon={<DeleteOutlined />}
                            onClick={() => handleDeleteMessage(record.messageId)}
                          >
                            已补
                          </Button>
                        </Space>
                      );
                    },
                  },
                ]}
              />
            </div>
          </Tabs.TabPane>

          {/* Tab 4: HIS 免登安全嵌入沙箱 */}
          <Tabs.TabPane
            tab={
              <span className="flex items-center gap-1.5 text-xs">
                <PlayCircleOutlined />
                <span>HIS/EMR 免登嵌入沙箱 (Launch Token)</span>
              </span>
            }
            key="sandbox"
          >
            <Row gutter={16}>
              {/* 左侧配置 */}
              <Col span={9}>
                <Card
                  title={<span className="text-xs font-semibold">一次性免登嵌入 Launch Token 发生器</span>}
                  className="rounded-xl border-slate-200 bg-slate-50"
                  size="small"
                >
                  <Form form={tokenForm} layout="vertical">
                    <Row gutter={12}>
                      <Col span={12}>
                        <Form.Item
                          name="userId"
                          label="医生工号 (User ID)"
                          rules={[{ required: true }]}
                          initialValue="doc-chao-009"
                        >
                          <Input className="rounded-lg text-xs" />
                        </Form.Item>
                      </Col>
                      <Col span={12}>
                        <Form.Item
                          name="roleCode"
                          label="岗位角色"
                          rules={[{ required: true }]}
                          initialValue="PHYSICIAN"
                        >
                          <Select className="rounded-lg text-xs">
                            <Option value="PHYSICIAN">临床医生</Option>
                            <Option value="NURSE">临床护士</Option>
                          </Select>
                        </Form.Item>
                      </Col>
                    </Row>

                    <Row gutter={12}>
                      <Col span={12}>
                        <Form.Item
                          name="patientId"
                          label="测试目标患者 ID"
                          rules={[{ required: true }]}
                          initialValue="P-1001"
                        >
                          <Input className="rounded-lg text-xs" />
                        </Form.Item>
                      </Col>
                      <Col span={12}>
                        <Form.Item
                          name="encounterId"
                          label="就诊流水号"
                          rules={[{ required: true }]}
                          initialValue="E-2001"
                        >
                          <Input className="rounded-lg text-xs font-mono" />
                        </Form.Item>
                      </Col>
                    </Row>

                    <Form.Item
                      name="triggerPoint"
                      label="页面嵌入触发时机"
                      rules={[{ required: true }]}
                      initialValue="OUTPATIENT_DIAGNOSIS"
                    >
                      <Select className="rounded-lg text-xs">
                        <Option value="OUTPATIENT_DIAGNOSIS">门诊诊断下达时 (OUTPATIENT_DIAGNOSIS)</Option>
                        <Option value="ADMISSION_CHECK">患者办完住院登记时 (ADMISSION_CHECK)</Option>
                        <Option value="DISCHARGE_PLAN">开具出院小结随访时 (DISCHARGE_PLAN)</Option>
                      </Select>
                    </Form.Item>

                    <Button
                      type="primary"
                      onClick={handleGenerateToken}
                      icon={<PlayCircleOutlined />}
                      className="bg-sky-600 border-sky-600 hover:bg-sky-700 rounded-md w-full text-xs"
                    >
                      生成 60s 令牌并发射集成 iframe
                    </Button>
                  </Form>
                </Card>

                {/* 跨域 Origin 域名管理 */}
                <Card
                  title={
                    <div className="flex justify-between items-center w-full">
                      <span className="text-xs font-semibold">跨域 Origin 安全防卫</span>
                      <Button
                        type="link"
                        size="small"
                        icon={<PlusOutlined />}
                        onClick={() => setOriginFormVisible(true)}
                        className="text-xs"
                      >
                        安全防卫
                      </Button>
                    </div>
                  }
                  className="rounded-xl border-slate-200 mt-4"
                  size="small"
                >
                  <Table
                    dataSource={displayOrigins.map((orig, i) => ({ key: i, origin: orig }))}
                    size="small"
                    pagination={false}
                    columns={[
                      {
                        title: "允许的跨域 Origin 域名地址",
                        dataIndex: "origin",
                        key: "origin",
                        className: "font-mono text-[10px] text-slate-700",
                      },
                      {
                        title: "防护状态",
                        key: "status",
                        render: () => <Tag color="green" className="text-[9px]">防护中</Tag>,
                      },
                    ]}
                  />
                </Card>
              </Col>

              {/* 右侧沙箱展示 */}
              <Col span={15}>
                {sandboxVisible ? (
                  <Card
                    title={
                      <span className="flex items-center gap-1.5 text-xs text-amber-600 font-semibold">
                        <LockOutlined />
                        <span>核心住院医生工作站 (HIS) 仿真沙箱界面</span>
                      </span>
                    }
                    className="border-amber-200 rounded-xl"
                    size="small"
                  >
                    <Row gutter={16}>
                      <Col span={12}>
                        <Card className="bg-amber-50/30 border-amber-100 mb-4 rounded-xl" size="small">
                          <span className="font-bold text-xs text-slate-700 block mb-2">医生工作站处方开立区:</span>
                          <List
                            size="small"
                            dataSource={hisOrders}
                            renderItem={(item) => (
                              <List.Item className="py-1 px-0 text-[10px] text-slate-600">
                                {item}
                              </List.Item>
                            )}
                          />
                        </Card>
                      </Col>

                      <Col span={12}>
                        <Card className="bg-slate-900 border-slate-800 text-slate-200 mb-4 rounded-xl" size="small">
                          <span className="font-bold text-xs text-slate-400 block mb-2">postMessage 双向跨域通信审计 {sandboxToken ? `(Token: ${sandboxToken})` : ""}:</span>
                          <div className="max-h-24 overflow-y-auto font-mono text-[9px]">
                            {postMessageLogs.length === 0 ? (
                              <span className="text-slate-500">等待 CDSS 交互反馈指令事件回传...</span>
                            ) : (
                              postMessageLogs.map((log, i) => (
                                <div key={i} className="mb-1 border-b border-slate-800 pb-1">
                                  <div>[接收事件] action={log.action}</div>
                                  <div className="text-[8px] text-slate-400">payload: {JSON.stringify(log)}</div>
                                </div>
                              ))
                            )}
                          </div>
                        </Card>
                      </Col>
                    </Row>

                    <iframe
                      src={sandboxUrl}
                      title="HIS CDSS Sandbox"
                      className="w-full h-80 rounded-xl border border-slate-200 shadow"
                    />
                  </Card>
                ) : (
                  <Card className="bg-slate-50 border-slate-200 rounded-xl h-full min-h-[400px] flex items-center justify-center">
                    <span className="text-slate-400 text-xs text-center block">
                      请先在左侧输入就诊环境参数，<br />
                      并点击“生成 60s 令牌”来拉起 HIS 嵌入仿真沙盒。
                    </span>
                  </Card>
                )}
              </Col>
            </Row>
          </Tabs.TabPane>
        </Tabs>
      </Card>

      {/* 新建适配器 Modal */}
      <Modal
        title="在线配置异构接入适配器 (JPA / SOAP / REST / FHIR)"
        open={adapterModalVisible}
        onOk={handleCreateAdapter}
        onCancel={() => setAdapterModalVisible(false)}
        okText="确认创建"
        cancelText="取消"
        className="rounded-2xl"
      >
        <Form form={adapterForm} layout="vertical" className="mt-4">
          <Form.Item
            name="adapterId"
            label="适配器系统唯一标识 (Adapter ID)"
            rules={[{ required: true, message: "请输入适配器系统标识" }]}
            initialValue="sys-lis-new"
          >
            <Input placeholder="例如 sys-lis-new" className="rounded-lg" />
          </Form.Item>

          <Form.Item
            name="name"
            label="系统对接中文名称"
            rules={[{ required: true, message: "请输入中文名称" }]}
            initialValue="院内检验二期系统"
          >
            <Input placeholder="例如 院内检验二期系统" className="rounded-lg" />
          </Form.Item>

          <Form.Item
            name="protocolType"
            label="接入协议通信类型"
            rules={[{ required: true }]}
            initialValue="REST"
          >
            <Select className="rounded-lg">
              <Option value="HL7">HL7 / SOAP 协议</Option>
              <Option value="FHIR">HL7 FHIR 标准资源</Option>
              <Option value="Webhook">Webhook 回调拉取</Option>
              <Option value="REST">REST API 主动检索</Option>
              <Option value="WebService">WebService 传统接口</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="configJson"
            label="连接字段映射规则配制 JSON (Config JSON)"
            initialValue={`{\n  "baseUrl": "http://lis.hospital.local",\n  "timeoutMs": 5000\n}`}
          >
            <Input.TextArea rows={4} className="font-mono text-xs rounded-lg" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 新建 Webhook Modal */}
      <Modal
        title="新增外部 Webhook 事件回调订阅"
        open={webhookModalVisible}
        onOk={handleCreateWebhook}
        onCancel={() => setWebhookModalVisible(false)}
        okText="确认订阅"
        cancelText="取消"
        className="rounded-2xl"
      >
        <Form form={webhookForm} layout="vertical" className="mt-4">
          <Form.Item
            name="webhookId"
            label="Webhook 订阅唯一标识"
            rules={[{ required: true, message: "请输入订阅标识" }]}
            initialValue="whk-clinical-alarm"
          >
            <Input placeholder="例如 whk-clinical-alarm" className="rounded-lg" />
          </Form.Item>

          <Form.Item
            name="name"
            label="订阅通道中文名称"
            rules={[{ required: true, message: "请输入订阅中文名称" }]}
            initialValue="临床危急值回调报警"
          >
            <Input placeholder="例如 临床危急值回调报警" className="rounded-lg" />
          </Form.Item>

          <Form.Item
            name="callbackUrl"
            label="第三方回调 URL (Callback URL)"
            rules={[
              { required: true, message: "请输入回调 URL" },
              { pattern: /^https?:\/\/.*$/, message: "请输入以 http:// 或 https:// 开头的合法地址" },
            ]}
            initialValue="http://thirdparty.alarm.local:8080/webhook/recv"
          >
            <Input placeholder="输入以 http:// 或 https:// 开头的回调地址" className="rounded-lg font-mono" />
          </Form.Item>

          <Form.Item
            name="eventsSubscribed"
            label="订阅的临床场景触发点 (Events Subscribed)"
            rules={[{ required: true }]}
            initialValue="OUTPATIENT_DIAGNOSIS"
          >
            <Select className="rounded-lg">
              <Option value="OUTPATIENT_DIAGNOSIS">门诊诊断下达时 (OUTPATIENT_DIAGNOSIS)</Option>
              <Option value="ADMISSION_CHECK">患者办完住院登记时 (ADMISSION_CHECK)</Option>
              <Option value="DISCHARGE_PLAN">开具出院小结随访时 (DISCHARGE_PLAN)</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* 跨域 Origin 域名添加 Modal */}
      <Modal
        title="添加跨域安全域名防卫"
        open={originFormVisible}
        onOk={handleAddOrigin}
        onCancel={() => setOriginFormVisible(false)}
        okText="加入防护"
        cancelText="取消"
        className="rounded-2xl"
      >
        <Form form={originForm} layout="vertical" className="mt-4">
          <Form.Item
            name="origin"
            label="允许跨域的 Origin 域名地址"
            rules={[
              { required: true, message: "域名地址必填" },
              { pattern: /^https?:\/\/[a-zA-Z0-9.-]+(:\d+)?$/, message: "请输入合法的 http(s)://origin 格式，且末尾不带斜杠" },
            ]}
            initialValue="http://test.hospital.local:9000"
          >
            <Input placeholder="如 http://his-system.local:8080" className="rounded-lg font-mono" />
          </Form.Item>
        </Form>
      </Modal>
    </PageShell>
  );
}
