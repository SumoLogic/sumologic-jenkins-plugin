package com.sumologic.jenkins.jenkinssumologicplugin.constants;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Log Type Enum
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */

public enum LogTypeEnum {

    JOB_STATUS("Job_Status"),
    AUDIT_EVENT("Audit_Event"),
    QUEUE_EVENT("Queue_Event"),
    SLAVE_EVENT("Slave_Event"),
    SCM_STATUS("Scm_Status"),
    JENKINS_LOG("Jenkins_Log");
    private String value;

    LogTypeEnum(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
