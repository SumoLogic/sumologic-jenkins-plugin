package com.sumologic.jenkins.jenkinssumologicplugin.model;

import com.google.gson.Gson;

/**
 * Created by deven on 8/6/15.
 *
 * Updates by Sourabh Jain 05/2019
 */
public class QueueModel extends BaseModel {
    /*protected int numItemsInQueue, numBlockedItemsInQueue;
    protected long maxWaitingTime, averageWaitingTime;*/

    private long queueId;
    private float queueTime;
    private boolean isBlocked;
    private String reasonForBlock;
    private boolean isConcurrentBuild;
    private String jobName;
    private String jobURL;

    public QueueModel() {

    }

    /*public QueueModel(int numItemsInQueue, int numBlockedItemsInQueue, long maxWaitingTime, long averageWaitingTime) {
        this.numItemsInQueue = numItemsInQueue;
        this.numBlockedItemsInQueue = numBlockedItemsInQueue;
        this.maxWaitingTime = maxWaitingTime;
        this.averageWaitingTime = averageWaitingTime;
    }

    public int getNumItemsInQueue() {
        return numItemsInQueue;
    }

    public void setNumItemsInQueue(int numItemsInQueue) {
        this.numItemsInQueue = numItemsInQueue;
    }

    public int getNumBlockedItemsInQueue() {
        return numBlockedItemsInQueue;
    }

    public void setNumBlockedItemsInQueue(int numBlockedItemsInQueue) {
        this.numBlockedItemsInQueue = numBlockedItemsInQueue;
    }

    public long getMaxWaitingTime() {
        return maxWaitingTime;
    }

    public void setMaxWaitingTime(long maxWaitingTime) {
        this.maxWaitingTime = maxWaitingTime;
    }

    public long getAverageWaitingTime() {
        return averageWaitingTime;
    }

    public void setAverageWaitingTime(long averageWaitingTime) {
        this.averageWaitingTime = averageWaitingTime;
    }
*/
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

    public long getQueueId() {
        return queueId;
    }

    public float getQueueTime() {
        return queueTime;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public String getReasonForBlock() {
        return reasonForBlock;
    }

    public boolean isConcurrentBuild() {
        return isConcurrentBuild;
    }

    public String getJobName() {
        return jobName;
    }

    public String getJobURL() {
        return jobURL;
    }

    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
