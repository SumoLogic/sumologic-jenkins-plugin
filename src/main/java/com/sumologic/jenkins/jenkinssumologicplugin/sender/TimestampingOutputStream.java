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
 * OutputStream decorator that for each new line in the stream additionally adds a consistent timestamp
 * and forwards the rest of the stream line without modifications.
 *
 * Created by lukasz on 3/21/17.
 */
public class TimestampingOutputStream extends LineTransformationOutputStream {

  private static final Logger LOGGER = Logger.getLogger(TimestampingOutputStream.class.getName());
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS ZZZZ");

  private OutputStream wrappedStream;

  public TimestampingOutputStream(OutputStream stream) {
    super();
    wrappedStream = stream;
  }


  @Override
  protected void eol(byte[] bytes, int i) throws IOException {
    String timeStampStr = "[" + DATE_FORMAT.format(new Date()) + "] ";
    byte[] timestampBytes = timeStampStr.getBytes();

    wrappedStream.write(timestampBytes, 0, timestampBytes.length);
    wrappedStream.write(bytes, 0, i);
  }
}
