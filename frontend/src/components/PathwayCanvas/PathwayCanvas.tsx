import React, { useEffect, useRef, useCallback } from "react";
import { Graph, Shape } from "@antv/x6";
import type { PathwayCanvasProps, PathwayNode, PathwayEdge } from "./types";
import "./PathwayCanvas.css";

const NODE_WIDTH = 160;
const NODE_HEIGHT = 40;

const nodeColorMap: Record<string, { fill: string; stroke: string; font: string }> = {
  stage: { fill: "var(--mk-primary-soft)", stroke: "var(--mk-primary)", font: "var(--mk-text-primary)" },
  task: { fill: "var(--mk-bg-elevated)", stroke: "var(--mk-border)", font: "var(--mk-text-primary)" },
  decision: { fill: "var(--mk-warning-soft)", stroke: "var(--mk-warning)", font: "var(--mk-text-primary)" },
  start: { fill: "var(--mk-success-soft)", stroke: "var(--mk-success)", font: "var(--mk-text-primary)" },
  end: { fill: "var(--mk-danger-soft)", stroke: "var(--mk-danger)", font: "var(--mk-text-primary)" },
};

function buildGraphNodes(graph: Graph, nodes: PathwayNode[]) {
  nodes.forEach((n: PathwayNode) => {
    const colors = nodeColorMap[n.type] ?? nodeColorMap.task;
    graph.addNode({
      id: n.id,
      shape: "rect",
      x: n.x,
      y: n.y,
      width: NODE_WIDTH,
      height: NODE_HEIGHT,
      label: n.label,
      attrs: {
        body: {
          fill: colors.fill,
          stroke: colors.stroke,
          strokeWidth: 1,
          rx: n.type === "decision" ? 0 : 6,
          ry: n.type === "decision" ? 0 : 6,
        },
        label: {
          fill: colors.font,
          fontSize: 13,
        },
      },
      ports: {
        groups: {
          top: { position: "top", attrs: { circle: { r: 3, magnet: true, stroke: "var(--mk-primary)", fill: "var(--mk-bg-elevated)" } } },
          bottom: { position: "bottom", attrs: { circle: { r: 3, magnet: true, stroke: "var(--mk-primary)", fill: "var(--mk-bg-elevated)" } } },
          left: { position: "left", attrs: { circle: { r: 3, magnet: true, stroke: "var(--mk-primary)", fill: "var(--mk-bg-elevated)" } } },
          right: { position: "right", attrs: { circle: { r: 3, magnet: true, stroke: "var(--mk-primary)", fill: "var(--mk-bg-elevated)" } } },
        },
        items: [
          { group: "top", id: `${n.id}-top` },
          { group: "bottom", id: `${n.id}-bottom` },
          { group: "left", id: `${n.id}-left` },
          { group: "right", id: `${n.id}-right` },
        ],
      },
      data: n,
    });
  });
}

function buildGraphEdges(graph: Graph, edges: PathwayEdge[]) {
  edges.forEach((e: PathwayEdge) => {
    graph.addEdge({
      id: e.id,
      source: { cell: e.source, port: `${e.source}-bottom` },
      target: { cell: e.target, port: `${e.target}-top` },
      labels: e.label ? [{ attrs: { label: { text: e.label } } }] : [],
      attrs: {
        line: {
          stroke: "var(--mk-border)",
          strokeWidth: 1,
          targetMarker: { name: "block", width: 8, height: 6 },
        },
      },
    });
  });
}

