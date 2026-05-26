# GA-ENG-BASE-08 产品体验底座实施计划

> **给 AI 执行者：** 必须使用 `superpowers:subagent-driven-development`（如可用）或 `superpowers:executing-plans` 实施本计划。步骤使用复选框（`- [ ]`）跟踪。

**目标：** 上线可复用的产品体验底座，并以“字典映射”作为只读核查样板页，满足一页一目标、角色默认视图、专家模式、服务端分页、详情抽屉、异步导出入口和保存视图门禁。

**架构：** 先在 `shared/ui` 建立体验类型、分页适配、视图快照与导出快照；再让 `routes.ts` 作为路由、菜单、面包屑、权限与页面体验声明的单一事实源；最后用公共组件改造 `TerminologyMapping`，继续调用已上线的 GA-ENG-API-04 真实接口。所有实现先写失败测试，再写最小实现。

**技术栈：** React 18、TypeScript 5.6、Ant Design 5、React Query 5、Vitest、Testing Library、现有 `apiClient` 与受控 UI 偏好存储。

---

## Chunk 1：体验声明、公共组件与字典映射样板

### 文件结构

| 文件 | 操作 | 责任 |
|---|---|---|
| `frontend/src/shared/ui/experienceTypes.ts` | 新增 | 存放体验声明、筛选、分页、列、部分成功、导出和视图快照类型 |
| `frontend/src/shared/ui/experienceView.ts` | 新增 | 归一化分页响应、校验并读写视图快照、生成导出请求快照 |
| `frontend/src/shared/ui/experienceView.test.ts` | 新增 | 验证分页适配、敏感内容阻断、保存视图、导出快照和选择态 |
| `frontend/src/shared/config/routes.ts` | 修改 | 引入 `RouteExperience` 并为所有有 `menuKey` 的认证路由补体验声明 |
| `frontend/src/shared/config/routes.test.ts` | 修改 | 增加体验声明门禁测试 |
| `frontend/src/shared/ui/PageExperienceShell.tsx` | 新增 | 包装 `PageShell`，展示主目标、默认视图、角色、证据入口和专家模式入口 |
| `frontend/src/shared/ui/PageExperienceShell.test.tsx` | 新增 | 验证主目标、专家模式权限和单一主按钮承载 |
| `frontend/src/shared/ui/ExperienceFilterBar.tsx` | 新增 | 渲染最多 3 个默认筛选，并把高级筛选留给受控扩展 |
| `frontend/src/shared/ui/ExperienceFilterBar.test.tsx` | 新增 | 验证筛选数量、选择项来源、中文占位文案和变更事件 |
| `frontend/src/shared/ui/EvidenceDetailDrawer.tsx` | 新增 | 右侧详情抽屉，普通模式隐藏专家字段 |
| `frontend/src/shared/ui/EvidenceDetailDrawer.test.tsx` | 新增 | 验证普通/专家模式、加载、错误和重试 |
| `frontend/src/shared/ui/AsyncExportAction.tsx` | 新增 | 统一导出入口，支持禁用态、权限不足、提交、轮询、失败重试 |
| `frontend/src/shared/ui/AsyncExportAction.test.tsx` | 新增 | 验证导出状态、审计快照和失败重试 |
| `frontend/src/shared/ui/ServerDataTable.tsx` | 新增 | 统一服务端分页表格、列管理、部分成功、选择态和详情打开 |
| `frontend/src/shared/ui/ServerDataTable.test.tsx` | 新增 | 验证分页、默认列限制、列可见性快照、部分成功、详情打开不刷新列表 |
| `frontend/src/pages/tenant/TerminologyMapping.tsx` | 修改 | 改造为公共底座样板页，只读核查，不实现确认、发布、回滚或批量处理 |
| `frontend/src/pages/tenant/TerminologyMapping.test.tsx` | 新增 | 验证字典映射样板页六态、默认筛选、分页、详情抽屉、保存视图、导出禁用态 |
| `frontend/src/shared/api/hooks.ts` | 修改 | 仅在查询参数类型不足时补类型，不改变 API 路径 |
| `frontend/src/test/visualDebtGuard.test.ts` | 修改 | 增加内网/外网门禁：生产代码不直连外部服务，不保存敏感视图数据 |
| `frontend/src/app/index.css` | 修改 | 如组件需要固定宽度或布局类，只添加 token 化类名，不写 JSX inline style |
| `frontend/README.md` | 修改 | 增加产品体验底座接入说明 |
| `docs/backlog.md` | 修改 | `GA-ENG-BASE-08` 完成后标记 `done` 并记录验证 |
| `docs/superpowers/plans/2026-05-26-base-08-product-experience-foundation.md` | 修改 | 执行时勾选步骤 |

