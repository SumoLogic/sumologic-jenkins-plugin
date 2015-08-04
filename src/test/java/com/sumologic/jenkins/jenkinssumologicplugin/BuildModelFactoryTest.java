package com.sumologic.jenkins.jenkinssumologicplugin;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import org.hamcrest.core.*;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;

public class BuildModelFactoryTest {

  @Test
  public void testGenerateBuildModelForAbstractBuild() throws Exception {

    final String name = "MockJob", status = "Success", version = "1.51010.1111", scm = "nothing";
    final int number = 101;
    Result result = Result.SUCCESS;

    AbstractBuild build = Mockito.mock(AbstractBuild.class);
    AbstractProject project = Mockito.mock(AbstractProject.class);


    Mockito.when(build.getProject()).thenReturn(project);
    Mockito.when(build.getNumber()).thenReturn(number);
    Mockito.when(build.getResult()).thenReturn(result);
    Mockito.when(build.getHudsonVersion()).thenReturn(version);

    Mockito.when(project.getDisplayName()).thenReturn(name);

    String jsonExpected = "{\"name\":\"MockJob\",\"hudsonVersion\":\"1.51010.1111\",\"result\":\"SUCCESS\",\"number\":101,\"start_time\":0,\"duration\":";

    String json = BuildModelFactory.generateBuildModelFor(build).toJson();

    assertThat(json, new StringContains(jsonExpected));
  }
}