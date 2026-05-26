import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function PatientPathways() {
  return (
    <PageShell
      title="患者路径"
      description="患者入径、节点流转、变异登记；医生只看与自己科室相关的患者"
    >
      <PageState
        state="disabled"
        description="本页依赖 GA-ENG-PATH-01、GA-ENG-API-06，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-ENG-PATH-01", "GA-ENG-API-06"]} />}
      />
    </PageShell>
  );
}
