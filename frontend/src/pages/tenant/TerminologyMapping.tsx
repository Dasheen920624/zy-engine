import { useMemo, useState } from "react";

import { ImportOutlined } from "@ant-design/icons";
import { Button, Space, Table, Tag } from "antd";

import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { MetricGrid } from "@/shared/ui/MetricGrid";
import { useTerminologyMappings, type TermMapping } from "@/shared/api/hooks";

const STATUS_COLOR: Record<TermMapping["status"], string> = {
  CONFIRMED: "green",
  DRAFT: "orange",
  SUPERSEDED: "blue",
  ROLLED_BACK: "red",
};

const STATUS_LABEL: Record<TermMapping["status"], string> = {
  CONFIRMED: "已确认",
  DRAFT: "草稿",
  SUPERSEDED: "已替换",
  ROLLED_BACK: "已回滚",
};

const RISK_COLOR: Record<TermMapping["riskLevel"], string> = {
  HIGH: "red",
  MEDIUM: "orange",
  LOW: "blue",
};

const PAGE_SIZE = 20;

export default function TerminologyMapping() {
  const [page, setPage] = useState(1);

  const query = useTerminologyMappings({ page, size: PAGE_SIZE });
  const items = useMemo(() => query.data?.items ?? [], [query.data]);

  let pageState: "loading" | "error" | "empty" | "ready" = "ready";
  if (query.isLoading) pageState = "loading";
  else if (query.isError) pageState = "error";
  else if (items.length === 0) pageState = "empty";

  return (
    <PageShell
      title="字典映射"
      description="把医院码映射到 ICD-10 / ICD-11 / LOINC / SNOMED CT，AI 推荐 + 人工确认"
      primary={
        <Button type="primary" icon={<ImportOutlined />} disabled>
          导入医院字典
        </Button>
      }
    >
      <MetricGrid
        items={[
          { key: "total", title: "总条目", value: query.data?.total ?? 0 },
          {
            key: "confirmed",
            title: "已确认",
            value: items.filter((m) => m.status === "CONFIRMED").length,
            tone: "success",
          },
          {
            key: "draft",
            title: "待确认",
            value: items.filter((m) => m.status === "DRAFT").length,
            tone: "warning",
          },
          {
            key: "superseded",
            title: "已替换 / 回滚",
            value: items.filter((m) => m.status === "SUPERSEDED" || m.status === "ROLLED_BACK")
              .length,
            tone: "primary",
          },
        ]}
      />
      <PageState
        state={pageState}
        title={pageState === "empty" ? "暂无字典映射条目" : undefined}
        description={
          pageState === "empty" ? "可通过导入医院字典或在候选库中确认映射；引擎已就绪" : undefined
        }
        onRetry={query.refetch}
      >
        <Table<TermMapping>
          rowKey="id"
          dataSource={items}
          scroll={{ x: "max-content" }}
          pagination={{
            current: query.data?.page ?? page,
            pageSize: query.data?.size ?? PAGE_SIZE,
            total: query.data?.total ?? 0,
            showSizeChanger: false,
            onChange: (p) => setPage(p),
          }}
          columns={[
            { title: "院内编码 ID", dataIndex: "localTermId" },
            { title: "标准编码 ID", dataIndex: "standardTermId" },
            { title: "来源系统", dataIndex: "sourceSystem" },
            { title: "类别", dataIndex: "category", render: (v) => <Tag>{v}</Tag> },
            {
              title: "风险等级",
              dataIndex: "riskLevel",
              render: (v: TermMapping["riskLevel"]) => <Tag color={RISK_COLOR[v]}>{v}</Tag>,
            },
            {
              title: "置信度",
              dataIndex: "confidence",
              render: (v: number) => `${(v * 100).toFixed(1)}%`,
            },
            {
              title: "状态",
              dataIndex: "status",
              render: (v: TermMapping["status"]) => (
                <Tag color={STATUS_COLOR[v]}>{STATUS_LABEL[v]}</Tag>
              ),
            },
            {
              title: "操作",
              render: () => (
                <Space>
                  <Button type="link" size="small" disabled>
                    查看
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </PageState>
    </PageShell>
  );
}
