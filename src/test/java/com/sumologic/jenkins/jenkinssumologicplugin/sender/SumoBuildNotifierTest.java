package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.SumoBuildNotifier;
import com.sumologic.jenkins.jenkinssumologicplugin.model.ModelFactory;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.tasks.Shell;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.atLeast;

public class SumoBuildNotifierTest {
  @Rule
  public JenkinsRule j = new JenkinsRule();
  @Mock
  HttpRequestHandler handler;
  private LocalTestServer server = null;
  private String serverUrl;


  @Before
  public void setUp() throws Exception {
    handler = Mockito.mock(HttpRequestHandler.class);
    server = new LocalTestServer(null, null);
    server.register("/jenkinstest/*", handler);
    server.start();

    serverUrl = "http://" + server.getServiceHostName() + ":"
        + server.getServicePort() + "/jenkinstest/123";

    // report how to access the server
    System.out.println("LocalTestServer available at " + serverUrl);
    j.get(PluginDescriptorImpl.class).setUrl(serverUrl);

  }

  @After
  public void tearDown() throws Exception {

    server.stop();

  }

  @Ignore
  @Test
  public void testSendBuildData() throws Exception {
    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    FreeStyleProject project = j.createFreeStyleProject();

    project.getBuildersList().add(new Shell("Echo Hello"));
    project.getBuildersList().add(new Shell("Echo Hello2"));
    project.getBuildersList().add(new Shell("Echo Hello3"));
    project.getBuildersList().add(new Shell("Echo Hello4"));
    project.getPublishersList().add(new SumoBuildNotifier());

    FreeStyleBuild build = project.scheduleBuild2(0).get();

    Mockito.verify(handler, atLeast(1)).handle(
        captor.capture(),
        Mockito.isA(HttpResponse.class),
        Mockito.isA(HttpContext.class));

    HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) captor.getValue();


    Assert.assertTrue("Message too short.", ModelFactory.createBuildModel(build).toJson().length() <= request.getEntity().getContentLength());
  }

  @Ignore
  @Test
  public void testSendJenkinsData() throws Exception {
    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    SumoPeriodicPublisher publisher = new SumoPeriodicPublisher();
    publisher.execute(Mockito.mock(TaskListener.class));

    Mockito.verify(handler, atLeast(1)).handle(
        captor.capture(),
        Mockito.isA(HttpResponse.class),
        Mockito.isA(HttpContext.class));

    HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) captor.getValue();

    Assert.assertTrue("Wrong message length.", ModelFactory.createJenkinsModel(j.getInstance()).toJson().length() >= request.getEntity().getContentLength());



  }
}