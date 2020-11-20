package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.PipelineStageModel;
import io.jenkins.blueocean.rest.impl.pipeline.FlowNodeWrapper;
import io.jenkins.blueocean.rest.impl.pipeline.PipelineNodeGraphVisitor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.actions.ArgumentsAction;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;
import static com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper.sendPipelineStages;

public class BlueOceanPipelineExtractor {

    public static void extractPipelineStages(WorkflowRun workflowRun, BuildModel buildModel) {
        PipelineNodeGraphVisitor pipelineNodeGraphVisitor = new PipelineNodeGraphVisitor(workflowRun);
        List<PipelineStageModel> pipelineStageModels = new ArrayList<>();
        for (FlowNodeWrapper flowNodeWrapper : pipelineNodeGraphVisitor.getPipelineNodes()) {
            PipelineStageModel pipelineStageModel = setStageDetails(flowNodeWrapper);
            List<String> steps = new ArrayList<>();
            flowNodeWrapper.edges.forEach(step -> {
                String data = setStepDetails(step);
                if (StringUtils.isNotBlank(data)) {
                    steps.add(data);
                }
            });
            if (CollectionUtils.isNotEmpty(steps)) {
                pipelineStageModel.setSteps(steps);
            }
            pipelineStageModels.add(pipelineStageModel);
        }
        if (CollectionUtils.isNotEmpty(pipelineStageModels)) {
            sendPipelineStages(pipelineStageModels, buildModel);
        }
    }

    private static PipelineStageModel setStageDetails(FlowNodeWrapper flowNodeWrapper) {
        final PipelineStageModel pipelineStageDTO = new PipelineStageModel();
        pipelineStageDTO.setStageId(flowNodeWrapper.getId());
        pipelineStageDTO.setName(flowNodeWrapper.getDisplayName());
        pipelineStageDTO.setStatus(flowNodeWrapper.getStatus().getResult().name());
        pipelineStageDTO.setStartTime(DATETIME_FORMATTER.format(new Date(flowNodeWrapper.getTiming().getStartTimeMillis())));
        pipelineStageDTO.setDuration(flowNodeWrapper.getTiming().getTotalDurationMillis() / 1000f);
        pipelineStageDTO.setPauseDuration(flowNodeWrapper.getTiming().getPauseDurationMillis() / 1000f);
        pipelineStageDTO.setArguments(ArgumentsAction.getStepArgumentsAsString(flowNodeWrapper.getNode()));
        pipelineStageDTO.setExecutionNode(findWhereCurrentNodeIsExecuting(flowNodeWrapper.getNode()));

        ErrorAction error = flowNodeWrapper.getNode().getError();
        if (error != null) {
            Throwable throwable = error.getError();
            String errorMessage;
            errorMessage = "StageErrorType - " + throwable.getClass().getName() + "," + "StageErrorMessage - " + throwable.getMessage() + "";
            errorMessage = errorMessage.replace("{", "(");
            errorMessage = errorMessage.replace("}", ")");
            pipelineStageDTO.setError(errorMessage);
        }
        return pipelineStageDTO;
    }

    private static String setStepDetails(FlowNodeWrapper flowNodeWrapper) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("StepName - ").append(flowNodeWrapper.getDisplayName()).append(",")
                .append("StepStatus - ").append(flowNodeWrapper.getStatus().getResult().name()).append(",")
                .append("StepDuration - ").append(flowNodeWrapper.getTiming().getTotalDurationMillis() / 1000f).append(",")
                .append("StepPauseDuration - ").append(flowNodeWrapper.getTiming().getPauseDurationMillis() / 1000f).append(",")
                .append("StepArguments - ").append(ArgumentsAction.getStepArgumentsAsString(flowNodeWrapper.getNode())).append(",")
                .append("StepExecutedOn - ").append(findWhereCurrentNodeIsExecuting(flowNodeWrapper.getNode()));

        ErrorAction error = flowNodeWrapper.getNode().getError();
        if (error != null) {
            Throwable throwable = error.getError();
            stringBuilder.append(",StepErrorType - ").append(throwable.getClass().getName()).append(",")
                    .append("StepErrorMessage - ").append(throwable.getMessage());
        }

        String step = stringBuilder.toString();
        step = step.replace("{", "(");
        step = step.replace("}", ")");
        return step;
    }

    private static String findWhereCurrentNodeIsExecuting(FlowNode flowNode) {
        String execNodeName = "(master)";
        WorkspaceAction workspaceAction = flowNode.getPersistentAction(WorkspaceAction.class);
        if (workspaceAction != null) {
            execNodeName = workspaceAction.getNode();
            if (StringUtils.isEmpty(execNodeName)) {
                execNodeName = "(master)";
            }
        }
        return execNodeName;
    }
}
