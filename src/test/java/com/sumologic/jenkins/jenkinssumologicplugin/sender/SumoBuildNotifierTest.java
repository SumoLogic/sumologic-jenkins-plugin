package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import com.sumologic.jenkins.jenkinssumologicplugin.model.ModelFactory;
import hudson.model.TaskListener;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.localserver.LocalServerTestBase;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.atLeast;

public class SumoBuildNotifierTest extends LocalServerTestBase {
  @Rule
  public JenkinsRule j = new JenkinsRule();
  @Mock
  HttpRequestHandler handler;
  private String serverUrl;


 @Before
  public void setUp() throws Exception {
    super.setUp();
    handler = Mockito.mock(HttpRequestHandler.class);
    this.serverBootstrap.registerHandler("/jenkinstest/*", handler);
    start();

    serverUrl = "http://" + server.getInetAddress().getCanonicalHostName() + ":"
        + server.getLocalPort() + "/jenkinstest/123";

    // report how to access the server
    System.out.println("LocalTestServer available at " + serverUrl);
    j.get(PluginDescriptorImpl.class).setUrl(serverUrl);
    j.get(PluginDescriptorImpl.class).setPeriodicLogEnabled(true);
  }

  @After
  public void tearDown() throws Exception {
    super.shutDown();
  }



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
