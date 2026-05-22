import { SettingOutlined } from "@ant-design/icons";
import { Alert, Button, Card, Empty, Typography } from "antd";
import styles from "../../ImplementationGuidePage.module.css";

const { Paragraph } = Typography;

interface Step5ValidationProps {
  validating: boolean;
  validationResult: { passed: boolean; details: string[] } | null;
  runValidation: () => void;
}

export default function Step5Validation({ validating, validationResult, runValidation }: Step5ValidationProps) {
  return (
    <Card title="验证测试">
      <Paragraph type="secondary" className={styles.marginBottom16}>
        运行验证测试，确保所有配置项正确无误。
      </Paragraph>
      {validationResult ? (
        <Alert
          type={validationResult.passed ? "success" : "warning"}
          message={validationResult.passed ? "验证测试通过" : "验证测试未完全通过"}
          description={
            <ul className={styles.resultList}>
              {validationResult.details.map((d, i) => (
                <li key={i}>{d}</li>
              ))}
            </ul>
          }
          showIcon
          className={styles.marginBottom16}
        />
      ) : (
        <Empty description="点击下方按钮运行验证测试" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      )}
      <div className={styles.textCenter}>
        <Button type="primary" icon={<SettingOutlined />} onClick={runValidation} loading={validating} size="large">
          运行验证测试
        </Button>
      </div>
    </Card>
  );
}
