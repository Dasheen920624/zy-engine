import React, { useState, useCallback, useRef, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Allotment } from "allotment";
import { message, Spin, Result } from "antd";
import "allotment/dist/style.css";
import PathwayCanvas from "../../../components/PathwayCanvas/PathwayCanvas";
import type { PathwayDef, PathwayNode } from "../../../components/PathwayCanvas/types";
import PathwayEditorHeader from "./PathwayEditorHeader";
import StageTree from "./StageTree";
import NodePropertyPanel from "./NodePropertyPanel";
import UnsavedChangesGuard from "./UnsavedChangesGuard";
import { getPathway, savePathwayDraft, validatePathway, submitPathwayReview } from "../../../api/pathway";

// Mock pathway data for development
const MOCK_PATHWAY: PathwayDef = {
  code: "AMI_STEMI",
  name: "AMI/STEMI",
  version: "v2.2",
  status: "DRAFT",
  nodes: [
    { id: "start", type: "start", label: "开始", x: 300, y: 40, properties: {} },
    { id: "stage-admission", type: "stage", label: "入院阶段", x: 300, y: 120, properties: {} },
    { id: "task-ecg", type: "task", label: "ECG 检查", stage: "stage-admission", x: 140, y: 220, properties: { timeout_minutes: 30, bound_rules: ["R_ECG_TIMELIMIT"], bound_graph_queries: ["AMI_RELATED_EXAM"], variation_required: true, source_verified: true } },
    { id: "task-antiplatelet", type: "task", label: "抗血小板治疗", stage: "stage-admission", x: 460, y: 220, properties: { timeout_minutes: 60, bound_rules: ["R_MEDICATION_CHECK"], variation_required: false, source_verified: true } },
    { id: "stage-pci", type: "stage", label: "PCI 阶段", x: 300, y: 340, properties: {} },
    { id: "task-catheter", type: "task", label: "导管室准备", stage: "stage-pci", x: 140, y: 440, properties: { timeout_minutes: 45, source_verified: false } },
    { id: "task-balloon", type: "task", label: "球囊扩张", stage: "stage-pci", x: 460, y: 440, properties: { timeout_minutes: 90, bound_rules: ["R_MEDICATION_CHECK"], source_verified: true } },
    { id: "stage-post", type: "stage", label: "术后阶段", x: 300, y: 560, properties: {} },
    { id: "task-monitor", type: "task", label: "术后监护", stage: "stage-post", x: 300, y: 660, properties: { timeout_minutes: 120, source_verified: true } },
    { id: "end", type: "end", label: "结束", x: 300, y: 760, properties: {} },
  ],
  edges: [
    { id: "e1", source: "start", target: "stage-admission" },
    { id: "e2", source: "stage-admission", target: "task-ecg" },
    { id: "e3", source: "stage-admission", target: "task-antiplatelet" },
    { id: "e4", source: "task-ecg", target: "stage-pci" },
    { id: "e5", source: "task-antiplatelet", target: "stage-pci" },
    { id: "e6", source: "stage-pci", target: "task-catheter" },
    { id: "e7", source: "stage-pci", target: "task-balloon" },
    { id: "e8", source: "task-catheter", target: "stage-post" },
    { id: "e9", source: "task-balloon", target: "stage-post" },
    { id: "e10", source: "stage-post", target: "task-monitor" },
    { id: "e11", source: "task-monitor", target: "end" },
  ],
};

