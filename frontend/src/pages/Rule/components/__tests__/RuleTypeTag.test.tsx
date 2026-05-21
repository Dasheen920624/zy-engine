import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import RuleTypeTag from "../RuleTypeTag";

describe("RuleTypeTag", () => {
  it("renders Chinese label for known types", () => {
    render(<RuleTypeTag ruleType="SAFETY" />);
    expect(screen.getByText("安全规则")).toBeInTheDocument();
  });

  it("renders 未分类 when no ruleType", () => {
    render(<RuleTypeTag ruleType={undefined} />);
    expect(screen.getByText("未分类")).toBeInTheDocument();
  });
});
