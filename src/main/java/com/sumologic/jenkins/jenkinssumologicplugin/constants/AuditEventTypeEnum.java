package com.sumologic.jenkins.jenkinssumologicplugin.constants;

public enum AuditEventTypeEnum {

    LOGIN("User_Login", "%s Logged in."),
    LOGOUT("User_Logout", "%s Logged Out."),
    LOGIN_FAILURE("User_Login_Failure", "%s failed to Login."),
    JOB_STARTED("Job_Started", "%s started the job with build number as %s."),
    JOB_ABORTED("Job_Aborted", "%s aborted the job with build number as %s."),
    CREATED("Created", "%s created the item %s."),
    DELETED("Deleted", "%s deleted the item %s."),
    UPDATED("Updated", "%s updated the item %s."),
    COPIED("Copied", "%s copied the item %s from %s."),
    LOCATION_CHANGED("Location_Changed", "%s changed the location of the item %s to %s."),
    CHANGES_IN_CONFIG("Config_Change", "%s changed configuration for the file %s.");

    private String value;
    private String message;

    AuditEventTypeEnum(final String value, final String message)
    {
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
