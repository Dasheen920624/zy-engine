/**
 * AntD ConfigProvider 主题桥接。
 *
 * <p>本文件用 `var(--mk-*)` 引用 {@link ./tokens.css} 中的 CSS 变量，把它们映射到 AntD
 * 的设计 token 键（colorPrimary / colorSuccess / ...）。这样 AntD 组件的色彩跟随
 * MedKernel token 一起变化，同时不在本文件中保留任何硬编码 hex（forbidden-patterns §1.1）。
 *
 * <p>详见 docs/engineering/AUDIT-20260519 §3.7 与 docs/engineering/forbidden-patterns.md §1.3。
 */
export const antTheme = {
  token: {
    colorPrimary: "var(--mk-brand-primary)",
    colorSuccess: "var(--mk-success)",
    colorWarning: "var(--mk-warning)",
    colorError: "var(--mk-danger)",
    colorInfo: "var(--mk-info)",
    colorBgContainer: "var(--mk-bg-panel)",
    colorBgLayout: "var(--mk-bg-soft)",
    colorBgElevated: "var(--mk-bg-panel)",
    colorBorder: "var(--mk-border)",
    colorBorderSecondary: "var(--mk-border-divider)",
    colorText: "var(--mk-text-primary)",
    colorTextSecondary: "var(--mk-text-secondary)",
    colorTextTertiary: "var(--mk-text-tertiary)",
    colorTextQuaternary: "var(--mk-text-tertiary)",
    borderRadius: 6,
    borderRadiusSM: 4,
    borderRadiusLG: 8,
    fontFamily: '-apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Source Han Sans CN", sans-serif',
    fontSize: 14,
    fontSizeSM: 13,
    fontSizeLG: 16,
    fontSizeXL: 18,
    controlHeight: 32,
    controlHeightSM: 28,
    controlHeightLG: 40,
    wireframe: false,
  },
  components: {
    Layout: {
      siderBg: "var(--mk-menu-bg)",
      headerBg: "var(--mk-bg-panel)",
      headerPadding: "0 24px",
    },
    Menu: {
      darkItemBg: "var(--mk-menu-bg)",
      darkItemHoverBg: "var(--mk-menu-bg-hover)",
      darkItemSelectedBg: "var(--mk-menu-bg-active)",
      darkItemColor: "var(--mk-menu-text)",
      darkItemHoverColor: "var(--mk-menu-text)",
    },
  },
};
