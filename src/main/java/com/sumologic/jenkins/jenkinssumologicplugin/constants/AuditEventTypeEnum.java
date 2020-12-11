package com.sumologic.jenkins.jenkinssumologicplugin.constants;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Audit Event Type Enum
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public enum AuditEventTypeEnum {

    LOGIN("User_Login", "%s Logged in."),
    LOGOUT("User_Logout", "%s Logged Out."),
    LOGIN_FAILURE("User_Login_Failure", "%s Login failed."),
    JOB_STARTED("Job_Started", "User %s started the job %s with build number as %s."),
    JOB_ABORTED("Job_Aborted", "User %s aborted the job %s with build number as %s."),
    JOB_FINISHED("Job_Finished", "Job %s with build number as %s finished with status as %s"),
    CREATED("Created", "%s created the item %s."),
    DELETED("Deleted", "%s deleted the item %s."),
    UPDATED("Updated", "%s updated the item %s."),
    COPIED("Copied", "%s copied the item %s from %s."),
    LOCATION_CHANGED("Location_Changed", "%s changed the location of the item %s to %s."),
    CHANGES_IN_CONFIG("Config_Change", "%s changed configuration for the file %s.");

    private final String value;
    private final String message;

    AuditEventTypeEnum(final String value, final String message) {
        this.value = value;
        this.message = message;
    }

    public String getValue() {
        return value;
    }

    public String getMessage() {
        return message;
    }
}
