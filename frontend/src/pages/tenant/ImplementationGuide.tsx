import { Card, Steps, Button, Space, Typography } from "antd";
import { CheckCircleOutlined, RocketOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";

const { Text } = Typography;

/**
 * GA-TENANT-01 · 客户实施向导
 * 实施工程师上线 checklist（与 docs/CONSTITUTION.md §2.2 试点准备 7 项之一）
 */
export default function ImplementationGuide() {
  const steps = [
    { title: "签合同 · 建立项目", status: "finish" as const, owner: "销售 + 客户成功" },
    { title: "现场调研 · 出方案", status: "finish" as const, owner: "实施 + 信息科" },
    { title: "租户开通 · 建管理员", status: "finish" as const, owner: "实施工程师" },
    { title: "接入适配器 · HIS/EMR/LIS/PACS", status: "process" as const, owner: "实施 + 信息科" },
    { title: "导入字典映射", status: "wait" as const, owner: "实施 + 病案科" },
    { title: "导入配置包 · 路径/规则", status: "wait" as const, owner: "实施 + 医务处" },
    { title: "试点科室培训", status: "wait" as const, owner: "客户成功 + 临床" },
    { title: "试运行 · 1-3 个月", status: "wait" as const, owner: "全员" },
    { title: "院方验收 · 双签", status: "wait" as const, owner: "院长 + 乙方" },
  ];

  return (
    <PageShell
      title="客户实施向导"
      description="按 9 步推进，每步均有明确产出和 owner"
      primary={
        <Button type="primary" icon={<RocketOutlined />}>
          继续下一步
        </Button>
      }
    >
      <Card>
        <Steps direction="vertical" current={3} items={steps.map((s) => ({
          title: s.title,
          status: s.status,
          description: (
            <Space>
              <Text type="secondary">{s.owner}</Text>
              {s.status === "finish" && <CheckCircleOutlined style={{ color: "#52c41a" }} />}
            </Space>
          ),
        }))} />
      </Card>
    </PageShell>
  );
}
