/**
 * 路径模板详情页（路由 /pathway/templates/:code）—— PATHWAY-ENGINE-COMPLETE 重写。
 *
 * 主要改动相对旧版（96 行 inline-style 实现）：
 *  - 零 inline style：全 CSS Modules + var(--mk-*) token
 *  - 版本时间轴（draft + 已发布 + 激活）
 *  - 引用警告卡（reference_warnings 缺失项可见，ADR-0004）
 *  - 实例统计 + 变异统计（调 /pathway-instances/summary + /pathway-variations/summary）
 *  - 草稿 / 已发布 JSON：纯 pre 只读视图，避免前端构建依赖漂移
 *  - 来源追溯 <SourceInfo>（已有，按 reference_sources 渲染）
 *  - 编辑 / 对比 / 删除 按钮明显化
 */

import { useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Button, Collapse, Empty, Result, Space, Spin, Statistic, Tabs, Tag, Typography, message, Popconfirm } from "antd";
import {
  ArrowLeftOutlined,
  DeleteOutlined,
  DiffOutlined,
  EditOutlined,
} from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { SourceInfo } from "../../components";
import {
  deletePathway,
  getPathway,
  nodeCompletionSummary,
  summarizePatientPathwayInstances,
  summarizeVariations,
} from "../../api/pathway";
import { formatPercent, stringifyJson } from "./helpers/pathwayFormatters";
import PathwayTimeline from "./components/PathwayTimeline";
import ReferenceWarnings from "./components/ReferenceWarnings";
import styles from "./styles.module.css";

const { Title, Text } = Typography;

