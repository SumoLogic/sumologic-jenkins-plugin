package com.sumologic.jenkins.jenkinssumologicplugin;

import com.google.gson.Gson;
import hudson.model.AbstractBuild;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by deven on 7/10/15.
 */
public class BuildModel {
  protected String name, hudsonVersion, scm, result;
  protected int number;
  protected long start_time, duration;
  protected List<String> causes;

  BuildModel(AbstractBuild build) {
    this.name = build.getProject().getDisplayName();
    this.duration = build.getTimeInMillis();
    this.start_time = build.getStartTimeInMillis();
    this.number = build.getNumber();
    this.scm = build.getChangeSet().getKind();
    this.hudsonVersion = build.getHudsonVersion();
    this.result = build.getResult().toString();
  }

  public BuildModel(String name, String hudsonVersion, String scm, String result, int number, long start_time, long duration) {
    this.name = name;
    this.hudsonVersion = hudsonVersion;
    this.scm = scm;
    this.result = result;
    this.number = number;
    this.start_time = start_time;
    this.duration = duration;
  }

  public BuildModel() {
    this.causes = new ArrayList<String>();
  }

  public String toJson() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }

  public String getName() {
    return name;

  }

  public void setName(String name) {
    this.name = name;
  }

  public String getHudsonVersion() {
    return hudsonVersion;
  }

  public void setHudsonVersion(String hudsonVersion) {
    this.hudsonVersion = hudsonVersion;
  }

  public String getScm() {
    return scm;
  }

  public void setScm(String scm) {
    this.scm = scm;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

  public long getStart() {
    return start_time;
  }

  public void setStart(long start) {
    this.start_time = start;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public void addCause(String cause){
    this.causes.add(cause);
  }

  public List<String> getCauses(){
    return this.causes;
  }
}
