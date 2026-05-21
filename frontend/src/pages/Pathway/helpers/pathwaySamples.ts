/**
 * 入径候选的国情样本（PATHWAY-ENGINE-COMPLETE）。
 *
 * 用于「患者路径管理」的「入径」对话框默认 patient_context，
 * 也作为开发期演示数据。当后端 candidates 端点返回真实结果时直接覆盖。
 */

export interface CandidatePatientContext {
  label: string;
  description: string;
  patient_context: {
    patient: { patient_id: string; gender?: string; age?: number };
    encounter: {
      encounter_id: string;
      visit_type?: string;
      department_code?: string;
      arrival_time?: string;
    };
    facts: Record<string, unknown>;
  };
}

export const CANDIDATE_SAMPLES: CandidatePatientContext[] = [
  {
    label: "AMI / STEMI 急诊入径",
    description: "急性 ST 段抬高型心肌梗死，符合 PCI 路径准入。",
    patient_context: {
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
    label: "急性脑梗死溶栓入径",
    description: "发病 4.5 小时内、NIHSS 评分 ≥ 4、无颅内出血禁忌。",
    patient_context: {
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
  {
    label: "COPD 急性加重入径",
    description: "慢阻肺急性加重，需要规范化吸入治疗 + 抗感染评估。",
    patient_context: {
      patient: { patient_id: "P-202605210008", gender: "M", age: 71 },
      encounter: {
        encounter_id: "E-202605210008",
        visit_type: "INPATIENT",
        department_code: "PULMONARY",
        arrival_time: "2026-05-20T14:00:00+08:00",
      },
      facts: {
        copd_grade: "GOLD_3",
        spo2_percent: 88,
        sputum_purulent: true,
        prior_steroid_use: false,
      },
    },
  },
];

export function findCandidateSample(label: string): CandidatePatientContext | undefined {
  return CANDIDATE_SAMPLES.find((s) => s.label === label);
}
