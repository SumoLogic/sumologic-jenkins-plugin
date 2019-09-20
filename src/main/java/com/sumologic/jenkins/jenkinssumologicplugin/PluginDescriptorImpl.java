package com.sumologic.jenkins.jenkinssumologicplugin;

import com.google.gson.Gson;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.EventSourceEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.LogTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.integration.SumoRestAPICallImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.metrics.SumoMetricDataPublisher;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSender;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import com.sumologic.jenkins.jenkinssumologicplugin.utility.SumoLogHandler;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.init.Initializer;
import hudson.init.TermMilestone;
import hudson.init.Terminator;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.util.Timer;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;
import static hudson.init.InitMilestone.JOB_LOADED;
import static hudson.init.InitMilestone.PLUGINS_STARTED;

/**
 * Sumo Logic plugin for Jenkins model.
 * Provides options to parametrize plugin.
 * <p>
 * Created by deven on 7/8/15.
 * Contributors: lukasz, Sourabh Jain
 */
@Extension
public final class PluginDescriptorImpl extends BuildStepDescriptor<Publisher> {

    private String url;
    private transient SumoMetricDataPublisher sumoMetricDataPublisher;
    private static LogSenderHelper logSenderHelper = null;
    private String queryPortal;
    private String sourceCategory;
    private String jenkinsMasterName;
    private boolean auditLogEnabled;
    private boolean keepOldConfigData;
    private boolean metricDataEnabled;
    private boolean periodicLogEnabled;
    private boolean jobStatusLogEnabled;
    private boolean jobConsoleLogEnabled;
    private boolean scmLogEnabled;
    private String password = null;
    private String email = null;
    private boolean createDashboards;
    private String buildDashboardId;
    private String jobDashboardId;

    public PluginDescriptorImpl() {
        super(SumoBuildNotifier.class);
        load();
        sumoMetricDataPublisher = new SumoMetricDataPublisher();
        if (metricDataEnabled && jenkinsMasterName != null) {
            getSumoMetricDataPublisher().stopReporter();
            getSumoMetricDataPublisher().publishMetricData(jenkinsMasterName);
        }
        if (!metricDataEnabled) {
            getSumoMetricDataPublisher().stopReporter();
        }
        setLogSenderHelper(LogSenderHelper.getInstance());
    }

    private static void setLogSenderHelper(LogSenderHelper logSenderHelper) {
        PluginDescriptorImpl.logSenderHelper = logSenderHelper;
    }

    public static PluginDescriptorImpl getInstance() {
        return (PluginDescriptorImpl) Jenkins.getInstance().getDescriptor(SumoBuildNotifier.class);
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "Sumo Logic build logger";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        boolean configOk = super.configure(req, formData);
        url = formData.getString("url");
        queryPortal = StringUtils.isNotEmpty(formData.getString("queryPortal")) ? formData.getString("queryPortal") : "service.sumologic.com";

        sourceCategory = StringUtils.isNotEmpty(formData.getString("sourceCategory")) ? formData.getString("sourceCategory") : "jenkinsSourceCategory";

        jenkinsMasterName = StringUtils.isNotEmpty(formData.getString("jenkinsMasterName")) ? formData.getString("jenkinsMasterName") : "jenkinsMasterName";

        auditLogEnabled = formData.getBoolean("auditLogEnabled");
        metricDataEnabled = formData.getBoolean("metricDataEnabled");
        periodicLogEnabled = formData.getBoolean("periodicLogEnabled");
        jobStatusLogEnabled = formData.getBoolean("jobStatusLogEnabled");
        jobConsoleLogEnabled = formData.getBoolean("jobConsoleLogEnabled");
        scmLogEnabled = formData.getBoolean("scmLogEnabled");
        keepOldConfigData = formData.getBoolean("keepOldConfigData");
        createDashboards = formData.getBoolean("createDashboards");

        email = formData.containsKey("email") && StringUtils.isNotEmpty(formData.getString("email")) ? formData.getString("email") : null;
        password = formData.containsKey("password") && StringUtils.isNotEmpty(formData.getString("password")) ? formData.getString("password") : null;

        save();
        if (metricDataEnabled && jenkinsMasterName != null) {
            getSumoMetricDataPublisher().stopReporter();
            getSumoMetricDataPublisher().publishMetricData(jenkinsMasterName);
        }
        if (!metricDataEnabled) {
            getSumoMetricDataPublisher().stopReporter();
        }

        SumoRestAPICallImpl.getInstance().installJenkinsApp();

        return configOk;
    }

    @Terminator(after = TermMilestone.STARTED)
    @Restricted(NoExternalUse.class)
    public static void shutdown() {
        PluginDescriptorImpl pluginDescriptor = checkIfPluginInUse();
        pluginDescriptor.getSumoMetricDataPublisher().stopReporter();

        Logger.getLogger("").removeHandler(SumoLogHandler.getInstance());

        Map<String, Object> shutDown = new HashMap<>();
        shutDown.put("logType", LogTypeEnum.SLAVE_EVENT.getValue());
        shutDown.put("eventTime", DATETIME_FORMATTER.format(new Date()));
        shutDown.put("eventSource", EventSourceEnum.SHUTDOWN.getValue());
        Gson gson = new Gson();
        logSenderHelper.sendLogsToPeriodicSourceCategory(gson.toJson(shutDown));
    }

    private static PluginDescriptorImpl checkIfPluginInUse() {
        PluginDescriptorImpl pluginDescriptor = ExtensionList.lookup(BuildStepDescriptor.class).get(PluginDescriptorImpl.class);
        if (pluginDescriptor == null) {
            throw new IllegalStateException("Sumo Logic Publisher is not part of the extension list");
        }
        return pluginDescriptor;
    }


