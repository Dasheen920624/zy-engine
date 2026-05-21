/**
 * 触发历史时间轴：展示最近 N 条 exec-logs。
 *
 * 视觉规约：
 *  - 命中（hit=true）：黄色框 + 触发详情
 *  - 未命中：灰色框
 *  - 错误（result_status=ERROR）：红色框 + 错误码
 */

import type { RuleExecLog } from "../../../api/rule";
import { formatElapsedMs, formatPublishedTime } from "../helpers/ruleFormatters";
import styles from "../styles.module.css";

export interface ExecLogTimelineProps {
  logs: RuleExecLog[];
  emptyText?: string;
}

function classifyLog(log: RuleExecLog): "hit" | "miss" | "error" {
  if (log.result_status === "ERROR" || log.error_code) return "error";
  if (log.hit) return "hit";
  return "miss";
}

export default function ExecLogTimeline({ logs, emptyText = "暂无触发记录" }: ExecLogTimelineProps) {
  if (!logs.length) {
    return <div className={styles.execLogEmpty}>{emptyText}</div>;
  }
  return (
    <div className={styles.execLogList} role="list">
      {logs.map((log) => {
        const kind = classifyLog(log);
        const itemClass =
          kind === "hit"
            ? `${styles.execLogItem} ${styles.execLogItemHit}`
            : kind === "error"
              ? `${styles.execLogItem} ${styles.execLogItemError}`
              : `${styles.execLogItem} ${styles.execLogItemMiss}`;
        const verdict = kind === "hit" ? "命中" : kind === "error" ? "异常" : "未命中";
        return (
          <div key={log.log_id} className={itemClass} role="listitem">
            <strong className={styles.inlineBadge}>{verdict}</strong>
            <div>
              <div className={styles.execLogMessage}>
                {log.message ?? log.error_message ?? "无消息"}
              </div>
              <span className={styles.execLogTrace}>trace: {log.trace_id ?? "—"}</span>
            </div>
            <div>
              <span className={styles.execLogTime}>{formatPublishedTime(log.created_time)}</span>
              <br />
              <span className={styles.execLogElapsed}>耗时 {formatElapsedMs(log.elapsed_ms)}</span>
            </div>
          </div>
        );
      })}
    </div>
  );
}
