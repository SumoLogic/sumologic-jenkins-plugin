package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import com.google.gson.Gson;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.actions.*;
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
@SuppressFBWarnings("DM_DEFAULT_ENCODING")
public class SumoGraphListener implements GraphListener {

    public final static Logger LOG = Logger.getLogger(SumoGraphListener.class.getName());

    @Override
    public void onNewHead(FlowNode node) {
        if (node instanceof StepEndNode) {
            FlowNode startNode = ((StepEndNode) node).getStartNode();
            if (!isStage(startNode)) {
                return;
            }
            // Create a JSON with Build ID, Build Number, Stage Name, Stage ID, Stage Status, Build URL,
            // and all Env variables with SUMO_ prefix.
            // Send the Data when it contains one env variable with Sumo_ prefix
            LOG.log(Level.FINE, "Env Variables for Stage Name :- " + startNode.getDisplayName());
            Run<?, ?> run = runFor(node.getExecution());
            if (run != null) {
                EnvActionImpl envAction = run.getAction(EnvActionImpl.class);
                try {
                    Map<String, Object> data = new HashMap<>();
                    Gson gson = new Gson();
                    AtomicBoolean sendData = new AtomicBoolean(false);
                    if (envAction != null) {
                        envAction.getEnvironment().forEach((key, value) -> {
                            if (key.startsWith("Sumo_")) {
                                data.put(key.replace("Sumo_", ""), value);
                                sendData.set(true);
                            }
                        });
                        if (sendData.get()) {
                            data.put("name", run.getParent().getFullName());
                            data.put("number", run.getNumber());
                            data.put("stageResult", resultForStage(node).toString());
                            data.put("stageId", startNode.getId());
                            data.put("stageName", startNode.getDisplayName());
                            data.put("stageStartTime", getStartTime(startNode));
                            data.put("stageRunDuration", getTime(startNode, node));
                            data.put("jobBuildURL", CommonModelFactory.getAbsoluteUrl(run));
                            data.put("upstreamJobURL", CommonModelFactory.getUpStreamUrl(run));
                            LogSenderHelper.getInstance().sendData(gson.toJson(data).getBytes());
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "An error occurred while capturing Env Variables", e);
                }
            }
        }
    }

    private static boolean isStage(FlowNode node) {
        if (node instanceof StepAtomNode) {
            return false;
        }
        return node != null && ((node.getAction(StageAction.class) != null)
                || (node.getAction(LabelAction.class) != null && node.getAction(ThreadNameAction.class) == null)
                || node.getAction(ThreadNameAction.class) != null);
    }

    static long getTime(FlowNode startNode, FlowNode endNode) {
        TimingAction startTime = startNode.getAction(TimingAction.class);
        TimingAction endTime = endNode.getAction(TimingAction.class);

        if (startTime != null && endTime != null) {
            return endTime.getStartTime() - startTime.getStartTime();
        }
        return 0;
    }

    static long getStartTime(FlowNode node) {
        TimingAction startTime = node.getAction(TimingAction.class);

        if (startTime != null) {
            return startTime.getStartTime();
        }
        return 0;
    }

    static Result resultForStage(FlowNode endNode) {
        Result result = Result.SUCCESS;

        ErrorAction errorAction = endNode.getError();
        if (errorAction != null) {
            if (errorAction.getError() instanceof FlowInterruptedException) {
                result = ((FlowInterruptedException) errorAction.getError()).getResult();
            } else {
                result = Result.FAILURE;
            }
        }
        return result;
    }

    private static @CheckForNull
    Run<?, ?> runFor(FlowExecution exec) {
        Queue.Executable executable;
        try {
            executable = exec.getOwner().getExecutable();
        } catch (IOException x) {
            return null;
        }
        if (executable instanceof Run) {
            return (Run<?, ?>) executable;
        } else {
            return null;
        }
    }
}
