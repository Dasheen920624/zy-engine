import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function AdapterHub() {
  return (
    <PageShell
      title="适配器中心"
      description="HIS / EMR / LIS / PACS 接入点，按系统看健康状态，技术参数折叠"
    >
      <PageState
        state="disabled"
        description="本页依赖 GA-SVC-PILOT-02，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-SVC-PILOT-02"]} />}
      />
    </PageShell>
  );
}
