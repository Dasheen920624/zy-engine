/* eslint-disable medkernel/no-page-mock */
import { Dropdown, Button, Tooltip } from "antd";
import { BgColorsOutlined } from "@ant-design/icons";
import type { MenuProps } from "antd";
import { useThemeStore } from "@/shared/lib/themeStore";

/**
 * 主题切换：默认 / 老年医生模式 / 暗黑 / 护眼 / 跟随系统。
 * 与产品宪法的设计 token 和产品体验固定规范保持一致。
 */
export function ThemeSwitcher({ compact = false }: { compact?: boolean }) {
  const { mode, setMode } = useThemeStore();

  const items: MenuProps["items"] = [
    { key: "default", label: "默认（医蓝 14px）" },
    { key: "elder", label: "老年医生模式（≥ 16pt + 大按钮 + 高对比）" },
    { key: "dark", label: "暗黑模式" },
    { key: "eye", label: "护眼模式（米色背景）" },
    { key: "system", label: "跟随系统" },
  ];

  const labelMap: Record<string, string> = {
    default: "默认",
    elder: "老年医生",
    dark: "暗黑",
    eye: "护眼",
    system: "跟随系统",
  };

  return (
    <Dropdown
      menu={{
        items,
        selectable: true,
        selectedKeys: [mode],
        onClick: (info) => setMode(info.key as never),
      }}
      placement="bottomRight"
    >
      <Tooltip title="主题模式">
        <Button type="text" icon={<BgColorsOutlined />}>
          {compact ? null : labelMap[mode]}
        </Button>
      </Tooltip>
    </Dropdown>
  );
}