export default function PathwayDetail() {
  const params = useParams<{ code: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const code = params.code ?? "";

  const [selectedVersion, setSelectedVersion] = useState<string | null>(null);

  const detailQuery = useQuery({
    queryKey: ["pathway", code, selectedVersion],
    queryFn: () => getPathway(code, selectedVersion ?? undefined),
    enabled: Boolean(code),
  });

  const instanceSummaryQuery = useQuery({
    queryKey: ["pathway-instance-summary", code],
    queryFn: () => summarizePatientPathwayInstances({ pathway_code: code }),
    enabled: Boolean(code),
  });

  const nodeCompletionQuery = useQuery({
    queryKey: ["pathway-node-completion", code],
    queryFn: () => nodeCompletionSummary({ pathway_code: code }),
    enabled: Boolean(code),
  });

  const variationSummaryQuery = useQuery({
    queryKey: ["pathway-variation-summary", code],
    queryFn: () => summarizeVariations({ pathway_code: code }),
    enabled: Boolean(code),
  });

  const deleteMutation = useMutation({
    mutationFn: () => deletePathway(code),
    onSuccess: () => {
      message.success("路径已删除");
      queryClient.invalidateQueries({ queryKey: ["pathways"] });
      navigate("/pathway/templates");
    },
    onError: (err: Error) => message.error(`删除失败：${err.message}`),
  });

  const detail = detailQuery.data;

  const tabs = useMemo(() => {
    if (!detail) return [];
    return [
      {
        key: "draft",
        label: "草稿配置",
        children: detail.draft_config ? (
          <div className={styles.jsonContainer} aria-label="draft-config-json">
            <pre className={styles.jsonReadOnly}>{stringifyJson(detail.draft_config)}</pre>
          </div>
        ) : (
          <div className={styles.jsonReadOnlyEmpty}>无草稿</div>
        ),
      },
      {
        key: "published",
        label: detail.active_published_version
          ? `已发布 · v${detail.active_published_version}`
          : "已发布版本",
        children: detail.published_config ? (
          <div className={styles.jsonContainer} aria-label="published-config-json">
            <pre className={styles.jsonReadOnly}>{stringifyJson(detail.published_config)}</pre>
          </div>
        ) : (
          <div className={styles.jsonReadOnlyEmpty}>暂无已发布版本</div>
        ),
      },
    ];
  }, [detail]);

  if (detailQuery.isLoading) {
    return (
      <div className={styles.page}>
        <Spin tip="加载路径详情中..." />
      </div>
    );
  }

  if (detailQuery.isError || !detail) {
    return (
      <Result
        status="404"
        title="路径未找到"
        subTitle={`未找到路径 ${code}，可能已被删除或无权访问。`}
        extra={
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate("/pathway/templates")}>
            返回路径库
          </Button>
        }
      />
    );
  }

  const instanceSummary = instanceSummaryQuery.data;
  const nodeCompletion = nodeCompletionQuery.data;
  const variationSummary = variationSummaryQuery.data;
  const completionRate =
    nodeCompletion && nodeCompletion.total > 0
      ? (nodeCompletion.by_node ?? []).reduce((acc, n) => acc + n.completion_rate, 0) /
        ((nodeCompletion.by_node ?? []).length || 1)
      : null;

  return (
    <div className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <Button
            type="link"
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate("/pathway/templates")}
          >
            返回路径库
          </Button>
          <Title level={3} className={styles.pageTitle}>
            {detail.pathway_code}
          </Title>
          <Space size="middle" wrap>
            {detail.active_published_version && (
              <Tag color="success">激活 v{detail.active_published_version}</Tag>
            )}
            {detail.draft_status === "DRAFT" && <Tag color="warning">含未发布草稿</Tag>}
            {selectedVersion && selectedVersion !== detail.active_published_version && (
              <Tag color="processing">查看 v{selectedVersion}</Tag>
            )}
            <SourceInfo
              variant="inline"
              source={{
                documentName: detail.pathway_code,
                documentId: detail.pathway_code,
              }}
              review={{ status: (detail.reference_warnings?.length ?? 0) > 0 ? "missing" : "reviewed" }}
              version={detail.active_published_version ?? undefined}
            />
          </Space>
        </div>
        <div className={styles.headerActions}>
          <Button
            icon={<EditOutlined />}
            onClick={() => navigate(`/pathway/templates/${encodeURIComponent(code)}/edit`)}
          >
            编辑草稿
          </Button>
          <Button
            icon={<DiffOutlined />}
            onClick={() => navigate(`/pathway/templates/${encodeURIComponent(code)}/diff`)}
            disabled={(detail.published_versions?.length ?? 0) < 2}
          >
            版本对比
          </Button>
          <Popconfirm
            title="确认删除该路径？"
            description="删除后无法恢复；建议先回滚或导出。"
            okText="删除"
            okButtonProps={{ danger: true }}
            cancelText="取消"
            onConfirm={() => deleteMutation.mutate()}
          >
            <Button danger icon={<DeleteOutlined />} loading={deleteMutation.isPending}>
              删除
            </Button>
          </Popconfirm>
        </div>
      </header>

      <ReferenceWarnings warnings={detail.reference_warnings ?? []} />

      <div className={styles.detailGrid}>
        <div className={styles.detailMain}>
          {/* ─── 元信息 ─── */}
          <section className={styles.sectionCard} aria-label="pathway-meta">
            <h3 className={styles.sectionTitle}>元信息</h3>
            <dl className={styles.metaList}>
              <dt className={styles.metaLabel}>路径编码</dt>
              <dd className={styles.metaValue}>{detail.pathway_code}</dd>
              <dt className={styles.metaLabel}>草稿状态</dt>
              <dd className={styles.metaValue}>
                {detail.draft_status === "DRAFT" ? "有未发布草稿" : "—"}
              </dd>
              <dt className={styles.metaLabel}>已发布版本</dt>
              <dd className={styles.metaValue}>
                {detail.published_versions?.length
                  ? detail.published_versions.map((v) => <Tag key={v}>v{v}</Tag>)
                  : "—"}
              </dd>
              <dt className={styles.metaLabel}>当前激活版本</dt>
              <dd className={styles.metaValue}>{detail.active_published_version || "—"}</dd>
              <dt className={styles.metaLabel}>查看版本</dt>
              <dd className={styles.metaValue}>{detail.selected_version || selectedVersion || "—"}</dd>
              <dt className={styles.metaLabel}>引用来源</dt>
              <dd className={styles.metaValue}>
                {detail.reference_sources && detail.reference_sources.length > 0
                  ? `${detail.reference_sources.length} 项`
                  : "—"}
              </dd>
            </dl>
          </section>

          {/* ─── 路径配置（技术详情，默认折叠）─── */}
          <Collapse
            ghost
            items={[
              {
                key: "config",
                label: "技术详情：路径配置",
                children: (
                  <Tabs items={tabs} />
                ),
              },
            ]}
          />
        </div>

        <aside className={styles.detailSide}>
          {/* ─── 版本时间轴 ─── */}
          <section className={styles.sectionCard} aria-label="pathway-versions">
            <h3 className={styles.sectionTitle}>版本时间轴</h3>
            <PathwayTimeline
              draftStatus={detail.draft_status}
              publishedVersions={detail.published_versions ?? []}
              activeVersion={detail.active_published_version}
              selectedVersion={detail.selected_version ?? selectedVersion}
              onPickVersion={setSelectedVersion}
              onDiffVersion={(v) => navigate(`/pathway/templates/${encodeURIComponent(code)}/diff?to=${encodeURIComponent(v)}`)}
            />
          </section>

          {/* ─── 实例统计 ─── */}
          <section className={styles.sectionCard} aria-label="pathway-instance-summary">
            <h3 className={styles.sectionTitle}>实例统计</h3>
            {instanceSummaryQuery.isLoading ? (
              <Spin />
            ) : instanceSummary ? (
              <div className={styles.statRow}>
                <Statistic title="总数" value={instanceSummary.total ?? 0} />
                <Statistic title="进行中" value={instanceSummary.active ?? 0} />
                <Statistic title="完成" value={instanceSummary.completed ?? 0} />
                <Statistic title="退出" value={instanceSummary.exited ?? 0} />
                {completionRate !== null && (
                  <Statistic title="平均完成率" value={formatPercent(completionRate)} />
                )}
              </div>
            ) : (
              <Empty description="暂无统计" />
            )}
          </section>

          {/* ─── 变异统计 ─── */}
          <section className={styles.sectionCard} aria-label="pathway-variation-summary">
            <h3 className={styles.sectionTitle}>变异统计</h3>
            {variationSummaryQuery.isLoading ? (
              <Spin />
            ) : variationSummary ? (
              <div className={styles.statRow}>
                <Statistic title="累计变异" value={variationSummary.total ?? 0} />
                {variationSummary.by_type?.map((row) => (
                  <Statistic key={row.variation_type} title={row.variation_type} value={row.count} />
                ))}
              </div>
            ) : (
              <Text type="secondary">暂无变异</Text>
            )}
          </section>
        </aside>
      </div>
    </div>
  );
}
