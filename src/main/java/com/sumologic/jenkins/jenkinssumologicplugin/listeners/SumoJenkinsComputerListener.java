package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import com.sumologic.jenkins.jenkinssumologicplugin.constants.EventSourceEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.LogTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.model.SlaveModel;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSender;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import hudson.slaves.OfflineCause;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;
import static com.sumologic.jenkins.jenkinssumologicplugin.sender.SumoPeriodicPublisher.getComputerStatus;

@Extension
public class SumoJenkinsComputerListener extends ComputerListener {

    private static LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();

    @Override
    public void onOnline(Computer computer, TaskListener listener) throws IOException, InterruptedException {
        updateStatus(computer, EventSourceEnum.COMPUTER_ONLINE.getValue());
        listener.getLogger().flush();
    }

    @Override
    public void onOffline(@Nonnull Computer computer, @CheckForNull OfflineCause cause) {
        updateStatus(computer, EventSourceEnum.COMPUTER_OFFLINE.getValue());
    }

    @Override
    public void onTemporarilyOnline(Computer computer) {
        updateStatus(computer, EventSourceEnum.COMPUTER_TEMP_ONLINE.getValue());
    }

    @Override
    public void onTemporarilyOffline(Computer computer, OfflineCause cause) {
        updateStatus(computer, EventSourceEnum.COMPUTER_TEMP_OFFLINE.getValue());
    }

    @Override
    public void onLaunchFailure(Computer computer, TaskListener taskListener) throws IOException, InterruptedException {
        updateStatus(computer, EventSourceEnum.LAUNCH_FAILURE.getValue());
        taskListener.getLogger().flush();
    }

    public static void updateStatus(Computer computer, String eventSource) {
        SlaveModel slaveModel = new SlaveModel();
        slaveModel.setLogType(LogTypeEnum.SLAVE_EVENT.getValue());
        slaveModel.setEventTime(DATETIME_FORMATTER.format(new Date()));
        slaveModel.setEventSource(eventSource);
        getComputerStatus(computer, slaveModel);
        logSenderHelper.sendLogsToPeriodicSourceCategory(slaveModel.toString());
    }
}
