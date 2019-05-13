package com.sumologic.jenkins.jenkinssumologicplugin.model;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Base Model
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class BaseModel {

    private String logType;
    private String eventTime;

    public BaseModel() {

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
