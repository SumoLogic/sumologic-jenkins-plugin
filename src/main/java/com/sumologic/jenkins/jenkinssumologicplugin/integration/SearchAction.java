package com.sumologic.jenkins.jenkinssumologicplugin.integration;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by lukasz on 3/24/17.
 */
public class SearchAction implements Action {

  private final String URL_TEMPLATE = "https://%s/ui/index.html?reason=st#section/search/@%d,%d@%s";
  private final int MILLIS_IN_MINUTE = 1000 * 60;
  private Run build;

  public SearchAction(Run build) {
    this.build = build;
  }

  @Override
  public String getIconFileName() {
    return "/plugin/sumologic-publisher/sumologic.ico";
  }

  @Override
  public String getDisplayName() {
    return "Search logs";
  }

  @Override
  public String getUrlName() {
    long queryFrom = build.getStartTimeInMillis() - (MILLIS_IN_MINUTE * 1);
    long queryTo;
    if (build.isBuilding()) {
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.HOUR, 10);
      queryTo = cal.getTimeInMillis();
    } else {
      queryTo = queryFrom + build.getDuration() + (MILLIS_IN_MINUTE * 5);
    }

    String buildName = build.getParent().getDisplayName();
    String buildNumber = build.getDisplayName();

    PluginDescriptorImpl descriptor = PluginDescriptorImpl.getInstance();
    String queryPortal = descriptor.getQueryPortal();
    String sourceCategory = descriptor.getSourceCategoryBuildLogs();

    String query = String.format("_sourceName=%s%s _sourceCategory=%s", buildName, buildNumber, sourceCategory);
    String encodedQuery = "";
    try {
      encodedQuery = URLEncoder.encode(query, "UTF-8").replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    return String.format(URL_TEMPLATE, queryPortal, queryFrom, queryTo, encodedQuery);
  }
}
