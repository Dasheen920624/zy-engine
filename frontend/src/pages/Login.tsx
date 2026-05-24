import { Card, Form, Input, Button, Typography, Space, Divider } from "antd";
import { UserOutlined, LockOutlined } from "@ant-design/icons";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

const { Title, Text, Link } = Typography;

/**
 * 默认登录路径 + MFA/SSO 折叠区。
 *
 * 与 docs/CONSTITUTION.md §1 第 6 条对齐：
 * - 默认只有账号密码 1 个主动作
 * - 统一身份认证（CAS/OIDC/SAML）作为次级折叠区
 * - MFA / 国密 / 演示账号 不让用户手动选，由系统按医院策略
 * - ICP/公安备案、用户协议、隐私政策必须保留
 */
export default function Login() {
  const [showSso, setShowSso] = useState(false);
  const navigate = useNavigate();

  function handleSubmit(values: { username: string; password: string }) {
    // 骨架版：不真实鉴权。GA-CORE-02 后端 OAuth2 接通后实装。
    console.info("login mock", values.username);
    navigate("/dashboard");
  }

  return (
    <div
      style={{
        height: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#f5f7fa",
      }}
    >
      <Card style={{ width: 420 }} bordered={false}>
        <Space direction="vertical" size="large" style={{ width: "100%" }}>
          <Space direction="vertical" size={0}>
            <Title level={3} style={{ marginBottom: 0, color: "#1565c0" }}>
              集团医疗智能中枢
            </Title>
            <Text type="secondary">MedKernel · v1.0 GA</Text>
          </Space>

          <Form layout="vertical" onFinish={handleSubmit}>
            <Form.Item name="username" rules={[{ required: true, message: "请输入工号或账号" }]}>
              <Input prefix={<UserOutlined />} placeholder="工号 / 账号" size="large" />
            </Form.Item>
            <Form.Item name="password" rules={[{ required: true, message: "请输入密码" }]}>
              <Input.Password prefix={<LockOutlined />} placeholder="密码" size="large" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" block size="large">
                登录
              </Button>
            </Form.Item>
          </Form>

          {/* 折叠区：统一身份认证（按医院配置展示） */}
          <div>
            <Divider style={{ margin: "8px 0" }}>
              <Link onClick={() => setShowSso(!showSso)} style={{ fontSize: 12 }}>
                {showSso ? "收起" : "院方统一身份认证"}
              </Link>
            </Divider>
            {showSso && (
              <Space direction="vertical" size="small" style={{ width: "100%" }}>
                <Button block>用 CAS 登录</Button>
                <Button block>用 OIDC 登录</Button>
                <Button block>用 SAML 登录</Button>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  具体可用方式由医院信息中心配置。MFA / 国密 / 国产 CA 由系统按策略自动选择。
                </Text>
              </Space>
            )}
          </div>

          {/* 合规底线（与 §1 第 1 条对齐） */}
          <Space direction="vertical" size={0} style={{ width: "100%", textAlign: "center" }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              <Link>用户协议</Link> · <Link>隐私政策</Link> · <Link>个人信息收集清单</Link>
            </Text>
            <Text type="secondary" style={{ fontSize: 12 }}>
              ICP 备案号待填 · 公安备案号待填 · 等保 2.0 三级 · 商密评测预审中
            </Text>
          </Space>
        </Space>
      </Card>
    </div>
  );
}
