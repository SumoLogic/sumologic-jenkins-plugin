package com.sumologic.jenkins.jenkinssumologicplugin.utility;

import com.sumologic.jenkins.jenkinssumologicplugin.BaseTest;
import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.SumoBuildNotifier;
import com.sumologic.jenkins.jenkinssumologicplugin.model.BuildModel;
import com.sumologic.jenkins.jenkinssumologicplugin.model.TestCaseResultModel;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AbstractTestResultAction;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestCaseReportTest extends BaseTest {

    @Test
    public void getTestCaseReport() throws Exception {
        final String name = "MockJob";

        FreeStyleProject project = j.createFreeStyleProject(name);
        project.getPublishersList().add(new SumoBuildNotifier());

        Run build = project.scheduleBuild2(0).get();

        AbstractTestResultAction abstractTestResultAction = new TestResultAction(build, createTestResult(), null);
        build.addAction(abstractTestResultAction);

        List<TestCaseResultModel> testCaseReport = TestCaseReport.getTestCaseReport(build);
        CommonModelFactory.populateGeneric(new BuildModel(), build, j.jenkins.getDescriptorByType(PluginDescriptorImpl.class), false);
        assertEquals(testCaseReport.size(), 6);
        assertEquals(testCaseReport.stream()
                .filter(testCaseResultModel -> "Failed".equals(testCaseResultModel.getStatus())).count(), 1);
        System.out.println(testCaseReport);
    }

    public static TestResult createTestResult() throws IOException {
        TestResult testResult = new TestResult();

        URL resource = Thread.currentThread().getContextClassLoader().getResource("xml/testReport.xml");
        File junitFile = new File(resource.getFile());
        testResult.parse(junitFile);
        testResult.tally();
        return testResult;
    }
}