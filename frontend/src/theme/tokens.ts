export const DEFAULT_THEME_ID = "medkernel-blue";
export const CUSTOM_THEME_ID = "custom";
export const THEME_STORAGE_KEY = "medkernel.theme.v1";

const WHITE = "#ffffff";
const BLACK = "#000000";

const DEFAULT_PRIMARY = "#168bd3";
const DEFAULT_MENU = "#2fa7e8";

export type ThemeId =
  | typeof DEFAULT_THEME_ID
  | "hospital-green"
  | "ai-violet"
  | typeof CUSTOM_THEME_ID;

export type ThemeCssVar =
  | "--mk-brand-primary"
  | "--mk-brand-primary-hover"
  | "--mk-brand-primary-active"
  | "--mk-brand-primary-soft"
  | "--mk-brand-primary-ghost"
  | "--mk-menu-bg"
  | "--mk-menu-bg-hover"
  | "--mk-menu-bg-active"
  | "--mk-menu-text"
  | "--mk-menu-text-secondary"
  | "--mk-info"
  | "--mk-info-soft"
  | "--mk-data-1"
  | "--mk-border-inverse";

export type ThemeOverrides = Partial<Record<ThemeCssVar, string>>;

export const BASE_THEME_VALUES: Record<ThemeCssVar, string> = {
  "--mk-brand-primary": DEFAULT_PRIMARY,
  "--mk-brand-primary-hover": "#0f73b6",
  "--mk-brand-primary-active": "#0b5a8f",
  "--mk-brand-primary-soft": "#e8f6ff",
  "--mk-brand-primary-ghost": "#f5fbff",
  "--mk-menu-bg": DEFAULT_MENU,
  "--mk-menu-bg-hover": "#178fd0",
  "--mk-menu-bg-active": "#0b78bd",
  "--mk-menu-text": WHITE,
  "--mk-menu-text-secondary": "#cce8f9",
  "--mk-info": DEFAULT_PRIMARY,
  "--mk-info-soft": "#e8f6ff",
  "--mk-data-1": DEFAULT_PRIMARY,
  "--mk-border-inverse": "#0b78bd",
};

export const BASE_APP_COLORS = {
  success: "#12966e",
  successSoft: "#e4f7ef",
  warning: "#c78200",
  warningSoft: "#fcf3df",
  danger: "#d33f49",
  dangerSoft: "#fdecee",
  textPrimary: "#1f2d3d",
  textSecondary: "#52677a",
  textTertiary: "#7c8b99",
  bgPage: WHITE,
  bgSoft: "#f5fbff",
  bgPanel: WHITE,
  border: "#d8e8f3",
  borderDivider: "#ebf2f8",
};

export interface CustomThemeSeed {
  primary: string;
  menu: string;
}

export interface ThemeDefinition {
  id: ThemeId;
  label: string;
  description: string;
  overrides: ThemeOverrides;
  custom?: boolean;
}

export const MANAGED_THEME_VARIABLES: ThemeCssVar[] = [
  "--mk-brand-primary",
  "--mk-brand-primary-hover",
  "--mk-brand-primary-active",
  "--mk-brand-primary-soft",
  "--mk-brand-primary-ghost",
  "--mk-menu-bg",
  "--mk-menu-bg-hover",
  "--mk-menu-bg-active",
  "--mk-menu-text",
  "--mk-menu-text-secondary",
  "--mk-info",
  "--mk-info-soft",
  "--mk-data-1",
  "--mk-border-inverse",
];

export const DEFAULT_CUSTOM_THEME: CustomThemeSeed = {
  primary: DEFAULT_PRIMARY,
  menu: DEFAULT_MENU,
};

export const BUILT_IN_THEMES: ThemeDefinition[] = [
  {
    id: DEFAULT_THEME_ID,
    label: "MedKernel 蓝",
    description: "默认医疗治理控制台主题",
    overrides: {},
  },
  {
    id: "hospital-green",
    label: "院区绿",
    description: "偏临床工作站的低刺激绿色主题",
    overrides: buildBrandOverrides("#147a5c", "#15906b"),
  },
  {
    id: "ai-violet",
    label: "AI 紫",
    description: "偏 AI 编排与知识治理的紫色主题",
    overrides: buildBrandOverrides("#5b4fd4", "#4f46c9"),
  },
];

