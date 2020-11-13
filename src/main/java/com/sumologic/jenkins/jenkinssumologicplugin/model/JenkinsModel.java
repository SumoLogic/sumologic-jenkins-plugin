package com.sumologic.jenkins.jenkinssumologicplugin.model;

import com.google.gson.Gson;

/**
 * Created by deven on 8/6/15.
 */
public class JenkinsModel {
    protected QueueModel queue;
    protected SlaveModel slaves;
    protected String description;

    public JenkinsModel(QueueModel queue, SlaveModel slaves, String description) {
        this.queue = queue;
        this.slaves = slaves;
        this.description = description;
    }

    public QueueModel getQueue() {
        return queue;
    }

    public void setQueue(QueueModel queue) {
        this.queue = queue;
    }

    public SlaveModel getSlaves() {
        return slaves;
    }

    public void setSlaves(SlaveModel slaves) {
        this.slaves = slaves;
    }

    public String getDescription() {
        return description;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
