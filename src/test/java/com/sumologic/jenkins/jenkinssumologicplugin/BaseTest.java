package com.sumologic.jenkins.jenkinssumologicplugin;

import com.google.gson.Gson;
import hudson.util.Secret;
import org.apache.http.localserver.LocalServerTestBase;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.Mockito;

public class BaseTest extends LocalServerTestBase {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Mock
    public HttpRequestHandler handler;
    private Secret serverUrl;

    public Gson gson;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        handler = Mockito.mock(HttpRequestHandler.class);
        gson = new Gson();

        this.serverBootstrap.registerHandler("/jenkinstest/*", handler);
        start();

        serverUrl = Secret.fromString("http://" + server.getInetAddress().getCanonicalHostName() + ":"
                + server.getLocalPort() + "/jenkinstest/123");

        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).setUrl(serverUrl);
        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).setJobStatusLogEnabled(true);
        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).setJobConsoleLogEnabled(true);
        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).setKeepOldConfigData(true);
        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).setAuditLogEnabled(true);
        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).setMetricDataEnabled(true);
        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).setPeriodicLogEnabled(true);
        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).setScmLogEnabled(true);
    }

    @After
    public void shutdown() throws Exception{
        super.shutDown();
    }
}
