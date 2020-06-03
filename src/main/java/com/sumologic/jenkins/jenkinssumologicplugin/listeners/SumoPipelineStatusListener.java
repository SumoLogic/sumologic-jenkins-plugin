package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.AuditEventTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.EventSourceEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.integration.SearchAction;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import hudson.Extension;
import hudson.Util;
import hudson.console.ConsoleNote;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import jenkins.model.CauseOfInterruption;
import jenkins.model.InterruptedBuildAction;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.GENERATION_ERROR;
import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.SUMO_PIPELINE;
import static com.sumologic.jenkins.jenkinssumologicplugin.pipeline.SumoPipelineJobStatusGenerator.generateJobStatusInformation;
import static com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory.*;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Run Listener that listen every build and for sumologic pipeline.
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
@Extension
public class SumoPipelineStatusListener extends RunListener<Run> {

    private static final Logger LOG = Logger.getLogger(SumoPipelineStatusListener.class.getName());
    private static LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();

    @Override
    public void onStarted(Run run, TaskListener listener) {
        try {
            String userId = getUserId(run);
            String message = String.format(AuditEventTypeEnum.JOB_STARTED.getMessage(), userId, run.getParent().getFullName(), run.getNumber());
            captureAuditEvent(userId, AuditEventTypeEnum.JOB_STARTED, message, null);
            updateSlaveInfoAfterJobRun(run);
        } catch (Exception e) {
            String errorMessage = GENERATION_ERROR + e.getMessage();
            LOG.log(Level.WARNING, errorMessage, e);
            listener.error(errorMessage);
        }
    }

    @Override
    public void onCompleted(Run run, @Nonnull TaskListener listener) {
        try {
            PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();
            /*
            Get the Last 10 Log Lines from the log file. Check if the lines have SumoPipelineLogCollection, then it is
            eligible for Job status sending.
            */
            boolean isSpecificJobFlagEnabled = isPipeLineJobWithSpecificFlagEnabled(run);

            BuildModel buildModel = generateJobStatusInformation(run, pluginDescriptor, isSpecificJobFlagEnabled);

            //For all jobs status || or for specific pipeline jobs
            if (StringUtils.isNotEmpty(buildModel.getJobType())) {
                if (pluginDescriptor.isJobStatusLogEnabled() || isSpecificJobFlagEnabled) {
                    //LOG.info("Job Status is "+buildModel.toJson());
                    logSenderHelper.sendJobStatusLogs(buildModel.toJson());
                }
                if (pluginDescriptor.isJobConsoleLogEnabled() || isSpecificJobFlagEnabled) {
                    run.addAction(new SearchAction(run));
                    sendConsoleLogs(run, listener);
                }
            }

            updateSlaveInfoAfterJobRun(run);

            //Send audit event for job finish
            if (!"ABORTED".equals(buildModel.getResult())) {
                String message = String.format(AuditEventTypeEnum.JOB_FINISHED.getMessage(), run.getParent().getFullName(), run.getNumber(), buildModel.getResult());
                captureAuditEvent(buildModel.getUser(), AuditEventTypeEnum.JOB_FINISHED, message, null);
            }

            //Send the Aborted information if any as part of audit.
            List<InterruptedBuildAction> actions = run.getActions(InterruptedBuildAction.class);
            for (InterruptedBuildAction action : actions) {
                List<CauseOfInterruption.UserInterruption> interrupts = Util.filter(action.getCauses()
                        , CauseOfInterruption.UserInterruption.class);
                if (!interrupts.isEmpty()) {
                    User user = interrupts.get(0).getUser();
                    String message = String.format(AuditEventTypeEnum.JOB_ABORTED.getMessage(), user.getId()
                            , run.getParent().getFullName(), run.getNumber());
                    captureAuditEvent(user.getId(), AuditEventTypeEnum.JOB_ABORTED, message, null);
                }
            }
        } catch (Exception e) {
            String errorMessage = GENERATION_ERROR + e.getMessage();
            LOG.log(Level.WARNING, errorMessage, e);
            listener.error(errorMessage);
        }
    }

    private static void updateSlaveInfoAfterJobRun(Run buildInfo) {
        //Update slave information as build has been done
        BuildModel buildModel = new BuildModel();
        getLabelAndNodeName(buildInfo, buildModel);
        if (buildModel.getNodeName() != null) {
            Node node = Jenkins.getInstance().getNode(buildModel.getNodeName());
            if (node != null && node.toComputer() != null) {
                Computer computer = node.toComputer();
                updateStatus(computer, EventSourceEnum.PERIODIC_UPDATE.getValue());
            }
        }
    }

    public static boolean isPipeLineJobWithSpecificFlagEnabled(Run run) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(run.getLogReader())) {
            AtomicBoolean isFlagEnabled = new AtomicBoolean(false);
            bufferedReader.lines().limit(15).forEach(value -> {
                String s1 = ConsoleNote.removeNotes(value);
                if (s1 != null && s1.contains(SUMO_PIPELINE)) {
                    isFlagEnabled.set(true);
                }
            });
            bufferedReader.close();
            return isFlagEnabled.get();
        }
    }

}
