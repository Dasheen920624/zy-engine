package com.zyengine.dto;

public class PatientPathwayInstance {
    private String instanceId;
    private String patientId;
    private String encounterId;
    private String pathwayCode;
    private String versionNo;
    private String status;
    private String currentNodeCode;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public String getPathwayCode() {
        return pathwayCode;
    }

    public void setPathwayCode(String pathwayCode) {
        this.pathwayCode = pathwayCode;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentNodeCode() {
        return currentNodeCode;
    }

    public void setCurrentNodeCode(String currentNodeCode) {
        this.currentNodeCode = currentNodeCode;
    }
}

