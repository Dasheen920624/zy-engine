import { http, HttpResponse } from "msw";
import { wrap, baseURL, demoUser } from "./shared";

export const authHandlers = [
  http.post(`${baseURL}/auth/login`, async ({ request }) => {
    const body = (await request.json().catch(() => ({}))) as { username?: string; password?: string };
    if (
      (body.username === "zhao01" && body.password === "demo123") ||
      (body.username === "admin" && body.password === "admin123")
    ) {
      return HttpResponse.json(
        wrap({
          token: "mock-jwt-zhao01-demo",
          user: demoUser,
        }),
      );
    }
    return HttpResponse.json(
      {
        success: false,
        code: "LOGIN_FAILED",
        message: "用户名或密码错误",
        data: null,
        trace_id: "mock-auth-failed",
      },
      { status: 401 },
    );
  }),

  http.get(`${baseURL}/auth/me`, () => HttpResponse.json(wrap(demoUser))),

  http.post(`${baseURL}/auth/logout`, () => HttpResponse.json(wrap(null))),
];
