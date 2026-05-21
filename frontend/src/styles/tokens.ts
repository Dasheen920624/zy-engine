/**
 * 原色板（hex 字面量），供运行时主题切换使用。
 *
 * <p>本文件不是 CSS 变量，不参与运行时样式渲染。运行时的 `--mk-*` CSS 变量
 * 由 {@link ../styles/tokens.css} 提供（clinical-navy 默认主题）。
 *
 * <p>当用户切换主题（{@link ../theme/ThemeProvider.tsx}）时，{@link ../theme/tokens.ts}
 * 会从本文件读取目标主题的 hex 值，通过 `document.documentElement.style.setProperty`
 * 覆盖 `--mk-*` CSS 变量，从而在不刷新页面的前提下完成换肤。
 *
 * <p>本文件经 ESLint 规则 `no-hardcoded-color` 文件名豁免（filename.includes("tokens.ts")），
 * 是允许出现 hex 的极少数文件之一。所有 hex 改动必须同步检查：
 * <ol>
 *   <li>{@link ../styles/tokens.css} 中对应 `--mk-*` 默认值是否需要同步</li>
 *   <li>{@link ../theme/tokens.ts} 中 `BASE_THEME_VALUES` / `BASE_APP_COLORS` 是否引用</li>
 * </ol>
 *
 * 详细架构见 docs/engineering/AUDIT-20260519 §3.7。
 */
export const COLOR_TOKEN = {
  white: "#ffffff",
  black: "#000000",
  // 默认医蓝主题 clinical-navy（v0.3-final）：与卫健委「健康码 / 医保电子凭证」同色系
  clinicalPrimary: "#1565c0",
  clinicalPrimaryHover: "#1976d2",
  clinicalPrimaryActive: "#0d47a1",
  clinicalPrimarySoft: "#e3f2fd",
  clinicalPrimaryGhost: "#f5faff",
  clinicalMenu: "#0a1e33",
  clinicalMenuHover: "#122d4a",
  clinicalMenuActive: "#061528",
  clinicalMenuText: "#f2f6fb",
  clinicalMenuTextSecondary: "#9bb1c9",
  clinicalBorderInverse: "#1976d2",
  medkernelBluePrimary: "#168bd3",
  medkernelBlueMenu: "#2fa7e8",
  hospitalGreenPrimary: "#147a5c",
  hospitalGreenMenu: "#15906b",
  aiVioletPrimary: "#5b4fd4",
  aiVioletMenu: "#4f46c9",
  success: "#12966e",
  successSoft: "#e4f7ef",
  warning: "#c78200",
  warningSoft: "#fcf3df",
  danger: "#d33f49",
  dangerSoft: "#fdecee",
  textPrimary: "#1f2d3d",
  textSecondary: "#52677a",
  textTertiary: "#7c8b99",
  bgSoft: "#f5faff",
  border: "#d6e3ef",
  borderDivider: "#eaf0f6",
} as const;
