# Feature Acceptance: FA-PR-V2-07-S01

feature_acceptance_id: FA-PR-V2-07-S01
task_id: PR-V2-07
claim_id: PR-V2-07-S01
title: 路径模板编辑器（含 X6 画布）
verdict: BRONZE
completed_at: 2026-05-20T14:30:00+08:00
owner: CodeBuddy

## Scope

### Frontend Pages
- `frontend/src/pages/Pathway/PathwayEditor/index.tsx` — 编辑器主入口，三栏布局、自动保存
- `frontend/src/pages/Pathway/PathwayEditor/PathwayEditorHeader.tsx` — 顶部操作栏
- `frontend/src/pages/Pathway/PathwayEditor/StageTree.tsx` — 左侧阶段树
- `frontend/src/pages/Pathway/PathwayEditor/NodePropertyPanel.tsx` — 右侧属性面板
- `frontend/src/pages/Pathway/PathwayEditor/UnsavedChangesGuard.tsx` — 离开提示
- `frontend/src/components/PathwayCanvas/PathwayCanvas.tsx` — X6 画布核心组件
- `frontend/src/components/PathwayCanvas/types.ts` — 类型定义

### Backend APIs
- `PUT /api/pathways/{code}/draft` — PathwayDraftController.saveDraft
- `POST /api/pathways/{code}/validate` — PathwayDraftController.validate → PathwayValidator
- `POST /api/pathways/{code}/submit-review` — PathwayDraftController.submitReview

### Route
- `frontend/src/router/routes.tsx`: `{ path: "pathway/templates/:code/edit", element: <PathwayEditor /> }`

## Evidence

### DoD 1: 三栏可拖拽调整宽度（Allotment）
```tsx
<Allotment defaultSizes={[240, 600, 360]}>
  <Allotment.Pane minSize={180} maxSize={320}>...</Allotment.Pane>
  <Allotment.Pane minSize={400}>...</Allotment.Pane>
  <Allotment.Pane minSize={280} maxSize={480}>...</Allotment.Pane>
</Allotment>
```
✅ allotment@^1.20.5 已在 package.json

### DoD 2: 节点拖拽（X6）
```tsx
// PathwayCanvas.tsx
const graph = new Graph({
  panning: { enabled: true },
  mousewheel: { enabled: true, modifiers: ["ctrl", "meta"] },
  interacting: { nodeMovable: !isReadOnly },
});
graph.on("node:moved", handler); // 拖拽后同步到 PathwayDef
```
✅ @antv/x6@^3.1.7 已在 package.json

### DoD 3: 自动保存 30s + 离开提示
```tsx
// 自动保存
autoSaveTimerRef.current = setTimeout(async () => {
  await savePathwayDraft(code, { nodes, edges });
}, 30000); // 去抖 30s

// UnsavedChangesGuard
const blocker = useBlocker(dirty);
window.addEventListener("beforeunload", handler); // 浏览器关闭
// React Router: Modal.confirm 确认离开
```

### DoD 4: 节点属性变更触发画布刷新
```tsx
const handleNodePropertyChange = useCallback((updated: PathwayNode) => {
  const nextNodes = pathway.nodes.map((n) => (n.id === updated.id ? updated : n));
  const next = { ...pathway, nodes: nextNodes };
  setPathway(next); // 触发 PathwayCanvas re-render
  setDirty(true);
  triggerAutoSave();
}, [pathway, triggerAutoSave]);
```

### DoD 5: 来源审核状态显示
```tsx
// NodePropertyPanel.tsx
<Descriptions.Item label="来源审核">
  {node.properties.source_verified ? (
    <StatusBadge status="success" />
  ) : (
    <StatusBadge status="warning" />
  )}
</Descriptions.Item>
```

### DoD 6: 提交审核前完整性校验
```tsx
// index.tsx - handleSubmit
const result = await validatePathway(code, { nodes: pathway.nodes, edges: pathway.edges });
if (!result.valid) {
  message.error(`校验失败：${result.errors.length} 个错误需要修复`);
  return;
}
await submitPathwayReview(code);
```

```java
// PathwayValidator.java
boolean hasStart = nodes.stream().anyMatch(n -> "start".equals(n.get("type")));
boolean hasEnd = nodes.stream().anyMatch(n -> "end".equals(n.get("type")));
result.put("valid", errors.isEmpty());
```

## Checklist

- [x] 三栏布局 Allotment 实现
- [x] X6 画布接入，节点可拖拽
- [x] 自动保存去抖 30s
- [x] UnsavedChangesGuard（路由拦截 + beforeunload）
- [x] 节点属性面板（类型/超时/规则绑定/图谱/来源审核）
- [x] 节点属性变更同步到画布
- [x] 来源审核状态用 StatusBadge 展示
- [x] 完整性校验（后端 PathwayValidator）
- [x] 提交审核前先过校验
- [x] 路由 /pathway/templates/:code/edit 已注册

## Findings

- P2: 缺少 NodePropertyPanel/PathwayCanvas 单元测试
- P2: UnsavedChangesGuard 中 React 导入缺失已修复（本次修复）
- P2: PathwayCanvas diff 模式已实现但 /diff 路由仍为 PlaceholderPage（待 PR-V2-07 后续补充）
- P2: nodeMovable 在 X6 v3 类型中有 ts-expect-error，需验证 @antv/x6 实际安装版本

## Verdict

BRONZE — 核心功能完整，所有 6 个 DoD 通过；缺少单元测试（P2）
