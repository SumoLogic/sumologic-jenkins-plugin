package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.SumoBuildNotifier;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.LogTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.model.ScmModel;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;
import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.SCM_ERROR;
import static hudson.Util.fixEmpty;

@Extension
public class SumoSCMListener extends SCMListener {

    private static LogSenderHelper logSenderHelper = LogSenderHelper.getInstance();

    private static final Logger LOG = Logger.getLogger(SumoSCMListener.class.getName());

    @Override
    public void onChangeLogParsed(Run<?, ?> build, SCM scm, TaskListener listener, ChangeLogSet<?> changelog) {
        try {
            ScmModel scmModel = new ScmModel();

            scmModel.setLogType(LogTypeEnum.SCM_STATUS.getValue());
            scmModel.setEventTime(DATETIME_FORMATTER.format(new Date()));
            scmModel.setJobName(build.getParent().getDisplayName());
            scmModel.setBuildNumber(build.getNumber());

            List<String> changes = new ArrayList<>();
            for (ChangeLogSet.Entry entry : changelog) {
                String sbr = entry.getTimestamp() +
                        " " + "commit:" + entry.getCommitId() +
                        " " + "author:" + entry.getAuthor() +
                        " " + "message:" + entry.getMsg();
                changes.add(sbr);
            }
            scmModel.setChangeLog(changes);

            populateGitScmDetails(scm, scmModel, build);
            populateSubversionDetails(scm, scmModel, build);

            PluginDescriptorImpl pluginDescriptor = PluginDescriptorImpl.getInstance();
            if(pluginDescriptor.isScmLogEnabled()){
                logSenderHelper.sendJobStatusLogs(scmModel.toString());
            }
        } catch (Exception exception) {
            String errorMessage = SCM_ERROR + Arrays.toString(exception.getStackTrace());
            LOG.log(Level.WARNING, errorMessage);
            listener.error(errorMessage);
        }
    }

    private void populateGitScmDetails(SCM scm, ScmModel scmModel, Run<?, ?> build) {
        if (scm instanceof GitSCM) {
            GitSCM gitSCM = (GitSCM) scm;
            scmModel.setScmType(gitSCM.getType());
            scmModel.setScmURLs(gitSCM.getKey());
            BuildData buildData = gitSCM.getBuildData(build);
            if(buildData != null){
                Revision rev = buildData.getLastBuiltRevision();
                if (rev != null) {
                    String sha1 = fixEmpty(rev.getSha1String());
                    if (sha1 != null && !sha1.isEmpty()) {
                        scmModel.setRevision(sha1);
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    rev.getBranches().forEach(branch -> {
                        String branchName = getBranchName(branch);
                        stringBuilder.append(branchName).append(" ");
                    });
                    scmModel.setBranches(stringBuilder.toString());
                }
            }
        }
    }

    private void populateSubversionDetails(SCM scm, ScmModel scmModel, Run<?, ?> build) {
        if (scm instanceof SubversionSCM) {
            SubversionSCM subversionSCM = (SubversionSCM) scm;
            scmModel.setScmType(subversionSCM.getType());
            scmModel.setScmURLs(subversionSCM.getKey());
        }
    }

    private String getBranchName(Branch branch) {
        String name = branch.getName();
        if (name.startsWith("refs/remotes/")) {
            //Restore expected previous behaviour
            name = name.substring("refs/remotes/".length());
        }
        return name;
    }
}
