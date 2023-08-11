package com.sumologic.jenkins.jenkinssumologicplugin.listeners;

import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Date;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;
import static org.mockito.Mockito.atLeast;

public class SumoJobConfigListenerTest extends BaseTest {

    @Test
    public void onChange() throws Exception {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

        HtmlPage configPage = j.createWebClient().goTo("configure");
        HtmlForm form = configPage.getFormByName("config");
        j.submit(form);

        Mockito.verify(handler, atLeast(1)).handle(
                captor.capture(),
                Mockito.isA(HttpResponse.class),
                Mockito.isA(HttpContext.class));

        AuditModel auditModel = new AuditModel("Sourabh", "Sourabh",
                AuditEventTypeEnum.CHANGES_IN_CONFIG.getValue(), DATETIME_FORMATTER.format(new Date()),
                "This is Delete", LogTypeEnum.AUDIT_EVENT.getValue(), null);

        HttpEntityEnclosingRequest value = (HttpEntityEnclosingRequest) captor.getValue();

        Assert.assertTrue("Delete Failed", gson.toJson(auditModel).length() >= value.getEntity().getContentLength());
    }
}