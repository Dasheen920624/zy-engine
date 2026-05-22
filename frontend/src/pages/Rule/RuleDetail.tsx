/**
 * 规则详情页（路由 /rule/definitions/:code）。
 *
 * 左侧主区：元信息 / DSL 只读视图 / 触发历史
 * 右侧侧栏：来源追溯 / 触发聚合
 *
 * 来源追溯使用 <SourceInfo>（ADR-0004），缺失也渲染「来源缺失」状态卡。
 */

import { useNavigate, useParams } from "react-router-dom";
import { Button, Collapse, Empty, Result, Space, Spin, Statistic, Typography } from "antd";
import { ArrowLeftOutlined, EditOutlined, HistoryOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import {
  getRule,
  listRuleExecLogs,
  summarizeRuleExecLogs,
} from "../../api/rule";
import RuleTypeTag from "./components/RuleTypeTag";
import SeverityTag from "./components/SeverityTag";
import SourceCitationCard from "./components/SourceCitationCard";
import ExecLogTimeline from "./components/ExecLogTimeline";
import {
  describeStatus,
  formatElapsedMs,
  formatPublishedTime,
  formatRuleScope,
  stringifyDsl,
} from "./helpers/ruleFormatters";
import styles from "./styles.module.css";

const { Title, Text } = Typography;

export default function RuleDetail() {
  const params = useParams<{ code: string }>();
  const navigate = useNavigate();
  const ruleCode = params.code ?? "";

  const ruleQuery = useQuery({
    queryKey: ["rule", ruleCode],
    queryFn: () => getRule(ruleCode),
    enabled: Boolean(ruleCode),
  });

  const logsQuery = useQuery({
    queryKey: ["rule-exec-logs", ruleCode],
    queryFn: () => listRuleExecLogs({ rule_code: ruleCode, limit: 50 }),
    enabled: Boolean(ruleCode),
  });

  const summaryQuery = useQuery({
    queryKey: ["rule-exec-summary", ruleCode],
    queryFn: () => summarizeRuleExecLogs({ rule_code: ruleCode }),
    enabled: Boolean(ruleCode),
  });

  if (ruleQuery.isLoading) {
    return (
      <div className={styles.page}>
        <Spin tip="加载规则中..." />
      </div>
    );
  }

  if (ruleQuery.isError || !ruleQuery.data) {
    return (
      <Result
        status="404"
        title="规则未找到"
        subTitle={`未找到规则 ${ruleCode}，可能已被下线或无权访问。`}
        extra={
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate("/rule/definitions")}>
            返回规则库
          </Button>
        }
      />
    );
  }

  const rule = ruleQuery.data;
  const dslText = stringifyDsl(rule.rule_json);
  const summary = summaryQuery.data;

  return (
    <div className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <Button
            type="link"
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate("/rule/definitions")}
          >
            返回规则库
          </Button>
          <Title level={3} className={styles.pageTitle}>
            {rule.rule_name || rule.rule_code}
          </Title>
          <Space size="middle" wrap>
            <RuleTypeTag ruleType={rule.rule_type} />
            <SeverityTag severity={rule.severity} />
            <Text type="secondary">版本 {rule.version_no}</Text>
            <Text type="secondary">状态 {describeStatus(rule.status)}</Text>
          </Space>
        </div>
        <div className={styles.headerActions}>
          <Button
            type="primary"
            icon={<EditOutlined />}
            onClick={() => navigate(`/rule/definitions/${encodeURIComponent(rule.rule_code)}/edit`)}
          >
            编辑 / 试运行
          </Button>
        </div>
      </header>

      <div className={styles.detailGrid}>
        <div className={styles.detailMain}>
          {/* ─── 元信息 ─── */}
          <section className={styles.sectionCard} aria-label="rule-meta">
            <h3 className={styles.sectionTitle}>元信息</h3>
            <dl className={styles.metaList}>
              <dt className={styles.metaLabel}>编码</dt>
              <dd className={styles.metaValue}>{rule.rule_code}</dd>
              <dt className={styles.metaLabel}>所属配置包</dt>
              <dd className={styles.metaValue}>
                {rule.package_code ? `${rule.package_code} @ ${rule.package_version ?? "—"}` : "未关联"}
              </dd>
              <dt className={styles.metaLabel}>组织范围</dt>
              <dd className={styles.metaValue}>{formatRuleScope(rule)}</dd>
              <dt className={styles.metaLabel}>动作模式</dt>
              <dd className={styles.metaValue}>{rule.action_mode ?? "—"}</dd>
              <dt className={styles.metaLabel}>需要医师决策</dt>
              <dd className={styles.metaValue}>{rule.decision_required ? "是" : "否"}</dd>
              <dt className={styles.metaLabel}>发布人 / 时间</dt>
              <dd className={styles.metaValue}>
                {rule.published_by ?? "—"} · {formatPublishedTime(rule.published_time)}
              </dd>
            </dl>
          </section>

          {/* ─── DSL 只读（技术详情，默认折叠）─── */}
          <Collapse
            ghost
            items={[
              {
                key: "dsl",
                label: "技术详情：规则 DSL",
                children: (
                  <div className={`${styles.dslContainer} ${styles.dslReadOnly}`}>
                    <pre className={styles.dslReadOnlyPre} aria-label="dsl-readonly">
                      {dslText}
                    </pre>
                  </div>
                ),
              },
            ]}
          />

          {/* ─── 触发历史 ─── */}
          <section className={styles.sectionCard} aria-label="rule-exec-logs">
            <h3 className={styles.sectionTitle}>
              <HistoryOutlined /> 触发历史（最近 50 条）
            </h3>
            {logsQuery.isLoading ? (
              <Spin />
            ) : logsQuery.data && logsQuery.data.length > 0 ? (
              <ExecLogTimeline logs={logsQuery.data} />
            ) : (
              <Empty description="暂无触发记录" />
            )}
          </section>
        </div>

        <aside className={styles.detailSide}>
          {/* ─── 来源追溯（ADR-0004）─── */}
          <section className={styles.sectionCard} aria-label="rule-source">
            <h3 className={styles.sectionTitle}>来源追溯</h3>
            <SourceCitationCard rule={rule} />
          </section>

          {/* ─── 触发聚合统计 ─── */}
          <section className={styles.sectionCard} aria-label="rule-exec-summary">
            <h3 className={styles.sectionTitle}>触发统计</h3>
            {summaryQuery.isLoading ? (
              <Spin />
            ) : summary ? (
              <Space size="large" wrap>
                <Statistic title="累计触发" value={summary.total ?? 0} />
                <Statistic title="命中" value={summary.hit_count ?? 0} />
                <Statistic title="未命中" value={summary.miss_count ?? 0} />
                <Statistic title="异常" value={summary.error_count ?? 0} />
                <Statistic
                  title="平均耗时"
                  value={formatElapsedMs(summary.avg_elapsed_ms)}
                />
              </Space>
            ) : (
              <Text type="secondary">暂无统计</Text>
            )}
          </section>
        </aside>
      </div>
    </div>
  );
}
