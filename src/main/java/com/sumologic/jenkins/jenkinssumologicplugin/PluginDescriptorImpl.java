package com.sumologic.jenkins.jenkinssumologicplugin;

import com.sumologic.jenkins.jenkinssumologicplugin.sender.SumoBuildNotifier;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Sumo Logic plugin for Jenkins model.
 * Provides options to parametrize plugin.
 *
 * Created by deven on 7/8/15.
 * Contributors: lukasz
 */
@Extension
public final class PluginDescriptorImpl extends BuildStepDescriptor<Publisher> {
  private final int MAX_LINES_DEFAULT = 2000;
  private String collectorUrl = "";
  private String maxLines = Integer.toString(MAX_LINES_DEFAULT);
  private String queryPortal = "service.sumologic.com";
  private boolean timestampingEnabled = true;
  private boolean buildLogEnabled = true;

  public PluginDescriptorImpl() {
    super(SumoBuildNotifier.class);
    load();
  }

  public static PluginDescriptorImpl getInstance() {
    return (PluginDescriptorImpl) Jenkins.getInstance().getDescriptor(SumoBuildNotifier.class);
  }

  @Override
  public boolean isApplicable(Class<? extends AbstractProject> aClass) {
    return true;
  }

  @Override
  public String getDisplayName() {
    return "Sumologic build logger";
  }

  @Override
  public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
    boolean configOk = super.configure(req, formData);
    collectorUrl = formData.getString("url");
    queryPortal = formData.getString("queryPortal");
    maxLines = formData.getString("maxLines");
    timestampingEnabled = formData.getBoolean("timestampingEnabled");
    buildLogEnabled = formData.getBoolean("buildLogEnabled");

    save();
    return configOk;
  }

  public FormValidation doCheckUrl(@QueryParameter String value) {
    if (value.isEmpty()) {
      return FormValidation.error("You must provide an URL.");
    }

    try {
      new URL(value);
    } catch (final MalformedURLException e) {
      return FormValidation.error("This is not a valid URL.");
    }

    return FormValidation.ok();
  }

  public FormValidation doCheckMaxLines(@QueryParameter String value) {
    if (value.isEmpty()) {
      return FormValidation.error("You must provide a value. Default is 200.");
    }
    int test = 0;
    try {
      test = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return FormValidation.error("Invalid input. Must be a number.");
    }

    if (test < -1 ) {
      return FormValidation.error("Invalid number. Must be non negative or -1 for batching a whole log.");
    }

    return FormValidation.ok();
  }


  public String getUrl() {
    return collectorUrl;
  }

  public void setUrl(String url) {
    this.collectorUrl = url;
  }

  public boolean isTimestampingEnabled() {
    return timestampingEnabled;
  }

  public void setTimestampingEnabled(boolean value) {
    timestampingEnabled = value;
  }

  public String getMaxLines() {
    return maxLines;
  }

  public void setMaxLines(String maxLines) {
    this.maxLines = maxLines;
  }

  public int getMaxLinesInt() {
    int value = 0;
    try {
      value = Integer.parseInt(maxLines);
    } catch (NumberFormatException e) {
      value = MAX_LINES_DEFAULT;
    }

    return value;
  }

  public String getQueryPortal() {
    return queryPortal;
  }

  public void setQueryPortal (String queryPortal) {
    this.queryPortal = queryPortal;
  }

  public boolean isBuildLogEnabled() {
    return buildLogEnabled;
  }

  public void setBuildLogEnabled(boolean value) {
    buildLogEnabled = value;
  }
}
