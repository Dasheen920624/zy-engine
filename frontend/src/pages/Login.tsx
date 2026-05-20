import { useState, useEffect } from "react";
import { Button, Card, Divider, Form, Input, Typography, message } from "antd";
import { LockOutlined, UserOutlined, LoginOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { login } from "../api/auth";
import { setAuth } from "../store/auth";
import { initiateSso, ldapAuthenticate, listSsoProviders } from "../api/sso";

const { Title, Text } = Typography;

const SSO_PROVIDERS = [
  { code: "cas", label: "CAS 统一认证", protocol: "CAS" },
  { code: "oidc", label: "OIDC 单点登录", protocol: "OIDC" },
  { code: "saml", label: "SAML 身份联邦", protocol: "SAML" },
];

export default function Login() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [ldapVisible, setLdapVisible] = useState(false);
  const [ldapForm] = Form.useForm();

  const [ssoProviders, setSsoProviders] = useState<Array<{id: number; providerCode: string; providerName: string; providerType: string}>>([]);

  useEffect(() => {
    listSsoProviders().then(setSsoProviders).catch(() => {});
  }, []);

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const result = await login(values);
      setAuth(result.token, result.user);
      message.success("登录成功");
      navigate("/dashboard", { replace: true });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "登录失败";
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleSsoLogin = async (providerId: number) => {
    setLoading(true);
    try {
      const result = await initiateSso(providerId);
      if (result.redirectUrl) {
        window.location.href = result.redirectUrl;
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "SSO 登录发起失败";
      message.error(msg);
      setLoading(false);
    }
  };

  const handleLdapLogin = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const ldapProvider = ssoProviders.find(p => p.providerType === "LDAP-AD");
      if (!ldapProvider) {
        message.error("未找到 LDAP 身份源");
        return;
      }
      const result = await ldapAuthenticate(ldapProvider.id, values.username, values.password);
      setAuth(result.token, result.user);
      message.success("LDAP 登录成功");
      navigate("/dashboard", { replace: true });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "LDAP 登录失败";
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mk-login-shell">
      <Card className="mk-login-card">
        <div className="mk-login-header">
          <Title level={3} style={{ marginBottom: 4 }}>
            医疗智能引擎平台
          </Title>
          <Text type="secondary">治理控制台 · 用户登录</Text>
        </div>

        <Form
          name="login"
          initialValues={{ remember: true }}
          onFinish={onFinish}
          size="large"
          autoComplete="off"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: "请输入用户名" }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="用户名"
              autoFocus
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: "请输入密码" }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 12 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              className="mk-login-button"
            >
              登录
            </Button>
          </Form.Item>
        </Form>

        <Divider style={{ margin: "12px 0" }}>
          <Text type="secondary" style={{ fontSize: 12 }}>其他登录方式</Text>
        </Divider>

        {/* SSO 登录按钮 */}
        <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
          {ssoProviders
            .filter(p => p.providerType !== "LDAP-AD")
            .map((p) => (
              <Button
                key={p.id}
                icon={<LoginOutlined />}
                loading={loading}
                onClick={() => handleSsoLogin(p.id)}
                style={{ flex: 1 }}
              >
                {p.providerName}
              </Button>
            ))}
          {ssoProviders.filter(p => p.providerType !== "LDAP-AD").length === 0 && (
            <>
              {SSO_PROVIDERS.map((p) => (
                <Button
                  key={p.code}
                  icon={<LoginOutlined />}
                  loading={loading}
                  onClick={() => message.info("请先配置 SSO 身份源")}
                  style={{ flex: 1 }}
                >
                  {p.label}
                </Button>
              ))}
            </>
          )}
        </div>

        {/* LDAP 登录 */}
        <Button
          type="link"
          onClick={() => setLdapVisible(!ldapVisible)}
          style={{ padding: 0, marginBottom: 8 }}
        >
          LDAP-AD 域账号登录
        </Button>

        {ldapVisible && (
          <Form form={ldapForm} onFinish={handleLdapLogin} size="large">
            <Form.Item name="username" rules={[{ required: true, message: "请输入域账号" }]}>
              <Input prefix={<UserOutlined />} placeholder="域账号" />
            </Form.Item>
            <Form.Item name="password" rules={[{ required: true, message: "请输入密码" }]}>
              <Input.Password prefix={<LockOutlined />} placeholder="域密码" />
            </Form.Item>
            <Form.Item style={{ marginBottom: 0 }}>
              <Button type="primary" htmlType="submit" loading={loading} block>
                LDAP 登录
              </Button>
            </Form.Item>
          </Form>
        )}

        <div style={{ textAlign: "center", marginTop: 8 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            演示账号：zhao01 / demo123
          </Text>
        </div>
      </Card>
    </div>
  );
}
