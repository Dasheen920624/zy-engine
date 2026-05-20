import { Layout } from "antd";
import { Outlet } from "react-router-dom";
import TopNav from "./TopNav";
import SideMenu from "./SideMenu";

const { Content } = Layout;

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
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
