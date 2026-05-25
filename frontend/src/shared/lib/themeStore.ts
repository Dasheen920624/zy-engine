import { create } from "zustand";

/**
 * 全局主题模式 store（Zustand 5）。
 * App.tsx 读这里的 mode 决定 ConfigProvider theme。
 */
export type ThemeMode = "default" | "elder" | "dark" | "eye" | "system";

interface ThemeState {
  mode: ThemeMode;
  setMode: (m: ThemeMode) => void;
}

const STORAGE_KEY = "medkernel.theme.mode";

function readInitial(): ThemeMode {
  if (typeof window === "undefined") return "default";
  const saved = window.localStorage.getItem(STORAGE_KEY);
  if (
    saved === "default" ||
    saved === "elder" ||
    saved === "dark" ||
    saved === "eye" ||
    saved === "system"
  ) {
    return saved;
  }
  return "default";
}

export const useThemeStore = create<ThemeState>((set) => ({
  mode: readInitial(),
  setMode: (mode) => {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(STORAGE_KEY, mode);
    }
    set({ mode });
  },
}));
