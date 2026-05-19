export interface DryRunResult {
  id: string;
  status: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  details?: Record<string, unknown>;
  timestamp?: string;
  duration?: number;
}

export interface DryRunResultPanelProps {
  results: DryRunResult[];
  loading?: boolean;
  title?: string;
  showTimestamp?: boolean;
  showDuration?: boolean;
  maxHeight?: number;
  onRetry?: () => void;
  onClear?: () => void;
  emptyText?: string;
}