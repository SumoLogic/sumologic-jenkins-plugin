package com.sumologic.jenkins.jenkinssumologicplugin.model;

import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.reporters.SurefireAggregatedReport;
import hudson.model.*;
import hudson.tasks.test.TestResult;
import jenkins.model.Jenkins;

import java.util.Map;
import java.util.logging.Logger;


/**
 * Created by deven on 7/11/15.
 *
 * Factory class providing static methods to convert builds to a convenient DTO as an intermediate
 * step to json serialization.
 */
public class ModelFactory {
  private final static Logger LOG = Logger.getLogger(ModelFactory.class.getName());

  protected static void populateGeneric(BuildModel buildModel, Run build) {
    buildModel.setName(build.getParent().getDisplayName());
    buildModel.setNumber(build.getNumber());
    buildModel.setDuration(System.currentTimeMillis() - build.getStartTimeInMillis());
    buildModel.setStart(build.getStartTimeInMillis());
    String result = build.getResult() != null ? build.getResult().toString() : "Unknown";
    buildModel.setResult(result);

    if(Hudson.getVersion() != null){
      buildModel.setHudsonVersion(Hudson.getVersion().toString());
    }
    buildModel.setDescription(build.getDescription());
  }

  public static JenkinsModel createJenkinsModel(Jenkins jenkins) {
    Queue queue = jenkins.getQueue();
    final Queue.Item[] items = queue.getItems();
    int queueLength = items.length;
    long maxwait = 0, averagewait = 0, now = System.currentTimeMillis();
    for(Queue.Item item : items){
      long waiting = now - item.getInQueueSince();
      maxwait = maxwait > waiting ? maxwait : waiting;
      averagewait += waiting;
    }
    if (queueLength > 0 ) {
      averagewait = averagewait / queueLength;
    }

    QueueModel queueModel = new QueueModel(queueLength,queueLength - queue.countBuildableItems(), maxwait, averagewait);

    int numFreeExecutors=0;

    for (Computer computer : jenkins.getComputers()) {
      if (computer.isOnline()){
        numFreeExecutors += computer.countIdle();
      }

    }
    SlaveModel slaveModel = new SlaveModel(jenkins.getComputers().length - 1, jenkins.getNumExecutors(), numFreeExecutors);

    return new JenkinsModel(queueModel, slaveModel, jenkins.getDescription());
  }

  public static BuildModel createBuildModel(Run build) {
    BuildModel buildModel = null;
    if (build instanceof MavenModuleSetBuild) {
      MavenModuleSetBuildModel mBuildModel = new MavenModuleSetBuildModel();
      ModelFactory.populateGeneric(mBuildModel, build);
      MavenModuleSetBuild mbuild = (MavenModuleSetBuild) build;
      SurefireAggregatedReport surefireAggregatedReport = mbuild.getAction(SurefireAggregatedReport.class);
      if (surefireAggregatedReport != null) {
        mBuildModel.setTotalTestCount(surefireAggregatedReport.getTotalCount());
        mBuildModel.setFailedTestCount(surefireAggregatedReport.getFailCount());
        mBuildModel.setSkippedTestCount(surefireAggregatedReport.getSkipCount());
        for (TestResult result : surefireAggregatedReport.getFailedTests()) {
          mBuildModel.addFailedTest(result.getDisplayName());
        }
      }
      Map<MavenModule, MavenBuild> modules = mbuild.getModuleLastBuilds();

      for (MavenBuild module : modules.values()) {
        mBuildModel.addModule((MavenModuleBuildModel) createBuildModel(module));
      }
      buildModel = mBuildModel;
    } else if (build instanceof MavenBuild) {
      MavenBuild mbuild = (MavenBuild) build;
      MavenModuleBuildModel mBuildModel = new MavenModuleBuildModel();
      ModelFactory.populateGeneric(mBuildModel, mbuild);
      buildModel = mBuildModel;
    } else {
      buildModel = new BuildModel();
      ModelFactory.populateGeneric(buildModel, build);
    }

    return buildModel;
  }
}
