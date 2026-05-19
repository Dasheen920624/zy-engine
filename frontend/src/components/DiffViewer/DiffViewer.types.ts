export interface DiffLine {
  type: 'add' | 'remove' | 'normal';
  content: string;
  lineNumber?: number;
  oldLineNumber?: number;
  newLineNumber?: number;
}

export interface DiffViewerProps {
  oldContent: string;
  newContent: string;
  title?: string;
  oldTitle?: string;
  newTitle?: string;
  mode?: 'split' | 'unified';
  showLineNumbers?: boolean;
  contextLines?: number;
  loading?: boolean;
}