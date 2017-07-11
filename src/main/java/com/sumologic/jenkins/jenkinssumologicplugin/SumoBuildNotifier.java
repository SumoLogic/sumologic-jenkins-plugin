package com.sumologic.jenkins.jenkinssumologicplugin;

import com.google.gson.Gson;
import com.sumologic.jenkins.jenkinssumologicplugin.model.ModelFactory;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSender;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This publisher will sendLogs build metadata to a Sumo Logic HTTP collector.
 *
 * Created by deven on 7/6/15.
 */
public class SumoBuildNotifier implements jenkins.tasks.SimpleBuildStep {

  private final static Logger LOG = Logger.getLogger(SumoBuildNotifier.class.getName());
  private static LogSender logSender = LogSender.getInstance();

  @DataBoundConstructor
  public SumoBuildNotifier() {
    super();
  }

  /*@SuppressWarnings("unchecked")
  public static SumoBuildNotifier getNotifier(AbstractProject project) {
    Map<Descriptor<Publisher>, Publisher> map = project.getPublishersList().toMap();
    for (Publisher publisher : map.values()) {
      if (publisher instanceof SumoBuildNotifier) {
        return (SumoBuildNotifier) publisher;
      }
    }

    return null;
  }*/

  @Override
  public boolean prebuild(AbstractBuild<?, ?> abstractBuild, BuildListener buildListener) {
    return false;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    send(build, listener);
    return true;
  }

  @Override
  public Action getProjectAction(AbstractProject<?, ?> abstractProject) {
    return null;
  }

  @Override
  public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> abstractProject) {
    return null;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  protected void send(AbstractBuild build, TaskListener listener) {
    Gson gson = new Gson();
    String json = gson.toJson(ModelFactory.createBuildModel(build));

    PluginDescriptorImpl descriptor = PluginDescriptorImpl.getInstance();

    LOG.info("Uploading build status to sumologic: " + json);

    String url = descriptor.getUrl();
    String sourceName = descriptor.getSourceNameJobStatus();
    String category = descriptor.getSourceCategoryJobStatus();
    byte[] bytes = json.getBytes();

    logSender.sendLogs(url, bytes, sourceName, category);
  }

  @Override
  public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {

  }
}
