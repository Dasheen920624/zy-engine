import { useState } from "react";
import {
  Button,
  Input,
  Popconfirm,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip,
  Typography,
} from "antd";
import type { ColumnsType, TablePaginationConfig } from "antd/es/table";
import {
  LockOutlined,
  ReloadOutlined,
  SearchOutlined,
  TeamOutlined,
  UnlockOutlined,
  UploadOutlined,
} from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import dayjs from "dayjs";
import {
  listUsers,
  updateUserStatus,
  unlockUser,
  STATUS_COLORS,
  STATUS_LABELS,
  USER_TYPE_LABELS,
  type AdminUser,
  type UserListFilters,
} from "../../../api/userAdmin";
import UserDetail from "./UserDetail";
import RoleAssignDialog from "./RoleAssignDialog";
import CsvImportDialog from "./CsvImportDialog";
import styles from "./styles.module.css";

const { Title, Paragraph } = Typography;

const DEFAULT_FILTERS: UserListFilters = { page: 1, size: 20 };

const STATUS_OPTIONS = [
  { label: "全部状态", value: "" },
  { label: "正常", value: "ACTIVE" },
  { label: "禁用", value: "DISABLED" },
];

/**
 * /admin/users 用户管理主页（PR-FINAL-08）。
 *
 * 功能：
 *  - 关键字 / 状态 / 角色筛选 + 分页列表
 *  - 启用 / 禁用 / 解锁用户（行内操作）
 *  - 点击行打开 UserDetail Drawer（含身份绑定 + 登录失败次数）
 *  - 角色分配 Dialog（幂等替换）
 *  - CSV 批量导入 Dialog（GB18030 支持）
 *
 * 等保 2.0 三级合规：
 *  - 锁定行用红色高亮显示
 *  - 登录失败次数在列表可见
 */
