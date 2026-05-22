import { vi } from "vitest";

export const mockHttpGet = vi.fn();
export const mockHttpPost = vi.fn();
export const mockHttpPut = vi.fn();
export const mockHttpDelete = vi.fn();
export const mockHttpPatch = vi.fn();

export const mockGet = vi.fn();
export const mockPost = vi.fn();
export const mockPut = vi.fn();
export const mockDel = vi.fn();

export function resetMocks() {
  vi.clearAllMocks();

  // `http` is the raw axios instance; callers typically access resp.data / resp.data.data
  const httpResp = { data: { success: true, data: {} } };
  mockHttpGet.mockResolvedValue(httpResp);
  mockHttpPost.mockResolvedValue(httpResp);
  mockHttpPut.mockResolvedValue(httpResp);
  mockHttpDelete.mockResolvedValue(httpResp);
  mockHttpPatch.mockResolvedValue(httpResp);

  // Thin wrappers already unwrap ApiResult, so they resolve to the inner data
  mockGet.mockResolvedValue({});
  mockPost.mockResolvedValue({});
  mockPut.mockResolvedValue({});
  mockDel.mockResolvedValue(undefined);
}
