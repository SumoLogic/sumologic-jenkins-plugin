package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.constants.EventSourceEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.LogTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.QueueModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.SlaveModel;
import com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory;
import hudson.Extension;
import hudson.model.Queue;
import hudson.model.*;
import hudson.model.queue.WorkUnit;
import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;
import static com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory.*;

/**
 * Created by deven on 8/6/15.
 * <p>
 * Periodically publish jenkins system metadata to sumo
 * <p>
 * Updated by Sourabh Jain 05/2019
 */
@Extension
public class SumoPeriodicPublisher extends AsyncPeriodicWork {

    private static final long recurrencePeriod = TimeUnit.MINUTES.toMillis(3);
    private static final Logger LOGGER = Logger.getLogger(SumoPeriodicPublisher.class.getName());
    private LogSenderHelper logSenderHelper;
    private static Set<String> slaveNames = new HashSet<>();

    private static void setSlaves(Set<String> slaveNames) {
        SumoPeriodicPublisher.slaveNames = slaveNames;
    }

    public SumoPeriodicPublisher() {
        super("Sumo Logic Periodic Data Publisher");
        logSenderHelper = LogSenderHelper.getInstance();
        LOGGER.log(Level.FINE, "Sumo Logic status publishing period is {0}ms", recurrencePeriod);
    }

    @Override
    protected void execute(TaskListener listener) throws IOException, InterruptedException {
        try {
            sendNodeDetailsForJenkins();

            sendTasksInQueue();

            sendRunningJobDetails();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "An error occurred while sending periodic data ", exception);
        }
    }


    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }

    private void sendTasksInQueue() {
        final Queue.Item[] items = Jenkins.getInstance().getQueue().getItems();
        List<String> queueModels = new ArrayList<>();
        if(items != null && items.length > 0){
            for (Queue.Item item : items) {
                QueueModel queueModel = new QueueModel();
                queueModel.setLogType(LogTypeEnum.QUEUE_EVENT.getValue());
                queueModel.setQueueId(item.id);
                queueModel.setQueueTime((System.currentTimeMillis() - item.getInQueueSince()) / 1000f);
                queueModel.setBlocked(item.isStuck());
                queueModel.setReasonForBlock(item.getWhy());
                queueModel.setConcurrentBuild(item.task.isConcurrentBuild());
                if (item.task instanceof Job) {
                    queueModel.setJobName(((Job) item.task).getFullName());
                } else {
                    queueModel.setJobName(item.task.getFullDisplayName());
                }
                queueModel.setJobURL(getAbsoluteUrl(item.task.getUrl()));
                queueModel.setEventTime(DATETIME_FORMATTER.format(new Date()));
                queueModels.add(queueModel.toString());
            }
        }
        logSenderHelper.sendMultiplePeriodicLogs(queueModels);
    }

    private void sendNodeDetailsForJenkins() {
        List<SlaveModel> slaveModels = getNodeMonitorsDetails();
        if (CollectionUtils.isNotEmpty(slaveModels)) {
            List<String> messages = new ArrayList<>();
            for (SlaveModel slaveModel : slaveModels) {
                messages.add(slaveModel.toString());
            }
            logSenderHelper.sendMultiplePeriodicLogs(messages);
        }

        Set<String> slavesUp = new HashSet<>();
        for (SlaveModel slaveModel : slaveModels) {
            slavesUp.add(slaveModel.getNodeName());
        }

        List<String> removedSlaves = new ArrayList<>();
        for (String slave : slaveNames) {
            if (!slavesUp.contains(slave)) {
                SlaveModel slaveModel = new SlaveModel();
                slaveModel.setLogType(LogTypeEnum.SLAVE_EVENT.getValue());
                slaveModel.setEventTime(DATETIME_FORMATTER.format(new Date()));
                slaveModel.setEventSource(EventSourceEnum.PERIODIC_UPDATE.getValue());
                slaveModel.setNodeStatus("removed");
                slaveModel.setNodeName(slave);
                removedSlaves.add(slaveModel.toString());
            }
        }
        if (CollectionUtils.isNotEmpty(removedSlaves)) {
            logSenderHelper.sendMultiplePeriodicLogs(removedSlaves);
        }
        setSlaves(slavesUp);
    }

    public void sendRunningJobDetails() {
        List<String> currentBuildDetails = new ArrayList<>();
        for (Computer computer : Jenkins.getInstance().getComputers()) {
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
                buildModel.setName(run.getParent().getFullName());
                buildModel.setNumber(run.getNumber());
                buildModel.setJobRunDuration(getJobRunDuration(run));
                buildModel.setJobStartTime(DATETIME_FORMATTER.format(run.getTimestamp()));
                currentBuildDetails.add(buildModel.toJson());
            }
        }
        if (CollectionUtils.isNotEmpty(currentBuildDetails)) {
            logSenderHelper.sendMultiplePeriodicLogs(currentBuildDetails);
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
