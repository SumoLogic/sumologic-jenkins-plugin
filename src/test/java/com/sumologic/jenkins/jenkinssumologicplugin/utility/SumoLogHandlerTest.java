package com.sumologic.jenkins.jenkinssumologicplugin.utility;

import com.sumologic.jenkins.jenkinssumologicplugin.BaseTest;
import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Handler;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

public class SumoLogHandlerTest extends BaseTest {

    @Before
    public void setUp(){
        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).setPeriodicLogEnabled(false);
        j.jenkins.getDescriptorByType(PluginDescriptorImpl.class).registerHandler();
    }

    @Test
    public void publish() throws Exception {
        Handler[] handlers = Logger.getLogger("").getHandlers();
        boolean found = false;
        for (Handler handler : handlers) {
            if (handler instanceof SumoLogHandler) {
                found = true;
                break;
            }
        }
        assertTrue("Sumo Log handler", found);

        handlers = Logger.getLogger("").getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof SumoLogHandler) {
                handler.close();
            }
        }
    }
}