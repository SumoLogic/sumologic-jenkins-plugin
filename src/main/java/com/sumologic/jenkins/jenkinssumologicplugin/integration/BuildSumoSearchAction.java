package com.sumologic.jenkins.jenkinssumologicplugin.integration;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import hudson.model.Action;
import hudson.model.Run;

import javax.annotation.CheckForNull;
import java.util.logging.Logger;

/**
 * Sourabh Jain
 * <p>
 * This action is added to each build happening in Jenkins. It adds a dashboard for that Build in personal folder of
 * SumoLogic Directory
 */
public class BuildSumoSearchAction implements Action {

    private static final Logger LOG = Logger.getLogger(BuildSumoSearchAction.class.getName());

    private String jobName;
    private String buildNumber;

    private final static String URL_TEMPLATE = "https://%s/ui/dashboard.html?k=%s&filters=Name*eq*%s**Number*eq*%s**StageStatus*eq*%s***Master*eq*null";

    /**
     * perform all rest api call and add the dashboard to Sumologic for build.
     */
    public BuildSumoSearchAction(Run build) {
        jobName = build.getParent().getFullName();
        buildNumber = String.valueOf(build.getNumber());
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "/plugin/sumologic-publisher/sumologic.ico";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Build Dashboard";
    }

    @CheckForNull
    @Override
    /*
     * create just the URL to reach the specific dashboard.
     */
    public String getUrlName() {
        return String.format(URL_TEMPLATE, PluginDescriptorImpl.getInstance().getQueryPortal(),
                PluginDescriptorImpl.getInstance().getBuildDashboardId(), jobName, buildNumber, "%5C");
    }
}
