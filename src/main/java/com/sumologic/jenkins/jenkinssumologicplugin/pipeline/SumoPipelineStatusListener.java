package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import com.sumologic.jenkins.jenkinssumologicplugin.SumoBuildNotifier;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.pipeline.SumoConstants.END_OF_SUMO_PIPELINE;
import static com.sumologic.jenkins.jenkinssumologicplugin.pipeline.SumoPipelineJobStatusGenerator.generateJobStatusInformation;

@Extension
public class SumoPipelineStatusListener extends RunListener<Run> {

    private static final Logger LOG = Logger.getLogger(SumoPipelineStatusListener.class.getName());

    @Override
    public void onCompleted(Run run, @Nonnull TaskListener listener) {
        try {
            /*
            Get the Last 10 Log Lines from the log file. Check if the lines have SumoPipelineLogCollection, then it is
            eligible for Job status sending.
            */
            List log = run.getLog(10);
            if (log.contains(END_OF_SUMO_PIPELINE)) {
                SumoBuildNotifier.getInstance().send(generateJobStatusInformation(run));
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Job Status Generation ended with exception as {0}", e.getStackTrace());
            //TODO send failure for JOb due to plugin issue
        }
    }
}
