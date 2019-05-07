package com.sumologic.jenkins.jenkinssumologicplugin.constants;

/**
 * Sumo Logic plugin for Jenkins model.
 *
 * Log Type Enum
 *
 * Created by Sourabh Jain on 5/2019.
 */

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
