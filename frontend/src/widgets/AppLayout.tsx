import { useState, useMemo } from "react";
import { Layout, Menu, theme as antdTheme, Typography, Space, Button, Tooltip } from "antd";
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SearchOutlined,
  ToolOutlined,
} from "@ant-design/icons";
import type { MenuProps } from "antd";
import { useNavigate, useLocation, Outlet } from "react-router-dom";
import { menuSections } from "@/shared/config/menu";
import { PermissionChip } from "@/features/permission-chip/PermissionChip";
import { CommandPalette } from "@/features/command-palette/CommandPalette";
import { AuditSnapshotButton } from "@/features/audit-snapshot/AuditSnapshotButton";
import { ThemeSwitcher } from "@/features/theme-switcher/ThemeSwitcher";

const { Header, Sider, Content } = Layout;

/**
 * GA-PROD-01 · 主布局：左侧 SideMenu + 顶部 Header
 *
 * 严格遵守 docs/CONSTITUTION.md §1 第 2 条：左侧 SideMenu 永远是主菜单，
 * 顶部 Header 只放品牌、面包屑、命令面板、审计快照、主题、权限指纹。
 */
export function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const [paletteOpen, setPaletteOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = antdTheme.useToken();

  const items: MenuProps["items"] = useMemo(
    () =>
      menuSections
        .filter((s) => !s.hidden)
        .map((section) => {
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
    [],
  );

  const advancedItems = useMemo(
    () => menuSections.find((s) => s.hidden)?.items ?? [],
    [],
  );

  const handleMenuClick: MenuProps["onClick"] = (info) => {
    if (info.key.startsWith("/")) {
      navigate(info.key);
    }
  };

  const siderWidth = collapsed ? 80 : 240;

  return (
    <Layout style={{ minHeight: "100vh" }} hasSider>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        trigger={null}
        width={240}
        style={{
          background: token.colorBgContainer,
          position: "fixed",
          left: 0,
          top: 0,
          bottom: 0,
          height: "100vh",
          overflow: "auto",
          zIndex: 20,
          borderRight: `1px solid ${token.colorBorderSecondary}`,
        }}
      >
        <div
          style={{
            height: 56,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: token.colorPrimary,
            fontWeight: 600,
            fontSize: collapsed ? 14 : 16,
            position: "sticky",
            top: 0,
            background: token.colorBgContainer,
            zIndex: 1,
          }}
        >
          {collapsed ? "MK" : "集团医疗智能中枢"}
        </div>
        <Menu
          mode="inline"
          theme="light"
          selectedKeys={[location.pathname]}
          defaultOpenKeys={menuSections.filter((s) => !s.hidden && s.items.length > 1).map((s) => s.key)}
          items={items}
          onClick={handleMenuClick}
          style={{ borderRight: 0 }}
        />
        {/* 高级工具：底部隐藏式入口（与 §2.1 对齐） */}
        {!collapsed && advancedItems.length > 0 && (
          <div style={{ padding: 12, borderTop: `1px solid ${token.colorBorderSecondary}`, marginTop: 16 }}>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              高级工具 ⊕
            </Typography.Text>
            <Menu
              mode="inline"
              theme="light"
              selectedKeys={[location.pathname]}
              items={advancedItems.map((it) => ({ key: it.path, label: it.label }))}
              onClick={handleMenuClick}
              style={{ borderRight: 0, marginTop: 8 }}
            />
          </div>
        )}
      </Sider>
      <Layout style={{ marginLeft: siderWidth, transition: "margin-left 0.2s" }}>
        <Header
          style={{
            padding: "0 16px",
            background: token.colorBgContainer,
            borderBottom: `1px solid ${token.colorBorderSecondary}`,
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            position: "sticky",
            top: 0,
            zIndex: 10,
            width: "100%",
          }}
        >
          <Space>
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
            />
            <Typography.Text strong>{location.pathname}</Typography.Text>
          </Space>
          <Space>
            <Tooltip title="命令面板 (Ctrl+K)">
              <Button type="text" icon={<SearchOutlined />} onClick={() => setPaletteOpen(true)}>
                搜索
              </Button>
            </Tooltip>
            <AuditSnapshotButton />
            <ThemeSwitcher />
            <PermissionChip />
            <Typography.Text type="secondary">医务处 · 张三</Typography.Text>
          </Space>
        </Header>
        <Content style={{ padding: 24, background: token.colorBgLayout }}>
          <Outlet />
        </Content>
      </Layout>
      <CommandPalette open={paletteOpen} onClose={() => setPaletteOpen(false)} />
    </Layout>
  );
}
