import { Button, Card, Col, Empty, Form, Input, Row, Select, Table, Typography } from "antd";
import type { FormInstance } from "antd";
import type { DepartmentInput, RoleInput } from "../types";
import { DEPT_TYPES } from "../constants";
import styles from "../../ImplementationGuidePage.module.css";

const { Paragraph } = Typography;

interface Step1OrgConfigProps {
  departments: DepartmentInput[];
  roles: RoleInput[];
  deptForm: FormInstance;
  roleForm: FormInstance;
  addDepartment: () => void;
  removeDepartment: (code: string) => void;
  addRole: () => void;
  removeRole: (code: string) => void;
  openConfigWizard: (type: "org" | "rule" | "permission") => void;
}

export default function Step1OrgConfig({
  departments,
  roles,
  deptForm,
  roleForm,
  addDepartment,
  removeDepartment,
  addRole,
  removeRole,
  openConfigWizard,
}: Step1OrgConfigProps) {
  return (
    <Card
      title="组织配置"
      extra={
        <Button type="link" onClick={() => openConfigWizard("org")}>
          批量配置向导
        </Button>
      }
    >
      <Paragraph type="secondary" className={styles.marginBottom16}>
        配置科室、病区和角色，建立组织架构基础。
      </Paragraph>

      <Row gutter={24}>
        {/* 科室管理 */}
        <Col span={12}>
          <Card type="inner" title="科室管理" size="small">
            <Form form={deptForm} layout="inline" className={styles.formInline}>
              <Form.Item name="code" className={styles.formItemNoMargin}>
                <Input placeholder="科室编码" size="small" />
              </Form.Item>
              <Form.Item name="name" className={styles.formItemNoMargin}>
                <Input placeholder="科室名称" size="small" />
              </Form.Item>
              <Form.Item name="type" initialValue="CLINICAL" className={styles.formItemNoMargin}>
                <Select size="small" className={styles.selectSmall} options={DEPT_TYPES} />
              </Form.Item>
              <Button size="small" type="primary" onClick={addDepartment}>
                添加
              </Button>
            </Form>
            {departments.length === 0 ? (
              <Empty description="暂无科室" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <Table
                size="small"
                pagination={false}
                dataSource={departments}
                rowKey="code"
                columns={[
                  { title: "编码", dataIndex: "code", width: 100 },
                  { title: "名称", dataIndex: "name" },
                  {
                    title: "类型",
                    dataIndex: "type",
                    width: 100,
                    render: (v: string) => DEPT_TYPES.find((d) => d.value === v)?.label ?? v,
                  },
                  {
                    title: "操作",
                    width: 60,
                    render: (_: unknown, r: DepartmentInput) => (
                      <Button type="link" danger size="small" onClick={() => removeDepartment(r.code)}>
                        删除
                      </Button>
                    ),
                  },
                ]}
              />
            )}
          </Card>
        </Col>

        {/* 角色管理 */}
        <Col span={12}>
          <Card type="inner" title="角色管理" size="small">
            <Form form={roleForm} layout="inline" className={styles.formInline}>
              <Form.Item name="code" className={styles.formItemNoMargin}>
                <Input placeholder="角色编码" size="small" />
              </Form.Item>
              <Form.Item name="name" className={styles.formItemNoMargin}>
                <Input placeholder="角色名称" size="small" />
              </Form.Item>
              <Button size="small" type="primary" onClick={addRole}>
                添加
              </Button>
            </Form>
            {roles.length === 0 ? (
              <Empty description="暂无角色" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <Table
                size="small"
                pagination={false}
                dataSource={roles}
                rowKey="code"
                columns={[
                  { title: "编码", dataIndex: "code", width: 120 },
                  { title: "名称", dataIndex: "name" },
                  {
                    title: "权限数",
                    dataIndex: "permissions",
                    width: 80,
                    render: (v: string[]) => v?.length ?? 0,
                  },
                  {
                    title: "操作",
                    width: 60,
                    render: (_: unknown, r: RoleInput) => (
                      <Button type="link" danger size="small" onClick={() => removeRole(r.code)}>
                        删除
                      </Button>
                    ),
                  },
                ]}
              />
            )}
          </Card>
        </Col>
      </Row>
    </Card>
  );
}
