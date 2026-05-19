import React, { useState, useEffect, useCallback } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Checkbox,
  Col,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  message,
} from "antd";
import {
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  ReloadOutlined,
  WarningOutlined,
} from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import type {
  TerminologyItem,
  AiCandidate,
  ConceptType,
  MappingSummary,
} from "../../api/types";
import {
  fetchTerminologyMappings,
  fetchMappingSummary,
  fetchAiCandidates,
  adoptMapping,
  batchAdoptMappings,
  rejectAiCandidate,
} from "../../api/terminology";

const { Text } = Typography;
const { Search } = Input;

// 概念类型配置
const CONCEPT_TYPE_MAP: Record<ConceptType, { label: string; color: string }> = {
  DIAGNOSIS: { label: "诊断", color: "blue" },
  PROCEDURE: { label: "手术/操作", color: "cyan" },
  DRUG: { label: "药品", color: "green" },
  LAB: { label: "检验", color: "purple" },
  OBSERVATION: { label: "观察", color: "orange" },
  DEVICE: { label: "器械", color: "magenta" },
};

// 标准字典选项
const STANDARD_DICT_OPTIONS = [
  { value: "ICD-11", label: "ICD-11 国际疾病分类" },
  { value: "ICD-9-CM-3", label: "ICD-9-CM-3 手术操作" },
  { value: "LOINC", label: "LOINC 检验术语" },
  { value: "ATC", label: "ATC 药品分类" },
  { value: "SNOMED-CT", label: "SNOMED-CT 临床术语" },
];

// AI Badge 组件
function AiBadge({ confidence }: { confidence?: number }) {
  return (
    <Tooltip title={confidence ? `AI置信度: ${(confidence * 100).toFixed(0)}%` : "AI候选"}>
      <Tag icon={<ExperimentOutlined />} color="processing">
        AI{confidence ? ` ${(confidence * 100).toFixed(0)}%` : ""}
      </Tag>
    </Tooltip>
  );
}

// DangerConfirm 组件（二次确认）
function DangerConfirm({
  level = "low",
  onConfirm,
  children,
  title,
  description,
}: {
  level?: "low" | "medium" | "high";
  onConfirm: () => void;
  children: React.ReactNode;
  title: string;
  description?: string;
}) {
  const handleClick = () => {
    Modal.confirm({
      title,
      content: description || "确认执行此操作？",
      okText: "确认",
      cancelText: "取消",
      okButtonProps: {
        danger: level === "high",
        type: level === "low" ? "primary" : "default",
      },
      onOk: onConfirm,
    });
  };

  return <span onClick={handleClick}>{children}</span>;
}

