/**
 * 编辑器顶栏：标题 + 状态 + 操作。
 *
 * 把保存 / 发布 / 校验状态外挂；具体动作由父组件实现。
 */

import { Button, Space } from "antd";
import { CheckCircleOutlined, CloseCircleOutlined, SaveOutlined, RocketOutlined } from "@ant-design/icons";
import styles from "../styles.module.css";

export interface EditorHeaderProps {
  title: string;
  valid: boolean;
  dirty: boolean;
  saving?: boolean;
  publishing?: boolean;
  onSave: () => void;
  onPublish: () => void;
}

export default function EditorHeader({
  title,
  valid,
  dirty,
  saving = false,
  publishing = false,
  onSave,
  onPublish,
}: EditorHeaderProps) {
  return (
    <div className={styles.editorHeader}>
      <div className={styles.editorTitle}>
        <span>{title}</span>
        {valid ? (
          <span className={styles.validationOk} aria-label="dsl-valid">
            <CheckCircleOutlined /> DSL 合规
          </span>
        ) : (
          <span className={styles.validationError} aria-label="dsl-invalid">
            <CloseCircleOutlined /> DSL 不合规
          </span>
        )}
      </div>
      <Space className={styles.editorActions}>
        <Button icon={<SaveOutlined />} onClick={onSave} loading={saving} disabled={!dirty || !valid}>
          保存草稿
        </Button>
        <Button
          type="primary"
          icon={<RocketOutlined />}
          onClick={onPublish}
          loading={publishing}
          disabled={!valid || dirty}
        >
          发布
        </Button>
      </Space>
    </div>
  );
}
