import {
  ArrowLeftOutlined,
  ArrowRightOutlined,
  CheckOutlined,
  ShopOutlined,
} from "@ant-design/icons";
import { Button, Form, Space, Steps, Typography, message } from "antd";
import { useEffect, useMemo, useState } from "react";
import {
  approveTenantApplication,
  sendTenantAdminInvitation,
  submitTenantApplication,
} from "../../../api/tenantOnboarding";
import type {
  TenantInfoInput,
  TenantInitDataInput,
  TenantOnboardingSubmitInput,
  TenantSubscriptionInput,
} from "../../../api/tenantOnboarding";
import OnboardingSuccess from "./OnboardingSuccess";
import Step1Info from "./steps/Step1Info";
import Step2Subscription from "./steps/Step2Subscription";
import Step3InitData from "./steps/Step3InitData";
import styles from "./styles.module.css";
import type { OnboardingDraft, TenantOnboardingResult } from "./types";

const { Paragraph, Title } = Typography;

const defaultInitData: TenantInitDataInput = {
  defaultPackageCodes: ["AMI_STEMI_BASELINE", "QC_GROUP_KPI", "SECURITY_BASELINE"],
  ssoProviderTypes: ["CAS", "OIDC"],
  dataLocalizationConfirmed: true,
  smsNotificationEnabled: true,
};

function adminUsernameFrom(info: TenantInfoInput) {
  return `${info.tenantCode.toLowerCase()}_admin`;
}

export default function OnboardingWizard() {
  const [current, setCurrent] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [draft, setDraft] = useState<OnboardingDraft>({});
  const [result, setResult] = useState<TenantOnboardingResult | null>(null);
  const [infoForm] = Form.useForm<TenantInfoInput>();
  const [subscriptionForm] = Form.useForm<TenantSubscriptionInput>();
  const [initForm] = Form.useForm<TenantInitDataInput>();

  useEffect(() => {
    initForm.setFieldsValue(defaultInitData);
  }, [initForm]);

  const steps = useMemo(
    () => [
      { title: "租户信息", description: "医院、编码、联系人" },
      { title: "套餐选择", description: "授权、容量、属地承诺" },
      { title: "初始化", description: "配置包、SSO、通知" },
    ],
    [],
  );

  const next = async () => {
    if (current === 0) {
      const info = await infoForm.validateFields();
      setDraft((prev) => ({ ...prev, info }));
    }
    if (current === 1) {
      const subscription = await subscriptionForm.validateFields();
      setDraft((prev) => ({ ...prev, subscription }));
    }
    setCurrent((value) => value + 1);
  };

  const previous = () => {
    setCurrent((value) => Math.max(0, value - 1));
  };

  const submit = async () => {
    const initData = await initForm.validateFields();
    const info = draft.info || (await infoForm.validateFields());
    const subscription = draft.subscription || (await subscriptionForm.validateFields());
    const payload: TenantOnboardingSubmitInput = {
      ...info,
      ...subscription,
      ...initData,
    };

    setSubmitting(true);
    try {
      const application = await submitTenantApplication(payload);
      const approved = await approveTenantApplication(application.applicationCode);
      const tenantId = approved.tenantId || info.tenantCode;
      const invitation = await sendTenantAdminInvitation({
        tenantId,
        email: info.contactEmail,
        phone: info.contactPhone,
        invitedBy: "platform-admin",
      });
      setResult({
        application: approved,
        invitation,
        adminUsername: adminUsernameFrom(info),
        smsNotificationStatus: initData.smsNotificationEnabled ? "PLANNED" : "DISABLED",
      });
      message.success("租户开通完成");
    } catch (error) {
      const msg = error instanceof Error ? error.message : "租户开通失败";
      message.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const restart = () => {
    infoForm.resetFields();
    subscriptionForm.resetFields();
    initForm.setFieldsValue(defaultInitData);
    setDraft({});
    setResult(null);
    setCurrent(0);
  };

  if (result) {
    return <OnboardingSuccess result={result} onRestart={restart} />;
  }

  return (
    <main className={styles.page}>
      <section className={styles.header}>
        <div>
          <Space className={styles.eyebrow}>
            <ShopOutlined />
            <span>用户与身份 / 租户开通</span>
          </Space>
          <Title level={2}>租户开通向导</Title>
          <Paragraph>
            面向医院集团、单体医院和试点客户的干净开通流程，默认本地化部署和租户隔离。
          </Paragraph>
        </div>
      </section>

      <section className={styles.wizardSurface}>
        <Steps current={current} items={steps} className={styles.steps} />

        <div className={styles.stepBody}>
          {current === 0 && <Step1Info form={infoForm} />}
          {current === 1 && <Step2Subscription form={subscriptionForm} />}
          {current === 2 && <Step3InitData form={initForm} />}
        </div>

        <div className={styles.actions}>
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={previous}
            disabled={current === 0 || submitting}
          >
            上一步
          </Button>
          {current < steps.length - 1 ? (
            <Button type="primary" icon={<ArrowRightOutlined />} onClick={next}>
              下一步
            </Button>
          ) : (
            <Button type="primary" icon={<CheckOutlined />} loading={submitting} onClick={submit}>
              开通租户
            </Button>
          )}
        </div>
      </section>
    </main>
  );
}
