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
  Tooltip,
  Alert,
  message,
  Tabs,
  Row,
  Col,
} from "antd";
import {
  PlusOutlined,
  PlayCircleOutlined,
  InfoCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  CodeOutlined,
} from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import {
  useRuleDefinitions,
  useRuleDetail,
  useCreateRule,
  useAddTestCase,
  useSimulateRule,
  usePublishRule,
} from "@/shared/api/hooks";
import type { RuleDefinition } from "@/shared/api/hooks";

const { TextArea } = Input;
const { Option } = Select;

// 默认规则 DSL 引导模板
const DEFAULT_DSL_TEMPLATE = `{
  "when": {
    "all": [
      {
        "fact": "patient.age",
        "operator": "gt",
        "value": 65
      },
      {
        "fact": "prescription.drug_code",
        "operator": "equals",
        "value": "DRUG-001"
      }
    ]
  },
  "then": [
    {
      "actionCode": "STRONG_REMINDER",
      "severity": "HIGH",
      "message": "老年患者使用本药物风险偏高，请确认肾功能指标是否正常",
      "requiresPhysicianConfirmation": true
    }
  ],
  "explain": "65岁以上老年患者关键处方剂量审核规则"
}`;

const DEFAULT_EXPLAIN_TEMPLATE = `{
  "template": "患者年龄为 \${patient.age} 岁，拟开具 \${prescription.drug_code}，触发高风险警示：\${message}",
  "variables": {
    "patient.age": "患者实际年龄",
    "prescription.drug_code": "处方药物品规编码",
    "message": "命中提醒内容"
  }
}`;

