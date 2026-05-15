package com.zyengine.pathway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PathwayConfigSupport {
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

    public String nextNodeCode(Map<String, Object> config, String currentNodeCode) {
        Map<String, Object> currentNode = findNode(config, currentNodeCode);
        if (currentNode == null) {
            return null;
        }

        // P1阶段先按配置中的优先级选择第一个流转目标；后续会把condition接入规则上下文判断。
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
