/**
 * AI 工作流引擎主页（路由 /ai-workflows，PR-FINAL-13）。
 *
 * 3 Tab 主结构：
 *  Tab 1 Provider 状态：8 家国产 + Ollama + LOCAL + Dify
 *  Tab 2 降级链：6 种 callType 的链路可视化
 *  Tab 3 工作流模板：DifyAdapterController 提供的模板 + 调用统计
 *
 * ADR-0013 去 Dify 化叙事：主流叙事强调「8 家国产大模型直连 + Ollama 本地 + LOCAL 规则兜底」。
 */

import { useMemo } from "react";
import { Alert, Button, Spin, Tabs, Typography } from "antd";
import { ReloadOutlined, RobotOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import {
  listDegradationChains,
  listProviders,
  listWorkflowTemplates,
  workflowInvocationStats,
} from "../../api/aiWorkflows";
import ProviderStatusGrid from "./components/ProviderStatusGrid";
import DegradationChainViewer from "./components/DegradationChainViewer";
import WorkflowTemplateList from "./components/WorkflowTemplateList";
import InvocationStatsCard from "./components/InvocationStatsCard";
import styles from "./styles.module.css";

const { Title } = Typography;

export default function AiWorkflowsPage() {
  const providersQuery = useQuery({
    queryKey: ["ai-workflows", "providers"],
    queryFn: listProviders,
    refetchInterval: 60_000,
  });

  const chainsQuery = useQuery({
    queryKey: ["ai-workflows", "chains"],
    queryFn: listDegradationChains,
  });

  const templatesQuery = useQuery({
    queryKey: ["ai-workflows", "templates"],
    queryFn: listWorkflowTemplates,
  });

  const statsQuery = useQuery({
    queryKey: ["ai-workflows", "stats"],
    queryFn: () => workflowInvocationStats({ limit: 20 }),
  });

  const refreshAll = () => {
    providersQuery.refetch();
    chainsQuery.refetch();
    templatesQuery.refetch();
    statsQuery.refetch();
  };

  const tabs = useMemo(
    () => [
      {
        key: "providers",
        label: "Provider 状态",
        children: providersQuery.isLoading ? (
          <Spin tip="加载 Provider 状态..." />
        ) : providersQuery.isError ? (
          <Alert
            type="error"
            showIcon
            message="无法加载 Provider 状态"
            description={(providersQuery.error as Error)?.message}
          />
        ) : (
          <ProviderStatusGrid providers={providersQuery.data ?? []} />
        ),
      },
      {
        key: "chains",
        label: "降级链",
        children: chainsQuery.isLoading ? (
          <Spin tip="加载降级链..." />
        ) : chainsQuery.isError ? (
          <Alert
            type="error"
            showIcon
            message="无法加载降级链"
            description={(chainsQuery.error as Error)?.message}
          />
        ) : (
          <DegradationChainViewer chains={chainsQuery.data ?? {}} />
        ),
      },
      {
        key: "workflows",
        label: "多步工作流",
        children: (
          <>
            <section aria-label="workflow-stats">
              <Title level={5}>调用统计</Title>
              {statsQuery.isLoading ? <Spin /> : <InvocationStatsCard stats={statsQuery.data} />}
            </section>
            <section aria-label="workflow-templates">
              <Title level={5}>模板列表</Title>
              {templatesQuery.isLoading ? (
                <Spin />
              ) : templatesQuery.isError ? (
                <Alert
                  type="error"
                  showIcon
                  message="无法加载工作流模板"
                  description={(templatesQuery.error as Error)?.message}
                />
              ) : (
                <WorkflowTemplateList templates={templatesQuery.data ?? []} />
              )}
            </section>
          </>
        ),
      },
    ],
    [providersQuery, chainsQuery, templatesQuery, statsQuery],
  );

  return (
    <div className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <Title level={3} className={styles.pageTitle}>
            <RobotOutlined /> AI 工作流引擎
          </Title>
          <p className={styles.pageSubtitle}>
            监控 AI 模型服务状态、查看降级策略、管理工作流模板。
          </p>
        </div>
        <div className={styles.headerActions}>
          <Button icon={<ReloadOutlined />} onClick={refreshAll}>
            刷新全部
          </Button>
        </div>
      </header>

      <section className={styles.heroBanner} aria-label="hero-banner">
        <h3 className={styles.heroTitle}>本平台 AI 编排核心方针</h3>
        <ul className={styles.heroPoints}>
          <li className={styles.heroPoint}>
            <span className={styles.heroPointAccent}>8 家国产大模型直连</span>：通义 / DeepSeek / Kimi / 智谱 / 豆包 / Yi / 百川 / 阶跃，全部走 OpenAI 兼容协议，不依赖 Dify
          </li>
          <li className={styles.heroPoint}>
            <span className={styles.heroPointAccent}>Ollama 本地兜底</span>：医院内网部署可选，断网也能推理
          </li>
          <li className={styles.heroPoint}>
            <span className={styles.heroPointAccent}>LOCAL 规则兜底</span>：永远可用，保证临床场景不会因 LLM 不可用而失服
          </li>
          <li className={styles.heroPoint}>
            <span className={styles.heroPointAccent}>Dify 仅 WORKFLOW</span>：仅复杂多步流程编排时使用，主调用链已收敛到国产直连
          </li>
        </ul>
      </section>

      <div className={styles.tabsCard}>
        <Tabs items={tabs} />
      </div>
    </div>
  );
}
