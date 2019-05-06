package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

public enum LogTypeEnum {

    JOB_STATUS("Job_Status");

    private String value;

    LogTypeEnum(final String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
