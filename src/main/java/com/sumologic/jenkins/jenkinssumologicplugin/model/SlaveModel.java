package com.sumologic.jenkins.jenkinssumologicplugin.model;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by deven on 8/6/15.
 *
 * Updated by Sourabh Jain 05/2019
 */
public class SlaveModel extends BaseModel {
    protected int numberOfExecutors, numberOfFreeExecutors;

    private String nodeName;
    private String nodeLabel;
    private String nodeStatus;
    private boolean isIdle;
    private boolean isOnline;
    private String reasonOffline;
    private boolean isRemoved;
    private boolean isConnecting;
    private String nodeURL;
    private String eventSource;
    private Map<String, Object> monitorData;

    public SlaveModel() {

    }

    public SlaveModel(int numberOfExecutors, int numberOfFreeExecutors) {
        //this.numberOfSlaves = numberOfSlaves;
        this.numberOfExecutors = numberOfExecutors;
        this.numberOfFreeExecutors = numberOfFreeExecutors;
    }

    /*public int getNumberOfSlaves() {
        return numberOfSlaves;
    }

    public void setNumberOfSlaves(int numberOfSlaves) {
        this.numberOfSlaves = numberOfSlaves;
    }*/

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

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setNodeLabel(String nodeLabel) {
        this.nodeLabel = nodeLabel;
    }

    public void setNodeStatus(String nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    public void setIdle(boolean idle) {
        isIdle = idle;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public void setReasonOffline(String reasonOffline) {
        this.reasonOffline = reasonOffline;
    }

    public void setRemoved(boolean removed) {
        isRemoved = removed;
    }

    public void setConnecting(boolean connecting) {
        isConnecting = connecting;
    }

    public void setNodeURL(String nodeURL) {
        this.nodeURL = nodeURL;
    }

    public void setEventSource(String eventSource) {
        this.eventSource = eventSource;
    }

    public void setMonitorData(Map<String, Object> monitorData) {
        this.monitorData = monitorData;
    }

    public Map<String, Object> getMonitorData() {
        if (this.monitorData == null) {
            this.monitorData = new HashMap<String, Object>();
        }
        return monitorData;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getNodeLabel() {
        return nodeLabel;
    }

    public String getNodeStatus() {
        return nodeStatus;
    }

    public boolean isIdle() {
        return isIdle;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public String getReasonOffline() {
        return reasonOffline;
    }

    public boolean isRemoved() {
        return isRemoved;
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    public String getNodeURL() {
        return nodeURL;
    }

    public String getEventSource() {
        return eventSource;
    }

    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
