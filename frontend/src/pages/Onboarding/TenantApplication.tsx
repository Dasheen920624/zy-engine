import React, { useState } from 'react';
import { Card, Form, Input, Select, Button, Space, Typography, Steps, message, Result } from 'antd';
import { 
  BankOutlined, 
  UserOutlined, 
  PhoneOutlined, 
  MailOutlined, 
  EnvironmentOutlined,
  FileTextOutlined,
  CheckCircleOutlined 
} from '@ant-design/icons';

const { Title, Paragraph, Text } = Typography;
const { Option } = Select;
const { TextArea } = Input;
const { Step } = Steps;

const TenantApplication: React.FC = () => {
  const [form] = Form.useForm();
  const [currentStep, setCurrentStep] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [applicationCode, setApplicationCode] = useState<string | null>(null);

  // 提交申请
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      
      // 实际项目中应该调用API
      console.log('Submitting application:', values);
      
      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      const code = 'APP-' + Date.now();
      setApplicationCode(code);
      setCurrentStep(1);
      message.success('申请提交成功！');
    } catch (error) {
      message.error('请填写完整信息');
    } finally {
      setSubmitting(false);
    }
  };

  // 步骤1：填写申请信息
  const renderStep0 = () => (
    <Form form={form} layout="vertical">
      <Card title="企业信息" style={{ marginBottom: 16 }}>
        <Form.Item
          name="companyName"
          label="企业/机构名称"
          rules={[{ required: true, message: '请输入企业名称' }]}
        >
          <Input prefix={<BankOutlined />} placeholder="请输入企业或医疗机构名称" />
        </Form.Item>

        <Form.Item
          name="companyType"
          label="机构类型"
          rules={[{ required: true, message: '请选择机构类型' }]}
        >
          <Select placeholder="请选择机构类型">
            <Option value="HOSPITAL">医院</Option>
            <Option value="CLINIC">诊所</Option>
            <Option value="PHARMA">药企</Option>
            <Option value="INSURANCE">保险公司</Option>
            <Option value="OTHER">其他</Option>
          </Select>
        </Form.Item>

        <Form.Item
          name="licenseNumber"
          label="医疗机构执业许可证号"
        >
          <Input prefix={<FileTextOutlined />} placeholder="请输入许可证号（如有）" />
        </Form.Item>

        <Form.Item
          name="licenseType"
          label="授权类型"
          rules={[{ required: true, message: '请选择授权类型' }]}
        >
          <Select placeholder="请选择授权类型">
            <Option value="TRIAL">试用版（30天）</Option>
            <Option value="STANDARD">标准版</Option>
            <Option value="ENTERPRISE">企业版</Option>
          </Select>
        </Form.Item>
      </Card>

      <Card title="联系人信息" style={{ marginBottom: 16 }}>
        <Form.Item
          name="contactName"
          label="联系人姓名"
          rules={[{ required: true, message: '请输入联系人姓名' }]}
        >
          <Input prefix={<UserOutlined />} placeholder="请输入联系人姓名" />
        </Form.Item>

        <Form.Item
          name="contactTitle"
          label="职务"
        >
          <Input placeholder="请输入联系人职务" />
        </Form.Item>

        <Form.Item
          name="contactPhone"
          label="联系电话"
          rules={[{ required: true, message: '请输入联系电话' }]}
        >
          <Input prefix={<PhoneOutlined />} placeholder="请输入联系电话" />
        </Form.Item>

        <Form.Item
          name="contactEmail"
          label="联系邮箱"
          rules={[
            { required: true, message: '请输入联系邮箱' },
            { type: 'email', message: '请输入有效的邮箱地址' }
          ]}
        >
          <Input prefix={<MailOutlined />} placeholder="请输入联系邮箱" />
        </Form.Item>
      </Card>

      <Card title="地址信息" style={{ marginBottom: 16 }}>
        <Form.Item
          name="province"
          label="省份"
        >
          <Input prefix={<EnvironmentOutlined />} placeholder="请输入省份" />
        </Form.Item>

        <Form.Item
          name="city"
          label="城市"
        >
          <Input placeholder="请输入城市" />
        </Form.Item>

        <Form.Item
          name="address"
          label="详细地址"
        >
          <Input placeholder="请输入详细地址" />
        </Form.Item>
      </Card>

      <Card title="业务需求" style={{ marginBottom: 16 }}>
        <Form.Item
          name="expectedUsers"
          label="预计用户数"
        >
          <Select placeholder="请选择预计用户数">
            <Option value="1-10">1-10人</Option>
            <Option value="11-50">11-50人</Option>
            <Option value="51-100">51-100人</Option>
            <Option value="101-500">101-500人</Option>
            <Option value="500+">500人以上</Option>
          </Select>
        </Form.Item>

        <Form.Item
          name="businessNeeds"
          label="业务需求说明"
        >
          <TextArea rows={4} placeholder="请简要说明您的业务需求和使用场景" />
        </Form.Item>
      </Card>

      <div style={{ textAlign: 'center' }}>
        <Button 
          type="primary" 
          size="large" 
          onClick={handleSubmit}
          loading={submitting}
        >
          提交申请
        </Button>
      </div>
    </Form>
  );

  // 步骤2：申请成功
  const renderStep1 = () => (
    <Result
      status="success"
      icon={<CheckCircleOutlined />}
      title="申请提交成功！"
      subTitle={
        <div>
          <Paragraph>
            您的申请编号为：<Text strong>{applicationCode}</Text>
          </Paragraph>
          <Paragraph>
            我们将在 1-3 个工作日内审核您的申请，审核结果将通过邮件通知您。
          </Paragraph>
        </div>
      }
      extra={[
        <Button type="primary" key="home" onClick={() => window.location.href = '/'}>
          返回首页
        </Button>,
        <Button key="new" onClick={() => {
          setCurrentStep(0);
          form.resetFields();
          setApplicationCode(null);
        }}>
          继续申请
        </Button>
      ]}
    />
  );

  return (
    <div style={{ padding: 24, maxWidth: 800, margin: '0 auto' }}>
      <Card>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Title level={3}>客户租户开通申请</Title>
          <Paragraph type="secondary">
            填写以下信息申请开通 MedKernel 平台账号
          </Paragraph>
        </div>

        <Steps current={currentStep} style={{ marginBottom: 24 }}>
          <Step title="填写申请" />
          <Step title="等待审核" />
          <Step title="开通完成" />
        </Steps>

        {currentStep === 0 && renderStep0()}
        {currentStep === 1 && renderStep1()}
      </Card>
    </div>
  );
};

export default TenantApplication;