    public FormValidation doCheckUrl(@QueryParameter String value) {
        if (value.isEmpty()) {
            return FormValidation.error("You must provide an URL.");
        }

        try {
            new URL(value);
        } catch (final MalformedURLException e) {
            return FormValidation.error("This is not a valid URL.");
        }

        return FormValidation.ok();
    }

    public FormValidation doTestURL(@QueryParameter("url") String url) {
        try {
            StatusLine output = LogSender.getInstance().testHTTPUrl(url);
            if (200 == output.getStatusCode()) {
                return FormValidation.ok("Success");
            } else {
                return FormValidation.error("URL not valid with message " + output.getReasonPhrase());
            }
        } catch (Exception e) {
            return FormValidation.error("Failure : " + e.getMessage());
        }
    }

    public FormValidation doCheckPassword(@QueryParameter String value) {
        if (value.isEmpty()) {
            return FormValidation.error("You must provide an Password.");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckEmail(@QueryParameter String value) {
        if (value.isEmpty()) {
            return FormValidation.error("You must provide an Email.");
        }
        return FormValidation.ok();
    }

    public FormValidation doTestCredentials(@QueryParameter("email") String email,
                                            @QueryParameter("password") String password) {
        try {
            String output = SumoRestAPICallImpl.getInstance().getSessionForUserAfterLoginWithCredentials(email, password);
            if (StringUtils.isNotEmpty(output)) {
                return FormValidation.ok("Success");
            } else {
                return FormValidation.error("Invalid Credentials");
            }
        } catch (Exception e) {
            return FormValidation.error("Failure : " + e.getMessage());
        }
    }

    public SumoMetricDataPublisher getSumoMetricDataPublisher() {
        return sumoMetricDataPublisher;
    }

    public void setSumoMetricDataPublisher(SumoMetricDataPublisher sumoMetricDataPublisher) {
        this.sumoMetricDataPublisher = sumoMetricDataPublisher;
    }

    public String getQueryPortal() {
        return queryPortal;
    }

    public void setQueryPortal(String queryPortal) {
        this.queryPortal = queryPortal;
    }

    public boolean isAuditLogEnabled() {
        return auditLogEnabled;
    }

    public void setAuditLogEnabled(boolean auditLogEnabled) {
        this.auditLogEnabled = auditLogEnabled;
    }

    public boolean isMetricDataEnabled() {
        return metricDataEnabled;
    }

    public void setMetricDataEnabled(boolean metricDataEnabled) {
        this.metricDataEnabled = metricDataEnabled;
    }

    public boolean isPeriodicLogEnabled() {
        return periodicLogEnabled;
    }

    public void setPeriodicLogEnabled(boolean periodicLogEnabled) {
        this.periodicLogEnabled = periodicLogEnabled;
    }

    public boolean isJobStatusLogEnabled() {
        return jobStatusLogEnabled;
    }

    public void setJobStatusLogEnabled(boolean jobStatusLogEnabled) {
        this.jobStatusLogEnabled = jobStatusLogEnabled;
    }

    public boolean isJobConsoleLogEnabled() {
        return jobConsoleLogEnabled;
    }

    public void setJobConsoleLogEnabled(boolean jobConsoleLogEnabled) {
        this.jobConsoleLogEnabled = jobConsoleLogEnabled;
    }

    public boolean isScmLogEnabled() {
        return scmLogEnabled;
    }

    public void setScmLogEnabled(boolean scmLogEnabled) {
        this.scmLogEnabled = scmLogEnabled;
    }

    public String getJenkinsMasterName() {
        return jenkinsMasterName;
    }

    public void setJenkinsMasterName(String jenkinsMasterName) {
        this.jenkinsMasterName = jenkinsMasterName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSourceCategory() {
        return sourceCategory;
    }

    public void setSourceCategory(String sourceCategory) {
        this.sourceCategory = sourceCategory;
    }

    public boolean isKeepOldConfigData() {
        return keepOldConfigData;
    }

    public void setKeepOldConfigData(boolean keepOldConfigData) {
        this.keepOldConfigData = keepOldConfigData;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isCreateDashboards() {
        return createDashboards;
    }

    public void setCreateDashboards(boolean createDashboards) {
        this.createDashboards = createDashboards;
    }

    public String getBuildDashboardId() {
        return buildDashboardId;
    }

    public void setBuildDashboardId(String buildDashboardId) {
        this.buildDashboardId = buildDashboardId;
    }

    public String getJobDashboardId() {
        return jobDashboardId;
    }

    public void setJobDashboardId(String jobDashboardId) {
        this.jobDashboardId = jobDashboardId;
    }

    private boolean isHandlerStarted;

    public boolean isHandlerStarted() {
        return isHandlerStarted;
    }

    public void setHandlerStarted(boolean handlerStarted) {
        isHandlerStarted = handlerStarted;
    }

    //Handler to get all the jenkins Logs
    @Initializer(after = JOB_LOADED)
    public void startSumoJenkinsLogHandler() {
        Timer.get().schedule(PluginDescriptorImpl.getInstance()::registerHandler, 3, TimeUnit.MINUTES);
    }

    @Initializer(after = PLUGINS_STARTED)
    public void startJenkinsApp() {
        SumoRestAPICallImpl.getInstance().installJenkinsApp();
    }

    public void registerHandler() {
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof SumoLogHandler) {
                return;
            }
        }
        Logger.getLogger("").addHandler(SumoLogHandler.getInstance());
        isHandlerStarted = true;
    }
}
