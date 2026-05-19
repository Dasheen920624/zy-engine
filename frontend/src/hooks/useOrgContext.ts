import { useSyncExternalStore } from "react";
import type { OrgContext } from "../api/types";
import { getOrgContext, subscribeOrgContext, setOrgContext } from "../store/orgContext";

export function useOrgContext(): [OrgContext, (next: OrgContext) => void] {
  const ctx = useSyncExternalStore(subscribeOrgContext, getOrgContext, getOrgContext);
  return [ctx, setOrgContext];
}
