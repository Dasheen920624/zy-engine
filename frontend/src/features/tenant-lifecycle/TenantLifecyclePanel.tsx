import { Card, Space, Typography } from "antd";

import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

const { Text } = Typography;

/**
 * 租户生命周期面板（与 docs/CONSTITUTION.md §6 对齐）。
 *
 * 6 阶段：0 准备 → 1 试运行 → 2 验收 → 3 推广 → 4 运行 → 5 续约
 * 多维治理切片：组织层级、病种/专病包、服务模块、角色、阶段、接入、风险和证据并行。
 *
 * GA-ENG-BASE-09 净化后占位：业务包装 GA-SVC-PILOT-01 完成后接入真实生命周期推进引擎。
 */
export function TenantLifecyclePanel() {
  return (
    <Card title={<Text strong>租户生命周期</Text>}>
      <Space direction="vertical" size="middle" className="mk-full-width">
        <PageState
          state="disabled"
          description="本面板依赖 GA-SVC-PILOT-01；E6 服务包装启动后接入真实多维生命周期引擎。"
          action={<RoadmapLink taskIds={["GA-SVC-PILOT-01"]} />}
        />
      </Space>
    </Card>
  );
}
