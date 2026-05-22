package com.medkernel.pathway;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PathwayTemplateService {

        public Map<String, Object> diffPathway(String pathwayCode, String fromVersion, String toVersion,
                                              Map<String, Map<String, Object>> pathwayDrafts,
                                              Map<String, Map<String, Object>> publishedPathways) {
            Map<String, Object> fromConfig = loadVersionConfig(pathwayCode, fromVersion, pathwayDrafts, publishedPathways);
            Map<String, Object> toConfig = loadVersionConfig(pathwayCode, toVersion, pathwayDrafts, publishedPathways);
            if (fromConfig == null) {
                throw new IllegalArgumentException("pathway version not found: " + pathwayCode + "@" + fromVersion);
            }
            if (toConfig == null) {
                throw new IllegalArgumentException("pathway version not found: " + pathwayCode + "@" + toVersion);
            }

            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("pathway_code", pathwayCode);
            result.put("from_version", fromVersion);
            result.put("to_version", toVersion);

            List<Map<String, Object>> metadataChanges = new ArrayList<Map<String, Object>>();
            for (String field : new String[] {"pathway_name", "specialty_code", "disease_code", "description"}) {
                String fromValue = string(fromConfig.get(field), null);
                String toValue = string(toConfig.get(field), null);
                if (!equalsNullable(fromValue, toValue)) {
                    Map<String, Object> change = new LinkedHashMap<String, Object>();
                    change.put("field", field);
                    change.put("from", fromValue);
                    change.put("to", toValue);
                    metadataChanges.add(change);
                }
            }
            result.put("metadata_changes", metadataChanges);

            Map<String, Map<String, Object>> fromNodes = indexNodes(fromConfig);
            Map<String, Map<String, Object>> toNodes = indexNodes(toConfig);

            List<String> nodesAdded = new ArrayList<String>();
            List<String> nodesRemoved = new ArrayList<String>();
            List<Map<String, Object>> nodesModified = new ArrayList<Map<String, Object>>();

            for (String nodeCode : toNodes.keySet()) {
                if (!fromNodes.containsKey(nodeCode)) {
                    nodesAdded.add(nodeCode);
                }
            }
            for (String nodeCode : fromNodes.keySet()) {
                if (!toNodes.containsKey(nodeCode)) {
                    nodesRemoved.add(nodeCode);
                    continue;
                }
                Map<String, Object> nodeDiff = diffNode(nodeCode, fromNodes.get(nodeCode), toNodes.get(nodeCode));
                if (nodeDiff != null) {
                    nodesModified.add(nodeDiff);
                }
            }
            Collections.sort(nodesAdded);
            Collections.sort(nodesRemoved);

            result.put("nodes_added", nodesAdded);
            result.put("nodes_removed", nodesRemoved);
            result.put("nodes_modified", nodesModified);

            // 顶层 summary 让看板与 PR 评审能一眼看到变更规模。
            Map<String, Object> summary = new LinkedHashMap<String, Object>();
            summary.put("metadata_changed", metadataChanges.size());
            summary.put("nodes_added", nodesAdded.size());
            summary.put("nodes_removed", nodesRemoved.size());
            summary.put("nodes_modified", nodesModified.size());
            result.put("summary", summary);

            return result;
        }

        private Map<String, Object> loadVersionConfig(String pathwayCode, String versionNo,
                                                   Map<String, Map<String, Object>> pathwayDrafts,
                                                   Map<String, Map<String, Object>> publishedPathways) {
            if (versionNo == null || versionNo.trim().isEmpty() || "draft".equalsIgnoreCase(versionNo)) {
                return pathwayDrafts.get(pathwayCode);
            }
            return publishedPathways.get(pathwayKey(pathwayCode, versionNo));
        }

        @SuppressWarnings("unchecked")
        private Map<String, Map<String, Object>> indexNodes(Map<String, Object> config) {
            Map<String, Map<String, Object>> map = new LinkedHashMap<String, Map<String, Object>>();
            Object stages = config == null ? null : config.get("stages");
            if (!(stages instanceof java.util.Collection)) {
                return map;
            }
            for (Object stageObject : (java.util.Collection<?>) stages) {
                if (!(stageObject instanceof Map)) {
                    continue;
                }
                Object nodes = ((Map<String, Object>) stageObject).get("nodes");
                if (!(nodes instanceof java.util.Collection)) {
                    continue;
                }
                for (Object nodeObject : (java.util.Collection<?>) nodes) {
                    if (nodeObject instanceof Map) {
                        Map<String, Object> node = (Map<String, Object>) nodeObject;
                        String nodeCode = string(node.get("node_code"), null);
                        if (nodeCode != null) {
                            map.put(nodeCode, node);
                        }
                    }
                }
            }
            return map;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> diffNode(String nodeCode, Map<String, Object> fromNode, Map<String, Object> toNode) {
            List<Map<String, Object>> fieldChanges = new ArrayList<Map<String, Object>>();
            for (String field : new String[] {"node_name", "node_type", "owner_role", "expected_minutes"}) {
                String fromValue = string(fromNode.get(field), null);
                String toValue = string(toNode.get(field), null);
                if (!equalsNullable(fromValue, toValue)) {
                    Map<String, Object> change = new LinkedHashMap<String, Object>();
                    change.put("field", field);
                    change.put("from", fromValue);
                    change.put("to", toValue);
                    fieldChanges.add(change);
                }
            }

            Map<String, Map<String, Object>> fromTasks = indexByCode(fromNode.get("tasks"), "task_code");
            Map<String, Map<String, Object>> toTasks = indexByCode(toNode.get("tasks"), "task_code");
            List<String> tasksAdded = new ArrayList<String>();
            List<String> tasksRemoved = new ArrayList<String>();
            List<Map<String, Object>> tasksModified = new ArrayList<Map<String, Object>>();
            for (String code : toTasks.keySet()) {
                if (!fromTasks.containsKey(code)) {
                    tasksAdded.add(code);
                }
            }
            for (String code : fromTasks.keySet()) {
                if (!toTasks.containsKey(code)) {
                    tasksRemoved.add(code);
                    continue;
                }
                List<Map<String, Object>> taskFieldChanges = diffFields(fromTasks.get(code), toTasks.get(code),
                        new String[] {"task_name", "task_type", "required", "source.adapter_code", "source.query_code"});
                if (!taskFieldChanges.isEmpty()) {
                    Map<String, Object> taskDiff = new LinkedHashMap<String, Object>();
                    taskDiff.put("task_code", code);
                    taskDiff.put("fields", taskFieldChanges);
                    tasksModified.add(taskDiff);
                }
            }
            Collections.sort(tasksAdded);
            Collections.sort(tasksRemoved);

            Map<String, Map<String, Object>> fromTransitions = indexByCode(fromNode.get("transitions"), "to_node");
            Map<String, Map<String, Object>> toTransitions = indexByCode(toNode.get("transitions"), "to_node");
            List<String> transitionsAdded = new ArrayList<String>();
            List<String> transitionsRemoved = new ArrayList<String>();
            for (String code : toTransitions.keySet()) {
                if (!fromTransitions.containsKey(code)) {
                    transitionsAdded.add(code);
                }
            }
            for (String code : fromTransitions.keySet()) {
                if (!toTransitions.containsKey(code)) {
                    transitionsRemoved.add(code);
                }
            }
            Collections.sort(transitionsAdded);
            Collections.sort(transitionsRemoved);

            if (fieldChanges.isEmpty() && tasksAdded.isEmpty() && tasksRemoved.isEmpty()
                    && tasksModified.isEmpty() && transitionsAdded.isEmpty() && transitionsRemoved.isEmpty()) {
                return null;
            }

            Map<String, Object> nodeDiff = new LinkedHashMap<String, Object>();
            nodeDiff.put("node_code", nodeCode);
            nodeDiff.put("fields", fieldChanges);
            nodeDiff.put("tasks_added", tasksAdded);
            nodeDiff.put("tasks_removed", tasksRemoved);
            nodeDiff.put("tasks_modified", tasksModified);
            nodeDiff.put("transitions_added", transitionsAdded);
            nodeDiff.put("transitions_removed", transitionsRemoved);
            return nodeDiff;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Map<String, Object>> indexByCode(Object collection, String codeField) {
            Map<String, Map<String, Object>> map = new LinkedHashMap<String, Map<String, Object>>();
            if (!(collection instanceof java.util.Collection)) {
                return map;
            }
            for (Object item : (java.util.Collection<?>) collection) {
                if (item instanceof Map) {
                    Map<String, Object> entry = (Map<String, Object>) item;
                    String code = string(entry.get(codeField), null);
                    if (code != null) {
                        map.put(code, entry);
                    }
                }
            }
            return map;
        }

        private List<Map<String, Object>> diffFields(Map<String, Object> fromMap, Map<String, Object> toMap, String[] fields) {
            List<Map<String, Object>> changes = new ArrayList<Map<String, Object>>();
            for (String field : fields) {
                String fromValue = readNested(fromMap, field);
                String toValue = readNested(toMap, field);
                if (!equalsNullable(fromValue, toValue)) {
                    Map<String, Object> change = new LinkedHashMap<String, Object>();
                    change.put("field", field);
                    change.put("from", fromValue);
                    change.put("to", toValue);
                    changes.add(change);
                }
            }
            return changes;
        }

        @SuppressWarnings("unchecked")
        private String readNested(Map<String, Object> map, String path) {
            if (map == null) {
                return null;
            }
            String[] parts = path.split("\\.");
            Object current = map;
            for (String part : parts) {
                if (!(current instanceof Map)) {
                    return null;
                }
                current = ((Map<String, Object>) current).get(part);
            }
            return current == null ? null : String.valueOf(current);
        }

        private boolean equalsNullable(String left, String right) {
            if (left == null && right == null) {
                return true;
            }
            if (left == null || right == null) {
                return false;
            }
            return left.equals(right);
        }

    private String pathwayKey(String pathwayCode, String versionNo) {
        return pathwayCode + "::" + versionNo;
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }
}
