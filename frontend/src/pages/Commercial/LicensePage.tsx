import React, { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Form,
  Input,
  Modal,
  Row,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  message,
} from "antd";
import {
  SafetyCertificateOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  CloseCircleOutlined,
} from "@ant-design/icons";
import {
  getLicenseInfo,
  updateLicense,
  type LicenseInfo,
} from "../../api/commercial";
import styles from "./commercial.module.css";

const STATUS_CONFIG: Record<string, { color: string; label: string; icon: React.ReactNode }> = {
  VALID: { color: "green", label: "有效", icon: <CheckCircleOutlined /> },
  EXPIRING_SOON: { color: "orange", label: "即将到期", icon: <WarningOutlined /> },
  EXPIRED: { color: "red", label: "已过期", icon: <CloseCircleOutlined /> },
};

const LicensePage: React.FC = () => {
  const [license, setLicense] = useState<LicenseInfo | null>(null);
  const [loading, setLoading] = useState(false);
  const [updateModalVisible, setUpdateModalVisible] = useState(false);
  const [updateForm] = Form.useForm();

  const fetchLicense = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getLicenseInfo();
      setLicense(data);
    } catch {
      message.error("获取授权信息失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchLicense();
  }, [fetchLicense]);

  const handleUpdateLicense = async (values: { license_key: string }) => {
    try {
      const data = await updateLicense(values);
      setLicense(data);
      message.success("授权更新成功");
      setUpdateModalVisible(false);
      updateForm.resetFields();
    } catch {
      message.error("授权更新失败");
    }
  };

  const statusCfg = license ? STATUS_CONFIG[license.status] ?? STATUS_CONFIG.EXPIRED : null;

  const featureColumns = [
    { title: "功能", dataIndex: "feature_name", key: "feature_name", width: 200 },
    { title: "功能标识", dataIndex: "feature_key", key: "feature_key", width: 180 },
    {
      title: "所需版本",
      dataIndex: "tier_required",
      key: "tier_required",
      width: 120,
      render: (v: string) => <Tag>{v}</Tag>,
    },
    {
      title: "可用状态",
      dataIndex: "available",
      key: "available",
      width: 100,
      render: (v: boolean) =>
        v ? (
          <Tag color="green" icon={<CheckCircleOutlined />}>可用</Tag>
        ) : (
          <Tag color="red" icon={<CloseCircleOutlined />}>不可用</Tag>
        ),
    },
  ];

  return (
    <div className={styles.page}>
      <Spin spinning={loading}>
        {/* 降级模式警告 */}
        {license?.degradation_mode && (
          <Alert
            className={styles.statusBanner}
            type="error"
            showIcon
            icon={<CloseCircleOutlined />}
            message="授权已过期，系统运行在降级模式"
            description="部分功能不可用，请尽快更新授权密钥以恢复完整功能。"
            action={
              <Button size="small" danger onClick={() => setUpdateModalVisible(true)}>
                立即更新
              </Button>
            }
          />
        )}

        {/* 即将到期警告 */}
        {license?.status === "EXPIRING_SOON" && !license.degradation_mode && (
          <Alert
            className={styles.statusBanner}
            type="warning"
            showIcon
            icon={<WarningOutlined />}
            message="授权即将到期"
            description={`授权将在 ${license.days_remaining} 天后到期，请及时续期以避免服务中断。`}
          />
        )}

        {/* 授权状态概览 */}
        <Card
          title={<Space><SafetyCertificateOutlined />授权状态</Space>}
          extra={
            <Button type="primary" onClick={() => setUpdateModalVisible(true)}>
              更新授权
            </Button>
          }
          className={styles.sectionCard}
        >
          {license && (
            <>
              <Row gutter={16}>
                <Col span={6}>
                  <Statistic
                    title="授权状态"
                    value={statusCfg?.label}
                    valueStyle={{ color: statusCfg?.color === "green" ? "var(--mk-success)" : statusCfg?.color === "orange" ? "var(--mk-warning)" : "var(--mk-danger)" }}
                    prefix={statusCfg?.icon}
                  />
                </Col>
                <Col span={6}>
                  <Statistic title="剩余天数" value={license.days_remaining} suffix="天" />
                </Col>
                <Col span={6}>
                  <Statistic title="授权类型" value={license.license_type} />
                </Col>
                <Col span={6}>
                  <Statistic title="授权层级" value={license.tier} />
                </Col>
              </Row>
              <Descriptions column={3} size="small" bordered style={{ marginTop: 16 }}>
                <Descriptions.Item label="被授权方">{license.licensee}</Descriptions.Item>
                <Descriptions.Item label="签发日期">{license.issued_date}</Descriptions.Item>
                <Descriptions.Item label="到期日期">
                  <Space>
                    {license.expiry_date}
                    <Tag color={statusCfg?.color} icon={statusCfg?.icon}>
                      {statusCfg?.label}
                    </Tag>
                  </Space>
                </Descriptions.Item>
              </Descriptions>
            </>
          )}
        </Card>

        {/* 功能可用性列表 */}
        <Card title="功能可用性" className={styles.sectionCard}>
          {license ? (
            <Table
              rowKey="feature_key"
              columns={featureColumns}
              dataSource={license.features}
              pagination={false}
              size="small"
            />
          ) : (
            <div className={styles.emptyHint}>暂无功能可用性数据</div>
          )}
        </Card>
      </Spin>

      {/* 更新授权弹窗 */}
      <Modal
        title="更新授权"
        open={updateModalVisible}
        onCancel={() => { setUpdateModalVisible(false); updateForm.resetFields(); }}
        onOk={() => updateForm.submit()}
      >
        <Form form={updateForm} layout="vertical" onFinish={handleUpdateLicense}>
          <Form.Item
            name="license_key"
            label="授权密钥"
            rules={[{ required: true, message: "请输入授权密钥" }]}
          >
            <Input.TextArea
              rows={4}
              placeholder="请输入新的授权密钥"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default LicensePage;
