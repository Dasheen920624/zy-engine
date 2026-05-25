import { describe, expect, it } from "vitest";
import { findRouteByPath, getRouteBreadcrumb, routeMetas, customerRouteMetas } from "./routes";

describe("route metadata", () => {
  it("registers every current frontend page route", () => {
    expect(routeMetas.length).toBeGreaterThanOrEqual(34);
    expect(findRouteByPath("/terminology/mapping")?.title).toBe("字典映射");
    expect(findRouteByPath("/advanced/graph")?.hidden).toBe(true);
  });

  it("keeps paths unique", () => {
    const paths = routeMetas.map((route) => route.path);
    expect(new Set(paths).size).toBe(paths.length);
  });

  it("requires breadcrumb metadata for authenticated pages", () => {
    routeMetas
      .filter((route) => route.requireAuth)
      .forEach((route) => {
        expect(route.breadcrumb.length).toBeGreaterThan(0);
        expect(route.title.length).toBeGreaterThan(0);
      });
  });

  it("returns customer routes without hidden advanced tools", () => {
    expect(customerRouteMetas.some((route) => route.hidden)).toBe(false);
    expect(customerRouteMetas.map((route) => route.path)).toContain("/dashboard");
    expect(customerRouteMetas.map((route) => route.path)).not.toContain("/advanced/dev-console");
  });

  it("builds breadcrumbs from route metadata", () => {
    expect(getRouteBreadcrumb("/qc/dashboard")).toEqual(["质控改进", "院级质控驾驶舱"]);
    expect(getRouteBreadcrumb("/missing")).toEqual(["未找到页面"]);
  });
});
