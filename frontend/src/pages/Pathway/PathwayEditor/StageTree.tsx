import React, { useCallback } from "react";
import { Tree, Typography } from "antd";
import type { PathwayDef } from "../../../components/PathwayCanvas/types";
import { SourceInfo } from "../../../components";
import styles from "./PathwayEditor.module.css";

const { Text } = Typography;

interface StageTreeProps {
  pathway: PathwayDef;
  selectedNodeId?: string;
  onSelect: (nodeId: string) => void;
}

const StageTree: React.FC<StageTreeProps> = ({ pathway, selectedNodeId, onSelect }) => {
  const stageNodes = pathway.nodes.filter((n) => n.type === "stage");
  const taskNodes = pathway.nodes.filter((n) => n.type === "task");

  const treeData = [
    {
      key: "root",
      title: (
        <span className={styles.treeRootTitle}>
          <Text strong>{pathway.name}</Text>
          <SourceInfo
            source={{ documentName: pathway.name, documentId: pathway.code }}
            review={{ status: "pending" }}
            version={pathway.version}
            variant="compact"
          />
        </span>
      ),
      children: stageNodes.map((stage) => ({
        key: stage.id,
        title: <Text>{stage.label}</Text>,
        children: taskNodes
          .filter((t) => t.stage === stage.id)
          .map((task) => ({
            key: task.id,
            title: <Text type="secondary">{task.label}</Text>,
            isLeaf: true,
          })),
      })),
    },
  ];

  const handleSelect = useCallback(
    (keys: React.Key[]) => {
      if (keys.length > 0) {
        onSelect(String(keys[0]));
      }
    },
    [onSelect],
  );

  return (
    <div className={styles.treePanel}>
      <Tree
        treeData={treeData}
        selectedKeys={selectedNodeId ? [selectedNodeId] : []}
        onSelect={handleSelect}
        defaultExpandAll
        blockNode
      />
    </div>
  );
};

export default StageTree;
