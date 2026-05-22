import React, { useState, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { Button, Card, Input, Select, Space, Statistic, Switch, Table, Tag, Typography } from "antd";
import { useQuery } from "@tanstack/react-query";
import type { StatusKey } from "../../components/StatusBadge/StatusBadge.types";
import { StatusBadge, OrgContextSelector, SourceInfo } from "../../components";
import { listAlerts, getAlertSummary } from "../../api/quality";
import type { QualityAlert, ListAlertsParams, AlertSeverity } from "../../api/types";
import AssignDialog from "./components/AssignDialog";
import styles from "./alertList.module.css";

const { Title } = Typography;

const severityOptions = [
  { value: "", label: "全部" },
  { value: "CRITICAL", label: "危急" },
  { value: "WARNING", label: "警告" },
  { value: "INFO", label: "提醒" },
];

const statusOptions = [
  { value: "", label: "全部" },
  { value: "PENDING", label: "待处理" },
  { value: "IN_PROGRESS", label: "进行中" },
  { value: "RESOLVED", label: "已解决" },
];

const severityColor = (s: AlertSeverity) => {
  switch (s) {
    case "CRITICAL": return "red";
    case "WARNING": return "orange";
    case "INFO": return "blue";
    default: return "default";
  }
};

const severityDots = (s: AlertSeverity) => {
  switch (s) {
    case "CRITICAL": return "●●●";
    case "WARNING": return "●●";
    case "INFO": return "●";
    default: return "●";
  }
};

const SEVERITY_DOT_COLOR: Record<string, string> = {
  red: "var(--mk-danger)",
  orange: "var(--mk-warning)",
  blue: "var(--mk-primary)",
};

const STATUS_BADGE_MAP: Record<string, StatusKey> = {
  PENDING: "pending",
  IN_PROGRESS: "processing",
  RESOLVED: "success",
};

const AlertList: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [realtimeMode, setRealtimeMode] = useState(false);
  const [assignTarget, setAssignTarget] = useState<QualityAlert | null>(null);

  const severity = searchParams.get("severity") || undefined;
  const status = searchParams.get("status") || undefined;
  const dept = searchParams.get("dept") || undefined;
  const page = Number(searchParams.get("page")) || 1;
  const size = Number(searchParams.get("size")) || 20;

  const params: ListAlertsParams = { severity, status, dept, page, size };

  const { data, isLoading, refetch } = useQuery({
    queryKey: ["quality-alerts", params],
    queryFn: () => listAlerts(params),
    refetchInterval: realtimeMode ? 5000 : false,
  });

  const { data: summary } = useQuery({
    queryKey: ["quality-alert-summary"],
    queryFn: getAlertSummary,
    refetchInterval: realtimeMode ? 5000 : false,
  });

  const updateFilter = (key: string, value: string | undefined) => {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value); else next.delete(key);
    next.set("page", "1");
    setSearchParams(next);
  };

  const handleAssigned = useCallback(() => {
    refetch();
  }, [refetch]);

  const columns = [
    {
      title: "严重度",
      dataIndex: "severity",
      key: "severity",
      width: 80,
      render: (s: AlertSeverity) => (
        <span style={{ color: SEVERITY_DOT_COLOR[severityColor(s)] || "var(--mk-primary)" }}>
          {severityDots(s)}
        </span>
      ),
    },
    {
      title: "时间",
      dataIndex: "time",
      key: "time",
      width: 100,
      render: (t: string) => t ? t.substring(11, 16) : "—",
    },
    {
      title: "患者",
      dataIndex: "patient_id",
      key: "patient",
      width: 80,
      render: (p: string) => p ? `${p.substring(0, 3)}XX` : "—",
    },
    {
      title: "医生",
      dataIndex: "doctor",
      key: "doctor",
      width: 60,
      render: (d: string | null) => d ? `${d.charAt(0)}` : "—",
    },
    {
      title: "规则",
      dataIndex: "rule_name",
      key: "rule",
      render: (name: string | null, record: QualityAlert) => name || record.rule_code,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 80,
      render: (s: string, record: QualityAlert) => {
        if (record.overtime) {
          return <Tag color="red">超时未改</Tag>;
        }
        return <StatusBadge status={STATUS_BADGE_MAP[s] ?? "success"} />;
      },
    },
    {
      title: "操作",
      key: "action",
      width: 80,
      render: (_: unknown, record: QualityAlert) => {
        if (record.status === "PENDING") {
          return <Button type="link" size="small" onClick={() => setAssignTarget(record)}>派单</Button>;
        }
        return <Button type="link" size="small" disabled>查看</Button>;
      },
    },
  ];

  return (
    <div className={styles.page}>
      <Title level={3} style={{ marginBottom: 24 }}>质控预警</Title>

      <SourceInfo source={{ documentName: "质控预警", documentId: "quality-alerts" }} />

      <Space style={{ marginBottom: 16 }} size="middle">
        <OrgContextSelector />
        <span>实时模式</span>
        <Switch checked={realtimeMode} onChange={setRealtimeMode} />
        <Input
          placeholder="科室"
          value={dept || ""}
          onChange={(e) => updateFilter("dept", e.target.value || undefined)}
          style={{ width: 120 }}
        />
        <Select
          value={severity || ""}
          onChange={(v) => updateFilter("severity", v || undefined)}
          options={severityOptions}
          style={{ width: 100 }}
        />
        <Select
          value={status || ""}
          onChange={(v) => updateFilter("status", v || undefined)}
          options={statusOptions}
          style={{ width: 100 }}
        />
      </Space>

      <Space style={{ marginBottom: 16, width: "100%" }} size="large">
        <Card size="small"><Statistic title="危急" value={summary?.critical || 0} valueStyle={{ color: "var(--mk-danger)" }} /></Card>
        <Card size="small"><Statistic title="警告" value={summary?.warning || 0} valueStyle={{ color: "var(--mk-warning)" }} /></Card>
        <Card size="small"><Statistic title="提醒" value={summary?.info || 0} valueStyle={{ color: "var(--mk-primary)" }} /></Card>
        <Card size="small"><Statistic title="超时未改" value={summary?.overtime || 0} valueStyle={{ color: "var(--mk-danger)" }} /></Card>
      </Space>

      <Table
        columns={columns}
        dataSource={data?.items || []}
        rowKey="id"
        loading={isLoading}
        rowClassName={(record) => record.overtime ? "ant-table-row-overtime" : ""}
        pagination={{
          current: page,
          pageSize: size,
          total: data?.total || 0,
          showSizeChanger: true,
          pageSizeOptions: ["10", "20", "50"],
          showTotal: (total) => `共 ${total} 条`,
          onChange: (p, s) => {
            const next = new URLSearchParams(searchParams);
            next.set("page", String(p));
            next.set("size", String(s));
            setSearchParams(next);
          },
        }}
      />

      <AssignDialog
        visible={!!assignTarget}
        alert={assignTarget}
        onClose={() => setAssignTarget(null)}
        onAssigned={handleAssigned}
      />

      <style>{`
        .ant-table-row-overtime td { background: var(--mk-danger-soft) !important; }
      `}</style>
    </div>
  );
};

export default AlertList;
