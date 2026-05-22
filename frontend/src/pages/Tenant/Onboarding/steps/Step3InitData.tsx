import { ClusterOutlined, DatabaseOutlined, SafetyCertificateOutlined } from "@ant-design/icons";
import { Alert, Card, Checkbox, Form, Space, Switch, Typography } from "antd";
import type { FormInstance } from "antd";
import type { TenantInitDataInput } from "../../../../api/tenantOnboarding";
import styles from "../styles.module.css";

const { Text } = Typography;

interface Step3InitDataProps {
  form: FormInstance<TenantInitDataInput>;
}

export default function Step3InitData({ form }: Step3InitDataProps) {
  return (
    <Form form={form} layout="vertical" requiredMark="optional">
      <Alert
        className={styles.complianceAlert}
        type="info"
        showIcon
        message="初始化只创建干净基线"
        description="当前系统未上线，租户开通只基于最新标准配置包初始化，不迁就历史脏表、临时数据或旧兼容路径。"
      />

      <Card className={styles.sectionCard} title="默认配置包">
        <Form.Item
          name="defaultPackageCodes"
          label="初始化配置包"
          rules={[{ required: true, message: "至少选择一个配置包" }]}
        >
          <Checkbox.Group className={styles.checkboxGrid}>
            <Checkbox value="AMI_STEMI_BASELINE">
              <Space>
                <DatabaseOutlined />
                <span>AMI STEMI 基线包</span>
              </Space>
            </Checkbox>
            <Checkbox value="QC_GROUP_KPI">
              <Space>
                <DatabaseOutlined />
                <span>集团质控 KPI 包</span>
              </Space>
            </Checkbox>
            <Checkbox value="SECURITY_BASELINE">
              <Space>
                <SafetyCertificateOutlined />
                <span>安全基线包</span>
              </Space>
            </Checkbox>
          </Checkbox.Group>
        </Form.Item>
      </Card>

      <Card className={styles.sectionCard} title="身份源预置">
        <Form.Item name="ssoProviderTypes" label="预置 SSO 提供商">
          <Checkbox.Group className={styles.checkboxGrid}>
            <Checkbox value="CAS">
              <Space>
                <ClusterOutlined />
                <span>CAS</span>
              </Space>
            </Checkbox>
            <Checkbox value="OIDC">
              <Space>
                <ClusterOutlined />
                <span>OIDC</span>
              </Space>
            </Checkbox>
            <Checkbox value="LDAP_AD">
              <Space>
                <ClusterOutlined />
                <span>LDAP / AD</span>
              </Space>
            </Checkbox>
          </Checkbox.Group>
        </Form.Item>

        <div className={styles.switchRows}>
          <Form.Item
            name="dataLocalizationConfirmed"
            valuePropName="checked"
            rules={[
              {
                validator: (_, value: boolean) =>
                  value ? Promise.resolve() : Promise.reject(new Error("请确认数据本地化承诺")),
              },
            ]}
          >
            <Switch checkedChildren="已确认" unCheckedChildren="待确认" />
          </Form.Item>
          <Text>确认租户数据默认落在客户指定地域 / 院内环境，未审批不出域。</Text>
        </div>

        <div className={styles.switchRows}>
          <Form.Item name="smsNotificationEnabled" valuePropName="checked">
            <Switch checkedChildren="预留" unCheckedChildren="关闭" />
          </Form.Item>
          <Text>开通成功短信通知依赖 PR-V3-COMPLIANCE-BACKEND，当前先记录预留状态。</Text>
        </div>
      </Card>
    </Form>
  );
}
