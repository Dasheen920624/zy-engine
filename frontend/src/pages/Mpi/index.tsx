import { useEffect, useMemo, useState } from "react";
import { Alert, Button, Space, Switch, Typography, message } from "antd";
import { IdcardOutlined, ReloadOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  detectConflicts,
  findPatientByExternalId,
  getPendingConflicts,
  listPatientIdentities,
  listPatientVisitIdentities,
  mergePatientIdentities,
  resolveConflict,
  verifyPatientIdentity,
} from "../../api/mpi";
import type { IdentityConflict, PatientIdentity, VisitIdentity } from "../../api/mpi";
import { getOrgContext } from "../../store/orgContext";
import MergeWorkbench from "./MergeWorkbench";
import type { ResolveConflictPayload } from "./MergeWorkbench";
import PatientDetail from "./PatientDetail";
import PatientList from "./PatientList";
import type { PatientSearchPayload } from "./PatientList";
import { buildPatientRecord } from "./helpers";
import type { MpiPatientRecord } from "./helpers";
import styles from "./styles.module.css";

const { Text, Title } = Typography;

interface LoadedPatientPayload {
  record: MpiPatientRecord;
  identities: PatientIdentity[];
  visits: VisitIdentity[];
}

async function loadPatientByPlatformId(
  tenantId: string,
  platformPatientId: string,
  conflicts: IdentityConflict[],
): Promise<LoadedPatientPayload> {
  const [identities, visits] = await Promise.all([
    listPatientIdentities(tenantId, platformPatientId),
    listPatientVisitIdentities(tenantId, platformPatientId),
  ]);
  return {
    record: buildPatientRecord({
      platformPatientId,
      identities,
      visits,
      conflicts,
    }),
    identities,
    visits,
  };
}

