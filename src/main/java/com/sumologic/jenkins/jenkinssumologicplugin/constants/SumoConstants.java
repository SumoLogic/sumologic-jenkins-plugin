package com.sumologic.jenkins.jenkinssumologicplugin.constants;

import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSender;
import org.apache.commons.lang.time.FastDateFormat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Sumo Constants
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class SumoConstants {

    private SumoConstants() {

    }

    public static final String SUMO_PIPELINE = "[Pipeline] SumoPipelineLogCollection";
    public static final String COMMA_SEPARATOR = ", ";
    // Changing the value has impact on APP. Also, jenkins calls its computer as master.
    public static final String MAIN = "master";
    public static final String JENKINS_MAIN = "(master)";

    public static final FastDateFormat DATETIME_FORMATTER
            = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss,SSS ZZZZ");

    public static final String GENERATION_ERROR = "Job Status Generation ended with exception as ";

    public static final String CONSOLE_ERROR = "Job Console Logs Sender ended with exception as ";

    public static final String SCM_ERROR = "Job SCM Logs Sender ended with exception as ";

    public static final String GRAPHITE_CONTENT_TYPE = "application/vnd.sumologic.graphite";

    public static final String CARBON_CONTENT_TYPE = "application/vnd.sumologic.carbon2";

    public static final String IGNORE_PATTERN = "(?:queue|nodeMonitors|UpdateCenter|global-build-stats).xml|/(?:fingerprint|builds|config-history)/.*?xml";

    public static final int DIVIDER_FOR_MESSAGES = 100;

    public static final String MONITOR_PATTERN_MATCHER = "error.*?>(.*?)</span>";

    public static final List<String> skipLoggerNames = Collections.unmodifiableList(Arrays.asList(LogSender.class.getName(), "hudson.Extension", "hudson.node_monitors",
            "jenkins.InitReactorRunner", "hudson.util.BootFailure", "shaded.splk.org.apache.http"));

    public static final String PIPELINE = "[Pipeline]";

    public static final String START_OF_PIPELINE = "[Pipeline] Start of Pipeline";

    public static final String END_OF_PIPELINE = "[Pipeline] End of Pipeline";

    public static final int MAX_DATA_SIZE = 50000;
}
