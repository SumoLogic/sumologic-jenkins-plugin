package com.sumologic.jenkins.jenkinssumologicplugin.model;

/**
* Created by deven on 8/6/15.
*/
public class QueueModel {
  protected int NumItemsInQueue, NumBlockedItemsInQueue;
  protected long MaxWaitingTime, AverageWaitingTime;

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
}
