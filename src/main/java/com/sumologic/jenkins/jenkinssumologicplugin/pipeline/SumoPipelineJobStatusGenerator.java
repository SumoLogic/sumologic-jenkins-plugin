package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.*;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import jenkins.model.CauseOfInterruption;
import jenkins.model.InterruptedBuildAction;
import jenkins.model.Jenkins;
import jenkins.triggers.SCMTriggerItem;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.pipeline.SumoConstants.COMMA_SEPARATOR;
import static com.sumologic.jenkins.jenkinssumologicplugin.pipeline.SumoConstants.MASTER;

class SumoPipelineJobStatusGenerator {
    private static final Logger LOG = Logger.getLogger(SumoPipelineJobStatusGenerator.class.getName());

    static PipelineStatusDTO generateJobStatusInformation(final Run buildInfo) {
        final PipelineStatusDTO pipelineStatusDTO = new PipelineStatusDTO();

        pipelineStatusDTO.setLogType(LogTypeEnum.JOB_STATUS.getValue());
        pipelineStatusDTO.setBuildNumber(buildInfo.getNumber());
        pipelineStatusDTO.setJobName(buildInfo.getParent().getFullName());
        pipelineStatusDTO.setUser(getUserName(buildInfo));
        pipelineStatusDTO.setJobStartTime(buildInfo.getTimestampString2());
        if (buildInfo.getParent() instanceof Describable) {
            String jobType = ((Describable) buildInfo.getParent()).getDescriptor().getDisplayName();
            pipelineStatusDTO.setJobType(jobType);
        }
        pipelineStatusDTO.setJobRunDuration(getJobRunDuration(buildInfo));
        if(buildInfo.getResult() != null){
            pipelineStatusDTO.setJobResult(buildInfo.getResult().toString());
        }
        pipelineStatusDTO.setJobBuildURL(getAbsoluteUrl(buildInfo));
        pipelineStatusDTO.setUpstreamJobURL(getUpStreamUrl(buildInfo));
        pipelineStatusDTO.setTriggerCauses(getJobTriggerCauses(buildInfo));

        getLabelAndNodeName(buildInfo, pipelineStatusDTO);

        TestCaseDTO testCaseDTO = getTestResultSummary(buildInfo);
        if (testCaseDTO != null) {
            pipelineStatusDTO.setTestResult(testCaseDTO);
        }

        if (buildInfo instanceof AbstractBuild) {
            AbstractBuild build = (AbstractBuild) buildInfo;
            List<String> changelog = getChangeLog(build);
            if (!changelog.isEmpty()) {
                pipelineStatusDTO.setChangeLogDetails(changelog);
            }
        }

        for (SumoPipelineJobIdentifier extendListener : SumoPipelineJobIdentifier.canApply(buildInfo)) {
            try {
                List<PipelineStageDTO> stages = extendListener.extractPipelineStages(buildInfo);
                if (CollectionUtils.isNotEmpty(stages)) {
                    pipelineStatusDTO.setStages(stages);
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "failed to extract job info", e);
            }
        }

        Map<String, Object> parameters = getBuildVariables(buildInfo);
        if (!parameters.isEmpty()) {
            pipelineStatusDTO.setPipelineMetaData(parameters);
        }

        return pipelineStatusDTO;
    }

    /**
     *
     * @param buildInfo Jenkins Job Build Information
     * @return All the causes that triggered the Job separated by comma(,)
     */
    private static String getJobTriggerCauses(Run buildInfo) {
        Set<String> causes = new LinkedHashSet<>();
        for (CauseAction action : buildInfo.getActions(CauseAction.class)) {
            if(action != null && action.getCauses() != null){
                for (Cause cause : action.getCauses()) {
                    causes.add(cause.getShortDescription());
                }
            }
        }
        for (InterruptedBuildAction action : buildInfo.getActions(InterruptedBuildAction.class)) {
            if(action != null && action.getCauses() != null){
                for (CauseOfInterruption cause : action.getCauses()) {
                    causes.add(cause.getShortDescription());
                }
            }
        }
        return StringUtils.join(causes, COMMA_SEPARATOR);
    }

    /**
     * @param buildInfo Jenkins Job Build Information
     * @return the user who triggered the build or upstream build
     */
    private static String getUserName(Run buildInfo) {
        String userName = "anonymous";
        if (buildInfo.getParent().getClass().getName().equals("hudson.maven.MavenModule")) {
            return "(maven)";
        }

        String triggerUserName;
        for (CauseAction action : buildInfo.getActions(CauseAction.class)) {
            if(action != null && action.getCauses() != null){
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
     *
     * @param cause cause for the trigger
     * @return UserName as User ID, Timer or scm
     */
    private static String getUsernameOrTimerORScm(Cause cause) {
        if (cause instanceof Cause.UserIdCause) {
            return ((Cause.UserIdCause) cause).getUserName();
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
     *  originally caused by:
     *  Started by upstream project "sumo_job" build number 2
     *      originally caused by:
     *      Started by user Sourabh Jain
     *
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
     *
     * @param buildInfo Jenkins Job Build Information
     * @return  URL for the JOB
     */
    private static String getAbsoluteUrl(Run buildInfo) {
        String rootUrl = Jenkins.getInstance().getRootUrl();
        if(rootUrl==null){
            return buildInfo.getUrl();
        } else {
            return Util.encode(rootUrl+buildInfo.getUrl());
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
     *
     * @param buildInfo Jenkins Job Build Information
     * @param pipelineStatusDTO Pipeline Job Status DTO
     */
    private static void getLabelAndNodeName(Run buildInfo, PipelineStatusDTO pipelineStatusDTO) {
        Executor executor = buildInfo.getExecutor();
        if (executor != null) {
            if (executor.getOwner().getNode() != null) {
                pipelineStatusDTO.setLabel(executor.getOwner().getNode().getLabelString());
            }
        }
        if (buildInfo instanceof AbstractBuild) {
            pipelineStatusDTO.setNodeName(((AbstractBuild) buildInfo).getBuiltOnStr());
        } else{
            if (executor != null && StringUtils.isEmpty(executor.getOwner().getName())) {
                pipelineStatusDTO.setNodeName(MASTER);
            }
        }
    }

    /**
     * @param buildInfo Jenkins Job Build Information
     * @return job duration
     */
    private static float getJobRunDuration(Run buildInfo) {
        float duration = buildInfo.getDuration() / 1000f;
        if (duration < 0.01f || buildInfo.isBuilding()) {
            duration = Math.max(0, (System.currentTimeMillis() - buildInfo.getStartTimeInMillis()) / 1000f);
        }
        return duration;
    }

    /**
     * @param buildInfo Jenkins Job Build Information
     * @return summary of failures,passes,skips, total and duration
     */
    private static TestCaseDTO getTestResultSummary(Run buildInfo) {
        AbstractTestResultAction action = buildInfo.getAction(AbstractTestResultAction.class);
        if(action != null){
            return new TestCaseDTO(action.getFailCount(), action.getTotalCount()-action.getFailCount()-action.getSkipCount(),
                    action.getSkipCount(), action.getTotalCount());
        }
        return null;
    }

    /**
     * @param build Jenkins job build
     * @return scm change log
     */
    private static List<String> getChangeLog(AbstractBuild build) {
        List<String> changelog = new ArrayList<>();
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
        appendScm(values, run);
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
}
