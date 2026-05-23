import { useState, useEffect, useCallback } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Input,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Typography,
  message,
} from "antd";
import {
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
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
import { SourceInfo, OrgContextSelector } from "../../components";
import { CONCEPT_TYPE_MAP, STANDARD_DICT_OPTIONS } from "./MappingWorkbench/constants";
import DangerConfirm from "./MappingWorkbench/components/DangerConfirm";
import { MOCK_UNMAPPED, MOCK_MAPPED, MOCK_CONFLICT, MOCK_AI_CANDIDATES, MOCK_SUMMARY } from "./MappingWorkbench/mockData";
import {
  getUnmappedColumns,
  getMappedColumns,
  getConflictColumns,
  getAiCandidateColumns,
} from "./MappingWorkbench/columns";
import styles from "./mappingWorkbench.module.css";

const { Text } = Typography;
const { Search } = Input;

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

  // 列定义
  const unmappedColumns = getUnmappedColumns(handleAdopt);
  const mappedColumns = getMappedColumns();
  const conflictColumns = getConflictColumns(handleAdopt);
  const aiCandidateColumns = getAiCandidateColumns({
    handleAdopt,
    handleReject,
    selectedRowKeys,
    onSelectedRowKeysChange: setSelectedRowKeys,
  });

  const displaySummary = summary || DEFAULT_SUMMARY;

  return (
    <div className={styles.page}>
      <Card
        title={
          <Space>
            <span>字典映射工作台</span>
            <SourceInfo source={{ documentName: "术语映射", documentId: "terminology-mapping" }} />
          </Space>
        }
        extra={
          <Space>
            <OrgContextSelector />
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
            className={styles.alertSpacing}
          />
        )}

        {/* 统计卡片 */}
        <Row gutter={16} className={styles.statsRow}>
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
        <Row gutter={16} className={styles.filterRow}>
          <Col span={6}>
            <Select
              placeholder="概念类型"
              allowClear
              className={styles.fullWidth}
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
              className={styles.fullWidth}
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
                  未映射 <Badge count={displaySummary.totalUnmapped} className={styles.badgeSpacing} />
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
                  已映射 <Badge count={displaySummary.totalMapped} className={styles.badgeSpacing} />
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
                    className={styles.badgeSpacing}
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
                    className={styles.badgeSpacing}
                    color="blue"
                  />
                </span>
              ),
              children: (
                <>
                  {selectedRowKeys.length > 0 && (
                    <div className={styles.selectionBar}>
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
