/**
 * 入径对话框：选择候选样本 → 拿候选推荐 → 选模板确认入径。
 */

import { useState } from "react";
import { Alert, Button, Form, Input, Modal, Select, Space, Typography, message } from "antd";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  admitPatientPathway,
  recommendPathwayCandidates,
  type RecommendationCard,
} from "../../../api/pathway";
import {
  CANDIDATE_SAMPLES,
  findCandidateSample,
} from "../helpers/pathwaySamples";
import { describeConfidence } from "../helpers/pathwayFormatters";
import { safeParseJson } from "../helpers/pathwaySamples.guards";
import styles from "../styles.module.css";

const { Text } = Typography;

export interface AdmitDialogProps {
  open: boolean;
  onClose: () => void;
  onAdmitted: (instanceId: string) => void;
}

export default function AdmitDialog({ open, onClose, onAdmitted }: AdmitDialogProps) {
  const [sampleLabel, setSampleLabel] = useState<string>(CANDIDATE_SAMPLES[0]?.label ?? "");
  const [contextText, setContextText] = useState<string>(() =>
    JSON.stringify(CANDIDATE_SAMPLES[0]?.patient_context ?? {}, null, 2),
  );
  const [selectedTargetCode, setSelectedTargetCode] = useState<string | null>(null);
  const [contextParseError, setContextParseError] = useState<string | null>(null);

  const candidatesMutation = useMutation({
    mutationFn: async () => {
      const parsed = safeParseJson(contextText);
      if (!parsed.ok) {
        setContextParseError(parsed.error);
        throw new Error(parsed.error);
      }
      setContextParseError(null);
      return recommendPathwayCandidates(parsed.value as Record<string, unknown>);
    },
    onError: (err: Error) => message.error(`获取候选失败：${err.message}`),
  });

  const candidatesQuery = useQuery<RecommendationCard[]>({
    queryKey: ["candidates-static"],
    queryFn: () => Promise.resolve([]),
    enabled: false,
  });

  const admitMutation = useMutation({
    mutationFn: async () => {
      const parsed = safeParseJson(contextText);
      if (!parsed.ok) throw new Error(parsed.error);
      const ctx = parsed.value as {
        patient: { patient_id: string };
        encounter: { encounter_id: string };
        facts?: Record<string, unknown>;
      };
      if (!selectedTargetCode) throw new Error("请先选择候选路径");
      return admitPatientPathway({
        pathway_code: selectedTargetCode,
        patient_id: ctx.patient.patient_id,
        encounter_id: ctx.encounter.encounter_id,
        initial_facts: ctx.facts ?? {},
      });
    },
    onSuccess: (instance) => {
      message.success("入径成功");
      onAdmitted(instance.instance_id);
      onClose();
    },
    onError: (err: Error) => message.error(`入径失败：${err.message}`),
  });

  const handleSampleChange = (label: string) => {
    setSampleLabel(label);
    const sample = findCandidateSample(label);
    if (sample) {
      setContextText(JSON.stringify(sample.patient_context, null, 2));
      setSelectedTargetCode(null);
      candidatesMutation.reset();
    }
  };

  const candidates = candidatesMutation.data ?? candidatesQuery.data ?? [];

  return (
    <Modal
      open={open}
      title="患者入径"
      onCancel={onClose}
      width={720}
      footer={[
        <Button key="cancel" onClick={onClose}>
          取消
        </Button>,
        <Button
          key="suggest"
          type="default"
          loading={candidatesMutation.isPending}
          onClick={() => candidatesMutation.mutate()}
        >
          拉取候选路径
        </Button>,
        <Button
          key="admit"
          type="primary"
          loading={admitMutation.isPending}
          disabled={!selectedTargetCode}
          onClick={() => admitMutation.mutate()}
        >
          确认入径
        </Button>,
      ]}
    >
      <Form layout="vertical">
        <Form.Item label="国情样本场景">
          <Select
            value={sampleLabel}
            onChange={handleSampleChange}
            options={CANDIDATE_SAMPLES.map((s) => ({ value: s.label, label: s.label }))}
            aria-label="admit-sample-select"
          />
          <Text type="secondary">
            {findCandidateSample(sampleLabel)?.description ?? "选择一个国情场景作为入径上下文。"}
          </Text>
        </Form.Item>
        <Form.Item
          label="患者上下文 JSON（patient + encounter + facts）"
          validateStatus={contextParseError ? "error" : ""}
          help={contextParseError ?? undefined}
        >
          <Input.TextArea
            value={contextText}
            onChange={(e) => setContextText(e.target.value)}
            rows={8}
            spellCheck={false}
            aria-label="admit-patient-context-json"
          />
        </Form.Item>

        {candidates.length > 0 ? (
          <Form.Item label="候选路径">
            <div className={styles.candidateList}>
              {candidates.map((c) => {
                const selected = c.target_code === selectedTargetCode;
                return (
                  <div
                    key={c.recommendation_id}
                    className={
                      selected
                        ? `${styles.candidateItem} ${styles.candidateItemSelected}`
                        : styles.candidateItem
                    }
                    role="button"
                    tabIndex={0}
                    onClick={() => setSelectedTargetCode(c.target_code)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") setSelectedTargetCode(c.target_code);
                    }}
                  >
                    <div className={styles.candidateName}>
                      {c.target_name ?? c.target_code} <Text type="secondary">{c.target_code}</Text>
                    </div>
                    <Space size="middle">
                      <span className={styles.candidateScore}>
                        分数 {c.score.toFixed(2)} · 置信 {describeConfidence(c.score)}
                      </span>
                      {c.action_level && <span className={styles.candidateScore}>{c.action_level}</span>}
                    </Space>
                  </div>
                );
              })}
            </div>
          </Form.Item>
        ) : (
          <Alert
            type="info"
            showIcon
            message="尚未拉取候选路径"
            description="点击「拉取候选路径」按钮调用后端 /patient-pathways/candidates。"
          />
        )}
      </Form>
    </Modal>
  );
}
