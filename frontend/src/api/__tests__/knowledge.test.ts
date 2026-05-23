import { describe, it, expect, vi, beforeEach } from "vitest";
import { mockGet, mockPost, mockPut, resetMocks } from "./testUtils";

vi.mock("../client", () => ({
  http: {
    get: () => vi.fn(),
    post: () => vi.fn(),
    put: () => vi.fn(),
    delete: () => vi.fn(),
    patch: () => vi.fn(),
  },
  get: (...args: unknown[]) => mockGet(...args),
  post: (...args: unknown[]) => mockPost(...args),
  put: (...args: unknown[]) => mockPut(...args),
  del: () => vi.fn(),
}));

import * as knowledge from "../knowledge";

beforeEach(resetMocks);

describe("knowledge", () => {
  it("listKnowledgeSources should GET /knowledge/sources with optional params", async () => {
    mockGet.mockResolvedValueOnce([]);
    await knowledge.listKnowledgeSources({ source_type: "GUIDELINE" });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain("/knowledge/sources?");
    expect(url).toContain("source_type=GUIDELINE");
  });

  it("listKnowledgeSources should GET /knowledge/sources without query", async () => {
    mockGet.mockResolvedValueOnce([]);
    await knowledge.listKnowledgeSources();
    expect(mockGet).toHaveBeenCalledWith("/knowledge/sources");
  });

  it("getKnowledgeSource should GET /knowledge/sources/{sourceCode}", async () => {
    mockGet.mockResolvedValueOnce({});
    await knowledge.getKnowledgeSource("SRC1");
    expect(mockGet).toHaveBeenCalledWith("/knowledge/sources/SRC1");
  });

  it("registerKnowledgeSource should POST /knowledge/sources", async () => {
    mockPost.mockResolvedValueOnce({});
    await knowledge.registerKnowledgeSource({
      source_name: "Test",
      source_type: "GUIDELINE",
    });
    expect(mockPost).toHaveBeenCalledWith(
      "/knowledge/sources",
      expect.objectContaining({ source_name: "Test" }),
    );
  });

  it("updateKnowledgeSource should PUT /knowledge/sources/{sourceCode}", async () => {
    mockPut.mockResolvedValueOnce({});
    await knowledge.updateKnowledgeSource("SRC1", { source_name: "Updated" });
    expect(mockPut).toHaveBeenCalledWith("/knowledge/sources/SRC1", { source_name: "Updated" });
  });

  it("reviewKnowledgeSource should POST /knowledge/sources/{sourceCode}/review", async () => {
    mockPost.mockResolvedValueOnce({});
    await knowledge.reviewKnowledgeSource("SRC1", {
      review_status: "APPROVED",
      reviewed_by: "admin",
    });
    expect(mockPost).toHaveBeenCalledWith(
      "/knowledge/sources/SRC1/review",
      { review_status: "APPROVED", reviewed_by: "admin" },
    );
  });

  it("listKnowledgeSubscriptions should GET /knowledge/subscriptions with optional params", async () => {
    mockGet.mockResolvedValueOnce([]);
    await knowledge.listKnowledgeSubscriptions({ topic_type: "DISEASE" });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain("/knowledge/subscriptions?");
    expect(url).toContain("topic_type=DISEASE");
  });

  it("createKnowledgeSubscription should POST /knowledge/subscriptions", async () => {
    mockPost.mockResolvedValueOnce({});
    await knowledge.createKnowledgeSubscription({
      topic_type: "DISEASE",
      topic_name: "AMI",
    });
    expect(mockPost).toHaveBeenCalledWith(
      "/knowledge/subscriptions",
      expect.objectContaining({ topic_name: "AMI" }),
    );
  });

  it("updateKnowledgeSubscription should PUT /knowledge/subscriptions/{id}", async () => {
    mockPut.mockResolvedValueOnce({});
    await knowledge.updateKnowledgeSubscription("SUB1", { topic_name: "Updated" });
    expect(mockPut).toHaveBeenCalledWith("/knowledge/subscriptions/SUB1", { topic_name: "Updated" });
  });

  it("pauseKnowledgeSubscription should POST /knowledge/subscriptions/{id}/pause", async () => {
    mockPost.mockResolvedValueOnce({});
    await knowledge.pauseKnowledgeSubscription("SUB1");
    expect(mockPost).toHaveBeenCalledWith("/knowledge/subscriptions/SUB1/pause", {});
  });

  it("cancelKnowledgeSubscription should POST /knowledge/subscriptions/{id}/cancel", async () => {
    mockPost.mockResolvedValueOnce({});
    await knowledge.cancelKnowledgeSubscription("SUB1");
    expect(mockPost).toHaveBeenCalledWith("/knowledge/subscriptions/SUB1/cancel", {});
  });
});
