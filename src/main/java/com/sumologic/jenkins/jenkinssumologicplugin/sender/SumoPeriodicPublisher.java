package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.EventSourceEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.LogTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.ModelFactory;
import com.sumologic.jenkins.jenkinssumologicplugin.model.QueueModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.SlaveModel;
import com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.model.Queue;
import hudson.model.queue.WorkUnit;
import hudson.node_monitors.NodeMonitor;
import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.*;
import static com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory.*;
import static org.apache.commons.lang.reflect.MethodUtils.getAccessibleMethod;

/**
 * Created by deven on 8/6/15.
 * <p>
 * Periodically publish jenkins system metadata to sumo
 */
@Extension
public class SumoPeriodicPublisher extends AsyncPeriodicWork {

    private static final long recurrencePeriod = TimeUnit.MINUTES.toMillis(3);
    private static final Logger LOGGER = Logger.getLogger(SumoPeriodicPublisher.class.getName());
    private LogSender logSender;
    private Set<String> slaveNames = new HashSet<>();
    private static LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();

    public SumoPeriodicPublisher() {
        super("Sumo Logic Periodic Data Publisher");
        logSender = LogSender.getInstance();
        LOGGER.log(Level.FINE, "Sumo Logic status publishing period is {0}ms", recurrencePeriod);
    }

