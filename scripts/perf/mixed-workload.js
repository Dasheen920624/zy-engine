// 混合工作负载模拟 — GA-PERF-01 主测试
// 模拟真实医院场景下的综合业务负载
// 40% CDSS 评估、20% 路径操作、15% 规则评估、10% 适配器查询、10% 知识查询、5% 认证操作
// 100 并发用户持续 30 分钟

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import {
  BASE_URL, API_PREFIX, buildOptions, sustainedLoadScenario,
  getAuthToken, authHeaders, randomPick, randomPatientId, randomEncounterId,
} from './config.js';

// ==================== 自定义指标 ====================
const workloadErrorRate = new Rate('mixed_workload_errors');
const cdssDuration = new Trend('mixed_cdss_duration');
const pathwayDuration = new Trend('mixed_pathway_duration');
const ruleDuration = new Trend('mixed_rule_duration');
const adapterDuration = new Trend('mixed_adapter_duration');
const knowledgeDuration = new Trend('mixed_knowledge_duration');
const authDuration = new Trend('mixed_auth_duration');

// ==================== 测试数据 ====================

// CDSS 触发点
const TRIGGER_POINTS = [
  'ORDER_SAVE', 'PRESCRIPTION_SAVE', 'LAB_RESULT',
  'DISCHARGE_SUMMARY', 'PATHWAY_ENTRY', 'EXAM_RATIONALITY',
];

// 路径编码
const PATHWAY_CODES = [
  { code: 'AMI_STEMI', version: '1.0.0' },
  { code: 'PNEUMONIA', version: '1.0.0' },
  { code: 'DIABETES_T2', version: '1.0.0' },
  { code: 'STROKE', version: '1.0.0' },
  { code: 'HIP_REPLACE', version: '1.0.0' },
  { code: 'CABG', version: '1.0.0' },
];

// 规则场景编码
const SCENARIO_CODES = [
  'PATHWAY_ENTRY', 'EMR_QC', 'INSURANCE_QC',
  'ORDER_SAFETY', 'DRUG_INDICATION', 'EXAM_RATIONALITY',
];

// 规则编码
const RULE_CODES = [
  'R_EMR_DISCHARGE_SUMMARY_COMPLETE',
  'R_INS_DRUG_INDICATION_MISMATCH',
  'R_ORDER_SAFETY_DUP_ANTIBIOTIC',
  'R_ORD_PPI_CLOPIDOGREL_INTERACTION',
  'R_ORD_RENAL_DOSE_UNADJUSTED',
  'R_ORD_ASTHMA_NONSELECTIVE_BETA_BLOCKER',
  'R_ORD_REPEAT_EXAM_OVER_LIMIT',
  'R_AMI_STEMI_CANDIDATE',
];

// 适配器查询
const ADAPTER_QUERIES = [
  { adapter_code: 'ECG_ADAPTER', query_code: 'QUERY_ECG_REPORT' },
  { adapter_code: 'LIS_ADAPTER', query_code: 'QUERY_TROPONIN' },
  { adapter_code: 'HIS_ADAPTER', query_code: 'QUERY_DIAGNOSES' },
  { adapter_code: 'EMR_ADAPTER', query_code: 'QUERY_CHIEF_COMPLAINTS' },
];

// 互联互通查询
const INTEROP_QUERIES = [
  { adapter_code: 'HIS_HL7_ADAPTER', query_code: 'QUERY_PATIENT_ADT' },
  { adapter_code: 'HIS_FHIR_ADAPTER', query_code: 'QUERY_PATIENT_RESOURCE' },
  { adapter_code: 'LIS_HL7_ADAPTER', query_code: 'QUERY_LAB_RESULT' },
  { adapter_code: 'INSURANCE_REST_ADAPTER', query_code: 'QUERY_INSURANCE_CATALOG' },
];

// 诊断编码
const DIAGNOSES = [
  { code: 'I21.0', standard_code: 'AMI_STEMI', name: '急性ST段抬高型心肌梗死', is_primary: true },
  { code: 'J18.9', standard_code: 'PNEUMONIA', name: '肺炎', is_primary: true },
  { code: 'E11.9', standard_code: 'DIABETES_T2', name: '2型糖尿病', is_primary: true },
  { code: 'I63.9', standard_code: 'STROKE', name: '脑梗死', is_primary: true },
  { code: 'I20.0', standard_code: 'ACS', name: '急性冠脉综合征', is_primary: true },
];

