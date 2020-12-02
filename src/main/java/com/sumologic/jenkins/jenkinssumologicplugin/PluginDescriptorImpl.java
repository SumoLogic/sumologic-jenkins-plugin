package com.sumologic.jenkins.jenkinssumologicplugin;

import com.google.gson.Gson;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.EventSourceEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.LogTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.metrics.SumoMetricDataPublisher;
import com.sumologic.jenkins.jenkinssumologicplugin.model.PluginConfiguration;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSender;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import com.sumologic.jenkins.jenkinssumologicplugin.utility.SumoLogHandler;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.init.Initializer;
import hudson.init.TermMilestone;
import hudson.init.Terminator;
import hudson.model.AbstractProject;
import hudson.remoting.Channel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.security.SlaveToMasterCallable;
import jenkins.util.Timer;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.StatusLine;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
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

/**
 * Sumo Logic plugin for Jenkins model.
 * Provides options to parametrize plugin.
 * <p>
 * Created by deven on 7/8/15.
 * Contributors: lukasz, Sourabh Jain
 */
@Extension
public final class PluginDescriptorImpl extends BuildStepDescriptor<Publisher> {

    private Secret url;
    private transient SumoMetricDataPublisher sumoMetricDataPublisher;
    private static LogSenderHelper logSenderHelper = null;
    private String queryPortal;
    private String sourceCategory;
    private String metricDataPrefix;
    private boolean auditLogEnabled;
    private boolean keepOldConfigData;
    private boolean metricDataEnabled;
    private boolean periodicLogEnabled;
    private boolean jobStatusLogEnabled;
    private boolean jobConsoleLogEnabled;
    private boolean scmLogEnabled;

    public PluginDescriptorImpl() {
        super(SumoBuildNotifier.class);
        load();
        sumoMetricDataPublisher = new SumoMetricDataPublisher();
        if (metricDataEnabled && metricDataPrefix != null) {
            getSumoMetricDataPublisher().stopReporter();
            getSumoMetricDataPublisher().publishMetricData(metricDataPrefix);
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
        return (PluginDescriptorImpl) Jenkins.get().getDescriptor(SumoBuildNotifier.class);
    }

    public static PluginConfiguration getPluginConfiguration() {
        Channel channel = Channel.current();
        if (channel != null) {
            try {
                return channel.call(new PluginConfigurationFromMain());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        PluginDescriptorImpl pluginDescriptor = (PluginDescriptorImpl) Jenkins.get().getDescriptor(SumoBuildNotifier.class);
        assert pluginDescriptor != null;
        return new PluginConfiguration(pluginDescriptor);
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
        url = Secret.fromString(formData.getString("url"));
        queryPortal = StringUtils.isNotEmpty(formData.getString("queryPortal")) ? formData.getString("queryPortal") : "service.sumologic.com";

        sourceCategory = StringUtils.isNotEmpty(formData.getString("sourceCategory")) ? formData.getString("sourceCategory") : "jenkinsSourceCategory";

        metricDataPrefix = StringUtils.isNotEmpty(formData.getString("metricDataPrefix")) ? formData.getString("metricDataPrefix") : "jenkinsMetricDataPrefix";

        auditLogEnabled = formData.getBoolean("auditLogEnabled");
        metricDataEnabled = formData.getBoolean("metricDataEnabled");
        periodicLogEnabled = formData.getBoolean("periodicLogEnabled");
        jobStatusLogEnabled = formData.getBoolean("jobStatusLogEnabled");
        jobConsoleLogEnabled = formData.getBoolean("jobConsoleLogEnabled");
        scmLogEnabled = formData.getBoolean("scmLogEnabled");
        keepOldConfigData = formData.getBoolean("keepOldConfigData");

        save();
        if (metricDataEnabled && metricDataPrefix != null) {
            getSumoMetricDataPublisher().stopReporter();
            getSumoMetricDataPublisher().publishMetricData(metricDataPrefix);
        }
        if (!metricDataEnabled) {
            getSumoMetricDataPublisher().stopReporter();
        }
        return configOk;
    }

    @Terminator(after = TermMilestone.STARTED)
    @Restricted(NoExternalUse.class)
    public static void shutdown() {
        PluginDescriptorImpl pluginDescriptor = checkIfPluginInUse();
        pluginDescriptor.getSumoMetricDataPublisher().stopReporter();

        Logger.getLogger("").removeHandler(SumoLogHandler.getInstance());

        Map<String, Object> shutDown = new HashMap<>();
        shutDown.put("logType", LogTypeEnum.AGENT_EVENT.getValue());
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
            StatusLine statusLine = LogSender.getInstance().testHTTPUrl(url);
            if (200 == statusLine.getStatusCode()) {
                return FormValidation.ok("Success");
            } else {
                return FormValidation.error("URL not valid with message " + statusLine.getReasonPhrase());
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

    public String getMetricDataPrefix() {
        return metricDataPrefix;
    }

    public void setMetricDataPrefix(String metricDataPrefix) {
        this.metricDataPrefix = metricDataPrefix;
    }

    public String getUrl() {
        return url.getPlainText();
    }

    public void setUrl(Secret url) {
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

    private static class PluginConfigurationFromMain extends SlaveToMasterCallable<PluginConfiguration, IllegalStateException> {
        protected static final long serialVersionUID = 1L;

        @Override
        public PluginConfiguration call() throws IllegalStateException {
            PluginDescriptorImpl pluginDescriptor = (PluginDescriptorImpl) Jenkins.get().getDescriptor(SumoBuildNotifier.class);
            assert pluginDescriptor != null;
            return new PluginConfiguration(pluginDescriptor);
        }
    }
}
