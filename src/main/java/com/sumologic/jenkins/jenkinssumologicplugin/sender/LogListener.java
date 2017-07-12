package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.integration.SearchAction;
import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Run;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * This class intercepts console output stream for every jenkins build and decorates it with additional functionality.
 * Depending on configuration it can add two functionalities:
 * 1) Add consistent timestamps to all output lines.
 * 2) Forward all output logs to Sumo Logic.
 *
 * Created by lukasz on 3/21/17.
 */
@Extension(ordinal = -1)
public class LogListener extends ConsoleLogFilter {
  private final static Logger LOG = Logger.getLogger(LogSender.class.getName());

  private String buildName;
  private String buildNumber;

  public LogListener(){
    super();
  }

  public LogListener(String buildName, String buildNumber) {
    this.buildName = buildName;
    this.buildNumber = buildNumber;
  }

  @Override
  public OutputStream decorateLogger(AbstractBuild abstractBuild, OutputStream outputStream)
    throws IOException, InterruptedException {
    return decorateLogger((Run) abstractBuild, outputStream);
  }

  @Override
  public OutputStream decorateLogger(Run build, OutputStream outputStream) throws IOException, InterruptedException {
    PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();
    OutputStream stream = outputStream;

    if (pluginDescriptor.isBuildLogEnabled()) {
      if (build != null) {
        build.addAction(new SearchAction(build));
        stream = new SumologicOutputStream(
            stream, build, pluginDescriptor);

      }
      else {
        stream = new SumologicOutputStream(
            stream, buildName, buildNumber, pluginDescriptor);
      }
    }

    return stream;
  }

  @Override
  public OutputStream decorateLogger(@Nonnull Computer computer, OutputStream logger) throws IOException, InterruptedException {
    return logger;
  }

}
