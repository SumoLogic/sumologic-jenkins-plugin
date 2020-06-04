package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import com.cloudbees.workflow.rest.external.ErrorExt;
import com.cloudbees.workflow.rest.external.FlowNodeExt;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.model.PipelineStageModel;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import hudson.Extension;
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
import static com.sumologic.jenkins.jenkinssumologicplugin.listeners.SumoPipelineStatusListener.isPipeLineJobWithSpecificFlagEnabled;

/**
 * Sumo Logic plugin for Jenkins model.
 *
 * <p> Help extract stages information for pipeline Jobs.
 * Created by Sourabh Jain on 5/2019.
 */
@Extension
public class PipelineStageExtractor extends SumoPipelineJobIdentifier<WorkflowRun> {

    private static final Logger LOG = Logger.getLogger(PipelineStageExtractor.class.getName());

    private static LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();

    @Override
    public List<PipelineStageModel> extractPipelineStages(WorkflowRun workflowRun, PluginDescriptorImpl pluginDescriptor) {
        try{
            FlowExecution flowExecution = workflowRun.getExecution();
            if (flowExecution != null) {
                NodeDetailsExtractor visitor = new NodeDetailsExtractor(workflowRun);
                ForkScanner.visitSimpleChunks(flowExecution.getCurrentHeads(), visitor, new LabelledChunkFinder());
                List<PipelineStageModel> stages = createStages(visitor);
                AtomicInteger counter = new AtomicInteger(1);
                stages.forEach(pipelineStageModel -> {
                    pipelineStageModel.setId(counter.getAndIncrement());
                });

                if(pluginDescriptor.isJobConsoleLogEnabled() || isPipeLineJobWithSpecificFlagEnabled(workflowRun)){
                    sendConsoleLogsForPipelineStages(workflowRun, visitor);
                }

                return stages;
            }
        } catch (Exception e){
            LOG.log(Level.WARNING, "Error while generating stages ", e);
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

    private void sendConsoleLogsForPipelineStages(WorkflowRun workflowRun, NodeDetailsExtractor visitor){

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

    private String getLogPrefix(FlowNode node, Map<String, String> nodeWithNames) {
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
