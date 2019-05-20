package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import com.sumologic.jenkins.jenkinssumologicplugin.constants.AuditEventTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.EventSourceEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import jenkins.model.CauseOfInterruption;
import jenkins.model.InterruptedBuildAction;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.END_OF_SUMO_PIPELINE;
import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.GENERATION_ERROR;
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
            String message = String.format(AuditEventTypeEnum.JOB_STARTED.getMessage(), userId, run.getParent().getDisplayName(), run.getNumber());
            captureAuditEvent(userId, AuditEventTypeEnum.JOB_STARTED, message, null);
            updateSlaveInfoAfterJobRun(run);
        } catch (Exception e) {
            String errorMessage = GENERATION_ERROR + Arrays.toString(e.getStackTrace());
            LOG.log(Level.WARNING, errorMessage);
            listener.error(errorMessage);
        }
    }

    @Override
    public void onCompleted(Run run, @Nonnull TaskListener listener) {
        try {
            /*
            Get the Last 10 Log Lines from the log file. Check if the lines have SumoPipelineLogCollection, then it is
            eligible for Job status sending.
            */
            List log = run.getLog(10);
            BuildModel buildModel = generateJobStatusInformation(run);
            if (log.contains(END_OF_SUMO_PIPELINE)) {
                logSenderHelper.sendLogsToStatusDataCategory(buildModel.toJson());
            }

            updateSlaveInfoAfterJobRun(run);

            //Send audit event for job finish
            if(!"ABORTED".equals(buildModel.getResult())){
                String message = String.format(AuditEventTypeEnum.JOB_FINISHED.getMessage(), run.getParent().getDisplayName(), run.getNumber(), buildModel.getResult());
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
                            , run.getParent().getDisplayName(), run.getNumber());
                    captureAuditEvent(user.getId(), AuditEventTypeEnum.JOB_ABORTED, message, null);
                }
            }
        } catch (Exception e) {
            String errorMessage = GENERATION_ERROR + Arrays.toString(e.getStackTrace());
            LOG.log(Level.WARNING, errorMessage);
            listener.error(errorMessage);
        }
    }

    private static void updateSlaveInfoAfterJobRun(Run buildInfo) {
        //Update slave information as build has been done
        BuildModel buildModel = new BuildModel();
        getLabelAndNodeName(buildInfo, buildModel);
        Computer.threadPoolForRemoting.submit(() -> {
            if (buildModel.getNodeName() != null) {
                Node node = Jenkins.get().getNode(buildModel.getNodeName());
                if (node != null && node.toComputer() != null) {
                    Computer computer = node.toComputer();
                    updateStatus(computer, EventSourceEnum.PERIODIC_UPDATE.getValue());
                }
            }
        });
    }
}
