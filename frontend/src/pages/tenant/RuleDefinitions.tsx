import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function RuleDefinitions() {
  return (
    <PageShell
      title="规则库"
      description="医保审核 / 医嘱安全 / 质控 3 大类规则，从模板创建，DSL 仅专家模式可见"
    >
      <PageState
        state="disabled"
        description="本页依赖 GA-ENG-RULE-01、GA-ENG-API-05，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-ENG-RULE-01", "GA-ENG-API-05"]} />}
      />
    </PageShell>
  );
}
