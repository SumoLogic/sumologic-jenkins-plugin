package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import com.sumologic.jenkins.jenkinssumologicplugin.BaseTest;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;

public class SumoPipelineStatusListenerTest extends BaseTest {

    @ClassRule
    public static BuildWatcher bw = new BuildWatcher();

    @Test
    public void onCompleted() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition("SumoPipelineLogCollection{ stage(\"build\") { echo 'hello' }}", true));

        WorkflowRun workflowRun = project.scheduleBuild2(0).get();

        j.assertBuildStatus(Result.FAILURE, workflowRun);
    }
}