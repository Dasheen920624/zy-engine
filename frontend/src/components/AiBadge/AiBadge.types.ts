export interface AiBadgeProps {
  confidence: number; // 0-100
  model: string;
  generatedAt: string;
  reviewStatus: 'pending' | 'accepted' | 'rejected' | 'modified';
  onAccept?: () => void;
  onModify?: () => void;
  onReject?: () => void;
  variant?: 'badge' | 'card';
}
