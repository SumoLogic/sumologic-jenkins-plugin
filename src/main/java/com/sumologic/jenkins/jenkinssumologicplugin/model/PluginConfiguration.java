package com.sumologic.jenkins.jenkinssumologicplugin.model;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;

import java.io.Serializable;

public class PluginConfiguration implements Serializable {
    protected static final long serialVersionUID = 1L;

    private String sumoLogicEndpoint;
    private String queryPortal;
    private String sourceCategory;
    private String metricDataPrefix;
    private boolean auditLogEnabled;
    private boolean keepOldConfigData;
    private boolean metricDataEnabled;
    private boolean periodicLogEnabled;
    private boolean jobStatusLogEnabled;
    private boolean jobConsoleLogEnabled;
    private boolean scmLogEnabled;

    public PluginConfiguration(PluginDescriptorImpl pluginDescriptor) {
        this.sumoLogicEndpoint = pluginDescriptor.getUrl();
        this.queryPortal = pluginDescriptor.getQueryPortal();
        this.sourceCategory = pluginDescriptor.getSourceCategory();
        this.metricDataPrefix = pluginDescriptor.getMetricDataPrefix();
        this.auditLogEnabled = pluginDescriptor.isAuditLogEnabled();
        this.keepOldConfigData = pluginDescriptor.isKeepOldConfigData();
        this.metricDataEnabled = pluginDescriptor.isMetricDataEnabled();
        this.periodicLogEnabled = pluginDescriptor.isPeriodicLogEnabled();
        this.jobStatusLogEnabled = pluginDescriptor.isJobStatusLogEnabled();
        this.jobConsoleLogEnabled = pluginDescriptor.isJobConsoleLogEnabled();
        this.scmLogEnabled = pluginDescriptor.isScmLogEnabled();
    }

    public String getSumoLogicEndpoint() {
        return sumoLogicEndpoint;
    }

    public void setSumoLogicEndpoint(String sumoLogicEndpoint) {
        this.sumoLogicEndpoint = sumoLogicEndpoint;
    }

    public String getQueryPortal() {
        return queryPortal;
    }

    public void setQueryPortal(String queryPortal) {
        this.queryPortal = queryPortal;
    }

    public String getSourceCategory() {
        return sourceCategory;
    }

    public void setSourceCategory(String sourceCategory) {
        this.sourceCategory = sourceCategory;
    }

    public String getMetricDataPrefix() {
        return metricDataPrefix;
    }

    public void setMetricDataPrefix(String metricDataPrefix) {
        this.metricDataPrefix = metricDataPrefix;
    }

    public boolean isAuditLogEnabled() {
        return auditLogEnabled;
    }

    public void setAuditLogEnabled(boolean auditLogEnabled) {
        this.auditLogEnabled = auditLogEnabled;
    }

    public boolean isKeepOldConfigData() {
        return keepOldConfigData;
    }

    public void setKeepOldConfigData(boolean keepOldConfigData) {
        this.keepOldConfigData = keepOldConfigData;
    }

    public boolean isMetricDataEnabled() {
        return metricDataEnabled;
    }

    public void setMetricDataEnabled(boolean metricDataEnabled) {
        this.metricDataEnabled = metricDataEnabled;
    }

    public boolean isPeriodicLogEnabled() {
        return periodicLogEnabled;
    }

    public void setPeriodicLogEnabled(boolean periodicLogEnabled) {
        this.periodicLogEnabled = periodicLogEnabled;
    }

    public boolean isJobStatusLogEnabled() {
        return jobStatusLogEnabled;
    }

    public void setJobStatusLogEnabled(boolean jobStatusLogEnabled) {
        this.jobStatusLogEnabled = jobStatusLogEnabled;
    }

    public boolean isJobConsoleLogEnabled() {
        return jobConsoleLogEnabled;
    }

    public void setJobConsoleLogEnabled(boolean jobConsoleLogEnabled) {
        this.jobConsoleLogEnabled = jobConsoleLogEnabled;
    }

    public boolean isScmLogEnabled() {
        return scmLogEnabled;
    }

    public void setScmLogEnabled(boolean scmLogEnabled) {
        this.scmLogEnabled = scmLogEnabled;
    }
}
