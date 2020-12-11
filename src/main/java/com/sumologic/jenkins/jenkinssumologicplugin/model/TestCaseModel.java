package com.sumologic.jenkins.jenkinssumologicplugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Test case model
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class TestCaseModel {
    private int failures;
    private int passes;
    private int skips;
    private int total;
    private double totalDuration;
    private List<TestCaseResultModel> testResults = new ArrayList<>();

    public List<TestCaseResultModel> getTestResults() {
        if (this.testResults == null) {
            testResults = new ArrayList<>();
        }
        return testResults;
    }

    public TestCaseModel(int failures, int passes, int skips, int total) {
        this.failures = failures;
        this.passes = passes;
        this.skips = skips;
        this.total = total;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public int getPasses() {
        return passes;
    }

    public void setPasses(int passes) {
        this.passes = passes;
    }

    public int getSkips() {
        return skips;
    }

    public void setSkips(int skips) {
        this.skips = skips;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public double getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(double totalDuration) {
        this.totalDuration = totalDuration;
    }

    public void setTestResults(List<TestCaseResultModel> testResults) {
        this.testResults = testResults;
    }
}