// 症状编码
const SYMPTOM_CODES = ['CHEST_PAIN', 'DYSPNEA', 'FEVER', 'HEADACHE', 'LIMB_WEAKNESS', 'POLYURIA'];

// 既往史
const HISTORIES = [
  { code: 'HYPERTENSION', name: '高血压' },
  { code: 'DIABETES', name: '糖尿病' },
  { code: 'CORONARY_DISEASE', name: '冠心病' },
  { code: 'ASTHMA', name: '支气管哮喘' },
];

// 药品
const MEDICATIONS = [
  { order_id: 'ORD_001', standard_code: 'ASPIRIN', name: '阿司匹林肠溶片', class_codes: ['ANTIPLATELET'] },
  { order_id: 'ORD_002', standard_code: 'CLOPIDOGREL', name: '氯吡格雷', class_codes: ['ANTIPLATELET', 'P2Y12_INHIBITOR'] },
  { order_id: 'ORD_003', standard_code: 'OMEPRAZOLE', name: '奥美拉唑', class_codes: ['PPI'] },
  { order_id: 'ORD_004', standard_code: 'METFORMIN', name: '二甲双胍', class_codes: ['BIGUANIDE'] },
];

// 科室编码
const DEPARTMENTS = ['DEPT_CARDIOLOGY', 'DEPT_RESPIRATORY', 'DEPT_NEUROLOGY', 'DEPT_ENDOCRINE', 'DEPT_ORTHOPEDICS'];

// 知识图谱查询 — 症状/发现编码
const SYMPTOM_QUERY_CODES = ['CHEST_PAIN', 'DYSPNEA', 'FEVER', 'HEADACHE', 'LIMB_WEAKNESS'];
const FINDING_QUERY_CODES = ['ST_ELEVATION_CONTIGUOUS_LEADS', 'LUNG_INFILTRATE', 'HYPERGLYCEMIA', 'CEREBRAL_INFARCTION_CT'];
const RISK_FACTOR_CODES = ['HYPERTENSION', 'DIABETES', 'SMOKING', 'OBESITY', 'HYPERLIPIDEMIA'];

// 测试用户
const TEST_USERS = [
  { username: 'admin', password: 'admin123' },
  { username: 'doctor_zhang', password: 'test123' },
  { username: 'doctor_li', password: 'test123' },
  { username: 'nurse_wang', password: 'test123' },
];

export const options = buildOptions(sustainedLoadScenario());

// 初始化阶段：登录获取令牌
export function setup() {
  const token = getAuthToken();
  if (!token) {
    throw new Error('无法获取认证令牌，测试终止');
  }
  return { token };
}

// ==================== 辅助函数 ====================

// 构建患者上下文
function buildPatientContext() {
  const patientId = randomPatientId();
  const encounterId = randomEncounterId();
  const diagnosis = randomPick(DIAGNOSES);
  const numMeds = Math.floor(Math.random() * 3) + 1;
  const selectedMeds = [];
  for (let i = 0; i < numMeds; i++) {
    selectedMeds.push(randomPick(MEDICATIONS));
  }

  return {
    patient: {
      patient_id: patientId,
      gender: Math.random() > 0.5 ? 'M' : 'F',
      age: Math.floor(Math.random() * 60) + 20,
      weight_kg: Math.floor(Math.random() * 40) + 50,
      creatinine_clearance_ml_min: Math.floor(Math.random() * 60) + 30,
    },
    encounter: {
      encounter_id: encounterId,
      visit_type: randomPick(['EMERGENCY', 'INPATIENT', 'OUTPATIENT']),
      department_code: randomPick(DEPARTMENTS),
      arrival_time: new Date().toISOString(),
    },
    facts: {
      chief_complaints: [{ code: randomPick(SYMPTOM_CODES), text: randomPick(SYMPTOM_CODES), time: new Date().toISOString() }],
      diagnoses: [diagnosis],
      labs: [],
      exams: [{ code: 'ECG_12_LEAD', name: '十二导联心电图', finding_codes: [randomPick(FINDING_QUERY_CODES)], report_time: new Date().toISOString() }],
      histories: [randomPick(HISTORIES)],
      medications_current: selectedMeds,
      allergies: [],
      orders: {
        antibiotic_duplicate_within_48h: Math.random() > 0.9,
        ppi_clopidogrel_interaction_present: Math.random() > 0.7,
        renal_unadjusted_high_risk_drug_present: Math.random() > 0.8,
      },
      emr: {
        discharge_summary: {
          chief_complaint_filled: Math.random() > 0.1,
          diagnosis_filled: Math.random() > 0.1,
          discharge_orders_filled: Math.random() > 0.2,
        },
      },
    },
  };
}

