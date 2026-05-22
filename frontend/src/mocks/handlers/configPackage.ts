import { http, HttpResponse } from "msw";
import { wrap, baseURL } from "./shared";
import type { ConfigPackageSummary, ConfigPackageDetail, ConfigPackageReview } from "../../api/types";

// ─── Mock 配置包数据 (FE-004) ────────────────────────────────────────

const mockPackages: ConfigPackageSummary[] = [
  {
    tenant_id: "TENANT_DEMO",
    package_code: "PKG_AMI_RULE_CONFIG",
    package_version: "2026.05.01",
    asset_type: "RULE",
    scope_level: "HOSPITAL",
    scope_code: "HOSPITAL_DEMO",
    scope_reference: "HOSPITAL · HOSPITAL_DEMO",
    status: "REVIEWED",
    base_version: "2026.04.02",
    content_hash: "sha256:a1b2c3d4e5f60718",
    created_by: "QC_LIU",
    reviewed_by: "QC_REVIEWER_WANG",
    created_time: "2026-05-16T16:24:00+08:00",
    reviewed_time: "2026-05-16T21:30:00+08:00",
  },
  {
    tenant_id: "TENANT_DEMO",
    package_code: "PKG_EMR_QC",
    package_version: "2026.05.02",
    asset_type: "RULE",
    scope_level: "HOSPITAL",
    scope_code: "HOSPITAL_DEMO",
    scope_reference: "HOSPITAL · HOSPITAL_DEMO",
    status: "DRAFT",
    content_hash: "sha256:7d2e91f0",
    created_by: "QC_LIU",
    created_time: "2026-05-16T19:14:00+08:00",
  },
  {
    tenant_id: "TENANT_DEMO",
    package_code: "PKG_INSURANCE_QC",
    package_version: "2026.05.03",
    asset_type: "RULE",
    scope_level: "GROUP",
    scope_code: "GROUP_DEMO",
    scope_reference: "GROUP · GROUP_DEMO",
    status: "ACTIVE",
    content_hash: "sha256:be4f08a2",
    created_by: "ADMIN",
    reviewed_by: "QC_REVIEWER_WANG",
    approved_by: "CIO_LI",
    created_time: "2026-05-14T10:00:00+08:00",
    reviewed_time: "2026-05-15T09:30:00+08:00",
    published_time: "2026-05-15T11:02:00+08:00",
  },
  {
    tenant_id: "TENANT_DEMO",
    package_code: "PKG_DICTIONARY_DRUG",
    package_version: "2026.05.04",
    asset_type: "TERMINOLOGY",
    scope_level: "HOSPITAL",
    scope_code: "HOSPITAL_DEMO",
    scope_reference: "HOSPITAL · HOSPITAL_DEMO",
    status: "REVIEWED",
    content_hash: "sha256:c92a1d3e",
    created_by: "TERM_ADMIN",
    reviewed_by: "QC_REVIEWER_WANG",
    created_time: "2026-05-16T12:00:00+08:00",
    reviewed_time: "2026-05-16T14:38:00+08:00",
  },
  {
    tenant_id: "TENANT_DEMO",
    package_code: "PKG_AMI_PATHWAY",
    package_version: "1.0.0",
    asset_type: "PATH",
    scope_level: "HOSPITAL",
    scope_code: "HOSPITAL_DEMO",
    scope_reference: "HOSPITAL · HOSPITAL_DEMO",
    status: "ACTIVE",
    content_hash: "sha256:8af3b2c1",
    created_by: "PATH_ADMIN",
    reviewed_by: "MEDICAL_DR_ZHAO",
    approved_by: "CIO_LI",
    created_time: "2026-05-10T09:00:00+08:00",
    reviewed_time: "2026-05-11T14:00:00+08:00",
    published_time: "2026-05-12T09:45:00+08:00",
  },
  {
    tenant_id: "TENANT_DEMO",
    package_code: "PKG_GRAPH_AMI",
    package_version: "KG_2026_05",
    asset_type: "GRAPH",
    scope_level: "GROUP",
    scope_code: "GROUP_DEMO",
    scope_reference: "GROUP · GROUP_DEMO",
    status: "PUBLISHED",
    content_hash: "sha256:4f1c87a9",
    created_by: "GRAPH_ADMIN",
    reviewed_by: "QC_REVIEWER_WANG",
    approved_by: "CIO_LI",
    created_time: "2026-05-13T16:00:00+08:00",
    reviewed_time: "2026-05-14T10:00:00+08:00",
    published_time: "2026-05-14T18:21:00+08:00",
  },
];

