import { useEffect, useState } from "react";
import {
  BankOutlined,
  CloudServerOutlined,
  SafetyCertificateOutlined,
} from "@ant-design/icons";
import { Alert, App, Card, Collapse, Spin, Typography } from "antd";
import { useNavigate } from "react-router-dom";
import { login } from "../../api/auth";
import {
  handleSsoCallback,
  initiateSso,
  listSsoProviders,
  type SsoProvider,
} from "../../api/sso";
import { setAuth } from "../../store/auth";
import { ComplianceFooter } from "./ComplianceFooter";
import { DemoHint } from "./DemoHint";
import { isDemoProfile, loginRuntimeConfig, type LoginTabKey } from "./config";
import { PasswordTab } from "./tabs/PasswordTab";
import { SsoTab } from "./tabs/SsoTab";
import type { PasswordLoginValues } from "./types";
import styles from "./styles.module.css";

const { Title, Text } = Typography;

interface LoginPageProps {
  initialTab?: LoginTabKey;
}

export default function LoginPage({ initialTab }: LoginPageProps) {
  const navigate = useNavigate();
  const { message } = App.useApp();
  const [loginLoading, setLoginLoading] = useState(false);
  const [providerLoading, setProviderLoading] = useState(true);
  const [providers, setProviders] = useState<SsoProvider[]>([]);
  const [providerError, setProviderError] = useState<string | null>(null);
  const [callbackProcessing, setCallbackProcessing] = useState(false);
  const [failedAttempts, setFailedAttempts] = useState(0);

  useEffect(() => {
    const loadProviders = async () => {
      try {
        setProviderLoading(true);
        setProviderError(null);
        const list = await listSsoProviders();
        setProviders(list);
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : "加载 SSO 身份源失败";
        setProviderError(msg);
      } finally {
        setProviderLoading(false);
      }
    };
    void loadProviders();
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const providerId = Number(params.get("providerId"));
    const code = params.get("code") || params.get("ticket");
    const state = params.get("state") ?? undefined;
    if (!providerId || !code) return;

    const completeCallback = async () => {
      try {
        setCallbackProcessing(true);
        const result = await handleSsoCallback(providerId, code, state);
        setAuth(result.token, result.user);
        message.success("SSO 登录成功");
        navigate("/dashboard", { replace: true });
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : "SSO 登录失败";
        setProviderError(msg);
      } finally {
        setCallbackProcessing(false);
      }
    };

    void completeCallback();
  }, [message, navigate]);

  const handlePasswordLogin = async (values: PasswordLoginValues) => {
    setLoginLoading(true);
    try {
      const result = await login({
        username: values.username,
        password: values.password,
      });
      setAuth(result.token, result.user);
      setFailedAttempts(0);
      message.success("登录成功");
      navigate("/dashboard", { replace: true });
    } catch (err: unknown) {
      setFailedAttempts((value) => value + 1);
      const msg = err instanceof Error ? err.message : "登录失败";
      message.error(msg);
    } finally {
      setLoginLoading(false);
    }
  };

  const handleSsoInitiate = async (provider: SsoProvider) => {
    setLoginLoading(true);
    try {
      const result = await initiateSso(provider.id);
      if (result.redirectUrl) {
        window.location.href = result.redirectUrl;
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "发起 SSO 登录失败";
      message.error(msg);
      setLoginLoading(false);
    }
  };
  const openSso = initialTab === "sso";

  if (callbackProcessing) {
    return (
      <main className={styles.shell}>
        <Card className={styles.callbackCard}>
          <Spin />
          <Text>正在完成 SSO 登录...</Text>
        </Card>
      </main>
    );
  }

  return (
    <main className={styles.shell}>
      <section className={styles.surface} aria-label="MedKernel 登录">
        <div className={styles.brandPanel}>
          <div className={styles.logoMark}>
            <BankOutlined aria-hidden="true" />
          </div>
          <Title level={1}>集团医疗智能中枢</Title>
          <Text>MedKernel · 管理工作台</Text>
          <div className={styles.securityStrip}>
            <SafetyCertificateOutlined aria-hidden="true" />
            <span>{loginRuntimeConfig.cryptoSuite} / 统一认证 / 审计留痕</span>
          </div>
        </div>
        <Card className={styles.loginCard}>
          <div className={styles.cardHeader}>
            <Title level={2}>登录 MedKernel</Title>
            <Text type="secondary">使用医院工号或管理员账号登录</Text>
          </div>
          <PasswordTab
            failedAttempts={failedAttempts}
            loading={loginLoading}
            onSubmit={handlePasswordLogin}
          />
          <Collapse
            className={styles.ssoCollapse}
            defaultActiveKey={openSso ? ["sso"] : []}
            ghost
            items={[
              {
                key: "sso",
                label: (
                  <span className={styles.ssoCollapseLabel}>
                    <CloudServerOutlined />
                    医院统一身份认证
                  </span>
                ),
                children: (
                  <SsoTab
                    providers={providers}
                    loading={providerLoading}
                    error={providerError}
                    onInitiate={handleSsoInitiate}
                  />
                ),
              },
            ]}
          />
          <Alert
            className={styles.timeoutAlert}
            type="warning"
            showIcon
            message="距会话过期 2 分钟时，系统将提示续期或自动登出。"
          />
          {isDemoProfile() ? <DemoHint /> : null}
          <ComplianceFooter />
        </Card>
      </section>
    </main>
  );
}
