package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import com.cloudbees.workflow.rest.external.*;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.PipelineStageModel;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.console.AnnotatedLargeText;
import hudson.model.Result;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.LogAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graphanalysis.ForkScanner;
import org.jenkinsci.plugins.workflow.graphanalysis.LabelledChunkFinder;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;
import static com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper.sendPipelineStages;

@SuppressFBWarnings({"DM_DEFAULT_ENCODING", "RV_RETURN_VALUE_IGNORED"})
public class PipelineStageViewExtractor {
    private static final LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();
    private static final Logger LOG = Logger.getLogger(PipelineStageViewExtractor.class.getName());

    /**
     * This method identifies all the stages as mentioned in the pipeline-stage-view plugin.
     *
     * @param workflowRun - pipeline run
     * @param buildModel  - the jenkins plugin build model
     */
    public static void extractPipelineStages(WorkflowRun workflowRun, BuildModel buildModel) {
        List<PipelineStageModel> pipelineStageModels = new ArrayList<>();
        RunExt runExt = RunExt.create(workflowRun);
        FlowExecution execution = workflowRun.getExecution();
        ExecutionNodeExtractor visitor = new ExecutionNodeExtractor(workflowRun);
        if (execution != null) {
            ForkScanner.visitSimpleChunks(execution.getCurrentHeads(), visitor, new LabelledChunkFinder());
        }

        if (Objects.nonNull(runExt) && CollectionUtils.isNotEmpty(runExt.getStages())) {
            for (StageNodeExt stageNodeExt : runExt.getStages()) {
                List<String> steps = new ArrayList<>();
                PipelineStageModel pipelineStageModel = setStageDetails(stageNodeExt, visitor.getWorkspaceNodes());
                List<AtomFlowNodeExt> stageFlowNodes = stageNodeExt.getStageFlowNodes();
                if (CollectionUtils.isNotEmpty(stageFlowNodes)) {
                    stageFlowNodes.forEach(atomFlowNodeExt -> {
                        String data = setStepDetails(atomFlowNodeExt, visitor.getWorkspaceNodes());
                        if (StringUtils.isNotBlank(data)) {
                            steps.add(data);
                        }
                    });
                    if (CollectionUtils.isNotEmpty(steps)) {
                        pipelineStageModel.setSteps(steps);
                    }
                }
                if (visitor.getParallelNodes().containsKey(stageNodeExt.getId())) {
                    pipelineStageModel.setParallelStage(visitor.getParallelNodes().get(stageNodeExt.getId()));
                }
                pipelineStageModels.add(pipelineStageModel);
            }
        }
        AtomicInteger counter = new AtomicInteger(1);
        pipelineStageModels.forEach(pipelineStageModel -> {
            pipelineStageModel.setId(counter.getAndIncrement());
        });
        if (CollectionUtils.isNotEmpty(pipelineStageModels)) {
            sendPipelineStages(pipelineStageModels, buildModel);
        }
    }

    private static PipelineStageModel setStageDetails(FlowNodeExt stageExt, Map<String, String> executionNodes) {
        final PipelineStageModel pipelineStageDTO = new PipelineStageModel();
        pipelineStageDTO.setStageId(stageExt.getId());
        pipelineStageDTO.setName(stageExt.getName());
        pipelineStageDTO.setStatus(convertToResult(stageExt.getStatus()));
        pipelineStageDTO.setStartTime(DATETIME_FORMATTER.format(stageExt.getStartTimeMillis()));
        pipelineStageDTO.setDuration(stageExt.getDurationMillis() / 1000f);
        pipelineStageDTO.setPauseDuration(stageExt.getPauseDurationMillis() / 1000f);
        pipelineStageDTO.setArguments(stageExt.getParameterDescription());
        pipelineStageDTO.setExecutionNode(executionNodes.getOrDefault(stageExt.getId(), "(master)"));

        ErrorExt error = stageExt.getError();
        if (error != null) {
            String errorMessage;
            errorMessage = "StageErrorType - " + error.getType() + "," + "StageErrorMessage - " + error.getMessage() + "";
            errorMessage = errorMessage.replace("{", "(");
            errorMessage = errorMessage.replace("}", ")");
            pipelineStageDTO.setError(errorMessage);
        }
        return pipelineStageDTO;
    }

