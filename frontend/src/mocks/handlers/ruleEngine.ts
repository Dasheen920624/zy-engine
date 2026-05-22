import { http, HttpResponse } from "msw";
import { wrap, baseURL } from "./shared";
import type { EvaluateResponse, RuleEngineResultSummary } from "../../api/types";

// ─── Mock 场景数据 (FE-003) ───────────────────────────────────────────

const mockScenarios: Record<string, EvaluateResponse> = {
  AMI_RECOMMEND: {
    result_id: "res-ami-001",
    scenario_code: "AMI_RECOMMEND",
    package_code: "PKG_AMI_CORE",
    package_version: "2026.05",
    evaluated_count: 8,
    hit_count: 2,
    elapsed_ms: 156,
    trace_id: "mock-ami-trace",
    hits: [
      {
        rule_code: "R_AMI_STEMI_CANDIDATE",
        rule_name: "AMI/STEMI 候选入径规则",
        rule_version: "1.0.0",
        package_code: "PKG_AMI_CORE",
        severity: "HIGH",
        action_type: "CREATE_RECOMMENDATION",
        message: "疑似 STEMI，请医生评估是否启动 AMI/STEMI 诊疗路径。",
        condition_summary:
          "chief_complaints.code IN [CHEST_PAIN] AND exams.finding_codes CONTAINS ST_ELEVATION_CONTIGUOUS_LEADS",
        facts_matched: {
          "chief_complaints.code": ["CHEST_PAIN"],
          "exams.finding_codes": ["ST_ELEVATION_CONTIGUOUS_LEADS"],
        },
        suggested_actions: [
          "启动 AMI/STEMI 诊疗路径",
          "12 导联心电图确认 ST 段抬高",
          "评估再灌注策略",
        ],
        source_document: {
          title: "急性 ST 段抬高型心肌梗死诊断和治疗指南",
          institution: "中华医学会心血管病学分会",
          version: "2025",
          section: "第三章 诊断标准",
          evidence_level: "GUIDELINE",
          reviewer: "QC_DR_WANG",
          summary: "持续性胸痛伴心电图相邻两个以上导联 ST 段抬高，应高度怀疑 STEMI。",
        },
      },
      {
        rule_code: "R_AMI_ECG_TIMELY",
        rule_name: "急诊心电图时限质控",
        severity: "MEDIUM",
        action_type: "REMIND",
        message: "患者到达急诊至完成首份心电图间隔超过 10 分钟。",
        condition_summary: "exams.ecg_elapsed_minutes > 10",
        source_document: {
          title: "胸痛中心建设与管理指导原则",
          institution: "国家卫健委",
          version: "2022",
          section: "第五条",
        },
      },
    ],
    org_source: "HEADER",
    tenant_id: "TENANT_DEMO",
    hospital_code: "HOSPITAL_DEMO",
    department_code: "DEPT_CARDIOLOGY",
    created_time: new Date().toISOString(),
  },

  EMR_QC: {
    result_id: "res-emr-001",
    scenario_code: "EMR_QC",
    package_code: "PKG_EMR_QC",
    package_version: "2026.05",
    evaluated_count: 12,
    hit_count: 4,
    elapsed_ms: 287,
    trace_id: "mock-emr-trace",
    hits: [
      {
        rule_code: "R_EMR_ADMISSION_RECORD_TIMELY",
        rule_name: "入院记录时限",
        severity: "HIGH",
        action_type: "STRONG_REMIND",
        message:
          "入院记录未在 24 小时内提交。首次书写时间 2026-05-15 10:30，超出入院后 24 小时窗口（要求 ≤ 2026-05-15 09:00）。",
        condition_summary: "emr.admission_record.submitted = false AND first_word_time > admit_time + 24h",
        facts_matched: {
          "emr.admission_record.submitted": false,
          "emr.admission_record.first_word_time": "2026-05-15T10:30:00+08:00",
        },
        suggested_actions: [
          "主管医师立即完成入院记录",
          "形成时限整改记录",
        ],
        source_document: {
          title: "病历书写基本规范（卫医政发〔2010〕11号）",
          institution: "国家卫健委",
          version: "2010",
          section: "第十条",
          evidence_level: "POLICY",
          reviewer: "QC_DR_LI",
          summary: "入院记录应当于患者入院后 24 小时内完成。",
        },
      },
      {
        rule_code: "R_EMR_CHIEF_COMPLAINT_MISSING",
        rule_name: "主诉字段缺失",
        severity: "HIGH",
        action_type: "REMIND",
        message: "主诉字段缺失。",
        condition_summary: "emr.chief_complaint.filled = false",
        source_document: {
          title: "病历书写基本规范",
          institution: "国家卫健委",
          section: "第八条 主诉",
          reviewer: "QC_DR_LI",
        },
      },
      {
        rule_code: "R_EMR_PRESENT_ILLNESS_TOO_SHORT",
        rule_name: "现病史字数不足",
        severity: "MEDIUM",
        action_type: "REMIND",
        message: "现病史字数过少（12 字 < 阈值 50 字）。",
        source_document: {
          title: "演示医院病历质控管理办法 v2026.03",
          institution: "演示医院质控办",
          section: "第 5 章 现病史",
        },
      },
      {
        rule_code: "R_EMR_PAST_HISTORY_MISSING",
        rule_name: "既往史空白",
        severity: "MEDIUM",
        action_type: "REMIND",
        message: "既往史空白。",
        source_document: {
          title: "病历书写基本规范（卫医政发〔2010〕11号）",
          institution: "国家卫健委",
          version: "2010",
          section: "第十八条 既往史",
          evidence_level: "POLICY",
          reviewer: "QC_DR_LI",
        },
      },
    ],
    org_source: "BODY",
    tenant_id: "TENANT_DEMO",
    hospital_code: "HOSPITAL_DEMO",
    department_code: "DEPT_CARDIOLOGY",
    created_time: new Date().toISOString(),
  },

  INSURANCE_QC: {
    result_id: "res-ins-001",
    scenario_code: "INSURANCE_QC",
    package_code: "PKG_INSURANCE_QC",
    package_version: "2026.05",
    evaluated_count: 6,
    hit_count: 2,
    elapsed_ms: 198,
    trace_id: "mock-ins-trace",
    hits: [
      {
        rule_code: "R_INS_RESTRICTED_DRUG_NO_INDICATION",
        rule_name: "医保限定支付药品无适应症",
        severity: "HIGH",
        action_type: "BLOCK",
        message: "使用了医保限定支付药品，但未匹配到对应适应症。该费用可能被医保拒付。",
        condition_summary: "orders.insurance_restricted_drug_used = true AND indication_matched = false",
        suggested_actions: [
          "确认患者诊断是否符合限定支付适应症",
          "补充适应症诊断编码",
          "如不符，评估替代用药方案",
        ],
        source_document: {
          title: "国家基本医疗保险药品目录（2024 年版）",
          institution: "国家医保局",
          section: "限定支付说明",
        },
      },
      {
        rule_code: "R_INS_HIGH_COST_ALERT",
        rule_name: "高额费用预警",
        severity: "MEDIUM",
        action_type: "REMIND",
        message: "本次住院费用超过同病种均值 2 倍，请关注费用合理性。",
        source_document: {
          title: "演示医院医保费用审核规则 v2026.05",
          institution: "演示医院医保办",
          version: "2026.05",
          section: "DRG 费用偏离预警",
          evidence_level: "LOCAL_POLICY",
          reviewer: "INSURANCE_REVIEWER",
        },
      },
    ],
    org_source: "HEADER",
    tenant_id: "TENANT_DEMO",
    hospital_code: "HOSPITAL_DEMO",
    created_time: new Date().toISOString(),
  },

  ORDER_SAFETY: {
    result_id: "res-ord-001",
    scenario_code: "ORDER_SAFETY",
    package_code: "PKG_ORDER_SAFETY",
    package_version: "2026.05",
    evaluated_count: 5,
    hit_count: 1,
    elapsed_ms: 92,
    trace_id: "mock-ord-trace",
    hits: [
      {
        rule_code: "R_ORD_ANTIBIOTIC_DUPLICATE",
        rule_name: "48h 内重复使用同类抗菌药物",
        severity: "HIGH",
        action_type: "BLOCK",
        message: "48 小时内重复开具同类抗菌药物，存在用药安全风险。",
        condition_summary: "orders.antibiotic_duplicate_within_48h = true",
        suggested_actions: [
          "核查是否为治疗需要的续用/换药",
          "如非必要，停用重复抗菌药物",
          "记录用药理由",
        ],
        source_document: {
          title: "抗菌药物临床应用指导原则（2015 年版）",
          institution: "国家卫健委",
          section: "第四章 联合用药",
          evidence_level: "GUIDELINE",
        },
      },
    ],
    org_source: "BODY",
    tenant_id: "TENANT_DEMO",
    hospital_code: "HOSPITAL_DEMO",
    department_code: "DEPT_PHARMACY",
    created_time: new Date().toISOString(),
  },
};

