/**
 * DSL 编辑器：CodeMirror 6 JSON 语法高亮 + 折叠 + 行号 + 实时 Schema 校验。
 *
 * 包装 @uiw/react-codemirror（社区事实标准）。
 * Schema 校验委托给 helpers/ruleSchema.ts（不引入 ajv，保持 bundle 小）。
 */

import { useEffect, useMemo, useState } from "react";
import CodeMirror from "@uiw/react-codemirror";
import { json } from "@codemirror/lang-json";
import { oneDark } from "@codemirror/theme-one-dark";
import type { Extension } from "@codemirror/state";
import {
  safeParseJson,
  validateRuleDsl,
  type DslValidationError,
} from "../helpers/ruleSchema";
import styles from "../styles.module.css";

export interface DslEditorProps {
  value: string;
  onChange: (value: string) => void;
  onValidationChange?: (result: { valid: boolean; errors: DslValidationError[] }) => void;
  readOnly?: boolean;
  minHeight?: string;
}

export default function DslEditor({
  value,
  onChange,
  onValidationChange,
  readOnly = false,
  minHeight = "380px",
}: DslEditorProps) {
  const [errors, setErrors] = useState<DslValidationError[]>([]);

  const extensions: Extension[] = useMemo(() => [json()], []);

  useEffect(() => {
    const parsed = safeParseJson(value);
    let result: { valid: boolean; errors: DslValidationError[] };
    if (!parsed.ok) {
      result = { valid: false, errors: [{ path: "$", message: `JSON 语法错误：${parsed.error}` }] };
    } else {
      result = validateRuleDsl(parsed.value);
    }
    setErrors(result.errors);
    onValidationChange?.(result);
  }, [value, onValidationChange]);

  return (
    <div>
      <div className={styles.dslContainer}>
        <CodeMirror
          value={value}
          onChange={(next) => onChange(next)}
          extensions={extensions}
          theme={oneDark}
          editable={!readOnly}
          basicSetup={{
            lineNumbers: true,
            highlightActiveLine: true,
            foldGutter: true,
            bracketMatching: true,
            closeBrackets: true,
            autocompletion: true,
            indentOnInput: true,
          }}
          minHeight={minHeight}
          className={styles.dslEditor}
        />
      </div>
      {errors.length > 0 ? (
        <ul className={styles.errorList} role="alert">
          {errors.slice(0, 10).map((err, idx) => (
            <li key={`${err.path}-${idx}`} className={styles.errorListItem}>
              <code>{err.path}</code>: {err.message}
            </li>
          ))}
        </ul>
      ) : (
        <span className={styles.dslHint}>
          DSL 结构合规。提示：可按 Schema 添加 trigger / condition / result，详见
          ai-dev-input/03_data_models/rule_dsl.schema.json
        </span>
      )}
    </div>
  );
}
