package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.integration.SearchAction;
import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Run;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * This class intercepts console output stream for every jenkins build and decorates it with additional functionality.
 * Depending on configuration it can add two functionalities:
 * 1) Add consistent timestamps to all output lines.
 * 2) Forward all output logs to Sumo Logic.
 * <p>
 * Created by lukasz on 3/21/17.
 * <p>
 * Modified By Sourabh Jain 5/2019
 */
@Extension(ordinal = -1)
public class LogListener extends ConsoleLogFilter implements Serializable {
    private final static Logger LOG = Logger.getLogger(LogSender.class.getName());

    private transient Run run;
    private SumologicOutputStream.State streamState;


    public LogListener() {
        super();
        this.streamState = new SumologicOutputStream.State();
    }

    public LogListener(Run build) {
        this();
        this.run = build;
    }


    @Override
    public OutputStream decorateLogger(AbstractBuild abstractBuild, OutputStream outputStream)
            throws IOException, InterruptedException {
        return decorateLogger((Run) abstractBuild, outputStream);
    }

    @Override
    public OutputStream decorateLogger(Run build, OutputStream outputStream) throws IOException, InterruptedException {
        PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();
        if (pluginDescriptor.isBuildLogEnabled()) {
            if (build != null) {
                build.addAction(new SearchAction(build));
                return new SumologicOutputStream(outputStream, build, pluginDescriptor, streamState);
            } else {
                return new SumologicOutputStream(outputStream, run, pluginDescriptor, streamState);
            }
        }

        return outputStream;
    }

    @Override
    public OutputStream decorateLogger(@Nonnull Computer computer, OutputStream logger) throws IOException, InterruptedException {
        return logger;
    }
}
