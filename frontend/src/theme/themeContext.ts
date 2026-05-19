import { createContext, useContext } from "react";
import type { CustomThemeSeed, ThemeDefinition, ThemeId, getThemeOptions } from "./tokens";

export interface ThemeContextValue {
  themeId: ThemeId;
  theme: ThemeDefinition;
  customTheme: CustomThemeSeed;
  themeOptions: ReturnType<typeof getThemeOptions>;
  setThemeId: (themeId: ThemeId) => void;
  updateCustomTheme: (patch: Partial<CustomThemeSeed>) => void;
  resetCustomTheme: () => void;
}

export const ThemeContext = createContext<ThemeContextValue | null>(null);

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useTheme must be used inside MedKernelThemeProvider");
  }
  return context;
}
