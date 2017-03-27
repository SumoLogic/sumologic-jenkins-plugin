package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.integration.SearchAction;
import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by lukasz on 3/21/17.
 */
@Extension(ordinal = -1)
public class LogListener extends ConsoleLogFilter {
  @Override
  public OutputStream decorateLogger(AbstractBuild abstractBuild, OutputStream outputStream)
    throws IOException, InterruptedException {

    PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();

    OutputStream stream = outputStream;

    if (pluginDescriptor.isTimestampingEnabled()) {
      stream = new TimestampingOutputStream(stream);
    }

    if (pluginDescriptor.isBuildLogEnabled()) {
      abstractBuild.addAction(new SearchAction(abstractBuild));
      stream = new SumologicOutputStream(
          stream, abstractBuild, pluginDescriptor);

    }

    return stream;
  }
}
