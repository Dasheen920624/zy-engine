import {
  DesktopOutlined,
  LockOutlined,
  MedicineBoxOutlined,
  RocketOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
  TeamOutlined,
} from "@ant-design/icons";
import type { EnvCheckItem } from "./types";

export const STEP_ITEMS = [
  { title: "环境检查", description: "检查系统环境", icon: <DesktopOutlined /> },
  { title: "组织配置", description: "科室、病区、角色", icon: <TeamOutlined /> },
  { title: "规则导入", description: "导入规则包", icon: <SafetyCertificateOutlined /> },
  { title: "路径配置", description: "配置临床路径", icon: <MedicineBoxOutlined /> },
  { title: "权限分配", description: "数据与菜单权限", icon: <LockOutlined /> },
  { title: "验证测试", description: "运行验证测试", icon: <SettingOutlined /> },
  { title: "完成上线", description: "确认上线", icon: <RocketOutlined /> },
];

export const DEFAULT_ENV_CHECKS: EnvCheckItem[] = [
  { key: "database", label: "数据库连接", passed: false, detail: "检查 PostgreSQL / H2 可用性" },
  { key: "graph", label: "图谱引擎", passed: false, detail: "检查 Neo4j / 内存图谱可用性" },
  { key: "dify", label: "AI 工作流引擎", passed: false, detail: "检查 Dify / 本地引擎可用性" },
  { key: "storage", label: "文件存储", passed: false, detail: "检查文件上传与存储服务" },
  { key: "auth", label: "认证服务", passed: false, detail: "检查 SSO / CAS / OIDC 配置" },
  { key: "network", label: "网络连通性", passed: false, detail: "检查内部服务间网络连通" },
];

export const DEPT_TYPES = [
  { value: "CLINICAL", label: "临床科室" },
  { value: "SURGICAL", label: "手术科室" },
  { value: "ICU", label: "重症医学科" },
  { value: "EMERGENCY", label: "急诊科" },
  { value: "PHARMACY", label: "药剂科" },
  { value: "LAB", label: "检验科" },
  { value: "RADIOLOGY", label: "影像科" },
  { value: "ADMIN", label: "行政科室" },
];

export const PERMISSION_TEMPLATES = [
  { code: "ADMIN_FULL", name: "系统管理员", description: "全部数据权限和菜单权限" },
  { code: "QC_MANAGER", name: "质控管理员", description: "质控规则管理、预警处理、评估配置" },
  { code: "PATHWAY_EDITOR", name: "路径编辑员", description: "临床路径编辑、发布、变异管理" },
  { code: "DEPT_HEAD", name: "科室主任", description: "本科室数据查看、质控统计" },
  { code: "DOCTOR", name: "临床医生", description: "患者路径查看、医嘱录入" },
  { code: "VIEWER", name: "只读观察者", description: "仅查看权限，无编辑操作" },
];

export const SAMPLE_PATHWAYS = [
  { code: "AMI_STEMI", name: "急性 ST 段抬高型心肌梗死", specialty: "心内科", enabled: true },
  { code: "PNEUMONIA_COMMUNITY", name: "社区获得性肺炎", specialty: "呼吸内科", enabled: true },
  { code: "STROKE_ISCHEMIC", name: "缺血性脑卒中", specialty: "神经内科", enabled: false },
  { code: "HIP_REPLACEMENT", name: "髋关节置换术", specialty: "骨科", enabled: false },
  { code: "CESAREAN_SECTION", name: "剖宫产术", specialty: "产科", enabled: false },
];
