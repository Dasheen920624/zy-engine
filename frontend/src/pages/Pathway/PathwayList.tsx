import React from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button, Input, Select, Table, Typography, Space } from "antd";
import { PlusOutlined, SearchOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import { StatusBadge, OrgContextSelector } from "../../components";
import { listPathways } from "../../api/pathway";
import type { PathwaySummary, ListPathwaysParams } from "../../api/types";
import ActionMenu from "./components/ActionMenu";
import styles from "./styles.module.css";

const { Text, Title } = Typography;

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
    <div className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <Title level={3} className={styles.pageTitle}>
            路径配置
          </Title>
          <Text type="secondary" className={styles.pageSubtitle}>
            从专病模板开始配置入径条件、关键节点和发布版本；专家能力在详情页继续展开。
          </Text>
        </div>
      </header>

      <Space className={styles.listToolbar} size="middle">
        <OrgContextSelector />
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => navigate("/pathway/templates/new/edit")}
        >
          新建专病路径
        </Button>
        <Input
          placeholder="搜索路径"
          prefix={<SearchOutlined />}
          allowClear
          value={search || ""}
          onChange={(e) => updateFilter("search", e.target.value || undefined)}
          className={styles.searchInput}
        />
        <Select
          value={status || ""}
          onChange={(v) => updateFilter("status", v || undefined)}
          options={statusOptions}
          className={styles.statusFilter}
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
        })}
        rowClassName={() => styles.clickableRow}
        locale={{
          emptyText: (
            <div className={styles.tableEmpty}>
              <Typography.Text type="secondary">还没有可用于试点的路径配置</Typography.Text>
              <br />
              <Button
                type="primary"
                icon={<PlusOutlined />}
                className={styles.emptyActionButton}
                onClick={() => navigate("/pathway/templates/new/edit")}
              >
                新建专病路径
              </Button>
            </div>
          ),
        }}
      />
    </div>
  );
};

export default PathwayList;
