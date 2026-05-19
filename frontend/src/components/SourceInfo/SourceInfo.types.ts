export interface SourceInfoProps {
  source: {
    documentName: string;
    documentId: string;
    section?: string;
    publishYear?: number;
  };
  citation?: {
    id: string;
    excerpt: string;
    pageNumber?: number;
  };
  review?: {
    status: 'reviewed' | 'pending' | 'rejected' | 'missing';
    reviewerName?: string;
    reviewedAt?: string;
  };
  version?: string;
  variant?: 'inline' | 'card' | 'compact';
  onClickDocument?: () => void;
}
