package com.sumologic.jenkins.jenkinssumologicplugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by deven on 7/11/15.
 */
public class MavenModuleSetBuildModel extends BuildModel {
  List<MavenModuleBuildModel> modules = null;
  int totalTestCount, failedTestCount, skippedTestCount;
  List<String> failedTests = null;

  public List<MavenModuleBuildModel> getModules() {
    return modules;
  }

  public void setModules(List<MavenModuleBuildModel> modules) {
    this.modules = modules;
  }

  public MavenModuleSetBuildModel() {
    super();
    modules = new ArrayList<MavenModuleBuildModel>();
    failedTests = new ArrayList<String>();
  }

  public void addModule(MavenModuleBuildModel buildModel) {
    modules.add(buildModel);
  }

  public int getTotalTestCount() {
    return totalTestCount;
  }

  public void setTotalTestCount(int totalTestCount) {
    this.totalTestCount = totalTestCount;
  }

  public int getFailedTestCount() {
    return failedTestCount;
  }

  public void setFailedTestCount(int failedTestCount) {
    this.failedTestCount = failedTestCount;
  }

  public int getSkippedTestCount() {
    return skippedTestCount;
  }

  public void setSkippedTestCount(int skippedTestCount) {
    this.skippedTestCount = skippedTestCount;
  }

  public List<String> getFailedTests() {
    return failedTests;
  }

  public void setFailedTests(List<String> failedTests) {
    this.failedTests = failedTests;
  }

  public void addFailedTest(String test) {
    this.failedTests.add(test);
  }
}
