import { Card, Switch, Table, Typography } from "antd";
import { SAMPLE_PATHWAYS } from "../constants";
import styles from "../../ImplementationGuidePage.module.css";

const { Paragraph } = Typography;

interface Step3PathwayConfigProps {
  pathwayEnabled: Record<string, boolean>;
  onPathwayEnabledChange: (updated: Record<string, boolean>) => void;
}

export default function Step3PathwayConfig({ pathwayEnabled, onPathwayEnabledChange }: Step3PathwayConfigProps) {
  return (
    <Card title="路径配置">
      <Paragraph type="secondary" className={styles.marginBottom16}>
        配置临床路径模板，启用或禁用路径实例化功能。
      </Paragraph>
      <Table
        dataSource={SAMPLE_PATHWAYS}
        rowKey="code"
        pagination={false}
        columns={[
          { title: "路径编码", dataIndex: "code", width: 180 },
          { title: "路径名称", dataIndex: "name" },
          { title: "专科", dataIndex: "specialty", width: 120 },
          {
            title: "启用",
            dataIndex: "code",
            width: 80,
            render: (code: string) => (
              <Switch
                size="small"
                checked={pathwayEnabled[code] ?? SAMPLE_PATHWAYS.find((p) => p.code === code)?.enabled ?? false}
                onChange={(checked) => onPathwayEnabledChange({ ...pathwayEnabled, [code]: checked })}
              />
            ),
          },
        ]}
      />
    </Card>
  );
}
