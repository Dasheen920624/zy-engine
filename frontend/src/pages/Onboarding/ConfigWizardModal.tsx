import {
  CloudUploadOutlined,
  DeleteOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
} from "@ant-design/icons";
import {
  Button,
  Checkbox,
  Col,
  Divider,
  Empty,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Transfer,
  Typography,
  message,
} from "antd";
import { useMemo, useState } from "react";
import type { ConfigPackageSummary } from "../../api/types";

const { Text, Paragraph } = Typography;

// ─── 类型 ────────────────────────────────────────────────────

interface DepartmentInput {
  code: string;
  name: string;
  type: string;
}

interface RoleInput {
  code: string;
  name: string;
  permissions: string[];
}

interface ConfigWizardModalProps {
  visible: boolean;
  type: "org" | "rule" | "permission";
  onClose: () => void;
  onDepartmentsChange: (departments: DepartmentInput[]) => void;
  onRolesChange: (roles: RoleInput[]) => void;
  onRulePackagesChange: (packages: string[]) => void;
  onPermTemplatesChange: (templates: string[]) => void;
  departments: DepartmentInput[];
  roles: RoleInput[];
  availableRulePackages: ConfigPackageSummary[];
  selectedRulePackages: string[];
  selectedPermTemplates: string[];
}

// ─── 科室类型选项 ─────────────────────────────────────────────

const DEPT_TYPES = [
  { value: "CLINICAL", label: "临床科室" },
  { value: "SURGICAL", label: "手术科室" },
  { value: "ICU", label: "重症医学科" },
  { value: "EMERGENCY", label: "急诊科" },
  { value: "PHARMACY", label: "药剂科" },
  { value: "LAB", label: "检验科" },
  { value: "RADIOLOGY", label: "影像科" },
  { value: "ADMIN", label: "行政科室" },
];

// ─── 预设科室模板 ─────────────────────────────────────────────

const PRESET_DEPARTMENTS: DepartmentInput[] = [
  { code: "CARDIOLOGY", name: "心内科", type: "CLINICAL" },
  { code: "RESPIRATORY", name: "呼吸内科", type: "CLINICAL" },
  { code: "NEUROLOGY", name: "神经内科", type: "CLINICAL" },
  { code: "GASTROENTEROLOGY", name: "消化内科", type: "CLINICAL" },
  { code: "ORTHOPEDICS", name: "骨科", type: "SURGICAL" },
  { code: "GENERAL_SURG", name: "普外科", type: "SURGICAL" },
  { code: "ICU", name: "重症医学科", type: "ICU" },
  { code: "EMERGENCY", name: "急诊科", type: "EMERGENCY" },
  { code: "PHARMACY", name: "药剂科", type: "PHARMACY" },
  { code: "LAB", name: "检验科", type: "LAB" },
  { code: "RADIOLOGY", name: "影像科", type: "RADIOLOGY" },
  { code: "OBSTETRICS", name: "产科", type: "CLINICAL" },
];

// ─── 预设角色模板 ─────────────────────────────────────────────

const PRESET_ROLES: RoleInput[] = [
  { code: "TENANT_ADMIN", name: "租户管理员", permissions: ["*"] },
  { code: "QC_MANAGER", name: "质控管理员", permissions: ["qc:*", "rule:read", "alert:*"] },
  { code: "PATHWAY_EDITOR", name: "路径编辑员", permissions: ["pathway:*", "rule:read"] },
  { code: "DEPT_HEAD", name: "科室主任", permissions: ["qc:read", "pathway:read", "alert:read"] },
  { code: "DOCTOR", name: "临床医生", permissions: ["pathway:read", "patient:read"] },
  { code: "VIEWER", name: "只读观察者", permissions: ["*:read"] },
];

// ─── 权限模板 ────────────────────────────────────────────────