    private static String setStepDetails(FlowNodeExt stepExt, Map<String, String> executionNodes) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("StepName - ").append(stepExt.getName()).append(",")
                .append("StepStatus - ").append(convertToResult(stepExt.getStatus())).append(",")
                .append("StepDuration - ").append(stepExt.getDurationMillis() / 1000f).append(",")
                .append("StepArguments - ").append(stepExt.getParameterDescription()).append(",")
                .append("StepExecutedOn - ").append(executionNodes.getOrDefault(stepExt.getId(), "(master)"));

        ErrorExt error = stepExt.getError();
        if (error != null) {
            stringBuilder.append(",StepErrorType - ").append(error.getType()).append(",")
                    .append("StepErrorMessage - ").append(error.getMessage());
        }

        stringBuilder.append(",StepPauseDuration - ").append(stepExt.getPauseDurationMillis() / 1000f).append(",")
                .append("StepId - ").append(stepExt.getId());

        String step = stringBuilder.toString();
        step = step.replace("{", "(");
        step = step.replace("}", ")");
        return step;
    }

    private static String convertToResult(StatusExt status) {
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

    /**
     * This method sends the console logs with source name as the stage/step name to identify the console logs
     * with respect to stage/step of the pipeline.
     *
     * @param workflowRun - pipeline run
     */
    public static void extractConsoleLogs(WorkflowRun workflowRun) {
        FlowExecution execution = workflowRun.getExecution();
        ExecutionNodeExtractor visitor = new ExecutionNodeExtractor(workflowRun);
        if (execution != null) {
            ForkScanner.visitSimpleChunks(execution.getCurrentHeads(), visitor, new LabelledChunkFinder());
        }

        Map<String, String> nodeWithNames = visitor.getStages().stream()
                .filter(stageNodeExt -> !stageNodeExt.getStatus().equals(StatusExt.NOT_EXECUTED))
                .collect(Collectors.toMap(StageNodeExt::getId, StageNodeExt::getName));

        Map<String, Set<String>> logsNodeWise = new HashMap<>();

        FlowExecution exec = workflowRun.getExecution();
        if (exec != null) {
            FlowGraphWalker walker = new FlowGraphWalker(exec);
            for (FlowNode node : walker) {
                try {
                    LogAction action = node.getAction(LogAction.class);
                    LinkedHashSet<String> messages = new LinkedHashSet<>();
                    if (action != null && action.getLogText() != null) {
                        AnnotatedLargeText<? extends FlowNode> logText = action.getLogText();
                        StringWriter writer = new StringWriter();

                        logText.writeLogTo(0, writer);
                        if (StringUtils.isNotEmpty(writer.toString())) {
                            messages.add(writer.toString());
                        }
                        writer.close();
                    }

                    ErrorAction error = node.getError();
                    if (error != null) {
                        Throwable throwable = error.getError();
                        messages.add(throwable.getMessage());
                    }

                    String nodeName = getLogPrefix(node, nodeWithNames);

                    if (CollectionUtils.isNotEmpty(messages)) {
                        if (logsNodeWise.containsKey(nodeName)) {
                            logsNodeWise.get(nodeName).addAll(messages);
                        } else {
                            logsNodeWise.put(nodeName, messages);
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error Sending console Logs", e);
                }
            }
            logsNodeWise.forEach((nodeName, messages) -> {
                StringBuilder stringBuilder = new StringBuilder();
                messages.forEach(s -> {
                    stringBuilder.append("[").append(DATETIME_FORMATTER.format(new Date()))
                            .append("] ").append(" ")
                            .append(s).append("\n");
                });
                logSenderHelper.sendConsoleLogs(stringBuilder.toString(), workflowRun.getParent().getFullName(), workflowRun.getNumber(), nodeName);
            });
        }
    }

    private static String getLogPrefix(FlowNode node, Map<String, String> nodeWithNames) {
        if (nodeWithNames.containsKey(node.getId())) {
            return nodeWithNames.get(node.getId());
        }
        for (FlowNode parentNode : node.getParents()) {
            String prefix = getLogPrefix(parentNode, nodeWithNames);
            if (prefix != null) {
                return prefix;
            }
        }
        return null;
    }
}
