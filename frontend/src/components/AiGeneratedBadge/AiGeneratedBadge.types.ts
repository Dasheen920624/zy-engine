export interface AiGeneratedBadgeProps {
  confidence?: number;              // 0-100
  model?: string;                   // 如"Dify AMI 路径工作流 v2.1"
  generatedAt?: string;
  reviewStatus?: 'pending' | 'accepted' | 'rejected' | 'modified';
  onAccept?: () => void;
  onModify?: () => void;
  onReject?: () => void;
  variant?: 'badge' | 'card';
}