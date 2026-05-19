import { BgColorsOutlined, ReloadOutlined } from "@ant-design/icons";
import { Button, ColorPicker, Divider, Input, Popover, Select, Space, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";
import { CUSTOM_THEME_ID, normalizeHexColor, type CustomThemeSeed, type ThemeId } from "./tokens";
import { useTheme } from "./themeContext";

const { Text } = Typography;

export default function ThemeSelector() {
  const {
    themeId,
    theme,
    customTheme,
    themeOptions,
    setThemeId,
    updateCustomTheme,
    resetCustomTheme,
  } = useTheme();

  const customActive = themeId === CUSTOM_THEME_ID;
  const title = useMemo(() => `主题：${theme.label}`, [theme.label]);

  return (
    <Popover
      trigger="click"
      placement="bottomRight"
      title={title}
      content={
        <div className="mk-theme-panel">
          <Select
            aria-label="主题"
            value={themeId}
            options={themeOptions}
            onChange={(value: ThemeId) => setThemeId(value)}
            className="mk-theme-select"
          />

          <Divider className="mk-theme-divider" />

          <Space direction="vertical" size={10} className="mk-theme-controls">
            <ThemeColorControl
              label="主色"
              value={customTheme.primary}
              disabled={!customActive}
              onChange={(primary) => updateCustomTheme({ primary })}
            />
            <ThemeColorControl
              label="菜单"
              value={customTheme.menu}
              disabled={!customActive}
              onChange={(menu) => updateCustomTheme({ menu })}
            />
          </Space>

          <div className="mk-theme-actions">
            <Button size="small" icon={<ReloadOutlined />} onClick={resetCustomTheme}>
              重置
            </Button>
          </div>
        </div>
      }
    >
      <Button aria-label="主题" icon={<BgColorsOutlined />} />
    </Popover>
  );
}

function ThemeColorControl({
  label,
  value,
  disabled,
  onChange,
}: {
  label: string;
  value: CustomThemeSeed[keyof CustomThemeSeed];
  disabled: boolean;
  onChange: (value: string) => void;
}) {
  return (
    <div className="mk-theme-color-row">
      <Text type={disabled ? "secondary" : undefined}>{label}</Text>
      <ThemeHexInput
        ariaLabel={`${label}十六进制颜色`}
        value={value}
        disabled={disabled}
        onChange={onChange}
      />
      <ColorPicker
        value={value}
        disabled={disabled}
        disabledAlpha
        showText
        onChange={(_, hex) => onChange(hex)}
      />
    </div>
  );
}

function ThemeHexInput({
  ariaLabel,
  value,
  disabled,
  onChange,
}: {
  ariaLabel: string;
  value: string;
  disabled: boolean;
  onChange: (value: string) => void;
}) {
  const [draft, setDraft] = useState(formatHex(value));
  const normalizedDraft = toCompleteHex(draft);
  const invalid = !disabled && draft.trim().length > 0 && !normalizedDraft;

  useEffect(() => {
    setDraft(formatHex(value));
  }, [value]);

  function commit(nextDraft = draft) {
    const hex = toCompleteHex(nextDraft);
    if (hex) {
      onChange(hex);
      setDraft(formatHex(hex));
      return;
    }
    setDraft(formatHex(value));
  }

  return (
    <Input
      aria-label={ariaLabel}
      className="mk-theme-color-input"
      value={draft}
      disabled={disabled}
      status={invalid ? "error" : undefined}
      maxLength={7}
      onBlur={() => commit()}
      onChange={(event) => {
        const nextDraft = event.target.value;
        setDraft(nextDraft);
        const hex = toCompleteHex(nextDraft);
        if (hex) {
          onChange(hex);
        }
      }}
      onPressEnter={() => commit()}
      placeholder={formatHex(value)}
      size="small"
    />
  );
}

function toCompleteHex(value: string): string | null {
  const trimmed = value.trim();
  const candidate = trimmed.startsWith("#") ? trimmed : `#${trimmed}`;
  if (!/^#[0-9a-fA-F]{6}$/.test(candidate)) {
    return null;
  }
  return normalizeHexColor(candidate, "").toUpperCase();
}

function formatHex(value: string): string {
  return normalizeHexColor(value, value).toUpperCase();
}
