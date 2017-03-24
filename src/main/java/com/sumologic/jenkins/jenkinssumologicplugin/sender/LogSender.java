package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;


/**
 * Created by deven on 8/6/15.
 */
public class LogSender {
  private final static Logger LOG = Logger.getLogger(LogSender.class.getName());
  private static LogSender INSTANCE = null;

  private final HttpClient httpClient;

  private LogSender() {
    MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    httpClient = new HttpClient(connectionManager);
  }

  protected String getHost() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e)
    {
      LOG.warning("Couldn't resolve jenkins host name... Using unknown.");
    }

    return "unkown";
  }

  public static LogSender getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new LogSender();
    }

    return INSTANCE;
  }

  public void sendLogs(String url, byte[] msg, String sumoName, String sumoCategory){
    PostMethod post = null;

    try {
      post = new PostMethod(url);
      post.addRequestHeader("X-Sumo-Name", sumoName);
      post.addRequestHeader("X-Sumo-Host", getHost());
      post.addRequestHeader("X-Sumo-Category", sumoCategory);

      post.setRequestEntity(new ByteArrayRequestEntity(msg));
      httpClient.executeMethod(post);
      int statusCode = post.getStatusCode();
      if (statusCode != 200) {
        LOG.warning(String.format("Received HTTP error from Sumo Service: %d", statusCode));
      }
    } catch (IOException e) {
      LOG.warning(String.format("Could not send log to Sumo Logic: %s", e.toString()));
    }
    finally {
      if (post != null) {
        post.releaseConnection();
      }
    }

  }
}