    @Override
    protected void execute(TaskListener listener) throws IOException, InterruptedException {
        String logs = ModelFactory.createJenkinsModel(Jenkins.get()).toJson();

        PluginDescriptorImpl descriptor = PluginDescriptorImpl.getInstance();
        String url = descriptor.getUrl();

        logSender.sendLogs(url, logs.getBytes(),
                descriptor.getSourceNamePeriodic(), descriptor.getSourceCategoryPeriodic());

        sendTasksInQueue();
        sendNodeDetailsForJenkins();
        sendRunningJobDetails();
    }


    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }

    private void sendTasksInQueue() {
        final Queue.Item[] items = Jenkins.get().getQueue().getItems();
        List<String> queueModels = new ArrayList<>();
        for (Queue.Item item : items) {
            QueueModel queueModel = new QueueModel();
            queueModel.setLogType(LogTypeEnum.QUEUE_EVENT.getValue());
            queueModel.setQueueId(item.getId());
            queueModel.setQueueTime((System.currentTimeMillis() - item.getInQueueSince()) / 1000f);
            queueModel.setBlocked(item.isStuck());
            queueModel.setReasonForBlock(item.getWhy());
            queueModel.setConcurrentBuild(item.task.isConcurrentBuild());
            if (item.task instanceof Job) {
                queueModel.setJobName(((Job) item.task).getFullName());
            } else {
                queueModel.setJobName(item.task.getDisplayName());
            }
            queueModel.setJobURL(item.task.getUrl());
            queueModel.setEventTime(DATETIME_FORMATTER.format(new Date()));
            queueModels.add(queueModel.toString());
        }
        logSenderHelper.sendMultipleLogsToPeriodicSourceCategory(queueModels);
    }

    private void sendNodeDetailsForJenkins() {
        List<SlaveModel> slaveModels = getNodeMonitorsDetails();
        LOGGER.info("slave models details "+slaveModels.size());
        if (CollectionUtils.isNotEmpty(slaveModels)) {
            List<String> messages = new ArrayList<>();
            slaveModels.forEach(slaveModel -> messages.add(slaveModel.toString()));
            logSenderHelper.sendMultipleLogsToPeriodicSourceCategory(messages);
        }

        Set<String> slavesUp = slaveModels.stream().map(SlaveModel::getNodeName).collect(Collectors.toSet());
        List<String> removedSlaves = new ArrayList<>();
        slaveNames.forEach(slave -> {
            if (!slavesUp.contains(slave)) {
                SlaveModel slaveModel = new SlaveModel();
                slaveModel.setLogType(LogTypeEnum.SLAVE_EVENT.getValue());
                slaveModel.setEventTime(DATETIME_FORMATTER.format(new Date()));
                slaveModel.setEventSource(EventSourceEnum.PERIODIC_UPDATE.getValue());
                slaveModel.setNodeStatus("removed");
                slaveModel.setNodeName(slave);
                removedSlaves.add(slaveModel.toString());
            }
        });
        if (CollectionUtils.isNotEmpty(removedSlaves)) {
            logSenderHelper.sendMultipleLogsToPeriodicSourceCategory(removedSlaves);
        }
        slaveNames = slavesUp;
    }

    private List<SlaveModel> getNodeMonitorsDetails() {
        List<SlaveModel> slaveModels = new ArrayList<>();
        Computer[] computers = Jenkins.get().getComputers();

        if (computers == null || computers.length == 0) {
            return slaveModels;
        }
        Collection<NodeMonitor> monitors = ComputerSet.getMonitors();
        for (Computer computer : computers) {
            if (computer != null) {
                SlaveModel slaveModel = new SlaveModel();
                slaveModel.setLogType(LogTypeEnum.SLAVE_EVENT.getValue());
                slaveModel.setEventTime(DATETIME_FORMATTER.format(new Date()));
                slaveModel.setEventSource(EventSourceEnum.PERIODIC_UPDATE.getValue());
                getComputerStatus(computer, slaveModel);
                for (NodeMonitor monitor : monitors) {
                    slaveModel.getMonitorData().putAll(getMonitorData(computer, monitor));
                }
                slaveModels.add(slaveModel);
            }
        }
        return slaveModels;
    }

    public static void getComputerStatus(Computer computer, SlaveModel slaveModel) {
        slaveModel.setNodeName(getNodeName(computer));
        Node slaveNode = computer.getNode();
        if (slaveNode != null) {
            slaveModel.setNodeLabel(slaveNode.getLabelString());
        }
        slaveModel.setNodeStatus("updated");
        slaveModel.setNumberOfExecutors(computer.getNumExecutors());
        slaveModel.setIdle(computer.isIdle());
        slaveModel.setOnline(computer.isOnline());
        if (computer.isOffline()) {
            slaveModel.setNumberOfExecutors(0);
            slaveModel.setRemoved(true);
            slaveModel.setReasonOffline(computer.getOfflineCauseReason());
            slaveModel.setConnecting(computer.isConnecting());
        }
        slaveModel.setNodeURL(getAbsoluteUrl(computer));
        long connectTime = computer.getConnectTime();
        if (connectTime != 0) {
            slaveModel.setConnectTime(Util.XS_DATETIME_FORMATTER.format(connectTime));
        }
    }

    private static String getAbsoluteUrl(Computer computer) {
        String rootUrl = Jenkins.get().getRootUrl();
        if (rootUrl == null) {
            return computer.getUrl();
        } else {
            return Util.encode(rootUrl + computer.getUrl());
        }
    }

    private static String getNodeName(Computer computer) {
        if (computer instanceof Jenkins.MasterComputer) {
            return MASTER;
        } else {
            return computer.getName();
        }
    }

    private Map<String, Object> getMonitorData(Computer computer, NodeMonitor monitor) {
        Map<String, Object> monitorDetails = new HashMap<>();
        Object data = monitor.data(computer);
        if (data != null) {
            String monitorName = monitor.getClass().getSimpleName();
            String monitorData;
            Method method = getAccessibleMethod(data.getClass(), "toHtml", new Class<?>[0]);
            if (method != null) {
                try {
                    monitorData = (String) method.invoke(data, new Object[0]);
                } catch (Exception e) {
                    monitorData = data.toString();
                }
            } else {
                monitorData = data.toString();
            }
            Pattern compile = Pattern.compile(ERROR_SPAN_CONTENT, Pattern.CASE_INSENSITIVE);
            Matcher matcher = compile.matcher(monitorData);
            if (matcher.find()) {
                monitorDetails.put(monitorName, "warning:" + matcher.group(1));
            } else {
                monitorDetails.put(monitorName, monitorData);
            }
        }
        return monitorDetails;
    }

    private static void sendRunningJobDetails() {
        List<String> currentBuildDetails = new ArrayList<>();
        for (Computer computer : Jenkins.get().getComputers()) {
            List<Run> runList = new ArrayList<>();
            for (Executor executor : computer.getExecutors()) {
                Run run = getRunningJob(executor);
                if (run != null) {
                    runList.add(run);
                }
            }
            for (Executor executor : computer.getOneOffExecutors()) {
                Run run = getRunningJob(executor);
                if (run != null) {
                    runList.add(run);
                }
            }
            for (Run run : runList) {
                BuildModel buildModel = new BuildModel();
                buildModel.setResult("In_Progress");

                getLabelAndNodeName(run, buildModel);
                buildModel.setJobBuildURL(CommonModelFactory.getAbsoluteUrl(run));
                buildModel.setName(run.getParent().getDisplayName());
                buildModel.setNumber(run.getNumber());
                buildModel.setJobRunDuration(getJobRunDuration(run));
                currentBuildDetails.add(buildModel.toJson());
            }
        }
        if (CollectionUtils.isNotEmpty(currentBuildDetails)) {
            logSenderHelper.sendMultipleLogsToPeriodicSourceCategory(currentBuildDetails);
        }
    }

    private static Run getRunningJob(Executor executor) {
        Run run = null;
        Queue.Executable executable = executor.getCurrentExecutable();
        WorkUnit workUnit = executor.getCurrentWorkUnit();
        if (executable == null && workUnit != null) {
            executable = workUnit.getExecutable();
        }
        if (executable != null && executable instanceof Run) {
            run = (Run) executable;
        }
        return run;
    }
}
