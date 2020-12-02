package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;


import com.cloudbees.workflow.rest.external.ChunkVisitor;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.ParallelNodeTypeEnum;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.FlowStartNode;
import org.jenkinsci.plugins.workflow.graphanalysis.ForkScanner;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.JENKINS_MAIN;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Finds the execution nodes.
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class ExecutionNodeExtractor extends ChunkVisitor {
    private static final Logger LOG = Logger.getLogger(ExecutionNodeExtractor.class.getName());
    private final Map<String, String> workspaceNodes = new HashMap<>();
    private final Map<String, Set<String>> parallelNodes = new HashMap<>();
    private String execNodeName = null;
    private String execNodeStartId = null;

    ExecutionNodeExtractor(@Nonnull WorkflowRun run) {
        super(run);
    }

    Map<String, String> getWorkspaceNodes() {
        return workspaceNodes;
    }

    Map<String, Set<String>> getParallelNodes() {
        return parallelNodes;
    }

    @Override
    public void atomNode(@CheckForNull FlowNode before, @Nonnull FlowNode atomNode, @CheckForNull FlowNode after, @Nonnull ForkScanner scan) {
        try {
            findWhereCurrentNodeIsExecuting(atomNode);
            findParallelNode(scan, atomNode);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "failed to extract plugin extension info", ex);
        }
        super.atomNode(before, atomNode, after, scan);
    }

    private void findWhereCurrentNodeIsExecuting(FlowNode atomNode) {
        if (execNodeName == null) {
            StepStartNode nodeStep = getPipelineBlockBoundaryStartNode(atomNode);
            if (nodeStep != null) {
                WorkspaceAction workspaceAction = nodeStep.getPersistentAction(WorkspaceAction.class);
                if (workspaceAction != null) {
                    execNodeName = workspaceAction.getNode();
                    execNodeStartId = nodeStep.getId();
                    if (StringUtils.isEmpty(execNodeName)) {
                        execNodeName = JENKINS_MAIN;
                    }
                }
            }
        } else if (atomNode instanceof StepStartNode && atomNode.getId().equals(execNodeStartId)) {
            execNodeName = null;
        }
        if (execNodeName != null) {
            workspaceNodes.put(atomNode.getId(), execNodeName);
        }
    }

    private void findParallelNode(@Nonnull ForkScanner scan, FlowNode atomNode) {
        if (ParallelNodeTypeEnum.NORMAL.toString().equals(String.valueOf(scan.getCurrentType()))
                && ParallelNodeTypeEnum.PARALLEL_BRANCH_START.toString().equals(String.valueOf(scan.getNextType()))
                && scan.getCurrentParallelStartNode() != null) {
            FlowNode currentParallelStartNode = scan.getCurrentParallelStartNode();
            if (currentParallelStartNode != null) {
                List<String> parentIds = currentParallelStartNode.getParents().stream().map(FlowNode::getId).collect(Collectors.toList());
                Set<String> childrenInParallel = atomNode.getParents().stream().map(FlowNode::getId).collect(Collectors.toSet());
                parentIds.forEach(parentId -> {
                    if (parallelNodes.containsKey(parentId)) {
                        parallelNodes.get(parentId).addAll(childrenInParallel);
                    } else {
                        parallelNodes.put(parentId, childrenInParallel);
                    }
                });
            }
        }
    }

    private StepStartNode getPipelineBlockBoundaryStartNode(FlowNode atomNode) {
        StepStartNode startNode = null;
        if (atomNode instanceof StepEndNode) {
            StepEndNode stepEndNode = (StepEndNode) atomNode;
            StepStartNode blockStart = stepEndNode.getStartNode();
            if (blockStart.getDescriptor() != null) {
                StepDescriptor descriptor = blockStart.getDescriptor();
                if (descriptor != null && descriptor.getFunctionName() != null && "node".equals(descriptor.getFunctionName())) {
                    if (blockStart.getParents().size() < 1) {
                        return null;
                    }
                    FlowNode boundaryNode = blockStart.getParents().get(0);
                    if (boundaryNode instanceof StepStartNode) {
                        startNode = (StepStartNode) boundaryNode;
                    } else if (boundaryNode instanceof FlowStartNode) {
                        //special handling for stage node
                        startNode = blockStart;
                    }
                }
            }
        }
        return startNode;
    }
}
