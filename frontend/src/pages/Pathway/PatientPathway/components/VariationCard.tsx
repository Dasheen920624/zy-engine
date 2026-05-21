/**
 * 变异记录卡：渲染单条 PathwayVariationRecord。
 */

import { Tag } from "antd";
import type { PathwayVariationRecord } from "../../../../api/pathway";
import {
  describeVariationType,
  formatTimestamp,
  VARIATION_TYPE_COLOR,
} from "../../helpers/pathwayFormatters";
import styles from "../../styles.module.css";

export interface VariationCardProps {
  variation: PathwayVariationRecord;
}

export default function VariationCard({ variation }: VariationCardProps) {
  return (
    <div className={styles.variationItem} role="listitem">
      <div className={styles.variationHeader}>
        <Tag color={VARIATION_TYPE_COLOR[variation.variation_type] ?? "default"}>
          {describeVariationType(variation.variation_type)}
        </Tag>
        {variation.node_code && <Tag>节点 {variation.node_code}</Tag>}
      </div>
      {variation.reason && <div className={styles.variationReason}>{variation.reason}</div>}
      <div className={styles.variationFooter}>
        {variation.operator_id ? `${variation.operator_id} · ` : ""}
        {formatTimestamp(variation.created_time)}
      </div>
    </div>
  );
}
