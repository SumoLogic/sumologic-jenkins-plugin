package com.sumologic.jenkins.jenkinssumologicplugin.utility;

import com.sumologic.jenkins.jenkinssumologicplugin.model.TestCaseResultModel;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Generate test case report
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class TestCaseReport {

    public static List<TestCaseResultModel> getTestCaseReport(Run buildInfo) {
        List<TestCaseResultModel> testResults = new ArrayList<>();
        if (buildInfo == null) {
            return testResults;
        }
        List<AbstractTestResultAction> testResultActionList = buildInfo.getActions(AbstractTestResultAction.class);

        testResultActionList.forEach(abstractTestResultAction -> {
            if (abstractTestResultAction instanceof AggregatedTestResultAction) {
                testResults.addAll(addTestResults((AggregatedTestResultAction) abstractTestResultAction));
            } else if (abstractTestResultAction instanceof TestResultAction) {
                testResults.addAll(addTestResult(buildInfo, abstractTestResultAction.getResult()));
            }
        });

        return testResults;
    }

    private static List<TestCaseResultModel> addTestResults(AggregatedTestResultAction testAction) {
        List<TestCaseResultModel> testResults = new ArrayList<>();
        List<AggregatedTestResultAction.ChildReport> childReports = testAction.getChildReports();
        for (AggregatedTestResultAction.ChildReport childReport : childReports) {
            testResults.addAll(addTestResult(childReport.run, childReport.result));
        }
        return testResults;
    }

    private static List<TestCaseResultModel> addTestResult(Run run, Object result) {
        List<TestCaseResultModel> caseResults = new ArrayList<>();
        if (run == null || result == null) {
            return caseResults;
        }
        if (result instanceof hudson.tasks.junit.TestResult) {
            hudson.tasks.junit.TestResult result1 = (hudson.tasks.junit.TestResult) result;
            for (SuiteResult suite : result1.getSuites()) {
                for (CaseResult testCase : suite.getCases()) {
                    if (!testCase.isSkipped()) {
                        caseResults.add(convertTestResult(testCase));
                    }
                }
            }
        }
        return caseResults;
    }

    private static TestCaseResultModel convertTestResult(CaseResult testCase) {
        TestCaseResultModel testCaseResultModel = new TestCaseResultModel();

        testCaseResultModel.setClassName(testCase.getClassName());
        testCaseResultModel.setDuration(testCase.getDuration());
        testCaseResultModel.setTestName(testCase.getName());
        testCaseResultModel.setErrorDetails(testCase.getErrorDetails());
        if (testCase.getErrorStackTrace() != null && testCase.getErrorStackTrace().length() > 1010) {
            testCaseResultModel.setErrorStackTrace(testCase.getErrorStackTrace().substring(0, 1000));
        } else {
            testCaseResultModel.setErrorStackTrace(testCase.getErrorStackTrace());
        }

        testCaseResultModel.setStatus(testCase.isFailed() ? "Failed" : "Passed");
        return testCaseResultModel;
    }
}
