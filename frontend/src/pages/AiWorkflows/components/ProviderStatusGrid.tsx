/**
 * Provider 状态宫格：把 Provider 按「国产大模型 / 本地兜底 / 工作流编排」三类分组渲染。
 */

import type { ProviderInfo } from "../../../api/aiWorkflows";
import { DOMESTIC_PROVIDERS } from "../../../api/aiWorkflows";
import ProviderStatusCard from "./ProviderStatusCard";
import styles from "../styles.module.css";

export interface ProviderStatusGridProps {
  providers: ProviderInfo[];
}

interface Group {
  key: string;
  title: string;
  subtitle: string;
  filter: (info: ProviderInfo) => boolean;
}

const GROUPS: Group[] = [
  {
    key: "domestic",
    title: "国产大模型直连（OpenAI 兼容协议）",
    subtitle: "通义 / DeepSeek / Kimi / 智谱 / 豆包 / Yi / 百川 / 阶跃 —— 8 家国产主流模型，按场景走降级链",
    filter: (info) => DOMESTIC_PROVIDERS.includes(info.provider_type as never),
  },
  {
    key: "local",
    title: "本地与兜底",
    subtitle: "Ollama 本地模型 + LOCAL 规则兜底（永远可用，保证医院内网部署也能跑）",
    filter: (info) => info.provider_type === "OLLAMA_LOCAL" || info.provider_type === "LOCAL",
  },
  {
    key: "workflow",
    title: "工作流编排（仅 WORKFLOW 类型）",
    subtitle: "Dify 仅用于跨系统多步流程编排，主流叙事中已退出 LLM 调用链",
    filter: (info) => info.provider_type === "DIFY",
  },
];

export default function ProviderStatusGrid({ providers }: ProviderStatusGridProps) {
  if (!providers.length) {
    return (
      <div className={styles.providerEmpty}>
        无 Provider 注册。请检查 application.yml 的 medkernel.model-gateway.* 配置。
      </div>
    );
  }
  const grouped = GROUPS.map((g) => ({
    ...g,
    items: providers.filter(g.filter),
  }));
  // 兜底分组：未被任何 group 匹配的 Provider（自定义 / 未来扩展）
  const matched = new Set(grouped.flatMap((g) => g.items.map((i) => i.provider_type)));
  const other = providers.filter((p) => !matched.has(p.provider_type));

  return (
    <div className={styles.providerGroups}>
      {grouped.map((g) =>
        g.items.length > 0 ? (
          <section key={g.key} aria-label={`provider-group-${g.key}`}>
            <h3 className={styles.providerGroupTitle}>{g.title}</h3>
            <p className={styles.providerGroupSubtitle}>{g.subtitle}</p>
            <div className={styles.providerGrid} role="list">
              {g.items.map((info) => (
                <ProviderStatusCard key={info.provider_type} info={info} />
              ))}
            </div>
          </section>
        ) : null,
      )}
      {other.length > 0 && (
        <section aria-label="provider-group-other">
          <h3 className={styles.providerGroupTitle}>其他 Provider</h3>
          <p className={styles.providerGroupSubtitle}>未分类（自定义或扩展）</p>
          <div className={styles.providerGrid} role="list">
            {other.map((info) => (
              <ProviderStatusCard key={info.provider_type} info={info} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
