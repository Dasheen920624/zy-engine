import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function QcAlerts() {
  return (
    <PageShell title="质控预警" description="按责任科室自动派单 + 整改闭环追踪 + 趋势看板">
      <PageState
        state="disabled"
        description="本页依赖 GA-ENG-EVAL-01，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-ENG-EVAL-01"]} />}
      />
    </PageShell>
  );
}
