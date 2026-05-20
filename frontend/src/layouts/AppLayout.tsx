import { Layout } from "antd";
import { Outlet } from "react-router-dom";
import TopNav from "./TopNav";
import SideMenu from "./SideMenu";

const { Content, Sider, Header } = Layout;

/**
 * 应用主布局（PR-V2-03 原始两段式恢复版）。
 *
 * - 顶部 Header：产品标题 + 面包屑 + 组织上下文 + 用户
 * - 左侧 Sider（固定 220px）：两段式分组菜单
 * - 主内容区：路由 Outlet
 */
export default function AppLayout() {
  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Header className="mk-app-header">
        <TopNav />
      </Header>
      <Layout className="mk-app-body">
        <Sider
          width={220}
          theme="light"
          className="mk-app-sider"
          breakpoint="lg"
          collapsedWidth="0"
        >
          <SideMenu />
        </Sider>
        <Content className="mk-app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
