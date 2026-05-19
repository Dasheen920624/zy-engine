import { useCallback, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Empty,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from "antd";
import {
  CheckCircleOutlined,
  CloudUploadOutlined,
  DiffOutlined,
  DownloadOutlined,
  ExclamationCircleOutlined,
  FileSearchOutlined,
  FilterOutlined,
  ImportOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  WarningOutlined,
} from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import {
  listPackages,
  getPackageDetail,
  reviewPackage,
  publishPackage,
  exportPackage,
} from "../api/configPackage";
import type {
  ApiError,
  AssetType,
  ConfigPackageDetail,
  ConfigPackageReview,
  ConfigPackageSummary,
  PackageStatus,
  ReviewIssue,
  ScopeLevel,
} from "../api/types";

const { Text } = Typography;

// ─── 常量 ──────────────────────────────────────────────────────────────

interface PackageKey {
  code: string;
  version: string;
}

const ASSET_TYPE_OPTIONS: { value: AssetType; label: string }[] = [
  { value: "RULE", label: "规则" },
  { value: "PATH", label: "路径" },
  { value: "GRAPH", label: "图谱" },
  { value: "DIFY", label: "Dify" },
  { value: "TERMINOLOGY", label: "术语" },
  { value: "ADAPTER", label: "适配器" },
  { value: "MIXED", label: "混合" },
];

const STATUS_OPTIONS: { value: PackageStatus; label: string; color: string }[] = [
  { value: "DRAFT", label: "草稿", color: "default" },
  { value: "REVIEWED", label: "已审核", color: "warning" },
  { value: "PUBLISHED", label: "已发布", color: "processing" },
  { value: "SYNCED", label: "已同步", color: "cyan" },
  { value: "ACTIVE", label: "生效中", color: "success" },
  { value: "RETIRED", label: "已下线", color: "error" },
];

const SCOPE_OPTIONS: { value: ScopeLevel; label: string }[] = [
  { value: "PLATFORM", label: "平台级" },
  { value: "GROUP", label: "集团级" },
  { value: "HOSPITAL", label: "院区级" },
  { value: "CAMPUS", label: "院区" },
  { value: "SITE", label: "站点" },
  { value: "DEPARTMENT", label: "科室级" },
];

// ─── 工具函数 ──────────────────────────────────────────────────────────

function statusColor(s: string): string {
  const opt = STATUS_OPTIONS.find((o) => o.value === s);
  return opt?.color ?? "default";
}

function shortHash(hash: string): string {
  if (!hash) return "—";
  const clean = hash.replace("sha256:", "");
  return clean.length > 10 ? `${clean.slice(0, 6)}…` : clean;
}

function formatTime(time?: string): string {
  if (!time) return "—";
  try {
    const d = new Date(time);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  } catch {
    return time;
  }
}

function changeTypeColor(ct?: string): string {
  switch (ct) {
    case "ADDED":
      return "success";
    case "MODIFIED":
      return "processing";
    case "REMOVED":
      return "error";
    case "UNCHANGED":
    default:
      return "default";
  }
}

function changeTypeLabel(ct?: string): string {
  switch (ct) {
    case "ADDED":
      return "新增";
    case "MODIFIED":
      return "修改";
    case "REMOVED":
      return "移除";
    case "UNCHANGED":
    default:
      return "未变";
  }
}

function requirePackageKey(key: PackageKey | null): PackageKey {
  if (!key) {
    throw new Error("selected package is required");
  }
  return key;
}

function diffLineColor(type: "add" | "del" | "neutral"): string {
  switch (type) {
    case "add":
      return "var(--mk-code-add)";
    case "del":
      return "var(--mk-code-del)";
    case "neutral":
    default:
      return "var(--mk-code-text)";
  }
}

function diffLineBackground(type: "add" | "del" | "neutral"): string {
  switch (type) {
    case "add":
      return "var(--mk-code-add-bg)";
    case "del":
      return "var(--mk-code-del-bg)";
    case "neutral":
    default:
      return "transparent";
  }
}

function issueIcon(severity: string) {
  switch (severity) {
    case "ERROR":
      return <ExclamationCircleOutlined style={{ color: "var(--mk-danger)" }} />;
    case "WARNING":
      return <WarningOutlined style={{ color: "var(--mk-warning)" }} />;
    default:
      return <CheckCircleOutlined style={{ color: "var(--mk-success)" }} />;
  }
}

// ─── 子组件 ──────────────────────────────────────────────────────────

/** 筛选工具栏 */
function FilterToolbar({
  filters,
  onChange,
  onReset,
}: {
  filters: FilterState;
  onChange: (patch: Partial<FilterState>) => void;
  onReset: () => void;
}) {
  return (
    <Card size="small" style={{ marginBottom: 16 }}>
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          gap: 12,
          alignItems: "flex-end",
        }}
      >
        <div style={{ minWidth: 140 }}>
          <Text type="secondary" style={{ fontSize: 12, display: "block", marginBottom: 4 }}>
            资产类型
          </Text>
          <Select
            allowClear
            placeholder="全部"
            style={{ width: "100%" }}
            size="small"
            value={filters.assetType}
            onChange={(v) => onChange({ assetType: v })}
            options={ASSET_TYPE_OPTIONS}
          />
        </div>
        <div style={{ minWidth: 140 }}>
          <Text type="secondary" style={{ fontSize: 12, display: "block", marginBottom: 4 }}>
            状态
          </Text>
          <Select
            allowClear
            placeholder="全部"
            style={{ width: "100%" }}
            size="small"
            value={filters.status}
            onChange={(v) => onChange({ status: v })}
            options={STATUS_OPTIONS}
          />
        </div>
        <div style={{ minWidth: 160 }}>
          <Text type="secondary" style={{ fontSize: 12, display: "block", marginBottom: 4 }}>
            组织范围
          </Text>
          <Select
            allowClear
            placeholder="全部"
            style={{ width: "100%" }}
            size="small"
            value={filters.scopeLevel}
            onChange={(v) => onChange({ scopeLevel: v })}
            options={SCOPE_OPTIONS}
          />
        </div>
        <div style={{ minWidth: 200, flex: 1 }}>
          <Text type="secondary" style={{ fontSize: 12, display: "block", marginBottom: 4 }}>
            关键词
          </Text>
          <Input
            size="small"
            placeholder="包编码 / 名称"
            prefix={<SearchOutlined style={{ color: "var(--mk-text-tertiary)" }} />}
            value={filters.keyword}
            onChange={(e) => onChange({ keyword: e.target.value })}
            allowClear
          />
        </div>
        <Space>
          <Button size="small" icon={<FilterOutlined />} onClick={onReset}>
            重置
          </Button>
        </Space>
      </div>
    </Card>
  );
}

