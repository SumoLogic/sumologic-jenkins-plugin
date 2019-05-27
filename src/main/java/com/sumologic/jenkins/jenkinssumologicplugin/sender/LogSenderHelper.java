package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;

import java.util.ArrayList;
import java.util.List;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DIVIDER_FOR_MESSAGES;
import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.GRAPHITE_CONTENT_TYPE;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Log Sender Helper
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class LogSenderHelper {

    private static class LogSenderHelperHolder {
        public static LogSenderHelper logSenderHelper = new LogSenderHelper();
    }

    public static LogSenderHelper getInstance() {
        return LogSenderHelperHolder.logSenderHelper;
    }

    public void sendLogsToPeriodicSourceCategory(String data) {

        PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();
        if (pluginDescriptor.isPeriodicLogEnabled()) {
            LogSender.getInstance().sendLogs(pluginDescriptor.getUrl(), data.getBytes()
                    , null, pluginDescriptor.getSourceCategory());
        }
    }

    public void sendMultiplePeriodicLogs(final List<String> messages) {
        List<String> strings = divideDataIntoEquals(messages);
        for (String data : strings) {
            sendLogsToPeriodicSourceCategory(data);
        }
    }

    public void sendLogsToMetricDataCategory(final List<String> messages) {
        PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();
        if (pluginDescriptor.isMetricDataEnabled()) {
            List<String> strings = divideDataIntoEquals(messages);
            for (String data : strings) {
                LogSender.getInstance().sendLogs(pluginDescriptor.getUrl(), data.getBytes()
                        , null, pluginDescriptor.getSourceCategory(), GRAPHITE_CONTENT_TYPE);
            }
        }

    }

    public void sendJobStatusLogs(String data) {
        PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();

        LogSender.getInstance().sendLogs(pluginDescriptor.getUrl(), data.getBytes()
                , null, pluginDescriptor.getSourceCategory());
    }

    public void sendAuditLogs(String data) {
        PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();
        if (pluginDescriptor.isAuditLogEnabled()) {
            LogSender.getInstance().sendLogs(pluginDescriptor.getUrl(), data.getBytes()
                    , null, pluginDescriptor.getSourceCategory());
        }
    }

    private static List<String> divideDataIntoEquals(final List<String> messages) {
        List<String> convertedMessages = new ArrayList<>();

        StringBuilder stringBuilder = new StringBuilder();
        int count = 1;
        for (String message : messages) {
            stringBuilder.append(message).append("\n");
            count++;
            if (count % DIVIDER_FOR_MESSAGES == 1) {
                convertedMessages.add(stringBuilder.toString());
                stringBuilder = new StringBuilder();
            }
        }
        convertedMessages.add(stringBuilder.toString());

        return convertedMessages;
    }
}
