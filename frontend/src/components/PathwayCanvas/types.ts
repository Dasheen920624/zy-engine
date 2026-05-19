export interface PathwayNode {
  id: string;
  type: "stage" | "task" | "decision" | "start" | "end";
  label: string;
  stage?: string;
  x: number;
  y: number;
  properties: {
    timeout_minutes?: number;
    bound_rules?: string[];
    bound_graph_queries?: string[];
    bound_dify?: string[];
    transition_condition?: string;
    variation_required?: boolean;
    source_verified?: boolean;
  };
}

export interface PathwayEdge {
  id: string;
  source: string;
  target: string;
  label?: string;
  condition?: string;
}

export interface PathwayDef {
  code: string;
  name: string;
  version: string;
  status: "DRAFT" | "PUBLISHED" | "RETIRED";
  nodes: PathwayNode[];
  edges: PathwayEdge[];
}

export interface PathwayCanvasProps {
  pathway: PathwayDef;
  mode: "edit" | "view" | "diff";
  onChange?: (next: PathwayDef) => void;
  onNodeSelect?: (nodeId: string) => void;
  diffWith?: PathwayDef;
  readOnly?: boolean;
}

export interface ValidationResult {
  valid: boolean;
  errors: ValidationIssue[];
  warnings: ValidationIssue[];
}

export interface ValidationIssue {
  nodeId?: string;
  edgeId?: string;
  code: string;
  message: string;
  severity: "error" | "warning";
}
