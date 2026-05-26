import { Tag, Popover, Typography, Space, List } from "antd";
import { SafetyOutlined } from "@ant-design/icons";

const { Text } = Typography;

/**
 * 权限指纹 chip。
 *
 * 任何页面右上角显示当前用户"能改什么 / 能看什么"，减少 90% 客户工单。
 *
 * 当前实装：骨架，从 mock 角色读权限。
 * GA-COMPLIANCE-01 实装后从 /api/v1/security/permissions 读真实权限。
 */
const MOCK_PERMS = {
  role: "医务处主任",
  canView: ["全院质控指标", "AI 提醒治理", "审计日志"],
  canEdit: ["质控规则", "提醒阈值", "整改派单"],
  canPublish: ["医保规则月更", "评估指标库"],
  cannotDo: ["国密密钥轮换", "Provider 配置", "数据库连接"],
};

export function PermissionChip() {
  const content = (
    <Space direction="vertical" size="middle" className="mk-popover-compact">
      <Text>
        当前角色：<Tag color="blue">{MOCK_PERMS.role}</Tag>
      </Text>
      <PermSection title="可查看" items={MOCK_PERMS.canView} color="default" />
      <PermSection title="可编辑" items={MOCK_PERMS.canEdit} color="processing" />
      <PermSection title="可发布" items={MOCK_PERMS.canPublish} color="success" />
      <PermSection title="不能做" items={MOCK_PERMS.cannotDo} color="default" italic />
      <Text type="secondary" className="mk-text-xs">
        权限由信息科主任在合规运维 → 用户管理配置。
      </Text>
    </Space>
  );

  return (
    <Popover content={content} trigger="click" placement="bottomRight" title="我的权限指纹">
      <Tag icon={<SafetyOutlined />} color="blue" className="mk-clickable">
        {MOCK_PERMS.role}
      </Tag>
    </Popover>
  );
}

function PermSection({
  title,
  items,
  color,
  italic = false,
}: {
  title: string;
  items: string[];
  color: string;
  italic?: boolean;
}) {
  return (
    <div>
      <Text strong className="mk-text-xs">
        {title}
      </Text>
      <List
        size="small"
        dataSource={items}
        renderItem={(t) => (
          <List.Item className="mk-list-item-compact">
            <Tag color={color} className={italic ? "mk-tag-muted-italic" : undefined}>
              {t}
            </Tag>
          </List.Item>
        )}
      />
    </div>
  );
}
