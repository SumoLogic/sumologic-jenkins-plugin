package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sumo Logic plugin for Jenkins model.
 *
 * <p> Help extract stages information for pipeline Jobs.
 * Created by Sourabh Jain on 5/2019.
 */
@Extension
public class PipelineStageExtractor extends SumoPipelineJobIdentifier<WorkflowRun> {

    private static final Logger LOG = Logger.getLogger(PipelineStageExtractor.class.getName());

    @Override
    public void sendPipelineStagesAndConsoleLogs(WorkflowRun workflowRun, BuildModel buildModel,
                                                 PluginDescriptorImpl pluginDescriptor, boolean isSpecificJobFlagEnabled) {
        try {
            FlowExecution flowExecution = workflowRun.getExecution();
            if (flowExecution != null) {
                if (pluginDescriptor.isJobStatusLogEnabled() || isSpecificJobFlagEnabled) {
                    // BlueOceanPipelineExtractor.extractPipelineStages(workflowRun, buildModel);
                    PipelineStageViewExtractor.extractPipelineStages(workflowRun, buildModel);
                }
                if (pluginDescriptor.isJobConsoleLogEnabled() || isSpecificJobFlagEnabled) {
                    PipelineStageViewExtractor.extractConsoleLogs(workflowRun);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error while generating stages ", e);
        }
    }
}
