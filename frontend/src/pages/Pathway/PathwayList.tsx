import React from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button, Input, Select, Table, Typography, Space } from "antd";
import { PlusOutlined, SearchOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import { StatusBadge } from "../../components";
import { listPathways } from "../../api/pathway";
import type { PathwaySummary, ListPathwaysParams } from "../../api/types";
import ActionMenu from "./components/ActionMenu";

const { Title } = Typography;

const statusOptions = [
  { value: "", label: "全部" },
  { value: "DRAFT", label: "草稿" },
  { value: "PUBLISHED", label: "已发布" },
];

const PathwayList: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const search = searchParams.get("search") || undefined;
  const status = searchParams.get("status") || undefined;
  const dept = searchParams.get("dept") || undefined;
  const page = Number(searchParams.get("page")) || 1;
  const size = Number(searchParams.get("size")) || 20;

  const params: ListPathwaysParams = { search, status, dept, page, size };

  const { data, isLoading, refetch } = useQuery({
    queryKey: ["pathways", params],
    queryFn: () => listPathways(params),
  });

  const updateFilter = (key: string, value: string | undefined) => {
    const next = new URLSearchParams(searchParams);
    if (value) {
      next.set(key, value);
    } else {
      next.delete(key);
    }
    next.set("page", "1");
    setSearchParams(next);
  };

  const columns = [
    {
      title: "路径名称",
      dataIndex: "pathway_name",
      key: "pathway_name",
      render: (name: string, record: PathwaySummary) => (
        <a onClick={() => navigate(`/pathway/templates/${record.pathway_code}`)}>
          {name || record.pathway_code}
        </a>
      ),
    },
    {
      title: "版本",
      dataIndex: "active_published_version",
      key: "version",
      render: (v: string | null) => v || "—",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      render: (s: string) => <StatusBadge status={s as never} />,
    },
    {
      title: "入径数",
      dataIndex: "instance_count",
      key: "instance_count",
      render: (n: number) => (n > 0 ? n : "—"),
    },
    {
      title: "完成率",
      dataIndex: "completion_rate",
      key: "completion_rate",
      render: (r: number) => (r > 0 ? `${r}%` : "—"),
    },
    {
      title: "操作",
      key: "action",
      width: 80,
      render: (_: unknown, record: PathwaySummary) => (
        <ActionMenu
          row={record}
          onRefresh={() => refetch()}
          onView={(code) => navigate(`/pathway/templates/${code}`)}
          onEdit={(code) => navigate(`/pathway/templates/${code}/edit`)}
        />
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={3} style={{ marginBottom: 24 }}>
        路径模板库
      </Title>

      <Space style={{ marginBottom: 16 }} size="middle">
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => navigate("/pathway/templates/new/edit")}
        >
          新建路径
        </Button>
        <Input
          placeholder="搜索路径名称或编码"
          prefix={<SearchOutlined />}
          allowClear
          value={search || ""}
          onChange={(e) => updateFilter("search", e.target.value || undefined)}
          style={{ width: 240 }}
        />
        <Select
          value={status || ""}
          onChange={(v) => updateFilter("status", v || undefined)}
          options={statusOptions}
          style={{ width: 120 }}
        />
      </Space>

      <Table
        columns={columns}
        dataSource={data?.items || []}
        rowKey="pathway_code"
        loading={isLoading}
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
        onRow={(record) => ({
          onClick: () => navigate(`/pathway/templates/${record.pathway_code}`),
          style: { cursor: "pointer" },
        })}
        locale={{
          emptyText: (
            <div style={{ padding: "40px 0" }}>
              <Typography.Text type="secondary">还没有路径模板</Typography.Text>
              <br />
              <Button
                type="primary"
                icon={<PlusOutlined />}
                style={{ marginTop: 16 }}
                onClick={() => navigate("/pathway/templates/new/edit")}
              >
                新建路径
              </Button>
            </div>
          ),
        }}
      />
    </div>
  );
};

export default PathwayList;
