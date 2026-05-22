import { useState } from "react";
import { LockOutlined, UserOutlined } from "@ant-design/icons";
import { Button, Checkbox, Form, Input, Typography } from "antd";
import { loginRuntimeConfig } from "../config";
import type { PasswordLoginValues } from "../types";
import styles from "../styles.module.css";

const { Text } = Typography;

interface PasswordTabProps {
  failedAttempts: number;
  loading: boolean;
  onSubmit: (values: PasswordLoginValues) => Promise<void>;
}

export function PasswordTab({ failedAttempts, loading, onSubmit }: PasswordTabProps) {
  const [form] = Form.useForm<PasswordLoginValues>();
  const [accepted, setAccepted] = useState(false);
  const captchaRequired = failedAttempts >= 3;
  const attemptsLeft = Math.max(loginRuntimeConfig.lockThreshold - failedAttempts, 0);

  return (
    <Form
      form={form}
      name="password-login"
      layout="vertical"
      onFinish={onSubmit}
      size="large"
      autoComplete="off"
    >
      <Form.Item label="账号" name="username" rules={[{ required: true, message: "请输入用户名" }]}>
        <Input prefix={<UserOutlined />} placeholder="用户名 / 工号" autoFocus />
      </Form.Item>
      <Form.Item label="密码" name="password" rules={[{ required: true, message: "请输入密码" }]}>
        <Input.Password prefix={<LockOutlined />} placeholder="密码" />
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
          登录失败 {loginRuntimeConfig.lockThreshold} 次后自动锁定；当前还可尝试 {attemptsLeft} 次。
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
        登录
      </Button>
    </Form>
  );
}
