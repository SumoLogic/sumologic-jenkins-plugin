package com.sumologic.jenkins.jenkinssumologicplugin.constants;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Event Source Enum
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public enum EventSourceEnum {

    PERIODIC_UPDATE("Periodic_Update"),
    COMPUTER_ONLINE("Computer_Online"),
    COMPUTER_OFFLINE("Computer_Offline"),
    COMPUTER_TEMP_ONLINE("Computer_Temp_Online"),
    COMPUTER_TEMP_OFFLINE("Computer_Temp_Offline"),
    COMPUTER_PRE_ONLINE("Computer_Pre_Online"),
    COMPUTER_PRE_LAUNCH("Computer_Pre_Launch"),
    LAUNCH_FAILURE("Launch_Failure"),
    SHUTDOWN("Shutdown");

    private String value;

    EventSourceEnum(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
