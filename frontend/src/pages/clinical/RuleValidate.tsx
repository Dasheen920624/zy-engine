import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function RuleValidate() {
  return (
    <PageShell
      title="规则校验"
      description="对单患者 / 单医嘱立刻试运行规则，看是否命中、为什么命中、有哪些建议"
    >
      <PageState
        state="disabled"
        description="本页依赖 GA-ENG-RULE-01，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-ENG-RULE-01"]} />}
      />
    </PageShell>
  );
}
