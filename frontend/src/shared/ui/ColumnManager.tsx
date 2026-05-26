import { useEffect, useState } from "react";
import { Button, Dropdown, Checkbox, Space, Divider, Input, message } from "antd";
import { SettingOutlined, SaveOutlined } from "@ant-design/icons";
import type { MenuProps } from "antd";
import { readUiPreference, writeUiPreference } from "@/shared/lib/browserStorage";

/**
 * 列管理 + 视图保存。
 *
 * 任何 Antd Table 接入此组件后获得：
 * - 显示/隐藏列（动态控制）
 * - 视图保存到受控 UI 偏好存储（按 viewKey 命名空间隔离）
 * - 视图分享（拷贝 URL 含 viewName）
 *
 * 使用方式：
 *   const { visibleColumns, columnManager } = useColumnManager(viewKey, allColumns);
 *   <Table columns={visibleColumns} ... />
 *   <PageShell extras={columnManager}>
 */
interface ColumnDef {
  key: string;
  title: string;
  always?: boolean; // 必显示，不允许隐藏
}

const STORAGE_PREFIX = "medkernel.view.";

export function useColumnManager<C extends ColumnDef>(viewKey: string, allColumns: C[]) {
  const storageKey = STORAGE_PREFIX + viewKey;

  const defaultVisible = allColumns.map((c) => c.key);
  const [visible, setVisible] = useState<string[]>(() => {
    if (typeof window === "undefined") return defaultVisible;
    const saved = readUiPreference(storageKey);
    if (!saved) return defaultVisible;
    try {
      const parsed = JSON.parse(saved) as { visible?: string[] };
      return parsed.visible ?? defaultVisible;
    } catch {
      return defaultVisible;
    }
  });
  const [viewName, setViewName] = useState("");

  useEffect(() => {
    writeUiPreference(storageKey, JSON.stringify({ visible }));
  }, [storageKey, visible]);

  const visibleColumns = allColumns.filter((c) => visible.includes(c.key) || c.always);

  function toggle(key: string) {
    setVisible((curr) => (curr.includes(key) ? curr.filter((k) => k !== key) : [...curr, key]));
  }

  function shareView() {
    const url = `${window.location.origin}${window.location.pathname}?view=${viewKey}`;
    void navigator.clipboard.writeText(url);
    message.success("视图链接已复制到剪贴板");
  }

  const items: MenuProps["items"] = [
    {
      key: "columns",
      label: (
        <Space direction="vertical" style={{ width: 200 }}>
          {allColumns.map((c) => (
            <Checkbox
              key={c.key}
              checked={visible.includes(c.key) || !!c.always}
              disabled={!!c.always}
              onChange={() => toggle(c.key)}
            >
              {c.title}
            </Checkbox>
          ))}
          <Divider style={{ margin: "8px 0" }} />
          <Input
            placeholder="视图名（可选）"
            value={viewName}
            onChange={(e) => setViewName(e.target.value)}
            size="small"
          />
          <Button size="small" type="primary" icon={<SaveOutlined />} block onClick={shareView}>
            保存并分享
          </Button>
        </Space>
      ),
    },
  ];

  const columnManager = (
    <Dropdown menu={{ items }} placement="bottomRight" trigger={["click"]}>
      <Button icon={<SettingOutlined />}>列管理</Button>
    </Dropdown>
  );

  return { visibleColumns, columnManager };
}