### 任务 1：体验类型、分页适配、保存视图与导出快照

**文件：**
- 新增：`frontend/src/shared/ui/experienceTypes.ts`
- 新增：`frontend/src/shared/ui/experienceView.ts`
- 新增：`frontend/src/shared/ui/experienceView.test.ts`

- [ ] **步骤 1：写失败测试，验证分页适配、保存视图和导出快照**

  创建 `frontend/src/shared/ui/experienceView.test.ts`：

  ```ts
  import { beforeEach, describe, expect, it } from "vitest";
  import {
    buildAsyncExportRequest,
    normalizePageResponse,
    readExperienceView,
    writeExperienceView,
  } from "./experienceView";

  const snapshot = {
    viewKey: "terminology.mapping",
    filters: [{ key: "status", value: "DRAFT" }],
    pageRequest: {
      pageNumber: 1,
      pageSize: 20,
      sortBy: "updatedAt",
      sortOrder: "desc",
      filters: { status: "DRAFT" },
    },
    visibleColumnKeys: ["sourceSystem", "status"],
    expertMode: false,
    capturedAt: "2026-05-26T00:00:00.000Z",
  } as const;

  describe("experienceView", () => {
    beforeEach(() => window.localStorage.clear());

    it("normalizes current PageResponse into the experience pagination contract", () => {
      const result = normalizePageResponse({
        items: [{ id: 1 }],
        page: 2,
        size: 20,
        total: 41,
        hasNext: true,
        totalEstimated: false,
        traceId: "trace-1",
      });

      expect(result).toMatchObject({
        items: [{ id: 1 }],
        pageNumber: 2,
        pageSize: 20,
        totalEstimate: 41,
        hasMore: true,
        traceId: "trace-1",
      });
    });

    it("stores reproducible UI view snapshots without sensitive content", () => {
      writeExperienceView("terminology.mapping", snapshot);

      expect(readExperienceView("terminology.mapping")?.visibleColumnKeys).toEqual([
        "sourceSystem",
        "status",
      ]);
      expect(readExperienceView("terminology.mapping")?.expertMode).toBe(false);
      expect(window.localStorage.getItem("medkernel.view.terminology.mapping")).toContain(
        "updatedAt",
      );
    });

    it("rejects sensitive snapshot keys before writing local storage", () => {
      expect(() =>
        writeExperienceView("terminology.mapping", {
          ...snapshot,
          pageRequest: {
            ...snapshot.pageRequest,
            filters: { patientId: "p-1" },
          },
        }),
      ).toThrow(/敏感/);
      expect(window.localStorage.getItem("medkernel.view.terminology.mapping")).toBeNull();
    });

    it("builds auditable export requests from view and selection snapshots", () => {
      const request = buildAsyncExportRequest({
        resourceType: "terminology.mapping",
        requestSnapshot: snapshot,
        selectedScope: "currentPage",
        selectionSnapshot: { selectedRowKeys: [1, 2], rowCount: 2 },
        reason: "导出当前页用于实施核查",
      });

      expect(request.requestSnapshot.pageRequest).toMatchObject({
        pageNumber: 1,
        pageSize: 20,
        sortBy: "updatedAt",
        sortOrder: "desc",
      });
      expect(request.selectionSnapshot?.selectedRowKeys).toEqual([1, 2]);
      expect(request.reason).toBe("导出当前页用于实施核查");
    });
  });
  ```

- [ ] **步骤 2：运行红灯测试**

  ```bash
  cd frontend
  npm test -- experienceView.test.ts
  ```

  预期：失败，原因是 `experienceView.ts` 和 `experienceTypes.ts` 尚不存在。

