/**
 * 路径版本时间轴：把已发布版本 + 当前激活 + 草稿状态串成可视化时间轴。
 *
 * 用于 PathwayDetail 右侧侧栏。
 */

import { Tag, Button } from "antd";
import { DiffOutlined } from "@ant-design/icons";
import styles from "../styles.module.css";

export interface PathwayTimelineProps {
  draftStatus: "DRAFT" | "NONE";
  publishedVersions: string[];
  activeVersion: string | null;
  selectedVersion: string | null;
  onPickVersion?: (versionNo: string) => void;
  onDiffVersion?: (versionNo: string) => void;
}

export default function PathwayTimeline({
  draftStatus,
  publishedVersions,
  activeVersion,
  selectedVersion,
  onPickVersion,
  onDiffVersion,
}: PathwayTimelineProps) {
  // 时间轴顺序：草稿在最上（最新），其次是已发布版本按字面序倒序
  const sortedPublished = [...publishedVersions].sort((a, b) => b.localeCompare(a, undefined, { numeric: true }));

  return (
    <div className={styles.timeline} role="list">
      {draftStatus === "DRAFT" && (
        <div className={`${styles.timelineItem} ${styles.timelineItemDraft}`} role="listitem">
          <span className={styles.timelineVersionLabel}>草稿</span>
          <div>
            <Tag color="warning">未发布</Tag>
            <span className={styles.timelineHint}>编辑器中的最新草稿，未对临床端生效。</span>
          </div>
          <div className={styles.timelineActions} />
        </div>
      )}
      {sortedPublished.length === 0 && draftStatus === "NONE" && (
        <div className={styles.timelineHint}>暂无版本</div>
      )}
      {sortedPublished.map((v) => {
        const isActive = v === activeVersion;
        const isSelected = v === selectedVersion;
        const itemClass = isActive
          ? `${styles.timelineItem} ${styles.timelineItemActive}`
          : styles.timelineItem;
        return (
          <div key={v} className={itemClass} role="listitem">
            <span className={styles.timelineVersionLabel}>v{v}</span>
            <div>
              {isActive && <Tag color="success">激活中</Tag>}
              {isSelected && !isActive && <Tag color="processing">查看中</Tag>}
              <span className={styles.timelineHint}>已发布版本</span>
            </div>
            <div className={styles.timelineActions}>
              {onPickVersion && !isSelected && (
                <Button size="small" onClick={() => onPickVersion(v)}>
                  查看
                </Button>
              )}
              {onDiffVersion && (
                <Button
                  size="small"
                  icon={<DiffOutlined />}
                  onClick={() => onDiffVersion(v)}
                  aria-label={`diff-${v}`}
                >
                  对比
                </Button>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
