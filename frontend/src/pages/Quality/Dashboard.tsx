import { useState, useEffect, useCallback } from "react";
import {
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
  AlertOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  BarChartOutlined,
  BugOutlined,
  DownloadOutlined,
  FileSearchOutlined,
  HeartOutlined,
  MedicineBoxOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import TrendChart from "./components/TrendChart";
import type {
  DashboardKpis,
  DepartmentRank,
  TrendDay,
} from "../../api/types";
import {
  fetchDashboardKpis,
  fetchDepartmentRanking,
  fetchTrendData,
} from "../../api/quality";
import { useNavigate } from "react-router-dom";

const { Text, Title } = Typography;

const PERIOD_LABEL: Record<string, string> = {
  month: "本月",
  week: "本周",
  today: "今日",
};

const RISK_LEVEL_TAG: Record<string, { color: string; label: string }> = {
  HIGH: { color: "error", label: "高风险" },
  MEDIUM: { color: "warning", label: "中风险" },
  LOW: { color: "success", label: "低风险" },
};

// 模拟数据
const MOCK_KPIS: DashboardKpis = {
  tenantId: "hospital-001",
  period: "month",
  departmentCode: null,
  generatedTime: "2026-05-19",
  pathway: {
    totalEnrolled: 1247,
    completed: 1103,
    variationRate: 11.5,
    enrolledChange: 8.2,
    completedChange: 5.1,
    variationRateChange: -2.3,
  },
  rule: {
    realtimeBlock: 8,
    softReminder: 1427,
    hitRate: 23.0,
    blockChange: -12.5,
    reminderChange: 15.3,
    hitRateChange: 3.2,
  },
  qc: {
    totalIssues: 384,
    closedIssues: 312,
    rectificationRate: 81.3,
    totalChange: -5.8,
    closedChange: 12.4,
    rectificationRateChange: 4.6,
  },
  insurance: {
    potentialRefund: 24800,
    refundChange: -12.0,
    riskLevel: "MEDIUM",
  },
};

const MOCK_DEPARTMENTS: DepartmentRank[] = [
  { name: "心内科", enrolled: 387, completionRate: 92.1, rectificationRate: 89.3, ruleHitRate: 18.5, stars: 5 },
  { name: "神经内科", enrolled: 256, completionRate: 88.4, rectificationRate: 75.2, ruleHitRate: 22.1, stars: 3 },
  { name: "骨科", enrolled: 198, completionRate: 95.2, rectificationRate: 91.0, ruleHitRate: 15.3, stars: 5 },
  { name: "呼吸内科", enrolled: 176, completionRate: 85.7, rectificationRate: 78.6, ruleHitRate: 25.8, stars: 3 },
  { name: "消化内科", enrolled: 145, completionRate: 90.3, rectificationRate: 82.1, ruleHitRate: 19.7, stars: 4 },
  { name: "普外科", enrolled: 132, completionRate: 91.8, rectificationRate: 86.5, ruleHitRate: 17.2, stars: 4 },
  { name: "妇产科", enrolled: 98, completionRate: 87.6, rectificationRate: 73.9, ruleHitRate: 28.4, stars: 3 },
  { name: "儿科", enrolled: 87, completionRate: 82.3, rectificationRate: 68.5, ruleHitRate: 31.2, stars: 2 },
  { name: "急诊科", enrolled: 76, completionRate: 79.5, rectificationRate: 65.8, ruleHitRate: 35.6, stars: 2 },
  { name: "ICU", enrolled: 45, completionRate: 94.1, rectificationRate: 92.7, ruleHitRate: 12.8, stars: 5 },
];

const MOCK_TREND: TrendDay[] = Array.from({ length: 30 }, (_, i) => ({
  date: `2026-04-${String(20 + i).padStart(2, "0")}`,
  pathwayCompletionRate: 85 + Math.random() * 15,
  ruleHitRate: 18 + Math.random() * 12,
  qcRectificationRate: 75 + Math.random() * 20,
  insuranceRiskAmount: 15000 + Math.floor(Math.random() * 20000),
}));

export default function QualityDashboard() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [kpis, setKpis] = useState<DashboardKpis | null>(null);
  const [departments, setDepartments] = useState<DepartmentRank[]>([]);
  const [trend, setTrend] = useState<TrendDay[]>([]);
  const [period, setPeriod] = useState<string>("month");

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [kpisData, deptData, trendData] = await Promise.all([
        fetchDashboardKpis({ period }),
        fetchDepartmentRanking({ period }),
        fetchTrendData({ days: 30 }),
      ]);
      setKpis(kpisData);
      setDepartments(deptData.departments);
      setTrend(trendData.trend);
    } catch {
      // 使用模拟数据
      setKpis(MOCK_KPIS);
      setDepartments(MOCK_DEPARTMENTS);
      setTrend(MOCK_TREND);
    } finally {
      setLoading(false);
    }
  }, [period]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // 科室排名列定义
  const deptColumns: ColumnsType<DepartmentRank> = [
    {
      title: "排名",
      key: "rank",
      width: 60,
      render: (_, __, index) => index + 1,
    },
    {
      title: "科室",
      dataIndex: "name",
      key: "name",
      width: 120,
    },
    {
      title: "入径数",
      dataIndex: "enrolled",
      key: "enrolled",
      width: 100,
      sorter: (a, b) => a.enrolled - b.enrolled,
    },
    {
      title: "完成率",
      dataIndex: "completionRate",
      key: "completionRate",
      width: 100,
      render: (v: number) => `${v.toFixed(1)}%`,
      sorter: (a, b) => a.completionRate - b.completionRate,
    },
    {
      title: "整改率",
      dataIndex: "rectificationRate",
      key: "rectificationRate",
      width: 100,
      render: (v: number) => `${v.toFixed(1)}%`,
      sorter: (a, b) => a.rectificationRate - b.rectificationRate,
    },
    {
      title: "规则命中率",
      dataIndex: "ruleHitRate",
      key: "ruleHitRate",
      width: 110,
      render: (v: number) => `${v.toFixed(1)}%`,
      sorter: (a, b) => a.ruleHitRate - b.ruleHitRate,
    },
    {
      title: "评级",
      key: "stars",
      width: 120,
      render: (_, record) => (
        <span style={{ color: "var(--mk-warning)" }}>
          {"★".repeat(record.stars)}
          {"☆".repeat(5 - record.stars)}
        </span>
      ),
    },
    {
      title: "操作",
      key: "action",
      width: 80,
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          onClick={() => navigate(`/qc/department/${encodeURIComponent(record.name)}`)}
        >
          钻取
        </Button>
      ),
    },
  ];

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

  const displayKpis = kpis || MOCK_KPIS;

  return (
    <div style={{ padding: 24 }}>
      <Spin spinning={loading}>
        {/* 页面标题 */}
        <div style={{ marginBottom: 24, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div>
            <Title level={4} style={{ margin: 0 }}>院级质控驾驶舱</Title>
            <Text type="secondary">
              时间：{PERIOD_LABEL[period] || "今日"} · 组织：全院
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
            <Button icon={<DownloadOutlined />} type="primary">
              导出周报
            </Button>
          </Space>
        </div>

        {/* 4 KPI 卡片 */}
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
                value={displayKpis.pathway.totalEnrolled}
                suffix={
                  <ChangeArrow value={displayKpis.pathway.enrolledChange} />
                }
              />
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">
                  完成 {displayKpis.pathway.completed} · 变异率{" "}
                  {displayKpis.pathway.variationRate}%
                  <ChangeArrow value={displayKpis.pathway.variationRateChange} />
                </Text>
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card
              title={
                <Space>
                  <AlertOutlined style={{ color: "var(--mk-warning)" }} />
                  <span>规则命中</span>
                </Space>
              }
              size="small"
            >
              <Statistic
                title="实时拦截"
                value={displayKpis.rule.realtimeBlock}
                suffix={
                  <ChangeArrow value={displayKpis.rule.blockChange} />
                }
              />
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">
                  软提醒 {displayKpis.rule.softReminder.toLocaleString()} · 命中率{" "}
                  {displayKpis.rule.hitRate}%
                  <ChangeArrow value={displayKpis.rule.hitRateChange} />
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
                value={displayKpis.qc.totalIssues}
                suffix={
                  <ChangeArrow value={displayKpis.qc.totalChange} />
                }
              />
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">
                  已闭环 {displayKpis.qc.closedIssues} · 整改率{" "}
                  {displayKpis.qc.rectificationRate}%
                  <ChangeArrow value={displayKpis.qc.rectificationRateChange} />
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
                value={displayKpis.insurance.potentialRefund}
                precision={0}
                prefix="¥"
                suffix={
                  <ChangeArrow value={displayKpis.insurance.refundChange} />
                }
              />
              <div style={{ marginTop: 8 }}>
                <Tag
                  color={RISK_LEVEL_TAG[displayKpis.insurance.riskLevel]?.color || "success"}
                >
                  {RISK_LEVEL_TAG[displayKpis.insurance.riskLevel]?.label || "低风险"}
                </Tag>
              </div>
            </Card>
          </Col>
        </Row>

        {/* 趋势图 */}
        <Card
          title={
            <Space>
              <BarChartOutlined />
              <span>路径完成率趋势（最近 30 天）</span>
            </Space>
          }
          style={{ marginBottom: 24 }}
        >
          <TrendChart data={trend} />
        </Card>

        {/* 科室排名 */}
        <Card
          title={
            <Space>
              <FileSearchOutlined />
              <span>科室排名（点击钻取）</span>
            </Space>
          }
        >
          <Table
            columns={deptColumns}
            dataSource={departments}
            rowKey="name"
            pagination={false}
            scroll={{ x: 800 }}
            size="middle"
          />
        </Card>
      </Spin>
    </div>
  );
}
