package com.sumologic.jenkins.jenkinssumologicplugin.model;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.utility.CommonModelFactory;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.reporters.SurefireAggregatedReport;
import hudson.model.Computer;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.tasks.test.TestResult;
import jenkins.model.Jenkins;

import java.util.Map;


/**
 * Created by deven on 7/11/15.
 * <p>
 * Factory class providing static methods to convert builds to a convenient DTO as an intermediate
 * step to json serialization.
 */
public class ModelFactory {

    public static JenkinsModel createJenkinsModel(Jenkins jenkins) {
        Queue queue = jenkins.getQueue();
        final Queue.Item[] items = queue.getItems();
        long maxwait = 0, now = System.currentTimeMillis();
        for (Queue.Item item : items) {
            long waiting = now - item.getInQueueSince();
            maxwait = Math.max(maxwait, waiting);
        }

        QueueModel queueModel = new QueueModel();

        int numFreeExecutors = 0;

        for (Computer computer : jenkins.getComputers()) {
            if (computer.isOnline()) {
                numFreeExecutors += computer.countIdle();
            }

        }
        AgentModel agentModel = new AgentModel(jenkins.getNumExecutors(), numFreeExecutors);

        return new JenkinsModel(queueModel, agentModel, jenkins.getDescription());
    }

    public static BuildModel createBuildModel(Run build, PluginDescriptorImpl pluginDescriptor) {
        BuildModel buildModel;
        if (build instanceof MavenModuleSetBuild) {
            MavenModuleSetBuildModel mBuildModel = new MavenModuleSetBuildModel();
            CommonModelFactory.populateGeneric(mBuildModel, build, pluginDescriptor, false);
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
                mBuildModel.addModule((MavenModuleBuildModel) createBuildModel(module, pluginDescriptor));
            }
            buildModel = mBuildModel;
        } else if (build instanceof MavenBuild) {
            MavenBuild mbuild = (MavenBuild) build;
            MavenModuleBuildModel mBuildModel = new MavenModuleBuildModel();
            CommonModelFactory.populateGeneric(mBuildModel, mbuild, pluginDescriptor, false);
            buildModel = mBuildModel;
        } else {
            buildModel = new BuildModel();
            CommonModelFactory.populateGeneric(buildModel, build, pluginDescriptor, false);
        }
        return buildModel;
    }
}
