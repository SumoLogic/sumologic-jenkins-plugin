package com.sumologic.jenkins.jenkinssumologicplugin.model;

public class BaseModel {

    private String logType;
    private String eventTime;

    public BaseModel(){

    }

    public BaseModel(String logType, String eventTime) {
        this.logType = logType;
        this.eventTime = eventTime;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }
}
