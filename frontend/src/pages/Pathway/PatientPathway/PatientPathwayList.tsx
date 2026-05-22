/**
 * 患者路径管理 - 列表页（路由 /pathway/patients）。
 *
 * 列表 + 状态/路径/患者筛选 + 入径对话框 + 跳转详情。
 */

import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button, Input, Select, Table, Tag, Typography } from "antd";
import { PlusOutlined, SearchOutlined, UserOutlined } from "@ant-design/icons";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  listPatientPathwayInstances,
  type InstanceStatus,
  type PatientPathwayInstance,
} from "../../../api/pathway";
import {
  describeInstanceStatus,
  INSTANCE_STATUS_COLOR,
  INSTANCE_STATUS_LABELS,
  maskPatientId,
} from "../helpers/pathwayFormatters";
import AdmitDialog from "./AdmitDialog";
import styles from "../styles.module.css";

const { Title } = Typography;

const STATUS_OPTIONS: Array<{ value: InstanceStatus | ""; label: string }> = [
  { value: "", label: "全部状态" },
  ...(Object.entries(INSTANCE_STATUS_LABELS) as Array<[InstanceStatus, string]>).map(
    ([value, label]) => ({ value, label }),
  ),
];

export default function PatientPathwayList() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [admitOpen, setAdmitOpen] = useState(false);

  const pathwayCode = searchParams.get("pathway_code") ?? "";
  const status = (searchParams.get("status") ?? "") as InstanceStatus | "";
  const patientId = searchParams.get("patient_id") ?? "";

  const { data, isLoading } = useQuery({
    queryKey: ["patient-pathway-instances", pathwayCode, status, patientId],
    queryFn: () =>
      listPatientPathwayInstances({
        pathway_code: pathwayCode || undefined,
        status: status || undefined,
        patient_id: patientId || undefined,
        limit: 100,
      }),
  });

  const updateFilter = (key: string, value: string) => {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value);
    else next.delete(key);
    setSearchParams(next, { replace: true });
  };

  const columns = [
    {
      title: "实例 ID",
      dataIndex: "instance_id",
      key: "instance_id",
      width: 200,
      render: (id: string, record: PatientPathwayInstance) => (
        <a
          className={styles.patientLink}
          onClick={() => navigate(`/pathway/patients/${encodeURIComponent(id)}`)}
        >
          {id}
          <span className={styles.maskedId}>· {record.pathway_code}</span>
        </a>
      ),
    },
    {
      title: "患者",
      dataIndex: "patient_id",
      key: "patient_id",
      width: 180,
      render: (pid: string) => (
        <span>
          <UserOutlined /> <span className={styles.maskedId}>{maskPatientId(pid)}</span>
        </span>
      ),
    },
    {
      title: "就诊号",
      dataIndex: "encounter_id",
      key: "encounter_id",
      width: 140,
      render: (eid: string) => <span className={styles.maskedId}>{eid}</span>,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (s: InstanceStatus) => (
        <Tag color={INSTANCE_STATUS_COLOR[s] ?? "default"}>{describeInstanceStatus(s)}</Tag>
      ),
    },
    {
      title: "当前节点",
      dataIndex: "current_node_code",
      key: "current_node_code",
      render: (code: string) => code || "—",
    },
    {
      title: "版本",
      dataIndex: "version_no",
      key: "version_no",
      width: 90,
      render: (v: string) => (v ? `v${v}` : "—"),
    },
    {
      title: "组织",
      key: "scope",
      render: (_: unknown, record: PatientPathwayInstance) =>
        record.hospital_code || record.department_code || record.scope_code || "—",
    },
  ];

  return (
    <div className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <Title level={3} className={styles.pageTitle}>
            <UserOutlined /> 患者路径管理
          </Title>
          <p className={styles.pageSubtitle}>
            按路径 / 状态 / 患者维度筛选实例；点击行进入实例详情，可推进节点 / 任务 / 记录变异。
          </p>
        </div>
        <div className={styles.headerActions}>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setAdmitOpen(true)}
          >
            患者入径
          </Button>
        </div>
      </header>

      <div className={styles.patientToolbar} role="search">
        <Input
          className={styles.searchInput}
          prefix={<SearchOutlined />}
          allowClear
          placeholder="按路径编码（如 PATH_AMI_STEMI）"
          defaultValue={pathwayCode}
          onBlur={(e) => updateFilter("pathway_code", e.target.value.trim())}
          aria-label="patient-pathway-search"
        />
        <Input
          className={styles.searchInput}
          prefix={<UserOutlined />}
          allowClear
          placeholder="按患者 ID"
          defaultValue={patientId}
          onBlur={(e) => updateFilter("patient_id", e.target.value.trim())}
          aria-label="patient-id-filter"
        />
        <span className={styles.toolbarLabel}>状态：</span>
        <Select
          className={styles.filterSelect}
          value={status}
          options={STATUS_OPTIONS}
          onChange={(v) => updateFilter("status", v)}
          aria-label="instance-status-filter"
        />
      </div>

      <div className={styles.tableCard}>
        <Table<PatientPathwayInstance>
          rowKey="instance_id"
          dataSource={data ?? []}
          columns={columns}
          loading={isLoading}
          locale={{
            emptyText: (
              <div className={styles.tableEmpty}>
                <p>暂无患者路径实例</p>
                <p className={styles.tableEmptyHint}>点击「患者入径」开始第一条。</p>
              </div>
            ),
          }}
          pagination={{
            pageSize: 20,
            showSizeChanger: true,
            pageSizeOptions: ["10", "20", "50", "100"],
            showTotal: (total) => `共 ${total} 条`,
          }}
        />
      </div>

      <AdmitDialog
        open={admitOpen}
        onClose={() => setAdmitOpen(false)}
        onAdmitted={(instanceId) => {
          queryClient.invalidateQueries({ queryKey: ["patient-pathway-instances"] });
          navigate(`/pathway/patients/${encodeURIComponent(instanceId)}`);
        }}
      />
    </div>
  );
}
