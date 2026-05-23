import { Card, Tag, Space, Progress, List, Typography } from "antd";
import { CheckCircleFilled, ExclamationCircleFilled } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";

const STACK = [
  { layer: "操作系统", component: "麒麟 V10 SP3", level: "core", status: "ok" },
  { layer: "JDK", component: "KAE-JDK 21（OpenJDK 21 LTS）", level: "core", status: "ok" },
  { layer: "数据库", component: "达梦 8.1.3", level: "core", status: "ok" },
  { layer: "数据库", component: "人大金仓 V9", level: "alt", status: "ok" },
  { layer: "中间件", component: "Tomcat 10.1 + Hikari 5", level: "open", status: "ok" },
  { layer: "国密 Provider", component: "BC-FJA 1.0.2.5 + KAE Provider", level: "core", status: "warn" },
  { layer: "前端 CDN", component: "未启用（内网部署）", level: "n/a", status: "ok" },
  { layer: "镜像基础", component: "openEuler:22.03-LTS-SP4 + distroless 备选", level: "core", status: "ok" },
];

const LEVEL: Record<string, string> = { core: "blue", alt: "cyan", open: "default", "n/a": "default" };
const LEVEL_LABEL: Record<string, string> = { core: "国产化核心", alt: "国产化备选", open: "开源通用", "n/a": "不适用" };

export default function DomesticCheck() {
  return (
    <PageShell
      title="国产化自检"
      description="一键展示当前 OS / JDK / DB / 中间件 / 国密 Provider 的国产化等级"
    >
      <Card title="本机国产化得分">
        <Progress percent={87} strokeColor="#52c41a" />
        <Typography.Text style={{ marginTop: 8, display: "block" }}>
          7/8 层已用国产化主选；BC-FJA + KAE Provider 互操作性测试中（GA-OPS-03 国产化矩阵任务中）。
        </Typography.Text>
      </Card>
      <Card title="技术栈清单">
        <List
          dataSource={STACK}
          renderItem={(s) => (
            <List.Item>
              <Space style={{ width: "100%", justifyContent: "space-between" }}>
                <Space>
                  {s.status === "ok" ? (
                    <CheckCircleFilled style={{ color: "#52c41a" }} />
                  ) : (
                    <ExclamationCircleFilled style={{ color: "#faad14" }} />
                  )}
                  <strong>{s.layer}</strong>
                  <span>{s.component}</span>
                </Space>
                <Tag color={LEVEL[s.level]}>{LEVEL_LABEL[s.level]}</Tag>
              </Space>
            </List.Item>
          )}
        />
      </Card>
    </PageShell>
  );
}
