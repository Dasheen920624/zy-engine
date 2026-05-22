// 规则引擎评估性能测试
// 测试 POST /api/rule-engine/evaluate 和 POST /api/rule-engine/batch-evaluate 的响应性能
// GA-PERF-01：100 并发用户持续 30 分钟

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import {
  BASE_URL, API_PREFIX, buildOptions, sustainedLoadScenario,
  getAuthToken, authHeaders, randomPick, randomPatientId, randomEncounterId,
} from './config.js';

// 自定义指标
const ruleErrorRate = new Rate('rule_engine_errors');
const evaluateDuration = new Trend('rule_evaluate_duration');
const batchEvaluateDuration = new Trend('rule_batch_evaluate_duration');

// 场景编码 — 对应规则引擎支持的评估场景
const SCENARIO_CODES = [
  'PATHWAY_ENTRY',     // 入径评估
  'EMR_QC',            // 病历质控
  'INSURANCE_QC',      // 医保质控
  'ORDER_SAFETY',      // 医嘱安全
  'DRUG_INDICATION',   // 药品适应症
  'EXAM_RATIONALITY',  // 检查合理性
];

// 规则编码 — 对应已定义的规则
const RULE_CODES = [
  'R_EMR_DISCHARGE_SUMMARY_COMPLETE',     // 出院小结完整性
  'R_INS_DRUG_INDICATION_MISMATCH',       // 医保适应症不匹配
  'R_ORDER_SAFETY_DUP_ANTIBIOTIC',        // 重复抗菌药物
  'R_ORD_PPI_CLOPIDOGREL_INTERACTION',    // PPI与氯吡格雷相互作用
  'R_ORD_RENAL_DOSE_UNADJUSTED',          // 肾功能未调剂量
  'R_ORD_ASTHMA_NONSELECTIVE_BETA_BLOCKER', // 哮喘禁用非选择性β阻滞剂
  'R_ORD_REPEAT_EXAM_OVER_LIMIT',         // 重复检查超限
  'R_AMI_STEMI_CANDIDATE',                // AMI-STEMI候选识别
];

// 诊断编码
const DIAGNOSES = [
  { code: 'I21.0', standard_code: 'AMI_STEMI', name: '急性ST段抬高型心肌梗死', is_primary: true },
  { code: 'J18.9', standard_code: 'PNEUMONIA', name: '肺炎', is_primary: true },
  { code: 'E11.9', standard_code: 'DIABETES_T2', name: '2型糖尿病', is_primary: true },
  { code: 'I63.9', standard_code: 'STROKE', name: '脑梗死', is_primary: true },
  { code: 'I20.0', standard_code: 'ACS', name: '急性冠脉综合征', is_primary: true },
];

// 既往史
const HISTORIES = [
  { code: 'HYPERTENSION', name: '高血压', status: 'ACTIVE' },
  { code: 'DIABETES', name: '糖尿病', status: 'ACTIVE' },
  { code: 'CORONARY_DISEASE', name: '冠心病', status: 'ACTIVE' },
  { code: 'ASTHMA', name: '支气管哮喘', status: 'ACTIVE' },
  { code: 'ATRIAL_FIBRILLATION', name: '房颤', status: 'ACTIVE' },
];

// 当前用药
const MEDICATIONS = [
  { order_id: 'ORD_001', standard_code: 'ASPIRIN', name: '阿司匹林肠溶片 100mg qd', dose_mg: 100, frequency: 'QD', class_codes: ['ANTIPLATELET'] },
  { order_id: 'ORD_002', standard_code: 'CLOPIDOGREL', name: '氯吡格雷 75mg qd', dose_mg: 75, frequency: 'QD', class_codes: ['ANTIPLATELET', 'P2Y12_INHIBITOR'] },
  { order_id: 'ORD_003', standard_code: 'OMEPRAZOLE', name: '奥美拉唑肠溶胶囊 40mg qd', dose_mg: 40, frequency: 'QD', class_codes: ['PPI'] },
  { order_id: 'ORD_004', standard_code: 'VANCOMYCIN', name: '万古霉素注射液 1g q12h', dose_mg: 1000, frequency: 'Q12H', class_codes: ['ANTIBIOTIC_GLYCOPEPTIDE'] },
  { order_id: 'ORD_005', standard_code: 'METFORMIN', name: '二甲双胍 500mg tid', dose_mg: 500, frequency: 'TID', class_codes: ['BIGUANIDE'] },
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
      department_code: randomPick(['DEPT_CARDIOLOGY', 'DEPT_RESPIRATORY', 'DEPT_NEUROLOGY', 'DEPT_ENDOCRINE']),
      admit_time: new Date().toISOString(),
    },
    facts: {
      diagnoses: [diagnosis],
      histories: [randomPick(HISTORIES)],
      medications_current: selectedMeds,
      allergies: [],
      orders: {
        antibiotic_duplicate_within_48h: Math.random() > 0.9,
        ppi_clopidogrel_interaction_present: Math.random() > 0.7,
        renal_unadjusted_high_risk_drug_present: Math.random() > 0.8,
        asthma_with_nonselective_beta_blocker_present: Math.random() > 0.85,
        repeat_chest_ct_within_24h_count: Math.floor(Math.random() * 4),
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

// 单条规则评估
function evaluateRules(headers) {
  const scenarioCode = randomPick(SCENARIO_CODES);
  const patientContext = buildPatientContext();

  // 随机选择 1-3 条规则
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

  evaluateDuration.add(elapsed);

  const passed = check(res, {
    '规则评估状态200': (r) => r.status === 200,
    '规则评估响应有效': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true || body.code === '0';
      } catch (e) {
        return false;
      }
    },
  });

  ruleErrorRate.add(!passed);

  if (!passed) {
    console.error(`规则评估失败: scenario=${scenarioCode} status=${res.status} body=${res.body?.substring(0, 200)}`);
  }
}

// 批量规则评估
function batchEvaluate(headers) {
  const scenarioCode = randomPick(SCENARIO_CODES);
  const batchSize = Math.floor(Math.random() * 3) + 2; // 2-4 条

  const items = [];
  for (let i = 0; i < batchSize; i++) {
    items.push({
      case_id: `CASE_${Math.floor(Math.random() * 10000)}`,
      patient_context: buildPatientContext(),
    });
  }

  const payload = JSON.stringify({
    scenario_code: scenarioCode,
    items,
    tenant_id: __ENV.TENANT_ID || 'default',
    operator_id: `DOC_${Math.floor(Math.random() * 1000)}`,
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}${API_PREFIX}/rule-engine/batch-evaluate`, payload, { headers });
  const elapsed = Date.now() - startTime;

  batchEvaluateDuration.add(elapsed);

  const passed = check(res, {
    '批量规则评估状态200': (r) => r.status === 200,
    '批量规则评估响应有效': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true || body.code === '0';
      } catch (e) {
        return false;
      }
    },
  });

  ruleErrorRate.add(!passed);

  if (!passed) {
    console.error(`批量规则评估失败: scenario=${scenarioCode} status=${res.status} body=${res.body?.substring(0, 200)}`);
  }
}

export default function (data) {
  const headers = authHeaders(data.token);

  // 70% 概率执行单条评估，30% 概率执行批量评估
  if (Math.random() < 0.7) {
    evaluateRules(headers);
  } else {
    batchEvaluate(headers);
  }

  sleep(Math.random() * 2 + 0.5);
}
