import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function Notifications() {
  return (
    <PageShell
      title="通知中心"
      description="业务通知、处理提醒、系统状态；不打扰策略可在合规运维 → 通知设置配置"
    >
      <PageState
        state="disabled"
        description="本页依赖 GA-SVC-CLINICAL-03，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-SVC-CLINICAL-03"]} />}
      />
    </PageShell>
  );
}
