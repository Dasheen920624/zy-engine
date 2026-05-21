import { Menu, type MenuProps } from "antd";
import { Link, useLocation } from "react-router-dom";
import { menuSections, type MenuItem } from "../router/menuConfig";

type AntMenuItem = Required<MenuProps>["items"][number];

/**
 * 左侧分组菜单（v0.3-final 命名收口）。
 *
 * 顶部：工作台 / 演示与校验 直放；
 * 之后：知识工厂 / 质控驾驶舱 / 用户与身份 / 平台监控 四组（M1/M3/M4），每组用 AntD Menu `type: 'group'` 显示标题。
 */
export default function SideMenu() {
  const location = useLocation();

  const items: AntMenuItem[] = menuSections.flatMap<AntMenuItem>((section) => {
    const renderedItems: AntMenuItem[] = section.items.map((item) =>
      renderMenuItem(item),
    );

    if (section.label === null) {
      // 顶部直放，不加组标题
      return renderedItems;
    }

    return [
      {
        type: "group",
        key: `group-${section.key}`,
        label: (
          <span className="mk-side-menu-section-title">{section.label}</span>
        ),
        children: renderedItems,
      } as AntMenuItem,
    ];
  });

  return (
    <div className="mk-side-menu">
      <Menu
        mode="inline"
        selectedKeys={[location.pathname]}
        items={items}
        style={{ borderInlineEnd: "none", background: "transparent" }}
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
        <span style={{ color: "var(--mk-text-tertiary)" }}>
          {item.label}
          {item.pr && (
            <span style={{ fontSize: 11, marginLeft: 4 }}>({item.pr})</span>
          )}
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
