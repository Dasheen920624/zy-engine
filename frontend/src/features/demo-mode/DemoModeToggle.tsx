import { Tag, Tooltip } from "antd";
import { ExperimentOutlined } from "@ant-design/icons";
import { create } from "zustand";

/**
 * 客户演示模式。
 *
 * 一键塞入 fixture + 屏幕隐藏调试信息 + 路演水印。
 * 销售刚需 — 跑 6 大客户验收剧本时使用。
 *
 * 当前实装：骨架（toggle store + chip）。
 * GA-QA-03 实装时接 fixture 注入 + 隐藏 traceId / Provider 细节 / DSL 等技术信息。
 */

interface DemoState {
  active: boolean;
  toggle: () => void;
}

export const useDemoMode = create<DemoState>((set) => ({
  active: false,
  toggle: () => set((s) => ({ active: !s.active })),
}));

export function DemoModeToggle() {
  const { active, toggle } = useDemoMode();
  return (
    <Tooltip title="客户演示模式：fixture + 隐藏技术信息 + 水印">
      <Tag
        icon={<ExperimentOutlined />}
        color={active ? "magenta" : "default"}
        className="mk-clickable"
        onClick={toggle}
      >
        {active ? "演示中" : "演示模式"}
      </Tag>
    </Tooltip>
  );
}
