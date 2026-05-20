import { Menu, type MenuProps } from "antd";
import { Link, useLocation } from "react-router-dom";
import { menuSections, type MenuItem } from "../router/menuConfig";

type AntMenuItem = Required<MenuProps>["items"][number];

/**
 * 左侧两段式分组菜单（PR-V2-03 原始设计）。
 *
 * 顶部：工作台 / 演示与校验 直放；
 * 之后：配置治理 / 运营治理 / 用户与组织 / 系统 四组，每组用 AntD Menu `type: 'group'` 显示标题。
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
        <span style={{ color: "var(--mk-text-disabled, #9ca3af)" }}>
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
