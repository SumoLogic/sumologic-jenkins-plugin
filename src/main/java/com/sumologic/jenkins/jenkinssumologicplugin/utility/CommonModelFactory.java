package com.sumologic.jenkins.jenkinssumologicplugin.utility;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.AuditEventTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.EventSourceEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.LogTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.model.*;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import hudson.Util;
import hudson.console.ConsoleNote;
import hudson.model.*;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import jenkins.model.CauseOfInterruption;
import jenkins.model.InterruptedBuildAction;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.*;
import static com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper.sendTestResult;
import static com.sumologic.jenkins.jenkinssumologicplugin.utility.TestCaseReport.getTestCaseReport;
import static org.apache.commons.lang.reflect.MethodUtils.getAccessibleMethod;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Common Model factory to update common build information
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class CommonModelFactory {

    private static final Logger LOG = Logger.getLogger(CommonModelFactory.class.getName());

    private static LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();

    public static void populateGeneric(BuildModel buildModel, Run buildInfo, PluginDescriptorImpl pluginDescriptor, boolean isSpecificJobFlagEnabled) {

        buildModel.setLogType(LogTypeEnum.JOB_STATUS.getValue());
        buildModel.setName(buildInfo.getParent().getFullName());
        buildModel.setNumber(buildInfo.getNumber());
        buildModel.setDescription(buildInfo.getParent().getDescription());
        if (Hudson.getVersion() != null) {
            buildModel.setHudsonVersion(Hudson.getVersion().toString());
        }
        if (buildInfo.getParent() instanceof Describable) {
            String jobType = ((Describable) buildInfo.getParent()).getDescriptor().getDisplayName();
            buildModel.setJobType(jobType);
        }
        String result = buildInfo.getResult() != null ? buildInfo.getResult().toString() : "Unknown";
        buildModel.setResult(result);
        buildModel.setUser(getUserId(buildInfo));

        //Backward compatibility duration
        buildModel.setDuration(System.currentTimeMillis() - buildInfo.getStartTimeInMillis());
        buildModel.setStart(buildInfo.getStartTimeInMillis());

        buildModel.setJobStartTime(DATETIME_FORMATTER.format(buildInfo.getTimestamp()));
        buildModel.setJobRunDuration(getJobRunDuration(buildInfo));

        buildModel.setJobBuildURL(getAbsoluteUrl(buildInfo));
        buildModel.setUpstreamJobURL(getUpStreamUrl(buildInfo));
        buildModel.setTriggerCauses(getJobTriggerCauses(buildInfo));

        getLabelAndNodeName(buildInfo, buildModel);

        TestCaseModel testCaseModel = getTestResultSummary(buildInfo);
        if (testCaseModel != null && (pluginDescriptor.isJobStatusLogEnabled() || isSpecificJobFlagEnabled)
                && StringUtils.isNotEmpty(buildModel.getJobType())) {
            sendTestResult(testCaseModel, buildModel);
            testCaseModel.setTestResults(null);
            buildModel.setTestResult(testCaseModel);
        }

        Map<String, Object> parameters = getBuildVariables(buildInfo);
        if (!parameters.isEmpty()) {
            buildModel.setJobMetaData(parameters);
        }
    }

    /**
     * @param buildInfo Jenkins Job Build Information
     * @return the user who triggered the build or upstream build
     */
    public static String getUserId(Run buildInfo) {
        String userName = "anonymous";
        String triggerUserName;
        for (CauseAction action : buildInfo.getActions(CauseAction.class)) {
            if (action != null && action.getCauses() != null) {
                for (Cause cause : action.getCauses()) {
                    triggerUserName = getUsernameOrTimerORScm(cause);
                    if (triggerUserName != null) {
                        return triggerUserName;
                    } else if (cause instanceof Cause.UpstreamCause) {
                        return getUpStreamUser((Cause.UpstreamCause) cause);
                    }
                }
            }
        }
        if (buildInfo.getParent().getClass().getName().equals("hudson.maven.MavenModule")) {
            return "(maven)";
        }
        return userName;
    }

    /**
     * @param cause cause for the trigger
     * @return UserName as User ID, Timer or scm
     */
    private static String getUsernameOrTimerORScm(Cause cause) {
        if (cause instanceof Cause.UserIdCause) {
            return ((Cause.UserIdCause) cause).getUserId();
        } else if (cause instanceof TimerTrigger.TimerTriggerCause) {
            return "(timer)";
        } else if (cause instanceof SCMTrigger.SCMTriggerCause) {
            return "(scm)";
        }
        return null;
    }

    /**
     * get the user name from UpstreamCause, also recursive check top level upstreams
     *
     * @param upstreamCause Cause for the upstream Job Trigger
     * @return UserName
     */
    private static String getUpStreamUser(Cause.UpstreamCause upstreamCause) {
        for (Cause upCause : upstreamCause.getUpstreamCauses()) {
            if (upCause instanceof Cause.UpstreamCause) {
                return getUpStreamUser((Cause.UpstreamCause) upCause);
            } else {
                String userName = getUsernameOrTimerORScm(upCause);
                if (userName != null) {
                    return userName;
                }
            }
        }
        return null;
    }

    /**
     * @param buildInfo Jenkins Job Build Information
     * @return job duration
     */
    public static float getJobRunDuration(Run buildInfo) {
        float duration = buildInfo.getDuration() / 1000f;
        if (duration < 0.01f || buildInfo.isBuilding()) {
            duration = Math.max(0, (System.currentTimeMillis() - buildInfo.getStartTimeInMillis()) / 1000f);
        }
        return duration;
    }

    /**
     * @param buildInfo Jenkins Job Build Information
     * @return All the causes that triggered the Job separated by comma(,)
     */
    private static String getJobTriggerCauses(Run buildInfo) {
        Set<String> causes = new LinkedHashSet<>();
        for (CauseAction action : buildInfo.getActions(CauseAction.class)) {
            if (action != null && action.getCauses() != null) {
                for (Cause cause : action.getCauses()) {
                    causes.add(cause.getShortDescription());
                }
            }
        }
        for (InterruptedBuildAction action : buildInfo.getActions(InterruptedBuildAction.class)) {
            if (action != null && action.getCauses() != null) {
                for (CauseOfInterruption cause : action.getCauses()) {
                    causes.add(cause.getShortDescription());
                }
            }
        }
        return StringUtils.join(causes, COMMA_SEPARATOR);
    }


    /**
     * @param buildInfo Jenkins Job Build Information
     * @return URL for the JOB
     */
    public static String getAbsoluteUrl(Run buildInfo) {
        String rootUrl = Jenkins.getInstance().getRootUrl();
        if (rootUrl == null) {
            return buildInfo.getUrl();
        } else {
            return Util.encode(rootUrl + buildInfo.getUrl());
        }
    }

    /**
     * @param buildInfo Jenkins Job Build Information
     * @return the upstream job url
     */
    private static String getUpStreamUrl(Run buildInfo) {
        for (CauseAction action : buildInfo.getActions(CauseAction.class)) {
            Cause.UpstreamCause upstreamCause = action.findCause(Cause.UpstreamCause.class);
            if (upstreamCause != null) {
                return upstreamCause.getUpstreamUrl() + upstreamCause.getUpstreamBuild() + "/";
            }
        }
        return "";
    }

    /**
     * @param buildInfo  Jenkins Job Build Information
     * @param BuildModel Pipeline Job Status DTO
     */
    public static void getLabelAndNodeName(Run buildInfo, BuildModel BuildModel) {
        Executor executor = buildInfo.getExecutor();

        if (executor != null) {
            if (executor.getOwner().getNode() != null) {
                BuildModel.setLabel(executor.getOwner().getNode().getLabelString());
            }
        }
        if (buildInfo instanceof AbstractBuild) {
            String builtOnStr = ((AbstractBuild) buildInfo).getBuiltOnStr();
            if ("".equals(builtOnStr)) {
                BuildModel.setNodeName(MASTER);
            } else {
                BuildModel.setNodeName(builtOnStr);
            }
        } else {
            if (executor != null && StringUtils.isEmpty(executor.getOwner().getName())) {
                BuildModel.setNodeName(MASTER);
            }
        }
    }


    /**
     * @param buildInfo Jenkins Job Build Information
     * @return summary of failures,passes,skips, total and duration
     */
    private static TestCaseModel getTestResultSummary(Run buildInfo) {
        AbstractTestResultAction action = buildInfo.getAction(AbstractTestResultAction.class);
        if (action != null) {
            TestCaseModel testCaseModel = new TestCaseModel(action.getFailCount(), action.getTotalCount() - action.getFailCount() - action.getSkipCount(),
                    action.getSkipCount(), action.getTotalCount());

            testCaseModel.getTestResults().addAll(getTestCaseReport(buildInfo));

            testCaseModel.setTotalDuration(testCaseModel.getTestResults().stream().mapToDouble(TestCaseResultModel::getDuration).sum());
            return testCaseModel;
        }
        return null;
    }

    /**
     * @param run the build
     * @return build variables with password masked
     */
    private static Map<String, Object> getBuildVariables(Run run) {
        Map<String, Object> values = new HashMap<>();
        List<ParametersAction> parameterActions = run.getActions(ParametersAction.class);
        for (ParametersAction parameters : parameterActions) {
            for (ParameterValue p : parameters) {
                if (p == null) continue;
                if (!p.isSensitive()) {
                    values.put(p.getName(), p.getValue());
                } else {
                    values.put(p.getName(), "***");
                }
            }
        }
        return values;
    }

    public static void captureUserLoginEvent(final String userName, final AuditEventTypeEnum auditEventTypeEnum) {
        try {
            String message = String.format(auditEventTypeEnum.getMessage(), userName);
            captureAuditEvent(userName, auditEventTypeEnum, message, null);
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "An error occurred while Capturing user login Event", exception);
        }
    }

    public static void captureItemAuditEvent(AuditEventTypeEnum auditEventTypeEnum, String itemName, String itemOldValue) {
        try {
            String userName = getUserId();
            String message = "";
            if (AuditEventTypeEnum.COPIED.equals(auditEventTypeEnum) || AuditEventTypeEnum.LOCATION_CHANGED.equals(auditEventTypeEnum)) {
                message = String.format(auditEventTypeEnum.getMessage(), userName, itemName, itemOldValue);
            } else {
                message = String.format(auditEventTypeEnum.getMessage(), userName, itemName);
            }

            captureAuditEvent(userName, auditEventTypeEnum, message, null);
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "An error occurred while Capturing Item Audit Event", exception);
        }
    }

    public static void captureConfigChanges(final String fileData, final String oldFileData, final AuditEventTypeEnum auditEventTypeEnum,
                                            String fileName) {
        String userName = getUserId();
        String message = String.format(AuditEventTypeEnum.CHANGES_IN_CONFIG.getMessage(),
                userName, fileName);

        Map<String, Object> fileDetails = new HashMap<>();

        fileDetails.put("Current_File_Data", fileData);
        fileDetails.put("Old_File_Data", oldFileData);
        captureAuditEvent(userName, auditEventTypeEnum, message, fileDetails);
    }

    public static void captureAuditEvent(final String userId, final AuditEventTypeEnum auditEventTypeEnum,
                                         final String message, Map<String, Object> fileDetails) {
        String userFullName = null;
        try {
            User user = User.getById(userId, false);
            if (user != null) {
                userFullName = user.getFullName();
            } else {
                userFullName = userId;
            }
        } catch (Exception exception) {
            userFullName = userId;
        }

        AuditModel auditModel = new AuditModel(userFullName, userId, auditEventTypeEnum.getValue(),
                DATETIME_FORMATTER.format(new Date()), message, LogTypeEnum.AUDIT_EVENT.getValue(), fileDetails);

        logSenderHelper.sendAuditLogs(auditModel.toString());
    }

    private static String getUserId() {
        User user = User.current();
        if (user == null) {
            return "anonymous";
        } else {
            return user.getId();
        }
    }

    public static String getRelativeJenkinsHomePath(String configPath) {
        String jenkinsHome = Jenkins.getInstance().getRootDir().getPath();
        String relativePath = configPath;
        if (configPath.startsWith(jenkinsHome)) {
            relativePath = configPath.substring(jenkinsHome.length() + 1);
        }
        return relativePath;
    }

    public static void updateStatus(Computer computer, String eventSource) {
        try {
            SlaveModel slaveModel = new SlaveModel();
            slaveModel.setLogType(LogTypeEnum.SLAVE_EVENT.getValue());
            slaveModel.setEventTime(DATETIME_FORMATTER.format(new Date()));
            slaveModel.setEventSource(eventSource);
            getComputerStatus(computer, slaveModel);
            logSenderHelper.sendLogsToPeriodicSourceCategory(slaveModel.toString());
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "An error occurred while Capturing Slave Event", exception);
        }
    }

    public static List<SlaveModel> getNodeMonitorsDetails() {
        List<SlaveModel> slaveModels = new ArrayList<>();
        Computer[] computers = Jenkins.getInstance().getComputers();

        if (computers == null || computers.length == 0) {
            return slaveModels;
        }
        for (Computer computer : computers) {
            if (computer != null) {
                SlaveModel slaveModel = new SlaveModel();
                slaveModel.setLogType(LogTypeEnum.SLAVE_EVENT.getValue());
                slaveModel.setEventTime(DATETIME_FORMATTER.format(new Date()));
                slaveModel.setEventSource(EventSourceEnum.PERIODIC_UPDATE.getValue());
                getComputerStatus(computer, slaveModel);

                computer.getMonitorData().forEach((key, value) -> {
                    String monitorName = key.split("\\.")[2];
                    String monitorData = null;
                    if (value != null) {
                        Method method = getAccessibleMethod(value.getClass(), "toHtml", new Class<?>[0]);
                        if (method != null) {
                            try {
                                monitorData = (String) method.invoke(value, new Object[0]);
                            } catch (Exception e) {
                                monitorData = value.toString();
                            }
                        } else {
                            monitorData = value.toString();
                        }
                        Pattern compile = Pattern.compile(MONITOR_PATTERN_MATCHER, Pattern.CASE_INSENSITIVE);
                        Matcher matcher = compile.matcher(monitorData);
                        if (matcher.find()) {
                            monitorData = matcher.group(1);
                        }
                    }
                    slaveModel.getMonitorData().put(monitorName, monitorData);
                });
                slaveModels.add(slaveModel);
            }
        }
        return slaveModels;
    }

    public static void getComputerStatus(Computer computer, SlaveModel slaveModel) {
        slaveModel.setNodeName(getNodeName(computer));
        Node slaveNode = computer.getNode();
        if (slaveNode != null) {
            slaveModel.setNodeLabel(slaveNode.getLabelString());
        }
        slaveModel.setNodeStatus("updated");
        slaveModel.setNumberOfExecutors(computer.getNumExecutors());
        slaveModel.setIdle(computer.isIdle());
        AtomicInteger countFreeExecutors = new AtomicInteger();
        if (computer.getExecutors() != null) {
            computer.getExecutors().forEach(executor -> {
                if (executor.isIdle()) {
                    countFreeExecutors.incrementAndGet();
                }
            });
        }
        slaveModel.setNumberOfFreeExecutors(countFreeExecutors.get());
        slaveModel.setOnline(computer.isOnline());
        if (computer.isOffline()) {
            slaveModel.setNumberOfExecutors(0);
            slaveModel.setRemoved(true);
            slaveModel.setReasonOffline(computer.getOfflineCauseReason());
            slaveModel.setConnecting(computer.isConnecting());
        }
        slaveModel.setNodeURL(getAbsoluteUrl(computer));
    }

    private static String getAbsoluteUrl(Computer computer) {
        String rootUrl = Jenkins.getInstance().getRootUrl();
        if (rootUrl == null) {
            return computer.getUrl();
        } else {
            return Util.encode(rootUrl + computer.getUrl());
        }
    }

    private static String getNodeName(Computer computer) {
        if (computer instanceof Jenkins.MasterComputer) {
            return MASTER;
        } else {
            return computer.getName();
        }
    }

    /**
     * @param relativeURL Relative URL
     * @return URL for the JOB
     */
    public static String getAbsoluteUrl(String relativeURL) {
        String rootUrl = Jenkins.getInstance().getRootUrl();
        if (rootUrl == null) {
            return relativeURL;
        } else {
            return Util.encode(rootUrl + relativeURL);
        }
    }

    public static void sendConsoleLogs(Run run, TaskListener listener) throws IOException {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(run.getLogReader());
            AtomicReference<StringBuilder> stringBuilder = new AtomicReference<>(new StringBuilder());
            AtomicInteger count = new AtomicInteger();
            count.addAndGet(1);
            AtomicBoolean sendLogs = new AtomicBoolean(true);
            bufferedReader.lines().forEach(s -> {
                String s1 = ConsoleNote.removeNotes(s);

                // if pipeline jobs all console logs are send via other node. Only logs which are after and before pipeline
                // will be sent here
                if (s1.startsWith(START_OF_PIPELINE)) {
                    sendLogs.set(false);
                }

                if (!s1.startsWith(PIPELINE) && sendLogs.get()) {
                    stringBuilder.get().append("[").append(DATETIME_FORMATTER.format(new Date()))
                            .append("] ").append(" ")
                            .append(s1).append("\n");
                    if (count.get() % DIVIDER_FOR_MESSAGES == 0) {
                        logSenderHelper.sendConsoleLogs(stringBuilder.toString(), run.getParent().getFullName(), run.getNumber(), null);
                        stringBuilder.set(new StringBuilder());
                    }
                    count.incrementAndGet();
                }

                if (s1.startsWith(END_OF_PIPELINE)) {
                    sendLogs.set(true);
                }
            });
            logSenderHelper.sendConsoleLogs(stringBuilder.toString(), run.getParent().getFullName(), run.getNumber(), null);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = CONSOLE_ERROR + e.getMessage();
            LOG.log(Level.WARNING, errorMessage, e);
            listener.error(errorMessage);
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
    }
}
