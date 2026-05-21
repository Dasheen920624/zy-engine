import { useEffect, useState } from "react";
import { Button, Card, Form, Input, Typography, message } from "antd";
import { UserOutlined, LockOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import {
  listSsoProviders,
  initiateSso,
  handleSsoCallback,
  ldapAuthenticate,
  type SsoProvider,
} from "../api/sso";
import { setAuth } from "../store/auth";

const { Title, Text } = Typography;

export default function SsoLogin() {
  const navigate = useNavigate();
  const [providers, setProviders] = useState<SsoProvider[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [ldapProvider, setLdapProvider] = useState<SsoProvider | null>(null);
  const [ldapLoading, setLdapLoading] = useState(false);

  useEffect(() => {
    // 检查是否为 SSO 回调
    const params = new URLSearchParams(window.location.search);
    const code = params.get("code");
    const providerId = params.get("providerId");
    const state = params.get("state");

    if (code && providerId) {
      handleCallback(Number(providerId), code, state ?? undefined);
      return;
    }

    // 加载 SSO 身份源列表
    loadProviders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadProviders = async () => {
    try {
      setLoading(true);
      const list = await listSsoProviders();
      setProviders(list);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "加载 SSO 身份源失败";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleCallback = async (providerId: number, code: string, state?: string) => {
    try {
      setLoading(true);
      const result = await handleSsoCallback(providerId, code, state);
      setAuth(result.token, result.user);
      message.success("SSO 登录成功");
      navigate("/dashboard", { replace: true });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "SSO 登录失败";
      setError(msg);
      setLoading(false);
    }
  };

  const handleInitiate = async (provider: SsoProvider) => {
    if (provider.providerType === "LDAP") {
      setLdapProvider(provider);
      return;
    }

    try {
      const result = await initiateSso(provider.id);
      if (result.redirectUrl) {
        window.location.href = result.redirectUrl;
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "发起 SSO 登录失败";
      setError(msg);
    }
  };

  const handleLdapLogin = async (values: { username: string; password: string }) => {
    if (!ldapProvider) return;
    try {
      setLdapLoading(true);
      const result = await ldapAuthenticate(
        ldapProvider.id,
        values.username,
        values.password
      );
      setAuth(result.token, result.user);
      message.success("LDAP 登录成功");
      navigate("/dashboard", { replace: true });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "LDAP 认证失败";
      setError(msg);
    } finally {
      setLdapLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="mk-login-shell">
        <Card className="mk-login-card">
          <div style={{ textAlign: "center", padding: 24 }}>
            <Text>正在处理 SSO 登录...</Text>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="mk-login-shell">
      <Card className="mk-login-card">
        <div className="mk-login-header">
          <Title level={3} style={{ marginBottom: 4 }}>
            集团医疗智能中枢
          </Title>
          <Text type="secondary">MedKernel · 单点登录</Text>
        </div>

        {error && (
          <div style={{ color: "var(--mk-danger)", marginBottom: 16, padding: 8, background: "var(--mk-danger-bg)", borderRadius: 4 }}>
            {error}
          </div>
        )}

        {ldapProvider ? (
          <div>
            <Title level={4} style={{ textAlign: "center", marginBottom: 16 }}>
              LDAP 登录 - {ldapProvider.providerName}
            </Title>
            <Form
              name="ldap-login"
              onFinish={handleLdapLogin}
              size="large"
              autoComplete="off"
            >
              <Form.Item
                name="username"
                rules={[{ required: true, message: "请输入用户名" }]}
              >
                <Input prefix={<UserOutlined />} placeholder="用户名" autoFocus />
              </Form.Item>
              <Form.Item
                name="password"
                rules={[{ required: true, message: "请输入密码" }]}
              >
                <Input.Password prefix={<LockOutlined />} placeholder="密码" />
              </Form.Item>
              <Form.Item style={{ marginBottom: 8 }}>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={ldapLoading}
                  block
                  className="mk-login-button"
                >
                  登录
                </Button>
              </Form.Item>
              <Form.Item>
                <Button
                  block
                  onClick={() => setLdapProvider(null)}
                  style={{ background: "var(--mk-bg-secondary)", border: "none" }}
                >
                  返回
                </Button>
              </Form.Item>
            </Form>
          </div>
        ) : (
          <div>
            {providers.length === 0 ? (
              <div style={{ textAlign: "center", padding: "24px 0" }}>
                <Text type="secondary">暂无可用的 SSO 身份源</Text>
              </div>
            ) : (
              providers.map((provider) => (
                <Button
                  key={provider.id}
                  type="primary"
                  block
                  onClick={() => handleInitiate(provider)}
                  style={{ marginBottom: 8, height: 44, fontSize: 14 }}
                >
                  {provider.providerName} ({provider.providerType})
                </Button>
              ))
            )}
            <div style={{ textAlign: "center", marginTop: 16 }}>
              <a href="/login" style={{ color: "var(--mk-primary)" }}>使用账号密码登录</a>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
