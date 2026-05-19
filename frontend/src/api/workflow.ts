import { get, post } from "./client";
import type {
  TodoTask,
  TodoTaskDetail,
  TodoSummary,
  ApprovalRequest,
  DelegateRequest,
  AddSignRequest,
  CancelRequest,
  BusinessType,
  TodoStatus,
  TodoPriority,
} from "./types";

/** 查询待办任务列表 */
export async function fetchTodoTasks(params?: {
  status?: TodoStatus;
  businessType?: BusinessType;
  assignedTo?: string;
  priority?: TodoPriority;
}): Promise<TodoTask[]> {
  const query = new URLSearchParams();
  if (params?.status) query.set("status", params.status);
  if (params?.businessType) query.set("businessType", params.businessType);
  if (params?.assignedTo) query.set("assignedTo", params.assignedTo);
  if (params?.priority) query.set("priority", params.priority);
  const qs = query.toString();
  return get<TodoTask[]>(`/workflow/todos${qs ? `?${qs}` : ""}`);
}

/** 获取待办任务详情 */
export async function fetchTodoDetail(taskCode: string): Promise<TodoTaskDetail> {
  return get<TodoTaskDetail>(`/workflow/todos/${taskCode}`);
}

/** 获取待办统计 */
export async function fetchTodoSummary(params?: {
  assignedTo?: string;
}): Promise<TodoSummary> {
  const query = new URLSearchParams();
  if (params?.assignedTo) query.set("assignedTo", params.assignedTo);
  const qs = query.toString();
  return get<TodoSummary>(`/workflow/todos/summary${qs ? `?${qs}` : ""}`);
}

/** 创建待办任务 */
export async function createTodoTask(body: {
  businessType: BusinessType;
  businessCode: string;
  businessVersion?: string;
  title: string;
  description?: string;
  priority?: TodoPriority;
  assignedType?: string;
  assignedTo?: string;
  dueTime?: string;
}): Promise<TodoTask> {
  return post<TodoTask>("/workflow/todos", body);
}

/** 审批通过 */
export async function approveTask(
  taskCode: string,
  body: ApprovalRequest,
): Promise<TodoTask> {
  return post<TodoTask>(`/workflow/todos/${taskCode}/approve`, body);
}

/** 驳回 */
export async function rejectTask(
  taskCode: string,
  body: ApprovalRequest,
): Promise<TodoTask> {
  return post<TodoTask>(`/workflow/todos/${taskCode}/reject`, body);
}

/** 转办 */
export async function delegateTask(
  taskCode: string,
  body: DelegateRequest,
): Promise<TodoTask> {
  return post<TodoTask>(`/workflow/todos/${taskCode}/delegate`, body);
}

/** 取消 */
export async function cancelTask(
  taskCode: string,
  body: CancelRequest,
): Promise<TodoTask> {
  return post<TodoTask>(`/workflow/todos/${taskCode}/cancel`, body);
}

/** 加签 */
export async function addSignTask(
  taskCode: string,
  body: AddSignRequest,
): Promise<TodoTask> {
  return post<TodoTask>(`/workflow/todos/${taskCode}/add-sign`, body);
}