- [ ] **步骤 3：实现 `experienceTypes.ts`**

  创建类型文件，至少包含：

  ```ts
  import type { Key, ReactNode } from "react";

  export type InterruptionLevel = "none" | "info" | "weak" | "strong";
  export type PageRiskLevel = "low" | "medium" | "high";
  export type ExperiencePageSize = 20 | 50 | 100;

  export interface ExperienceFilterOption {
    label: string;
    value: string;
  }

  export interface ExperienceFilterDefinition {
    key: string;
    label: string;
    kind: "select" | "dateRange" | "search";
    placeholder?: string;
    options?: ExperienceFilterOption[];
    optionSource?: "static" | "api" | "routeMeta";
    apiPath?: string;
  }

  export interface ExperienceFilterValue {
    key: string;
    value: string | [string, string] | undefined;
  }

  export interface RouteExperience {
    primaryRole: string;
    goal: string;
    defaultView: string;
    defaultFilters: ExperienceFilterDefinition[];
    expertContent: string[];
    interruptionLevel: InterruptionLevel;
    evidence: string;
    dataScale: {
      expected: "small" | "large" | "massive";
      pagination: "page" | "cursor";
      exportStrategy: "none" | "disabled" | "async";
    };
    riskLevel: PageRiskLevel;
  }

  export interface ExperiencePageRequest {
    pageNumber?: number;
    pageSize: ExperiencePageSize;
    pageToken?: string;
    sortBy?: string;
    sortOrder?: "asc" | "desc";
    filters: Record<string, unknown>;
  }

  export interface ExperiencePageResponse<T> {
    items: T[];
    pageNumber?: number;
    pageSize: number;
    nextPageToken?: string | null;
    totalEstimate: number;
    hasMore: boolean;
    traceId?: string;
  }

  export interface ExperienceColumn<T> {
    key: string;
    title: string;
    dataIndex?: keyof T;
    width?: number;
    always?: boolean;
    expertOnly?: boolean;
    render?: (value: unknown, record: T) => ReactNode;
  }

  export interface ExperiencePartialResult {
    successCount: number;
    failureCount: number;
    failures: Array<{ key: string; reason: string; retryable: boolean }>;
    onRetryFailures?: () => void;
  }

  export interface ExperienceViewSnapshot {
    viewKey: string;
    filters: ExperienceFilterValue[];
    pageRequest: ExperiencePageRequest;
    visibleColumnKeys: string[];
    expertMode: boolean;
    capturedAt: string;
  }

  export interface AsyncExportRequest {
    resourceType: string;
    requestSnapshot: ExperienceViewSnapshot;
    selectedScope: "currentPage" | "filteredResult";
    selectionSnapshot?: { selectedRowKeys: Key[]; rowCount: number };
    reason: string;
  }

  export type ExportJobStatus =
    | "pending"
    | "running"
    | "succeeded"
    | "failed"
    | "expired"
    | "disabled"
    | "forbidden";

  export interface AsyncExportJob {
    jobId: string;
    status: ExportJobStatus;
    submittedAt: string;
    submittedBy: string;
    traceId?: string;
    auditId?: string;
    downloadUrl?: string;
    failureReason?: string;
  }

  export interface AsyncExportActionProps {
    enabled: boolean;
    disabledReason?: string;
    permissionGranted: boolean;
    request: AsyncExportRequest;
    onSubmit?: (request: AsyncExportRequest) => Promise<AsyncExportJob>;
    onPoll?: (jobId: string) => Promise<AsyncExportJob>;
  }
  ```

- [ ] **步骤 4：实现 `experienceView.ts`**

  创建分页适配、敏感内容校验、视图存储和导出请求构建：

  ```ts
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
    const json = JSON.stringify(value);
    if (SENSITIVE_SNAPSHOT_PATTERN.test(json)) {
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
      return JSON.parse(value) as ExperienceViewSnapshot;
    } catch {
      return null;
    }
  }

  export function buildAsyncExportRequest(request: AsyncExportRequest): AsyncExportRequest {
    assertNoSensitiveSnapshotContent(request);
    return request;
  }
  ```

- [ ] **步骤 5：运行绿灯测试**

  ```bash
  cd frontend
  npm test -- experienceView.test.ts browserStorage.test.ts
  ```

  预期：全部通过。

- [ ] **步骤 6：提交**

  ```bash
  git add frontend/src/shared/ui/experienceTypes.ts frontend/src/shared/ui/experienceView.ts frontend/src/shared/ui/experienceView.test.ts
  git commit -m "feat(GA-ENG-BASE-08): 增加体验分页与视图快照"
  ```

### 任务 2：路由体验声明门禁

**文件：**
- 修改：`frontend/src/shared/config/routes.ts`
- 修改：`frontend/src/shared/config/routes.test.ts`

- [ ] **步骤 1：写失败测试，要求有菜单入口的认证路由必须有体验声明**

  在 `frontend/src/shared/config/routes.test.ts` 增加测试：

  ```ts
  it("requires experience metadata for authenticated menu routes", () => {
    const menuRoutes = routeMetas.filter((route) => route.requireAuth && route.menuKey);

    expect(menuRoutes.length).toBeGreaterThan(0);
    menuRoutes.forEach((route) => {
      expect(route.experience, `${route.path} 缺少 experience`).toBeDefined();
      expect(route.experience?.primaryRole).toBeTruthy();
      expect(route.experience?.goal).toBeTruthy();
      expect(route.experience?.defaultView).toBeTruthy();
      expect(route.experience?.evidence).toBeTruthy();
      expect(route.experience?.interruptionLevel).toMatch(/^(none|info|weak|strong)$/);
      expect(route.experience?.dataScale.exportStrategy).toMatch(/^(none|disabled|async)$/);
      expect(route.experience?.defaultFilters.length ?? 0).toBeLessThanOrEqual(3);
    });
  });
  ```