export function MpiPatientsPage() {
  const queryClient = useQueryClient();
  const tenantId = getOrgContext().tenant_id || "TENANT_DEMO";
  const [patients, setPatients] = useState<MpiPatientRecord[]>([]);
  const [selectedPatientId, setSelectedPatientId] = useState<string>();
  const [showSensitive, setShowSensitive] = useState(false);

  const conflictsQuery = useQuery({
    queryKey: ["mpi", "conflicts", tenantId],
    queryFn: () => getPendingConflicts(tenantId),
  });

  const identitiesQuery = useQuery({
    queryKey: ["mpi", "patient-identities", tenantId, selectedPatientId],
    queryFn: () => listPatientIdentities(tenantId, selectedPatientId || ""),
    enabled: Boolean(selectedPatientId),
  });

  const visitsQuery = useQuery({
    queryKey: ["mpi", "patient-visits", tenantId, selectedPatientId],
    queryFn: () => listPatientVisitIdentities(tenantId, selectedPatientId || ""),
    enabled: Boolean(selectedPatientId),
  });

  const selectedPatient = useMemo(
    () => patients.find((patient) => patient.platformPatientId === selectedPatientId),
    [patients, selectedPatientId],
  );

  useEffect(() => {
    if (!selectedPatientId || !identitiesQuery.data || !visitsQuery.data) return;
    const nextRecord = buildPatientRecord({
      platformPatientId: selectedPatientId,
      identities: identitiesQuery.data,
      visits: visitsQuery.data,
      conflicts: conflictsQuery.data ?? [],
    });
    setPatients((prev) => {
      const withoutCurrent = prev.filter((patient) => patient.platformPatientId !== selectedPatientId);
      return [nextRecord, ...withoutCurrent];
    });
  }, [conflictsQuery.data, identitiesQuery.data, selectedPatientId, visitsQuery.data]);

  const searchMutation = useMutation({
    mutationFn: async (payload: PatientSearchPayload) => {
      if (!payload.keyword) {
        throw new Error("请输入患者 ID、证件号或外部号");
      }
      if (payload.identityType === "PLATFORM_PATIENT_ID") {
        return loadPatientByPlatformId(tenantId, payload.keyword, conflictsQuery.data ?? []);
      }
      const identity = await findPatientByExternalId({
        tenantId,
        identityType: payload.identityType,
        sourceSystem: payload.sourceSystem,
        externalId: payload.keyword,
      });
      return loadPatientByPlatformId(tenantId, identity.platformPatientId, conflictsQuery.data ?? []);
    },
    onSuccess: ({ record }) => {
      setPatients((prev) => {
        const withoutCurrent = prev.filter((patient) => patient.platformPatientId !== record.platformPatientId);
        return [record, ...withoutCurrent];
      });
      setSelectedPatientId(record.platformPatientId);
      message.success("患者主索引已加载");
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : "患者主索引加载失败");
    },
  });

  const verifyMutation = useMutation({
    mutationFn: (identityId: number) => verifyPatientIdentity(identityId, "platform-admin"),
    onSuccess: () => {
      message.success("标识已人工核验");
      queryClient.invalidateQueries({ queryKey: ["mpi", "patient-identities", tenantId, selectedPatientId] });
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : "核验失败");
    },
  });

  const detectMutation = useMutation({
    mutationFn: () => detectConflicts(tenantId),
    onSuccess: () => {
      message.success("冲突检测完成");
      queryClient.invalidateQueries({ queryKey: ["mpi", "conflicts", tenantId] });
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : "冲突检测失败");
    },
  });

  const resolveMutation = useMutation({
    mutationFn: async (payload: ResolveConflictPayload) => {
      if (payload.resolutionType === "MERGE" && payload.targetPatientIdentityId) {
        const sourceIds = payload.sourcePatientIdentityIds.filter(
          (identityId) => identityId !== payload.targetPatientIdentityId,
        );
        await Promise.all(
          sourceIds.map((sourceId) =>
            mergePatientIdentities({
              sourceId,
              targetId: payload.targetPatientIdentityId as number,
              mergedBy: "platform-admin",
            }),
          ),
        );
      }
      await resolveConflict(payload.conflictId, {
        resolutionType: payload.resolutionType,
        resolutionNotes: payload.resolutionNotes,
        resolvedBy: "platform-admin",
        targetPatientIdentityId: payload.targetPatientIdentityId,
      });
    },
    onSuccess: () => {
      message.success("冲突处理完成");
      queryClient.invalidateQueries({ queryKey: ["mpi"] });
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : "冲突处理失败");
    },
  });

  const currentIdentities = identitiesQuery.data ?? selectedPatient?.identities ?? [];
  const currentVisits = visitsQuery.data ?? selectedPatient?.visits ?? [];

  return (
    <main className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <Space className={styles.eyebrow}>
            <IdcardOutlined />
            <span>用户与身份 / 患者主索引</span>
          </Space>
          <Title level={2}>患者主索引</Title>
          <Text type="secondary">
            汇聚同一患者在 HIS、EMR、医保、门诊住院系统中的身份映射，并提供冲突合并闭环。
          </Text>
        </div>
        <div className={styles.headerActions}>
          <Space>
            <span>完整信息权限</span>
            <Switch
              checked={showSensitive}
              onChange={setShowSensitive}
              aria-label="完整信息权限"
            />
          </Space>
          <Button
            icon={<ReloadOutlined />}
            loading={conflictsQuery.isFetching}
            onClick={() => conflictsQuery.refetch()}
          >
            刷新冲突
          </Button>
        </div>
      </header>

      <Alert
        type="warning"
        showIcon
        className={styles.topAlert}
        message="国情合规保护已开启"
        description="身份证、手机号默认脱敏；民族筛选使用 56 项标准枚举；人工合并会记录处理方式、目标标识和经办人。"
      />

      <div className={styles.workspaceGrid}>
        <PatientList
          patients={patients}
          selectedPatientId={selectedPatientId}
          loading={searchMutation.isPending}
          showSensitive={showSensitive}
          onSearch={(payload) => searchMutation.mutate(payload)}
          onSelectPatient={setSelectedPatientId}
        />
        <div className={styles.rightColumn}>
          <PatientDetail
            patient={selectedPatient}
            identities={currentIdentities}
            visits={currentVisits}
            loading={identitiesQuery.isFetching || visitsQuery.isFetching}
            showSensitive={showSensitive}
            onRevealChange={setShowSensitive}
            onVerifyIdentity={(identityId) => verifyMutation.mutate(identityId)}
          />
          <MergeWorkbench
            conflicts={conflictsQuery.data ?? []}
            identities={currentIdentities}
            loading={conflictsQuery.isFetching || detectMutation.isPending}
            resolving={resolveMutation.isPending}
            onDetect={() => detectMutation.mutate()}
            onResolve={(payload) => resolveMutation.mutate(payload)}
          />
        </div>
      </div>
    </main>
  );
}

export default MpiPatientsPage;
