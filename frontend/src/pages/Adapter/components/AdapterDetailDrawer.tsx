import { Drawer, Spin, Tag } from "antd";
import { useQuery } from "@tanstack/react-query";
import { ADAPTER_CATEGORY_LABELS, getAdapterDefinition } from "../../../api/adapterHub";
import type { AdapterDefinition } from "../../../api/adapterHub";
import styles from "../styles.module.css";

interface Props {
  open: boolean;
  adapterCode?: string;
  queryCode?: string;
  onClose: () => void;
}

/**
 * 业务适配器详情 Drawer（PR-FINAL-12）。
 *
 * 调 GET /api/adapters/definitions/{adapterCode}/{queryCode}，渲染：
 *  - 基础信息（编码/名称/分类/查询类型）
 *  - 接入端点（endpoint_url）
 *  - 请求模板（request_template，JSON 美化）
 *  - 响应映射（response_mapping，JSON 美化）
 */
export default function AdapterDetailDrawer({ open, adapterCode, queryCode, onClose }: Props) {
  const detailQuery = useQuery({
    queryKey: ["adapter-hub", "definition", adapterCode, queryCode],
    queryFn: () => getAdapterDefinition(adapterCode!, queryCode!),
    enabled: open && Boolean(adapterCode && queryCode),
  });

  const def: AdapterDefinition | undefined = detailQuery.data;

  return (
    <Drawer
      title={`业务适配器详情 · ${adapterCode ?? "-"} / ${queryCode ?? "-"}`}
      open={open}
      onClose={onClose}
      width={720}
      aria-label="adapter-definition-detail"
    >
      {detailQuery.isLoading ? (
        <Spin tip="加载中..." />
      ) : !def ? (
        <p className={styles.detailMissing}>未加载到适配器定义</p>
      ) : (
        <>
          <Section title="基础信息">
            <Pair label="编码" value={def.adapter_code} />
            <Pair label="名称" value={def.adapter_name} />
            <Pair
              label="分类"
              value={
                def.adapter_category
                  ? ADAPTER_CATEGORY_LABELS[def.adapter_category] ?? def.adapter_category
                  : undefined
              }
            />
            <Pair label="查询编码" value={def.query_code} />
            <Pair label="查询名称" value={def.query_name} />
            <Pair label="查询类型" value={def.query_type} />
            <Pair label="状态" value={renderEnabled(def.enabled)} />
            <Pair label="租户" value={def.tenant_id} />
            <Pair label="医院" value={def.hospital_code} />
            <Pair label="创建时间" value={def.created_time} />
            <Pair label="更新时间" value={def.updated_time} />
          </Section>

          <Section title="接入端点">
            <Pair
              label="endpoint_url"
              value={def.endpoint_url ? <code>{def.endpoint_url}</code> : undefined}
            />
            <Pair label="描述" value={def.description} />
          </Section>

          <Section title="请求模板">
            <PayloadBlock value={def.request_template} />
          </Section>

          <Section title="响应映射">
            <PayloadBlock value={def.response_mapping} />
          </Section>
        </>
      )}
    </Drawer>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className={styles.detailSection}>
      <div className={styles.detailSectionTitle}>{title}</div>
      <div className={styles.detailGrid}>{children}</div>
    </div>
  );
}

function Pair({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <>
      <div className={styles.detailLabel}>{label}</div>
      <div className={styles.detailValue}>
        {value === undefined || value === null || value === "" ? (
          <span className={styles.detailMissing}>—</span>
        ) : (
          value
        )}
      </div>
    </>
  );
}

function PayloadBlock({ value }: { value?: string | Record<string, unknown> }) {
  if (value === undefined || value === null || value === "") {
    return <div className={styles.detailMissing}>无配置</div>;
  }
  if (typeof value === "string") {
    try {
      const parsed = JSON.parse(value) as unknown;
      return <pre className={styles.payloadBlock}>{JSON.stringify(parsed, null, 2)}</pre>;
    } catch {
      return <pre className={styles.payloadBlock}>{value}</pre>;
    }
  }
  return <pre className={styles.payloadBlock}>{JSON.stringify(value, null, 2)}</pre>;
}

function renderEnabled(value?: boolean | string) {
  if (value === true || value === "Y" || value === "true") {
    return <Tag color="success">启用</Tag>;
  }
  if (value === false || value === "N" || value === "false") {
    return <Tag>停用</Tag>;
  }
  return undefined;
}
