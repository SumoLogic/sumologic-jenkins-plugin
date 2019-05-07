package com.sumologic.jenkins.jenkinssumologicplugin.model;

import java.util.List;
import java.util.Set;

/**
 * Sumo Logic plugin for Jenkins model.
 *
 * Pipeline stages details model
 *
 * Created by Sourabh Jain on 5/2019.
 */
public class PipelineStageModel {

    private String id;
    private String name;
    private String status;
    private float startTime;
    private float duration;
    private float pauseDuration;
    private String arguments;
    private String executionNode;
    private Set<String> parallelStage;
    private ErrorModel errorModel;
    private List<PipelineStageModel> steps;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStartTime(float startTime) {
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

    public void setSteps(List<PipelineStageModel> steps) {
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

    public float getStartTime() {
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

    public List<PipelineStageModel> getSteps() {
        return steps;
    }

    public ErrorModel getErrorModel() {
        return errorModel;
    }

    public void setErrorModel(ErrorModel errorModel) {
        this.errorModel = errorModel;
    }

    public Set<String> getParallelStage() {
        return parallelStage;
    }

    public void setParallelStage(Set<String> parallelStage) {
        this.parallelStage = parallelStage;
    }
}
