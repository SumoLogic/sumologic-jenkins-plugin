package com.sumologic.jenkins.jenkinssumologicplugin;

import com.sumologic.jenkins.jenkinssumologicplugin.model.ModelFactory;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Result;
import jenkins.model.Jenkins;
import org.hamcrest.core.StringContains;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import java.util.GregorianCalendar;

import static org.junit.Assert.assertThat;

public class ModelFactoryTest {

  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Test
  public void testGenerateBuildModelForAbstractBuild() throws Exception {

    final String name = "MockJob", status = "Success", version = "1.51010.1111", scm = "nothing";
    GregorianCalendar c = new GregorianCalendar();
    final int number = 101;
    Result result = Result.SUCCESS;

    AbstractBuild build = Mockito.mock(AbstractBuild.class);
    AbstractProject project = Mockito.mock(AbstractProject.class);
    Jenkins jenkins = Mockito.mock(Jenkins.class);

    Mockito.when(build.getParent()).thenReturn(project);
    Mockito.when(build.getNumber()).thenReturn(number);
    Mockito.when(build.getResult()).thenReturn(result);

    Mockito.when(build.getTimestamp()).thenReturn(c);
    Mockito.when(project.getDisplayName()).thenReturn(name);
    Mockito.when(jenkins.getRootUrl()).thenReturn("http://localhost:8080/");

    String jsonExpected = "{\"name\":\"MockJob\",\"hudsonVersion\":\"?\",\"result\":\"SUCCESS\",\"number\":101,\"start_time\":0,\"duration\":";
    String replace = jsonExpected.replace("?", Hudson.getVersion().toString());
    String json = ModelFactory.createBuildModel(build, null).toJson();

    assertThat(json, new StringContains(replace));
  }
}