const PathwayCanvas: React.FC<PathwayCanvasProps> = ({
  pathway,
  mode,
  onChange,
  onNodeSelect,
  diffWith,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const graphRef = useRef<Graph | null>(null);

  const isReadOnly = mode === "view" || mode === "diff";

  // Initialize graph
  useEffect(() => {
    if (!containerRef.current) return;

    const graph = new Graph({
      container: containerRef.current,
      autoResize: true,
      background: { color: "var(--mk-bg-base)" },
      grid: { visible: true, type: "dot", size: 10, args: { color: "var(--mk-border)", thickness: 1 } },
      panning: { enabled: true },
      mousewheel: { enabled: true, modifiers: ["ctrl", "meta"] },
      connecting: {
        snap: { radius: 30 },
        allowBlank: false,
        allowLoop: false,
        allowMulti: false,
        highlight: true,
        createEdge() {
          return new Shape.Edge({
            attrs: {
              line: {
                stroke: "var(--mk-primary)",
                strokeWidth: 1,
                targetMarker: { name: "block", width: 8, height: 6 },
              },
            },
          });
        },
      },
      // @ts-expect-error X6 selecting plugin type mismatch
      selecting: { enabled: !isReadOnly, rubberband: true },
      interacting: { nodeMovable: !isReadOnly },
    });

    graphRef.current = graph;

    return () => {
      graph.dispose();
      graphRef.current = null;
    };
  }, [isReadOnly]);

  // Render nodes and edges
  useEffect(() => {
    const graph = graphRef.current;
    if (!graph || !pathway) return;

    graph.clearCells();
    buildGraphNodes(graph, pathway.nodes);
    buildGraphEdges(graph, pathway.edges);

    // Diff mode: highlight differences
    if (mode === "diff" && diffWith) {
      const oldNodeIds = new Set(diffWith.nodes.map((n: PathwayNode) => n.id));

      pathway.nodes.forEach((n: PathwayNode) => {
        if (!oldNodeIds.has(n.id)) {
          const cell = graph.getCellById(n.id);
          if (cell) {
            cell.attr("body/stroke", "var(--mk-success)");
            cell.attr("body/strokeWidth", 3);
          }
        }
      });

      diffWith.nodes.forEach((n: PathwayNode) => {
        if (!pathway.nodes.some((pn: PathwayNode) => pn.id === n.id)) {
          graph.addNode({
            id: `deleted-${n.id}`,
            shape: "rect",
            x: n.x + 20,
            y: n.y + 20,
            width: NODE_WIDTH,
            height: NODE_HEIGHT,
            label: n.label,
            attrs: {
              body: { fill: "var(--mk-danger-soft)", stroke: "var(--mk-danger)", strokeWidth: 3, strokeDasharray: 5, rx: 6, ry: 6 },
              label: { fill: "var(--mk-danger)", fontSize: 13, textDecoration: "line-through" },
            },
          });
        }
      });
    }

    // Center content
    graph.centerContent();
  }, [pathway, mode, diffWith]);

  // Node selection
  useEffect(() => {
    const graph = graphRef.current;
    if (!graph) return;

    const handler = () => {
      const cells = graph.getSelectedCells();
      if (cells.length > 0 && onNodeSelect) {
        onNodeSelect(cells[0].id);
      }
    };

    graph.on("node:click", handler);
    graph.on("blank:click", () => {
      // Reset selection on blank click
      const cells = graph.getSelectedCells();
      cells.forEach((c) => c.removeAttrs());
    });

    return () => {
      graph.off("node:click", handler);
    };
  }, [onNodeSelect]);

  // Node move → onChange
  useEffect(() => {
    const graph = graphRef.current;
    if (!graph || isReadOnly) return;

    const handler = () => {
      if (!onChange || !pathway) return;
      const nodes = graph.getNodes().map((cell) => {
        const pos = cell.getPosition();
        const data = cell.getData() as PathwayNode;
        return { ...data, x: pos.x, y: pos.y };
      });
      onChange({ ...pathway, nodes });
    };

    graph.on("node:moved", handler);
    return () => {
      graph.off("node:moved", handler);
    };
  }, [onChange, pathway, isReadOnly]);

  // Edge added → onChange
  useEffect(() => {
    const graph = graphRef.current;
    if (!graph || isReadOnly) return;

    const handler = () => {
      if (!onChange || !pathway) return;
      const edges = graph.getEdges().map((cell) => {
        const source = cell.getSourceCellId();
        const target = cell.getTargetCellId();
        return {
          id: cell.id,
          source: source ?? "",
          target: target ?? "",
          label: cell.getLabels()[0]?.attrs?.label?.text as string | undefined,
        };
      });
      onChange({ ...pathway, edges: edges as PathwayEdge[] });
    };

    graph.on("edge:connected", handler);
    return () => {
      graph.off("edge:connected", handler);
    };
  }, [onChange, pathway, isReadOnly]);

  const handleZoomReset = useCallback(() => {
    const graph = graphRef.current;
    if (!graph) return;
    graph.zoomTo(1);
    graph.centerContent();
  }, []);

  const handleAutoLayout = useCallback(() => {
    const graph = graphRef.current;
    if (!graph) return;
    // Simple auto-layout: arrange nodes vertically
    const nodes = graph.getNodes();
    let yOffset = 40;
    const xOffset = 300;
    nodes.forEach((cell) => {
      cell.setPosition(xOffset, yOffset);
      yOffset += 80;
    });
    graph.centerContent();
  }, []);

  return (
    <div className="pathway-canvas-wrapper">
      <div ref={containerRef} className="pathway-canvas-container" />
      <div className="pathway-canvas-toolbar">
        <button type="button" className="canvas-toolbar-btn" onClick={handleZoomReset} title="重置缩放">
          100%
        </button>
        <button type="button" className="canvas-toolbar-btn" onClick={handleAutoLayout} title="自动布局">
          布局
        </button>
      </div>
    </div>
  );
};

export default PathwayCanvas;
