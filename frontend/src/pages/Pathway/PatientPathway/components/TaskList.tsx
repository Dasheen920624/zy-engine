/**
 * 任务列表：渲染节点下的任务，含完成 / 跳过按钮。
 */

import { Button, Space } from "antd";
import { CheckOutlined, MinusCircleOutlined } from "@ant-design/icons";
import type { PatientTaskState, TaskStatus } from "../../../../api/pathway";
import { describeTaskStatus, formatTimestamp } from "../../helpers/pathwayFormatters";
import styles from "../../styles.module.css";

export interface TaskListProps {
  tasks: PatientTaskState[];
  onComplete?: (task: PatientTaskState) => void;
  onSkip?: (task: PatientTaskState) => void;
  disabled?: boolean;
}

function pickClass(status: TaskStatus): string {
  if (status === "COMPLETED") return `${styles.taskItem} ${styles.taskItemDone}`;
  if (status === "SKIPPED") return `${styles.taskItem} ${styles.taskItemSkipped}`;
  if (status === "FAILED") return `${styles.taskItem} ${styles.taskItemFailed}`;
  return styles.taskItem;
}

export default function TaskList({ tasks, onComplete, onSkip, disabled }: TaskListProps) {
  if (!tasks.length) {
    return <div className={styles.tableEmptyHint}>该节点暂无任务</div>;
  }
  return (
    <div className={styles.taskList} role="list">
      {tasks.map((task) => {
        const isFinal = task.status === "COMPLETED" || task.status === "SKIPPED";
        return (
          <div key={task.task_code} className={pickClass(task.status)} role="listitem">
            <span className={styles.taskName}>
              {task.required ? "★ " : ""}
              {task.task_name ?? task.task_code}
            </span>
            <div>
              <div>
                <span className={styles.nodeMeta}>
                  {task.task_code}
                  {task.task_type ? ` · ${task.task_type}` : ""}
                </span>
                <span className={styles.taskHint}>
                  状态：{describeTaskStatus(task.status)}
                  {task.updated_time ? ` · 更新 ${formatTimestamp(task.updated_time)}` : ""}
                </span>
              </div>
            </div>
            <Space className={styles.taskActions}>
              {!isFinal && onComplete && (
                <Button
                  size="small"
                  type="primary"
                  icon={<CheckOutlined />}
                  disabled={disabled}
                  onClick={() => onComplete(task)}
                  aria-label={`complete-${task.task_code}`}
                >
                  完成
                </Button>
              )}
              {!isFinal && onSkip && (
                <Button
                  size="small"
                  icon={<MinusCircleOutlined />}
                  disabled={disabled}
                  onClick={() => onSkip(task)}
                  aria-label={`skip-${task.task_code}`}
                >
                  跳过
                </Button>
              )}
            </Space>
          </div>
        );
      })}
    </div>
  );
}
