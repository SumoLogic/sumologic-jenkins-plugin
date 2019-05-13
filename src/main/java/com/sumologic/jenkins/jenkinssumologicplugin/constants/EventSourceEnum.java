package com.sumologic.jenkins.jenkinssumologicplugin.constants;

public enum EventSourceEnum {

    PERIODIC_UPDATE("Periodic_Update"),
    COMPUTER_ONLINE("Computer_Online"),
    COMPUTER_OFFLINE("Computer_Offline"),
    COMPUTER_TEMP_ONLINE("Computer_Temp_Online"),
    COMPUTER_TEMP_OFFLINE("Computer_Temp_Offline"),
    LAUNCH_FAILURE("Launch_Failure"),
    SHUTDOWN("Shutdown");

    private String value;

    EventSourceEnum(final String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
