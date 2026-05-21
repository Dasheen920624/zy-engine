/**
 * 路径版本对比页（路由 /pathway/templates/:code/diff?from=&to=）。
 *
 * 顶栏：选 from / to 两个已发布版本
 * 主区：调 /api/pathways/{code}/diff，渲染 nodes / edges / tasks 三段式增删改
 * 侧栏：两个版本的 JSON 并排只读视图
 *
 * 国情合规：来源追溯、中文文案。
 */

import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { Alert, Button, Empty, Result, Select, Spin, Typography } from "antd";
import { ArrowLeftOutlined, DiffOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import CodeMirror from "@uiw/react-codemirror";
import { json } from "@codemirror/lang-json";
import { oneDark } from "@codemirror/theme-one-dark";
import { diffPathway, getPathway } from "../../api/pathway";
import type { PathwayDiffResult } from "../../api/pathway";
import { describeDiffItem, diffTotals, pickDiffSection } from "./helpers/pathwayDiff";
import { stringifyJson } from "./helpers/pathwayFormatters";
import styles from "./styles.module.css";

const { Title, Text } = Typography;

const SECTION_LABELS: Record<"nodes" | "edges" | "tasks", string> = {
  nodes: "节点",
  edges: "连接",
  tasks: "任务",
};

interface DiffSectionViewProps {
  kind: "nodes" | "edges" | "tasks";
  diff: PathwayDiffResult | undefined;
}

function DiffSectionView({ kind, diff }: DiffSectionViewProps) {
  const section = pickDiffSection(diff, kind);
  const label = SECTION_LABELS[kind];
  const total = section.added.length + section.removed.length + section.modified.length;
  if (total === 0) {
    return (
      <section className={styles.sectionCard} aria-label={`diff-${kind}-empty`}>
        <h3 className={styles.sectionTitle}>{label}：无变更</h3>
      </section>
    );
  }
  return (
    <section className={styles.sectionCard} aria-label={`diff-${kind}`}>
      <h3 className={styles.sectionTitle}>
        {label} · {total} 项变更
      </h3>
      <div className={styles.diffSectionList}>
        {section.added.map((item, idx) => (
          <div key={`add-${idx}`} className={`${styles.diffItem} ${styles.diffItemAdded}`}>
            <span className={styles.diffItemBadge}>+ 新增</span>
            <span>{describeDiffItem(item)}</span>
          </div>
        ))}
        {section.removed.map((item, idx) => (
          <div key={`rm-${idx}`} className={`${styles.diffItem} ${styles.diffItemRemoved}`}>
            <span className={styles.diffItemBadge}>− 移除</span>
            <span>{describeDiffItem(item)}</span>
          </div>
        ))}
        {section.modified.map((item, idx) => (
          <div key={`mod-${idx}`} className={`${styles.diffItem} ${styles.diffItemModified}`}>
            <span className={styles.diffItemBadge}>± 修改</span>
            <span>{describeDiffItem(item)}</span>
          </div>
        ))}
      </div>
    </section>
  );
}

export default function PathwayDiff() {
  const params = useParams<{ code: string }>();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const code = params.code ?? "";

  const baseQuery = useQuery({
    queryKey: ["pathway-base", code],
    queryFn: () => getPathway(code),
    enabled: Boolean(code),
  });

  const publishedVersions = useMemo(
    () => baseQuery.data?.published_versions ?? [],
    [baseQuery.data],
  );

  const urlFrom = searchParams.get("from") ?? "";
  const urlTo = searchParams.get("to") ?? "";

  const [fromVersion, setFromVersion] = useState<string>(urlFrom);
  const [toVersion, setToVersion] = useState<string>(urlTo);

  // 自动选 from / to：from = 倒数第二个已发布版本，to = 最新版本
  useEffect(() => {
    if (publishedVersions.length === 0) return;
    const sorted = [...publishedVersions].sort((a, b) =>
      a.localeCompare(b, undefined, { numeric: true }),
    );
    const last = sorted[sorted.length - 1];
    const prev = sorted.length >= 2 ? sorted[sorted.length - 2] : last;
    if (!fromVersion && !urlFrom) setFromVersion(prev);
    if (!toVersion && !urlTo) setToVersion(last);
  }, [publishedVersions, fromVersion, toVersion, urlFrom, urlTo]);

  const applyVersionPair = (next: { from?: string; to?: string }) => {
    const nextSearch = new URLSearchParams(searchParams);
    if (next.from !== undefined) {
      if (next.from) nextSearch.set("from", next.from);
      else nextSearch.delete("from");
      setFromVersion(next.from);
    }
    if (next.to !== undefined) {
      if (next.to) nextSearch.set("to", next.to);
      else nextSearch.delete("to");
      setToVersion(next.to);
    }
    setSearchParams(nextSearch, { replace: true });
  };

  const canDiff = Boolean(fromVersion && toVersion && fromVersion !== toVersion);

  const diffQuery = useQuery({
    queryKey: ["pathway-diff", code, fromVersion, toVersion],
    queryFn: () => diffPathway(code, fromVersion, toVersion),
    enabled: canDiff,
  });

  const fromConfigQuery = useQuery({
    queryKey: ["pathway-config-from", code, fromVersion],
    queryFn: () => getPathway(code, fromVersion),
    enabled: Boolean(fromVersion),
  });

  const toConfigQuery = useQuery({
    queryKey: ["pathway-config-to", code, toVersion],
    queryFn: () => getPathway(code, toVersion),
    enabled: Boolean(toVersion),
  });

  if (baseQuery.isLoading) {
    return (
      <div className={styles.page}>
        <Spin tip="加载路径基线..." />
      </div>
    );
  }

  if (baseQuery.isError || !baseQuery.data) {
    return (
      <Result
        status="404"
        title="路径未找到"
        subTitle={`无法加载 ${code}`}
        extra={
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate("/pathway/templates")}>
            返回路径库
          </Button>
        }
      />
    );
  }

  const totals = diffTotals(diffQuery.data);
  const versionOptions = publishedVersions.map((v) => ({ value: v, label: `v${v}` }));

  return (
    <div className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <Button
            type="link"
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate(`/pathway/templates/${encodeURIComponent(code)}`)}
          >
            返回路径详情
          </Button>
          <Title level={3} className={styles.pageTitle}>
            <DiffOutlined /> 版本对比 · {code}
          </Title>
          <Text type="secondary">
            选择两个已发布版本，查看节点 / 连接 / 任务 的增删改差异。
          </Text>
        </div>
      </header>

      <div className={styles.diffToolbar} role="toolbar">
        <span className={styles.toolbarLabel}>From</span>
        <Select
          className={styles.filterSelect}
          value={fromVersion || undefined}
          options={versionOptions}
          placeholder="选择基线版本"
          onChange={(v) => applyVersionPair({ from: v })}
          aria-label="pathway-diff-from"
        />
        <span className={styles.toolbarLabel}>To</span>
        <Select
          className={styles.filterSelect}
          value={toVersion || undefined}
          options={versionOptions}
          placeholder="选择目标版本"
          onChange={(v) => applyVersionPair({ to: v })}
          aria-label="pathway-diff-to"
        />
        {!canDiff && (
          <Text type="warning">请选择两个不同的版本</Text>
        )}
      </div>

      {publishedVersions.length < 2 && (
        <Alert
          showIcon
          type="info"
          message="该路径已发布版本不足 2 个，暂无法对比。请先发布至少 2 个版本。"
        />
      )}

      {canDiff && diffQuery.isLoading && (
        <div className={styles.page}>
          <Spin tip="生成版本差异中..." />
        </div>
      )}

      {canDiff && diffQuery.isError && (
        <Alert showIcon type="error" message="无法生成版本差异" description={(diffQuery.error as Error)?.message} />
      )}

      {canDiff && diffQuery.data && (
        <>
          <div className={styles.diffSummary} role="status">
            <span className={styles.diffSummaryItem}>
              <span className={styles.diffSummaryLabel}>节点变更：</span>
              <span className={styles.diffSummaryValue}>{totals.nodes}</span>
            </span>
            <span className={styles.diffSummaryItem}>
              <span className={styles.diffSummaryLabel}>连接变更：</span>
              <span className={styles.diffSummaryValue}>{totals.edges}</span>
            </span>
            <span className={styles.diffSummaryItem}>
              <span className={styles.diffSummaryLabel}>任务变更：</span>
              <span className={styles.diffSummaryValue}>{totals.tasks}</span>
            </span>
            <span className={styles.diffSummaryItem}>
              <span className={styles.diffSummaryLabel}>From：</span>
              <span className={styles.diffSummaryValue}>v{fromVersion}</span>
            </span>
            <span className={styles.diffSummaryItem}>
              <span className={styles.diffSummaryLabel}>To：</span>
              <span className={styles.diffSummaryValue}>v{toVersion}</span>
            </span>
          </div>

          <div className={styles.diffStack}>
            <DiffSectionView kind="nodes" diff={diffQuery.data} />
            <DiffSectionView kind="edges" diff={diffQuery.data} />
            <DiffSectionView kind="tasks" diff={diffQuery.data} />
          </div>

          <section className={styles.sectionCard} aria-label="diff-json-side-by-side">
            <h3 className={styles.sectionTitle}>JSON 并排对比</h3>
            <div className={styles.diffPanel}>
              <div className={styles.diffColumn}>
                <span className={styles.diffColumnTitle}>v{fromVersion}</span>
                {fromConfigQuery.isLoading ? (
                  <Spin />
                ) : fromConfigQuery.data?.published_config ? (
                  <div className={styles.jsonContainer} aria-label="diff-from-json">
                    <CodeMirror
                      value={stringifyJson(fromConfigQuery.data.published_config)}
                      extensions={[json()]}
                      theme={oneDark}
                      editable={false}
                      basicSetup={{ lineNumbers: true, foldGutter: true, highlightActiveLine: false }}
                    />
                  </div>
                ) : (
                  <Empty description="该版本无已发布配置" />
                )}
              </div>
              <div className={styles.diffColumn}>
                <span className={styles.diffColumnTitle}>v{toVersion}</span>
                {toConfigQuery.isLoading ? (
                  <Spin />
                ) : toConfigQuery.data?.published_config ? (
                  <div className={styles.jsonContainer} aria-label="diff-to-json">
                    <CodeMirror
                      value={stringifyJson(toConfigQuery.data.published_config)}
                      extensions={[json()]}
                      theme={oneDark}
                      editable={false}
                      basicSetup={{ lineNumbers: true, foldGutter: true, highlightActiveLine: false }}
                    />
                  </div>
                ) : (
                  <Empty description="该版本无已发布配置" />
                )}
              </div>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
