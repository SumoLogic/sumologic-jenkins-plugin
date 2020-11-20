package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import com.cloudbees.workflow.rest.external.*;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.PipelineStageModel;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import hudson.console.AnnotatedLargeText;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.LogAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.StringWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;
import static com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper.sendPipelineStages;

public class PipelineStageViewExtractor {
    private static final LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();
    private static final Logger LOG = Logger.getLogger(PipelineStageViewExtractor.class.getName());

    public static void extractPipelineStages(WorkflowRun workflowRun, BuildModel buildModel) {
        List<PipelineStageModel> pipelineStageModels = new ArrayList<>();
        RunExt runExt = RunExt.create(workflowRun);
        if (Objects.nonNull(runExt) && CollectionUtils.isNotEmpty(runExt.getStages())) {
            for (StageNodeExt stageNodeExt : runExt.getStages()) {
                List<String> steps = new ArrayList<>();
                PipelineStageModel pipelineStageModel = setStageDetails(stageNodeExt);
                List<AtomFlowNodeExt> stageFlowNodes = stageNodeExt.getStageFlowNodes();
                if (CollectionUtils.isNotEmpty(stageFlowNodes)) {
                    stageFlowNodes.forEach(atomFlowNodeExt -> {
                        String data = setStepDetails(atomFlowNodeExt);
                        if (StringUtils.isNotBlank(data)) {
                            steps.add(data);
                        }
                    });
                    if (CollectionUtils.isNotEmpty(steps)) {
                        pipelineStageModel.setSteps(steps);
                    }
                }
                pipelineStageModels.add(pipelineStageModel);
            }
        }
        if (CollectionUtils.isNotEmpty(pipelineStageModels)) {
            sendPipelineStages(pipelineStageModels, buildModel);
        }
    }

    private static PipelineStageModel setStageDetails(FlowNodeExt stageExt) {
        final PipelineStageModel pipelineStageDTO = new PipelineStageModel();
        pipelineStageDTO.setStageId(stageExt.getId());
        pipelineStageDTO.setName(stageExt.getName());
        pipelineStageDTO.setStatus(stageExt.getStatus().name());
        pipelineStageDTO.setStartTime(DATETIME_FORMATTER.format(stageExt.getStartTimeMillis()));
        pipelineStageDTO.setDuration(stageExt.getDurationMillis() / 1000f);
        pipelineStageDTO.setPauseDuration(stageExt.getPauseDurationMillis() / 1000f);
        pipelineStageDTO.setArguments(stageExt.getParameterDescription());
        pipelineStageDTO.setExecutionNode(StringUtils.isNotBlank(stageExt.getExecNode()) ? stageExt.getExecNode() : "(master)");

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

    private static String setStepDetails(FlowNodeExt stepExt) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("StepName - ").append(stepExt.getName()).append(",")
                .append("StepStatus - ").append(stepExt.getStatus().name()).append(",")
                .append("StepDuration - ").append(stepExt.getDurationMillis() / 1000f).append(",")
                .append("StepPauseDuration - ").append(stepExt.getPauseDurationMillis() / 1000f).append(",")
                .append("StepArguments - ").append(stepExt.getParameterDescription()).append(",")
                .append("StepExecutedOn - ").append(StringUtils.isNotBlank(stepExt.getExecNode()) ? stepExt.getExecNode() : "(master)");

        ErrorExt error = stepExt.getError();
        if (error != null) {
            stringBuilder.append(",StepErrorType - ").append(error.getType()).append(",")
                    .append("StepErrorMessage - ").append(error.getMessage());
        }

        String step = stringBuilder.toString();
        step = step.replace("{", "(");
        step = step.replace("}", ")");
        return step;
    }

    public static void extractConsoleLogs(WorkflowRun workflowRun) {
        RunExt runExt = RunExt.create(workflowRun);
        Map<String, String> nodeWithNames = runExt.getStages().stream()
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
                        if(StringUtils.isNotEmpty(writer.toString())){
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
                        if(logsNodeWise.containsKey(nodeName)){
                            logsNodeWise.get(nodeName).addAll(messages);
                        } else{
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
