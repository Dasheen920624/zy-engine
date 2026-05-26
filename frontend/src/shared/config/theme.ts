import type { ThemeConfig } from "antd";

/**
 * MedKernel 设计 token（与 docs/CONSTITUTION.md §8 对齐）。
 */
export const theme: ThemeConfig = {
  token: {
    colorPrimary: "#1565c0",
    colorInfo: "#1565c0",
    colorSuccess: "#52c41a",
    colorWarning: "#faad14",
    colorError: "#ff4d4f",
    borderRadius: 6,
    fontSize: 14,
  },
};

export const eyeModeToken: ThemeConfig["token"] = {
  colorBgLayout: "#f5f1e8",
  colorBgContainer: "#fdfaf2",
};
