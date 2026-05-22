import { CheckCircleOutlined, DesktopOutlined } from "@ant-design/icons";
import { Alert, Button, Card, List, Tag, Typography } from "antd";
import type { EnvCheckItem } from "../types";
import styles from "../../ImplementationGuidePage.module.css";

const { Paragraph } = Typography;

interface Step0EnvCheckProps {
  envChecks: EnvCheckItem[];
  checkingEnv: boolean;
  runEnvCheck: () => void;
  envAllPassed: boolean;
}

export default function Step0EnvCheck({ envChecks, checkingEnv, runEnvCheck, envAllPassed }: Step0EnvCheckProps) {
  return (
    <Card title="环境检查" extra={<Button icon={<DesktopOutlined />} onClick={runEnvCheck} loading={checkingEnv} type="primary">开始检查</Button>}>
      <Paragraph type="secondary" className={styles.marginBottom16}>
        检查系统环境是否满足运行要求，确保数据库、图谱引擎、AI 工作流引擎等核心服务可用。
      </Paragraph>
      <List
        dataSource={envChecks}
        renderItem={(item) => (
          <List.Item>
            <List.Item.Meta
              avatar={
                item.passed ? (
                  <CheckCircleOutlined className={styles.iconSuccess} />
                ) : (
                  <DesktopOutlined className={styles.iconMuted} />
                )
              }
              title={item.label}
              description={item.detail}
            />
            <Tag color={item.passed ? "green" : "default"}>{item.passed ? "通过" : "未检查"}</Tag>
          </List.Item>
        )}
      />
      {envChecks.some((c) => c.passed) && (
        <Alert
          className={styles.marginTop16}
          type={envAllPassed ? "success" : "warning"}
          message={envAllPassed ? "所有环境检查已通过" : "部分环境检查未通过，可继续配置但不影响演示模式"}
          showIcon
        />
      )}
    </Card>
  );
}
