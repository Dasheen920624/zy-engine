import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function QcEvalResults() {
  return (
    <PageShell title="评估结果" description="月度 / 季度 / 年度评估结果，可导出 PDF/Excel 院级报告">
      <PageState
        state="disabled"
        description="本页依赖 GA-ENG-EVAL-01，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-ENG-EVAL-01"]} />}
      />
    </PageShell>
  );
}
