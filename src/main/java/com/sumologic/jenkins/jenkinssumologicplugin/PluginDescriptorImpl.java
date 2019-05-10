package com.sumologic.jenkins.jenkinssumologicplugin;

import com.sumologic.jenkins.jenkinssumologicplugin.pluginextension.metrics.SumoMetricDataPublisher;
import com.sumologic.jenkins.jenkinssumologicplugin.pluginextension.metrics.SumoMetricReporter;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.init.TermMilestone;
import hudson.init.Terminator;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
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

  private String sourceNamePeriodic = "jenkinsStatus";
  private String sourceNameJobStatus = "jenkinsJobStatus";

  private String sourceCategoryPeriodic = "jenkinsStatus";
  private String sourceCategoryJobStatus = "jenkinsJobStatus";
  private String sourceCategoryBuildLogs = "jenkinsBuildLogs";

  private boolean buildLogEnabled = true;

  private transient SumoMetricDataPublisher sumoMetricDataPublisher;

  public PluginDescriptorImpl() {
    super(SumoBuildNotifier.class);
    load();
    sumoMetricDataPublisher = new SumoMetricDataPublisher();
    getSumoMetricDataPublisher().publishMetricData();
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
    return "Sumo Logic build logger";
  }

  @Override
  public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
    boolean configOk = super.configure(req, formData);
    collectorUrl = formData.getString("url");
    queryPortal = formData.getString("queryPortal");
    maxLines = formData.getString("maxLines");
    buildLogEnabled = formData.getBoolean("buildLogEnabled");

    sourceNamePeriodic = formData.getString("sourceNamePeriodic");
    sourceNameJobStatus = formData.getString("sourceNameJobStatus");
    sourceCategoryPeriodic = formData.getString("sourceCategoryPeriodic");
    sourceCategoryJobStatus = formData.getString("sourceCategoryJobStatus");
    sourceCategoryBuildLogs = formData.getString("sourceCategoryBuildLogs");

    save();
    getSumoMetricDataPublisher().publishMetricData();
    return configOk;
  }

  @Terminator(after = TermMilestone.STARTED)
  @Restricted(NoExternalUse.class)
  public static void shutdown(){
    PluginDescriptorImpl pluginDescriptor = checkIfPluginInUse();
    pluginDescriptor.getSumoMetricDataPublisher().stopReporter();
  }

  private SumoMetricDataPublisher getSumoMetricDataPublisher() {
    return sumoMetricDataPublisher;
  }

  private static PluginDescriptorImpl checkIfPluginInUse(){
    PluginDescriptorImpl pluginDescriptor = ExtensionList.lookup(BuildStepDescriptor.class).get(PluginDescriptorImpl.class);
    if(pluginDescriptor == null){
        throw new IllegalStateException("Sumo Logic Publisher is not part of the extension list");
    }
    return pluginDescriptor;
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

  public String getSourceNamePeriodic() {
    return sourceNamePeriodic;
  }

  public void setSourceNamePeriodic(String sourceNamePeriodic) {
    this.sourceNamePeriodic = sourceNamePeriodic;
  }

  public String getSourceNameJobStatus() {
    return sourceNameJobStatus;
  }

  public void setSourceNameJobStatus(String sourceNameJobStatus) {
    this.sourceNameJobStatus = sourceNameJobStatus;
  }

  public String getSourceCategoryPeriodic() {
    return sourceCategoryPeriodic;
  }

  public void setSourceCategoryPeriodic(String sourceCategoryPeriodic) {
    this.sourceCategoryPeriodic = sourceCategoryPeriodic;
  }

  public String getSourceCategoryJobStatus() {
    return sourceCategoryJobStatus;
  }

  public void setSourceCategoryJobStatus(String sourceCategoryJobStatus) {
    this.sourceCategoryJobStatus = sourceCategoryJobStatus;
  }

  public String getSourceCategoryBuildLogs() {
    return sourceCategoryBuildLogs;
  }

  public void setSourceCategoryBuildLogs(String sourceCategoryBuildLogs) {
    this.sourceCategoryBuildLogs = sourceCategoryBuildLogs;
  }
}
