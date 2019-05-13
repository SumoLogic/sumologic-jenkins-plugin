package com.sumologic.jenkins.jenkinssumologicplugin.model;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

public class AuditModel extends BaseModel{

    private String userName;
    private String auditEventType;
    private String userId;
    private String message;
    private Map<String, Object> fileDetails;

    public AuditModel(String userName, String userId, String auditEventType, String eventTime
            , String message, String logType, Map<String, Object> fileDetails) {
        super(logType, eventTime);
        this.userName = userName;
        this.auditEventType = auditEventType;
        this.userId = userId;
        this.message = message;
        this.fileDetails = fileDetails;
    }

    public String getUserName() {
        return userName;
    }

    public String getAuditEventType() {
        return auditEventType;
    }

    public String getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getFileDetails() {
        return fileDetails;
    }

    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
