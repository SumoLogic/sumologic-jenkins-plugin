package com.sumologic.jenkins.jenkinssumologicplugin;

import com.google.gson.Gson;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.http.protocol.HTTP;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by deven on 7/6/15.
 */
public class SumoBuildNotifier extends Notifier {

  private final static Logger LOG = Logger.getLogger(SumoBuildNotifier.class.getName());

  private int connectionTimeout = 1000;
  private int socketTimeout = 60000;

  private HttpClient httpClient = null;

  @DataBoundConstructor
  public SumoBuildNotifier(){
    super();

  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    send(build, listener);
    return true;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }


  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  protected void send (AbstractBuild build, TaskListener listener) {
    httpClient = new HttpClient();
    String url = getDescriptor().getUrl();
    Gson gson = new Gson();

    if (url == null || url.trim().length() == 0) {
      listener.error("no sumologic url configured.");
      return;
    }

    String json = gson.toJson(BuildModelFactory.generateBuildModelFor(build));
    //listener.getLogger().println("Uploading build status to sumologic: " + json);

    PostMethod post = null;
    try {
      post = new PostMethod(url);
      post.setRequestEntity(new StringRequestEntity(json, HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8));
      httpClient.executeMethod(post);
      int statusCode = post.getStatusCode();
      if (statusCode != 200) {
        LOG.warning(String.format("Received HTTP error from Sumo Service: %d", statusCode));
      }
      //need to consume the body if you want to re-use the connection.
      post.releaseConnection();
    } catch (IOException e) {
      LOG.warning(String.format("Could not send log to Sumo Logic: %s", e.toString()));
      try { post.abort(); } catch (Exception ignore) {}
    }

  }


  @SuppressWarnings("unchecked")
  public static SumoBuildNotifier getNotifier(AbstractProject project) {
    Map<Descriptor<Publisher>, Publisher> map = project.getPublishersList().toMap();
    for (Publisher publisher : map.values()) {
      if (publisher instanceof SumoBuildNotifier) {
        return (SumoBuildNotifier) publisher;
      }
    }
    return null;
  }


}
