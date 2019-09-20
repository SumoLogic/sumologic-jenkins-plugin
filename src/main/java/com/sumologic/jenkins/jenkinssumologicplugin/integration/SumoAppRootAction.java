package com.sumologic.jenkins.jenkinssumologicplugin.integration;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import hudson.Extension;
import hudson.model.RootAction;

import javax.annotation.CheckForNull;

@Extension
public class SumoAppRootAction implements RootAction {

    private final static String URL_TEMPLATE = "https://%s/ui/#/library/folder/%s";

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "/plugin/sumologic-publisher/sumologic.ico";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Jenkins Sumo App";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return String.format(URL_TEMPLATE, PluginDescriptorImpl.getInstance().getQueryPortal(),
                PluginDescriptorImpl.getInstance().getAppFolderId());
    }
}