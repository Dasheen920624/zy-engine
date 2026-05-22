import { Button, Progress, Space, Tag } from "antd";
import { CheckCircleOutlined, IdcardOutlined, PhoneOutlined, UserOutlined } from "@ant-design/icons";
import type { MpiPatientRecord } from "../helpers";
import { maskIdCard, maskPhone, statusColor, statusLabel } from "../helpers";
import styles from "../styles.module.css";

export interface PatientCardProps {
  patient: MpiPatientRecord;
  selected?: boolean;
  showSensitive?: boolean;
  onSelect?: (platformPatientId: string) => void;
}

export default function PatientCard({
  patient,
  selected = false,
  showSensitive = false,
  onSelect,
}: PatientCardProps) {
  return (
    <article className={`${styles.patientCard} ${selected ? styles.patientCardSelected : ""}`}>
      <div className={styles.patientCardHeader}>
        <div>
          <div className={styles.patientName}>
            <UserOutlined />
            <span>{patient.displayName}</span>
          </div>
          <div className={styles.patientId}>{patient.platformPatientId}</div>
        </div>
        <Tag color={statusColor(patient.status)}>{statusLabel(patient.status)}</Tag>
      </div>

      <dl className={styles.patientMeta}>
        <div>
          <dt>
            <IdcardOutlined /> 身份证
          </dt>
          <dd>{maskIdCard(patient.idCardNo, showSensitive)}</dd>
        </div>
        <div>
          <dt>
            <PhoneOutlined /> 手机号
          </dt>
          <dd>{maskPhone(patient.phone, showSensitive)}</dd>
        </div>
        <div>
          <dt>民族</dt>
          <dd>{patient.ethnicity || "未登记"}</dd>
        </div>
        <div>
          <dt>来源</dt>
          <dd>{patient.sourceSystems.join(" / ") || "未同步"}</dd>
        </div>
      </dl>

      <div className={styles.patientCardFooter}>
        <Space size="small" wrap>
          <Tag>{patient.identityCount} 个关联 ID</Tag>
          <Tag>{patient.visitCount} 次就诊</Tag>
          {patient.conflictCount > 0 && <Tag color="error">{patient.conflictCount} 个冲突</Tag>}
          <Tag icon={<CheckCircleOutlined />}>{patient.verifiedCount} 已核验</Tag>
        </Space>
        <div className={styles.confidenceBlock} aria-label="confidence-score">
          <span>置信度</span>
          <Progress percent={patient.confidence} size="small" showInfo={false} />
        </div>
      </div>

      <Button
        type={selected ? "primary" : "default"}
        block
        onClick={() => onSelect?.(patient.platformPatientId)}
      >
        {selected ? "正在查看" : "查看主索引"}
      </Button>
    </article>
  );
}
