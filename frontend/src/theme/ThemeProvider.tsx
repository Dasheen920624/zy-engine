import {
  useEffect,
  useLayoutEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { ConfigProvider, type ThemeConfig } from "antd";
import zhCN from "antd/locale/zh_CN";
import { antTheme } from "../styles/theme-tokens";
import {
  BASE_APP_COLORS,
  BUILT_IN_THEMES,
  CUSTOM_THEME_ID,
  DEFAULT_CUSTOM_THEME,
  DEFAULT_THEME_ID,
  MANAGED_THEME_VARIABLES,
  THEME_STORAGE_KEY,
  getThemeOptions,
  getThemeValue,
  normalizeHexColor,
  resolveTheme,
  type CustomThemeSeed,
  type ThemeDefinition,
  type ThemeId,
} from "./tokens";
import { ThemeContext, type ThemeContextValue } from "./themeContext";

interface StoredThemeState {
  themeId?: ThemeId;
  customTheme?: Partial<CustomThemeSeed>;
}

interface ResolvedThemeState {
  themeId: ThemeId;
  customTheme: CustomThemeSeed;
}

export function MedKernelThemeProvider({ children }: { children: ReactNode }) {
  const stored = useMemo(readStoredThemeState, []);
  const [themeId, setThemeIdState] = useState<ThemeId>(stored.themeId);
  const [customTheme, setCustomTheme] = useState<CustomThemeSeed>(stored.customTheme);
  const themeOptions = useMemo(getThemeOptions, []);
  const theme = useMemo(() => resolveTheme(themeId, customTheme), [themeId, customTheme]);
  const antdTheme = useMemo(() => buildAntdTheme(theme), [theme]);

  useLayoutEffect(() => {
    applyTheme(theme);
  }, [theme]);

  useEffect(() => {
    persistThemeState({ themeId, customTheme });
  }, [themeId, customTheme]);

  const value = useMemo<ThemeContextValue>(
    () => ({
      themeId,
      theme,
      customTheme,
      themeOptions,
      setThemeId: setThemeIdState,
      updateCustomTheme: (patch) => {
        setCustomTheme((current) => normalizeCustomTheme({ ...current, ...patch }));
        setThemeIdState(CUSTOM_THEME_ID);
      },
      resetCustomTheme: () => {
        setCustomTheme(DEFAULT_CUSTOM_THEME);
        setThemeIdState(DEFAULT_THEME_ID);
      },
    }),
    [customTheme, theme, themeId, themeOptions],
  );

  return (
    <ThemeContext.Provider value={value}>
      <ConfigProvider locale={zhCN} theme={antdTheme}>
        {children}
      </ConfigProvider>
    </ThemeContext.Provider>
  );
}

function readStoredThemeState(): ResolvedThemeState {
  if (typeof window === "undefined") {
    return { themeId: DEFAULT_THEME_ID, customTheme: DEFAULT_CUSTOM_THEME };
  }

  try {
    const raw = window.localStorage.getItem(THEME_STORAGE_KEY);
    if (!raw) {
      return { themeId: DEFAULT_THEME_ID, customTheme: DEFAULT_CUSTOM_THEME };
    }
    const parsed = JSON.parse(raw) as StoredThemeState;
    return {
      themeId: normalizeThemeId(parsed.themeId),
      customTheme: normalizeCustomTheme(parsed.customTheme),
    };
  } catch {
    return { themeId: DEFAULT_THEME_ID, customTheme: DEFAULT_CUSTOM_THEME };
  }
}

function persistThemeState(state: ResolvedThemeState) {
  try {
    window.localStorage.setItem(THEME_STORAGE_KEY, JSON.stringify(state));
  } catch {
    // localStorage may be disabled in strict hospital desktops.
  }
}

function normalizeThemeId(themeId: unknown): ThemeId {
  if (themeId === CUSTOM_THEME_ID) {
    return CUSTOM_THEME_ID;
  }
  if (BUILT_IN_THEMES.some((theme) => theme.id === themeId)) {
    return themeId as ThemeId;
  }
  return DEFAULT_THEME_ID;
}

function normalizeCustomTheme(seed: Partial<CustomThemeSeed> | undefined): CustomThemeSeed {
  return {
    primary: normalizeHexColor(seed?.primary ?? DEFAULT_CUSTOM_THEME.primary, DEFAULT_CUSTOM_THEME.primary),
    menu: normalizeHexColor(seed?.menu ?? DEFAULT_CUSTOM_THEME.menu, DEFAULT_CUSTOM_THEME.menu),
  };
}

function applyTheme(theme: ThemeDefinition) {
  const root = document.documentElement;
  root.dataset.theme = theme.id;
  for (const variable of MANAGED_THEME_VARIABLES) {
    root.style.removeProperty(variable);
  }
  for (const [variable, value] of Object.entries(theme.overrides)) {
    root.style.setProperty(variable, value);
  }
}

function buildAntdTheme(theme: ThemeDefinition): ThemeConfig {
  const primary = getThemeValue(theme, "--mk-brand-primary");
  const primaryHover = getThemeValue(theme, "--mk-brand-primary-hover");
  const primaryActive = getThemeValue(theme, "--mk-brand-primary-active");
  const primarySoft = getThemeValue(theme, "--mk-brand-primary-soft");
  const menuBg = getThemeValue(theme, "--mk-menu-bg");
  const menuHover = getThemeValue(theme, "--mk-menu-bg-hover");
  const menuActive = getThemeValue(theme, "--mk-menu-bg-active");
  const menuText = getThemeValue(theme, "--mk-menu-text");
  const menuTextSecondary = getThemeValue(theme, "--mk-menu-text-secondary");

  return {
    ...antTheme,
    token: {
      ...antTheme.token,
      colorPrimary: primary,
      colorPrimaryHover: primaryHover,
      colorPrimaryActive: primaryActive,
      colorPrimaryBg: primarySoft,
      colorInfo: primary,
      colorInfoBg: primarySoft,
      colorLink: primary,
      colorLinkHover: primaryHover,
      colorLinkActive: primaryActive,
      colorSuccess: BASE_APP_COLORS.success,
      colorSuccessBg: BASE_APP_COLORS.successSoft,
      colorWarning: BASE_APP_COLORS.warning,
      colorWarningBg: BASE_APP_COLORS.warningSoft,
      colorError: BASE_APP_COLORS.danger,
      colorErrorBg: BASE_APP_COLORS.dangerSoft,
      colorText: BASE_APP_COLORS.textPrimary,
      colorTextSecondary: BASE_APP_COLORS.textSecondary,
      colorTextTertiary: BASE_APP_COLORS.textTertiary,
      colorBgBase: BASE_APP_COLORS.bgPage,
      colorBgContainer: BASE_APP_COLORS.bgPanel,
      colorBgLayout: BASE_APP_COLORS.bgSoft,
      colorBorder: BASE_APP_COLORS.border,
      colorSplit: BASE_APP_COLORS.borderDivider,
      fontFamily: "var(--mk-font-family)",
    },
    components: {
      ...antTheme.components,
      Layout: {
        ...antTheme.components?.Layout,
        bodyBg: BASE_APP_COLORS.bgSoft,
        headerBg: BASE_APP_COLORS.bgPanel,
        siderBg: menuBg,
        triggerBg: menuActive,
        triggerColor: menuText,
      },
      Menu: {
        ...antTheme.components?.Menu,
        darkItemBg: menuBg,
        darkSubMenuItemBg: menuBg,
        darkItemColor: menuTextSecondary,
        darkItemHoverBg: menuHover,
        darkItemHoverColor: menuText,
        darkItemSelectedBg: menuActive,
        darkItemSelectedColor: menuText,
        groupTitleColor: menuTextSecondary,
      },
    },
  };
}
