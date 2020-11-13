package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import com.sumologic.jenkins.jenkinssumologicplugin.constants.EventSourceEnum;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.ComputerListener;
import hudson.slaves.OfflineCause;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import static com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory.updateStatus;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Computer Listener for any computer related event
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
@Extension
public class SumoJenkinsComputerListener extends ComputerListener {

    @Override
    public void preLaunch(Computer computer, TaskListener listener) {
        updateStatus(computer, EventSourceEnum.COMPUTER_PRE_LAUNCH.getValue());
        listener.getLogger().flush();
    }

    @Override
    public void preOnline(Computer computer, Channel channel, FilePath root, TaskListener listener) {
        updateStatus(computer, EventSourceEnum.COMPUTER_PRE_ONLINE.getValue());
        listener.getLogger().flush();
    }

    @Override
    public void onOnline(Computer computer, TaskListener listener) {
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
    public void onLaunchFailure(Computer computer, TaskListener taskListener) {
        updateStatus(computer, EventSourceEnum.LAUNCH_FAILURE.getValue());
        taskListener.getLogger().flush();
    }
}
