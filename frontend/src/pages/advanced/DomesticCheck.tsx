import { Card, Tag, Space, Progress, Descriptions, Typography, Spin, Alert } from "antd";
import { PageShell } from "@/shared/ui/PageShell";
import { useDomesticSnapshot } from "@/shared/api/hooks";

const LEVEL: Record<string, string> = { core: "blue", alt: "cyan", open: "default" };
const LEVEL_LABEL: Record<string, string> = { core: "国产化核心", alt: "国产化备选", open: "开源通用" };

export default function DomesticCheck() {
  const snap = useDomesticSnapshot();

  if (snap.isLoading) {
    return (
      <PageShell title="国产化自检" description="实时检测当前 OS / JDK / DB / 中间件">
        <Spin />
      </PageShell>
    );
  }

  const data = snap.data as Record<string, unknown>;
  const os = data?.os as { name: string; version: string; arch: string; domesticLevel: string } | undefined;
  const jdk = data?.jdk as { vendor: string; version: string; vmName: string; domesticLevel: string } | undefined;
  const middleware = data?.middleware as Array<{ name: string; version: string; domesticLevel: string }> | undefined;
  const cryptoMeta = data?.crypto as { provider: string; supports: string[]; domesticLevel: string } | undefined;
  const score = data?.score as number | undefined;

  return (
    <PageShell
      title="国产化自检"
      description="实时检测当前 OS / JDK / DB / 中间件 / 国密 Provider 的国产化等级"
    >
      <Card title="本机国产化得分">
        <Progress percent={score ?? 0} strokeColor={(score ?? 0) >= 85 ? "#52c41a" : "#faad14"} />
        <Typography.Text style={{ marginTop: 8, display: "block" }}>
          基于实时 JVM/OS 探测 · 信通院评测前自查就绪
        </Typography.Text>
      </Card>

      <Card title="操作系统">
        <Descriptions column={2} size="small">
          <Descriptions.Item label="名称">{os?.name}</Descriptions.Item>
          <Descriptions.Item label="版本">{os?.version}</Descriptions.Item>
          <Descriptions.Item label="架构">{os?.arch}</Descriptions.Item>
          <Descriptions.Item label="国产化等级">
            <Tag color={LEVEL[os?.domesticLevel ?? "open"]}>{LEVEL_LABEL[os?.domesticLevel ?? "open"]}</Tag>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="JDK / JVM">
        <Descriptions column={2} size="small">
          <Descriptions.Item label="厂商">{jdk?.vendor}</Descriptions.Item>
          <Descriptions.Item label="版本">{jdk?.version}</Descriptions.Item>
          <Descriptions.Item label="JVM">{jdk?.vmName}</Descriptions.Item>
          <Descriptions.Item label="国产化等级">
            <Tag color={LEVEL[jdk?.domesticLevel ?? "open"]}>{LEVEL_LABEL[jdk?.domesticLevel ?? "open"]}</Tag>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="国密 Provider">
        <Descriptions column={2} size="small">
          <Descriptions.Item label="提供方">{cryptoMeta?.provider}</Descriptions.Item>
          <Descriptions.Item label="支持算法">
            <Space>{cryptoMeta?.supports.map((a) => <Tag key={a} color="purple">{a}</Tag>)}</Space>
          </Descriptions.Item>
          <Descriptions.Item label="等级">
            <Tag color={LEVEL[cryptoMeta?.domesticLevel ?? "open"]}>{LEVEL_LABEL[cryptoMeta?.domesticLevel ?? "open"]}</Tag>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="中间件">
        <Space wrap>
          {middleware?.map((m) => (
            <Tag key={m.name} color={LEVEL[m.domesticLevel]}>
              {m.name} {m.version} · {LEVEL_LABEL[m.domesticLevel]}
            </Tag>
          ))}
        </Space>
      </Card>

      {(score ?? 0) < 85 && (
        <Alert
          type="warning"
          showIcon
          message="国产化得分低于 85 分"
          description="信通院评测建议切换到麒麟/统信 OS + KAE-JDK21 + 达梦/人大金仓数据库以达到 core 等级。"
        />
      )}
    </PageShell>
  );
}
