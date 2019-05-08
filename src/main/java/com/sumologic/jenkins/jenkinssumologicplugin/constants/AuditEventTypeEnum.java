package com.sumologic.jenkins.jenkinssumologicplugin.constants;

public enum AuditEventTypeEnum {

    LOGIN("User_Login"),
    LOGOUT("User_Logout"),
    LOGIN_FAILURE("User_Login_Failure");

    private String value;

    AuditEventTypeEnum(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
