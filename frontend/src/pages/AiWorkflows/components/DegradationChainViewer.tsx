/**
 * 降级链可视化（PR-FINAL-13）。
 *
 * 6 种 callType（RESEARCH / EXTRACT / EMBEDDING / RERANK / CRITIC / WORKFLOW）的链路
 * 横向展示：[QIANWEN ✓] → [DEEPSEEK ✓] → [OLLAMA_LOCAL ✗] → [LOCAL ✓]。
 *
 * 国情合规：每条链路顶部明示「8 家国产 + Ollama + LOCAL」叙事，Dify 仅 WORKFLOW 出现。
 */

import { ArrowRightOutlined } from "@ant-design/icons";
import type {
  AllDegradationChains,
  CallType,
  ProviderInfo,
  ProviderType,
} from "../../../api/aiWorkflows";
import {
  CALL_TYPE_DESCRIPTIONS,
  CALL_TYPE_LABELS,
  PROVIDER_LABELS,
} from "../../../api/aiWorkflows";
import styles from "../styles.module.css";

export interface DegradationChainViewerProps {
  chains: AllDegradationChains;
}

const ALL_CALL_TYPES: CallType[] = [
  "RESEARCH",
  "EXTRACT",
  "EMBEDDING",
  "RERANK",
  "CRITIC",
  "WORKFLOW",
];

function nodeClass(provider: ProviderInfo): string {
  if (provider.ready) return `${styles.chainNode} ${styles.chainNodeReady}`;
  return `${styles.chainNode} ${styles.chainNodeUnavailable}`;
}

function providerLabel(providerType: string): string {
  return PROVIDER_LABELS[providerType as ProviderType] ?? providerType;
}

export default function DegradationChainViewer({ chains }: DegradationChainViewerProps) {
  return (
    <div className={styles.chainList} role="list">
      {ALL_CALL_TYPES.map((callType) => {
        const chain = chains[callType];
        if (!chain) return null;
        const nodes: ProviderInfo[] =
          Array.isArray(chain.providers) && chain.providers.length > 0
            ? chain.providers
            : (chain.chain ?? "").split(",").filter(Boolean).map((p) => ({
                provider_type: p.trim(),
                ready: false,
                status: "UNAVAILABLE",
              }));
        return (
          <section
            key={callType}
            className={styles.chainCard}
            role="listitem"
            aria-label={`chain-${callType}`}
          >
            <header className={styles.chainHeader}>
              <div>
                <span className={styles.chainTitle}>{CALL_TYPE_LABELS[callType]}</span>
                <span className={styles.chainCallType}>call_type = {callType}</span>
              </div>
            </header>
            <p className={styles.chainDescription}>{CALL_TYPE_DESCRIPTIONS[callType]}</p>
            <div className={styles.chainNodes}>
              {nodes.map((node, idx) => (
                <span key={`${node.provider_type}-${idx}`} role="presentation">
                  <span className={nodeClass(node)} aria-label={`node-${node.provider_type}`}>
                    {providerLabel(node.provider_type)}
                  </span>
                  {idx < nodes.length - 1 && (
                    <ArrowRightOutlined className={styles.chainArrow} aria-hidden />
                  )}
                </span>
              ))}
            </div>
            <div className={styles.chainLegend}>
              <span>就绪 ✓</span>
              <span>不可用 ✗（自动跳过，走下一个）</span>
            </div>
          </section>
        );
      })}
    </div>
  );
}
