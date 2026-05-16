import { get } from "./client";
import { SystemProviders } from "./types";

export async function fetchSystemProviders(): Promise<SystemProviders> {
  return get<SystemProviders>("/system/providers");
}

export async function fetchOrgContext(): Promise<unknown> {
  return get("/system/org-context");
}
