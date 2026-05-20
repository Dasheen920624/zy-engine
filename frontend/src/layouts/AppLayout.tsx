import { Layout } from "antd";
import { Outlet } from "react-router-dom";
import TopNav from "./TopNav";
import SideMenu from "./SideMenu";

const { Content } = Layout;
      { key: "/config/packages", icon: <FileSearchOutlined />, label: <Link to="/config/packages">配置包中心</Link> },
      { key: "/provenance", icon: <ReadOutlined />, label: <Link to="/provenance">来源追溯</Link> },
      { key: "/pathways", icon: <NodeIndexOutlined />, label: "路径配置", disabled: true },
      { key: "/rules", icon: <SafetyCertificateOutlined />, label: "规则配置", disabled: true },
      { key: "/graphs", icon: <ShareAltOutlined />, label: "图谱配置", disabled: true },
      { key: "/dify", icon: <ClusterOutlined />, label: "Dify 工作流", disabled: true },
      { key: "/terminology", icon: <MedicineBoxOutlined />, label: "标准化中心", disabled: true },
      { key: "/adapters", icon: <LinkOutlined />, label: "适配器中心", disabled: true },
    ],
  },
  {
    key: "ops-group",
    type: "group" as const,
    label: "运营治理",
    children: [
      { key: "/hospital-dashboard", icon: <BankOutlined />, label: "院级驾驶舱", disabled: true },
      { key: "/quality", icon: <AuditOutlined />, label: "质控运营", disabled: true },
      { key: "/release-sync", icon: <ToolOutlined />, label: "发布与同步", disabled: true },
      { key: "/system/providers", icon: <ClusterOutlined />, label: <Link to="/system/providers">Provider 状态</Link> },
    ],
  },
];
>>>>>>> origin/codex/product-audit-cleanup

/**
 * 应用主布局组件
 * 顶 + 侧 + 内容三段布局
 */
export default function AppLayout() {
  return (
    <Layout style={{ minHeight: "100vh" }}>
      {/* 顶部导航栏 */}
      <TopNav />
      <Layout>
        {/* 侧边菜单 */}
        <SideMenu />
        {/* 主内容区 */}
        <Content style={{ padding: 24, overflow: "auto", background: "var(--mk-bg-layout)" }}>
          style={{ borderInlineEnd: "none" }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: "var(--mk-bg-panel)",
            borderBottom: "var(--mk-border-width) solid var(--mk-border)",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "0 24px",
          }}
        >
          <div style={{ color: "var(--mk-text-secondary)", fontSize: 13 }}>
            首页 /{" "}
            <strong style={{ color: "var(--mk-text-primary)" }}>
              {describePath(location.pathname)}
            </strong>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <Tooltip title="组织上下文 · Header X-* 自动随请求发送">
              <Tag color="blue" style={{ margin: 0 }}>
                {org.hospital_code || org.group_code || org.tenant_id || "DEFAULT"}
                {org.department_code ? ` / ${org.department_code}` : ""}
              </Tag>
            </Tooltip>
            <Tag color="default" style={{ fontFamily: "var(--mk-font-mono)" }}>
              v0.1
            </Tag>
          </div>
        </Header>
        <Content style={{ padding: 24, overflow: "auto" }}>
>>>>>>> origin/codex/product-audit-cleanup
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
  }
}
>>>>>>> origin/codex/product-audit-cleanup
