import { useState, useEffect, useCallback } from "react";
import {
  Breadcrumb,
  Button,
  Card,
  Col,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Typography,
} from "antd";
import {
  ArrowLeftOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  BugOutlined,
  ExperimentOutlined,
  HeartOutlined,
  MedicineBoxOutlined,
  ReloadOutlined,
  WarningOutlined,
} from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import { useParams, Link } from "react-router-dom";
import type {
  DepartmentDetail,
  VariationItem,
  DoctorPerformance,
} from "../../api/types";
import { fetchDepartmentDetail } from "../../api/quality";

const { Text, Title } = Typography;

const PERIOD_LABEL: Record<string, string> = {
  month: "本月",
  week: "本周",
  today: "今日",
};

// 模拟数据
const MOCK_DETAIL: DepartmentDetail = {
  tenantId: "hospital-001",
  departmentCode: "心内科",
  period: "month",
  kpis: {
    pathway: { totalEnrolled: 387, completed: 356, variationRate: 8.0 },
    rule: { realtimeBlock: 3, softReminder: 285, hitRate: 18.5 },
    qc: { totalIssues: 72, closedIssues: 64, rectificationRate: 88.9 },
    insurance: { potentialRefund: 4200, refundChange: -8.5 },
  },
  topVariations: [
    { pathwayCode: "AMI_STEMI", variationNode: "PCI 时间窗超时", count: 7, reason: "导管室排程冲突" },
    { pathwayCode: "AMI_STEMI", variationNode: "抗血小板调整", count: 5, reason: "合并出血风险" },
    { pathwayCode: "HF_DECOMP", variationNode: "利尿剂剂量调整", count: 4, reason: "肾功能受限" },
    { pathwayCode: "AMI_NSTEMI", variationNode: "介入时机选择", count: 3, reason: "造影剂过敏史" },
    { pathwayCode: "HF_DECOMP", variationNode: "出院标准评估", count: 3, reason: "BNP 下降不明显" },
  ],
  doctorPerformance: [
    { name: "李明", cases: 87, completionRate: 93.1, rectificationRate: 88.2, ruleHitRate: 18.3 },
    { name: "周强", cases: 82, completionRate: 95.2, rectificationRate: 91.0, ruleHitRate: 15.1 },
    { name: "王芳", cases: 76, completionRate: 91.5, rectificationRate: 85.7, ruleHitRate: 20.4 },
    { name: "张伟", cases: 68, completionRate: 89.3, rectificationRate: 82.1, ruleHitRate: 22.8 },
    { name: "刘洋", cases: 54, completionRate: 86.7, rectificationRate: 79.5, ruleHitRate: 25.6 },
  ],
};

