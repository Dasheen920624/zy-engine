import { useCallback, useEffect, useState } from "react";

/**
 * PR-FINAL-26：sidebar 折叠状态 + 响应式 + localStorage 持久化。
 *
 * - 桌面态：`collapsed` 控制 sider 220px / 64px（图标态）。
 * - 移动态（< 768px）：sider 退化为 drawer，由 `mobileOpen` 控制 transform。
 * - SSR 友好：所有 window 访问都做了 typeof 检查。
 * - localStorage 失败（隐私模式 / quota）静默忽略，不影响功能。
 */

const STORAGE_KEY = "mk-sidebar-collapsed";
const MOBILE_BREAKPOINT_PX = 768;

export interface SidebarCollapsedState {
  /** 桌面态折叠（图标态）；移动态忽略此字段。 */
  collapsed: boolean;
  /** 切换桌面态折叠。 */
  toggle: () => void;
  /** 当前视窗是否移动端尺寸（< 768px）。 */
  isMobile: boolean;
  /** 移动端 drawer 是否打开。 */
  mobileOpen: boolean;
  /** 设置移动端 drawer 显隐。 */
  setMobileOpen: (open: boolean) => void;
}

function readInitialCollapsed(): boolean {
  if (typeof window === "undefined") return false;
  try {
    return window.localStorage.getItem(STORAGE_KEY) === "1";
  } catch {
    return false;
  }
}

function readInitialIsMobile(): boolean {
  if (typeof window === "undefined") return false;
  return window.innerWidth < MOBILE_BREAKPOINT_PX;
}

export function useSidebarCollapsed(): SidebarCollapsedState {
  const [collapsed, setCollapsed] = useState<boolean>(readInitialCollapsed);
  const [isMobile, setIsMobile] = useState<boolean>(readInitialIsMobile);
  const [mobileOpen, setMobileOpen] = useState<boolean>(false);

  // 折叠状态变化 → 持久化
  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, collapsed ? "1" : "0");
    } catch {
      /* 隐私模式 / quota 满，静默忽略 */
    }
  }, [collapsed]);

  // 视窗宽度变化 → 重算 isMobile；切换桌面态时自动关闭 drawer
  useEffect(() => {
    const onResize = (): void => {
      const next = window.innerWidth < MOBILE_BREAKPOINT_PX;
      setIsMobile((prev) => {
        if (prev && !next) {
          // 从移动端切到桌面端，关闭 drawer
          setMobileOpen(false);
        }
        return next;
      });
    };
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  const toggle = useCallback(() => {
    setCollapsed((c) => !c);
  }, []);

  return { collapsed, toggle, isMobile, mobileOpen, setMobileOpen };
}
