import { useEffect, useState } from "react";
import { MobileOutlined, SafetyCertificateOutlined } from "@ant-design/icons";
import { Button, Checkbox, Form, Input, Typography, message } from "antd";
import type { SmsLoginValues } from "../types";
import styles from "../styles.module.css";

const { Text } = Typography;

interface SmsTabProps {
  loading: boolean;
  onSubmit: (values: SmsLoginValues) => Promise<void>;
}

export function SmsTab({ loading, onSubmit }: SmsTabProps) {
  const [form] = Form.useForm<SmsLoginValues>();
  const [accepted, setAccepted] = useState(false);
  const [countdown, setCountdown] = useState(0);

  useEffect(() => {
    if (countdown <= 0) return undefined;
    const timer = window.setTimeout(() => setCountdown((value) => value - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [countdown]);

  const sendCode = async () => {
    try {
      await form.validateFields(["phone"]);
      setCountdown(60);
      message.success("验证码已发送");
    } catch {
      message.warning("请先输入有效手机号");
    }
  };

  const codeLabel = countdown > 0 ? `${countdown}s` : "获取验证码";

  return (
    <Form
      form={form}
      name="sms-login"
      layout="vertical"
      onFinish={onSubmit}
      size="large"
      autoComplete="off"
    >
      <div className={styles.smsBanner}>
        <SafetyCertificateOutlined aria-hidden="true" />
        <Text>默认使用手机号最小化身份核验，演示验证码为 123456。</Text>
      </div>
      <Form.Item
        label="手机号"
        name="phone"
        rules={[
          { required: true, message: "请输入手机号" },
          { pattern: /^1[3-9]\d{9}$/, message: "请输入 11 位中国大陆手机号" },
        ]}
      >
        <Input prefix={<MobileOutlined />} placeholder="手机号" inputMode="tel" />
      </Form.Item>
      <Form.Item
        label="短信验证码"
        name="code"
        rules={[{ required: true, message: "请输入短信验证码" }]}
      >
        <Input
          placeholder="短信验证码"
          addonAfter={
            <Button
              type="link"
              onClick={sendCode}
              disabled={countdown > 0}
              className={styles.codeButton}
            >
              {codeLabel}
            </Button>
          }
        />
      </Form.Item>
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
        手机短信登录
      </Button>
    </Form>
  );
}
