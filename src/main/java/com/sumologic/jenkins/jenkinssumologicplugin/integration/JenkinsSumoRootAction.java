package com.sumologic.jenkins.jenkinssumologicplugin.integration;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import hudson.Extension;
import hudson.model.RootAction;

import javax.annotation.CheckForNull;

@Extension
public class JenkinsSumoRootAction implements RootAction {

    private final static String URL_TEMPLATE = "https://%s/ui/dashboard.html?k=%s";

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "/plugin/sumologic-publisher/sumologic.ico";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Jobs Overview";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return String.format(URL_TEMPLATE, PluginDescriptorImpl.getInstance().getQueryPortal(),
                PluginDescriptorImpl.getInstance().getJobOverviewId());
    }
}