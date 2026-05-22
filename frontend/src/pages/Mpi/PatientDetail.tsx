import { Alert, Button, Descriptions, Empty, Space, Switch, Table, Tag, Typography } from "antd";
import { CheckCircleOutlined, EyeOutlined, LinkOutlined } from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import type { PatientIdentity, VisitIdentity } from "../../api/mpi";
import type { MpiPatientRecord } from "./helpers";
import {
  formatTime,
  maskExternalId,
  maskIdCard,
  maskPhone,
  patientIdentityTypeLabel,
  statusColor,
  statusLabel,
  visitIdentityTypeLabel,
} from "./helpers";
import styles from "./styles.module.css";

const { Text, Title } = Typography;

export interface PatientDetailProps {
  patient?: MpiPatientRecord;
  identities: PatientIdentity[];
  visits: VisitIdentity[];
  loading?: boolean;
  showSensitive: boolean;
  onRevealChange: (checked: boolean) => void;
  onVerifyIdentity: (identityId: number) => void;
}

export default function PatientDetail({
  patient,
  identities,
  visits,
  loading = false,
  showSensitive,
  onRevealChange,
  onVerifyIdentity,
}: PatientDetailProps) {
  const identityColumns: ColumnsType<PatientIdentity> = [
    {
      title: "标识类型",
      dataIndex: "identity_type",
      key: "identity_type",
      render: (type: string) => patientIdentityTypeLabel(type),
    },
    {
      title: "外部 ID",
      dataIndex: "external_id",
      key: "external_id",
      render: (_: string, record) => <span className={styles.mono}>{maskExternalId(record, showSensitive)}</span>,
    },
    {
      title: "来源系统",
      dataIndex: "source_system",
      key: "source_system",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      render: (status: string) => <Tag color={statusColor(status)}>{statusLabel(status)}</Tag>,
    },
    {
      title: "置信度",
      dataIndex: "confidence",
      key: "confidence",
      render: (confidence?: number) => (typeof confidence === "number" ? `${confidence}%` : "—"),
    },
    {
      title: "核验",
      key: "verify",
      render: (_, record) =>
        record.manually_verified ? (
          <Tag icon={<CheckCircleOutlined />} color="success">
            已核验
          </Tag>
        ) : (
          <Button size="small" onClick={() => onVerifyIdentity(record.id)}>
            人工核验
          </Button>
        ),
    },
  ];

  const visitColumns: ColumnsType<VisitIdentity> = [
    {
      title: "平台就诊 ID",
      dataIndex: "platform_visit_id",
      key: "platform_visit_id",
      render: (value: string) => <span className={styles.mono}>{value}</span>,
    },
    {
      title: "就诊类型",
      dataIndex: "visit_type",
      key: "visit_type",
    },
    {
      title: "外部标识",
      key: "external",
      render: (_, record) => (
        <span>
          {visitIdentityTypeLabel(record.identity_type)} · <span className={styles.mono}>{maskExternalId(record as unknown as PatientIdentity, showSensitive)}</span>
        </span>
      ),
    },
    {
      title: "日期",
      dataIndex: "visit_date",
      key: "visit_date",
      render: (value?: string) => value || "—",
    },
    {
      title: "科室",
      dataIndex: "department_code",
      key: "department_code",
      render: (value?: string) => value || "—",
    },
  ];

  if (!patient) {
    return (
      <section className={styles.detailPanel} aria-label="患者详情">
        <Empty description="请选择或搜索一名患者" />
      </section>
    );
  }

  return (
    <section className={styles.detailPanel} aria-label="患者详情">
      <div className={styles.detailHeader}>
        <div>
          <Title level={4}>{patient.displayName}</Title>
          <Text className={styles.mono}>{patient.platform_patient_id}</Text>
        </div>
        <Space>
          <EyeOutlined />
          <span>查看完整敏感信息</span>
          <Switch
            checked={showSensitive}
            onChange={onRevealChange}
            aria-label="查看完整敏感信息"
          />
        </Space>
      </div>

      <Alert
        type="info"
        showIcon
        className={styles.privacyAlert}
        message="隐私字段默认脱敏"
        description="身份证按前 4 后 4 展示，手机号按前 3 后 4 展示；完整信息仅在授权开关打开后显示。"
      />

      <Descriptions
        bordered
        size="small"
        column={{ xs: 1, sm: 2, lg: 3 }}
        items={[
          { key: "idCard", label: "居民身份证", children: maskIdCard(patient.idCardNo, showSensitive) },
          { key: "phone", label: "手机号", children: maskPhone(patient.phone, showSensitive) },
          { key: "ethnicity", label: "民族", children: patient.ethnicity || "未登记" },
          { key: "status", label: "主索引状态", children: <Tag color={statusColor(patient.status)}>{statusLabel(patient.status)}</Tag> },
          { key: "confidence", label: "平均置信度", children: `${patient.confidence}%` },
          { key: "updated", label: "最近更新", children: formatTime(patient.updated_time) },
        ]}
      />

      <div className={styles.detailSection}>
        <div className={styles.sectionTitle}>
          <LinkOutlined />
          <span>关联 ID</span>
        </div>
        <Table<PatientIdentity>
          rowKey={(record) => String(record.id)}
          size="small"
          loading={loading}
          dataSource={identities}
          columns={identityColumns}
          pagination={false}
        />
      </div>

      <div className={styles.detailSection}>
        <div className={styles.sectionTitle}>
          <LinkOutlined />
          <span>就诊记录</span>
        </div>
        <Table<VisitIdentity>
          rowKey={(record) => String(record.id)}
          size="small"
          loading={loading}
          dataSource={visits}
          columns={visitColumns}
          pagination={false}
          locale={{ emptyText: "暂无就诊标识" }}
        />
      </div>
    </section>
  );
}
