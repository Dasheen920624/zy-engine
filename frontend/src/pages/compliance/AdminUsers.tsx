import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function AdminUsers() {
  return (
    <PageShell title="用户管理" description="医院员工身份、角色、权限；与身份绑定中的 SSO 自动同步">
      <PageState
        state="disabled"
        description="本页依赖 GA-SVC-COMPLIANCE-01，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-SVC-COMPLIANCE-01"]} />}
      />
    </PageShell>
  );
}
