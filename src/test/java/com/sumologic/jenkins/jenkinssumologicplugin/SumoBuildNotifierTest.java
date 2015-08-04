package com.sumologic.jenkins.jenkinssumologicplugin;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.InputStream;

import static org.junit.Assert.*;

public class SumoBuildNotifierTest {
  private LocalTestServer server = null;

  @Mock
  HttpRequestHandler handler;

  @Rule
  public JenkinsRule j = new JenkinsRule();
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

  }

  @After
  public void tearDown() throws Exception {

    server.stop();

  }

  @Test
  public void testSend() throws Exception {
    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    j.get(SumoDescriptorImpl.class).setUrl(serverUrl);
    FreeStyleProject project = j.createFreeStyleProject();

    project.getBuildersList().add(new Shell("Echo Hello"));
    project.getPublishersList().add(new SumoBuildNotifier());

    FreeStyleBuild build = project.scheduleBuild2(0).get();

    Mockito.verify(handler).handle(
        captor.capture(),
        Mockito.isA(HttpResponse.class),
        Mockito.isA(HttpContext.class));

    HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) captor.getValue();


    Assert.assertEquals("Wrong length message.", BuildModelFactory.generateBuildModelFor(build).toJson().length(), request.getEntity().getContentLength());
  }
}