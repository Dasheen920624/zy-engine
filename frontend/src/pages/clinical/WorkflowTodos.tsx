import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function WorkflowTodos() {
  return (
    <PageShell title="待办中心" description="审批 / 整改 / 发布 / 回滚 4 类待办，按 SLA 倒序">
      <PageState
        state="disabled"
        description="本页依赖 GA-SVC-CLINICAL-03，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-SVC-CLINICAL-03"]} />}
      />
    </PageShell>
  );
}
