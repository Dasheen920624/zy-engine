import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";

const createMemberMock = vi.fn();
const refetchCredentials = vi.fn();

vi.mock("@/shared/api/hooks", () => ({
  useUserRoleAssignments: () => ({ data: [], isLoading: false, refetch: vi.fn() }),
  useCreateUserRoleAssignment: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDeleteUserRoleAssignment: () => ({ mutateAsync: vi.fn(), isPending: false }),
  usePlatformCredentials: () => ({ data: [], refetch: refetchCredentials }),
  useCreateMember: () => ({ mutateAsync: createMemberMock, isPending: false }),
  useResetMemberPassword: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useSetCredentialStatus: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));

import AdminUsers from "./AdminUsers";

describe("AdminUsers · 成员账号管理", () => {
  beforeEach(() => {
    createMemberMock.mockReset();
    refetchCredentials.mockReset();
  });

  it("开通成员后展示一次性临时密码", async () => {
    createMemberMock.mockResolvedValue({
      userId: "drwang",
      username: "drwang",
      tempPassword: "TmpPwd@123",
    });
    render(<AdminUsers />);

    fireEvent.click(screen.getByRole("button", { name: "开通新成员" }));
    fireEvent.change(screen.getByPlaceholderText("如 drwang"), { target: { value: "drwang" } });
    fireEvent.click(screen.getByRole("button", { name: "确认开通成员" }));

    await waitFor(() =>
      expect(createMemberMock).toHaveBeenCalledWith({
        username: "drwang",
        roleCode: "doctor",
        initialPassword: undefined,
      }),
    );
    expect(await screen.findByText(/临时密码：TmpPwd@123/)).toBeInTheDocument();
  });

  it("登录名为空时拦截提交且不调用接口", async () => {
    render(<AdminUsers />);
    fireEvent.click(screen.getByRole("button", { name: "开通新成员" }));
    fireEvent.click(screen.getByRole("button", { name: "确认开通成员" }));
    await waitFor(() => expect(screen.getByText("登录名不能为空")).toBeInTheDocument());
    expect(createMemberMock).not.toHaveBeenCalled();
  });
});
