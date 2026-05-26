import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function PathwayTemplates() {
  return (
    <PageShell
      title="路径配置"
      description="按专病维度的路径模板。普通模式表单+时间线，专家模式才打开 X6 节点画布"
    >
      <PageState
        state="disabled"
        description="本页依赖 GA-ENG-PATH-01、GA-ENG-API-06，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-ENG-PATH-01", "GA-ENG-API-06"]} />}
      />
    </PageShell>
  );
}
