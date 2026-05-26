import { Row, Col, Card, Space, Typography, List, Button } from "antd";
import {
  ClusterOutlined,
  SafetyCertificateOutlined,
  AuditOutlined,
  AlertOutlined,
  RocketOutlined,
} from "@ant-design/icons";
import { StatusBadge } from "@/shared/ui/StatusBadge";
import { TenantLifecyclePanel } from "@/features/tenant-lifecycle/TenantLifecyclePanel";
import { MetricGrid } from "@/shared/ui/MetricGrid";
import { PageState } from "@/shared/ui/PageState";

/**
 * 工作台：一屏看懂系统健康、试点进度、待办和生命周期。
 *
 * 严格遵守 docs/CONSTITUTION.md §5 第 1 条：院长首屏 ≤ 10 秒看懂今天系统状态，0 技术名词。
 * 不滚动即可看完核心四件事：今天系统健康、试点进度、待办、生命周期。
 */
export function WorkbenchPanel() {
  const todoMock = [
    {
      id: 1,
      title: "神经内科卒中路径还差 2 节点",
      actor: "信息科 · 李四",
      status: "in_progress" as const,
    },
    { id: 2, title: "第 2 院区 HIS 适配器待联通", actor: "实施工程师", status: "unread" as const },
    { id: 3, title: "DRG 8 月规则未导入", actor: "医保办", status: "unread" as const },
  ];

  return (
    <PageState state="ready">
      <Space direction="vertical" size="large" className="mk-full-width">
        {/* 1. 院级生命周期面板 */}
        <TenantLifecyclePanel />

        {/* 2. 今日健康四指标（一眼看完） */}
        <MetricGrid
          items={[
            {
              key: "pilot",
              title: "试点准备 · 在径科室",
              value: 5,
              suffix: "/ 8",
              prefix: <ClusterOutlined />,
              tone: "primary",
            },
            {
              key: "clinical",
              title: "临床运行 · 今日提醒",
              value: 1283,
              prefix: <SafetyCertificateOutlined />,
              tone: "success",
            },
            {
              key: "quality",
              title: "质控改进 · 未闭环",
              value: 17,
              prefix: <AlertOutlined />,
              tone: "warning",
            },
            {
              key: "compliance",
              title: "合规运维 · 待审",
              value: 3,
              prefix: <AuditOutlined />,
              tone: "primary",
            },
          ]}
        />

        {/* 3. 本周建议动作 */}
        <Card
          title="本周建议动作"
          extra={<Typography.Text type="secondary">{todoMock.length} 项</Typography.Text>}
        >
          <List
            dataSource={todoMock}
            renderItem={(item) => (
              <List.Item
                actions={[
                  <Button type="link" key="act">
                    处理 →
                  </Button>,
                ]}
              >
                <Space>
                  <StatusBadge machine="todo" status={item.status} />
                  <Typography.Text>{item.title}</Typography.Text>
                  <Typography.Text type="secondary">· {item.actor}</Typography.Text>
                </Space>
              </List.Item>
            )}
          />
        </Card>

        {/* 4. 演示与校验入口 */}
        <Card
          title={
            <Space>
              <RocketOutlined />
              演示与校验
            </Space>
          }
          extra={
            <Typography.Text type="secondary">
              6 大客户验收剧本，30 分钟跑完即懂业务价值
            </Typography.Text>
          }
        >
          <Row gutter={[12, 12]}>
            {[
              "S1 院级质控驾驶舱",
              "S2 AMI 路径发布",
              "S3 HIS 嵌入式推荐",
              "S4 提醒疲劳治理",
              "S5 AI 知识审核",
              "S6 身份联邦",
            ].map((s) => (
              <Col span={8} key={s}>
                <Button block>{s}</Button>
              </Col>
            ))}
          </Row>
        </Card>
      </Space>
    </PageState>
  );
}
