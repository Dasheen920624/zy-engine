import { Button, Tooltip, message } from "antd";
import { ExportOutlined } from "@ant-design/icons";
import { useLocation } from "react-router-dom";

/**
 * 全局“导出审计快照”按钮（与 docs/CONSTITUTION.md §5 第 7 角色对齐）。
 *
 * 任意页面右上角统一存在。点击后导出包含：
 * - 当前页面路径 + traceId + 时间戳
 * - 当前用户身份 + 角色 + 权限指纹
 * - 当前页面查询条件 + 列状态
 * - 后端响应 + 业务上下文
 *
 * 等保 2.0 三级 + 个保法审计留痕硬约束。
 *
 * 当前实装级别：骨架 + 本地下载 mock 文件。GA-COMPLIANCE-01 业务域实装时接后端 API。
 */
export function AuditSnapshotButton() {
  const location = useLocation();

  function handleClick() {
    const snapshot = {
      product: "MedKernel",
      version: "1.0.0-SNAPSHOT",
      timestamp: new Date().toISOString(),
      path: location.pathname,
      traceId: crypto.randomUUID(),
      user: {
        id: "u-demo-001",
        name: "医务处 · 张三",
        role: "MEDICAL_AFFAIRS",
      },
      note: "GA-COMPLIANCE-01 实装后此字段将由后端填充完整审计上下文",
    };
    const blob = new Blob([JSON.stringify(snapshot, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `medkernel-audit-${snapshot.timestamp.replace(/[:.]/g, "-")}.json`;
    a.click();
    URL.revokeObjectURL(url);
    message.success("审计快照已导出");
  }

  return (
    <Tooltip title="导出当前页审计快照（含路径 / traceId / 用户 / 上下文）">
      <Button type="text" icon={<ExportOutlined />} onClick={handleClick}>
        审计快照
      </Button>
    </Tooltip>
  );
}
