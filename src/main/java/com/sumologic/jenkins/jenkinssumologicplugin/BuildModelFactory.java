package com.sumologic.jenkins.jenkinssumologicplugin;

import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.reporters.SurefireAggregatedReport;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.tasks.test.TestResult;

import java.util.List;
import java.util.Map;


/**
 * Created by deven on 7/11/15.
 *
 * Factory class providing static methods to convert builds to a convenient DTO as an intermediate
 * step to json serialization.
 */
public class BuildModelFactory {
  public static final BuildModel generateBuildModelFor(AbstractBuild build) {
    BuildModel buildModel = null;
    if (build instanceof MavenModuleSetBuild) {
      MavenModuleSetBuildModel mBuildModel = new MavenModuleSetBuildModel();
      BuildModelFactory.populateGeneric(mBuildModel, build);
      MavenModuleSetBuild mbuild = (MavenModuleSetBuild) build;
      SurefireAggregatedReport surefireAggregatedReport = mbuild.getAction(SurefireAggregatedReport.class);
      if(surefireAggregatedReport != null){
        mBuildModel.setTotalTestCount(surefireAggregatedReport.getTotalCount());
        mBuildModel.setFailedTestCount(surefireAggregatedReport.getFailCount());
        mBuildModel.setSkippedTestCount(surefireAggregatedReport.getSkipCount());
        for (TestResult result : surefireAggregatedReport.getFailedTests()) {
          mBuildModel.addFailedTest(result.getDisplayName());
        }
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


    List<Cause> causes = build.getCauses();

    for(Cause cause : causes){
      buildModel.addCause(cause.getShortDescription());
    }

  }
}
