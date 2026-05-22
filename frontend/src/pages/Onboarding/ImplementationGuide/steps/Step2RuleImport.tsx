import { CloudUploadOutlined } from "@ant-design/icons";
import { Button, Card, Checkbox, Col, Empty, Row, Space, Spin, Tag, Typography } from "antd";
import type { ConfigPackageSummary } from "../../../../api/types";
import styles from "../../ImplementationGuidePage.module.css";

const { Paragraph, Text } = Typography;

interface Step2RuleImportProps {
  rulePackages: ConfigPackageSummary[];
  selectedRulePackages: string[];
  loadingRules: boolean;
  importingRules: boolean;
  loadRulePackages: () => void;
  importSelectedRules: () => void;
  onSelectedRulePackagesChange: (vals: string[]) => void;
  openConfigWizard: (type: "org" | "rule" | "permission") => void;
}

export default function Step2RuleImport({
  rulePackages,
  selectedRulePackages,
  loadingRules,
  importingRules,
  loadRulePackages,
  importSelectedRules,
  onSelectedRulePackagesChange,
  openConfigWizard,
}: Step2RuleImportProps) {
  return (
    <Card
      title="规则导入"
      extra={
        <Space>
          <Button onClick={loadRulePackages} loading={loadingRules}>
            刷新列表
          </Button>
          <Button type="link" onClick={() => openConfigWizard("rule")}>
            规则配置向导
          </Button>
        </Space>
      }
    >
      <Paragraph type="secondary" className={styles.marginBottom16}>
        选择并导入质控规则包、路径规则包等，为系统提供规则引擎支持。
      </Paragraph>

      <Spin spinning={loadingRules}>
        {rulePackages.length === 0 ? (
          <Empty description="暂无可用规则包" />
        ) : (
          <>
            <Checkbox.Group
              value={selectedRulePackages}
              onChange={(vals) => onSelectedRulePackagesChange(vals as string[])}
              className={styles.fullWidth}
            >
              <Row gutter={[12, 12]}>
                {rulePackages.map((pkg) => (
                  <Col span={12} key={pkg.package_code}>
                    <Card size="small" hoverable>
                      <Space direction="vertical" size={4} className={styles.fullWidth}>
                        <Space>
                          <Checkbox value={pkg.package_code} />
                          <Text strong>{pkg.package_code}</Text>
                          <Tag>{pkg.package_version}</Tag>
                          <Tag color="green">{pkg.status}</Tag>
                        </Space>
                        <Text type="secondary" className={styles.textSmall}>
                          作用域: {pkg.scope_level} / {pkg.scope_code}
                        </Text>
                      </Space>
                    </Card>
                  </Col>
                ))}
              </Row>
            </Checkbox.Group>
            <div className={`${styles.marginTop16} ${styles.textRight}`}>
              <Text type="secondary" className={styles.marginRight12}>
                已选择 {selectedRulePackages.length} / {rulePackages.length} 个规则包
              </Text>
              <Button
                type="primary"
                icon={<CloudUploadOutlined />}
                onClick={importSelectedRules}
                loading={importingRules}
                disabled={selectedRulePackages.length === 0}
              >
                导入选中规则包
              </Button>
            </div>
          </>
        )}
      </Spin>
    </Card>
  );
}
