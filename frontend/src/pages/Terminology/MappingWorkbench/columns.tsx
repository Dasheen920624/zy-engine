import type { ColumnsType } from "antd/es/table";
import { Button, Checkbox, Space, Tag, Typography } from "antd";
import { WarningOutlined } from "@ant-design/icons";
import type { TerminologyItem, AiCandidate, ConceptType } from "../../../api/types";
import { AiGeneratedBadge } from "../../../components";
import { CONCEPT_TYPE_MAP } from "../constants";
import DangerConfirm from "./DangerConfirm";
import styles from "../../mappingWorkbench.module.css";

const { Text } = Typography;

export function getUnmappedColumns(onAdopt: (item: TerminologyItem) => void): ColumnsType<TerminologyItem> {
  return [
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
          <Button size="small" type="primary" onClick={() => onAdopt(record)}>
            手动映射
          </Button>
        </Space>
      ),
    },
  ];
}

export function getMappedColumns(): ColumnsType<TerminologyItem> {
  return [
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
}

export function getConflictColumns(onAdopt: (item: TerminologyItem) => void): ColumnsType<TerminologyItem> {
  return [
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
          <Button size="small" onClick={() => onAdopt(record)}>
            手动解决
          </Button>
        </Space>
      ),
    },
  ];
}

export function getAiCandidateColumns(callbacks: {
  handleAdopt: (item: AiCandidate) => void;
  handleReject: (candidate: AiCandidate) => void;
  selectedRowKeys: string[];
  onSelectedRowKeysChange: (keys: string[]) => void;
}): ColumnsType<AiCandidate> {
  const { handleAdopt, handleReject, selectedRowKeys, onSelectedRowKeysChange } = callbacks;

  return [
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
              onSelectedRowKeysChange([...selectedRowKeys, key]);
            } else {
              onSelectedRowKeysChange(selectedRowKeys.filter((k) => k !== key));
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
            <AiGeneratedBadge confidence={record.confidence} />
            <Text strong>{record.proposedStandardCode}</Text>
          </Space>
          <Text type="secondary" className={styles.smallText}>
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
}
