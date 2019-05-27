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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Finds the parallel nodes if present.
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class NodeDetailsExtractor extends ChunkVisitor {
    private static final Logger LOG = Logger.getLogger(NodeDetailsExtractor.class.getName());
    private Map<String, String> workspaceNodes = new HashMap<>();
    private Map<String, Set<String>> parallelNodes = new HashMap<>();
    private String execNodeName = null;
    private String execNodeStartId = null;

    NodeDetailsExtractor(@Nonnull WorkflowRun run) {
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
        //reverse-order, traverse from end node to start node
        try {
            findWhereCurrentNodeIsExecuting(atomNode);
            findParallelNode(scan, atomNode);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "failed to extract pluginextension info", ex);
        }
        super.atomNode(before, atomNode, after, scan);
    }

    /**
     * store the jenkins node name where pluginextension ran
     *
     * @param atomNode flow  node
     */
    private void findWhereCurrentNodeIsExecuting(FlowNode atomNode) {
        if (execNodeName == null) {
            StepStartNode nodeStep = getPipelineBlockBoundaryStartNode(atomNode, "node");
            if (nodeStep != null) {
                //WorkspaceAction is recorded in node start
                WorkspaceAction workspaceAction = nodeStep.getAction(WorkspaceAction.class);
                if (workspaceAction != null) {
                    //store which jenkins node it is built on
                    execNodeName = workspaceAction.getNode();
                    execNodeStartId = nodeStep.getId();
                    if (StringUtils.isEmpty(execNodeName)) {
                        execNodeName = "(master)";
                    }
                    LOG.log(Level.FINE, "found workspace node id={0}, name={1}", new String[]{execNodeStartId, execNodeName});
                }
            }
        } else if (atomNode instanceof StepStartNode && atomNode.getId().equals(execNodeStartId)) {
            execNodeName = null;
        }
        if (execNodeName != null) {
            workspaceNodes.put(atomNode.getId(), execNodeName);
        }
    }

    /**
     * store parallel node info
     *
     * @param scan Scanner
     */
    private void findParallelNode(@Nonnull ForkScanner scan, FlowNode atomNode) {
        if (ParallelNodeTypeEnum.NORMAL.toString().equals(String.valueOf(scan.getCurrentType()))
                && ParallelNodeTypeEnum.PARALLEL_BRANCH_START.toString().equals(String.valueOf(scan.getNextType()))
                && scan.getCurrentParallelStartNode() != null) {
            FlowNode currentParallelStartNode = scan.getCurrentParallelStartNode();
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

    /**
     * Check whether it is an enclose functional node (with BodyInvocationAction)
     */
    private StepStartNode getPipelineBlockBoundaryStartNode(FlowNode atomNode, String functionName) {
        StepStartNode startNode = null;
        // it should have BodyInvocationAction
        if (atomNode instanceof StepEndNode) {
            StepEndNode stepEndNode = (StepEndNode) atomNode;
            if (stepEndNode.getStartNode().getDescriptor() != null
                    && stepEndNode.getStartNode().getDescriptor().getFunctionName() != null
                    && functionName.equals(stepEndNode.getStartNode().getDescriptor().getFunctionName())) {
                StepStartNode blockStart = stepEndNode.getStartNode();
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
        return startNode;
    }
}
