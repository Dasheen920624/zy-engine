import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function QcDashboard() {
  return (
    <PageShell
      title="院级质控驾驶舱"
      description="院长 / 医务处 / 质控办 1 屏看院级 + 科室级指标，0 技术名词"
    >
      <PageState
        state="disabled"
        description="本页依赖 GA-ENG-EVAL-01，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-ENG-EVAL-01"]} />}
      />
    </PageShell>
  );
}
