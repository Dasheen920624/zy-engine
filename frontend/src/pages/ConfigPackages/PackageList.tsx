import { useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  Button,
  Card,
  Input,
  Select,
  Space,
  Table,
  Typography,
} from "antd";
import {
  ExportOutlined,
  FileSearchOutlined,
  FilterOutlined,
  ImportOutlined,
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import { useNavigate } from "react-router-dom";
import { listPackages, exportPackage } from "@/api/configPackage";
import type {
  ApiError,
  AssetType,
  ConfigPackageSummary,
  PackageStatus,
  ScopeLevel,
} from "@/api/types";
import { StatusBadge } from "@/components/StatusBadge";
import { OrgContextSelector } from "../../components";
import type { FilterState } from "./index";
import styles from "./PackageList.module.css";

const { Text } = Typography;

const ASSET_TYPE_OPTIONS: { value: AssetType; label: string }[] = [
  { value: "RULE", label: "规则配置" },
  { value: "PATH", label: "路径配置" },
  { value: "GRAPH", label: "医学图谱" },
  { value: "DIFY", label: "AI 工作流" },
  { value: "TERMINOLOGY", label: "字典映射" },
  { value: "ADAPTER", label: "系统接入" },
  { value: "MIXED", label: "混合" },
];

const STATUS_OPTIONS: { value: PackageStatus; label: string }[] = [
  { value: "DRAFT", label: "草稿" },
  { value: "REVIEWED", label: "已审核" },
  { value: "PUBLISHED", label: "已发布" },
  { value: "SYNCED", label: "已同步" },
  { value: "ACTIVE", label: "生效中" },
  { value: "RETIRED", label: "已下线" },
];

const SCOPE_OPTIONS: { value: ScopeLevel; label: string }[] = [
  { value: "PLATFORM", label: "平台级" },
  { value: "GROUP", label: "集团级" },
  { value: "HOSPITAL", label: "院区级" },
  { value: "CAMPUS", label: "院区" },
  { value: "SITE", label: "站点" },
  { value: "DEPARTMENT", label: "科室级" },
];

function formatTime(time?: string): string {
  if (!time) return "—";
  try {
    const d = new Date(time);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  } catch {
    return time;
  }
}

interface PackageListProps {
  filters: FilterState;
  onFilterChange: (patch: Partial<FilterState>) => void;
  onFilterReset: () => void;
  selectedPkg: ConfigPackageSummary | null;
  onSelectPkg: (pkg: ConfigPackageSummary) => void;
}

export default function PackageList({
  filters,
  onFilterChange,
  onFilterReset,
  selectedPkg,
  onSelectPkg,
}: PackageListProps) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

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

  const filteredPackages = useMemo(() => {
    if (!filters.keyword) return packages;
    const kw = filters.keyword.toLowerCase();
    return packages.filter(
      (p) =>
        p.package_code.toLowerCase().includes(kw) ||
        p.package_version.toLowerCase().includes(kw),
    );
  }, [packages, filters.keyword]);

  const exportMut = useMutation({
    mutationFn: (pkg: ConfigPackageSummary) =>
      exportPackage(pkg.package_code, pkg.package_version),
    onSuccess: (data, pkg) => {
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${pkg.package_code}_${pkg.package_version}_snapshot.json`;
      a.click();
      URL.revokeObjectURL(url);
    },
  });

  const columns: ColumnsType<ConfigPackageSummary> = [
    {
      title: "包编码",
      dataIndex: "package_code",
      key: "code",
      render: (v: string) => (
        <Text strong className={styles.monoText}>{v}</Text>
      ),
    },
    {
      title: "版本",
      dataIndex: "package_version",
      key: "version",
      width: 100,
      render: (v: string) => (
        <code className={styles.monoCode}>{v}</code>
      ),
    },
    {
      title: "类型",
      dataIndex: "asset_type",
      key: "type",
      width: 70,
      render: (v: string) => <span className={styles.textSmall}>{v}</span>,
    },
    {
      title: "环境",
      key: "environment",
      width: 70,
      render: (_: unknown, r: ConfigPackageSummary) => {
        const isProd = r.status === "ACTIVE" || r.status === "PUBLISHED" || r.status === "SYNCED";
        return (
          <span className={`${styles.environmentText} ${isProd ? styles.environmentProd : styles.environmentTest}`}>
            {isProd ? "生产" : "测试"}
          </span>
        );
      },
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (v: string) => <StatusBadge status={v.toLowerCase() as never} size="sm" />,
    },
    {
      title: "更新时间",
      dataIndex: "reviewed_time",
      key: "time",
      width: 130,
      render: (_: string, r) => (
        <Text type="secondary" className={styles.textSmall}>
          {formatTime(r.reviewed_time || r.published_time || r.created_time)}
        </Text>
      ),
    },
    {
      title: "操作",
      key: "action",
      width: 180,
      render: (_: unknown, r: ConfigPackageSummary) => (
        <Space size={4}>
          <Button size="small" icon={<FileSearchOutlined />} onClick={() => onSelectPkg(r)}>
            详情
          </Button>
          <Button
            size="small"
            icon={<ExportOutlined />}
            loading={exportMut.isPending}
            onClick={(e) => {
              e.stopPropagation();
              exportMut.mutate(r);
            }}
          >
            导出
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* 筛选工具栏 */}
      <Card size="small" className={styles.filterCard}>
        <div className={styles.filterBar}>
          <div className={styles.filterField}>
            <Text type="secondary" className={styles.filterLabel}>
              配置内容
            </Text>
            <Select
              allowClear
              placeholder="全部"
              className={styles.filterSelect}
              size="small"
              value={filters.assetType}
              onChange={(v) => onFilterChange({ assetType: v })}
              options={ASSET_TYPE_OPTIONS}
            />
          </div>
          <div className={styles.filterField}>
            <Text type="secondary" className={styles.filterLabel}>
              状态
            </Text>
            <Select
              allowClear
              placeholder="全部"
              className={styles.filterSelect}
              size="small"
              value={filters.status}
              onChange={(v) => onFilterChange({ status: v })}
              options={STATUS_OPTIONS}
            />
          </div>
          <div className={styles.filterField}>
            <Text type="secondary" className={styles.filterLabel}>
              组织范围
            </Text>
            <Select
              allowClear
              placeholder="全部"
              className={styles.filterSelect}
              size="small"
              value={filters.scopeLevel}
              onChange={(v) => onFilterChange({ scopeLevel: v })}
              options={SCOPE_OPTIONS}
            />
          </div>
          <div className={styles.filterFieldWide}>
            <Text type="secondary" className={styles.filterLabel}>
              关键词
            </Text>
            <Input
              size="small"
              placeholder="搜索配置包"
              prefix={<SearchOutlined className={styles.iconMuted} />}
              value={filters.keyword}
              onChange={(e) => onFilterChange({ keyword: e.target.value })}
              allowClear
            />
          </div>
          <Space>
            <OrgContextSelector />
            <Button size="small" icon={<FilterOutlined />} onClick={onFilterReset}>
              重置
            </Button>
            <Button
              size="small"
              icon={<ReloadOutlined />}
              onClick={() => queryClient.invalidateQueries({ queryKey: ["config-packages"] })}
            >
              刷新
            </Button>
            <Button
              size="small"
              icon={<ImportOutlined />}
              onClick={() => navigate("/config/packages/import")}
            >
              导入配置
            </Button>
          </Space>
        </div>
      </Card>

      {/* 列表错误 */}
      {listError && (
        <Alert
          type="error"
          showIcon
          closable
          className={styles.marginBottom12}
          message={`加载失败: ${listError.code}`}
          description={listError.message}
        />
      )}

      {/* 配置包列表 */}
      <Card
        title={
          <span>
            待处理配置包 · <strong>{filteredPackages.length}</strong> 条
          </span>
        }
        size="small"
      >
        <Table<ConfigPackageSummary>
          dataSource={filteredPackages}
          columns={columns}
          rowKey={(r) => `${r.package_code}@${r.package_version}`}
          loading={isLoading}
          size="small"
          pagination={false}
          onRow={(r) => ({
            onClick: () => onSelectPkg(r),
          })}
          rowClassName={(r) =>
            selectedPkg?.package_code === r.package_code &&
            selectedPkg?.package_version === r.package_version
              ? `${styles.packageRow} ${styles.packageRowSelected}`
              : styles.packageRow
          }
        />
      </Card>
    </div>
  );
}
