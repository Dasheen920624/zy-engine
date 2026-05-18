package com.medkernel.dto;

import java.util.ArrayList;
import java.util.List;

public class PatientNodeState {
    private String instanceId;
    private String nodeCode;
    private String nodeName;
    private String status;
    private String enterTime;
    private String completeTime;
    private boolean timeoutFlag;
    private List<PatientTaskState> tasks = new ArrayList<PatientTaskState>();

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getNodeCode() {
        return nodeCode;
    }

    public void setNodeCode(String nodeCode) {
        this.nodeCode = nodeCode;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEnterTime() {
        return enterTime;
    }

    public void setEnterTime(String enterTime) {
        this.enterTime = enterTime;
    }

    public String getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(String completeTime) {
        this.completeTime = completeTime;
    }

    public boolean isTimeoutFlag() {
        return timeoutFlag;
    }

    public void setTimeoutFlag(boolean timeoutFlag) {
        this.timeoutFlag = timeoutFlag;
    }

    public List<PatientTaskState> getTasks() {
        return tasks;
    }

    public void setTasks(List<PatientTaskState> tasks) {
        this.tasks = tasks == null ? new ArrayList<PatientTaskState>() : tasks;
    }
}
