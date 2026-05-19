package com.medkernel.pathway;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PathwayValidator {

    @SuppressWarnings("unchecked")
    public Map<String, Object> validate(Map<String, Object> draft) {
        List<Map<String, Object>> errors = new ArrayList<>();
        List<Map<String, Object>> warnings = new ArrayList<>();

        List<Map<String, Object>> nodes = (List<Map<String, Object>>) draft.get("nodes");
        List<Map<String, Object>> edges = (List<Map<String, Object>>) draft.get("edges");

        if (nodes == null || nodes.isEmpty()) {
            errors.add(issue("STRUCTURE", "路径必须包含至少一个节点", null, null));
            return result(errors, warnings);
        }

        // Check for start node
        boolean hasStart = nodes.stream().anyMatch(n -> "start".equals(n.get("type")));
        if (!hasStart) {
            errors.add(issue("MISSING_START", "路径必须包含开始节点", null, null));
        }

        // Check for end node
        boolean hasEnd = nodes.stream().anyMatch(n -> "end".equals(n.get("type")));
        if (!hasEnd) {
            errors.add(issue("MISSING_END", "路径必须包含结束节点", null, null));
        }

        // Check for orphan nodes (no incoming or outgoing edges)
        if (edges != null) {
            for (Map<String, Object> node : nodes) {
                String nodeId = (String) node.get("id");
                String nodeType = (String) node.get("type");
                if ("start".equals(nodeType) || "end".equals(nodeType)) continue;

                boolean hasIncoming = edges.stream().anyMatch(e -> nodeId.equals(e.get("target")));
                boolean hasOutgoing = edges.stream().anyMatch(e -> nodeId.equals(e.get("source")));

                if (!hasIncoming && !hasOutgoing) {
                    warnings.add(issue("ORPHAN_NODE", "节点 '" + node.get("label") + "' 没有连接", nodeId, null));
                } else if (!hasIncoming) {
                    warnings.add(issue("NO_INCOMING", "节点 '" + node.get("label") + "' 没有入边", nodeId, null));
                } else if (!hasOutgoing) {
                    warnings.add(issue("NO_OUTGOING", "节点 '" + node.get("label") + "' 没有出边", nodeId, null));
                }
            }
        }

        // Check task nodes for required properties
        for (Map<String, Object> node : nodes) {
            if ("task".equals(node.get("type"))) {
                Map<String, Object> props = (Map<String, Object>) node.get("properties");
                if (props == null || props.get("timeout_minutes") == null) {
                    warnings.add(issue("MISSING_TIMEOUT", "任务 '" + node.get("label") + "' 缺少超时设置",
                            (String) node.get("id"), null));
                }
                if (props != null && !Boolean.TRUE.equals(props.get("source_verified"))) {
                    warnings.add(issue("SOURCE_NOT_VERIFIED", "任务 '" + node.get("label") + "' 来源未审核",
                            (String) node.get("id"), null));
                }
            }
        }

        return result(errors, warnings);
    }

    private Map<String, Object> issue(String code, String message, String nodeId, String edgeId) {
        Map<String, Object> issue = new HashMap<>();
        issue.put("code", code);
        issue.put("message", message);
        issue.put("severity", code.startsWith("MISSING_START") || code.startsWith("MISSING_END")
                || code.startsWith("STRUCTURE") ? "error" : "warning");
        if (nodeId != null) issue.put("nodeId", nodeId);
        if (edgeId != null) issue.put("edgeId", edgeId);
        return issue;
    }

    private Map<String, Object> result(List<Map<String, Object>> errors, List<Map<String, Object>> warnings) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("warnings", warnings);
        return result;
    }
}