- [ ] **步骤 2：运行红灯测试**

  ```bash
  cd frontend
  npm test -- routes.test.ts
  ```

  预期：失败，原因是有 `menuKey` 的路由尚未声明 `experience`。

- [ ] **步骤 3：扩展 `RouteMeta` 并引入类型**

  在 `frontend/src/shared/config/routes.ts` 引入类型：

  ```ts
  import type { RouteExperience } from "@/shared/ui/experienceTypes";
  ```

  并给 `RouteMeta` 增加：

  ```ts
  experience?: RouteExperience;
  ```

- [ ] **步骤 4：为所有有 `menuKey` 的认证路由补体验声明**

  可使用本地 helper 减少重复，但每个声明必须完整：

  ```ts
  const disabledListExperience = (
    primaryRole: string,
    goal: string,
    defaultView: string,
  ): RouteExperience => ({
    primaryRole,
    goal,
    defaultView,
    defaultFilters: [],
    expertContent: ["traceId", "原始字段"],
    interruptionLevel: "info",
    evidence: "保留来源、版本、审计和导出入口",
    dataScale: { expected: "small", pagination: "page", exportStrategy: "disabled" },
    riskLevel: "low",
  });
  ```

  `TerminologyMapping` 使用专门声明：

  ```ts
  experience: {
    primaryRole: "实施工程师 / 信息科 / 医务处",
    goal: "核查院内码与标准码的映射关系，降低后续规则和路径执行风险",
    defaultView: "最近更新的待确认和高风险映射优先",
    defaultFilters: [
      {
        key: "status",
        label: "映射状态",
        kind: "select",
        placeholder: "请选择映射状态",
        optionSource: "static",
        options: [
          { label: "草稿", value: "DRAFT" },
          { label: "已确认", value: "CONFIRMED" },
          { label: "已替换", value: "SUPERSEDED" },
          { label: "已回滚", value: "ROLLED_BACK" },
        ],
      },
      { key: "sourceSystem", label: "来源系统", kind: "search", placeholder: "输入来源系统" },
      {
        key: "keyword",
        label: "关键词",
        kind: "search",
        placeholder: "输入院内码或标准码关键词",
      },
    ],
    expertContent: ["映射 ID", "院内编码 ID", "标准编码 ID", "traceId", "接口原始状态"],
    interruptionLevel: "info",
    evidence: "详情抽屉展示证据文本、确认人、确认时间和审计入口",
    dataScale: { expected: "large", pagination: "page", exportStrategy: "disabled" },
    riskLevel: "medium",
  }
  ```

- [ ] **步骤 5：运行绿灯测试**

  ```bash
  cd frontend
  npm test -- routes.test.ts
  ```

  预期：通过。

- [ ] **步骤 6：提交**

  ```bash
  git add frontend/src/shared/config/routes.ts frontend/src/shared/config/routes.test.ts
  git commit -m "feat(GA-ENG-BASE-08): 增加页面体验声明门禁"
  ```

### 任务 3：PageExperienceShell 专家模式权限

**文件：**
- 新增：`frontend/src/shared/ui/PageExperienceShell.tsx`
- 新增：`frontend/src/shared/ui/PageExperienceShell.test.tsx`

- [ ] **步骤 1：写失败测试**

  创建测试，覆盖权限画像允许时显示专家开关、权限画像不允许时隐藏专家开关：

  ```ts
  const expertProfile = {
    permissions: [{ code: "advanced.read", displayName: "高级工具", risk: "LOW" }],
    menuKeys: ["advanced-tools"],
  };
  const normalProfile = { permissions: [], menuKeys: [] };

  render(
    <PageExperienceShell meta={meta} securityProfile={expertProfile}>
      内容
    </PageExperienceShell>,
  );
  expect(screen.getByText("目标：核查映射风险")).toBeInTheDocument();
  expect(screen.getByText("默认视图：最近更新")).toBeInTheDocument();
  expect(screen.getByRole("switch", { name: "专家模式" })).toBeInTheDocument();

  rerender(
    <PageExperienceShell meta={meta} securityProfile={normalProfile}>
      内容
    </PageExperienceShell>,
  );
  expect(screen.queryByRole("switch", { name: "专家模式" })).not.toBeInTheDocument();
  ```

