import { useState, useMemo } from "react";
import { Breadcrumb, Drawer, Grid, Layout, Menu, Typography, Space, Button, Tooltip } from "antd";
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SearchOutlined,
  ToolOutlined,
} from "@ant-design/icons";
import type { MenuProps } from "antd";
import { useNavigate, useLocation, Outlet } from "react-router-dom";
import { menuSections } from "@/shared/config/menu";
import { findRouteByPath, getRouteBreadcrumb } from "@/shared/config/routes";
import { useSecurityProfile } from "@/shared/api/hooks";
import { PageState } from "@/shared/ui/PageState";
import { PermissionChip } from "@/features/permission-chip/PermissionChip";
import { CommandPalette } from "@/features/command-palette/CommandPalette";
import { AuditSnapshotButton } from "@/features/audit-snapshot/AuditSnapshotButton";
import { ThemeSwitcher } from "@/features/theme-switcher/ThemeSwitcher";

const { Header, Sider, Content } = Layout;

/**
 * 主布局：左侧 SideMenu + 顶部 Header。
 *
 * 严格遵守 docs/CONSTITUTION.md §1 第 2 条：左侧 SideMenu 永远是主菜单，
 * 顶部 Header 只放品牌、面包屑、命令面板、审计快照、主题、权限指纹。
 */
export function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [paletteOpen, setPaletteOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const screens = Grid.useBreakpoint();
  const isDesktop = screens.md ?? (typeof window === "undefined" ? true : window.innerWidth >= 768);
  const currentRoute = findRouteByPath(location.pathname);
  const breadcrumb = getRouteBreadcrumb(location.pathname);
  const securityProfile = useSecurityProfile();
  const allowedMenuKeys = securityProfile.data?.menuKeys;
  const canViewCurrentRoute =
    !currentRoute?.sectionKey ||
    currentRoute.sectionKey === "workbench" ||
    Boolean(allowedMenuKeys?.includes(currentRoute.sectionKey));

  const visibleMenuSections = useMemo(
    () =>
      menuSections
        .filter((s) => !s.hidden)
        .filter((s) => s.key === "workbench" || allowedMenuKeys?.includes(s.key)),
    [allowedMenuKeys],
  );

  const items: MenuProps["items"] = useMemo(
    () =>
      visibleMenuSections.map((section) => {
        if (section.key === "workbench") {
          return section.items.map((it) => ({
            key: it.path,
            label: it.label,
            icon: <ToolOutlined />,
          }))[0];
        }
        return {
          key: section.key,
          label: section.label,
          children: section.items.map((it) => ({ key: it.path, label: it.label })),
        };
      }),
    [visibleMenuSections],
  );

  const advancedItems = useMemo(
    () =>
      allowedMenuKeys?.includes("advanced-tools")
        ? (menuSections.find((s) => s.hidden)?.items ?? [])
        : [],
    [allowedMenuKeys],
  );
  let mainLayoutClassName = "mk-layout-main mk-layout-main-mobile";
  if (isDesktop) {
    mainLayoutClassName = collapsed
      ? "mk-layout-main mk-layout-main-collapsed"
      : "mk-layout-main mk-layout-main-expanded";
  }
  const headerClassName = isDesktop ? "mk-app-header" : "mk-app-header mk-app-header-mobile";
  const routeTitleClassName = isDesktop
    ? "mk-route-title mk-route-title-desktop"
    : "mk-route-title mk-route-title-mobile";

  const handleMenuClick: MenuProps["onClick"] = (info) => {
    if (info.key.startsWith("/")) {
      navigate(info.key);
      setMobileMenuOpen(false);
    }
  };

  const renderContent = () => {
    if (canViewCurrentRoute) {
      return <Outlet />;
    }
    if (!securityProfile.data) {
      return securityProfile.isError ? (
        <PageState
          state="error"
          title="暂时无法核验权限"
          description="当前无法获取授权信息，请稍后重试或联系信息科。"
        />
      ) : (
        <PageState
          state="loading"
          title="正在核验权限"
          description="正在确认当前角色与数据范围。"
        />
      );
    }
    return <PageState state="forbidden" />;
  };

  const renderNavigation = (isCollapsed: boolean) => (
    <>
      <div className={isCollapsed ? "mk-nav-brand mk-nav-brand-collapsed" : "mk-nav-brand"}>
        {isCollapsed ? "MK" : "集团医疗智能中枢"}
      </div>
      <Menu
        mode="inline"
        theme="light"
        selectedKeys={[location.pathname]}
        defaultOpenKeys={visibleMenuSections.filter((s) => s.items.length > 1).map((s) => s.key)}
        items={items}
        onClick={handleMenuClick}
        className="mk-menu-borderless"
      />
      {!isCollapsed && advancedItems.length > 0 && (
        <div className="mk-advanced-menu-wrap">
          <Typography.Text type="secondary" className="mk-text-xs">
            高级工具 ⊕
          </Typography.Text>
          <Menu
            mode="inline"
            theme="light"
            selectedKeys={[location.pathname]}
            items={advancedItems.map((it) => ({ key: it.path, label: it.label }))}
            onClick={handleMenuClick}
            className="mk-advanced-menu"
          />
        </div>
      )}
    </>
  );

  return (
    <Layout className="mk-layout-shell" hasSider={isDesktop}>
      {isDesktop && (
        <Sider
          collapsible
          collapsed={collapsed}
          onCollapse={setCollapsed}
          trigger={null}
          width={240}
          className="mk-sider-root"
        >
          {renderNavigation(collapsed)}
        </Sider>
      )}
      <Drawer
        title="集团医疗智能中枢"
        placement="left"
        open={!isDesktop && mobileMenuOpen}
        onClose={() => setMobileMenuOpen(false)}
        width={300}
        rootClassName="mk-drawer-no-body-padding"
      >
        {renderNavigation(false)}
      </Drawer>
      <Layout className={mainLayoutClassName}>
        <Header className={headerClassName}>
          <Space className="mk-min-0">
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() =>
                isDesktop ? setCollapsed(!collapsed) : setMobileMenuOpen((open) => !open)
              }
            />
            <Space direction="vertical" size={0} className="mk-min-0">
              <Typography.Text strong ellipsis className={routeTitleClassName}>
                {currentRoute?.title ?? "未找到页面"}
              </Typography.Text>
              <Breadcrumb
                items={(isDesktop ? breadcrumb : breadcrumb.slice(-1)).map((title) => ({
                  title,
                }))}
              />
            </Space>
          </Space>
          <Space size={isDesktop ? "small" : 4}>
            <Tooltip title="命令面板 (Ctrl+K)">
              <Button type="text" icon={<SearchOutlined />} onClick={() => setPaletteOpen(true)}>
                {isDesktop ? "搜索" : null}
              </Button>
            </Tooltip>
            <AuditSnapshotButton compact={!isDesktop} />
            <ThemeSwitcher compact={!isDesktop} />
            {isDesktop && <PermissionChip />}
          </Space>
        </Header>
        <Content className="mk-app-content">{renderContent()}</Content>
      </Layout>
      <CommandPalette open={paletteOpen} onClose={() => setPaletteOpen(false)} />
    </Layout>
  );
}
