/**
 * DSL 编辑器：JSON 文本编辑 + 实时 Schema 校验。
 *
 * Schema 校验委托给 helpers/ruleSchema.ts（不引入 ajv，保持 bundle 小）。
 */

import { useEffect, useState } from "react";
import { Input } from "antd";
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
}

export default function DslEditor({
  value,
  onChange,
  onValidationChange,
  readOnly = false,
}: DslEditorProps) {
  const [errors, setErrors] = useState<DslValidationError[]>([]);

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
        <Input.TextArea
          value={value}
          onChange={(event) => onChange(event.target.value)}
          readOnly={readOnly}
          rows={18}
          spellCheck={false}
          className={styles.dslTextArea}
          aria-label="rule-dsl-json"
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
