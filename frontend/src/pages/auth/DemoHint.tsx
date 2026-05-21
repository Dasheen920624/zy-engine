import { Collapse, Typography } from "antd";
import styles from "./styles.module.css";

const { Text } = Typography;

export function DemoHint() {
  return (
    <Collapse
      ghost
      className={styles.demoHint}
      items={[
        {
          key: "demo",
          label: "演示环境凭据",
          children: (
            <div className={styles.demoHintBody}>
              <Text type="secondary">账号密码：zhao01 / demo123</Text>
              <Text type="secondary">短信验证码：123456</Text>
              <Text type="secondary">演示信息仅在 demo profile 展示，生产环境由配置关闭。</Text>
            </div>
          ),
        },
      ]}
    />
  );
}
