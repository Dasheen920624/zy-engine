import type { ActionMode } from '../../api/ruleActionLog';

export type OrderSafetyDecision = 'CONTINUE' | 'MODIFY' | 'CANCEL';

export interface OrderSafetyBlockerProps {
  /** 是否显示弹窗 */
  open: boolean;
  /** 规则代码 */
  ruleCode: string;
  /** 规则版本 */
  ruleVersion: string;
  /** 患者ID */
  patientId: string;
  /** 就诊ID */
  encounterId: string;
  /** 医嘱ID */
  orderId: string;
  /** 动作模式 */
  actionMode: ActionMode;
  /** 严重程度 */
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM';
  /** 标题 */
  title: string;
  /** 患者信息 */
  patientInfo: {
    name: string;
    age: number;
    gender: string;
    patientId: string;
  };
  /** 医嘱信息 */
  orderInfo: {
    name: string;
    dose: string;
    frequency: string;
  };
  /** 风险说明 */
  riskDescription: string;
  /** 证据信息 */
  evidence: string;
  /** 来源信息 */
  source?: {
    documentName: string;
    section?: string;
    publishYear?: number;
  };
  /** 决策回调 */
  onDecision: (decision: OrderSafetyDecision, data?: {
    reason?: string;
    informedConsent?: boolean;
    familyNotified?: boolean;
  }) => Promise<void>;
  /** 关闭回调 */
  onClose: () => void;
}

export interface ReasonDialogProps {
  open: boolean;
  onConfirm: (data: {
    reason: string;
    informedConsent: boolean;
    familyNotified: boolean;
  }) => void;
  onCancel: () => void;
  loading?: boolean;
}