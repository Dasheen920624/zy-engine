import { useMemo } from "react";
import { Typography, Spin, Empty } from "antd";
import type { DiffViewerProps, DiffLine } from "./DiffViewer.types";

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

const lineStyle = {
  padding: "2px 8px",
  fontFamily: "var(--mk-font-mono)",
  fontSize: 13,
  lineHeight: "20px",
  whiteSpace: "pre-wrap" as const,
  wordBreak: "break-all" as const,
};

const lineNumberStyle = {
  display: "inline-block",
  width: 40,
  textAlign: "right" as const,
  paddingRight: 8,
  color: "var(--mk-text-tertiary)",
  userSelect: "none" as const,
};

const LINE_BG_COLOR: Record<string, string> = {
  add: "var(--mk-success-soft)",
  remove: "var(--mk-danger-soft)",
};

const LINE_BORDER_COLOR: Record<string, string> = {
  add: "var(--mk-success)",
  remove: "var(--mk-danger)",
};

const LINE_TEXT_COLOR: Record<string, string> = {
  add: "var(--mk-success)",
  remove: "var(--mk-danger)",
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
      <div style={{ textAlign: "center", padding: 24 }}>
        <Spin tip="加载差异中..." />
      </div>
    );
  }
  
  if (!oldContent && !newContent) {
    return <Empty description="无内容可对比" />;
  }
  
  const renderLine = (line: DiffLine, index: number) => {
    const bgColor = LINE_BG_COLOR[line.type] || "transparent";
    const borderColor = LINE_BORDER_COLOR[line.type] || "transparent";
    
    return (
      <div
        key={index}
        style={{
          ...lineStyle,
          backgroundColor: bgColor,
          borderLeft: `3px solid ${borderColor}`,
        }}
      >
        {showLineNumbers && (
          <>
            {mode === "split" ? (
              <span style={lineNumberStyle}>
                {line.oldLineNumber || ""}
              </span>
            ) : (
              <>
                <span style={lineNumberStyle}>
                  {line.oldLineNumber || ""}
                </span>
                <span style={lineNumberStyle}>
                  {line.newLineNumber || ""}
                </span>
              </>
            )}
          </>
        )}
        <span style={{ color: LINE_TEXT_COLOR[line.type] || "var(--mk-text-primary)" }}>
          {LINE_PREFIX[line.type] || " "}
        </span>
        <span>{line.content}</span>
      </div>
    );
  };
  
  const renderUnified = () => (
    <div style={{ border: "1px solid var(--mk-border)", borderRadius: "var(--mk-radius-md)", overflow: "hidden" }}>
      {diffLines.map((line, index) => renderLine(line, index))}
    </div>
  );
  
  const renderSplit = () => {
    const oldLines = diffLines.filter(line => line.type !== "add");
    const newLines = diffLines.filter(line => line.type !== "remove");
    
    return (
      <div style={{ display: "flex", gap: 16 }}>
        <div style={{ flex: 1, border: "1px solid var(--mk-border)", borderRadius: "var(--mk-radius-md)", overflow: "hidden" }}>
          <div style={{ padding: "8px 12px", background: "var(--mk-bg-soft)", borderBottom: "1px solid var(--mk-border)" }}>
            <Text strong>{oldTitle}</Text>
          </div>
          {oldLines.map((line, index) => renderLine(line, index))}
        </div>
        <div style={{ flex: 1, border: "1px solid var(--mk-border)", borderRadius: "var(--mk-radius-md)", overflow: "hidden" }}>
          <div style={{ padding: "8px 12px", background: "var(--mk-bg-soft)", borderBottom: "1px solid var(--mk-border)" }}>
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
        <div style={{ marginBottom: 12 }}>
          <Text strong>{title}</Text>
        </div>
      )}
      {mode === "split" ? renderSplit() : renderUnified()}
    </div>
  );
}