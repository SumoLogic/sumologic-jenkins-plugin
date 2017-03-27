package com.sumologic.jenkins.jenkinssumologicplugin.sender;

import com.sumologic.jenkins.jenkinssumologicplugin.PluginDescriptorImpl;
import hudson.console.LineTransformationOutputStream;
import hudson.model.AbstractBuild;
import org.apache.http.util.ByteArrayBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Created by lukasz on 3/21/17.
 */
public class SumologicOutputStream extends LineTransformationOutputStream {

  private static final Logger LOGGER = Logger.getLogger(SumologicOutputStream.class.getName());
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS ZZZZ");

  private LogSender logSender;
  private OutputStream wrappedStream;
  private ByteArrayBuffer buffer;

  private String url;
  private String jobName;
  private String jobNumber;
  private int maxLinesPerBatch;
  private int currentLines;

  private boolean timestampingEnabled;

  public SumologicOutputStream(OutputStream stream, AbstractBuild build, PluginDescriptorImpl descriptor) {
    super();
    wrappedStream = stream;
    logSender = LogSender.getInstance();

    this.jobName = build.getProject().getDisplayName();
    this.jobNumber = build.getDisplayName();

    timestampingEnabled = descriptor.isTimestampingEnabled();
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

    String timeStampStr = "[" + DATE_FORMAT.format(new Date()) + "] ";
    byte[] timestampBytes = timeStampStr.getBytes();
    buffer.append(timestampBytes, 0, timestampBytes.length);
    buffer.append(bytes, 0, i);

    if (timestampingEnabled) {
      wrappedStream.write(timestampBytes, 0, timestampBytes.length);
    }

    wrappedStream.write(bytes, 0, i);
    currentLines++;

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
      logSender.sendLogs(url, lines, jobName + jobNumber, "jenkinsStatus");
    }
    catch (Exception e)
    {
      e.printStackTrace(new PrintStream(wrappedStream));
    }
  }
}
