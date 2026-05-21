/**
 * 试运行面板的国情样本（PR-FINAL-11）。
 *
 * 与后端 `ai-dev-input/06_samples/sample_rule_engine_scenarios.json` 同构。
 * 当后端样本更新时，前端样本可手工同步（非强约束，只是用户体验默认值）。
 */

import type { EvaluateRequest, ScenarioCode } from "../../../api/types";

interface SampleScenario {
  code: ScenarioCode;
  label: string;
  description: string;
  patientContext: EvaluateRequest["patient_context"];
}

export const SAMPLE_SCENARIOS: SampleScenario[] = [
  {
    code: "AMI_RECOMMEND",
    label: "AMI 急性心梗推荐",
    description: "急性心梗（STEMI）患者入急诊，触发抗血小板+他汀+PCI 路径推荐。",
    patientContext: {
      patient: { patient_id: "P-202605210001", gender: "M", age: 62 },
      encounter: {
        encounter_id: "E-202605210001",
        visit_type: "EMERGENCY",
        department_code: "CARDIO",
        arrival_time: "2026-05-21T08:30:00+08:00",
      },
      facts: {
        chief_complaint: "胸痛 2 小时",
        ecg_st_elevation: true,
        troponin_i_ng_ml: 5.2,
        creatinine_umol_l: 78,
        history_pci: false,
        contraindication_dapt: false,
      },
    },
  },
  {
    code: "EMR_QC",
    label: "病历时限质控",
    description: "入院记录 24 小时未完成 / 首程 8 小时未完成 / 病程 3 天未签字。",
    patientContext: {
      patient: { patient_id: "P-202605210002", gender: "F", age: 45 },
      encounter: {
        encounter_id: "E-202605210002",
        visit_type: "INPATIENT",
        department_code: "NEURO",
        arrival_time: "2026-05-19T14:00:00+08:00",
      },
      facts: {
        admission_note_signed_at: null,
        first_progress_note_signed_at: null,
        progress_notes_overdue_days: 4,
      },
    },
  },
  {
    code: "ORDER_SAFETY",
    label: "医嘱安全（红线）",
    description: "QT 延长 + 联用红线药物 / eGFR<30 + 肾毒性药物。",
    patientContext: {
      patient: { patient_id: "P-202605210003", gender: "M", age: 71 },
      encounter: {
        encounter_id: "E-202605210003",
        visit_type: "INPATIENT",
        department_code: "GERIATRIC",
        arrival_time: "2026-05-20T09:00:00+08:00",
      },
      facts: {
        qtc_ms: 478,
        creatinine_clearance_ml_min: 24,
        proposed_order_code: "AMIODARONE_IV",
        baseline_orders: ["LEVOFLOXACIN_PO"],
      },
    },
  },
  {
    code: "INSURANCE_QC",
    label: "医保智能审核",
    description: "病种与诊断不匹配 / 重复收费 / 超限价 / 适应症外用药。",
    patientContext: {
      patient: { patient_id: "P-202605210004", gender: "F", age: 58 },
      encounter: {
        encounter_id: "E-202605210004",
        visit_type: "INPATIENT",
        department_code: "ONCOLOGY",
      },
      facts: {
        diagnosis_code: "C50.9",
        coding_drg: "JC11",
        charges: [
          { item: "DRUG_BEVACIZUMAB", amount: 18000, indication_match: false },
          { item: "ITEM_X_RAY_CHEST", count: 3 },
        ],
      },
    },
  },
  {
    code: "PATHWAY_ENTRY",
    label: "路径准入",
    description: "判断患者是否符合急性脑梗死溶栓路径准入条件。",
    patientContext: {
      patient: { patient_id: "P-202605210005", gender: "M", age: 67 },
      encounter: {
        encounter_id: "E-202605210005",
        visit_type: "EMERGENCY",
        department_code: "NEURO",
        arrival_time: "2026-05-21T07:15:00+08:00",
      },
      facts: {
        onset_to_door_minutes: 75,
        nihss_score: 8,
        ct_excluded_hemorrhage: true,
        contraindication_thrombolysis: false,
      },
    },
  },
];

export function findSampleScenario(code: ScenarioCode): SampleScenario | undefined {
  return SAMPLE_SCENARIOS.find((s) => s.code === code);
}

export function defaultDslTemplate(): Record<string, unknown> {
  return {
    rule_code: "DEMO_RULE",
    rule_name: "示例规则",
    rule_type: "TIME_LIMIT_QC",
    version: "1.0.0",
    trigger: {
      events: ["admission_recorded"],
      scope: "ENCOUNTER",
    },
    data_requirements: [
      {
        fact_name: "admission_note_signed_at",
        source: { adapter_code: "EMR_DEFAULT", query_code: "Q_ADMISSION_NOTE" },
      },
    ],
    condition: {
      all: [
        { field: "admission_note_signed_at", exists: "admission_note_signed_at" },
      ],
    },
    result: {
      hit: { severity: "HIGH", message: "入院记录超时未签字" },
      not_hit: { severity: "INFO", message: "入院记录已及时签字" },
    },
  };
}
