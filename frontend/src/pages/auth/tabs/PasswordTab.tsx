import { useMemo, useState } from "react";
import {
  CheckCircleOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { Alert, Button, Checkbox, Form, Input, Select, Typography } from "antd";
import { loginRuntimeConfig } from "../config";
import type { PasswordLoginValues } from "../types";
import styles from "../styles.module.css";

const { Text } = Typography;

interface PasswordTabProps {
  failedAttempts: number;
  loading: boolean;
  onSubmit: (values: PasswordLoginValues) => Promise<void>;
}

const passwordRules = [
  { key: "length", label: "8 位以上", test: (value: string) => value.length >= 8 },
  { key: "upper", label: "含大写字母", test: (value: string) => /[A-Z]/.test(value) },
  { key: "lower", label: "含小写字母", test: (value: string) => /[a-z]/.test(value) },
  { key: "number", label: "含数字", test: (value: string) => /\d/.test(value) },
  {
    key: "symbol",
    label: "含特殊字符",
    test: (value: string) => /[^A-Za-z0-9]/.test(value),
  },
];

export function PasswordTab({ failedAttempts, loading, onSubmit }: PasswordTabProps) {
  const [form] = Form.useForm<PasswordLoginValues>();
  const [accepted, setAccepted] = useState(false);
  const password = Form.useWatch("password", form) ?? "";
  const captchaRequired = failedAttempts >= 3;
  const attemptsLeft = Math.max(loginRuntimeConfig.lockThreshold - failedAttempts, 0);

  const strength = useMemo(
    () =>
      passwordRules.map((rule) => ({
        ...rule,
        passed: rule.test(password),
      })),
    [password],
  );

  return (
    <Form
      form={form}
      name="password-login"
      layout="vertical"
      onFinish={onSubmit}
      size="large"
      autoComplete="off"
    >
      <Alert
        className={styles.tabAlert}
        type="info"
        showIcon
        message={`当前密码通道：${loginRuntimeConfig.cryptoSuite} 加密传输 · MFA 可选`}
      />
      <Form.Item label="账号" name="username" rules={[{ required: true, message: "请输入用户名" }]}>
        <Input prefix={<UserOutlined />} placeholder="用户名 / 工号" autoFocus />
      </Form.Item>
      <Form.Item label="密码" name="password" rules={[{ required: true, message: "请输入密码" }]}>
        <Input.Password prefix={<LockOutlined />} placeholder="密码" />
      </Form.Item>
      <div className={styles.passwordPolicy} aria-live="polite">
        <Text type="secondary">密码强度策略</Text>
        <ul className={styles.policyList}>
          {strength.map((item) => (
            <li key={item.key} className={item.passed ? styles.policyPassed : undefined}>
              <CheckCircleOutlined aria-hidden="true" />
              <span>{item.label}</span>
            </li>
          ))}
        </ul>
      </div>
      <Form.Item label="二次验证" name="mfaMethod" initialValue="sms">
        <Select
          options={[
            { value: "sms", label: "短信验证码" },
            { value: "totp", label: "TOTP 动态口令" },
            { value: "u2f", label: "U2F 安全密钥" },
          ]}
          suffixIcon={<SafetyCertificateOutlined />}
        />
      </Form.Item>
      {captchaRequired ? (
        <Form.Item
          label="图形验证码"
          name="captcha"
          rules={[{ required: true, message: "请输入图形验证码" }]}
        >
          <Input placeholder="连续失败后启用验证码" />
        </Form.Item>
      ) : null}
      <div className={styles.lockNotice}>
        <Text type="secondary">
          登录失败锁定阈值 {loginRuntimeConfig.lockThreshold} 次；当前还可尝试 {attemptsLeft} 次。
        </Text>
      </div>
      <Checkbox
        className={styles.agreement}
        checked={accepted}
        onChange={(event) => setAccepted(event.target.checked)}
      >
        已阅读并同意 <a href="/legal/user-agreement">用户协议</a> 与{" "}
        <a href="/legal/privacy-policy">隐私政策</a>
      </Checkbox>
      <Button
        type="primary"
        htmlType="submit"
        loading={loading}
        disabled={!accepted}
        block
        className={styles.submitButton}
      >
        账号密码登录
      </Button>
    </Form>
  );
}
