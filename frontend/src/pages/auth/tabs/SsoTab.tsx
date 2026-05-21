import { LoginOutlined, SafetyCertificateOutlined } from "@ant-design/icons";
import { Alert, Button, Empty, Skeleton, Tag, Typography } from "antd";
import type { SsoProvider } from "../../../api/sso";
import styles from "../styles.module.css";

const { Text } = Typography;

interface SsoTabProps {
  providers: SsoProvider[];
  loading: boolean;
  error: string | null;
  onInitiate: (provider: SsoProvider) => Promise<void>;
  onUseLdap: () => void;
}

const fallbackProviders = [
  { code: "cas", label: "CAS 统一认证", type: "CAS" },
  { code: "oidc", label: "OIDC 单点登录", type: "OIDC" },
  { code: "saml", label: "SAML 身份联邦", type: "SAML" },
  { code: "oauth2", label: "OAuth2 统一入口", type: "OAUTH2" },
];

function isLdapProvider(provider: SsoProvider) {
  return provider.providerType.toUpperCase().includes("LDAP");
}

export function SsoTab({ providers, loading, error, onInitiate, onUseLdap }: SsoTabProps) {
  const externalProviders = providers.filter((provider) => !isLdapProvider(provider));

  if (loading) {
    return <Skeleton active paragraph={{ rows: 4 }} />;
  }

  return (
    <div className={styles.ssoPanel}>
      <Alert
        className={styles.tabAlert}
        type="info"
        showIcon
        message="统一身份认证按医院配置动态展示，LDAP 域账号已归入域账号 Tab。"
      />
      {error ? <Alert className={styles.tabAlert} type="error" showIcon message={error} /> : null}
      {externalProviders.length > 0 ? (
        <div className={styles.providerGrid}>
          {externalProviders.map((provider) => (
            <Button
              key={provider.id}
              icon={<LoginOutlined />}
              onClick={() => onInitiate(provider)}
              className={styles.providerButton}
            >
              <span>{provider.providerName}</span>
              <Tag>{provider.providerType}</Tag>
            </Button>
          ))}
        </div>
      ) : (
        <div className={styles.fallbackProviders}>
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无已启用的 SSO 身份源" />
          <div className={styles.providerGrid}>
            {fallbackProviders.map((provider) => (
              <Button
                key={provider.code}
                icon={<LoginOutlined />}
                disabled
                className={styles.providerButton}
              >
                <span>{provider.label}</span>
                <Tag>{provider.type}</Tag>
              </Button>
            ))}
          </div>
        </div>
      )}
      <button type="button" className={styles.linkButton} onClick={onUseLdap}>
        <SafetyCertificateOutlined aria-hidden="true" />
        <Text>使用 LDAP-AD 域账号</Text>
      </button>
    </div>
  );
}
