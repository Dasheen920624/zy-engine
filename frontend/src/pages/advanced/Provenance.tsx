import { Card, Input, Timeline, Tag, Space } from "antd";
import { SearchOutlined, BookOutlined, FileTextOutlined, RobotOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";

export default function Provenance() {
  return (
    <PageShell
      title="来源追溯"
      description="任何提醒可追溯到指南 / 文献 / 知识库哪一条"
    >
      <Card>
        <Input.Search
          placeholder="搜规则 ID / 提醒 ID / 知识点关键词"
          prefix={<SearchOutlined />}
          defaultValue="R-AB-024"
          style={{ marginBottom: 16 }}
        />
        <Timeline
          items={[
            {
              dot: <BookOutlined style={{ color: "#1565c0" }} />,
              children: (
                <Space direction="vertical" size="small">
                  <strong>《2023 中国抗菌药物临床应用管理办法》</strong>
                  <Tag color="purple">指南</Tag>
                  <span>第 18 条 · 头孢类必须皮试通过后使用</span>
                </Space>
              ),
            },
            {
              dot: <FileTextOutlined style={{ color: "#1565c0" }} />,
              children: (
                <Space direction="vertical" size="small">
                  <strong>抽取规则：R-AB-024 · 头孢曲松皮试缺失</strong>
                  <Tag color="blue">规则定义</Tag>
                  <span>规则库 → 医嘱安全 → R-AB-024</span>
                </Space>
              ),
            },
            {
              dot: <RobotOutlined style={{ color: "#52c41a" }} />,
              children: (
                <Space direction="vertical" size="small">
                  <strong>触发提醒 · 张** 病例</strong>
                  <Tag color="success">运行证据</Tag>
                  <span>2026-05-23 10:23 · 已采纳 · 医生：王医生</span>
                </Space>
              ),
            },
          ]}
        />
      </Card>
    </PageShell>
  );
}