export function normalizeHexColor(value: string, fallback: string): string {
  const trimmed = value.trim();
  if (/^#[0-9a-fA-F]{6}$/.test(trimmed)) {
    return trimmed.toLowerCase();
  }
  if (/^#[0-9a-fA-F]{3}$/.test(trimmed)) {
    const [, r, g, b] = trimmed;
    return `#${r}${r}${g}${g}${b}${b}`.toLowerCase();
  }
  return fallback;
}

export function createCustomTheme(seed: CustomThemeSeed): ThemeDefinition {
  const primary = normalizeHexColor(seed.primary, DEFAULT_PRIMARY);
  const menu = normalizeHexColor(seed.menu, primary);

  return {
    id: CUSTOM_THEME_ID,
    label: "自定义",
    description: "本地自定义主题色，可替换为租户主题包",
    overrides: buildBrandOverrides(primary, menu),
    custom: true,
  };
}

export function resolveTheme(themeId: ThemeId, customTheme: CustomThemeSeed): ThemeDefinition {
  if (themeId === CUSTOM_THEME_ID) {
    return createCustomTheme(customTheme);
  }
  return BUILT_IN_THEMES.find((theme) => theme.id === themeId) ?? BUILT_IN_THEMES[0];
}

export function getThemeValue(theme: ThemeDefinition, variable: ThemeCssVar): string {
  return theme.overrides[variable] ?? BASE_THEME_VALUES[variable];
}

export function getThemeOptions() {
  return [
    ...BUILT_IN_THEMES.map((theme) => ({
      value: theme.id,
      label: theme.label,
    })),
    { value: CUSTOM_THEME_ID, label: "自定义" },
  ];
}

function buildBrandOverrides(primaryInput: string, menuInput: string): ThemeOverrides {
  const primary = normalizeHexColor(primaryInput, DEFAULT_PRIMARY);
  const menu = normalizeHexColor(menuInput, primary);

  return {
    "--mk-brand-primary": primary,
    "--mk-brand-primary-hover": mixHex(primary, BLACK, 0.12),
    "--mk-brand-primary-active": mixHex(primary, BLACK, 0.28),
    "--mk-brand-primary-soft": mixHex(primary, WHITE, 0.9),
    "--mk-brand-primary-ghost": mixHex(primary, WHITE, 0.96),
    "--mk-menu-bg": menu,
    "--mk-menu-bg-hover": mixHex(menu, BLACK, 0.12),
    "--mk-menu-bg-active": mixHex(menu, BLACK, 0.28),
    "--mk-menu-text": WHITE,
    "--mk-menu-text-secondary": mixHex(menu, WHITE, 0.76),
    "--mk-info": primary,
    "--mk-info-soft": mixHex(primary, WHITE, 0.9),
    "--mk-data-1": primary,
    "--mk-border-inverse": mixHex(menu, BLACK, 0.28),
  };
}

function mixHex(baseHex: string, targetHex: string, targetWeight: number): string {
  const base = parseHex(baseHex);
  const target = parseHex(targetHex);
  const weight = clamp(targetWeight, 0, 1);
  const mixed = base.map((channel, index) =>
    Math.round(channel * (1 - weight) + target[index] * weight),
  ) as [number, number, number];
  return toHex(mixed);
}

function parseHex(hex: string): [number, number, number] {
  const normalized = normalizeHexColor(hex, DEFAULT_PRIMARY).slice(1);
  return [
    parseInt(normalized.slice(0, 2), 16),
    parseInt(normalized.slice(2, 4), 16),
    parseInt(normalized.slice(4, 6), 16),
  ];
}

function toHex(rgb: [number, number, number]): string {
  return `#${rgb.map((value) => clamp(value, 0, 255).toString(16).padStart(2, "0")).join("")}`;
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}
