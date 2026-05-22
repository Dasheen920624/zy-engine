import { Alert, Spin, Table, Tabs, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useQuery } from "@tanstack/react-query";
import {
  INTEROP_STANDARD_LABELS,
  listCdsHooksServices,
  listInteropAdapters,
  listSmartApps,
} from "../../../api/adapterHub";
import type { CdsHooksService, InteropAdapter, SmartApp } from "../../../api/adapterHub";
import styles from "../styles.module.css";

const { Paragraph } = Typography;

/**
 * 互联互通适配器列表（PR-FINAL-12 Tab 2）。
 *
 * 3 子 Tab 合并展示：
 *  - HL7/FHIR/CDA/IHE/DICOM 适配器（/interop/adapters）
 *  - CDS Hooks 服务（/interop/cds-hooks）
 *  - SMART on FHIR 应用（/interop/smart-apps）
 *
 * 全部只读列表（写操作走 import 或后端配置文件）。
 */
export default function InteropAdapterList() {
  const interopQuery = useQuery({
    queryKey: ["adapter-hub", "interop-adapters"],
    queryFn: listInteropAdapters,
  });
  const cdsQuery = useQuery({
    queryKey: ["adapter-hub", "cds-hooks"],
    queryFn: listCdsHooksServices,
  });
  const smartQuery = useQuery({
    queryKey: ["adapter-hub", "smart-apps"],
    queryFn: listSmartApps,
  });

  return (
    <>
      <Paragraph type="secondary" className={styles.sectionHint}>
        遵循 INTEROP-001 国情合规：HL7 v2 / FHIR R4 / CDA / IHE / DICOM 标准适配，
        CDS Hooks 与 SMART on FHIR 用于第三方临床决策插件接入。
      </Paragraph>
      <Tabs
        items={[
          {
            key: "interop",
            label: "标准适配器",
            children: (
              <InteropTable
                rows={interopQuery.data}
                loading={interopQuery.isLoading}
                error={interopQuery.error as Error | undefined}
              />
            ),
          },
          {
            key: "cds-hooks",
            label: "CDS Hooks 服务",
            children: (
              <CdsHooksTable
                rows={cdsQuery.data}
                loading={cdsQuery.isLoading}
                error={cdsQuery.error as Error | undefined}
              />
            ),
          },
          {
            key: "smart-apps",
            label: "SMART apps",
            children: (
              <SmartAppsTable
                rows={smartQuery.data}
                loading={smartQuery.isLoading}
                error={smartQuery.error as Error | undefined}
              />
            ),
          },
        ]}
      />
    </>
  );
}

function InteropTable({
  rows,
  loading,
  error,
}: {
  rows?: InteropAdapter[];
  loading: boolean;
  error?: Error;
}) {
  const columns: ColumnsType<InteropAdapter> = [
    { title: "编码", dataIndex: "adapter_code", key: "adapter_code", width: 200 },
    { title: "名称", dataIndex: "adapter_name", key: "adapter_name", width: 200 },
    {
      title: "标准",
      dataIndex: "standard",
      key: "standard",
      width: 160,
      render: (v?: string) =>
        v ? <Tag color="processing">{INTEROP_STANDARD_LABELS[v] ?? v}</Tag> : "—",
    },
    {
      title: "端点",
      dataIndex: "endpoint_url",
      key: "endpoint_url",
      render: (v?: string) =>
        v ? <span className={styles.endpointCell}>{v}</span> : "—",
    },
    {
      title: "状态",
      dataIndex: "enabled",
      key: "enabled",
      width: 80,
      render: (v?: boolean | string) => renderEnabled(v),
    },
  ];
  return renderTable(columns, rows, loading, error, "interop-adapter-table");
}

function CdsHooksTable({
  rows,
  loading,
  error,
}: {
  rows?: CdsHooksService[];
  loading: boolean;
  error?: Error;
}) {
  const columns: ColumnsType<CdsHooksService> = [
    { title: "服务编码", dataIndex: "service_code", key: "service_code", width: 200 },
    { title: "Hook 名", dataIndex: "hook", key: "hook", width: 180 },
    { title: "标题", dataIndex: "title", key: "title" },
    {
      title: "端点",
      dataIndex: "endpoint_url",
      key: "endpoint_url",
      render: (v?: string) =>
        v ? <span className={styles.endpointCell}>{v}</span> : "—",
    },
    {
      title: "状态",
      dataIndex: "enabled",
      key: "enabled",
      width: 80,
      render: (v?: boolean | string) => renderEnabled(v),
    },
  ];
  return renderTable(columns, rows, loading, error, "cds-hooks-table");
}

function SmartAppsTable({
  rows,
  loading,
  error,
}: {
  rows?: SmartApp[];
  loading: boolean;
  error?: Error;
}) {
  const columns: ColumnsType<SmartApp> = [
    { title: "应用编码", dataIndex: "app_code", key: "app_code", width: 200 },
    { title: "应用名称", dataIndex: "app_name", key: "app_name" },
    {
      title: "Launch URL",
      dataIndex: "launch_url",
      key: "launch_url",
      render: (v?: string) =>
        v ? <span className={styles.endpointCell}>{v}</span> : "—",
    },
    { title: "OAuth Scope", dataIndex: "scope", key: "scope", width: 200 },
    {
      title: "状态",
      dataIndex: "enabled",
      key: "enabled",
      width: 80,
      render: (v?: boolean | string) => renderEnabled(v),
    },
  ];
  return renderTable(columns, rows, loading, error, "smart-apps-table");
}

function renderTable<T extends object>(
  columns: ColumnsType<T>,
  rows: T[] | undefined,
  loading: boolean,
  error: Error | undefined,
  ariaLabel: string,
) {
  if (error) {
    return (
      <Alert
        type="error"
        showIcon
        message="无法加载列表"
        description={error.message}
      />
    );
  }
  if (loading) {
    return <Spin tip="加载中..." />;
  }
  return (
    <Table<T>
      rowKey={(row, index) => String((row as Record<string, unknown>).adapter_code ?? (row as Record<string, unknown>).service_code ?? (row as Record<string, unknown>).app_code ?? index)}
      columns={columns}
      dataSource={rows ?? []}
      pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }}
      size="small"
      aria-label={ariaLabel}
    />
  );
}

function renderEnabled(value?: boolean | string) {
  if (value === true || value === "Y" || value === "true") {
    return <Tag color="success" className={styles.enabledTag}>启用</Tag>;
  }
  if (value === false || value === "N" || value === "false") {
    return <Tag className={styles.disabledTag}>停用</Tag>;
  }
  return <Tag>—</Tag>;
}
