import { useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Row,
  Segmented,
  Space,
  Statistic,
  Steps,
  Table,
  Tag,
  Typography,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import {
  ExperimentOutlined,
  FileSearchOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
} from "@ant-design/icons";
import { get, post } from "../api/client";
import type { ApiError, EvaluateResponse, HitItem, RuleEngineResultSummary } from "../api/types";

const { Paragraph, Text } = Typography;

type DemoScenarioCode = "PATHWAY_ENTRY" | "EMR_QC" | "INSURANCE_QC" | "ORDER_SAFETY";

interface ScenarioProfile {
  code: DemoScenarioCode;
  label: string;
  title: string;
  owner: string;
  packageCode: string;
  patientId: string;
  encounterId: string;
  facts: Record<string, unknown>;
  checkpoints: string[];
}

interface RuleResultRow extends HitItem {
  key: string;
  hit?: boolean;
  rule_type?: string;
  scenario_code?: string;
  reference_document_code?: string;
  actions?: string[];
  evidence?: Record<string, unknown>[];
}

const scenarios: ScenarioProfile[] = [
  {
    code: "PATHWAY_ENTRY",
    label: "AMI 入径",
    title: "AMI/STEMI 路径推荐与入径",
    owner: "急诊主治 / 胸痛中心主任",
    packageCode: "PKG_AMI_CORE",
    patientId: "P_DEMO_AMI_001",
    encounterId: "E_DEMO_AMI_001",
    facts: {
      chief_complaints: [{ code: "CHEST_PAIN", text: "胸痛 2 小时" }],
      exams: [{ finding_codes: ["ST_ELEVATION_CONTIGUOUS_LEADS"], ecg_elapsed_minutes: 12 }],
    },
    checkpoints: ["HIS 事件触发", "路径候选识别", "医生确认入径", "审计留痕"],
  },
  {
    code: "EMR_QC",
    label: "病历质控",
    title: "病历内涵质控",
    owner: "医务质控 / 主管医师",
    packageCode: "PKG_EMR_QC",
    patientId: "P_DEMO_EMR_001",
    encounterId: "E_DEMO_EMR_001",
    facts: {
      emr: {
        admission_record: { submitted: false, first_word_time: "2026-05-15T10:30:00+08:00" },
        chief_complaint: { filled: false },
        discharge_summary: {
          chief_complaint_filled: false,
          diagnosis_filled: true,
          discharge_orders_filled: false,
        },
      },
    },
    checkpoints: ["病历保存", "时限/完整性检查", "整改任务生成", "闭环复核"],
  },
  {
    code: "INSURANCE_QC",
    label: "医保审核",
    title: "医保智能审核",
    owner: "医保审核员 / 药师",
    packageCode: "PKG_INSURANCE_QC",
    patientId: "P_DEMO_INS_001",
    encounterId: "E_DEMO_INS_001",
    facts: {
      orders: {
        insurance_restricted_drug_used: true,
        insurance_restricted_drug_indication_matched: false,
        total_cost_ratio_to_drg_avg: 2.3,
      },
    },
    checkpoints: ["医嘱提交", "医保限定支付校验", "拒付风险提示", "处置记录"],
  },
  {
    code: "ORDER_SAFETY",
    label: "医嘱安全",
    title: "医嘱安全实时拦截",
    owner: "临床医生 / 药师",
    packageCode: "PKG_ORDER_SAFETY",
    patientId: "P_DEMO_SAFE_001",
    encounterId: "E_DEMO_SAFE_001",
    facts: {
      orders: {
        antibiotic_duplicate_within_48h: true,
        platelet_count: 42,
        anticoagulant_requested: true,
      },
    },
    checkpoints: ["医嘱录入", "安全红线扫描", "阻断或升级", "医生确认"],
  },
];

function buildEvaluateRequest(profile: ScenarioProfile) {
  return {
    scenario_code: profile.code,
    rule_package_code: profile.packageCode,
    operator_id: "DEMO_OPERATOR",
    patient_context: {
      patient: {
        patient_id: profile.patientId,
        gender: "M",
        age: 66,
      },
      encounter: {
        encounter_id: profile.encounterId,
        visit_type: profile.code === "PATHWAY_ENTRY" ? "EMERGENCY" : "INPATIENT",
        department_code: profile.code === "ORDER_SAFETY" ? "DEPT_PHARMACY" : "DEPT_CARDIOLOGY",
        arrival_time: "2026-05-20T08:12:00+08:00",
      },
      facts: profile.facts,
    },
    tenant_id: "TENANT_DEMO",
    hospital_code: "HOSPITAL_DEMO",
    department_code: profile.code === "ORDER_SAFETY" ? "DEPT_PHARMACY" : "DEPT_CARDIOLOGY",
  };
}

function normalizeRows(result?: EvaluateResponse): RuleResultRow[] {
  if (!result) {
    return [];
  }
  const rawRows = ((result.hits || result.results || []) as RuleResultRow[]);
  return rawRows.map((row, index) => ({
    ...row,
    key: row.rule_code || `row-${index}`,
  }));
}

function severityColor(severity?: string) {
  if (severity === "CRITICAL" || severity === "HIGH" || severity === "ERROR") return "red";
  if (severity === "MEDIUM" || severity === "WARNING") return "gold";
  if (severity === "LOW") return "blue";
  return "default";
}

function sourceText(row: RuleResultRow) {
  if (row.source_document?.title) {
    return row.source_document.title;
  }
  if (row.reference_document_code) {
    return row.reference_document_code;
  }
  if (row.evidence?.length) {
    return `${row.evidence.length} 条运行证据`;
  }
  return "未返回来源";
}

export default function DemoValidationPlaceholder() {
  const [selected, setSelected] = useState<DemoScenarioCode>("PATHWAY_ENTRY");
  const activeScenario = scenarios.find((item) => item.code === selected) || scenarios[0];

  const historyQuery = useQuery<RuleEngineResultSummary[], ApiError>({
    queryKey: ["demo-validation-history", selected],
    queryFn: () => get<RuleEngineResultSummary[]>(`/rule-engine/results?scenarioCode=${selected}`),
  });

  const runMutation = useMutation<EvaluateResponse, ApiError, ScenarioProfile>({
    mutationFn: (profile) => post<EvaluateResponse>("/rule-engine/evaluate", buildEvaluateRequest(profile)),
    onSuccess: () => {
      void historyQuery.refetch();
    },
  });

  const rows = normalizeRows(runMutation.data);
  const blockingRows = useMemo(
    () => rows.filter((row) => row.hit !== false && ["CRITICAL", "HIGH", "ERROR"].includes(String(row.severity))),
    [rows],
  );
  const stepCurrent = runMutation.data ? 3 : 0;
  const currentStep = runMutation.isPending ? 1 : stepCurrent;

  const columns: ColumnsType<RuleResultRow> = [
    {
      title: "规则",
      dataIndex: "rule_code",
      width: 210,
      render: (value: string, row) => (
        <Space direction="vertical" size={0}>
          <Text strong>{value}</Text>
          <Text type="secondary">{row.rule_name || row.rule_type || "规则结果"}</Text>
        </Space>
      ),
    },
    {
      title: "等级",
      dataIndex: "severity",
      width: 100,
      render: (value?: string) => <Tag color={severityColor(value)}>{value || "INFO"}</Tag>,
    },
    {
      title: "结果",
      dataIndex: "message",
      render: (value: string, row) => (
        <Space direction="vertical" size={4}>
          <Text>{value}</Text>
          <Text type="secondary">{sourceText(row)}</Text>
        </Space>
      ),
    },
    {
      title: "动作",
      dataIndex: "actions",
      width: 180,
      render: (_value, row) => {
        const actions = row.suggested_actions || row.actions || [];
        return actions.length ? actions.slice(0, 2).map((action) => <Tag key={action}>{action}</Tag>) : <Text type="secondary">记录</Text>;
      },
    },
  ];

  return (
    <div>
      <div className="page-header">
        <h1>演示与校验工作台</h1>
        <div className="subtitle">客户验收剧本 · 规则执行 · 来源证据 · traceId</div>
      </div>

      <Space direction="vertical" size={16} style={{ width: "100%" }}>
        <Card>
          <Row gutter={[16, 16]} align="middle">
            <Col flex="auto">
              <Segmented
                value={selected}
                onChange={(value) => setSelected(value as DemoScenarioCode)}
                options={scenarios.map((item) => ({ label: item.label, value: item.code }))}
              />
            </Col>
            <Col>
              <Space>
                <Button
                  icon={<ReloadOutlined />}
                  onClick={() => void historyQuery.refetch()}
                >
                  刷新记录
                </Button>
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  loading={runMutation.isPending}
                  onClick={() => runMutation.mutate(activeScenario)}
                >
                  执行验证
                </Button>
              </Space>
            </Col>
          </Row>
        </Card>

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={16}>
            <Card
              title={
                <Space>
                  <ExperimentOutlined />
                  {activeScenario.title}
                </Space>
              }
            >
              <Descriptions size="small" column={2}>
                <Descriptions.Item label="剧本角色">{activeScenario.owner}</Descriptions.Item>
                <Descriptions.Item label="规则包">{activeScenario.packageCode}</Descriptions.Item>
                <Descriptions.Item label="患者">{activeScenario.patientId}</Descriptions.Item>
                <Descriptions.Item label="就诊">{activeScenario.encounterId}</Descriptions.Item>
              </Descriptions>
              <Steps
                size="small"
                current={currentStep}
                items={activeScenario.checkpoints.map((title) => ({ title }))}
                style={{ marginTop: 20 }}
              />
            </Card>
          </Col>
          <Col xs={24} lg={8}>
            <Card title={<Space><SafetyCertificateOutlined />验证结果</Space>}>
              <Row gutter={12}>
                <Col span={8}>
                  <Statistic title="评估规则" value={runMutation.data?.evaluated_count ?? 0} />
                </Col>
                <Col span={8}>
                  <Statistic title="命中" value={runMutation.data?.hit_count ?? 0} />
                </Col>
                <Col span={8}>
                  <Statistic title="高风险" value={blockingRows.length} />
                </Col>
              </Row>
              <Paragraph style={{ marginTop: 12, marginBottom: 0 }}>
                <Text type="secondary">traceId：</Text>
                <Text code>{runMutation.data?.trace_id || "待执行"}</Text>
              </Paragraph>
            </Card>
          </Col>
        </Row>

        {runMutation.error ? (
          <Alert
            type="error"
            showIcon
            message={`${runMutation.error.code}: ${runMutation.error.message}`}
            description={runMutation.error.traceId ? `traceId: ${runMutation.error.traceId}` : undefined}
          />
        ) : null}

        <Card title={<Space><FileSearchOutlined />规则命中与证据</Space>}>
          {rows.length ? (
            <Table
              rowKey="key"
              columns={columns}
              dataSource={rows}
              pagination={false}
              size="middle"
            />
          ) : (
            <Empty description="尚未执行当前剧本" />
          )}
        </Card>

        <Card title="最近验证记录">
          <Table
            rowKey="result_id"
            loading={historyQuery.isLoading}
            dataSource={historyQuery.data || []}
            pagination={{ pageSize: 5 }}
            size="small"
            columns={[
              { title: "结果编号", dataIndex: "result_id" },
              { title: "场景", dataIndex: "scenario_code", width: 140 },
              { title: "命中", dataIndex: "hit_count", width: 90 },
              { title: "耗时(ms)", dataIndex: "elapsed_ms", width: 110 },
              { title: "时间", dataIndex: "created_time", width: 210 },
            ]}
          />
        </Card>
      </Space>
    </div>
  );
}
