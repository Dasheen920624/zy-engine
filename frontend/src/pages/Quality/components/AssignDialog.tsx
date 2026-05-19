import React, { useState } from "react";
import { Modal, Form, Input, DatePicker, Select, message } from "antd";
import { assignProblem } from "../../../api/quality";
import type { QualityAlert, AssignRequest } from "../../../api/types";

interface AssignDialogProps {
  visible: boolean;
  alert: QualityAlert | null;
  onClose: () => void;
  onAssigned?: () => void;
}

const roleOptions = [
  { value: "R04", label: "质控专员" },
  { value: "R06", label: "主治医师" },
  { value: "R07", label: "住院医师" },
  { value: "R08", label: "科室主任" },
];

const AssignDialog: React.FC<AssignDialogProps> = ({ visible, alert, onClose, onAssigned }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const request: AssignRequest = {
        assignee: values.assignee,
        assignee_role: values.assignee_role,
        deadline: values.deadline?.toISOString(),
        note: values.note,
        assigned_by: "current_user",
      };
      await assignProblem(alert!.id, request);
      message.success("派单成功");
      form.resetFields();
      onAssigned?.();
      onClose();
    } catch (err) {
      if (err instanceof Error) {
        message.error(err.message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={`派单 — ${alert?.rule_name || alert?.rule_code || ""}`}
      open={visible}
      onOk={handleOk}
      onCancel={() => { form.resetFields(); onClose(); }}
      confirmLoading={loading}
      okText="确认派单"
      cancelText="取消"
      width={480}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="assignee" label="指派给" rules={[{ required: true, message: "请输入指派人" }]}>
          <Input placeholder="医生姓名或工号" />
        </Form.Item>
        <Form.Item name="assignee_role" label="角色" rules={[{ required: true, message: "请选择角色" }]}>
          <Select options={roleOptions} placeholder="选择角色" />
        </Form.Item>
        <Form.Item name="deadline" label="整改截止时间">
          <DatePicker showTime style={{ width: "100%" }} />
        </Form.Item>
        <Form.Item name="note" label="备注">
          <Input.TextArea rows={3} placeholder="派单说明" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default AssignDialog;
