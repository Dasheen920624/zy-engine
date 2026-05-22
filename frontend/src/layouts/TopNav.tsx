import { Avatar, Dropdown, Tag, Tooltip } from "antd";
import {
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
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
import styles from "./TopNav.module.css";

interface TopNavProps {
  /** 桌面态 sider 是否折叠；移动态时此字段表示 drawer 是否打开。 */
  collapsed: boolean;
  /** 当前是否移动端尺寸（< 768px）。 */
  isMobile: boolean;
  /** 折叠按钮点击回调（由 AppLayout 决定切桌面态折叠还是移动态 drawer）。 */
  onToggleCollapse: () => void;
}

/** 拆出 aria-label 构造避免嵌套三元（ESLint no-nested-ternary）。 */
function buildTriggerAriaLabel(isMobile: boolean, collapsed: boolean): string {
  if (isMobile) {
    return collapsed ? "关闭菜单" : "打开菜单";
  }
  return collapsed ? "展开菜单" : "折叠菜单";
}

/**
 * 顶部导航栏（PR-FINAL-26：CSS Module + 折叠按钮 + 0 inline style）。
 *
 * 左：折叠按钮 → 品牌 → 面包屑；右：主题 + 组织 Tag + 版本 + 用户。
 * 折叠按钮在桌面态切 220px ↔ 64px；移动态切 drawer 显隐。
 */
export default function TopNav({
  collapsed,
  isMobile,
  onToggleCollapse,
}: TopNavProps) {
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

  // 折叠按钮 icon：桌面态折叠 → Unfold(展开)；其余 → Fold(折叠)
  // 移动态固定 Fold（因为点击是「打开 drawer」语义）
  const FoldIcon = !isMobile && collapsed ? MenuUnfoldOutlined : MenuFoldOutlined;
  const triggerAriaLabel = buildTriggerAriaLabel(isMobile, collapsed);

  const goHome = () => navigate("/dashboard");

  return (
    <div className={styles.nav}>
      <button
        type="button"
        className={styles.collapseTrigger}
        onClick={onToggleCollapse}
        aria-label={triggerAriaLabel}
        data-testid="sidebar-collapse-trigger"
      >
        <FoldIcon />
      </button>

      <div
        className={styles.brand}
        onClick={goHome}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            goHome();
          }
        }}
      >
        <div className={styles.brandTitle}>集团医疗智能中枢</div>
        <div className={styles.brandSubtitle}>MedKernel · 管理工作台</div>
      </div>

      <div className={styles.breadcrumb}>
        <Link to="/dashboard">首页</Link>
        {section?.label && (
          <>
            <span className={styles.breadcrumbSep}>/</span>
            <span className={styles.breadcrumbSection}>{section.label}</span>
          </>
        )}
        {current?.label && (
          <>
            <span className={styles.breadcrumbSep}>/</span>
            <span className={styles.breadcrumbCurrent}>{current.label}</span>
          </>
        )}
      </div>

      <div className={styles.actions}>
        <ThemeSelector />
        <Tooltip title="组织上下文 · Header X-* 自动随请求发送">
          <Tag className={`mk-tag-primary ${styles.tagReset}`}>
            {org.hospital_code || org.group_code || org.tenant_id || "DEFAULT"}
            {org.department_code ? ` / ${org.department_code}` : ""}
          </Tag>
        </Tooltip>
        <Tag className={styles.tagReset}>v0.2</Tag>
        {authenticated && user && (
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <div className={styles.user}>
              <Avatar
                size="small"
                icon={<UserOutlined />}
                src={user.avatar_url}
              />
              <span className={styles.userName}>
                {user.display_name || user.username}
              </span>
            </div>
          </Dropdown>
        )}
      </div>
    </div>
  );
}
