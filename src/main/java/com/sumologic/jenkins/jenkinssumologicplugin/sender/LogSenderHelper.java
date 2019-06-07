package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.LogTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.PipelineStageModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.TestCaseModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.TestCaseResultModel;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            if (count % DIVIDER_FOR_MESSAGES == 0) {
                convertedMessages.add(stringBuilder.toString());
                stringBuilder = new StringBuilder();
            }
            count++;
        }
        convertedMessages.add(stringBuilder.toString());

        return convertedMessages;
    }

    public void sendJobStatusBySeparatingTestResultAndStages(BuildModel buildModel) {
        PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();
        Gson gson = new Gson();

        sendTestResult(buildModel, pluginDescriptor, gson);
        sendPipelineStages(buildModel, pluginDescriptor, gson);

        if(CollectionUtils.isNotEmpty(buildModel.getStages())){
            buildModel.getStages().clear();
        }
        if(buildModel.getTestResult() != null && CollectionUtils.isNotEmpty(buildModel.getTestResult().getTestResults())){
            buildModel.getTestResult().getTestResults().clear();
        }

        LogSender.getInstance().sendLogs(pluginDescriptor.getUrl(), buildModel.toJson().getBytes()
                , null, pluginDescriptor.getSourceCategory());
    }

    private void sendTestResult(BuildModel buildModel, PluginDescriptorImpl pluginDescriptor, Gson gson) {
        Map<String, Object> data = new HashMap<>();

        data.put("logType", LogTypeEnum.TEST_RESULT.getValue());
        data.put("name", buildModel.getName());
        data.put("number", buildModel.getNumber());

        if (buildModel.getTestResult() != null) {
            TestCaseModel testResult = buildModel.getTestResult();
            if (CollectionUtils.isNotEmpty(testResult.getTestResults())) {
                List<TestCaseResultModel> testResults = testResult.getTestResults();

                //100 test cases at a time
                List<List<TestCaseResultModel>> partition = Lists.partition(testResults, 100);
                for(List<TestCaseResultModel> testCaseResultModels : partition){
                    testCaseResultModels.forEach(testCase -> {
                        if("Failed".equals(testCase.getStatus())){
                            testCase.setErrorDetails(format(testCase.getErrorDetails()));
                            testCase.setErrorStackTrace(format(testCase.getErrorStackTrace()));
                        }
                    });
                    data.put("testResult", testCaseResultModels);
                    LogSender.getInstance().sendLogs(pluginDescriptor.getUrl(), gson.toJson(data).getBytes()
                            , null, pluginDescriptor.getSourceCategory());
                }
            }
        }
    }

    private void sendPipelineStages(BuildModel buildModel, PluginDescriptorImpl pluginDescriptor, Gson gson) {
        Map<String, Object> data = new HashMap<>();

        data.put("logType", LogTypeEnum.PIPELINE_STAGES.getValue());
        data.put("name", buildModel.getName());
        data.put("number", buildModel.getNumber());

        if (CollectionUtils.isNotEmpty(buildModel.getStages())) {
            List<PipelineStageModel> stages = buildModel.getStages();

            //20 Stages at a time
            List<List<PipelineStageModel>> partition = Lists.partition(stages, 20);
            for(List<PipelineStageModel> stageModels : partition){
                data.put("stages", stageModels);
                LogSender.getInstance().sendLogs(pluginDescriptor.getUrl(), gson.toJson(data).getBytes()
                        , null, pluginDescriptor.getSourceCategory());
            }
        }
    }

    private String format(String data) {
        if(StringUtils.isNotEmpty(data)){
            data = data.replace("{", "(");
            return data.replace("}", ")");
        }
        return null;
    }
}
