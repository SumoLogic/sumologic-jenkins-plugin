package com.sumologic.jenkins.jenkinssumologicplugin.integration;

import com.sumologic.jenkins.jenkinssumologicplugin.BaseTest;
import hudson.model.Action;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearchActionTest extends BaseTest {

    @Test
    public void testSearchActionWhenBuildComplete() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();

        Run build = project.scheduleBuild2(0).get();

        build.addAction(new SearchAction(build));

        for (Action a : build.getAllActions()) {
            if (a instanceof SearchAction) {
                assertEquals(a.getDisplayName(), "Search logs");
                assertEquals(a.getIconFileName(), "/plugin/sumologic-publisher/sumologic.ico");
                assertTrue(a.getUrlName().contains(build.getParent().getFullName()));
            }
        }
    }
}