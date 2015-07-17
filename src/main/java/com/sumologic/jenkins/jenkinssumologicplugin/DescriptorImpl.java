package com.sumologic.jenkins.jenkinssumologicplugin;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
* Created by deven on 7/8/15.
*/
@Extension
public final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

  private String url = "";

  public DescriptorImpl() {
    super(SumoBuildNotifier.class);
    load();
  }

  @Override
  public boolean isApplicable(Class<? extends AbstractProject> aClass) {
    return true;
  }

  @Override
  public String getDisplayName() {
    return "Sumologic build logger";
  }

  @Override
  public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

    url = formData.getString("url");
    save();
    return super.configure(req, formData);
  }


  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
