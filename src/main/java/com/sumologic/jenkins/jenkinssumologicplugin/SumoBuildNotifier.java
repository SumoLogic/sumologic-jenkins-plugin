package com.sumologic.jenkins.jenkinssumologicplugin;

import com.google.gson.Gson;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
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

  private CloseableHttpClient httpClient = null;

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
    httpClient = HttpClients.createDefault();
    String url = getDescriptor().getUrl();
    Gson gson = new Gson();

    if (url == null || url.trim().length() == 0) {
      listener.error("no sumologic url configured.");
      return;
    }

    String json = gson.toJson(BuildModelFactory.generateBuildModelFor(build));
    //listener.getLogger().println("Uploading build status to sumologic: " + json);

    HttpPost post = null;
    try {
      post = new HttpPost(url);
      post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
      HttpResponse response = httpClient.execute(post);
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        LOG.warning(String.format("Received HTTP error from Sumo Service: %d", statusCode));
      }
      //need to consume the body if you want to re-use the connection.
      EntityUtils.consume(response.getEntity());
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
