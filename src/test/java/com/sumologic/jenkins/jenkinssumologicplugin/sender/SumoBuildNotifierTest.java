package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.BaseTest;
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
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.Mockito.atLeast;

public class SumoBuildNotifierTest extends BaseTest {

    @Test
    public void testSendBuildData() throws Exception {
        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).setJobStatusLogEnabled(false);
        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).setJobConsoleLogEnabled(false);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        FreeStyleProject project = j.createFreeStyleProject();
        for (int i = 0; i < 30; i++) {
            project.getBuildersList().add(new Shell("Echo Hello world for sumo  build notifier"));
        }

        project.getPublishersList().add(new SumoBuildNotifier());

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        Mockito.verify(handler, atLeast(1)).handle(
                captor.capture(),
                Mockito.isA(HttpResponse.class),
                Mockito.isA(HttpContext.class));

        HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) captor.getValue();

        Assert.assertTrue("Message too short.", ModelFactory
                .createBuildModel(build, (PluginDescriptorImpl) j.getInstance().getDescriptor(SumoBuildNotifier.class)).toJson().length() >= request.getEntity().getContentLength());

        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).setJobStatusLogEnabled(true);
        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).setJobConsoleLogEnabled(true);
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
