package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import hudson.console.LineTransformationOutputStream;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import org.apache.http.util.ByteArrayBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * OutputStream decorator that adds functionality of forwarding the stream to Sumo Logic Http Source.
 * Does not modify the original stream content.
 *
 * Created by lukasz on 3/21/17.
 */
public class SumologicOutputStream extends LineTransformationOutputStream {

  private static final Logger LOGGER = Logger.getLogger(SumologicOutputStream.class.getName());

  private LogSender logSender;
  private OutputStream wrappedStream;
  private ByteArrayBuffer buffer;

  private String url;
  private String jobName;
  private String jobNumber;
  private int maxLinesPerBatch;
  private int currentLines;
  private PluginDescriptorImpl descriptor;

  public SumologicOutputStream(OutputStream stream, Run build, PluginDescriptorImpl descriptor) {
    super();
    wrappedStream = stream;
    logSender = LogSender.getInstance();

    this.descriptor = descriptor;
    this.jobName = build.getParent().getDisplayName();
    this.jobNumber = build.getDisplayName();
    maxLinesPerBatch = descriptor.getMaxLinesInt();

    currentLines = 0;
    buffer = new ByteArrayBuffer(1);
    this.url = descriptor.getUrl();
  }

  @Override
  public void close() throws IOException {
    super.close();
    flushBuffer();
  }

  @Override
  protected void eol(byte[] bytes, int i) throws IOException {
    if (TimestampingOutputStream.shouldPutTimestamp(bytes, i)) {
      byte[] timestamp = TimestampingOutputStream.getTimestampAsByteArray();
      buffer.append(timestamp, 0, timestamp.length);
    }

    buffer.append(bytes, 0, i);
    currentLines++;

    wrappedStream.write(bytes, 0, i);

    if (currentLines >= maxLinesPerBatch) {
      flushBuffer();
    }
  }

  private synchronized void flushBuffer(){
    if (currentLines <= 0) {
      return;
    }

    byte[] lines = buffer.toByteArray();
    buffer.clear();
    currentLines = 0;

    try {
      // jobNumber is a build number with #, e.g. #42
      logSender.sendLogs(url, lines, jobName + jobNumber, descriptor.getSourceCategoryBuildLogs());
    } catch (Exception e) {
      e.printStackTrace(new PrintStream(wrappedStream));
    }
  }
}
