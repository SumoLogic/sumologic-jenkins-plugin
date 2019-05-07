package com.sumologic.jenkins.jenkinssumologicplugin.constants;

import org.apache.commons.lang.time.FastDateFormat;

/**
 * Sumo Logic plugin for Jenkins model.
 *
 * Sumo Constants
 *
 * Created by Sourabh Jain on 5/2019.
 */
public class SumoConstants {

    private SumoConstants(){

    }

    public static final String END_OF_SUMO_PIPELINE = "[Pipeline] // SumoPipelineLogCollection";
    public static final String COMMA_SEPARATOR = ", ";
    public static final String MASTER = "master";

    public static final FastDateFormat DATETIME_FORMATTER
            = FastDateFormat.getInstance("dd MMM yyyy HH:mm:ss Z");

    public static final String GENERATION_ERROR = "Job Status Generation ended with exception as ";
}
