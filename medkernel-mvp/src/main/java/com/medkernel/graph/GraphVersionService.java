package com.medkernel.graph;

import com.medkernel.audit.PublishGateService;
import com.medkernel.common.exception.MissingSourceException;
import com.medkernel.persistence.EnginePersistenceService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GraphVersionService {

    private final EnginePersistenceService persistenceService;
    private final PublishGateService publishGateService;

    public GraphVersionService(EnginePersistenceService persistenceService,
                               PublishGateService publishGateService) {
        this.persistenceService = persistenceService;
        this.publishGateService = publishGateService;
    }

        public List<Map<String, Object>> importGraphVersions(Object request,
                                                           Map<String, Map<String, Object>> graphVersions) {
            return importGraphVersions(request, graphVersions, "default");
        }

        public List<Map<String, Object>> importGraphVersions(Object request,
                                                           Map<String, Map<String, Object>> graphVersions,
                                                           String tenantId) {
            List<Map<String, Object>> entries = normalize(request, "versions", "graph_version");
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("graph versions list is empty");
            }
            List<String> errors = new ArrayList<String>();
            List<Map<String, Object>> staged = new ArrayList<Map<String, Object>>();
            for (int index = 0; index < entries.size(); index++) {
                try {
                    staged.add(toVersionEntry(entries.get(index)));
                } catch (IllegalArgumentException ex) {
                    errors.add("versions[" + index + "]: " + ex.getMessage());
                }
            }
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("graph versions invalid: " + errors);
            }

            List<Map<String, Object>> imported = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> entry : staged) {
                entry.put("tenant_id", tenantId);
                graphVersions.put(versionKey(tenantId, (String) entry.get("graph_version")), entry);
                imported.add(entry);
            }
            return imported;
        }

        public List<Map<String, Object>> listGraphVersions(Map<String, Map<String, Object>> graphVersions) {
            return listGraphVersions(graphVersions, "default");
        }

        public List<Map<String, Object>> listGraphVersions(Map<String, Map<String, Object>> graphVersions,
                                                           String tenantId) {
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> entry : graphVersions.values()) {
                if (matchesTenant(entry, tenantId)) {
                    list.add(entry);
                }
            }
            Collections.sort(list, new Comparator<Map<String, Object>>() {
                @Override
                public int compare(Map<String, Object> left, Map<String, Object> right) {
                    return String.valueOf(left.get("graph_version")).compareTo(String.valueOf(right.get("graph_version")));
                }
            });
            return list;
        }

        public Map<String, Object> getGraphVersion(String graphVersion,
                                                 Map<String, Map<String, Object>> graphVersions) {
            return getGraphVersion(graphVersion, graphVersions, "default");
        }

        public Map<String, Object> getGraphVersion(String graphVersion,
                                                 Map<String, Map<String, Object>> graphVersions,
                                                 String tenantId) {
            // 先尝试租户键
            Map<String, Object> entry = graphVersions.get(versionKey(tenantId, graphVersion));
            if (entry != null) {
                return entry;
            }
            // 回退到旧格式键
            entry = graphVersions.get(graphVersion);
            if (entry == null) {
                throw new IllegalArgumentException("graph version not found: " + graphVersion);
            }
            return entry;
        }

        public Map<String, Object> activateGraphVersion(String graphVersion, Map<String, Object> request,
                                                      Map<String, Map<String, Object>> graphVersions,
                                                      java.util.concurrent.locks.ReentrantLock graphVersionLock) {
            return activateGraphVersion(graphVersion, request, graphVersions, graphVersionLock, "default");
        }

        public Map<String, Object> activateGraphVersion(String graphVersion, Map<String, Object> request,
                                                      Map<String, Map<String, Object>> graphVersions,
                                                      java.util.concurrent.locks.ReentrantLock graphVersionLock,
                                                      String tenantId) {
            graphVersionLock.lock();
            try {
                // 先尝试租户键，回退旧格式键
                Map<String, Object> entry = graphVersions.get(versionKey(tenantId, graphVersion));
                if (entry == null) {
                    entry = graphVersions.get(graphVersion);
                }
                if (entry == null) {
                    throw new IllegalArgumentException("graph version not found: " + graphVersion);
                }

                String refDoc = string(entry.get("reference_document_code"), null);
                PublishGateService.GateCheckResult gateResult = publishGateService.checkGraphReference(graphVersion, refDoc);
                String operatorId = string(request == null ? null : request.get("published_by"), "SYSTEM");
                publishGateService.auditGateCheck("GRAPH", "ACTIVATE", "GRAPH_VERSION", graphVersion, operatorId, gateResult);
                if (!gateResult.isReadyToPublish()) {
                    throw new MissingSourceException(publishGateService.formatBlockingMessage(gateResult));
                }

                // 同一版本号即唯一键，激活时把所有同 family 前缀（::之前）的其他版本置 RETIRED，便于多版本共存时切换。
                String family = versionFamily(graphVersion);
                for (Map<String, Object> other : graphVersions.values()) {
                    String otherVersion = String.valueOf(other.get("graph_version"));
                    if (!otherVersion.equals(graphVersion) && versionFamily(otherVersion).equals(family)
                            && "ACTIVE".equals(other.get("status"))) {
                        other.put("status", "RETIRED");
                        other.put("retired_time", nowText());
                    }
                }
                entry.put("status", "ACTIVE");
                entry.put("published_by", operatorId);
                entry.put("published_time", nowText());
                entry.put("reference_warnings", gateResult.toMapList());

                // 审计日志：图谱版本激活操作
                Map<String, Object> auditDetail = new LinkedHashMap<String, Object>();
                auditDetail.put("graph_version", graphVersion);
                auditDetail.put("action", "ACTIVATE");
                auditDetail.put("operator_id", operatorId);
                auditDetail.put("gate_check_ready", gateResult.isReadyToPublish());
                persistenceService.saveAuditLog("GRAPH", "ACTIVATE", "GRAPH_VERSION", graphVersion, null, null, operatorId, auditDetail);

                return entry;
            } finally {
                graphVersionLock.unlock();
            }
        }

        public Map<String, Object> rollbackVersion(String graphVersion, Map<String, Object> request,
                                                 Map<String, Map<String, Object>> graphVersions,
                                                 java.util.concurrent.locks.ReentrantLock graphVersionLock) {
            return rollbackVersion(graphVersion, request, graphVersions, graphVersionLock, "default");
        }

        public Map<String, Object> rollbackVersion(String graphVersion, Map<String, Object> request,
                                                 Map<String, Map<String, Object>> graphVersions,
                                                 java.util.concurrent.locks.ReentrantLock graphVersionLock,
                                                 String tenantId) {
            // 与 activateGraphVersion 共享 graphVersionLock：回滚也是 read-modify-write 序列，
            // 必须互斥防止并发回滚导致多版本同时 ACTIVE。
            graphVersionLock.lock();
            try {
                // 先尝试租户键，回退旧格式键
                Map<String, Object> target = graphVersions.get(versionKey(tenantId, graphVersion));
                if (target == null) {
                    target = graphVersions.get(graphVersion);
                }
                if (target == null) {
                    throw new IllegalArgumentException("graph version not found: " + graphVersion);
                }

                String previousActiveVersion = null;
                for (Map<String, Object> existing : graphVersions.values()) {
                    if ("ACTIVE".equals(string(existing.get("status"), null))) {
                        previousActiveVersion = string(existing.get("graph_version"), null);
                        existing.put("status", "RETIRED");
                        existing.put("retired_time", nowText());
                    }
                }

                target.put("status", "ACTIVE");
                String operatorId = string(request == null ? null : request.get("published_by"), "SYSTEM");
                target.put("published_by", operatorId);
                target.put("published_time", nowText());

                Map<String, Object> result = new LinkedHashMap<String, Object>();
                result.put("graph_version", graphVersion);
                result.put("status", "ACTIVE");
                result.put("previous_active_version", previousActiveVersion);
                result.put("rolled_back_by", operatorId);
                result.put("rolled_back_time", nowText());

                // 审计日志：图谱版本回滚操作
                Map<String, Object> auditDetail = new LinkedHashMap<String, Object>();
                auditDetail.put("graph_version", graphVersion);
                auditDetail.put("action", "ROLLBACK");
                auditDetail.put("previous_active_version", previousActiveVersion);
                auditDetail.put("operator_id", operatorId);
                persistenceService.saveAuditLog("GRAPH", "ROLLBACK", "GRAPH_VERSION", graphVersion, null, null, operatorId, auditDetail);

                return result;
            } finally {
                graphVersionLock.unlock();
            }
        }

        @SuppressWarnings("unchecked")
        private List<Map<String, Object>> normalize(Object request, String nestedKey, String singleHintField) {
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            if (request instanceof List) {
                for (Object item : (List<?>) request) {
                    if (item instanceof Map) {
                        list.add((Map<String, Object>) item);
                    }
                }
                return list;
            }
            if (request instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) request;
                Object nested = map.get(nestedKey);
                if (nested instanceof List) {
                    return normalize(nested, nestedKey, singleHintField);
                }
                if (map.containsKey(singleHintField)) {
                    list.add(map);
                }
            }
            return list;
        }

        private Map<String, Object> toVersionEntry(Map<String, Object> entry) {
            String graphVersion = requireField(entry, "graph_version");
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("graph_version", graphVersion);
            view.put("name", string(entry.get("name"), graphVersion));
            view.put("status", canonical(string(entry.get("status"), "DRAFT")));
            view.put("description", string(entry.get("description"), null));
            view.put("source_uri", string(entry.get("source_uri"), null));
            view.put("reference_document_code", string(entry.get("reference_document_code"), null));
            view.put("reference_binding_type", string(entry.get("reference_binding_type"), null));
            view.put("published_by", string(entry.get("published_by"), null));
            view.put("published_time", string(entry.get("published_time"), null));
            view.put("created_time", nowText());
            return view;
        }

        private String versionFamily(String graphVersion) {
            if (graphVersion == null) {
                return "";
            }
            int idx = graphVersion.indexOf("::");
            return idx > 0 ? graphVersion.substring(0, idx) : graphVersion.replaceAll("_[0-9]{4,}_[0-9]{2}.*$", "");
        }

        private String canonical(String value) {
            return value == null ? "" : value.trim().toUpperCase();
        }

        private String requireField(Map<String, Object> entry, String field) {
            String value = string(entry.get(field), null);
            if (value == null) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value;
        }

        private String nowText() {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
        }

        private String string(Object value, String defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            String text = String.valueOf(value);
            return text.trim().isEmpty() ? defaultValue : text;
        }

        private String versionKey(String tenantId, String graphVersion) {
            return tenantId + "::" + string(graphVersion, "");
        }

        private boolean matchesTenant(Map<String, Object> entry, String tenantId) {
            String entryTenant = string(entry.get("tenant_id"), null);
            if (entryTenant == null) {
                return true; // 旧数据视为所有租户可见
            }
            return tenantId.equals(entryTenant);
        }
}
