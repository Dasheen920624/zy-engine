/**
 * 试运行面板：选场景 → 注入 facts → 调用 /api/rules/simulate → 渲染命中结果。
 *
 * 国情样本走 helpers/ruleSamples.ts（AMI / EMR-QC / 医保 / 红线 / 路径准入），
 * 用户也可自由编辑 facts JSON。
 */

import { useEffect, useState } from "react";
import { Button, Card, Form, Input, Select, Space, Typography, message } from "antd";
import { ExperimentOutlined } from "@ant-design/icons";
import { useMutation } from "@tanstack/react-query";
import { simulateRule } from "../../../api/rule";
import type { ScenarioCode } from "../../../api/types";
import type { HitItem } from "../../../api/types";
import { SAMPLE_SCENARIOS, findSampleScenario } from "../helpers/ruleSamples";
import { safeParseJson } from "../helpers/ruleSchema";
import { describeSeverity } from "../helpers/ruleFormatters";
import SeverityTag from "../components/SeverityTag";
import styles from "../styles.module.css";

export interface DryRunPanelProps {
  ruleCode?: string;
  ruleDslText?: string;
}

const { Text } = Typography;

export default function DryRunPanel({ ruleCode, ruleDslText }: DryRunPanelProps) {
  const [scenarioCode, setScenarioCode] = useState<ScenarioCode>("AMI_RECOMMEND");
  const [factsText, setFactsText] = useState<string>("");
  const [lastResult, setLastResult] = useState<HitItem | null>(null);
  const [factsParseError, setFactsParseError] = useState<string | null>(null);

  useEffect(() => {
    const sample = findSampleScenario(scenarioCode);
    if (sample) {
      setFactsText(JSON.stringify(sample.patientContext, null, 2));
    }
  }, [scenarioCode]);

  const mutation = useMutation({
    mutationFn: async () => {
      const parsed = safeParseJson(factsText);
      if (!parsed.ok) {
        setFactsParseError(parsed.error);
        throw new Error(parsed.error);
      }
      setFactsParseError(null);

      const patientContext = parsed.value as {
        patient: { patient_id: string };
        encounter: { encounter_id: string };
        facts: Record<string, unknown>;
      };

      const dslParsed = ruleDslText ? safeParseJson(ruleDslText) : { ok: false as const, error: "" };
      const rulePayload: Record<string, unknown> | undefined =
        dslParsed.ok && typeof dslParsed.value === "object" && dslParsed.value !== null
          ? (dslParsed.value as Record<string, unknown>)
          : undefined;

      return simulateRule({
        rule_code: ruleCode,
        rule_json: rulePayload,
        patient_context: patientContext,
      });
    },
    onSuccess: (data) => {
      setLastResult(data);
      message.success("试运行完成");
    },
    onError: (err: Error) => {
      message.error(`试运行失败：${err.message}`);
    },
  });

  const scenarioOptions = SAMPLE_SCENARIOS.map((s) => ({
    value: s.code,
    label: `${s.label} · ${s.code}`,
  }));

  return (
    <Card
      title={
        <Space>
          <ExperimentOutlined />
          <span>试运行</span>
        </Space>
      }
      className={styles.dryRunCard}
    >
      <Form layout="vertical">
        <Form.Item label="场景" className={styles.dryRunFormItem}>
          <Select
            value={scenarioCode}
            onChange={(v) => setScenarioCode(v as ScenarioCode)}
            options={scenarioOptions}
          />
          <Text type="secondary">
            {findSampleScenario(scenarioCode)?.description ?? "选择一个国情场景作为默认 facts。"}
          </Text>
        </Form.Item>
        <Form.Item
          label="患者上下文（patient + encounter + facts）"
          className={styles.dryRunFormItem}
          validateStatus={factsParseError ? "error" : ""}
          help={factsParseError ?? undefined}
        >
          <Input.TextArea
            value={factsText}
            onChange={(e) => setFactsText(e.target.value)}
            className={styles.factsTextarea}
            rows={10}
            spellCheck={false}
            aria-label="patient-context-json"
          />
        </Form.Item>
        <Button
          type="primary"
          icon={<ExperimentOutlined />}
          onClick={() => mutation.mutate()}
          loading={mutation.isPending}
          aria-label="trigger-dry-run"
        >
          运行
        </Button>
      </Form>

      {lastResult && (
        <>
          <div className={styles.dryRunSummary} role="status">
            <span className={styles.dryRunSummaryItem}>
              <span className={styles.dryRunSummaryLabel}>规则：</span>
              <span className={styles.dryRunSummaryValue}>
                {lastResult.rule_code} {lastResult.rule_name ? `· ${lastResult.rule_name}` : ""}
              </span>
            </span>
            <span className={styles.dryRunSummaryItem}>
              <span className={styles.dryRunSummaryLabel}>严重度：</span>
              <SeverityTag severity={lastResult.severity} />
              <Text type="secondary"> ({describeSeverity(lastResult.severity)})</Text>
            </span>
            <span className={styles.dryRunSummaryItem}>
              <span className={styles.dryRunSummaryLabel}>命中：</span>
              <span className={styles.dryRunSummaryValue}>{lastResult.hit ? "是" : "否"}</span>
            </span>
          </div>
          <div className={styles.dryRunMessage}>{lastResult.message ?? "（无消息）"}</div>
        </>
      )}
    </Card>
  );
}
