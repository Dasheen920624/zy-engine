import { PageShell } from "@/shared/ui/PageShell";
import { PageState } from "@/shared/ui/PageState";
import { RoadmapLink } from "@/shared/ui/RoadmapLink";

export default function IdentityBinding() {
  return (
    <PageShell
      title="身份绑定"
      description="一处配置，所有员工通用。CAS / LDAP / OIDC / SAML / 国密 CA 五选 N"
    >
      <PageState
        state="disabled"
        description="本页依赖 GA-SVC-COMPLIANCE-01，引擎完成后激活。"
        action={<RoadmapLink taskIds={["GA-SVC-COMPLIANCE-01"]} />}
      />
    </PageShell>
  );
}
