/**
 * 记录变异对话框：记录 SKIP / DEFER / EXTEND_TIME / SUBSTITUTE / EXIT / ROLLBACK / MANUAL_OVERRIDE。
 */

import { useState } from "react";
import { Form, Input, Modal, Select, message } from "antd";
import { useMutation } from "@tanstack/react-query";
import {
  recordVariation,
  type RecordVariationRequest,
  type VariationType,
} from "../../../api/pathway";
import { VARIATION_TYPE_LABELS } from "../helpers/pathwayFormatters";

const VARIATION_OPTIONS: Array<{ value: VariationType; label: string }> = (
  Object.entries(VARIATION_TYPE_LABELS) as Array<[VariationType, string]>
).map(([value, label]) => ({ value, label }));

export interface VariationDialogProps {
  open: boolean;
  instanceId: string;
  currentNodeCode?: string;
  onClose: () => void;
  onRecorded: () => void;
}

export default function VariationDialog({
  open,
  instanceId,
  currentNodeCode,
  onClose,
  onRecorded,
}: VariationDialogProps) {
  const [type, setType] = useState<VariationType>("SKIP");
  const [reason, setReason] = useState<string>("");
  const [nodeCode, setNodeCode] = useState<string>(currentNodeCode ?? "");

  const recordMutation = useMutation({
    mutationFn: async () => {
      const req: RecordVariationRequest = {
        variation_type: type,
        node_code: nodeCode || undefined,
        reason: reason.trim() || `${VARIATION_TYPE_LABELS[type]}（无说明）`,
      };
      return recordVariation(instanceId, req);
    },
    onSuccess: () => {
      message.success("变异已记录");
      onRecorded();
      onClose();
    },
    onError: (err: Error) => message.error(`记录失败：${err.message}`),
  });

  return (
    <Modal
      open={open}
      title="记录路径变异"
      onCancel={onClose}
      onOk={() => recordMutation.mutate()}
      okText="记录"
      cancelText="取消"
      confirmLoading={recordMutation.isPending}
    >
      <Form layout="vertical">
        <Form.Item label="变异类型">
          <Select
            value={type}
            options={VARIATION_OPTIONS}
            onChange={(v) => setType(v as VariationType)}
            aria-label="variation-type"
          />
        </Form.Item>
        <Form.Item label="节点编码">
          <Input
            value={nodeCode}
            onChange={(e) => setNodeCode(e.target.value)}
            placeholder="可选；默认填当前节点"
            aria-label="variation-node-code"
          />
        </Form.Item>
        <Form.Item label="原因 / 备注">
          <Input.TextArea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={4}
            placeholder="为何要记录该变异？请如实填写以保证审计可追溯。"
            aria-label="variation-reason"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
