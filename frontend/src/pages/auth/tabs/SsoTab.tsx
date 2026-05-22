import { LoginOutlined } from "@ant-design/icons";
import { Alert, Button, Empty, Skeleton } from "antd";
import type { SsoProvider } from "../../../api/sso";
import styles from "../styles.module.css";

interface SsoTabProps {
  providers: SsoProvider[];
  loading: boolean;
  error: string | null;
  onInitiate: (provider: SsoProvider) => Promise<void>;
}

function isLdapProvider(provider: SsoProvider) {
  return provider.providerType.toUpperCase().includes("LDAP");
}

export function SsoTab({ providers, loading, error, onInitiate }: SsoTabProps) {
  const externalProviders = providers.filter((provider) => !isLdapProvider(provider));

  if (loading) {
    return <Skeleton active paragraph={{ rows: 2 }} />;
  }

  return (
    <div className={styles.ssoPanel}>
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
            </Button>
          ))}
        </div>
      ) : (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="本院暂未启用统一身份认证" />
      )}
    </div>
  );
}
