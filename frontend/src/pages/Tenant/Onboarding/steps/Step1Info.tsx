import {
  BankOutlined,
  EnvironmentOutlined,
  MailOutlined,
  PhoneOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { Card, Col, Form, Input, Row, Select } from "antd";
import type { FormInstance } from "antd";
import type { TenantInfoInput } from "../../../../api/tenantOnboarding";
import styles from "../styles.module.css";

interface Step1InfoProps {
  form: FormInstance<TenantInfoInput>;
}

export default function Step1Info({ form }: Step1InfoProps) {
  return (
    <Form form={form} layout="vertical" requiredMark="optional" className={styles.formGrid}>
      <Card title="租户信息" className={styles.sectionCard}>
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Form.Item
              name="companyName"
              label="医院 / 集团名称"
              rules={[{ required: true, message: "请输入医院或集团名称" }]}
            >
              <Input prefix={<BankOutlined />} placeholder="如：华东示范医院集团" />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item
              name="tenantCode"
              label="租户编码"
              rules={[
                { required: true, message: "请输入租户编码" },
                {
                  pattern: /^[A-Z0-9_-]{3,32}$/,
                  message: "仅支持 3-32 位大写字母、数字、下划线或连字符",
                },
              ]}
            >
              <Input placeholder="如：HD_DEMO_HOSPITAL" />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item
              name="companyType"
              label="机构类型"
              rules={[{ required: true, message: "请选择机构类型" }]}
            >
              <Select
                placeholder="请选择"
                options={[
                  { value: "HOSPITAL_GROUP", label: "医院集团" },
                  { value: "HOSPITAL", label: "单体医院" },
                  { value: "CLINIC", label: "专科/诊所" },
                  { value: "INSURANCE", label: "医保/商保机构" },
                ]}
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="licenseNumber" label="医疗机构执业许可证号">
              <Input placeholder="选填，用于合规备案" />
            </Form.Item>
          </Col>
        </Row>
      </Card>

      <Card title="联系人" className={styles.sectionCard}>
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Form.Item
              name="contactName"
              label="联系人姓名"
              rules={[{ required: true, message: "请输入联系人姓名" }]}
            >
              <Input prefix={<UserOutlined />} placeholder="信息科 / 医务处联系人" />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="contactTitle" label="职务">
              <Input placeholder="如：信息科主任" />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item
              name="contactPhone"
              label="手机号"
              rules={[{ required: true, message: "请输入手机号" }]}
            >
              <Input prefix={<PhoneOutlined />} placeholder="用于开通通知和 MFA 预留" />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item
              name="contactEmail"
              label="邮箱"
              rules={[
                { required: true, message: "请输入邮箱" },
                { type: "email", message: "请输入有效邮箱" },
              ]}
            >
              <Input prefix={<MailOutlined />} placeholder="用于管理员邀请" />
            </Form.Item>
          </Col>
        </Row>
      </Card>

      <Card title="属地信息" className={styles.sectionCard}>
        <Row gutter={16}>
          <Col xs={24} md={8}>
            <Form.Item name="province" label="省份">
              <Input prefix={<EnvironmentOutlined />} placeholder="如：浙江省" />
            </Form.Item>
          </Col>
          <Col xs={24} md={8}>
            <Form.Item name="city" label="城市">
              <Input placeholder="如：杭州市" />
            </Form.Item>
          </Col>
          <Col xs={24} md={8}>
            <Form.Item name="address" label="详细地址">
              <Input placeholder="院区或总部地址" />
            </Form.Item>
          </Col>
        </Row>
      </Card>
    </Form>
  );
}
