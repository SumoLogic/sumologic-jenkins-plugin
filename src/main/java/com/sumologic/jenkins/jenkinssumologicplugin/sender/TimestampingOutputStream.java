package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DATETIME_FORMATTER;

/**
 * OutputStream decorator that for each new line in the stream additionally adds a consistent timestamp
 * and forwards the rest of the stream line without modifications.
 * <p>
 * Created by lukasz on 3/21/17.
 */
public class TimestampingOutputStream extends LineTransformationOutputStream {

    private final OutputStream wrappedStream;

    public TimestampingOutputStream(OutputStream stream) {
        super();
        wrappedStream = stream;
    }

    /**
     * Heuristic used for determining multiline log messages, e.g. stack traces.
     * For Sumo Logic purposes only lines prefixed with timestamp will be considered a beginning of new log message.
     *
     * @param bytes - byte array containing single log line
     * @param i     - log line length (can be less that bytes.length)
     * @return false if line starts with whitespace, true otherwise
     */
    public static boolean shouldPutTimestamp(byte[] bytes, int i) {
        String prefix = new String(bytes, 0, Math.min(i, 4), StandardCharsets.UTF_8);

        return prefix.length() > 0 && !Character.isWhitespace(prefix.charAt(0));
    }

    public static byte[] getTimestampAsByteArray(String jobName, String jobNumber) {
        String timeStampStr = "[" + DATETIME_FORMATTER.format(new Date()) + "] " + " " + jobName + "#" + jobNumber + " ";

        return timeStampStr.getBytes();
    }

    public static byte[] getTimestampAsByteArray() {
        String timeStampStr = "[" + DATETIME_FORMATTER.format(new Date()) + "] ";

        return timeStampStr.getBytes();
    }

    @Override
    protected void eol(byte[] bytes, int i) throws IOException {
        if (shouldPutTimestamp(bytes, i)) {
            byte[] timestampBytes = getTimestampAsByteArray();
            wrappedStream.write(timestampBytes, 0, timestampBytes.length);
        }

        wrappedStream.write(bytes, 0, i);
    }
}
