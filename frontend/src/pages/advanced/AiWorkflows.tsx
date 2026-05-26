import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function AiWorkflows() {
  return (
    <PageShell title="AI 工作流" description="模型网关 + 降级链 + 工作流模板（专家调试）">
      <PageState
        state="disabled"
        description="本页依赖 GA-ENG-LLM-01，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-ENG-LLM-01"]} />}
      />
    </PageShell>
  );
}
