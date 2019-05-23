package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;
import hudson.model.Run;
import org.apache.http.util.ByteArrayBuffer;

import java.io.*;
import java.util.logging.Logger;

import static com.sumologic.jenkins.jenkinssumologicplugin.constants.SumoConstants.DIVIDER_FOR_MESSAGES;

/**
 * OutputStream decorator that adds functionality of forwarding the stream to Sumo Logic Http Source.
 * Does not modify the original stream content.
 * <p>
 * Created by lukasz on 3/21/17.
 * <p>
 * Modified by Sourabh Jain 5/2019
 */
public class SumologicOutputStream extends LineTransformationOutputStream {

    static public class State implements Serializable {
        ByteArrayBuffer buffer;
        Integer currentLines;

        public State() {
            buffer = new ByteArrayBuffer(1);
            currentLines = 0;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(SumologicOutputStream.class.getName());

    private static final String FLUSH_COMMAND = "%%%FLUSH_COMMAND%%%";
    private LogSender logSender;
    private OutputStream wrappedStream;

    private String url;
    private String jobName;
    private String jobNumber;
    private PluginDescriptorImpl descriptor;

    private State state;

    public SumologicOutputStream(OutputStream stream, Run build, PluginDescriptorImpl descriptor, State state) {
        super();
        wrappedStream = stream;
        logSender = LogSender.getInstance();

        this.descriptor = descriptor;
        this.jobName = build.getParent().getDisplayName();
        this.jobNumber = String.valueOf(build.getNumber());

        this.url = descriptor.getUrl();

        this.state = state != null ? state : new State();
    }

    @Override
    public void close() throws IOException {
        flushBuffer();
        super.close();
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        if (state.currentLines > 0) {
            flushBuffer();
        }
    }

    @Override
    protected void eol(byte[] bytes, int i) throws IOException {
        if (new String(bytes).startsWith(FLUSH_COMMAND)) {
            flushBuffer();
            return;
        }
        //Append JobName and Number to Console Logs
        if (TimestampingOutputStream.shouldPutTimestamp(bytes, i)) {
            byte[] timestamp = TimestampingOutputStream.getTimestampAsByteArray(jobName, jobNumber);
            state.buffer.append(timestamp, 0, timestamp.length);
        }

        state.buffer.append(bytes, 0, i);
        state.currentLines++;

        wrappedStream.write(bytes, 0, i);

        if (state.currentLines >= DIVIDER_FOR_MESSAGES) {
            flushBuffer();
        }

    }

    private synchronized void flushBuffer() {
        if (state.currentLines <= 0) {
            return;
        }

        byte[] lines = state.buffer.toByteArray();
        state.buffer.clear();
        state.currentLines = 0;

        try {
            // jobNumber is a build number with #, e.g. #42
            LOGGER.info("Sending " + lines.length + " bytes of build logs to sumo");
            logSender.sendLogs(url, lines, null, descriptor.getSourceCategory());
        } catch (Exception e) {
            e.printStackTrace(new PrintStream(wrappedStream));
        }
    }
}
