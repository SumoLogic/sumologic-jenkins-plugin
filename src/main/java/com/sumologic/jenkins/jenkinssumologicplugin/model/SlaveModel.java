package com.sumologic.jenkins.jenkinssumologicplugin.model;

/**
* Created by deven on 8/6/15.
*/
public class SlaveModel {
  protected int numberOfSlaves, numberOfExecutors, numberOfFreeExecutors;

  public SlaveModel(int numberOfSlaves, int numberOfExecutors, int numberOfFreeExecutors) {
    this.numberOfSlaves = numberOfSlaves;
    this.numberOfExecutors = numberOfExecutors;
    this.numberOfFreeExecutors = numberOfFreeExecutors;
  }

  public int getNumberOfSlaves() {
    return numberOfSlaves;
  }

  public void setNumberOfSlaves(int numberOfSlaves) {
    this.numberOfSlaves = numberOfSlaves;
  }

  public int getNumberOfExecutors() {
    return numberOfExecutors;
  }

  public void setNumberOfExecutors(int numberOfExecutors) {
    this.numberOfExecutors = numberOfExecutors;
  }

  public int getNumberOfFreeExecutors() {
    return numberOfFreeExecutors;
  }

  public void setNumberOfFreeExecutors(int numberOfFreeExecutors) {
    this.numberOfFreeExecutors = numberOfFreeExecutors;
  }
}
