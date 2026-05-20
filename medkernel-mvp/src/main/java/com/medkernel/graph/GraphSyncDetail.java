package com.medkernel.graph;

import java.time.LocalDateTime;

/**
 * 图谱同步任务明细实体：记录每个同步项的结果。
 * 对应表 OPS_SYNC_TASK_DETAIL，GRAPH-005 核心交付。
 */
public class GraphSyncDetail {
    private Long id;
    private String tenantId;
    private Long taskId;
    private String itemType;
    private String itemCode;
    private String operation;
    private String status;
    private String errorMessage;
    private String neo4jNodeId;
    private LocalDateTime createdTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getNeo4jNodeId() { return neo4jNodeId; }
    public void setNeo4jNodeId(String neo4jNodeId) { this.neo4jNodeId = neo4jNodeId; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
