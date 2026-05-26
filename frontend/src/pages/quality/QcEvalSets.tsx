import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function QcEvalSets() {
  return (
    <PageShell
      title="评估指标库"
      description="按评级 / 上报 / 医保 / 公共卫生 分类的指标定义，支持版本化"
    >
      <PageState
        state="disabled"
        description="本页依赖 GA-ENG-EVAL-01，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-ENG-EVAL-01"]} />}
      />
    </PageShell>
  );
}
