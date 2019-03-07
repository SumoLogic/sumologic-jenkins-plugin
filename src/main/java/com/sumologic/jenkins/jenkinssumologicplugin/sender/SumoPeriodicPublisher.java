package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.model.ModelFactory;
import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by deven on 8/6/15.
 *
 * Periodically publish jenkins system metadata to sumo
 */
@Extension
public class SumoPeriodicPublisher extends AsyncPeriodicWork {
  protected static final long recurrencePeriod = TimeUnit.MINUTES.toMillis(3);
  private static final Logger LOGGER = Logger.getLogger(SumoPeriodicPublisher.class.getName());
  private LogSender logSender;

  public SumoPeriodicPublisher() {
    super("Sumo Logic Periodic Data Publisher");
    logSender = LogSender.getInstance();
    LOGGER.log(Level.FINE, "Sumo Logic status publishing period is {0}ms", recurrencePeriod);
  }

  @Override
  protected void execute(TaskListener listener) throws IOException, InterruptedException {
    String logs = ModelFactory.createJenkinsModel(Jenkins.getInstance()).toJson();

    PluginDescriptorImpl descriptor = PluginDescriptorImpl.getInstance();
    String url = descriptor.getUrl();

    // Proxy settings
    boolean enableProxy = descriptor.getEnableProxy();
    String proxyHost = descriptor.getProxyHost();
    int proxyPort = descriptor.getProxyPort();
    boolean enableProxyAuth = descriptor.getEnableProxyAuth();
    String proxyAuthUsername = descriptor.getProxyAuthUsername();
    String proxyAuthPassword = descriptor.getProxyAuthPassword();
    
    if(enableProxy) {
      logSender.setHttpClientProxy(proxyHost, proxyPort, enableProxyAuth, proxyAuthUsername, proxyAuthPassword);
    }

    logSender.sendLogs(url,logs.getBytes(),
        descriptor.getSourceNamePeriodic(), descriptor.getSourceCategoryPeriodic());
  }

  @Override
  public long getRecurrencePeriod() {
    return recurrencePeriod;
  }
}
