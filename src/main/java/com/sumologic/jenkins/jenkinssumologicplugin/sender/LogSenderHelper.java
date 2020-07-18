package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.google.gson.Gson;
import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.LogTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.PipelineStageModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.TestCaseModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.TestCaseResultModel;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.*;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Log Sender Helper
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class LogSenderHelper {

    public final static Logger LOG = Logger.getLogger(LogSenderHelper.class.getName());

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

    public void sendFilesData(final List<String> messages, String localFileString, String url, String sourceCategory, HashMap<String, String> fields, String host) {
        if (CollectionUtils.isNotEmpty(messages)) {
            List<String> strings = divideDataIntoEquals(messages);
            for (String data : strings) {
                LogSender.getInstance().sendLogs(url, data.getBytes(), localFileString, sourceCategory, null, fields, host);
            }
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

    public void sendConsoleLogs(String data, String jobName, int buildNumber, String stageName) {
        PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();

        String sourceName = jobName + "#" + buildNumber;
        if (StringUtils.isNotEmpty(stageName)) {
            sourceName = sourceName + "#" + stageName;
        }
        LogSender.getInstance().sendLogs(pluginDescriptor.getUrl(), data.getBytes()
                , sourceName, pluginDescriptor.getSourceCategory());
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
            if (count % DIVIDER_FOR_MESSAGES == 0) {
                convertedMessages.add(stringBuilder.toString());
                stringBuilder = new StringBuilder();
            }
            count++;
        }
        convertedMessages.add(stringBuilder.toString());

        return convertedMessages;
    }

    public static void sendTestResult(TestCaseModel testCaseModel, BuildModel buildModel) {
        PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();
        Gson gson = new Gson();

        Map<String, Object> data = new HashMap<>();

        data.put("logType", LogTypeEnum.TEST_RESULT.getValue());
        data.put("name", buildModel.getName());
        data.put("number", buildModel.getNumber());

        if (testCaseModel != null) {
            if (CollectionUtils.isNotEmpty(testCaseModel.getTestResults())) {
                List<TestCaseResultModel> testResults = testCaseModel.getTestResults();
                // send test cases based on the size
                List<TestCaseResultModel> toBeSent = new LinkedList<>();
                data.put("testResult", toBeSent);
                int size = gson.toJson(data).getBytes().length;
                for (TestCaseResultModel testCaseResultModel : testResults) {
                    if ("Failed".equals(testCaseResultModel.getStatus())) {
                        testCaseResultModel.setErrorDetails(format(testCaseResultModel.getErrorDetails()));
                        testCaseResultModel.setErrorStackTrace(format(testCaseResultModel.getErrorStackTrace()));
                    }
                    size = size + gson.toJson(testCaseResultModel).getBytes().length;
                    if (size > MAX_DATA_SIZE) {
                        sendTestResultInChunksOfPreDefinedSize(buildModel, gson, pluginDescriptor, toBeSent, data);
                        toBeSent.clear();
                    }
                    toBeSent.add(testCaseResultModel);
                    size = gson.toJson(data).getBytes().length;
                }
                if (CollectionUtils.isNotEmpty(toBeSent)) {
                    sendTestResultInChunksOfPreDefinedSize(buildModel, gson, pluginDescriptor, toBeSent, data);
                    toBeSent.clear();
                }
            }
        }
    }

    private static void sendTestResultInChunksOfPreDefinedSize(BuildModel buildModel, Gson gson,
                                                               PluginDescriptorImpl pluginDescriptor,
                                                               List<TestCaseResultModel> toBeSent, Map<String, Object> data) {
        LOG.log(Level.INFO, "Job Name - " + buildModel.getName() + ", Build Number - " + buildModel.getNumber() + ", test result count is " + toBeSent.size() +
                ", number of bytes is " + gson.toJson(data).getBytes().length);
        LogSender.getInstance().sendLogs(pluginDescriptor.getUrl(), gson.toJson(data).getBytes()
                , null, pluginDescriptor.getSourceCategory());
    }

    public static void sendPipelineStages(List<PipelineStageModel> stages, BuildModel buildModel) {
        PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();
        Gson gson = new Gson();
        Map<String, Object> data = new HashMap<>();

        data.put("logType", LogTypeEnum.PIPELINE_STAGES.getValue());
        data.put("name", buildModel.getName());
        data.put("number", buildModel.getNumber());

        if (CollectionUtils.isNotEmpty(stages)) {
            // send Stages based on the size
            List<PipelineStageModel> toBeSent = new LinkedList<>();
            data.put("stages", toBeSent);
            int size = gson.toJson(data).getBytes().length;
            for (PipelineStageModel pipelineStageModel : stages) {
                size = size + gson.toJson(pipelineStageModel).getBytes().length;
                if (size > MAX_DATA_SIZE) {
                    sendStagesInChunksOfPreDefinedSize(buildModel, gson, pluginDescriptor, toBeSent, data);
                    toBeSent.clear();
                }
                toBeSent.add(pipelineStageModel);
                size = gson.toJson(data).getBytes().length;
            }
            if (CollectionUtils.isNotEmpty(toBeSent)) {
                sendStagesInChunksOfPreDefinedSize(buildModel, gson, pluginDescriptor, toBeSent, data);
                toBeSent.clear();
            }
        }
    }

    private static void sendStagesInChunksOfPreDefinedSize(BuildModel buildModel, Gson gson,
                                                           PluginDescriptorImpl pluginDescriptor,
                                                           List<PipelineStageModel> toBeSent, Map<String, Object> data) {
        LOG.log(Level.INFO, "Job Name - " + buildModel.getName() + ", Build Number - " + buildModel.getNumber() + ", Stage count is " + toBeSent.size() +
                ", number of bytes is " + gson.toJson(data).getBytes().length);
        LogSender.getInstance().sendLogs(pluginDescriptor.getUrl(), gson.toJson(data).getBytes()
                , null, pluginDescriptor.getSourceCategory());
    }

    private static String format(String data) {
        if (StringUtils.isNotEmpty(data)) {
            data = data.replace("{", "(");
            return data.replace("}", ")");
        }
        return null;
    }
}