const PERMISSION_TEMPLATES = [
  { code: "ADMIN_FULL", name: "系统管理员", description: "全部数据权限和菜单权限", permissions: ["*"] },
  { code: "QC_MANAGER", name: "质控管理员", description: "质控规则管理、预警处理、评估配置", permissions: ["qc:*", "rule:*", "alert:*"] },
  { code: "PATHWAY_EDITOR", name: "路径编辑员", description: "临床路径编辑、发布、变异管理", permissions: ["pathway:*"] },
  { code: "DEPT_HEAD", name: "科室主任", description: "本科室数据查看、质控统计", permissions: ["qc:read", "pathway:read", "dept:read"] },
  { code: "DOCTOR", name: "临床医生", description: "患者路径查看、医嘱录入", permissions: ["patient:*", "pathway:read"] },
  { code: "VIEWER", name: "只读观察者", description: "仅查看权限，无编辑操作", permissions: ["*:read"] },
];

// ─── 组件 ────────────────────────────────────────────────────

export default function ConfigWizardModal({
  visible,
  type,
  onClose,
  onDepartmentsChange,
  onRolesChange,
  onRulePackagesChange,
  onPermTemplatesChange,
  departments,
  roles,
  availableRulePackages,
  selectedRulePackages,
  selectedPermTemplates,
}: ConfigWizardModalProps) {
  // 组织配置向导状态
  const [orgForm] = Form.useForm();
  const [localDepts, setLocalDepts] = useState<DepartmentInput[]>(departments);
  const [localRoles, setLocalRoles] = useState<RoleInput[]>(roles);

  // 规则配置向导状态
  const [localSelectedRules, setLocalSelectedRules] = useState<string[]>(selectedRulePackages);

  // 权限配置向导状态
  const [localSelectedPerms, setLocalSelectedPerms] = useState<string[]>(selectedPermTemplates);

  // 重置本地状态当弹窗打开
  const handleOpen = () => {
    setLocalDepts([...departments]);
    setLocalRoles([...roles]);
    setLocalSelectedRules([...selectedRulePackages]);
    setLocalSelectedPerms([...selectedPermTemplates]);
  };

  // 添加科室
  const addDept = () => {
    const values = orgForm.getFieldsValue();
    if (!values.code || !values.name) {
      message.warning("请填写科室编码和名称");
      return;
    }
    if (localDepts.some((d) => d.code === values.code)) {
      message.warning("科室编码已存在");
      return;
    }
    setLocalDepts((prev) => [...prev, { code: values.code, name: values.name, type: values.type || "CLINICAL" }]);
    orgForm.resetFields();
  };

  // 批量导入预设科室
  const importPresetDepts = () => {
    const existingCodes = new Set(localDepts.map((d) => d.code));
    const newDepts = PRESET_DEPARTMENTS.filter((d) => !existingCodes.has(d.code));
    setLocalDepts((prev) => [...prev, ...newDepts]);
    message.success(`已导入 ${newDepts.length} 个预设科室`);
  };

  // 删除科室
  const removeDept = (code: string) => {
    setLocalDepts((prev) => prev.filter((d) => d.code !== code));
  };

  // 添加角色
  const addRole = () => {
    const values = orgForm.getFieldValue("roleCode");
    const name = orgForm.getFieldValue("roleName");
    if (!values || !name) {
      message.warning("请填写角色编码和名称");
      return;
    }
    if (localRoles.some((r) => r.code === values)) {
      message.warning("角色编码已存在");
      return;
    }
    setLocalRoles((prev) => [...prev, { code: values, name, permissions: [] }]);
    orgForm.setFieldsValue({ roleCode: undefined, roleName: undefined });
  };

  // 批量导入预设角色
  const importPresetRoles = () => {
    const existingCodes = new Set(localRoles.map((r) => r.code));
    const newRoles = PRESET_ROLES.filter((r) => !existingCodes.has(r.code));
    setLocalRoles((prev) => [...prev, ...newRoles]);
    message.success(`已导入 ${newRoles.length} 个预设角色`);
  };

  // 删除角色
  const removeRole = (code: string) => {
    setLocalRoles((prev) => prev.filter((r) => r.code !== code));
  };

  // Transfer 数据源（规则包）
  const ruleTransferDataSource = useMemo(
    () =>
      availableRulePackages.map((pkg) => ({
        key: pkg.package_code,
        title: pkg.package_code,
        description: `${pkg.package_version} · ${pkg.scope_level}`,
      })),
    [availableRulePackages],
  );

  // Transfer 数据源（权限模板）
  const permTransferDataSource = useMemo(
    () =>
      PERMISSION_TEMPLATES.map((tpl) => ({
        key: tpl.code,
        title: tpl.name,
        description: tpl.description,
      })),
    [],
  );

  // 确认保存
  const handleOk = () => {
    switch (type) {
      case "org":
        onDepartmentsChange(localDepts);
        onRolesChange(localRoles);
        message.success("组织配置已保存");
        break;
      case "rule":
        onRulePackagesChange(localSelectedRules);
        message.success("规则配置已保存");
        break;
      case "permission":
        onPermTemplatesChange(localSelectedPerms);
        message.success("权限配置已保存");
        break;
    }
    onClose();
  };

  // ─── 组织配置向导内容 ──────────────────────────────────

  const renderOrgWizard = () => (
    <div>
      <Tabs
        items={[
          {
            key: "departments",
            label: (
              <Space>
                <TeamOutlined />
                科室管理
              </Space>
            ),
            children: (
              <div>
                <div style={{ marginBottom: 12, display: "flex", justifyContent: "space-between" }}>
                  <Paragraph type="secondary">添加或批量导入科室</Paragraph>
                  <Button icon={<PlusOutlined />} onClick={importPresetDepts}>
                    导入预设科室
                  </Button>
                </div>
                <Form form={orgForm} layout="inline" style={{ marginBottom: 12 }}>
                  <Form.Item name="code">
                    <Input placeholder="科室编码" size="small" />
                  </Form.Item>
                  <Form.Item name="name">
                    <Input placeholder="科室名称" size="small" />
                  </Form.Item>
                  <Form.Item name="type" initialValue="CLINICAL">
                    <Select size="small" style={{ width: 120 }} options={DEPT_TYPES} />
                  </Form.Item>
                  <Button size="small" type="primary" onClick={addDept}>
                    添加
                  </Button>
                </Form>
                {localDepts.length === 0 ? (
                  <Empty description="暂无科室，请添加或导入" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                ) : (
                  <Table
                    size="small"
                    pagination={false}
                    dataSource={localDepts}
                    rowKey="code"
                    scroll={{ y: 240 }}
                    columns={[
                      { title: "编码", dataIndex: "code", width: 120 },
                      { title: "名称", dataIndex: "name" },
                      {
                        title: "类型",
                        dataIndex: "type",
                        width: 100,
                        render: (v: string) => DEPT_TYPES.find((d) => d.value === v)?.label ?? v,
                      },
                      {
                        title: "操作",
                        width: 60,
                        render: (_: unknown, r: DepartmentInput) => (
                          <Button type="link" danger size="small" icon={<DeleteOutlined />} onClick={() => removeDept(r.code)} />
                        ),
                      },
                    ]}
                  />
                )}
              </div>
            ),
          },
          {
            key: "roles",
            label: (
              <Space>
                <SafetyCertificateOutlined />
                角色管理
              </Space>
            ),
            children: (
              <div>
                <div style={{ marginBottom: 12, display: "flex", justifyContent: "space-between" }}>
                  <Paragraph type="secondary">添加或批量导入角色</Paragraph>
                  <Button icon={<PlusOutlined />} onClick={importPresetRoles}>
                    导入预设角色
                  </Button>
                </div>
                <Form form={orgForm} layout="inline" style={{ marginBottom: 12 }}>
                  <Form.Item name="roleCode">
                    <Input placeholder="角色编码" size="small" />
                  </Form.Item>
                  <Form.Item name="roleName">
                    <Input placeholder="角色名称" size="small" />
                  </Form.Item>
                  <Button size="small" type="primary" onClick={addRole}>
                    添加
                  </Button>
                </Form>
                {localRoles.length === 0 ? (
                  <Empty description="暂无角色，请添加或导入" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                ) : (
                  <Table
                    size="small"
                    pagination={false}
                    dataSource={localRoles}
                    rowKey="code"
                    scroll={{ y: 240 }}
                    columns={[
                      { title: "编码", dataIndex: "code", width: 140 },
                      { title: "名称", dataIndex: "name" },
                      {
                        title: "权限数",
                        dataIndex: "permissions",
                        width: 80,
                        render: (v: string[]) => (
                          <Tag>{v?.includes("*") ? "全部" : `${v?.length ?? 0} 项`}</Tag>
                        ),
                      },
                      {
                        title: "操作",
                        width: 60,
                        render: (_: unknown, r: RoleInput) => (
                          <Button type="link" danger size="small" icon={<DeleteOutlined />} onClick={() => removeRole(r.code)} />
                        ),
                      },
                    ]}
                  />
                )}
              </div>
            ),
          },
        ]}
      />
    </div>
  );

  // ─── 规则配置向导内容 ──────────────────────────────────

  const renderRuleWizard = () => (
    <div>
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        选择要导入的规则包，使用穿梭框将规则包从左侧移到右侧。
      </Paragraph>
      {availableRulePackages.length === 0 ? (
        <Empty description="暂无可用规则包" />
      ) : (
        <Transfer
          dataSource={ruleTransferDataSource}
          targetKeys={localSelectedRules}
          onChange={(targetKeys) => setLocalSelectedRules(targetKeys as string[])}
          render={(item) => (
            <Space>
              <Text strong>{item.title}</Text>
              <Text type="secondary" style={{ fontSize: 12 }}>
                {item.description}
              </Text>
            </Space>
          )}
          listStyle={{ width: 320, height: 360 }}
          titles={["可用规则包", "已选规则包"]}
          showSearch
          filterOption={(inputValue, item) =>
            item?.title?.toLowerCase().includes(inputValue.toLowerCase()) ?? false
          }
        />
      )}
      <Divider />
      <div style={{ textAlign: "right" }}>
        <Text type="secondary">已选择 {localSelectedRules.length} 个规则包</Text>
      </div>
    </div>
  );

  // ─── 权限配置向导内容 ──────────────────────────────────

  const renderPermissionWizard = () => (
    <div>
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        选择要应用的权限模板，使用穿梭框将模板从左侧移到右侧。
      </Paragraph>
      <Transfer
        dataSource={permTransferDataSource}
        targetKeys={localSelectedPerms}
        onChange={(targetKeys) => setLocalSelectedPerms(targetKeys as string[])}
        render={(item) => (
          <Space direction="vertical" size={0}>
            <Text strong>{item.title}</Text>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {item.description}
            </Text>
          </Space>
        )}
        listStyle={{ width: 320, height: 360 }}
        titles={["可用模板", "已选模板"]}
        showSearch
        filterOption={(inputValue, item) =>
          item?.title?.toLowerCase().includes(inputValue.toLowerCase()) ?? false
        }
      />
      <Divider />
      <div>
        <Text strong>已选模板详情：</Text>
        {localSelectedPerms.length === 0 ? (
          <Text type="secondary"> 未选择任何模板</Text>
        ) : (
          <Row gutter={[8, 8]} style={{ marginTop: 8 }}>
            {PERMISSION_TEMPLATES.filter((tpl) => localSelectedPerms.includes(tpl.code)).map((tpl) => (
              <Col span={12} key={tpl.code}>
                <Checkbox checked disabled>
                  <Space direction="vertical" size={0}>
                    <Text>{tpl.name}</Text>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {tpl.description}
                    </Text>
                  </Space>
                </Checkbox>
              </Col>
            ))}
          </Row>
        )}
      </div>
    </div>
  );

  // ─── 标题映射 ──────────────────────────────────────────

  const titleMap = {
    org: "组织配置向导",
    rule: "规则配置向导",
    permission: "权限配置向导",
  };

  const iconMap = {
    org: <TeamOutlined />,
    rule: <CloudUploadOutlined />,
    permission: <SafetyCertificateOutlined />,
  };

  const contentMap = {
    org: renderOrgWizard,
    rule: renderRuleWizard,
    permission: renderPermissionWizard,
  };

  return (
    <Modal
      title={
        <Space>
          {iconMap[type]}
          {titleMap[type]}
        </Space>
      }
      open={visible}
      afterOpenChange={(open) => {
        if (open) handleOpen();
      }}
      onOk={handleOk}
      onCancel={onClose}
      okText="确认保存"
      cancelText="取消"
      width={800}
      destroyOnClose
    >
      {contentMap[type]()}
    </Modal>
  );
}
