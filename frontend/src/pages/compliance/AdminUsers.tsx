import { Table, Tag, Button, Space, Input } from "antd";
import { PlusOutlined, SearchOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";

const MOCK = [
  {
    id: "u1",
    name: "张三",
    workId: "MA-001",
    role: "医务处主任",
    dept: "医务处",
    active: true,
    lastLogin: "今天 09:12",
  },
  {
    id: "u2",
    name: "李四",
    workId: "IT-018",
    role: "信息科工程师",
    dept: "信息科",
    active: true,
    lastLogin: "今天 08:30",
  },
  {
    id: "u3",
    name: "王五",
    workId: "HZ-203",
    role: "心内科医生",
    dept: "心内科",
    active: true,
    lastLogin: "今天 07:45",
  },
  {
    id: "u4",
    name: "赵六",
    workId: "Q-088",
    role: "质控办主任",
    dept: "质控办",
    active: false,
    lastLogin: "3 天前",
  },
];

export default function AdminUsers() {
  return (
    <PageShell
      title="用户管理"
      description="医院员工身份、角色、权限；与身份绑定中的 SSO 自动同步"
      primary={
        <Button type="primary" icon={<PlusOutlined />}>
          新增用户
        </Button>
      }
    >
      <Space style={{ marginBottom: 12 }}>
        <Input.Search
          placeholder="搜姓名 / 工号"
          style={{ width: 280 }}
          prefix={<SearchOutlined />}
        />
      </Space>
      <Table
        rowKey="id"
        dataSource={MOCK}
        scroll={{ x: "max-content" }}
        pagination={{ pageSize: 20, showSizeChanger: true }}
        columns={[
          { title: "姓名", dataIndex: "name" },
          { title: "工号", dataIndex: "workId" },
          { title: "角色", dataIndex: "role", render: (v) => <Tag color="blue">{v}</Tag> },
          { title: "科室", dataIndex: "dept" },
          {
            title: "状态",
            dataIndex: "active",
            render: (v: boolean) => (v ? <Tag color="green">启用</Tag> : <Tag>停用</Tag>),
          },
          { title: "最后登录", dataIndex: "lastLogin" },
          {
            title: "操作",
            render: () => (
              <Button type="link" size="small">
                编辑角色
              </Button>
            ),
          },
        ]}
      />
    </PageShell>
  );
}
