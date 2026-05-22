import { Outlet, useLocation } from "react-router-dom";
import { useEffect } from "react";
import TopNav from "./TopNav";
import SideMenu from "./SideMenu";
import { useSidebarCollapsed } from "./useSidebarCollapsed";
import styles from "./AppLayout.module.css";

/**
 * 应用主布局（PR-FINAL-26 重构）。
 *
 * - 顶部 Header（fixed，56px）：品牌 + 面包屑 + 用户 + 折叠按钮
 * - 左侧 Sider（fixed，220px / 64px 折叠）：分组菜单，状态持久化到 localStorage
 * - 中间 Content：margin 自动跟随 Sider 折叠态，独立 overflow-y 滚动
 * - 响应式：< 768px Sider 退化为 drawer，由 Header 汉堡按钮 + 遮罩控制
 */
export default function AppLayout() {
  const { collapsed, toggle, isMobile, mobileOpen, setMobileOpen } =
    useSidebarCollapsed();
  const location = useLocation();

  // 路由变化时关闭移动端 drawer（避免点完菜单后 drawer 还盖在内容上）
  useEffect(() => {
    if (isMobile && mobileOpen) {
      setMobileOpen(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.pathname]);

  const siderClass = [
    styles.sider,
    !isMobile && collapsed ? styles.siderCollapsed : "",
    isMobile && mobileOpen ? styles.siderMobileOpen : "",
  ]
    .filter(Boolean)
    .join(" ");

  const contentClass = [
    styles.content,
    !isMobile && collapsed ? styles.contentCollapsed : "",
  ]
    .filter(Boolean)
    .join(" ");

  const handleToggle = () => {
    if (isMobile) {
      setMobileOpen(!mobileOpen);
    } else {
      toggle();
    }
  };

  return (
    <div className={styles.shell}>
      <header className={styles.header} role="banner">
        <TopNav
          collapsed={isMobile ? mobileOpen : collapsed}
          isMobile={isMobile}
          onToggleCollapse={handleToggle}
        />
      </header>

      <aside
        className={siderClass}
        role="navigation"
        aria-label="主菜单"
        aria-expanded={isMobile ? mobileOpen : !collapsed}
      >
        <SideMenu collapsed={!isMobile && collapsed} />
      </aside>

      {isMobile && mobileOpen && (
        <button
          type="button"
          className={styles.mobileMask}
          aria-label="关闭菜单"
          onClick={() => setMobileOpen(false)}
        />
      )}

      <main className={contentClass} role="main">
        <Outlet />
      </main>
    </div>
  );
}
