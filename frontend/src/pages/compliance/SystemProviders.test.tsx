import { ConfigProvider } from "antd";
import { render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import SystemProviders from "./SystemProviders";
import { useRuntimeOperations } from "@/shared/api/hooks";

vi.mock("@/shared/api/hooks", () => ({
  useRuntimeOperations: vi.fn(),
}));

const snapshot = {
  serviceName: "medkernel",
  environment: "container",
  deploymentMode: "docker-core",
  databaseDialect: "postgres",
  migrationLocation: "classpath:db/migration/postgres",
  activeProfiles: ["dev", "container"],
  healthStatus: "UP",
  featureFlags: [
    {
      key: "graph-projection",
      displayName: "知识图谱投影",
      enabled: false,
      risk: "MEDIUM",
      owner: "信息科 / 架构组",
      description: "控制 Neo4j 图谱投影和图谱查询能力是否参与运行。",
    },
    {
      key: "dify-workflow",
      displayName: "Dify 工作流",
      enabled: true,
      risk: "MEDIUM",
      owner: "AI 平台组",
      description: "控制 Dify 工作流接入。",
    },
  ],
  dependencies: [
    {
      key: "database",
      displayName: "关系数据库",
      status: "UP",
      detail: "postgres · classpath:db/migration/postgres",
    },
    {
      key: "backup-restore",
      displayName: "备份恢复",
      status: "UP",
      detail: "SHA-256 摘要随备份文件生成，恢复前自动校验",
    },
  ],
  backup: {
    enabled: true,
    rpo: "24 小时",
    rto: "4 小时",
    backupScript: "./deploy/docker/scripts/backup.sh",
    restoreScript: "./deploy/docker/scripts/restore.sh",
    checksumPolicy: "SHA-256 摘要随备份文件生成，恢复前自动校验",
  },
  domesticProfile: {
    targetOs: "麒麟 / 统信 / openEuler",
    targetJdk: "KAE-JDK 21 / BiSheng JDK 21",
    databaseVendors: ["达梦", "人大金仓"],
    cryptoAlgorithms: ["SM2", "SM3", "SM4"],
    evidence: "国产化自检、五方言迁移合同、国密算法 smoke",
  },
  generatedAt: "2026-05-26T04:00:00Z",
};

describe("SystemProviders", () => {
  it("renders the real runtime operations snapshot instead of static provider demos", () => {
    vi.mocked(useRuntimeOperations).mockReturnValue({
      data: snapshot,
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
    } as never);

    render(
      <ConfigProvider>
        <SystemProviders />
      </ConfigProvider>,
    );

    expect(screen.getByRole("heading", { name: "Provider 状态" })).toBeInTheDocument();
    expect(screen.getByText("docker-core")).toBeInTheDocument();
    expect(screen.getByText("postgres")).toBeInTheDocument();
    expect(screen.getByText("知识图谱投影")).toBeInTheDocument();
    expect(screen.getByText("Dify 工作流")).toBeInTheDocument();
    expect(screen.getAllByText("备份恢复").length).toBeGreaterThan(0);
    expect(
      screen.getAllByText("SHA-256 摘要随备份文件生成，恢复前自动校验").length,
    ).toBeGreaterThan(0);
    expect(screen.getAllByText(/麒麟 \/ 统信 \/ openEuler/).length).toBeGreaterThan(0);
    expect(
      within(screen.getByTestId("runtime-dependencies")).getByText("关系数据库"),
    ).toBeInTheDocument();

    expect(screen.queryByText("Oracle 23ai · 主库")).not.toBeInTheDocument();
    expect(screen.queryByText(/总院 PACS/)).not.toBeInTheDocument();
  });
});
