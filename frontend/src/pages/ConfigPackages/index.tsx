import { useCallback, useState } from "react";
import { Typography } from "antd";
import type { AssetType, ConfigPackageSummary, PackageStatus, ScopeLevel } from "@/api/types";
import PackageList from "./PackageList";
import PackageDetail from "./PackageDetail";

const { Text } = Typography;

export interface FilterState {
  assetType?: AssetType;
  status?: PackageStatus;
  scopeLevel?: ScopeLevel;
  keyword?: string;
}

export default function ConfigPackages() {
  const [filters, setFilters] = useState<FilterState>({});
  const [selectedPkg, setSelectedPkg] = useState<ConfigPackageSummary | null>(null);

  const handleFilterChange = useCallback(
    (patch: Partial<FilterState>) => setFilters((prev) => ({ ...prev, ...patch })),
    [],
  );

  const handleFilterReset = useCallback(() => setFilters({}), []);

  const handleSelectPkg = useCallback((pkg: ConfigPackageSummary) => {
    setSelectedPkg(pkg);
  }, []);

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
      </div>

      {/* 左右分栏布局 */}
      <div style={{ display: "flex", gap: 16, alignItems: "flex-start" }}>
        <div style={{ width: "45%", minWidth: 0 }}>
          <PackageList
            filters={filters}
            onFilterChange={handleFilterChange}
            onFilterReset={handleFilterReset}
            selectedPkg={selectedPkg}
            onSelectPkg={handleSelectPkg}
          />
        </div>
        <div style={{ width: "55%", minWidth: 0 }}>
          <PackageDetail
            selectedPkg={selectedPkg}
          />
        </div>
      </div>
    </div>
  );
}