// 变更箭头组件
function ChangeArrow({ value }: { value: number }) {
  if (value === 0) return null;
  const isPositive = value > 0;
  return (
    <Text type={isPositive ? "success" : "danger"} style={{ fontSize: 12, marginLeft: 4 }}>
      {isPositive ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
      {Math.abs(value).toFixed(1)}%
    </Text>
  );
}

export default function DepartmentDrillDown() {
  const { deptCode } = useParams<{ deptCode: string }>();
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<DepartmentDetail | null>(null);
  const [period, setPeriod] = useState<string>("month");

  const fetchData = useCallback(async () => {
    if (!deptCode) return;
    setLoading(true);
    try {
      const data = await fetchDepartmentDetail(decodeURIComponent(deptCode), { period });
      setDetail(data);
    } catch {
      // 使用模拟数据
      setDetail({ ...MOCK_DETAIL, departmentCode: decodeURIComponent(deptCode) });
    } finally {
      setLoading(false);
    }
  }, [deptCode, period]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // 变异 TOP 列定义
  const variationColumns: ColumnsType<VariationItem> = [
    {
      title: "路径",
      dataIndex: "pathwayCode",
      key: "pathwayCode",
      width: 150,
    },
    {
      title: "变异节点",
      dataIndex: "variationNode",
      key: "variationNode",
      width: 200,
    },
    {
      title: "次数",
      dataIndex: "count",
      key: "count",
      width: 80,
      sorter: (a, b) => a.count - b.count,
      defaultSortOrder: "descend",
    },
    {
      title: "主要原因",
      dataIndex: "reason",
      key: "reason",
      width: 250,
    },
  ];

  // 医生绩效列定义
  const doctorColumns: ColumnsType<DoctorPerformance> = [
    {
      title: "医生",
      dataIndex: "name",
      key: "name",
      width: 100,
    },
    {
      title: "接诊",
      dataIndex: "cases",
      key: "cases",
      width: 80,
      sorter: (a, b) => a.cases - b.cases,
    },
    {
      title: "路径完成率",
      dataIndex: "completionRate",
      key: "completionRate",
      width: 120,
      render: (v: number) => `${v.toFixed(1)}%`,
      sorter: (a, b) => a.completionRate - b.completionRate,
    },
    {
      title: "整改及时率",
      dataIndex: "rectificationRate",
      key: "rectificationRate",
      width: 120,
      render: (v: number) => `${v.toFixed(1)}%`,
      sorter: (a, b) => a.rectificationRate - b.rectificationRate,
    },
    {
      title: "规则命中率",
      dataIndex: "ruleHitRate",
      key: "ruleHitRate",
      width: 120,
      render: (v: number) => `${v.toFixed(1)}%`,
      sorter: (a, b) => a.ruleHitRate - b.ruleHitRate,
    },
  ];

  const displayDetail = detail || MOCK_DETAIL;

  return (
    <div style={{ padding: 24 }}>
      <Spin spinning={loading}>
        {/* 面包屑导航 */}
        <Breadcrumb
          style={{ marginBottom: 16 }}
          items={[
            {
              title: (
                <Link to="/qc/dashboard">
                  <Space>
                    <ArrowLeftOutlined />
                    <span>返回院级</span>
                  </Space>
                </Link>
              ),
            },
            { title: `${displayDetail.departmentCode} 质控钻取` },
          ]}
        />

        {/* 页面标题 */}
        <div style={{ marginBottom: 24, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div>
            <Title level={4} style={{ margin: 0 }}>
              {displayDetail.departmentCode} 质控钻取
            </Title>
            <Text type="secondary">
              时间：{PERIOD_LABEL[period] || "今日"}
            </Text>
          </div>
          <Space>
            <Select
              value={period}
              onChange={setPeriod}
              style={{ width: 120 }}
              options={[
                { value: "today", label: "今日" },
                { value: "week", label: "本周" },
                { value: "month", label: "本月" },
              ]}
            />
            <Button icon={<ReloadOutlined />} onClick={fetchData}>
              刷新
            </Button>
          </Space>
        </div>

        {/* 科室 4 KPI 卡片 */}
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={6}>
            <Card
              title={
                <Space>
                  <HeartOutlined style={{ color: "var(--mk-success)" }} />
                  <span>路径执行</span>
                </Space>
              }
              size="small"
            >
              <Statistic
                title="入径患者"
                value={displayDetail.kpis.pathway.totalEnrolled}
              />
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">
                  完成 {displayDetail.kpis.pathway.completed} · 变异率{" "}
                  {displayDetail.kpis.pathway.variationRate}%
                </Text>
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card
              title={
                <Space>
                  <WarningOutlined style={{ color: "var(--mk-warning)" }} />
                  <span>规则命中</span>
                </Space>
              }
              size="small"
            >
              <Statistic
                title="实时拦截"
                value={displayDetail.kpis.rule.realtimeBlock}
              />
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">
                  软提醒 {displayDetail.kpis.rule.softReminder} · 命中率{" "}
                  {displayDetail.kpis.rule.hitRate}%
                </Text>
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card
              title={
                <Space>
                  <BugOutlined style={{ color: "var(--mk-danger)" }} />
                  <span>质控问题</span>
                </Space>
              }
              size="small"
            >
              <Statistic
                title="总计"
                value={displayDetail.kpis.qc.totalIssues}
              />
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">
                  已闭环 {displayDetail.kpis.qc.closedIssues} · 整改率{" "}
                  {displayDetail.kpis.qc.rectificationRate}%
                </Text>
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card
              title={
                <Space>
                  <MedicineBoxOutlined style={{ color: "var(--mk-primary)" }} />
                  <span>医保风险</span>
                </Space>
              }
              size="small"
            >
              <Statistic
                title="可能退费"
                value={displayDetail.kpis.insurance.potentialRefund}
                precision={0}
                prefix="¥"
                suffix={
                  <ChangeArrow value={displayDetail.kpis.insurance.refundChange} />
                }
              />
            </Card>
          </Col>
        </Row>

        {/* 变异 TOP10 */}
        <Card
          title={
            <Space>
              <WarningOutlined />
              <span>变异 TOP10</span>
            </Space>
          }
          style={{ marginBottom: 24 }}
        >
          <Table
            columns={variationColumns}
            dataSource={displayDetail.topVariations}
            rowKey={(record) => `${record.pathwayCode}-${record.variationNode}`}
            pagination={false}
            size="middle"
          />
        </Card>

        {/* 医生绩效 */}
        <Card
          title={
            <Space>
              <ExperimentOutlined />
              <span>医生绩效</span>
              <Tag color="processing">仅主任可见</Tag>
            </Space>
          }
        >
          <Table
            columns={doctorColumns}
            dataSource={displayDetail.doctorPerformance}
            rowKey="name"
            pagination={false}
            size="middle"
          />
        </Card>
      </Spin>
    </div>
  );
}
