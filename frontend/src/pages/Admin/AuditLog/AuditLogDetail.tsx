import { Drawer, Tag } from "antd";
import { CheckCircleOutlined, WarningOutlined } from "@ant-design/icons";
import type { AuditLogEntry } from "../../../api/auditLog";
import { ACTION_TYPE_LABELS, ENGINE_TYPE_LABELS } from "../../../api/auditLog";
import styles from "./styles.module.css";

interface Props {
  entry: AuditLogEntry | null;
  open: boolean;
  onClose: () => void;
}

/**
 * 审计日志详情 Drawer（PR-FINAL-09）。
 *
 * - 只读视图（等保 2.0 三级要求审计不可改不可删）
 * - 4 个分组：基础信息 / 组织上下文 / 请求响应 payload / 验签结果
 * - patient_id / encounter_id / operator_id 默认显示，调用方在列表层已脱敏
 */
export default function AuditLogDetail({ entry, open, onClose }: Props) {
  if (!entry) {
    return (
      <Drawer
        title="审计详情"
        open={open}
        onClose={onClose}
        width={720}
        aria-label="audit-detail-empty"
      >
        <p className={styles.detailMissing}>未选中审计记录</p>
      </Drawer>
    );
  }

  return (
    <Drawer
      title={`审计详情 · ${entry.trace_id ?? "无 trace_id"}`}
      open={open}
      onClose={onClose}
      width={720}
      aria-label="audit-detail"
    >
      <Section title="基础信息">
        <Pair label="ID" value={entry.id} />
        <Pair label="Trace ID" value={entry.trace_id} />
        <Pair
          label="引擎类型"
          value={resolveLabel(entry.engine_type, ENGINE_TYPE_LABELS)}
        />
        <Pair
          label="操作类型"
          value={resolveLabel(entry.action_type, ACTION_TYPE_LABELS)}
        />
        <Pair label="目标类型" value={entry.target_type} />
        <Pair label="目标编码" value={entry.target_code} />
        <Pair label="目标版本" value={entry.target_version} />
        <Pair label="创建时间" value={entry.created_time} />
      </Section>

      <Section title="组织上下文">
        <Pair label="租户" value={entry.tenant_id} />
        <Pair label="医院" value={entry.hospital_code} />
        <Pair label="患者 ID" value={mask4_4(entry.patient_id)} />
        <Pair label="就诊 ID" value={entry.encounter_id} />
        <Pair label="操作人" value={entry.operator_name ?? entry.operator_id} />
        <Pair label="客户端 IP" value={entry.client_ip} />
      </Section>

      <Section title="请求 payload">
        <PayloadBlock value={entry.request_payload} />
      </Section>

      <Section title="响应 payload">
        <PayloadBlock value={entry.response_payload} />
      </Section>

      <Section title="验签结果">
        <Pair
          label="签名"
          value={
            entry.signature ? (
              <code className={styles.signatureCell}>{entry.signature}</code>
            ) : (
              <span className={styles.detailMissing}>缺失</span>
            )
          }
        />
        <Pair
          label="前置签名"
          value={
            entry.prev_signature ? (
              <code className={styles.signatureCell}>{entry.prev_signature}</code>
            ) : (
              <span className={styles.detailMissing}>缺失</span>
            )
          }
        />
        <Pair
          label="校验状态"
          value={renderSignatureState(entry.signature_valid)}
        />
      </Section>
    </Drawer>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className={styles.detailSection}>
      <div className={styles.detailSectionTitle}>{title}</div>
      <div className={styles.detailGrid}>{children}</div>
    </div>
  );
}

function Pair({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <>
      <div className={styles.detailLabel}>{label}</div>
      <div className={styles.detailValue}>
        {value === undefined || value === null || value === "" ? (
          <span className={styles.detailMissing}>—</span>
        ) : (
          value
        )}
      </div>
    </>
  );
}

function PayloadBlock({ value }: { value?: string | Record<string, unknown> }) {
  if (value === undefined || value === null || value === "") {
    return <div className={styles.detailMissing}>无 payload</div>;
  }
  if (typeof value === "string") {
    // 尝试解析为 JSON 美化；解析失败按原文展示
    try {
      const parsed = JSON.parse(value) as unknown;
      return <pre className={styles.payloadBlock}>{JSON.stringify(parsed, null, 2)}</pre>;
    } catch {
      return <pre className={styles.payloadBlock}>{value}</pre>;
    }
  }
  return <pre className={styles.payloadBlock}>{JSON.stringify(value, null, 2)}</pre>;
}

function renderSignatureState(valid?: boolean) {
  if (valid === true) {
    return (
      <Tag color="success" className={styles.signatureValid}>
        <CheckCircleOutlined /> 校验通过
      </Tag>
    );
  }
  if (valid === false) {
    return (
      <Tag color="error" className={styles.signatureInvalid}>
        <WarningOutlined /> 校验失败（疑似篡改）
      </Tag>
    );
  }
  return <span className={styles.detailMissing}>未校验</span>;
}

function resolveLabel(
  raw: string | undefined,
  table: Record<string, string>,
): string | undefined {
  if (!raw) return raw;
  return table[raw] ?? raw;
}

function mask4_4(value?: string): string | undefined {
  if (!value) return value;
  if (value.length <= 8) return value;
  return `${value.slice(0, 4)}****${value.slice(-4)}`;
}