- [ ] **步骤 2：运行红灯测试**

  ```bash
  cd frontend
  npm test -- PageExperienceShell.test.tsx
  ```

  预期：失败，原因是组件不存在。

- [ ] **步骤 3：实现组件**

  用 `PageShell` 包装，不写 JSX inline style。Props 至少包含：

  ```ts
  interface PageExperienceShellProps {
    meta: { title: string; experience: RouteExperience };
    securityProfile?: Pick<SecurityProfile, "permissions" | "menuKeys">;
    expertMode?: boolean;
    onExpertModeChange?: (enabled: boolean) => void;
    primary?: React.ReactNode;
    extras?: React.ReactNode;
    children: React.ReactNode;
  }
  ```

  只有 `securityProfile.menuKeys` 包含 `advanced-tools`，或 `securityProfile.permissions` 包含 `advanced.read` / `system.debug` 等专家权限，且 `experience.expertContent.length > 0` 时显示专家模式开关。未取得权限画像时默认不显示专家入口。

- [ ] **步骤 4：运行绿灯测试**

  ```bash
  cd frontend
  npm test -- PageExperienceShell.test.tsx
  ```

  预期：通过。

- [ ] **步骤 5：提交**

  ```bash
  git add frontend/src/shared/ui/PageExperienceShell.tsx frontend/src/shared/ui/PageExperienceShell.test.tsx
  git commit -m "feat(GA-ENG-BASE-08): 增加体验页骨架"
  ```

### 任务 4：ExperienceFilterBar 默认筛选

**文件：**
- 新增：`frontend/src/shared/ui/ExperienceFilterBar.tsx`
- 新增：`frontend/src/shared/ui/ExperienceFilterBar.test.tsx`

- [ ] **步骤 1：写失败测试**

  测试覆盖：
  - 默认筛选超过 3 个时抛错。
  - `select` 没有 `options` 且没有 `apiPath` 或自定义渲染时抛错。
  - `search` 使用中文占位文案。
  - 输入后触发 `onChange`。

- [ ] **步骤 2：运行红灯测试**

  ```bash
  cd frontend
  npm test -- ExperienceFilterBar.test.tsx
  ```

  预期：失败，原因是组件不存在。

- [ ] **步骤 3：实现组件**

  使用 `Space`、`Select`、`Input.Search`、`DatePicker.RangePicker`。`renderFilter` 存在时优先使用自定义渲染；不直接调用外部 API。

- [ ] **步骤 4：运行绿灯测试**

  ```bash
  cd frontend
  npm test -- ExperienceFilterBar.test.tsx
  ```

  预期：通过。

- [ ] **步骤 5：提交**

  ```bash
  git add frontend/src/shared/ui/ExperienceFilterBar.tsx frontend/src/shared/ui/ExperienceFilterBar.test.tsx
  git commit -m "feat(GA-ENG-BASE-08): 增加体验筛选栏"
  ```

### 任务 5：EvidenceDetailDrawer 详情抽屉

**文件：**
- 新增：`frontend/src/shared/ui/EvidenceDetailDrawer.tsx`
- 新增：`frontend/src/shared/ui/EvidenceDetailDrawer.test.tsx`

- [ ] **步骤 1：写失败测试**

  测试覆盖：
  - 普通模式隐藏 `expertOnly` 字段。
  - 专家模式展示 `traceId` 和专家字段。
  - 加载态显示“正在加载详情”。
  - 错误态显示“重试”按钮并触发 `onRetry`。

- [ ] **步骤 2：运行红灯测试**

  ```bash
  cd frontend
  npm test -- EvidenceDetailDrawer.test.tsx
  ```

  预期：失败，原因是组件不存在。

- [ ] **步骤 3：实现组件**

  使用 `Drawer`、`Descriptions`、`Alert`、`Spin`、`Button`。打开详情不得刷新列表，组件只通过 props 渲染。

- [ ] **步骤 4：运行绿灯测试**

  ```bash
  cd frontend
  npm test -- EvidenceDetailDrawer.test.tsx
  ```

  预期：通过。

- [ ] **步骤 5：提交**

  ```bash
  git add frontend/src/shared/ui/EvidenceDetailDrawer.tsx frontend/src/shared/ui/EvidenceDetailDrawer.test.tsx
  git commit -m "feat(GA-ENG-BASE-08): 增加证据详情抽屉"
  ```

### 任务 6：AsyncExportAction 导出入口

