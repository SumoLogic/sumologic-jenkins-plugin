package com.sumologic.jenkins.jenkinssumologicplugin;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by deven on 7/8/15.
 */
@Extension
public final class SumoDescriptorImpl extends BuildStepDescriptor<Publisher> {

  private String url = "";

  public SumoDescriptorImpl() {
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

  public FormValidation doCheckUrl(@QueryParameter String value) {
    if (value.isEmpty()) {
      return FormValidation.error("You must provide an URL.");
    }

    try {
      new URL(value);
    } catch (final MalformedURLException e) {
      return FormValidation.error("This is not a valid URL.");
    }

    return FormValidation.ok();
  }


  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
