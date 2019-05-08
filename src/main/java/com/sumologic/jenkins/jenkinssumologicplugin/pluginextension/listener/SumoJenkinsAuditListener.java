package com.sumologic.jenkins.jenkinssumologicplugin.pluginextension.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.AuditEventTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.model.AuditModel;
import hudson.Extension;
import hudson.model.User;
import jenkins.security.SecurityListener;
import org.acegisecurity.userdetails.UserDetails;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;

@Extension
public class SumoJenkinsAuditListener extends SecurityListener {
    private static final Logger LOG = Logger.getLogger(SumoJenkinsAuditListener.class.getName());

    @Override
    protected void authenticated(@Nonnull UserDetails details) {
        captureUserAuditEvent(details.getUsername(), AuditEventTypeEnum.LOGIN);
    }

    @Override
    protected void failedToAuthenticate(@Nonnull String username) {
        captureUserAuditEvent(username, AuditEventTypeEnum.LOGIN_FAILURE);
    }

    @Override
    protected void loggedIn(@Nonnull String username) {
        //Covered by Authenticated
    }

    @Override
    protected void failedToLogIn(@Nonnull String username) {
        //Covered by failedToAuthenticate
    }

    @Override
    protected void loggedOut(@Nonnull String username) {
        captureUserAuditEvent(username, AuditEventTypeEnum.LOGOUT);
    }

    private final static Cache<String, AuditModel> auditCache = CacheBuilder.newBuilder().
            maximumSize(1000).expireAfterWrite(TimeUnit.MINUTES.toSeconds(4), TimeUnit.SECONDS)
            .build();

    private void captureUserAuditEvent(String userName, final AuditEventTypeEnum auditEventTypeEnum){
        User user = User.get(userName);
        auditCache.put(user.getId(), new AuditModel(user.getFullName(), user.getId(), auditEventTypeEnum.getValue(), DATETIME_FORMATTER.format(new Date())));
    }

    public static Collection<AuditModel> getRecentAuditEvents(){
        /*Collection<AuditModel> values = auditCache.asMap().values();
        values.forEach(auditModel -> {
            LOG.info("Audit details fetched are "+auditModel.getUserId()+ " "+auditModel.getAuditEventType());
        });

        return values;*/
        return auditCache.asMap().values();
    }
}