**文件：**
- 新增：`frontend/src/shared/ui/AsyncExportAction.tsx`
- 新增：`frontend/src/shared/ui/AsyncExportAction.test.tsx`

- [ ] **步骤 1：写失败测试**

  测试覆盖：
  - `enabled=false` 显示禁用原因，不调用 `onSubmit`。
  - `permissionGranted=false` 显示权限不足。
  - 提交成功显示 `jobId`、`traceId`。
  - 提交成功返回 `running` 时显示“导出任务运行中”，并调用 `onPoll` 查询后续状态。
  - `onPoll` 返回 `succeeded` 时显示 `auditId`、`downloadUrl` 和完成状态。
  - 提交失败显示中文失败原因和重试入口。
  - 重试时复用原 `requestSnapshot` 和 `selectionSnapshot`。

- [ ] **步骤 2：运行红灯测试**

  ```bash
  cd frontend
  npm test -- AsyncExportAction.test.tsx
  ```

  预期：失败，原因是组件不存在。

- [ ] **步骤 3：实现组件**

  使用 `Button`、`Modal`、`Alert`。组件接收完整 `AsyncExportActionProps`；提交时只调用传入的 `onSubmit`；当任务状态为 `running` 且存在 `onPoll` 时轮询一次或按测试可控条件查询状态；禁用态不伪造导出任务。`AsyncExportJob` 必须包含 `jobId`、`status`、`submittedAt`、`submittedBy`，并可带 `traceId`、`auditId`、`downloadUrl`、`failureReason`。

- [ ] **步骤 4：运行绿灯测试**

  ```bash
  cd frontend
  npm test -- AsyncExportAction.test.tsx
  ```

  预期：通过。

- [ ] **步骤 5：提交**

  ```bash
  git add frontend/src/shared/ui/AsyncExportAction.tsx frontend/src/shared/ui/AsyncExportAction.test.tsx
  git commit -m "feat(GA-ENG-BASE-08): 增加异步导出入口"
  ```

### 任务 7：ServerDataTable 分页、列管理与部分成功

**文件：**
- 新增：`frontend/src/shared/ui/ServerDataTable.tsx`
- 新增：`frontend/src/shared/ui/ServerDataTable.test.tsx`
- 修改：`frontend/src/app/index.css`（仅当需要新布局类）

- [ ] **步骤 1：写失败测试**

  测试覆盖：
  - 默认可见列超过 8 个时抛错。
  - 分页变化调用 `onRequestChange` 并带 `pageNumber/pageSize/sort/filters`。
  - 点击“查看”调用 `onOpenDetail`，不调用 `onRequestChange`。
  - `partial` 显示成功数、失败数、失败原因和重试入口。
  - 列管理变更时调用 `onViewSnapshotChange`，写入 `visibleColumnKeys`。
  - 当前页选择时生成 `selectionSnapshot` 所需的 `selectedRowKeys` 与 `rowCount`。

- [ ] **步骤 2：运行红灯测试**

  ```bash
  cd frontend
  npm test -- ServerDataTable.test.tsx
  ```

  预期：失败，原因是组件不存在。

- [ ] **步骤 3：实现组件**

  使用 Ant Design `Table`，复用现有 `useColumnManager` 或等价逻辑；不得前端全量分页。Props 至少包含：

  ```ts
  interface ServerDataTableProps<T> {
    viewKey: string;
    rowKey: keyof T | ((record: T) => React.Key);
    columns: Array<ExperienceColumn<T>>;
    query: ExperiencePageResponse<T>;
    request: ExperiencePageRequest;
    loading: boolean;
    error?: Error;
    partial?: ExperiencePartialResult;
    onRequestChange: (request: ExperiencePageRequest) => void;
    onOpenDetail: (record: T) => void;
    onViewSnapshotChange?: (snapshot: ExperienceViewSnapshot) => void;
    onSelectionSnapshotChange?: (snapshot: { selectedRowKeys: React.Key[]; rowCount: number }) => void;
  }
  ```

- [ ] **步骤 4：运行绿灯测试**

  ```bash
  cd frontend
  npm test -- ServerDataTable.test.tsx visualDebtGuard.test.ts
  ```

  预期：全部通过。

- [ ] **步骤 5：提交**

  ```bash
  git add frontend/src/shared/ui/ServerDataTable.tsx frontend/src/shared/ui/ServerDataTable.test.tsx frontend/src/app/index.css
  git commit -m "feat(GA-ENG-BASE-08): 增加服务端分页表格"
  ```

### 任务 8：字典映射只读核查样板页

