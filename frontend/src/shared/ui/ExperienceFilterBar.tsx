import { SaveOutlined } from "@ant-design/icons";
import { Button, DatePicker, Input, Select, Space } from "antd";
import type { ReactNode } from "react";

import type { ExperienceFilterDefinition, ExperienceFilterValue } from "./experienceTypes";

interface ExperienceFilterBarProps {
  filters: ExperienceFilterDefinition[];
  value: ExperienceFilterValue[];
  advanced?: ReactNode;
  renderFilter?: (filter: ExperienceFilterDefinition) => ReactNode;
  onChange: (next: ExperienceFilterValue[]) => void;
  onSaveView?: () => void;
}

export function ExperienceFilterBar({
  filters,
  value,
  advanced,
  renderFilter,
  onChange,
  onSaveView,
}: ExperienceFilterBarProps) {
  if (filters.length > 3) {
    throw new Error("默认筛选最多 3 个，其余筛选应进入高级筛选");
  }

  filters.forEach((filter) => {
    if (filter.kind === "select" && !filter.options && !filter.apiPath && !renderFilter) {
      throw new Error(`${filter.label}缺少受控选项来源`);
    }
  });

  function currentValue(key: string) {
    return value.find((entry) => entry.key === key)?.value;
  }

  function updateValue(key: string, nextValue: ExperienceFilterValue["value"]) {
    const remaining = value.filter((entry) => entry.key !== key);
    onChange(nextValue ? [...remaining, { key, value: nextValue }] : remaining);
  }

  function renderDefaultFilter(filter: ExperienceFilterDefinition) {
    const storedValue = currentValue(filter.key);

    if (filter.kind === "select") {
      return (
        <Select
          key={filter.key}
          aria-label={filter.label}
          placeholder={filter.placeholder ?? `请选择${filter.label}`}
          value={typeof storedValue === "string" ? storedValue : undefined}
          options={filter.options}
          allowClear
          onChange={(nextValue) => updateValue(filter.key, nextValue)}
          className="mk-search-sm"
        />
      );
    }

    if (filter.kind === "dateRange") {
      return (
        <DatePicker.RangePicker
          key={filter.key}
          aria-label={filter.label}
          placeholder={["开始日期", "结束日期"]}
          onChange={(_, dates) =>
            updateValue(filter.key, dates[0] && dates[1] ? [dates[0], dates[1]] : undefined)
          }
        />
      );
    }

    return (
      <Input.Search
        key={filter.key}
        aria-label={filter.label}
        placeholder={filter.placeholder ?? `请输入${filter.label}`}
        value={typeof storedValue === "string" ? storedValue : undefined}
        allowClear
        onChange={(event) => updateValue(filter.key, event.target.value || undefined)}
        className="mk-search-sm"
      />
    );
  }

  return (
    <Space wrap className="mk-filter-row">
      {filters.map((filter) => renderFilter?.(filter) ?? renderDefaultFilter(filter))}
      {advanced}
      {onSaveView && (
        <Button aria-label="保存视图" icon={<SaveOutlined />} onClick={onSaveView}>
          保存视图
        </Button>
      )}
    </Space>
  );
}
