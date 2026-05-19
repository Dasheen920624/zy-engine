export interface ReviewDrawerProps {
  visible: boolean;
  onClose: () => void;
  title: string;
  reviewStatus: 'pending' | 'approved' | 'rejected' | 'transferred';
  onApprove: () => void;
  onReject: (reason: string) => void;
  onTransfer: () => void;
  loading?: boolean;
  children: React.ReactNode; // 审核内容
  width?: number;
  showApprove?: boolean;
  showReject?: boolean;
  showTransfer?: boolean;
  rejectReasonRequired?: boolean;
  transferReasonRequired?: boolean;
}