**文件：**
- 修改：`frontend/src/pages/tenant/TerminologyMapping.tsx`
- 新增：`frontend/src/pages/tenant/TerminologyMapping.test.tsx`
- 修改：`frontend/src/shared/api/hooks.ts`（只在类型或查询参数不足时修改）

- [ ] **步骤 1：写失败测试，覆盖正常态、默认筛选、分页、详情和保存视图**

  用 `vi.mock("@/shared/api/hooks")` 模拟 `useTerminologyMappings`，断言：
  - 页面展示主目标“核查院内码与标准码的映射关系”。
  - 默认筛选只有“映射状态 / 来源系统 / 关键词”。
  - 默认排序请求为 `updatedAt,desc`。
  - 表格默认列不超过 8 个。
  - 分页变化调用 hook 参数更新。
  - 点击“查看”打开详情抽屉。
  - 普通模式不展示 `traceId`，专家模式展示。
  - 保存视图后 `medkernel.view.terminology.mapping` 包含筛选、排序、列配置和 `expertMode`，重新进入页面能恢复专家模式状态，且不包含敏感字段。
  - 导出按钮为禁用态并说明接口未接入。
  - 页面按钮、链接、菜单中不出现“导入医院字典”“确认映射”“提交审核”“发布”“回滚”“批量处理”等写入型入口；允许状态筛选中出现“已确认”“已回滚”等只读状态文本。

- [ ] **步骤 2：写失败测试，覆盖六态**

  同一测试文件继续覆盖：
  - `isLoading=true` 显示加载态。
  - `items=[]` 显示空状态。
  - `isError=true` 显示错误和重试。
  - 无权限场景显示 `PageState state="forbidden"` 或等价无权限文案。
  - `partial` 显示成功数、失败数、失败明细和重试入口。
  - 正常态显示表格和详情入口。

- [ ] **步骤 3：运行红灯测试**

  ```bash
  cd frontend
  npm test -- TerminologyMapping.test.tsx
  ```

  预期：失败，原因是页面尚未接公共底座。

- [ ] **步骤 4：改造页面**

  实现要点：
  - 从 `findRouteByPath("/terminology/mapping")` 读取体验声明。
  - 从 `useSecurityProfile` 或上层权限画像向 `PageExperienceShell` 传入 `securityProfile`；未取得权限画像时不展示专家入口。
  - 用 `PageExperienceShell` 替代直接使用 `PageShell`。
  - 用 `ExperienceFilterBar` 管理 `status`、`sourceSystem`、`keyword`。
  - 默认排序固定传 `updatedAt,desc`；如后端暂不支持，仍保留参数传递并在 PR 未完成事项中说明。
  - 用 `normalizePageResponse` 转换当前 `PageResponse`。
  - 用 `ServerDataTable` 渲染默认列、保存列配置、选择态和 `expertMode` 视图快照。
  - 用 `EvidenceDetailDrawer` 展示详情。
  - 用 `AsyncExportAction` 展示禁用态，不伪造任务。
  - 不实现确认、发布、回滚、批量处理或业务 mock。

- [ ] **步骤 5：运行页面绿灯测试**

  ```bash
  cd frontend
  npm test -- TerminologyMapping.test.tsx pages.smoke.test.tsx client.test.ts
  ```

  预期：全部通过；仍调用 `/engine/terminology/mappings`，不新增 `/terminology/mappings` 业务包装路径。

- [ ] **步骤 6：提交**

  ```bash
  git add frontend/src/pages/tenant/TerminologyMapping.tsx frontend/src/pages/tenant/TerminologyMapping.test.tsx frontend/src/shared/api/hooks.ts
  git commit -m "feat(GA-ENG-BASE-08): 接入字典映射体验样板页"
  ```

### 任务 9：内外网门禁、文档、台账与完整验证

**文件：**
- 修改：`frontend/src/test/visualDebtGuard.test.ts`
- 修改：`frontend/README.md`
- 修改：`docs/backlog.md`
- 修改：`docs/superpowers/plans/2026-05-26-base-08-product-experience-foundation.md`

- [ ] **步骤 1：写内网 DB-only / 无外部依赖门禁测试**

  在 `visualDebtGuard.test.ts` 增加静态检查：
  - 新增 `shared/ui` 和 `pages/tenant/TerminologyMapping.tsx` 不得直接调用 `fetch("http`、`axios.create` 或硬编码外部 URL。
  - 只允许通过 `apiClient` 或传入的 props 与后端交互。
  - 生产代码不得引用 Dify、模型 provider、图数据库 URL 作为前端直连目标。

