/**
 * 单个 LLM Provider 状态卡（PR-FINAL-13）。
 *
 * 8 家国产 + Ollama 本地 + Dify + LOCAL 用统一卡渲染，按 ready/status 区分边框色。
 */

import { Tag } from "antd";
import { CheckCircleOutlined, MinusCircleOutlined, WarningOutlined } from "@ant-design/icons";
import type { ProviderInfo, ProviderType } from "../../../api/aiWorkflows";
import { PROVIDER_LABELS } from "../../../api/aiWorkflows";
import styles from "../styles.module.css";

export interface ProviderStatusCardProps {
  info: ProviderInfo;
}

function cardClass(info: ProviderInfo): string {
  if (info.status === "READY" && info.ready) {
    return `${styles.providerCard} ${styles.providerCardReady}`;
  }
  if (info.status === "NOT_FOUND" || info.registered === false) {
    return `${styles.providerCard} ${styles.providerCardDanger}`;
  }
  return `${styles.providerCard} ${styles.providerCardUnavailable}`;
}

function statusIcon(info: ProviderInfo) {
  if (info.status === "READY" && info.ready) {
    return <CheckCircleOutlined aria-label="ready" />;
  }
  if (info.status === "NOT_FOUND" || info.registered === false) {
    return <WarningOutlined aria-label="not-found" />;
  }
  return <MinusCircleOutlined aria-label="unavailable" />;
}

function statusLabel(info: ProviderInfo): string {
  if (info.ready) return "就绪";
  if (info.status === "NOT_FOUND") return "未注册";
  if (info.registered === false) return "未配置";
  return "不可用";
}

function statusColor(info: ProviderInfo): "success" | "default" | "error" | "warning" {
  if (info.ready) return "success";
  if (info.status === "NOT_FOUND") return "error";
  if (info.registered === false) return "error";
  return "warning";
}

export default function ProviderStatusCard({ info }: ProviderStatusCardProps) {
  const friendly =
    info.provider_name ??
    PROVIDER_LABELS[info.provider_type as ProviderType] ??
    info.provider_type;
  return (
    <div className={cardClass(info)} role="listitem">
      <div className={styles.providerName}>
        <span>{friendly}</span>
        <Tag color={statusColor(info)} icon={statusIcon(info)}>
          {statusLabel(info)}
        </Tag>
      </div>
      <span className={styles.providerType}>{info.provider_type}</span>
      {info.reason && <div className={styles.providerReason}>{info.reason}</div>}
    </div>
  );
}
