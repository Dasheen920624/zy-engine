import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function ConfigPackages() {
  return (
    <PageShell
      title="配置包中心"
      description="路径 / 规则 / 字典 / 适配器配置打包发布，跨院区一键复制"
    >
      <PageState
        state="disabled"
        description="本页依赖 GA-ENG-PKG-01，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-ENG-PKG-01"]} />}
      />
    </PageShell>
  );
}