- [ ] **步骤 2：写外网 SaaS / 后端开关门禁测试**

  同一文件增加检查：
  - 组件不得根据前端环境变量自行启停外部能力。
  - 导出、模型、Dify、图投影状态只展示后端返回或受控禁用态。
  - `medkernel.view.` 快照不得包含 `token`、`patient`、`idcard`、`identity`、`身份证`、`患者` 等敏感内容。

- [ ] **步骤 3：运行红灯门禁测试**

  ```bash
  cd frontend
  npm test -- visualDebtGuard.test.ts
  ```

  预期：若门禁尚未实现则失败；若现有代码天然满足，至少新增断言应被执行。

- [ ] **步骤 4：实现最小门禁修正**

  如发现误伤，调整正则或允许列表；不得降低现有 inline style、浏览器存储和 `console` 门禁。

- [ ] **步骤 5：更新前端 README**

  在 `frontend/README.md` 增加“产品体验底座接入规则”，写明：
  - 任何有菜单入口的认证路由必须配置 `experience`。
  - 大列表必须使用 `ServerDataTable` 或等价服务端分页。
  - 详情进入 `EvidenceDetailDrawer`。
  - 异步导出必须使用 `AsyncExportAction`，暂无接口时禁用。
  - 样板页为“字典映射”，且仅只读核查。

- [ ] **步骤 6：更新 backlog**

  在 `docs/backlog.md`：
  - 将 `GA-ENG-BASE-08` owner 改为 `codex`，status 改为 `done`。
  - 增加修订记录，说明公共体验声明、组件底座、字典映射样板页、内外网门禁和验证命令。

- [ ] **步骤 7：运行完整前端验证**

  ```bash
  cd frontend
  npm run lint
  npm run format:check
  npm run typecheck
  npm test
  npm run build
  ```

  预期：全部通过。若格式检查失败，运行 `npm run format` 后重新执行 `npm run format:check`。

- [ ] **步骤 8：运行全仓检查**

  ```bash
  git diff --check
  git status --short
  ```

  预期：无空白错误；只出现本任务相关修改。

- [ ] **步骤 9：提交**

  ```bash
  git add frontend/src/test/visualDebtGuard.test.ts frontend/README.md docs/backlog.md docs/superpowers/plans/2026-05-26-base-08-product-experience-foundation.md
  git commit -m "docs(GA-ENG-BASE-08): 完成产品体验底座验收记录"
  ```

### 任务 10：PR 与远程主干交付

**文件：**
- 不新增文件；只处理 git 与 GitHub。

- [ ] **步骤 1：确认分支与提交**

  ```bash
  git status --short --branch
  git log --oneline origin/main..HEAD
  ```

  预期：在 `codex/ga-eng-base-08-product-experience`，工作区干净，提交只包含本任务。

- [ ] **步骤 2：推送分支**

  ```bash
  git push -u origin codex/ga-eng-base-08-product-experience
  ```

- [ ] **步骤 3：创建中文 PR**

  PR 描述必须包含：
  - 变更范围：公共体验声明、公共组件、字典映射样板页、文档台账。
  - 验证结果：列出 `npm run lint`、`format:check`、`typecheck`、`npm test`、`npm run build`。
  - 未完成事项：其它页面后续分批迁移；字典映射确认、发布、回滚不在本任务中。
  - 医疗安全影响：不新增医疗建议、不实现自动确认、不写入病历，前端只做只读核查样板。
  - 部署与数据迁移：不改数据库迁移，不新增后端接口，内外网均通过 `apiClient` 调后端。

- [ ] **步骤 4：等待远端检查并合并**

  等待 CI 全部通过后，通过 PR 合并到远程 `main`。禁止直接推送远程 `main`。

- [ ] **步骤 5：确认远程 main**

  ```bash
  git fetch origin
  git log --oneline -1 origin/main
  ```

  预期：`origin/main` 包含 PR 合并提交。

---

## 实施注意事项

1. 本任务只实现“字典映射只读核查样板页”，不得增加确认、发布、回滚、批量处理或业务 mock。
2. 默认排序在实施中固定为 `updatedAt,desc`；如后端不支持该字段，必须在实现中保留参数传递并在 PR 未完成事项中说明后端排序字段待统一。
3. 表格选择态与导出范围必须通过 `ExperienceViewSnapshot` 和 `selectionSnapshot` 表达，不能靠当前 DOM 状态推断。
4. 内网 DB-only、无模型、无 Dify、无图投影时，字典映射样板页仍可加载、筛选、分页、打开详情和保存视图。
5. 外网 SaaS 形态下，外部能力状态只以后端开关、授权和审计结果呈现，前端不得直连第三方服务。
