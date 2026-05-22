import { useCallback, useState } from "react";
import { Typography } from "antd";
import type { AssetType, ConfigPackageSummary, PackageStatus, ScopeLevel } from "@/api/types";
import PackageList from "./PackageList";
import PackageDetail from "./PackageDetail";
import styles from "./ConfigPackages.module.css";

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
      <div className={styles.pageHeader}>
        <div>
          <h1 className={styles.pageTitle}>
            配置包中心
          </h1>
          <Text type="secondary" className={styles.pageSubtitle}>
            把本院试点需要的路径、规则、字典和接口配置一次打包、自动校验、审核发布，并保留回滚证据。
          </Text>
        </div>
      </div>

      {/* 左右分栏布局 */}
      <div className={styles.splitLayout}>
        <div className={styles.listPane}>
          <PackageList
            filters={filters}
            onFilterChange={handleFilterChange}
            onFilterReset={handleFilterReset}
            selectedPkg={selectedPkg}
            onSelectPkg={handleSelectPkg}
          />
        </div>
        <div className={styles.detailPane}>
          <PackageDetail
            selectedPkg={selectedPkg}
          />
        </div>
      </div>
    </div>
  );
}