// ==================== 业务操作函数 ====================

// CDSS 评估（40%）
function doCdssEvaluate(headers) {
  const triggerPoint = randomPick(TRIGGER_POINTS);
  const patientContext = buildPatientContext();

  const payload = JSON.stringify({
    trigger_point: triggerPoint,
    patient_context: patientContext,
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}${API_PREFIX}/cdss/evaluate`, payload, { headers });
  const elapsed = Date.now() - startTime;

  cdssDuration.add(elapsed);

  const passed = check(res, {
    'CDSS评估状态正常': (r) => r.status === 200,
  });

  workloadErrorRate.add(!passed);

  if (!passed) {
    console.error(`混合负载-CDSS评估失败: trigger=${triggerPoint} status=${res.status}`);
  }
}

// 路径操作（20%）
function doPathwayOperation(headers) {
  const patientContext = buildPatientContext();
  const pathway = randomPick(PATHWAY_CODES);

  // 候选路径识别
  const startTime = Date.now();
  const candidateRes = http.post(
    `${BASE_URL}${API_PREFIX}/patient-pathways/candidates`,
    JSON.stringify(patientContext),
    { headers },
  );
  let elapsed = Date.now() - startTime;

  const candidatePassed = check(candidateRes, {
    '候选路径识别状态正常': (r) => r.status === 200,
  });

  workloadErrorRate.add(!candidatePassed);

  // 患者入径
  const admitPayload = JSON.stringify({
    patient_id: patientContext.patient.patient_id,
    encounter_id: patientContext.encounter.encounter_id,
    pathway_code: pathway.code,
    version_no: pathway.version,
    doctor_id: `DOC_${Math.floor(Math.random() * 1000)}`,
    tenant_id: __ENV.TENANT_ID || 'default',
    hospital_code: 'HOSPITAL_DEMO',
    department_code: patientContext.encounter.department_code,
  });

  const admitStart = Date.now();
  const admitRes = http.post(
    `${BASE_URL}${API_PREFIX}/patient-pathways/admit`,
    admitPayload,
    { headers },
  );
  elapsed = Date.now() - admitStart;

  pathwayDuration.add(elapsed);

  const admitPassed = check(admitRes, {
    '患者入径状态正常': (r) => r.status === 200,
  });

  workloadErrorRate.add(!admitPassed);

  if (!candidatePassed || !admitPassed) {
    console.error(`混合负载-路径操作失败: pathway=${pathway.code} candidate=${candidateRes.status} admit=${admitRes.status}`);
  }
}

// 规则评估（15%）
function doRuleEvaluate(headers) {
  const scenarioCode = randomPick(SCENARIO_CODES);
  const patientContext = buildPatientContext();

  const numRules = Math.floor(Math.random() * 3) + 1;
  const selectedRules = [];
  for (let i = 0; i < numRules; i++) {
    const rule = randomPick(RULE_CODES);
    if (!selectedRules.includes(rule)) {
      selectedRules.push(rule);
    }
  }

  const payload = JSON.stringify({
    scenario_code: scenarioCode,
    rule_codes: selectedRules,
    patient_context: patientContext,
    operator_id: `DOC_${Math.floor(Math.random() * 1000)}`,
    tenant_id: __ENV.TENANT_ID || 'default',
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}${API_PREFIX}/rule-engine/evaluate`, payload, { headers });
  const elapsed = Date.now() - startTime;

  ruleDuration.add(elapsed);

  const passed = check(res, {
    '规则评估状态正常': (r) => r.status === 200,
  });

  workloadErrorRate.add(!passed);

  if (!passed) {
    console.error(`混合负载-规则评估失败: scenario=${scenarioCode} status=${res.status}`);
  }
}

