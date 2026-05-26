import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function SecurityBaseline() {
  return (
    <PageShell
      title="安全基线"
      description="等保 2.0 三级 + 商密评测 + 个保法 自查清单，状态一目了然"
    >
      <PageState
        state="disabled"
        description="本页依赖 GA-SVC-COMPLIANCE-01，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-SVC-COMPLIANCE-01"]} />}
      />
    </PageShell>
  );
}
