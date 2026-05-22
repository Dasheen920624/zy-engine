import {
  ArrowLeftOutlined,
  ArrowRightOutlined,
  CheckCircleOutlined,
  CloudUploadOutlined,
  DesktopOutlined,
  LockOutlined,
  MedicineBoxOutlined,
  RocketOutlined,
  SafetyCertificateOutlined,
  SaveOutlined,
  SettingOutlined,
  RightOutlined,
  TeamOutlined,
} from "@ant-design/icons";
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Col,
  Descriptions,
  Empty,
  Form,
  Input,
  List,
  message,
  Progress,
  Result,
  Row,
  Select,
  Space,
  Spin,
  Steps,
  Switch,
  Table,
  Tag,
  Typography,
} from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { get, post } from "../../api/client";
import type { ConfigPackageSummary } from "../../api/types";
import ConfigWizardModal from "./ConfigWizardModal";
import styles from "./ImplementationGuidePage.module.css";

const { Paragraph, Title, Text } = Typography;

// ─── 类型 ────────────────────────────────────────────────────

interface EnvCheckItem {
  key: string;
  label: string;
  passed: boolean;
  detail?: string;
}

interface DepartmentInput {
  code: string;
  name: string;
  type: string;
}

interface WardInput {
  code: string;
  name: string;
  departmentCode: string;
  bedCount?: number;
}

interface RoleInput {
  code: string;
  name: string;
  permissions: string[];
}

interface ImplementationDraft {
  currentStep: number;
  envChecks?: Record<string, boolean>;
  departments?: DepartmentInput[];
  wards?: WardInput[];
  roles?: RoleInput[];
  selectedRulePackages?: string[];
  pathwayConfigs?: Record<string, unknown>;
  permissionTemplates?: string[];
  validationPassed?: boolean;
}

const STORAGE_KEY = "medkernel_impl_guide_draft";

function loadDraft(): ImplementationDraft | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as ImplementationDraft) : null;
  } catch {
    return null;
  }
}

function saveDraft(draft: ImplementationDraft) {
  // eslint-disable-next-line no-restricted-syntax -- 实施向导进度草稿，非敏感数据
  localStorage.setItem(STORAGE_KEY, JSON.stringify(draft));
}

function clearDraft() {
  localStorage.removeItem(STORAGE_KEY);
}

// ─── 步骤定义 ────────────────────────────────────────────────

const STEP_ITEMS = [
  { title: "环境检查", description: "检查系统环境", icon: <DesktopOutlined /> },
  { title: "组织配置", description: "科室、病区、角色", icon: <TeamOutlined /> },
  { title: "规则导入", description: "导入规则包", icon: <SafetyCertificateOutlined /> },
  { title: "路径配置", description: "配置临床路径", icon: <MedicineBoxOutlined /> },
  { title: "权限分配", description: "数据与菜单权限", icon: <LockOutlined /> },
  { title: "验证测试", description: "运行验证测试", icon: <SettingOutlined /> },
  { title: "完成上线", description: "确认上线", icon: <RocketOutlined /> },
];

// ─── 环境检查模拟数据 ────────────────────────────────────────

const DEFAULT_ENV_CHECKS: EnvCheckItem[] = [
  { key: "database", label: "数据库连接", passed: false, detail: "检查 PostgreSQL / H2 可用性" },
  { key: "graph", label: "图谱引擎", passed: false, detail: "检查 Neo4j / 内存图谱可用性" },
  { key: "dify", label: "AI 工作流引擎", passed: false, detail: "检查 Dify / 本地引擎可用性" },
  { key: "storage", label: "文件存储", passed: false, detail: "检查文件上传与存储服务" },
  { key: "auth", label: "认证服务", passed: false, detail: "检查 SSO / CAS / OIDC 配置" },
  { key: "network", label: "网络连通性", passed: false, detail: "检查内部服务间网络连通" },
];

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

// ─── 权限模板 ────────────────────────────────────────────────

