package com.sumologic.jenkins.jenkinssumologicplugin.constants;

import org.apache.commons.lang.time.FastDateFormat;

import java.util.regex.Pattern;

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
            = FastDateFormat.getInstance("dd-MM-yyyy HH:mm:ss Z");

    public static final String GENERATION_ERROR = "Job Status Generation ended with exception as ";

    public static final String GRAPHITE_CONTENT_TYPE = "application/vnd.sumologic.graphite";

    public static final String CARBON_CONTENT_TYPE = "application/vnd.sumologic.carbon2";

    public static final String IGNORE_PATTERN = "(queue|nodeMonitors|UpdateCenter|global-build-stats\" +\n" +
            "            \"|fingerprint|build)(.*?xml)";

    public static final int DIVIDER_FOR_MESSAGES = 100;


    public static final String ERROR_SPAN_CONTENT = "error.*?>(.*?)</span>";
}
