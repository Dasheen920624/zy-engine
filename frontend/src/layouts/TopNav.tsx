import { Avatar, Dropdown, Tag, Tooltip } from "antd";
import {
  LogoutOutlined,
  SettingOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useOrgContext } from "../hooks/useOrgContext";
import { clearAuth, getUser, isAuthenticated } from "../store/auth";
import {
  findMenuItemByPath,
  findSectionByPath,
} from "../router/menuConfig";
import ThemeSelector from "../theme/ThemeSelector";

/**
 * 顶部导航栏（PR-V2-03 原始设计）。
 *
 * 左：产品标题 + 子标题；中：面包屑（section / page）；右：主题切换 + 组织上下文 Tag + 版本号 + 用户头像。
 * 菜单从顶部移除，全部下沉到 SideMenu 的两段式分组，避免横竖菜单同时争注意力。
 */
export default function TopNav() {
  const location = useLocation();
  const navigate = useNavigate();
  const [org] = useOrgContext();
  const user = getUser();
  const authenticated = isAuthenticated();

  const section = findSectionByPath(location.pathname);
  const current = findMenuItemByPath(location.pathname);

  const handleLogout = () => {
    clearAuth();
    navigate("/login");
  };

  const userMenuItems = [
    { key: "profile", icon: <UserOutlined />, label: "个人信息" },
    { key: "settings", icon: <SettingOutlined />, label: "系统设置" },
    { type: "divider" as const },
    {
      key: "logout",
      icon: <LogoutOutlined />,
      label: "退出登录",
      onClick: handleLogout,
    },
  ];

  return (
    <div className="mk-top-nav">
      <div className="mk-top-nav__brand" onClick={() => navigate("/dashboard")}>
        <div className="mk-top-nav__brand-title">集团医疗智能中枢</div>
        <div className="mk-top-nav__brand-subtitle">MedKernel · 管理工作台</div>
      </div>

      <div className="mk-top-nav__breadcrumb">
        <Link to="/dashboard">首页</Link>
        {section?.label && (
          <>
            <span className="mk-top-nav__breadcrumb-sep">/</span>
            <span className="mk-top-nav__breadcrumb-section">
              {section.label}
            </span>
          </>
        )}
        {current?.label && (
          <>
            <span className="mk-top-nav__breadcrumb-sep">/</span>
            <span className="mk-top-nav__breadcrumb-current">
              {current.label}
            </span>
          </>
        )}
      </div>

      <div className="mk-top-nav__actions">
        <ThemeSelector />
        <Tooltip title="组织上下文 · Header X-* 自动随请求发送">
          <Tag className="mk-tag-primary" style={{ margin: 0 }}>
            {org.hospital_code || org.group_code || org.tenant_id || "DEFAULT"}
            {org.department_code ? ` / ${org.department_code}` : ""}
          </Tag>
        </Tooltip>
        <Tag style={{ margin: 0 }}>v0.2</Tag>
        {authenticated && user && (
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <div className="mk-top-nav__user">
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
