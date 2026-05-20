export type AlertSeverity = 'info' | 'warning' | 'danger' | 'success';

export interface EmbeddedAlertAction {
  text: string;
  intent: 'primary' | 'secondary' | 'tertiary';
  onClick: () => void;
}

export interface EmbeddedAlertSource {
  documentName: string;
  section?: string;
  publishYear?: number;
}

export interface EmbeddedAlertProps {
  /** 警告级别 */
  severity: AlertSeverity;
  /** 标题，≤ 20 字 */
  title: string;
  /** 命中证据，≤ 50 字 */
  evidence: string;
  /** 来源信息 */
  source?: EmbeddedAlertSource;
  /** 规则引用 */
  ruleRef?: { code: string; version: string };
  /** AI 置信度 0-100 */
  confidence?: number;
  /** 操作按钮，2-3 个 */
  actions: EmbeddedAlertAction[];
  /** 关闭回调 */
  onClose?: () => void;
  /** 自动消失时间(ms)，仅 success 用 */
  autoHide?: number;
}
