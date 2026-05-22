import { useMemo, useState } from "react";
import {
  Alert,
  Button,
  Col,
  Collapse,
  Input,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from "antd";
import type { ColumnsType, TablePaginationConfig } from "antd/es/table";
import { DownloadOutlined, ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import {
  ACTION_TYPES,
  ACTION_TYPE_LABELS,
  ENGINE_TYPES,
  ENGINE_TYPE_LABELS,
  listAuditLogs,
  summarizeAuditLogs,
  type ActionType,
  type AuditLogEntry,
  type AuditLogFilters,
  type AuditLogSummary,
  type EngineType,
} from "../../../api/auditLog";
import SignatureVerifyBanner from "./SignatureVerifyBanner";
import AuditLogDetail from "./AuditLogDetail";
import styles from "./styles.module.css";

const { Title, Paragraph } = Typography;

const LIMIT_OPTIONS = [20, 50, 100, 200];

const DEFAULT_FILTERS: AuditLogFilters = { limit: 20 };

/**
 * /admin/audit 审计日志查询主页（PR-FINAL-09，等保 2.0 三级合规）。
 *
 * 结构：
 *  - 顶部 SignatureVerifyBanner（3 个表的链验签状态 + 立即校验）
 *  - 8 filter + limit 工具栏（搜索 / 重置 / 导出 / 刷新）
 *  - 3 聚合卡（总数 / engine_type 分布 / action_type 分布）
 *  - 主列表（红色高亮验签失败行，点击打开 AuditLogDetail Drawer）
 *
 * 不做的事（等保要求）：
 *  - 无删除 / 修改按钮（审计不可改）
 *  - 不显示原始 patient_id（前端 4+4 脱敏）
 */
export default function AuditLogList() {
  const [draft, setDraft] = useState<AuditLogFilters>(DEFAULT_FILTERS);
  const [applied, setApplied] = useState<AuditLogFilters>(DEFAULT_FILTERS);
  const [drawerEntry, setDrawerEntry] = useState<AuditLogEntry | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const listQuery = useQuery({
    queryKey: ["audit-logs", "list", applied],
    queryFn: () => listAuditLogs(applied),
  });

  const summaryQuery = useQuery({
    queryKey: ["audit-logs", "summary", applied],
    queryFn: () => summarizeAuditLogs(applied),
  });

  const applyFilters = () => setApplied({ ...draft });

  const resetFilters = () => {
    setDraft(DEFAULT_FILTERS);
    setApplied(DEFAULT_FILTERS);
  };

  const refresh = () => {
    listQuery.refetch();
    summaryQuery.refetch();
  };

  const exportCsv = () => {
    const rows = listQuery.data ?? [];
    if (rows.length === 0) return;
    downloadCsv(buildCsv(rows), `audit-logs-${Date.now()}.csv`);
  };

  const columns = useMemo<ColumnsType<AuditLogEntry>>(
    () => [
      {
        title: "时间",
        dataIndex: "created_time",
        key: "created_time",
        width: 170,
        render: (v?: string) => v ?? "—",
      },
      {
        title: "引擎",
        dataIndex: "engine_type",
        key: "engine_type",
        width: 110,
        render: (v?: string) => (v ? ENGINE_TYPE_LABELS[v as EngineType] ?? v : "—"),
      },
      {
        title: "操作",
        dataIndex: "action_type",
        key: "action_type",
        width: 100,
        render: (v?: string) => (v ? ACTION_TYPE_LABELS[v as ActionType] ?? v : "—"),
      },
      {
        title: "目标",
        key: "target",
        width: 200,
        render: (_v, record) => (
          <span>
            {record.target_type ?? "?"}
            {record.target_name ? ` · ${record.target_name}` : ""}
          </span>
        ),
      },
      {
        title: "患者 ID",
        dataIndex: "patient_id",
        key: "patient_id",
        width: 140,
        render: (v?: string) => mask4_4(v) ?? "—",
      },
      {
        title: "操作人",
        key: "operator",
        width: 140,
        render: (_v, record) => record.operator_name ?? record.operator_id ?? "—",
      },
      {
        title: "验签",
        dataIndex: "signature_valid",
        key: "signature_valid",
        width: 100,
        render: (v?: boolean) => renderSignatureCell(v),
      },
    ],
    [],
  );

  const pagination: TablePaginationConfig = {
    pageSize: 20,
    showSizeChanger: true,
    showTotal: (total) => `共 ${total} 条`,
  };

  return (
    <div>
      <div className={`mk-page-header ${styles.pageHeader}`}>
        <Title level={3} className={styles.pageTitle}>
          审计日志
        </Title>
        <Paragraph type="secondary" className={styles.pageHint}>
          AUDIT-001 / SEC-010：ENGINE_AUDIT_LOG 防篡改链 · 等保 2.0 三级要求 · 不可改不可删
        </Paragraph>
      </div>

      <SignatureVerifyBanner />

      <div className={styles.toolbar} aria-label="audit-filter-toolbar">
        <div className={styles.toolbarItem}>
          <label className={styles.toolbarItemLabel}>引擎类型</label>
          <Select<string>
            allowClear
            placeholder="全部"
            value={draft.engine_type}
            options={ENGINE_TYPES.map((t) => ({ value: t, label: ENGINE_TYPE_LABELS[t] }))}
            onChange={(v) => setDraft({ ...draft, engine_type: v })}
          />
        </div>
        <div className={styles.toolbarItem}>
          <label className={styles.toolbarItemLabel}>操作类型</label>
          <Select<string>
            allowClear
            placeholder="全部"
            value={draft.action_type}
            options={ACTION_TYPES.map((t) => ({ value: t, label: ACTION_TYPE_LABELS[t] }))}
            onChange={(v) => setDraft({ ...draft, action_type: v })}
          />
        </div>
        <div className={styles.toolbarItem}>
          <label className={styles.toolbarItemLabel}>操作人</label>
          <Input
            allowClear
            placeholder="操作人"
            value={draft.operator_id ?? ""}
            onChange={(e) => setDraft({ ...draft, operator_id: e.target.value })}
          />
        </div>
        <div className={styles.toolbarActions}>
          <Button type="primary" icon={<SearchOutlined />} onClick={applyFilters}>
            查询
          </Button>
          <Button onClick={resetFilters}>重置</Button>
        </div>
      </div>

      <Collapse
        ghost
        items={[
          {
            key: "advanced",
            label: "高级筛选",
            children: (
              <div className={styles.toolbar} aria-label="audit-advanced-filter">
                <div className={styles.toolbarItem}>
                  <label className={styles.toolbarItemLabel} htmlFor="filter-trace">
                    Trace ID
                  </label>
                  <Input
                    id="filter-trace"
                    allowClear
                    placeholder="trace_id 精确匹配"
                    value={draft.trace_id ?? ""}
                    onChange={(e) => setDraft({ ...draft, trace_id: e.target.value })}
                  />
                </div>
                <div className={styles.toolbarItem}>
                  <label className={styles.toolbarItemLabel}>目标类型</label>
                  <Input
                    allowClear
                    placeholder="如 PathwayTemplate"
                    value={draft.target_type ?? ""}
                    onChange={(e) => setDraft({ ...draft, target_type: e.target.value })}
                  />
                </div>
                <div className={styles.toolbarItem}>
                  <label className={styles.toolbarItemLabel}>目标编码</label>
                  <Input
                    allowClear
                    placeholder="target_code"
                    value={draft.target_code ?? ""}
                    onChange={(e) => setDraft({ ...draft, target_code: e.target.value })}
                  />
                </div>
                <div className={styles.toolbarItem}>
                  <label className={styles.toolbarItemLabel}>患者 ID</label>
                  <Input
                    allowClear
                    placeholder="患者 ID"
                    value={draft.patient_id ?? ""}
                    onChange={(e) => setDraft({ ...draft, patient_id: e.target.value })}
                  />
                </div>
                <div className={styles.toolbarItem}>
                  <label className={styles.toolbarItemLabel}>就诊 ID</label>
                  <Input
                    allowClear
                    placeholder="encounter_id"
                    value={draft.encounter_id ?? ""}
                    onChange={(e) => setDraft({ ...draft, encounter_id: e.target.value })}
                  />
                </div>
                <div className={styles.toolbarItem}>
                  <label className={styles.toolbarItemLabel}>最大条数</label>
                  <Select<number>
                    value={Number(draft.limit ?? 20)}
                    options={LIMIT_OPTIONS.map((n) => ({ value: n, label: String(n) }))}
                    onChange={(v) => setDraft({ ...draft, limit: v })}
                  />
                </div>
              </div>
            ),
          },
        ]}
      />

      <Row gutter={[16, 16]} className={styles.summaryRow}>
        <Col xs={24} md={8}>
          <SummaryTotalCard summary={summaryQuery.data} loading={summaryQuery.isLoading} />
        </Col>
        <Col xs={24} md={8}>
          <SummaryListCard
            title="按引擎类型"
            loading={summaryQuery.isLoading}
            entries={summaryQuery.data?.by_engine_type?.map((e) => ({
              label: ENGINE_TYPE_LABELS[e.engine_type as EngineType] ?? e.engine_type,
              value: e.count,
            }))}
          />
        </Col>
        <Col xs={24} md={8}>
          <SummaryListCard
            title="按操作类型"
            loading={summaryQuery.isLoading}
            entries={summaryQuery.data?.by_action_type?.map((e) => ({
              label: ACTION_TYPE_LABELS[e.action_type as ActionType] ?? e.action_type,
              value: e.count,
            }))}
          />
        </Col>
      </Row>

      <div className={styles.listSection} aria-label="audit-list-section">
        <div className={styles.listToolbar}>
          <div className={styles.listToolbarHint}>
            {listQuery.data
              ? `当前 ${listQuery.data.length} 条（受 limit 限制；进一步过滤请用上方表单）`
              : "—"}
          </div>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refresh}>
              刷新
            </Button>
            <Button
              icon={<DownloadOutlined />}
              onClick={exportCsv}
              disabled={!listQuery.data || listQuery.data.length === 0}
            >
              导出 CSV
            </Button>
          </Space>
        </div>
        {listQuery.isError ? (
          <Alert
            type="error"
            showIcon
            message="无法加载审计日志"
            description={(listQuery.error as Error)?.message}
          />
        ) : listQuery.isLoading ? (
          <Spin tip="加载中..." />
        ) : (
          <Table<AuditLogEntry>
            rowKey={(row) => String(row.id ?? `${row.trace_id ?? ""}-${row.created_time ?? ""}`)}
            columns={columns}
            dataSource={listQuery.data ?? []}
            pagination={pagination}
            size="small"
            rowClassName={(row) => (row.signature_valid === false ? styles.brokenRow : "")}
            onRow={(row) => ({
              onClick: () => {
                setDrawerEntry(row);
                setDrawerOpen(true);
              },
            })}
            aria-label="audit-table"
          />
        )}
      </div>

      <AuditLogDetail
        entry={drawerEntry}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
      />
    </div>
  );
}

function SummaryTotalCard({
  summary,
  loading,
}: {
  summary?: AuditLogSummary;
  loading: boolean;
}) {
  return (
    <div className={styles.summaryCard}>
      <div className={styles.summaryCardTitle}>记录总数</div>
      <div className={styles.summaryCardValue}>
        {loading ? <Spin size="small" /> : (summary?.total ?? 0)}
      </div>
    </div>
  );
}

function SummaryListCard({
  title,
  entries,
  loading,
}: {
  title: string;
  entries?: Array<{ label: string; value: number }>;
  loading: boolean;
}) {
  return (
    <div className={styles.summaryCard}>
      <div className={styles.summaryCardTitle}>{title}</div>
      {loading ? (
        <Spin size="small" />
      ) : !entries || entries.length === 0 ? (
        <span className={styles.summaryListLabel}>暂无数据</span>
      ) : (
        <ul className={styles.summaryList}>
          {entries.slice(0, 6).map((e) => (
            <li key={e.label} className={styles.summaryListItem}>
              <span className={styles.summaryListLabel}>{e.label}</span>
              <span className={styles.summaryListValue}>{e.value}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function renderSignatureCell(valid?: boolean) {
  if (valid === true) {
    return (
      <Tag color="success" className={styles.signatureValidTag}>
        通过
      </Tag>
    );
  }
  if (valid === false) {
    return (
      <Tag color="error" className={styles.signatureInvalidTag}>
        失败
      </Tag>
    );
  }
  return <Tag>未校验</Tag>;
}

function mask4_4(value?: string): string | undefined {
  if (!value) return value;
  if (value.length <= 8) return value;
  return `${value.slice(0, 4)}****${value.slice(-4)}`;
}

function buildCsv(rows: AuditLogEntry[]): string {
  const header = [
    "created_time",
    "engine_type",
    "action_type",
    "target_type",
    "target_code",
    "patient_id_masked",
    "operator_id",
    "trace_id",
    "signature_valid",
  ];
  const lines = [header.join(",")];
  for (const row of rows) {
    lines.push(
      [
        csvCell(row.created_time),
        csvCell(row.engine_type),
        csvCell(row.action_type),
        csvCell(row.target_type),
        csvCell(row.target_code),
        csvCell(mask4_4(row.patient_id)),
        csvCell(row.operator_id),
        csvCell(row.trace_id),
        csvCell(row.signature_valid === undefined ? "" : String(row.signature_valid)),
      ].join(","),
    );
  }
  return lines.join("\n");
}

function csvCell(value: unknown): string {
  if (value === undefined || value === null) return "";
  const text = String(value);
  if (text.includes(",") || text.includes("\"") || text.includes("\n")) {
    return `"${text.replace(/"/g, '""')}"`;
  }
  return text;
}

function downloadCsv(content: string, filename: string) {
  // 添加 UTF-8 BOM，让 Excel 正确识别中文
  const blob = new Blob(["﻿" + content], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
