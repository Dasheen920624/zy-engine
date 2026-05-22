import React from "react";
import { Descriptions, Input, InputNumber, Switch, Select, Tag, Typography, Empty } from "antd";
import type { PathwayNode } from "../../../components/PathwayCanvas/types";
import { StatusBadge } from "../../../components";
import styles from "./PathwayEditor.module.css";

const { Text } = Typography;

interface NodePropertyPanelProps {
  node?: PathwayNode;
  onChange: (updated: PathwayNode) => void;
  readOnly?: boolean;
}

const ruleOptions = [
  { value: "R_ECG_TIMELIMIT", label: "R_ECG_TIMELIMIT — ECG 时限" },
  { value: "R_MEDICATION_CHECK", label: "R_MEDICATION_CHECK — 药物检查" },
  { value: "R_PROGRESS_TIMEOUT", label: "R_PROGRESS_TIMEOUT — 病程超时" },
];

const graphQueryOptions = [
  { value: "AMI_RELATED_EXAM", label: "AMI 相关检查" },
  { value: "PCI_PROCEDURE", label: "PCI 术式" },
];

const NodePropertyPanel: React.FC<NodePropertyPanelProps> = ({ node, onChange, readOnly }) => {
  if (!node) {
    return (
      <div className={styles.emptyState}>
        <Empty description="请在画布中选择节点" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      </div>
    );
  }

  const updateProperty = (key: string, value: unknown) => {
    onChange({
      ...node,
      properties: { ...node.properties, [key]: value },
    });
  };

  const typeLabels: Record<string, string> = {
    stage: "阶段",
    task: "检查任务",
    decision: "决策节点",
    start: "开始",
    end: "结束",
  };

  return (
    <div className={styles.nodePanel}>
      <Descriptions column={1} size="small" bordered>
        <Descriptions.Item label="类型">
          <Tag>{typeLabels[node.type] ?? node.type}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="名称">
          {readOnly ? (
            <Text>{node.label}</Text>
          ) : (
            <Input
              value={node.label}
              onChange={(e) => onChange({ ...node, label: e.target.value })}
              size="small"
            />
          )}
        </Descriptions.Item>
        {node.type === "task" && (
          <>
            <Descriptions.Item label="超时（分钟）">
              <InputNumber
                value={node.properties.timeout_minutes}
                onChange={(v) => updateProperty("timeout_minutes", v ?? undefined)}
                min={1}
                size="small"
                disabled={readOnly}
                className={styles.fullWidth}
              />
            </Descriptions.Item>
            <Descriptions.Item label="绑定规则">
              <Select
                mode="multiple"
                value={node.properties.bound_rules ?? []}
                onChange={(v) => updateProperty("bound_rules", v)}
                options={ruleOptions}
                size="small"
                disabled={readOnly}
                className={styles.fullWidth}
              />
            </Descriptions.Item>
            <Descriptions.Item label="绑定图谱查询">
              <Select
                mode="multiple"
                value={node.properties.bound_graph_queries ?? []}
                onChange={(v) => updateProperty("bound_graph_queries", v)}
                options={graphQueryOptions}
                size="small"
                disabled={readOnly}
                className={styles.fullWidth}
              />
            </Descriptions.Item>
            <Descriptions.Item label="绑定 Dify">
              <Text type="secondary">无</Text>
            </Descriptions.Item>
            <Descriptions.Item label="流转条件">
              <Input
                value={node.properties.transition_condition ?? ""}
                onChange={(e) => updateProperty("transition_condition", e.target.value || undefined)}
                placeholder="如：ECG完成"
                size="small"
                disabled={readOnly}
              />
            </Descriptions.Item>
            <Descriptions.Item label="变异记录必填">
              <Switch
                checked={node.properties.variation_required ?? false}
                onChange={(v) => updateProperty("variation_required", v)}
                disabled={readOnly}
                size="small"
              />
            </Descriptions.Item>
            <Descriptions.Item label="来源审核">
              {node.properties.source_verified ? (
                <StatusBadge status="success" />
              ) : (
                <StatusBadge status="warning" />
              )}
            </Descriptions.Item>
          </>
        )}
      </Descriptions>
    </div>
  );
};

export default NodePropertyPanel;
