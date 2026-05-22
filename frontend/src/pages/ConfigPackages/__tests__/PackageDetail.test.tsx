import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import PackageDetail from "../PackageDetail";

// Mock API
vi.mock("../../../api/configPackage", () => ({
  getPackageDetail: vi.fn().mockResolvedValue({
    package_code: "PKG-001",
    package_version: "1.0.0",
    manifest: { items: [] },
  }),
  reviewPackage: vi.fn().mockResolvedValue({
    ready_to_publish: true,
    issues: [],
  }),
  publishPackage: vi.fn().mockResolvedValue({}),
  exportPackage: vi.fn().mockResolvedValue({}),
}));

// Mock CSS module
vi.mock("../PackageDetail.module.css", () => ({
  default: {
    loadingContainer: "loadingContainer",
    sectionTitle: "sectionTitle",
    marginBottomSmall: "marginBottomSmall",
    codeFont: "codeFont",
    codeFontSmall: "codeFontSmall",
    dividerCompact: "dividerCompact",
    dividerNormal: "dividerNormal",
    marginRightSmall: "marginRightSmall",
    publishStatus: "publishStatus",
    publishStatusReady: "publishStatusReady",
    publishStatusNotReady: "publishStatusNotReady",
    iconSuccess: "iconSuccess",
    iconDanger: "iconDanger",
    iconWarning: "iconWarning",
    textSuccess: "textSuccess",
    textDanger: "textDanger",
    diffView: "diffView",
    diffViewDark: "diffViewDark",
    diffLine: "diffLine",
    diffLineAdd: "diffLineAdd",
    diffLineDel: "diffLineDel",
    diffLineNeutral: "diffLineNeutral",
    reviewPass: "reviewPass",
    reviewList: "reviewList",
    reviewItem: "reviewItem",
    reviewItemIcon: "reviewItemIcon",
    reviewMessage: "reviewMessage",
    sourceReview: "sourceReview",
    sourceReviewLabel: "sourceReviewLabel",
    reviewSummary: "reviewSummary",
    publishModalTitle: "publishModalTitle",
    publishWarning: "publishWarning",
    publishFormSection: "publishFormSection",
    publishFormLabel: "publishFormLabel",
    publishFormError: "publishFormError",
  },
}));

// Mock StatusBadge
vi.mock("../../../components/StatusBadge", () => ({
  StatusBadge: () => <span data-testid="status-badge" />,
}));

// Mock SourceInfo
vi.mock("../../../components/SourceInfo", () => ({
  SourceInfo: () => <div data-testid="source-info" />,
}));

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>
    {children}
  </QueryClientProvider>
);

describe("PackageDetail", () => {
  it("应在无选中包时显示空状态", () => {
    render(wrapper(<PackageDetail selectedPkg={null} />));
    expect(screen.getByText("选择配置包查看详情")).toBeTruthy();
  });

  it("应在选中包时渲染详情", () => {
    const selectedPkg = {
      package_code: "AMI_STEMI_BASELINE",
      package_version: "1.0.0",
      status: "DRAFT",
      asset_type: "RULE",
      scope_level: "TENANT",
      scope_code: "T-001",
      content_hash: "sha256:abc123",
      created_by: "admin",
      created_time: "2026-05-01T10:00:00",
    };
    render(wrapper(<PackageDetail selectedPkg={selectedPkg as never} />));
    expect(screen.getByText("基础信息")).toBeTruthy();
  });

  it("应显示重新校验按钮", () => {
    const selectedPkg = {
      package_code: "AMI_STEMI_BASELINE",
      package_version: "1.0.0",
      status: "DRAFT",
      asset_type: "RULE",
      scope_level: "TENANT",
      scope_code: "T-001",
      content_hash: "sha256:abc123",
      created_by: "admin",
      created_time: "2026-05-01T10:00:00",
    };
    render(wrapper(<PackageDetail selectedPkg={selectedPkg as never} />));
    expect(screen.getByText("重新校验")).toBeTruthy();
  });

  it("应显示导出按钮", () => {
    const selectedPkg = {
      package_code: "AMI_STEMI_BASELINE",
      package_version: "1.0.0",
      status: "DRAFT",
      asset_type: "RULE",
      scope_level: "TENANT",
      scope_code: "T-001",
      content_hash: "sha256:abc123",
      created_by: "admin",
      created_time: "2026-05-01T10:00:00",
    };
    render(wrapper(<PackageDetail selectedPkg={selectedPkg as never} />));
    expect(screen.getByText("导出")).toBeTruthy();
  });
});
