import { RocketOutlined } from "@ant-design/icons";
import { Button, Card, Descriptions, Typography } from "antd";
import type { DepartmentInput, RoleInput } from "../types";
import { SAMPLE_PATHWAYS } from "../constants";
import styles from "../../ImplementationGuidePage.module.css";

const { Paragraph } = Typography;

interface Step6GoLiveProps {
  departments: DepartmentInput[];
  roles: RoleInput[];
  selectedRulePackages: string[];
  selectedPermTemplates: string[];
  pathwayEnabled: Record<string, boolean>;
  envAllPassed: boolean;
  validationResult: { passed: boolean; details: string[] } | null;
  handleGoLive: () => void;
}

export default function Step6GoLive({
  departments,
  roles,
  selectedRulePackages,
  selectedPermTemplates,
  pathwayEnabled,
  envAllPassed,
  validationResult,
  handleGoLive,
}: Step6GoLiveProps) {
  return (
    <Card title="完成上线">
      <Paragraph type="secondary" className={styles.marginBottom16}>
        确认所有配置项，完成实施并正式上线。
      </Paragraph>
      <Descriptions bordered column={2}>
        <Descriptions.Item label="科室数量">{departments.length}</Descriptions.Item>
        <Descriptions.Item label="角色数量">{roles.length}</Descriptions.Item>
        <Descriptions.Item label="规则包">{selectedRulePackages.length} 个已选择</Descriptions.Item>
        <Descriptions.Item label="权限模板">{selectedPermTemplates.length} 个已选择</Descriptions.Item>
        <Descriptions.Item label="启用路径">
          {Object.values(pathwayEnabled).filter(Boolean).length} / {SAMPLE_PATHWAYS.length}
        </Descriptions.Item>
        <Descriptions.Item label="环境检查">{envAllPassed ? "全部通过" : "部分未通过"}</Descriptions.Item>
        <Descriptions.Item label="验证测试">{validationResult?.passed ? "通过" : "未运行"}</Descriptions.Item>
      </Descriptions>
      <div className={styles.textCenterWithMargin}>
        <Button type="primary" size="large" icon={<RocketOutlined />} onClick={handleGoLive}>
          确认上线
        </Button>
      </div>
    </Card>
  );
}
