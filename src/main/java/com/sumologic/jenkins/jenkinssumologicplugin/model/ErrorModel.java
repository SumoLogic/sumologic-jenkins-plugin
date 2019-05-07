package com.sumologic.jenkins.jenkinssumologicplugin.model;

/**
 * Sumo Logic plugin for Jenkins model.
 *
 * Error model
 *
 * Created by Sourabh Jain on 5/2019.
 */
public class ErrorModel {

    private String errorType;
    private String errorMessage;

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
