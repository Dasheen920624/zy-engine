/**
 * 规则 DSL 编辑器主容器（路由 /rule/definitions/:code/edit / new）。
 *
 * 子组件：
 *   - EditorHeader：标题 + 校验状态 + 保存/发布按钮
 *   - DslEditor：JSON DSL 编辑器
 *   - DryRunPanel：试运行（场景 + facts + simulate）
 */

import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Result, Spin, message } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getRule, importRules, publishRule, type RuleDefinition } from "../../../api/rule";
import { defaultDslTemplate } from "../helpers/ruleSamples";
import { safeParseJson, type DslValidationError } from "../helpers/ruleSchema";
import { stringifyDsl } from "../helpers/ruleFormatters";
import EditorHeader from "./EditorHeader";
import DslEditor from "./DslEditor";
import DryRunPanel from "./DryRunPanel";
import styles from "../styles.module.css";

export default function RuleEditor() {
  const params = useParams<{ code?: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isNew = !params.code || params.code === "new";
  const ruleCode = isNew ? undefined : params.code;

  const { data, isLoading, isError } = useQuery({
    queryKey: ["rule", ruleCode],
    queryFn: () => (ruleCode ? getRule(ruleCode) : Promise.resolve(null)),
    enabled: !isNew,
  });

  const initialDslText = useMemo(() => {
    if (isNew) return stringifyDsl(defaultDslTemplate());
    if (data?.rule_json) return stringifyDsl(data.rule_json);
    return "";
  }, [isNew, data]);

  const [dslText, setDslText] = useState<string>("");
  const [dirty, setDirty] = useState(false);
  const [validation, setValidation] = useState<{ valid: boolean; errors: DslValidationError[] }>({
    valid: true,
    errors: [],
  });

  useEffect(() => {
    if (initialDslText && dslText === "") {
      setDslText(initialDslText);
    }
  }, [initialDslText, dslText]);

  const handleChange = (next: string) => {
    setDslText(next);
    setDirty(next !== initialDslText);
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      const parsed = safeParseJson(dslText);
      if (!parsed.ok) throw new Error(parsed.error);
      const dsl = parsed.value as Record<string, unknown>;
      const ruleDef: Partial<RuleDefinition> = {
        rule_code: String(dsl.rule_code ?? data?.rule_code ?? "DEMO_RULE"),
        rule_name: String(dsl.rule_name ?? "示例规则"),
        rule_type: (dsl.rule_type as RuleDefinition["rule_type"]) ?? "TIME_LIMIT_QC",
        version_no: String(dsl.version ?? data?.version_no ?? "1.0.0"),
        severity: data?.severity ?? "MEDIUM",
        status: "DRAFT",
        enabled: data?.enabled ?? true,
        rule_json: dsl,
      };
      return importRules(ruleDef as RuleDefinition);
    },
    onSuccess: (rules) => {
      message.success("草稿已保存");
      setDirty(false);
      queryClient.invalidateQueries({ queryKey: ["rule"] });
      queryClient.invalidateQueries({ queryKey: ["rules"] });
      const saved = rules[0];
      if (isNew && saved?.rule_code) {
        navigate(`/rule/definitions/${encodeURIComponent(saved.rule_code)}/edit`, { replace: true });
      }
    },
    onError: (err: Error) => {
      message.error(`保存失败：${err.message}`);
    },
  });

  const publishMutation = useMutation({
    mutationFn: async () => {
      if (!ruleCode) throw new Error("新规则请先保存草稿再发布");
      return publishRule(ruleCode, { reviewer_comment: "经 DSL 编辑器发布" });
    },
    onSuccess: () => {
      message.success("规则已发布");
      queryClient.invalidateQueries({ queryKey: ["rule"] });
      queryClient.invalidateQueries({ queryKey: ["rules"] });
    },
    onError: (err: Error) => {
      message.error(`发布失败：${err.message}`);
    },
  });

  if (!isNew && isLoading) {
    return (
      <div className={styles.page}>
        <Spin tip="加载规则中..." />
      </div>
    );
  }

  if (!isNew && isError) {
    return (
      <Result
        status="error"
        title="无法加载规则"
        subTitle={`规则编码 ${params.code} 不存在或网络异常`}
        extra={<a onClick={() => navigate("/rule/definitions")}>返回规则库</a>}
      />
    );
  }

  return (
    <div className={styles.page}>
      <EditorHeader
        title={isNew ? "新建规则" : `编辑：${data?.rule_name ?? params.code}`}
        valid={validation.valid}
        dirty={dirty}
        saving={saveMutation.isPending}
        publishing={publishMutation.isPending}
        onSave={() => saveMutation.mutate()}
        onPublish={() => publishMutation.mutate()}
      />
      <div className={styles.editorLayout}>
        <DslEditor
          value={dslText}
          onChange={handleChange}
          onValidationChange={setValidation}
        />
        <DryRunPanel ruleCode={ruleCode} ruleDslText={dslText} />
      </div>
    </div>
  );
}
