// 适配器查询性能测试
// 测试 POST /api/adapters/query、POST /api/interop/query、POST /api/interop/cds-hooks 的响应性能
// GA-PERF-01：100 并发用户持续 30 分钟

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import {
  BASE_URL, API_PREFIX, buildOptions, sustainedLoadScenario,
  getAuthToken, authHeaders, randomPick, randomPatientId, randomEncounterId,
} from './config.js';

// 自定义指标
const adapterErrorRate = new Rate('adapter_query_errors');
const adapterQueryDuration = new Trend('adapter_query_duration');
const interopQueryDuration = new Trend('interop_query_duration');
const cdsHooksDuration = new Trend('cds_hooks_duration');

// 适配器定义 — 覆盖 REST/SQL/WEBSERVICE/HL7/FHIR/CDA/DICOM/IHE/CDS_HOOKS 类型
const ADAPTER_QUERIES = [
  { adapter_code: 'ECG_ADAPTER', query_code: 'QUERY_ECG_REPORT', description: '查询心电图报告' },
  { adapter_code: 'LIS_ADAPTER', query_code: 'QUERY_TROPONIN', description: '查询肌钙蛋白结果' },
  { adapter_code: 'HIS_ADAPTER', query_code: 'QUERY_DIAGNOSES', description: '查询诊断' },
  { adapter_code: 'EMR_ADAPTER', query_code: 'QUERY_CHIEF_COMPLAINTS', description: '查询主诉' },
  { adapter_code: 'EMR_WS_ADAPTER', query_code: 'QUERY_ADMISSION_NOTE', description: '查询入院记录' },
  { adapter_code: 'PACS_ADAPTER', query_code: 'QUERY_CHEST_CT', description: '查询胸部CT报告' },
];

// 互联互通适配器查询
const INTEROP_QUERIES = [
  { adapter_code: 'HIS_HL7_ADAPTER', query_code: 'QUERY_PATIENT_ADT', description: '查询患者入出院信息(HL7)' },
  { adapter_code: 'HIS_FHIR_ADAPTER', query_code: 'QUERY_PATIENT_RESOURCE', description: '查询FHIR Patient资源' },
  { adapter_code: 'EMR_CDA_ADAPTER', query_code: 'QUERY_DISCHARGE_SUMMARY', description: '查询出院小结(CDA)' },
  { adapter_code: 'LIS_HL7_ADAPTER', query_code: 'QUERY_LAB_RESULT', description: '查询检验结果(HL7)' },
  { adapter_code: 'LIS_FHIR_ADAPTER', query_code: 'QUERY_DIAGNOSTIC_REPORT', description: '查询FHIR DiagnosticReport' },
  { adapter_code: 'PACS_DICOM_ADAPTER', query_code: 'QUERY_CT_IMAGE', description: '查询CT影像(DICOM)' },
  { adapter_code: 'PACS_IHE_ADAPTER', query_code: 'QUERY_IMAGING_STUDY', description: '查询IHE影像研究' },
  { adapter_code: 'INSURANCE_REST_ADAPTER', query_code: 'QUERY_INSURANCE_CATALOG', description: '查询医保目录' },
];

// CDS Hooks 服务定义
const CDS_HOOKS_QUERIES = [
  { hook_id: 'HOOK_CDS_001', hook_type: 'order-sign', service_id: 'ami-risk-assessment', service_title: 'AMI风险评估' },
  { hook_id: 'HOOK_CDS_002', hook_type: 'order-select', service_id: 'drug-interaction-check', service_title: '药物相互作用检查' },
  { hook_id: 'HOOK_CDS_003', hook_type: 'patient-view', service_id: 'pathway-recommendation', service_title: '路径推荐' },
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

// 适配器查询 — POST /api/adapters/query
function queryAdapter(headers) {
  const query = randomPick(ADAPTER_QUERIES);
  const patientId = randomPatientId();
  const encounterId = randomEncounterId();

  const payload = JSON.stringify({
    adapter_code: query.adapter_code,
    query_code: query.query_code,
    params: {
      patient_id: patientId,
      encounter_id: encounterId,
    },
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}${API_PREFIX}/adapters/query`, payload, { headers });
  const elapsed = Date.now() - startTime;

  adapterQueryDuration.add(elapsed);

  const passed = check(res, {
    '适配器查询状态200': (r) => r.status === 200,
    '适配器查询响应有效': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true || body.code === '0';
      } catch (e) {
        return false;
      }
    },
  });

  adapterErrorRate.add(!passed);

  if (!passed) {
    console.error(`适配器查询失败: adapter=${query.adapter_code} query=${query.query_code} status=${res.status} body=${res.body?.substring(0, 200)}`);
  }
}

// 互联互通查询 — POST /api/interop/query
function queryInterop(headers) {
  const query = randomPick(INTEROP_QUERIES);
  const patientId = randomPatientId();
  const encounterId = randomEncounterId();

  const payload = JSON.stringify({
    adapter_code: query.adapter_code,
    query_code: query.query_code,
    params: {
      patient_id: patientId,
      encounter_id: encounterId,
    },
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}${API_PREFIX}/interop/query`, payload, { headers });
  const elapsed = Date.now() - startTime;

  interopQueryDuration.add(elapsed);

  const passed = check(res, {
    '互联互通查询状态200': (r) => r.status === 200,
    '互联互通查询响应有效': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true || body.code === '0';
      } catch (e) {
        return false;
      }
    },
  });

  adapterErrorRate.add(!passed);

  if (!passed) {
    console.error(`互联互通查询失败: adapter=${query.adapter_code} query=${query.query_code} status=${res.status} body=${res.body?.substring(0, 200)}`);
  }
}

// CDS Hooks 查询 — POST /api/interop/cds-hooks
function queryCdsHooks(headers) {
  const hook = randomPick(CDS_HOOKS_QUERIES);
  const patientId = randomPatientId();
  const encounterId = randomEncounterId();

  const payload = JSON.stringify({
    hook_id: hook.hook_id,
    hook_type: hook.hook_type,
    patient_id: patientId,
    encounter_id: encounterId,
    context: {
      userId: `DOC_${Math.floor(Math.random() * 1000)}`,
      patientId,
      encounterId,
    },
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}${API_PREFIX}/interop/cds-hooks`, payload, { headers });
  const elapsed = Date.now() - startTime;

  cdsHooksDuration.add(elapsed);

  const passed = check(res, {
    'CDS Hooks查询状态200': (r) => r.status === 200,
    'CDS Hooks查询响应有效': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true || body.code === '0';
      } catch (e) {
        return false;
      }
    },
  });

  adapterErrorRate.add(!passed);

  if (!passed) {
    console.error(`CDS Hooks查询失败: hook=${hook.hook_id} type=${hook.hook_type} status=${res.status} body=${res.body?.substring(0, 200)}`);
  }
}

export default function (data) {
  const headers = authHeaders(data.token);

  // 按比例分配查询类型：40% 适配器查询、40% 互联互通查询、20% CDS Hooks
  const rand = Math.random();
  if (rand < 0.4) {
    queryAdapter(headers);
  } else if (rand < 0.8) {
    queryInterop(headers);
  } else {
    queryCdsHooks(headers);
  }

  sleep(Math.random() * 2 + 0.5);
}
