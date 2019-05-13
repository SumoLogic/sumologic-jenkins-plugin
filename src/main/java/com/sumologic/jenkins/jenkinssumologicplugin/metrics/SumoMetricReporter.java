package com.sumologic.jenkins.jenkinssumologicplugin.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.sumologic.jenkins.jenkinssumologicplugin.SumoBuildNotifier;
import com.sumologic.jenkins.jenkinssumologicplugin.sender.LogSenderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Metric Reporter which publishes metric data to SumoLogic
 */
public class SumoMetricReporter extends ScheduledReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SumoMetricReporter.class);

    private final Clock clock;
    private final String prefix;
    private final LogSenderHelper logSenderHelper;

    private static final String[] snapshotStatisticsKeys = new String[]{"max", "mean", "min", "stddev", "p50", "p75", "p95", "p98", "p99", "p999"};
    private static final String[] snapshotRateKeys = new String[]{"m1_rate", "m5_rate", "m15_rate", "mean_rate"};

    public static Builder forRegistry(MetricRegistry metricRegistry) {
        return new Builder(metricRegistry);
    }

    private SumoMetricReporter(MetricRegistry metricRegistry,
                               Clock clock,
                               String prefix,
                               TimeUnit rateUnit,
                               TimeUnit durationUnit,
                               MetricFilter filter,
                               LogSenderHelper logSenderHelper) {
        super(metricRegistry, "sumo-metric-reporter", filter, rateUnit, durationUnit);
        this.clock = clock;
        this.prefix = prefix;
        this.logSenderHelper = logSenderHelper;
    }

    private String prefix(String... components) {
        return MetricRegistry.name(prefix, components);
    }

    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        final long timeInMilli = this.clock.getTime() / 1000;

        List<String> messages = new ArrayList<>();

        try {
            for (Map.Entry<String, Gauge> gauge : gauges.entrySet()) {
                reportGauge(gauge.getKey(), gauge.getValue(), timeInMilli, messages);
            }

            for (Map.Entry<String, Counter> counter : counters.entrySet()) {
                reportCounter(counter.getKey(), counter.getValue(), timeInMilli, messages);
            }

            for (Map.Entry<String, Histogram> histogram : histograms.entrySet()) {
                reportHistogram(histogram.getKey(), histogram.getValue(), timeInMilli, messages);
            }

            for (Map.Entry<String, Meter> meter : meters.entrySet()) {
                reportMetered(meter.getKey(), meter.getValue(), timeInMilli, messages);
            }

            for (Map.Entry<String, Timer> timer : timers.entrySet()) {
                reportTimer(timer.getKey(), timer.getValue(), timeInMilli, messages);
            }

            LogSenderHelper.getInstance().sendLogsToMetricDataCategory(messages);
        } catch (IOException e) {
            LOGGER.warn("Unable to send metrics to SumoLogic");
        }
    }

    private void reportGauge(String name, Gauge gauge, long timestamp, List<String> messages) throws IOException {
        final String value = format(gauge.getValue());
        if (value != null) {
            messages.add(buildMessage(prefix(name), value, timestamp));
        }
    }

    private void reportCounter(String name, Counter counter, long timestamp, List<String> messages) throws IOException {
        messages.add(buildMessage(prefix(name, "count"), format(counter.getCount()), timestamp));
    }

    private void reportTimer(String name, Timer timer, long timestamp, List<String> messages) throws IOException {

        double[] values = prepareDataFromSnapshotForStatistics(timer.getSnapshot());
        for (int i =0; i<snapshotStatisticsKeys.length;i++) {
            messages.add(buildMessage(prefix(name, snapshotStatisticsKeys[i]), format(convertDuration(values[i])), timestamp));
        }

        reportMetered(name, timer, timestamp, messages);
    }

    private void reportMetered(String name, Metered meter, long timestamp, List<String> messages) throws IOException {
        messages.add(buildMessage(prefix(name, "count"), format(meter.getCount()), timestamp));

        String[] values = prepareDataFromMeterForRate(meter);
        for (int i =0; i<snapshotRateKeys.length;i++) {
            messages.add(buildMessage(prefix(name, snapshotRateKeys[i]), values[i], timestamp));
        }
    }

    private void reportHistogram(String name, Histogram histogram, long timestamp, List<String> messages) throws IOException {

        messages.add(buildMessage(prefix(name, "count"), format(histogram.getCount()), timestamp));

        double[] values = prepareDataFromSnapshotForStatistics(histogram.getSnapshot());
        for (int i =0; i<snapshotStatisticsKeys.length;i++) {
            messages.add(buildMessage(prefix(name, snapshotStatisticsKeys[i]), format(values[i]), timestamp));
        }
    }

    private double[] prepareDataFromSnapshotForStatistics(final Snapshot snapshot){
        return new double[]{
                snapshot.getMax(), snapshot.getMean(), snapshot.getMin(),
                snapshot.getStdDev(), snapshot.getMedian(), snapshot.get75thPercentile(),
                snapshot.get95thPercentile(), snapshot.get98thPercentile(),
                snapshot.get99thPercentile(), snapshot.get999thPercentile()};
    }

    private String[] prepareDataFromMeterForRate(final Metered metered){
        return new String[]{
                format(convertRate(metered.getOneMinuteRate())),
                format(convertRate(metered.getFiveMinuteRate())),
                format(convertRate(metered.getFifteenMinuteRate())),
                format(convertRate(metered.getMeanRate()))
        };
    }

    private String buildMessage(String name, String value, long timeStamp){
        return name + " " + value + " " + timeStamp;
    }

    private String format(Object o) {
        if (o instanceof Float) {
            return format(((Float) o).doubleValue());
        } else if (o instanceof Double) {
            return format(((Double) o).doubleValue());
        } else if (o instanceof Byte) {
            return format(((Byte) o).longValue());
        } else if (o instanceof Short) {
            return format(((Short) o).longValue());
        } else if (o instanceof Integer) {
            return format(((Integer) o).longValue());
        } else if (o instanceof Long) {
            return format(((Long) o).longValue());
        }
        return null;
    }

    private String format(long n) {
        return Long.toString(n);
    }

    private String format(double v) {
        return String.format(Locale.US, "%2.2f", v);
    }

    public static class Builder {
        private final MetricRegistry metricRegistry;
        private Clock clock;
        private String prefix;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;

        private Builder(MetricRegistry metricRegistry) {
            this.metricRegistry = metricRegistry;
            this.clock = Clock.defaultClock();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
        }

        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder prefixedWith(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        public SumoMetricReporter build(LogSenderHelper logSenderHelper) {
            return new SumoMetricReporter(metricRegistry, clock, prefix, rateUnit, durationUnit, filter, logSenderHelper);
        }
    }
}
