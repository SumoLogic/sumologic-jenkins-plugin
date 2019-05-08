package com.sumologic.jenkins.jenkinssumologicplugin.model;

public class AuditModel {

    private String userName;
    private String auditEventType;
    private String eventTime;
    private String userId;

    public AuditModel(String userName, String userId, String auditEventType, String eventTime) {
        this.userName = userName;
        this.auditEventType = auditEventType;
        this.eventTime = eventTime;
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getAuditEventType() {
        return auditEventType;
    }

    public String getEventTime() {
        return eventTime;
    }

    public String getUserId() {
        return userId;
    }
}
