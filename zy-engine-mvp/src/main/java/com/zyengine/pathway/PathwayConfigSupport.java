package com.zyengine.pathway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PathwayConfigSupport {
    public List<String> validate(Map<String, Object> config) {
        List<String> errors = new ArrayList<String>();
        require(config, "pathway_code", errors);
        require(config, "pathway_name", errors);
        if (versionNo(config, null) == null) {
            errors.add("version or version_no is required");
        }

        Object stages = config == null ? null : config.get("stages");
        if (!(stages instanceof Collection) || ((Collection<?>) stages).isEmpty()) {
            errors.add("stages must contain at least one stage");
            return errors;
        }

        Set<String> nodeCodes = new LinkedHashSet<String>();
        Set<String> duplicatedNodeCodes = new LinkedHashSet<String>();
        Set<String> taskKeys = new HashSet<String>();
        List<Map<String, Object>> nodes = allNodes(config);
        if (nodes.isEmpty()) {
            errors.add("stages.nodes must contain at least one node");
        }

        for (Map<String, Object> node : nodes) {
            String nodeCode = string(node.get("node_code"), null);
            if (nodeCode == null) {
                errors.add("node_code is required");
                continue;
            }
            if (!nodeCodes.add(nodeCode)) {
                duplicatedNodeCodes.add(nodeCode);
            }
            if (string(node.get("node_name"), null) == null) {
                errors.add("node_name is required: " + nodeCode);
            }
            for (Map<String, Object> task : maps(node.get("tasks"))) {
                String taskCode = string(task.get("task_code"), null);
                if (taskCode == null) {
                    errors.add("task_code is required: " + nodeCode);
                    continue;
                }
                if (!taskKeys.add(nodeCode + "::" + taskCode)) {
                    errors.add("duplicate task_code in node " + nodeCode + ": " + taskCode);
                }
                validateTaskSource(nodeCode, taskCode, task, errors);
            }
        }

        for (String duplicated : duplicatedNodeCodes) {
            errors.add("duplicate node_code: " + duplicated);
        }
        for (Map<String, Object> node : nodes) {
            String nodeCode = string(node.get("node_code"), null);
            for (Map<String, Object> transition : maps(node.get("transitions"))) {
                String target = string(transition.get("to_node"), null);
                if (target == null) {
                    errors.add("transition.to_node is required: " + nodeCode);
                } else if (!nodeCodes.contains(target)) {
                    errors.add("transition target not found: " + nodeCode + " -> " + target);
                }
            }
        }
        return errors;
    }

    public String versionNo(Map<String, Object> config, String defaultValue) {
        String version = string(config == null ? null : config.get("version_no"), null);
        if (version == null) {
            version = string(config == null ? null : config.get("version"), null);
        }
        return version == null ? defaultValue : version;
    }

    public String firstNodeCode(Map<String, Object> config) {
        Map<String, Object> firstNode = firstNode(config);
        return firstNode == null ? null : string(firstNode.get("node_code"), null);
    }

    public String nodeName(Map<String, Object> config, String nodeCode) {
        Map<String, Object> node = findNode(config, nodeCode);
        return node == null ? nodeCode : string(node.get("node_name"), nodeCode);
    }

    public List<Map<String, Object>> nodeTasks(Map<String, Object> config, String nodeCode) {
        Map<String, Object> node = findNode(config, nodeCode);
        if (node == null) {
            return new ArrayList<Map<String, Object>>();
        }
        return maps(node.get("tasks"));
    }

    public Map<String, Object> nodeTask(Map<String, Object> config, String nodeCode, String taskCode) {
        for (Map<String, Object> task : nodeTasks(config, nodeCode)) {
            if (taskCode.equals(string(task.get("task_code"), null))) {
                return task;
            }
        }
        return null;
    }

    public String nextNodeCode(Map<String, Object> config, String currentNodeCode) {
        Map<String, Object> currentNode = findNode(config, currentNodeCode);
        if (currentNode == null) {
            return null;
        }

        List<Map<String, Object>> transitions = maps(currentNode.get("transitions"));
        Collections.sort(transitions, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                return integer(left.get("priority"), 100).compareTo(integer(right.get("priority"), 100));
            }
        });
        for (Map<String, Object> transition : transitions) {
            String next = string(transition.get("to_node"), null);
            if (next != null) {
                return next;
            }
        }
        return null;
    }

    public Map<String, Object> nodeReferenceInfo(Map<String, Object> config, String nodeCode) {
        Map<String, Object> node = findNode(config, nodeCode);
        Map<String, Object> ref = new java.util.LinkedHashMap<String, Object>();
        if (node == null) {
            return ref;
        }
        ref.put("reference_document_code", string(node.get("reference_document_code"), null));
        ref.put("reference_citation_id", string(node.get("reference_citation_id"), null));
        ref.put("reference_binding_type", string(node.get("reference_binding_type"), null));
        return ref;
    }

    public List<Map<String, Object>> collectNodeReferences(Map<String, Object> config) {
        List<Map<String, Object>> references = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> node : allNodes(config)) {
            String nodeCode = string(node.get("node_code"), null);
            if (nodeCode == null) {
                continue;
            }
            String docCode = string(node.get("reference_document_code"), null);
            if (docCode != null) {
                Map<String, Object> ref = new java.util.LinkedHashMap<String, Object>();
                ref.put("element_type", "NODE");
                ref.put("element_code", nodeCode);
                ref.put("reference_document_code", docCode);
                ref.put("reference_citation_id", string(node.get("reference_citation_id"), null));
                ref.put("reference_binding_type", string(node.get("reference_binding_type"), null));
                references.add(ref);
            }
            for (Map<String, Object> transition : maps(node.get("transitions"))) {
                String transDocCode = string(transition.get("reference_document_code"), null);
                if (transDocCode != null) {
                    Map<String, Object> ref = new java.util.LinkedHashMap<String, Object>();
                    ref.put("element_type", "TRANSITION");
                    ref.put("element_code", nodeCode + "->" + string(transition.get("to_node"), "?"));
                    ref.put("reference_document_code", transDocCode);
                    ref.put("reference_citation_id", string(transition.get("reference_citation_id"), null));
                    ref.put("reference_binding_type", string(transition.get("reference_binding_type"), null));
                    references.add(ref);
                }
            }
        }
        return references;
    }

    public List<Map<String, Object>> collectMissingReferences(Map<String, Object> config) {
        List<Map<String, Object>> warnings = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> node : allNodes(config)) {
            String nodeCode = string(node.get("node_code"), null);
            if (nodeCode == null) {
                continue;
            }
            String docCode = string(node.get("reference_document_code"), null);
            if (docCode == null) {
                Map<String, Object> warning = new java.util.LinkedHashMap<String, Object>();
                warning.put("element_type", "NODE");
                warning.put("element_code", nodeCode);
                warning.put("field", "reference_document_code");
                warning.put("severity", "WARN");
                warning.put("message", "节点缺少来源文档绑定（reference_document_code）");
                warnings.add(warning);
            }
        }
        return warnings;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> findNode(Map<String, Object> config, String nodeCode) {
        if (config == null || nodeCode == null) {
            return null;
        }
        for (Map<String, Object> node : allNodes(config)) {
            if (nodeCode.equals(string(node.get("node_code"), null))) {
                return node;
            }
        }
        return null;
    }

    private Map<String, Object> firstNode(Map<String, Object> config) {
        List<Map<String, Object>> nodes = allNodes(config);
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> allNodes(Map<String, Object> config) {
        List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
        if (config == null) {
            return nodes;
        }
        // 路径配置按“阶段 -> 节点”组织，运行时统一展开为节点列表，便于查找和流转。
        Object stages = config.get("stages");
        if (!(stages instanceof Collection)) {
            return nodes;
        }
        for (Object stageObject : (Collection<?>) stages) {
            if (!(stageObject instanceof Map)) {
                continue;
            }
            Object stageNodes = ((Map<String, Object>) stageObject).get("nodes");
            nodes.addAll(maps(stageNodes));
        }
        return nodes;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> maps(Object value) {
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                if (item instanceof Map) {
                    maps.add((Map<String, Object>) item);
                }
            }
        }
        return maps;
    }

    @SuppressWarnings("unchecked")
    private void validateTaskSource(String nodeCode, String taskCode, Map<String, Object> task, List<String> errors) {
        Object source = task.get("source");
        if (source == null) {
            return;
        }
        if (!(source instanceof Map)) {
            errors.add("task source must be object: " + nodeCode + "/" + taskCode);
            return;
        }
        Map<String, Object> sourceMap = (Map<String, Object>) source;
        if (string(sourceMap.get("adapter_code"), null) == null) {
            errors.add("source.adapter_code is required: " + nodeCode + "/" + taskCode);
        }
        if (string(sourceMap.get("query_code"), null) == null) {
            errors.add("source.query_code is required: " + nodeCode + "/" + taskCode);
        }
    }

    private void require(Map<String, Object> config, String field, List<String> errors) {
        if (string(config == null ? null : config.get(field), null) == null) {
            errors.add(field + " is required");
        }
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    private Integer integer(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
