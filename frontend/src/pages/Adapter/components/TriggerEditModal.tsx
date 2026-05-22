import { useEffect } from "react";
import { Form, Input, InputNumber, Modal, Select, message } from "antd";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ACCESS_STRATEGIES,
  ACCESS_STRATEGY_LABELS,
  RISK_LEVELS,
  RISK_LEVEL_LABELS,
  TRIGGER_TYPES,
  TRIGGER_TYPE_LABELS,
  registerTriggerPoint,
  updateTriggerPoint,
} from "../../../api/adapterHub";
import type { TriggerPoint } from "../../../api/adapterHub";
import styles from "../styles.module.css";

interface Props {
  open: boolean;
  mode: "create" | "edit";
  initial: TriggerPoint | null;
  onClose: () => void;
}

/**
 * 触发点 注册 / 更新 Modal（PR-FINAL-12）。
 *
 * - mode = "create"：POST /api/cdss/triggers（registerTriggerPoint）
 * - mode = "edit"：POST /api/cdss/triggers/{id}（updateTriggerPoint，需 initial.id）
 */
export default function TriggerEditModal({ open, mode, initial, onClose }: Props) {
  const [form] = Form.useForm<TriggerPoint>();
  const queryClient = useQueryClient();

  useEffect(() => {
    if (open) {
      form.setFieldsValue(initial ?? defaults());
    }
  }, [open, initial, form]);

  const mutation = useMutation({
    mutationFn: async (values: TriggerPoint) => {
      if (mode === "edit" && initial?.id) {
        return updateTriggerPoint(initial.id, { ...initial, ...values });
      }
      return registerTriggerPoint(values);
    },
    onSuccess: () => {
      message.success(mode === "edit" ? "触发点已更新" : "触发点已注册");
      queryClient.invalidateQueries({ queryKey: ["adapter-hub", "triggers"] });
      onClose();
    },
    onError: (error: Error) => {
      message.error(`操作失败：${error.message}`);
    },
  });

  return (
    <Modal
      title={mode === "edit" ? `编辑触发点 · ${initial?.triggerCode}` : "新建触发点"}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      okText="保存"
      cancelText="取消"
      confirmLoading={mutation.isPending}
      width={720}
      aria-label="trigger-edit-modal"
    >
      <Form<TriggerPoint>
        form={form}
        layout="vertical"
        className={styles.editForm}
        onFinish={(values) => mutation.mutate(values)}
      >
        <div className={styles.editFormRow}>
          <Form.Item
            label="触发点编码"
            name="triggerCode"
            rules={[{ required: true, message: "请输入触发点编码" }]}
          >
            <Input placeholder="如 AMI_ON_ADMIT" disabled={mode === "edit"} />
          </Form.Item>
          <Form.Item
            label="触发点名称"
            name="triggerName"
            rules={[{ required: true, message: "请输入名称" }]}
          >
            <Input placeholder="如 AMI 入院触发" />
          </Form.Item>
        </div>

        <div className={styles.editFormRow}>
          <Form.Item label="触发类型" name="triggerType">
            <Select
              options={TRIGGER_TYPES.map((t) => ({
                value: t,
                label: TRIGGER_TYPE_LABELS[t],
              }))}
              placeholder="EVENT/ORDER/..."
            />
          </Form.Item>
          <Form.Item label="接入策略" name="accessStrategy">
            <Select
              options={ACCESS_STRATEGIES.map((s) => ({
                value: s,
                label: ACCESS_STRATEGY_LABELS[s],
              }))}
              placeholder="CDS_HOOKS/SMART_APP/..."
            />
          </Form.Item>
        </div>

        <Form.Item label="业务场景" name="businessScenario">
          <Input placeholder="如 EmergencyAdmit / OutpatientOrder" />
        </Form.Item>

        <div className={styles.editFormRow}>
          <Form.Item label="适配器编码" name="adapterCode">
            <Input placeholder="adapter_code（链接业务适配器）" />
          </Form.Item>
          <Form.Item label="端点 URL" name="endpointUrl">
            <Input placeholder="https://..." />
          </Form.Item>
        </div>

        <div className={styles.editFormRow}>
          <Form.Item label="关联规则" name="ruleCodes">
            <Input placeholder="逗号分隔 rule_code，如 AMI_STEMI_DOOR_TO_BALLOON" />
          </Form.Item>
          <Form.Item label="关联路径" name="pathwayCodes">
            <Input placeholder="逗号分隔 pathway_code" />
          </Form.Item>
        </div>

        <div className={styles.editFormRow}>
          <Form.Item label="优先级" name="priority">
            <InputNumber min={0} max={1000} placeholder="0-1000" />
          </Form.Item>
          <Form.Item label="风险等级" name="riskLevel">
            <Select
              options={RISK_LEVELS.map((r) => ({ value: r, label: RISK_LEVEL_LABELS[r] }))}
              placeholder="LOW/MEDIUM/HIGH/CRITICAL"
              allowClear
            />
          </Form.Item>
          <Form.Item label="超时（ms）" name="timeoutMs">
            <InputNumber min={0} max={60000} placeholder="毫秒" />
          </Form.Item>
        </div>

        <Form.Item label="启用" name="enabled">
          <Select
            options={[
              { value: "Y", label: "启用" },
              { value: "N", label: "停用" },
            ]}
          />
        </Form.Item>

        <Form.Item label="描述" name="description">
          <Input.TextArea rows={3} placeholder="触发点用途、上下文、变更说明等" />
        </Form.Item>
      </Form>
    </Modal>
  );
}

function defaults(): TriggerPoint {
  return {
    triggerCode: "",
    triggerName: "",
    triggerType: "EVENT",
    accessStrategy: "CDS_HOOKS",
    priority: 100,
    timeoutMs: 3000,
    enabled: "Y",
  };
}
