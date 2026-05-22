// 患者路径入径性能测试
// 测试候选路径识别、患者入径、路径实例查询的响应性能
// GA-PERF-01：100 并发用户持续 30 分钟

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import {
  BASE_URL, API_PREFIX, buildOptions, sustainedLoadScenario,
  getAuthToken, authHeaders, randomPick, randomPatientId, randomEncounterId,
} from './config.js';

// 自定义指标
const pathwayErrorRate = new Rate('pathway_errors');
const candidateDuration = new Trend('pathway_candidate_duration');
const admitDuration = new Trend('pathway_admit_duration');
const statusDuration = new Trend('pathway_status_duration');

// 路径编码 — 覆盖主要专科
const PATHWAY_CODES = [
  { code: 'AMI_STEMI', name: '急性ST段抬高型心肌梗死诊疗路径', version: '1.0.0' },
  { code: 'PNEUMONIA', name: '社区获得性肺炎诊疗路径', version: '1.0.0' },
  { code: 'DIABETES_T2', name: '2型糖尿病诊疗路径', version: '1.0.0' },
  { code: 'STROKE', name: '急性脑梗死诊疗路径', version: '1.0.0' },
  { code: 'HIP_REPLACE', name: '髋关节置换术诊疗路径', version: '1.0.0' },
  { code: 'CABG', name: '冠状动脉旁路移植术诊疗路径', version: '1.0.0' },
];

// 症状编码
const SYMPTOM_CODES = [
  'CHEST_PAIN',          // 胸痛
  'DYSPNEA',             // 呼吸困难
  'FEVER',               // 发热
  'HEADACHE',            // 头痛
  'LIMB_WEAKNESS',       // 肢体无力
  'HIP_PAIN',            // 髋部疼痛
  'POLYURIA',            // 多尿
  'PALPITATION',         // 心悸
];

// 检查发现编码
const FINDING_CODES = [
  'ST_ELEVATION_CONTIGUOUS_LEADS',  // 相邻导联ST段抬高
  'LUNG_INFILTRATE',                // 肺部浸润影
  'HYPERGLYCEMIA',                  // 高血糖
  'CEREBRAL_INFARCTION_CT',         // CT脑梗死
  'JOINT_SPACE_NARROWING',          // 关节间隙狭窄
];

// 既往史编码
const HISTORY_CODES = [
  'HYPERTENSION',    // 高血压
  'DIABETES',        // 糖尿病
  'CORONARY_DISEASE', // 冠心病
  'ATRIAL_FIBRILLATION', // 房颤
  'HYPERLIPIDEMIA',  // 高脂血症
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

// 构建患者上下文 — 用于候选路径识别
function buildPatientContext() {
  const patientId = randomPatientId();
  const encounterId = randomEncounterId();
  const numSymptoms = Math.floor(Math.random() * 3) + 1;
  const symptoms = [];
  for (let i = 0; i < numSymptoms; i++) {
    symptoms.push(randomPick(SYMPTOM_CODES));
  }

  return {
    patient: {
      patient_id: patientId,
      gender: Math.random() > 0.5 ? 'M' : 'F',
      age: Math.floor(Math.random() * 60) + 20,
    },
    encounter: {
      encounter_id: encounterId,
      visit_type: randomPick(['EMERGENCY', 'INPATIENT']),
      department_code: randomPick(['DEPT_CARDIOLOGY', 'DEPT_RESPIRATORY', 'DEPT_NEUROLOGY', 'DEPT_ENDOCRINE', 'DEPT_ORTHOPEDICS']),
      arrival_time: new Date().toISOString(),
    },
    facts: {
      chief_complaints: symptoms.map((code) => ({
        code,
        text: code,
        time: new Date().toISOString(),
      })),
      diagnoses: [],
      labs: [],
      exams: [
        {
          code: 'ECG_12_LEAD',
          name: '十二导联心电图',
          finding_codes: [randomPick(FINDING_CODES)],
          report_time: new Date().toISOString(),
        },
      ],
      histories: [
        { code: randomPick(HISTORY_CODES), name: randomPick(HISTORY_CODES) },
      ],
      medications: [],
      allergies: [],
    },
  };
}

// 步骤1：候选路径识别
function findCandidates(headers) {
  const patientContext = buildPatientContext();
  const payload = JSON.stringify(patientContext);

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}${API_PREFIX}/patient-pathways/candidates`, payload, { headers });
  const elapsed = Date.now() - startTime;

  candidateDuration.add(elapsed);

  let candidateData = null;
  const passed = check(res, {
    '候选路径识别状态200': (r) => r.status === 200,
    '候选路径识别响应有效': (r) => {
      try {
        const body = JSON.parse(r.body);
        candidateData = body.data;
        return body.success === true || body.code === '0';
      } catch (e) {
        return false;
      }
    },
  });

  pathwayErrorRate.add(!passed);

  if (!passed) {
    console.error(`候选路径识别失败: status=${res.status} body=${res.body?.substring(0, 200)}`);
  }

  return candidateData;
}

// 步骤2：患者入径
function admitPatient(headers, patientId, encounterId) {
  const pathway = randomPick(PATHWAY_CODES);
  const payload = JSON.stringify({
    patient_id: patientId,
    encounter_id: encounterId,
    pathway_code: pathway.code,
    version_no: pathway.version,
    doctor_id: `DOC_${Math.floor(Math.random() * 1000)}`,
    tenant_id: __ENV.TENANT_ID || 'default',
    hospital_code: 'HOSPITAL_DEMO',
    department_code: 'DEPT_CARDIOLOGY',
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}${API_PREFIX}/patient-pathways/admit`, payload, { headers });
  const elapsed = Date.now() - startTime;

  admitDuration.add(elapsed);

  let instanceId = null;
  const passed = check(res, {
    '患者入径状态200': (r) => r.status === 200,
    '患者入径响应有效': (r) => {
      try {
        const body = JSON.parse(r.body);
        instanceId = body.data?.instance_id;
        return body.success === true || body.code === '0';
      } catch (e) {
        return false;
      }
    },
  });

  pathwayErrorRate.add(!passed);

  if (!passed) {
    console.error(`患者入径失败: pathway=${pathway.code} status=${res.status} body=${res.body?.substring(0, 200)}`);
  }

  return instanceId;
}

// 步骤3：查询路径实例状态
function checkPathwayStatus(headers, instanceId) {
  if (!instanceId) return;

  const startTime = Date.now();
  const res = http.get(`${BASE_URL}${API_PREFIX}/patient-pathways/${instanceId}`, { headers });
  const elapsed = Date.now() - startTime;

  statusDuration.add(elapsed);

  const passed = check(res, {
    '路径实例查询状态200': (r) => r.status === 200,
    '路径实例查询响应有效': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true || body.code === '0';
      } catch (e) {
        return false;
      }
    },
  });

  pathwayErrorRate.add(!passed);

  if (!passed) {
    console.error(`路径实例查询失败: instanceId=${instanceId} status=${res.status}`);
  }
}

export default function (data) {
  const headers = authHeaders(data.token);

  // 执行完整的路径入径流程
  const candidateData = findCandidates(headers);
  sleep(Math.random() * 1 + 0.3);

  const patientId = randomPatientId();
  const encounterId = randomEncounterId();
  const instanceId = admitPatient(headers, patientId, encounterId);
  sleep(Math.random() * 1 + 0.3);

  checkPathwayStatus(headers, instanceId);
  sleep(Math.random() * 2 + 0.5);
}
