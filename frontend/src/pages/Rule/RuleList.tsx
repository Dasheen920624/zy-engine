/**
 * 规则库列表页（路由 /rule/definitions）。
 *
 * 支持搜索（rule_code / rule_name 模糊）、类型筛选、状态筛选、新建规则跳编辑器。
 * 组织上下文走 client.ts 自动注入 Header，不在 URL 中携带。
 */

import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button, Input, Select, Space, Table, Typography } from "antd";
import { PlusOutlined, SafetyCertificateOutlined, SearchOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import { listRules, type RuleDefinition, type RuleStatus, type RuleType } from "../../api/rule";
import RuleTypeTag from "./components/RuleTypeTag";
import SeverityTag from "./components/SeverityTag";
import {
  describeStatus,
  formatPublishedTime,
  formatRuleScope,
  RULE_TYPE_LABELS,
  STATUS_LABELS,
} from "./helpers/ruleFormatters";
import styles from "./styles.module.css";

const { Title } = Typography;

const TYPE_OPTIONS: Array<{ value: RuleType | ""; label: string }> = [
  { value: "", label: "全部类型" },
  ...(Object.entries(RULE_TYPE_LABELS) as Array<[RuleType, string]>).map(([value, label]) => ({
    value,
    label,
  })),
];

const STATUS_OPTIONS: Array<{ value: RuleStatus | ""; label: string }> = [
  { value: "", label: "全部状态" },
  ...(Object.entries(STATUS_LABELS) as Array<[string, string]>).map(([value, label]) => ({
    value: value as RuleStatus,
    label,
  })),
];

export default function RuleList() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [localSearch, setLocalSearch] = useState(searchParams.get("search") ?? "");

  const search = searchParams.get("search") ?? "";
  const ruleType = (searchParams.get("rule_type") ?? "") as RuleType | "";
  const status = (searchParams.get("status") ?? "") as RuleStatus | "";

  const { data, isLoading, refetch } = useQuery({
    queryKey: ["rules", ruleType, status],
    queryFn: () =>
      listRules({
        rule_type: ruleType || undefined,
        status: status || undefined,
      }),
  });

  const rules = (data ?? []).filter((r) =>
    search ? (r.rule_code + r.rule_name).toLowerCase().includes(search.toLowerCase()) : true,
  );

  const updateFilter = (key: string, value: string) => {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value);
    else next.delete(key);
    setSearchParams(next, { replace: true });
  };

  const handleSearchSubmit = () => {
    updateFilter("search", localSearch.trim());
  };

  const columns = [
    {
      title: "规则",
      dataIndex: "rule_name",
      key: "rule_name",
      render: (_: string, record: RuleDefinition) => (
        <div>
          <a
            className={styles.ruleNameLink}
            onClick={() => navigate(`/rule/definitions/${encodeURIComponent(record.rule_code)}`)}
          >
            {record.rule_name || record.rule_code}
          </a>
          <span className={styles.ruleCodeMono}>{record.rule_code}</span>
        </div>
      ),
    },
    {
      title: "类型",
      dataIndex: "rule_type",
      key: "rule_type",
      width: 120,
      render: (t: string) => <RuleTypeTag ruleType={t as RuleType} />,
    },
    {
      title: "严重度",
      dataIndex: "severity",
      key: "severity",
      width: 90,
      render: (s: string) => <SeverityTag severity={s} />,
    },
    {
      title: "版本",
      dataIndex: "version_no",
      key: "version_no",
      width: 90,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 90,
      render: (s: string) => describeStatus(s),
    },
    {
      title: "组织范围",
      key: "scope",
      render: (_: unknown, record: RuleDefinition) => formatRuleScope(record),
    },
    {
      title: "发布时间",
      dataIndex: "published_time",
      key: "published_time",
      width: 160,
      render: (t: string) => formatPublishedTime(t),
    },
    {
      title: "操作",
      key: "action",
      width: 140,
      render: (_: unknown, record: RuleDefinition) => (
        <Space size="small">
          <a onClick={() => navigate(`/rule/definitions/${encodeURIComponent(record.rule_code)}`)}>
            查看
          </a>
          <a onClick={() => navigate(`/rule/definitions/${encodeURIComponent(record.rule_code)}/edit`)}>
            编辑
          </a>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <Title level={3} className={styles.pageTitle}>
            <SafetyCertificateOutlined /> 规则库
          </Title>
          <p className={styles.pageSubtitle}>
            从医保审核、医嘱安全、路径质控模板开始配置规则；系统自动校验来源、影响范围和发布风险。
          </p>
        </div>
        <div className={styles.headerActions}>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => navigate("/rule/definitions/new/edit")}
          >
            新建规则
          </Button>
        </div>
      </header>

      <div className={styles.toolbar} role="search">
        <Input
          className={styles.searchInput}
          allowClear
          prefix={<SearchOutlined />}
          placeholder="搜索规则"
          value={localSearch}
          onChange={(e) => setLocalSearch(e.target.value)}
          onPressEnter={handleSearchSubmit}
          onBlur={handleSearchSubmit}
          aria-label="rule-search"
        />
        <span className={styles.toolbarLabel}>类型：</span>
        <Select
          className={styles.filterSelect}
          value={ruleType}
          options={TYPE_OPTIONS}
          onChange={(v) => updateFilter("rule_type", v)}
          aria-label="rule-type-filter"
        />
        <span className={styles.toolbarLabel}>状态：</span>
        <Select
          className={styles.filterSelect}
          value={status}
          options={STATUS_OPTIONS}
          onChange={(v) => updateFilter("status", v)}
          aria-label="rule-status-filter"
        />
        <Button onClick={() => refetch()}>刷新</Button>
      </div>

      <div className={styles.tableCard}>
        <Table<RuleDefinition>
          rowKey={(r) => `${r.rule_code}@${r.version_no}`}
          dataSource={rules}
          columns={columns}
          loading={isLoading}
          locale={{
            emptyText: (
              <div className={styles.tableEmpty}>
                <p>暂无规则</p>
                <p className={styles.tableEmptyHint}>
                  你可以新建第一条规则，或从配置包中心导入本院规则集。
                </p>
              </div>
            ),
          }}
          pagination={{
            pageSize: 20,
            showSizeChanger: true,
            pageSizeOptions: ["10", "20", "50"],
            showTotal: (total) => `共 ${total} 条`,
          }}
        />
      </div>
    </div>
  );
}