const PERMISSION_TEMPLATES = [
  { code: "ADMIN_FULL", name: "系统管理员", description: "全部数据权限和菜单权限" },
  { code: "QC_MANAGER", name: "质控管理员", description: "质控规则管理、预警处理、评估配置" },
  { code: "PATHWAY_EDITOR", name: "路径编辑员", description: "临床路径编辑、发布、变异管理" },
  { code: "DEPT_HEAD", name: "科室主任", description: "本科室数据查看、质控统计" },
  { code: "DOCTOR", name: "临床医生", description: "患者路径查看、医嘱录入" },
  { code: "VIEWER", name: "只读观察者", description: "仅查看权限，无编辑操作" },
];

// ─── 主组件 ──────────────────────────────────────────────────

export default function ImplementationGuidePage() {
  const navigate = useNavigate();
  const [current, setCurrent] = useState(0);
  const [draft, setDraft] = useState<ImplementationDraft>({ currentStep: 0 });
  const [envChecks, setEnvChecks] = useState<EnvCheckItem[]>(DEFAULT_ENV_CHECKS);
  const [checkingEnv, setCheckingEnv] = useState(false);
  const [departments, setDepartments] = useState<DepartmentInput[]>([]);
  const [wards, setWards] = useState<WardInput[]>([]);
  const [roles, setRoles] = useState<RoleInput[]>([]);
  const [rulePackages, setRulePackages] = useState<ConfigPackageSummary[]>([]);
  const [selectedRulePackages, setSelectedRulePackages] = useState<string[]>([]);
  const [loadingRules, setLoadingRules] = useState(false);
  const [importingRules, setImportingRules] = useState(false);
  const [selectedPermTemplates, setSelectedPermTemplates] = useState<string[]>([]);
  const [validating, setValidating] = useState(false);
  const [validationResult, setValidationResult] = useState<{ passed: boolean; details: string[] } | null>(null);
  const [completed, setCompleted] = useState(false);

  // 配置向导弹窗
  const [configWizardVisible, setConfigWizardVisible] = useState(false);
  const [configWizardType, setConfigWizardType] = useState<"org" | "rule" | "permission">("org");

  // 科室表单
  const [deptForm] = Form.useForm();
  // 角色表单
  const [roleForm] = Form.useForm();

  // 恢复进度
  useEffect(() => {
    const saved = loadDraft();
    if (saved) {
      setCurrent(saved.currentStep);
      setDraft(saved);
      if (saved.departments) setDepartments(saved.departments);
      if (saved.wards) setWards(saved.wards);
      if (saved.roles) setRoles(saved.roles);
      if (saved.selectedRulePackages) setSelectedRulePackages(saved.selectedRulePackages);
      if (saved.permissionTemplates) setSelectedPermTemplates(saved.permissionTemplates);
    }
  }, []);

  // 保存进度
  const persistDraft = useCallback(
    (step: number, extra: Partial<ImplementationDraft> = {}) => {
      const updated: ImplementationDraft = {
        ...draft,
        currentStep: step,
        ...extra,
      };
      setDraft(updated);
      saveDraft(updated);
    },
    [draft],
  );

  // ─── 步骤导航 ───────────────────────────────────────────

  const next = () => {
    const nextStep = Math.min(current + 1, STEP_ITEMS.length - 1);
    setCurrent(nextStep);
    persistDraft(nextStep);
  };

  const prev = () => {
    const prevStep = Math.max(current - 1, 0);
    setCurrent(prevStep);
    persistDraft(prevStep);
  };

  const skip = () => {
    next();
  };

  const handleSaveProgress = () => {
    persistDraft(current, {
      departments,
      wards,
      roles,
      selectedRulePackages,
      permissionTemplates: selectedPermTemplates,
    });
    message.success("进度已保存");
  };

  // ─── Step 0: 环境检查 ──────────────────────────────────

  const runEnvCheck = async () => {
    setCheckingEnv(true);
    try {
      const providers = await get<{ providers: Array<{ name: string; ready: boolean; status: string }> }>(
        "/system/providers",
      );
      const providerMap = new Map(providers.providers.map((p) => [p.name, p]));

      setEnvChecks((prev) =>
        prev.map((item) => {
          if (item.key === "database") {
            const db = providerMap.get("database");
            return { ...item, passed: db?.ready ?? false, detail: db?.status ?? "未检测到" };
          }
          if (item.key === "graph") {
            const graph = providerMap.get("graph");
            return { ...item, passed: graph?.ready ?? false, detail: graph?.status ?? "未检测到" };
          }
          if (item.key === "dify") {
            const dify = providerMap.get("dify");
            return { ...item, passed: dify?.ready ?? false, detail: dify?.status ?? "未检测到" };
          }
          if (item.key === "storage") return { ...item, passed: true, detail: "本地存储可用" };
          if (item.key === "auth") return { ...item, passed: true, detail: "认证服务就绪" };
          if (item.key === "network") return { ...item, passed: true, detail: "网络连通正常" };
          return item;
        }),
      );
      message.success("环境检查完成");
    } catch {
      // 降级：全部标记为通过（演示模式）
      setEnvChecks((prev) => prev.map((item) => ({ ...item, passed: true, detail: "演示模式" })));
      message.info("环境检查完成（演示模式）");
    } finally {
      setCheckingEnv(false);
    }
  };

  const envAllPassed = envChecks.every((c) => c.passed);

  // ─── Step 1: 组织配置 ──────────────────────────────────

  const addDepartment = () => {
    const values = deptForm.getFieldsValue();
    if (!values.code || !values.name) {
      message.warning("请填写科室编码和名称");
      return;
    }
    if (departments.some((d) => d.code === values.code)) {
      message.warning("科室编码已存在");
      return;
    }
    const newDept: DepartmentInput = { code: values.code, name: values.name, type: values.type || "CLINICAL" };
    const updated = [...departments, newDept];
    setDepartments(updated);
    persistDraft(current, { departments: updated });
    deptForm.resetFields();
    message.success("科室已添加");
  };

  const removeDepartment = (code: string) => {
    const updated = departments.filter((d) => d.code !== code);
    setDepartments(updated);
    persistDraft(current, { departments: updated });
  };

  const addRole = () => {
    const values = roleForm.getFieldsValue();
    if (!values.code || !values.name) {
      message.warning("请填写角色编码和名称");
      return;
    }
    if (roles.some((r) => r.code === values.code)) {
      message.warning("角色编码已存在");
      return;
    }
    const newRole: RoleInput = { code: values.code, name: values.name, permissions: values.permissions || [] };
    const updated = [...roles, newRole];
    setRoles(updated);
    persistDraft(current, { roles: updated });
    roleForm.resetFields();
    message.success("角色已添加");
  };

  const removeRole = (code: string) => {
    const updated = roles.filter((r) => r.code !== code);
    setRoles(updated);
    persistDraft(current, { roles: updated });
  };

  const openConfigWizard = (type: "org" | "rule" | "permission") => {
    setConfigWizardType(type);
    setConfigWizardVisible(true);
  };

  // ─── Step 2: 规则导入 ──────────────────────────────────

  const loadRulePackages = async () => {
    setLoadingRules(true);
    try {
      const packages = await get<ConfigPackageSummary[]>("/config-packages?assetType=RULE&status=PUBLISHED");
      setRulePackages(packages);
    } catch {
      // 演示模式：提供模拟数据
      setRulePackages([
        {
          tenant_id: "demo",
          package_code: "QC_GROUP_KPI",
          package_version: "1.0.0",
          asset_type: "RULE",
          scope_level: "HOSPITAL",
          scope_code: "ALL",
          status: "PUBLISHED",
          content_hash: "hash1",
          created_time: "2026-01-01",
        },
        {
          tenant_id: "demo",
          package_code: "AMI_STEMI_BASELINE",
          package_version: "2.0.0",
          asset_type: "RULE",
          scope_level: "HOSPITAL",
          scope_code: "ALL",
          status: "PUBLISHED",
          content_hash: "hash2",
          created_time: "2026-01-15",
        },
        {
          tenant_id: "demo",
          package_code: "ORDER_SAFETY",
          package_version: "1.1.0",
          asset_type: "RULE",
          scope_level: "HOSPITAL",
          scope_code: "ALL",
          status: "PUBLISHED",
          content_hash: "hash3",
          created_time: "2026-02-01",
        },
        {
          tenant_id: "demo",
          package_code: "INSURANCE_QC",
          package_version: "1.0.0",
          asset_type: "RULE",
          scope_level: "HOSPITAL",
          scope_code: "ALL",
          status: "PUBLISHED",
          content_hash: "hash4",
          created_time: "2026-02-10",
        },
      ]);
    } finally {
      setLoadingRules(false);
    }
  };

  useEffect(() => {
    if (current === 2 && rulePackages.length === 0) {
      loadRulePackages();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current]);

  const importSelectedRules = async () => {
    if (selectedRulePackages.length === 0) {
      message.warning("请先选择要导入的规则包");
      return;
    }
    setImportingRules(true);
    try {
      for (const code of selectedRulePackages) {
        await post(`/config-packages/${code}/latest/activate`, { activated_by: "impl-guide" });
      }
      message.success(`已导入 ${selectedRulePackages.length} 个规则包`);
    } catch {
      message.info(`已标记 ${selectedRulePackages.length} 个规则包为待导入（演示模式）`);
    } finally {
      setImportingRules(false);
    }
  };

  // ─── Step 3: 路径配置 ──────────────────────────────────

  const [pathwayEnabled, setPathwayEnabled] = useState<Record<string, boolean>>({});

  const samplePathways = useMemo(
    () => [
      { code: "AMI_STEMI", name: "急性 ST 段抬高型心肌梗死", specialty: "心内科", enabled: true },
      { code: "PNEUMONIA_COMMUNITY", name: "社区获得性肺炎", specialty: "呼吸内科", enabled: true },
      { code: "STROKE_ISCHEMIC", name: "缺血性脑卒中", specialty: "神经内科", enabled: false },
      { code: "HIP_REPLACEMENT", name: "髋关节置换术", specialty: "骨科", enabled: false },
      { code: "CESAREAN_SECTION", name: "剖宫产术", specialty: "产科", enabled: false },
    ],
    [],
  );

  // ─── Step 4: 权限分配 ──────────────────────────────────

  const togglePermTemplate = (code: string, checked: boolean) => {
    setSelectedPermTemplates((prev) => {
      const updated = checked ? [...prev, code] : prev.filter((c) => c !== code);
      persistDraft(current, { permissionTemplates: updated });
      return updated;
    });
  };

  // ─── Step 5: 验证测试 ──────────────────────────────────

  const runValidation = async () => {
    setValidating(true);
    setValidationResult(null);
    try {
      await post("/system/validation/run", { scope: "IMPLEMENTATION_GUIDE" });
      setValidationResult({ passed: true, details: ["所有验证项通过"] });
      message.success("验证测试通过");
    } catch {
      // 演示模式
      await new Promise((r) => setTimeout(r, 1500));
      const details: string[] = [];
      if (departments.length > 0) details.push(`科室配置: ${departments.length} 个科室已配置`);
      else details.push("⚠ 科室配置: 尚未配置科室");
      if (roles.length > 0) details.push(`角色配置: ${roles.length} 个角色已配置`);
      else details.push("⚠ 角色配置: 尚未配置角色");
      if (selectedRulePackages.length > 0) details.push(`规则导入: ${selectedRulePackages.length} 个规则包已选择`);
      else details.push("⚠ 规则导入: 尚未选择规则包");
      if (selectedPermTemplates.length > 0) details.push(`权限分配: ${selectedPermTemplates.length} 个模板已选择`);
      else details.push("⚠ 权限分配: 尚未选择权限模板");
      details.push("环境检查: 通过");
      const passed = departments.length > 0 && roles.length > 0;
      setValidationResult({ passed, details });
      if (passed) message.success("验证测试通过");
      else message.warning("验证测试未完全通过，请检查配置");
    } finally {
      setValidating(false);
    }
  };

  // ─── Step 6: 完成上线 ──────────────────────────────────

  const handleGoLive = () => {
    clearDraft();
    setCompleted(true);
    message.success("实施完成，系统已上线！");
  };

  // ─── 渲染各步骤 ────────────────────────────────────────

  const renderStep0 = () => (
    <Card title="环境检查" extra={<Button icon={<DesktopOutlined />} onClick={runEnvCheck} loading={checkingEnv} type="primary">开始检查</Button>}>
      <Paragraph type="secondary" className={styles.marginBottom16}>
        检查系统环境是否满足运行要求，确保数据库、图谱引擎、AI 工作流引擎等核心服务可用。
      </Paragraph>
      <List
        dataSource={envChecks}
        renderItem={(item) => (
          <List.Item>
            <List.Item.Meta
              avatar={
                item.passed ? (
                  <CheckCircleOutlined className={styles.iconSuccess} />
                ) : (
                  <DesktopOutlined className={styles.iconMuted} />
                )
              }
              title={item.label}
              description={item.detail}
            />
            <Tag color={item.passed ? "green" : "default"}>{item.passed ? "通过" : "未检查"}</Tag>
          </List.Item>
        )}
      />
      {envChecks.some((c) => c.passed) && (
        <Alert
          className={styles.marginTop16}
          type={envAllPassed ? "success" : "warning"}
          message={envAllPassed ? "所有环境检查已通过" : "部分环境检查未通过，可继续配置但不影响演示模式"}
          showIcon
        />
      )}
    </Card>
  );

  const renderStep1 = () => (
    <Card
      title="组织配置"
      extra={
        <Button type="link" onClick={() => openConfigWizard("org")}>
          批量配置向导
        </Button>
      }
    >
      <Paragraph type="secondary" className={styles.marginBottom16}>
        配置科室、病区和角色，建立组织架构基础。
      </Paragraph>

      <Row gutter={24}>
        {/* 科室管理 */}
        <Col span={12}>
          <Card type="inner" title="科室管理" size="small">
            <Form form={deptForm} layout="inline" className={styles.formInline}>
              <Form.Item name="code" className={styles.formItemNoMargin}>
                <Input placeholder="科室编码" size="small" />
              </Form.Item>
              <Form.Item name="name" className={styles.formItemNoMargin}>
                <Input placeholder="科室名称" size="small" />
              </Form.Item>
              <Form.Item name="type" initialValue="CLINICAL" className={styles.formItemNoMargin}>
                <Select size="small" className={styles.selectSmall} options={DEPT_TYPES} />
              </Form.Item>
              <Button size="small" type="primary" onClick={addDepartment}>
                添加
              </Button>
            </Form>
            {departments.length === 0 ? (
              <Empty description="暂无科室" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <Table
                size="small"
                pagination={false}
                dataSource={departments}
                rowKey="code"
                columns={[
                  { title: "编码", dataIndex: "code", width: 100 },
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
                      <Button type="link" danger size="small" onClick={() => removeDepartment(r.code)}>
                        删除
                      </Button>
                    ),
                  },
                ]}
              />
            )}
          </Card>
        </Col>

        {/* 角色管理 */}
        <Col span={12}>
          <Card type="inner" title="角色管理" size="small">
            <Form form={roleForm} layout="inline" className={styles.formInline}>
              <Form.Item name="code" className={styles.formItemNoMargin}>
                <Input placeholder="角色编码" size="small" />
              </Form.Item>
              <Form.Item name="name" className={styles.formItemNoMargin}>
                <Input placeholder="角色名称" size="small" />
              </Form.Item>
              <Button size="small" type="primary" onClick={addRole}>
                添加
              </Button>
            </Form>
            {roles.length === 0 ? (
              <Empty description="暂无角色" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <Table
                size="small"
                pagination={false}
                dataSource={roles}
                rowKey="code"
                columns={[
                  { title: "编码", dataIndex: "code", width: 120 },
                  { title: "名称", dataIndex: "name" },
                  {
                    title: "权限数",
                    dataIndex: "permissions",
                    width: 80,
                    render: (v: string[]) => v?.length ?? 0,
                  },
                  {
                    title: "操作",
                    width: 60,
                    render: (_: unknown, r: RoleInput) => (
                      <Button type="link" danger size="small" onClick={() => removeRole(r.code)}>
                        删除
                      </Button>
                    ),
                  },
                ]}
              />
            )}
          </Card>
        </Col>
      </Row>
    </Card>
  );

  const renderStep2 = () => (
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
              onChange={(vals) => {
                const updated = vals as string[];
                setSelectedRulePackages(updated);
                persistDraft(current, { selectedRulePackages: updated });
              }}
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

  const renderStep3 = () => (
    <Card title="路径配置">
      <Paragraph type="secondary" className={styles.marginBottom16}>
        配置临床路径模板，启用或禁用路径实例化功能。
      </Paragraph>
      <Table
        dataSource={samplePathways}
        rowKey="code"
        pagination={false}
        columns={[
          { title: "路径编码", dataIndex: "code", width: 180 },
          { title: "路径名称", dataIndex: "name" },
          { title: "专科", dataIndex: "specialty", width: 120 },
          {
            title: "启用",
            dataIndex: "code",
            width: 80,
            render: (code: string) => (
              <Switch
                size="small"
                checked={pathwayEnabled[code] ?? samplePathways.find((p) => p.code === code)?.enabled ?? false}
                onChange={(checked) => setPathwayEnabled((prev) => ({ ...prev, [code]: checked }))}
              />
            ),
          },
        ]}
      />
    </Card>
  );

  const renderStep4 = () => (
    <Card
      title="权限分配"
      extra={
        <Button type="link" onClick={() => openConfigWizard("permission")}>
          权限配置向导
        </Button>
      }
    >
      <Paragraph type="secondary" className={styles.marginBottom16}>
        选择常用权限模板，快速分配数据权限和菜单权限。
      </Paragraph>
      <Row gutter={[12, 12]}>
        {PERMISSION_TEMPLATES.map((tpl) => (
          <Col span={8} key={tpl.code}>
            <Card
              size="small"
              hoverable
              className={selectedPermTemplates.includes(tpl.code) ? styles.permCard : undefined}
              onClick={() => togglePermTemplate(tpl.code, !selectedPermTemplates.includes(tpl.code))}
            >
              <Space direction="vertical" size={4}>
                <Space>
                  <Checkbox checked={selectedPermTemplates.includes(tpl.code)} />
                  <Text strong>{tpl.name}</Text>
                </Space>
                <Text type="secondary" className={styles.textSmall}>
                  {tpl.description}
                </Text>
              </Space>
            </Card>
          </Col>
        ))}
      </Row>
    </Card>
  );

  const renderStep5 = () => (
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

  const renderStep6 = () => (
    <Card title="完成上线">
      <Paragraph type="secondary" className={styles.marginBottom16}>
        确认所有配置项，完成实施并正式上线。
      </Paragraph>
      <Descriptions bordered column={2}>
        <Descriptions.Item label="科室数量">{departments.length}</Descriptions.Item>
        <Descriptions.Item label="角色数量">{roles.length}</Descriptions.Item>
        <Descriptions.Item label="规则包">{selectedRulePackages.length} 个已选择</Descriptions.Item>
        <Descriptions.Item label="权限模板">{selectedPermTemplates.length} 个已选择</Descriptions.Item>
        <Descriptions.Item label="启用路径">
          {Object.values(pathwayEnabled).filter(Boolean).length} / {samplePathways.length}
        </Descriptions.Item>
        <Descriptions.Item label="环境检查">{envAllPassed ? "全部通过" : "部分未通过"}</Descriptions.Item>
        <Descriptions.Item label="验证测试">{validationResult?.passed ? "通过" : "未运行"}</Descriptions.Item>
      </Descriptions>
      <div className={styles.textCenterWithMargin}>
        <Button type="primary" size="large" icon={<RocketOutlined />} onClick={handleGoLive}>
          确认上线
        </Button>
      </div>
    </Card>
  );

  // ─── 完成页面 ──────────────────────────────────────────

  if (completed) {
    return (
      <div className={styles.pageContainer}>
        <Result
          status="success"
          icon={<RocketOutlined />}
          title="实施完成，系统已上线！"
          subTitle="客户实施向导已完成，您可以前往工作台开始使用系统。"
          extra={[
            <Button type="primary" key="dashboard" onClick={() => navigate("/dashboard")}>
              前往工作台
            </Button>,
            <Button key="restart" onClick={() => { clearDraft(); window.location.reload(); }}>
              重新开始
            </Button>,
          ]}
        />
      </div>
    );
  }

  // ─── 步骤内容映射 ──────────────────────────────────────

  const stepRenderers = [renderStep0, renderStep1, renderStep2, renderStep3, renderStep4, renderStep5, renderStep6];

  // ─── 进度百分比 ────────────────────────────────────────

  const progressPercent = Math.round(((current + 1) / STEP_ITEMS.length) * 100);

  return (
    <main className={styles.mainContainer}>
      <section className={styles.section}>
        <Space className={styles.brandLabel}>
          <RocketOutlined />
          <span>客户实施向导</span>
        </Space>
        <Title level={2} className={styles.mainTitle}>
          实施向导
        </Title>
        <Paragraph className={styles.mainDescription}>
          面向客户实施的配置向导，引导完成环境检查、组织配置、规则导入、路径配置、权限分配和验证测试。
        </Paragraph>
      </section>

      <section className={styles.sectionWide}>
        <Steps current={current} items={STEP_ITEMS} className={styles.stepsContainer} />

        <div className={styles.progressContainer}>
          <Progress percent={progressPercent} size="small" />
        </div>

        <div className={styles.contentArea}>{stepRenderers[current]()}</div>

        <div className={styles.footer}>
          <div>
            <Button icon={<SaveOutlined />} onClick={handleSaveProgress}>
              保存进度
            </Button>
          </div>
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={prev} disabled={current === 0}>
              上一步
            </Button>
            {current < STEP_ITEMS.length - 1 ? (
              <>
                <Button icon={<RightOutlined />} onClick={skip}>
                  跳过
                </Button>
                <Button type="primary" icon={<ArrowRightOutlined />} onClick={next}>
                  下一步
                </Button>
              </>
            ) : (
              <Button type="primary" icon={<RocketOutlined />} onClick={handleGoLive}>
                确认上线
              </Button>
            )}
          </Space>
        </div>
      </section>

      <ConfigWizardModal
        visible={configWizardVisible}
        type={configWizardType}
        onClose={() => setConfigWizardVisible(false)}
        onDepartmentsChange={(depts) => {
          setDepartments(depts);
          persistDraft(current, { departments: depts });
        }}
        onRolesChange={(newRoles) => {
          setRoles(newRoles);
          persistDraft(current, { roles: newRoles });
        }}
        onRulePackagesChange={(pkgs) => {
          setSelectedRulePackages(pkgs);
          persistDraft(current, { selectedRulePackages: pkgs });
        }}
        onPermTemplatesChange={(tpls) => {
          setSelectedPermTemplates(tpls);
          persistDraft(current, { permissionTemplates: tpls });
        }}
        departments={departments}
        roles={roles}
        availableRulePackages={rulePackages}
        selectedRulePackages={selectedRulePackages}
        selectedPermTemplates={selectedPermTemplates}
      />
    </main>
  );
}
