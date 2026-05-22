import { useState } from "react";
import { Alert, Button, Modal, Select, Space, Spin, Table, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { PlusOutlined, PlayCircleOutlined, EditOutlined } from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  ACCESS_STRATEGIES,
  ACCESS_STRATEGY_LABELS,
  RISK_LEVEL_LABELS,
  TRIGGER_TYPE_LABELS,
  executeTriggerPoint,
  listTriggerPoints,
} from "../../../api/adapterHub";
import type { RiskLevel, TriggerPoint, TriggerType } from "../../../api/adapterHub";
import TriggerEditModal from "./TriggerEditModal";
import styles from "../styles.module.css";

const { Paragraph } = Typography;

/**
 * CDSS 触发点列表（PR-FINAL-12 Tab 3）。
 *
 * - 上方 filter：business_scenario（自由文本）+ access_strategy（下拉）
 * - 顶部「新建触发点」按钮（弹 TriggerEditModal create mode）
 * - 行操作：编辑（弹 edit mode）+ 执行测试（弹简单 JSON payload Modal，调 executeTriggerPoint）
 */
export default function TriggerPointList() {
  const [businessScenario, setBusinessScenario] = useState<string>("");
  const [businessScenarioDraft, setBusinessScenarioDraft] = useState<string>("");
  const [accessStrategy, setAccessStrategy] = useState<string | undefined>(undefined);

  const [editing, setEditing] = useState<{
    open: boolean;
    mode: "create" | "edit";
    initial: TriggerPoint | null;
  }>({ open: false, mode: "create", initial: null });

  const [execTarget, setExecTarget] = useState<TriggerPoint | null>(null);
  const [execPayload, setExecPayload] = useState<string>("");
  const [execResult, setExecResult] = useState<unknown>(null);

  const listQuery = useQuery({
    queryKey: ["adapter-hub", "triggers", businessScenario, accessStrategy],
    queryFn: () =>
      listTriggerPoints({
        businessScenario: businessScenario || undefined,
        accessStrategy,
      }),
  });

  const execMutation = useMutation({
    mutationFn: async () => {
      if (!execTarget?.triggerCode) throw new Error("缺少 triggerCode");
      let payload: Record<string, unknown> = {};
      if (execPayload.trim()) {
        try {
          payload = JSON.parse(execPayload) as Record<string, unknown>;
        } catch {
          throw new Error("payload 不是合法 JSON");
        }
      }
      return executeTriggerPoint(execTarget.triggerCode, payload);
    },
    onSuccess: (data) => {
      setExecResult(data);
      message.success("触发点执行完成");
    },
    onError: (error: Error) => {
      message.error(`执行失败：${error.message}`);
    },
  });

  const columns: ColumnsType<TriggerPoint> = [
    {
      title: "触发点编码",
      dataIndex: "triggerCode",
      key: "triggerCode",
      width: 200,
      render: (v?: string) => (v ? <code>{v}</code> : "—"),
    },
    {
      title: "名称",
      dataIndex: "triggerName",
      key: "triggerName",
      width: 200,
    },
    {
      title: "类型",
      dataIndex: "triggerType",
      key: "triggerType",
      width: 100,
      render: (v?: string) =>
        v ? TRIGGER_TYPE_LABELS[v as TriggerType] ?? v : "—",
    },
    {
      title: "业务场景",
      dataIndex: "businessScenario",
      key: "businessScenario",
      width: 180,
    },
    {
      title: "接入策略",
      dataIndex: "accessStrategy",
      key: "accessStrategy",
      width: 140,
      render: (v?: string) =>
        v ? <Tag color="processing">{ACCESS_STRATEGY_LABELS[v as keyof typeof ACCESS_STRATEGY_LABELS] ?? v}</Tag> : "—",
    },
    {
      title: "风险",
      dataIndex: "riskLevel",
      key: "riskLevel",
      width: 80,
      render: (v?: string) => renderRisk(v as RiskLevel | undefined),
    },
    {
      title: "优先级",
      dataIndex: "priority",
      key: "priority",
      width: 80,
    },
    {
      title: "状态",
      dataIndex: "enabled",
      key: "enabled",
      width: 80,
      render: (v?: string) => renderEnabled(v),
    },
    {
      title: "操作",
      key: "actions",
      width: 180,
      render: (_v, row) => (
        <Space>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => setEditing({ open: true, mode: "edit", initial: row })}
          >
            编辑
          </Button>
          <Button
            size="small"
            icon={<PlayCircleOutlined />}
            onClick={() => {
              setExecTarget(row);
              setExecPayload("{}");
              setExecResult(null);
            }}
          >
            测试
          </Button>
        </Space>
      ),
    },
  ];

  const applyFilters = () => setBusinessScenario(businessScenarioDraft);

  return (
    <>
      <Paragraph type="secondary" className={styles.sectionHint}>
        CDSS 触发点定义院内业务事件 → 接入策略 → 关联规则/路径 的映射。在 HIS/EMR 触发对应业务场景时，
        平台按 triggerCode 命中并按 access_strategy（CDS Hooks / SMART app / 内嵌 / 后端推送）下发结果。
      </Paragraph>

      <div className={styles.toolbar} aria-label="trigger-toolbar">
        <div className={styles.toolbarItem}>
          <label className={styles.toolbarItemLabel}>业务场景</label>
          <input
            className="ant-input"
            placeholder="business_scenario"
            value={businessScenarioDraft}
            onChange={(e) => setBusinessScenarioDraft(e.target.value)}
          />
        </div>
        <div className={styles.toolbarItem}>
          <label className={styles.toolbarItemLabel}>接入策略</label>
          <Select<string>
            allowClear
            placeholder="全部"
            value={accessStrategy}
            options={ACCESS_STRATEGIES.map((s) => ({
              value: s,
              label: ACCESS_STRATEGY_LABELS[s],
            }))}
            onChange={(v) => setAccessStrategy(v)}
          />
        </div>
        <div className={styles.toolbarActions}>
          <Button type="primary" onClick={applyFilters}>
            查询
          </Button>
          <Button
            onClick={() => {
              setBusinessScenarioDraft("");
              setBusinessScenario("");
              setAccessStrategy(undefined);
            }}
          >
            重置
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setEditing({ open: true, mode: "create", initial: null })}
          >
            新建触发点
          </Button>
        </div>
      </div>

      {listQuery.isError ? (
        <Alert
          type="error"
          showIcon
          message="无法加载触发点列表"
          description={(listQuery.error as Error)?.message}
        />
      ) : listQuery.isLoading ? (
        <Spin tip="加载中..." />
      ) : (
        <Table<TriggerPoint>
          rowKey={(row) => String(row.id ?? row.triggerCode ?? "")}
          columns={columns}
          dataSource={listQuery.data ?? []}
          pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }}
          size="small"
          aria-label="trigger-point-table"
        />
      )}

      <TriggerEditModal
        open={editing.open}
        mode={editing.mode}
        initial={editing.initial}
        onClose={() => setEditing({ open: false, mode: "create", initial: null })}
      />

      <Modal
        title={`执行触发点 · ${execTarget?.triggerCode}`}
        open={execTarget !== null}
        onCancel={() => {
          setExecTarget(null);
          setExecResult(null);
        }}
        onOk={() => execMutation.mutate()}
        okText="执行"
        cancelText="关闭"
        confirmLoading={execMutation.isPending}
        width={680}
        aria-label="trigger-exec-modal"
      >
        <Paragraph type="secondary">
          以 JSON 形式输入 event payload；执行结果会写一条 audit log，可在「平台监控 / 审计日志」中复核。
        </Paragraph>
        <textarea
          className="ant-input"
          rows={8}
          value={execPayload}
          onChange={(e) => setExecPayload(e.target.value)}
        />
        {execResult !== null && (
          <pre className={styles.execResult}>
            {JSON.stringify(execResult, null, 2)}
          </pre>
        )}
      </Modal>
    </>
  );
}

function renderEnabled(value?: string) {
  if (value === "Y") {
    return <Tag color="success" className={styles.enabledTag}>启用</Tag>;
  }
  if (value === "N") {
    return <Tag className={styles.disabledTag}>停用</Tag>;
  }
  return <Tag>—</Tag>;
}

function renderRisk(value?: RiskLevel) {
  if (!value) return <Tag>—</Tag>;
  const className =
    value === "CRITICAL"
      ? styles.riskCritical
      : value === "HIGH"
        ? styles.riskHigh
        : value === "MEDIUM"
          ? styles.riskMedium
          : styles.riskLow;
  return <span className={className}>{RISK_LEVEL_LABELS[value]}</span>;
}
