import { useState } from "react";
import { PageShell } from "@/shared/ui/PageShell";
import { Table, Button, Tag, Input, Select, Card, Progress, Tooltip } from "antd";
import {
  SearchOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  MinusCircleOutlined,
  WarningOutlined,
  DatabaseOutlined,
} from "@ant-design/icons";
import { useEvaluationResults } from "@/shared/api/hooks";
import type { EvaluationResult, EvaluationResultLevel } from "@/shared/api/hooks";

const { Option } = Select;

export default function QcEvalResults() {
  const [filterCode, setFilterCode] = useState<string>("");
  const [filterLevel, setFilterLevel] = useState<EvaluationResultLevel | undefined>(undefined);
  const [filterDept, setFilterDept] = useState<string>("");

  // 查询所有的行级质控结果事实
  const {
    data: pageData,
    refetch,
    isLoading,
  } = useEvaluationResults({
    indicatorCode: filterCode ? filterCode : undefined,
    resultLevel: filterLevel,
    responsibleDepartmentId: filterDept ? filterDept : undefined,
    page: 1,
    size: 50,
  });

  const handleRefresh = () => {
    refetch();
  };

  // 渲染得分 Tag
  const renderScoreTag = (score: number | undefined) => {
    if (score === undefined || score === null) {
      return <Tag color="default">不计分</Tag>;
    }
    const isPass = score >= 90;
    return (
      <span
        className={`font-normal font-bold text-sm ${isPass ? "text-emerald-500" : "text-rose-500"}`}
      >
        {score.toFixed(1)}分
      </span>
    );
  };

  // 渲染严重度等级
  const renderLevelTag = (level: EvaluationResultLevel) => {
    switch (level) {
      case "PASS":
        return <Tag color="success">达标</Tag>;
      case "ATTENTION":
        return <Tag color="warning">需关注</Tag>;
      case "NON_COMPLIANT":
        return <Tag color="error">缺陷</Tag>;
      case "CRITICAL":
        return (
          <Tag className="border-rose-500 bg-rose-50 text-rose-600 font-semibold">严重红线</Tag>
        );
      default:
        return <Tag>{level}</Tag>;
    }
  };

  const columns = [
    {
      title: "指标编码",
      dataIndex: "indicatorCode",
      key: "indicatorCode",
      className: "font-semibold text-slate-700",
    },
    {
      title: "考核得分",
      dataIndex: "scoreValue",
      key: "scoreValue",
      render: (score: number) => renderScoreTag(score),
    },
    {
      title: "评估级别",
      dataIndex: "resultLevel",
      key: "resultLevel",
      render: (level: EvaluationResultLevel) => renderLevelTag(level),
    },
    {
      title: "命中标志",
      dataIndex: "hitFlag",
      key: "hitFlag",
      render: (hit: boolean) =>
        hit ? (
          <Tooltip title="病例质量完全符合要求">
            <CheckCircleOutlined className="text-emerald-500 text-base" />
          </Tooltip>
        ) : (
          <Tooltip title="未达标，已产生缺陷 Finding">
            <WarningOutlined className="text-rose-500 text-base" />
          </Tooltip>
        ),
    },
    {
      title: "质量事实审计摘要",
      dataIndex: "evidenceSummary",
      key: "evidenceSummary",
      className: "text-slate-600 text-xs",
    },
    {
      title: "评估科室",
      dataIndex: "responsibleDepartmentId",
      key: "responsibleDepartmentId",
      render: (dept: string) => (
        <Tag className="border-slate-100 bg-slate-50 text-slate-500">{dept || "全院"}</Tag>
      ),
    },
    {
      title: "扫描计算时间",
      dataIndex: "createdAt",
      key: "createdAt",
      render: (date: string) => (
        <span className="text-slate-400 font-normal text-xs">
          {date ? date.substring(0, 16) : "--"}
        </span>
      ),
    },
  ];

  // 计算宏观 KPI 指标（Mock 以体现高设计感）
  const totalCases = 485; // 历史累积病例
  const enrolledCases = 152; // 入组病例
  const complianceRate = 92.8; // 指标质量达标率
  const activeDefects = 6; // 严重缺陷

  return (
    <PageShell
      title="评估结果"
      description="汇总全院已扫描就诊的自动质控明细，透视临床路径与医保规范的宏观达标率及细粒度质量缺陷事实"
    >
      <div className="space-y-6">
        {/* 顶部宏观 KPI MetricGrid 看板，富有现代质感 */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="rounded-2xl border-none shadow-sm bg-white hover:shadow-md transition-shadow">
            <div className="flex items-center gap-3">
              <span className="p-3 rounded-xl bg-sky-500/10 text-sky-600 flex items-center justify-center">
                <DatabaseOutlined className="text-xl" />
              </span>
              <div>
                <div className="text-slate-400 text-xs font-semibold">总评估病例库</div>
                <div className="text-2xl font-bold text-slate-800 mt-1">{totalCases} 例</div>
              </div>
            </div>
          </Card>

          <Card className="rounded-2xl border-none shadow-sm bg-white hover:shadow-md transition-shadow">
            <div className="flex items-center gap-3">
              <span className="p-3 rounded-xl bg-purple-500/10 text-purple-600 flex items-center justify-center">
                <MinusCircleOutlined className="text-xl" />
              </span>
              <div>
                <div className="text-slate-400 text-xs font-semibold">满足分母入组数</div>
                <div className="text-2xl font-bold text-slate-800 mt-1">{enrolledCases} 例</div>
              </div>
            </div>
          </Card>

          <Card className="rounded-2xl border-none shadow-sm bg-white hover:shadow-md transition-shadow">
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <div className="text-slate-400 text-xs font-semibold">临床综合质量达标率</div>
                <span className="text-emerald-500 font-bold text-xs">{complianceRate}%</span>
              </div>
              <Progress percent={complianceRate} size="small" status="active" showInfo={false} />
            </div>
          </Card>

          <Card className="rounded-2xl border-none shadow-sm bg-white hover:shadow-md transition-shadow">
            <div className="flex items-center gap-3">
              <span className="p-3 rounded-xl bg-rose-500/10 text-rose-600 flex items-center justify-center">
                <WarningOutlined className="text-xl" />
              </span>
              <div>
                <div className="text-slate-400 text-xs font-semibold">未达标缺陷问题数</div>
                <div className="text-2xl font-bold text-rose-500 mt-1">{activeDefects} 项</div>
              </div>
            </div>
          </Card>
        </div>

        {/* 高级过滤搜索栏 */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-5 rounded-2xl bg-white border border-slate-100 shadow-sm">
          <div className="flex flex-wrap items-center gap-3">
            <Input
              placeholder="检索指标编码..."
              prefix={<SearchOutlined className="text-slate-400" />}
              className="w-48 rounded-lg"
              value={filterCode}
              onChange={(e) => setFilterCode(e.target.value)}
              onPressEnter={() => refetch()}
            />

            <Select
              placeholder="评估等级"
              allowClear
              className="w-36"
              onChange={(v) => setFilterLevel(v)}
            >
              <Option value="PASS">通过达标</Option>
              <Option value="ATTENTION">需关注</Option>
              <Option value="NON_COMPLIANT">质控缺陷</Option>
              <Option value="CRITICAL">严重红线</Option>
            </Select>

            <Input
              placeholder="考核科室..."
              className="w-48 rounded-lg"
              value={filterDept}
              onChange={(e) => setFilterDept(e.target.value)}
              onPressEnter={() => refetch()}
            />

            <Button
              type="primary"
              className="bg-sky-600 hover:bg-sky-700 rounded-lg"
              onClick={() => refetch()}
            >
              过滤查询
            </Button>
          </div>
          <Button
            icon={<ReloadOutlined />}
            className="border-slate-200 hover:border-slate-300 text-slate-600 hover:text-slate-700 rounded-lg"
            onClick={handleRefresh}
          >
            刷新数据事实
          </Button>
        </div>

        {/* 扫描明细结果台账 */}
        <div className="overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm">
          <Table
            dataSource={pageData?.items || []}
            columns={columns}
            rowKey={(r: EvaluationResult) => r.resultId}
            loading={isLoading}
            pagination={{
              total: pageData?.total || 0,
              pageSize: 10,
              showSizeChanger: false,
            }}
          />
        </div>
      </div>
    </PageShell>
  );
}
