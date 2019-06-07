package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import com.cloudbees.workflow.rest.external.ErrorExt;
import com.cloudbees.workflow.rest.external.FlowNodeExt;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import com.sumologic.jenkins.jenkinssumologicplugin.model.PipelineStageModel;
import hudson.Extension;
import hudson.model.Result;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graphanalysis.ForkScanner;
import org.jenkinsci.plugins.workflow.graphanalysis.LabelledChunkFinder;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;

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
    public List<PipelineStageModel> extractPipelineStages(WorkflowRun workflowRun) {
        FlowExecution flowExecution = workflowRun.getExecution();
        if (flowExecution != null) {
            NodeDetailsExtractor visitor = new NodeDetailsExtractor(workflowRun);
            ForkScanner.visitSimpleChunks(flowExecution.getCurrentHeads(), visitor, new LabelledChunkFinder());
            List<PipelineStageModel> stages = createStages(visitor);
            AtomicInteger counter = new AtomicInteger(1);
            stages.forEach(pipelineStageModel -> {
                pipelineStageModel.setId(counter.getAndIncrement());
            });
            return stages;
        }
        return new ArrayList<>();
    }

    private List<PipelineStageModel> createStages(NodeDetailsExtractor visitor) {
        List<PipelineStageModel> stages = new ArrayList<>();

        Collection<StageNodeExt> stageNodes = visitor.getStages();
        Map<String, String> workspaceNodes = visitor.getWorkspaceNodes();
        Map<String, Set<String>> parallelNodes = visitor.getParallelNodes();
        if (CollectionUtils.isNotEmpty(stageNodes)) {
            stageNodes.forEach(stageNodeExt -> {
                PipelineStageModel stage = getNodeDetails(stageNodeExt, workspaceNodes);
                List<String> steps = new ArrayList<>();

                stageNodeExt.getStageFlowNodes().forEach(atomFlowNodeExt -> {
                    String stepDetails = getStepDetails(atomFlowNodeExt, workspaceNodes);
                    if (StringUtils.isNotEmpty(stepDetails)) {
                        steps.add(stepDetails);
                    }
                });

                if (CollectionUtils.isNotEmpty(steps)) {
                    stage.setSteps(steps);
                }
                if (parallelNodes.containsKey(stageNodeExt.getId())) {
                    stage.setParallelStage(parallelNodes.get(stageNodeExt.getId()));
                }
                stages.add(stage);
            });
        }
        //beautifyForDeclarativeParallelBranches(stages);
        return stages;
    }

    private String getStepDetails(FlowNodeExt stageNodeExt, Map<String, String> workspaceNodes) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("StepName - ").append(stageNodeExt.getName()).append(",")
                .append("StepStatus - ").append(convertToResult(stageNodeExt.getStatus())).append(",")
                .append("StepDuration - ").append(stageNodeExt.getDurationMillis() / 1000f).append(",")
                .append("StepArguments - ").append(stageNodeExt.getParameterDescription()).append(",");

        String execNodeName = stageNodeExt.getExecNode();
        if (StringUtils.isEmpty(execNodeName)) {
            execNodeName = workspaceNodes.get(stageNodeExt.getId());
        }
        stringBuilder.append("StepExecutedOn - ").append(execNodeName);

        if (stageNodeExt.getError() != null) {
            stringBuilder.append(",StepErrorType - ").append(stageNodeExt.getError().getType()).append(",")
                    .append("StepErrorMessage - ").append(stageNodeExt.getError().getMessage());
        }
        String step = stringBuilder.toString();
        step = step.replace("{", "(");
        step = step.replace("}", ")");
        return step;
    }

    private PipelineStageModel getNodeDetails(FlowNodeExt stageNodeExt, Map<String, String> workspaceNodes) {
        final PipelineStageModel pipelineStageDTO = new PipelineStageModel();
        pipelineStageDTO.setStageId(stageNodeExt.getId());
        pipelineStageDTO.setName(stageNodeExt.getName());
        pipelineStageDTO.setStatus(convertToResult(stageNodeExt.getStatus()));
        pipelineStageDTO.setStartTime(DATETIME_FORMATTER.format(new Date(stageNodeExt.getStartTimeMillis())));
        pipelineStageDTO.setDuration(stageNodeExt.getDurationMillis() / 1000f);
        pipelineStageDTO.setPauseDuration(stageNodeExt.getPauseDurationMillis() / 1000f);
        pipelineStageDTO.setArguments(stageNodeExt.getParameterDescription());

        String execNodeName = stageNodeExt.getExecNode();
        if (StringUtils.isEmpty(execNodeName)) {
            //lookup the workspace nodes
            execNodeName = workspaceNodes.get(stageNodeExt.getId());
        }
        pipelineStageDTO.setExecutionNode(execNodeName);

        ErrorExt error = stageNodeExt.getError();
        if (error != null) {
            String errorMessage = "StageErrorType - " + stageNodeExt.getError().getType() + "," + "StageErrorMessage - " + stageNodeExt.getError().getMessage() + "";
            errorMessage = errorMessage.replace("{", "(");
            errorMessage = errorMessage.replace("}", ")");
            pipelineStageDTO.setError(errorMessage);
        }
        return pipelineStageDTO;
    }

    private String convertToResult(StatusExt status) {
        if (status == null) {
            return "UNKNOWN";
        }
        switch (status) {
            case FAILED:
                return Result.FAILURE.toString();
            case NOT_EXECUTED:
                return Result.NOT_BUILT.toString();
            default:
                return status.toString();
        }
    }

    /*private void beautifyForDeclarativeParallelBranches(List<PipelineStageModel> stages) {
        if (CollectionUtils.isNotEmpty(stages)) {
            Set<PipelineStageModel> collect = stages.stream().filter(pipelineStageModel -> pipelineStageModel.getName().startsWith("Branch: ")).collect(Collectors.toSet());

            for (PipelineStageModel pipelineStageModel : collect) {
                Optional<PipelineStageModel> first = stages.stream().filter(pipelineStageModel1 -> pipelineStageModel.getName().equals("Branch: " + pipelineStageModel1.getName())).findFirst();
                first.ifPresent(stageModel -> {
                    stageModel.setId(pipelineStageModel.getId());
                    stages.remove(pipelineStageModel);
                });
            }
        }
    }*/
}
