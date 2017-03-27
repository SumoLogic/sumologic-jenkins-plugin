package com.sumologic.jenkins.jenkinssumologicplugin.integration;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import hudson.model.AbstractBuild;
import hudson.model.Action;

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
  private AbstractBuild build;

  public SearchAction(AbstractBuild build)
  {
    this.build = build;
  }

  @Override
  public String getIconFileName() {
    return "setting.png";
  }

  @Override
  public String getDisplayName() {
    return "Search logs";
  }

  @Override
  public String getUrlName() {
    long queryFrom = build.getStartTimeInMillis();
    long queryTo;
    if (build.isBuilding()) {
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.HOUR, 10);
      queryTo = cal.getTimeInMillis();
    }
    else {
      queryTo = queryFrom + build.getDuration() + (1000 * 60 * 5);
    }

    String buildName = build.getProject().getDisplayName();
    String buildNumber = build.getDisplayName();
    String queryPortal = PluginDescriptorImpl.getInstance().getQueryPortal();
    String query = String.format("_sourceName=%s _sourceCategory=%s", buildName, buildNumber);
    String encodedQuery = "";
    try {
      encodedQuery = URLEncoder.encode(query, "UTF-8").replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    return String.format(URL_TEMPLATE, queryPortal, queryFrom, queryTo, encodedQuery);
  }
}
