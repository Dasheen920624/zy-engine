import { CopyOutlined } from "@ant-design/icons";
import { Button, Descriptions, Result, Space, Tag, Typography, message } from "antd";
import type { TenantOnboardingResult } from "./types";
import styles from "./styles.module.css";

const { Paragraph, Text } = Typography;

interface OnboardingSuccessProps {
  result: TenantOnboardingResult;
  onRestart: () => void;
}

export default function OnboardingSuccess({ result, onRestart }: OnboardingSuccessProps) {
  const copy = async (value: string) => {
    await navigator.clipboard.writeText(value);
    message.success("已复制");
  };

  const tenantId = result.application.tenantId || "待后端生成";

  return (
    <div className={styles.successWrap}>
      <Result
        status="success"
        title="租户开通完成"
        subTitle="租户申请已审批通过，管理员邀请已生成。请在客户确认后发送正式开通信。"
        extra={[
          <Button key="restart" onClick={onRestart}>
            继续开通
          </Button>,
        ]}
      />

      <Descriptions bordered column={1} className={styles.successDetails}>
        <Descriptions.Item label="申请编号">
          <Space>
            <Text code>{result.application.applicationCode}</Text>
            <Button
              type="text"
              size="small"
              icon={<CopyOutlined />}
              onClick={() => copy(result.application.applicationCode)}
            />
          </Space>
        </Descriptions.Item>
        <Descriptions.Item label="租户 ID">
          <Space>
            <Text code>{tenantId}</Text>
            <Button
              type="text"
              size="small"
              icon={<CopyOutlined />}
              onClick={() => copy(tenantId)}
            />
          </Space>
        </Descriptions.Item>
        <Descriptions.Item label="管理员账号">
          <Text code>{result.adminUsername}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="邀请码">
          <Text code>{result.invitation?.invitationCode || "待生成"}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="短信通知">
          <Tag color={result.smsNotificationStatus === "PLANNED" ? "gold" : "green"}>
            {result.smsNotificationStatus === "PLANNED" ? "已预留，等待短信通道" : "已处理"}
          </Tag>
        </Descriptions.Item>
      </Descriptions>

      <Paragraph type="secondary" className={styles.securityNote}>
        不生成或展示初始明文密码；租户管理员通过邀请链接完成实名核验、MFA 绑定和密码设置。
      </Paragraph>
    </div>
  );
}
