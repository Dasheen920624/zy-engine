import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  SafetyCertificateOutlined,
} from "@ant-design/icons";
import { Alert, Card, Form, Radio, Space, Typography } from "antd";
import type { FormInstance } from "antd";
import type { TenantSubscriptionInput } from "../../../../api/tenantOnboarding";
import styles from "../styles.module.css";

const { Text } = Typography;

interface Step2SubscriptionProps {
  form: FormInstance<TenantSubscriptionInput>;
}

const planOptions = [
  {
    value: "TRIAL",
    title: "试用",
    price: "30 天评估",
    desc: "适合售前演示、院内 PoC 和数据脱敏试点。",
    features: ["30 天到期提醒", "演示知识包", "基础 Provider 状态"],
  },
  {
    value: "STANDARD",
    title: "标准",
    price: "按项目合同",
    desc: "适合单院部署，覆盖知识工厂与质控驾驶舱。",
    features: ["本地化部署", "标准配置包", "基础 SSO"],
  },
  {
    value: "PROFESSIONAL",
    title: "专业",
    price: "集团试点",
    desc: "适合多院区、多科室协同治理。",
    features: ["多组织治理", "AI 知识审核", "实施协作"],
  },
  {
    value: "ENTERPRISE",
    title: "旗舰",
    price: "集团正式上线",
    desc: "适合集团级 SLA、合规评测和专属对接。",
    features: ["专属实施", "高级集成", "SLA 保障"],
  },
];

export default function Step2Subscription({ form }: Step2SubscriptionProps) {
  return (
    <Form form={form} layout="vertical" requiredMark="optional">
      <Alert
        className={styles.complianceAlert}
        type="success"
        showIcon
        icon={<SafetyCertificateOutlined />}
        message="数据本地化承诺"
        description="所有套餐默认遵循院内本地化部署与租户隔离原则；未完成数据出境评估前，不接收真实患者隐私数据到公网环境。"
      />

      <Form.Item name="licenseType" rules={[{ required: true, message: "请选择套餐" }]}>
        <Radio.Group className={styles.planGrid}>
          {planOptions.map((plan) => (
            <Radio.Button key={plan.value} value={plan.value} className={styles.planOption}>
              <Space direction="vertical" size={10} className={styles.planContent}>
                <span className={styles.planHeader}>
                  <Text strong>{plan.title}</Text>
                  <Text type="secondary">{plan.price}</Text>
                </span>
                <Text type="secondary">{plan.desc}</Text>
                <span className={styles.featureList}>
                  {plan.features.map((feature) => (
                    <span key={feature} className={styles.featureItem}>
                      <CheckCircleOutlined />
                      {feature}
                    </span>
                  ))}
                </span>
              </Space>
            </Radio.Button>
          ))}
        </Radio.Group>
      </Form.Item>

      <Card className={styles.sectionCard} title="容量与试用提示">
        <Form.Item
          name="expectedUsers"
          label="预计用户规模"
          rules={[{ required: true, message: "请选择预计用户规模" }]}
        >
          <Radio.Group
            optionType="button"
            buttonStyle="solid"
            options={[
              { value: "1-50", label: "1-50" },
              { value: "51-200", label: "51-200" },
              { value: "201-1000", label: "201-1000" },
              { value: "1000+", label: "1000+" },
            ]}
          />
        </Form.Item>
        <Form.Item name="businessNeeds" label="开通目标">
          <Radio.Group
            options={[
              { value: "PILOT_QC", label: "质控试点" },
              { value: "KNOWLEDGE_FACTORY", label: "知识工厂" },
              { value: "GROUP_ROLLOUT", label: "集团推广" },
            ]}
          />
        </Form.Item>
        <div className={styles.trialHint}>
          <ClockCircleOutlined />
          <span>选择试用套餐时，系统会在开通成功页标记 30 天到期，并预留短信提醒。</span>
        </div>
      </Card>
    </Form>
  );
}
