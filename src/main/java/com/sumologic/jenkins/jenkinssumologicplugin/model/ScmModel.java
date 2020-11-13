package com.sumologic.jenkins.jenkinssumologicplugin.model;

import com.google.gson.Gson;

import java.util.List;

public class ScmModel extends BaseModel {

    private int buildNumber;
    private String jobName;
    private String scmType;
    private String scmURLs;
    private String revision;
    private String branches;
    private List<String> changeLog;

    public int getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getScmType() {
        return scmType;
    }

    public void setScmType(String scmType) {
        this.scmType = scmType;
    }

    public String getScmURLs() {
        return scmURLs;
    }

    public void setScmURLs(String scmURLs) {
        this.scmURLs = scmURLs;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getBranches() {
        return branches;
    }

    public void setBranches(String branches) {
        this.branches = branches;
    }

    public List<String> getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(List<String> changeLog) {
        this.changeLog = changeLog;
    }

    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
