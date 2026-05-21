/**
 * 工作流模板列表（PR-FINAL-13）：渲染 /api/dify/workflows 模板。
 *
 * 国情合规：列表本身不强调 Dify 品牌（标题为「多步工作流模板」），仅在 Provider 状态
 * 卡和 WORKFLOW 降级链尾部显示 Dify。
 */

import { Tag } from "antd";
import type { WorkflowTemplate } from "../../../api/aiWorkflows";
import styles from "../styles.module.css";

export interface WorkflowTemplateListProps {
  templates: WorkflowTemplate[];
}

export default function WorkflowTemplateList({ templates }: WorkflowTemplateListProps) {
  if (!templates.length) {
    return (
      <div className={styles.workflowEmpty}>
        <p>暂无多步工作流模板</p>
        <p className={styles.workflowEmptyHint}>
          可通过 POST <code>/api/dify/workflows</code> 导入模板包，或在配置中心管理。
        </p>
      </div>
    );
  }
  return (
    <div className={styles.workflowList} role="list">
      {templates.map((tpl) => (
        <article
          key={`${tpl.workflow_code}@${tpl.workflow_version ?? "0"}`}
          className={styles.workflowCard}
          role="listitem"
          aria-label={`workflow-${tpl.workflow_code}`}
        >
          <div className={styles.workflowName}>
            <span>{tpl.workflow_name ?? tpl.workflow_code}</span>
            {tpl.workflow_version ? <Tag>v{tpl.workflow_version}</Tag> : null}
          </div>
          {tpl.description && (
            <div className={styles.workflowDescription}>{tpl.description}</div>
          )}
          <div className={styles.workflowMeta}>
            <span className={styles.workflowMetaItem}>code: {tpl.workflow_code}</span>
            {tpl.timeout_ms !== undefined && (
              <span className={styles.workflowMetaItem}>timeout: {tpl.timeout_ms} ms</span>
            )}
            {tpl.retry_count !== undefined && (
              <span className={styles.workflowMetaItem}>retry: {tpl.retry_count}</span>
            )}
            {tpl.required_inputs && tpl.required_inputs.length > 0 && (
              <span className={styles.workflowMetaItem}>
                inputs: {tpl.required_inputs.join(", ")}
              </span>
            )}
            {tpl.reference_document_code && (
              <span className={styles.workflowMetaItem}>
                source: {tpl.reference_document_code}
              </span>
            )}
          </div>
        </article>
      ))}
    </div>
  );
}
