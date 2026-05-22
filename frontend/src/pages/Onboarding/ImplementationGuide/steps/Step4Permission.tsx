import { Button, Card, Checkbox, Col, Row, Space, Typography } from "antd";
import { PERMISSION_TEMPLATES } from "../constants";
import styles from "../../ImplementationGuidePage.module.css";

const { Paragraph, Text } = Typography;

interface Step4PermissionProps {
  selectedPermTemplates: string[];
  togglePermTemplate: (code: string, checked: boolean) => void;
  openConfigWizard: (type: "org" | "rule" | "permission") => void;
}

export default function Step4Permission({ selectedPermTemplates, togglePermTemplate, openConfigWizard }: Step4PermissionProps) {
  return (
    <Card
      title="权限分配"
      extra={
        <Button type="link" onClick={() => openConfigWizard("permission")}>
          权限配置向导
        </Button>
      }
    >
      <Paragraph type="secondary" className={styles.marginBottom16}>
        选择常用权限模板，快速分配数据权限和菜单权限。
      </Paragraph>
      <Row gutter={[12, 12]}>
        {PERMISSION_TEMPLATES.map((tpl) => (
          <Col span={8} key={tpl.code}>
            <Card
              size="small"
              hoverable
              className={selectedPermTemplates.includes(tpl.code) ? styles.permCard : undefined}
              onClick={() => togglePermTemplate(tpl.code, !selectedPermTemplates.includes(tpl.code))}
            >
              <Space direction="vertical" size={4}>
                <Space>
                  <Checkbox checked={selectedPermTemplates.includes(tpl.code)} />
                  <Text strong>{tpl.name}</Text>
                </Space>
                <Text type="secondary" className={styles.textSmall}>
                  {tpl.description}
                </Text>
              </Space>
            </Card>
          </Col>
        ))}
      </Row>
    </Card>
  );
}
