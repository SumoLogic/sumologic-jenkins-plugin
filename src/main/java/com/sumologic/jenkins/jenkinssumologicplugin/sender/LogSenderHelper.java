package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;

import java.util.ArrayList;
import java.util.List;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DIVIDER_FOR_MESSAGES;
import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.GRAPHITE_CONTENT_TYPE;

public class LogSenderHelper {

    private static LogSenderHelper INSTANCE = null;

    public static LogSenderHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LogSenderHelper();
        }

        return INSTANCE;
    }

    public void sendLogsToPeriodicSourceCategory(String data){

        PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();
        //TODO add flag for periodic and audit data
        LogSender.getInstance().sendLogs(pluginDescriptor.getUrl(), data.getBytes()
                , pluginDescriptor.getSourceNamePeriodic(), pluginDescriptor.getSourceCategoryPeriodic());
    }

    public void sendMultipleLogsToPeriodicSourceCategory(final List<String> messages) {
        List<String> strings = divideDataIntoEquals(messages);
        for(String data : strings){
            sendLogsToPeriodicSourceCategory(data);
        }
    }

    public void sendLogsToMetricDataCategory(final List<String> messages){
        PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();
        //TODO add flag for Metric data category
        List<String> strings = divideDataIntoEquals(messages);
        for(String data :strings){
            LogSender.getInstance().sendLogs(pluginDescriptor.getUrl(), data.getBytes()
                    , pluginDescriptor.getSourceNamePeriodic(), pluginDescriptor.getSourceCategoryPeriodic(), GRAPHITE_CONTENT_TYPE);
        }
    }

    public void sendLogsToStatusDataCategory(String data){
        PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();

        //TODO add flag for for Status data
        LogSender.getInstance().sendLogs(pluginDescriptor.getUrl(), data.getBytes()
                , pluginDescriptor.getSourceNameJobStatus(), pluginDescriptor.getSourceCategoryJobStatus());
    }

    private static List<String> divideDataIntoEquals(final List<String> messages){
        List<String> convertedMessages = new ArrayList<>();

        StringBuilder stringBuilder = new StringBuilder();
        int count = 1;
        for(String message : messages){
            stringBuilder.append(message).append("\n");
            count++;
            if(count % DIVIDER_FOR_MESSAGES == 1){
                convertedMessages.add(stringBuilder.toString());
                stringBuilder = new StringBuilder();
            }
        }
        convertedMessages.add(stringBuilder.toString());

        return convertedMessages;
    }
}
