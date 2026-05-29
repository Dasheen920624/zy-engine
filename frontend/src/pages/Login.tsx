import { Alert, Card, Form, Input, Button, Typography, Divider } from "antd";
import { UserOutlined, LockOutlined } from "@ant-design/icons";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useLogin } from "@/shared/api/hooks";
import styles from "./Login.module.css";

const { Title, Text } = Typography;

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
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const navigate = useNavigate();
  const login = useLogin();

  async function handleSubmit(values: { username: string; password: string; tenantId?: string }) {
    setErrorMsg(null);
    try {
      await login.mutateAsync({
        username: values.username,
        password: values.password,
        tenantId: values.tenantId?.trim() || undefined,
      });
      navigate("/dashboard");
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { detail?: string; message?: string } } };
      setErrorMsg(
        axiosErr.response?.data?.detail ??
          axiosErr.response?.data?.message ??
          "登录失败：用户名或密码不正确",
      );
    }
  }

  return (
    <main className={styles.page}>
      <section className={styles.contextPanel} aria-label="登录上下文">
        <Text className={styles.kicker}>MedKernel · v1.0 GA</Text>
        <Title level={1} className={styles.brandTitle}>
          集团医疗智能中枢
        </Title>
        <Text className={styles.primaryGoal}>当前任务：确认身份并进入工作台</Text>

        <ul className={styles.signalList} aria-label="当前入口状态">
          <li className={styles.signalItem}>
            <span>试点组织</span>
            <strong>集团总院 · 信息科联调环境</strong>
          </li>
          <li className={styles.signalItem}>
            <span>安全审计</span>
            <strong>安全审计已开启</strong>
          </li>
          <li className={styles.signalItem}>
            <span>身份策略</span>
            <strong>MFA / 国密 / 国产 CA 按医院策略自动选择</strong>
          </li>
        </ul>

        <Text className={styles.safetyCopy}>
          本入口只完成身份确认和工作台进入；临床建议、发布、回滚等高风险动作仍需按页面流程留痕。
        </Text>
      </section>

      <Card className={styles.loginCard} bordered={false}>
        <div className={styles.cardStack}>
          <div className={styles.cardHeader}>
            <Title level={2} className={styles.cardTitle}>
              登录工作台
            </Title>
            <Text type="secondary">使用医院账号继续</Text>
          </div>

          {errorMsg && <Alert type="error" showIcon message="登录失败" description={errorMsg} />}

          <Form layout="vertical" requiredMark={false} onFinish={handleSubmit}>
            <Form.Item name="username" rules={[{ required: true, message: "请输入工号或账号" }]}>
              <Input prefix={<UserOutlined />} placeholder="工号 / 账号" size="large" />
            </Form.Item>
            <Form.Item name="password" rules={[{ required: true, message: "请输入密码" }]}>
              <Input.Password prefix={<LockOutlined />} placeholder="密码" size="large" />
            </Form.Item>
            <Form.Item name="tenantId">
              <Input placeholder="租户标识（选填，跨院区/外网租户登录时填写）" size="large" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" block size="large" loading={login.isPending}>
                进入工作台
              </Button>
            </Form.Item>
          </Form>

          <div>
            <Divider className={styles.divider}>
              <Button
                type="link"
                size="small"
                className={styles.ssoToggle}
                onClick={() => setShowSso(!showSso)}
              >
                {showSso ? "收起" : "院方统一身份认证"}
              </Button>
            </Divider>
            {showSso && (
              <div className={styles.ssoStack}>
                <Button block>用 CAS 登录</Button>
                <Button block>用 OIDC 登录</Button>
                <Button block>用 SAML 登录</Button>
                <Text type="secondary" className={styles.helperText}>
                  统一身份由医院信息中心配置。MFA / 国密 / 国产 CA 由系统按策略自动选择。
                </Text>
              </div>
            )}
          </div>

          <footer className={styles.complianceFooter}>
            <Text type="secondary" className={styles.helperText}>
              <Button type="link" size="small">
                用户协议
              </Button>
              <span aria-hidden="true"> · </span>
              <Button type="link" size="small">
                隐私政策
              </Button>
              <span aria-hidden="true"> · </span>
              <Button type="link" size="small">
                个人信息收集清单
              </Button>
            </Text>
            <Text type="secondary" className={styles.helperText}>
              ICP 备案号待填 · 公安备案号待填 · 等保 2.0 三级 · 商密评测预审中
            </Text>
          </footer>
        </div>
      </Card>
    </main>
  );
}
