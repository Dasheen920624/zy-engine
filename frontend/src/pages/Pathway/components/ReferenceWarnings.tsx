/**
 * 引用警告卡：把后端 reference_warnings 渲染为可见警告（ADR-0004 来源追溯）。
 *
 * reference_warnings 是 unknown[]，逐项 stringify 展示。
 */

import { WarningOutlined } from "@ant-design/icons";
import styles from "../styles.module.css";

export interface ReferenceWarningsProps {
  warnings: unknown[];
}

function describeWarning(w: unknown): string {
  if (w === null || w === undefined) return "(空警告)";
  if (typeof w === "string") return w;
  if (typeof w === "object") {
    const obj = w as Record<string, unknown>;
    if (typeof obj.message === "string") return obj.message;
    if (typeof obj.detail === "string") return obj.detail;
    if (typeof obj.reason === "string") return obj.reason;
    return JSON.stringify(obj);
  }
  return String(w);
}

export default function ReferenceWarnings({ warnings }: ReferenceWarningsProps) {
  if (!warnings || warnings.length === 0) return null;
  return (
    <div className={styles.warningCard} role="alert">
      <div className={styles.warningTitle}>
        <WarningOutlined /> 引用警告 · {warnings.length} 项
      </div>
      <ul className={styles.warningList}>
        {warnings.map((w, idx) => (
          <li key={idx}>{describeWarning(w)}</li>
        ))}
      </ul>
    </div>
  );
}
