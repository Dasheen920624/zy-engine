import { get } from "./client";
import type {
  OrgContext,
  ProviderStatus,
  RawSystemProviders,
  SystemProviders,
} from "./types";

/**
 * 适配后端 HealthController.providers() 的 { database, graph, dify } 对象结构，
 * 展平成视图层期望的 ProviderStatus[] 数组。
 * Mock 若返回数组形式也兼容（避免破坏既有 mock 数据）。
 */
export async function fetchSystemProviders(): Promise<SystemProviders> {
  const raw = await get<RawSystemProviders>("/system/providers");
  const providers: ProviderStatus[] = [];
  if (Array.isArray(raw.providers)) {
    raw.providers.forEach((p, index) => {
      const provider = p as ProviderStatus;
      providers.push({
        name: provider.name ?? `provider-${index}`,
        role: p.role ?? "",
        ready: Boolean(p.ready),
        status: p.status ?? (p.ready ? "READY" : "DISABLED"),
        provider: p.provider ?? "",
        reason: p.degraded_reason ?? provider.reason ?? null,
      });
    });
  } else if (raw.providers && typeof raw.providers === "object") {
    Object.entries(raw.providers).forEach(([name, p]) => {
      providers.push({
        name,
        role: p.role ?? "",
        ready: Boolean(p.ready),
        status: p.status ?? (p.ready ? "READY" : "DISABLED"),
        provider: p.provider ?? "",
        reason: p.degraded_reason ?? null,
      });
    });
  }
  return {
    run_mode: raw.run_mode ?? "IN_MEMORY_DEMO",
    providers,
  };
}

export async function fetchOrgContext(): Promise<OrgContext> {
  return get<OrgContext>("/system/org-context");
}