const mockPackageDetails: Record<string, ConfigPackageDetail> = {
  "PKG_AMI_RULE_CONFIG@2026.05.01": {
    ...mockPackages[0],
    declared_content_hash: "sha256:a1b2c3d4e5f60718",
    manifest: {
      items: [
        { asset_code: "R_AMI_STEMI_CANDIDATE", asset_type: "RULE", version: "3", change_type: "ADDED" },
        { asset_code: "R_AMI_ECG_TIMELY", asset_type: "RULE", version: "2", change_type: "MODIFIED" },
        { asset_code: "R_AMI_DOOR_TO_BALLOON_90", asset_type: "RULE", version: "1", change_type: "UNCHANGED" },
        { asset_code: "R_AMI_DUAL_ANTIPLATELET_REQ", asset_type: "RULE", version: "1", change_type: "REMOVED" },
      ],
      content_hash: "sha256:a1b2c3d4e5f60718",
    },
    diff: {
      rules: [
        {
          rule_code: "R_AMI_STEMI_CANDIDATE",
          version_change: "2 → 3",
          added_fields: ["scenario_codes", "reference_title", "reference_section", "reviewed_by"],
        },
        {
          rule_code: "R_AMI_DUAL_ANTIPLATELET_REQ",
          change: "REMOVED",
        },
      ],
    },
    full_snapshot: { rules: [{ rule_code: "R_AMI_STEMI_CANDIDATE" }, { rule_code: "R_AMI_ECG_TIMELY" }, { rule_code: "R_AMI_DOOR_TO_BALLOON_90" }] },
  },
};

const mockPackageReviews: Record<string, ConfigPackageReview> = {
  "PKG_AMI_RULE_CONFIG@2026.05.01": {
    ...mockPackageDetails["PKG_AMI_RULE_CONFIG@2026.05.01"],
    ready_to_publish: true,
    issues: [
      { severity: "WARNING", field: "high_risk_change", message: "R_AMI_DUAL_ANTIPLATELET_REQ 自上一版本起被移除，建议医学审核确认" },
    ],
    summary: {
      asset_count: 4,
      manifest_keys: ["items", "content_hash"],
      full_snapshot_present: true,
      diff_present: true,
      scope_exists: true,
    },
    source_review: {
      enabled: true,
      blocked: false,
      missing_count: 0,
      expired_count: 0,
      unreviewed_count: 0,
      allow_publish: true,
      message: "SOURCE_REVIEW_PASSED",
    },
  },
};

