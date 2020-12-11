package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory;
import hudson.model.Run;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Helper class to create Pipeline job information
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class SumoPipelineJobStatusGenerator {
    private static final Logger LOG = Logger.getLogger(SumoPipelineJobStatusGenerator.class.getName());

    public static BuildModel generateJobStatusInformation(final Run buildInfo, PluginDescriptorImpl pluginDescriptor,
                                                          final boolean isSpecificJobFlagEnabled) {
        final BuildModel buildModel = new BuildModel();

        CommonModelFactory.populateGeneric(buildModel, buildInfo, pluginDescriptor, isSpecificJobFlagEnabled);

        for (SumoPipelineJobIdentifier extendListener : SumoPipelineJobIdentifier.canApply(buildInfo)) {
            try {
                extendListener.sendPipelineStagesAndConsoleLogs(buildInfo, buildModel, pluginDescriptor, isSpecificJobFlagEnabled);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "failed to extract job info", e);
            }
        }
        return buildModel;
    }
}