const mockResultSummaries: RuleEngineResultSummary[] = Object.values(mockScenarios).map((r) => ({
  result_id: r.result_id,
  batch_id: r.batch_id,
  scenario_code: r.scenario_code,
  package_code: r.package_code,
  package_version: r.package_version,
  patient_id: r.hits?.[0]?.facts_matched?.patient_id as string | undefined,
  encounter_id: undefined,
  evaluated_count: r.evaluated_count,
  hit_count: r.hit_count,
  elapsed_ms: r.elapsed_ms,
  source: r.org_source,
  created_time: r.created_time,
}));

export const ruleEngineHandlers = [
  // ─── Rule Engine: evaluate ────────────────────────────────────────
  http.post(`${baseURL}/rule-engine/evaluate`, async ({ request }) => {
    const body = (await request.json()) as { scenario_code?: string };
    const code = body?.scenario_code;
    const lookupCode = code === "PATHWAY_ENTRY" ? "AMI_RECOMMEND" : code;
    const result = lookupCode ? mockScenarios[lookupCode] : undefined;
    if (!result) {
      return HttpResponse.json(
        {
          success: false,
          code: "VALIDATION_ERROR",
          message: `未知场景: ${code ?? "(空)"}`,
          data: null,
          trace_id: "mock-err",
        },
        { status: 400 },
      );
    }
    // 每次返回新的 result_id 和时间戳
    const fresh: EvaluateResponse = {
      ...result,
      scenario_code: (code || result.scenario_code) as EvaluateResponse["scenario_code"],
      result_id: `res-${code?.toLowerCase()}-${Date.now()}`,
      trace_id: `mock-${code?.toLowerCase()}-${Date.now()}`,
      created_time: new Date().toISOString(),
    };
    return HttpResponse.json(wrap(fresh));
  }),

  // ─── Rule Engine: batch-evaluate ──────────────────────────────────
  http.post(`${baseURL}/rule-engine/batch-evaluate`, async ({ request }) => {
    const body = (await request.json()) as {
      scenario_code?: string;
      items?: unknown[];
    };
    const code = body?.scenario_code;
    const lookupCode = code === "PATHWAY_ENTRY" ? "AMI_RECOMMEND" : code;
    const base = lookupCode ? mockScenarios[lookupCode] : undefined;
    if (!base) {
      return HttpResponse.json(
        {
          success: false,
          code: "VALIDATION_ERROR",
          message: `未知场景: ${code ?? "(空)"}`,
          data: null,
          trace_id: "mock-err",
        },
        { status: 400 },
      );
    }
    const items = body?.items ?? [];
    const batchId = `batch-${Date.now()}`;
    const results: EvaluateResponse[] = items.map((_, i) => ({
      ...base,
      scenario_code: (code || base.scenario_code) as EvaluateResponse["scenario_code"],
      result_id: `res-${code?.toLowerCase()}-${Date.now()}-${i}`,
      batch_id: batchId,
      trace_id: `mock-batch-${Date.now()}-${i}`,
      created_time: new Date().toISOString(),
    }));
    return HttpResponse.json(
      wrap({
        batch_id: batchId,
        scenario_code: code,
        results,
        total_evaluated: results.reduce((s, r) => s + r.evaluated_count, 0),
        total_hit: results.reduce((s, r) => s + r.hit_count, 0),
        elapsed_ms: results.reduce((s, r) => s + r.elapsed_ms, 0),
      }),
    );
  }),

  // ─── Rule Engine: results list ────────────────────────────────────
  http.get(`${baseURL}/rule-engine/results`, ({ request }) => {
    const url = new URL(request.url);
    const scenarioCode = url.searchParams.get("scenarioCode");
    let filtered = mockResultSummaries;
    if (scenarioCode) {
      const lookupCode = scenarioCode === "PATHWAY_ENTRY" ? "AMI_RECOMMEND" : scenarioCode;
      filtered = filtered.filter((r) => r.scenario_code === lookupCode || r.scenario_code === scenarioCode);
    }
    return HttpResponse.json(wrap(filtered));
  }),

  // ─── Rule Engine: result detail ───────────────────────────────────
  http.get(`${baseURL}/rule-engine/results/:resultId`, ({ params }) => {
    const id = params.resultId as string;
    // 从 mock 结果中查找（前缀匹配）
    const found = Object.values(mockScenarios).find((r) =>
      id.startsWith(`res-${r.scenario_code.toLowerCase()}`),
    );
    if (!found) {
      return HttpResponse.json(
        {
          success: false,
          code: "DATA_MISSING",
          message: `结果不存在: ${id}`,
          data: null,
          trace_id: "mock-err",
        },
        { status: 404 },
      );
    }
    return HttpResponse.json(
      wrap({
        ...found,
        result_id: id,
        trace_id: `mock-detail-${Date.now()}`,
      }),
    );
  }),
];
