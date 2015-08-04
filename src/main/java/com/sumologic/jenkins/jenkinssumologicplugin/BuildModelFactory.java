package com.sumologic.jenkins.jenkinssumologicplugin;

import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.reporters.SurefireAggregatedReport;
import hudson.model.AbstractBuild;
import hudson.tasks.test.TestResult;

import java.util.Map;


/**
 * Created by deven on 7/11/15.
 */
public class BuildModelFactory {
  public static final BuildModel generateBuildModelFor(AbstractBuild build) {
    BuildModel buildModel = null;
    if (build instanceof MavenModuleSetBuild) {
      MavenModuleSetBuildModel mBuildModel = new MavenModuleSetBuildModel();
      BuildModelFactory.populateGeneric(mBuildModel, build);
      MavenModuleSetBuild mbuild = (MavenModuleSetBuild) build;
      //mBuildModel.setScm(mbuild.get);
      SurefireAggregatedReport surefireAggregatedReport = mbuild.getAction(SurefireAggregatedReport.class);
      mBuildModel.setTotalTestCount(surefireAggregatedReport.getTotalCount());
      mBuildModel.setFailedTestCount(surefireAggregatedReport.getFailCount());
      mBuildModel.setSkippedTestCount(surefireAggregatedReport.getSkipCount());
      for (TestResult result : surefireAggregatedReport.getFailedTests()) {
        mBuildModel.addFailedTest(result.getDisplayName());
      }
      Map<MavenModule, MavenBuild> modules = mbuild.getModuleLastBuilds();

      for (MavenBuild module : modules.values()) {
        mBuildModel.addModule((MavenModuleBuildModel) generateBuildModelFor(module));
      }
      buildModel = mBuildModel;
    } else if (build instanceof MavenBuild) {
      MavenBuild mbuild = (MavenBuild) build;
      MavenModuleBuildModel mBuildModel = new MavenModuleBuildModel();
      BuildModelFactory.populateGeneric(mBuildModel, mbuild);
/*      for(SuiteResult result: mbuild.getAction(SurefireReport.class).getResult().getSuites()) {

      }*/
      buildModel = mBuildModel;
    } else {
      buildModel = new BuildModel();
      BuildModelFactory.populateGeneric(buildModel, build);
    }

    return buildModel;
  }

  protected static final void populateGeneric(BuildModel buildModel, AbstractBuild build) {

    buildModel.setName(build.getProject().getDisplayName());
    buildModel.setNumber(build.getNumber());
    buildModel.setDuration(System.currentTimeMillis() - build.getStartTimeInMillis());
    buildModel.setStart(build.getStartTimeInMillis());
    buildModel.setResult(build.getResult().toString());
    buildModel.setHudsonVersion(build.getHudsonVersion());

  }
}
