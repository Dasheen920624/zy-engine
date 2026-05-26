import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function InsuranceAudit() {
  return (
    <PageShell
      title="医保智能审核"
      description="DRG / DIP 自动入组、规则命中、人工复审、拒付申诉一站式"
    >
      <PageState
        state="disabled"
        description="本页依赖 GA-SVC-QUALITY-02，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-SVC-QUALITY-02"]} />}
      />
    </PageShell>
  );
}
