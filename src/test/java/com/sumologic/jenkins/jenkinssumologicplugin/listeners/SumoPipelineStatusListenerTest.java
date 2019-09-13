package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import com.sumologic.jenkins.jenkinssumologicplugin.BaseTest;
import com.sumologic.jenkins.jenkinssumologicplugin.SumoBuildNotifier;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Shell;
import org.junit.Test;

public class SumoPipelineStatusListenerTest extends BaseTest {

    @Test
    public void onCompleted() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        for (int i = 0; i < 30; i++) {
            project.getBuildersList().add(new Shell("Echo Hello world for sumo  build notifier"));
        }

        project.getPublishersList().add(new SumoBuildNotifier());

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        j.assertBuildStatus(Result.FAILURE, build);
    }
}