import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import { MemoryRouter } from "react-router-dom";
import UserList from "../UserList";
import { listUsers, updateUserStatus, unlockUser } from "../../../../api/userAdmin";

vi.mock("../../../../api/userAdmin", async () => {
  const actual =
    await vi.importActual<typeof import("../../../../api/userAdmin")>(
      "../../../../api/userAdmin",
    );
  return {
    ...actual,
    listUsers: vi.fn(),
    getUserDetail: vi.fn(),
    updateUserStatus: vi.fn(),
    unlockUser: vi.fn(),
    assignRoles: vi.fn(),
    listRoles: vi.fn(),
    importUsers: vi.fn(),
  };
});

const MOCK_USER_ACTIVE = {
  id: 1,
  tenant_id: 1,
  username: "admin_user",
  display_name: "管理员",
  email: "admin@hospital.com",
  phone: null,
  avatar_url: null,
  status: "ACTIVE" as const,
  user_type: "ADMIN",
  employee_id: null,
  last_login_time: "2026-05-22T10:00:00",
  last_login_ip: "192.168.1.1",
  login_attempts: 0,
  locked_until: null,
  roles: ["PLATFORM_ADMIN"],
};

const MOCK_USER_LOCKED = {
  ...MOCK_USER_ACTIVE,
  id: 2,
  username: "locked_user",
  display_name: "被锁定用户",
  status: "ACTIVE" as const,
  login_attempts: 6,
  locked_until: "2099-12-31T23:59:59",
  roles: [],
};

const MOCK_PAGE = {
  items: [MOCK_USER_ACTIVE, MOCK_USER_LOCKED],
  total: 2,
  page: 1,
  page_size: 20,
  total_pages: 1,
};

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <MemoryRouter>
      <ConfigProvider>
        <QueryClientProvider client={queryClient}>
          <UserList />
        </QueryClientProvider>
      </ConfigProvider>
    </MemoryRouter>,
  );
}

describe("UserList", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listUsers).mockResolvedValue(MOCK_PAGE);
    vi.mocked(updateUserStatus).mockResolvedValue(undefined);
    vi.mocked(unlockUser).mockResolvedValue(undefined);
  });

  it("renders page title", async () => {
    renderPage();
    expect(screen.getByText("用户管理")).toBeInTheDocument();
  });

  it("renders user list after load", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("admin_user")).toBeInTheDocument();
      expect(screen.getByText("locked_user")).toBeInTheDocument();
    });
  });

  it("shows ACTIVE tag for active user", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("正常")).toBeInTheDocument();
    });
  });

  it("shows locked tag for locked user", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("已锁定")).toBeInTheDocument();
    });
  });

  it("shows login attempts count", async () => {
    renderPage();
    await waitFor(() => {
      // locked user has 6 attempts — rendered as error Tag
      expect(screen.getByText("6")).toBeInTheDocument();
    });
  });

  it("applies keyword filter on search click", async () => {
    renderPage();
    await waitFor(() => screen.getByText("admin_user"));
    const input = screen.getByPlaceholderText("用户名 / 显示名 / 邮箱");
    fireEvent.change(input, { target: { value: "admin" } });
    fireEvent.click(screen.getByText("搜索"));
    expect(vi.mocked(listUsers)).toHaveBeenCalledWith(
      expect.objectContaining({ keyword: "admin", page: 1 }),
    );
  });

  it("opens CSV import dialog on button click", async () => {
    renderPage();
    await waitFor(() => screen.getByText("admin_user"));
    fireEvent.click(screen.getByText("批量导入"));
    expect(screen.getByText("批量导入用户（CSV）")).toBeInTheDocument();
  });

  it("calls listUsers once on mount", async () => {
    renderPage();
    await waitFor(() => {
      expect(vi.mocked(listUsers)).toHaveBeenCalledTimes(1);
    });
  });
});
