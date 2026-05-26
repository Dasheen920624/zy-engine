import { useEffect, useState, useMemo } from "react";
import { Modal, Input, List, Typography, Tag } from "antd";
import { useNavigate } from "react-router-dom";
import { menuSections } from "@/shared/config/menu";

/**
 * 全局命令面板。
 *
 * 极致易用，对老 IT 友好 — 任何人 1 秒打开 → 输入关键词 → Enter 跳菜单。
 * 后续会扩展到：搜患者 / 搜配置包 / 搜规则 / 搜审计快照。
 */

interface CommandPaletteProps {
  open: boolean;
  onClose: () => void;
}

interface CommandItem {
  key: string;
  label: string;
  group: string;
  path: string;
}

export function CommandPalette({ open, onClose }: CommandPaletteProps) {
  const [q, setQ] = useState("");
  const navigate = useNavigate();

  const allCommands: CommandItem[] = useMemo(
    () =>
      menuSections.flatMap((s) =>
        s.items.map((it) => ({
          key: it.key,
          label: it.label,
          group: s.label,
          path: it.path,
        })),
      ),
    [],
  );

  const filtered = useMemo(() => {
    const lc = q.trim().toLowerCase();
    if (!lc) return allCommands.slice(0, 12);
    return allCommands.filter(
      (c) => c.label.toLowerCase().includes(lc) || c.group.toLowerCase().includes(lc),
    );
  }, [q, allCommands]);

  useEffect(() => {
    if (!open) setQ("");
  }, [open]);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        // 外层用 prop 控制 open；这里只做 dev hint
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      title="命令面板（⌘K / Ctrl+K）"
      width={680}
      destroyOnClose
    >
      <Input.Search
        placeholder="搜菜单 / 患者 / 配置包 / 规则 / 审计快照"
        value={q}
        onChange={(e) => setQ(e.target.value)}
        autoFocus
      />
      <List
        size="small"
        className="mk-command-results"
        dataSource={filtered}
        locale={{ emptyText: "无匹配" }}
        renderItem={(item) => (
          <List.Item
            className="mk-clickable"
            onClick={() => {
              navigate(item.path);
              onClose();
            }}
          >
            <Tag color="default">{item.group}</Tag>
            <Typography.Text>{item.label}</Typography.Text>
            <Typography.Text type="secondary" className="mk-push-inline-start-auto mk-text-xs">
              {item.path}
            </Typography.Text>
          </List.Item>
        )}
      />
    </Modal>
  );
}
