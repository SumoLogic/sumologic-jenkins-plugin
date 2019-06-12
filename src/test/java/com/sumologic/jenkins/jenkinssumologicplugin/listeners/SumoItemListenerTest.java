package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import com.sumologic.jenkins.jenkinssumologicplugin.BaseTest;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.AuditEventTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.constants.LogTypeEnum;
import com.sumologic.jenkins.jenkinssumologicplugin.model.AuditModel;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hudson.test.MockFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Date;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;
import static org.mockito.Mockito.atLeast;

public class SumoItemListenerTest extends BaseTest {

    @Test
    public void verify() throws Exception {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

        MockFolder test = j.createFolder("test");
        Mockito.verify(handler, atLeast(1)).handle(
                captor.capture(),
                Mockito.isA(HttpResponse.class),
                Mockito.isA(HttpContext.class));

        AuditModel auditModel = new AuditModel("Sourabh", "Sourabh",
                AuditEventTypeEnum.CREATED.getValue(), DATETIME_FORMATTER.format(new Date()),
                "This is Create", LogTypeEnum.AUDIT_EVENT.getValue(), null);

        HttpEntityEnclosingRequest value = (HttpEntityEnclosingRequest) captor.getValue();

        Assert.assertTrue("Create Failed", gson.toJson(auditModel).length() >= value.getEntity().getContentLength());

        MockFolder test2 = test.copy(test, "test2");

        Mockito.verify(handler, atLeast(1)).handle(
                captor.capture(),
                Mockito.isA(HttpResponse.class),
                Mockito.isA(HttpContext.class));

        auditModel = new AuditModel("Sourabh", "Sourabh",
                AuditEventTypeEnum.COPIED.getValue(), DATETIME_FORMATTER.format(new Date()),
                "This is copy", LogTypeEnum.AUDIT_EVENT.getValue(), null);

        value = (HttpEntityEnclosingRequest) captor.getValue();

        Assert.assertTrue("Copy Failed", gson.toJson(auditModel).length() >= value.getEntity().getContentLength());

        test2.delete();

        Mockito.verify(handler, atLeast(1)).handle(
                captor.capture(),
                Mockito.isA(HttpResponse.class),
                Mockito.isA(HttpContext.class));

        auditModel = new AuditModel("Sourabh", "Sourabh",
                AuditEventTypeEnum.DELETED.getValue(), DATETIME_FORMATTER.format(new Date()),
                "This is Delete", LogTypeEnum.AUDIT_EVENT.getValue(), null);

        value = (HttpEntityEnclosingRequest) captor.getValue();

        Assert.assertTrue("Delete Failed", gson.toJson(auditModel).length() >= value.getEntity().getContentLength());
    }
}