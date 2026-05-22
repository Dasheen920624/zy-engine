import { useMemo } from "react";
import { Typography, Spin, Empty } from "antd";
import type { DiffViewerProps, DiffLine } from "./DiffViewer.types";
import styles from "./diffViewer.module.css";

const { Text } = Typography;

const generateDiff = (oldText: string, newText: string): DiffLine[] => {
  const oldLines = oldText.split("\n");
  const newLines = newText.split("\n");
  const result: DiffLine[] = [];
  
  let oldIndex = 0;
  let newIndex = 0;
  
  while (oldIndex < oldLines.length || newIndex < newLines.length) {
    const oldLine = oldLines[oldIndex];
    const newLine = newLines[newIndex];
    
    if (oldIndex >= oldLines.length) {
      // Only new lines left
      result.push({
        type: "add",
        content: newLine,
        newLineNumber: newIndex + 1,
      });
      newIndex++;
    } else if (newIndex >= newLines.length) {
      // Only old lines left
      result.push({
        type: "remove",
        content: oldLine,
        oldLineNumber: oldIndex + 1,
      });
      oldIndex++;
    } else if (oldLine === newLine) {
      // Lines are the same
      result.push({
        type: "normal",
        content: oldLine,
        oldLineNumber: oldIndex + 1,
        newLineNumber: newIndex + 1,
      });
      oldIndex++;
      newIndex++;
    } else {
      // Lines are different
      result.push({
        type: "remove",
        content: oldLine,
        oldLineNumber: oldIndex + 1,
      });
      result.push({
        type: "add",
        content: newLine,
        newLineNumber: newIndex + 1,
      });
      oldIndex++;
      newIndex++;
    }
  }
  
  return result;
};

const LINE_CONTAINER_CLASS: Record<string, string> = {
  add: styles.lineAdd,
  remove: styles.lineRemove,
  normal: styles.lineNormal,
};

const LINE_PREFIX_CLASS: Record<string, string> = {
  add: styles.linePrefixAdd,
  remove: styles.linePrefixRemove,
  normal: styles.linePrefixNormal,
};

const LINE_PREFIX: Record<string, string> = {
  add: "+",
  remove: "-",
};

export default function DiffViewer({
  oldContent,
  newContent,
  title,
  oldTitle = "旧版本",
  newTitle = "新版本",
  mode = "unified",
  showLineNumbers = true,
  loading = false,
}: DiffViewerProps) {
  const diffLines = useMemo(() => generateDiff(oldContent, newContent), [oldContent, newContent]);
  
  if (loading) {
    return (
      <div className={styles.loadingContainer}>
        <Spin tip="加载差异中..." />
      </div>
    );
  }
  
  if (!oldContent && !newContent) {
    return <Empty description="无内容可对比" />;
  }
  
  const renderLine = (line: DiffLine, index: number) => {
    return (
      <div
        key={index}
        className={`${styles.line} ${LINE_CONTAINER_CLASS[line.type] || styles.lineNormal}`}
      >
        {showLineNumbers && (
          <>
            {mode === "split" ? (
              <span className={styles.lineNumber}>
                {line.oldLineNumber || ""}
              </span>
            ) : (
              <>
                <span className={styles.lineNumber}>
                  {line.oldLineNumber || ""}
                </span>
                <span className={styles.lineNumber}>
                  {line.newLineNumber || ""}
                </span>
              </>
            )}
          </>
        )}
        <span className={`${styles.linePrefix} ${LINE_PREFIX_CLASS[line.type] || styles.linePrefixNormal}`}>
          {LINE_PREFIX[line.type] || " "}
        </span>
        <span>{line.content}</span>
      </div>
    );
  };
  
  const renderUnified = () => (
    <div className={styles.unifiedContainer}>
      {diffLines.map((line, index) => renderLine(line, index))}
    </div>
  );
  
  const renderSplit = () => {
    const oldLines = diffLines.filter(line => line.type !== "add");
    const newLines = diffLines.filter(line => line.type !== "remove");
    
    return (
      <div className={styles.splitContainer}>
        <div className={styles.splitPanel}>
          <div className={styles.panelHeader}>
            <Text strong>{oldTitle}</Text>
          </div>
          {oldLines.map((line, index) => renderLine(line, index))}
        </div>
        <div className={styles.splitPanel}>
          <div className={styles.panelHeader}>
            <Text strong>{newTitle}</Text>
          </div>
          {newLines.map((line, index) => renderLine(line, index))}
        </div>
      </div>
    );
  };
  
  return (
    <div>
      {title && (
        <div className={styles.titleContainer}>
          <Text strong>{title}</Text>
        </div>
      )}
      {mode === "split" ? renderSplit() : renderUnified()}
    </div>
  );
}
