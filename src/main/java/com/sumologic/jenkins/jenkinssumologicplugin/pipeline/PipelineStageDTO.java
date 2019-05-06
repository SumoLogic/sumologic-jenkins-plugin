package com.sumologic.jenkins.jenkinssumologicplugin.pipeline;

import java.util.List;

class PipelineStageDTO {

    private String id;
    private String name;
    private String status;
    private float startTime;
    private float duration;
    private float pauseDuration;
    private String arguments;
    private String executionNode;
    private String errorType;
    private String errorMessage;
    private String parallelStage;
    private List<PipelineStageDTO> steps;

    void setId(String id) {
        this.id = id;
    }

    void setName(String name) {
        this.name = name;
    }

    void setStatus(String status) {
        this.status = status;
    }

    void setStartTime(float startTime) {
        this.startTime = startTime;
    }

    void setDuration(float duration) {
        this.duration = duration;
    }

    void setPauseDuration(float pauseDuration) {
        this.pauseDuration = pauseDuration;
    }

    void setArguments(String arguments) {
        this.arguments = arguments;
    }

    void setExecutionNode(String executionNode) {
        this.executionNode = executionNode;
    }

    void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    void setSteps(List<PipelineStageDTO> steps) {
        this.steps = steps;
    }

    void setParallelStage(String parallelStage) {
        this.parallelStage = parallelStage;
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

    public String getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getParallelStage() {
        return parallelStage;
    }

    public List<PipelineStageDTO> getSteps() {
        return steps;
    }
}
