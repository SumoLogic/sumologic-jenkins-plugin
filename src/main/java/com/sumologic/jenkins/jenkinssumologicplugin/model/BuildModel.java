package com.sumologic.jenkins.jenkinssumologicplugin.model;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

/**
 * Created by deven on 7/10/15.
 *
 * Updated by Sourabh Jain 5/2019
 */
public class BuildModel {
  private String name;
  private String hudsonVersion;
  private String scm;
  private String result;
  private String description;
  private int number;
  private long start_time;
  private long duration;
  private String logType;
  private String user;
  private String jobStartTime;
  private String jobType;
  private float jobRunDuration;
  private String jobBuildURL;
  private String upstreamJobURL;
  private String triggerCauses;
  private String label;
  private String nodeName;
  private TestCaseModel testResult;
  private List<String> changeLogDetails;
  private Map<String, Object> jobMetaData;
  private List<PipelineStageModel> stages;
  private ErrorModel errorModel;

  public String toJson() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getHudsonVersion() {
    return hudsonVersion;
  }

  public void setHudsonVersion(String hudsonVersion) {
    this.hudsonVersion = hudsonVersion;
  }

  public String getScm() {
    return scm;
  }

  public void setScm(String scm) {
    this.scm = scm;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

  public long getStart() {
    return start_time;
  }

  public void setStart(long start_time) {
    this.start_time = start_time;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public long getStart_time() {
    return start_time;
  }

  public void setStart_time(long start_time) {
    this.start_time = start_time;
  }

  public String getLogType() {
    return logType;
  }

  public void setLogType(String logType) {
    this.logType = logType;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getJobStartTime() {
    return jobStartTime;
  }

  public void setJobStartTime(String jobStartTime) {
    this.jobStartTime = jobStartTime;
  }

  public String getJobType() {
    return jobType;
  }

  public void setJobType(String jobType) {
    this.jobType = jobType;
  }

  public float getJobRunDuration() {
    return jobRunDuration;
  }

  public void setJobRunDuration(float jobRunDuration) {
    this.jobRunDuration = jobRunDuration;
  }

  public String getJobBuildURL() {
    return jobBuildURL;
  }

  public void setJobBuildURL(String jobBuildURL) {
    this.jobBuildURL = jobBuildURL;
  }

  public String getUpstreamJobURL() {
    return upstreamJobURL;
  }

  public void setUpstreamJobURL(String upstreamJobURL) {
    this.upstreamJobURL = upstreamJobURL;
  }

  public String getTriggerCauses() {
    return triggerCauses;
  }

  public void setTriggerCauses(String triggerCauses) {
    this.triggerCauses = triggerCauses;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getNodeName() {
    return nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  public TestCaseModel getTestResult() {
    return testResult;
  }

  public void setTestResult(TestCaseModel testResult) {
    this.testResult = testResult;
  }

  public List<String> getChangeLogDetails() {
    return changeLogDetails;
  }

  public void setChangeLogDetails(List<String> changeLogDetails) {
    this.changeLogDetails = changeLogDetails;
  }

  public Map<String, Object> getJobMetaData() {
    return jobMetaData;
  }

  public void setJobMetaData(Map<String, Object> jobMetaData) {
    this.jobMetaData = jobMetaData;
  }

  public List<PipelineStageModel> getStages() {
    return stages;
  }

  public void setStages(List<PipelineStageModel> stages) {
    this.stages = stages;
  }

  public ErrorModel getErrorModel() {
    return errorModel;
  }

  public void setErrorModel(ErrorModel errorModel) {
    this.errorModel = errorModel;
  }
}
