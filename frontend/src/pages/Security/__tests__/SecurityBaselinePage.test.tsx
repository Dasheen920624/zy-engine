import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import SecurityBaselinePage from "../SecurityBaselinePage";

// Mock API
vi.mock("../../../api/securityBaseline", () => ({
  getSecurityBaseline: vi.fn().mockResolvedValue({
    jwt_algorithm: "RS256",
    active_key_id: 1,
    total_key_versions: 3,
    tls_min_version: "TLSv1.3",
    password_hash_algorithm: "bcrypt",
    hsts_enabled: true,
    sbom_format: "CycloneDX",
    grace_keys: 0,
    retired_keys: 1,
    revoked_keys: 0,
    audit_chain: {
      total_records: 1000,
      last_record_hash: "abc123",
      hash_algorithm: "SHA-256",
    },
  }),
  verifyAuditChain: vi.fn(),
  listKeyVersions: vi.fn().mockResolvedValue([]),
  rotateKey: vi.fn(),
  revokeKey: vi.fn(),
  performVulnerabilityScan: vi.fn(),
}));

// Mock CSS module
vi.mock("../securityBaseline.module.css", () => ({
  default: {},
}));

describe("SecurityBaselinePage", () => {
  it("应渲染安全基线概览", () => {
    render(<SecurityBaselinePage />);
    expect(screen.getByText("安全基线概览")).toBeTruthy();
  });

  it("应显示审计链完整性区域", () => {
    render(<SecurityBaselinePage />);
    expect(screen.getByText("审计链完整性")).toBeTruthy();
  });

  it("应显示漏洞扫描区域", () => {
    render(<SecurityBaselinePage />);
    expect(screen.getByText("漏洞扫描")).toBeTruthy();
  });

  it("应显示密钥版本管理区域", () => {
    render(<SecurityBaselinePage />);
    expect(screen.getByText("密钥版本管理")).toBeTruthy();
  });

  it("应显示校验按钮", () => {
    render(<SecurityBaselinePage />);
    expect(screen.getByText("校验链完整性")).toBeTruthy();
  });

  it("应显示密钥轮换按钮", () => {
    render(<SecurityBaselinePage />);
    expect(screen.getByText("密钥轮换")).toBeTruthy();
  });
});
