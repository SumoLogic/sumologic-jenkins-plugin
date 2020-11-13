package com.sumologic.jenkins.jenkinssumologicplugin;

import com.google.gson.Gson;
import com.sumologic.jenkins.jenkinssumologicplugin.integration.SearchAction;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.GENERATION_ERROR;
import static com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory.sendConsoleLogs;

/**
 * This publisher will sendLogs build metadata to a Sumo Logic HTTP collector.
 * <p>
 * Created by deven on 7/6/15.
 * <p>
 * Modified by Sourabh Jain 5/2019
 */
public class SumoBuildNotifier extends Notifier implements SimpleBuildStep {

    private final static Logger LOG = Logger.getLogger(SumoBuildNotifier.class.getName());
    private static final LogSender logSender = LogSender.getInstance();

    @DataBoundConstructor
    public SumoBuildNotifier() {
        super();
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

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        try {
            send(build, null);
        } catch (Exception e) {
            String errorMessage = GENERATION_ERROR + e.getMessage();
            LOG.log(Level.WARNING, errorMessage, e);
            listener.error(errorMessage);
        }
        return true;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) {
        try {
            send(run, taskListener);
        } catch (Exception e) {
            String errorMessage = GENERATION_ERROR + e.getMessage();
            LOG.log(Level.WARNING, errorMessage, e);
            taskListener.error(errorMessage);
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    protected void send(Run build, TaskListener taskListener) {
        try {
            PluginDescriptorImpl descriptor = PluginDescriptorImpl.getInstance();
            Gson gson = new Gson();
            BuildModel buildModel = ModelFactory.createBuildModel(build, descriptor);
            String json = gson.toJson(buildModel);


            String url = descriptor.getUrl();
            String category = descriptor.getSourceCategory();
            byte[] bytes = json.getBytes();
            if (StringUtils.isNotEmpty(buildModel.getJobType())) {
                if (!descriptor.isJobStatusLogEnabled()) {
                    LOG.info("Uploading build status to sumologic: " + json);
                    logSender.sendLogs(url, bytes, null, category);
                }
                if (!descriptor.isJobConsoleLogEnabled()) {
                    build.addAction(new SearchAction(build));
                    sendConsoleLogs(build, taskListener);
                }
            }
        } catch (Exception e) {
            String errorMessage = GENERATION_ERROR + e.getMessage();
            LOG.log(Level.WARNING, errorMessage, e);
            taskListener.error(errorMessage);
        }
    }

    @Override
    public PluginDescriptorImpl getDescriptor() {
        return (PluginDescriptorImpl) super.getDescriptor();
    }
}