/** 校验检查列表 */
function ReviewCheckList({ issues }: { issues: ReviewIssue[] }) {
  if (!issues || issues.length === 0) {
    return (
      <div style={{ padding: "8px 0" }}>
        <Space>
          <CheckCircleOutlined style={{ color: "var(--mk-success)" }} />
          <Text style={{ color: "var(--mk-success)" }}>全部检查通过</Text>
        </Space>
      </div>
    );
  }

  return (
    <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
      {issues.map((issue, i) => (
        <li
          key={i}
          style={{
            padding: "8px 0",
            borderBottom:
              i < issues.length - 1 ? "var(--mk-border-width) solid var(--mk-border-divider)" : "none",
            display: "flex",
            gap: 8,
            alignItems: "flex-start",
            fontSize: 13,
          }}
        >
          <span style={{ flexShrink: 0, marginTop: 2 }}>{issueIcon(issue.severity)}</span>
          <div>
            <strong>{issue.field}</strong>
            <br />
            <Text type="secondary" style={{ fontSize: 12 }}>
              {issue.message}
            </Text>
          </div>
        </li>
      ))}
    </ul>
  );
}

/** Manifest 资产清单表格 */
function ManifestTable({ manifest }: { manifest?: Record<string, unknown> }) {
  const items = useMemo(() => {
    if (!manifest?.items || !Array.isArray(manifest.items)) return [];
    return manifest.items as Array<{
      asset_code: string;
      asset_type: string;
      version?: string;
      change_type?: string;
    }>;
  }, [manifest]);

  if (items.length === 0) {
    return <Empty description="无资产清单" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  const cols: ColumnsType<(typeof items)[0]> = [
    { title: "资产编码", dataIndex: "asset_code", key: "code", render: (v: string) => <code style={{ fontFamily: "var(--mk-font-mono)", fontSize: 12 }}>{v}</code> },
    { title: "类型", dataIndex: "asset_type", key: "type", width: 80 },
    { title: "版本", dataIndex: "version", key: "version", width: 60 },
    {
      title: "变更",
      dataIndex: "change_type",
      key: "change",
      width: 80,
      render: (v: string) => <Tag color={changeTypeColor(v)}>{changeTypeLabel(v)}</Tag>,
    },
  ];

  return <Table dataSource={items} columns={cols} rowKey="asset_code" size="small" pagination={false} />;
}

/** Diff 展示（简化版） */
function DiffView({ diff }: { diff?: Record<string, unknown> }) {
  if (!diff || Object.keys(diff).length === 0) {
    return <Text type="secondary">无 diff 数据</Text>;
  }

  // 简单的 JSON 格式化展示
  const lines: Array<{ text: string; type: "add" | "del" | "neutral" }> = [];
  const rules = diff.rules as Array<Record<string, unknown>> | undefined;
  if (rules) {
    for (const rule of rules) {
      if (rule.change === "REMOVED") {
        lines.push({ text: `- ${rule.rule_code}`, type: "del" });
      } else if (rule.version_change) {
        lines.push({ text: `  ${rule.rule_code}: ${rule.version_change}`, type: "neutral" });
        const added = rule.added_fields as string[] | undefined;
        if (added && added.length > 0) {
          for (const f of added) {
            lines.push({ text: `+   ${f}`, type: "add" });
          }
        }
      }
    }
  }

  if (lines.length === 0) {
    return (
      <pre
        style={{
          background: "var(--mk-bg-muted)",
          padding: 12,
          borderRadius: 4,
          fontSize: 12,
          fontFamily: "var(--mk-font-mono)",
          maxHeight: 300,
          overflow: "auto",
          margin: 0,
        }}
      >
        {JSON.stringify(diff, null, 2)}
      </pre>
    );
  }

  return (
    <pre
      style={{
        background: "var(--mk-bg-inverse)",
        padding: 12,
        borderRadius: 4,
        fontSize: 12,
        fontFamily: "var(--mk-font-mono)",
        maxHeight: 300,
        overflow: "auto",
        margin: 0,
      }}
    >
      {lines.map((line, i) => (
        <div
          key={i}
          style={{
            color: diffLineColor(line.type),
            background: diffLineBackground(line.type),
            padding: "1px 4px",
            borderRadius: 2,
          }}
        >
          {line.text}
        </div>
      ))}
    </pre>
  );
}

// ─── 主页面 ──────────────────────────────────────────────────────────

interface FilterState {
  assetType?: AssetType;
  status?: PackageStatus;
  scopeLevel?: ScopeLevel;
  keyword?: string;
}

export default function ConfigPackages() {
  const queryClient = useQueryClient();

  // 筛选状态
  const [filters, setFilters] = useState<FilterState>({});
  // 选中的包
  const [selectedPkg, setSelectedPkg] = useState<ConfigPackageSummary | null>(null);
  // 发布确认弹窗
  const [publishModalOpen, setPublishModalOpen] = useState(false);
  const [approvedBy, setApprovedBy] = useState("");
  const [approvedNote, setApprovedNote] = useState("");
  const selectedPackageKey = useMemo<PackageKey | null>(
    () =>
      selectedPkg
        ? {
            code: selectedPkg.package_code,
            version: selectedPkg.package_version,
          }
        : null,
    [selectedPkg],
  );

  // ─── 数据查询 ────────────────────────────────────────────────

  const {
    data: packages = [],
    isLoading,
    error: listError,
  } = useQuery<ConfigPackageSummary[], ApiError>({
    queryKey: ["config-packages", filters],
    queryFn: () =>
      listPackages({
        assetType: filters.assetType,
        status: filters.status,
        scopeLevel: filters.scopeLevel,
      }),
  });

  // 客户端关键词过滤
  const filteredPackages = useMemo(() => {
    if (!filters.keyword) return packages;
    const kw = filters.keyword.toLowerCase();
    return packages.filter(
      (p) =>
        p.package_code.toLowerCase().includes(kw) ||
        p.package_version.toLowerCase().includes(kw),
    );
  }, [packages, filters.keyword]);

  // 详情查询（当有选中包时）
  const {
    data: pkgDetail,
    isLoading: detailLoading,
  } = useQuery<ConfigPackageDetail, ApiError>({
    queryKey: ["config-package-detail", selectedPackageKey?.code, selectedPackageKey?.version],
    queryFn: () => {
      const key = requirePackageKey(selectedPackageKey);
      return getPackageDetail(key.code, key.version);
    },
    enabled: Boolean(selectedPackageKey),
  });

  // Review 查询
  const {
    data: pkgReview,
    isLoading: reviewLoading,
  } = useQuery<ConfigPackageReview, ApiError>({
    queryKey: ["config-package-review", selectedPackageKey?.code, selectedPackageKey?.version],
    queryFn: () => {
      const key = requirePackageKey(selectedPackageKey);
      return reviewPackage(key.code, key.version);
    },
    enabled: Boolean(selectedPackageKey),
  });

  // ─── 操作 mutation ──────────────────────────────────────────

  const reviewMutation = useMutation<ConfigPackageReview, ApiError>({
    mutationFn: () => {
      const key = requirePackageKey(selectedPackageKey);
      return reviewPackage(key.code, key.version);
    },
    onSuccess: (data) => {
      message.success(`校验完成 · ${data.ready_to_publish ? "可发布" : "存在问题"}`);
      queryClient.invalidateQueries({ queryKey: ["config-package-review"] });
      queryClient.invalidateQueries({ queryKey: ["config-packages"] });
    },
    onError: (err) => message.error(`校验失败: ${err.message}`),
  });

  const publishMut = useMutation<ConfigPackageDetail, ApiError, { approved_by: string; approved_note?: string }>({
    mutationFn: (req) => {
      const key = requirePackageKey(selectedPackageKey);
      return publishPackage(key.code, key.version, req);
    },
    onSuccess: () => {
      message.success("发布成功");
      setPublishModalOpen(false);
      setApprovedBy("");
      setApprovedNote("");
      queryClient.invalidateQueries({ queryKey: ["config-packages"] });
      queryClient.invalidateQueries({ queryKey: ["config-package-detail"] });
      queryClient.invalidateQueries({ queryKey: ["config-package-review"] });
    },
    onError: (err) => message.error(`发布失败: ${err.message}`),
  });

  const exportMut = useMutation({
    mutationFn: () => {
      const key = requirePackageKey(selectedPackageKey);
      return exportPackage(key.code, key.version);
    },
    onSuccess: (data) => {
      const key = requirePackageKey(selectedPackageKey);
      // 下载为 JSON
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${key.code}_${key.version}_snapshot.json`;
      a.click();
      URL.revokeObjectURL(url);
      message.success("导出成功");
    },
    onError: (err) => message.error(`导出失败: ${err.message}`),
  });

  // ─── 事件处理 ────────────────────────────────────────────────

  const handleFilterChange = useCallback(
    (patch: Partial<FilterState>) => setFilters((prev) => ({ ...prev, ...patch })),
    [],
  );

  const handleFilterReset = useCallback(() => setFilters({}), []);

  const handleSelectPkg = useCallback((pkg: ConfigPackageSummary) => {
    setSelectedPkg(pkg);
  }, []);

  const handlePublish = useCallback(() => {
    if (!approvedBy.trim()) {
      message.warning("请输入审批人");
      return;
    }
    publishMut.mutate({ approved_by: approvedBy.trim(), approved_note: approvedNote.trim() || undefined });
  }, [approvedBy, approvedNote, publishMut]);

  // ─── 表格列定义 ──────────────────────────────────────────────

  const columns: ColumnsType<ConfigPackageSummary> = [
    {
      title: "包编码",
      dataIndex: "package_code",
      key: "code",
      render: (v: string) => <Text strong style={{ fontFamily: "var(--mk-font-mono)", fontSize: 13 }}>{v}</Text>,
    },
    {
      title: "版本",
      dataIndex: "package_version",
      key: "version",
      width: 120,
      render: (v: string) => <code style={{ fontFamily: "var(--mk-font-mono)", fontSize: 12 }}>{v}</code>,
    },
    {
      title: "类型",
      dataIndex: "asset_type",
      key: "type",
      width: 80,
      render: (v: string) => <Tag color="blue">{v}</Tag>,
    },
    {
      title: "组织范围",
      dataIndex: "scope_reference",
      key: "scope",
      width: 180,
      render: (v: string, r) => v || `${r.scope_level} · ${r.scope_code}`,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (v: string) => <Tag color={statusColor(v)}>{v}</Tag>,
    },
    {
      title: "哈希",
      dataIndex: "content_hash",
      key: "hash",
      width: 100,
      render: (v: string) => (
        <Tooltip title={v}>
          <code style={{ fontFamily: "var(--mk-font-mono)", fontSize: 11 }}>{shortHash(v)}</code>
        </Tooltip>
      ),
    },
    {
      title: "更新时间",
      dataIndex: "reviewed_time",
      key: "time",
      width: 150,
      render: (_: string, r) => (
        <Text type="secondary" style={{ fontSize: 12 }}>
          {formatTime(r.reviewed_time || r.published_time || r.created_time)}
        </Text>
      ),
    },
    {
      title: "操作",
      key: "action",
      width: 160,
      render: (_: unknown, r: ConfigPackageSummary) => (
        <Space size="small">
          <Button size="small" icon={<FileSearchOutlined />} onClick={() => handleSelectPkg(r)}>
            详情
          </Button>
          {(r.status === "DRAFT" || r.status === "REVIEWED") && (
            <Button size="small" type="primary" icon={<CloudUploadOutlined />} onClick={() => handleSelectPkg(r)}>
              发布
            </Button>
          )}
        </Space>
      ),
    },
  ];

  // ─── 渲染 ──────────────────────────────────────────────────

  return (
    <div>
      {/* 页面头部 */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          marginBottom: 16,
        }}
      >
        <div>
          <h1 style={{ margin: 0, fontSize: 22, fontWeight: 600 }}>
            配置包中心
          </h1>
          <Text type="secondary" style={{ fontSize: 13 }}>
            路径 / 规则 / 图谱 / Dify / 字典 / 适配器 统一包生命周期 · DRAFT → REVIEWED → PUBLISHED → SYNCED → ACTIVE → RETIRED
          </Text>
        </div>
        <Space>
          <Button icon={<ImportOutlined />} disabled>
            导入包
          </Button>
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ["config-packages"] })}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} disabled>
            新建包
          </Button>
        </Space>
      </div>

      {/* 筛选工具栏 */}
      <FilterToolbar filters={filters} onChange={handleFilterChange} onReset={handleFilterReset} />

      {/* 列表错误 */}
      {listError && (
        <Alert
          type="error"
          showIcon
          closable
          style={{ marginBottom: 16 }}
          message={`加载失败: ${listError.code}`}
          description={listError.message}
        />
      )}

      {/* 配置包列表 */}
      <Card
        title={
          <span>
            配置包列表 · <strong>{filteredPackages.length}</strong> 条
          </span>
        }
        size="small"
        style={{ marginBottom: 16 }}
      >
        <Table<ConfigPackageSummary>
          dataSource={filteredPackages}
          columns={columns}
          rowKey={(r) => `${r.package_code}@${r.package_version}`}
          loading={isLoading}
          size="small"
          pagination={false}
          onRow={(r) => ({
            onClick: () => handleSelectPkg(r),
            style: {
              cursor: "pointer",
              background:
                selectedPkg?.package_code === r.package_code &&
                selectedPkg?.package_version === r.package_version
                  ? "var(--mk-brand-primary-soft)"
                  : undefined,
            },
          })}
        />
      </Card>

      {/* 详情面板 */}
      {selectedPkg && (
        <Card
          title={
            <span>
              {selectedPkg.package_code} @ {selectedPkg.package_version} ·{" "}
              <Tag color={statusColor(selectedPkg.status)}>{selectedPkg.status}</Tag>
            </span>
          }
          size="small"
          extra={
            <Space>
              <Button
                size="small"
                icon={<ReloadOutlined />}
                loading={reviewMutation.isPending}
                onClick={() => reviewMutation.mutate()}
              >
                重新校验
              </Button>
              <Button
                size="small"
                icon={<DownloadOutlined />}
                loading={exportMut.isPending}
                onClick={() => exportMut.mutate()}
              >
                导出 snapshot
              </Button>
              {pkgReview?.ready_to_publish && selectedPkg.status !== "PUBLISHED" && selectedPkg.status !== "ACTIVE" && (
                <Button
                  size="small"
                  type="primary"
                  icon={<CloudUploadOutlined />}
                  onClick={() => setPublishModalOpen(true)}
                >
                  发布
                </Button>
              )}
            </Space>
          }
        >
          {detailLoading || reviewLoading ? (
            <div style={{ textAlign: "center", padding: 40, color: "var(--mk-text-tertiary)" }}>
              加载中...
            </div>
          ) : (
            <Row gutter={16}>
              {/* 左栏：基础信息 + manifest */}
              <Col xs={24} lg={12}>
                <h4 style={{ marginBottom: 12, fontWeight: 500 }}>基础信息</h4>
                <Descriptions
                  column={1}
                  size="small"
                  labelStyle={{ color: "var(--mk-text-tertiary)", width: 140 }}
                  style={{ marginBottom: 16 }}
                >
                  <Descriptions.Item label="包编码">
                    <code style={{ fontFamily: "var(--mk-font-mono)" }}>{selectedPkg.package_code}</code>
                  </Descriptions.Item>
                  <Descriptions.Item label="版本">
                    <code style={{ fontFamily: "var(--mk-font-mono)" }}>{selectedPkg.package_version}</code>
                  </Descriptions.Item>
                  <Descriptions.Item label="资产类型">
                    <Tag color="blue">{selectedPkg.asset_type}</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="组织范围">{selectedPkg.scope_reference || `${selectedPkg.scope_level} · ${selectedPkg.scope_code}`}</Descriptions.Item>
                  <Descriptions.Item label="基础版本">
                    <code style={{ fontFamily: "var(--mk-font-mono)" }}>{selectedPkg.base_version || "—"}</code>
                  </Descriptions.Item>
                  <Descriptions.Item label="内容哈希">
                    <Tooltip title={selectedPkg.content_hash}>
                      <code style={{ fontFamily: "var(--mk-font-mono)", fontSize: 11 }}>{shortHash(selectedPkg.content_hash)}</code>
                    </Tooltip>
                  </Descriptions.Item>
                  <Descriptions.Item label="创建人">{selectedPkg.created_by || "—"} · {formatTime(selectedPkg.created_time)}</Descriptions.Item>
                  <Descriptions.Item label="审核人">{selectedPkg.reviewed_by || "—"} · {formatTime(selectedPkg.reviewed_time)}</Descriptions.Item>
                  <Descriptions.Item label="审批人">
                    {selectedPkg.approved_by ? (
                      <span>{selectedPkg.approved_by} · {formatTime(selectedPkg.published_time)}</span>
                    ) : (
                      <Text type="secondary">— 未发布</Text>
                    )}
                  </Descriptions.Item>
                </Descriptions>

                <Divider style={{ margin: "12px 0" }} />

                <h4 style={{ marginBottom: 12, fontWeight: 500 }}>资产清单</h4>
                <ManifestTable manifest={pkgDetail?.manifest} />
              </Col>

              {/* 右栏：校验检查 */}
              <Col xs={24} lg={12}>
                <h4 style={{ marginBottom: 12, fontWeight: 500 }}>校验检查</h4>
                <ReviewCheckList issues={pkgReview?.issues || []} />

                {/* 来源完整性 */}
                {pkgReview?.source_review && pkgReview.source_review.enabled && (
                  <div style={{ marginTop: 12 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>来源完整性：</Text>
                    {pkgReview.source_review.missing_count === 0 &&
                    pkgReview.source_review.expired_count === 0 &&
                    pkgReview.source_review.unreviewed_count === 0 ? (
                      <Tag color="success" style={{ marginLeft: 8 }}>100%</Tag>
                    ) : (
                      <Tag color="error" style={{ marginLeft: 8 }}>
                        缺 {pkgReview.source_review.missing_count} 条
                      </Tag>
                    )}
                  </div>
                )}

                {/* 校验摘要 */}
                {pkgReview?.summary && (
                  <div style={{ marginTop: 12 }}>
                    <h4 style={{ marginBottom: 8, fontWeight: 500 }}>校验摘要</h4>
                    <Descriptions
                      column={1}
                      size="small"
                      labelStyle={{ color: "var(--mk-text-tertiary)", width: 140 }}
                    >
                      <Descriptions.Item label="资产数量">{pkgReview.summary.asset_count}</Descriptions.Item>
                      <Descriptions.Item label="完整快照">{pkgReview.summary.full_snapshot_present ? <Tag color="success">✓</Tag> : <Tag color="error">✗</Tag>}</Descriptions.Item>
                      <Descriptions.Item label="差异">{pkgReview.summary.diff_present ? <Tag color="success">✓</Tag> : <Tag color="default">无</Tag>}</Descriptions.Item>
                      <Descriptions.Item label="范围存在">{pkgReview.summary.scope_exists ? <Tag color="success">✓</Tag> : <Tag color="error">✗</Tag>}</Descriptions.Item>
                    </Descriptions>
                  </div>
                )}

                {/* ready_to_publish 状态 */}
                <div
                  style={{
                    marginTop: 16,
                    padding: "8px 12px",
                    background: pkgReview?.ready_to_publish
                      ? "var(--mk-success-soft)"
                      : "var(--mk-danger-soft)",
                    borderRadius: 4,
                    border: `1px solid ${
                      pkgReview?.ready_to_publish
                        ? "var(--mk-success-border)"
                        : "var(--mk-danger-border)"
                    }`,
                  }}
                >
                  <Space>
                    {pkgReview?.ready_to_publish ? (
                      <CheckCircleOutlined style={{ color: "var(--mk-success)" }} />
                    ) : (
                      <ExclamationCircleOutlined style={{ color: "var(--mk-danger)" }} />
                    )}
                    <Text
                      strong
                      style={{
                        color: pkgReview?.ready_to_publish
                          ? "var(--mk-success)"
                          : "var(--mk-danger)",
                      }}
                    >
                      {pkgReview?.ready_to_publish ? "可发布" : "不可发布"}
                    </Text>
                  </Space>
                </div>
              </Col>

              {/* diff 全宽 */}
              {pkgDetail?.diff && Object.keys(pkgDetail.diff).length > 0 && (
                <Col span={24}>
                  <Divider style={{ margin: "16px 0 12px" }} />
                  <h4 style={{ marginBottom: 12, fontWeight: 500 }}>
                    <DiffOutlined style={{ marginRight: 8 }} />
                    版本差异（基础 {selectedPkg.base_version || "—"} → 目标 {selectedPkg.package_version}）
                  </h4>
                  <DiffView diff={pkgDetail.diff} />
                </Col>
              )}
            </Row>
          )}
        </Card>
      )}

      {/* 空状态 */}
      {!selectedPkg && !isLoading && (
        <Card size="small">
          <Empty description="选择配置包查看详情" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        </Card>
      )}

      {/* 发布确认弹窗 */}
      <Modal
        title={`发布配置包: ${selectedPkg?.package_code}@${selectedPkg?.package_version}`}
        open={publishModalOpen}
        onCancel={() => {
          setPublishModalOpen(false);
          setApprovedBy("");
          setApprovedNote("");
        }}
        onOk={handlePublish}
        confirmLoading={publishMut.isPending}
        okText="确认发布"
        okButtonProps={{ danger: true }}
      >
        <div style={{ marginBottom: 16 }}>
          <Text type="secondary">
            发布后将写入 ENGINE_AUDIT_LOG（PKG/PUBLISH）。请确认以下信息：
          </Text>
        </div>
        <div style={{ marginBottom: 12 }}>
          <Text type="secondary" style={{ fontSize: 12, display: "block", marginBottom: 4 }}>
            审批人 (approved_by) <span style={{ color: "var(--mk-danger)" }}>*</span>
          </Text>
          <Input
            placeholder="必填 · 例：CIO_LI"
            value={approvedBy}
            onChange={(e) => setApprovedBy(e.target.value)}
          />
        </div>
        <div>
          <Text type="secondary" style={{ fontSize: 12, display: "block", marginBottom: 4 }}>
            审批备注
          </Text>
          <Input
            placeholder="例：医学审核已通过，建议本周二上线"
            value={approvedNote}
            onChange={(e) => setApprovedNote(e.target.value)}
          />
        </div>
      </Modal>
    </div>
  );
}
