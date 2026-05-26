import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function Mpi() {
  return (
    <PageShell title="患者主索引" description="跨院区患者唯一身份；自动合并多就诊号 + 人工处理冲突">
      <PageState
        state="disabled"
        description="本页依赖 GA-SVC-CLINICAL-01，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-SVC-CLINICAL-01"]} />}
      />
    </PageShell>
  );
}
