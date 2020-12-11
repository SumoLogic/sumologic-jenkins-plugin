package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.Computer;
import hudson.model.Run;

import javax.annotation.Nonnull;
import java.io.OutputStream;
import java.io.Serializable;

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

    private static final long serialVersionUID = -131194971357180671L;

    private transient Run run;


    public LogListener() {
        super();
    }

    public LogListener(Run build) {
        this();
        this.run = build;
        //build.addAction(new SearchAction(build));
    }

    @Override
    public OutputStream decorateLogger(Run build, OutputStream outputStream) {
        /*try {
            PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();
            if (!pluginDescriptor.isJobConsoleLogEnabled()) {
                if (build != null) {
                    build.addAction(new SearchAction(build));
                    return new SumologicOutputStream(outputStream, build, pluginDescriptor, streamState);
                } else {
                    return new SumologicOutputStream(outputStream, run, pluginDescriptor, streamState);
                }
            }
        } catch (Exception e) {
            String errorMessage = CONSOLE_ERROR + Arrays.toString(e.getStackTrace());
            LOG.log(Level.WARNING, errorMessage);
            outputStream.write(errorMessage.getBytes());
        }*/
        return outputStream;
    }

    @Override
    public OutputStream decorateLogger(@Nonnull Computer computer, OutputStream logger) {
        return logger;
    }
}
