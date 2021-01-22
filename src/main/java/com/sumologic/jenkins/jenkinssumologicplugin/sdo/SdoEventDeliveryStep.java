package com.sumologic.jenkins.jenkinssumologicplugin.sdo;

import com.google.gson.Gson;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Queue;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.StageAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@SuppressFBWarnings("DM_DEFAULT_ENCODING")
public class SdoEventDeliveryStep extends Step {

    private String eventType;
    private HashMap<String, Object> keyValueMap = new HashMap<>();
    private HashMap<String, String> fields = new HashMap<>();

    public HashMap<String, Object> getKeyValueMap() {
        return keyValueMap;
    }

    @DataBoundSetter
    public void setKeyValueMap(HashMap<String, Object> keyValueMap) {
        this.keyValueMap = keyValueMap;
    }

    public HashMap<String, String> getFields() {
        return fields;
    }

    @DataBoundSetter
    public void setFields(HashMap<String, String> fields) {
        this.fields = fields;
    }

    public String getEventType() {
        return eventType;
    }

    @DataBoundSetter
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @DataBoundConstructor
    public SdoEventDeliveryStep() {
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new SdoEventDeliveryStep.Execution(this, stepContext);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<>();
        }

        @Override
        public String getFunctionName() {
            return "SumoSDOEvent";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Upload Events specific to Software Delivery Optimization Solution to Sumo Logic.";
        }
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<String> {

        protected static final long serialVersionUID = 1L;

        protected final transient SdoEventDeliveryStep step;
        private static final LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();

        protected Execution(SdoEventDeliveryStep sdoEventDeliveryStep, @Nonnull StepContext context) {
            super(context);
            this.step = sdoEventDeliveryStep;
        }

        @Override
        protected String run() throws Exception {
            final HashMap<String, Object> keyValueMap = this.step.getKeyValueMap();
            final HashMap<String, String> fields = this.step.getFields();
            final String eventType = this.step.getEventType();

            EnvVars envVars = this.getContext().get(EnvVars.class);
            if (envVars != null && !envVars.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                Gson gson = new Gson();
                envVars.forEach((key, value) -> {
                    if (key.toLowerCase().equals("sumo_event_type")) {
                        if (StringUtils.isBlank(eventType)) {
                            data.put("event_type", value);
                        } else {
                            data.put("event_type", eventType);
                        }
                    } else if (key.toLowerCase().startsWith("sumo_")) {
                        data.put(key.replaceFirst("(?i)" + Pattern.quote("sumo_"), ""), value);
                    }
                });
                if (!keyValueMap.isEmpty()) {
                    data.putAll(keyValueMap);
                }
                FlowNode parentStage = getParentStage();
                if (parentStage != null) {
                    Run<?, ?> run = runFor(parentStage.getExecution());
                    if (run != null) {
                        data.put("name", run.getParent().getFullName());
                        data.put("number", run.getNumber());
                        data.put("stageId", parentStage.getId());
                        data.put("stageName", parentStage.getDisplayName());
                        data.put("stageStartTime", getStartTime(parentStage));
                        data.put("jobBuildURL", CommonModelFactory.getAbsoluteUrl(run));
                        data.put("upstreamJobURL", CommonModelFactory.getUpStreamUrl(run));
                    }
                }
                logSenderHelper.sendDataWithFields(gson.toJson(data).getBytes(), fields);
            }
            return null;
        }

        private FlowNode getParentStage() {
            try {
                FlowNode flowNode = this.getContext().get(FlowNode.class);
                if (flowNode != null) {
                    if (isStage(flowNode)) {
                        return flowNode;
                    } else {
                        return getFlowNodeFromParents(flowNode);
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        private boolean isStage(FlowNode node) {
            if (node instanceof StepAtomNode) {
                return false;
            }
            return node != null && ((node.getAction(StageAction.class) != null)
                    || (node.getAction(LabelAction.class) != null && node.getAction(ThreadNameAction.class) == null));
        }

        private FlowNode getFlowNodeFromParents(FlowNode flownode) {
            List<FlowNode> parents = flownode.getParents();
            for (FlowNode parentNode : parents) {
                if (isStage(parentNode)) {
                    return parentNode;
                } else {
                    return getFlowNodeFromParents(parentNode);
                }
            }
            return flownode;
        }

        static long getStartTime(FlowNode node) {
            TimingAction startTime = node.getAction(TimingAction.class);

            if (startTime != null) {
                return startTime.getStartTime();
            }
            return 0;
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
}
