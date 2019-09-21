package com.sumologic.jenkins.jenkinssumologicplugin.integration;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import hudson.model.Action;
import hudson.model.Job;

import javax.annotation.CheckForNull;
import java.util.logging.Logger;

public class JobSumoSearchAction implements Action {

    private static final Logger LOG = Logger.getLogger(JobSumoSearchAction.class.getName());

    private String jobName;

    private final static String URL_TEMPLATE = "https://%s/ui/dashboard.html?k=%s&filters=Name*eq*%s**Master*eq*null";

    JobSumoSearchAction(final Job job) {
        jobName = job.getFullName();
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "/plugin/sumologic-publisher/sumologic.ico";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Job Dashboard";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return String.format(URL_TEMPLATE, PluginDescriptorImpl.getInstance().getQueryPortal(),
                PluginDescriptorImpl.getInstance().getJobDashboardId(), jobName);
    }
}