export const configPackageHandlers = [
  // ─── Config Packages: list ─────────────────────────────────────
  http.get(`${baseURL}/config-packages`, ({ request }) => {
    const url = new URL(request.url);
    const assetType = url.searchParams.get("assetType");
    const status = url.searchParams.get("status");
    const scopeLevel = url.searchParams.get("scopeLevel");
    const scopeCode = url.searchParams.get("scopeCode");

    let filtered = [...mockPackages];
    if (assetType) filtered = filtered.filter((p) => p.asset_type === assetType);
    if (status) filtered = filtered.filter((p) => p.status === status);
    if (scopeLevel) filtered = filtered.filter((p) => p.scope_level === scopeLevel);
    if (scopeCode) filtered = filtered.filter((p) => p.scope_code === scopeCode);

    return HttpResponse.json(wrap(filtered));
  }),

  // ─── Config Packages: detail ───────────────────────────────────
  http.get(`${baseURL}/config-packages/:code/:version`, ({ params }) => {
    const code = params.code as string;
    const version = params.version as string;
    const key = `${code}@${version}`;

    const detail = mockPackageDetails[key];
    if (!detail) {
      // Fallback: find from summary list
      const summary = mockPackages.find((p) => p.package_code === code && p.package_version === version);
      if (!summary) {
        return HttpResponse.json(
          { success: false, code: "DATA_MISSING", message: `配置包不存在: ${code}@${version}`, data: null, trace_id: "mock-err" },
          { status: 404 },
        );
      }
      return HttpResponse.json(wrap({ ...summary, manifest: {}, diff: {}, full_snapshot: {} }));
    }
    return HttpResponse.json(wrap({ ...detail, trace_id: `mock-pkg-${Date.now()}` }));
  }),

  // ─── Config Packages: review (GET = read-only, POST = submit) ──
  http.get(`${baseURL}/config-packages/:code/:version/review`, ({ params }) => {
    const code = params.code as string;
    const version = params.version as string;
    const key = `${code}@${version}`;

    const review = mockPackageReviews[key];
    if (!review) {
      // Auto-generate a basic review
      const summary = mockPackages.find((p) => p.package_code === code && p.package_version === version);
      if (!summary) {
        return HttpResponse.json(
          { success: false, code: "DATA_MISSING", message: `配置包不存在: ${code}@${version}`, data: null, trace_id: "mock-err" },
          { status: 404 },
        );
      }
      const autoReview: ConfigPackageReview = {
        ...summary,
        ready_to_publish: summary.status === "DRAFT" || summary.status === "REVIEWED",
        issues: [],
        summary: { asset_count: 3, manifest_keys: [], full_snapshot_present: true, diff_present: false, scope_exists: true },
        source_review: { enabled: false, blocked: false, missing_count: 0, expired_count: 0, unreviewed_count: 0, allow_publish: true, message: "SOURCE_REVIEW_NOT_ENABLED" },
      };
      return HttpResponse.json(wrap(autoReview));
    }
    return HttpResponse.json(wrap({ ...review, trace_id: `mock-review-${Date.now()}` }));
  }),

  http.post(`${baseURL}/config-packages/:code/:version/review`, async ({ params, request }) => {
    const code = params.code as string;
    const version = params.version as string;
    const body = (await request.json().catch(() => ({}))) as { reviewed_by?: string };
    const key = `${code}@${version}`;

    const review = mockPackageReviews[key];
    if (!review) {
      const summary = mockPackages.find((p) => p.package_code === code && p.package_version === version);
      if (!summary) {
        return HttpResponse.json(
          { success: false, code: "DATA_MISSING", message: `配置包不存在: ${code}@${version}`, data: null, trace_id: "mock-err" },
          { status: 404 },
        );
      }
      // Simulate review pass
      const result: ConfigPackageReview = {
        ...summary,
        status: "REVIEWED",
        reviewed_by: body?.reviewed_by || "MOCK_REVIEWER",
        reviewed_time: new Date().toISOString(),
        ready_to_publish: true,
        issues: [],
        summary: { asset_count: 3, manifest_keys: [], full_snapshot_present: true, diff_present: false, scope_exists: true },
        source_review: { enabled: false, blocked: false, missing_count: 0, expired_count: 0, unreviewed_count: 0, allow_publish: true },
      };
      return HttpResponse.json(wrap(result));
    }
    return HttpResponse.json(wrap({ ...review, reviewed_by: body?.reviewed_by || review.reviewed_by, reviewed_time: new Date().toISOString(), trace_id: `mock-review-${Date.now()}` }));
  }),

  // ─── Config Packages: publish ──────────────────────────────────
  http.post(`${baseURL}/config-packages/:code/:version/publish`, async ({ params, request }) => {
    const code = params.code as string;
    const version = params.version as string;
    const body = (await request.json().catch(() => ({}))) as { approved_by?: string; approved_note?: string };

    if (!body?.approved_by) {
      return HttpResponse.json(
        { success: false, code: "VALIDATION_ERROR", message: "approved_by is required", data: null, trace_id: "mock-err" },
        { status: 400 },
      );
    }

    const summary = mockPackages.find((p) => p.package_code === code && p.package_version === version);
    if (!summary) {
      return HttpResponse.json(
        { success: false, code: "DATA_MISSING", message: `配置包不存在: ${code}@${version}`, data: null, trace_id: "mock-err" },
        { status: 404 },
      );
    }

    const published: ConfigPackageDetail = {
      ...summary,
      status: "PUBLISHED",
      approved_by: body.approved_by,
      published_time: new Date().toISOString(),
      manifest: mockPackageDetails[`${code}@${version}`]?.manifest || {},
      diff: mockPackageDetails[`${code}@${version}`]?.diff || {},
      full_snapshot: mockPackageDetails[`${code}@${version}`]?.full_snapshot || {},
    };
    return HttpResponse.json(wrap({ ...published, trace_id: `mock-publish-${Date.now()}` }));
  }),

  // ─── Config Packages: export ───────────────────────────────────
  http.post(`${baseURL}/config-packages/:code/:version/export`, ({ params }) => {
    const code = params.code as string;
    const version = params.version as string;

    const summary = mockPackages.find((p) => p.package_code === code && p.package_version === version);
    if (!summary) {
      return HttpResponse.json(
        { success: false, code: "DATA_MISSING", message: `配置包不存在: ${code}@${version}`, data: null, trace_id: "mock-err" },
        { status: 404 },
      );
    }

    return HttpResponse.json(
      wrap({
        ...summary,
        ...mockPackageDetails[`${code}@${version}`],
        exported_time: new Date().toISOString(),
        export_format: "MEDKERNEL_CONFIG_PACKAGE_V1",
        trace_id: `mock-export-${Date.now()}`,
      }),
    );
  }),
];
