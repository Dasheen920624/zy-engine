export interface TracedCardProps {
  traceId: string;
  apiPath?: string;
  timestamp: string;
  children: React.ReactNode;
  variant?: 'default' | 'highlight';
  showTraceByDefault?: boolean;
}
