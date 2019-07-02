package com.sumologic.jenkins.jenkinssumologicplugin.model;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Test Case result Model
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class TestCaseResultModel {

    private String className;
    private String testName;
    private String errorStackTrace;
    private String errorDetails;
    private String status;
    private float duration;

    public void setClassName(String className) {
        this.className = className;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public String getClassName() {
        return className;
    }

    public String getTestName() {
        return testName;
    }

    public String getErrorStackTrace() {
        return errorStackTrace;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public String getStatus() {
        return status;
    }

    public float getDuration() {
        return duration;
    }
}
