package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import com.sumologic.jenkins.jenkinssumologicplugin.constants.AuditEventTypeEnum;
import hudson.Extension;
import jenkins.security.SecurityListener;
import org.acegisecurity.userdetails.UserDetails;

import javax.annotation.Nonnull;

import static com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory.captureUserLoginEvent;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>¬
 * Security Listener for Audit Related information
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
@Extension
public class SumoJenkinsUserLoginListener extends SecurityListener {

    @Override
    protected void authenticated(@Nonnull UserDetails details) {
        captureUserLoginEvent(details.getUsername(), AuditEventTypeEnum.LOGIN);
    }

    @Override
    protected void failedToAuthenticate(@Nonnull String username) {
        captureUserLoginEvent(username, AuditEventTypeEnum.LOGIN_FAILURE);
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
        captureUserLoginEvent(username, AuditEventTypeEnum.LOGOUT);
    }
}
