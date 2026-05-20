export interface SourceInfoSource {
  documentName: string;          // 如"2023 ACC/AHA AMI 指南"
  documentId: string;            // 后端 doc id
  section?: string;              // 如"§4.2"
  publishYear?: number;
}

export interface SourceInfoCitation {
  id: string;
  excerpt: string;               // 引用原文段落
  pageNumber?: number;
}

export interface SourceInfoReview {
  status: 'reviewed' | 'pending' | 'rejected' | 'missing';
  reviewerName?: string;
  reviewedAt?: string;
}

export interface SourceEvidenceCardProps {
  source: SourceInfoSource;
  citation?: SourceInfoCitation;
  review: SourceInfoReview;
  version: string;                 // 来源版本
  variant?: 'inline' | 'card' | 'compact';   // 三种展示
  onClickDocument?: () => void;    // 点击查看原文
}