// 适配器查询（10%）
function doAdapterQuery(headers) {
  const query = randomPick(ADAPTER_QUERIES);
  const patientId = randomPatientId();
  const encounterId = randomEncounterId();

  const payload = JSON.stringify({
    adapter_code: query.adapter_code,
    query_code: query.query_code,
    params: { patient_id: patientId, encounter_id: encounterId },
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}${API_PREFIX}/adapters/query`, payload, { headers });
  const elapsed = Date.now() - startTime;

  adapterDuration.add(elapsed);

  const passed = check(res, {
    '适配器查询状态正常': (r) => r.status === 200,
  });

  workloadErrorRate.add(!passed);

  if (!passed) {
    console.error(`混合负载-适配器查询失败: adapter=${query.adapter_code} status=${res.status}`);
  }
}

// 知识查询（10%）— 图谱候选疾病召回 + 证据查询
function doKnowledgeQuery(headers) {
  const rand = Math.random();

  if (rand < 0.6) {
    // 图谱候选疾病召回
    const numSymptoms = Math.floor(Math.random() * 3) + 1;
    const symptomCodes = [];
    for (let i = 0; i < numSymptoms; i++) {
      symptomCodes.push(randomPick(SYMPTOM_QUERY_CODES));
    }

    const payload = JSON.stringify({
      symptom_codes: symptomCodes,
      finding_codes: [randomPick(FINDING_QUERY_CODES)],
      risk_factor_codes: [randomPick(RISK_FACTOR_CODES)],
      limit: 10,
    });

    const startTime = Date.now();
    const res = http.post(`${BASE_URL}${API_PREFIX}/graph/disease-candidates`, payload, { headers });
    const elapsed = Date.now() - startTime;

    knowledgeDuration.add(elapsed);

    const passed = check(res, {
      '图谱候选疾病召回状态正常': (r) => r.status === 200,
    });

    workloadErrorRate.add(!passed);

    if (!passed) {
      console.error(`混合负载-图谱查询失败: status=${res.status}`);
    }
  } else {
    // 证据查询
    const targetCodes = ['AMI_STEMI', 'PNEUMONIA', 'DIABETES_T2', 'STROKE', 'ACS'];
    const payload = JSON.stringify({
      target_code: randomPick(targetCodes),
      target_type: 'DISEASE',
      graph_version: 'AMI_GRAPH_2026_01',
    });

    const startTime = Date.now();
    const res = http.post(`${BASE_URL}${API_PREFIX}/graph/evidence`, payload, { headers });
    const elapsed = Date.now() - startTime;

    knowledgeDuration.add(elapsed);

    const passed = check(res, {
      '证据查询状态正常': (r) => r.status === 200,
    });

    workloadErrorRate.add(!passed);

    if (!passed) {
      console.error(`混合负载-证据查询失败: status=${res.status}`);
    }
  }
}

// 认证操作（5%）
function doAuthOperation() {
  const user = randomPick(TEST_USERS);

  const payload = JSON.stringify({
    username: user.username,
    password: user.password,
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}${API_PREFIX}/auth/login`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  const elapsed = Date.now() - startTime;

  authDuration.add(elapsed);

  const passed = check(res, {
    '登录状态正常': (r) => r.status === 200,
  });

  workloadErrorRate.add(!passed);

  if (!passed) {
    console.error(`混合负载-认证失败: user=${user.username} status=${res.status}`);
  }
}

// ==================== 主测试函数 ====================

export default function (data) {
  const headers = authHeaders(data.token);

  // 按业务比例分配：40% CDSS、20% 路径、15% 规则、10% 适配器、10% 知识、5% 认证
  const rand = Math.random() * 100;

  if (rand < 40) {
    doCdssEvaluate(headers);
  } else if (rand < 60) {
    doPathwayOperation(headers);
  } else if (rand < 75) {
    doRuleEvaluate(headers);
  } else if (rand < 85) {
    doAdapterQuery(headers);
  } else if (rand < 95) {
    doKnowledgeQuery(headers);
  } else {
    doAuthOperation();
  }

  // 模拟用户思考时间
  sleep(Math.random() * 2 + 0.5);
}
