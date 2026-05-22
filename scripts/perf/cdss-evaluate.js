// CDSS 临床决策支持评估性能测试
// 测试 POST /api/cdss/evaluate 在不同触发点下的响应性能
// GA-PERF-01：100 并发用户持续 30 分钟

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import {
  BASE_URL, API_PREFIX, buildOptions, sustainedLoadScenario,
  getAuthToken, authHeaders, randomPick, randomPatientId, randomEncounterId,
} from './config.js';

// 自定义指标
const cdssErrorRate = new Rate('cdss_evaluate_errors');
const cdssDuration = new Trend('cdss_evaluate_duration');

// 触发点类型 — 对应 CDSS 标准业务场景
const TRIGGER_POINTS = [
  'ORDER_SAVE',            // 医嘱保存
  'PRESCRIPTION_SAVE',     // 处方保存
  'LAB_RESULT',            // 检验结果
  'DISCHARGE_SUMMARY',     // 出院小结
  'PATHWAY_ENTRY',         // 入径评估
  'EXAM_RATIONALITY',      // 检查合理性
];

// 科室编码
const DEPARTMENTS = [
  'DEPT_CARDIOLOGY',   // 心内科
  'DEPT_RESPIRATORY',  // 呼吸科
  'DEPT_NEUROLOGY',    // 神经内科
  'DEPT_ENDOCRINE',    // 内分泌科
  'DEPT_ORTHOPEDICS',  // 骨科
  'DEPT_GENERAL',      // 普外科
];

// 诊断编码
const DIAGNOSES = [
  { code: 'I21.0', name: '急性ST段抬高型心肌梗死', standard_code: 'AMI_STEMI' },
  { code: 'J18.9', name: '肺炎', standard_code: 'PNEUMONIA' },
  { code: 'E11.9', name: '2型糖尿病', standard_code: 'DIABETES_T2' },
  { code: 'I63.9', name: '脑梗死', standard_code: 'STROKE' },
  { code: 'M16.1', name: '原发性髋关节病', standard_code: 'HIP_REPLACE' },
  { code: 'I20.0', name: '不稳定型心绞痛', standard_code: 'ACS' },
];

// 药品编码
const MEDICATIONS = [
  { standard_code: 'ASPIRIN', name: '阿司匹林肠溶片', class_codes: ['ANTIPLATELET'] },
  { standard_code: 'CLOPIDOGREL', name: '氯吡格雷', class_codes: ['ANTIPLATELET', 'P2Y12_INHIBITOR'] },
  { standard_code: 'OMEPRAZOLE', name: '奥美拉唑', class_codes: ['PPI'] },
  { standard_code: 'METFORMIN', name: '二甲双胍', class_codes: ['BIGUANIDE'] },
  { standard_code: 'VANCOMYCIN', name: '万古霉素', class_codes: ['ANTIBIOTIC_GLYCOPEPTIDE'] },
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
  const medication = randomPick(MEDICATIONS);
  const department = randomPick(DEPARTMENTS);

  return {
    patient: {
      patient_id: patientId,
      gender: Math.random() > 0.5 ? 'M' : 'F',
      age: Math.floor(Math.random() * 60) + 20,
    },
    encounter: {
      encounter_id: encounterId,
      visit_type: randomPick(['EMERGENCY', 'INPATIENT', 'OUTPATIENT']),
      department_code: department,
      arrival_time: new Date().toISOString(),
    },
    facts: {
      diagnoses: [
        {
          code: diagnosis.code,
          standard_code: diagnosis.standard_code,
          name: diagnosis.name,
          is_primary: true,
        },
      ],
      medications_current: [
        {
          order_id: `ORD_${Math.floor(Math.random() * 10000)}`,
          standard_code: medication.standard_code,
          name: medication.name,
          class_codes: medication.class_codes,
        },
      ],
      histories: [
        { code: 'HYPERTENSION', name: '高血压' },
        { code: 'DIABETES', name: '糖尿病' },
      ],
      allergies: [],
      orders: {
        antibiotic_duplicate_within_48h: Math.random() > 0.9,
        ppi_clopidogrel_interaction_present: Math.random() > 0.7,
        renal_unadjusted_high_risk_drug_present: Math.random() > 0.8,
      },
    },
  };
}

export default function (data) {
  const headers = authHeaders(data.token);
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
    'CDSS评估状态200': (r) => r.status === 200,
    'CDSS评估响应体有效': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true || body.code === '0';
      } catch (e) {
        return false;
      }
    },
    'CDSS评估P95<300ms': () => elapsed < 300,
  });

  cdssErrorRate.add(!passed);

  if (!passed) {
    console.error(`CDSS评估失败: trigger=${triggerPoint} status=${res.status} body=${res.body?.substring(0, 200)}`);
  }

  sleep(Math.random() * 2 + 0.5);
}
