package com.sumologic.jenkins.jenkinssumologicplugin.model;

import java.util.List;
import java.util.Set;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Pipeline stages details model
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class PipelineStageModel {

    private String id;
    private String name;
    private String status;
    private String startTime;
    private float duration;
    private float pauseDuration;
    private String arguments;
    private String executionNode;
    private Set<String> parallelStage;
    private List<String> steps;
    private String error;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public void setPauseDuration(float pauseDuration) {
        this.pauseDuration = pauseDuration;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public void setExecutionNode(String executionNode) {
        this.executionNode = executionNode;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getStartTime() {
        return startTime;
    }

    public float getDuration() {
        return duration;
    }

    public float getPauseDuration() {
        return pauseDuration;
    }

    public String getArguments() {
        return arguments;
    }

    public String getExecutionNode() {
        return executionNode;
    }

    public List<String> getSteps() {
        return steps;
    }

    public Set<String> getParallelStage() {
        return parallelStage;
    }

    public void setParallelStage(Set<String> parallelStage) {
        this.parallelStage = parallelStage;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
