import { SafetyCertificateOutlined } from "@ant-design/icons";
import { Typography } from "antd";
import { loginRuntimeConfig } from "./config";
import styles from "./styles.module.css";

const { Text } = Typography;

export function ComplianceFooter() {
  return (
    <footer className={styles.complianceFooter}>
      <nav className={styles.footerLinks} aria-label="登录合规链接">
        <a href="/legal/user-agreement">用户协议</a>
        <a href="/legal/privacy-policy">隐私政策</a>
        <a href="/forgot-password">忘记密码</a>
      </nav>
      <div className={styles.recordRow}>
        <Text type="secondary">{loginRuntimeConfig.icpNumber}</Text>
        <Text type="secondary">{loginRuntimeConfig.psbNumber}</Text>
      </div>
      <div className={styles.sessionNotice}>
        <SafetyCertificateOutlined aria-hidden="true" />
        <Text type="secondary">
          {loginRuntimeConfig.appVersion} · 会话空闲 {loginRuntimeConfig.sessionTimeoutMinutes}{" "}
          分钟后自动保护
        </Text>
      </div>
    </footer>
  );
}
