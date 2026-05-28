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
} from "antd";
import {
  PlusOutlined,
  PlayCircleOutlined,
  FolderOpenOutlined,
  ApartmentOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import {
  useSpecialtyPackages,
  useCreateSpecialtyPackage,
  usePathwayTemplates,
  usePathwayTemplateDetail,
  useCreatePathwayTemplate,
  usePublishPathwayTemplate,
  useSimulatePathway,
} from "@/shared/api/hooks";
import type { SpecialtyPackage, PathwayTemplate, PathwayTemplateStatus } from "@/shared/api/hooks";

const { TextArea } = Input;
const { Option } = Select;

// 默认路径节点 JSON 引导模板
const DEFAULT_NODES_JSON = `[
  { "nodeCode": "START", "name": "入径准入与评估", "nodeType": "START", "sortOrder": 1, "responsibleRole": "PRIMARY_NURSE", "timeWindowMinutes": 30, "terminalFlag": false },
  { "nodeCode": "TREAT_PLAN", "name": "制定抗感染化疗方案", "nodeType": "PROCESS", "sortOrder": 2, "responsibleRole": "ATTENDING_PHYSICIAN", "timeWindowMinutes": 120, "terminalFlag": false },
  { "nodeCode": "CHECK_LEVEL", "name": "不良反应监测与疗效评估", "nodeType": "BRANCH", "sortOrder": 3, "responsibleRole": "ATTENDING_PHYSICIAN", "timeWindowMinutes": 1440, "terminalFlag": false },
  { "nodeCode": "DISCHARGE", "name": "办理出院与健康宣教", "nodeType": "STOP", "sortOrder": 4, "responsibleRole": "PRIMARY_NURSE", "timeWindowMinutes": 180, "terminalFlag": true }
]`;

// 默认连线 JSON 引导模板
const DEFAULT_EDGES_JSON = `[
  { "edgeCode": "E1", "fromNodeCode": "START", "toNodeCode": "TREAT_PLAN", "edgeType": "STANDARD", "priority": 1 },
  { "edgeCode": "E2", "fromNodeCode": "TREAT_PLAN", "toNodeCode": "CHECK_LEVEL", "edgeType": "STANDARD", "priority": 1 },
  { "edgeCode": "E3", "fromNodeCode": "CHECK_LEVEL", "toNodeCode": "DISCHARGE", "edgeType": "CONDITIONAL", "conditionJson": "{\\"fact\\": \\"patient.condition\\", \\"operator\\": \\"equals\\", \\"value\\": \\"STABLE\\"}", "priority": 1 },
  { "edgeCode": "E4", "fromNodeCode": "CHECK_LEVEL", "toNodeCode": "TREAT_PLAN", "edgeType": "VARIANCE", "conditionJson": "{\\"fact\\": \\"patient.condition\\", \\"operator\\": \\"equals\\", \\"value\\": \\"DETERIORATED\\"}", "priority": 2 }
]`;

export default function PathwayTemplates() {
  const [page, setPage] = useState<number>(1);
  const [size] = useState<number>(10);

  // 过滤条件状态
  const [statusFilter, setStatusFilter] = useState<PathwayTemplateStatus | undefined>(undefined);
  const [diseaseFilter, setDiseaseFilter] = useState<string>("");
  const [packageFilter, setPackageFilter] = useState<string>("");

  // 弹窗与抽屉可见性
  const [packageDrawerVisible, setPackageDrawerVisible] = useState<boolean>(false);
  const [createTemplateVisible, setCreateTemplateVisible] = useState<boolean>(false);
  const [selectedTemplateId, setSelectedTemplateId] = useState<string | null>(null);

  // 仿真运行态
  const [simulateStartNode, setSimulateStartNode] = useState<string>("START");
  const [simulateResult, setSimulateResult] = useState<string[] | null>(null);

  // API 查询
  const {
    data: listData,
    isLoading: listLoading,
    refetch: refetchList,
  } = usePathwayTemplates({
    status: statusFilter,
    diseaseCode: diseaseFilter || undefined,
    packageId: packageFilter || undefined,
    page,
    size,
  });

  const {
    data: detailData,
    isLoading: detailLoading,
    refetch: refetchDetail,
  } = usePathwayTemplateDetail(selectedTemplateId || "");

  const { data: packagesData, refetch: refetchPackages } = useSpecialtyPackages({
    page: 1,
    size: 100,
  });

  // API 突变动作
  const createPackageMutation = useCreateSpecialtyPackage();
  const createTemplateMutation = useCreatePathwayTemplate();
  const publishTemplateMutation = usePublishPathwayTemplate();
  const simulateMutation = useSimulatePathway(selectedTemplateId || "");

  // 表单绑定
  const [packageForm] = Form.useForm();
  const [templateForm] = Form.useForm();

  // 创建专病包
  const handleCreatePackage = async () => {
    try {
      const values = await packageForm.validateFields();
      await createPackageMutation.mutateAsync(values);
      message.success("专病包资产草稿创建成功");
      packageForm.resetFields();
      refetchPackages();
    } catch {
      message.error("创建专病包失败，请检查参数");
    }
  };

  // 创建路径模板
  const handleCreateTemplate = async () => {
    try {
      const values = await templateForm.validateFields();

      // 解析节点与连线 JSON
      let parsedNodes = [];
      let parsedEdges = [];
      try {
        parsedNodes = JSON.parse(values.nodesJson);
        parsedEdges = JSON.parse(values.edgesJson);
      } catch {
        message.error("节点或连线 JSON 格式不合法，请检查！");
        return;
      }

      await createTemplateMutation.mutateAsync({
        packageId: values.packageId,
        templateCode: values.templateCode,
        name: values.name,
        diseaseCode: values.diseaseCode,
        templateLevel: values.templateLevel,
        sourceRef: values.sourceRef,
        description: values.description,
        entryCriteriaJson: "{}",
        exitCriteriaJson: "{}",
        nodes: parsedNodes,
        edges: parsedEdges,
      });

      message.success("专病路径模板草稿创建成功");
      setCreateTemplateVisible(false);
      templateForm.resetFields();
      refetchList();
    } catch (err: any) {
      message.error(err.response?.data?.message || "创建路径模板失败");
    }
  };

  // 发布路径模板 (门禁校验)
  const handlePublishTemplate = async () => {
    if (!selectedTemplateId) return;
    try {
      await publishTemplateMutation.mutateAsync(selectedTemplateId);
      message.success("路径模板发布成功，已正式上线运行！");
      refetchDetail();
      refetchList();
    } catch (err: any) {
      Modal.error({
        title: "路径发布门禁拒绝",
        content: err.response?.data?.message || "未通过路径闭环或时窗门禁核查，禁止上线。",
      });
    }
  };

  // 沙箱运行轨迹仿真
  const handleSimulate = async () => {
    if (!selectedTemplateId) return;
    try {
      const result = await simulateMutation.mutateAsync({
        startNodeCode: simulateStartNode,
        contextJson: "{}",
      });
      setSimulateResult(result.simulatedPath || []);
      message.success("路径轨迹仿真模拟运行成功");
    } catch (err: any) {
      message.error(err.response?.data?.message || "路径仿真推进失败");
    }
  };

  // 路径模板表格列
  const columns = [
    {
      title: "模板代码",
      dataIndex: "templateCode",
      key: "templateCode",
      render: (text: string) => <Tag color="geekblue">{text}</Tag>,
    },
    {
      title: "路径名称",
      dataIndex: "name",
      key: "name",
      className: "font-semibold text-gray-800",
    },
    {
      title: "关联病种",
      dataIndex: "diseaseCode",
      key: "diseaseCode",
      render: (text: string) => <Tag color="cyan">{text}</Tag>,
    },
    {
      title: "层级",
      dataIndex: "templateLevel",
      key: "templateLevel",
    },
    {
      title: "版本",
      dataIndex: "templateVersion",
      key: "templateVersion",
      render: (v: number) => `v${v}.0`,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      render: (status: PathwayTemplateStatus) => {
        const config = {
          DRAFT: { color: "warning", text: "设计中(DRAFT)" },
          PUBLISHED: { color: "success", text: "运行中(PUBLISHED)" },
          OFFLINE: { color: "default", text: "已下线(OFFLINE)" },
        };
        return <Badge status={config[status].color as any} text={config[status].text} />;
      },
    },
    {
      title: "管理动作",
      key: "action",
      render: (record: PathwayTemplate) => (
        <Button
          type="link"
          icon={<ApartmentOutlined />}
          onClick={() => {
            setSelectedTemplateId(record.templateId);
            setSimulateResult(null);
          }}
          className="text-emerald-600 hover:text-emerald-900 font-semibold"
        >
          设计与仿真校验
        </Button>
      ),
    },
  ];

  return (
    <PageShell
      title="路径中枢"
      description="配置并维护专病临床路径标准，设定生命周期节点与变异流转边拓扑，提供沙箱仿真与时窗门禁发布验证。"
    >
      {/* 筛选过滤条 */}
      <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 mb-6">
        <Form layout="inline" className="flex flex-wrap gap-4 items-center w-full">
          <Form.Item label="状态">
            <Select
              placeholder="选择状态"
              allowClear
              value={statusFilter}
              onChange={setStatusFilter}
              className="w-[140px]"
            >
              <Option value="DRAFT">设计中</Option>
              <Option value="PUBLISHED">运行中</Option>
              <Option value="OFFLINE">已下线</Option>
            </Select>
          </Form.Item>
          <Form.Item label="病种编码">
            <Input
              placeholder="例如 I10"
              allowClear
              value={diseaseFilter}
              onChange={(e) => setDiseaseFilter(e.target.value)}
              className="w-[140px]"
            />
          </Form.Item>
          <Form.Item label="归属专病包">
            <Select
              placeholder="全部专病包"
              allowClear
              value={packageFilter}
              onChange={setPackageFilter}
              className="w-[200px]"
            >
              {packagesData?.items?.map((pkg: SpecialtyPackage) => (
                <Option key={pkg.packageId} value={pkg.packageId}>
                  {pkg.name} ({pkg.packageVersion})
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item className="ml-auto flex gap-2">
            <Button
              icon={<FolderOpenOutlined />}
              onClick={() => setPackageDrawerVisible(true)}
              className="rounded-lg font-medium border-emerald-500 text-emerald-600 hover:bg-emerald-50"
            >
              管理专病包
            </Button>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => {
                templateForm.setFieldsValue({
                  templateLevel: "CLINICAL",
                  nodesJson: DEFAULT_NODES_JSON,
                  edgesJson: DEFAULT_EDGES_JSON,
                });
                setCreateTemplateVisible(true);
              }}
              className="rounded-lg font-medium bg-emerald-600 border-emerald-600 hover:bg-emerald-700"
            >
              新建路径模板
            </Button>
          </Form.Item>
        </Form>
      </div>

      {/* 主数据台账列表 */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <Table
          columns={columns}
          dataSource={listData?.items || []}
          rowKey="id"
          loading={listLoading}
          pagination={{
            current: page,
            pageSize: size,
            total: listData?.total || 0,
            onChange: (p) => setPage(p),
            showTotal: (t) => `共 ${t} 个临床受控路径模型`,
          }}
          className="medkernel-table"
        />
      </div>

      {/* 专病包资产管理 Drawer */}
      <Drawer
        title="租户专病包资产管理"
        width={560}
        onClose={() => setPackageDrawerVisible(false)}
        open={packageDrawerVisible}
        destroyOnClose
      >
        <Alert
          message="专病包是临床路径和质控资产的容器实体，受租户级别物理强隔离与版本升级灰度发布控制。"
          type="info"
          showIcon
          className="mb-6 rounded-lg"
        />

        <Card title="新建专病包草稿" className="mb-6 border-gray-200 shadow-sm rounded-xl">
          <Form form={packageForm} layout="vertical" onFinish={handleCreatePackage}>
            <Row gutter={12}>
              <Col span={12}>
                <Form.Item name="packageCode" label="专病包编码" rules={[{ required: true }]}>
                  <Input placeholder="如 PKG-COP-001" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="diseaseCode" label="病种代码 (ICD)" rules={[{ required: true }]}>
                  <Input placeholder="如 J44" />
                </Form.Item>
              </Col>
            </Row>
            <Form.Item name="name" label="专病包名称" rules={[{ required: true }]}>
              <Input placeholder="如 慢性阻塞性肺疾病合理化诊疗包" />
            </Form.Item>
            <Row gutter={12}>
              <Col span={12}>
                <Form.Item name="packageVersion" label="版本" rules={[{ required: true }]}>
                  <Input placeholder="如 1.0.0" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="sourceRef" label="知识来源" rules={[{ required: true }]}>
                  <Input placeholder="如 中华医学会诊疗指南2025" />
                </Form.Item>
              </Col>
            </Row>
            <Form.Item name="description" label="功能说明与收治摘要">
              <TextArea rows={2} placeholder="输入专病画像说明..." />
            </Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              icon={<PlusOutlined />}
              loading={createPackageMutation.isPending}
              className="w-full bg-emerald-600 border-emerald-600 hover:bg-emerald-700 mt-2"
            >
              提交创建并留痕审计
            </Button>
          </Form>
        </Card>

        <div className="font-semibold text-gray-800 mb-3">已有专病包列表</div>
        <div className="flex flex-col gap-3 overflow-y-auto max-h-[300px]">
          {packagesData?.items?.map((pkg: SpecialtyPackage) => (
            <Card
              key={pkg.packageId}
              size="small"
              className="border-gray-100 bg-gray-50 rounded-lg shadow-sm"
            >
              <Descriptions size="small" column={1} bordered={false}>
                <Descriptions.Item label="名称">
                  <span className="font-semibold text-gray-800">{pkg.name}</span>
                </Descriptions.Item>
                <Descriptions.Item label="包编码">
                  <span className="font-mono text-xs">{pkg.packageCode}</span>
                </Descriptions.Item>
                <Descriptions.Item label="病种/版本">
                  <Tag color="cyan">{pkg.diseaseCode}</Tag>
                  <Tag color="purple">{pkg.packageVersion}</Tag>
                </Descriptions.Item>
              </Descriptions>
            </Card>
          ))}
        </div>
      </Drawer>

      {/* 新建路径模板 Modal */}
      <Modal
        title="新建路径模板模型"
        open={createTemplateVisible}
        onOk={handleCreateTemplate}
        onCancel={() => setCreateTemplateVisible(false)}
        width={780}
        confirmLoading={createTemplateMutation.isPending}
        destroyOnClose
      >
        <Form form={templateForm} layout="vertical" className="mt-4">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="packageId" label="归属专病包" rules={[{ required: true }]}>
                <Select placeholder="选择包">
                  {packagesData?.items?.map((pkg: SpecialtyPackage) => (
                    <Option key={pkg.packageId} value={pkg.packageId}>
                      {pkg.name} (v{pkg.packageVersion})
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="name" label="路径模型名称" rules={[{ required: true }]}>
                <Input placeholder="如 社区获得性肺炎标准诊疗路径" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="templateCode" label="路径模型代码" rules={[{ required: true }]}>
                <Input placeholder="如 PT-CAP-01" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="diseaseCode" label="病种代码" rules={[{ required: true }]}>
                <Input placeholder="如 J18" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="templateLevel" label="路径层级" rules={[{ required: true }]}>
                <Select>
                  <Option value="CLINICAL">CLINICAL (临床规范级)</Option>
                  <Option value="BUSINESS">BUSINESS (业务质控级)</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="sourceRef" label="临床知识与指南基础" rules={[{ required: true }]}>
            <Input placeholder="如 社区获得性肺炎诊断和治疗指南 (2025年版)" />
          </Form.Item>
          <Form.Item name="description" label="收治标准与排除指标">
            <TextArea rows={2} placeholder="输入路径说明..." />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="nodesJson"
                label="生命周期节点配置 (JSON 列表)"
                rules={[{ required: true }]}
              >
                <TextArea rows={8} className="font-mono text-xs" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="edgesJson"
                label="拓扑流转连线配置 (JSON 列表)"
                rules={[{ required: true }]}
              >
                <TextArea rows={8} className="font-mono text-xs" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* 路径详情配置与沙箱仿真 Drawer */}
      <Drawer
        title={
          <div className="flex items-center justify-between w-full">
            <span>路径配置与沙箱仿真控制台</span>
            {detailData?.template.status === "DRAFT" && (
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                onClick={handlePublishTemplate}
                loading={publishTemplateMutation.isPending}
                className="mr-6 bg-emerald-600 border-emerald-600 hover:bg-emerald-700"
              >
                校验并申请发布上线 (门禁校验)
              </Button>
            )}
          </div>
        }
        width={960}
        onClose={() => {
          setSelectedTemplateId(null);
          setSimulateResult(null);
        }}
        open={!!selectedTemplateId}
        loading={detailLoading}
        destroyOnClose
      >
        {detailData && (
          <div>
            <Alert
              message={
                detailData.template.status === "PUBLISHED"
                  ? "当前临床路径处于已上线（PUBLISHED）状态，为保障临床运行安全，拓扑结构已被写保护锁定。如需修改，请发布新版本包升级。"
                  : "当前临床路径处于设计中（DRAFT）状态，您可以在左侧面板内预览拓扑，在右侧进行沙箱仿真运行，校验用例无误后申请发布。"
              }
              type={detailData.template.status === "PUBLISHED" ? "success" : "info"}
              showIcon
              className="mb-6 rounded-lg"
            />

            <Descriptions title="路径主数据事实" bordered column={2} className="mb-6">
              <Descriptions.Item label="名称">{detailData.template.name}</Descriptions.Item>
              <Descriptions.Item label="代码编码">
                {detailData.template.templateCode}
              </Descriptions.Item>
              <Descriptions.Item label="相关病种">
                {detailData.template.diseaseCode}
              </Descriptions.Item>
              <Descriptions.Item label="发布版本">
                v{detailData.template.templateVersion}.0 版
              </Descriptions.Item>
              <Descriptions.Item label="层级定位">
                {detailData.template.templateLevel}
              </Descriptions.Item>
              <Descriptions.Item label="发布状态">
                <Badge
                  status={detailData.template.status === "PUBLISHED" ? "success" : "warning"}
                  text={detailData.template.status}
                />
              </Descriptions.Item>
              <Descriptions.Item label="学术指南基础" span={2}>
                {detailData.template.sourceRef}
              </Descriptions.Item>
            </Descriptions>

            <Tabs defaultActiveKey="topology">
              <Tabs.TabPane tab="标准节点 (Nodes)" key="nodes">
                <Table
                  dataSource={detailData.nodes}
                  rowKey="nodeId"
                  pagination={false}
                  size="small"
                  columns={[
                    {
                      title: "节点代码",
                      dataIndex: "nodeCode",
                      render: (c) => <Tag color="blue">{c}</Tag>,
                    },
                    { title: "名称", dataIndex: "name", className: "font-semibold" },
                    {
                      title: "节点类型",
                      dataIndex: "nodeType",
                      render: (t) => <Tag color="purple">{t}</Tag>,
                    },
                    {
                      title: "时窗限制",
                      dataIndex: "timeWindowMinutes",
                      render: (m) => (m ? `${m} 分钟` : "无限制"),
                    },
                    { title: "默认责任角色", dataIndex: "responsibleRole" },
                    {
                      title: "终止节点",
                      dataIndex: "terminalFlag",
                      render: (t) => (t ? "是" : "否"),
                    },
                  ]}
                  className="medkernel-table"
                />
              </Tabs.TabPane>

              <Tabs.TabPane tab="决策边拓扑 (Edges)" key="edges">
                <Table
                  dataSource={detailData.edges}
                  rowKey="edgeId"
                  pagination={false}
                  size="small"
                  columns={[
                    { title: "推进边代码", dataIndex: "edgeCode" },
                    {
                      title: "自源节点",
                      dataIndex: "fromNodeCode",
                      render: (c) => <Tag color="orange">{c}</Tag>,
                    },
                    {
                      title: "至目标节点",
                      dataIndex: "toNodeCode",
                      render: (c) => <Tag color="green">{c}</Tag>,
                    },
                    {
                      title: "流转类型",
                      dataIndex: "edgeType",
                      render: (t) => <Tag color="cyan">{t}</Tag>,
                    },
                    {
                      title: "流转条件 (DSL)",
                      dataIndex: "conditionJson",
                      render: (c) => (
                        <span className="font-mono text-xs">{c || "无条件直接推进"}</span>
                      ),
                    },
                    { title: "优先级", dataIndex: "priority" },
                  ]}
                  className="medkernel-table"
                />
              </Tabs.TabPane>

              <Tabs.TabPane tab="沙箱仿真校验 (Sandbox)" key="simulate">
                <Row gutter={16}>
                  <Col span={10}>
                    <Card
                      title="仿真输入设置"
                      size="small"
                      className="border-gray-200 shadow-sm rounded-lg"
                    >
                      <Form layout="vertical">
                        <Form.Item label="仿真流转起点节点">
                          <Select value={simulateStartNode} onChange={setSimulateStartNode}>
                            {detailData.nodes.map((n) => (
                              <Option key={n.nodeCode} value={n.nodeCode}>
                                {n.name} ({n.nodeCode})
                              </Option>
                            ))}
                          </Select>
                        </Form.Item>
                        <Button
                          type="primary"
                          icon={<PlayCircleOutlined />}
                          onClick={handleSimulate}
                          loading={simulateMutation.isPending}
                          className="w-full bg-emerald-600 border-emerald-600 hover:bg-emerald-700 mt-4"
                        >
                          开始沙箱推进仿真
                        </Button>
                      </Form>
                    </Card>
                  </Col>
                  <Col span={14}>
                    <Card
                      title="预测仿真流转轨迹 (Simulation Path)"
                      size="small"
                      className="border-gray-200 shadow-sm rounded-lg"
                    >
                      {simulateResult ? (
                        <div className="p-4 bg-gray-50 rounded-lg min-h-48 flex flex-col justify-center">
                          <Timeline>
                            {simulateResult.map((nodeCode, idx) => {
                              const nodeDetail = detailData.nodes.find(
                                (n) => n.nodeCode === nodeCode,
                              );
                              return (
                                <Timeline.Item key={idx} color={idx === 0 ? "blue" : "green"}>
                                  <div className="font-semibold text-gray-800 text-xs">
                                    {nodeDetail?.name || "未知节点"}
                                  </div>
                                  <div className="text-gray-400 text-xs font-mono mt-0.5">
                                    {nodeCode}
                                  </div>
                                </Timeline.Item>
                              );
                            })}
                          </Timeline>
                        </div>
                      ) : (
                        <div className="flex flex-col items-center justify-center min-h-48 text-gray-400">
                          <ExclamationCircleOutlined className="text-48px mb-4" />
                          <span>在左侧选择起点后，点击仿真以进行预测计算</span>
                        </div>
                      )}
                    </Card>
                  </Col>
                </Row>
              </Tabs.TabPane>
            </Tabs>
          </div>
        )}
      </Drawer>
    </PageShell>
  );
}
