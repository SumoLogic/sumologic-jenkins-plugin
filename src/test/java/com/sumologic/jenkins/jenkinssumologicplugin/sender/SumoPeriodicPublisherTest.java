package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.BaseTest;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.PeriodicWork;
import hudson.model.TaskListener;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hudson.test.TouchBuilder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;

public class SumoPeriodicPublisherTest extends BaseTest {

    @Test
    public void execute() throws Exception {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

        SumoPeriodicPublisher sumoPeriodicPublisher = PeriodicWork.all().get(SumoPeriodicPublisher.class);
        FreeStyleProject p = j.createFreeStyleProject("job" + UUID.randomUUID());
        p.setAssignedLabel(Label.get("node"));
        p.getBuildersList().add(new TouchBuilder());
        p.scheduleBuild2(0);

        sumoPeriodicPublisher.execute(TaskListener.NULL);

        Mockito.verify(handler, atLeast(1)).handle(
                captor.capture(),
                Mockito.isA(HttpResponse.class),
                Mockito.isA(HttpContext.class));

        HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) captor.getValue();

        Assert.assertTrue("Message too short.", gson.toJson(j.getInstance().getQueue().getItems()).length() >= request.getEntity().getContentLength());

    }

    @Test
    public void getRecurrencePeriod() throws Exception {
        long period = PeriodicWork.all().get(SumoPeriodicPublisher.class).getRecurrencePeriod();
        assertEquals(180000, period);
    }
}