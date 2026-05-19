import { Avatar, Dropdown, Menu, Tag, Tooltip } from "antd";
import type { ReactNode } from "react";
import {
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
} from "@ant-design/icons";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useOrgContext } from "../hooks/useOrgContext";
import { getUser, isAuthenticated, clearAuth } from "../store/auth";
import { topMenuItems, getTopMenuKeyByPath } from "../router/menuConfig";
import ThemeSelector from "../theme/ThemeSelector";
function renderChildLabel(child: { disabled?: boolean; label: ReactNode; path?: string }): ReactNode {
  if (child.disabled) {
    return <span style={{ color: "var(--mk-text-disabled)" }}>{child.label}</span>;
  }
  if (child.path) {
    return <Link to={child.path}>{child.label}</Link>;
  }
  return child.label;
}

/**
 * 顶部导航栏组件
 * 左侧 Logo + 中部一级菜单 + 右侧组织选择器 + 用户头像
 */
export default function TopNav() {
  const location = useLocation();
  const navigate = useNavigate();
  const [org] = useOrgContext();
  const user = getUser();
  const authenticated = isAuthenticated();

  const topMenuKey = getTopMenuKeyByPath(location.pathname);

  const handleLogout = () => {
    clearAuth();
    navigate("/login");
  };

  const userMenuItems = [
    {
      key: "profile",
      icon: <UserOutlined />,
      label: "个人信息",
    },
    {
      key: "settings",
      icon: <SettingOutlined />,
      label: "系统设置",
    },
    {
      type: "divider" as const,
    },
    {
      key: "logout",
      icon: <LogoutOutlined />,
      label: "退出登录",
      onClick: handleLogout,
    },
  ];

  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        height: 48,
        padding: "0 24px",
        background: "var(--mk-bg-panel)",
        borderBottom: "var(--mk-border-width) solid var(--mk-border)",
      }}
    >
      {/* 左侧 Logo */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          marginRight: 32,
          cursor: "pointer",
        }}
        onClick={() => navigate("/dashboard")}
      >
        <div
          style={{
            fontSize: 18,
            fontWeight: 700,
            color: "var(--mk-primary)",
            marginRight: 8,
          }}
        >
          MK
        </div>
        <div
          style={{
            fontSize: 14,
            color: "var(--mk-text-secondary)",
            borderLeft: "1px solid var(--mk-border)",
            paddingLeft: 8,
          }}
        >
          医疗智能引擎
        </div>
      </div>

      {/* 中部一级菜单 */}
      <Menu
        mode="horizontal"
        selectedKeys={[topMenuKey]}
        items={topMenuItems.map((item) => ({
          key: item.key,
          label: item.path ? (
            <Link to={item.path}>{item.label}</Link>
          ) : (
            item.label
          ),
          children: item.children?.map((child) => ({
            key: child.key,
            label: renderChildLabel(child),
            disabled: child.disabled,
          })),
        }))}
        style={{
          flex: 1,
          borderInlineEnd: "none",
          background: "transparent",
        }}
      />

      {/* 右侧区域 */}
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
        <ThemeSelector />
        <Tooltip title="组织上下文 · Header X-* 自动随请求发送">
          <Tag className="mk-tag-primary" style={{ margin: 0 }}>
            {org.hospital_code || org.group_code || org.tenant_id || "DEFAULT"}
            {org.department_code ? ` / ${org.department_code}` : ""}
          </Tag>
        </Tooltip>
        {authenticated && user && (
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                cursor: "pointer",
                padding: "4px 8px",
                borderRadius: "var(--mk-border-radius)",
                transition: "background 0.2s",
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = "var(--mk-bg-hover)";
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = "transparent";
              }}
            >
              <Avatar
                size="small"
                icon={<UserOutlined />}
                src={user.avatar_url}
              />
              <span style={{ color: "var(--mk-text-primary)", fontSize: 13 }}>
                {user.display_name || user.username}
              </span>
            </div>
          </Dropdown>
        )}
      </div>
    </div>
  );
}
