import { Menu, type MenuProps } from "antd";
import { Link, useLocation } from "react-router-dom";
import { menuSections, type MenuItem } from "../router/menuConfig";
import styles from "./SideMenu.module.css";

type AntMenuItem = Required<MenuProps>["items"][number];

interface SideMenuProps {
  /** 折叠（图标态）。AntD Menu 的 inlineCollapsed 由本组件传入。 */
  collapsed?: boolean;
}

/**
 * 左侧分组菜单（PR-FINAL-26：CSS Module + 折叠支持 + 0 inline style）。
 *
 * 顶部「工作台 / 演示与校验」直放（section.label === null），其余按 v1.0 GA 客户主线分组。
 * 折叠态由 AntD Menu 的 inlineCollapsed 接管，CSS Module 仅做样式增强。
 */
export default function SideMenu({ collapsed = false }: SideMenuProps) {
  const location = useLocation();

  const items: AntMenuItem[] = menuSections.flatMap<AntMenuItem>((section) => {
    const rendered: AntMenuItem[] = section.items.map((item) =>
      renderMenuItem(item),
    );
    if (section.label === null) {
      return rendered;
    }
    return [
      {
        type: "group",
        key: `group-${section.key}`,
        label: (
          <span className={styles.sectionTitle}>{section.label}</span>
        ),
        children: rendered,
      } as AntMenuItem,
    ];
  });

  const rootClass = [
    styles.menu,
    collapsed ? styles.menuCollapsed : "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={rootClass}>
      <Menu
        mode="inline"
        inlineCollapsed={collapsed}
        selectedKeys={[location.pathname]}
        items={items}
      />
    </div>
  );
}

function renderMenuItem(item: MenuItem): AntMenuItem {
  if (item.disabled) {
    return {
      key: item.path || item.key,
      icon: item.icon,
      label: (
        <span className={styles.disabledLabel}>
          {item.label}
          {item.pr && <span className={styles.prBadge}>({item.pr})</span>}
        </span>
      ),
      disabled: true,
    } as AntMenuItem;
  }
  if (item.path) {
    return {
      key: item.path,
      icon: item.icon,
      label: <Link to={item.path}>{item.label}</Link>,
    } as AntMenuItem;
  }
  return {
    key: item.key,
    icon: item.icon,
    label: item.label,
  } as AntMenuItem;
}
