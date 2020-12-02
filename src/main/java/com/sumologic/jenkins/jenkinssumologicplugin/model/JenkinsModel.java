package com.sumologic.jenkins.jenkinssumologicplugin.model;

import com.google.gson.Gson;

/**
 * Created by deven on 8/6/15.
 */
public class JenkinsModel {
    protected QueueModel queue;
    protected AgentModel agents;
    protected String description;

    public JenkinsModel(QueueModel queue, AgentModel agents, String description) {
        this.queue = queue;
        this.agents = agents;
        this.description = description;
    }

    public QueueModel getQueue() {
        return queue;
    }

    public void setQueue(QueueModel queue) {
        this.queue = queue;
    }

    public AgentModel getAgents() {
        return agents;
    }

    public void setAgents(AgentModel agents) {
        this.agents = agents;
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
