import { readUiPreference, writeUiPreference } from "@/shared/lib/browserStorage";

import type {
  AsyncExportRequest,
  ExperiencePageResponse,
  ExperienceViewSnapshot,
} from "./experienceTypes";

type CurrentPageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  total: number;
  hasNext: boolean;
  totalEstimated?: boolean;
  traceId?: string;
};

const SENSITIVE_SNAPSHOT_PATTERN =
  /(token|secret|password|passwd|api[-_.]?key|authorization|credential|patient|idcard|identity|身份证|患者)/i;
const storageKey = (key: string) => `medkernel.view.${key}`;

function assertNoSensitiveSnapshotContent(value: unknown) {
  const serialized = JSON.stringify(value);
  if (SENSITIVE_SNAPSHOT_PATTERN.test(serialized)) {
    throw new Error("敏感内容禁止写入体验视图快照");
  }
}

export function normalizePageResponse<T>(
  response: CurrentPageResponse<T>,
): ExperiencePageResponse<T> {
  return {
    items: response.items,
    pageNumber: response.page,
    pageSize: response.size,
    totalEstimate: response.total,
    hasMore: response.hasNext,
    traceId: response.traceId,
  };
}

export function writeExperienceView(key: string, snapshot: ExperienceViewSnapshot) {
  assertNoSensitiveSnapshotContent(snapshot);
  writeUiPreference(storageKey(key), JSON.stringify(snapshot));
}

export function readExperienceView(key: string): ExperienceViewSnapshot | null {
  const value = readUiPreference(storageKey(key));
  if (!value) return null;

  try {
    const snapshot = JSON.parse(value) as ExperienceViewSnapshot;
    assertNoSensitiveSnapshotContent(snapshot);
    return snapshot;
  } catch {
    return null;
  }
}

export function buildAsyncExportRequest(request: AsyncExportRequest): AsyncExportRequest {
  assertNoSensitiveSnapshotContent(request);
  return request;
}
