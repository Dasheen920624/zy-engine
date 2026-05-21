import { LockOutlined, UserOutlined } from "@ant-design/icons";
import { Alert, Button, Form, Input, Select } from "antd";
import type { SsoProvider } from "../../../api/sso";
import type { LdapLoginValues } from "../types";
import styles from "../styles.module.css";

interface LdapTabProps {
  providers: SsoProvider[];
  loading: boolean;
  onSubmit: (values: LdapLoginValues) => Promise<void>;
}

function toProviderOption(provider: SsoProvider) {
  return {
    value: provider.id,
    label: `${provider.providerName} (${provider.providerType})`,
  };
}

export function LdapTab({ providers, loading, onSubmit }: LdapTabProps) {
  const ldapProviders = providers.filter((provider) =>
    provider.providerType.toUpperCase().includes("LDAP"),
  );

  return (
    <Form
      name="ldap-login"
      layout="vertical"
      onFinish={onSubmit}
      size="large"
      autoComplete="off"
      initialValues={{
        providerId: ldapProviders[0]?.id,
        domain: "hospital.local",
      }}
    >
      <Alert
        className={styles.tabAlert}
        type="info"
        showIcon
        message="域账号用于医院内网 AD / LDAP 身份源，凭据不在前端留存。"
      />
      {ldapProviders.length > 0 ? (
        <Form.Item
          label="身份源"
          name="providerId"
          rules={[{ required: true, message: "请选择 LDAP 身份源" }]}
        >
          <Select options={ldapProviders.map(toProviderOption)} />
        </Form.Item>
      ) : (
        <Form.Item label="域名" name="domain">
          <Select
            options={[
              { value: "hospital.local", label: "hospital.local" },
              { value: "medkernel.local", label: "medkernel.local" },
            ]}
          />
        </Form.Item>
      )}
      <Form.Item
        label="域账号"
        name="username"
        rules={[{ required: true, message: "请输入域账号" }]}
      >
        <Input prefix={<UserOutlined />} placeholder="域账号" />
      </Form.Item>
      <Form.Item
        label="域密码"
        name="password"
        rules={[{ required: true, message: "请输入域密码" }]}
      >
        <Input.Password prefix={<LockOutlined />} placeholder="域密码" />
      </Form.Item>
      <Button
        type="primary"
        htmlType="submit"
        loading={loading}
        block
        className={styles.submitButton}
      >
        域账号登录
      </Button>
    </Form>
  );
}
