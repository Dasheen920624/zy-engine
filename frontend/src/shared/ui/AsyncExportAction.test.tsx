import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ConfigProvider } from "antd";
import type { ComponentProps } from "react";
import { describe, expect, it, vi } from "vitest";

import { AsyncExportAction } from "./AsyncExportAction";
import type { AsyncExportJob, AsyncExportRequest } from "./experienceTypes";

const request: AsyncExportRequest = {
  resourceType: "terminology.mapping",
  requestSnapshot: {
    viewKey: "terminology.mapping",
    filters: [],
    pageRequest: { pageNumber: 1, pageSize: 20, filters: {} },
    visibleColumnKeys: ["status"],
    expertMode: false,
    capturedAt: "2026-05-26T00:00:00.000Z",
  },
  selectedScope: "currentPage",
  reason: "实施核查",
};

function renderAction(props: Partial<ComponentProps<typeof AsyncExportAction>> = {}) {
  return render(
    <ConfigProvider>
      <AsyncExportAction enabled permissionGranted request={request} {...props} />
    </ConfigProvider>,
  );
}

async function submitExport() {
  await userEvent.click(screen.getByRole("button", { name: "导出" }));
  await userEvent.click(screen.getByRole("button", { name: "提交导出任务" }));
}

describe("AsyncExportAction", () => {
  it("shows controlled disabled and forbidden states without submitting", async () => {
    const onSubmit = vi.fn();
    const { rerender } = renderAction({
      enabled: false,
      disabledReason: "导出任务接口待引擎包发布任务接入",
      onSubmit,
    });

    expect(screen.getByText("导出任务接口待引擎包发布任务接入")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "导出" })).toBeDisabled();

    rerender(
      <ConfigProvider>
        <AsyncExportAction
          enabled
          permissionGranted={false}
          request={request}
          onSubmit={onSubmit}
        />
      </ConfigProvider>,
    );

    expect(screen.getByText("当前权限不足，无法提交导出任务")).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("polls a running export and displays audit evidence after completion", async () => {
    let finishPoll: ((value: AsyncExportJob) => void) | undefined;
    const onSubmit = vi.fn().mockResolvedValue({
      jobId: "job-1",
      status: "running",
      submittedAt: "2026-05-26T01:00:00.000Z",
      submittedBy: "tester",
      traceId: "trace-1",
    });
    const onPoll = vi.fn().mockImplementation(
      () =>
        new Promise((resolve) => {
          finishPoll = resolve;
        }),
    );
    renderAction({ onSubmit, onPoll });

    await submitExport();
    expect(await screen.findByText("导出任务运行中")).toBeInTheDocument();

    finishPoll?.({
      jobId: "job-1",
      status: "succeeded",
      submittedAt: "2026-05-26T01:00:00.000Z",
      submittedBy: "tester",
      traceId: "trace-1",
      auditId: "audit-1",
      downloadUrl: "/exports/job-1",
    });

    expect(await screen.findByText("导出已完成")).toBeInTheDocument();
    expect(screen.getByText(/job-1/)).toBeInTheDocument();
    expect(screen.getByText(/trace-1/)).toBeInTheDocument();
    expect(screen.getByText(/audit-1/)).toBeInTheDocument();
    expect(onPoll).toHaveBeenCalledWith("job-1");
  });

  it("retries a failed submission using the original snapshot", async () => {
    const onSubmit = vi
      .fn()
      .mockRejectedValueOnce(new Error("服务暂时不可用"))
      .mockResolvedValueOnce({
        jobId: "job-2",
        status: "succeeded",
        submittedAt: "2026-05-26T01:00:00.000Z",
        submittedBy: "tester",
      });
    renderAction({ onSubmit });

    await submitExport();
    expect(await screen.findByText(/服务暂时不可用/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "重试导出" }));

    expect(await screen.findByText("导出已完成")).toBeInTheDocument();
    expect(onSubmit).toHaveBeenNthCalledWith(1, request);
    expect(onSubmit).toHaveBeenNthCalledWith(2, request);
  });
});