export default function UserList() {
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState<UserListFilters>(DEFAULT_FILTERS);
  const [applied, setApplied] = useState<UserListFilters>(DEFAULT_FILTERS);

  // Drawer / Dialog 状态
  const [detailUserId, setDetailUserId] = useState<number | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [roleUser, setRoleUser] = useState<AdminUser | null>(null);
  const [roleOpen, setRoleOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);

  const listQuery = useQuery({
    queryKey: ["admin-users", "list", applied],
    queryFn: () => listUsers(applied),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: "ACTIVE" | "DISABLED" }) =>
      updateUserStatus(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-users"] }),
  });

  const unlockMutation = useMutation({
    mutationFn: (id: number) => unlockUser(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-users"] }),
  });

  const applyFilters = () => setApplied({ ...draft, page: 1 });
  const resetFilters = () => {
    setDraft(DEFAULT_FILTERS);
    setApplied(DEFAULT_FILTERS);
  };

  const handleTableChange = (pagination: TablePaginationConfig) => {
    const next = { ...applied, page: pagination.current ?? 1, size: pagination.pageSize ?? 20 };
    setApplied(next);
  };

  const openDetail = (user: AdminUser) => {
    setDetailUserId(user.id);
    setDetailOpen(true);
  };

  const openRoleAssign = (user: AdminUser, e: React.MouseEvent) => {
    e.stopPropagation();
    setRoleUser(user);
    setRoleOpen(true);
  };

  const isLocked = (user: AdminUser) =>
    !!user.locked_until && dayjs(user.locked_until).isAfter(dayjs());

  const columns: ColumnsType<AdminUser> = [
    {
      title: "用户名",
      dataIndex: "username",
      width: 140,
      render: (v, row) => (
        <Button type="link" size="small" onClick={() => openDetail(row)} className={styles.usernameLinkBtn}>
          {v}
        </Button>
      ),
    },
    {
      title: "显示名",
      dataIndex: "display_name",
      width: 120,
    },
    {
      title: "类型",
      dataIndex: "user_type",
      width: 90,
      render: (v) => USER_TYPE_LABELS[v] ?? v,
    },
    {
      title: "状态",
      dataIndex: "status",
      width: 80,
      render: (_, row) => {
        const locked = isLocked(row);
        const label = locked ? "已锁定" : (STATUS_LABELS[row.status] ?? row.status);
        const color = locked ? "error" : (STATUS_COLORS[row.status] ?? "default");
        return <Tag color={color}>{label}</Tag>;
      },
    },
    {
      title: "失败次数",
      dataIndex: "login_attempts",
      width: 80,
      align: "center",
      render: (v) => {
        if (v >= 5) return <Tag color="error">{v}</Tag>;
        if (v >= 3) return <Tag color="warning">{v}</Tag>;
        return v;
      },
    },
    {
      title: "角色",
      dataIndex: "roles",
      render: (roles: string[]) =>
        roles.length === 0 ? (
          <span className={styles.emptyText}>无角色</span>
        ) : (
          roles.slice(0, 2).map((r) => (
            <Tag key={r} color="blue" className={styles.roleTagSmall}>
              {r}
            </Tag>
          ))
        ),
    },
    {
      title: "最近登录",
      dataIndex: "last_login_time",
      width: 150,
      render: (v) =>
        v ? dayjs(v).format("YYYY-MM-DD HH:mm") : <span className={styles.neverLoginText}>从未</span>,
    },
    {
      title: "操作",
      width: 200,
      render: (_, row) => (
        <Space size={4} onClick={(e) => e.stopPropagation()}>
          <Tooltip title="分配角色">
            <Button
              size="small"
              icon={<TeamOutlined />}
              onClick={(e) => openRoleAssign(row, e)}
            />
          </Tooltip>
          {isLocked(row) && (
            <Tooltip title="解锁">
              <Popconfirm
                title="确认解锁此用户？"
                onConfirm={() => unlockMutation.mutate(row.id)}
                okText="解锁"
                cancelText="取消"
              >
                <Button
                  size="small"
                  icon={<UnlockOutlined />}
                  loading={unlockMutation.isPending}
                />
              </Popconfirm>
            </Tooltip>
          )}
          {row.status === "ACTIVE" ? (
            <Popconfirm
              title="确认禁用此用户？"
              onConfirm={() => statusMutation.mutate({ id: row.id, status: "DISABLED" })}
              okText="禁用"
              cancelText="取消"
              okButtonProps={{ danger: true }}
            >
              <Button
                size="small"
                danger
                icon={<LockOutlined />}
                loading={statusMutation.isPending}
              >
                禁用
              </Button>
            </Popconfirm>
          ) : (
            <Popconfirm
              title="确认启用此用户？"
              onConfirm={() => statusMutation.mutate({ id: row.id, status: "ACTIVE" })}
              okText="启用"
              cancelText="取消"
            >
              <Button size="small" loading={statusMutation.isPending}>
                启用
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  const page = listQuery.data;
  const total = page?.total ?? 0;

  return (
    <div>
      {/* 页头 */}
      <div className={styles.pageHeader}>
        <Title level={4} className={styles.pageTitle}>
          用户管理
        </Title>
        <Paragraph className={styles.pageHint}>
          管理平台用户账户、角色分配及账户状态。锁定行（红色）表示当前账户已被锁定，点击解锁恢复访问。
        </Paragraph>
      </div>

      {/* 工具栏 */}
      <div className={styles.toolbar}>
        <div className={styles.toolbarItem}>
          <label className={styles.toolbarItemLabel}>关键字</label>
          <Input
            placeholder="用户名 / 显示名 / 邮箱"
            value={draft.keyword ?? ""}
            onChange={(e) => setDraft((d) => ({ ...d, keyword: e.target.value }))}
            onPressEnter={applyFilters}
            prefix={<SearchOutlined />}
            allowClear
          />
        </div>
        <div className={styles.toolbarItem}>
          <label className={styles.toolbarItemLabel}>状态</label>
          <Select
            value={draft.status ?? ""}
            onChange={(v) => setDraft((d) => ({ ...d, status: v }))}
            options={STATUS_OPTIONS}
          />
        </div>
        <div className={styles.toolbarActions}>
          <Button type="primary" onClick={applyFilters} icon={<SearchOutlined />}>
            搜索
          </Button>
          <Button onClick={resetFilters}>重置</Button>
          <Button
            onClick={() => {
              listQuery.refetch();
            }}
            icon={<ReloadOutlined />}
          />
          <Button
            icon={<UploadOutlined />}
            onClick={() => setImportOpen(true)}
          >
            批量导入
          </Button>
        </div>
      </div>

      {/* 列表 */}
      <div className={styles.listSection}>
        <div className={styles.listToolbar}>
          <span className={styles.listToolbarHint}>
            共 {total} 个用户
          </span>
        </div>
        {listQuery.isLoading ? (
          <Spin />
        ) : (
          <Table
            dataSource={page?.items ?? []}
            columns={columns}
            rowKey="id"
            size="small"
            onRow={(row) => ({ onClick: () => openDetail(row), style: { cursor: "pointer" } })}
            rowClassName={(row) =>
              isLocked(row) ? styles.lockedRow : ""
            }
            pagination={{
              current: applied.page ?? 1,
              pageSize: applied.size ?? 20,
              total,
              showSizeChanger: true,
              pageSizeOptions: ["10", "20", "50", "100"],
              showTotal: (t) => `共 ${t} 条`,
            }}
            onChange={handleTableChange}
          />
        )}
      </div>

      {/* 详情 Drawer */}
      <UserDetail
        userId={detailUserId}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
      />

      {/* 角色分配 Dialog */}
      <RoleAssignDialog
        user={roleUser}
        open={roleOpen}
        onClose={() => setRoleOpen(false)}
      />

      {/* CSV 导入 Dialog */}
      <CsvImportDialog open={importOpen} onClose={() => setImportOpen(false)} />
    </div>
  );
}
