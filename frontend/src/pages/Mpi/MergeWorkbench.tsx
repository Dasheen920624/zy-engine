import { useEffect, useMemo, useState } from "react";
import { Button, Empty, Input, List, Select, Space, Tag, Typography } from "antd";
import { MergeCellsOutlined, ReloadOutlined } from "@ant-design/icons";
import type { ConflictResolutionType, IdentityConflict, PatientIdentity } from "../../api/mpi";
import {
  patientIdentityTypeLabel,
  parseIdList,
  severityColor,
  statusColor,
  statusLabel,
} from "./helpers";
import styles from "./styles.module.css";

const { Paragraph, Text, Title } = Typography;

export interface ResolveConflictPayload {
  conflictId: number;
  resolution_type: ConflictResolutionType;
  resolution_notes?: string;
  target_patient_identity_id?: number;
  source_patient_identity_ids: number[];
}

export interface MergeWorkbenchProps {
  conflicts: IdentityConflict[];
  identities: PatientIdentity[];
  loading?: boolean;
  resolving?: boolean;
  onDetect: () => void;
  onResolve: (payload: ResolveConflictPayload) => void;
}

const RESOLUTION_OPTIONS: Array<{ value: ConflictResolutionType; label: string }> = [
  { value: "MERGE", label: "合并到目标标识" },
  { value: "KEEP_BOTH", label: "保留两个标识" },
  { value: "SPLIT", label: "拆分为不同患者" },
  { value: "MANUAL_LINK", label: "人工建立关联" },
];

export default function MergeWorkbench({
  conflicts,
  identities,
  loading = false,
  resolving = false,
  onDetect,
  onResolve,
}: MergeWorkbenchProps) {
  const [selectedConflictId, setSelectedConflictId] = useState<number | undefined>(conflicts[0]?.id);
  const [resolution_type, setResolutionType] = useState<ConflictResolutionType>("MERGE");
  const [target_patient_identity_id, setTargetPatientIdentityId] = useState<number | undefined>();
  const [notes, setNotes] = useState("");

  useEffect(() => {
    if (!conflicts.length) {
      setSelectedConflictId(undefined);
      return;
    }
    if (!selectedConflictId || !conflicts.some((conflict) => conflict.id === selectedConflictId)) {
      setSelectedConflictId(conflicts[0].id);
    }
  }, [conflicts, selectedConflictId]);

  const selectedConflict = useMemo(
    () => conflicts.find((conflict) => conflict.id === selectedConflictId),
    [conflicts, selectedConflictId],
  );

  const conflictIdentityIds = parseIdList(selectedConflict?.patient_identity_ids);
  const identityOptions = identities
    .filter((identity) => !conflictIdentityIds.length || conflictIdentityIds.includes(identity.id))
    .map((identity) => ({
      value: identity.id,
      label: `${patientIdentityTypeLabel(identity.identity_type)} · ${identity.source_system} · #${identity.id}`,
    }));

  const submitResolution = () => {
    if (!selectedConflict) return;
    onResolve({
      conflictId: selectedConflict.id,
      resolution_type,
      resolution_notes: notes.trim() || undefined,
      target_patient_identity_id,
      source_patient_identity_ids: conflictIdentityIds,
    });
  };

  return (
    <section className={styles.mergePanel} aria-label="冲突合并工作台">
      <div className={styles.mergeHeader}>
        <div>
          <Title level={4}>
            <MergeCellsOutlined /> 冲突合并工作台
          </Title>
          <Text type="secondary">检测重复外部号、多平台映射和哈希冲突，人工确认后再落库。</Text>
        </div>
        <Button icon={<ReloadOutlined />} loading={loading} onClick={onDetect}>
          重新检测
        </Button>
      </div>

      <div className={styles.mergeGrid}>
        <div className={styles.conflictList}>
          {conflicts.length ? (
            <List
              dataSource={conflicts}
              renderItem={(conflict) => (
                <List.Item
                  className={
                    conflict.id === selectedConflictId
                      ? styles.conflictItemSelected
                      : styles.conflictItem
                  }
                  onClick={() => setSelectedConflictId(conflict.id)}
                >
                  <List.Item.Meta
                    title={
                      <Space wrap>
                        <span>#{conflict.id}</span>
                        <Tag color={severityColor(conflict.severity)}>{conflict.severity}</Tag>
                        <Tag color={statusColor(conflict.status)}>{statusLabel(conflict.status)}</Tag>
                      </Space>
                    }
                    description={conflict.conflict_description || conflict.conflict_type}
                  />
                </List.Item>
              )}
            />
          ) : (
            <Empty description="暂无待处理冲突" />
          )}
        </div>

        <div className={styles.resolvePanel}>
          {selectedConflict ? (
            <>
              <Paragraph className={styles.conflictDescription}>
                {selectedConflict.conflict_description || selectedConflict.conflict_type}
              </Paragraph>
              <div className={styles.resolveForm}>
                <label>
                  <span>处理方式</span>
                  <Select
                    value={resolution_type}
                    options={RESOLUTION_OPTIONS}
                    onChange={setResolutionType}
                    aria-label="处理方式"
                  />
                </label>
                <label>
                  <span>目标标识</span>
                  <Select
                    allowClear
                    value={target_patient_identity_id}
                    options={identityOptions}
                    placeholder="选择目标标识"
                    onChange={setTargetPatientIdentityId}
                    aria-label="目标标识"
                  />
                </label>
                <label>
                  <span>处理备注</span>
                  <Input.TextArea
                    value={notes}
                    rows={4}
                    onChange={(event) => setNotes(event.target.value)}
                    placeholder="记录人工判断依据"
                    aria-label="处理备注"
                  />
                </label>
                <Button
                  type="primary"
                  icon={<MergeCellsOutlined />}
                  loading={resolving}
                  disabled={resolution_type === "MERGE" && !target_patient_identity_id}
                  onClick={submitResolution}
                >
                  确认处理
                </Button>
              </div>
            </>
          ) : (
            <Empty description="请选择一条冲突" />
          )}
        </div>
      </div>
    </section>
  );
}
