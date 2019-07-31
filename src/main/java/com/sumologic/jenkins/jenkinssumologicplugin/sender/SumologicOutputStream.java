package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import hudson.console.LineTransformationOutputStream;
import hudson.model.Run;
import org.apache.http.util.ByteArrayBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * OutputStream decorator that adds functionality of forwarding the stream to Sumo Logic Http Source.
 * Does not modify the original stream content.
 *
 * Created by lukasz on 3/21/17.
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
  private int maxLinesPerBatch;
  private PluginDescriptorImpl descriptor;

  private State state;

  public SumologicOutputStream(OutputStream stream, Run build, PluginDescriptorImpl descriptor, State state) {
    super();
    wrappedStream = stream;
    logSender = LogSender.getInstance();

    this.descriptor = descriptor;
    this.jobName = build.getParent().getDisplayName();
    this.jobNumber = build.getDisplayName();
    maxLinesPerBatch = descriptor.getMaxLinesInt();

    this.url = descriptor.getUrl();

    this.state = state;
  }

  public SumologicOutputStream(OutputStream stream, String buildName, String buildNumber, PluginDescriptorImpl descriptor,
                               State state) {
    super();
    wrappedStream = stream;
    logSender = LogSender.getInstance();

    this.descriptor = descriptor;
    this.jobName = buildName;
    this.jobNumber = "#" + buildNumber;
    maxLinesPerBatch = descriptor.getMaxLinesInt();

    this.url = descriptor.getUrl();

    this.state = state;
  }

  @Override
  public void close() throws IOException {
    flushBuffer();
    super.close();
  }

  @Override
  protected void eol(byte[] bytes, int i) throws IOException {
    if (new String(bytes).startsWith(FLUSH_COMMAND)) {
      flushBuffer();
      return;
    }

    if (TimestampingOutputStream.shouldPutTimestamp(bytes, i)) {
      byte[] timestamp = TimestampingOutputStream.getTimestampAsByteArray();
      try {
        state.buffer.append(timestamp, 0, timestamp.length);
      } catch (ArrayIndexOutOfBoundsException e) {
        LOGGER.warning("Failed to append timestamp " + new String(timestamp) + ", its length was " +
            timestamp.length + ". The buffer length was " + state.buffer.buffer().length + " or " +
            state.buffer.length());
      }
    }

    state.buffer.append(bytes, 0, i);
    state.currentLines++;

    wrappedStream.write(bytes, 0, i);

    if (state.currentLines >= maxLinesPerBatch) {
      flushBuffer();
    }
  }

  private synchronized void flushBuffer(){
    if (state.currentLines <= 0) {
      return;
    }

    byte[] lines = state.buffer.toByteArray();
    state.buffer.clear();
    state.currentLines = 0;

    try {
      // jobNumber is a build number with #, e.g. #42
      LOGGER.info("Sending " + lines.length + " bytes of build logs to sumo");
      logSender.sendLogs(url, lines, jobName + jobNumber, descriptor.getSourceCategoryBuildLogs());
    } catch (Exception e) {
      e.printStackTrace(new PrintStream(wrappedStream));
    }
  }
}
