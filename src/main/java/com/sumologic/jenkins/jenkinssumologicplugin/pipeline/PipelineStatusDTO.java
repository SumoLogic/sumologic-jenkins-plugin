package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import hudson.model.Result;

import java.util.List;
import java.util.Map;

public class PipelineStatusDTO {

    private String logType;
    private int buildNumber;
    private String jobName;
    private String user;
    private String jobStartTime;
    private String jobType;
    private float jobRunDuration;
    private String jobResult;
    private String jobBuildURL;
    private String upstreamJobURL;
    private String triggerCauses;
    private String label;
    private String nodeName;
    private TestCaseDTO testResult;
    private List<String> changeLogDetails;
    private List<PipelineStageDTO> stages;
    private Map<String, Object> pipelineMetaData;

    void setLogType(String logType) {
        this.logType = logType;
    }

    void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }

    void setTriggerCauses(String triggerCauses) {
        this.triggerCauses = triggerCauses;
    }

    void setUser(String user) {
        this.user = user;
    }

    void setJobBuildURL(String jobBuildURL) {
        this.jobBuildURL = jobBuildURL;
    }

    void setUpstreamJobURL(String upstreamJobURL) {
        this.upstreamJobURL = upstreamJobURL;
    }

    void setJobName(String jobName) {
        this.jobName = jobName;
    }

    void setJobStartTime(String jobStartTime) {
        this.jobStartTime = jobStartTime;
    }

    void setJobType(String jobType) {
        this.jobType = jobType;
    }

    void setLabel(String label) {
        this.label = label;
    }

    void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    void setJobRunDuration(float jobRunDuration) {
        this.jobRunDuration = jobRunDuration;
    }

    void setJobResult(String jobResult) {
        this.jobResult = jobResult;
    }

    void setTestResult(TestCaseDTO testResult) {
        this.testResult = testResult;
    }

    void setChangeLogDetails(List<String> changeLogDetails) {
        this.changeLogDetails = changeLogDetails;
    }

    void setStages(List<PipelineStageDTO> stages) {
        this.stages = stages;
    }

    void setPipelineMetaData(Map<String, Object> pipelineMetaData) {
        this.pipelineMetaData = pipelineMetaData;
    }

    public String getLogType() {
        return logType;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public String getJobName() {
        return jobName;
    }

    public String getUser() {
        return user;
    }

    public String getJobStartTime() {
        return jobStartTime;
    }

    public String getJobType() {
        return jobType;
    }

    public float getJobRunDuration() {
        return jobRunDuration;
    }

    public String getJobResult() {
        return jobResult;
    }

    public String getJobBuildURL() {
        return jobBuildURL;
    }

    public String getUpstreamJobURL() {
        return upstreamJobURL;
    }

    public String getTriggerCauses() {
        return triggerCauses;
    }

    public String getLabel() {
        return label;
    }

    public String getNodeName() {
        return nodeName;
    }

    public TestCaseDTO getTestResult() {
        return testResult;
    }

    public List<String> getChangeLogDetails() {
        return changeLogDetails;
    }

    public List<PipelineStageDTO> getStages() {
        return stages;
    }

    public Map<String, Object> getPipelineMetaData() {
        return pipelineMetaData;
    }
}
