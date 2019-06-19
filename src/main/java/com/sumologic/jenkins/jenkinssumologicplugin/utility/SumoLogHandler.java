package com.sumologic.jenkins.jenkinssumologicplugin.utility;

import com.google.gson.Gson;
import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.LogTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;
import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.skipLoggerNames;

public class SumoLogHandler extends Handler {

    private PluginDescriptorImpl pluginDescriptor;
    private Gson gson;
    private LogSenderHelper logSenderHelper;
    private LogRecordFormatter logRecordFormatter;

    public static SumoLogHandler getInstance() {
        return new SumoLogHandler();
    }

    public SumoLogHandler() {
        pluginDescriptor = PluginDescriptorImpl.getInstance();
        gson = new Gson();
        logRecordFormatter = new LogRecordFormatter();
        logSenderHelper = LogSenderHelper.getInstance();
    }

    @Override
    public void publish(LogRecord record) {
        if (!pluginDescriptor.isHandlerStarted() || !isLoggable(record)) {
            return;
        }
        if (pluginDescriptor.isPeriodicLogEnabled()) {
            String message = logRecordFormatter.formatRecord(record);
            logSenderHelper.sendLogsToPeriodicSourceCategory(message);
        }
    }



    @Override
    public void flush() {
        //Send the data to sumo logic
    }

    @Override
    public void close() throws SecurityException {
        //close necessary things
    }

    private class LogRecordFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            return formatMessage(record);
        }

        private String formatRecord(LogRecord record) {
            Map<String, Object> logMessage = new HashMap<>();
            logMessage.put("threadId", record.getThreadID());
            logMessage.put("logType", LogTypeEnum.JENKINS_LOG.getValue());
            logMessage.put("eventTime", DATETIME_FORMATTER.format(new Date()));
            logMessage.put("logLevel", record.getLevel().getName());
            logMessage.put("logMessage", formatMessage(record));
            logMessage.put("logSource", record.getLoggerName());
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                logMessage.put("logStackTrace", sw.toString());
            }
            return gson.toJson(logMessage);
        }
    }

    private static class LogRecordFilter implements Filter {
        @Override
        public boolean isLoggable(LogRecord record) {
            String logSource = record.getSourceClassName();
            String loggerName = record.getLoggerName();
            if (logSource == null || loggerName == null) {
                return false;
            }

            for (String name : skipLoggerNames) {
                if (loggerName.startsWith(name) || logSource.startsWith(name)) {
                    return false;
                }
            }

            if (record.getThrown() != null) {
                StackTraceElement[] cause = record.getThrown().getStackTrace();
                for (StackTraceElement element : cause) {
                    if (element.getClassName().equals(SumoLogHandler.class.getName())) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