export default function MappingWorkbench() {
  const [activeTab, setActiveTab] = useState<string>("unmapped");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 数据状态
  const [unmappedItems, setUnmappedItems] = useState<TerminologyItem[]>([]);
  const [mappedItems, setMappedItems] = useState<TerminologyItem[]>([]);
  const [conflictItems, setConflictItems] = useState<TerminologyItem[]>([]);
  const [aiCandidates, setAiCandidates] = useState<AiCandidate[]>([]);
  const [summary, setSummary] = useState<MappingSummary | null>(null);

  // 筛选状态
  const [filterConceptType, setFilterConceptType] = useState<ConceptType | undefined>();
  const [filterStandardDict, setFilterStandardDict] = useState<string>("ICD-11");
  const [searchKeyword, setSearchKeyword] = useState("");

  // 批量选择状态
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([]);

  // 默认统计
  const DEFAULT_SUMMARY: MappingSummary = {
    totalUnmapped: 0,
    totalMapped: 0,
    totalConflict: 0,
    totalAiCandidate: 0,
    byConceptType: {} as Record<ConceptType, number>,
  };

  // 获取数据
  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [summaryData, unmapped, mapped, conflicts, aiCands] = await Promise.all([
        fetchMappingSummary(),
        fetchTerminologyMappings({ status: "UNMAPPED", conceptType: filterConceptType }),
        fetchTerminologyMappings({ status: "MAPPED", conceptType: filterConceptType }),
        fetchTerminologyMappings({ status: "CONFLICT", conceptType: filterConceptType }),
        fetchAiCandidates({ conceptType: filterConceptType }),
      ]);
      setSummary(summaryData);
      setUnmappedItems(unmapped);
      setMappedItems(mapped);
      setConflictItems(conflicts);
      setAiCandidates(aiCands);
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载失败");
      // 使用模拟数据
      setUnmappedItems(MOCK_UNMAPPED);
      setMappedItems(MOCK_MAPPED);
      setConflictItems(MOCK_CONFLICT);
      setAiCandidates(MOCK_AI_CANDIDATES);
      setSummary(MOCK_SUMMARY);
    } finally {
      setLoading(false);
    }
  }, [filterConceptType]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // 采纳映射
  const handleAdopt = async (item: TerminologyItem | AiCandidate) => {
    try {
      await adoptMapping({
        sourceCode: item.sourceCode,
        conceptType: item.conceptType,
        standardCode: "proposedStandardCode" in item ? (item as AiCandidate).proposedStandardCode : (item as TerminologyItem).standardCode || "",
        standardName: "proposedStandardName" in item ? (item as AiCandidate).proposedStandardName : (item as TerminologyItem).standardName,
        operatorId: "current-user",
      });
      message.success("采纳成功");
      fetchData();
    } catch {
      message.error("采纳失败");
    }
  };

  // 批量采纳
  const handleBatchAdopt = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning("请选择要采纳的项目");
      return;
    }

    try {
      const items = selectedRowKeys.map((key) => {
        const candidate = aiCandidates.find((c) => `${c.sourceCode}-${c.conceptType}` === key);
        if (candidate) {
          return {
            sourceCode: candidate.sourceCode,
            conceptType: candidate.conceptType,
            standardCode: candidate.proposedStandardCode,
            standardName: candidate.proposedStandardName,
            operatorId: "current-user",
          };
        }
        throw new Error("未找到候选");
      });

      const result = await batchAdoptMappings({
        items,
        operatorId: "current-user",
      });
      message.success(`批量采纳成功: ${result.successCount} 项`);
      setSelectedRowKeys([]);
      fetchData();
    } catch {
      message.error("批量采纳失败");
    }
  };

  // 驳回 AI 候选
  const handleReject = async (candidate: AiCandidate) => {
    try {
      await rejectAiCandidate({
        sourceCode: candidate.sourceCode,
        conceptType: candidate.conceptType,
        operatorId: "current-user",
      });
      message.success("已驳回");
      fetchData();
    } catch {
      message.error("驳回失败");
    }
  };

  // 未映射列表列定义
  const unmappedColumns: ColumnsType<TerminologyItem> = [
    {
      title: "院内编码",
      dataIndex: "sourceCode",
      key: "sourceCode",
      width: 150,
    },
    {
      title: "院内名称",
      dataIndex: "sourceName",
      key: "sourceName",
      width: 200,
    },
    {
      title: "类型",
      dataIndex: "conceptType",
      key: "conceptType",
      width: 100,
      render: (type: ConceptType) => (
        <Tag color={CONCEPT_TYPE_MAP[type]?.color}>{CONCEPT_TYPE_MAP[type]?.label}</Tag>
      ),
    },
    {
      title: "来源系统",
      dataIndex: "sourceSystem",
      key: "sourceSystem",
      width: 120,
    },
    {
      title: "出现次数",
      dataIndex: "occurrenceCount",
      key: "occurrenceCount",
      width: 100,
      sorter: (a, b) => (a.occurrenceCount || 0) - (b.occurrenceCount || 0),
    },
    {
      title: "操作",
      key: "action",
      width: 150,
      render: (_, record) => (
        <Space>
          <Button size="small" type="primary" onClick={() => handleAdopt(record)}>
            手动映射
          </Button>
        </Space>
      ),
    },
  ];

  // 已映射列表列定义
  const mappedColumns: ColumnsType<TerminologyItem> = [
    {
      title: "院内编码",
      dataIndex: "sourceCode",
      key: "sourceCode",
      width: 150,
    },
    {
      title: "院内名称",
      dataIndex: "sourceName",
      key: "sourceName",
      width: 200,
    },
    {
      title: "类型",
      dataIndex: "conceptType",
      key: "conceptType",
      width: 100,
      render: (type: ConceptType) => (
        <Tag color={CONCEPT_TYPE_MAP[type]?.color}>{CONCEPT_TYPE_MAP[type]?.label}</Tag>
      ),
    },
    {
      title: "标准编码",
      dataIndex: "standardCode",
      key: "standardCode",
      width: 150,
    },
    {
      title: "标准名称",
      dataIndex: "standardName",
      key: "standardName",
      width: 200,
    },
    {
      title: "审核人",
      dataIndex: "reviewedBy",
      key: "reviewedBy",
      width: 120,
    },
  ];

  // 冲突列表列定义
  const conflictColumns: ColumnsType<TerminologyItem> = [
    {
      title: "院内编码",
      dataIndex: "sourceCode",
      key: "sourceCode",
      width: 150,
    },
    {
      title: "院内名称",
      dataIndex: "sourceName",
      key: "sourceName",
      width: 200,
    },
    {
      title: "类型",
      dataIndex: "conceptType",
      key: "conceptType",
      width: 100,
      render: (type: ConceptType) => (
        <Tag color={CONCEPT_TYPE_MAP[type]?.color}>{CONCEPT_TYPE_MAP[type]?.label}</Tag>
      ),
    },
    {
      title: "冲突说明",
      dataIndex: "reviewComment",
      key: "reviewComment",
      width: 250,
      render: (text) => (
        <Text type="danger">
          <WarningOutlined /> {text || "多个映射候选冲突"}
        </Text>
      ),
    },
    {
      title: "操作",
      key: "action",
      width: 150,
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => handleAdopt(record)}>
            手动解决
          </Button>
        </Space>
      ),
    },
  ];

  // AI 候选列表列定义
  const aiCandidateColumns: ColumnsType<AiCandidate> = [
    {
      title: "",
      key: "select",
      width: 50,
      render: (_, record) => (
        <Checkbox
          checked={selectedRowKeys.includes(`${record.sourceCode}-${record.conceptType}`)}
          onChange={(e) => {
            const key = `${record.sourceCode}-${record.conceptType}`;
            if (e.target.checked) {
              setSelectedRowKeys([...selectedRowKeys, key]);
            } else {
              setSelectedRowKeys(selectedRowKeys.filter((k) => k !== key));
            }
          }}
        />
      ),
    },
    {
      title: "院内编码",
      dataIndex: "sourceCode",
      key: "sourceCode",
      width: 150,
    },
    {
      title: "院内名称",
      dataIndex: "sourceName",
      key: "sourceName",
      width: 180,
    },
    {
      title: "类型",
      dataIndex: "conceptType",
      key: "conceptType",
      width: 100,
      render: (type: ConceptType) => (
        <Tag color={CONCEPT_TYPE_MAP[type]?.color}>{CONCEPT_TYPE_MAP[type]?.label}</Tag>
      ),
    },
    {
      title: "AI建议",
      key: "aiSuggestion",
      width: 250,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Space>
            <AiBadge confidence={record.confidence} />
            <Text strong>{record.proposedStandardCode}</Text>
          </Space>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {record.proposedStandardName}
          </Text>
        </Space>
      ),
    },
    {
      title: "操作",
      key: "action",
      width: 150,
      render: (_, record) => (
        <Space>
          <DangerConfirm
            level="low"
            title="确认采纳此映射？"
            description={`将 ${record.sourceName} 映射到 ${record.proposedStandardCode}`}
            onConfirm={() => handleAdopt(record)}
          >
            <Button size="small" type="primary">
              采纳
            </Button>
          </DangerConfirm>
          <Button size="small" danger onClick={() => handleReject(record)}>
            驳回
          </Button>
        </Space>
      ),
    },
  ];

  const displaySummary = summary || DEFAULT_SUMMARY;

  return (
    <div style={{ padding: 24 }}>
      <Card
        title="字典映射工作台"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={fetchData}>
              刷新
            </Button>
          </Space>
        }
      >
        {error && (
          <Alert
            message="数据加载失败"
            description={error}
            type="warning"
            showIcon
            closable
            style={{ marginBottom: 16 }}
          />
        )}

        {/* 统计卡片 */}
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="未映射"
                value={displaySummary.totalUnmapped}
                valueStyle={{ color: "var(--mk-warning)" }}
                prefix={<FileSearchOutlined />}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="已映射"
                value={displaySummary.totalMapped}
                valueStyle={{ color: "var(--mk-success)" }}
                prefix={<CheckCircleOutlined />}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="冲突"
                value={displaySummary.totalConflict}
                valueStyle={{ color: "var(--mk-danger)" }}
                prefix={<ExclamationCircleOutlined />}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="AI候选"
                value={displaySummary.totalAiCandidate}
                valueStyle={{ color: "var(--mk-primary)" }}
                prefix={<ExperimentOutlined />}
              />
            </Card>
          </Col>
        </Row>

        {/* 筛选条件 */}
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Select
              placeholder="概念类型"
              allowClear
              style={{ width: "100%" }}
              value={filterConceptType}
              onChange={setFilterConceptType}
              options={Object.entries(CONCEPT_TYPE_MAP).map(([value, { label }]) => ({
                value,
                label,
              }))}
            />
          </Col>
          <Col span={6}>
            <Select
              placeholder="标准字典"
              style={{ width: "100%" }}
              value={filterStandardDict}
              onChange={setFilterStandardDict}
              options={STANDARD_DICT_OPTIONS}
            />
          </Col>
          <Col span={8}>
            <Search
              placeholder="搜索院内编码或名称"
              allowClear
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onSearch={() => fetchData()}
            />
          </Col>
        </Row>

        {/* Tabs */}
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: "unmapped",
              label: (
                <span>
                  未映射 <Badge count={displaySummary.totalUnmapped} style={{ marginLeft: 8 }} />
                </span>
              ),
              children: (
                <Table
                  columns={unmappedColumns}
                  dataSource={unmappedItems}
                  rowKey="id"
                  loading={loading}
                  pagination={{ pageSize: 20 }}
                  scroll={{ x: 900 }}
                />
              ),
            },
            {
              key: "mapped",
              label: (
                <span>
                  已映射 <Badge count={displaySummary.totalMapped} style={{ marginLeft: 8 }} />
                </span>
              ),
              children: (
                <Table
                  columns={mappedColumns}
                  dataSource={mappedItems}
                  rowKey="id"
                  loading={loading}
                  pagination={{ pageSize: 20 }}
                  scroll={{ x: 900 }}
                />
              ),
            },
            {
              key: "conflict",
              label: (
                <span>
                  冲突{" "}
                  <Badge
                    count={displaySummary.totalConflict}
                    style={{ marginLeft: 8 }}
                    color="red"
                  />
                </span>
              ),
              children: (
                <Table
                  columns={conflictColumns}
                  dataSource={conflictItems}
                  rowKey="id"
                  loading={loading}
                  pagination={{ pageSize: 20 }}
                  scroll={{ x: 900 }}
                />
              ),
            },
            {
              key: "ai-candidates",
              label: (
                <span>
                  AI候选{" "}
                  <Badge
                    count={displaySummary.totalAiCandidate}
                    style={{ marginLeft: 8 }}
                    color="blue"
                  />
                </span>
              ),
              children: (
                <>
                  {selectedRowKeys.length > 0 && (
                    <div style={{ marginBottom: 16 }}>
                      <Space>
                        <Text>已选择 {selectedRowKeys.length} 项</Text>
                        <DangerConfirm
                          level="low"
                          title="确认批量采纳？"
                          description={`将采纳 ${selectedRowKeys.length} 个AI候选映射`}
                          onConfirm={handleBatchAdopt}
                        >
                          <Button type="primary">批量采纳</Button>
                        </DangerConfirm>
                        <Button onClick={() => setSelectedRowKeys([])}>取消选择</Button>
                      </Space>
                    </div>
                  )}
                  <Table
                    columns={aiCandidateColumns}
                    dataSource={aiCandidates}
                    rowKey={(record) => `${record.sourceCode}-${record.conceptType}`}
                    loading={loading}
                    pagination={{ pageSize: 20 }}
                    scroll={{ x: 1000 }}
                  />
                </>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}

// ─── 模拟数据 ────────────────────────────────────────────────────────────────

const MOCK_SUMMARY: MappingSummary = {
  totalUnmapped: 18,
  totalMapped: 156,
  totalConflict: 2,
  totalAiCandidate: 7,
  byConceptType: {
    DIAGNOSIS: 45,
    PROCEDURE: 32,
    DRUG: 68,
    LAB: 28,
    OBSERVATION: 12,
    DEVICE: 7,
  },
};

const MOCK_UNMAPPED: TerminologyItem[] = [
  {
    id: 1,
    sourceSystem: "HIS",
    sourceCode: "LIS_LDH_001",
    sourceName: "乳酸脱氢酶",
    conceptType: "LAB",
    mappingStatus: "UNMAPPED",
    occurrenceCount: 1250,
  },
  {
    id: 2,
    sourceSystem: "HIS",
    sourceCode: "LIS_TNI_07",
    sourceName: "肌钙蛋白 I",
    conceptType: "LAB",
    mappingStatus: "UNMAPPED",
    occurrenceCount: 980,
  },
  {
    id: 3,
    sourceSystem: "HIS",
    sourceCode: "DRUG_ASP_001",
    sourceName: "阿司匹林肠溶片",
    conceptType: "DRUG",
    mappingStatus: "UNMAPPED",
    occurrenceCount: 3500,
  },
  {
    id: 4,
    sourceSystem: "HIS",
    sourceCode: "DX_AMI_001",
    sourceName: "急性心肌梗死",
    conceptType: "DIAGNOSIS",
    mappingStatus: "UNMAPPED",
    occurrenceCount: 420,
  },
];

const MOCK_MAPPED: TerminologyItem[] = [
  {
    id: 101,
    sourceSystem: "HIS",
    sourceCode: "LIS_CRP_001",
    sourceName: "C反应蛋白",
    conceptType: "LAB",
    mappingStatus: "MAPPED",
    standardCode: "LOINC:1988-5",
    standardName: "C-reactive protein [Mass/volume] in Serum or Plasma",
    reviewedBy: "张信息",
    reviewedTime: "2026-05-18T10:00:00",
  },
];

const MOCK_CONFLICT: TerminologyItem[] = [
  {
    id: 201,
    sourceSystem: "HIS",
    sourceCode: "LIS_WBC_001",
    sourceName: "白细胞计数",
    conceptType: "LAB",
    mappingStatus: "CONFLICT",
    reviewComment: "存在多个LOINC候选: 6690-2 (血液) vs 26464-8 (体液)",
  },
  {
    id: 202,
    sourceSystem: "HIS",
    sourceCode: "DX_HTN_001",
    sourceName: "高血压病",
    conceptType: "DIAGNOSIS",
    mappingStatus: "CONFLICT",
    reviewComment: "ICD-11 BA00 vs BA01 分型不明确",
  },
];

const MOCK_AI_CANDIDATES: AiCandidate[] = [
  {
    sourceCode: "LIS_LDH_001",
    sourceName: "乳酸脱氢酶",
    conceptType: "LAB",
    proposedStandardCode: "LOINC:2532-0",
    proposedStandardName: "Lactate dehydrogenase [Enzymatic activity/volume] in Serum or Plasma",
    confidence: 0.95,
    mappingSource: "GPT-4-med",
  },
  {
    sourceCode: "LIS_TNI_07",
    sourceName: "肌钙蛋白 I",
    conceptType: "LAB",
    proposedStandardCode: "LOINC:6597-9",
    proposedStandardName: "Troponin I.cardiac [Mass/volume] in Serum or Plasma",
    confidence: 0.89,
    mappingSource: "GPT-4-med",
  },
  {
    sourceCode: "DRUG_ASP_001",
    sourceName: "阿司匹林肠溶片",
    conceptType: "DRUG",
    proposedStandardCode: "ATC:B01AC06",
    proposedStandardName: "Acetylsalicylic acid",
    confidence: 0.98,
    mappingSource: "GPT-4-med",
  },
  {
    sourceCode: "DX_AMI_001",
    sourceName: "急性心肌梗死",
    conceptType: "DIAGNOSIS",
    proposedStandardCode: "ICD-11:BA41",
    proposedStandardName: "Acute myocardial infarction",
    confidence: 0.92,
    mappingSource: "GPT-4-med",
  },
];
