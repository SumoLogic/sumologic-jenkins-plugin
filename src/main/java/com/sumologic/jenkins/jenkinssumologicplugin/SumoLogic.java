package com.sumologic.jenkins.jenkinssumologicplugin;

import jenkins.model.Jenkins;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.util.logging.Logger;


/**
 * Created by deven on 8/6/15.
 */
public class SumoLogic {

  private final static Logger LOG = Logger.getLogger(SumoLogic.class.getName());


  protected SumoDescriptorImpl getDescriptor(){
    return (SumoDescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(SumoBuildNotifier.class);
  }

  public void push (String msg){
    HttpClient httpClient = new HttpClient();
    String url = getDescriptor().getUrl();
    PostMethod post = null;

    try {
      post = new PostMethod(url);
      post.setRequestEntity(new StringRequestEntity(msg, HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8));
      httpClient.executeMethod(post);
      int statusCode = post.getStatusCode();
      if (statusCode != 200) {
        LOG.warning(String.format("Received HTTP error from Sumo Service: %d", statusCode));
      }
      //need to consume the body if you want to re-use the connection.
      post.releaseConnection();
    } catch (IOException e) {
      LOG.warning(String.format("Could not send log to Sumo Logic: %s", e.toString()));
      try {
        post.abort();
      } catch (Exception ignore) {
      }
    }
  }
}
