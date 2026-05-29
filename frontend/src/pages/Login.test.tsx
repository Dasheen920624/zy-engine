import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";

const navigateMock = vi.fn();
const mutateAsyncMock = vi.fn();
vi.mock("react-router-dom", () => ({ useNavigate: () => navigateMock }));
vi.mock("@/shared/api/hooks", () => ({
  useLogin: () => ({ mutateAsync: mutateAsyncMock, isPending: false }),
}));

import Login from "./Login";

describe("Login", () => {
  beforeEach(() => {
    navigateMock.mockReset();
    mutateAsyncMock.mockReset();
  });

  it("登录成功跳转 /dashboard", async () => {
    mutateAsyncMock.mockResolvedValue({
      userId: "doctor-1",
      tenantId: "t-1",
      roles: ["doctor"],
      mustChangePwd: false,
    });
    render(<Login />);
    fireEvent.change(screen.getByPlaceholderText("工号 / 账号"), { target: { value: "doctor" } });
    fireEvent.change(screen.getByPlaceholderText("密码"), { target: { value: "Mk@2026dev" } });
    fireEvent.click(screen.getByRole("button", { name: /进入工作台/ }));
    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith("/dashboard"));
  });

  it("登录失败显示错误且不跳转", async () => {
    mutateAsyncMock.mockRejectedValue({
      response: { data: { detail: "用户名或密码不正确" } },
    });
    render(<Login />);
    fireEvent.change(screen.getByPlaceholderText("工号 / 账号"), { target: { value: "x" } });
    fireEvent.change(screen.getByPlaceholderText("密码"), { target: { value: "y" } });
    fireEvent.click(screen.getByRole("button", { name: /进入工作台/ }));
    await waitFor(() => expect(screen.getByText("用户名或密码不正确")).toBeInTheDocument());
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
