import { describe, it, expect, vi, beforeEach } from "vitest";
import { mockHttpGet, mockHttpPost, mockGet, mockPost, resetMocks } from "./testUtils";

vi.mock("../client", () => ({
  http: {
    get: (...args: unknown[]) => mockHttpGet(...args),
    post: (...args: unknown[]) => mockHttpPost(...args),
    put: () => vi.fn(),
    delete: () => vi.fn(),
    patch: () => vi.fn(),
  },
  get: (...args: unknown[]) => mockGet(...args),
  post: (...args: unknown[]) => mockPost(...args),
  put: () => vi.fn(),
  del: () => vi.fn(),
}));

import * as auth from "../auth";

beforeEach(resetMocks);

describe("auth", () => {
  it("login should POST /auth/login with credentials", async () => {
    mockHttpPost.mockResolvedValueOnce({
      data: { success: true, data: { token: "t", user: { id: 1, username: "a" } } },
    });
    await auth.login({ username: "admin", password: "123" });
    expect(mockHttpPost).toHaveBeenCalledWith(
      "/auth/login",
      { username: "admin", password: "123" },
    );
  });

  it("fetchCurrentUser should GET /auth/me", async () => {
    mockHttpGet.mockResolvedValueOnce({
      data: { success: true, data: { id: 1, username: "admin" } },
    });
    await auth.fetchCurrentUser();
    expect(mockHttpGet).toHaveBeenCalledWith("/auth/me");
  });

  it("logout should POST /auth/logout", async () => {
    await auth.logout();
    expect(mockHttpPost).toHaveBeenCalledWith("/auth/logout");
  });
});
