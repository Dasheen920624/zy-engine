export type DangerLevel = 'low' | 'medium' | 'high';

export interface DangerConfirmProps {
  /** 危险等级 */
  level: DangerLevel;
  /** 弹窗标题 */
  title: string;
  /** 描述文字 */
  description: string;
  /** 后果列表 */
  consequences: string[];
  /** medium/high 级需要用户输入的文字（匹配用） */
  confirmText?: string;
  /** high 级必须填原因 */
  reasonRequired?: boolean;
  /** high 级特殊提示，如"此操作不可撤销" */
  irreversibleNote?: string;
  /** 确认回调 */
  onConfirm: (input?: { reason?: string }) => Promise<void> | void;
  /** 取消回调 */
  onCancel: () => void;
  /** 外部控制弹窗可见性 */
  open?: boolean;
  /** 确认按钮loading状态 */
  loading?: boolean;
}
