package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.PipelineStageModel;
import com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory;
import hudson.model.Run;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper.sendPipelineStages;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Helper class to create Pipeline job information
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class SumoPipelineJobStatusGenerator {
    private static final Logger LOG = Logger.getLogger(SumoPipelineJobStatusGenerator.class.getName());

    public static BuildModel generateJobStatusInformation(final Run buildInfo, PluginDescriptorImpl pluginDescriptor) {
        final BuildModel buildModel = new BuildModel();

        CommonModelFactory.populateGeneric(buildModel, buildInfo, pluginDescriptor);

        for (SumoPipelineJobIdentifier extendListener : SumoPipelineJobIdentifier.canApply(buildInfo)) {
            try {
                List<PipelineStageModel> stages = extendListener.extractPipelineStages(buildInfo, pluginDescriptor);
                if (CollectionUtils.isNotEmpty(stages) && pluginDescriptor.isJobStatusLogEnabled()) {
                    sendPipelineStages(stages, buildModel);
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "failed to extract job info", e);
            }
        }
        return buildModel;
    }
}