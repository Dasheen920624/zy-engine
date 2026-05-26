import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function CdssFatigue() {
  return (
    <PageShell
      title="临床提醒治理"
      description={'医嘱提醒疲劳治理 · 医生只看到与自己患者相关的，主操作只有"采纳 / 不采纳"'}
    >
      <PageState
        state="disabled"
        description="本页依赖 GA-ENG-CDSS-01，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-ENG-CDSS-01"]} />}
      />
    </PageShell>
  );
}
