package com.sumologic.jenkins.jenkinssumologicplugin.utility;

import com.sumologic.jenkins.jenkinssumologicplugin.constants.AuditEventTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.EventSourceEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.LogTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.model.*;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import hudson.EnvVars;
import hudson.Util;
import hudson.model.*;
import hudson.node_monitors.NodeMonitor;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import jenkins.model.CauseOfInterruption;
import jenkins.model.InterruptedBuildAction;
import jenkins.model.Jenkins;
import jenkins.triggers.SCMTriggerItem;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.*;
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

    public static void populateGeneric(BuildModel buildModel, Run buildInfo) {

        buildModel.setLogType(LogTypeEnum.JOB_STATUS.getValue());
        buildModel.setName(buildInfo.getParent().getDisplayName());
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
        if (testCaseModel != null) {
            buildModel.setTestResult(testCaseModel);
        }

        if (buildInfo instanceof AbstractBuild) {
            AbstractBuild build = (AbstractBuild) buildInfo;
            List<String> changelog = getChangeLog(build);
            if (!changelog.isEmpty()) {
                buildModel.setChangeLogDetails(changelog);
            }
        }

        Map<String, Object> parameters = getBuildVariables(buildInfo);
        if (!parameters.isEmpty()) {
            buildModel.setJobMetaData(parameters);
        }

        Map<String, Object> scmInfo = new HashMap<>();
        appendScm(scmInfo, buildInfo);
        if(!scmInfo.isEmpty()){
            buildModel.setScmInfo(scmInfo);
        }
    }

    /**
     * @param buildInfo Jenkins Job Build Information
     * @return the user who triggered the build or upstream build
     */
    public static String getUserId(Run buildInfo) {
        String userName = "anonymous";
        if (buildInfo.getParent().getClass().getName().equals("hudson.maven.MavenModule")) {
            return "(maven)";
        }

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
     * e.g.
     * Started by upstream project "sumo_list" build number 47
     * originally caused by:
     * Started by upstream project "sumo_job" build number 2
     * originally caused by:
     * Started by user Sourabh Jain
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
        String rootUrl = Jenkins.get().getRootUrl();
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
     * @param build Jenkins job build
     * @return scm change log
     */
    private static List<String> getChangeLog(AbstractBuild build) {
        List<String> changelog = new ArrayList<>();
        LOG.info("Is this coming here or not");
        if (build.hasChangeSetComputed()) {
            ChangeLogSet<? extends ChangeLogSet.Entry> changeSet = build.getChangeSet();
            for (ChangeLogSet.Entry entry : changeSet) {
                String sbr = entry.getTimestamp() +
                        " " + "commit:" + entry.getCommitId() +
                        " " + "author:" + entry.getAuthor() +
                        " " + "message:" + entry.getMsg();
                changelog.add(sbr);
            }
        }
        return changelog;
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

    private static void appendScm(Map eventToAppend, Run run) {
        Map<String, Object> scmInfo = getScmInfo(run);
        //append scm info build parameter if no conflicts
        for (Map.Entry<String, Object> scmEntry : scmInfo.entrySet()) {
            if (!eventToAppend.containsKey(scmEntry.getKey())) {
                eventToAppend.put(scmEntry.getKey(), scmEntry.getValue());
            }
        }
    }

    private static Map<String, Object> getScmInfo(Run build) {
        SCMTriggerItem scmTrigger = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(build.getParent());
        if (scmTrigger == null) {
            return Collections.emptyMap();
        }
        Collection<? extends SCM> scmConfigs = scmTrigger.getSCMs();
        Map<String, Object> event = new HashMap<>();
        Map<String, Object> singleEvent = new HashMap<>();
        EnvVars envVars = new EnvVars();
        for (SCM scm : scmConfigs) {
            if (build instanceof AbstractBuild) {
                scm.buildEnvVars((AbstractBuild) build, envVars);
            }
            String scmName = scm.getClass().getName();
            if (!event.containsKey(scmName)) {
                singleEvent = getScmInfo(scmName, envVars);
                event.put(scmName, singleEvent);
        }
        }
        if (event.size() == 1) {
            return singleEvent;
        } else { //there are multiple scm
        return event;
        }
    }

    /**
     * @param scmName scm class name
     * @param envVars environment variables
     * @return scm information, we only support git,svn and p4
     */
    private static Map<String, Object> getScmInfo(String scmName, EnvVars envVars) {
        Map<String, Object> event = new HashMap<>();
        //not support GIT_URL_N or SVN_URL_n
        // scm can be found at https://wiki.jenkins-ci.org/display/JENKINS/Plugins
        switch (scmName) {
            case "hudson.plugins.git.GitSCM":
                event.put("scm", "git");
                event.put("scm_url", getScmURL(envVars, "GIT_URL"));
                event.put("branch", envVars.get("GIT_BRANCH"));
                event.put("revision", envVars.get("GIT_COMMIT"));
                break;
            case "hudson.scm.SubversionSCM":
                event.put("scm", "svn");
                event.put("scm_url", getScmURL(envVars, "SVN_URL"));
                event.put("revision", envVars.get("SVN_REVISION"));
                break;
            case "org.jenkinsci.plugins.p4.PerforceScm":
                event.put("scm", "p4");
                event.put("p4_client", envVars.get("P4_CLIENT"));
                event.put("revision", envVars.get("P4_CHANGELIST"));
                break;
            case "hudson.plugins.mercurial.MercurialSCM":
                event.put("scm", "hg");
                event.put("scm_url", envVars.get("MERCURIAL_REPOSITORY_URL"));
                event.put("branch", envVars.get("MERCURIAL_REVISION_BRANCH"));
                event.put("revision", envVars.get("MERCURIAL_REVISION"));
                break;
            case "hudson.scm.NullSCM":
                break;
            default:
                event.put("scm", scmName);
        }
        return event;
    }

    /**
     * @param envVars environment variables
     * @param prefix  scm prefix, such as GIT_URL, SVN_URL
     * @return parsed scm urls from build env, e.g. GIT_URL_1, GIT_URL_2, ... GIT_URL_10 or GIT_URL
     */
    private static String getScmURL(EnvVars envVars, String prefix) {
        String value = envVars.get(prefix);
        if (value == null) {
            List<String> urls = new ArrayList<>();
            //just probe max 10 url
            for (int i = 0; i < 10; i++) {
                String probe_url = envVars.get(prefix + "_" + i);
                if (probe_url != null) {
                    urls.add(Util.replaceMacro(probe_url, envVars));
                } else {
                    break;
                }
            }
            if (!urls.isEmpty()) {
                value = StringUtils.join(urls, ",");
            }
        } else {
            value = Util.replaceMacro(value, envVars);
        }
        return value;
    }

    public static void captureUserLoginEvent(final String userName, final AuditEventTypeEnum auditEventTypeEnum) {
        String message = String.format(auditEventTypeEnum.getMessage(), userName);
        captureAuditEvent(userName, auditEventTypeEnum, message, null);
    }

    public static void captureItemAuditEvent(AuditEventTypeEnum auditEventTypeEnum, String itemName, String itemOldValue) {
        String userName = getUserId();
        String message = "";
        if (AuditEventTypeEnum.COPIED.equals(auditEventTypeEnum) || AuditEventTypeEnum.LOCATION_CHANGED.equals(auditEventTypeEnum)) {
            message = String.format(auditEventTypeEnum.getMessage(), userName, itemName, itemOldValue);
        } else {
            message = String.format(auditEventTypeEnum.getMessage(), userName, itemName);
        }

        captureAuditEvent(userName, auditEventTypeEnum, message, null);
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
        User user = User.getById(userId, false);
        String userFullName = userId;
        if (user != null) {
            userFullName = user.getFullName();
        }
        AuditModel auditModel = new AuditModel(userFullName, userId, auditEventTypeEnum.getValue(),
                DATETIME_FORMATTER.format(new Date()), message, LogTypeEnum.AUDIT_EVENT.getValue(), fileDetails);

        logSenderHelper.sendLogsToPeriodicSourceCategory(auditModel.toString());
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
        String jenkinsHome = Jenkins.get().getRootDir().getPath();
        String relativePath = configPath;
        if (configPath.startsWith(jenkinsHome)) {
            relativePath = configPath.substring(jenkinsHome.length() + 1);
        }
        return relativePath;
    }

    public static void updateStatus(Computer computer, String eventSource) {
        SlaveModel slaveModel = new SlaveModel();
        slaveModel.setLogType(LogTypeEnum.SLAVE_EVENT.getValue());
        slaveModel.setEventTime(DATETIME_FORMATTER.format(new Date()));
        slaveModel.setEventSource(eventSource);
        getComputerStatus(computer, slaveModel);
        logSenderHelper.sendLogsToPeriodicSourceCategory(slaveModel.toString());
    }

    public static List<SlaveModel> getNodeMonitorsDetails() {
        List<SlaveModel> slaveModels = new ArrayList<>();
        Computer[] computers = Jenkins.get().getComputers();

        if (computers == null || computers.length == 0) {
            return slaveModels;
        }
        Collection<NodeMonitor> monitors = ComputerSet.getMonitors();
        for (Computer computer : computers) {
            if (computer != null) {
                SlaveModel slaveModel = new SlaveModel();
                slaveModel.setLogType(LogTypeEnum.SLAVE_EVENT.getValue());
                slaveModel.setEventTime(DATETIME_FORMATTER.format(new Date()));
                slaveModel.setEventSource(EventSourceEnum.PERIODIC_UPDATE.getValue());
                getComputerStatus(computer, slaveModel);
                for (NodeMonitor monitor : monitors) {
                    slaveModel.getMonitorData().putAll(getMonitorData(computer, monitor));
                }
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
        slaveModel.setOnline(computer.isOnline());
        if (computer.isOffline()) {
            slaveModel.setNumberOfExecutors(0);
            slaveModel.setRemoved(true);
            slaveModel.setReasonOffline(computer.getOfflineCauseReason());
            slaveModel.setConnecting(computer.isConnecting());
        }
        slaveModel.setNodeURL(getAbsoluteUrl(computer));
        long connectTime = computer.getConnectTime();
    }

    private static String getAbsoluteUrl(Computer computer) {
        String rootUrl = Jenkins.get().getRootUrl();
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

    private static Map<String, Object> getMonitorData(Computer computer, NodeMonitor monitor) {
        Map<String, Object> monitorDetails = new HashMap<>();
        Object data = monitor.data(computer);
        if (data != null) {
            String monitorName = monitor.getClass().getSimpleName();
            String monitorData;
            Method method = getAccessibleMethod(data.getClass(), "toHtml", new Class<?>[0]);
            if (method != null) {
                try {
                    monitorData = (String) method.invoke(data, new Object[0]);
                } catch (Exception e) {
                    monitorData = data.toString();
                }
            } else {
                monitorData = data.toString();
            }
            Pattern compile = Pattern.compile(MONITOR_PATTERN_MATCHER, Pattern.CASE_INSENSITIVE);
            Matcher matcher = compile.matcher(monitorData);
            if (matcher.find()) {
                monitorDetails.put(monitorName, "warning:" + matcher.group(1));
            } else {
                monitorDetails.put(monitorName, monitorData);
            }
        }
        return monitorDetails;
    }
}
