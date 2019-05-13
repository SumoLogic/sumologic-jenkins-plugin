package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.ErrorModel;
import com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory;
import com.sumologic.jenkins.jenkinssumologicplugin.model.PipelineStageModel;
import hudson.model.Result;
import hudson.model.Run;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sumo Logic plugin for Jenkins model.
 *
 * Helper class to create Pipeline job information
 *
 * Created by Sourabh Jain on 5/2019.
 */
public class SumoPipelineJobStatusGenerator {
    private static final Logger LOG = Logger.getLogger(SumoPipelineJobStatusGenerator.class.getName());

    public static BuildModel generateJobStatusInformation(final Run buildInfo) {
        final BuildModel buildModel = new BuildModel();

        CommonModelFactory.populateGeneric(buildModel, buildInfo);

        for (SumoPipelineJobIdentifier extendListener : SumoPipelineJobIdentifier.canApply(buildInfo)) {
            try {
                List<PipelineStageModel> stages = extendListener.extractPipelineStages(buildInfo);
                if(!Result.SUCCESS.toString().equals(buildModel.getResult())){
                    Optional<PipelineStageModel> first = stages.stream().filter(pipelineStageModel -> pipelineStageModel.getStatus().equals(buildModel.getResult())).findFirst();
                    first.ifPresent(pipelineStageModel -> buildModel.setErrorModel(pipelineStageModel.getErrorModel()));
                }
                if (CollectionUtils.isNotEmpty(stages)) {
                    buildModel.setStages(stages);
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "failed to extract job info", e);
            }
        }

        return buildModel;
    }
}