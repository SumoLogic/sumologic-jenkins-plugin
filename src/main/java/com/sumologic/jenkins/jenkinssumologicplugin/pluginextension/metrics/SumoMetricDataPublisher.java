package com.sumologic.jenkins.jenkinssumologicplugin.pluginextension.metrics;

import com.codahale.metrics.MetricRegistry;
import com.sumologic.jenkins.jenkinssumologicplugin.SumoBuildNotifier;
import jenkins.metrics.api.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SumoMetricDataPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SumoMetricDataPublisher.class);

    private transient SumoMetricReporter sumoMetricReporter;

    public void stopReporter(){
        LOGGER.info("Stopping Reporter");
        if(sumoMetricReporter != null){
            sumoMetricReporter.stop();
        }
    }

    public synchronized void publishMetricData(){
        LOGGER.info("Starting Reporter");
        MetricRegistry metricRegistry = Metrics.metricRegistry();

        sumoMetricReporter = SumoMetricReporter
                .forRegistry(metricRegistry)
                .prefixedWith("sourabh")
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(SumoBuildNotifier.getInstance());

        sumoMetricReporter.start(2, TimeUnit.MINUTES);

    }
}