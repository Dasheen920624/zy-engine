import { Menu } from "antd";
import { Link, useLocation } from "react-router-dom";
import { getTopMenuKeyByPath, getSideMenuItems } from "../router/menuConfig";

/**
 * 侧边菜单组件
 * 根据当前一级菜单展开二级菜单
 */
export default function SideMenu() {
  const location = useLocation();
  const topMenuKey = getTopMenuKeyByPath(location.pathname);
  const sideMenuItems = getSideMenuItems(topMenuKey);

  if (sideMenuItems.length === 0) {
    return null;
  }

  return (
    <div
      style={{
        width: 200,
        background: "var(--mk-bg-panel)",
        borderRight: "var(--mk-border-width) solid var(--mk-border)",
        height: "100%",
        overflow: "auto",
      }}
    >
      <Menu
        mode="inline"
        selectedKeys={[location.pathname]}
        items={sideMenuItems.map((item) => ({
          key: item.path || item.key,
          label: item.disabled ? (
            <span style={{ color: "var(--mk-text-disabled)" }}>
              {item.label}
              {item.pr && (
                <span style={{ fontSize: 11, marginLeft: 4 }}>({item.pr})</span>
              )}
            </span>
          ) : item.path ? (
            <Link to={item.path}>{item.label}</Link>
          ) : (
            item.label
          ),
          disabled: item.disabled,
        }))}
        style={{
          borderInlineEnd: "none",
          background: "transparent",
        }}
      />
    </div>
  );
}