const PathwayEditor: React.FC = () => {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();
  const [pathway, setPathway] = useState<PathwayDef | null>(null);
  const [loading, setLoading] = useState(true);
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [selectedNodeId, setSelectedNodeId] = useState<string | undefined>();

  // Auto-save timer
  const autoSaveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pathwayRef = useRef<PathwayDef | null>(null);

  // Load pathway data
  useEffect(() => {
    const loadPathway = async () => {
      try {
        if (code && code !== "new") {
          const data = await getPathway(code);
          // Convert API response to PathwayDef
          if (data.draft_config && typeof data.draft_config === "object") {
            const config = data.draft_config as { nodes?: PathwayNode[]; edges?: PathwayDef["edges"] };
            if (config.nodes && config.edges) {
              const def: PathwayDef = {
                code: data.pathway_code,
                name: data.pathway_code,
                version: "v1.0",
                status: data.draft_status === "DRAFT" ? "DRAFT" : "PUBLISHED",
                nodes: config.nodes,
                edges: config.edges,
              };
              setPathway(def);
              pathwayRef.current = def;
              setLoading(false);
              return;
            }
          }
        }
        // Fallback to mock data
        setPathway(MOCK_PATHWAY);
        pathwayRef.current = MOCK_PATHWAY;
      } catch {
        // Fallback to mock data on error
        setPathway(MOCK_PATHWAY);
        pathwayRef.current = MOCK_PATHWAY;
      }
      setLoading(false);
    };
    loadPathway();
  }, [code]);

  // Auto-save with 30s debounce
  const triggerAutoSave = useCallback(() => {
    if (autoSaveTimerRef.current) {
      clearTimeout(autoSaveTimerRef.current);
    }
    autoSaveTimerRef.current = setTimeout(async () => {
      if (!pathwayRef.current || !code) return;
      try {
        setSaving(true);
        await savePathwayDraft(code, {
          nodes: pathwayRef.current.nodes,
          edges: pathwayRef.current.edges,
        });
        setDirty(false);
      } catch {
        // Auto-save failure is non-critical
      } finally {
        setSaving(false);
      }
    }, 30000);
  }, [code]);

  // Cleanup auto-save timer
  useEffect(() => {
    return () => {
      if (autoSaveTimerRef.current) {
        clearTimeout(autoSaveTimerRef.current);
      }
    };
  }, []);

  const handleChange = useCallback(
    (next: PathwayDef) => {
      setPathway(next);
      pathwayRef.current = next;
      setDirty(true);
      triggerAutoSave();
    },
    [triggerAutoSave],
  );

  const handleNodeSelect = useCallback((nodeId: string) => {
    setSelectedNodeId(nodeId);
  }, []);

  const handleNodePropertyChange = useCallback(
    (updated: PathwayNode) => {
      if (!pathway) return;
      const nextNodes = pathway.nodes.map((n) => (n.id === updated.id ? updated : n));
      const next = { ...pathway, nodes: nextNodes };
      setPathway(next);
      pathwayRef.current = next;
      setDirty(true);
      triggerAutoSave();
    },
    [pathway, triggerAutoSave],
  );

  const handleSave = useCallback(async () => {
    if (!pathway || !code) return;
    try {
      await savePathwayDraft(code, {
        nodes: pathway.nodes,
        edges: pathway.edges,
      });
      setDirty(false);
      message.success("保存成功");
    } catch {
      message.error("保存失败");
    }
  }, [pathway, code]);

  const handleSubmit = useCallback(async () => {
    if (!pathway || !code) return;
    try {
      // Validate first
      const result = await validatePathway(code, {
        nodes: pathway.nodes,
        edges: pathway.edges,
      });
      if (!result.valid) {
        const errorCount = result.errors.length;
        message.error(`校验失败：${errorCount} 个错误需要修复`);
        return;
      }
      await submitPathwayReview(code);
      setDirty(false);
      message.success("已提交审核");
      navigate(`/pathway/templates/${code}`);
    } catch {
      message.error("提交失败");
    }
  }, [pathway, code, navigate]);

  const handleDiff = useCallback(() => {
    if (code) {
      navigate(`/pathway/templates/${code}/diff`);
    }
  }, [code, navigate]);

  if (loading) {
    return (
      <div style={{ padding: 48, textAlign: "center" }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!pathway) {
    return (
      <Result
        status="404"
        title="路径模板不存在"
        extra={
          <button type="button" onClick={() => navigate("/pathway/templates")}>
            返回列表
          </button>
        }
      />
    );
  }

  const selectedNode = pathway.nodes.find((n) => n.id === selectedNodeId);

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100vh" }}>
      <PathwayEditorHeader
        pathway={pathway}
        saving={saving}
        onSave={handleSave}
        onSubmit={handleSubmit}
        onDiff={handleDiff}
      />
      <div style={{ flex: 1, overflow: "hidden" }}>
        <Allotment defaultSizes={[240, 600, 360]}>
          <Allotment.Pane minSize={180} maxSize={320}>
            <div
              style={{
                height: "100%",
                borderRight: "1px solid var(--mk-border)",
                background: "var(--mk-bg-elevated)",
                overflow: "auto",
              }}
            >
              <StageTree
                pathway={pathway}
                selectedNodeId={selectedNodeId}
                onSelect={handleNodeSelect}
              />
            </div>
          </Allotment.Pane>
          <Allotment.Pane minSize={400}>
            <PathwayCanvas
              pathway={pathway}
              mode="edit"
              onChange={handleChange}
              onNodeSelect={handleNodeSelect}
            />
          </Allotment.Pane>
          <Allotment.Pane minSize={280} maxSize={480}>
            <div
              style={{
                height: "100%",
                borderLeft: "1px solid var(--mk-border)",
                background: "var(--mk-bg-elevated)",
                overflow: "auto",
              }}
            >
              <NodePropertyPanel
                node={selectedNode}
                onChange={handleNodePropertyChange}
              />
            </div>
          </Allotment.Pane>
        </Allotment>
      </div>
      <UnsavedChangesGuard dirty={dirty} />
    </div>
  );
};

export default PathwayEditor;