export default function RuleDefinitions() {
  const [page, setPage] = useState(1);
  const [size] = useState(10);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [typeFilter, setTypeFilter] = useState<string | undefined>(undefined);
  const [riskFilter, setRiskFilter] = useState<string | undefined>(undefined);

  // 抽屉详情态
  const [selectedRuleId, setSelectedRuleId] = useState<string | null>(null);

  // 创建规则模态框态
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [createForm] = Form.useForm();

  // 新增测试用例模态框态
  const [caseModalVisible, setCaseModalVisible] = useState(false);
  const [caseForm] = Form.useForm();

  // 仿真运行态
  const [simulatePayload, setSimulatePayload] = useState<string>('{\n  "patient": {\n    "age": 70\n  },\n  "prescription": {\n    "drug_code": "DRUG-001"\n  }\n}');
  const [simulateResult, setSimulateResult] = useState<any | null>(null);

  // 载入定义数据
  const {
    data: listData,
    isLoading: listLoading,
    refetch: refetchList,
  } = useRuleDefinitions({
    page,
    size,
    status: statusFilter,
    ruleType: typeFilter,
    riskLevel: riskFilter,
  });

  // 载入指定规则的详情（包含版本和测试用例）
  const {
    data: detailData,
    isLoading: detailLoading,
    refetch: refetchDetail,
  } = useRuleDetail(selectedRuleId || "");

  // API 突变逻辑
  const createRuleMutation = useCreateRule();
  const addTestCaseMutation = useAddTestCase(selectedRuleId || "");
  const simulateMutation = useSimulateRule(selectedRuleId || "");
  const publishMutation = usePublishRule();

  // 处理创建规则
  const handleCreateRule = async () => {
    try {
      const values = await createForm.validateFields();
      // 简单验证 JSON 格式
      try {
        JSON.parse(values.dslJson);
        JSON.parse(values.explanationJson);
      } catch {
        message.error("DSL 或解释模板的 JSON 格式不合法，请检查！");
        return;
      }

      await createRuleMutation.mutateAsync({
        ruleCode: values.ruleCode,
        name: values.name,
        ruleType: values.ruleType,
        authoringMode: "DSL",
        riskLevel: values.riskLevel,
        sourceRef: values.sourceRef,
        changeSummary: values.changeSummary,
        dslJson: values.dslJson,
        explanationJson: values.explanationJson,
      });

      message.success("新规则创建成功，状态为 DRAFT(草稿)");
      setCreateModalVisible(false);
      createForm.resetFields();
      refetchList();
    } catch (err: any) {
      message.error(err.response?.data?.message || "创建规则失败");
    }
  };

  // 处理添加测试用例
  const handleAddTestCase = async () => {
    try {
      const values = await caseForm.validateFields();
      try {
        JSON.parse(values.inputPayload);
      } catch {
        message.error("用例输入载荷 payload 的 JSON 格式不合法");
        return;
      }

      await addTestCaseMutation.mutateAsync({
        caseType: values.caseType,
        inputPayload: values.inputPayload,
        expectedHit: values.expectedHit,
        expectedSeverity: values.expectedSeverity,
        expectedActionCode: values.expectedActionCode,
      });

      message.success("成功新增测试用例");
      setCaseModalVisible(false);
      caseForm.resetFields();
      refetchDetail();
    } catch (err: any) {
      message.error(err.response?.data?.message || "添加用例失败");
    }
  };

  // 触发仿真运行
  const handleSimulate = async () => {
    try {
      try {
        JSON.parse(simulatePayload);
      } catch {
        message.error("仿真输入 payload 的 JSON 格式不合法");
        return;
      }

      const result = await simulateMutation.mutateAsync({
        inputPayload: simulatePayload,
      });
      setSimulateResult(result);
      message.success("规则仿真运行成功，已输出求值结果");
      refetchDetail(); // 仿真也会刷新最后一次用例的状态
    } catch (err: any) {
      message.error(err.response?.data?.message || "规则仿真求值失败");
    }
  };

  // 触发发布门禁并推进上线
  const handlePublish = async () => {
    if (!selectedRuleId) return;
    try {
      await publishMutation.mutateAsync(selectedRuleId);
      message.success("发布成功！所有门禁测试通过，规则已跃迁为 PUBLISHED");
      refetchDetail();
      refetchList();
    } catch (err: any) {
      // 包含测试用例不全等业务拒绝信息
      Modal.error({
        title: "规则发布门禁拒绝",
        content: err.response?.data?.message || "发布门禁校验未通过，请检查测试用例是否齐全且全部 PASS。",
      });
    }
  };

  // 表格列定义
  const columns = [
    {
      title: "规则编码",
      dataIndex: "ruleCode",
      key: "ruleCode",
      render: (text: string) => <Tag color="blue">{text}</Tag>,
    },
    {
      title: "规则名称",
      dataIndex: "name",
      key: "name",
      className: "font-semibold text-gray-800",
    },
    {
      title: "规则类别",
      dataIndex: "ruleType",
      key: "ruleType",
      render: (type: string) => {
        const types: Record<string, string> = {
          DRUG_SAFETY: "合理用药安全",
          INSURANCE_AUDIT: "医保规范核查",
          CLINICAL_QUALITY: "临床诊疗质控",
        };
        return types[type] || type;
      },
    },
    {
      title: "风险评级",
      dataIndex: "riskLevel",
      key: "riskLevel",
      render: (level: string) => {
        const colors: Record<string, string> = {
          LOW: "green",
          MEDIUM: "orange",
          HIGH: "red",
        };
        return <Tag color={colors[level]}>{level}</Tag>;
      },
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      render: (status: string) => {
        const statusMap: Record<string, { text: string; color: string }> = {
          DRAFT: { text: "草稿设计中", color: "warning" },
          PUBLISHED: { text: "已上线运行", color: "success" },
          OFFLINE: { text: "已下线封存", color: "default" },
          ARCHIVED: { text: "已归档历史", color: "default" },
        };
        const config = statusMap[status] || { text: status, color: "processing" };
        return <Badge status={config.color as any} text={config.text} />;
      },
    },
    {
      title: "当前包版本",
      dataIndex: "packageVersion",
      key: "packageVersion",
      render: (val: string) => val || <span className="text-gray-400">未锁定</span>,
    },
    {
      title: "操作",
      key: "action",
      render: (_: any, record: RuleDefinition) => (
        <Button
          type="link"
          onClick={() => setSelectedRuleId(record.ruleId)}
          className="text-indigo-600 hover:text-indigo-900 font-medium"
        >
          查看配置 & 仿真
        </Button>
      ),
    },
  ];

  return (
    <PageShell
      title="规则中枢"
      description="管理合理用药、医保规范和临床质控核心规则资产，提供版本控制、仿真模拟以及门禁测试闭环。"
    >
      <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 mb-6">
        <Form layout="inline" className="flex flex-wrap gap-4 items-center">
          <Form.Item label="状态">
            <Select
              placeholder="全部状态"
              allowClear
              value={statusFilter}
              onChange={setStatusFilter}
              className="w-[140px]"
            >
              <Option value="DRAFT">草稿设计中</Option>
              <Option value="PUBLISHED">已上线运行</Option>
              <Option value="OFFLINE">已下线封存</Option>
            </Select>
          </Form.Item>
          <Form.Item label="类别">
            <Select
              placeholder="全部类别"
              allowClear
              value={typeFilter}
              onChange={setTypeFilter}
              className="w-[150px]"
            >
              <Option value="DRUG_SAFETY">合理用药安全</Option>
              <Option value="INSURANCE_AUDIT">医保规范核查</Option>
              <Option value="CLINICAL_QUALITY">临床诊疗质控</Option>
            </Select>
          </Form.Item>
          <Form.Item label="风险评级">
            <Select
              placeholder="全部评级"
              allowClear
              value={riskFilter}
              onChange={setRiskFilter}
              className="w-[120px]"
            >
              <Option value="LOW">LOW (低度警示)</Option>
              <Option value="MEDIUM">MEDIUM (中度阻断)</Option>
              <Option value="HIGH">HIGH (红线拦截)</Option>
            </Select>
          </Form.Item>
          <Form.Item className="ml-auto">
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => {
                createForm.setFieldsValue({
                  dslJson: DEFAULT_DSL_TEMPLATE,
                  explanationJson: DEFAULT_EXPLAIN_TEMPLATE,
                  ruleType: "DRUG_SAFETY",
                  riskLevel: "LOW",
                  changeSummary: "初始化创建草稿版本",
                });
                setCreateModalVisible(true);
              }}
              className="rounded-lg font-medium"
            >
              新建规则模板
            </Button>
          </Form.Item>
        </Form>
      </div>

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
            showTotal: (t) => `共 ${t} 个受控规则实体`,
          }}
          className="medkernel-table"
        />
      </div>

      {/* 规则详情抽屉（最核心面板） */}
      <Drawer
        title={
          <div className="flex items-center justify-between w-full">
            <span>规则配置详情 & 仿真面板</span>
            {detailData?.definition.status === "DRAFT" && (
              <Button
                type="primary"
                onClick={handlePublish}
                loading={publishMutation.isPending}
                className="mr-6"
              >
                发布此规则上线 (门禁校验)
              </Button>
            )}
          </div>
        }
        width={960}
        onClose={() => {
          setSelectedRuleId(null);
          setSimulateResult(null);
        }}
        open={!!selectedRuleId}
        loading={detailLoading}
        destroyOnClose
      >
        {detailData && (
          <div>
            <Alert
              message={
                detailData.definition.status === "PUBLISHED"
                  ? "当前规则处于 PUBLISHED(运行中) 状态，不能直接编辑 DSL。如需修改，请通过引擎包灰度发布升级。"
                  : "当前规则处于 DRAFT(草稿) 状态，可以在下方编辑其测试用例，运行仿真求值，并在用例全绿通过后申请发布。"
              }
              type={detailData.definition.status === "PUBLISHED" ? "success" : "info"}
              showIcon
              className="mb-6 rounded-lg"
            />

            <Descriptions title="基本元数据" bordered column={2} className="mb-6">
              <Descriptions.Item label="规则编码">{detailData.definition.ruleCode}</Descriptions.Item>
              <Descriptions.Item label="名称">{detailData.definition.name}</Descriptions.Item>
              <Descriptions.Item label="类型">{detailData.definition.ruleType}</Descriptions.Item>
              <Descriptions.Item label="风险级别">
                <Tag color={detailData.definition.riskLevel === "HIGH" ? "red" : "orange"}>
                  {detailData.definition.riskLevel}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Badge
                  status={detailData.definition.status === "PUBLISHED" ? "success" : "warning"}
                  text={detailData.definition.status}
                />
              </Descriptions.Item>
              <Descriptions.Item label="当前版本">{detailData.version?.versionNo || 0} 版</Descriptions.Item>
            </Descriptions>

            <Tabs defaultActiveKey="dsl">
              <Tabs.TabPane
                tab={
                  <span>
                    <CodeOutlined /> 规则主体 DSL & 解释
                  </span>
                }
                key="dsl"
              >
                <Row gutter={16}>
                  <Col span={12}>
                    <Card title="确定性条件树 (JSON DSL)" className="h-full">
                      <pre className="bg-gray-50 p-4 rounded-lg overflow-auto max-h-96 text-xs font-mono">
                        {JSON.stringify(JSON.parse(detailData.version?.dslJson || "{}"), null, 2)}
                      </pre>
                    </Card>
                  </Col>
                  <Col span={12}>
                    <Card title="临床可信解释模板" className="h-full">
                      <pre className="bg-gray-50 p-4 rounded-lg overflow-auto max-h-96 text-xs font-mono">
                        {JSON.stringify(JSON.parse(detailData.version?.explanationJson || "{}"), null, 2)}
                      </pre>
                    </Card>
                  </Col>
                </Row>
              </Tabs.TabPane>

              <Tabs.TabPane
                tab={
                  <span>
                    <InfoCircleOutlined /> 发布门禁测试用例 ({detailData.testCases.length})
                  </span>
                }
                key="cases"
              >
                <div className="flex justify-between items-center mb-4">
                  <span className="text-gray-500 text-xs">
                    发布前必须添加阳性、阴性、边界和冲突用例，且所有用例必须校验 PASS。
                  </span>
                  {detailData.definition.status === "DRAFT" && (
                    <Button
                      type="dashed"
                      icon={<PlusOutlined />}
                      onClick={() => {
                        caseForm.setFieldsValue({
                          inputPayload: '{\n  "patient": {\n    "age": 70\n  },\n  "prescription": {\n    "drug_code": "DRUG-001"\n  }\n}',
                          expectedHit: true,
                          expectedSeverity: "HIGH",
                          expectedActionCode: "STRONG_REMINDER",
                          caseType: "POSITIVE",
                        });
                        setCaseModalVisible(true);
                      }}
                    >
                      新增测试用例
                    </Button>
                  )}
                </div>

                <Table
                  dataSource={detailData.testCases}
                  rowKey="id"
                  pagination={false}
                  columns={[
                    {
                      title: "用例类别",
                      dataIndex: "caseType",
                      key: "caseType",
                      render: (t) => <Tag color="blue">{t}</Tag>,
                    },
                    {
                      title: "期望命中",
                      dataIndex: "expectedHit",
                      key: "expectedHit",
                      render: (val: boolean) => (val ? "应该触发" : "不该触发"),
                    },
                    {
                      title: "期望严重度",
                      dataIndex: "expectedSeverity",
                      key: "expectedSeverity",
                    },
                    {
                      title: "最新执行结果",
                      key: "lastStatus",
                      render: (_, row) => {
                        if (!row.lastStatus || row.lastStatus === "PENDING") {
                          return <Badge status="default" text="待执行" />;
                        }
                        return row.lastStatus === "PASS" ? (
                          <span className="text-green-600 font-medium">
                            <CheckCircleOutlined className="mr-1" /> PASS
                          </span>
                        ) : (
                          <Tooltip title={row.lastMessage}>
                            <span className="text-red-600 font-medium cursor-help">
                              <CloseCircleOutlined className="mr-1" /> FAIL
                            </span>
                          </Tooltip>
                        );
                      },
                    },
                    {
                      title: "执行时间",
                      dataIndex: "lastRunAt",
                      key: "lastRunAt",
                      render: (val) => (val ? new Date(val).toLocaleTimeString() : "-"),
                    },
                  ]}
                />
              </Tabs.TabPane>

              <Tabs.TabPane
                tab={
                  <span>
                    <PlayCircleOutlined /> 仿真运行校验
                  </span>
                }
                key="simulate"
              >
                <Row gutter={16}>
                  <Col span={12}>
                    <Card title="仿真输入 Payload (JSON)">
                      <TextArea
                        rows={12}
                        value={simulatePayload}
                        onChange={(e) => setSimulatePayload(e.target.value)}
                        className="font-mono text-xs"
                      />
                      <Button
                        type="primary"
                        icon={<PlayCircleOutlined />}
                        onClick={handleSimulate}
                        loading={simulateMutation.isPending}
                        className="w-full mt-4"
                      >
                        运行规则仿真求值
                      </Button>
                    </Card>
                  </Col>
                  <Col span={12}>
                    <Card title="仿真输出结果 (DSL Evaluation)">
                      {simulateResult ? (
                        <div className="bg-gray-50 p-4 rounded-lg min-h-64 overflow-auto max-h-96">
                          <Descriptions column={1} size="small" bordered className="mb-4">
                            <Descriptions.Item label="规则是否命中">
                              {simulateResult.hit ? (
                                <Tag color="red">命中</Tag>
                              ) : (
                                <Tag color="green">未命中</Tag>
                              )}
                            </Descriptions.Item>
                            {simulateResult.hit && (
                              <>
                                <Descriptions.Item label="动作代码">
                                  {simulateResult.actionCode}
                                </Descriptions.Item>
                                <Descriptions.Item label="最高严重等级">
                                  {simulateResult.severity}
                                </Descriptions.Item>
                              </>
                            )}
                          </Descriptions>
                          <div className="text-xs font-semibold text-gray-700 mb-2">详细决策动作说明:</div>
                          <div className="text-xs text-gray-600 bg-white p-3 rounded border border-gray-200 font-mono mb-4">
                            {simulateResult.explanation || "未命中，无动作输出。"}
                          </div>
                        </div>
                      ) : (
                        <div className="flex flex-col items-center justify-center min-h-64 text-gray-400">
                          <PlayCircleOutlined className="text-[48px] mb-4" />
                          <span>在左侧填入上下文快照后，点击运行开始仿真</span>
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

      {/* 创建规则 Modal */}
      <Modal
        title="创建新临床规则"
        open={createModalVisible}
        onOk={handleCreateRule}
        onCancel={() => setCreateModalVisible(false)}
        width={800}
        confirmLoading={createRuleMutation.isPending}
        destroyOnClose
      >
        <Form form={createForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="ruleCode"
                label="规则唯一业务编码 (Rule Code)"
                rules={[{ required: true, message: "请输入编码，同租户下不可重复" }]}
              >
                <Input placeholder="例如: DRUG-SAFETY-GERIATRIC" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="name"
                label="规则显示名称"
                rules={[{ required: true, message: "请输入规则名称" }]}
              >
                <Input placeholder="例如: 65岁以上老年患者剂量安全规则" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="ruleType" label="规则门类" rules={[{ required: true }]}>
                <Select>
                  <Option value="DRUG_SAFETY">合理用药安全</Option>
                  <Option value="INSURANCE_AUDIT">医保规范核查</Option>
                  <Option value="CLINICAL_QUALITY">临床诊疗质控</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="riskLevel" label="风险严重等级" rules={[{ required: true }]}>
                <Select>
                  <Option value="LOW">LOW (低度警示)</Option>
                  <Option value="MEDIUM">MEDIUM (中度阻断)</Option>
                  <Option value="HIGH">HIGH (红线拦截)</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="sourceRef"
                label="医学依据/来源"
                rules={[{ required: true, message: "请输入依据来源" }]}
              >
                <Input placeholder="例如: 高血压合理用药指南2025" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="changeSummary" label="初始化变更内容说明" rules={[{ required: true }]}>
            <Input placeholder="本次创建版本的修改概述" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="dslJson"
                label="确定性条件 JSON DSL"
                rules={[{ required: true, message: "请输入条件树 JSON" }]}
              >
                <TextArea rows={12} className="font-mono text-xs" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="explanationJson"
                label="可解释追溯模板 JSON"
                rules={[{ required: true, message: "请输入解释模板" }]}
              >
                <TextArea rows={12} className="font-mono text-xs" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* 新增用例 Modal */}
      <Modal
        title="新增测试用例 (发布门禁指标)"
        open={caseModalVisible}
        onOk={handleAddTestCase}
        onCancel={() => setCaseModalVisible(false)}
        confirmLoading={addTestCaseMutation.isPending}
        destroyOnClose
      >
        <Form form={caseForm} layout="vertical">
          <Form.Item name="caseType" label="用例类别" rules={[{ required: true }]}>
            <Select>
              <Option value="POSITIVE">POSITIVE (阳性命中用例)</Option>
              <Option value="NEGATIVE">NEGATIVE (阴性不命中用例)</Option>
              <Option value="BOUNDARY">BOUNDARY (边界条件用例)</Option>
              <Option value="CONFLICT">CONFLICT (规则冲突校验用例)</Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="inputPayload"
            label="测试输入 payload JSON 快照"
            rules={[{ required: true, message: "请输入快照" }]}
          >
            <TextArea rows={8} className="font-mono text-xs" />
          </Form.Item>
          <Form.Item name="expectedHit" label="期望求值结果" valuePropName="checked">
            <Select>
              <Option value={true}>应当触发规则命中</Option>
              <Option value={false}>不应当命中</Option>
            </Select>
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="expectedSeverity" label="期望动作严重度" rules={[{ required: true }]}>
                <Select>
                  <Option value="LOW">LOW</Option>
                  <Option value="MEDIUM">MEDIUM</Option>
                  <Option value="HIGH">HIGH</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="expectedActionCode"
                label="期望动作代码"
                rules={[{ required: true }]}
              >
                <Input placeholder="如: STRONG_REMINDER / BLOCK" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </PageShell>
  );
}
