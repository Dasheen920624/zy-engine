/** 动作模式 */
export type ActionMode = 'NOTICE' | 'SOFT' | 'BLOCK';

/** 医生决策 */
export type DoctorDecision = 'CANCEL' | 'MODIFY' | 'INSIST';

/** 医嘱安全拦截事件 */
export interface OrderSafetyEvent {
  /** 事件唯一 ID */
  event_id: string;
  /** 命中规则编码 */
  rule_code: string;
  /** 命中规则名称 */
  rule_name: string;
  /** 动作模式 */
  action_mode: ActionMode;
  /** 患者 ID */
  patient_id: string;
  /** 患者姓名 */
  patient_name?: string;
  /** 患者年龄 */
  patient_age?: number;
  /** 就诊 ID */
  encounter_id?: string;
  /** 住院号 */
  admission_no?: string;
  /** 医嘱编码 */
  order_code: string;
  /** 医嘱名称 */
  order_name: string;
  /** 医嘱剂量 */
  order_dosage?: string;
  /** 拦截原因（证据描述） */
  intercept_reason: string;
  /** 来源文献 */
  source?: {
    documentName: string;
    section?: string;
    publishYear?: number;
  };
  /** 医生 ID */
  doctor_id?: string;
  /** 医生姓名 */
  doctor_name?: string;
}

/** 规则动作日志 */
export interface RuleActionLog {
  action_log_id: string;
  rule_code: string;
  rule_name?: string;
  action_mode: ActionMode;
  patient_id: string;
  encounter_id?: string;
  order_code: string;
  order_name?: string;
  doctor_id: string;
  doctor_name?: string;
  decision: DoctorDecision;
  reason?: string;
  informed_consent?: boolean;
  created_time: string;
  trace_id?: string;
  tenant_id?: string;
  hospital_code?: string;
  department_code?: string;
}

/** 坚持使用理由表单 */
export interface InsistFormValues {
  reason: string;
  informedRisk: boolean;
  signedConsent: boolean;
}
