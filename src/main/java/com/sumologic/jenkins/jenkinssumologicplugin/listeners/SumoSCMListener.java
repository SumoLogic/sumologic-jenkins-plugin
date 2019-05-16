package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;

import javax.annotation.CheckForNull;
import java.io.File;
import java.util.logging.Logger;

@Extension
public class SumoSCMListener extends SCMListener {

    private static final Logger LOG = Logger.getLogger(SumoSCMListener.class.getName());

    @Override
    public void onCheckout(Run<?,?> build, SCM scm, FilePath workspace, TaskListener listener, @CheckForNull File changelogFile, @CheckForNull SCMRevisionState pollingBaseline) throws Exception {
        LOG.info("In Check out Method");
    }

    @Override
    public void onChangeLogParsed(Run<?,?> build, SCM scm, TaskListener listener, ChangeLogSet<?> changelog) throws Exception {
        LOG.info("In oN change log parsed");

        changelog.
    }
}
