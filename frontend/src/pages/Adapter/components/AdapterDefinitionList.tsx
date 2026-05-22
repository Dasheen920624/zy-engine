import { useState } from "react";
import { Alert, Spin, Table, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useQuery } from "@tanstack/react-query";
import { ADAPTER_CATEGORY_LABELS, listAdapterDefinitions } from "../../../api/adapterHub";
import type { AdapterDefinition } from "../../../api/adapterHub";
import AdapterDetailDrawer from "./AdapterDetailDrawer";
import styles from "../styles.module.css";

/**
 * 业务适配器（HIS/EMR/LIS）列表（PR-FINAL-12 Tab 1）。
 *
 * 调 GET /api/adapters/definitions 返回适配器 + 查询定义的笛卡尔行。
 * 行点击打开 AdapterDetailDrawer（按 adapter_code + query_code 二级主键）。
 */
export default function AdapterDefinitionList() {
  const [active, setActive] = useState<{ adapterCode?: string; queryCode?: string } | null>(null);

  const listQuery = useQuery({
    queryKey: ["adapter-hub", "definitions"],
    queryFn: listAdapterDefinitions,
  });

  const columns: ColumnsType<AdapterDefinition> = [
    {
      title: "适配器编码",
      dataIndex: "adapter_code",
      key: "adapter_code",
      width: 160,
      render: (v?: string) => (v ? <code>{v}</code> : "—"),
    },
    {
      title: "适配器名称",
      dataIndex: "adapter_name",
      key: "adapter_name",
      width: 200,
    },
    {
      title: "分类",
      dataIndex: "adapter_category",
      key: "adapter_category",
      width: 140,
      render: (v?: string) =>
        v ? <Tag>{ADAPTER_CATEGORY_LABELS[v] ?? v}</Tag> : "—",
    },
    {
      title: "查询编码",
      dataIndex: "query_code",
      key: "query_code",
      width: 160,
      render: (v?: string) => (v ? <code>{v}</code> : "—"),
    },
    {
      title: "查询名称",
      dataIndex: "query_name",
      key: "query_name",
      width: 180,
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

  return (
    <>
      <div className={styles.sectionHint}>
        业务适配器封装 HIS/EMR/LIS/PACS 等院内系统的具名查询（如「按门诊号取最新检验」），由 CDSS/路径/规则引擎按 adapter_code + query_code 调用。
      </div>
      {listQuery.isError ? (
        <Alert
          type="error"
          showIcon
          message="无法加载业务适配器列表"
          description={(listQuery.error as Error)?.message}
        />
      ) : listQuery.isLoading ? (
        <Spin tip="加载中..." />
      ) : (
        <Table<AdapterDefinition>
          rowKey={(row) =>
            `${row.adapter_code ?? ""}::${row.query_code ?? ""}`
          }
          columns={columns}
          dataSource={listQuery.data ?? []}
          pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }}
          size="small"
          onRow={(row) => ({
            onClick: () => {
              setActive({
                adapterCode: row.adapter_code,
                queryCode: row.query_code,
              });
            },
          })}
          aria-label="adapter-definition-table"
        />
      )}
      <AdapterDetailDrawer
        open={active !== null}
        adapterCode={active?.adapterCode}
        queryCode={active?.queryCode}
        onClose={() => setActive(null)}
      />
    </>
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
