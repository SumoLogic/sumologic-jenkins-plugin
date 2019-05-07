package com.sumologic.jenkins.jenkinssumologicplugin.pluginextension.listener;

import com.sumologic.jenkins.jenkinssumologicplugin.SumoBuildNotifier;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.END_OF_SUMO_PIPELINE;
import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.GENERATION_ERROR;
import static com.sumologic.jenkins.jenkinssumologicplugin.pluginextension.helper.SumoPipelineJobStatusGenerator.generateJobStatusInformation;

/**
 * Sumo Logic plugin for Jenkins model.
 *
 * Run Listener that listen every build and for sumologic pipeline.
 *
 * Created by Sourabh Jain on 5/2019.
 */
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
            String errorMessage = GENERATION_ERROR + Arrays.toString(e.getStackTrace());
            LOG.log(Level.WARNING, errorMessage);
            listener.error(errorMessage);
        }
    }
}
