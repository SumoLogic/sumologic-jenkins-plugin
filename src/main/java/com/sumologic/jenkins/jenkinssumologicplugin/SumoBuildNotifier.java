package com.sumologic.jenkins.jenkinssumologicplugin;

import com.google.gson.Gson;
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
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.GENERATION_ERROR;
import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.GRAPHITE_CONTENT_TYPE;

/**
 * This publisher will sendLogs build metadata to a Sumo Logic HTTP collector.
 * <p>
 * Created by deven on 7/6/15.
 * <p>
 * Modified by Sourabh Jain 5/2019
 */
public class SumoBuildNotifier extends Notifier implements SimpleBuildStep {

    private final static Logger LOG = Logger.getLogger(SumoBuildNotifier.class.getName());
    private static LogSender logSender = LogSender.getInstance();

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

    public static SumoBuildNotifier getInstance() {
        return new SumoBuildNotifier();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        try {
            send(build);
        } catch (Exception e) {
            String errorMessage = GENERATION_ERROR + Arrays.toString(e.getStackTrace());
            LOG.log(Level.WARNING, errorMessage);
            listener.error(errorMessage);
        }
        return true;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        try {
            send(run);
        } catch (Exception e) {
            String errorMessage = GENERATION_ERROR + Arrays.toString(e.getStackTrace());
            LOG.log(Level.WARNING, errorMessage);
            taskListener.error(errorMessage);
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public void send(Run build) {
        Gson gson = new Gson();
        String json = gson.toJson(ModelFactory.createBuildModel(build));
        send(json);
    }

    public void send(final BuildModel buildModel) {
        Gson gson = new Gson();
        String json = gson.toJson(buildModel);
        send(json);
    }

    public void send(final List<String> messages) {
        PluginDescriptorImpl descriptor = PluginDescriptorImpl.getInstance();

        //Divide messages into equal parts
        int timesTheLoopShouldRun = messages.size() / 100 + 1;
        for (int i = 0; i < timesTheLoopShouldRun; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            int start = i+100*i;
            int end = Math.min(100+100*i, messages.size());
            for (int j = start; j < end; j++) {
                stringBuilder.append(messages.get(j)).append("\n");
            }
            //LOG.info("Uploading Metric data to SumoLogic "+stringBuilder.toString());
            logSender.sendLogs(descriptor.getUrl(), stringBuilder.toString().getBytes(), "MetricData", "Labs/Jenkins/MetricsData", GRAPHITE_CONTENT_TYPE);
        }
    }

    public void send(String json) {
        PluginDescriptorImpl descriptor = PluginDescriptorImpl.getInstance();

        LOG.info("Uploading build status to sumologic: " + json);

        String url = descriptor.getUrl();
        String sourceName = descriptor.getSourceNameJobStatus();
        String category = descriptor.getSourceCategoryJobStatus();
        byte[] bytes = json.getBytes();

        logSender.sendLogs(url, bytes, sourceName, category);
    }

    @Override
    public PluginDescriptorImpl getDescriptor() {
        PluginDescriptorImpl result = (PluginDescriptorImpl) super.getDescriptor();
        return result;
    }
}
