package com.sumologic.jenkins.jenkinssumologicplugin;

import com.sumologic.jenkins.jenkinssumologicplugin.model.ModelFactory;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Result;
import org.hamcrest.core.StringContains;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.GregorianCalendar;

import static org.junit.Assert.assertThat;

public class ModelFactoryTest {

  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Test
  public void testGenerateBuildModelForAbstractBuild() throws Exception {

    final String name = "MockJob";
    GregorianCalendar c = new GregorianCalendar();
    final int number = 101;
    Result result = Result.SUCCESS;

    FreeStyleProject project = j.createFreeStyleProject(name);
    project.getPublishersList().add(new SumoBuildNotifier());

    FreeStyleBuild build = project.scheduleBuild2(0).get();

    String jsonExpected = "{\"name\":\"MockJob\",\"hudsonVersion\":\"2.60.1\",\"result\":\"SUCCESS\",\"number\":1,";
    String replace = jsonExpected.replace("?", Hudson.getVersion().toString());
    String json = ModelFactory.createBuildModel(build,(PluginDescriptorImpl) j.getInstance().getDescriptor(SumoBuildNotifier.class)).toJson();

    assertThat(json, new StringContains(replace));
  }
}