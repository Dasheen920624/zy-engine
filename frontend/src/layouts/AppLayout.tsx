import { Layout, Menu, Tag, Tooltip } from "antd";
import {
  AppstoreOutlined,
  AuditOutlined,
  BankOutlined,
  ClusterOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  LinkOutlined,
  MedicineBoxOutlined,
  NodeIndexOutlined,
  ReadOutlined,
  SafetyCertificateOutlined,
  ShareAltOutlined,
  ToolOutlined,
} from "@ant-design/icons";
import { Link, Outlet, useLocation } from "react-router-dom";
import { useOrgContext } from "../hooks/useOrgContext";
import ThemeSelector from "../theme/ThemeSelector";

const { Header, Sider, Content } = Layout;

const menuItems = [
  { key: "/dashboard", icon: <AppstoreOutlined />, label: <Link to="/dashboard">工作台</Link> },
  { key: "/demo-validation", icon: <ExperimentOutlined />, label: <Link to="/demo-validation">演示与校验</Link> },
  {
    key: "config-group",
    type: "group" as const,
    label: "配置治理",
    children: [
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

export default function AppLayout() {
  const location = useLocation();
  const [org] = useOrgContext();

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider width={224} theme="dark" style={{ background: "var(--mk-menu-bg)" }}>
        <div
          style={{
            color: "var(--mk-text-inverse)",
            padding: "16px 20px 12px",
            borderBottom: "var(--mk-border-width) solid var(--mk-border-inverse)",
            marginBottom: 8,
          }}
        >
          <div style={{ fontSize: 16, fontWeight: 700 }}>医疗智能引擎平台</div>
          <div style={{ fontSize: 12, color: "var(--mk-text-inverse-muted)" }}>
            medkernel · 治理控制台
          </div>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems as never}
          style={{ borderInlineEnd: "none", background: "var(--mk-menu-bg)" }}
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
            <ThemeSelector />
            <Tooltip title="组织上下文 · Header X-* 自动随请求发送">
              <Tag className="mk-tag-primary" style={{ margin: 0 }}>
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
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}

function describePath(path: string): string {
  switch (path) {
    case "/dashboard":
      return "工作台";
    case "/demo-validation":
      return "演示与校验";
    case "/config/packages":
    case "/config-packages":
      return "配置包中心";
    case "/provenance":
      return "来源追溯";
    case "/system/providers":
      return "Provider 状态";
    default:
      return path;
  }
}
