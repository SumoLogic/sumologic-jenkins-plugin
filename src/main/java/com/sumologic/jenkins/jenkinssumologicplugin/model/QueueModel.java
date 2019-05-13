package com.sumologic.jenkins.jenkinssumologicplugin.model;

import com.google.gson.Gson;

/**
* Created by deven on 8/6/15.
*/
public class QueueModel extends BaseModel{
  protected int NumItemsInQueue, NumBlockedItemsInQueue;
  protected long MaxWaitingTime, AverageWaitingTime;

  private long queueId;
  private float queueTime;
  private boolean isBlocked;
  private String reasonForBlock;
  private boolean isConcurrentBuild;
  private String jobName;
  private String jobURL;

  public QueueModel(){

  }

  public QueueModel(int numItemsInQueue, int numBlockedItemsInQueue, long maxWaitingTime, long averageWaitingTime) {
    NumItemsInQueue = numItemsInQueue;
    NumBlockedItemsInQueue = numBlockedItemsInQueue;
    MaxWaitingTime = maxWaitingTime;
    AverageWaitingTime = averageWaitingTime;
  }

  public int getNumItemsInQueue() {
    return NumItemsInQueue;
  }

  public void setNumItemsInQueue(int numItemsInQueue) {
    NumItemsInQueue = numItemsInQueue;
  }

  public int getNumBlockedItemsInQueue() {
    return NumBlockedItemsInQueue;
  }

  public void setNumBlockedItemsInQueue(int numBlockedItemsInQueue) {
    NumBlockedItemsInQueue = numBlockedItemsInQueue;
  }

  public long getMaxWaitingTime() {
    return MaxWaitingTime;
  }

  public void setMaxWaitingTime(long maxWaitingTime) {
    MaxWaitingTime = maxWaitingTime;
  }

  public long getAverageWaitingTime() {
    return AverageWaitingTime;
  }

  public void setAverageWaitingTime(long averageWaitingTime) {
    AverageWaitingTime = averageWaitingTime;
  }

  public void setQueueId(long queueId) {
    this.queueId = queueId;
  }

  public void setQueueTime(float queueTime) {
    this.queueTime = queueTime;
  }

  public void setBlocked(boolean blocked) {
    isBlocked = blocked;
  }

  public void setReasonForBlock(String reasonForBlock) {
    this.reasonForBlock = reasonForBlock;
  }

  public void setConcurrentBuild(boolean concurrentBuild) {
    isConcurrentBuild = concurrentBuild;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public void setJobURL(String jobURL) {
    this.jobURL = jobURL;
  }

  public String toString(){
    Gson gson = new Gson();
    return gson.toJson(this);
  }
}
