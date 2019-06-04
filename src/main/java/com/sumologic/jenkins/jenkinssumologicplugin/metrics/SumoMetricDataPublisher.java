package com.sumologic.jenkins.jenkinssumologicplugin.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import jenkins.metrics.api.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Sumo Logic plugin for Jenkins model.
 * <p>
 * Sumo Metric data publisher to send metric data to sumologic
 * <p>
 * Created by Sourabh Jain on 5/2019.
 */
public class SumoMetricDataPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SumoMetricDataPublisher.class);

    private transient SumoMetricReporter sumoMetricReporter;


    public synchronized void stopReporter() {
        LOGGER.info("Stopping Reporter");
        if (sumoMetricReporter != null) {
            sumoMetricReporter.stop();
        }
    }

    public synchronized void publishMetricData(String metricDataPrefix) {
        LOGGER.info("Starting Reporter with prefix as "+metricDataPrefix);
        MetricRegistry metricRegistry = Metrics.metricRegistry();

        sumoMetricReporter = SumoMetricReporter
                .forRegistry(metricRegistry)
                .prefixedWith(metricDataPrefix)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(new WhitelistMetricFilter(createMetricFilter()))
                .build(LogSenderHelper.getInstance());

        sumoMetricReporter.start(2, TimeUnit.MINUTES);

    }

    private Set<String> createMetricFilter() {
        final Set<String> whitelist = new HashSet<>();

        whitelist.add("jenkins.executor.count.value");
        whitelist.add("jenkins.executor.free.value");
        whitelist.add("jenkins.executor.in-use.value");
        whitelist.add("jenkins.job.count.value");
        whitelist.add("jenkins.node.offline.value");
        whitelist.add("jenkins.node.online.value");
        whitelist.add("jenkins.node.count.value");
        whitelist.add("jenkins.queue.blocked.value");
        whitelist.add("jenkins.queue.buildable.value");
        whitelist.add("jenkins.queue.pending.value");
        whitelist.add("jenkins.queue.stuck.value");
        whitelist.add("jenkins.queue.size.value");
        whitelist.add("jenkins.job.total.duration");
        whitelist.add("jenkins.job.blocked.duration");
        whitelist.add("jenkins.job.waiting.duration");
        whitelist.add("jenkins.job.queuing.duration");
        whitelist.add("vm.memory.heap.init");
        whitelist.add("vm.memory.heap.max");
        whitelist.add("vm.memory.heap.used");
        whitelist.add("vm.memory.total.init");
        whitelist.add("vm.memory.total.max");
        whitelist.add("vm.memory.total.used");
        whitelist.add("vm.memory.non-heap.init");
        whitelist.add("vm.memory.non-heap.max");
        whitelist.add("vm.memory.non-heap.used");
        whitelist.add("system.cpu.load");
        whitelist.add("vm.cpu.load");
        whitelist.add("vm.daemon.count");
        whitelist.add("vm.blocked.count");
        whitelist.add("vm.deadlock.count");
        whitelist.add("vm.runnable.count");
        whitelist.add("vm.waiting.count");
        whitelist.add("vm.gc.");

        return whitelist;
    }

    static class WhitelistMetricFilter implements MetricFilter {
        private final Set<String> whitelist;

        private WhitelistMetricFilter(Set<String> whitelist) {
            this.whitelist = whitelist;
        }

        @Override
        public boolean matches(String name, Metric metric) {
            for (String whitelisted : whitelist) {
                if (whitelisted.endsWith(name) || name.contains(whitelisted))
                    return true;
            }
            return false;
        }
    }
}